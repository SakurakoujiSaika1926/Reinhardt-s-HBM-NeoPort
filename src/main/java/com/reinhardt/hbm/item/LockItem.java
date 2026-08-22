package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.LockableBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LockItem extends KeyPinItem {
    private final double lockMod;

    public LockItem(Properties properties, double lockMod) {
        super(properties);
        this.lockMod = lockMod;
    }

    public double lockMod() {
        return this.lockMod;
    }

    public static boolean isLock(ItemStack stack) {
        return stack.getItem() instanceof LockItem;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        int pins = KeyPinItem.pins(stack);
        if (pins == 0) {
            return InteractionResult.PASS;
        }
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            blockEntity = dummy.core();
        }
        if (!(blockEntity instanceof LockableBlockEntity lockable) || lockable.isLocked()) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide) {
            lockable.setPins(pins);
            lockable.setLockMod(this.lockMod);
            lockable.setCheesable(this.lockMod != 0.0D);
            lockable.lock();
            context.getLevel().playSound(null, context.getClickedPos(), HbmSoundEvents.LOCK_HANG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
