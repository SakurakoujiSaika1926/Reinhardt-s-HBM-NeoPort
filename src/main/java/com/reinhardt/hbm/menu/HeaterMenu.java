package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FluidIdentifierItem;
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

public class HeaterMenu extends AbstractContainerMenu {
    private static final int COMMON_MACHINE_SLOT_COUNT = 2;
    private static final int OILBURNER_KIND = HeaterBlockEntity.Kind.OILBURNER.ordinal();
    private static final int HEATEX_KIND = HeaterBlockEntity.Kind.HEATEX.ordinal();

    private final Container container;
    private final ContainerData data;
    private final int layoutKind;
    private final int machineSlotCount;
    private final int playerInventoryStart;
    private final int playerInventoryEnd;
    private final int hotbarStart;
    private final int hotbarEnd;
    private final BlockPos blockPos;

    public HeaterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, menuContext(playerInventory, buffer));
    }

    private HeaterMenu(int containerId, Inventory playerInventory, MenuContext context) {
        this(containerId, playerInventory, context.container(), new SimpleContainerData(HeaterBlockEntity.DATA_COUNT), context.layoutKind(), context.blockPos());
    }

    public HeaterMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        this(containerId, playerInventory, container, data, kindFromContainer(container, 0), blockPosFromContainer(container));
    }

    private HeaterMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, int layoutKind, BlockPos blockPos) {
        super(HbmMenus.HEATER.get(), containerId);
        this.container = container;
        this.data = data;
        this.layoutKind = kindFromContainer(container, layoutKind);
        this.blockPos = blockPos;
        this.machineSlotCount = isOilburnerLayout()
                ? HeaterBlockEntity.OILBURNER_SLOT_COUNT
                : isHeatExchangerLayout() ? HeaterBlockEntity.HEATEX_SLOT_COUNT : COMMON_MACHINE_SLOT_COUNT;
        this.playerInventoryStart = this.machineSlotCount;
        this.playerInventoryEnd = this.playerInventoryStart + 27;
        this.hotbarStart = this.playerInventoryEnd;
        this.hotbarEnd = this.hotbarStart + 9;
        checkContainerSize(container, this.machineSlotCount);
        checkContainerDataCount(data, HeaterBlockEntity.DATA_COUNT);

        if (isOilburnerLayout()) {
            this.addSlot(new HeaterSlot(container, 0, 26, 17));
            this.addSlot(new TakeOnlySlot(container, 1, 26, 53));
            this.addSlot(new HeaterSlot(container, 2, 44, 71));
            addPlayerInventory(playerInventory, 8, 121);
        } else if (isHeatExchangerLayout()) {
            this.addSlot(new HeaterSlot(container, HeaterBlockEntity.HEATEX_IDENTIFIER_SLOT, 80, 72));
            addPlayerInventory(playerInventory, 8, 122);
        } else {
            this.addSlot(new HeaterSlot(container, 0, 44, 27));
            this.addSlot(new HeaterSlot(container, 1, 62, 27));
            addPlayerInventory(playerInventory, 8, 86);
        }
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return moved;
        }

        ItemStack stack = slot.getItem();
        moved = stack.copy();
        if (index < this.machineSlotCount) {
            if (!moveItemStackTo(stack, this.playerInventoryStart, this.hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if ((isOilburnerLayout() || isHeatExchangerLayout()) && stack.getItem() instanceof FluidIdentifierItem) {
            int identifierSlot = isOilburnerLayout() ? HeaterBlockEntity.OILBURNER_IDENTIFIER_SLOT : HeaterBlockEntity.HEATEX_IDENTIFIER_SLOT;
            if (!moveItemStackTo(stack, identifierSlot, identifierSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.container.canPlaceItem(0, stack) || this.container.canPlaceItem(1, stack)) {
            if (!moveItemStackTo(stack, 0, this.machineSlotCount, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!isOilburnerLayout() && !isHeatExchangerLayout() && HeaterBlockEntity.fuelDuration(stack) > 0) {
            if (!moveItemStackTo(stack, 0, this.machineSlotCount, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < this.playerInventoryEnd) {
            if (!moveItemStackTo(stack, this.hotbarStart, this.hotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, this.playerInventoryStart, this.playerInventoryEnd, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.container instanceof HeaterBlockEntity heater) {
            heater.menuClosed();
        }
    }

    public int kind() {
        int syncedKind = this.data.get(0);
        return syncedKind == 0 && this.layoutKind != 0 ? this.layoutKind : syncedKind;
    }

    public boolean isOilburnerLayout() {
        return this.layoutKind == OILBURNER_KIND;
    }

    public boolean isHeatExchangerLayout() {
        return this.layoutKind == HEATEX_KIND;
    }

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public int heat() {
        return this.data.get(1);
    }

    public int setting() {
        return this.data.get(2);
    }

    public boolean enabled() {
        return this.data.get(3) != 0;
    }

    public int power() {
        return this.data.get(4);
    }

    public int lastInput() {
        return this.data.get(5);
    }

    public int burnTime() {
        return this.data.get(6);
    }

    public int maxBurnTime() {
        return Math.max(1, this.data.get(7));
    }

    public int burnHeat() {
        return this.data.get(8);
    }

    public HbmFluidDefinition oilFluid() {
        return HbmFluids.byOldId(this.data.get(10)).orElse(HbmFluids.none());
    }

    public int oilAmount() {
        return this.data.get(11);
    }

    public int oilCapacity() {
        return Math.max(1, this.data.get(12));
    }

    public HbmFluidDefinition heatExchangerInputFluid() {
        return HbmFluids.byOldId(this.data.get(13)).orElse(HbmFluids.none());
    }

    public int heatExchangerInputAmount() {
        return this.data.get(14);
    }

    public int heatExchangerInputCapacity() {
        return Math.max(1, this.data.get(15));
    }

    public HbmFluidDefinition heatExchangerOutputFluid() {
        return HbmFluids.byOldId(this.data.get(16)).orElse(HbmFluids.none());
    }

    public int heatExchangerOutputAmount() {
        return this.data.get(17);
    }

    public int heatExchangerOutputCapacity() {
        return Math.max(1, this.data.get(18));
    }

    public int heatExchangerAmountToCool() {
        return this.data.get(19);
    }

    public int heatExchangerTickDelay() {
        return this.data.get(20);
    }

    public int maxHeat() {
        return HeaterBlockEntity.maxHeatForKind(kind());
    }

    public int heatScaled(int pixels) {
        return Math.min(pixels, (int) ((long) this.heat() * pixels / maxHeat()));
    }

    public int burnScaled(int pixels) {
        return Math.min(pixels, this.burnTime() * pixels / maxBurnTime());
    }

    public int oilScaled(int pixels) {
        return Math.min(pixels, this.oilAmount() * pixels / this.oilCapacity());
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

    private static MenuContext menuContext(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int layoutKind = buffer.readableBytes() > 0 ? buffer.readVarInt() : 0;
        return new MenuContext(getContainer(playerInventory, pos), layoutKind, pos);
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof HeaterBlockEntity heater) {
            return heater;
        }
        return new SimpleContainer(HeaterBlockEntity.OILBURNER_SLOT_COUNT);
    }

    private static int kindFromContainer(Container container, int fallback) {
        if (container instanceof HeaterBlockEntity heater) {
            return heater.kind().ordinal();
        }
        return Math.max(0, Math.min(HeaterBlockEntity.Kind.values().length - 1, fallback));
    }

    private static BlockPos blockPosFromContainer(Container container) {
        if (container instanceof HeaterBlockEntity heater) {
            return heater.getBlockPos();
        }
        return BlockPos.ZERO;
    }

    private record MenuContext(Container container, int layoutKind, BlockPos blockPos) {
    }

    private static final class HeaterSlot extends Slot {
        private HeaterSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }

    private static final class TakeOnlySlot extends Slot {
        private TakeOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
