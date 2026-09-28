package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
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

public class WatzStructBlockEntity extends BlockEntity {
    private static volatile Consumer<WatzStructBlockEntity> clientRenderSnapshotUpdater = ignored -> { };
    private static volatile long clientRenderSnapshotRevision;
    private static final Set<WatzStructBlockEntity> LOADED_CLIENT_INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());

    private volatile RenderSnapshot renderSnapshot;
    private boolean clientRenderSnapshotDirty = true;

    public WatzStructBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.WATZ_STRUCT.get(), pos, state);
        // No geometry token means the renderer emits nothing; publish the
        // matching zero-volume envelope until post-bake geometry is ready.
        this.renderSnapshot = new RenderSnapshot(null, emptyBounds(pos), -1L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WatzStructBlockEntity struct) {
        if (level.isClientSide) {
            if (struct.renderSnapshot.revision() != clientRenderSnapshotRevision) {
                struct.clientRenderSnapshotDirty = true;
            }
            struct.refreshClientRenderSnapshot();
            return;
        }
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        if (!struct.isValidStructure(level, pos)) {
            return;
        }
        level.setBlock(pos, HbmBlocks.WATZ.get().defaultBlockState(), Block.UPDATE_ALL);
        WatzBlockEntity.fillDummies(level, pos);
    }

    /** Installed by the client renderer without linking client classes on a server. */
    public static void setClientRenderSnapshotUpdater(Consumer<WatzStructBlockEntity> updater) {
        clientRenderSnapshotUpdater = Objects.requireNonNull(updater);
        markLoadedClientSnapshotsDirty(false);
    }

    /** Publishes a new post-bake geometry generation to every loaded preview. */
    public static void invalidateClientRenderSnapshotsAfterBake() {
        clientRenderSnapshotRevision++;
        markLoadedClientSnapshotsDirty(true);
    }

    public static long clientRenderSnapshotRevision() {
        return clientRenderSnapshotRevision;
    }

    private static void markLoadedClientSnapshotsDirty(boolean refreshNow) {
        ArrayList<WatzStructBlockEntity> loaded;
        synchronized (LOADED_CLIENT_INSTANCES) {
            loaded = new ArrayList<>(LOADED_CLIENT_INSTANCES);
        }
        for (WatzStructBlockEntity struct : loaded) {
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

    private boolean isValidStructure(Level level, BlockPos core) {
        if (!has(level, core, HbmBlocks.WATZ_COOLER.get(), 0, 1, 0)) {
            return false;
        }
        if (!has(level, core, HbmBlocks.WATZ_COOLER.get(), 0, 2, 0)) {
            return false;
        }
        for (int y = 0; y < 3; y++) {
            for (int[] offset : WatzBlockEntity.ELEMENT_OFFSETS) {
                if (!has(level, core, HbmBlocks.WATZ_ELEMENT.get(), offset[0], y, offset[1])) {
                    return false;
                }
            }
            for (int[] offset : WatzBlockEntity.COOLER_OFFSETS) {
                if (!has(level, core, HbmBlocks.WATZ_COOLER.get(), offset[0], y, offset[1])) {
                    return false;
                }
            }
            for (int[] offset : WatzBlockEntity.CASING_OFFSETS) {
                if (!hasVariant(level, core, HbmBlocks.WATZ_END.get(), 1, offset[0], y, offset[1])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean has(Level level, BlockPos core, Block block, int x, int y, int z) {
        return level.getBlockState(core.offset(x, y, z)).is(block);
    }

    private static boolean hasVariant(Level level, BlockPos core, Block block, int variant, int x, int y, int z) {
        BlockState state = level.getBlockState(core.offset(x, y, z));
        return state.is(block)
                && state.hasProperty(LegacyVariantBlock.VARIANT)
                && state.getValue(LegacyVariantBlock.VARIANT) == variant;
    }

}
