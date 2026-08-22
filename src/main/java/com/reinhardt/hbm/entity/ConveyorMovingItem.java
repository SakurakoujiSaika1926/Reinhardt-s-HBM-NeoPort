package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.block.ConveyorBlock;
import com.reinhardt.hbm.blockentity.ConveyorPressBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Modern equivalent of 1.7.10 EntityMovingItem. */
public final class ConveyorMovingItem extends Entity {
    private static final EntityDataAccessor<ItemStack> ITEM =
            SynchedEntityData.defineId(ConveyorMovingItem.class, EntityDataSerializers.ITEM_STACK);

    public ConveyorMovingItem(EntityType<? extends ConveyorMovingItem> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ConveyorMovingItem(Level level, ItemStack stack) {
        this(HbmEntityTypes.CONVEYOR_ITEM.get(), level);
        setItemStack(stack);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ITEM, ItemStack.EMPTY);
    }

    public ItemStack getItemStack() { return this.entityData.get(ITEM); }
    public void setItemStack(ItemStack stack) { this.entityData.set(ITEM, stack.copy()); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount <= 5) return;
        if (getItemStack().isEmpty()) {
            discard();
            return;
        }
        BlockState state = level().getBlockState(blockPosition());
        Vec3 target;
        if (state.getBlock() instanceof ConveyorBlock conveyor) {
            target = conveyor.travelLocation(level(), blockPosition(), state, position(), 0.0625D);
        } else if (level().getBlockEntity(blockPosition()) instanceof MachineDummyBlockEntity dummy
                && dummy.core() instanceof ConveyorPressBlockEntity press
                && press.isBeltPosition(blockPosition())) {
            target = press.travelLocation(position(), 0.0625D);
        } else {
            leaveConveyor();
            return;
        }
        Vec3 movement = target.subtract(position());
        setDeltaMovement(movement);
        move(MoverType.SELF, movement);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide && !isRemoved() && player.getInventory().add(getItemStack().copy())) {
            discard();
            player.inventoryMenu.broadcastChanges();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isRemoved()) {
            ItemEntity item = new ItemEntity(level(), getX(), getY(), getZ(), getItemStack().copy());
            item.setDeltaMovement(getDeltaMovement());
            level().addFreshEntity(item);
            discard();
        }
        return true;
    }

    @Override public boolean isPickable() { return isAlive(); }
    @Override public float getPickRadius() { return 0.1875F; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 102400.0D; }

    private void leaveConveyor() {
        Vec3 motion = getDeltaMovement();
        ItemEntity item = new ItemEntity(level(), getX() + motion.x * 2.0D, getY() + motion.y * 2.0D, getZ() + motion.z * 2.0D,
                getItemStack().copy());
        item.setDeltaMovement(motion.x * 2.0D, 0.1D, motion.z * 2.0D);
        level().addFreshEntity(item);
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Item", getItemStack().saveOptional(registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setItemStack(ItemStack.parseOptional(registryAccess(), tag.getCompound("Item")));
    }
}
