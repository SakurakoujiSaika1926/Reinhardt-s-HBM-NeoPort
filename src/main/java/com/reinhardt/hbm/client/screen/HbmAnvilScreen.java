package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.HbmAnvilMenu;
import com.reinhardt.hbm.recipe.anvil.AnvilConstructionRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HbmAnvilScreen extends AbstractContainerScreen<HbmAnvilMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_anvil.png");

    private final List<AnvilConstructionRecipe> originRecipes;
    private final List<AnvilConstructionRecipe> recipes = new ArrayList<>();
    private EditBox search;
    private int page;
    private int pageCount;
    private int selection = -1;
    private int detailWidth = 1;

    public HbmAnvilScreen(HbmAnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.originRecipes = List.copyOf(menu.constructionRecipes());
        regenerateRecipes("");
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 61 - this.font.width(this.title) / 2;
        this.search = new EditBox(this.font, this.leftPos + 10, this.topPos + 111, 84, 12, Component.translatable("container.reinhardtshbm.anvil.search"));
        this.search.setTextColor(0xFFFFFF);
        this.search.setTextColorUneditable(0xFFFFFF);
        this.search.setBordered(false);
        this.search.setMaxLength(25);
        this.search.setResponder(this::regenerateRecipes);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.search.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        int hoveredRecipe = recipeAt(mouseX, mouseY);
        if (hoveredRecipe >= 0) {
            ItemStack display = this.recipes.get(hoveredRecipe).getDisplay();
            if (!display.isEmpty()) {
                guiGraphics.renderTooltip(this.font, display, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int slide = Mth.clamp(this.detailWidth - 42, 0, 1000);
        int multiplier = 1;
        while (slide >= 51 * multiplier) {
            guiGraphics.blit(TEXTURE, this.leftPos + 125 + 51 * multiplier, this.topPos + 17, 125, 17, 54, 108);
            multiplier++;
        }
        guiGraphics.blit(TEXTURE, this.leftPos + 125 + slide, this.topPos + 17, 125, 17, 54, 108);

        if (this.search != null && this.search.isFocused()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 108, 168, 222, 88, 16);
        }
        if (isInside(mouseX, mouseY, 7, 71, 9, 36)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 7, this.topPos + 71, 176, 186, 9, 36);
        }
        if (isInside(mouseX, mouseY, 106, 71, 9, 36)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 106, this.topPos + 71, 185, 186, 9, 36);
        }
        if (isInside(mouseX, mouseY, 52, 53, 18, 18)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 52, this.topPos + 53, 176, 150, 18, 18);
        }
        if (isInside(mouseX, mouseY, 97, 107, 18, 18)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 97, this.topPos + 107, 176, 168, 18, 18);
        }

        for (int recipeIndex = this.page * 2; recipeIndex < this.page * 2 + 10 && recipeIndex < this.recipes.size(); recipeIndex++) {
            int visibleIndex = recipeIndex - this.page * 2;
            int x = 16 + 18 * (visibleIndex / 2);
            int y = 71 + 18 * (visibleIndex % 2);
            AnvilConstructionRecipe recipe = this.recipes.get(recipeIndex);
            ItemStack display = recipe.getDisplay();
            if (!display.isEmpty()) {
                guiGraphics.renderItem(display, this.leftPos + x + 1, this.topPos + y + 1);
                guiGraphics.renderItemDecorations(this.font, display, this.leftPos + x + 1, this.topPos + y + 1);
            }
            guiGraphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, 18 + 18 * recipe.overlay().ordinal(), 222, 18, 18);
            if (this.selection == recipeIndex) {
                guiGraphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, 0, 222, 18, 18);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        if (this.selection < 0 || this.selection >= this.recipes.size()) {
            this.detailWidth = 1;
            return;
        }

        List<Component> lines = this.recipes.get(this.selection).describe();
        int longest = 0;
        for (Component line : lines) {
            longest = Math.max(longest, this.font.width(line.getString()));
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
        int y = 50;
        for (Component line : lines) {
            guiGraphics.drawString(this.font, line, 260, y, 0xFFFFFF, false);
            y += 9;
        }
        guiGraphics.pose().popPose();
        this.detailWidth = (int) (longest * 0.5F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.search.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int x = (int) mouseX;
        int y = (int) mouseY;
        if (isInside(x, y, 7, 71, 9, 36)) {
            playClick();
            if (this.page > 0) {
                this.page--;
            }
            return true;
        }
        if (isInside(x, y, 106, 71, 9, 36)) {
            playClick();
            if (this.page < this.pageCount) {
                this.page++;
            }
            return true;
        }
        if (isInside(x, y, 52, 53, 18, 18)) {
            if (this.selection >= 0 && this.minecraft != null && this.minecraft.gameMode != null) {
                int buttonId = selectedRecipeButtonId();
                if (buttonId >= 0) {
                    playClick();
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
                }
            }
            return true;
        }
        if (isInside(x, y, 97, 107, 18, 18)) {
            playClick();
            regenerateRecipes(this.search.getValue());
            return true;
        }

        int recipeIndex = recipeAt(x, y);
        if (recipeIndex >= 0) {
            playClick();
            this.selection = this.selection == recipeIndex ? -1 : recipeIndex;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInside((int) mouseX, (int) mouseY, 0, 0, this.imageWidth, this.imageHeight)) {
            if (scrollY > 0.0D && this.page > 0) {
                this.page--;
                return true;
            }
            if (scrollY < 0.0D && this.page < this.pageCount) {
                this.page++;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.search.keyPressed(keyCode, scanCode, modifiers)) {
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

    private void regenerateRecipes(String query) {
        this.recipes.clear();
        String normalized = query.toLowerCase(Locale.ROOT);
        for (AnvilConstructionRecipe recipe : this.originRecipes) {
            if (normalized.isBlank() || matchesSearch(recipe, normalized)) {
                this.recipes.add(recipe);
            }
        }
        this.page = 0;
        this.selection = -1;
        this.pageCount = Math.max(0, (int) Math.ceil((this.recipes.size() - 10) / 2.0D));
    }

    private static boolean matchesSearch(AnvilConstructionRecipe recipe, String query) {
        for (var input : recipe.inputs()) {
            for (ItemStack stack : input.searchStacks()) {
                if (stackMatchesSearch(stack, query)) {
                    return true;
                }
            }
        }
        for (var output : recipe.outputs()) {
            if (stackMatchesSearch(output.stack(), query)) {
                return true;
            }
        }
        return false;
    }

    private static boolean stackMatchesSearch(ItemStack stack, String query) {
        return !stack.isEmpty() && stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private int recipeAt(int mouseX, int mouseY) {
        for (int recipeIndex = this.page * 2; recipeIndex < this.page * 2 + 10 && recipeIndex < this.recipes.size(); recipeIndex++) {
            int visibleIndex = recipeIndex - this.page * 2;
            int x = 16 + 18 * (visibleIndex / 2);
            int y = 71 + 18 * (visibleIndex % 2);
            if (isInside(mouseX, mouseY, x, y, 18, 18)) {
                return recipeIndex;
            }
        }
        return -1;
    }

    private int selectedRecipeButtonId() {
        if (this.selection < 0 || this.selection >= this.recipes.size()) {
            return -1;
        }

        int originIndex = this.originRecipes.indexOf(this.recipes.get(this.selection));
        if (originIndex < 0) {
            return -1;
        }
        return originIndex * 2 + (Screen.hasShiftDown() ? 1 : 0);
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
}
