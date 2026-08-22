package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.AmmoPressBlockEntity;
import com.reinhardt.hbm.menu.AmmoPressMenu;
import com.reinhardt.hbm.recipe.AmmoPressRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class AmmoPressScreen extends AbstractContainerScreen<AmmoPressMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_ammo_press.png");
    private static final float GHOST_ALPHA = 0.20F;
    private static final int RECIPES_PER_PAGE = 12;

    private EditBox search;
    private List<RecipeHolder<AmmoPressRecipe>> filteredRecipes = List.of();
    private int page;

    public AmmoPressScreen(AmmoPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.inventoryLabelY = 106;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.search = new EditBox(this.font, this.leftPos + 10, this.topPos + 75, 66, 12, Component.translatable("container.reinhardtshbm.ammo_press.search"));
        this.search.setBordered(false);
        this.search.setTextColor(0xFFFFFF);
        this.search.setResponder(text -> {
            this.page = 0;
            refreshRecipes();
        });
        addRenderableWidget(this.search);
        refreshRecipes();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshRecipes();
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        RecipeHolder<AmmoPressRecipe> hovered = recipeAt(mouseX, mouseY);
        if (hovered != null) {
            graphics.renderTooltip(this.font, hovered.value().result(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (isHovering(7, 17, 9, 54, mouseX, mouseY) && this.page > 0) {
            graphics.blit(TEXTURE, this.leftPos + 7, this.topPos + 17, 176, 0, 9, 54);
        }
        if (isHovering(88, 17, 9, 54, mouseX, mouseY) && this.page < maxPage()) {
            graphics.blit(TEXTURE, this.leftPos + 88, this.topPos + 17, 185, 0, 9, 54);
        }
        if (this.search.isFocused()) {
            graphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 72, 176, 54, 70, 16);
        }

        renderRecipeButtons(graphics);
        renderGhostInputs(graphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.search.mouseClicked(mouseX, mouseY, button)) {
            setFocused(this.search);
            return true;
        }
        if (isHovering(7, 17, 9, 54, mouseX, mouseY) && this.page > 0) {
            this.page--;
            return true;
        }
        if (isHovering(88, 17, 9, 54, mouseX, mouseY) && this.page < maxPage()) {
            this.page++;
            return true;
        }

        RecipeHolder<AmmoPressRecipe> recipe = recipeAt(mouseX, mouseY);
        if (recipe != null && this.minecraft != null && this.minecraft.gameMode != null) {
            int id = this.menu.buttonIdForRecipe(recipe.id());
            if (id > 0) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0.0D && this.page > 0) {
            this.page--;
            return true;
        }
        if (scrollY < 0.0D && this.page < maxPage()) {
            this.page++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void refreshRecipes() {
        String filter = this.search == null ? "" : this.search.getValue().toLowerCase(Locale.ROOT);
        this.filteredRecipes = this.menu.recipes().stream()
                .filter(holder -> holder.value().result().getHoverName().getString().toLowerCase(Locale.ROOT).contains(filter))
                .toList();
        this.page = Math.min(this.page, maxPage());
    }

    private void renderRecipeButtons(GuiGraphics graphics) {
        // The legacy GUI advances three entries per page while showing twelve.
        int start = this.page * 3;
        int selected = this.menu.selectedRecipeIndex();
        for (int local = 0; local < RECIPES_PER_PAGE; local++) {
            int index = start + local;
            if (index >= this.filteredRecipes.size()) {
                break;
            }

            int column = local / 3;
            int row = local % 3;
            int x = this.leftPos + 16 + column * 18;
            int y = this.topPos + 17 + row * 18;
            RecipeHolder<AmmoPressRecipe> recipe = this.filteredRecipes.get(index);

            graphics.renderItem(recipe.value().result(), x + 1, y + 1);
            graphics.renderItemDecorations(this.font, recipe.value().result(), x + 1, y + 1);
            boolean isSelected = selected >= 0 && this.menu.recipes().get(selected).id().equals(recipe.id());
            graphics.blit(TEXTURE, x, y, isSelected ? 194 : 212, 0, 18, 18);
        }
    }

    private void renderGhostInputs(GuiGraphics graphics) {
        Optional<RecipeHolder<AmmoPressRecipe>> selected = this.menu.selectedRecipe();
        if (selected.isEmpty()) {
            return;
        }
        for (int index = 0; index < 9; index++) {
            AmmoPressRecipe.SlotIngredient required = selected.get().value().input().get(index);
            if (required.isEmpty()) {
                continue;
            }

            Slot slot = this.menu.getSlot(index);
            if (slot.hasItem()) {
                continue;
            }

            ItemStack[] candidates = required.ingredient().orElseThrow().getItems();
            if (candidates.length == 0) {
                continue;
            }
            ItemStack ghost = candidates[(int) ((System.currentTimeMillis() / 1000L) % candidates.length)].copy();
            ghost.setCount(required.count());
            GhostItemRenderer.render(graphics, ghost, this.leftPos + slot.x, this.topPos + slot.y, GHOST_ALPHA);
        }
    }

    private RecipeHolder<AmmoPressRecipe> recipeAt(double mouseX, double mouseY) {
        int start = this.page * 3;
        for (int local = 0; local < RECIPES_PER_PAGE; local++) {
            int index = start + local;
            if (index >= this.filteredRecipes.size()) {
                return null;
            }
            int x = 16 + (local / 3) * 18;
            int y = 17 + (local % 3) * 18;
            if (isHovering(x, y, 18, 18, mouseX, mouseY)) {
                return this.filteredRecipes.get(index);
            }
        }
        return null;
    }

    private int maxPage() {
        return Math.max(0, (int) Math.ceil((this.filteredRecipes.size() - RECIPES_PER_PAGE) / 3.0D));
    }
}
