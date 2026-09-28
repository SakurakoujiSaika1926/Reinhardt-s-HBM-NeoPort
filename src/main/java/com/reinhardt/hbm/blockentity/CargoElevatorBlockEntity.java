package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CargoElevatorBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.rtty.RttyInfo;
import com.reinhardt.hbm.rtty.RttyInteractive;
import com.reinhardt.hbm.rtty.RttyValueProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class CargoElevatorBlockEntity extends BlockEntity implements RttyValueProvider, RttyInteractive {
    public static final double SPEED = 2.0D / 20.0D;
    private static volatile Consumer<CargoElevatorBlockEntity> clientRenderSnapshotUpdater = ignored -> { };
    private static volatile Consumer<CargoElevatorBlockEntity> clientGpuCacheDisposer = ignored -> { };
    private static volatile long clientRenderSnapshotRevision;
    private static final Set<CargoElevatorBlockEntity> LOADED_CLIENT_INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());

    private BlockPos corePos;
    private int height;
    private double extension;
    private double previousExtension;
    private int targetExtension;
    private boolean renderPlatform;
    private volatile RenderSnapshot renderSnapshot;
    private boolean clientRenderSnapshotDirty = true;
    private boolean loadedOnClient;

    public CargoElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CARGO_ELEVATOR.get(), pos, state);
        this.corePos = pos.immutable();
        this.renderSnapshot = RenderSnapshot.empty(pos);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CargoElevatorBlockEntity elevator) {
        if (!elevator.isCore()) {
            return;
        }
        elevator.previousExtension = elevator.extension;
        if (!level.isClientSide) {
            if (elevator.extension < elevator.targetExtension) {
                elevator.extension += SPEED;
                elevator.extension = Mth.clamp(elevator.extension, 0.0D, elevator.targetExtension);
            } else if (elevator.extension > elevator.targetExtension) {
                elevator.extension -= SPEED;
                elevator.extension = Mth.clamp(elevator.extension, elevator.targetExtension, elevator.height);
            }
            elevator.extension = Math.max(0.0D, Math.min(elevator.height, elevator.extension));
            boolean renderPlatformChanged = !elevator.renderPlatform;
            elevator.renderPlatform = true;
            if (elevator.extension != elevator.previousExtension || renderPlatformChanged) {
                elevator.syncRenderState();
            }
        }
        if (elevator.extension != elevator.previousExtension) {
            double lower = elevator.getBlockPos().getY() + 1.0D + Math.min(elevator.extension, elevator.previousExtension);
            double upper = elevator.getBlockPos().getY() + 1.0D + Math.max(elevator.extension, elevator.previousExtension);
            AABB box = new AABB(elevator.getBlockPos().getX() - 0.99D, lower, elevator.getBlockPos().getZ() - 0.99D,
                    elevator.getBlockPos().getX() + 1.99D, upper, elevator.getBlockPos().getZ() + 1.99D);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
                if (entity instanceof Player && !level.isClientSide) {
                    continue;
                }
                if (entity.getY() >= lower && entity.getY() <= upper) {
                    double delta = entity.getY() - (elevator.getBlockPos().getY() + 1.0D + elevator.extension);
                    entity.setPos(entity.getX(), entity.getY() - delta - 0.125D, entity.getZ());
                    entity.setOnGround(true);
                }
            }
        }
        if (level.isClientSide) {
            if (elevator.renderSnapshot.revision() != clientRenderSnapshotRevision) {
                elevator.clientRenderSnapshotDirty = true;
            }
            elevator.refreshClientRenderSnapshot();
        }
        elevator.setChanged();
    }

    public boolean isCore() { return getBlockState().getValue(CargoElevatorBlock.CORE) && getBlockPos().equals(corePos); }
    public int height() { return height; }
    public double extension(float partialTick) { return previousExtension + (extension - previousExtension) * partialTick; }
    public boolean renderPlatform() { return renderPlatform; }
    public RenderSnapshot renderSnapshot() { return renderSnapshot; }
    public RenderInputs captureRenderInputs() {
        return new RenderInputs(isCore(), height, renderPlatform, previousExtension, extension);
    }
    public Optional<CargoElevatorBlockEntity> core() {
        if (isCore()) return Optional.of(this);
        return level != null && level.getBlockEntity(corePos) instanceof CargoElevatorBlockEntity core && core.isCore()
                ? Optional.of(core) : Optional.empty();
    }

    public void setCorePos(BlockPos corePos) {
        this.corePos = corePos.immutable();
        syncRenderState();
    }

    public void toggleElevator() {
        this.targetExtension = this.targetExtension == 0 ? this.height : 0;
        syncRenderState();
    }

    @Override
    public String[] rttyFunctionInfo() {
        return new String[] {
                RttyInfo.PREFIX_VALUE + "extension",
                RttyInfo.PREFIX_FUNCTION + "setextension" + RttyInteractive.NAME_SEPARATOR + "height"
        };
    }

    @Override
    public String provideRttyValue(String name) {
        if (name.equals(RttyInfo.PREFIX_VALUE + "extension")) {
            return Integer.toString((int) Math.round(this.extension));
        }
        return null;
    }

    @Override
    public String runRttyFunction(String name, String[] params) {
        if (name.equals(RttyInfo.PREFIX_FUNCTION + "setextension") && params.length > 0) {
            this.targetExtension = RttyInteractive.parseInt(params[0], 0, this.height);
            syncRenderState();
        }
        return null;
    }

    public boolean addLayer(ItemStack stack, Player player) {
        if (level == null || !isCore()) return false;
        int y = getBlockPos().getY() + height + 1;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (!level.getBlockState(getBlockPos().offset(x, height + 1, z)).canBeReplaced()) return false;
        }
        CargoElevatorBlock.setBuilding(true);
        try {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos target = getBlockPos().offset(x, height + 1, z);
                level.setBlock(target, com.reinhardt.hbm.registry.HbmBlocks.CARGO_ELEVATOR.get().defaultBlockState().setValue(CargoElevatorBlock.CORE, false), Block.UPDATE_ALL);
                if (level.getBlockEntity(target) instanceof CargoElevatorBlockEntity segment) segment.setCorePos(getBlockPos());
            }
        } finally {
            CargoElevatorBlock.setBuilding(false);
        }
        height++;
        if (!player.getAbilities().instabuild) stack.shrink(1);
        syncRenderState();
        return true;
    }

    public void removeStructure() {
        if (level == null || !isCore()) return;
        BlockPos origin = getBlockPos();
        CargoElevatorBlock.setBuilding(true);
        try {
            for (int y = 0; y <= height; y++) for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos target = getBlockPos().offset(x, y, z);
                if (level.getBlockState(target).is(com.reinhardt.hbm.registry.HbmBlocks.CARGO_ELEVATOR.get())
                        && (target.equals(origin)
                        || level.getBlockEntity(target) instanceof CargoElevatorBlockEntity segment
                        && segment.corePos.equals(origin))) {
                    level.removeBlock(target, false);
                }
            }
        } finally {
            CargoElevatorBlock.setBuilding(false);
        }
    }

    public static VoxelShape shapeFor(BlockGetter level, BlockPos queried) {
        if (!(level.getBlockEntity(queried) instanceof CargoElevatorBlockEntity segment)) {
            return Shapes.empty();
        }
        Optional<CargoElevatorBlockEntity> optional = segment.core();
        if (optional.isEmpty()) return Shapes.empty();
        CargoElevatorBlockEntity core = optional.get();
        BlockPos origin = core.getBlockPos();
        int h = core.height + 1;
        VoxelShape shape = Shapes.empty();
        shape = Shapes.or(shape, box(origin.getX() - 1, origin.getY(), origin.getZ() - 1,
                origin.getX() - 0.75D, origin.getY() + h, origin.getZ() - 0.75D, queried));
        shape = Shapes.or(shape, box(origin.getX() - 1, origin.getY(), origin.getZ() + 1.75D,
                origin.getX() - 0.75D, origin.getY() + h, origin.getZ() + 2, queried));
        shape = Shapes.or(shape, box(origin.getX() + 1.75D, origin.getY(), origin.getZ() - 1,
                origin.getX() + 2, origin.getY() + h, origin.getZ() - 0.75D, queried));
        shape = Shapes.or(shape, box(origin.getX() + 1.75D, origin.getY(), origin.getZ() + 1.75D,
                origin.getX() + 2, origin.getY() + h, origin.getZ() + 2, queried));
        shape = Shapes.or(shape, box(origin.getX() - 1, origin.getY() + 0.75D + core.extension, origin.getZ() - 1,
                origin.getX() + 2, origin.getY() + 1 + core.extension, origin.getZ() + 2, queried));
        return shape;
    }

    private static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, BlockPos relativeTo) {
        return Shapes.box(minX - relativeTo.getX(), minY - relativeTo.getY(), minZ - relativeTo.getZ(),
                maxX - relativeTo.getX(), maxY - relativeTo.getY(), maxZ - relativeTo.getZ());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CoreX", corePos.getX()); tag.putInt("CoreY", corePos.getY()); tag.putInt("CoreZ", corePos.getZ());
        tag.putInt("Height", height); tag.putDouble("Extension", extension); tag.putInt("TargetExtension", targetExtension);
        tag.putBoolean("RenderPlatform", renderPlatform);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        corePos = new BlockPos(tag.getInt("CoreX"), tag.getInt("CoreY"), tag.getInt("CoreZ"));
        height = Math.max(0, tag.getInt("Height"));
        double loadedExtension = tag.getDouble("Extension");
        extension = Double.isFinite(loadedExtension) ? Mth.clamp(loadedExtension, 0.0D, height) : 0.0D;
        previousExtension = extension;
        if (tag.contains("TargetExtension")) {
            targetExtension = Mth.clamp(tag.getInt("TargetExtension"), 0, height);
        } else {
            targetExtension = tag.getBoolean("Extending") ? height : 0;
        }
        renderPlatform = tag.getBoolean("RenderPlatform");
        clientRenderSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncRenderState() {
        setChanged();
        if (level != null && level.isClientSide) {
            clientRenderSnapshotDirty = true;
            refreshClientRenderSnapshot();
        } else if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Installed by the client renderer without linking client-only classes on a server. */
    public static void setClientRenderSnapshotUpdater(Consumer<CargoElevatorBlockEntity> updater) {
        clientRenderSnapshotUpdater = Objects.requireNonNull(updater);
        markLoadedClientSnapshotsDirty(false);
    }

    /** Installed by the client renderer so removed instances release only their own GPU buffers. */
    public static void setClientGpuCacheDisposer(Consumer<CargoElevatorBlockEntity> disposer) {
        clientGpuCacheDisposer = Objects.requireNonNull(disposer);
    }

    /** Invalidates every loaded snapshot after a new post-bake geometry generation is published. */
    public static void invalidateClientRenderSnapshotsAfterBake() {
        clientRenderSnapshotRevision++;
        markLoadedClientSnapshotsDirty(true);
    }

    public static long clientRenderSnapshotRevision() {
        return clientRenderSnapshotRevision;
    }

    private static void markLoadedClientSnapshotsDirty(boolean refreshNow) {
        ArrayList<CargoElevatorBlockEntity> loaded;
        synchronized (LOADED_CLIENT_INSTANCES) {
            loaded = new ArrayList<>(LOADED_CLIENT_INSTANCES);
        }
        for (CargoElevatorBlockEntity elevator : loaded) {
            elevator.clientRenderSnapshotDirty = true;
            if (refreshNow) {
                elevator.refreshClientRenderSnapshot();
            }
        }
    }

    public void refreshClientRenderSnapshot() {
        if (clientRenderSnapshotDirty && level != null && level.isClientSide) {
            clientRenderSnapshotUpdater.accept(this);
        }
    }

    public void publishClientRenderSnapshot(RenderSnapshot snapshot) {
        if (level == null || !level.isClientSide) {
            return;
        }
        RenderSnapshot previous = this.renderSnapshot;
        this.renderSnapshot = Objects.requireNonNull(snapshot);
        this.clientRenderSnapshotDirty = false;
        if (!sameSectionCoverage(previous.bounds(), snapshot.bounds())) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            loadedOnClient = true;
            synchronized (LOADED_CLIENT_INSTANCES) {
                LOADED_CLIENT_INSTANCES.add(this);
            }
            clientRenderSnapshotDirty = true;
            refreshClientRenderSnapshot();
        }
    }

    @Override
    public void setRemoved() {
        if (loadedOnClient) {
            loadedOnClient = false;
            synchronized (LOADED_CLIENT_INSTANCES) {
                LOADED_CLIENT_INSTANCES.remove(this);
            }
            clientGpuCacheDisposer.accept(this);
        }
        super.setRemoved();
    }

    private static boolean sameSectionCoverage(AABB first, AABB second) {
        return section(first.minX) == section(second.minX)
                && section(inclusiveMaximum(first.minX, first.maxX)) == section(inclusiveMaximum(second.minX, second.maxX))
                && section(first.minY) == section(second.minY)
                && section(inclusiveMaximum(first.minY, first.maxY)) == section(inclusiveMaximum(second.minY, second.maxY))
                && section(first.minZ) == section(second.minZ)
                && section(inclusiveMaximum(first.minZ, first.maxZ)) == section(inclusiveMaximum(second.minZ, second.maxZ));
    }

    private static int section(double coordinate) {
        return SectionPos.blockToSectionCoord(Mth.floor(coordinate));
    }

    private static double inclusiveMaximum(double min, double max) {
        return max > min ? Math.nextDown(max) : max;
    }

    private static AABB emptyBounds(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX(), pos.getY(), pos.getZ());
    }

    public record RenderInputs(boolean core, int height, boolean renderPlatform,
                               double previousExtension, double extension) {
    }

    public record RenderSnapshot(Object geometryToken, boolean core, int height, boolean renderPlatform,
                                 double previousExtension, double extension, AABB bounds, long revision) {
        public RenderSnapshot {
            Objects.requireNonNull(bounds);
        }

        private static RenderSnapshot empty(BlockPos pos) {
            return new RenderSnapshot(null, false, 0, false,
                    0.0D, 0.0D, emptyBounds(pos), -1L);
        }

        public double extension(float partialTick) {
            return previousExtension + (extension - previousExtension) * partialTick;
        }
    }
}
