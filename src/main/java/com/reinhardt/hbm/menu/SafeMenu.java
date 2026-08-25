package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.SafeBlockEntity;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SafeMenu extends AbstractContainerMenu {
    private static final int STORAGE_END = SafeBlockEntity.SLOT_COUNT;
    private static final int PLAYER_END = STORAGE_END + 27;
    private final Container container;

    public SafeMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer.readBlockPos()));
    }

    public SafeMenu(int id, Inventory inventory, SafeBlockEntity safe) {
        this(id, inventory, (Container) safe);
    }

    private SafeMenu(int id, Inventory inventory, Container container) {
        super(HbmMenus.SAFE.get(), id);
        this.container = container;
        checkContainerSize(container, SafeBlockEntity.SLOT_COUNT);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 5; column++) {
                addSlot(new Slot(container, column + row * 5, 44 + column * 18, 18 + row * 18));
            }
        }
        addPlayerInventory(inventory, 8, 86);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        ItemStack original = slot.getItem();
        ItemStack result = original.copy();
        if (index < STORAGE_END) {
            if (!moveItemStackTo(original, STORAGE_END, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, STORAGE_END, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, left + column * 18, top + 58));
    }

    private static Container resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof SafeBlockEntity safe ? safe : new SimpleContainer(SafeBlockEntity.SLOT_COUNT);
    }
}
