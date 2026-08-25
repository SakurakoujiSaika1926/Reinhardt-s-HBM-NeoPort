package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.DroneDockBlockEntity;
import com.reinhardt.hbm.blockentity.DroneProviderBlockEntity;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared old 3x3 menu geometry used by the logistics drone dock and provider crate. */
public final class DroneGridMenu extends AbstractContainerMenu {
    private static final int SLOTS = 9;
    private final Container container;

    public DroneGridMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer, boolean dock) {
        this(id, inventory, resolve(inventory, buffer.readBlockPos(), dock), dock ? HbmMenus.DRONE_DOCK.get() : HbmMenus.DRONE_PROVIDER.get());
    }

    public DroneGridMenu(int id, Inventory inventory, Container container, MenuType<?> menuType) {
        super(menuType, id);
        this.container = container;
        checkContainerSize(container, SLOTS);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(container, column + row * 3, 62 + column * 18, 17 + row * 18));
            }
        }
        addPlayerInventory(inventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 103 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 161));
        }
    }

    private static Container resolve(Inventory inventory, BlockPos pos, boolean dock) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        if (dock && entity instanceof DroneDockBlockEntity dockEntity) {
            return dockEntity;
        }
        if (!dock && entity instanceof DroneProviderBlockEntity provider) {
            return provider;
        }
        return new SimpleContainer(SLOTS);
    }
}
