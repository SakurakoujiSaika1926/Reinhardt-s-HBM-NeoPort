package com.reinhardt.hbm.client.screen;

import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.api.entity.LegacyRadarDetectable;
import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.RadarTarget;
import com.reinhardt.hbm.menu.RadarMenu;
import com.reinhardt.hbm.network.RadarControlPayload;
import com.reinhardt.hbm.network.RadarCommandPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Direct visual layout port of GUIMachineRadarNT's main display. */
public final class RadarScreen extends AbstractContainerScreen<RadarMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_radar_nt.png");
    private int lastMouseX;
    private int lastMouseY;

    public RadarScreen(RadarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 216;
        this.imageHeight = 234;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderHover(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        graphics.blit(TEXTURE, leftPos - 14, topPos + 84, 224, 0, 14, 66, 256, 256);
        graphics.blit(TEXTURE, leftPos - 14, topPos + 154, 224, 66, 14, 36, 256, 256);

        LegacyMachineBlockEntity machine = menu.machine();
        long power = menu.power();
        int bar = (int) Math.min(200L, power * 200L / Math.max(1L, menu.powerCapacity()));
        if (bar > 0) graphics.blit(TEXTURE, leftPos + 8, topPos + 221, 0, 234, bar, 16, 256, 256);
        if (machine == null) return;

        drawToggle(graphics, 0, 88, machine.radarScanMissiles());
        drawToggle(graphics, 1, 98, machine.radarScanShells());
        drawToggle(graphics, 2, 108, machine.radarScanPlayers());
        drawToggle(graphics, 3, 118, machine.radarSmartMode());
        drawToggle(graphics, 4, 128, machine.radarRedMode());
        drawToggle(graphics, 5, 138, machine.radarShowMap());
        if (power < machine.radarConsumptionValue()) return;

        if (machine.radarShowMap()) drawMap(graphics);
        drawSweep(graphics, machine.radarRotation(partialTick));
        for (RadarTarget target : menu.targets()) drawTarget(graphics, target, machine.radarRangeValue());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The original main radar GUI has no title or player inventory labels.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int action = actionAt(mouseX, mouseY);
        if (action < 0) return super.mouseClicked(mouseX, mouseY, button);
        PacketDistributor.sendToServer(new RadarControlPayload(menu.blockPos(), action));
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    private void drawToggle(GuiGraphics graphics, int action, int y, boolean enabled) {
        LegacyMachineBlockEntity machine = menu.machine();
        boolean noisyState = enabled;
        if (machine != null && machine.radarJammed() && minecraft != null && minecraft.level != null
                && minecraft.level.random.nextBoolean()) noisyState = !enabled;
        if (noisyState) graphics.blit(TEXTURE, leftPos - 10, topPos + y, 238, 4 + action * 10, 8, 8, 256, 256);
    }

    private void drawMap(GuiGraphics graphics) {
        for (int index = 0; index < 40_000; index++) {
            int value = menu.mapCell(index) & 0xFF;
            if (value == 0) continue;
            int green = (value - 50) * 255 / 78;
            int color = 0xFF000000 | (Math.max(0, Math.min(255, green)) << 8);
            int x = index % 200;
            int z = index / 200;
            graphics.fill(leftPos + 8 + x, topPos + 17 + z, leftPos + 9 + x, topPos + 18 + z, color);
        }
    }

    private void drawSweep(GuiGraphics graphics, float rotation) {
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + 108, topPos + 117, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(-(rotation + 180.0F)));
        graphics.fill(0, -1, 100, 1, 0xA000FF00);
        graphics.pose().popPose();
    }

    private void drawTarget(GuiGraphics graphics, RadarTarget target, int range) {
        int x = (int) ((target.x() - menu.blockPos().getX()) / ((double) range * 2.0D + 1.0D) * 192.0D) + leftPos + 104;
        int z = (int) ((target.z() - menu.blockPos().getZ()) / ((double) range * 2.0D + 1.0D) * 192.0D) + topPos + 113;
        int type = Math.max(0, Math.min(LegacyRadarDetectable.SPECIAL, target.blipLevel()));
        graphics.blit(TEXTURE, x, z, 216, type * 8, 8, 8, 256, 256);
    }

    private void renderHover(GuiGraphics graphics, int mouseX, int mouseY) {
        int action = actionAt(mouseX, mouseY);
        if (action >= 0) {
            String key = switch (action) {
                case 0 -> "gui.reinhardtshbm.radar.detect_missiles";
                case 1 -> "gui.reinhardtshbm.radar.detect_shells";
                case 2 -> "gui.reinhardtshbm.radar.detect_players";
                case 3 -> "gui.reinhardtshbm.radar.smart_mode";
                case 4 -> "gui.reinhardtshbm.radar.redstone_mode";
                case 5 -> "gui.reinhardtshbm.radar.show_map";
                case 6 -> "gui.reinhardtshbm.radar.configure";
                default -> "gui.reinhardtshbm.radar.clear_map";
            };
            graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
            return;
        }
        if (inside(mouseX, mouseY, 8, 17, 200, 200)) {
            for (RadarTarget target : menu.targets()) {
                int x = (int) ((target.x() - menu.blockPos().getX()) / ((double) menu.machine().radarRangeValue() * 2.0D + 1.0D) * 192.0D) + leftPos + 104;
                int z = (int) ((target.z() - menu.blockPos().getZ()) / ((double) menu.machine().radarRangeValue() * 2.0D + 1.0D) * 192.0D) + topPos + 113;
                if (mouseX + 5 > x && mouseX - 4 <= x && mouseY + 5 > z && mouseY - 4 <= z) {
                    graphics.renderComponentTooltip(font, List.of(Component.translatable(target.name()),
                            Component.literal(target.x() + " / " + target.z()),
                            Component.translatable("gui.reinhardtshbm.radar.altitude", target.y())), mouseX, mouseY);
                    return;
                }
            }
            int x = (int) ((mouseX - leftPos - 108) * ((double) menu.machine().radarRangeValue() * 2.0D + 1.0D) / 192.0D + menu.blockPos().getX());
            int z = (int) ((mouseY - topPos - 117) * ((double) menu.machine().radarRangeValue() * 2.0D + 1.0D) / 192.0D + menu.blockPos().getZ());
            graphics.renderTooltip(font, Component.literal(x + " / " + z), mouseX, mouseY);
        }
    }

    private int actionAt(double mouseX, double mouseY) {
        if (inside(mouseX, mouseY, -10, 88, 8, 8)) return 0;
        if (inside(mouseX, mouseY, -10, 98, 8, 8)) return 1;
        if (inside(mouseX, mouseY, -10, 108, 8, 8)) return 2;
        if (inside(mouseX, mouseY, -10, 118, 8, 8)) return 3;
        if (inside(mouseX, mouseY, -10, 128, 8, 8)) return 4;
        if (inside(mouseX, mouseY, -10, 138, 8, 8)) return 5;
        if (inside(mouseX, mouseY, -10, 158, 8, 8)) return 7;
        if (inside(mouseX, mouseY, -10, 178, 8, 8)) return 6;
        return -1;
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_8
                && inside(lastMouseX, lastMouseY, 8, 17, 200, 200)) {
            int relaySlot = keyCode - GLFW.GLFW_KEY_1;
            RadarTarget target = targetAt(lastMouseX, lastMouseY);
            if (target != null) {
                PacketDistributor.sendToServer(new RadarCommandPayload(menu.blockPos(), relaySlot,
                        target.entityId(), 0, 0));
            } else {
                LegacyMachineBlockEntity machine = menu.machine();
                if (machine == null) return true;
                int range = machine.radarRangeValue();
                int x = (int) ((lastMouseX - leftPos - 108) * ((double) range * 2.0D + 1.0D) / 192.0D + menu.blockPos().getX());
                int z = (int) ((lastMouseY - topPos - 117) * ((double) range * 2.0D + 1.0D) / 192.0D + menu.blockPos().getZ());
                PacketDistributor.sendToServer(new RadarCommandPayload(menu.blockPos(), relaySlot, -1, x, z));
            }
            if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private RadarTarget targetAt(int mouseX, int mouseY) {
        LegacyMachineBlockEntity machine = menu.machine();
        if (machine == null) return null;
        int range = machine.radarRangeValue();
        for (RadarTarget target : menu.targets()) {
            int x = (int) ((target.x() - menu.blockPos().getX()) / ((double) range * 2.0D + 1.0D) * 192.0D) + leftPos + 104;
            int z = (int) ((target.z() - menu.blockPos().getZ()) / ((double) range * 2.0D + 1.0D) * 192.0D) + topPos + 113;
            if (mouseX + 5 > x && mouseX - 4 <= x && mouseY + 5 > z && mouseY - 4 <= z) {
                return target;
            }
        }
        return null;
    }
}
