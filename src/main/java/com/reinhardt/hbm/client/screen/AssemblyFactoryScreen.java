package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.AssemblyFactoryBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.AssemblyFactoryMenu;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.HbmFluidTooltip;
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

public class AssemblyFactoryScreen extends AbstractContainerScreen<AssemblyFactoryMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_assembly_factory.png");
    private static final float GHOST_ITEM_ALPHA = 0.20F;
    private static final float GHOST_SLOT_OVERLAY_ALPHA = 0.65F;
    private static final int GHOST_VEIL_COLOR = 0x55D8D3B8;
    private static final int GHOST_COUNT_COLOR = 0xB0FFFFFF;
    private static final int GHOST_COUNT_SHADOW = 0x60000000;

    public AssemblyFactoryScreen(AssemblyFactoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 240;
        this.inventoryLabelX = 33;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 113 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        for (int module = 0; module < AssemblyFactoryBlockEntity.MODULE_COUNT; module++) {
            int left = (module % 2) * 109;
            int top = (module / 2) * 56;
            if (isHovering(105 + left, 20 + top, 5, 32, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font, fluidTooltip(module), mouseX, mouseY);
                return;
            }
            if (isHovering(105 + left, 54 + top, 5, 16, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font, fluidTooltip(module + AssemblyFactoryBlockEntity.MODULE_COUNT), mouseX, mouseY);
                return;
            }
            if (isHovering(6 + left, 53 + top, 18, 18, mouseX, mouseY)) {
                Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe = this.menu.selectedRecipe(module);
                if (selectedRecipe.isPresent()) {
                    guiGraphics.renderComponentTooltip(this.font, AssemblyRecipeSelectorScreen.describeRecipe(selectedRecipe.get()), mouseX, mouseY);
                } else {
                    guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.setRecipe")), mouseX, mouseY);
                }
                return;
            }
        }

        if (isHovering(232, 149, 7, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(8), mouseX, mouseY);
            return;
        }
        if (isHovering(241, 149, 7, 52, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, fluidTooltip(9), mouseX, mouseY);
            return;
        }
        if (isHovering(234, 18, 16, 92, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.energy(),
                    this.menu.energyCapacity()
            )), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 256, 140);
        guiGraphics.blit(TEXTURE, this.leftPos + 25, this.topPos + 140, 25, 140, 231, 100);

        int energyPixels = this.menu.energyScaled(92);
        if (energyPixels > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 234, this.topPos + 110 - energyPixels, 0, 232 - energyPixels, 16, energyPixels);
        }

        for (int module = 0; module < AssemblyFactoryBlockEntity.MODULE_COUNT; module++) {
            int left = (module % 2) * 109;
            int top = (module / 2) * 56;
            int progressPixels = this.menu.progressScaled(module, 37);
            if (progressPixels > 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + 45 + left, this.topPos + 63 + top, 0, 240, progressPixels, 6);
            }

            if (this.menu.isWorking(module)) {
                guiGraphics.blit(TEXTURE, this.leftPos + 45 + left, this.topPos + 55 + top, 4, 236, 4, 4);
                guiGraphics.blit(TEXTURE, this.leftPos + 53 + left, this.topPos + 55 + top, 4, 236, 4, 4);
            } else if (this.menu.hasRecipe(module)) {
                guiGraphics.blit(TEXTURE, this.leftPos + 45 + left, this.topPos + 55 + top, 0, 236, 4, 4);
                if (this.menu.energy() >= this.menu.currentDemand(module)) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 53 + left, this.topPos + 55 + top, 0, 236, 4, 4);
                }
            }

            renderTemplateIcon(guiGraphics, module, left, top);
            renderRecipeGhosts(guiGraphics, module);
            drawFluid(guiGraphics, 105 + left, 52 + top, 5, this.menu.tankScaled(module, 32), this.menu.tankFluid(module));
            drawFluid(guiGraphics, 105 + left, 70 + top, 5, this.menu.tankScaled(module + AssemblyFactoryBlockEntity.MODULE_COUNT, 16), this.menu.tankFluid(module + AssemblyFactoryBlockEntity.MODULE_COUNT));
        }

        drawFluid(guiGraphics, 232, 201, 7, this.menu.tankScaled(8, 52), this.menu.tankFluid(8));
        drawFluid(guiGraphics, 241, 201, 7, this.menu.tankScaled(9, 52), this.menu.tankFluid(9));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int module = 0; module < AssemblyFactoryBlockEntity.MODULE_COUNT; module++) {
            int left = (module % 2) * 109;
            int top = (module / 2) * 56;
            if (isHovering(6 + left, 53 + top, 18, 18, mouseX, mouseY)) {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new AssemblyFactoryRecipeSelectorScreen(this, this.menu, module));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderTemplateIcon(GuiGraphics guiGraphics, int module, int left, int top) {
        ItemStack icon = this.menu.selectedRecipe(module)
                .map(holder -> holder.value().result().copy())
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        guiGraphics.renderItem(icon, this.leftPos + 7 + left, this.topPos + 54 + top);
        guiGraphics.renderItemDecorations(this.font, icon, this.leftPos + 7 + left, this.topPos + 54 + top);
    }

    private void renderRecipeGhosts(GuiGraphics guiGraphics, int module) {
        Optional<RecipeHolder<AssemblyMachineRecipe>> selectedRecipe = this.menu.selectedRecipe(module);
        if (selectedRecipe.isEmpty()) {
            return;
        }

        List<AssemblyMachineRecipe.CountedIngredient> ingredients = selectedRecipe.get().value().ingredients();
        for (int ingredientIndex = 0; ingredientIndex < ingredients.size(); ingredientIndex++) {
            int slotIndex = AssemblyFactoryBlockEntity.inputSlotStart(module) + ingredientIndex;
            if (slotIndex >= AssemblyFactoryBlockEntity.inputSlotStart(module) + AssemblyFactoryBlockEntity.MODULE_INPUT_COUNT) {
                return;
            }
            Slot slot = this.menu.getSlot(slotIndex);
            if (slot.hasItem()) {
                continue;
            }
            ItemStack ghost = AssemblyRecipeSelectorScreen.displayIngredient(ingredients.get(ingredientIndex));
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
