package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.PrecisionAssemblerMenu;
import com.reinhardt.hbm.recipe.PrecisionAssemblerRecipe;
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

/** Exact 176x256 precision assembler GUI, based on GUIMachinePrecAss. */
public final class PrecisionAssemblerScreen extends AbstractContainerScreen<PrecisionAssemblerMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_precass.png");
    private static final float GHOST_ITEM_ALPHA = 0.20F;
    private static final float GHOST_SLOT_OVERLAY_ALPHA = 0.50F;
    private static final int GHOST_COUNT_COLOR = 0xB0FFFFFF;
    private static final int GHOST_COUNT_SHADOW = 0x60000000;

    public PrecisionAssemblerScreen(PrecisionAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = 162;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 70 - this.font.width(this.title) / 2;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(152, 18, 16, 61, mouseX, mouseY)) graphics.renderComponentTooltip(this.font,
                List.of(Component.translatable("tooltip.reinhardtshbm.energy", this.menu.energy(), this.menu.capacity())), mouseX, mouseY);
        else if (isHovering(8, 99, 52, 16, mouseX, mouseY)) graphics.renderComponentTooltip(this.font, fluidTooltip(0), mouseX, mouseY);
        else if (isHovering(80, 99, 52, 16, mouseX, mouseY)) graphics.renderComponentTooltip(this.font, fluidTooltip(1), mouseX, mouseY);
        else if (isHovering(62, 126, 70, 16, mouseX, mouseY)) graphics.renderComponentTooltip(this.font,
                List.of(Component.translatable("tooltip.reinhardtshbm.progress", this.menu.progress(), this.menu.workTime())), mouseX, mouseY);
        else if (isHovering(7, 125, 18, 18, mouseX, mouseY)) {
            Optional<RecipeHolder<PrecisionAssemblerRecipe>> selected = this.menu.selectedRecipe();
            graphics.renderComponentTooltip(this.font, selected.map(PrecisionAssemblerRecipeSelectorScreen::describeRecipe)
                    .orElseGet(() -> List.of(Component.translatable("gui.recipe.setRecipe"))), mouseX, mouseY);
        } else super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        int energy = this.menu.energyScaled(61);
        if (energy > 0) graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 79 - energy, 176, 61 - energy, 16, energy);
        int progress = this.menu.progressScaled(70);
        if (progress > 0) graphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 126, 176, 61, progress, 16);
        if (this.menu.isWorking()) {
            graphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 195, 0, 3, 6);
            graphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 121, 195, 0, 3, 6);
        } else if (this.menu.selectedRecipe().isPresent()) {
            graphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 121, 192, 0, 3, 6);
        }
        renderRecipeIcon(graphics);
        renderGhostIngredients(graphics);
        drawFluid(graphics, 0, 8, 99);
        drawFluid(graphics, 1, 80, 99);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(7, 125, 18, 18, mouseX, mouseY) && this.minecraft != null) {
            this.minecraft.setScreen(new PrecisionAssemblerRecipeSelectorScreen(this, this.menu));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderRecipeIcon(GuiGraphics graphics) {
        ItemStack icon = this.menu.selectedRecipe()
                .map(holder -> holder.value().outputs().getFirst().stack().copy())
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, this.leftPos + 8, this.topPos + 126);
            graphics.renderItemDecorations(this.font, icon, this.leftPos + 8, this.topPos + 126);
        }
    }

    private void renderGhostIngredients(GuiGraphics graphics) {
        Optional<RecipeHolder<PrecisionAssemblerRecipe>> selected = this.menu.selectedRecipe();
        if (selected.isEmpty()) return;
        List<PrecisionAssemblerRecipe.CountedIngredient> ingredients = selected.get().value().ingredients();
        for (int index = 0; index < ingredients.size(); index++) {
            Slot slot = this.menu.getSlot(PrecisionAssemblerMenu.INPUT_START + index);
            if (slot.hasItem()) continue;
            ItemStack ghost = PrecisionAssemblerRecipeSelectorScreen.displayIngredient(ingredients.get(index));
            if (ghost.isEmpty()) continue;
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            GhostItemRenderer.render(graphics, ghost, x, y, GHOST_ITEM_ALPHA);
            // GUIMachinePrecAss overlays the matching slot art at 50% alpha;
            // a solid veil obscures and visually doubles the ghost item.
            graphics.setColor(1.0F, 1.0F, 1.0F, GHOST_SLOT_OVERLAY_ALPHA);
            graphics.blit(TEXTURE, x, y, slot.x, slot.y, 16, 16);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            renderGhostCount(graphics, ghost, x, y);
        }
    }

    private void renderGhostCount(GuiGraphics graphics, ItemStack ghost, int x, int y) {
        if (ghost.getCount() <= 1) return;
        String count = String.valueOf(ghost.getCount());
        int textX = x + 17 - this.font.width(count);
        int textY = y + 9;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.drawString(this.font, count, textX + 1, textY + 1, GHOST_COUNT_SHADOW, false);
        graphics.drawString(this.font, count, textX, textY, GHOST_COUNT_COLOR, false);
        graphics.pose().popPose();
    }

    private void drawFluid(GuiGraphics graphics, int tank, int x, int y) {
        int width = this.menu.tankScaled(tank, 52);
        HbmFluidDefinition fluid = this.menu.tankFluid(tank);
        if (width <= 0 || fluid.isNone()) return;
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + y, this.leftPos + x + width, this.topPos + y + 16, 0xFF000000 | fluid.color());
            return;
        }
        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        for (int offset = 0; offset < width; offset += 16) graphics.blit(texture, this.leftPos + x + offset, this.topPos + y, 0, 0, Math.min(16, width - offset), 16, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private List<Component> fluidTooltip(int tank) {
        return HbmFluidTooltip.forTank(this.menu.tankFluid(tank), this.menu.tankAmount(tank), this.menu.tankCapacity(tank), 0);
    }
}
