package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.registry.HbmFluids;
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

/** Exact five-slot layout of ContainerMachineTurbofan from HBM 1.7.10. */
public final class TurbofanMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 5;
    private static final int PLAYER_START = MACHINE_SLOTS;
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public TurbofanMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, getContainer(inventory, buffer.readBlockPos()),
                new SimpleContainerData(LegacyMachineBlockEntity.DATA_COUNT));
    }

    public TurbofanMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine) {
        this(containerId, inventory, machine, machine.menuData());
    }

    private TurbofanMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(HbmMenus.TURBOFAN.get(), containerId);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, LegacyMachineBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof BlockEntity entity ? entity.getBlockPos().immutable() : BlockPos.ZERO;

        addSlot(new ValidatedSlot(container, 0, 17, 17));
        addSlot(new TakeOnlySlot(container, 1, 17, 53));
        addSlot(new ValidatedSlot(container, 2, 98, 71));
        addSlot(new ValidatedSlot(container, 3, 143, 71));
        addSlot(new ValidatedSlot(container, 4, 44, 71));
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
            if (!moveItemStackTo(stack, PLAYER_START, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean placed;
            if (BatteryPackItem.isBattery(stack)) {
                placed = moveItemStackTo(stack, 3, 4, false);
            } else if (stack.getItem() instanceof FluidIdentifierItem) {
                placed = moveItemStackTo(stack, 4, 5, false);
            } else if (MachineUpgradeItem.isMachineUpgrade(stack) || isFlamePony(stack)) {
                placed = moveItemStackTo(stack, 2, 3, false);
            } else {
                placed = moveItemStackTo(stack, 0, 1, false);
            }
            if (!placed) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    public int energy() { return this.data.get(0); }
    public int energyCapacity() { return 1_000_000; }
    public int fuelId() { return this.data.get(5); }
    public int fuelAmount() { return this.data.get(6); }
    public int bloodId() { return this.data.get(7); }
    public int bloodAmount() { return this.data.get(8); }
    public int afterburner() { return this.data.get(11); }
    public boolean showBlood() { return this.data.get(12) != 0; }
    public com.reinhardt.hbm.fluid.HbmFluidDefinition fuel() { return HbmFluids.byOldId(fuelId()).orElse(HbmFluids.none()); }
    public com.reinhardt.hbm.fluid.HbmFluidDefinition blood() { return HbmFluids.byOldId(bloodId()).orElse(HbmFluids.none()); }
    public BlockPos blockPos() { return this.blockPos; }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 121 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 179));
        }
    }

    private static Container getContainer(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyMachineBlockEntity machine && machine.machineId().equals("machine_turbofan")
                ? machine : new SimpleContainer(MACHINE_SLOTS);
    }

    private static boolean isFlamePony(ItemStack stack) {
        return !stack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("flame_pony");
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
