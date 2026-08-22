package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.PurexBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.PurexMenu;
import com.reinhardt.hbm.recipe.PurexRecipe;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

public class PurexScreen extends AbstractContainerScreen<PurexMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_purex.png");
    private static final float GHOST_ITEM_ALPHA = 0.20F;
    private static final int GHOST_VEIL_COLOR = 0x55D8D3B8;

    public PurexScreen(PurexMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 70 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        for (int tank = 0; tank < 3; tank++) {
            int x = 8 + tank * 18;
            if (isHovering(x, 18, 16, 52, mouseX, mouseY)) {
                graphics.renderComponentTooltip(this.font, tankTooltip(tank), mouseX, mouseY);
                return;
            }
        }
        if (isHovering(116, 36, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, tankTooltip(3), mouseX, mouseY);
            return;
        }
        if (isHovering(152, 18, 16, 61, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy", this.menu.energy(), this.menu.capacity())), mouseX, mouseY);
            return;
        }
        if (isHovering(62, 126, 70, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.progress", this.menu.progress(), this.menu.workTime())), mouseX, mouseY);
            return;
        }
        if (isHovering(7, 125, 18, 18, mouseX, mouseY)) {
            Optional<RecipeHolder<PurexRecipe>> selected = this.menu.selectedRecipe();
            graphics.renderComponentTooltip(this.font,
                    selected.map(PurexRecipeSelectorScreen::describeRecipe)
                            .orElseGet(() -> List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW))),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int energy = this.menu.energyScaled(61);
        if (energy > 0) {
            graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 79 - energy, 176, 61 - energy, 16, energy);
        }
        int progress = this.menu.progressScaled(70);
        if (progress > 0) {
            graphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 126, 176, 61, progress, 16);
        }

        if (this.menu.working()) {
            graphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 195, 0, 3, 6);
            graphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 195, 0, 3, 6);
        } else if (this.menu.hasRecipe()) {
            graphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 192, 0, 3, 6);
            if (this.menu.energy() >= this.menu.demand()) {
                graphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 192, 0, 3, 6);
            }
        }

        renderTemplateIcon(graphics);
        renderRecipeGhosts(graphics);
        for (int tank = 0; tank < 3; tank++) {
            drawFluid(graphics, 8 + tank * 18, 70, 16, this.menu.tankScaled(tank, 52), fluid(tank));
        }
        drawFluid(graphics, 116, 88, 16, this.menu.tankScaled(3, 52), fluid(3));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(7, 125, 18, 18, mouseX, mouseY)) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new PurexRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderTemplateIcon(GuiGraphics graphics) {
        ItemStack icon = this.menu.selectedRecipe()
                .map(PurexRecipeSelectorScreen::displayIcon)
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        graphics.renderItem(icon, this.leftPos + 8, this.topPos + 126);
        graphics.renderItemDecorations(this.font, icon, this.leftPos + 8, this.topPos + 126);
    }

    private void renderRecipeGhosts(GuiGraphics graphics) {
        Optional<RecipeHolder<PurexRecipe>> selected = this.menu.selectedRecipe();
        if (selected.isEmpty()) {
            return;
        }
        List<PurexRecipe.CountedIngredient> ingredients = selected.get().value().inputItems();
        for (int index = 0; index < ingredients.size() && index < 3; index++) {
            Slot slot = this.menu.getSlot(PurexBlockEntity.INPUT_START + index);
            if (slot.hasItem()) {
                continue;
            }
            ItemStack ghost = PurexRecipeSelectorScreen.displayIngredient(ingredients.get(index));
            if (!ghost.isEmpty()) {
                int x = this.leftPos + slot.x;
                int y = this.topPos + slot.y;
                GhostItemRenderer.render(graphics, ghost, x, y, GHOST_ITEM_ALPHA);
                graphics.fill(x, y, x + 16, y + 16, GHOST_VEIL_COLOR);
            }
        }
    }

    private HbmFluidDefinition fluid(int tank) {
        return HbmFluids.byOldId(this.menu.tankFluidId(tank)).orElse(HbmFluids.none());
    }

    private List<Component> tankTooltip(int tank) {
        return HbmFluidTooltip.forTank(fluid(tank), this.menu.tankAmount(tank), PurexBlockEntity.TANK_CAPACITY, this.menu.tankPressure(tank));
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width,
                    this.topPos + bottomY, 0xFF000000 | fluid.color());
            return;
        }
        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        int top = bottomY - height;
        for (int tileY = 0; tileY < height; tileY += 16) {
            int tileHeight = Math.min(16, height - tileY);
            graphics.blit(texture, this.leftPos + x, this.topPos + top + tileY, 0, 16 - tileHeight,
                    width, tileHeight, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
