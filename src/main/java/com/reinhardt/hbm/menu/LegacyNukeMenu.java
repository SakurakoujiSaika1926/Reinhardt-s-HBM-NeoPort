package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.block.LegacyNukeBlock;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class LegacyNukeMenu extends AbstractContainerMenu {
    private final Container container;
    private final LegacyNukeDefinition definition;
    private final int machineSlots;
    private final int playerStart;

    public LegacyNukeMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, contextAt(inventory, buffer));
    }

    public LegacyNukeMenu(int containerId, Inventory inventory, LegacyNukeBlockEntity container) {
        this(containerId, inventory, container, container.definition());
    }

    private LegacyNukeMenu(int containerId, Inventory inventory, MenuContext context) {
        this(containerId, inventory, context.container(), context.definition());
    }

    private LegacyNukeMenu(int containerId, Inventory inventory, Container container, LegacyNukeDefinition definition) {
        super(HbmMenus.LEGACY_NUKE.get(), containerId);
        this.container = container;
        this.definition = definition;
        this.machineSlots = this.definition.slotCount();
        this.playerStart = machineSlots;
        for (int slot = 0; slot < machineSlots; slot++) {
            addSlot(new ManualSlot(container, slot, this.definition.slotX(slot), this.definition.slotY(slot), this.definition.isCustom() ? 64 : 64));
        }
        // The legacy containers already store the absolute inventory origin.
        // Applying the old metadata offset a second time moves tall layouts.
        addPlayerInventory(inventory, this.definition.playerLeft(), this.definition.playerTop());
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
    public boolean isFilled() { return container instanceof LegacyNukeBlockEntity nuke && nuke.isFilled(); }
    public boolean slotHasExpectedItem(int slot) { return container instanceof LegacyNukeBlockEntity nuke && nuke.slotHasExpectedItem(slot); }
    public LegacyNukeDefinition definition() { return definition; }

    private static MenuContext contextAt(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        LegacyNukeDefinition networkDefinition = readDefinition(buffer);
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        if (entity instanceof LegacyNukeBlockEntity nuke) {
            return new MenuContext(nuke, nuke.definition());
        }
        BlockState state = inventory.player.level().getBlockState(pos);
        if (state.getBlock() instanceof LegacyNukeBlock block) {
            LegacyNukeDefinition blockDefinition = block.definition();
            return new MenuContext(new SimpleContainer(blockDefinition.slotCount()), blockDefinition);
        }
        return new MenuContext(new SimpleContainer(networkDefinition.slotCount()), networkDefinition);
    }

    private static LegacyNukeDefinition readDefinition(RegistryFriendlyByteBuf buffer) {
        if (buffer.readableBytes() <= 0) {
            return LegacyNukeDefinition.CUSTOM;
        }
        int ordinal = buffer.readVarInt();
        LegacyNukeDefinition[] values = LegacyNukeDefinition.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : LegacyNukeDefinition.CUSTOM;
    }

    private record MenuContext(Container container, LegacyNukeDefinition definition) {}

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
