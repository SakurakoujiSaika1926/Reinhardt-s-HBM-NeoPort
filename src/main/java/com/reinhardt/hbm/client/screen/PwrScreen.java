package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.PwrFuelItem;
import com.reinhardt.hbm.menu.PwrMenu;
import com.reinhardt.hbm.network.PwrControlPayload;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

public class PwrScreen extends AbstractContainerScreen<PwrMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/reactors/gui_pwr.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int TANK_CAPACITY = 128_000;
    private static final int HULL_HEAT_CAPACITY = 10_000_000;

    private static final int CONTROL_FIELD_X = 57;
    private static final int CONTROL_FIELD_Y = 63;
    private static final int CONTROL_FIELD_WIDTH = 30;
    private static final int CONTROL_FIELD_HEIGHT = 8;
    private static final int CONTROL_CONFIRM_X = 88;
    private static final int CONTROL_CONFIRM_Y = 58;
    private static final int CONTROL_CONFIRM_WIDTH = 18;
    private static final int CONTROL_CONFIRM_HEIGHT = 18;

    private EditBox rodTarget;

    public PwrScreen(PwrMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 188;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.rodTarget = new EditBox(
                this.font,
                this.leftPos + CONTROL_FIELD_X,
                this.topPos + CONTROL_FIELD_Y,
                CONTROL_FIELD_WIDTH,
                CONTROL_FIELD_HEIGHT,
                Component.empty()
        );
        this.rodTarget.setBordered(false);
        this.rodTarget.setTextColor(0x00FF00);
        this.rodTarget.setTextColorUneditable(0x008000);
        this.rodTarget.setMaxLength(3);
        this.rodTarget.setFilter(value -> value.isEmpty() || value.matches("\\d{0,3}"));
        // The legacy field is the user-facing withdrawal percentage. The
        // reactor stores the inverse (100 means fully inserted).
        this.rodTarget.setValue(Integer.toString(Mth.clamp(100 - (int) Math.round(this.menu.rodTarget()), 0, 100)));
        this.addRenderableWidget(this.rodTarget);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderHoverInfo(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        blit(guiGraphics, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.menu.hullHeat() > HULL_HEAT_CAPACITY * 0.8D
                || this.menu.coreHeat() > this.menu.coreHeatCapacity() * 0.8D) {
            blit(guiGraphics, this.leftPos + 147, this.topPos, 176, 14, 26, 26);
        }

        int progress = this.menu.progressScaled(33);
        if (progress > 0) {
            blit(guiGraphics, this.leftPos + 54, this.topPos + 33, 176, 0, progress, 14);
        }

        int controlRod = Mth.clamp((int) Math.round(this.menu.rodLevel() * 52.0D / 100.0D), 0, 52);
        if (controlRod > 0) {
            blit(guiGraphics, this.leftPos + 53, this.topPos + 54, 176, 40, controlRod, 2);
        }

        drawLegacyGauge(guiGraphics, 124, 40, this.menu.coreHeat() / (double) this.menu.coreHeatCapacity());
        drawLegacyGauge(guiGraphics, 160, 40, this.menu.hullHeat() / (double) HULL_HEAT_CAPACITY);
        drawLoadedFuel(guiGraphics);

        DfcScreenUtil.drawFluid(
                this.minecraft,
                guiGraphics,
                this.leftPos,
                this.topPos,
                8,
                57,
                16,
                scaledTank(this.menu.coolantAmount()),
                this.menu.coolantFluid()
        );
        DfcScreenUtil.drawFluid(
                this.minecraft,
                guiGraphics,
                this.leftPos,
                this.topPos,
                26,
                57,
                16,
                scaledTank(this.menu.hotCoolantAmount()),
                this.menu.hotCoolantFluid()
        );
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // GUIPWR did not render a title; the texture's upper half is fully
        // occupied by instruments and the title overlaps them.
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        String flux = String.format(Locale.US, "%,.1f", (double) this.menu.flux());
        float scale = 1.25F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(1.0F / scale, 1.0F / scale, 1.0F);
        guiGraphics.drawString(
                this.font,
                flux,
                (int) (165.0F * scale - this.font.width(flux)),
                (int) (64.0F * scale),
                0x00FF00,
                false
        );
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && mouseX >= this.leftPos + CONTROL_CONFIRM_X
                && mouseX < this.leftPos + CONTROL_CONFIRM_X + CONTROL_CONFIRM_WIDTH
                && mouseY > this.topPos + CONTROL_CONFIRM_Y
                && mouseY <= this.topPos + CONTROL_CONFIRM_Y + CONTROL_CONFIRM_HEIGHT) {
            int withdrawal = parseWithdrawal();
            this.rodTarget.setValue(Integer.toString(withdrawal));
            PacketDistributor.sendToServer(new PwrControlPayload(this.menu.blockPos(), 100 - withdrawal));
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int parseWithdrawal() {
        if (this.rodTarget == null || this.rodTarget.getValue().isEmpty()) {
            return 0;
        }
        try {
            return Mth.clamp(Integer.parseInt(this.rodTarget.getValue()), 0, 100);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void renderHoverInfo(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(115, 31, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.pwr.core_heat",
                    formatHeat(this.menu.coreHeat(), this.menu.coreHeatCapacity())
            )), mouseX, mouseY);
            return;
        }
        if (isHovering(151, 31, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.pwr.hull_heat",
                    formatHeat(this.menu.hullHeat(), HULL_HEAT_CAPACITY)
            )), mouseX, mouseY);
            return;
        }
        if (isHovering(52, 31, 36, 18, mouseX, mouseY)) {
            int percent = Mth.clamp((int) Math.floor(this.menu.progress() * 100.0D / this.menu.processTime()), 0, 100);
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.pwr.depletion",
                    percent
            )), mouseX, mouseY);
            return;
        }
        if (isHovering(52, 53, 54, 4, mouseX, mouseY)) {
            double withdrawal = 100.0D - Math.round(this.menu.rodLevel() * 100.0D) / 100.0D;
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.pwr.control_rods",
                    withdrawal
            )), mouseX, mouseY);
            return;
        }
        if (isHovering(88, 4, 18, 18, mouseX, mouseY)) {
            ItemStack fuel = loadedFuelStack();
            if (!fuel.isEmpty()) {
                graphics.renderTooltip(this.font, fuel, mouseX, mouseY);
                return;
            }
        }
        if (isHovering(8, 5, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.coolantFluid(),
                    this.menu.coolantAmount(),
                    TANK_CAPACITY
            ), mouseX, mouseY);
            return;
        }
        if (isHovering(26, 5, 16, 52, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(
                    this.menu.hotCoolantFluid(),
                    this.menu.hotCoolantAmount(),
                    TANK_CAPACITY
            ), mouseX, mouseY);
        }
    }

    private void drawLoadedFuel(GuiGraphics graphics) {
        ItemStack fuel = loadedFuelStack();
        if (fuel.isEmpty()) {
            return;
        }
        graphics.renderItem(fuel, this.leftPos + 89, this.topPos + 5);

        String count = this.menu.amountLoaded() + "/" + this.menu.rodCount();
        graphics.pose().pushPose();
        graphics.pose().translate(this.leftPos + 97.5F, this.topPos + 24.5F, 300.0F);
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(this.font, count, -this.font.width(count) / 2, 0, 0xFFFF55, true);
        graphics.pose().popPose();
    }

    private ItemStack loadedFuelStack() {
        if (this.menu.typeLoaded() < 0
                || this.menu.typeLoaded() >= PwrFuelItem.FUELS.size()
                || this.menu.amountLoaded() <= 0) {
            return ItemStack.EMPTY;
        }
        return PwrFuelItem.stackForFuel(HbmItems.PWR_FUEL.get(), this.menu.typeLoaded());
    }

    private int scaledTank(int amount) {
        return Mth.clamp(amount * 52 / TANK_CAPACITY, 0, 52);
    }

    private void drawLegacyGauge(GuiGraphics graphics, int centerX, int centerY, double progress) {
        progress = Mth.clamp(progress, 0.0D, 1.0D);
        double angle = Math.toRadians(-progress * 270.0D - 45.0D);
        double[] tip = rotate(0.0D, 5.0D, angle);
        double[] left = rotate(1.0D, -2.0D, angle);
        double[] right = rotate(-1.0D, -2.0D, angle);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        drawGaugeTriangle(graphics, centerX, centerY, tip, left, right, 1.5D, 0x000000);
        drawGaugeTriangle(graphics, centerX, centerY, tip, left, right, 1.0D, 0x7F0000);
        RenderSystem.disableBlend();
    }

    private void drawGaugeTriangle(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            double[] tip,
            double[] left,
            double[] right,
            double scale,
            int color
    ) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        var pose = graphics.pose().last();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        addGaugeVertex(buffer, pose, centerX + tip[0] * scale, centerY + tip[1] * scale, red, green, blue);
        addGaugeVertex(buffer, pose, centerX + left[0] * scale, centerY + left[1] * scale, red, green, blue);
        addGaugeVertex(buffer, pose, centerX + right[0] * scale, centerY + right[1] * scale, red, green, blue);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    private void addGaugeVertex(
            com.mojang.blaze3d.vertex.VertexConsumer buffer,
            com.mojang.blaze3d.vertex.PoseStack.Pose pose,
            double x,
            double y,
            int red,
            int green,
            int blue
    ) {
        buffer.addVertex(pose, (float) (this.leftPos + x), (float) (this.topPos + y), 0.0F)
                .setColor(red, green, blue, 255);
    }

    private static double[] rotate(double x, double y, double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new double[]{x * cos - y * sin, x * sin + y * cos};
    }

    private static String formatHeat(int value, int capacity) {
        return String.format(Locale.US, "%,d / %,d", value, capacity);
    }

    private void blit(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(TEXTURE, x, y, u, v, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
