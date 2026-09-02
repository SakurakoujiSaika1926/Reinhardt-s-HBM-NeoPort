package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.PrecisionAssemblerMenu;
import com.reinhardt.hbm.recipe.PrecisionAssemblerRecipe;
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
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Precision assembler's old searchable recipe selector, kept separate from the normal assembler pool. */
public final class PrecisionAssemblerRecipeSelectorScreen extends Screen {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_recipe_selector.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 132;
    private final PrecisionAssemblerScreen previous;
    private final PrecisionAssemblerMenu menu;
    private final List<RecipeHolder<PrecisionAssemblerRecipe>> allRecipes;
    private final List<RecipeHolder<PrecisionAssemblerRecipe>> recipes = new ArrayList<>();
    @Nullable private ResourceLocation selected;
    private EditBox search;
    private int left;
    private int top;
    private int page;
    private int maxPage;
    private boolean dirty;

    public PrecisionAssemblerRecipeSelectorScreen(PrecisionAssemblerScreen previous, PrecisionAssemblerMenu menu) {
        super(Component.translatable("container.reinhardtshbm.precision_assembler.recipe_selector"));
        this.previous = previous;
        this.menu = menu;
        this.allRecipes = List.copyOf(menu.recipes());
        this.selected = menu.selectedRecipeId().orElse(null);
        rebuild("");
    }

