package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.FluidDuctBlock;
import com.reinhardt.hbm.block.MachineDummyBlock;
import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.entity.DigammaSpearEntity;
import com.reinhardt.hbm.entity.RbmkDebrisEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class RbmkMeltdownManager {
    private static final int WORLD_OPS_PER_TICK = 384;
    private static final int DEBRIS_OPS_PER_TICK = 48;
    private static final int OVERPRESSURE_PIPE_SCAN_PER_TICK = 512;
    private static final int OVERPRESSURE_RECEIVER_SCAN_PER_TICK = 192;
    private static final int OVERPRESSURE_REMOVALS_PER_TICK = 32;
    private static final int OVERPRESSURE_MAX_PIPES = 4096;
    private static final ExecutorService PLANNER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "RHbm-RBMK-MeltdownPlanner");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<ResourceLocation, Queue<MeltdownTask>> TASKS = new HashMap<>();

    private RbmkMeltdownManager() {
    }

    public static void schedule(ServerLevel level, Request request) {
        TASKS.computeIfAbsent(level.dimension().location(), unused -> new ArrayDeque<>())
                .add(new MeltdownTask(request));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Queue<MeltdownTask> tasks = TASKS.get(level.dimension().location());
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        MeltdownTask task = tasks.peek();
        if (task.tick(level)) {
            tasks.remove();
        }
        if (tasks.isEmpty()) {
            TASKS.remove(level.dimension().location());
        }
    }

    public record Request(
            BlockPos origin,
            int avgX,
            int avgZ,
            int smallDim,
            boolean digamma,
            long seed,
            List<ColumnSnapshot> columns,
            List<BlockPos> boilerOutputs
    ) {
        public Request {
            origin = origin.immutable();
            columns = List.copyOf(columns);
            boilerOutputs = List.copyOf(boilerOutputs);
        }
    }

    public record ColumnSnapshot(
            BlockPos pos,
            RbmkComponentBlock.Kind kind,
            boolean hasFuel,
            boolean hadNormalLid,
            int reduce,
            int height
    ) {
        public ColumnSnapshot {
            pos = pos.immutable();
        }
    }

    private enum OpKind {
        SET,
        REMOVE,
        SPAWN_DEBRIS,
        RADIATE_CORIUM
    }

    private enum StateKind {
        CORIUM,
        PRIBRIS,
        PRIBRIS_BURNING,
        PRIBRIS_RADIATING,
        PRIBRIS_DIGAMMA
    }

    private record ApplyOp(
            OpKind kind,
            BlockPos pos,
            StateKind state,
            RbmkDebrisEntity.DebrisType debris,
            double motionX,
            double motionY,
            double motionZ
    ) {
        static ApplyOp set(BlockPos pos, StateKind state) {
            return new ApplyOp(OpKind.SET, pos.immutable(), state, null, 0.0D, 0.0D, 0.0D);
        }

        static ApplyOp remove(BlockPos pos) {
            return new ApplyOp(OpKind.REMOVE, pos.immutable(), null, null, 0.0D, 0.0D, 0.0D);
        }

        static ApplyOp debris(BlockPos pos, RbmkDebrisEntity.DebrisType type, Random random) {
            double motionX = random.nextGaussian() * 0.25D;
            double motionZ = random.nextGaussian() * 0.25D;
            double motionY = 0.25D + random.nextDouble() * 1.25D;
            if (type == RbmkDebrisEntity.DebrisType.LID) {
                motionX *= 0.5D;
                motionY += 0.5D;
                motionZ *= 0.5D;
            }
            return new ApplyOp(OpKind.SPAWN_DEBRIS, pos.immutable(), null, type, motionX, motionY, motionZ);
        }

        static ApplyOp radiate(BlockPos pos, boolean digamma) {
            return new ApplyOp(OpKind.RADIATE_CORIUM, pos.immutable(),
                    digamma ? StateKind.PRIBRIS_DIGAMMA : StateKind.PRIBRIS_RADIATING,
                    null, 0.0D, 0.0D, 0.0D);
        }
    }

    private record MeltdownPlan(List<ApplyOp> ops) {
        MeltdownPlan {
            ops = List.copyOf(ops);
        }
    }

    private static final class MeltdownTask {
        private final Request request;
        private final CompletableFuture<MeltdownPlan> planFuture;
        private MeltdownPlan plan;
        private OverpressureTask overpressureTask;
        private int opIndex;
        private boolean effectsSent;

        private MeltdownTask(Request request) {
            this.request = request;
            this.planFuture = CompletableFuture.supplyAsync(() -> createPlan(request), PLANNER);
        }

        private boolean tick(ServerLevel level) {
            if (plan == null) {
                if (!planFuture.isDone()) {
                    return false;
                }
                if (planFuture.isCompletedExceptionally() || planFuture.isCancelled()) {
                    ReinhardtsHBM.LOGGER.error("RBMK meltdown planner failed at {}; cancelling async meltdown", request.origin());
                    clearMeltdownFlags(level);
                    return true;
                }
                plan = planFuture.join();
            }
            if (!effectsSent) {
                sendEffects(level);
                effectsSent = true;
            }
            applyQueuedOps(level);
            if (opIndex < plan.ops().size()) {
                return false;
            }
            if (!HbmConfig.RBMK_OVERPRESSURE.get() || request.boilerOutputs().isEmpty()) {
                return true;
            }
            if (overpressureTask == null) {
                overpressureTask = new OverpressureTask(request.boilerOutputs());
            }
            return overpressureTask.tick(level);
        }

        private void applyQueuedOps(ServerLevel level) {
            int worldOps = 0;
            int debrisOps = 0;
            while (opIndex < plan.ops().size()) {
                ApplyOp op = plan.ops().get(opIndex);
                if (op.kind() == OpKind.SPAWN_DEBRIS) {
                    if (debrisOps >= DEBRIS_OPS_PER_TICK) {
                        return;
                    }
                    spawnDebris(level, op);
                    debrisOps++;
                } else {
                    if (worldOps >= WORLD_OPS_PER_TICK) {
                        return;
                    }
                    applyWorldOp(level, op);
                    worldOps++;
                }
                opIndex++;
            }
        }

        private void clearMeltdownFlags(ServerLevel level) {
            for (ColumnSnapshot column : request.columns()) {
                if (level.getBlockEntity(column.pos()) instanceof RbmkComponentBlockEntity rbmk) {
                    rbmk.finishAsyncMeltdown();
                }
            }
        }

        private void sendEffects(ServerLevel level) {
            double x = request.avgX() + 0.5D;
            double y = request.origin().getY() + 1.0D;
            double z = request.avgZ() + 0.5D;
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(x, y, z) <= 250.0D * 250.0D) {
                    level.sendParticles(player, HbmParticleTypes.RBMK_MUSH.get(), true,
                            x, y, z, 0,
                            request.smallDim(), 0.0D, 0.0D, 1.0D);
                }
            }
            level.playSound(null, x, y, z, HbmSoundEvents.RBMK_EXPLOSION.get(), SoundSource.BLOCKS, 50.0F, 1.0F);
            AdvancementHolder boom = level.getServer().getAdvancements().get(ReinhardtsHBM.id("rbmk_boom"));
            if (boom != null) {
                BlockPos origin = request.origin();
                AABB area = new AABB(
                        origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D,
                        origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D
                ).inflate(50.0D);
                for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
                    player.getAdvancements().award(boom, "meltdown");
                }
            }
            if (request.digamma()) {
                DigammaSpearEntity spear = new DigammaSpearEntity(HbmEntityTypes.DIGAMMA_SPEAR.get(), level);
                spear.setPos(x, request.origin().getY() + 100.0D, z);
                level.addFreshEntity(spear);
            }
        }
    }

    private static MeltdownPlan createPlan(Request request) {
        Random random = new Random(request.seed());
        List<ApplyOp> ops = new ArrayList<>();
        List<BlockPos> coriumCores = new ArrayList<>();
        for (ColumnSnapshot column : request.columns()) {
            if (appendColumnOps(ops, column, random)) {
                coriumCores.add(column.pos());
            }
        }
        for (BlockPos core : coriumCores) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (random.nextInt(3) == 0) {
                            ops.add(ApplyOp.radiate(core.offset(x, y, z), request.digamma()));
                        }
                    }
                }
            }
        }
        return new MeltdownPlan(ops);
    }

    private static boolean appendColumnOps(List<ApplyOp> ops, ColumnSnapshot column, Random random) {
        RbmkComponentBlock.Kind kind = column.kind();
        if (kind.acceptsFuel()) {
            appendFuelRodColumnOps(ops, column, random);
            return column.hasFuel();
        }
        if (kind.isControl()) {
            appendControlColumnOps(ops, column, random);
            return false;
        }
        switch (kind) {
            case BLANK, ABSORBER, REFLECTOR, BOILER, HEATER -> {
                appendDebris(ops, column.pos(), RbmkDebrisEntity.DebrisType.BLANK, 1 + random.nextInt(2), random);
                appendStandardMeltOps(ops, column, random);
                appendLidIfNormal(ops, column, random);
            }
            case OUTGASSER -> {
                appendDebris(ops, column.pos(), RbmkDebrisEntity.DebrisType.BLANK, 4 + random.nextInt(2), random);
                appendStandardMeltOps(ops, column, random);
                appendLidIfNormal(ops, column, random);
            }
            case MODERATOR -> {
                appendDebris(ops, column.pos(), RbmkDebrisEntity.DebrisType.GRAPHITE, 2 + random.nextInt(2), random);
                appendStandardMeltOps(ops, column, random);
                appendLidIfNormal(ops, column, random);
            }
            default -> {
                appendStandardMeltOps(ops, column, random);
                appendLidIfNormal(ops, column, random);
            }
        }
        return false;
    }

    private static void appendFuelRodColumnOps(List<ApplyOp> ops, ColumnSnapshot column, Random random) {
        if (column.hasFuel()) {
            for (int y = column.height() - 1; y >= 0; y--) {
                ops.add(ApplyOp.set(column.pos().above(y), StateKind.CORIUM));
            }
            appendDebris(ops, column.pos(), RbmkDebrisEntity.DebrisType.FUEL,
                    1 + random.nextInt(Math.max(1, column.height() - 1)), random);
        } else {
            appendStandardMeltOps(ops, column, random);
        }
        if (column.kind() == RbmkComponentBlock.Kind.FUEL_ROD_MOD
                || column.kind() == RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD) {
            appendDebris(ops, column.pos(), RbmkDebrisEntity.DebrisType.GRAPHITE, 2 + random.nextInt(2), random);
        }
        appendDebris(ops, column.pos(), RbmkDebrisEntity.DebrisType.ELEMENT, 1, random);
        appendLidIfNormal(ops, column, random);
    }

    private static void appendControlColumnOps(List<ApplyOp> ops, ColumnSnapshot column, Random random) {
        if (column.kind() == RbmkComponentBlock.Kind.CONTROL_MOD) {
            appendDebris(ops, column.pos(), RbmkDebrisEntity.DebrisType.GRAPHITE, 2 + random.nextInt(2), random);
        }
        appendDebris(ops, column.pos(), RbmkDebrisEntity.DebrisType.ROD, 2 + random.nextInt(2), random);
        appendStandardMeltOps(ops, column, random);
    }

    private static void appendStandardMeltOps(List<ApplyOp> ops, ColumnSnapshot column, Random random) {
        int height = column.height();
        int clampedReduce = Math.max(1, Math.min(Math.max(1, height - 1), column.reduce()));
        if (random.nextInt(3) == 0) {
            clampedReduce++;
        }
        for (int y = height - 1; y >= 0; y--) {
            BlockPos target = column.pos().above(y);
            if (y <= height - clampedReduce) {
                if (clampedReduce > 1 && y == height - clampedReduce) {
                    ops.add(ApplyOp.set(target, StateKind.PRIBRIS_BURNING));
                } else {
                    ops.add(ApplyOp.set(target, StateKind.PRIBRIS));
                }
            } else {
                ops.add(ApplyOp.remove(target));
            }
        }
    }

    private static void appendLidIfNormal(List<ApplyOp> ops, ColumnSnapshot column, Random random) {
        if (column.hadNormalLid()) {
            appendDebris(ops, column.pos(), RbmkDebrisEntity.DebrisType.LID, 1, random);
        }
    }

    private static void appendDebris(List<ApplyOp> ops, BlockPos pos, RbmkDebrisEntity.DebrisType type, int count, Random random) {
        for (int i = 0; i < count; i++) {
            ops.add(ApplyOp.debris(pos, type, random));
        }
    }

    private static void applyWorldOp(ServerLevel level, ApplyOp op) {
        switch (op.kind()) {
            case SET -> MachineDummyBlock.runWithoutCoreDestroy(() ->
                    level.setBlock(op.pos(), stateFor(op.state()), 3));
            case REMOVE -> MachineDummyBlock.runWithoutCoreDestroy(() ->
                    level.removeBlock(op.pos(), false));
            case RADIATE_CORIUM -> {
                if (!level.isLoaded(op.pos())) {
                    return;
                }
                BlockState targetState = level.getBlockState(op.pos());
                if (targetState.is(HbmBlocks.PRIBRIS.get()) || targetState.is(HbmBlocks.PRIBRIS_BURNING.get())) {
                    level.setBlock(op.pos(), stateFor(op.state()), 3);
                }
            }
            default -> {
            }
        }
    }

    private static BlockState stateFor(StateKind state) {
        return switch (state) {
            case CORIUM -> HbmBlocks.CORIUM_BLOCK.get().defaultBlockState();
            case PRIBRIS -> HbmBlocks.PRIBRIS.get().defaultBlockState();
            case PRIBRIS_BURNING -> HbmBlocks.PRIBRIS_BURNING.get().defaultBlockState();
            case PRIBRIS_RADIATING -> HbmBlocks.PRIBRIS_RADIATING.get().defaultBlockState();
            case PRIBRIS_DIGAMMA -> HbmBlocks.PRIBRIS_DIGAMMA.get().defaultBlockState();
        };
    }

    private static void spawnDebris(ServerLevel level, ApplyOp op) {
        if (!level.isLoaded(op.pos())) {
            return;
        }
        RbmkDebrisEntity debris = new RbmkDebrisEntity(level,
                op.pos().getX() + 0.5D,
                op.pos().getY() + 4.0D,
                op.pos().getZ() + 0.5D,
                op.debris());
        debris.setDeltaMovement(op.motionX(), op.motionY(), op.motionZ());
        level.addFreshEntity(debris);
    }

    private static final class OverpressureTask {
        private final List<BlockPos> boilerOutputs;
        private final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        private final Set<BlockPos> pipes = new HashSet<>();
        private final Set<BlockPos> receivers = new HashSet<>();
        private Stage stage = Stage.COLLECT_PIPES;
        private Iterator<BlockPos> pipeIterator;
        private Iterator<BlockPos> receiverIterator;
        private int pipeRemoveLimit;
        private int removedPipes;
        private boolean seeded;

        private OverpressureTask(List<BlockPos> boilerOutputs) {
            this.boilerOutputs = List.copyOf(boilerOutputs);
        }

        private boolean tick(ServerLevel level) {
            seed(level);
            return switch (stage) {
                case COLLECT_PIPES -> collectPipes(level);
                case COLLECT_RECEIVERS -> collectReceivers(level);
                case REMOVE_PIPES -> removePipes(level);
                case REMOVE_RECEIVERS -> removeReceivers(level);
                case DONE -> true;
            };
        }

        private void seed(ServerLevel level) {
            if (seeded) {
                return;
            }
            seeded = true;
            for (BlockPos output : boilerOutputs) {
                if (level.isLoaded(output) && level.getBlockState(output).getBlock() instanceof FluidDuctBlock) {
                    queue.add(output.immutable());
                }
            }
        }

        private boolean collectPipes(ServerLevel level) {
            int scanned = 0;
            while (!queue.isEmpty() && pipes.size() < OVERPRESSURE_MAX_PIPES && scanned < OVERPRESSURE_PIPE_SCAN_PER_TICK) {
                BlockPos pipe = queue.removeFirst();
                if (!pipes.add(pipe.immutable())) {
                    continue;
                }
                scanned++;
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = pipe.relative(direction);
                    if (!pipes.contains(neighbor)
                            && level.isLoaded(neighbor)
                            && level.getBlockState(neighbor).getBlock() instanceof FluidDuctBlock) {
                        queue.add(neighbor.immutable());
                    }
                }
            }
            if (queue.isEmpty() || pipes.size() >= OVERPRESSURE_MAX_PIPES) {
                pipeIterator = pipes.iterator();
                stage = Stage.COLLECT_RECEIVERS;
            }
            return false;
        }

        private boolean collectReceivers(ServerLevel level) {
            int scanned = 0;
            while (pipeIterator != null && pipeIterator.hasNext() && scanned < OVERPRESSURE_RECEIVER_SCAN_PER_TICK) {
                BlockPos pipePos = pipeIterator.next();
                scanned++;
                if (!(level.getBlockEntity(pipePos) instanceof FluidPipeBlockEntity pipe)) {
                    continue;
                }
                for (HbmFluidDefinition type : pipe.connectableFluidTypes()) {
                    for (Direction direction : Direction.values()) {
                        BlockPos target = pipePos.relative(direction);
                        if (pipes.contains(target)
                                || !level.isLoaded(target)
                                || !HbmFluidNetworks.canPipeConnect(level, pipePos, direction, type)) {
                            continue;
                        }
                        if (level.getCapability(Capabilities.FluidHandler.BLOCK, target, direction.getOpposite()) != null) {
                            receivers.add(target.immutable());
                        }
                    }
                }
            }
            if (pipeIterator == null || !pipeIterator.hasNext()) {
                pipeRemoveLimit = Math.min(pipes.size() / 5, 100);
                pipeIterator = pipes.iterator();
                stage = Stage.REMOVE_PIPES;
            }
            return false;
        }

        private boolean removePipes(ServerLevel level) {
            int removed = 0;
            while (pipeIterator != null
                    && pipeIterator.hasNext()
                    && removedPipes < pipeRemoveLimit
                    && removed < OVERPRESSURE_REMOVALS_PER_TICK) {
                BlockPos pipe = pipeIterator.next();
                if (level.isLoaded(pipe)) {
                    level.removeBlock(pipe, false);
                }
                removed++;
                removedPipes++;
            }
            if (pipeIterator == null || !pipeIterator.hasNext() || removedPipes >= pipeRemoveLimit) {
                receiverIterator = receivers.iterator();
                stage = Stage.REMOVE_RECEIVERS;
            }
            return false;
        }

        private boolean removeReceivers(ServerLevel level) {
            int removed = 0;
            while (receiverIterator != null
                    && receiverIterator.hasNext()
                    && removed < OVERPRESSURE_REMOVALS_PER_TICK) {
                BlockPos receiver = receiverIterator.next();
                if (level.isLoaded(receiver)) {
                    level.removeBlock(receiver, false);
                    level.explode(null,
                            receiver.getX() + 0.5D,
                            receiver.getY() + 0.5D,
                            receiver.getZ() + 0.5D,
                            5.0F,
                            false,
                            Level.ExplosionInteraction.NONE);
                }
                removed++;
            }
            if (receiverIterator == null || !receiverIterator.hasNext()) {
                stage = Stage.DONE;
                return true;
            }
            return false;
        }

        private enum Stage {
            COLLECT_PIPES,
            COLLECT_RECEIVERS,
            REMOVE_PIPES,
            REMOVE_RECEIVERS,
            DONE
        }
    }
}
