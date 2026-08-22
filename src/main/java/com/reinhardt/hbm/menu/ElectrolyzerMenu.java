package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.ElectrolyzerBlockEntity;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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

public class ElectrolyzerMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = ElectrolyzerBlockEntity.SLOT_COUNT;

    private final Container container;
    private final ContainerData data;
    private final int mode;
    private final int playerInventoryStart;
    private final int playerInventoryEnd;
    private final int hotbarStart;
    private final int hotbarEnd;

    public ElectrolyzerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, readOpenData(playerInventory, buffer));
    }

    private ElectrolyzerMenu(int containerId, Inventory playerInventory, OpenData openData) {
        this(containerId, playerInventory, openData.container(), new SimpleContainerData(ElectrolyzerBlockEntity.DATA_COUNT), openData.mode());
    }

    public ElectrolyzerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        this(containerId, playerInventory, container, data, container instanceof ElectrolyzerBlockEntity electrolyzer ? electrolyzer.selectedGui() : ElectrolyzerBlockEntity.GUI_FLUID);
    }

    public ElectrolyzerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, int mode) {
        super(HbmMenus.ELECTROLYZER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, ElectrolyzerBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.mode = mode == ElectrolyzerBlockEntity.GUI_METAL ? ElectrolyzerBlockEntity.GUI_METAL : ElectrolyzerBlockEntity.GUI_FLUID;

        addSlot(new ValidatedSlot(container, ElectrolyzerBlockEntity.BATTERY_SLOT, 186, 109));
        addSlot(new ValidatedSlot(container, ElectrolyzerBlockEntity.UPGRADE_START, 186, 140));
        addSlot(new ValidatedSlot(container, ElectrolyzerBlockEntity.UPGRADE_START + 1, 186, 158));

        if (isFluidMode()) {
            addFluidSlots(container);
        } else {
            addMetalSlots(container);
        }

        this.playerInventoryStart = this.slots.size();
        addPlayerInventory(playerInventory, 8, 122);
        this.playerInventoryEnd = this.playerInventoryStart + 27;
        this.hotbarStart = this.playerInventoryEnd;
        this.hotbarEnd = this.hotbarStart + 9;
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return moved;

        ItemStack stack = slot.getItem();
        moved = stack.copy();

        if (index < this.playerInventoryStart) {
            if (!moveItemStackTo(stack, this.playerInventoryStart, this.hotbarEnd, true)) return ItemStack.EMPTY;
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, 1, 3, false)) return ItemStack.EMPTY;
        } else if (isFluidMode() && stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
        } else if (!isFluidMode() && !moveItemStackTo(stack, 3, 4, false)) {
            if (index < this.playerInventoryEnd) {
                if (!moveItemStackTo(stack, this.hotbarStart, this.hotbarEnd, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, this.playerInventoryStart, this.playerInventoryEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isFluidMode()) {
            if (index < this.playerInventoryEnd) {
                if (!moveItemStackTo(stack, this.hotbarStart, this.hotbarEnd, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, this.playerInventoryStart, this.playerInventoryEnd, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != ElectrolyzerBlockEntity.GUI_FLUID && id != ElectrolyzerBlockEntity.GUI_METAL) {
            return false;
        }
        if (this.container instanceof ElectrolyzerBlockEntity electrolyzer) {
            electrolyzer.setSelectedGui(id);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(electrolyzer, buffer -> {
                    buffer.writeBlockPos(electrolyzer.getBlockPos());
                    buffer.writeVarInt(electrolyzer.selectedGui());
                });
            }
            return true;
        }
        return false;
    }

    public int mode() {
        return this.mode;
    }

    public boolean isFluidMode() {
        return this.mode == ElectrolyzerBlockEntity.GUI_FLUID;
    }

    public int energy() { return this.data.get(0); }
    public int progressFluid() { return this.data.get(2); }
    public int processFluidTime() { return Math.max(1, this.data.get(3)); }
    public int progressOre() { return this.data.get(4); }
    public int processOreTime() { return Math.max(1, this.data.get(5)); }
    public int usageFluid() { return Math.max(1, this.data.get(6)); }
    public int usageOre() { return Math.max(1, this.data.get(7)); }
    public HbmFluidDefinition inputFluid() { return HbmFluids.byOldId(this.data.get(8)).orElse(HbmFluids.none()); }
    public int inputAmount() { return this.data.get(9); }
    public HbmFluidDefinition outputFluid1() { return HbmFluids.byOldId(this.data.get(10)).orElse(HbmFluids.none()); }
    public int outputAmount1() { return this.data.get(11); }
    public HbmFluidDefinition outputFluid2() { return HbmFluids.byOldId(this.data.get(12)).orElse(HbmFluids.none()); }
    public int outputAmount2() { return this.data.get(13); }
    public HbmFluidDefinition acidFluid() { return HbmFluids.byOldId(this.data.get(14)).orElse(HbmFluids.none()); }
    public int acidAmount() { return this.data.get(15); }
    public FoundryMaterial leftMaterial() { return FoundryMaterial.byId(this.data.get(16)).orElse(null); }
    public int leftAmount() { return this.data.get(17); }
    public FoundryMaterial rightMaterial() { return FoundryMaterial.byId(this.data.get(18)).orElse(null); }
    public int rightAmount() { return this.data.get(19); }

    public int energyScaled(int pixels) { return Math.min(pixels, (int) (Integer.toUnsignedLong(this.energy()) * pixels / ElectrolyzerBlockEntity.MAX_POWER)); }
    public int fluidProgressScaled(int pixels) { return Math.min(pixels, this.progressFluid() * pixels / this.processFluidTime()); }
    public int oreProgressScaled(int pixels) { return Math.min(pixels, this.progressOre() * pixels / this.processOreTime()); }
    public int tankScaled(int amount, int pixels) { return Math.min(pixels, amount * pixels / ElectrolyzerBlockEntity.TANK_CAPACITY); }

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

    private void addFluidSlots(Container container) {
        addSlot(new ValidatedSlot(container, ElectrolyzerBlockEntity.FLUID_IDENTIFIER_START, 6, 18));
        addSlot(new TakeOnlySlot(container, ElectrolyzerBlockEntity.FLUID_IDENTIFIER_START + 1, 6, 54));
        addSlot(new ValidatedSlot(container, 5, 24, 18));
        addSlot(new TakeOnlySlot(container, 6, 24, 54));
        addSlot(new ValidatedSlot(container, 7, 78, 18));
        addSlot(new TakeOnlySlot(container, 8, 78, 54));
        addSlot(new ValidatedSlot(container, 9, 134, 18));
        addSlot(new TakeOnlySlot(container, 10, 134, 54));
        addSlot(new TakeOnlySlot(container, 11, 154, 18));
        addSlot(new TakeOnlySlot(container, 12, 154, 36));
        addSlot(new TakeOnlySlot(container, 13, 154, 54));
    }

    private void addMetalSlots(Container container) {
        addSlot(new MetalInputSlot(container, ElectrolyzerBlockEntity.METAL_INPUT_SLOT, 10, 22));
        addSlot(new TakeOnlySlot(container, 15, 136, 18));
        addSlot(new TakeOnlySlot(container, 16, 154, 18));
        addSlot(new TakeOnlySlot(container, 17, 136, 36));
        addSlot(new TakeOnlySlot(container, 18, 154, 36));
        addSlot(new TakeOnlySlot(container, 19, 136, 54));
        addSlot(new TakeOnlySlot(container, 20, 154, 54));
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof ElectrolyzerBlockEntity electrolyzer ? electrolyzer : new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static OpenData readOpenData(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int mode = buffer.readableBytes() > 0 ? buffer.readVarInt() : ElectrolyzerBlockEntity.GUI_FLUID;
        return new OpenData(getContainer(playerInventory, pos), mode);
    }

    private record OpenData(Container container, int mode) {
    }

    private static class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return this.container.canPlaceItem(this.index, stack); }
    }

    private static final class MetalInputSlot extends Slot {
        private MetalInputSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return ElectrolyzerBlockEntity.isMetalInputCandidate(stack); }
    }

    private static final class TakeOnlySlot extends Slot {
        private TakeOnlySlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
