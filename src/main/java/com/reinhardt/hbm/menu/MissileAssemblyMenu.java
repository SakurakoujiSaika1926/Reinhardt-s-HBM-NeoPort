package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.item.MissilePartItem;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Exact six-slot placement layout from ContainerMachineMissileAssembly. */
public final class MissileAssemblyMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 6;
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public MissileAssemblyMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, getContainer(inventory, buffer.readBlockPos()), new SimpleContainerData(LegacyMachineBlockEntity.DATA_COUNT));
    }

    public MissileAssemblyMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine) {
        this(containerId, inventory, machine, machine.menuData());
    }

    private MissileAssemblyMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(HbmMenus.MISSILE_ASSEMBLY.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, LegacyMachineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof BlockEntity entity ? entity.getBlockPos().immutable() : BlockPos.ZERO;

        addSlot(new ValidatedSlot(container, 0, 8, 36));
        addSlot(new ValidatedSlot(container, 1, 26, 36));
        addSlot(new ValidatedSlot(container, 2, 44, 36));
        addSlot(new ValidatedSlot(container, 3, 62, 36));
        addSlot(new ValidatedSlot(container, 4, 80, 36));
        addSlot(new TakeOnlySlot(container, 5, 152, 36));
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot source = this.slots.get(index);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = source.getItem();
        ItemStack moved = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            MissilePartItem.Definition definition = MissilePartItem.definition(stack);
            int slot = definition == null ? -1 : switch (definition.type()) {
                case CHIP -> 0;
                case WARHEAD -> 1;
                case FUSELAGE -> 2;
                case FINS -> 3;
                case THRUSTER -> 4;
            };
            if (slot < 0 || !moveItemStackTo(stack, slot, slot + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public BlockPos blockPos() { return this.blockPos; }
    public int stateMask() { return this.data.get(13); }
    public boolean canBuild() { return this.data.get(14) != 0; }
    public ItemStack part(int slot) { return slot >= 0 && slot < 5 ? this.container.getItem(slot) : ItemStack.EMPTY; }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 140 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 198));
        }
    }

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyMachineBlockEntity machine && machine.machineId().equals("machine_missile_assembly")
                ? machine : new SimpleContainer(MACHINE_SLOTS);
    }

    private static class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }

    private static final class TakeOnlySlot extends ValidatedSlot {
        private TakeOnlySlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
