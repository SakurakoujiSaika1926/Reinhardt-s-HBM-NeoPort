package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.CrucibleBlockEntity;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.menu.CrucibleMenu;
import com.reinhardt.hbm.recipe.CrucibleRecipe;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

public class CrucibleScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<CrucibleMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_crucible.png");

    public CrucibleScreen(CrucibleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = 120;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(125, 81, 34, 7, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.progress",
                    this.menu.progress(),
                    CrucibleBlockEntity.PROCESS_TIME
            )), mouseX, mouseY);
        }
        if (isHovering(125, 90, 34, 7, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.literal(
                    String.format("%,d / %,d TU", this.menu.heat(), CrucibleBlockEntity.MAX_HEAT)
            )), mouseX, mouseY);
        }
        if (isHovering(106, 80, 18, 18, mouseX, mouseY)) {
            Optional<RecipeHolder<CrucibleRecipe>> recipe = this.menu.selectedRecipe();
            guiGraphics.renderComponentTooltip(this.font, recipe
                    .map(CrucibleRecipeSelectorScreen::describeRecipe)
                    .orElseGet(() -> List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW))), mouseX, mouseY);
        }
        if (isHovering(16, 17, 36, 81, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, materialStackTooltip(this.menu.wasteStack()), mouseX, mouseY);
        }
        if (isHovering(61, 17, 36, 81, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, materialStackTooltip(this.menu.recipeStack()), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int progress = this.menu.progressScaled(33);
        if (progress > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 126, this.topPos + 82, 176, 0, progress, 5);
        }

        int heat = this.menu.heatScaled(33);
        if (heat > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 126, this.topPos + 91, 176, 5, heat, 5);
        }

        drawStack(guiGraphics, this.menu.recipeStack(), CrucibleBlockEntity.RECIPE_CAPACITY, 62, 97);
        drawStack(guiGraphics, this.menu.wasteStack(), CrucibleBlockEntity.WASTE_CAPACITY, 17, 97);
        renderRecipeIcon(guiGraphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(106, 80, 18, 18, mouseX, mouseY)) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new CrucibleRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawStack(GuiGraphics guiGraphics, List<FoundryMaterialStack> stacks, int capacity, int x, int y) {
        if (stacks.isEmpty() || capacity <= 0) {
            return;
        }

        int lastHeight = 0;
        int lastAmount = 0;

        RenderSystem.enableBlend();
        for (FoundryMaterialStack stack : stacks) {
            int targetHeight = Math.min(79, (lastAmount + stack.amount()) * 79 / capacity);
            if (lastHeight == targetHeight) {
                lastAmount += stack.amount();
                continue;
            }

            int offset = stack.material().behavior() == com.reinhardt.hbm.foundry.FoundryMaterial.SmeltingBehavior.ADDITIVE ? 34 : 0;
            int color = stack.material().moltenColor();
            float red = ((color >> 16) & 0xFF) / 255.0F;
            float green = ((color >> 8) & 0xFF) / 255.0F;
            float blue = (color & 0xFF) / 255.0F;
            int height = targetHeight - lastHeight;

            guiGraphics.setColor(red, green, blue, 1.0F);
            guiGraphics.blit(TEXTURE, this.leftPos + x, this.topPos + y - targetHeight, 176 + offset, 89 - targetHeight, 34, height);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 0.3F);
            guiGraphics.blit(TEXTURE, this.leftPos + x, this.topPos + y - targetHeight, 176 + offset, 89 - targetHeight, 34, height);

            lastAmount += stack.amount();
            lastHeight = targetHeight;
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void renderRecipeIcon(GuiGraphics guiGraphics) {
        ItemStack icon = this.menu.selectedRecipe()
                .map(holder -> holder.value().icon().copy())
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        if (icon.isEmpty()) {
            return;
        }
        guiGraphics.renderItem(icon, this.leftPos + 107, this.topPos + 81);
        guiGraphics.renderItemDecorations(this.font, icon, this.leftPos + 107, this.topPos + 81);
    }

    private List<Component> materialStackTooltip(List<FoundryMaterialStack> stacks) {
        if (stacks.isEmpty()) {
            return List.of(Component.literal("Empty").withStyle(ChatFormatting.RED));
        }
        return stacks.stream()
                .map(stack -> Component.translatable(stack.material().translationKey())
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(": "))
                        .append(ScrapsItem.formatAmountComponent(stack.amount())))
                .map(Component.class::cast)
                .toList();
    }
}
