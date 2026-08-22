package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

public class RbmkDebrisEntity extends Entity {
    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(RbmkDebrisEntity.class, EntityDataSerializers.INT);

    public float rot;
    public float lastRot;

    public RbmkDebrisEntity(EntityType<? extends RbmkDebrisEntity> entityType, Level level) {
        super(entityType, level);
        this.rot = this.lastRot = this.random.nextFloat() * 360.0F;
    }

    public RbmkDebrisEntity(Level level, double x, double y, double z, DebrisType type) {
        this(HbmEntityTypes.RBMK_DEBRIS.get(), level);
        setPos(x, y, z);
        setDebrisType(type);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, DebrisType.BLANK.ordinal());
    }

    public void setDebrisType(DebrisType type) {
        this.entityData.set(TYPE, type.ordinal());
        refreshDimensions();
    }

    public DebrisType debrisType() {
        return DebrisType.values()[Math.floorMod(this.entityData.get(TYPE), DebrisType.values().length)];
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            ItemStack stack = pickupStack();
            if (!stack.isEmpty() && player.getInventory().add(stack)) {
                discard();
                player.inventoryMenu.broadcastChanges();
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        DebrisType type = debrisType();
        if (!level().isClientSide) {
            if (type == DebrisType.LID && getDeltaMovement().y > 0.0D) {
                breakLidImpact();
            }
            if (type == DebrisType.FUEL || type == DebrisType.GRAPHITE) {
                irradiateNearby(type == DebrisType.FUEL ? 9.0F : 4.0F);
            }
            if (this.tickCount > lifetime() + getId() % 50) {
                discard();
                return;
            }
        }

        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement().add(0.0D, -0.04D, 0.0D);
        setDeltaMovement(motion);
        move(net.minecraft.world.entity.MoverType.SELF, motion);

        this.lastRot = this.rot;
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(0.85D, -0.5D, 0.85D));
        } else {
            this.rot += 10.0F;
            if (this.rot >= 360.0F) {
                this.rot -= 360.0F;
                this.lastRot -= 360.0F;
            }
        }
    }

    private void breakLidImpact() {
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement().scale(2.0D));
        BlockHitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos center = hit.getBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int rn = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (rn <= 1 || random.nextInt(rn) == 0) {
                        BlockPos target = center.offset(dx, dy, dz);
                        if (level().getBlockState(target).getDestroySpeed(level(), target) >= 0.0F) {
                            level().destroyBlock(target, false);
                        }
                    }
                }
            }
        }
        discard();
    }

    private void irradiateNearby(float dose) {
        AABB area = getBoundingBox().inflate(2.5D);
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, area)) {
            HbmLivingRadiation data = HbmLivingRadiation.get(entity);
            data.addEnvironmentRadiation(dose);
            if (!(entity instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                data.addRadiation(dose);
            }
            HbmLivingRadiation.set(entity, data);
        }
    }

    private int lifetime() {
        return switch (debrisType()) {
            case FUEL -> 10 * 60 * 20;
            case GRAPHITE -> 15 * 60 * 20;
            case LID -> 30 * 20;
            case ROD -> 60 * 20;
            default -> 3 * 60 * 20;
        };
    }

    private ItemStack pickupStack() {
        return switch (debrisType()) {
            case FUEL -> legacyItem("debris_fuel");
            case GRAPHITE -> legacyItem("debris_graphite");
            case LID -> new ItemStack(HbmItems.RBMK_LID.get());
            default -> legacyItem("debris_metal");
        };
    }

    private static ItemStack legacyItem(String id) {
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(id));
        return item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public float getPickRadius() {
        return 0.35F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return switch (debrisType()) {
            case BLANK -> EntityDimensions.scalable(0.5F, 0.5F);
            case ELEMENT -> EntityDimensions.scalable(1.0F, 1.0F);
            case FUEL, GRAPHITE -> EntityDimensions.scalable(0.25F, 0.25F);
            case LID -> EntityDimensions.scalable(1.0F, 0.5F);
            case ROD -> EntityDimensions.scalable(0.75F, 0.5F);
        };
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("debtype", debrisType().ordinal());
        tag.putFloat("rot", rot);
        tag.putFloat("lastRot", lastRot);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setDebrisType(DebrisType.values()[Math.floorMod(tag.getInt("debtype"), DebrisType.values().length)]);
        rot = tag.getFloat("rot");
        lastRot = tag.getFloat("lastRot");
    }

    public enum DebrisType {
        BLANK,
        ELEMENT,
        FUEL,
        ROD,
        GRAPHITE,
        LID
    }
}
