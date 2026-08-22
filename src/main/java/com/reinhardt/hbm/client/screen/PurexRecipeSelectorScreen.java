package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.menu.PurexMenu;
import com.reinhardt.hbm.recipe.PurexRecipe;
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

public final class PurexRecipeSelectorScreen extends Screen {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_recipe_selector.png");
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 132;

    private final PurexScreen previousScreen;
    private final PurexMenu menu;
    private final List<RecipeHolder<PurexRecipe>> originRecipes;
    private final List<RecipeHolder<PurexRecipe>> recipes = new ArrayList<>();
    @Nullable private ResourceLocation selectedRecipeId;
    private EditBox search;
    private int leftPos;
    private int topPos;
    private int pageIndex;
    private int pageCount;
    private boolean dirty;

    public PurexRecipeSelectorScreen(PurexScreen previousScreen, PurexMenu menu) {
        super(Component.translatable("container.reinhardtshbm.purex.recipe_selector"));
        this.previousScreen = previousScreen;
        this.menu = menu;
        this.originRecipes = List.copyOf(menu.purexRecipes());
        this.selectedRecipeId = menu.selectedRecipeId().orElse(null);
        regenerateRecipes("");
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;
        this.search = new EditBox(this.font, this.leftPos + 28, this.topPos + 111, 102, 12,
                Component.translatable("container.reinhardtshbm.recipe_selector.search"));
        this.search.setTextColor(0xFFFFFF);
        this.search.setTextColorUneditable(0xFFFFFF);
        this.search.setBordered(false);
        this.search.setMaxLength(32);
        this.search.setResponder(this::regenerateRecipes);
        this.search.setFocused(true);
        setInitialFocus(this.search);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        if (this.search.isFocused()) graphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 108, 0, 132, 106, 16);
        if (inside(mouseX, mouseY, 152, 18, 16, 16)) graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 18, 176, 0, 16, 16);
        if (inside(mouseX, mouseY, 152, 36, 16, 16)) graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 36, 176, 16, 16, 16);
        if (inside(mouseX, mouseY, 152, 90, 16, 16)) graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 90, 176, 32, 16, 16);
        if (inside(mouseX, mouseY, 134, 108, 16, 16)) graphics.blit(TEXTURE, this.leftPos + 134, this.topPos + 108, 176, 48, 16, 16);
        if (inside(mouseX, mouseY, 8, 108, 16, 16)) graphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 108, 176, 64, 16, 16);

        int start = this.pageIndex * 8;
        int end = Math.min(start + 40, this.recipes.size());
        for (int index = start; index < end; index++) {
            int visible = index - start;
            RecipeHolder<PurexRecipe> holder = this.recipes.get(index);
            if (holder.id().equals(this.selectedRecipeId)) {
                graphics.blit(TEXTURE, this.leftPos + 7 + 18 * (visible % 8), this.topPos + 17 + 18 * (visible / 8), 192, 0, 18, 18);
            }
        }
        for (int index = start; index < end; index++) {
            int visible = index - start;
            renderIcon(graphics, this.recipes.get(index), 8 + 18 * (visible % 8), 18 + 18 * (visible / 8));
        }
        RecipeHolder<PurexRecipe> selected = selectedRecipe();
        if (selected != null) renderIcon(graphics, selected, 152, 72);

        this.search.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.search.mouseClicked(mouseX, mouseY, button)) return true;
        int x = (int) mouseX;
        int y = (int) mouseY;
        if (inside(x, y, 152, 18, 16, 16)) {
            playClick();
            if (this.pageIndex > 0) this.pageIndex--;
            return true;
        }
        if (inside(x, y, 152, 36, 16, 16)) {
            playClick();
            if (this.pageIndex < this.pageCount) this.pageIndex++;
            return true;
        }
        if (inside(x, y, 134, 108, 16, 16)) {
            playClick();
            this.search.setValue("");
            this.search.setFocused(true);
            return true;
        }
        if (inside(x, y, 8, 108, 16, 16)) {
            playClick();
            this.search.setFocused(true);
            setInitialFocus(this.search);
            return true;
        }
        if (inside(x, y, 151, 71, 18, 18)) {
            if (this.selectedRecipeId != null) {
                playClick();
                this.selectedRecipeId = null;
                this.dirty = true;
            }
            return true;
        }
        if (inside(x, y, 152, 90, 16, 16)) {
            playClick();
            closeSelector();
            return true;
        }
        int recipeIndex = recipeAt(x, y);
        if (recipeIndex >= 0) {
            playClick();
            ResourceLocation clicked = this.recipes.get(recipeIndex).id();
            this.selectedRecipeId = clicked.equals(this.selectedRecipeId) ? null : clicked;
            this.dirty = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside((int) mouseX, (int) mouseY, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT)) {
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
        if (this.search.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            closeSelector();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.search.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        closeSelector();
    }

    private void closeSelector() {
        if (this.dirty && this.minecraft != null && this.minecraft.gameMode != null) {
            int buttonId = this.selectedRecipeId == null ? 0 : this.menu.buttonIdForRecipe(this.selectedRecipeId);
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
            this.dirty = false;
        }
        if (this.minecraft != null) this.minecraft.setScreen(this.previousScreen);
    }

    private void regenerateRecipes(String query) {
        this.recipes.clear();
        String normalized = query.toLowerCase(Locale.ROOT);
        for (RecipeHolder<PurexRecipe> holder : this.originRecipes) {
            if (normalized.isBlank() || matchesSearch(holder, normalized)) this.recipes.add(holder);
        }
        this.pageCount = Math.max(0, (int) Math.ceil((this.recipes.size() - 40) / 8.0D));
        this.pageIndex = Mth.clamp(this.pageIndex, 0, this.pageCount);
    }

    private boolean matchesSearch(RecipeHolder<PurexRecipe> holder, String query) {
        PurexRecipe recipe = holder.value();
        if (holder.id().toString().toLowerCase(Locale.ROOT).contains(query)
                || recipe.group().toLowerCase(Locale.ROOT).contains(query)
                || displayIcon(holder).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        for (PurexRecipe.CountedIngredient input : recipe.inputItems()) {
            if (displayIngredient(input).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        for (PurexRecipe.PurexItemOutput output : recipe.outputItems()) {
            if (output.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        for (PurexRecipe.PurexFluidStack fluid : recipe.inputFluids()) {
            if (Component.translatable(fluid.type().translationKey()).getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        for (PurexRecipe.PurexFluidStack fluid : recipe.outputFluids()) {
            if (Component.translatable(fluid.type().translationKey()).getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        return false;
    }

    private void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int hovered = recipeAt(mouseX, mouseY);
        if (hovered >= 0) {
            graphics.renderComponentTooltip(this.font, describeRecipe(this.recipes.get(hovered)), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 151, 71, 18, 18) && selectedRecipe() != null) {
            graphics.renderComponentTooltip(this.font, describeRecipe(selectedRecipe()), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 152, 90, 16, 16)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.close").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 134, 108, 16, 16)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.clear_search").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
        }
    }

    private void renderIcon(GuiGraphics graphics, RecipeHolder<PurexRecipe> holder, int x, int y) {
        ItemStack icon = displayIcon(holder);
        graphics.renderItem(icon, this.leftPos + x, this.topPos + y);
        graphics.renderItemDecorations(this.font, icon, this.leftPos + x, this.topPos + y);
    }

    private int recipeAt(int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, 7, 17, 144, 90)) return -1;
        int start = this.pageIndex * 8;
        int end = Math.min(start + 40, this.recipes.size());
        for (int index = start; index < end; index++) {
            int visible = index - start;
            if (inside(mouseX, mouseY, 7 + 18 * (visible % 8), 17 + 18 * (visible / 8), 18, 18)) return index;
        }
        return -1;
    }

    @Nullable
    private RecipeHolder<PurexRecipe> selectedRecipe() {
        if (this.selectedRecipeId == null) return null;
        return this.originRecipes.stream().filter(holder -> holder.id().equals(this.selectedRecipeId)).findFirst().orElse(null);
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return this.leftPos + x <= mouseX && this.leftPos + x + width > mouseX
                && this.topPos + y < mouseY && this.topPos + y + height >= mouseY;
    }

    private void playClick() {
        if (this.minecraft != null) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public static ItemStack displayIcon(RecipeHolder<PurexRecipe> holder) {
        PurexRecipe recipe = holder.value();
        if (!recipe.inputItems().isEmpty()) return displayIngredient(recipe.inputItems().getFirst());
        if (!recipe.inputFluids().isEmpty()) {
            PurexRecipe.PurexFluidStack fluid = recipe.inputFluids().getFirst();
            return FluidIconItem.forFluid(fluid.type(), fluid.amount(), fluid.pressure());
        }
        return recipe.outputItems().isEmpty() ? ItemStack.EMPTY : recipe.outputItems().getFirst().stack().copy();
    }

    static ItemStack displayIngredient(PurexRecipe.CountedIngredient ingredient) {
        ItemStack display = Arrays.stream(ingredient.ingredient().getItems()).findFirst().orElse(ItemStack.EMPTY).copy();
        if (!display.isEmpty()) display.setCount(ingredient.count());
        return display;
    }

    public static List<Component> describeRecipe(RecipeHolder<PurexRecipe> holder) {
        PurexRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        lines.add(displayIcon(holder).getHoverName().copy().withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("info.reinhardtshbm.template_power", recipe.power()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.reinhardtshbm.template_duration", recipe.duration()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.reinhardtshbm.template_in").withStyle(ChatFormatting.BOLD));
        for (PurexRecipe.CountedIngredient input : recipe.inputItems()) {
            ItemStack display = displayIngredient(input);
            lines.add(Component.literal(" - ").withStyle(ChatFormatting.GRAY).append(display.getHoverName())
                    .append(Component.literal(" x" + input.count())));
        }
        for (PurexRecipe.PurexFluidStack fluid : recipe.inputFluids()) lines.add(fluidLine(fluid));
        lines.add(Component.translatable("info.reinhardtshbm.template_out").withStyle(ChatFormatting.BOLD));
        for (PurexRecipe.PurexItemOutput output : recipe.outputItems()) {
            MutableComponent line = Component.literal(" - ").withStyle(ChatFormatting.GRAY).append(output.stack().getHoverName())
                    .append(Component.literal(" x" + output.stack().getCount()));
            if (output.chance() < 1.0F) {
                line.append(Component.literal(" (" + (int) (output.chance() * 1000.0F) / 10.0F + "%)").withStyle(ChatFormatting.RED));
            }
            lines.add(line);
        }
        for (PurexRecipe.PurexFluidStack fluid : recipe.outputFluids()) lines.add(fluidLine(fluid));
        return lines;
    }

    private static Component fluidLine(PurexRecipe.PurexFluidStack fluid) {
        MutableComponent line = Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                .append(Component.translatable(fluid.type().translationKey()))
                .append(Component.literal(" " + fluid.amount() + "mB"));
        if (fluid.pressure() > 0) line.append(Component.literal(" " + fluid.pressure() + " PU").withStyle(ChatFormatting.RED));
        return line;
    }
}
