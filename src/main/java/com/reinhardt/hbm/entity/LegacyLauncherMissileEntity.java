package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.api.entity.LegacyRadarDetectable;
import com.reinhardt.hbm.item.CustomMissileData;
import com.reinhardt.hbm.item.CustomMissileItem;
import com.reinhardt.hbm.item.LegacyMissileItem;
import com.reinhardt.hbm.item.MissilePartItem;
import com.reinhardt.hbm.registry.HbmChunkTickets;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** EntityMissileBaseNT/Custom/AntiBallistic: stored course is multiplied by velocity when moving. */
public final class LegacyLauncherMissileEntity extends Entity implements LegacyRadarDetectable {
    private static final EntityDataAccessor<ItemStack> MISSILE = SynchedEntityData.defineId(LegacyLauncherMissileEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Direction> FACING = SynchedEntityData.defineId(LegacyLauncherMissileEntity.class, EntityDataSerializers.DIRECTION);
    private int startX, startZ, targetX, targetZ, activationTimer;
    private double velocity, accelXZ, decelY;
    private float health = 50, fuel, consumption;
    private UUID tracking;
    private final Set<Long> forcedChunks = new HashSet<>();
    private double syncX, syncY, syncZ;
    private float syncYaw, syncPitch;
    private int interpolationTicks;

    public LegacyLauncherMissileEntity(EntityType<? extends LegacyLauncherMissileEntity> type, Level level) {
        super(type, level); noCulling = true;
    }
    public static LegacyLauncherMissileEntity create(Level level, ItemStack stack, double x, double y, double z,
                                                     int targetX, int targetZ, Direction facing) {
        if (!(stack.getItem() instanceof LegacyMissileItem) && !(stack.getItem() instanceof CustomMissileItem))
            throw new IllegalArgumentException("Not a legacy launchable missile: " + stack);
        LegacyLauncherMissileEntity missile = new LegacyLauncherMissileEntity(HbmEntityTypes.LEGACY_LAUNCHER_MISSILE.get(), level);
        missile.entityData.set(MISSILE, stack.copyWithCount(1));
        missile.entityData.set(FACING, facing);
        missile.setPos((float)x, (float)y, (float)z);
        missile.startX = (int)(float)x; missile.startZ = (int)(float)z;
        missile.targetX = targetX; missile.targetZ = targetZ;
        missile.setDeltaMovement(0, missile.isAntiBallistic() ? 1.5 : 2, 0);
        double distance = new Vec3(targetX - missile.startX, 0, targetZ - missile.startZ).length();
        missile.accelXZ = 1 / distance; missile.decelY = 2 / distance;
        missile.setYRot((float)Math.toDegrees(Math.atan2(targetX - missile.getX(), targetZ - missile.getZ())));
        if (stack.getItem() instanceof CustomMissileItem) {
            CustomMissileData custom = CustomMissileData.read(stack);
            if (custom == null) throw new IllegalArgumentException("Invalid assembled missile: " + stack);
            missile.fuel = custom.fuselageDefinition().primary();
            missile.consumption = MissilePartItem.definition(custom.thruster()).primary();
        }
        missile.yRotO = missile.getYRot();
        return missile;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MISSILE, ItemStack.EMPTY); builder.define(FACING, Direction.EAST);
    }
    public ItemStack missileItem() { return entityData.get(MISSILE); }
    public Direction launchFacing() { return entityData.get(FACING); }
    public String missileId() { return BuiltInRegistries.ITEM.getKey(missileItem().getItem()).getPath(); }
    public boolean isAntiBallistic() { return missileId().equals("missile_anti_ballistic"); }
    public void setTrackingTarget(Entity entity) { if (isAntiBallistic()) tracking = entity.getUUID(); }
    public double velocity() { return velocity; }
    public boolean hasPropulsion() { return !(missileItem().getItem() instanceof CustomMissileItem) || fuel > 0; }

