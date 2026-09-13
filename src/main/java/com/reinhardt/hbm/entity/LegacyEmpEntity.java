package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.power.PowerEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/** Dedicated port of the 1.7.10 EntityEMP/EntityEMPBlast pair. */
public final class LegacyEmpEntity extends Entity {
    private static final EntityDataAccessor<Integer> MAX_AGE =
            SynchedEntityData.defineId(LegacyEmpEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PERSISTENT =
            SynchedEntityData.defineId(LegacyEmpEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(LegacyEmpEntity.class, EntityDataSerializers.FLOAT);
    private final List<BlockPos> machines = new ArrayList<>();
    private boolean allocated;

    public LegacyEmpEntity(EntityType<? extends LegacyEmpEntity> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    public static void spawnBlast(ServerLevel level, double x, double y, double z, int maxAge) {
        LegacyEmpEntity entity = new LegacyEmpEntity(com.reinhardt.hbm.registry.HbmEntityTypes.LEGACY_EMP.get(), level);
        entity.setPos(x, y, z);
        entity.entityData.set(MAX_AGE, maxAge);
        entity.entityData.set(PERSISTENT, false);
        entity.refreshDimensions();
        level.addFreshEntity(entity);
    }

    public static void spawnPersistent(ServerLevel level, double x, double y, double z) {
        LegacyEmpEntity entity = new LegacyEmpEntity(com.reinhardt.hbm.registry.HbmEntityTypes.LEGACY_EMP.get(), level);
        entity.setPos(x, y, z);
        entity.entityData.set(MAX_AGE, 12000);
        entity.entityData.set(PERSISTENT, true);
        entity.refreshDimensions();
        level.addFreshEntity(entity);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAX_AGE, 100).define(PERSISTENT, false).define(SCALE, 0.0F);
    }

    @Override public void tick() {
        super.tick();
        // EntityEMPBlast.scale was a public client-visible counter increased
        // every tick; the ring renderer uses this exact value as its X/Z size.
        entityData.set(SCALE, entityData.get(SCALE) + 1.0F);
        if (tickCount >= entityData.get(MAX_AGE)) { discard(); return; }
        if (level() instanceof ServerLevel server && entityData.get(PERSISTENT)) {
            // EntityEMP allocates on its first server update and only starts
            // draining machines on the following update.  An empty result is
            // still a completed allocation and must not trigger another scan.
            if (!allocated) {
                allocate(server);
                allocated = true;
            } else {
                shock(server);
            }
        }
    }

    private void allocate(ServerLevel level) {
        int radius = 100;
        BlockPos center = blockPosition();
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++) for (int z = -radius; z <= radius; z++) {
            if (x * x + y * y + z * z > radius * radius) continue;
            BlockPos pos = center.offset(x, y, z);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof PowerEndpoint) machines.add(pos.immutable());
        }
    }

    private void shock(ServerLevel level) {
        for (BlockPos pos : machines) {
            if (!(level.getBlockEntity(pos) instanceof PowerEndpoint endpoint)) continue;
            endpoint.applyPower(Long.MAX_VALUE, 0L);
            if (level.random.nextInt(20) == 0) {
                // EntityEMP only sent the old stained-glass destruction burst;
                // unlike ExplosionNukeGeneric.emp it never replaced the machine.
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                                Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()),
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        64, 0.5D, 0.5D, 0.5D, 0.05D);
            }
        }
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("maxAge", entityData.get(MAX_AGE));
        tag.putBoolean("persistent", entityData.get(PERSISTENT));
        tag.putFloat("scale", entityData.get(SCALE));
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(MAX_AGE, tag.getInt("maxAge"));
        entityData.set(PERSISTENT, tag.getBoolean("persistent"));
        entityData.set(SCALE, tag.getFloat("scale"));
    }
    public float scale() { return entityData.get(SCALE); }
    public boolean persistent() { return entityData.get(PERSISTENT); }
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PERSISTENT.equals(key)) {
            refreshDimensions();
        }
    }
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return persistent()
                ? EntityDimensions.scalable(0.6F, 1.8F)
                : EntityDimensions.scalable(1.5F, 1.5F);
    }
    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < (persistent() ? 4096.0D : 9216.0D);
    }
}
