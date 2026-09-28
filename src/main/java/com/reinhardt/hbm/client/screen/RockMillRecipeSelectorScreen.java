package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.search.JechSearchCompat;
import com.reinhardt.hbm.client.screen.widget.SearchEditBox;
import com.reinhardt.hbm.menu.RockMillMenu;
import com.reinhardt.hbm.recipe.RockMillRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class RockMillRecipeSelectorScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/gui/processing/gui_recipe_selector.png");
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 132;

    private final RockMillScreen previousScreen;
    private final RockMillMenu menu;
    private final List<RecipeHolder<RockMillRecipe>> originRecipes;
    private final List<RecipeHolder<RockMillRecipe>> recipes = new ArrayList<>();

    @Nullable
    private ResourceLocation selectedRecipeId;
    private SearchEditBox search;
    private int leftPos;
    private int topPos;
    private int pageIndex;
    private int pageCount;
    private boolean dirty;

    public RockMillRecipeSelectorScreen(RockMillScreen previousScreen, RockMillMenu menu) {
        super(Component.translatable("container.reinhardtshbm.rock_mill.recipe_selector"));
        this.previousScreen = previousScreen;
        this.menu = menu;
        this.originRecipes = List.copyOf(menu.rockMillRecipes());
        this.selectedRecipeId = menu.selectedRecipeId().orElse(null);
        regenerateRecipes("");
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - IMAGE_WIDTH) / 2;
        this.topPos = (this.height - IMAGE_HEIGHT) / 2;
        this.search = new SearchEditBox(this.font, this.leftPos + 28, this.topPos + 111,
                102, 12, Component.translatable("container.reinhardtshbm.recipe_selector.search"));
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
        renderBackground(graphics, mouseX, mouseY, partialTick);
        renderSelector(graphics, mouseX, mouseY);
        this.search.render(graphics, mouseX, mouseY, partialTick);
        renderSelectorTooltip(graphics, mouseX, mouseY);
    }

    private void renderSelector(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        if (this.search.isFocused()) {
            graphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 108, 0, 132, 106, 16);
        }
        if (inside(mouseX, mouseY, 152, 18, 16, 16)) {
            graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 18, 176, 0, 16, 16);
        }
        if (inside(mouseX, mouseY, 152, 36, 16, 16)) {
            graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 36, 176, 16, 16, 16);
        }
        if (inside(mouseX, mouseY, 152, 90, 16, 16)) {
            graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 90, 176, 32, 16, 16);
        }
        if (inside(mouseX, mouseY, 134, 108, 16, 16)) {
            graphics.blit(TEXTURE, this.leftPos + 134, this.topPos + 108, 176, 48, 16, 16);
        }
        if (inside(mouseX, mouseY, 8, 108, 16, 16)) {
            graphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 108, 176, 64, 16, 16);
        }

        int first = this.pageIndex * 8;
        int last = Math.min(first + 40, this.recipes.size());
        for (int index = first; index < last; index++) {
            int visible = index - first;
            RecipeHolder<RockMillRecipe> holder = this.recipes.get(index);
            if (holder.id().equals(this.selectedRecipeId)) {
                graphics.blit(TEXTURE, this.leftPos + 7 + 18 * (visible % 8),
                        this.topPos + 17 + 18 * (visible / 8), 192, 0, 18, 18);
            }
            renderRecipeIcon(graphics, holder, 8 + 18 * (visible % 8), 18 + 18 * (visible / 8));
        }
        RecipeHolder<RockMillRecipe> selected = selectedRecipe();
        if (selected != null) {
            renderRecipeIcon(graphics, selected, 152, 72);
        }
    }

    private void renderSelectorTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int hovered = recipeAt(mouseX, mouseY);
        if (hovered >= 0) {
            graphics.renderComponentTooltip(this.font, describeRecipe(this.recipes.get(hovered)), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 151, 71, 18, 18)) {
            RecipeHolder<RockMillRecipe> selected = selectedRecipe();
            if (selected != null) {
                graphics.renderComponentTooltip(this.font, describeRecipe(selected), mouseX, mouseY);
            }
        } else if (inside(mouseX, mouseY, 152, 90, 16, 16)) {
            graphics.renderComponentTooltip(this.font,
                    List.of(Component.translatable("gui.recipe.close").withStyle(ChatFormatting.YELLOW)),
                    mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 134, 108, 16, 16)) {
            graphics.renderComponentTooltip(this.font,
                    List.of(Component.translatable("gui.recipe.clear_search").withStyle(ChatFormatting.YELLOW)),
                    mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 8, 108, 16, 16)) {
            graphics.renderComponentTooltip(this.font,
                    List.of(Component.translatable("gui.recipe.search_focus").withStyle(ChatFormatting.ITALIC)),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.search.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int x = (int) mouseX;
        int y = (int) mouseY;
        if (inside(x, y, 152, 18, 16, 16)) {
            playClick();
            this.pageIndex = Math.max(0, this.pageIndex - 1);
            return true;
        }
        if (inside(x, y, 152, 36, 16, 16)) {
            playClick();
            this.pageIndex = Math.min(this.pageCount, this.pageIndex + 1);
            return true;
        }
        if (inside(x, y, 134, 108, 16, 16)) {
            playClick();
            this.search.setValue("");
            this.search.setFocused(true);
            regenerateRecipes("");
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
        int recipe = recipeAt(x, y);
        if (recipe >= 0) {
            playClick();
            ResourceLocation clicked = this.recipes.get(recipe).id();
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
        if (!this.dirty || this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        int id = this.selectedRecipeId == null ? 0 : this.menu.buttonIdForRecipe(this.selectedRecipeId);
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        this.dirty = false;
    }

    private void regenerateRecipes(String query) {
        this.recipes.clear();
        String normalized = JechSearchCompat.normalizeQuery(query);
        for (RecipeHolder<RockMillRecipe> holder : this.originRecipes) {
            if (normalized.isBlank() || matchesSearch(holder, normalized)) {
                this.recipes.add(holder);
            }
        }
        this.pageCount = Math.max(0, (int) Math.ceil((this.recipes.size() - 40) / 8.0D));
        this.pageIndex = Mth.clamp(this.pageIndex, 0, this.pageCount);
    }

    private static boolean matchesSearch(RecipeHolder<RockMillRecipe> holder, String query) {
        RockMillRecipe recipe = holder.value();
        if (JechSearchCompat.contains(holder.id().toString(), query)
                || JechSearchCompat.contains(recipe.group(), query)
                || JechSearchCompat.contains(recipe.displayIcon().getHoverName().getString(), query)) {
            return true;
        }
        for (ItemStack candidate : recipe.input().ingredient().getItems()) {
            if (JechSearchCompat.contains(candidate.getHoverName().getString(), query)) {
                return true;
            }
        }
        for (RockMillRecipe.WeightedOutput output : recipe.outputs()) {
            if (JechSearchCompat.contains(output.stack().getHoverName().getString(), query)) {
                return true;
            }
        }
        return recipe.inputFluid().filter(stack -> fluidMatches(stack, query)).isPresent()
                || recipe.outputFluid().filter(stack -> fluidMatches(stack, query)).isPresent();
    }

    private static boolean fluidMatches(RockMillRecipe.FluidStack stack, String query) {
        return JechSearchCompat.contains(stack.type().name(), query)
                || JechSearchCompat.contains(Component.translatable(stack.type().translationKey()).getString(), query);
    }

    private int recipeAt(int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, 7, 17, 144, 90)) {
            return -1;
        }
        int first = this.pageIndex * 8;
        int last = Math.min(first + 40, this.recipes.size());
        for (int index = first; index < last; index++) {
            int visible = index - first;
            if (inside(mouseX, mouseY, 7 + 18 * (visible % 8),
                    17 + 18 * (visible / 8), 18, 18)) {
                return index;
            }
        }
        return -1;
    }

    @Nullable
    private RecipeHolder<RockMillRecipe> selectedRecipe() {
        if (this.selectedRecipeId == null) {
            return null;
        }
        for (RecipeHolder<RockMillRecipe> holder : this.originRecipes) {
            if (holder.id().equals(this.selectedRecipeId)) {
                return holder;
            }
        }
        return null;
    }

    private void renderRecipeIcon(GuiGraphics graphics, RecipeHolder<RockMillRecipe> holder, int x, int y) {
        ItemStack icon = holder.value().displayIcon();
        graphics.renderItem(icon, this.leftPos + x, this.topPos + y);
        graphics.renderItemDecorations(this.font, icon, this.leftPos + x, this.topPos + y);
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return this.leftPos + x <= mouseX && this.leftPos + x + width > mouseX
                && this.topPos + y < mouseY && this.topPos + y + height >= mouseY;
    }

    private void playClick() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    public static List<Component> describeRecipe(RecipeHolder<RockMillRecipe> holder) {
        RockMillRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("recipe.reinhardtshbm.rock_mill.title")
                .withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("info.reinhardtshbm.template_in").withStyle(ChatFormatting.BOLD));
        lines.add(Component.literal(recipe.input().count() + "x ")
                .append(recipe.displayIcon().getHoverName()));
        recipe.inputFluid().ifPresent(fluid -> lines.add(Component.literal(fluid.amount() + " mB ")
                .append(Component.translatable(fluid.type().translationKey()))));
        lines.add(Component.translatable("info.reinhardtshbm.rock_mill.weighted_output")
                .withStyle(ChatFormatting.BOLD));
        int total = recipe.outputs().stream().mapToInt(RockMillRecipe.WeightedOutput::weight).sum();
        for (RockMillRecipe.WeightedOutput output : recipe.outputs()) {
            double percent = output.weight() * 100.0D / total;
            lines.add(Component.literal(output.stack().getCount() + "x ")
                    .append(output.stack().getHoverName())
                    .append(Component.literal(String.format(java.util.Locale.ROOT, " (%.1f%%)", percent))));
        }
        recipe.outputFluid().ifPresent(fluid -> lines.add(Component.literal(fluid.amount() + " mB ")
                .append(Component.translatable(fluid.type().translationKey()))));
        lines.add(Component.translatable("info.reinhardtshbm.rock_mill.stats",
                recipe.duration(), recipe.power()).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }
}
