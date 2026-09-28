package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.block.CraneMachineBlock;
import com.reinhardt.hbm.blockentity.CraneMachineBlockEntity;
import com.reinhardt.hbm.registry.HbmItems;
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

/** One protocol with the original, machine-specific crane slot layouts. */
public final class CraneMachineMenu extends AbstractContainerMenu {
    private static final int STORAGE_SIZE = CraneMachineBlockEntity.STORAGE_SIZE;

    private final Container container;
    private final CraneMachineBlockEntity crane;
    private final CraneMachineBlock.Kind kind;
    private final ContainerData data;
    private int machineSlotCount;

    public CraneMachineMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer.readBlockPos()));
    }

    public CraneMachineMenu(int id, Inventory inventory, CraneMachineBlockEntity crane) {
        this(id, inventory, new Resolved(crane, crane, crane.kind(), crane.menuData()));
    }

    private CraneMachineMenu(int id, Inventory inventory, Resolved resolved) {
        super(HbmMenus.CRANE_MACHINE.get(), id);
        this.container = resolved.container();
        this.crane = resolved.crane();
        this.kind = resolved.kind();
        this.data = resolved.data();
        checkContainerSize(container, STORAGE_SIZE);
        checkContainerDataCount(data, 12);
        addMachineSlots();
        this.machineSlotCount = slots.size();
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    public CraneMachineBlock.Kind kind() {
        return kind;
    }

    public int primaryMode() {
        return data.get(2);
    }

    public boolean maxEject() {
        return data.get(3) != 0;
    }

    public int routerMode(int side) {
        return side >= 0 && side < 6 ? data.get(6 + side) : 0;
    }

    public DirectionPair directions() {
        return new DirectionPair(data.get(0), data.get(1));
    }

    public String filterMode(int slot) {
        return crane == null ? null : crane.filterMode(slot);
    }

    public boolean isFilterSlot(Slot slot) {
        return slot instanceof FilterSlot;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof FilterSlot filter) {
            if (crane != null) {
                if (clickType == ClickType.PICKUP && button == 1) {
                    crane.cycleFilterMode(filter.getContainerSlot());
                } else if (clickType == ClickType.PICKUP && button == 0) {
                    crane.setFilter(filter.getContainerSlot(), getCarried());
                } else if (clickType == ClickType.SWAP && button >= 0 && button < 9) {
                    crane.setFilter(filter.getContainerSlot(), player.getInventory().getItem(button));
                }
            }
            // Every filter interaction is ghost-only. In particular, number-key
            // swap, throw, drag and double-click must not move a real stack.
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (crane == null) return false;
        crane.handleMenuButton(id);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || slots.get(index) instanceof FilterSlot) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        if (index < machineSlotCount) {
            if (!moveItemStackTo(stack, machineSlotCount, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!movePlayerStackToMachine(stack)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    private boolean movePlayerStackToMachine(ItemStack stack) {
        int original = stack.getCount();
        // Filter slots are ghost patterns. They must never consume a shift-clicked
        // stack; this loop only addresses real storage/upgrade slots whose own
        // mayPlace rules remain authoritative.
        for (int menuSlot = 0; menuSlot < machineSlotCount && !stack.isEmpty(); menuSlot++) {
            Slot target = slots.get(menuSlot);
            if (target instanceof FilterSlot || !target.mayPlace(stack)) continue;
            moveItemStackTo(stack, menuSlot, menuSlot + 1, false);
        }
        return stack.getCount() != original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    private void addMachineSlots() {
        switch (kind) {
            case BOXER, INSERTER -> addSevenByThree(0);
            case EXTRACTOR -> addExtractorSlots();
            case GRABBER -> addGrabberSlots();
            case ROUTER -> addRouterSlots();
            case UNBOXER -> addUnboxerSlots();
            case PARTITIONER, SPLITTER -> { }
        }
    }

    private void addSevenByThree(int start) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 7; column++) {
                addSlot(new Slot(container, start + column + row * 7, 8 + column * 18, 17 + row * 18));
            }
        }
    }

    private void addExtractorSlots() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(container, 9 + column + row * 3, 8 + column * 18, 17 + row * 18));
                addSlot(new FilterSlot(container, column + row * 3, 71 + column * 18, 17 + row * 18));
            }
        }
        addSlot(new UpgradeSlot(container, 18, 152, 23, true));
        addSlot(new UpgradeSlot(container, 19, 152, 47, false));
    }

    private void addGrabberSlots() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new FilterSlot(container, column + row * 3, 40 + column * 18, 17 + row * 18));
            }
        }
        addSlot(new UpgradeSlot(container, 9, 121, 23, true));
        addSlot(new UpgradeSlot(container, 10, 121, 47, false));
    }

    private void addRouterSlots() {
        for (int bank = 0; bank < 2; bank++) {
            for (int row = 0; row < 3; row++) {
                int side = bank * 3 + row;
                for (int column = 0; column < 5; column++) {
                    int slot = side * 5 + column;
                    addSlot(new FilterSlot(container, slot, 34 + column * 18 + bank * 98, 17 + row * 26));
                }
            }
        }
    }

    private void addUnboxerSlots() {
        addSevenByThree(0);
        addSlot(new UpgradeSlot(container, 21, 152, 23, true));
        addSlot(new UpgradeSlot(container, 22, 152, 47, false));
    }

    private void addPlayerInventory(Inventory inventory) {
        int x = switch (kind) {
            case ROUTER -> 47;
            case EXTRACTOR -> 26;
            default -> 8;
        };
        int y = kind == CraneMachineBlock.Kind.ROUTER ? 119 : 103;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, x + column * 18, y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, x + column * 18, y + 58));
        }
    }

    private static Resolved resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        if (entity instanceof CraneMachineBlockEntity crane) {
            return new Resolved(crane, crane, crane.kind(), crane.menuData());
        }
        CraneMachineBlock.Kind kind = inventory.player.level().getBlockState(pos).getBlock() instanceof CraneMachineBlock block
                ? block.kind()
                : CraneMachineBlock.Kind.BOXER;
        return new Resolved(new SimpleContainer(STORAGE_SIZE), null, kind, new SimpleContainerData(12));
    }

    private record Resolved(Container container, CraneMachineBlockEntity crane, CraneMachineBlock.Kind kind,
                            ContainerData data) {
    }

    public record DirectionPair(int inputOrdinal, int outputOrdinal) {
    }

    private static final class FilterSlot extends Slot {
        private FilterSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    private static final class UpgradeSlot extends Slot {
        private final boolean stackUpgrade;

        private UpgradeSlot(Container container, int index, int x, int y, boolean stackUpgrade) {
            super(container, index, x, y);
            this.stackUpgrade = stackUpgrade;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(stackUpgrade ? HbmItems.UPGRADE_STACK.get() : HbmItems.UPGRADE_EJECTOR.get());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
