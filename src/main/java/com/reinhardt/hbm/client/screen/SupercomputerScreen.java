package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.SupercomputerMenu;
import com.reinhardt.hbm.recipe.SupercomputerRecipe;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Exact 176x211 GUIMachineSuperComputer layout. */
public final class SupercomputerScreen extends AbstractContainerScreen<SupercomputerMenu> {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/gui/processing/gui_supercomputer.png");

    public SupercomputerScreen(SupercomputerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 211;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 70 - this.font.width(this.title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (isHovering(8, 54, 52, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font,
                    HbmFluidTooltip.forTank(this.menu.inputFluid(), this.menu.inputAmount(),
                            this.menu.inputCapacity(), this.menu.inputPressure()), mouseX, mouseY);
        } else if (isHovering(80, 54, 52, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font,
                    HbmFluidTooltip.forTank(this.menu.outputFluid(), this.menu.outputAmount(),
                            this.menu.outputCapacity(), this.menu.outputPressure()), mouseX, mouseY);
        } else if (isHovering(152, 18, 16, 61, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("tooltip.reinhardtshbm.energy",
                            this.menu.power(), this.menu.maxPower()),
                    Component.translatable("tooltip.reinhardtshbm.consumption", this.menu.consumption())
            ), mouseX, mouseY);
        } else if (isHovering(7, 80, 18, 18, mouseX, mouseY)) {
            Optional<RecipeHolder<SupercomputerRecipe>> selected = this.menu.selectedRecipe();
            graphics.renderComponentTooltip(this.font,
                    selected.map(SupercomputerRecipeSelectorScreen::describeRecipe)
                            .orElseGet(() -> List.of(Component.translatable("gui.recipe.setRecipe")
                                    .withStyle(ChatFormatting.YELLOW))), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight, 256, 256);

        int power = this.menu.powerScaled(61);
        if (power > 0) {
            graphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 79 - power,
                    176, 61 - power, 16, power, 256, 256);
        }

        int progress = this.menu.progressScaled(70);
        if (progress > 0) {
            graphics.blit(TEXTURE, this.leftPos + 62, this.topPos + 81,
                    176, 61, progress, 16, 256, 256);
        }

        if (this.menu.processing()) {
            graphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 76,
                    195, 0, 3, 6, 256, 256);
            graphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 76,
                    195, 0, 3, 6, 256, 256);
        } else if (this.menu.selectedRecipe().isPresent()) {
            graphics.blit(TEXTURE, this.leftPos + 51, this.topPos + 76,
                    192, 0, 3, 6, 256, 256);
            if (this.menu.power() >= this.menu.consumption()) {
                graphics.blit(TEXTURE, this.leftPos + 56, this.topPos + 76,
                        192, 0, 3, 6, 256, 256);
            }
        }

        ItemStack icon = this.menu.selectedRecipe()
                .map(holder -> holder.value().displayIcon())
                .orElseGet(() -> new ItemStack(HbmItems.TEMPLATE_FOLDER.get()));
        graphics.renderItem(icon, this.leftPos + 8, this.topPos + 81);
        graphics.renderItemDecorations(this.font, icon, this.leftPos + 8, this.topPos + 81);

        renderInputGhosts(graphics);
        drawHorizontalFluid(graphics, 8, 70, this.menu.inputScaled(52), 16, this.menu.inputFluid());
        drawHorizontalFluid(graphics, 80, 70, this.menu.outputScaled(52), 16, this.menu.outputFluid());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(7, 80, 18, 18, mouseX, mouseY)) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new SupercomputerRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderInputGhosts(GuiGraphics graphics) {
        Optional<RecipeHolder<SupercomputerRecipe>> selected = this.menu.selectedRecipe();
        if (selected.isEmpty()) {
            return;
        }
        List<SupercomputerRecipe.CountedIngredient> ingredients = selected.get().value().ingredients();
        for (int index = 0; index < ingredients.size(); index++) {
            Slot slot = this.menu.getSlot(2 + index);
            if (slot.hasItem()) {
                continue;
            }
            ItemStack[] candidates = ingredients.get(index).ingredient().getItems();
            if (candidates.length == 0) {
                continue;
            }
            ItemStack ghost = Arrays.stream(candidates).findFirst().orElse(ItemStack.EMPTY).copy();
            ghost.setCount(ingredients.get(index).count());
            GhostItemRenderer.render(graphics, ghost,
                    this.leftPos + slot.x, this.topPos + slot.y, 0.20F);
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.5F);
            graphics.blit(TEXTURE, this.leftPos + slot.x, this.topPos + slot.y,
                    slot.x, slot.y, 16, 16, 256, 256);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void drawHorizontalFluid(
            GuiGraphics graphics, int x, int y, int width, int height, HbmFluidDefinition fluid) {
        if (width <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + y,
                    this.leftPos + x + width, this.topPos + y + height,
                    0xFF000000 | fluid.color());
            return;
        }
        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        for (int tileX = 0; tileX < width; tileX += 16) {
            int tileWidth = Math.min(16, width - tileX);
            graphics.blit(texture, this.leftPos + x + tileX, this.topPos + y,
                    0, 0, tileWidth, height, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
