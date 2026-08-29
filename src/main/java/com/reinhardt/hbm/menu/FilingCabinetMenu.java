package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.FilingCabinetBlockEntity;
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

public final class FilingCabinetMenu extends AbstractContainerMenu {
    private static final int CABINET_END = FilingCabinetBlockEntity.SLOT_COUNT;
    private final Container container;

    public FilingCabinetMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer.readBlockPos()));
    }

    public FilingCabinetMenu(int id, Inventory inventory, FilingCabinetBlockEntity cabinet) {
        this(id, inventory, (Container) cabinet);
    }

    private FilingCabinetMenu(int id, Inventory inventory, Container container) {
        super(HbmMenus.FILING_CABINET.get(), id);
        this.container = container;
        checkContainerSize(container, CABINET_END);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 4; column++) {
                addSlot(new Slot(container, column + row * 4, 53 + column * 18, 18 + row * 36));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 88 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 146));
        }
        if (container instanceof FilingCabinetBlockEntity cabinet) cabinet.openInventory();
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        ItemStack original = slot.getItem();
        ItemStack result = original.copy();
        if (index < CABINET_END) {
            if (!moveItemStackTo(original, CABINET_END, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, CABINET_END, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        if (container instanceof FilingCabinetBlockEntity cabinet) cabinet.closeInventory();
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }

    private static Container resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof FilingCabinetBlockEntity cabinet ? cabinet : new SimpleContainer(CABINET_END);
    }
}
