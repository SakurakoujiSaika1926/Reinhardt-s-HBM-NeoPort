package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.recipe.anvil.AnvilConstructionRecipe;
import com.reinhardt.hbm.recipe.anvil.AnvilSmithingRecipe;
import com.reinhardt.hbm.recipe.anvil.HbmAnvilRecipes;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class HbmAnvilMenu extends AbstractContainerMenu {
    private static final int LEFT_INPUT_SLOT = 0;
    private static final int RIGHT_INPUT_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final int ANVIL_SLOT_COUNT = 3;
    private static final int PLAYER_INVENTORY_START = ANVIL_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final SimpleContainer input = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            HbmAnvilMenu.this.updateSmithing();
        }
    };
    private final SimpleContainer output = new SimpleContainer(1);
    private final int tier;

    public HbmAnvilMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readInt());
    }

    public HbmAnvilMenu(int containerId, Inventory playerInventory, int tier) {
        super(HbmMenus.ANVIL.get(), containerId);
        this.tier = tier;

        this.addSlot(new InputSlot(this.input, LEFT_INPUT_SLOT, 17, 27));
        this.addSlot(new InputSlot(this.input, RIGHT_INPUT_SLOT, 53, 27));
        this.addSlot(new OutputSlot(this.output, 0, 89, 27));
        addPlayerInventory(playerInventory, 8, 140);
    }

    public int tier() {
        return this.tier;
    }

    public List<AnvilConstructionRecipe> constructionRecipes() {
        return HbmAnvilRecipes.constructionForTier(this.tier);
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

        if (index == OUTPUT_SLOT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        } else if (index < ANVIL_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, LEFT_INPUT_SLOT, RIGHT_INPUT_SLOT + 1, false)) {
            if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
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
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide) {
            return;
        }

        for (int i = 0; i < this.input.getContainerSize(); i++) {
            ItemStack stack = this.input.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0) {
            return false;
        }

        List<AnvilConstructionRecipe> recipes = constructionRecipes();
        int recipeIndex = id / 2;
        if (recipeIndex >= recipes.size()) {
            return false;
        }

        int crafts = (id & 1) == 1 ? 64 : 1;
        AnvilConstructionRecipe recipe = recipes.get(recipeIndex);
        boolean crafted = false;
        for (int i = 0; i < crafts; i++) {
            if (!recipe.craft(player)) {
                break;
            }
            crafted = true;
        }
        return crafted;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == this.input) {
            updateSmithing();
        }
    }

    private void updateSmithing() {
        ItemStack left = this.input.getItem(LEFT_INPUT_SLOT);
        ItemStack right = this.input.getItem(RIGHT_INPUT_SLOT);
        AnvilSmithingRecipe.Match match = HbmAnvilRecipes.findSmithing(left, right, this.tier);
        if (match == null) {
            this.output.setItem(0, ItemStack.EMPTY);
            return;
        }
        this.output.setItem(0, match.recipe().getOutput(left, right));
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

    private final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            HbmAdvancements.awardForCraftedStack(player, stack);
            ItemStack left = HbmAnvilMenu.this.input.getItem(LEFT_INPUT_SLOT);
            ItemStack right = HbmAnvilMenu.this.input.getItem(RIGHT_INPUT_SLOT);
            AnvilSmithingRecipe.Match match = HbmAnvilRecipes.findSmithing(left, right, HbmAnvilMenu.this.tier);
            if (match != null) {
                HbmAnvilMenu.this.input.removeItem(LEFT_INPUT_SLOT, match.recipe().amountConsumed(0, match.mirrored()));
                HbmAnvilMenu.this.input.removeItem(RIGHT_INPUT_SLOT, match.recipe().amountConsumed(1, match.mirrored()));
                HbmAnvilMenu.this.updateSmithing();
            }
            super.onTake(player, stack);
        }
    }

    private static final class InputSlot extends Slot {
        private InputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }
}
