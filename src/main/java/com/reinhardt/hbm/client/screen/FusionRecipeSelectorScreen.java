package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.FusionMachineMenu;
import com.reinhardt.hbm.recipe.FusionRecipe;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The modern counterpart to 1.7.10's GUIScreenRecipeSelector for the fusion
 * torus.  The legacy GUI was a searchable selector, not a recipe-cycle button.
 */
public final class FusionRecipeSelectorScreen extends Screen {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_recipe_selector.png");
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 132;
    private static final int RECIPES_PER_PAGE = 40;

    private final FusionMachineScreen previousScreen;
    private final FusionMachineMenu menu;
    private final List<RecipeHolder<FusionRecipe>> allRecipes;
    private final List<RecipeHolder<FusionRecipe>> recipes = new ArrayList<>();

    @Nullable
    private ResourceLocation selectedRecipeId;
    private EditBox search;
    private int leftPos;
    private int topPos;
    private int pageIndex;
    private int pageCount;
    private boolean selectionChanged;

    public FusionRecipeSelectorScreen(FusionMachineScreen previousScreen, FusionMachineMenu menu) {
        super(Component.translatable("container.reinhardtshbm.fusion_torus.recipe_selector"));
        this.previousScreen = previousScreen;
        this.menu = menu;
        this.allRecipes = List.copyOf(menu.fusionRecipes());
        this.selectedRecipeId = menu.selectedFusionRecipeId().orElse(null);
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
        renderControls(graphics, mouseX, mouseY);
        renderRecipes(graphics);
        this.search.render(graphics, mouseX, mouseY, partialTick);
        renderTooltips(graphics, mouseX, mouseY);
    }

