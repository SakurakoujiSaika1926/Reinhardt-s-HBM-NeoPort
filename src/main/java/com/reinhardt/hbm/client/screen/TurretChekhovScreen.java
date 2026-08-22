package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.TurretChekhovBlockEntity;
import com.reinhardt.hbm.menu.TurretChekhovMenu;
import com.reinhardt.hbm.network.TurretChekhovControlPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class TurretChekhovScreen extends AbstractContainerScreen<TurretChekhovMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/weapon/gui_turret_base.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private EditBox whitelistName;
    private int whitelistIndex;

    public TurretChekhovScreen(TurretChekhovMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 6;
        this.whitelistName = new EditBox(this.font, this.leftPos + 10, this.topPos + 65, 50, 14, Component.empty());
        this.whitelistName.setTextColor(0x00FF00);
        this.whitelistName.setTextColorUneditable(0x00FF00);
        this.whitelistName.setBordered(false);
        this.whitelistName.setMaxLength(25);
        addWidget(this.whitelistName);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderLegacyHoverHints(guiGraphics, mouseX, mouseY);
        renderOldTooltips(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        if (inside(mouseX, mouseY, 7, 80, 18, 18)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 7, this.topPos + 80, 176, 58, 18, 18, TEX_W, TEX_H);
        }
        if (inside(mouseX, mouseY, 43, 80, 18, 18)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 43, this.topPos + 80, 194, 58, 18, 18, TEX_W, TEX_H);
        }
        if (inside(mouseX, mouseY, 7, 98, 18, 18)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 7, this.topPos + 98, 176, 76, 18, 18, TEX_W, TEX_H);
        }
        if (inside(mouseX, mouseY, 43, 98, 18, 18)) {
            guiGraphics.blit(TEXTURE, this.leftPos + 43, this.topPos + 98, 194, 76, 18, 18, TEX_W, TEX_H);
        }

        int power = this.menu.powerScaled(53);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 152, this.topPos + 97 - power, 194, 52 - power, 16, power, TEX_W, TEX_H);
        }

        if (this.menu.on()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 115, this.topPos + 26, 176, 40, 18, 18, TEX_W, TEX_H);
        }
        if (this.menu.targetPlayers()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 30, 176, 0, 10, 10, TEX_W, TEX_H);
        }
        if (this.menu.targetAnimals()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 22, this.topPos + 30, 176, 10, 10, 10, TEX_W, TEX_H);
        }
        if (this.menu.targetMobs()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 36, this.topPos + 30, 176, 20, 10, 10, TEX_W, TEX_H);
        }
        if (this.menu.targetMachines()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 50, this.topPos + 30, 176, 30, 10, 10, TEX_W, TEX_H);
        }

        int tallies = this.menu.stattrak();
        if (tallies >= 36) {
            guiGraphics.blit(TEXTURE, this.leftPos + 77, this.topPos + 50, 176, 120, 63, 6, TEX_W, TEX_H);
        } else {
            int steps = (int) Math.ceil(tallies / 5.0D);
            for (int step = 0; step < steps; step++) {
                int mark = tallies % 5;
                if (step < steps - 1 || mark == 0) {
                    guiGraphics.blit(TEXTURE, this.leftPos + 77 + 9 * step, this.topPos + 50, 194, 94, 9, 6, TEX_W, TEX_H);
                } else {
                    guiGraphics.blit(TEXTURE, this.leftPos + 77 + 9 * step, this.topPos + 50, 176, 94, mark * 2, 6, TEX_W, TEX_H);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);

        List<String> names = this.menu.whitelist();
        while (this.whitelistIndex >= names.size()) {
            this.whitelistIndex--;
        }
        if (this.whitelistIndex < 0) {
            this.whitelistIndex = 0;
        }
        Component shown = names.isEmpty()
                ? Component.translatable("gui.reinhardtshbm.turret.none").withStyle(ChatFormatting.ITALIC)
                : Component.literal(names.get(this.whitelistIndex));
        LegacyTurretScreenCompat.renderWhitelistText(guiGraphics, this.font, shown, this.whitelistName);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.whitelistName != null && this.whitelistName.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (inside(mouseX, mouseY, 115, 26, 18, 18)) {
            clickToggle(0);
            return true;
        }
        if (inside(mouseX, mouseY, 8, 30, 10, 10)) {
            clickToggle(1);
            return true;
        }
        if (inside(mouseX, mouseY, 22, 30, 10, 10)) {
            clickToggle(2);
            return true;
        }
        if (inside(mouseX, mouseY, 36, 30, 10, 10)) {
            clickToggle(3);
            return true;
        }
        if (inside(mouseX, mouseY, 50, 30, 10, 10)) {
            clickToggle(4);
            return true;
        }

        int count = this.menu.whitelist().size();
        if (count > 0 && inside(mouseX, mouseY, 7, 80, 18, 18)) {
            this.whitelistIndex--;
            if (this.whitelistIndex < 0) {
                this.whitelistIndex = count - 1;
            }
            playButtonSound();
            return true;
        }
        if (count > 0 && inside(mouseX, mouseY, 43, 80, 18, 18)) {
            this.whitelistIndex++;
            this.whitelistIndex %= count;
            playButtonSound();
            return true;
        }
        if (inside(mouseX, mouseY, 7, 98, 18, 18)) {
            playButtonSound();
            sendWhitelistAdd();
            return true;
        }
        if (inside(mouseX, mouseY, 43, 98, 18, 18)) {
            playButtonSound();
            PacketDistributor.sendToServer(new TurretChekhovControlPayload(this.menu.position(), 1, this.whitelistIndex, ""));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.whitelistName != null && this.whitelistName.isFocused() && this.whitelistName.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.whitelistName != null && this.whitelistName.isFocused() && this.whitelistName.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void renderOldTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (inside(mouseX, mouseY, 152, 45, 16, 52)) {
            guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.power(),
                    TurretChekhovBlockEntity.MAX_POWER
            )), mouseX, mouseY);
        }
        if (inside(mouseX, mouseY, 8, 30, 10, 10)) {
            renderToggleTooltip(guiGraphics, mouseX, mouseY, "gui.reinhardtshbm.turret.players", this.menu.targetPlayers());
        }
        if (inside(mouseX, mouseY, 22, 30, 10, 10)) {
            renderToggleTooltip(guiGraphics, mouseX, mouseY, "gui.reinhardtshbm.turret.animals", this.menu.targetAnimals());
        }
        if (inside(mouseX, mouseY, 36, 30, 10, 10)) {
            renderToggleTooltip(guiGraphics, mouseX, mouseY, "gui.reinhardtshbm.turret.mobs", this.menu.targetMobs());
        }
        if (inside(mouseX, mouseY, 50, 30, 10, 10)) {
            renderToggleTooltip(guiGraphics, mouseX, mouseY, "gui.reinhardtshbm.turret.machines", this.menu.targetMachines());
        }
    }

    private void renderLegacyHoverHints(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.menu.getCarried().isEmpty()) {
            return;
        }
        for (int slot = TurretChekhovBlockEntity.AMMO_START; slot < TurretChekhovBlockEntity.AMMO_END; slot++) {
            if (this.menu.getSlot(slot).hasItem() && isHovering(this.menu.getSlot(slot).x, this.menu.getSlot(slot).y, 16, 16, mouseX, mouseY)) {
                return;
            }
        }
        LegacyTurretScreenCompat.renderAmmoHint(
                guiGraphics,
                this.font,
                mouseX,
                mouseY,
                this.leftPos,
                this.topPos,
                LegacyTurretScreenCompat.chekhovAmmoStacks()
        );
    }

    private void renderToggleTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, String key, boolean enabled) {
        Component state = Component.translatable(enabled ? "gui.reinhardtshbm.turret.on" : "gui.reinhardtshbm.turret.off")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
        guiGraphics.renderComponentTooltip(this.font, List.of(Component.translatable(key, state)), mouseX, mouseY);
    }

    private void clickToggle(int id) {
        playButtonSound();
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private void sendWhitelistAdd() {
        String value = this.whitelistName.getValue();
        if (!value.isEmpty()) {
            PacketDistributor.sendToServer(new TurretChekhovControlPayload(this.menu.position(), 0, -1, value));
            this.whitelistName.setValue("");
        }
    }

    private void playButtonSound() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + width
                && mouseY > this.topPos + y && mouseY <= this.topPos + y + height;
    }
}