    @Override public void tick() {
        super.tick();
        xo = getX(); yo = getY(); zo = getZ(); yRotO = getYRot(); xRotO = getXRot();
        if (level().isClientSide) { interpolateClient(); return; }
        if (missileItem().isEmpty()) throw new IllegalStateException("Missile entity has no missile item");
        if (missileItem().getItem() instanceof CustomMissileItem && hasPropulsion()) fuel -= consumption;
        updateForcedChunks();
        Vec3 motion = getDeltaMovement();
        Vec3 end = position().add(motion.scale(velocity));
        BlockHitResult hit = level().clip(new ClipContext(position(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK && (!isAntiBallistic() || activationTimer >= 40)) {
            // EntityMissileAntiBallistic.onImpact used the fixed 20F blast when
            // it reached a block after arming.  Keep this separate from the
            // proximity detonation (15F) below; they are two different legacy
            // paths and must not be collapsed into one generic radius.
            if (isAntiBallistic()) LegacyLauncherImpact.antiBallistic(this, 20F);
            else LegacyLauncherImpact.impact(this, hit);
            discard(); return;
        }
        setPos(end);
        if (isAntiBallistic()) tickAntiBallistic();
        else {
            if (velocity < 4) velocity += Mth.clamp(tickCount / 60D * .05, 0, .05);
            if (hasPropulsion()) {
                motion = motion.add(0, -decelY * velocity, 0);
                Vec3 course = new Vec3(targetX - startX, 0, targetZ - startZ).normalize().scale(accelXZ * velocity);
                if (motion.y > 0) motion = motion.add(course);
                if (motion.y < 0) motion = motion.subtract(course);
            } else motion = new Vec3(motion.x * .99, motion.y > -1.5 ? motion.y - .05 : motion.y, motion.z * .99);
            setDeltaMovement(motion);
            if (motion.y < -1.5 && Set.of("missile_cluster", "missile_cluster_strong", "missile_rain").contains(missileId())) {
                LegacyLauncherImpact.impact(this, null); discard(); return;
            }
            setYRot((float)Math.toDegrees(Math.atan2(targetX - getX(), targetZ - getZ())));
        }
        motion = getDeltaMovement();
        if (isAntiBallistic()) setYRot((float)Math.toDegrees(Math.atan2(motion.x, motion.z)));
        setXRot((float)Math.toDegrees(Math.atan2(motion.y, motion.horizontalDistance())) - 90);
        while (getXRot() - xRotO < -180) xRotO -= 360;
        while (getXRot() - xRotO >= 180) xRotO += 360;
        while (getYRot() - yRotO < -180) yRotO -= 360;
        while (getYRot() - yRotO >= 180) yRotO += 360;
        hasImpulse = true;
    }
    private void tickAntiBallistic() {
        if (velocity < 6) velocity += .1;
        if (activationTimer < 40) {
            activationTimer++;
            setDeltaMovement(getDeltaMovement().x, 1.5, getDeltaMovement().z);
            return;
        }
        ServerLevel server = (ServerLevel)level();
        Entity target = tracking == null ? null : server.getEntity(tracking);
        if (target == null || !target.isAlive()) {
            target = null;
            // Old targetMissile deliberately kept the last eligible entity inside 1000, not the closest.
            for (Entity candidate : server.getAllEntities()) {
                if (candidate instanceof LegacyLauncherMissileEntity m && !m.isAntiBallistic()
                        && !m.missileId().equals("missile_stealth") && m != this && m.isAlive() && distanceTo(m) < 1000)
                    target = m;
            }
            tracking = target == null ? null : target.getUUID();
        }
        if (target != null && target.isAlive()) {
            Vec3 delta = target.position().subtract(position());
            double eta = delta.length() / (1.5 * velocity);
            Vec3 predicted = target.position().add(target.position().subtract(new Vec3(target.xo, target.yo, target.zo)).scale(eta));
            if (delta.length() < 10) { LegacyLauncherImpact.antiBallistic(this, 15); discard(); return; }
            setDeltaMovement(predicted.subtract(position()).normalize().scale(1.5));
        } else if (tickCount > 600 || getY() > 2000) discard();
    }
    private void interpolateClient() {
        if (interpolationTicks > 0) {
            setPos(getX() + (syncX-getX())/interpolationTicks, getY() + (syncY-getY())/interpolationTicks, getZ() + (syncZ-getZ())/interpolationTicks);
            setYRot(getYRot() + Mth.wrapDegrees(syncYaw-getYRot())/interpolationTicks);
            setXRot(getXRot() + (syncPitch-getXRot())/interpolationTicks--);
        }
        // Custom exhaust has a distinct fuel selection, exactly as EntityMissileCustom.
        var custom = CustomMissileData.read(missileItem());
        if (custom != null) {
            if (custom.fuselageDefinition().fuel() == MissilePartItem.Fuel.XENON) return;
            var particle = switch (custom.fuselageDefinition().fuel()) {
                case KEROSENE -> HbmParticleTypes.KEROSENE_ROCKET_FLAME.get();
                case HYDROGEN -> HbmParticleTypes.HYDROGEN_ROCKET_FLAME.get();
                case BALEFIRE -> HbmParticleTypes.BALEFIRE_ROCKET_FLAME.get();
                case SOLID -> HbmParticleTypes.SOLID_ROCKET_FLAME.get();
                case XENON -> throw new IllegalStateException();
            };
            Vec3 displacement = position().subtract(new Vec3(xo,yo,zo));
            Vec3 direction = displacement.normalize();
            for (int i=0; i<displacement.length(); i++) level().addParticle(particle, getX()-direction.x*i,getY()-direction.y*i,getZ()-direction.z*i,0,0,0);
        } else {
            // EntityMissileBaseNT.spawnContrail emits the legacy black contrail
            // along the displacement of the last network update (1..10 quads).
            Vec3 displacement = position().subtract(new Vec3(xo, yo, zo));
            double length = Math.min(Math.max(displacement.length(), 1.0D), 10.0D);
            Vec3 direction = displacement.lengthSqr() < 1.0E-8D ? new Vec3(0, 1, 0) : displacement.normalize();
            for (int i = 0; i < (int) length; i++)
                level().addParticle(HbmParticleTypes.KEROSENE_ROCKET_FLAME.get(),
                        getX() - direction.x * (i - length), getY() - direction.y * (i - length),
                        getZ() - direction.z * (i - length), 0, 0, 0);
        }
    }
    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        syncX=x; syncY=y; syncZ=z; syncYaw=yaw; syncPitch=pitch; interpolationTicks=steps;
    }
    @Override public boolean isPickable() { return true; }
    @Override public boolean canBeCollidedWith() { return isAlive(); }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (!level().isClientSide && health > 0 && (health -= amount) <= 0) {
            LegacyLauncherImpact.destroyInFlight(this); discard();
        }
        return true;
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return true; }
    private void updateForcedChunks() {
        if (!(level() instanceof ServerLevel server)) return;
        int x = Mth.floor(getX()) >> 4, z = Mth.floor(getZ()) >> 4, radius = isAntiBallistic() ? 1 : 0;
        Set<Long> desired = new HashSet<>();
        for (int dx=-radius; dx<=radius; dx++) for (int dz=-radius; dz<=radius; dz++)
            desired.add(net.minecraft.world.level.ChunkPos.asLong(x+dx,z+dz));
        for (long chunk : Set.copyOf(forcedChunks)) if (!desired.contains(chunk)) {
            ticket(server, chunk, false); forcedChunks.remove(chunk);
        }
        for (long chunk : desired) if (forcedChunks.add(chunk)) ticket(server, chunk, true);
    }
    private void ticket(ServerLevel level, long chunk, boolean force) {
        HbmChunkTickets.LAUNCHER_MISSILES.forceChunk(level, this, net.minecraft.world.level.ChunkPos.getX(chunk), net.minecraft.world.level.ChunkPos.getZ(chunk), force, true);
    }
    @Override public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel server) for (long chunk : forcedChunks) ticket(server, chunk, false);
        forcedChunks.clear(); super.remove(reason);
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Missile", missileItem().saveOptional(registryAccess()));
        tag.putInt("Facing", launchFacing().get3DDataValue());
        tag.putInt("sX",startX); tag.putInt("sZ",startZ); tag.putInt("tX",targetX); tag.putInt("tZ",targetZ);
        tag.putDouble("veloc",velocity); tag.putDouble("accel",accelXZ); tag.putDouble("decel",decelY);
        tag.putFloat("fuel",fuel); tag.putFloat("consumption",consumption);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(MISSILE, ItemStack.parseOptional(registryAccess(),tag.getCompound("Missile")));
        entityData.set(FACING, Direction.from3DDataValue(tag.getInt("Facing")));
        startX=tag.getInt("sX"); startZ=tag.getInt("sZ"); targetX=tag.getInt("tX"); targetZ=tag.getInt("tZ");
        velocity=tag.getDouble("veloc"); accelXZ=tag.getDouble("accel"); decelY=tag.getDouble("decel");
        fuel=tag.getFloat("fuel"); consumption=tag.getFloat("consumption");
    }
    @Override public String translationKey() {
        if (isAntiBallistic()) return "radar.target.ab";
        if (missileId().equals("missile_doomsday") || missileId().equals("missile_doomsday_rusted")) return "radar.target.doomsday";
        if (missileId().equals("missile_shuttle")) return "radar.target.shuttle";
        if (missileItem().getItem() instanceof CustomMissileItem) return switch (blipLevel()) {
            case TIER10 -> "radar.target.custom10"; case TIER10_15 -> "radar.target.custom1015";
            case TIER15 -> "radar.target.custom15"; case TIER15_20 -> "radar.target.custom1520";
            case TIER20 -> "radar.target.custom20"; default -> "radar.target.custom";
        };
        return "radar.target.tier" + blipLevel();
    }
    @Override public int blipLevel() {
        if (isAntiBallistic()) return TIER_AB;
        if (missileId().equals("missile_decoy")) return TIER4;
        var custom = CustomMissileData.read(missileItem());
        if (custom != null) {
            var f=custom.fuselageDefinition();
            if (f.top()==MissilePartItem.Size.SIZE_10) return f.bottom()==MissilePartItem.Size.SIZE_15 ? TIER10_15 : TIER10;
            if (f.top()==MissilePartItem.Size.SIZE_15) return f.bottom()==MissilePartItem.Size.SIZE_20 ? TIER15_20 : TIER15;
            if (f.top()==MissilePartItem.Size.SIZE_20) return TIER20;
            return TIER1;
        }
        return missileItem().getItem() instanceof LegacyMissileItem m ? m.tier().ordinal() : SPECIAL;
    }
    @Override public boolean canBeSeenBy(Object radar) { return !missileId().equals("missile_stealth"); }
    @Override public boolean paramsApplicable(RadarScanParams params) { return params.scanMissiles(); }
    @Override public boolean suppliesRedstone(RadarScanParams params) { return !isAntiBallistic() && (!params.smartMode() || getDeltaMovement().y < 0); }
}
