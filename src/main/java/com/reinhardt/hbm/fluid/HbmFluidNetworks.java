package com.reinhardt.hbm.fluid;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.FluidTankBlockEntity;
import com.reinhardt.hbm.blockentity.FluidPipeBlockEntity;
import com.reinhardt.hbm.integration.sable.HbmSablePowerCompat;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.EnumSet;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class HbmFluidNetworks {
    private static final int MAX_PIPE_SEARCH = 1024;
    private static final int MAX_BALANCE_PER_ENDPOINT = 1_000_000_000;
    private static final long PRUNE_INTERVAL_TICKS = 20L;
    private static final TagKey<Block> MOBILE_FLUID_COMPATIBLE = TagKey.create(
            Registries.BLOCK,
            ReinhardtsHBM.id("mobile_fluid_compatible")
    );
    private static final ExecutorService SOLVER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "RHbm-FluidNetSolver");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<NetworkKey, LevelNetwork> NETWORKS = new ConcurrentHashMap<>();

    private HbmFluidNetworks() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            for (Map.Entry<NetworkKey, LevelNetwork> entry : NETWORKS.entrySet()) {
                if (entry.getKey().dimension().equals(level.dimension())) {
                    entry.getValue().tickBalance(level);
                }
            }
        }
    }

    public static void registerPipe(Level level, BlockPos pos, HbmFluidDefinition type, boolean open) {
        if (!level.isClientSide) {
            if (!mobileFluidNodeAllowed(level, pos)) {
                removePipe(level, pos);
                return;
            }
            Set<String> names = type == null || type.isNone() ? Set.of() : Set.of(type.name());
            network(level, pos).upsert(pos, names, open, networkLinks(level, pos),
                    allowedDirections(level, pos, type == null ? List.of() : List.of(type)));
        }
    }

    public static void registerPipe(Level level, BlockPos pos, List<HbmFluidDefinition> types, boolean open) {
        if (!level.isClientSide) {
            if (!mobileFluidNodeAllowed(level, pos)) {
                removePipe(level, pos);
                return;
            }
            Set<String> typeNames = new HashSet<>();
            if (types != null) {
                for (HbmFluidDefinition type : types) {
                    if (type != null && !type.isNone()) {
                        typeNames.add(type.name());
                    }
                }
            }
            network(level, pos).upsert(pos, typeNames, open, networkLinks(level, pos),
                    allowedDirections(level, pos, types == null ? List.of() : types));
        }
    }

    private static List<BlockPos> networkLinks(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof FluidPipeBlockEntity pipe)) {
            return List.of();
        }
        ArrayList<BlockPos> links = new ArrayList<>();
        for (BlockPos link : pipe.networkLinks()) {
            if (link != null && sameFluidSpace(level, pos, link) && mobileFluidNodeAllowed(level, link)) {
                links.add(link.immutable());
            }
        }
        return List.copyOf(links);
    }

    private static Set<Direction> allowedDirections(Level level, BlockPos pos, List<HbmFluidDefinition> types) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof FluidPipeBlockEntity pipe)) {
            return Set.of();
        }
        EnumSet<Direction> directions = EnumSet.noneOf(Direction.class);
        for (Direction direction : Direction.values()) {
            for (HbmFluidDefinition type : types) {
                if (type != null && pipe.canConnectFrom(direction, type)) {
                    directions.add(direction);
                    break;
                }
            }
        }
        return Set.copyOf(directions);
    }

    public static void unregisterPipe(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            removePipe(level, pos);
        }
    }

    public static boolean canPipeConnect(LevelAccessor level, BlockPos pipePos, Direction direction, HbmFluidDefinition type) {
        if (!mobileFluidNodeAllowed(level, pipePos)) {
            return false;
        }
        BlockEntity current = level.getBlockEntity(pipePos);
        if (!(current instanceof FluidPipeBlockEntity pipe) || !pipe.canConnectFrom(direction, type)) {
            return false;
        }
        BlockPos target = pipePos.relative(direction);
        if (!sameFluidSpace(level, pipePos, target)) {
            return false;
        }
        BlockEntity neighbor = level.getBlockEntity(target);
        if (neighbor instanceof FluidPipeBlockEntity neighborPipe) {
            return mobileFluidNodeAllowed(level, target)
                    && neighborPipe.canConnectFrom(direction.getOpposite(), type);
        }
        if (type == null || type.isNone()) {
            return false;
        }
        if (level instanceof Level realLevel) {
            if (!realLevel.isLoaded(target) || !mobileFluidEndpointAllowed(realLevel, target)) {
                return false;
            }
            return realLevel.getCapability(Capabilities.FluidHandler.BLOCK, target, direction.getOpposite()) != null;
        }
        return false;
    }

    /**
     * The direct modern counterpart to FluidDuctBase#getDebugInfo().  It only
     * reports a live connected component, never a guessed machine endpoint.
     */
    public static List<String> debugInfo(Level level, BlockPos pos) {
        if (!mobileFluidNodeAllowed(level, pos) || !(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe)) {
            return List.of();
        }
        HbmFluidDefinition type = pipe.type();
        if (type == null || type.isNone()) {
            return List.of("Fluid: none", "Links: 0", "Subscribers: 0", "Providers: 0", "Transfer: 0");
        }

        Set<BlockPos> pipes = cachedPipes(level, pos, type);
        int links = 0;
        for (BlockPos pipePos : pipes) {
            for (Direction direction : Direction.values()) {
                if (pipes.contains(pipePos.relative(direction))) {
                    links++;
                }
            }
        }
        int subscribers = 0;
        int providers = 0;
        FluidStack probe = HbmFluids.toNeoStack(type, MAX_BALANCE_PER_ENDPOINT);
        for (Endpoint endpoint : endpoints(level, type, pipes, null).endpoints()) {
            if (endpoint.handler().fill(probe, IFluidHandler.FluidAction.SIMULATE) > 0) {
                subscribers++;
            }
            FluidStack supplied = endpoint.handler().drain(probe, IFluidHandler.FluidAction.SIMULATE);
            if (!supplied.isEmpty() && HbmFluids.fromNeoFluid(supplied.getFluid()).filter(definition -> definition == type).isPresent()) {
                providers++;
            }
        }
        return List.of(
                "Fluid: " + type.name(),
                "Links: " + links / 2,
                "Subscribers: " + subscribers,
                "Providers: " + providers,
                "Transfer: " + network(level, pos).lastTransfer(cursorKey(type, pipes))
        );
    }

    public static FluidStack drainFrom(Level level, BlockPos target, Direction side, HbmFluidDefinition type, int amount, @Nullable BlockPos excluded, boolean execute) {
        if (type == null || type.isNone() || amount <= 0) {
            return FluidStack.EMPTY;
        }
        if (excluded != null && !sameFluidSpace(level, target, excluded)) {
            return FluidStack.EMPTY;
        }
        BlockEntity blockEntity = level.getBlockEntity(target);
        if (blockEntity instanceof FluidPipeBlockEntity pipe && pipe.canConnect(type)) {
            if (!mobileFluidNodeAllowed(level, target)) {
                return FluidStack.EMPTY;
            }
            return drainFromPipeNetwork(level, target, type, amount, excluded, execute);
        }
        if (!mobileFluidEndpointAllowed(level, target)) {
            return FluidStack.EMPTY;
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, target, side);
        if (handler == null) {
            return FluidStack.EMPTY;
        }
        return handler.drain(HbmFluids.toNeoStack(type, amount), execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
    }

    public static int fillInto(Level level, BlockPos target, Direction side, FluidStack stack, @Nullable BlockPos excluded, boolean execute) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (excluded != null && !sameFluidSpace(level, target, excluded)) {
            return 0;
        }
        HbmFluidDefinition type = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
        if (type.isNone()) {
            return 0;
        }
        BlockEntity blockEntity = level.getBlockEntity(target);
        if (blockEntity instanceof FluidPipeBlockEntity pipe && pipe.canConnect(type)) {
            if (!mobileFluidNodeAllowed(level, target)) {
                return 0;
            }
            return fillPipeNetwork(level, target, type, stack, excluded, execute);
        }
        if (!mobileFluidEndpointAllowed(level, target)) {
            return 0;
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, target, side);
        return handler == null ? 0 : handler.fill(stack, execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
    }

    public static FluidStack drainFromPipeNetwork(Level level, BlockPos start, HbmFluidDefinition type, int amount, @Nullable BlockPos excluded, boolean execute) {
        if (type == null || type.isNone() || amount <= 0) {
            return FluidStack.EMPTY;
        }
        if (!mobileFluidNodeAllowed(level, start)
                || excluded != null && !sameFluidSpace(level, start, excluded)) {
            return FluidStack.EMPTY;
        }

        EndpointSelection selection = endpoints(level, start, type, excluded);
        if (selection.endpoints().isEmpty()) {
            return FluidStack.EMPTY;
        }

        LevelNetwork network = network(level, start);
        int startIndex = network.cursor(CursorKind.DRAIN, selection.cursorKey(), selection.endpoints().size());
        int remaining = amount;
        int drainedTotal = 0;
        int lastSuccessIndex = -1;

        for (int i = 0; i < selection.endpoints().size() && remaining > 0; i++) {
            int endpointIndex = (startIndex + i) % selection.endpoints().size();
            Endpoint endpoint = selection.endpoints().get(endpointIndex);
            FluidStack requested = HbmFluids.toNeoStack(type, remaining);
            FluidStack drained = endpoint.handler().drain(requested, execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty()) {
                continue;
            }
            int accepted = Math.min(remaining, drained.getAmount());
            drainedTotal += accepted;
            remaining -= accepted;
            lastSuccessIndex = endpointIndex;
        }

        if (execute && lastSuccessIndex >= 0) {
            network.advance(CursorKind.DRAIN, selection.cursorKey(), lastSuccessIndex + 1, selection.endpoints().size());
        }
        if (execute && drainedTotal > 0) {
            network.recordTransfer(selection.cursorKey(), drainedTotal);
        }
        return drainedTotal <= 0 ? FluidStack.EMPTY : HbmFluids.toNeoStack(type, drainedTotal);
    }

    public static int fillPipeNetwork(Level level, BlockPos start, HbmFluidDefinition type, FluidStack offered, @Nullable BlockPos excluded, boolean execute) {
        if (!mobileFluidNodeAllowed(level, start)
                || excluded != null && !sameFluidSpace(level, start, excluded)) {
            return 0;
        }
        int remaining = offered.getAmount();
        int filled = 0;
        EndpointSelection selection = endpoints(level, start, type, excluded);
        if (selection.endpoints().isEmpty()) {
            return 0;
        }

        LevelNetwork network = network(level, start);
        int startIndex = network.cursor(CursorKind.FILL, selection.cursorKey(), selection.endpoints().size());
        int lastSuccessIndex = -1;

        for (int i = 0; i < selection.endpoints().size() && remaining > 0; i++) {
            int endpointIndex = (startIndex + i) % selection.endpoints().size();
            Endpoint endpoint = selection.endpoints().get(endpointIndex);
            FluidStack slice = offered.copy();
            slice.setAmount(remaining);
            int accepted = endpoint.handler().fill(slice, execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) {
                continue;
            }
            int moved = Math.min(remaining, accepted);
            remaining -= moved;
            filled += moved;
            lastSuccessIndex = endpointIndex;
        }

        if (execute && lastSuccessIndex >= 0) {
            network.advance(CursorKind.FILL, selection.cursorKey(), lastSuccessIndex + 1, selection.endpoints().size());
        }
        if (execute && filled > 0) {
            network.recordTransfer(selection.cursorKey(), filled);
        }
        return filled;
    }

    private static EndpointSelection endpoints(Level level, BlockPos start, HbmFluidDefinition type, @Nullable BlockPos excluded) {
        Set<BlockPos> pipes = cachedPipes(level, start, type);
        return endpoints(level, type, pipes, excluded);
    }

    private static EndpointSelection endpoints(Level level, HbmFluidDefinition type, Set<BlockPos> pipes, @Nullable BlockPos excluded) {
        ArrayList<Endpoint> endpoints = new ArrayList<>();
        for (BlockPos pipePos : pipes) {
            for (Direction direction : Direction.values()) {
                BlockPos target = pipePos.relative(direction);
                if (target.equals(excluded) || pipes.contains(target)
                        || !canPipeConnect(level, pipePos, direction, type)) {
                    continue;
                }
                IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, target, direction.getOpposite());
                if (handler != null) {
                    endpoints.add(new Endpoint(target, direction.getOpposite(), handler));
                }
            }
        }
        endpoints.sort(Comparator
                .comparingInt((Endpoint endpoint) -> endpoint.pos().getX())
                .thenComparingInt(endpoint -> endpoint.pos().getY())
                .thenComparingInt(endpoint -> endpoint.pos().getZ())
                .thenComparingInt(endpoint -> endpoint.side().ordinal()));
        return new EndpointSelection(cursorKey(type, pipes), List.copyOf(endpoints));
    }

    private static EndpointCursorKey cursorKey(HbmFluidDefinition type, Set<BlockPos> pipes) {
        BlockPos anchor = pipes.stream()
                .min(Comparator
                        .comparingInt((BlockPos pos) -> pos.getX())
                        .thenComparingInt(pos -> pos.getY())
                        .thenComparingInt(pos -> pos.getZ()))
                .orElse(BlockPos.ZERO)
                .immutable();
        return new EndpointCursorKey(type == null ? "none" : type.name(), anchor);
    }

    private static BalanceSnapshot createBalanceSnapshot(Level level, long gameTime, Set<PipeComponent> components) {
        List<BalanceComponentSnapshot> snapshots = new ArrayList<>();
        for (PipeComponent component : components) {
            HbmFluidDefinition type = HbmFluids.byName(component.typeName()).orElse(HbmFluids.none());
            if (type.isNone()) {
                continue;
            }

            List<BalanceEndpointSnapshot> endpoints = new ArrayList<>();
            FluidStack probe = HbmFluids.toNeoStack(type, MAX_BALANCE_PER_ENDPOINT);
            for (Endpoint endpoint : endpoints(level, type, component.pipes(), null).endpoints()) {
                int demand = Math.max(0, endpoint.handler().fill(probe, IFluidHandler.FluidAction.SIMULATE));
                FluidStack availableStack = endpoint.handler().drain(probe, IFluidHandler.FluidAction.SIMULATE);
                int available = HbmFluids.fromNeoFluid(availableStack.getFluid())
                        .filter(definition -> definition == type)
                        .map(ignored -> Math.max(0, availableStack.getAmount()))
                        .orElse(0);
                if (demand > 0 || available > 0) {
                    endpoints.add(new BalanceEndpointSnapshot(
                            new EndpointKey(endpoint.pos(), endpoint.side()),
                            available,
                            demand,
                            fluidEndpointPriority(level, endpoint)
                    ));
                }
            }
            if (!endpoints.isEmpty()) {
                snapshots.add(new BalanceComponentSnapshot(type.name(), component.pipes(), endpoints));
            }
        }
        return new BalanceSnapshot(gameTime, snapshots);
    }

    private static PowerEndpoint.ConnectionPriority fluidEndpointPriority(Level level, Endpoint endpoint) {
        BlockEntity blockEntity = level.getBlockEntity(endpoint.pos());
        FluidTankBlockEntity tank = null;
        if (blockEntity instanceof FluidTankBlockEntity directTank) {
            tank = directTank;
        } else if (blockEntity instanceof MachineDummyBlockEntity dummy
                && dummy.core() instanceof FluidTankBlockEntity dummyTank) {
            tank = dummyTank;
        }
        return tank != null && tank.mode() == FluidTankBlockEntity.Mode.BUFFER
                ? PowerEndpoint.ConnectionPriority.LOW
                : PowerEndpoint.ConnectionPriority.NORMAL;
    }

    private static BalanceResult solveBalance(BalanceSnapshot snapshot) {
        List<BalanceComponentResult> results = new ArrayList<>();
        for (BalanceComponentSnapshot component : snapshot.components()) {
            BalanceComponentResult result = solveBalanceComponent(component, snapshot.gameTime());
            if (!result.providerAllocations().isEmpty() && !result.receiverAllocations().isEmpty()) {
                results.add(result);
            }
        }
        return new BalanceResult(results);
    }

    private static BalanceComponentResult solveBalanceComponent(BalanceComponentSnapshot component, long gameTime) {
        long totalAvailable = 0L;
        long totalDemand = 0L;
        for (BalanceEndpointSnapshot endpoint : component.endpoints()) {
            totalAvailable += endpoint.available();
            totalDemand += endpoint.demand();
        }
        int planned = (int) Math.min(Integer.MAX_VALUE, Math.min(totalAvailable, totalDemand));
        if (planned <= 0) {
            return BalanceComponentResult.empty(component.typeName(), component.pipes());
        }

        Map<EndpointKey, Integer> providers = distributeBalanceProviders(component.endpoints(), planned, gameTime + 17L);
        Map<EndpointKey, Integer> receivers = distributeBalanceReceivers(component.endpoints(), planned, gameTime);
        return new BalanceComponentResult(component.typeName(), component.pipes(), providers, receivers);
    }

    private static Map<EndpointKey, Integer> distributeBalanceReceivers(
            List<BalanceEndpointSnapshot> endpoints,
            int planned,
            long rotationSeed
    ) {
        Map<EndpointKey, Integer> allocations = new HashMap<>();
        int remaining = planned;
        for (int i = PowerEndpoint.ConnectionPriority.VALUES.length - 1; i >= 0 && remaining > 0; i--) {
            PowerEndpoint.ConnectionPriority priority = PowerEndpoint.ConnectionPriority.VALUES[i];
            List<BalanceShareTarget> targets = rotateBalanceTargets(endpoints.stream()
                    .filter(endpoint -> endpoint.demand() > 0 && endpoint.priority() == priority)
                    .map(endpoint -> new BalanceShareTarget(endpoint.key(), endpoint.demand()))
                    .toList(), rotationSeed + i * 31L);
            long priorityDemand = 0L;
            for (BalanceShareTarget target : targets) {
                priorityDemand += target.capacity();
            }
            int priorityPlanned = (int) Math.min(remaining, priorityDemand);
            allocations.putAll(distributeBalanceShares(targets, priorityDemand, priorityPlanned));
            remaining -= priorityPlanned;
        }
        return allocations;
    }

    private static Map<EndpointKey, Integer> distributeBalanceProviders(
            List<BalanceEndpointSnapshot> endpoints,
            int planned,
            long rotationSeed
    ) {
        List<BalanceShareTarget> targets = rotateBalanceTargets(endpoints.stream()
                .filter(endpoint -> endpoint.available() > 0)
                .map(endpoint -> new BalanceShareTarget(endpoint.key(), endpoint.available()))
                .toList(), rotationSeed);
        long totalAvailable = 0L;
        for (BalanceShareTarget target : targets) {
            totalAvailable += target.capacity();
        }
        return distributeBalanceShares(targets, totalAvailable, planned);
    }

    private static Map<EndpointKey, Integer> distributeBalanceShares(List<BalanceShareTarget> targets, long total, int planned) {
        Map<EndpointKey, Integer> allocations = new HashMap<>();
        if (targets.isEmpty() || total <= 0L || planned <= 0) {
            return allocations;
        }

        int remaining = planned;
        List<BalanceShare> shares = new ArrayList<>();
        for (BalanceShareTarget target : targets) {
            long numerator = (long) planned * target.capacity();
            int allocated = (int) Math.min(target.capacity(), numerator / total);
            long remainder = numerator % total;
            remaining -= allocated;
            BalanceShare share = new BalanceShare(target.key(), target.capacity(), allocated, remainder);
            shares.add(share);
        }

        List<BalanceShare> byRemainder = new ArrayList<>(shares);
        byRemainder.sort(Comparator.comparingLong((BalanceShare share) -> share.remainder).reversed());
        for (BalanceShare share : byRemainder) {
            if (remaining <= 0) {
                break;
            }
            if (share.allocated < share.capacity) {
                share.allocated++;
                remaining--;
            }
        }
        for (BalanceShare share : shares) {
            if (share.allocated > 0) {
                allocations.put(share.key, share.allocated);
            }
        }
        return allocations;
    }

    private static List<BalanceShareTarget> rotateBalanceTargets(List<BalanceShareTarget> targets, long seed) {
        if (targets.size() <= 1) {
            return targets;
        }
        int offset = (int) Math.floorMod(seed, (long) targets.size());
        if (offset == 0) {
            return targets;
        }
        List<BalanceShareTarget> rotated = new ArrayList<>(targets.size());
        rotated.addAll(targets.subList(offset, targets.size()));
        rotated.addAll(targets.subList(0, offset));
        return rotated;
    }

    private static void applyBalance(Level level, BalanceResult result, LevelNetwork network) {
        for (BalanceComponentResult component : result.components()) {
            HbmFluidDefinition type = HbmFluids.byName(component.typeName()).orElse(HbmFluids.none());
            if (type.isNone()) {
                continue;
            }
            int drained = drainBalanceProviders(level, type, component.providerAllocations(), network.key);
            if (drained <= 0) {
                continue;
            }
            network.recordTransfer(cursorKey(type, component.pipes()), drained);
            Map<EndpointKey, Integer> receiverAllocations = component.receiverAllocations();
            int plannedReceivers = 0;
            for (int amount : receiverAllocations.values()) {
                plannedReceivers += amount;
            }
            if (drained < plannedReceivers) {
                List<BalanceShareTarget> targets = new ArrayList<>();
                for (Map.Entry<EndpointKey, Integer> entry : receiverAllocations.entrySet()) {
                    targets.add(new BalanceShareTarget(entry.getKey(), entry.getValue()));
                }
                receiverAllocations = distributeBalanceShares(targets, plannedReceivers, drained);
            }
            fillBalanceReceivers(level, type, receiverAllocations, network.key);
        }
    }

    private static int drainBalanceProviders(Level level, HbmFluidDefinition type, Map<EndpointKey, Integer> allocations,
                                             NetworkKey networkKey) {
        int drained = 0;
        for (Map.Entry<EndpointKey, Integer> entry : allocations.entrySet()) {
            if (!fluidEndpointMatchesNetwork(level, entry.getKey().pos(), networkKey)) {
                continue;
            }
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, entry.getKey().pos(), entry.getKey().side());
            if (handler == null) {
                continue;
            }
            FluidStack stack = handler.drain(HbmFluids.toNeoStack(type, entry.getValue()), IFluidHandler.FluidAction.EXECUTE);
            if (!stack.isEmpty() && HbmFluids.fromNeoFluid(stack.getFluid()).filter(definition -> definition == type).isPresent()) {
                drained += stack.getAmount();
            }
        }
        return drained;
    }

    private static void fillBalanceReceivers(Level level, HbmFluidDefinition type, Map<EndpointKey, Integer> allocations,
                                             NetworkKey networkKey) {
        for (Map.Entry<EndpointKey, Integer> entry : allocations.entrySet()) {
            if (!fluidEndpointMatchesNetwork(level, entry.getKey().pos(), networkKey)) {
                continue;
            }
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, entry.getKey().pos(), entry.getKey().side());
            if (handler != null) {
                handler.fill(HbmFluids.toNeoStack(type, entry.getValue()), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private static Set<BlockPos> cachedPipes(Level level, BlockPos start, HbmFluidDefinition type) {
        Set<BlockPos> cached = network(level, start).componentFor(start, type);
        return cached == null ? collectPipes(level, start, type) : cached;
    }

    private static Set<BlockPos> collectPipes(Level level, BlockPos start, HbmFluidDefinition type) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty() && visited.size() < MAX_PIPE_SEARCH) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof FluidPipeBlockEntity pipe) || !pipe.canConnect(type)) {
                visited.remove(pos);
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (visited.contains(next)) {
                    continue;
                }
                BlockEntity neighbor = level.getBlockEntity(next);
                if (neighbor instanceof FluidPipeBlockEntity nextPipe
                        && canPipeConnect(level, pos, direction, type)) {
                    queue.addLast(next);
                }
            }
            for (BlockPos link : pipe.networkLinks()) {
                BlockEntity remote = level.getBlockEntity(link);
                if (sameFluidSpace(level, pos, link)
                        && mobileFluidNodeAllowed(level, link)
                        && remote instanceof FluidPipeBlockEntity remotePipe
                        && remotePipe.networkLinks().contains(pos)
                        && remotePipe.canConnect(type)) {
                    queue.addLast(link.immutable());
                }
            }
        }
        return visited;
    }

    private static LevelNetwork network(Level level, BlockPos pos) {
        return NETWORKS.computeIfAbsent(networkKey(level, pos), LevelNetwork::new);
    }

    private static NetworkKey networkKey(Level level, BlockPos pos) {
        return new NetworkKey(level.dimension(), HbmSablePowerCompat.powerSpaceId(level, pos));
    }

    private static void removePipe(Level level, BlockPos pos) {
        BlockPos removed = pos.immutable();
        for (Map.Entry<NetworkKey, LevelNetwork> entry : NETWORKS.entrySet()) {
            if (entry.getKey().dimension().equals(level.dimension())) {
                entry.getValue().remove(removed);
            }
        }
    }

    private static boolean sameFluidSpace(LevelAccessor level, BlockPos first, BlockPos second) {
        return HbmSablePowerCompat.samePowerSpace(level, first, second);
    }

    private static boolean mobileFluidNodeAllowed(LevelAccessor level, BlockPos pos) {
        return !HbmSablePowerCompat.isSubLevelBlock(level, pos)
                || level.getBlockState(pos).is(MOBILE_FLUID_COMPATIBLE);
    }

    private static boolean mobileFluidEndpointAllowed(LevelAccessor level, BlockPos pos) {
        if (!HbmSablePowerCompat.isSubLevelBlock(level, pos)) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(MOBILE_FLUID_COMPATIBLE)) {
            return true;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            BlockPos corePos = dummy.getCorePos();
            if (sameFluidSpace(level, pos, corePos)
                    && level.getBlockState(corePos).is(MOBILE_FLUID_COMPATIBLE)) {
                return true;
            }
        }
        return !ReinhardtsHBM.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
    }

    private static boolean fluidEndpointMatchesNetwork(Level level, BlockPos pos, NetworkKey networkKey) {
        return networkKey.equals(networkKey(level, pos)) && mobileFluidEndpointAllowed(level, pos);
    }

    private record Endpoint(BlockPos pos, Direction side, IFluidHandler handler) {
    }

    private record EndpointSelection(EndpointCursorKey cursorKey, List<Endpoint> endpoints) {
    }

    private record EndpointCursorKey(String typeName, BlockPos anchor) {
    }

    private enum CursorKind {
        FILL,
        DRAIN
    }

    private record EndpointKey(BlockPos pos, Direction side) {
    }

    private record BalanceSnapshot(long gameTime, List<BalanceComponentSnapshot> components) {
    }

    private record BalanceComponentSnapshot(String typeName, Set<BlockPos> pipes, List<BalanceEndpointSnapshot> endpoints) {
    }

    private record BalanceEndpointSnapshot(
            EndpointKey key,
            int available,
            int demand,
            PowerEndpoint.ConnectionPriority priority
    ) {
    }

    private record BalanceResult(List<BalanceComponentResult> components) {
        private static final BalanceResult EMPTY = new BalanceResult(List.of());
    }

    private record BalanceComponentResult(
            String typeName,
            Set<BlockPos> pipes,
            Map<EndpointKey, Integer> providerAllocations,
            Map<EndpointKey, Integer> receiverAllocations
    ) {
        private static BalanceComponentResult empty(String typeName, Set<BlockPos> pipes) {
            return new BalanceComponentResult(typeName, pipes, Map.of(), Map.of());
        }
    }

    private record BalanceShareTarget(EndpointKey key, int capacity) {
    }

    private static final class BalanceShare {
        private final EndpointKey key;
        private final int capacity;
        private int allocated;
        private final long remainder;

        private BalanceShare(EndpointKey key, int capacity, int allocated, long remainder) {
            this.key = key;
            this.capacity = capacity;
            this.allocated = allocated;
            this.remainder = remainder;
        }
    }

    private record NetworkKey(ResourceKey<Level> dimension, String fluidSpace) {
    }

    private static final class LevelNetwork {
        private final NetworkKey key;
        private final Map<BlockPos, PipeNode> pipes = new HashMap<>();
        private final Map<EndpointCursorKey, Integer> fillCursors = new HashMap<>();
        private final Map<EndpointCursorKey, Integer> drainCursors = new HashMap<>();
        private final Map<EndpointCursorKey, Integer> lastTransfers = new HashMap<>();
        private final Map<EndpointCursorKey, Integer> currentTransfers = new HashMap<>();
        private Map<PipeComponentKey, PipeComponent> componentsByPipe = Map.of();
        private CompletableFuture<SolveResult> inFlight;
        private CompletableFuture<BalanceResult> balanceInFlight;
        private long graphVersion;
        private long lastBalanceTick = Long.MIN_VALUE;
        private long lastPruneTick = Long.MIN_VALUE;
        private long transferTick = Long.MIN_VALUE;
        private boolean dirty = true;

        private LevelNetwork(NetworkKey key) {
            this.key = key;
        }

        synchronized void upsert(BlockPos pos, Set<String> typeNames, boolean open, List<BlockPos> links,
                                  Set<Direction> directions) {
            Set<BlockPos> linkSet = new HashSet<>();
            if (links != null) {
                for (BlockPos link : links) {
                    if (link != null && !link.equals(pos)) {
                        linkSet.add(link.immutable());
                    }
                }
            }
            PipeNode next = new PipeNode(pos.immutable(), Set.copyOf(typeNames), open,
                    Set.copyOf(linkSet), Set.copyOf(directions));
            PipeNode old = this.pipes.put(next.pos(), next);
            if (!next.equals(old)) {
                markDirty();
            }
            tick();
        }

        synchronized void remove(BlockPos pos) {
            BlockPos removed = pos.immutable();
            boolean changed = this.pipes.remove(removed) != null;
            for (Map.Entry<BlockPos, PipeNode> entry : new ArrayList<>(this.pipes.entrySet())) {
                PipeNode node = entry.getValue();
                if (node.links().contains(removed)) {
                    Set<BlockPos> links = new HashSet<>(node.links());
                    links.remove(removed);
                    this.pipes.put(entry.getKey(), new PipeNode(node.pos(), node.typeNames(), node.open(),
                            Set.copyOf(links), node.directions()));
                    changed = true;
                }
            }
            if (changed) {
                markDirty();
            }
            tick();
        }

        synchronized Set<BlockPos> componentFor(BlockPos start, HbmFluidDefinition type) {
            tick();
            PipeComponent component = this.componentsByPipe.get(new PipeComponentKey(start.immutable(), type == null ? "none" : type.name()));
            if (component == null || type == null || type.isNone()) {
                return null;
            }
            return component.pipes();
        }

        synchronized int cursor(CursorKind kind, EndpointCursorKey key, int size) {
            if (size <= 0) {
                return 0;
            }
            return cursorMap(kind).getOrDefault(key, 0) % size;
        }

        synchronized void advance(CursorKind kind, EndpointCursorKey key, int nextIndex, int size) {
            if (size <= 0) {
                return;
            }
            cursorMap(kind).put(key, Math.floorMod(nextIndex, size));
        }

        synchronized int lastTransfer(EndpointCursorKey key) {
            return this.lastTransfers.getOrDefault(key, 0);
        }

        synchronized void recordTransfer(EndpointCursorKey key, int amount) {
            if (amount > 0) {
                this.currentTransfers.merge(key, amount, Integer::sum);
            }
        }

        void tickBalance(Level level) {
            BalanceResult completed = null;
            Set<PipeComponent> components = Set.of();
            long gameTime = level.getGameTime();
            synchronized (this) {
                if (this.lastPruneTick == Long.MIN_VALUE || gameTime - this.lastPruneTick >= PRUNE_INTERVAL_TICKS) {
                    pruneInvalidPipes(level);
                    this.lastPruneTick = gameTime;
                }
                tick();
                if (this.transferTick != level.getGameTime()) {
                    this.lastTransfers.clear();
                    this.lastTransfers.putAll(this.currentTransfers);
                    this.currentTransfers.clear();
                    this.transferTick = level.getGameTime();
                }
                if (this.balanceInFlight != null && this.balanceInFlight.isDone()) {
                    completed = this.balanceInFlight.join();
                    this.balanceInFlight = null;
                }
                if (!this.componentsByPipe.isEmpty()) {
                    components = Set.copyOf(this.componentsByPipe.values());
                }
            }

            if (completed != null) {
                applyBalance(level, completed, this);
            }

            if (components.isEmpty()) {
                return;
            }
            synchronized (this) {
                if (this.balanceInFlight != null || this.lastBalanceTick == gameTime) {
                    return;
                }
                this.lastBalanceTick = gameTime;
            }

            BalanceSnapshot snapshot = createBalanceSnapshot(level, gameTime, components);
            if (snapshot.components().isEmpty()) {
                return;
            }

            synchronized (this) {
                if (this.balanceInFlight == null) {
                this.balanceInFlight = CompletableFuture.supplyAsync(() -> solveBalance(snapshot), SOLVER);
                }
            }
        }

        private void pruneInvalidPipes(Level level) {
            List<BlockPos> removed = new ArrayList<>();
            for (BlockPos pos : this.pipes.keySet()) {
                if (!this.key.equals(networkKey(level, pos))
                        || !mobileFluidNodeAllowed(level, pos)
                        || !(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity)) {
                    removed.add(pos);
                }
            }
            if (!removed.isEmpty()) {
                for (BlockPos pos : removed) {
                    this.pipes.remove(pos);
                }
                markDirty();
            }
        }

        private void markDirty() {
            this.dirty = true;
            this.graphVersion++;
            this.componentsByPipe = Map.of();
        }

        private Map<EndpointCursorKey, Integer> cursorMap(CursorKind kind) {
            return kind == CursorKind.FILL ? this.fillCursors : this.drainCursors;
        }

        private void tick() {
            if (this.inFlight != null && this.inFlight.isDone()) {
                SolveResult result = this.inFlight.join();
                this.inFlight = null;
                if (result.graphVersion() == this.graphVersion) {
                    this.componentsByPipe = result.componentsByPipe();
                }
            }

            if (this.dirty && this.inFlight == null) {
                SolveSnapshot snapshot = new SolveSnapshot(this.graphVersion, Map.copyOf(this.pipes));
                this.dirty = false;
                this.inFlight = CompletableFuture.supplyAsync(() -> solve(snapshot), SOLVER);
            }
        }
    }

    private static SolveResult solve(SolveSnapshot snapshot) {
        Map<PipeComponentKey, PipeComponent> componentsByPipe = new HashMap<>();
        Set<PipeComponentKey> visited = new HashSet<>();

        for (PipeNode node : snapshot.pipes().values()) {
            if (!node.active()) {
                continue;
            }

            for (String typeName : node.typeNames()) {
                PipeComponentKey startKey = new PipeComponentKey(node.pos(), typeName);
                if (visited.contains(startKey)) {
                    continue;
                }

                List<BlockPos> component = new ArrayList<>();
                ArrayDeque<BlockPos> queue = new ArrayDeque<>();
                queue.add(node.pos());

                while (!queue.isEmpty() && component.size() < MAX_PIPE_SEARCH) {
                    BlockPos current = queue.removeFirst();
                    PipeComponentKey currentKey = new PipeComponentKey(current, typeName);
                    if (!visited.add(currentKey)) {
                        continue;
                    }
                    PipeNode currentNode = snapshot.pipes().get(current);
                    if (currentNode == null || !currentNode.active() || !currentNode.typeNames().contains(typeName)) {
                        continue;
                    }
                    component.add(current);

                    for (Direction direction : Direction.values()) {
                        BlockPos next = current.relative(direction).immutable();
                        PipeComponentKey nextKey = new PipeComponentKey(next, typeName);
                        if (!visited.contains(nextKey)) {
                            PipeNode nextNode = snapshot.pipes().get(next);
                            if (nextNode != null && nextNode.active() && nextNode.typeNames().contains(typeName)
                                    && currentNode.directions().contains(direction)
                                    && nextNode.directions().contains(direction.getOpposite())) {
                                queue.addLast(next);
                            }
                        }
                    }
                    for (BlockPos link : currentNode.links()) {
                        PipeNode nextNode = snapshot.pipes().get(link);
                        PipeComponentKey nextKey = new PipeComponentKey(link, typeName);
                        if (!visited.contains(nextKey)
                                && nextNode != null
                                && nextNode.active()
                                && nextNode.typeNames().contains(typeName)
                                && nextNode.links().contains(current)) {
                            queue.addLast(link);
                        }
                    }
                }

                if (!component.isEmpty()) {
                    Set<BlockPos> immutablePipes = Set.copyOf(component);
                    PipeComponent pipeComponent = new PipeComponent(typeName, immutablePipes);
                    for (BlockPos pos : immutablePipes) {
                        componentsByPipe.put(new PipeComponentKey(pos, typeName), pipeComponent);
                    }
                }
            }
        }

        return new SolveResult(snapshot.graphVersion(), Map.copyOf(componentsByPipe));
    }

    private record PipeNode(BlockPos pos, Set<String> typeNames, boolean open, Set<BlockPos> links,
                            Set<Direction> directions) {
        private boolean active() {
            return open && !typeNames.isEmpty();
        }
    }

    private record PipeComponentKey(BlockPos pos, String typeName) {
    }

    private record PipeComponent(String typeName, Set<BlockPos> pipes) {
    }

    private record SolveSnapshot(long graphVersion, Map<BlockPos, PipeNode> pipes) {
    }

    private record SolveResult(long graphVersion, Map<PipeComponentKey, PipeComponent> componentsByPipe) {
    }
}
