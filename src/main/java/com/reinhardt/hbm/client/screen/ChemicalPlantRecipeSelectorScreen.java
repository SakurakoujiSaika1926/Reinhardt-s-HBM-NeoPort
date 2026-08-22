package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.menu.ChemicalPlantMenu;
import com.reinhardt.hbm.recipe.ChemicalPlantRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ChemicalPlantRecipeSelectorScreen extends Screen {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_recipe_selector.png");
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 132;

    private final ChemicalPlantScreen previousScreen;
    private final ChemicalPlantMenu menu;
    private final List<RecipeHolder<ChemicalPlantRecipe>> originRecipes;
    private final List<RecipeHolder<ChemicalPlantRecipe>> recipes = new ArrayList<>();

    @Nullable
    private ResourceLocation selectedRecipeId;
    private EditBox search;
    private int leftPos;
    private int topPos;
    private int pageIndex;
    private int pageCount;
    private boolean dirty;

    public ChemicalPlantRecipeSelectorScreen(ChemicalPlantScreen previousScreen, ChemicalPlantMenu menu) {
        super(Component.translatable("container.reinhardtshbm.chemical_plant.recipe_selector"));
        this.previousScreen = previousScreen;
        this.menu = menu;
        this.originRecipes = List.copyOf(menu.chemicalPlantRecipes());
        this.selectedRecipeId = menu.selectedRecipeId().orElse(null);
        regenerateRecipes("");
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;

        this.search = new EditBox(this.font, this.leftPos + 28, this.topPos + 111, 102, 12, Component.translatable("container.reinhardtshbm.recipe_selector.search"));
        this.search.setTextColor(0xFFFFFF);
        this.search.setTextColorUneditable(0xFFFFFF);
        this.search.setBordered(false);
        this.search.setMaxLength(32);
        this.search.setResponder(this::regenerateRecipes);
        this.search.setFocused(true);
        setInitialFocus(this.search);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        renderSelector(guiGraphics, mouseX, mouseY);
        this.search.render(guiGraphics, mouseX, mouseY, partialTick);
        renderSelectorTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderSelector(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        if (this.search.isFocused()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 108, 0, 132, 106, 16);
        }
        if (isInside(mouseX, mouseY, 152, 18, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 18, 176, 0, 16, 16);
        }
        if (isInside(mouseX, mouseY, 152, 36, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 36, 176, 16, 16, 16);
        }
        if (isInside(mouseX, mouseY, 152, 90, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 90, 176, 32, 16, 16);
        }
        if (isInside(mouseX, mouseY, 134, 108, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 134, this.topPos + 108, 176, 48, 16, 16);
        }
        if (isInside(mouseX, mouseY, 8, 108, 16, 16)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 108, 176, 64, 16, 16);
        }

        for (int recipeIndex = this.pageIndex * 8; recipeIndex < this.pageIndex * 8 + 40 && recipeIndex < this.recipes.size(); recipeIndex++) {
            int visibleIndex = recipeIndex - this.pageIndex * 8;
            RecipeHolder<ChemicalPlantRecipe> holder = this.recipes.get(recipeIndex);
            if (holder.id().equals(this.selectedRecipeId)) {
                guiGraphics.blit(TEXTURE, this.leftPos + 7 + 18 * (visibleIndex % 8), this.topPos + 17 + 18 * (visibleIndex / 8), 192, 0, 18, 18);
            }
        }

        for (int recipeIndex = this.pageIndex * 8; recipeIndex < this.pageIndex * 8 + 40 && recipeIndex < this.recipes.size(); recipeIndex++) {
            int visibleIndex = recipeIndex - this.pageIndex * 8;
            renderRecipeIcon(guiGraphics, this.recipes.get(recipeIndex), 8 + 18 * (visibleIndex % 8), 18 + 18 * (visibleIndex / 8));
        }

        RecipeHolder<ChemicalPlantRecipe> selected = selectedRecipe();
        if (selected != null) {
            renderRecipeIcon(guiGraphics, selected, 152, 72);
        }
    }

    private void renderSelectorTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int hoveredRecipe = recipeAt(mouseX, mouseY);
        if (hoveredRecipe >= 0) {
            guiGraphics.renderComponentTooltip(this.font, describeRecipe(this.recipes.get(hoveredRecipe)), mouseX, mouseY);
            return;
        }

        if (isInside(mouseX, mouseY, 151, 71, 18, 18)) {
            RecipeHolder<ChemicalPlantRecipe> selected = selectedRecipe();
            if (selected != null) {
                guiGraphics.renderComponentTooltip(this.font, describeRecipe(selected), mouseX, mouseY);
            }
            return;
        }

        if (isInside(mouseX, mouseY, 152, 90, 16, 16)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.close").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
            return;
        }

        if (isInside(mouseX, mouseY, 134, 108, 16, 16)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.clear_search").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
            return;
        }

        if (isInside(mouseX, mouseY, 8, 108, 16, 16)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.search_focus").withStyle(ChatFormatting.ITALIC)), mouseX, mouseY);
        }
    }

    private void renderRecipeIcon(GuiGraphics guiGraphics, RecipeHolder<ChemicalPlantRecipe> holder, int x, int y) {
        ItemStack icon = holder.value().displayIcon();
        guiGraphics.renderItem(icon, this.leftPos + x, this.topPos + y);
        guiGraphics.renderItemDecorations(this.font, icon, this.leftPos + x, this.topPos + y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.search.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int x = (int) mouseX;
        int y = (int) mouseY;
        if (isInside(x, y, 152, 18, 16, 16)) {
            playClick();
            if (this.pageIndex > 0) {
                this.pageIndex--;
            }
            return true;
        }
        if (isInside(x, y, 152, 36, 16, 16)) {
            playClick();
            if (this.pageIndex < this.pageCount) {
                this.pageIndex++;
            }
            return true;
        }
        if (isInside(x, y, 134, 108, 16, 16)) {
            playClick();
            this.search.setValue("");
            this.search.setFocused(true);
            regenerateRecipes("");
            return true;
        }
        if (isInside(x, y, 8, 108, 16, 16)) {
            playClick();
            this.search.setFocused(true);
            setInitialFocus(this.search);
            return true;
        }
        if (isInside(x, y, 151, 71, 18, 18)) {
            if (this.selectedRecipeId != null) {
                playClick();
                this.selectedRecipeId = null;
                this.dirty = true;
            }
            return true;
        }
        if (isInside(x, y, 152, 90, 16, 16)) {
            playClick();
            closeSelector();
            return true;
        }

        int recipeIndex = recipeAt(x, y);
        if (recipeIndex >= 0) {
            playClick();
            ResourceLocation clickedRecipe = this.recipes.get(recipeIndex).id();
            this.selectedRecipeId = clickedRecipe.equals(this.selectedRecipeId) ? null : clickedRecipe;
            this.dirty = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInside((int) mouseX, (int) mouseY, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT)) {
            if (scrollY > 0.0D && this.pageIndex > 0) {
                this.pageIndex--;
                return true;
            }
            if (scrollY < 0.0D && this.pageIndex < this.pageCount) {
                this.pageIndex++;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.search.setFocused(!this.search.isFocused());
            return true;
        }
        if (this.search.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            closeSelector();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.search.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        closeSelector();
    }

    private void closeSelector() {
        sendSelection();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previousScreen);
        }
    }

    private void sendSelection() {
        if (!this.dirty || this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        int buttonId = this.selectedRecipeId == null ? 0 : this.menu.buttonIdForRecipe(this.selectedRecipeId);
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        this.dirty = false;
    }

    private void regenerateRecipes(String query) {
        this.recipes.clear();
        String normalized = query.toLowerCase(Locale.ROOT);
        for (RecipeHolder<ChemicalPlantRecipe> holder : this.originRecipes) {
            if (normalized.isBlank() || matchesSearch(holder, normalized)) {
                this.recipes.add(holder);
            }
        }
        this.pageIndex = Mth.clamp(this.pageIndex, 0, Math.max(0, (int) Math.ceil((this.recipes.size() - 40) / 8.0D)));
        this.pageCount = Math.max(0, (int) Math.ceil((this.recipes.size() - 40) / 8.0D));
    }

    private boolean matchesSearch(RecipeHolder<ChemicalPlantRecipe> holder, String query) {
        ChemicalPlantRecipe recipe = holder.value();
        if (holder.id().toString().toLowerCase(Locale.ROOT).contains(query) || recipe.group().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        if (recipe.displayIcon().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        for (ChemicalPlantRecipe.CountedIngredient ingredient : recipe.inputItems()) {
            ItemStack display = displayIngredient(ingredient);
            if (!display.isEmpty() && display.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        for (ItemStack output : recipe.outputItems()) {
            if (!output.isEmpty() && output.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        for (ChemicalPlantRecipe.ChemicalFluidStack fluid : recipe.inputFluids()) {
            if (Component.translatable(fluid.type().translationKey()).getString().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        for (ChemicalPlantRecipe.ChemicalFluidStack fluid : recipe.outputFluids()) {
            if (Component.translatable(fluid.type().translationKey()).getString().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private int recipeAt(int mouseX, int mouseY) {
        if (!isInside(mouseX, mouseY, 7, 17, 144, 90)) {
            return -1;
        }
        for (int recipeIndex = this.pageIndex * 8; recipeIndex < this.pageIndex * 8 + 40 && recipeIndex < this.recipes.size(); recipeIndex++) {
            int visibleIndex = recipeIndex - this.pageIndex * 8;
            int x = 7 + 18 * (visibleIndex % 8);
            int y = 17 + 18 * (visibleIndex / 8);
            if (isInside(mouseX, mouseY, x, y, 18, 18)) {
                return recipeIndex;
            }
        }
        return -1;
    }

    @Nullable
    private RecipeHolder<ChemicalPlantRecipe> selectedRecipe() {
        if (this.selectedRecipeId == null) {
            return null;
        }
        for (RecipeHolder<ChemicalPlantRecipe> holder : this.originRecipes) {
            if (holder.id().equals(this.selectedRecipeId)) {
                return holder;
            }
        }
        return null;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return this.leftPos + x <= mouseX
                && this.leftPos + x + width > mouseX
                && this.topPos + y < mouseY
                && this.topPos + y + height >= mouseY;
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    public static List<Component> describeRecipe(RecipeHolder<ChemicalPlantRecipe> holder) {
        ChemicalPlantRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        lines.add(recipe.displayIcon().getHoverName().copy().withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("info.reinhardtshbm.template_power", recipe.power()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.reinhardtshbm.template_duration", recipe.duration()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.reinhardtshbm.template_in").withStyle(ChatFormatting.BOLD));
        for (ChemicalPlantRecipe.CountedIngredient ingredient : recipe.inputItems()) {
            ItemStack display = displayIngredient(ingredient);
            if (!display.isEmpty()) {
                lines.add(Component.literal(" - ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(display.getHoverName())
                        .append(Component.literal(" x" + ingredient.count())));
            }
        }
        for (ChemicalPlantRecipe.ChemicalFluidStack fluid : recipe.inputFluids()) {
            lines.add(fluidLine(fluid));
        }
        lines.add(Component.translatable("info.reinhardtshbm.template_out").withStyle(ChatFormatting.BOLD));
        for (ItemStack output : recipe.outputItems()) {
            if (!output.isEmpty()) {
                lines.add(Component.literal(" - ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(output.getHoverName())
                        .append(Component.literal(" x" + output.getCount())));
            }
        }
        for (ChemicalPlantRecipe.ChemicalFluidStack fluid : recipe.outputFluids()) {
            lines.add(fluidLine(fluid));
        }
        return lines;
    }

    static ItemStack displayIngredient(ChemicalPlantRecipe.CountedIngredient ingredient) {
        ItemStack[] stacks = ingredient.ingredient().getItems();
        if (stacks.length == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack display = Arrays.stream(stacks).findFirst().orElse(ItemStack.EMPTY).copy();
        display.setCount(ingredient.count());
        return display;
    }

    private static Component fluidLine(ChemicalPlantRecipe.ChemicalFluidStack fluid) {
        MutableComponent line = Component.literal(" - ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(fluid.type().translationKey()))
                .append(Component.literal(" " + fluid.amount() + "mB"));
        if (fluid.pressure() > 0) {
            line.append(Component.literal(" " + fluid.pressure() + " PU").withStyle(ChatFormatting.RED));
        }
        return line;
    }

    public static ItemStack displayFluid(ChemicalPlantRecipe.ChemicalFluidStack fluid) {
        return FluidIconItem.forFluid(fluid.type(), fluid.amount(), fluid.pressure());
    }
}
