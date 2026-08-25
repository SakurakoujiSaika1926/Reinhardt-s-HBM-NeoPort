package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.item.UniversalGrenadeItem;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/** Direct modern counterpart of 1.7.10 EntityGrenadeUniversal. */
public final class UniversalGrenadeEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> GRENADE =
            SynchedEntityData.defineId(UniversalGrenadeEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> TIMER =
            SynchedEntityData.defineId(UniversalGrenadeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STUCK =
            SynchedEntityData.defineId(UniversalGrenadeEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ownerId;

    public UniversalGrenadeEntity(EntityType<? extends UniversalGrenadeEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public UniversalGrenadeEntity(Level level, Player owner, ItemStack stack) {
        this(HbmEntityTypes.UNIVERSAL_GRENADE.get(), level);
        setGrenade(stack);
        ownerId = owner.getUUID();
        Vec3 direction = owner.getLookAngle();
        Vec3 lateral = new Vec3(0.25D, -0.25D, 0.0D).yRot((float) Math.toRadians(-owner.getYRot() + 180.0F));
        setPos(owner.getX() + lateral.x, owner.getEyeY() + lateral.y, owner.getZ() + lateral.z);
        setDeltaMovement(direction.scale(shell().throwForce()));
        updateRotation(getDeltaMovement());
    }

    private UniversalGrenadeEntity(Level level, ItemStack stack, @Nullable UUID ownerId, Vec3 motion) {
        this(HbmEntityTypes.UNIVERSAL_GRENADE.get(), level);
        setGrenade(stack);
        this.ownerId = ownerId;
        setDeltaMovement(motion);
        updateRotation(motion);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(GRENADE, ItemStack.EMPTY);
        builder.define(TIMER, 0);
        builder.define(STUCK, false);
    }

    public ItemStack grenadeStack() {
        return entityData.get(GRENADE);
    }

    @Override
    public void tick() {
        super.tick();
        if (grenadeStack().isEmpty()) {
            discard();
            return;
        }

        if (!entityData.get(STUCK)) {
            moveGrenade();
        }
        if (!level().isClientSide) {
            int timer = entityData.get(TIMER) + 1;
            entityData.set(TIMER, timer);
            if (checkFuze(timer) || checkProximityFuze(timer)) {
                detonate();
                return;
            }
        } else if (extra() == UniversalGrenadeItem.Extra.TRIPLEX) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, 0.01D, 0.0D);
        }
    }

    private void moveGrenade() {
        Vec3 start = position();
        Vec3 motion = getDeltaMovement();
        Vec3 end = start.add(motion);
        BlockHitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        EntityHitResult entityHit = findEntityHit(start, entityEnd);

        if (entityHit != null) {
            if (!level().isClientSide && fuze() == UniversalGrenadeItem.Fuze.IMPACT && timer() >= 10) {
                setPos(entityHit.getLocation());
                detonate();
                return;
            }
            move(MoverType.SELF, motion);
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            if (!level().isClientSide && fuze() == UniversalGrenadeItem.Fuze.IMPACT && timer() >= 10) {
                setPos(blockHit.getLocation());
                detonate();
                return;
            }
            if (extra() == UniversalGrenadeItem.Extra.GLUE) {
                setPos(blockHit.getLocation().add(Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()).scale(0.05D)));
                entityData.set(STUCK, true);
                setDeltaMovement(Vec3.ZERO);
                return;
            }
            Direction side = blockHit.getDirection();
            setPos(blockHit.getLocation().add(Vec3.atLowerCornerOf(side.getNormal()).scale(0.05D)));
            if (motion.length() > 0.2D && !level().isClientSide) {
                level().playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_HIT, SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            motion = reflect(motion, side).scale(shell().bounce());
        } else {
            move(MoverType.SELF, motion);
        }

        updateRotation(motion);
        if (isInWater() && level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                level().addParticle(ParticleTypes.BUBBLE, getX(), getY(), getZ(), motion.x, motion.y, motion.z);
            }
        }
        setDeltaMovement(motion.scale(isInWater() ? 0.8D : 0.99D).add(0.0D, -0.03D, 0.0D));
    }

    private boolean checkFuze(int timer) {
        return switch (fuze()) {
            case S3 -> timer >= 60;
            case S7 -> timer >= 140;
            case S15 -> timer >= 300;
            case IMPACT -> false;
            case AIRBURST -> timer >= 30 && hasGroundWithinTenBlocks();
        };
    }

    private boolean checkProximityFuze(int timer) {
        if (extra() != UniversalGrenadeItem.Extra.PROXY_FUZE || timer < 10 || timer % 3 != 0) {
            return false;
        }
        Entity owner = owner();
        AABB area = getBoundingBox().inflate(10.0D);
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, area, Entity::isAlive)) {
            if (living != owner && living.distanceTo(this) <= 10.0F) {
                return true;
            }
        }
        return false;
    }

    private boolean hasGroundWithinTenBlocks() {
        HitResult hit = level().clip(new ClipContext(position(), position().add(0.0D, -10.0D, 0.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.BLOCK;
    }

    private void detonate() {
        if (isRemoved()) return;
        Vec3 center = position();
        switch (filling()) {
            case POWDER -> explosion(center, 5.0F, 10.0F, false);
            case HE -> explosion(center, 7.5F, 25.0F, false);
            case DEMO -> explosion(center, 5.0F, 10.0F, true);
            case INC -> {
                explosion(center, 3.0F, 10.0F, false);
                linger(center, LegacyLingeringFireEntity.FireType.DIESEL, 6.0F, 2.0F, 200);
                igniteCube(center, 2);
            }
            case WP -> {
                explosion(center, 3.0F, 10.0F, false);
                linger(center, LegacyLingeringFireEntity.FireType.PHOSPHORUS, 6.0F, 2.0F, 600);
                igniteCube(center, 3);
            }
            case CLUSTER -> {
                explosion(center, 7.5F, 15.0F, false);
                spawnFragments(shell() == UniversalGrenadeItem.Shell.FRAG ? 37 : 30, 0.5D, 0.75D);
            }
            case EMP -> energyBlast(center, 3.0F, 15.0F, HbmDamageTypes.ELECTRICITY, 5.0D);
            case PLASMA -> energyBlast(center, 5.0F, 50.0F, HbmDamageTypes.SEDNA_EXPLOSIVE, 0.0D);
            case LASER -> laserBurst(center);
            case CLUSTER_HEAVY -> {
                explosion(center, 7.5F, 15.0F, false);
                spawnFragments(15, 0.5D, 1.25D);
            }
            case NUCLEAR -> {
                explosion(center, 10.0F, 100.0F, false);
                addRadiation(1.0D);
            }
            case NUCLEAR_DEMO -> {
                explosion(center, 10.0F, 50.0F, true);
                addRadiation(1.5D);
                igniteCube(center, 2);
            }
            case SCHRAB -> {
                // EntityCloudFleija and the MK3 fleija core are not legacy
                // catalog stand-ins: this is the actual available modern
                // explosion pipeline with the old 20-block blast radius.
                explosion(center, 20.0F, 100.0F, true);
                addRadiation(4.0D);
            }
        }
        if (extra() == UniversalGrenadeItem.Extra.FRAG_SLEEVE) {
            spawnFragments(shell() == UniversalGrenadeItem.Shell.FRAG ? 37 : 25, 1.0D, 1.0D);
        } else if (extra() == UniversalGrenadeItem.Extra.TRIPLEX) {
            spawnTriplex();
        }
        discard();
    }

    private void explosion(Vec3 center, float radius, float damage, boolean breakBlocks) {
        LegacyProjectileUtil.fixedDamageExplosion(level(), this, center, radius, damage, breakBlocks);
    }

    private void energyBlast(Vec3 center, float radius, float damage,
                             net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> damageType,
                             double empRadius) {
        explosion(center, radius, damage, false);
        AABB area = getBoundingBox().inflate(radius);
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, area, Entity::isAlive)) {
            if (living.distanceToSqr(center) <= radius * radius) {
                living.hurt(damageSources().source(damageType, this, owner()), damage);
            }
        }
        if (empRadius > 0.0D) {
            for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(empRadius), Entity::isAlive)) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
            }
        }
    }

    private void laserBurst(Vec3 center) {
        explosion(center, 2.0F, 5.0F, false);
        Entity owner = owner();
        for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(15.0D), Entity::isAlive)) {
            if (target == owner || target.distanceToSqr(center) > 225.0D) continue;
            target.hurt(damageSources().source(HbmDamageTypes.SEDNA_EXPLOSIVE, this, owner), 30.0F);
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                        4, 0.05D, 0.05D, 0.05D, 0.0D);
            }
        }
    }

    private void linger(Vec3 center, LegacyLingeringFireEntity.FireType type, float width, float height, int duration) {
        level().addFreshEntity(new LegacyLingeringFireEntity(level(), center, type, width, height, duration));
    }

    private void igniteCube(Vec3 center, int radius) {
        BlockPos origin = BlockPos.containing(center);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            if (level().getBlockState(pos).isAir() && Blocks.FIRE.defaultBlockState().canSurvive(level(), pos)) {
                level().setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }

    private void spawnFragments(int count, double horizontalScale, double verticalScale) {
        for (int index = 0; index < count; index++) {
            Vec3 motion = randomDirection().scale(1.5D);
            motion = new Vec3(motion.x * horizontalScale, Math.abs(motion.y) * verticalScale, motion.z * horizontalScale);
            level().addFreshEntity(new LegacyShrapnelEntity(level(), getX(), getY() + 0.05D, getZ(), motion, false));
        }
    }

    private void spawnTriplex() {
        ItemStack fragment = UniversalGrenadeItem.make(shell(), filling(), UniversalGrenadeItem.Fuze.S3, null);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        for (int index = 0; index < 3; index++) {
            Vec3 motion = new Vec3(Math.cos(angle) * 0.25D, 0.75D, Math.sin(angle) * 0.25D);
            UniversalGrenadeEntity child = new UniversalGrenadeEntity(level(), fragment, ownerId, motion);
            child.setPos(position());
            level().addFreshEntity(child);
            angle += Math.PI * 2.0D / 3.0D;
        }
    }

    private void addRadiation(double multiplier) {
        if (!(level() instanceof ServerLevel server)) return;
        BlockPos origin = blockPosition();
        ChunkRadiationData data = ChunkRadiationData.get(server);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                int distance = Math.abs(x) + Math.abs(z);
                if (distance < 4) {
                    data.incrementRadiation(origin.offset(x * 16, 0, z * 16), 50.0D / (distance + 1) * multiplier);
                }
            }
        }
    }

    @Nullable
    private Entity owner() {
        if (ownerId == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(ownerId);
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        Entity owner = owner();
        AABB area = getBoundingBox().expandTowards(end.subtract(start)).inflate(0.3D);
        Entity closest = null;
        Vec3 closestPoint = null;
        double distance = Double.MAX_VALUE;
        for (Entity entity : level().getEntities(this, area, entity -> entity.isAlive() && entity.isPickable() && entity != owner)) {
            var hit = entity.getBoundingBox().inflate(0.2D).clip(start, end);
            if (hit.isEmpty()) continue;
            double candidate = start.distanceToSqr(hit.get());
            if (candidate < distance) {
                closest = entity;
                closestPoint = hit.get();
                distance = candidate;
            }
        }
        return closest == null ? null : new EntityHitResult(closest, closestPoint);
    }

    private static Vec3 reflect(Vec3 motion, Direction side) {
        return switch (side.getAxis()) {
            case X -> new Vec3(-motion.x, motion.y, motion.z);
            case Y -> new Vec3(motion.x, -motion.y, motion.z);
            case Z -> new Vec3(motion.x, motion.y, -motion.z);
        };
    }

    private Vec3 randomDirection() {
        double y = random.nextDouble() * 2.0D - 1.0D;
        double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
        double angle = random.nextDouble() * Math.PI * 2.0D;
        return new Vec3(Math.cos(angle) * horizontal, y, Math.sin(angle) * horizontal);
    }

    private void setGrenade(ItemStack stack) {
        entityData.set(GRENADE, stack.copyWithCount(1));
    }

    private int timer() { return entityData.get(TIMER); }
    private UniversalGrenadeItem.Shell shell() { return UniversalGrenadeItem.shell(grenadeStack()); }
    private UniversalGrenadeItem.Filling filling() { return UniversalGrenadeItem.filling(grenadeStack()); }
    private UniversalGrenadeItem.Fuze fuze() { return UniversalGrenadeItem.fuze(grenadeStack()); }
    @Nullable private UniversalGrenadeItem.Extra extra() { return UniversalGrenadeItem.extra(grenadeStack()); }

    private void updateRotation(Vec3 motion) {
        if (motion.lengthSqr() < 1.0E-8D) return;
        setYRot((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
        setXRot(getXRot() - (float) (motion.length() * 25.0D));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("grenade", grenadeStack().save(registryAccess()));
        tag.putInt("timer", timer());
        tag.putBoolean("stuck", entityData.get(STUCK));
        if (ownerId != null) tag.putUUID("owner", ownerId);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setGrenade(ItemStack.parseOptional(registryAccess(), tag.getCompound("grenade")));
        entityData.set(TIMER, tag.getInt("timer"));
        entityData.set(STUCK, tag.getBoolean("stuck"));
        ownerId = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 65536.0D; }
}
