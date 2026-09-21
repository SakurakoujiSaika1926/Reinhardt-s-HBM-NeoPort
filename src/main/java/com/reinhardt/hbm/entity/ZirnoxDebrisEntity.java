package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.radiation.RadiationShielding;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Physical debris emitted by a ZIRNOX meltdown, matching EntityZirnoxDebris. */
public final class ZirnoxDebrisEntity extends Entity {
    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(ZirnoxDebrisEntity.class, EntityDataSerializers.INT);

    public float rot;
    public float lastRot;

    public ZirnoxDebrisEntity(EntityType<? extends ZirnoxDebrisEntity> type, Level level) {
        super(type, level);
        this.rot = this.lastRot = this.random.nextFloat() * 360.0F;
    }

    public ZirnoxDebrisEntity(Level level, double x, double y, double z, DebrisType type) {
        this(HbmEntityTypes.ZIRNOX_DEBRIS.get(), level);
        setPos(x, y, z);
        setDebrisType(type);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, DebrisType.BLANK.ordinal());
    }

    public DebrisType debrisType() {
        return DebrisType.values()[Math.floorMod(this.entityData.get(TYPE), DebrisType.values().length)];
    }

    public void setDebrisType(DebrisType type) {
        this.entityData.set(TYPE, type.ordinal());
        refreshDimensions();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            ItemStack stack = renderStack();
            if (!stack.isEmpty() && player.getInventory().add(stack)) {
                discard();
                player.inventoryMenu.broadcastChanges();
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick() {
        super.tick();
        DebrisType type = debrisType();
        if (!level().isClientSide) {
            if ((type == DebrisType.CONCRETE || type == DebrisType.EXCHANGER)
                    && getDeltaMovement().y > 0.0D) {
                breakImpact();
                if (!isAlive()) {
                    return;
                }
            }
            if (type == DebrisType.ELEMENT || type == DebrisType.GRAPHITE) {
                irradiateNearby(type == DebrisType.ELEMENT ? 7.0F : 4.0F);
            }
            if (!HbmConfig.RBMK_PERMANENT_SCRAP.get() && this.tickCount > lifetime() + getId() % 50) {
                discard();
                return;
            }
        }

        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        Vec3 requested = getDeltaMovement().add(0.0D, -0.04D, 0.0D);
        Vec3 before = position();
        move(MoverType.SELF, requested);
        Vec3 actual = position().subtract(before);
        double motionX = requested.x;
        double motionY = requested.y;
        double motionZ = requested.z;
        if (collided(requested.x, actual.x)) motionX *= -0.75D;
        if (collided(requested.y, actual.y)) motionY = 0.0D;
        if (collided(requested.z, actual.z)) motionZ *= -0.75D;

        this.lastRot = this.rot;
        if (onGround()) {
            motionX *= 0.85D;
            motionZ *= 0.85D;
            motionY *= -0.5D;
        } else {
            this.rot += 10.0F;
            if (this.rot >= 360.0F) {
                this.rot -= 360.0F;
                this.lastRot -= 360.0F;
            }
        }
        setDeltaMovement(motionX, motionY, motionZ);
    }

    private static boolean collided(double requested, double actual) {
        return Math.abs(requested - actual) > 1.0E-7D;
    }

    private void breakImpact() {
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement().scale(2.0D));
        BlockHitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos center = hit.getBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int rn = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (rn <= 1 || random.nextInt(rn) == 0) {
                        level().removeBlock(center.offset(dx, dy, dz), false);
                    }
                }
            }
        }
        discard();
    }

    private void irradiateNearby(float dose) {
        AABB area = getBoundingBox().inflate(2.5D);
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, area)) {
            float attenuated = level() instanceof ServerLevel serverLevel
                    ? RadiationShielding.attenuateDirectDose(serverLevel, blockPosition(), entity, dose)
                    : dose;
            if (attenuated <= 0.0F) continue;
            HbmLivingRadiation radiation = HbmLivingRadiation.get(entity);
            radiation.addEnvironmentRadiation(attenuated);
            if (!(entity instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                radiation.addRadiation((float) (attenuated * HbmArmorProtection.radiationMultiplier(entity)));
            }
            HbmLivingRadiation.set(entity, radiation);
        }
    }

    private int lifetime() {
        return switch (debrisType()) {
            case BLANK -> 3 * 60 * 20;
            case ELEMENT -> 10 * 60 * 20;
            case SHRAPNEL, GRAPHITE -> 15 * 60 * 20;
            case CONCRETE, EXCHANGER -> 60 * 20;
        };
    }

    public ItemStack renderStack() {
        String id = switch (debrisType()) {
            case BLANK -> "debris_metal";
            case ELEMENT -> "debris_element";
            case SHRAPNEL -> "debris_shrapnel";
            case GRAPHITE -> "debris_graphite";
            case CONCRETE -> "debris_concrete";
            case EXCHANGER -> "debris_exchanger";
        };
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ReinhardtsHBM.MOD_ID, id));
        return item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return switch (debrisType()) {
            case BLANK, SHRAPNEL -> EntityDimensions.scalable(0.5F, 0.5F);
            case ELEMENT, CONCRETE -> EntityDimensions.scalable(0.75F, 0.5F);
            case GRAPHITE -> EntityDimensions.scalable(0.25F, 0.25F);
            case EXCHANGER -> EntityDimensions.scalable(1.0F, 0.5F);
        };
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive();
    }

    @Override
    public float getPickRadius() {
        return 0.35F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0D * 128.0D;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("debtype", debrisType().ordinal());
        tag.putFloat("rot", this.rot);
        tag.putFloat("lastRot", this.lastRot);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setDebrisType(DebrisType.values()[Math.floorMod(tag.getInt("debtype"), DebrisType.values().length)]);
        this.rot = tag.getFloat("rot");
        this.lastRot = tag.getFloat("lastRot");
    }

    public enum DebrisType {
        BLANK, ELEMENT, SHRAPNEL, GRAPHITE, CONCRETE, EXCHANGER
    }
}
