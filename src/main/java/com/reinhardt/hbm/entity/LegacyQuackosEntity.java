package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

/** 1.7.10 EntityQuackos giant duck. */
public final class LegacyQuackosEntity extends Chicken {
    public LegacyQuackosEntity(EntityType<? extends LegacyQuackosEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        // EntityQuackos inherits EntityChicken's four-heart health; the old
        // class changed only its dimensions and boss presentation.
        return Chicken.createAttributes().add(Attributes.MAX_HEALTH, 4.0D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return HbmSoundEvents.MEGAQUACC.get();
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return HbmSoundEvents.MEGAQUACC.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HbmSoundEvents.MEGAQUACC.get();
    }

    @Override
    public Chicken getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        // EntityQuackos#createChild returned another EntityQuackos rather than
        // falling back to the ordinary duck/chicken offspring.
        return new LegacyQuackosEntity(com.reinhardt.hbm.registry.HbmEntityTypes.QUACKOS.get(), level);
    }

    @Override
    public void setHealth(float health) {
        // EntityQuackos#setHealth ignored the requested value and restored
        // the maximum health, including when external effects attempted to
        // modify it.
        super.setHealth(getMaxHealth());
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        // The legacy EntityQuackos implementation returned true from
        // isEntityInvulnerable: server-side damage must never enter the
        // normal hurt/death path.  The client-only setDead prank below is
        // separate from this immunity.
        return true;
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        if (level().isClientSide) super.die(source);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult result = super.mobInteract(player, hand);
        if (result.consumesAction()) return result;
        if (!level().isClientSide && (getPassengers().isEmpty() || getPassengers().contains(player))) {
            player.startRiding(this);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getY() < -30.0D) {
            moveTo(getX() + random.nextGaussian() * 30.0D, 256.0D,
                    getZ() + random.nextGaussian() * 30.0D);
        }
    }

    @Override
    public void checkDespawn() {
        // EntityQuackos overrides EntityCreature#despawnEntity: when the
        // normal creature distance/random-age test fires it leaves the world
        // with the old balefire burst and three spawn-duck drops.  Reproduce
        // that decision here instead of allowing the vanilla discard path.
        if (level().isClientSide || isPersistenceRequired() || requiresCustomPersistence()) {
            noActionTime = 0;
            return;
        }
        Entity nearest = level().getNearestPlayer(this, -1.0D);
        if (nearest == null) {
            return;
        }
        double distanceSquared = nearest.distanceToSqr(this);
        int despawnDistance = getType().getCategory().getDespawnDistance();
        int noDespawnDistance = getType().getCategory().getNoDespawnDistance();
        if (distanceSquared > (double) despawnDistance * despawnDistance
                || noActionTime > 600 && random.nextInt(800) == 0
                && distanceSquared > (double) noDespawnDistance * noDespawnDistance) {
            legacyDespawn();
        } else if (distanceSquared < (double) noDespawnDistance * noDespawnDistance) {
            noActionTime = 0;
        }
    }

    private void legacyDespawn() {
        if (level() instanceof ServerLevel serverLevel) {
            // EntityQuackos emitted 150 individually positioned balefire
            // particles with a uniform box distribution.  The old packet
            // used a 150-block TargetPoint, so use the explicit player
            // overload rather than the modern default tracking radius.
            for (int i = 0; i < 150; i++) {
                double x = getX() + random.nextDouble() * 20.0D - 10.0D;
                double y = getY() + random.nextDouble() * 25.0D;
                double z = getZ() + random.nextDouble() * 20.0D - 10.0D;
                for (ServerPlayer player : serverLevel.players()) {
                    if (player.distanceToSqr(getX(), getY(), getZ()) <= 22500.0D) {
                        serverLevel.sendParticles(player,
                                HbmParticleTypes.FLAMETHROWER_BALEFIRE.get(),
                                true, x, y, z, 1,
                                0.0D, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
            net.minecraft.world.item.Item duckSpawner = BuiltInRegistries.ITEM.get(
                    ReinhardtsHBM.id("spawn_duck"));
            if (duckSpawner != net.minecraft.world.item.Items.AIR) {
                spawnAtLocation(new net.minecraft.world.item.ItemStack(duckSpawner, 3));
            }
        }
        discard();
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        float yaw = yBodyRot * ((float) Math.PI / 180.0F);
        double offsetX = Math.sin(yaw) * 0.1D;
        double offsetZ = -Math.cos(yaw) * 0.1D;
        // Entity#positionRider subtracts the passenger's vehicle attachment
        // point.  Add that point back here so the final rider coordinates
        // stay at the exact 1.7.10 height (height - 0.125 + yOffset).
        Vec3 attachment = passenger.getVehicleAttachmentPoint(this);
        return position().add(offsetX + attachment.x, getBbHeight() - 0.125D + attachment.y,
                offsetZ + attachment.z);
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        if (passenger instanceof LivingEntity living) {
            living.yBodyRot = yBodyRot;
            living.yHeadRot = yBodyRot;
            living.yHeadRotO = living.yHeadRot;
        }
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }
}