    private void renderControls(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.search.isFocused()) {
            graphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 108, 0, 132, 106, 16);
        }
        hoverSprite(graphics, mouseX, mouseY, 152, 18, 0);
        hoverSprite(graphics, mouseX, mouseY, 152, 36, 16);
        hoverSprite(graphics, mouseX, mouseY, 152, 90, 32);
        hoverSprite(graphics, mouseX, mouseY, 134, 108, 48);
        hoverSprite(graphics, mouseX, mouseY, 8, 108, 64);
    }

    private void hoverSprite(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int sourceY) {
        if (inside(mouseX, mouseY, x, y, 16, 16)) {
            graphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, 176, sourceY, 16, 16);
        }
    }

    private void renderRecipes(GuiGraphics graphics) {
        for (int recipeIndex = this.pageIndex * 8;
                recipeIndex < this.pageIndex * 8 + RECIPES_PER_PAGE && recipeIndex < this.recipes.size();
                recipeIndex++) {
            int visibleIndex = recipeIndex - this.pageIndex * 8;
            RecipeHolder<FusionRecipe> holder = this.recipes.get(recipeIndex);
            int x = 8 + 18 * (visibleIndex % 8);
            int y = 18 + 18 * (visibleIndex / 8);
            if (holder.id().equals(this.selectedRecipeId)) {
                graphics.blit(TEXTURE, this.leftPos + x - 1, this.topPos + y - 1, 192, 0, 18, 18);
            }
            renderIcon(graphics, holder, x, y);
        }
        RecipeHolder<FusionRecipe> selected = selectedRecipe();
        if (selected != null) {
            renderIcon(graphics, selected, 152, 72);
        }
    }

    private void renderIcon(GuiGraphics graphics, RecipeHolder<FusionRecipe> holder, int x, int y) {
        ItemStack icon = holder.value().displayIcon();
        graphics.renderItem(icon, this.leftPos + x, this.topPos + y);
        graphics.renderItemDecorations(this.font, icon, this.leftPos + x, this.topPos + y);
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int recipeIndex = recipeAt(mouseX, mouseY);
        if (recipeIndex >= 0) {
            graphics.renderComponentTooltip(this.font, describeRecipe(this.recipes.get(recipeIndex)), mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, 151, 71, 18, 18)) {
            RecipeHolder<FusionRecipe> selected = selectedRecipe();
            if (selected != null) {
                graphics.renderComponentTooltip(this.font, describeRecipe(selected), mouseX, mouseY);
            }
            return;
        }
        if (inside(mouseX, mouseY, 152, 90, 16, 16)) {
            graphics.renderComponentTooltip(this.font,
                    List.of(Component.translatable("gui.recipe.close").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 134, 108, 16, 16)) {
            graphics.renderComponentTooltip(this.font,
                    List.of(Component.translatable("gui.recipe.clear_search").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 8, 108, 16, 16)) {
            graphics.renderComponentTooltip(this.font,
                    List.of(Component.translatable("gui.recipe.search_focus").withStyle(ChatFormatting.ITALIC)), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.search.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (inside(mouseX, mouseY, 152, 18, 16, 16)) {
            playClick();
            this.pageIndex = Math.max(0, this.pageIndex - 1);
            return true;
        }
        if (inside(mouseX, mouseY, 152, 36, 16, 16)) {
            playClick();
            this.pageIndex = Math.min(this.pageCount, this.pageIndex + 1);
            return true;
        }
        if (inside(mouseX, mouseY, 134, 108, 16, 16)) {
            playClick();
            this.search.setValue("");
            this.search.setFocused(true);
            return true;
        }
        if (inside(mouseX, mouseY, 8, 108, 16, 16)) {
            playClick();
            this.search.setFocused(true);
            setInitialFocus(this.search);
            return true;
        }
        if (inside(mouseX, mouseY, 151, 71, 18, 18)) {
            if (this.selectedRecipeId != null) {
                playClick();
                this.selectedRecipeId = null;
                this.selectionChanged = true;
            }
            return true;
        }
        if (inside(mouseX, mouseY, 152, 90, 16, 16)) {
            playClick();
            closeSelector();
            return true;
        }
        int recipeIndex = recipeAt((int) mouseX, (int) mouseY);
        if (recipeIndex >= 0) {
            playClick();
            ResourceLocation id = this.recipes.get(recipeIndex).id();
            this.selectedRecipeId = id.equals(this.selectedRecipeId) ? null : id;
            this.selectionChanged = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!inside(mouseX, mouseY, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (scrollY > 0.0D) {
            this.pageIndex = Math.max(0, this.pageIndex - 1);
        } else if (scrollY < 0.0D) {
            this.pageIndex = Math.min(this.pageCount, this.pageIndex + 1);
        }
        return true;
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
        return this.search.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
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
        if (!this.selectionChanged || this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        int buttonId = this.selectedRecipeId == null ? 0 : this.menu.buttonIdForFusionRecipe(this.selectedRecipeId);
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        this.selectionChanged = false;
    }

    private void regenerateRecipes(String query) {
        this.recipes.clear();
        String normalized = query.toLowerCase(Locale.ROOT);
        for (RecipeHolder<FusionRecipe> holder : this.allRecipes) {
            if (normalized.isBlank() || matchesSearch(holder, normalized)) {
                this.recipes.add(holder);
            }
        }
        this.pageCount = Math.max(0, (int) Math.ceil((this.recipes.size() - RECIPES_PER_PAGE) / 8.0D));
        this.pageIndex = Mth.clamp(this.pageIndex, 0, this.pageCount);
    }

    private static boolean matchesSearch(RecipeHolder<FusionRecipe> holder, String query) {
        FusionRecipe recipe = holder.value();
        if (holder.id().toString().toLowerCase(Locale.ROOT).contains(query)
                || recipe.group().toLowerCase(Locale.ROOT).contains(query)
                || recipe.displayIcon().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        return recipe.inputFluids().stream().anyMatch(fluid -> fluid.fluid().name().toLowerCase(Locale.ROOT).contains(query))
                || recipe.outputFluids().stream().anyMatch(fluid -> fluid.fluid().name().toLowerCase(Locale.ROOT).contains(query));
    }

    private int recipeAt(int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, 7, 17, 144, 90)) {
            return -1;
        }
        for (int index = this.pageIndex * 8;
                index < this.pageIndex * 8 + RECIPES_PER_PAGE && index < this.recipes.size();
                index++) {
            int visibleIndex = index - this.pageIndex * 8;
            if (inside(mouseX, mouseY, 7 + 18 * (visibleIndex % 8), 17 + 18 * (visibleIndex / 8), 18, 18)) {
                return index;
            }
        }
        return -1;
    }

    @Nullable
    private RecipeHolder<FusionRecipe> selectedRecipe() {
        if (this.selectedRecipeId == null) {
            return null;
        }
        for (RecipeHolder<FusionRecipe> holder : this.allRecipes) {
            if (holder.id().equals(this.selectedRecipeId)) {
                return holder;
            }
        }
        return null;
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + width
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + height;
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private static List<Component> describeRecipe(RecipeHolder<FusionRecipe> holder) {
        FusionRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        lines.add(recipe.displayIcon().getHoverName().copy().withStyle(ChatFormatting.YELLOW));
        lines.add(Component.literal("" + HbmFluidTooltip.shortNumber(recipe.duration()) + " ticks").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal(HbmFluidTooltip.shortNumber(recipe.power()) + " HE/t").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.recipe.fusion.input", HbmFluidTooltip.shortNumber(recipe.ignitionTemp()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("gui.recipe.fusion.output", HbmFluidTooltip.shortNumber(recipe.outputTemp()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("gui.recipe.fusion.flux", recipe.neutronFlux())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        for (FusionRecipe.FusionFluidStack fluid : recipe.inputFluids()) {
            lines.add(Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable(fluid.fluid().translationKey()))
                    .append(Component.literal(" x" + fluid.amount() + "mB")));
        }
        for (FusionRecipe.FusionFluidStack fluid : recipe.outputFluids()) {
            lines.add(Component.literal(" + ").withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable(fluid.fluid().translationKey()))
                    .append(Component.literal(" x" + fluid.amount() + "mB")));
        }
        recipe.outputItem().ifPresent(output -> lines.add(Component.literal(" + ").withStyle(ChatFormatting.GRAY)
                .append(output.getHoverName())));
        return lines;
    }
}