    @Override protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;
        this.search = new EditBox(this.font, this.left + 28, this.top + 111, 102, 12, Component.translatable("container.reinhardtshbm.recipe_selector.search"));
        this.search.setTextColor(0xFFFFFF);
        this.search.setTextColorUneditable(0xFFFFFF);
        this.search.setBordered(false);
        this.search.setMaxLength(32);
        this.search.setResponder(this::rebuild);
        this.search.setFocused(true);
        setInitialFocus(this.search);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TEXTURE, this.left, this.top, 0, 0, WIDTH, HEIGHT);
        if (this.search.isFocused()) graphics.blit(TEXTURE, this.left + 26, this.top + 108, 0, 132, 106, 16);
        renderHoverButtons(graphics, mouseX, mouseY);
        for (int index = this.page * 8; index < Math.min(this.page * 8 + 40, this.recipes.size()); index++) {
            int visible = index - this.page * 8;
            int x = this.left + 8 + 18 * (visible % 8);
            int y = this.top + 18 + 18 * (visible / 8);
            if (this.recipes.get(index).id().equals(this.selected)) graphics.blit(TEXTURE, x - 1, y - 1, 192, 0, 18, 18);
            ItemStack icon = this.recipes.get(index).value().outputs().getFirst().stack();
            graphics.renderItem(icon, x, y);
        }
        RecipeHolder<PrecisionAssemblerRecipe> current = selectedRecipe();
        if (current != null) graphics.renderItem(current.value().outputs().getFirst().stack(), this.left + 152, this.top + 72);
        this.search.render(graphics, mouseX, mouseY, partialTick);
        int recipe = recipeAt(mouseX, mouseY);
        if (recipe >= 0) graphics.renderComponentTooltip(this.font, describeRecipe(this.recipes.get(recipe)), mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.search.mouseClicked(mouseX, mouseY, button)) return true;
        int x = (int) mouseX;
        int y = (int) mouseY;
        if (inside(x, y, 152, 18, 16, 16)) { if (this.page > 0) this.page--; click(); return true; }
        if (inside(x, y, 152, 36, 16, 16)) { if (this.page < this.maxPage) this.page++; click(); return true; }
        if (inside(x, y, 134, 108, 16, 16)) { this.search.setValue(""); this.search.setFocused(true); click(); return true; }
        if (inside(x, y, 8, 108, 16, 16)) { this.search.setFocused(true); setInitialFocus(this.search); click(); return true; }
        if (inside(x, y, 151, 71, 18, 18)) { this.selected = null; this.dirty = true; click(); return true; }
        if (inside(x, y, 152, 90, 16, 16)) { closeSelector(); click(); return true; }
        int recipe = recipeAt(x, y);
        if (recipe >= 0) { ResourceLocation clicked = this.recipes.get(recipe).id(); this.selected = clicked.equals(this.selected) ? null : clicked; this.dirty = true; click(); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!inside((int) mouseX, (int) mouseY, 0, 0, WIDTH, HEIGHT)) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        if (verticalAmount > 0 && this.page > 0) { this.page--; return true; }
        if (verticalAmount < 0 && this.page < this.maxPage) { this.page++; return true; }
        return true;
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { this.search.setFocused(!this.search.isFocused()); return true; }
        if (this.search.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) { closeSelector(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char codePoint, int modifiers) { return this.search.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers); }
    @Override public void onClose() { closeSelector(); }

    private void renderHoverButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        int[][] buttons = {{152,18,0}, {152,36,16}, {152,90,32}, {134,108,48}, {8,108,64}};
        for (int[] button : buttons) if (inside(mouseX, mouseY, button[0], button[1], 16, 16)) graphics.blit(TEXTURE, this.left + button[0], this.top + button[1], 176, button[2], 16, 16);
    }

    private void rebuild(String query) {
        this.recipes.clear();
        String normalized = query.toLowerCase(Locale.ROOT);
        for (RecipeHolder<PrecisionAssemblerRecipe> holder : this.allRecipes) if (normalized.isBlank() || matches(holder, normalized)) this.recipes.add(holder);
        this.maxPage = Math.max(0, (int) Math.ceil((this.recipes.size() - 40) / 8.0D));
        this.page = Mth.clamp(this.page, 0, this.maxPage);
    }

    private static boolean matches(RecipeHolder<PrecisionAssemblerRecipe> holder, String query) {
        PrecisionAssemblerRecipe recipe = holder.value();
        if (holder.id().toString().toLowerCase(Locale.ROOT).contains(query) || recipe.group().toLowerCase(Locale.ROOT).contains(query)) return true;
        for (PrecisionAssemblerRecipe.ChanceOutput output : recipe.outputs()) if (output.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        for (PrecisionAssemblerRecipe.CountedIngredient ingredient : recipe.ingredients()) for (ItemStack stack : ingredient.ingredient().getItems()) if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        return false;
    }

    private int recipeAt(int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, 7, 17, 144, 90)) return -1;
        for (int index = this.page * 8; index < Math.min(this.page * 8 + 40, this.recipes.size()); index++) {
            int visible = index - this.page * 8;
            if (inside(mouseX, mouseY, 7 + 18 * (visible % 8), 17 + 18 * (visible / 8), 18, 18)) return index;
        }
        return -1;
    }

    @Nullable private RecipeHolder<PrecisionAssemblerRecipe> selectedRecipe() {
        if (this.selected == null) return null;
        return this.allRecipes.stream().filter(holder -> holder.id().equals(this.selected)).findFirst().orElse(null);
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) { return this.left + x <= mouseX && this.left + x + width > mouseX && this.top + y <= mouseY && this.top + y + height > mouseY; }
    private void click() { if (this.minecraft != null) this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)); }
    private void closeSelector() {
        if (this.dirty && this.minecraft != null && this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.selected == null ? 0 : this.menu.buttonIdForRecipe(this.selected));
        this.dirty = false;
        if (this.minecraft != null) this.minecraft.setScreen(this.previous);
    }

    static List<Component> describeRecipe(RecipeHolder<PrecisionAssemblerRecipe> holder) {
        PrecisionAssemblerRecipe recipe = holder.value();
        List<Component> lines = new ArrayList<>();
        for (PrecisionAssemblerRecipe.ChanceOutput output : recipe.outputs()) lines.add(output.stack().getHoverName().copy().withStyle(ChatFormatting.YELLOW).append(Component.literal(" " + output.weight() + "%").withStyle(ChatFormatting.GRAY)));
        lines.add(Component.translatable("info.reinhardtshbm.template_in").withStyle(ChatFormatting.GRAY));
        for (PrecisionAssemblerRecipe.CountedIngredient ingredient : recipe.ingredients()) {
            ItemStack display = displayIngredient(ingredient);
            if (!display.isEmpty()) lines.add(Component.literal(" - ").withStyle(ChatFormatting.GRAY).append(display.getHoverName()).append(Component.literal(" x" + ingredient.count())));
        }
        lines.add(Component.translatable("info.reinhardtshbm.template_out").withStyle(ChatFormatting.GRAY));
        for (PrecisionAssemblerRecipe.ChanceOutput output : recipe.outputs()) {
            lines.add(Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                    .append(output.stack().getHoverName())
                    .append(Component.literal(" x" + output.stack().getCount())));
        }
        lines.add(Component.translatable("info.reinhardtshbm.template_power", recipe.power()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("info.reinhardtshbm.template_duration", recipe.duration()).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    static ItemStack displayIngredient(PrecisionAssemblerRecipe.CountedIngredient ingredient) {
        if (ingredient.ingredient().hasNoItems()) return ItemStack.EMPTY;
        ItemStack display = Arrays.stream(ingredient.ingredient().getItems())
                .findFirst()
                .orElse(ItemStack.EMPTY)
                .copy();
        if (!display.isEmpty()) display.setCount(ingredient.count());
        return display;
    }
}
