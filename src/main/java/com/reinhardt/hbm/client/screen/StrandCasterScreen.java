package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.menu.StrandCasterMenu;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class StrandCasterScreen extends AbstractContainerScreen<StrandCasterMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_strand_caster.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public StrandCasterScreen(StrandCasterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.titleLabelY = 4;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isHovering(16, 17, 36, 81, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, materialTooltip(), mouseX, mouseY);
        }
        if (isHovering(82, 14, 16, 24, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(
                    this.font,
                    HbmFluidTooltip.forTank(this.menu.waterFluid(), this.menu.waterAmount(), this.menu.waterCapacity()),
                    mouseX,
                    mouseY
            );
        }
        if (isHovering(82, 65, 16, 24, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(
                    this.font,
                    HbmFluidTooltip.forTank(this.menu.steamFluid(), this.menu.steamAmount(), this.menu.steamCapacity()),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(guiGraphics, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int materialPixels = this.menu.materialScaled(79);
        FoundryMaterial material = this.menu.material();
        if (material != null && materialPixels > 0) {
            int color = 0xFF000000 | material.moltenColor();
            guiGraphics.fill(
                    this.leftPos + 17,
                    this.topPos + 93 - materialPixels,
                    this.leftPos + 51,
                    this.topPos + 93,
                    color
            );
            guiGraphics.fill(
                    this.leftPos + 17,
                    this.topPos + 93 - materialPixels,
                    this.leftPos + 51,
                    this.topPos + 94 - materialPixels,
                    0x66FFFFFF
            );
        }

        drawFluid(guiGraphics, 82, 62, 16, this.menu.waterScaled(24), this.menu.waterFluid());
        drawFluid(guiGraphics, 82, 113, 16, this.menu.steamScaled(24), this.menu.steamFluid());
    }

    private List<Component> materialTooltip() {
        FoundryMaterial material = this.menu.material();
        if (material == null || this.menu.amount() <= 0) {
            return List.of(Component.translatable("message.reinhardtshbm.power.empty_slot").withStyle(ChatFormatting.RED));
        }
        return List.of(Component.translatable(material.translationKey())
                .append(Component.literal(": " + this.menu.amount() + " / " + this.menu.capacity() + " TU"))
                .withStyle(ChatFormatting.YELLOW));
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, com.reinhardt.hbm.fluid.HbmFluidDefinition fluid) {
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation texture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(texture).isEmpty()) {
            int color = 0xFF000000 | fluid.color();
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, color);
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY - height + 1, 0x66FFFFFF);
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

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(TEXTURE, x, y, u, v, width, height, TEX_W, TEX_H);
    }
}
