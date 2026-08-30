package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.block.LegacyNukeDefinition;
import com.reinhardt.hbm.blockentity.LegacyNukeBlockEntity;
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

public final class LegacyNukeMenu extends AbstractContainerMenu {
    private final Container container;
    private final LegacyNukeDefinition definition;
    private final int machineSlots;
    private final int playerStart;

    public LegacyNukeMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, containerAt(inventory, buffer.readBlockPos()));
    }

    public LegacyNukeMenu(int containerId, Inventory inventory, LegacyNukeBlockEntity container) {
        super(HbmMenus.LEGACY_NUKE.get(), containerId);
        this.container = container;
        this.definition = container.definition();
        this.machineSlots = definition.slotCount();
        this.playerStart = machineSlots;
        for (int slot = 0; slot < machineSlots; slot++) {
            addSlot(new ManualSlot(container, slot, definition.slotX(slot), definition.slotY(slot), definition.isCustom() ? 64 : 64));
        }
        // The legacy containers already store the absolute inventory origin.
        // Applying the old metadata offset a second time moves tall layouts.
        addPlayerInventory(inventory, definition.playerLeft(), definition.playerTop());
    }

    private LegacyNukeMenu(int containerId, Inventory inventory, Container missing) {
        super(HbmMenus.LEGACY_NUKE.get(), containerId);
        this.container = missing;
        this.definition = LegacyNukeDefinition.CUSTOM;
        this.machineSlots = 0;
        this.playerStart = 0;
        addPlayerInventory(inventory, 8, 84);
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || index < machineSlots) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem().copy();
        if (!moveItemStackTo(slot.getItem(), 0, machineSlots, false)) return ItemStack.EMPTY;
        if (slot.getItem().isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    public boolean isReady() { return container instanceof LegacyNukeBlockEntity nuke && nuke.isReady(); }
    public boolean slotHasExpectedItem(int slot) { return container instanceof LegacyNukeBlockEntity nuke && nuke.slotHasExpectedItem(slot); }
    public LegacyNukeDefinition definition() { return definition; }

    private static Container containerAt(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyNukeBlockEntity nuke ? nuke : new SimpleContainer(27);
    }

    private static final class ManualSlot extends Slot {
        private final int maxStackSize;
        private ManualSlot(Container container, int slot, int x, int y, int maxStackSize) {
            super(container, slot, x, y);
            this.maxStackSize = maxStackSize;
        }
        @Override public boolean mayPlace(ItemStack stack) { return true; }
        @Override public int getMaxStackSize() { return maxStackSize; }
    }
}
