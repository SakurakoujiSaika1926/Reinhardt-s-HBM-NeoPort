package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.IcfControllerBlock;
import com.reinhardt.hbm.block.IcfLaserComponentBlock;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact graph assembly and one-tick discharge logic from TileEntityICFController. */
public final class IcfControllerBlockEntity extends BlockEntity implements PowerEndpoint {
    public static final int MAX_SIZE = 1024;
    public static final long CAPACITOR_POWER = 2_500_000L;
    public static final long TURBO_POWER = 5_000_000L;

    private long power;
    private int laserLength;
    private int cellCount;
    private int emitterCount;
    private int capacitorCount;
    private int turbochargerCount;
    private List<BlockPos> ports = List.of();
    private boolean assembled;

    public IcfControllerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.ICF_CONTROLLER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IcfControllerBlockEntity controller) {
        if (level.isClientSide) {
            return;
        }
        controller.tickServer(level);
    }

    public boolean assembled() {
        return assembled;
    }

    public void setAssembled(boolean assembled) {
        if (this.assembled != assembled) {
            this.assembled = assembled;
            markDirtyAndSync();
            if (level != null) {
                PowerNetworkManager.markDirty(level);
            }
        }
    }

    public long power() {
        return Math.min(power, maxPower());
    }

    public int laserLength() {
        return laserLength;
    }

    public int capacitorCount() {
        return capacitorCount;
    }

    public int turbochargerCount() {
        return turbochargerCount;
    }

    public long maxPower() {
        return (long) (Math.sqrt(capacitorCount) * CAPACITOR_POWER
                + Math.sqrt(Math.min(turbochargerCount, capacitorCount)) * TURBO_POWER);
    }

    public void assemble(Player player) {
        if (this.level == null || this.level.isClientSide || this.assembled) {
            return;
        }
        Direction backward = facing().getOpposite();
        BlockPos first = this.worldPosition.relative(backward);
        Map<BlockPos, Integer> found = new HashMap<>();
        Set<BlockPos> casing = new HashSet<>();
        Set<BlockPos> foundPorts = new HashSet<>();
        Set<BlockPos> cells = new HashSet<>();
        Set<BlockPos> emitters = new HashSet<>();
        Set<BlockPos> capacitors = new HashSet<>();
        Set<BlockPos> turbos = new HashSet<>();
        String error = floodFill(first, found, casing, foundPorts, cells, emitters, capacitors, turbos);
        if (error != null) {
            player.displayClientMessage(Component.literal(error), true);
            return;
        }

        for (Map.Entry<BlockPos, Integer> entry : found.entrySet()) {
            BlockPos pos = entry.getKey();
            int variant = entry.getValue();
            BlockState assembledState = HbmBlocks.ICF_BLOCK.get().defaultBlockState()
                    .setValue(com.reinhardt.hbm.block.LegacyVariantBlock.VARIANT,
                            variant == IcfLaserComponentBlock.Part.PORT.ordinal() ? 1 : 0);
            this.level.setBlock(pos, assembledState, Block.UPDATE_ALL);
            if (this.level.getBlockEntity(pos) instanceof IcfAssembledLaserBlockEntity assembledPart) {
                assembledPart.configure(HbmBlocks.ICF_LASER_COMPONENT.get(), variant, this.worldPosition);
            }
        }

        setup(foundPorts, cells, emitters, capacitors, turbos);
        this.assembled = true;
        markDirtyAndSync();
        PowerNetworkManager.markDirty(this.level);
    }

    private String floodFill(
            BlockPos start,
            Map<BlockPos, Integer> found,
            Set<BlockPos> casing,
            Set<BlockPos> foundPorts,
            Set<BlockPos> cells,
            Set<BlockPos> emitters,
            Set<BlockPos> capacitors,
            Set<BlockPos> turbos
    ) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (found.containsKey(pos)) {
                continue;
            }
            if (found.size() >= MAX_SIZE) {
                return "Max size exceeded";
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(HbmBlocks.ICF_LASER_COMPONENT.get())) {
                return "Non-laser block";
            }
            int variant = state.getValue(com.reinhardt.hbm.block.LegacyVariantBlock.VARIANT);
            IcfLaserComponentBlock.Part part = IcfLaserComponentBlock.Part.byVariant(variant);
            found.put(pos.immutable(), variant);
            switch (part) {
                case CASING -> casing.add(pos.immutable());
                case PORT -> foundPorts.add(pos.immutable());
                case CELL -> {
                    cells.add(pos.immutable());
                    enqueueNeighbors(queue, pos);
                }
                case EMITTER -> {
                    emitters.add(pos.immutable());
                    enqueueNeighbors(queue, pos);
                }
                case CAPACITOR -> {
                    capacitors.add(pos.immutable());
                    enqueueNeighbors(queue, pos);
                }
                case TURBO -> {
                    turbos.add(pos.immutable());
                    enqueueNeighbors(queue, pos);
                }
            }
        }
        return null;
    }

    private void setup(Set<BlockPos> foundPorts, Set<BlockPos> cells, Set<BlockPos> emitters,
                       Set<BlockPos> capacitors, Set<BlockPos> turbos) {
        this.cellCount = 0;
        this.emitterCount = 0;
        this.capacitorCount = 0;
        this.turbochargerCount = 0;
        Direction backward = facing().getOpposite();
        Set<BlockPos> validCells = new HashSet<>();
        Set<BlockPos> validEmitters = new HashSet<>();
        Set<BlockPos> validCapacitors = new HashSet<>();

        for (int distance = 1; cells.contains(worldPosition.relative(backward, distance)); distance++) {
            BlockPos cell = worldPosition.relative(backward, distance).immutable();
            validCells.add(cell);
            cellCount++;
        }
        for (BlockPos emitter : emitters) {
            if (touches(emitter, validCells)) {
                validEmitters.add(emitter);
                emitterCount++;
            }
        }
        for (BlockPos capacitor : capacitors) {
            if (touches(capacitor, validEmitters)) {
                validCapacitors.add(capacitor);
                capacitorCount++;
            }
        }
        for (BlockPos turbo : turbos) {
            if (touches(turbo, validCapacitors)) {
                turbochargerCount++;
            }
        }
        this.ports = foundPorts.stream()
                .map(BlockPos::immutable)
                .sorted(Comparator.comparingInt((BlockPos pos) -> pos.getX())
                        .thenComparingInt(pos -> pos.getY())
                        .thenComparingInt(pos -> pos.getZ()))
                .toList();
        this.power = Math.min(this.power, maxPower());
    }

    private static boolean touches(BlockPos pos, Set<BlockPos> targets) {
        for (Direction direction : Direction.values()) {
            if (targets.contains(pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    private static void enqueueNeighbors(ArrayDeque<BlockPos> queue, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            queue.addLast(pos.relative(direction));
        }
    }

    private void tickServer(Level level) {
        PowerNetworkManager.tickFromEndpoint(level, this);
        if (!assembled || power <= 0L) {
            laserLength = 0;
            if (level.getGameTime() % 10L == 0L) {
                markDirtyAndSync();
            }
            return;
        }

        Direction forward = facing();
        for (int distance = 1; distance < 50; distance++) {
            this.laserLength = distance;
            BlockPos hit = this.worldPosition.relative(forward, distance);
            BlockEntity hitEntity = level.getBlockEntity(hit);
            IcfCoreBlockEntity core = hitEntity instanceof IcfCoreBlockEntity direct ? direct
                    : hitEntity instanceof MachineDummyBlockEntity dummy && dummy.core() instanceof IcfCoreBlockEntity reactor ? reactor
                    : null;
            if (core != null) {
                core.receiveLaser(power(), maxPower());
                break;
            }
            BlockState state = level.getBlockState(hit);
            if (!state.isAir()) {
                if (state.getBlock().getExplosionResistance() < 6000.0F) {
                    level.destroyBlock(hit, false);
                }
                break;
            }
        }

        AABB beam = beamBounds(forward, laserLength);
        for (Entity entity : level.getEntities(null, beam)) {
            entity.hurt(level.damageSources().inFire(), 50.0F);
            entity.igniteForSeconds(5.0F);
        }
        if (level instanceof ServerLevel serverLevel && level.random.nextInt(5) == 0) {
            Direction side = forward.getClockWise();
            double offsetXZ = level.random.nextDouble() * 0.25D - 0.125D;
            double offsetY = level.random.nextDouble() * 0.25D - 0.125D;
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.0F),
                    worldPosition.getX() + 0.5D + forward.getStepX() * 0.55D + side.getStepX() * offsetXZ,
                    worldPosition.getY() + 0.5D + offsetY,
                    worldPosition.getZ() + 0.5D + forward.getStepZ() * 0.55D + side.getStepZ() * offsetXZ,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        power = 0L;
        markDirtyAndSync();
    }

    private AABB beamBounds(Direction direction, int distance) {
        double startX = worldPosition.getX() + 0.2D;
        double startY = worldPosition.getY() + 0.2D;
        double startZ = worldPosition.getZ() + 0.2D;
        double endX = worldPosition.getX() + direction.getStepX() * distance + 0.8D;
        double endY = worldPosition.getY() + direction.getStepY() * distance + 0.8D;
        double endZ = worldPosition.getZ() + direction.getStepZ() * distance + 0.8D;
        return new AABB(Math.min(startX, endX), Math.min(startY, endY), Math.min(startZ, endZ),
                Math.max(startX, endX), Math.max(startY, endY), Math.max(startZ, endZ));
    }

    private Direction facing() {
        return getBlockState().hasProperty(IcfControllerBlock.FACING)
                ? getBlockState().getValue(IcfControllerBlock.FACING)
                : Direction.SOUTH;
    }

    @Override
    public BlockPos getPowerPos() {
        return worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return ports;
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return assembled && ports.contains(connectorPos);
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return assembled ? Math.max(0L, maxPower() - power) : 0L;
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        if (receivedInput > 0L) {
            power = Math.min(maxPower(), power + receivedInput);
            markDirtyAndSync();
        }
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(power() + " / " + maxPower() + " HE");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Power", power);
        tag.putBoolean("Assembled", assembled);
        tag.putInt("CellCount", cellCount);
        tag.putInt("EmitterCount", emitterCount);
        tag.putInt("CapacitorCount", capacitorCount);
        tag.putInt("TurbochargerCount", turbochargerCount);
        tag.putInt("LaserLength", laserLength);
        tag.putInt("PortCount", ports.size());
        for (int index = 0; index < ports.size(); index++) {
            BlockPos port = ports.get(index);
            tag.putIntArray("Port" + index, new int[]{port.getX(), port.getY(), port.getZ()});
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = Math.max(0L, tag.getLong("Power"));
        assembled = tag.getBoolean("Assembled");
        cellCount = Math.max(0, tag.getInt("CellCount"));
        emitterCount = Math.max(0, tag.getInt("EmitterCount"));
        capacitorCount = Math.max(0, tag.getInt("CapacitorCount"));
        turbochargerCount = Math.max(0, tag.getInt("TurbochargerCount"));
        laserLength = Math.max(0, tag.getInt("LaserLength"));
        ArrayList<BlockPos> loadedPorts = new ArrayList<>();
        for (int index = 0; index < Math.min(MAX_SIZE, tag.getInt("PortCount")); index++) {
            int[] values = tag.getIntArray("Port" + index);
            if (values.length == 3) {
                loadedPorts.add(new BlockPos(values[0], values[1], values[2]));
            }
        }
        ports = List.copyOf(loadedPorts);
        power = Math.min(power, maxPower());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
