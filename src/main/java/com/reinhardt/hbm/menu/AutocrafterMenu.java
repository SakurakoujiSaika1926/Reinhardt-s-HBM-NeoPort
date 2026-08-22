package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 1.7.10 ContainerAutocrafter, including its server-side template controls. */
public final class AutocrafterMenu extends AbstractContainerMenu {
    private static final int SLOT_COUNT = 21;
    private static final int PLAYER_START = SLOT_COUNT;
    private static final int PLAYER_END = PLAYER_START + 36;
    private final Container container;
    private final ContainerData data;
    private final LegacyMachineBlockEntity machine;
    private final BlockPos blockPos;

    public AutocrafterMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, findMachine(inventory, buffer.readBlockPos()), new SimpleContainerData(LegacyMachineBlockEntity.DATA_COUNT));
    }

    public AutocrafterMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine) {
        this(containerId, inventory, machine, machine.menuData());
    }

    private AutocrafterMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine, ContainerData data) {
        super(HbmMenus.AUTOCRAFTER.get(), containerId);
        this.machine = machine;
        this.container = machine == null ? new SimpleContainer(SLOT_COUNT) : machine;
        this.data = data;
        this.blockPos = machine == null ? BlockPos.ZERO : machine.getBlockPos().immutable();
        checkContainerSize(this.container, SLOT_COUNT);
        checkContainerDataCount(data, LegacyMachineBlockEntity.DATA_COUNT);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(new TemplateSlot(this.container, column + row * 3, 44 + column * 18, 22 + row * 18));
            }
        }
        this.addSlot(new PreviewSlot(this.container, 9, 116, 40));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(new ValidatedSlot(this.container, 10 + column + row * 3, 44 + column * 18, 86 + row * 18));
            }
        }
        this.addSlot(new OutputSlot(this.container, 19, 116, 104));
        this.addSlot(new ValidatedSlot(this.container, 20, 17, 99));
        addPlayerInventory(inventory, 8, 158);
        addDataSlots(data);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (this.machine != null && slotId >= 0 && slotId <= 8) {
            if (button == 1 && clickType == ClickType.PICKUP && this.slots.get(slotId).hasItem()) {
                this.machine.cycleAutocrafterMode(slotId);
            } else {
                this.machine.setAutocrafterTemplate(slotId, this.getCarried());
            }
            this.broadcastChanges();
            return;
        }
        if (slotId == 9) {
            if (this.machine != null && button == 1 && clickType == ClickType.PICKUP && this.slots.get(slotId).hasItem()) {
                this.machine.nextAutocrafterRecipe();
                this.broadcastChanges();
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size() || !this.slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index >= 10 && index <= 20) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (index >= PLAYER_START && BatteryPackItem.isBattery(stack)) {
            if (!moveItemStackTo(stack, 20, 21, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public long power() { return Integer.toUnsignedLong(this.data.get(0)); }
    public long maxPower() { return 10_000L; }
    public String mode(int slot) { return this.machine == null ? "" : this.machine.autocrafterMode(slot); }
    public int recipeIndex() { return this.machine == null ? this.data.get(9) : this.machine.autocrafterRecipeIndex(); }
    public int recipeCount() { return this.machine == null ? this.data.get(10) : this.machine.autocrafterRecipeCount(); }
    public BlockPos blockPos() { return this.blockPos; }

    private static LegacyMachineBlockEntity findMachine(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyMachineBlockEntity machine ? machine : null;
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    private static class TemplateSlot extends Slot {
        private TemplateSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public int getMaxStackSize() { return 1; }
    }

    private static final class PreviewSlot extends TemplateSlot {
        private PreviewSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }
    }

    private static final class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
