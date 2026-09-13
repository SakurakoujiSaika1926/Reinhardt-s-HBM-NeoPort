package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.CyclotronBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.recipe.CyclotronRecipe;
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

public class CyclotronMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = CyclotronBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container container;
    private final ContainerData data;

    public CyclotronMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(CyclotronBlockEntity.DATA_COUNT));
    }

    public CyclotronMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.CYCLOTRON.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, CyclotronBlockEntity.DATA_COUNT);
        this.playerInventory = playerInventory;
        this.container = container;
        this.data = data;

        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.PARTICLE_A_SLOT, 11, 18));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.PARTICLE_B_SLOT, 11, 36));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.PARTICLE_C_SLOT, 11, 54));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.INPUT_A_SLOT, 101, 18));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.INPUT_B_SLOT, 101, 36));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.INPUT_C_SLOT, 101, 54));
        addSlot(new OutputSlot(container, CyclotronBlockEntity.OUTPUT_A_SLOT, 131, 18));
        addSlot(new OutputSlot(container, CyclotronBlockEntity.OUTPUT_B_SLOT, 131, 36));
        addSlot(new OutputSlot(container, CyclotronBlockEntity.OUTPUT_C_SLOT, 131, 54));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.BATTERY_SLOT, 168, 83));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.UPGRADE_A_SLOT, 60, 81));
        addSlot(new ValidatedSlot(container, CyclotronBlockEntity.UPGRADE_B_SLOT, 78, 81));

        addPlayerInventory(playerInventory, 15, 133);
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

        if (index >= CyclotronBlockEntity.OUTPUT_A_SLOT && index <= CyclotronBlockEntity.OUTPUT_C_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        } else if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (BatteryPackItem.isBattery(stack)) {
            if (!moveItemStackTo(stack, CyclotronBlockEntity.BATTERY_SLOT, CyclotronBlockEntity.BATTERY_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (MachineUpgradeItem.isMachineUpgrade(stack)) {
            if (!moveItemStackTo(stack, CyclotronBlockEntity.UPGRADE_A_SLOT, CyclotronBlockEntity.UPGRADE_B_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (isParticle(stack)) {
            if (!moveItemStackTo(stack, CyclotronBlockEntity.PARTICLE_A_SLOT, CyclotronBlockEntity.PARTICLE_C_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (hasTargetRecipe(stack)) {
            if (!moveItemStackTo(stack, CyclotronBlockEntity.INPUT_A_SLOT, CyclotronBlockEntity.INPUT_C_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) return ItemStack.EMPTY;
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

    public int power() { return data.get(0); }
    public int progress() { return data.get(1); }
    public int plugs() { return data.get(2); }
    public int waterAmount() { return data.get(3); }
    public int spentSteamAmount() { return data.get(4); }
    public int antimatterAmount() { return data.get(5); }
    public int waterCapacity() { return Math.max(1, data.get(6)); }
    public int spentSteamCapacity() { return Math.max(1, data.get(7)); }
    public int antimatterCapacity() { return Math.max(1, data.get(8)); }
    public int consumption() { return data.get(9); }
    public boolean active() { return data.get(10) != 0; }

    public int powerScaled(int pixels) {
        return Math.min(pixels, (int) (power() * (long) pixels / CyclotronBlockEntity.MAX_POWER));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, progress() * pixels / CyclotronBlockEntity.DURATION);
    }

    public int tankScaled(int amount, int capacity, int pixels) {
        return Math.min(pixels, amount * pixels / Math.max(1, capacity));
    }

    private boolean isParticle(ItemStack stack) {
        return !stack.isEmpty() && this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CYCLOTRON.get())
                .stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> recipe.particle().test(stack));
    }

    private boolean hasTargetRecipe(ItemStack stack) {
        return !stack.isEmpty() && this.playerInventory.player.level().getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CYCLOTRON.get())
                .stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> recipe.input().test(stack));
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

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof CyclotronBlockEntity cyclotron ? cyclotron : new SimpleContainer(MACHINE_SLOT_COUNT);
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
}
