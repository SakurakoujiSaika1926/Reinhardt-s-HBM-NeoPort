package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.item.UniversalGrenadeItem;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.UUID;

/** Exact EntityUFOBase/EntityFBIDrone flight, scan and grenade behaviour. */
public final class LegacyFbiDroneEntity extends Monster {
    private int scanCooldown;
    private int courseChangeCooldown;
    private int attackCooldown;
    private int waypointX;
    private int waypointY;
    private int waypointZ;
    private UUID targetUuid;

    public LegacyFbiDroneEntity(EntityType<? extends LegacyFbiDroneEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 35.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // EntityUFOBase drove this entity directly rather than through
        // PathfinderMob goals; keeping the selector empty is intentional.
    }

    @Override
    public void tick() {
        // EntityUFOBase zeroed its motion before each scan/course update;
        // otherwise the previous waypoint velocity would be applied twice.
        setDeltaMovement(Vec3.ZERO);
        super.tick();
        setNoGravity(true);
        if (level().isClientSide) return;
        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            discard();
            return;
        }
        if (scanCooldown > 0) scanCooldown--;
        if (courseChangeCooldown > 0) courseChangeCooldown--;
        if (attackCooldown > 0) attackCooldown--;

        Player target = target();
        if (target == null || !target.isAlive() || target.isCreative() || target.isSpectator() || target.isInvisible()) {
            targetUuid = null;
            target = null;
        }
        if (scanCooldown <= 0) {
            target = scanTarget();
            scanCooldown = 100;
        }
        if (courseChangeCooldown <= 0) {
            setCourse(target);
        }
        if (courseChangeCooldown > 0) approachWaypoint(target == null ? 0.25D : 0.5D);

        if (target != null && attackCooldown <= 0) {
            Vec3 delta = position().subtract(target.position());
            if (Math.abs(delta.x) < 5.0D && Math.abs(delta.z) < 5.0D && delta.y > 3.0D) {
                attackCooldown = 60;
                ItemStack grenade = UniversalGrenadeItem.make(
                        UniversalGrenadeItem.Shell.FRAG,
                UniversalGrenadeItem.Filling.HE,
                        UniversalGrenadeItem.Fuze.S7,
                        null);
                Vec3 motion = Vec3.ZERO;
                UniversalGrenadeEntity projectile = new UniversalGrenadeEntity(level(), this, grenade, motion);
                // The legacy drone placed the grenade at its own base
                // position, not at the eye-height throw offset.
                projectile.setPos(getX(), getY(), getZ());
                level().addFreshEntity(projectile);
            }
        }
    }

    private Player scanTarget() {
        // EntityUFOBase cleared its target on every scan.  Clear the UUID as
        // well so an empty scan cannot resurrect a stale player next tick.
        targetUuid = null;
        return level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(100.0D, 50.0D, 100.0D),
                        player -> !player.isCreative() && !player.isSpectator() && !player.isInvisible())
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).map(player -> {
                    targetUuid = player.getUUID();
                    return player;
                }).orElse(null);
    }

    private Player target() {
        if (targetUuid == null || !(level() instanceof net.minecraft.server.level.ServerLevel server)) return null;
        Entity entity = server.getEntity(targetUuid);
        return entity instanceof Player player ? player : null;
    }

    private void setCourse(Player target) {
        if (target != null) {
            Vec3 fromTarget = position().subtract(target.position());
            if (fromTarget.lengthSqr() < 1.0E-6D) fromTarget = new Vec3(1.0D, 0.0D, 0.0D);
            double angle = random.nextFloat() * Math.PI * 2.0D;
            fromTarget = new Vec3(fromTarget.x * Math.cos(angle) - fromTarget.z * Math.sin(angle),
                    0.0D, fromTarget.x * Math.sin(angle) + fromTarget.z * Math.cos(angle));
            double length = Math.max(1.0D, fromTarget.length());
            double overshoot = 10.0D + random.nextDouble() * 10.0D;
            int x = Mth.floor(target.getX() - fromTarget.x / length * overshoot);
            int z = Mth.floor(target.getZ() - fromTarget.z / length * overshoot);
            waypointX = x;
            waypointZ = z;
            waypointY = Math.max(level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), Mth.floor(target.getY()))
                    + 7 + random.nextInt(4);
            courseChangeCooldown = 20 + random.nextInt(20);
        } else {
            waypointX = Mth.floor(getX() + random.nextGaussian() * 5.0D);
            waypointZ = Mth.floor(getZ() + random.nextGaussian() * 5.0D);
            waypointY = level().getHeight(Heightmap.Types.MOTION_BLOCKING, waypointX, waypointZ)
                    + 7 + random.nextInt(4);
            courseChangeCooldown = 60 + random.nextInt(20);
        }
    }

    private void approachWaypoint(double speed) {
        Vec3 delta = new Vec3(waypointX - getX(), waypointY - getY(), waypointZ - getZ());
        double length = delta.length();
        if (length <= 5.0D) return;
        Vec3 motion = delta.scale(speed / length);
        AABB box = getBoundingBox();
        Vec3 step = delta.scale(1.0D / length);
        for (int i = 1; i < length; i++) {
            box = box.move(step);
            if (!level().noCollision(this, box)) {
                courseChangeCooldown = 0;
                return;
            }
        }
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("waypointX", waypointX); tag.putInt("waypointY", waypointY); tag.putInt("waypointZ", waypointZ);
        if (targetUuid != null) tag.putUUID("target", targetUuid);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        waypointX = tag.getInt("waypointX"); waypointY = tag.getInt("waypointY"); waypointZ = tag.getInt("waypointZ");
        targetUuid = tag.hasUUID("target") ? tag.getUUID("target") : null;
    }
}
