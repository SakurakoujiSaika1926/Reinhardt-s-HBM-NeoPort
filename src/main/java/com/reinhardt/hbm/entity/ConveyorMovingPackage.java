package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.block.ConveyorBlock;
import com.reinhardt.hbm.blockentity.ConveyorPressBlockEntity;
import com.reinhardt.hbm.blockentity.CraneMachineBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

import java.util.ArrayList;
import java.util.List;

/** Modern equivalent of the old EntityMovingPackage used by boxer logistics. */
public final class ConveyorMovingPackage extends Entity {
    private static final int MAX_CONTENTS = 21;
    private final List<ItemStack> contents = new ArrayList<>();

    public ConveyorMovingPackage(EntityType<? extends ConveyorMovingPackage> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ConveyorMovingPackage(Level level, List<ItemStack> stacks) {
        this(HbmEntityTypes.CONVEYOR_PACKAGE.get(), level);
        setContents(stacks);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    public void setContents(List<ItemStack> stacks) {
        contents.clear();
        for (ItemStack stack : stacks) {
            if (contents.size() >= MAX_CONTENTS) break;
            if (!stack.isEmpty()) contents.add(stack.copy());
        }
    }

    public List<ItemStack> contents() {
        return contents.stream().map(ItemStack::copy).toList();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount <= 5) return;
        if (contents.isEmpty()) {
            discard();
            return;
        }
        if (ConveyorCrowding.check(this)) {
            return;
        }

        BlockPos before = blockPosition();
        BlockState state = level().getBlockState(before);
        Vec3 target;
        if (state.getBlock() instanceof ConveyorBlock conveyor) {
            target = conveyor.travelLocation(level(), before, state, position(), 0.0625D);
        } else if (level().getBlockEntity(before) instanceof MachineDummyBlockEntity dummy
                && dummy.core() instanceof ConveyorPressBlockEntity press
                && press.isBeltPosition(before)) {
            target = press.travelLocation(position(), 0.0625D);
        } else if (level().getBlockEntity(before) instanceof CraneMachineBlockEntity crane
                && crane.isConveyorSurface()) {
            target = crane.travelLocation(position(), 0.0625D);
        } else {
            leaveConveyor();
            return;
        }

        Vec3 movement = target.subtract(position());
        setDeltaMovement(movement);
        move(MoverType.SELF, movement);
        BlockPos after = blockPosition();
        if (!before.equals(after)) {
            tryEnterCrane(before, after);
        }
    }

    private void tryEnterCrane(BlockPos before, BlockPos after) {
        DirectionEntry entry = DirectionEntry.between(before, after);
        if (entry != null && level().getBlockEntity(after) instanceof CraneMachineBlockEntity crane
                && crane.acceptMovingPackage(contents(), entry.side())) {
            discard();
            return;
        }
        BlockState enteredState = level().getBlockState(after);
        if (!enteredState.isCollisionShapeFullBlock(level(), after)) {
            BlockPos below = after.below();
            if (level().getBlockEntity(below) instanceof CraneMachineBlockEntity crane
                    && crane.acceptMovingPackage(contents(), net.minecraft.core.Direction.UP)) {
                discard();
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide && !isRemoved()) {
            for (ItemStack stack : contents) {
                ItemStack remaining = stack.copy();
                player.getInventory().add(remaining);
                if (!remaining.isEmpty()) {
                    level().addFreshEntity(new ItemEntity(level(), getX(), getY() + 0.125D, getZ(), remaining));
                }
            }
            contents.clear();
            discard();
            player.inventoryMenu.broadcastChanges();
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isRemoved()) {
            dropContents(getDeltaMovement(), false);
            discard();
        }
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive();
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public float getPickRadius() {
        return 0.25F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 576.0D;
    }

    private void leaveConveyor() {
        dropContents(getDeltaMovement(), true);
        discard();
    }

    private void dropContents(Vec3 motion, boolean carryMomentum) {
        for (ItemStack stack : contents) {
            ItemEntity item = new ItemEntity(level(),
                    getX() + (carryMomentum ? motion.x * 2.0D : 0.0D),
                    getY() + 0.125D + (carryMomentum ? motion.y * 2.0D : 0.0D),
                    getZ() + (carryMomentum ? motion.z * 2.0D : 0.0D), stack.copy());
            if (carryMomentum) item.setDeltaMovement(motion.x * 2.0D, 0.1D, motion.z * 2.0D);
            level().addFreshEntity(item);
        }
        contents.clear();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Count", contents.size());
        for (int slot = 0; slot < contents.size(); slot++) {
            tag.put("Item" + slot, contents.get(slot).saveOptional(registryAccess()));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        contents.clear();
        int count = Math.clamp(tag.getInt("Count"), 0, MAX_CONTENTS);
        for (int slot = 0; slot < count; slot++) {
            ItemStack stack = ItemStack.parseOptional(registryAccess(), tag.getCompound("Item" + slot));
            if (!stack.isEmpty()) contents.add(stack);
        }
        if (contents.isEmpty()) discard();
    }

    /** Entry side points back toward the block the package just left. */
    private record DirectionEntry(net.minecraft.core.Direction side) {
        private static DirectionEntry between(BlockPos before, BlockPos after) {
            int dx = before.getX() - after.getX();
            int dy = before.getY() - after.getY();
            int dz = before.getZ() - after.getZ();
            if (dx > 0 && dy == 0 && dz == 0) return new DirectionEntry(net.minecraft.core.Direction.EAST);
            if (dx < 0 && dy == 0 && dz == 0) return new DirectionEntry(net.minecraft.core.Direction.WEST);
            if (dy > 0 && dx == 0 && dz == 0) return new DirectionEntry(net.minecraft.core.Direction.UP);
            if (dy < 0 && dx == 0 && dz == 0) return new DirectionEntry(net.minecraft.core.Direction.DOWN);
            if (dz > 0 && dx == 0 && dy == 0) return new DirectionEntry(net.minecraft.core.Direction.SOUTH);
            if (dz < 0 && dx == 0 && dy == 0) return new DirectionEntry(net.minecraft.core.Direction.NORTH);
            return null;
        }
    }
}
