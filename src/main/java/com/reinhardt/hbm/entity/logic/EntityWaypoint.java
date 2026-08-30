package com.reinhardt.hbm.entity.logic;

import com.reinhardt.hbm.entity.GlyphidEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;

import java.util.UUID;

/**
 * Server-side task marker used by the 1.7.10 Glyphid task system.
 *
 * The old waypoint was an invisible, non-colliding entity rather than a
 * block. Keeping that contract matters because several tasks communicate by
 * passing the same waypoint instance to nearby Glyphids.
 */
public final class EntityWaypoint extends Entity {
    private static final EntityDataAccessor<Integer> WAYPOINT_TYPE =
            SynchedEntityData.defineId(EntityWaypoint.class, EntityDataSerializers.INT);
    private int maxAge = 2400;
    private int radius = 3;
    private boolean highPriority;
    private boolean additionalSpawned;
    private EntityWaypoint additional;
    private UUID additionalUuid;

    public EntityWaypoint(EntityType<? extends EntityWaypoint> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    public int getWaypointType() {
        return entityData.get(WAYPOINT_TYPE);
    }

    public void setWaypointType(int waypointType) {
        entityData.set(WAYPOINT_TYPE, waypointType);
    }

    public int getColor() {
        return switch (getWaypointType()) {
            case GlyphidEntity.TASK_RETREAT_FOR_REINFORCEMENTS -> 0x5FA6E8;
            case GlyphidEntity.TASK_BUILD_HIVE, GlyphidEntity.TASK_INITIATE_RETREAT -> 0x127766;
            default -> 0x566573;
        };
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = Math.max(1, maxAge);
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = Math.max(0, radius);
    }

    public boolean isHighPriority() {
        return highPriority;
    }

    public void setHighPriority() {
        highPriority = true;
    }

    public void setAdditionalWaypoint(EntityWaypoint waypoint) {
        additional = waypoint;
        additionalUuid = waypoint == null ? null : waypoint.getUUID();
    }

    public EntityWaypoint getAdditionalWaypoint() {
        return additional;
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount >= maxAge) {
            discard();
            return;
        }

        AABB area = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ())
                .inflate(radius, radius, radius);

        if (level() instanceof ServerLevel serverLevel) {
            if (tickCount % 40 != 0) {
                return;
            }

            resolveAdditionalWaypoint(serverLevel);
            for (GlyphidEntity glyphid : serverLevel.getEntitiesOfClass(GlyphidEntity.class, area)) {
                if (additional != null && !additionalSpawned) {
                    serverLevel.addFreshEntity(additional);
                    additionalSpawned = true;
                }

                // This is the same exclusion used by EntityWaypoint in 1.7.10:
                // scouts and nuclear Glyphids own their special task transitions.
                boolean excluded = glyphid.getWaypoint() != this
                        || glyphid.getVariant() == GlyphidEntity.Variant.SCOUT
                        || glyphid.getVariant() == GlyphidEntity.Variant.NUCLEAR;
                if (!excluded) {
                    glyphid.setCurrentTask(getWaypointType(), additional);
                }

                if (getWaypointType() != GlyphidEntity.TASK_BUILD_HIVE
                        || glyphid.getVariant() == GlyphidEntity.Variant.SCOUT) {
                    discard();
                }
                break;
            }
            return;
        }

        if (HbmConfig.GLYPHID_WAYPOINT_DEBUG.get()) {
            double x = area.minX + (random.nextDouble() - 0.5D) * (area.maxX - area.minX);
            double y = area.minY + random.nextDouble() * (area.maxY - area.minY);
            double z = area.minZ + (random.nextDouble() - 0.5D) * (area.maxZ - area.minZ);
            int color = getColor();
            level().addParticle(
                    HbmParticleTypes.LEGACY_MIST.get(),
                    x, y, z,
                    ((color >>> 16) & 0xFF) / 255.0D,
                    ((color >>> 8) & 0xFF) / 255.0D,
                    (color & 0xFF) / 255.0D
            );
        }
    }

    private void resolveAdditionalWaypoint(ServerLevel level) {
        if (additional != null || additionalUuid == null) {
            return;
        }
        Entity entity = level.getEntity(additionalUuid);
        if (entity instanceof EntityWaypoint waypoint) {
            additional = waypoint;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WAYPOINT_TYPE, GlyphidEntity.TASK_IDLE);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setWaypointType(tag.getInt("type"));
        maxAge = Math.max(1, tag.getInt("maxAge"));
        radius = Math.max(0, tag.getInt("radius"));
        highPriority = tag.getBoolean("highPriority");
        additionalUuid = tag.hasUUID("additional") ? tag.getUUID("additional") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("type", getWaypointType());
        tag.putInt("maxAge", maxAge);
        tag.putInt("radius", radius);
        tag.putBoolean("highPriority", highPriority);
        if (additionalUuid != null) {
            tag.putUUID("additional", additionalUuid);
        }
    }
}
