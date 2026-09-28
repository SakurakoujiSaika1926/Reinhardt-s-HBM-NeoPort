package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CrashedBombBlock;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/** Exact two-tick linear falloff from TileEntityCrashedBomb. */
public final class CrashedBombBlockEntity extends BlockEntity {
    private static final Set<CrashedBombBlockEntity> CLIENT_INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static volatile Consumer<CrashedBombBlockEntity> clientRenderSnapshotUpdater = ignored -> { };
    private static volatile long clientRenderSnapshotRevision;

    private volatile RenderSnapshot renderSnapshot;
    private boolean clientRenderSnapshotDirty = true;

    public CrashedBombBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CRASHED_BOMB.get(), pos, state);
        this.renderSnapshot = new RenderSnapshot(
                CrashedBombBlock.type(state), null, fallbackBounds(pos), -1L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrashedBombBlockEntity bomb) {
        if (level.isClientSide) {
            CrashedBombBlock.Type type = CrashedBombBlock.type(state);
            RenderSnapshot snapshot = bomb.renderSnapshot;
            if (snapshot.type() != type || snapshot.revision() != clientRenderSnapshotRevision) {
                bomb.clientRenderSnapshotDirty = true;
            }
            bomb.refreshClientRenderSnapshot();
            return;
        }
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.getGameTime() % 2L != 0L) {
            return;
        }
        CrashedBombBlock.Type type = CrashedBombBlock.type(state);
        double range = switch (type) {
            case BALEFIRE -> 15.0D;
            case NUKE, SALTED -> 10.0D;
            default -> 0.0D;
        };
        float multiplier = switch (type) {
            case BALEFIRE -> 1.0F;
            case NUKE -> 0.25F;
            case SALTED -> 0.5F;
            default -> 0.0F;
        };
        if (range <= 0.0D) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(range))) {
            Vec3 target = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
            double distance = center.distanceTo(target);
            if (distance > range) {
                continue;
            }
            HbmLivingRadiation radiation = HbmLivingRadiation.get(entity);
            radiation.addRadiationWithReadout((float) ((1.0D - distance / range) * multiplier));
            HbmLivingRadiation.set(entity, radiation);
        }
    }

    /** Installed by the client renderer without introducing client classes on a dedicated server. */
    public static void setClientRenderSnapshotUpdater(Consumer<CrashedBombBlockEntity> updater) {
        clientRenderSnapshotUpdater = Objects.requireNonNull(updater);
        markLoadedClientSnapshotsDirty(false);
    }

    /** Publishes a new immutable post-bake generation to every loaded client instance. */
    public static void invalidateClientRenderSnapshotsAfterBake() {
        clientRenderSnapshotRevision++;
        markLoadedClientSnapshotsDirty(true);
    }

    /** Removes old baked-quad generations from block entities during client logout. */
    public static void clearClientRenderSnapshotsAfterLogout() {
        clientRenderSnapshotRevision++;
        synchronized (CLIENT_INSTANCES) {
            for (CrashedBombBlockEntity bomb : CLIENT_INSTANCES) {
                bomb.renderSnapshot = new RenderSnapshot(
                        CrashedBombBlock.type(bomb.getBlockState()), null,
                        fallbackBounds(bomb.worldPosition), -1L);
                bomb.clientRenderSnapshotDirty = true;
            }
            CLIENT_INSTANCES.clear();
        }
    }

    private static void markLoadedClientSnapshotsDirty(boolean refreshNow) {
        ArrayList<CrashedBombBlockEntity> loaded;
        synchronized (CLIENT_INSTANCES) {
            loaded = new ArrayList<>(CLIENT_INSTANCES);
        }
        for (CrashedBombBlockEntity bomb : loaded) {
            bomb.clientRenderSnapshotDirty = true;
            if (refreshNow) {
                bomb.refreshClientRenderSnapshot();
            }
        }
    }

    public static long clientRenderSnapshotRevision() {
        return clientRenderSnapshotRevision;
    }

    public RenderSnapshot renderSnapshot() {
        return renderSnapshot;
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
        RenderSnapshot previous = renderSnapshot;
        renderSnapshot = Objects.requireNonNull(snapshot);
        clientRenderSnapshotDirty = false;
        if (!sameSectionCoverage(previous.bounds(), snapshot.bounds())) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        clientRenderSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            synchronized (CLIENT_INSTANCES) {
                CLIENT_INSTANCES.add(this);
            }
            clientRenderSnapshotDirty = true;
            refreshClientRenderSnapshot();
        }
    }

    @Override
    public void setRemoved() {
        synchronized (CLIENT_INSTANCES) {
            CLIENT_INSTANCES.remove(this);
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

    private static AABB fallbackBounds(BlockPos pos) {
        /*
         * This is a proved startup envelope, not an estimated permanent box.
         * The largest source OBJ vertex radius is 4.506475 blocks (dud_nuke);
         * the legacy random axial shift contributes < 1 block and its nuke
         * offset contributes 1.25 blocks.  Inflating the owning block by 7
         * therefore contains every possible pre-snapshot pose.  The exact
         * transformed vertex AABB replaces it as soon as geometry is ready.
         */
        return new AABB(pos).inflate(7.0D);
    }

    public record RenderSnapshot(CrashedBombBlock.Type type, Object geometryToken,
                                 AABB bounds, long revision) {
        public RenderSnapshot {
            Objects.requireNonNull(type);
            Objects.requireNonNull(bounds);
        }
    }
}
