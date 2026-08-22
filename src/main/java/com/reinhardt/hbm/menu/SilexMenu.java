package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.SilexBlockEntity;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.InfiniteFluidContainerItem;
import com.reinhardt.hbm.recipe.SilexRecipe;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmMenus;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.Wavelength;
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

public class SilexMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOT_COUNT = SilexBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public SilexMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, getContainer(playerInventory, buffer.readBlockPos()), new SimpleContainerData(SilexBlockEntity.DATA_COUNT));
    }

    public SilexMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(HbmMenus.SILEX.get(), containerId);
        checkContainerSize(container, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, SilexBlockEntity.DATA_COUNT);
        this.container = container;
        this.data = data;
        this.blockPos = container instanceof SilexBlockEntity silex ? silex.getBlockPos() : BlockPos.ZERO;

        this.addSlot(new ValidatedSlot(container, SilexBlockEntity.INPUT_SLOT, 80, 12));
        this.addSlot(new ValidatedSlot(container, SilexBlockEntity.FLUID_IDENTIFIER_SLOT, 8, 24));
        this.addSlot(new ValidatedSlot(container, SilexBlockEntity.FLUID_INPUT_SLOT, 26, 24));
        this.addSlot(new OutputSlot(container, SilexBlockEntity.FLUID_OUTPUT_SLOT, 44, 24));
        this.addSlot(new OutputSlot(container, SilexBlockEntity.CURRENT_OUTPUT_SLOT, 116, 90));
        this.addSlot(new OutputSlot(container, SilexBlockEntity.QUEUE_START, 134, 72));
        this.addSlot(new OutputSlot(container, SilexBlockEntity.QUEUE_START + 1, 152, 72));
        this.addSlot(new OutputSlot(container, SilexBlockEntity.QUEUE_START + 2, 134, 90));
        this.addSlot(new OutputSlot(container, SilexBlockEntity.QUEUE_START + 3, 152, 90));
        this.addSlot(new OutputSlot(container, SilexBlockEntity.QUEUE_START + 4, 134, 108));
        this.addSlot(new OutputSlot(container, SilexBlockEntity.QUEUE_START + 5, 152, 108));

        addPlayerInventory(playerInventory, 8, 140);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && this.container instanceof SilexBlockEntity silex && stillValid(player)) {
            silex.clearCurrent();
            return true;
        }
        return false;
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
        } else if (stack.getItem() instanceof FluidIdentifierItem) {
            if (!moveItemStackTo(stack, SilexBlockEntity.FLUID_IDENTIFIER_SLOT, SilexBlockEntity.FLUID_IDENTIFIER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (isFilledFluidContainer(stack)) {
            if (!moveItemStackTo(stack, SilexBlockEntity.FLUID_INPUT_SLOT, SilexBlockEntity.FLUID_INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (hasRecipe(stack)) {
            if (!moveItemStackTo(stack, SilexBlockEntity.INPUT_SLOT, SilexBlockEntity.INPUT_SLOT + 1, false)) {
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

    public BlockPos blockPos() {
        return this.blockPos;
    }

    public com.reinhardt.hbm.fluid.HbmFluidDefinition tankFluid() {
        return HbmFluids.byOldId(this.data.get(0)).orElse(HbmFluids.none());
    }

    public int tankAmount() {
        return this.data.get(1);
    }

    public int currentFill() {
        return this.data.get(2);
    }

    public int progress() {
        return this.data.get(3);
    }

    public Wavelength mode() {
        Wavelength[] values = Wavelength.values();
        int ordinal = this.data.get(4);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : Wavelength.NULL;
    }

    public int currentItemId() {
        return this.data.get(5);
    }

    public int currentItemDamage() {
        return this.data.get(6);
    }

    public ItemStack currentDisplay() {
        if (this.container instanceof SilexBlockEntity silex) {
            return silex.currentDisplay().copy();
        }
        int itemId = currentItemId();
        if (itemId < 0) {
            return ItemStack.EMPTY;
        }
        net.minecraft.world.item.Item item = net.minecraft.world.item.Item.byId(itemId);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        if (stack.isDamageableItem()) {
            stack.setDamageValue(currentItemDamage());
        }
        return stack;
    }

    public int recipeIndex() {
        return this.data.get(7);
    }

    public int processTime() {
        return Math.max(1, this.data.get(8));
    }

    public int tankCapacity() {
        return Math.max(1, this.data.get(9));
    }

    public int maxFill() {
        return Math.max(1, this.data.get(10));
    }

    public int progressScaled(int pixels) {
        return Math.min(pixels, this.progress() * pixels / this.processTime());
    }

    public int tankScaled(int pixels) {
        return Math.min(pixels, this.tankAmount() * pixels / this.tankCapacity());
    }

    public int currentFillScaled(int pixels) {
        return Math.min(pixels, this.currentFill() * pixels / this.maxFill());
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
        return !stack.isEmpty() && this.slots.getFirst().container instanceof SilexBlockEntity silex && silex.getLevel() != null
                && silex.getLevel().getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SILEX.get()).stream()
                .map(RecipeHolder::value)
                .anyMatch(recipe -> recipe.ingredient().isPresent() && recipe.ingredient().get().test(stack));
    }

    private static boolean isFilledFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof InfiniteFluidContainerItem
                || (stack.getItem() instanceof HbmFluidContainerItem item && item.isFilledContainer())
                || FluidUtil.getFluidContained(stack).isPresent();
    }

    private static Container getContainer(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof SilexBlockEntity silex ? silex : new SimpleContainer(MACHINE_SLOT_COUNT);
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
