package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.blockentity.SatelliteDockBlockEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Exact modern port of the latest 1.7.10 EntitySatellitePod. */
public final class MinerRocketEntity extends Entity {
    private static final EntityDataAccessor<Boolean> LEGS_DEPLOYED =
            SynchedEntityData.defineId(MinerRocketEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FREQUENCY =
            SynchedEntityData.defineId(MinerRocketEntity.class, EntityDataSerializers.INT);
    private static final float LEG_SPEED = 1.0F / 20.0F;

    private int timer;
    private int callerYPos;
    private double speed = 0.75D;
    public float legs;
    public float previousLegs;
    private final List<ItemStack> cargo = new ArrayList<>();

    public MinerRocketEntity(EntityType<? extends MinerRocketEntity> type, Level level) {
        super(type, level);
    }

    public MinerRocketEntity(Level level, int frequency) {
        this(HbmEntityTypes.MINER_ROCKET.get(), level);
        setFrequency(frequency);
    }

    public MinerRocketEntity(Level level, int frequency, int callerYPos, List<ItemStack> cargo) {
        this(level, frequency);
        this.callerYPos = callerYPos;
        for (ItemStack stack : cargo) {
            if (!stack.isEmpty()) this.cargo.add(stack.copy());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LEGS_DEPLOYED, false);
        builder.define(FREQUENCY, 0);
    }

    public int frequency() { return entityData.get(FREQUENCY); }
    public void setFrequency(int frequency) { entityData.set(FREQUENCY, frequency); }
    public boolean deploysLegs() { return entityData.get(LEGS_DEPLOYED); }
    private void setDeployLegs(boolean deploy) { entityData.set(LEGS_DEPLOYED, deploy); }
    private boolean isLanding() { return !cargo.isEmpty(); }

    @Override
    public void tick() {
        super.tick();
        setYRot(0.0F);
        setXRot(0.0F);

        if (!level().isClientSide) {
            if (isLanding()) {
                if (timer > 0) {
                    timer++;
                    setPos(getX(), Math.ceil(getY()), getZ());
                    setDeltaMovement(Vec3.ZERO);
                    if (timer >= 100) unloadItems();
                } else if (onGround()) {
                    speed = 0.0D;
                    timer = 1;
                    setDeployLegs(true);
                    setDeltaMovement(Vec3.ZERO);
                } else {
                    if (getY() < callerYPos + 17.0D && !deploysLegs()) setDeployLegs(true);
                    if (getY() < callerYPos + 25.0D) speed -= 0.01D;
                    speed = Mth.clamp(speed, 0.025D, 0.75D);
                    setDeltaMovement(0.0D, -speed, 0.0D);
                    move(MoverType.SELF, getDeltaMovement());
                }
            } else {
                setOnGround(false);
                speed += 0.01D;
                if (speed >= 0.2D) setDeployLegs(false);
                speed = Mth.clamp(speed, 0.0D, 2.0D);
                setDeltaMovement(0.0D, speed, 0.0D);
                move(MoverType.SELF, getDeltaMovement());
                if (getY() > 300.0D) discard();
            }
        } else {
            previousLegs = legs;
            legs = Mth.clamp(legs + (deploysLegs() ? LEG_SPEED : -LEG_SPEED), 0.0F, 1.0F);
            if ((legs > 0.0F && getDeltaMovement().y < 0.0D) || getDeltaMovement().y > 0.0D) {
                level().addParticle(HbmParticleTypes.GAS_FLARE_FLAME.get(),
                        getX(), getY() + 0.5D, getZ(), 0.0D, speed - 1.0D, 0.0D);
            }
        }
    }

    private void unloadItems() {
        BlockPos below = BlockPos.containing(getX(), getY() - 0.5D, getZ());
        List<ItemStack> remainder = cargo.stream().map(ItemStack::copy).toList();
        if (level().getBlockEntity(below) instanceof SatelliteDockBlockEntity dock) {
            remainder = dock.acceptCargo(remainder);
        }
        for (ItemStack stack : remainder) {
            if (!stack.isEmpty()) spawnAtLocation(stack, 0.25F);
        }
        cargo.clear();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (amount >= 5.0F && !level().isClientSide && isAlive()) {
            Vec3 motion = getDeltaMovement();
            discard();
            level().explode(null, getX(), getY() + 1.5D, getZ(), 15.0F,
                    true, Level.ExplosionInteraction.BLOCK);
            if (motion.lengthSqr() > 0.0D) {
                LegacyProjectileUtil.spawnShrapnelShower(level(),
                        new Vec3(getX(), getY() + 1.5D, getZ()), motion, 15, 0.25D);
            }
        }
        return true;
    }

    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 500_000.0D; }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("LegsDeployed", deploysLegs());
        tag.putInt("Satellite", frequency());
        tag.putInt("Timer", timer);
        tag.putInt("CallerYPos", callerYPos);
        tag.putDouble("Speed", speed);
        ListTag cargoTag = new ListTag();
        HolderLookup.Provider registries = level().registryAccess();
        for (ItemStack stack : cargo) {
            if (!stack.isEmpty()) cargoTag.add(stack.saveOptional(registries));
        }
        tag.put("Cargo", cargoTag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setDeployLegs(tag.getBoolean("LegsDeployed"));
        setFrequency(tag.getInt("Satellite"));
        timer = tag.getInt("Timer");
        callerYPos = tag.getInt("CallerYPos");
        speed = tag.contains("Speed") ? tag.getDouble("Speed") : 0.75D;
        cargo.clear();
        HolderLookup.Provider registries = level().registryAccess();
        ListTag cargoTag = tag.getList("Cargo", Tag.TAG_COMPOUND);
        for (int index = 0; index < cargoTag.size(); index++) {
            ItemStack stack = ItemStack.parseOptional(registries, cargoTag.getCompound(index));
            if (!stack.isEmpty()) cargo.add(stack);
        }
    }
}
