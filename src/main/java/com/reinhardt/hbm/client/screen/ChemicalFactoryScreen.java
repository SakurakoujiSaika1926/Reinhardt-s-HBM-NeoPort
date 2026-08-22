package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.ChemicalFactoryBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.ChemicalFactoryMenu;
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

import java.util.List;
import java.util.Optional;

public class ChemicalFactoryScreen extends AbstractContainerScreen<ChemicalFactoryMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_chemical_factory.png");
    private static final float GHOST_ITEM_ALPHA = 0.20F;
    private static final float GHOST_SLOT_OVERLAY_ALPHA = 0.65F;
    private static final int GHOST_VEIL_COLOR = 0x55D8D3B8;
    private static final int GHOST_COUNT_COLOR = 0xB0FFFFFF;
    private static final int GHOST_COUNT_SHADOW = 0x60000000;

    public ChemicalFactoryScreen(ChemicalFactoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 248;
        this.imageHeight = 216;
        this.inventoryLabelX = 26;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 106 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        for (int module = 0; module < ChemicalFactoryBlockEntity.MODULE_COUNT; module++) {
            for (int index = 0; index < ChemicalFactoryBlockEntity.TANKS_PER_MODULE; index++) {
                int tank = ChemicalFactoryBlockEntity.tankIndex(module, index);
                if (isHovering(60 + index * 5, 20 + module * 22, 3, 16, mouseX, mouseY)) {
                    guiGraphics.renderComponentTooltip(this.font, fluidTooltip(tank), mouseX, mouseY);
                    return;
                }
                int outputTank = ChemicalFactoryBlockEntity.MODULE_COUNT * ChemicalFactoryBlockEntity.TANKS_PER_MODULE + tank;
                if (isHovering(189 + index * 5, 20 + module * 22, 3, 16, mouseX, mouseY)) {
                    guiGraphics.renderComponentTooltip(this.font, fluidTooltip(outputTank), mouseX, mouseY);
                    return;
                }
            }
            if (isHovering(74, 19 + module * 22, 18, 18, mouseX, mouseY)) {
                Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe = this.menu.selectedRecipe(module);
                if (selectedRecipe.isPresent()) {
                    guiGraphics.renderComponentTooltip(this.font, ChemicalPlantRecipeSelectorScreen.describeRecipe(selectedRecipe.get()), mouseX, mouseY);
                } else {
                    guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
                }
                return;
            }
        }

        if (isHovering(224, 125, 7, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(24), mouseX, mouseY);
            return;
        }
        if (isHovering(233, 125, 7, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(25), mouseX, mouseY);
            return;
        }
        if (isHovering(224, 18, 16, 68, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.energy(),
                    this.menu.energyCapacity()
            )), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 248, 116);
        guiGraphics.blit(TEXTURE, this.leftPos + 18, this.topPos + 116, 18, 116, 230, 100);

        int energyPixels = this.menu.energyScaled(68);
        if (energyPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 224, this.topPos + 86 - energyPixels, 0, 184 - energyPixels, 16, energyPixels);
        }

        for (int module = 0; module < ChemicalFactoryBlockEntity.MODULE_COUNT; module++) {
            int progressPixels = this.menu.progressScaled(module, 22);
            if (progressPixels > 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + 113, this.topPos + 29 + module * 22, 0, 216, progressPixels, 6);
            }

            if (this.menu.isWorking(module)) {
                guiGraphics.blit(TEXTURE, this.leftPos + 113, this.topPos + 21 + module * 22, 4, 222, 4, 4);
                guiGraphics.blit(TEXTURE, this.leftPos + 121, this.topPos + 21 + module * 22, 4, 222, 4, 4);
            } else if (this.menu.hasRecipe(module)) {
                guiGraphics.blit(TEXTURE, this.leftPos + 113, this.topPos + 21 + module * 22, 0, 222, 4, 4);
                if (this.menu.energy() >= this.menu.currentDemand(module)) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 121, this.topPos + 21 + module * 22, 0, 222, 4, 4);
                }
            }

            renderTemplateIcon(guiGraphics, module);
            renderRecipeGhosts(guiGraphics, module);
            for (int index = 0; index < ChemicalFactoryBlockEntity.TANKS_PER_MODULE; index++) {
                int tank = ChemicalFactoryBlockEntity.tankIndex(module, index);
                drawFluid(guiGraphics, 60 + index * 5, 36 + module * 22, 3, this.menu.tankScaled(tank, 16), this.menu.tankFluid(tank));
                int outputTank = ChemicalFactoryBlockEntity.MODULE_COUNT * ChemicalFactoryBlockEntity.TANKS_PER_MODULE + tank;
                drawFluid(guiGraphics, 189 + index * 5, 36 + module * 22, 3, this.menu.tankScaled(outputTank, 16), this.menu.tankFluid(outputTank));
            }
        }

        drawFluid(guiGraphics, 224, 177, 7, this.menu.tankScaled(24, 52), this.menu.tankFluid(24));
        drawFluid(guiGraphics, 233, 177, 7, this.menu.tankScaled(25, 52), this.menu.tankFluid(25));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int module = 0; module < ChemicalFactoryBlockEntity.MODULE_COUNT; module++) {
            if (isHovering(74, 19 + module * 22, 18, 18, mouseX, mouseY)) {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new ChemicalFactoryRecipeSelectorScreen(this, this.menu, module));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderTemplateIcon(GuiGraphics guiGraphics, int module) {
        ItemStack icon = this.menu.selectedRecipe(module)
                .map(holder -> holder.value().displayIcon())
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        guiGraphics.renderItem(icon, this.leftPos + 75, this.topPos + 20 + module * 22);
        guiGraphics.renderItemDecorations(this.font, icon, this.leftPos + 75, this.topPos + 20 + module * 22);
    }

    private void renderRecipeGhosts(GuiGraphics guiGraphics, int module) {
        Optional<RecipeHolder<ChemicalPlantRecipe>> selectedRecipe = this.menu.selectedRecipe(module);
        if (selectedRecipe.isEmpty()) {
            return;
        }
        List<ChemicalPlantRecipe.CountedIngredient> ingredients = selectedRecipe.get().value().inputItems();
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            int slotIndex = ChemicalFactoryBlockEntity.inputSlotStart(module) + ingredientIndex;
            if (slotIndex >= ChemicalFactoryBlockEntity.inputSlotStart(module) + ChemicalFactoryBlockEntity.MODULE_ITEM_COUNT) {
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

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, 0xFF000000 | fluid.color());
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
                graphics.blit(texture, this.leftPos + x + tileX, this.topPos + top + tileY, 0, 16 - tileHeight, tileWidth, tileHeight, 16, 16);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private List<Component> fluidTooltip(int tank) {
        return HbmFluidTooltip.forTank(
                this.menu.tankFluid(tank),
                this.menu.tankAmount(tank),
                this.menu.tankCapacity(tank),
                this.menu.tankPressure(tank)
        );
    }
}
