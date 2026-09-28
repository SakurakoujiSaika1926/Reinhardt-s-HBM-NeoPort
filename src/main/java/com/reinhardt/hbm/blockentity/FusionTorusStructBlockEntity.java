package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FusionMachineBlock;
import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public class FusionTorusStructBlockEntity extends BlockEntity {
    private static volatile Consumer<FusionTorusStructBlockEntity> clientRenderSnapshotUpdater = ignored -> { };
    private static volatile long clientRenderSnapshotRevision;
    private static final Set<FusionTorusStructBlockEntity> LOADED_CLIENT_INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());

    private int age;
    private volatile RenderSnapshot renderSnapshot;
    private boolean clientRenderSnapshotDirty = true;

    public FusionTorusStructBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.FUSION_TORUS_STRUCT.get(), pos, blockState);
        this.renderSnapshot = new RenderSnapshot(null, emptyBounds(pos), -1L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionTorusStructBlockEntity entity) {
        if (level.isClientSide) {
            if (entity.renderSnapshot.revision() != clientRenderSnapshotRevision) {
                entity.clientRenderSnapshotDirty = true;
            }
            entity.refreshClientRenderSnapshot();
            return;
        }
        entity.age++;
        if (entity.age < 20) {
            return;
        }
        entity.age = 0;
        if (matchesTorus(level, pos)) {
            buildTorus(level, pos);
        }
    }

    public static void setClientRenderSnapshotUpdater(Consumer<FusionTorusStructBlockEntity> updater) {
        clientRenderSnapshotUpdater = Objects.requireNonNull(updater);
        markLoadedClientSnapshotsDirty(false);
    }

    public static void invalidateClientRenderSnapshotsAfterBake() {
        clientRenderSnapshotRevision++;
        markLoadedClientSnapshotsDirty(true);
    }

    public static long clientRenderSnapshotRevision() {
        return clientRenderSnapshotRevision;
    }

    private static void markLoadedClientSnapshotsDirty(boolean refreshNow) {
        ArrayList<FusionTorusStructBlockEntity> loaded;
        synchronized (LOADED_CLIENT_INSTANCES) {
            loaded = new ArrayList<>(LOADED_CLIENT_INSTANCES);
        }
        for (FusionTorusStructBlockEntity struct : loaded) {
            struct.clientRenderSnapshotDirty = true;
            if (refreshNow) {
                struct.refreshClientRenderSnapshot();
            }
        }
    }

    public RenderSnapshot renderSnapshot() {
        return this.renderSnapshot;
    }

    public void refreshClientRenderSnapshot() {
        if (this.clientRenderSnapshotDirty && this.level != null && this.level.isClientSide) {
            clientRenderSnapshotUpdater.accept(this);
        }
    }

    public void publishClientRenderSnapshot(RenderSnapshot snapshot) {
        if (this.level == null || !this.level.isClientSide) {
            return;
        }
        RenderSnapshot previous = this.renderSnapshot;
        this.renderSnapshot = Objects.requireNonNull(snapshot);
        this.clientRenderSnapshotDirty = false;
        if (!sameSectionCoverage(previous.bounds(), snapshot.bounds())) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide) {
            synchronized (LOADED_CLIENT_INSTANCES) {
                LOADED_CLIENT_INSTANCES.add(this);
            }
            this.clientRenderSnapshotDirty = true;
            refreshClientRenderSnapshot();
        }
    }

    @Override
    public void setRemoved() {
        synchronized (LOADED_CLIENT_INSTANCES) {
            LOADED_CLIENT_INSTANCES.remove(this);
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

    public record RenderSnapshot(Object geometryToken, AABB bounds, long revision) {
        public RenderSnapshot {
            Objects.requireNonNull(bounds);
        }
    }

    private static boolean matchesTorus(Level level, BlockPos corePos) {
        for (int y = 0; y < 5; y++) {
            int layerIndex = y > 2 ? 4 - y : y;
            int[][] layer = FusionMachineBlock.TORUS_LAYOUT[layerIndex];
            for (int x = 0; x < layer.length; x++) {
                for (int z = 0; z < layer[x].length; z++) {
                    int required = layer[x][z];
                    if (required == 0 || (x == 7 && y == 0 && z == 7)) {
                        continue;
                    }
                    if (!matchesComponent(level, corePos.offset(x - 7, y, z - 7), required)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean matchesComponent(Level level, BlockPos pos, int variant) {
        BlockState state = level.getBlockState(pos);
        return state.is(HbmBlocks.FUSION_COMPONENT.get())
                && state.hasProperty(LegacyVariantBlock.VARIANT)
                && state.getValue(LegacyVariantBlock.VARIANT) == variant;
    }

    private static void buildTorus(Level level, BlockPos corePos) {
        Direction facing = Direction.SOUTH;
        BlockState torusState = HbmBlocks.FUSION_TORUS.get()
                .defaultBlockState()
                .setValue(LargeMachineBlock.FACING, facing);
        level.setBlock(corePos, torusState, Block.UPDATE_ALL);
        LargeMachineBlock.placeDummies(
                level,
                corePos,
                facing,
                FusionMachineBlock.Kind.TORUS.footprint(),
                FusionMachineBlock::mapTorusStructureOffset
        );
        FusionMachineBlock.markTorusPortDummies(level, corePos, facing);
    }
}
