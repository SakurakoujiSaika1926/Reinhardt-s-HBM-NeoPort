package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.blockentity.SolderingStationBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.recipe.SolderingStationRecipe;
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

import java.util.List;

public class SolderingStationMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = SolderingStationBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public SolderingStationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(SolderingStationBlockEntity.DATA_COUNT));
    }

    public SolderingStationMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.SOLDERING_STATION.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, SolderingStationBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        addRecipeInputSlots(container);
        this.addSlot(new OutputSlot(container, SolderingStationBlockEntity.OUTPUT_SLOT, 107, 27));
        this.addSlot(new BatterySlot(container, SolderingStationBlockEntity.BATTERY_SLOT, 152, 72));
        this.addSlot(new FluidIdentifierSlot(container, SolderingStationBlockEntity.FLUID_IDENTIFIER_SLOT, 17, 63));
        this.addSlot(new UpgradeSlot(container, SolderingStationBlockEntity.UPGRADE_START, 89, 63));
        this.addSlot(new UpgradeSlot(container, SolderingStationBlockEntity.UPGRADE_START + 1, 107, 63));
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

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            if (index == SolderingStationBlockEntity.OUTPUT_SLOT) {
                slot.onTake(player, stack);
            }
        } else if (ShredderBlockEntity.isBattery(stack)) {
            if (!moveItemStackTo(stack, SolderingStationBlockEntity.BATTERY_SLOT, SolderingStationBlockEntity.BATTERY_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, SolderingStationBlockEntity.FLUID_IDENTIFIER_SLOT, SolderingStationBlockEntity.FLUID_IDENTIFIER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, SolderingStationBlockEntity.UPGRADE_START, SolderingStationBlockEntity.UPGRADE_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isSolderingIngredient(stack, SolderingStationBlockEntity.Group.TOPPING)) {
            if (!moveItemStackTo(stack, SolderingStationBlockEntity.TOPPING_START, SolderingStationBlockEntity.TOPPING_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isSolderingIngredient(stack, SolderingStationBlockEntity.Group.PCB)) {
            if (!moveItemStackTo(stack, SolderingStationBlockEntity.PCB_START, SolderingStationBlockEntity.PCB_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isSolderingIngredient(stack, SolderingStationBlockEntity.Group.SOLDER)) {
            if (!moveItemStackTo(stack, SolderingStationBlockEntity.SOLDER_SLOT, SolderingStationBlockEntity.SOLDER_SLOT + 1, false)) {
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

    public int processTime() {
        return Math.max(1, this.data.get(3));
    }

    public int consumption() {
        return Math.max(1, this.data.get(4));
    }

    public int maxPower() {
        return Math.max((int) SolderingStationBlockEntity.BASE_MAX_POWER, this.data.get(5));
    }

    public boolean hasRecipe() {
        return this.data.get(6) != 0;
    }

    public int completedCycles() {
        return this.data.get(7);
    }

    public HbmFluidDefinition fluid() {
        return HbmFluids.byOldId(this.data.get(8)).orElse(HbmFluids.none());
    }

    public int tankAmount() {
        return Math.max(0, this.data.get(9));
    }

    public int tankCapacity() {
        return Math.max(1, this.data.get(10));
    }

    public int energyScaled(int pixels) {
        return Math.min(pixels, this.energy() * pixels / this.maxPower());
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.processTime());
    }

    public int fluidScaled(int pixels) {
        return Math.min(pixels, this.tankAmount() * pixels / this.tankCapacity());
    }

    public boolean isWorking() {
        return this.progress() > 0 && this.energy() >= this.consumption();
    }

    private void addRecipeInputSlots(Container container) {
        int slot = SolderingStationBlockEntity.TOPPING_START;
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(new ValidatedSlot(container, slot++, 17 + column * 18, 18 + row * 18));
            }
        }
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

    private boolean isSolderingIngredient(ItemStack stack, SolderingStationBlockEntity.Group group) {
        if (stack.isEmpty()) {
            return false;
        }
        if (this.container instanceof SolderingStationBlockEntity solderingStation) {
            return solderingStation.canAcceptInput(stack, group);
        }
        return this.playerInventory.player.level().getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SOLDERING_STATION.get()).stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> switch (group) {
                    case TOPPING -> containsIngredient(recipe.toppings(), stack);
                    case PCB -> containsIngredient(recipe.pcb(), stack);
                    case SOLDER -> containsIngredient(recipe.solder(), stack);
                });
    }

    private static boolean containsIngredient(List<SolderingStationRecipe.CountedIngredient> ingredients, ItemStack stack) {
        for (SolderingStationRecipe.CountedIngredient ingredient : ingredients) {
            if (ingredient.ingredient().test(stack)) {
                return true;
            }
        }
        return false;
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof SolderingStationBlockEntity solderingStation) {
            return solderingStation;
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

    private static final class OutputSlot extends LegacyAchievementOutputSlot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    private static final class BatterySlot extends ValidatedSlot {
        private BatterySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }

    private static final class FluidIdentifierSlot extends ValidatedSlot {
        private FluidIdentifierSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }

    private static final class UpgradeSlot extends ValidatedSlot {
        private UpgradeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }
}
