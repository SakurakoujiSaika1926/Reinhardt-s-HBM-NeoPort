package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.ChemicalPlantBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.ChemicalPlantMenu;
import com.reinhardt.hbm.recipe.ChemicalPlantRecipe;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChemicalPlantScreen extends AbstractContainerScreen<ChemicalPlantMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_chemplant.png");
    private static final float GHOST_ITEM_ALPHA = 0.20F;
    private static final float GHOST_SLOT_OVERLAY_ALPHA = 0.65F;
    private static final int GHOST_VEIL_COLOR = 0x55D8D3B8;
    private static final int GHOST_COUNT_COLOR = 0xB0FFFFFF;
    private static final int GHOST_COUNT_SHADOW = 0x60000000;

    public ChemicalPlantScreen(ChemicalPlantMenu menu, Inventory playerInventory, Component title) {
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        for (int index = 0; index < 3; index++) {
            if (isHovering(8 + index * 18, 18, 16, 34, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font, fluidTooltip(index), mouseX, mouseY);
                return;
            }
            if (isHovering(80 + index * 18, 18, 16, 34, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font, fluidTooltip(index + 3), mouseX, mouseY);
                return;
            }
        }

        if (isHovering(152, 18, 16, 61, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.energy(),
                    this.menu.energyCapacity()
            )), mouseX, mouseY);
        }

        if (isHovering(62, 126, 70, 16, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.progress",
                    this.menu.progress(),
                    this.menu.workTime()
            )), mouseX, mouseY);
        }

        if (isHovering(7, 125, 18, 18, mouseX, mouseY)) {
            Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe = this.menu.selectedRecipe();
            if (selectedRecipe.isPresent()) {
                guiGraphics.renderComponentTooltip(this.font, ChemicalPlantRecipeSelectorScreen.describeRecipe(selectedRecipe.get()), mouseX, mouseY);
            } else {
                guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int energyPixels = this.menu.energyScaled(61);
        if (energyPixels > 0) {
            guiGraphics.blit(
                    TEXTURE,
                    this.leftPos + 152,
                    this.topPos + 79 - energyPixels,
                    176,
                    61 - energyPixels,
                    16,
                    energyPixels
            );
        }

        int progressPixels = this.menu.progressScaled(70);
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 126, 176, 61, progressPixels, 16);
        }

        if (this.menu.isWorking()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 195, 0, 3, 6);
            guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 195, 0, 3, 6);
        } else if (this.menu.hasRecipe()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 192, 0, 3, 6);
            if (this.menu.energy() >= this.menu.currentDemand()) {
                guiGraphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 192, 0, 3, 6);
            }
        }

        renderTemplateIcon(guiGraphics);
        renderRecipeGhosts(guiGraphics);
        renderFluidTanks(guiGraphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(7, 125, 18, 18, mouseX, mouseY)) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new ChemicalPlantRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderTemplateIcon(GuiGraphics guiGraphics) {
        ItemStack icon = this.menu.selectedRecipe()
                .map(holder -> holder.value().displayIcon())
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        guiGraphics.renderItem(icon, this.leftPos + 8, this.topPos + 126);
        guiGraphics.renderItemDecorations(this.font, icon, this.leftPos + 8, this.topPos + 126);
    }

    private void renderRecipeGhosts(GuiGraphics guiGraphics) {
        Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe = this.menu.selectedRecipe();
        if (selectedRecipe.isEmpty()) {
            return;
        }

        List<ChemicalPlantRecipe.CountedIngredient> ingredients = selectedRecipe.get().value().inputItems();
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            int slotIndex = ChemicalPlantBlockEntity.SOLID_INPUT_START + ingredientIndex;
            if (slotIndex >= ChemicalPlantBlockEntity.SOLID_INPUT_END) {
                return;
            }
            Slot slot = this.menu.getSlot(slotIndex);
            if (slot.hasItem()) {
                continue;
            }

            ItemStack ghost = ChemicalPlantRecipeSelectorScreen.displayIngredient(ingredients.get(ingredientIndex));
            if (!ghost.isEmpty()) {
                renderGhostItem(guiGraphics, ghost, this.leftPos + slot.x, this.topPos + slot.y, slot.x, slot.y);
            }
        }
    }

    private void renderGhostItem(GuiGraphics guiGraphics, ItemStack ghost, int x, int y, int slotU, int slotV) {
        GhostItemRenderer.render(guiGraphics, ghost, x, y, GHOST_ITEM_ALPHA);

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, GHOST_SLOT_OVERLAY_ALPHA);
        guiGraphics.blit(TEXTURE, x, y, slotU, slotV, 16, 16);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.fill(x, y, x + 16, y + 16, GHOST_VEIL_COLOR);
        renderGhostCount(guiGraphics, ghost, x, y);
    }

    private void renderGhostCount(GuiGraphics guiGraphics, ItemStack ghost, int x, int y) {
        if (ghost.getCount() <= 1) {
            return;
        }

        String count = String.valueOf(ghost.getCount());
        int textX = x + 17 - this.font.width(count);
        int textY = y + 9;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
        guiGraphics.drawString(this.font, count, textX + 1, textY + 1, GHOST_COUNT_SHADOW, false);
        guiGraphics.drawString(this.font, count, textX, textY, GHOST_COUNT_COLOR, false);
        guiGraphics.pose().popPose();
    }

    private void renderFluidTanks(GuiGraphics guiGraphics) {
        for (int index = 0; index < 3; index++) {
            drawFluid(guiGraphics, 8 + index * 18, 52, 16, this.menu.tankScaled(index, 34), this.menu.tankFluid(index));
            drawFluid(guiGraphics, 80 + index * 18, 52, 16, this.menu.tankScaled(index + 3, 34), this.menu.tankFluid(index + 3));
        }
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }

        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            int color = 0xFF000000 | fluid.color();
            graphics.fill(
                    this.leftPos + x,
                    this.topPos + bottomY - height,
                    this.leftPos + x + width,
                    this.topPos + bottomY,
                    color
            );
            return;
        }

        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);

        int top = bottomY - height;
        for (int tileX = 0; tileX < width; tileX += 16) {
            int tileWidth = Math.min(16, width - tileX);
            for (int tileY = 0; tileY < height; tileY += 16) {
                int tileHeight = Math.min(16, height - tileY);
                graphics.blit(
                        texture,
                        this.leftPos + x + tileX,
                        this.topPos + top + tileY,
                        0,
                        16 - tileHeight,
                        tileWidth,
                        tileHeight,
                        16,
                        16
                );
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private List<Component> fluidTooltip(int tank) {
        return HbmFluidTooltip.forTank(
                this.menu.tankFluid(tank),
                this.menu.tankAmount(tank),
                this.menu.tankCapacity(),
                this.menu.tankPressure(tank)
        );
    }
}
