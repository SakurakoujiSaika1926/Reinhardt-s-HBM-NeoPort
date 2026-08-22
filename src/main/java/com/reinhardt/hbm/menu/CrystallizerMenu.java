package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.CrystallizerBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.InfiniteFluidContainerItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.recipe.CrystallizerRecipe;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmMenus;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidUtil;

public class CrystallizerMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = CrystallizerBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public CrystallizerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(CrystallizerBlockEntity.DATA_COUNT));
    }

    public CrystallizerMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.CRYSTALLIZER.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, CrystallizerBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        this.addSlot(new ValidatedSlot(container, CrystallizerBlockEntity.INPUT_SLOT, 62, 45));
        this.addSlot(new ValidatedSlot(container, CrystallizerBlockEntity.BATTERY_SLOT, 152, 72));
        this.addSlot(new OutputSlot(container, CrystallizerBlockEntity.OUTPUT_SLOT, 113, 45));
        this.addSlot(new ValidatedSlot(container, CrystallizerBlockEntity.FLUID_INPUT_SLOT, 17, 18));
        this.addSlot(new OutputSlot(container, CrystallizerBlockEntity.FLUID_OUTPUT_SLOT, 17, 54));
        this.addSlot(new ValidatedSlot(container, CrystallizerBlockEntity.UPGRADE_START, 80, 18));
        this.addSlot(new ValidatedSlot(container, CrystallizerBlockEntity.UPGRADE_START + 1, 98, 18));
        this.addSlot(new ValidatedSlot(container, CrystallizerBlockEntity.FLUID_IDENTIFIER_SLOT, 35, 72));

        addPlayerInventory(playerInventory, 8, 122);
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

        if (index == CrystallizerBlockEntity.OUTPUT_SLOT || index == CrystallizerBlockEntity.FLUID_OUTPUT_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (BatteryPackItem.isBattery(stack)) {
            if (!moveItemStackTo(stack, CrystallizerBlockEntity.BATTERY_SLOT, CrystallizerBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, CrystallizerBlockEntity.UPGRADE_START, CrystallizerBlockEntity.UPGRADE_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, CrystallizerBlockEntity.FLUID_IDENTIFIER_SLOT, CrystallizerBlockEntity.FLUID_IDENTIFIER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isFilledFluidContainer(stack)) {
            if (!moveItemStackTo(stack, CrystallizerBlockEntity.FLUID_INPUT_SLOT, CrystallizerBlockEntity.FLUID_INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (hasRecipe(stack)) {
            if (!moveItemStackTo(stack, CrystallizerBlockEntity.INPUT_SLOT, CrystallizerBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
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

    public int energy() {
        return this.data.get(0);
    }

    public int lastInput() {
        return this.data.get(1);
    }

    public int progress() {
        return this.data.get(2);
    }

    public int workTime() {
        return Math.max(1, this.data.get(3));
    }

    public int currentDemand() {
        return Math.max(1, this.data.get(4));
    }

    public int completedCycles() {
        return this.data.get(5);
    }

    public boolean isWorking() {
        return this.data.get(6) != 0;
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition tankFluid() {
        return HbmFluids.byOldId(this.data.get(7)).orElse(HbmFluids.none());
    }

    public int tankAmount() {
        return this.data.get(8);
    }

    public int tankCapacity() {
        return CrystallizerBlockEntity.TANK_CAPACITY;
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, (int) (this.energy() * (long) pixels / CrystallizerBlockEntity.MAX_POWER));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.workTime());
    }

    public int tankScaled(int pixels) {
        return Math.min(pixels, this.tankAmount() * pixels / this.tankCapacity());
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

    private boolean hasRecipe(ItemStack stack) {
        return !stack.isEmpty() && this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CRYSTALLIZER.get())
                .stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> recipe.ingredient().test(stack));
    }

    private static boolean isFilledFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof InfiniteFluidContainerItem
                || (stack.getItem() instanceof HbmFluidContainerItem item && item.isFilledContainer())
                || FluidUtil.getFluidContained(stack).isPresent();
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof CrystallizerBlockEntity crystallizer) {
            return crystallizer;
        }
        return new SimpleContainer(MACHINE_SLOT_COUNT);
    }

    private static class ValidatedSlot extends Slot {
        private ValidatedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.index, stack);
        }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
