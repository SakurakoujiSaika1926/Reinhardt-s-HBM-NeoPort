package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.AutocalBlockEntity;
import com.reinhardt.hbm.menu.AutocalMenu;
import com.reinhardt.hbm.network.AutocalControlPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Pixel-for-pixel layout port of GUIScreenRadioAUTOCAL. */
public final class AutocalScreen extends AbstractContainerScreen<AutocalMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/machine/gui_rtty_autocal.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int SIZE_X = 170;
    private static final int SIZE_Y = 138;

    public AutocalScreen(AutocalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = SIZE_X;
        imageHeight = SIZE_Y;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        AutocalBlockEntity autocal = autocal();
        if (autocal != null) {
            if (autocal.isOn()) graphics.blit(TEXTURE, leftPos + 8, topPos + 36, SIZE_X, 0, 18, 18, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            if (!autocal.ignoresErrors()) graphics.blit(TEXTURE, leftPos + 28, topPos + 36, SIZE_X, 18, 18, 18, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            if (!autocal.autoReboots()) graphics.blit(TEXTURE, leftPos + 48, topPos + 36, SIZE_X, 36, 18, 18, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        AutocalBlockEntity autocal = autocal();
        if (autocal == null) return;
        String[] history = autocal.history();
        for (int index = 0; index < history.length; index++) {
            if (!history[index].isEmpty()) graphics.drawString(font, history[index], 7, 73 + index * 10, 0x00FF00, false);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(8, 36, 18, 18, mouseX, mouseY)) tooltip(graphics, mouseX, mouseY, "gui.reinhardtshbm.autocal.on_off");
        else if (isHovering(28, 36, 18, 18, mouseX, mouseY)) tooltip(graphics, mouseX, mouseY, "gui.reinhardtshbm.autocal.ignore_errors", "gui.reinhardtshbm.autocal.ignore_errors.detail");
        else if (isHovering(48, 36, 18, 18, mouseX, mouseY)) tooltip(graphics, mouseX, mouseY, "gui.reinhardtshbm.autocal.auto_reboot", "gui.reinhardtshbm.autocal.auto_reboot.detail");
        else if (isHovering(84, 36, 18, 18, mouseX, mouseY)) tooltip(graphics, mouseX, mouseY, "gui.reinhardtshbm.autocal.upload");
        else if (isHovering(104, 36, 18, 18, mouseX, mouseY)) tooltip(graphics, mouseX, mouseY, "gui.reinhardtshbm.autocal.open_program");
        else if (isHovering(124, 36, 18, 18, mouseX, mouseY)) tooltip(graphics, mouseX, mouseY, "gui.reinhardtshbm.autocal.download", "gui.reinhardtshbm.autocal.unsupported");
        else if (isHovering(144, 36, 18, 18, mouseX, mouseY)) tooltip(graphics, mouseX, mouseY, "gui.reinhardtshbm.autocal.documentation");
        else super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (isHovering(8, 36, 18, 18, mouseX, mouseY)) return send(AutocalControlPayload.TOGGLE_ON, "");
        if (isHovering(28, 36, 18, 18, mouseX, mouseY)) return send(AutocalControlPayload.TOGGLE_IGNORE, "");
        if (isHovering(48, 36, 18, 18, mouseX, mouseY)) return send(AutocalControlPayload.TOGGLE_REBOOT, "");
        if (isHovering(84, 36, 18, 18, mouseX, mouseY)) return upload();
        if (isHovering(104, 36, 18, 18, mouseX, mouseY)) return openFile(programPath(), false);
        if (isHovering(144, 36, 18, 18, mouseX, mouseY)) return openFile(documentationPath(), true);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean send(int action, String payload) {
        PacketDistributor.sendToServer(new AutocalControlPayload(menu.blockPos(), action, payload));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    private boolean upload() {
        try {
            Path script = programPath();
            if (!Files.exists(script)) {
                Files.createDirectories(script.getParent());
                Files.createFile(script);
                return true;
            }
            return send(AutocalControlPayload.UPLOAD, Files.readString(script, StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            return true;
        }
    }

    private boolean openFile(Path file, boolean documentation) {
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) Files.writeString(file, documentation ? documentationText() : "", StandardCharsets.UTF_8);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file.toFile());
        } catch (IOException ignored) {
        }
        return true;
    }

    private Path programPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("hbmComputerUpload").resolve("script.txt");
    }

    private Path documentationPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("hbmComputerUpload").resolve("documentation.md");
    }

    private static String documentationText() {
        return "# AUTOCAL - The Automatic Calculator\n\n"
                + "MS-ES1 commands: nop, clockspeed, dest, jmp, jmpif, jmpnot, endtick, shutdown, load, save, buffer, eval, evalr, floor, ceil, round, concat, eq, gtb, ltb, geb, leb, send, listen.\n";
    }

    private AutocalBlockEntity autocal() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.blockPos()) instanceof AutocalBlockEntity value ? value : null;
    }

    private void tooltip(GuiGraphics graphics, int mouseX, int mouseY, String... keys) {
        graphics.renderComponentTooltip(font, java.util.Arrays.stream(keys)
                .<Component>map(Component::translatable).toList(), mouseX, mouseY);
    }
}
