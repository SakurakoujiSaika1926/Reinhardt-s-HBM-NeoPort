package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.item.KeyPinItem;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface LockableBlockEntity {
    boolean isLocked();

    void lock();

    void unlock();

    int pins();

    void setPins(int pins);

    double lockMod();

    void setLockMod(double mod);

    boolean cheesable();

    void setCheesable(boolean cheesable);

    void lockChanged();

    default boolean canAccess(Player player) {
        if (!isLocked()) {
            return true;
        }
        if (player == null) {
            return false;
        }
        ItemStack stack = player.getMainHandItem();
        if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("key_red")) {
            playLockOpen(player);
            return true;
        }
        if (!stack.isEmpty() && stack.getItem() instanceof KeyPinItem && KeyPinItem.pins(stack) == pins()) {
            playLockOpen(player);
            return true;
        }
        return tryPick(player);
    }

    default boolean tryPick(Player player) {
        Level level = player.level();
        ItemStack stack = player.getMainHandItem();
        boolean canPick = false;

        if (isNamedItem(stack, "pin") && hasInventoryItem(player, "screwdriver", "screwdriver_desh")) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            canPick = true;
        } else if (isScrewdriver(stack) && consumeInventoryItem(player, "pin")) {
            canPick = true;
        }

        if (!canPick) {
            return false;
        }

        double chanceOfSuccess = lockMod() * 100.0D;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (isNamedItem(chest, "jackt") || isNamedItem(chest, "jackt2")) {
            chanceOfSuccess *= 100.0D;
        }

        if (chanceOfSuccess > level.random.nextDouble() * 100.0D) {
            if (!level.isClientSide) {
                level.playSound(null, player.blockPosition(), HbmSoundEvents.PIN_UNLOCK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return true;
        }

        if (!level.isClientSide) {
            level.playSound(null, player.blockPosition(), HbmSoundEvents.PIN_BREAK.get(), SoundSource.PLAYERS, 1.0F, 0.8F + level.random.nextFloat() * 0.2F);
        }
        return false;
    }

    default void playLockOpen(Player player) {
        Level level = player.level();
        if (!level.isClientSide) {
            level.playSound(null, player.blockPosition(), HbmSoundEvents.LOCK_OPEN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private static boolean isScrewdriver(ItemStack stack) {
        return stack.is(HbmItems.SCREWDRIVER.get()) || stack.is(HbmItems.SCREWDRIVER_DESH.get());
    }

    private static boolean hasInventoryItem(Player player, String... names) {
        for (ItemStack stack : player.getInventory().items) {
            for (String name : names) {
                if (isNamedItem(stack, name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean consumeInventoryItem(Player player, String name) {
        for (ItemStack stack : player.getInventory().items) {
            if (isNamedItem(stack, name)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.getInventory().setChanged();
                    player.inventoryMenu.broadcastChanges();
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isNamedItem(ItemStack stack, String name) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals(name);
    }
}
