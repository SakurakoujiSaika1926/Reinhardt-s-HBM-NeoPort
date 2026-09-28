package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.DemonLampBlock;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
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

/** Exact 1.7.10 DemonLamp radiation falloff and resistance ray sampling. */
public final class DemonLampBlockEntity extends BlockEntity {
    private static final Set<DemonLampBlockEntity> CLIENT_INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final float RADIATION_PER_TICK = 100_000.0F;
    private static final double RANGE = 25.0D;
    private static volatile Consumer<DemonLampBlockEntity> clientRenderSnapshotUpdater = ignored -> { };
    private static volatile long clientRenderSnapshotRevision;
    private volatile RenderSnapshot renderSnapshot;
    private boolean clientRenderSnapshotDirty = true;

    public DemonLampBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DEMON_LAMP.get(), pos, state);
        // A null token makes the renderer emit nothing until the first exact
        // post-bake model/cone generation is available.
        this.renderSnapshot = new RenderSnapshot(state, facing(state), null, emptyBounds(pos), -1L);
    }

    public RenderSnapshot renderSnapshot() {
        return this.renderSnapshot;
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        this.clientRenderSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    private static Direction facing(BlockState state) {
        return state.hasProperty(DemonLampBlock.FACING)
                ? state.getValue(DemonLampBlock.FACING)
                : Direction.UP;
    }

    /** Installed by the client renderer without linking client classes on a server. */
    public static void setClientRenderSnapshotUpdater(Consumer<DemonLampBlockEntity> updater) {
        clientRenderSnapshotUpdater = Objects.requireNonNull(updater);
        markLoadedClientSnapshotsDirty(false);
    }

    /** Called after an immutable captured-geometry generation is published. */
    public static void invalidateClientRenderSnapshotsAfterBake() {
        clientRenderSnapshotRevision++;
        markLoadedClientSnapshotsDirty(true);
    }

    public static long clientRenderSnapshotRevision() {
        return clientRenderSnapshotRevision;
    }

    private static void markLoadedClientSnapshotsDirty(boolean refreshNow) {
        ArrayList<DemonLampBlockEntity> loaded;
        synchronized (CLIENT_INSTANCES) {
            loaded = new ArrayList<>(CLIENT_INSTANCES);
        }
        for (DemonLampBlockEntity lamp : loaded) {
            lamp.clientRenderSnapshotDirty = true;
            if (refreshNow) {
                lamp.refreshClientRenderSnapshot();
            }
        }
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
            synchronized (CLIENT_INSTANCES) {
                CLIENT_INSTANCES.add(this);
            }
            this.clientRenderSnapshotDirty = true;
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

    public static void tick(Level level, BlockPos pos, BlockState state, DemonLampBlockEntity lamp) {
        if (level.isClientSide) {
            if (lamp.renderSnapshot.revision() != clientRenderSnapshotRevision) {
                lamp.clientRenderSnapshotDirty = true;
            }
            lamp.refreshClientRenderSnapshot();
            return;
        }
        lamp.radiate(level);
    }

    private void radiate(Level level) {
        Vec3 origin = Vec3.atCenterOf(worldPosition);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin).inflate(RANGE))) {
            Vec3 target = new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
            Vec3 delta = target.subtract(origin);
            double distance = delta.length();
            if (distance <= 0.0D || distance > RANGE) {
                continue;
            }

            Vec3 unit = delta.scale(1.0D / distance);
            float resistance = 0.0F;
            for (int step = 1; step < distance; step++) {
                BlockPos sample = BlockPos.containing(origin.add(unit.scale(step)));
                resistance += level.getBlockState(sample).getBlock().getExplosionResistance();
            }
            resistance = Math.max(1.0F, resistance);

            HbmLivingRadiation radiation = HbmLivingRadiation.get(entity);
            radiation.addRadiationWithReadout((float) (RADIATION_PER_TICK / resistance / (distance * distance)));
            HbmLivingRadiation.set(entity, radiation);
            if (distance < 2.0D) {
                entity.hurt(entity.damageSources().inFire(), 100.0F);
            }
        }
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

    public record RenderSnapshot(BlockState blockState, Direction facing, Object geometryToken,
                                 AABB bounds, long revision) {
        public RenderSnapshot {
            Objects.requireNonNull(blockState);
            Objects.requireNonNull(facing);
            Objects.requireNonNull(bounds);
        }
    }
}
