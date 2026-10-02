package com.reinhardt.hbm.client.qmaw;

import com.mojang.blaze3d.platform.NativeImage;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Developer/wiki screenshot batch renderer added by the latest upstream branch. */
public final class WikiRenderScreen extends Screen {
    private static final int TRANSPARENT_KEY = 0x00FF00FF;
    private final List<ItemStack> stacks;
    private final String prefix;
    private int index;

    public WikiRenderScreen(List<ItemStack> stacks, String prefix) {
        super(Component.literal("NTM wiki renderer"));
        this.stacks = List.copyOf(stacks);
        this.prefix = prefix;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFFFF00FF);
        if (index >= stacks.size()) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable(
                        "command.reinhardtshbm.ntmwikirender.done", stacks.size()), false);
            }
            onClose();
            return;
        }

        ItemStack stack = stacks.get(index);
        int renderScale = Math.max(1, Math.min(16, (Math.min(width, height) - 20) / 16));
        int logicalSize = renderScale * 16;
        int left = 10;
        int top = 10;

        graphics.pose().pushPose();
        graphics.pose().translate(left + logicalSize / 2.0F, top + logicalSize / 2.0F, 200.0F);
        graphics.pose().scale(renderScale, renderScale, 1.0F);
        graphics.renderItem(stack, -8, -8);
        graphics.pose().popPose();

        saveRenderedItem(stack, left, top, logicalSize);
        index++;
    }

    private void saveRenderedItem(ItemStack stack, int logicalX, int logicalY, int logicalSize) {
        Minecraft client = Minecraft.getInstance();
        double guiScale = client.getWindow().getGuiScale();
        int sourceX = Math.max(0, (int) Math.round(logicalX * guiScale));
        int sourceY = Math.max(0, (int) Math.round(logicalY * guiScale));
        int sourceSize = Math.max(1, (int) Math.round(logicalSize * guiScale));

        try (NativeImage framebuffer = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
            int width = Math.min(sourceSize, framebuffer.getWidth() - sourceX);
            int height = Math.min(sourceSize, framebuffer.getHeight() - sourceY);
            if (width <= 0 || height <= 0) {
                throw new IOException("Rendered item lies outside the framebuffer");
            }
            try (NativeImage crop = new NativeImage(width, height, true)) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int color = framebuffer.getPixelRGBA(sourceX + x, sourceY + y);
                        crop.setPixelRGBA(x, y, (color & 0x00FFFFFF) == TRANSPARENT_KEY ? 0 : color);
                    }
                }
                Path directory = client.gameDirectory.toPath().resolve("wiki-block-renders-256");
                Files.createDirectories(directory);
                String registryName = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                String displayName = stack.getHoverName().getString().replaceAll("§.", "");
                String fileName = sanitize(prefix + "-" + registryName + "-" + displayName) + ".png";
                crop.writeToFile(directory.resolve(fileName));
            }
        } catch (Exception exception) {
            ReinhardtsHBM.LOGGER.warn("Failed to save wiki render for {}",
                    BuiltInRegistries.ITEM.getKey(stack.getItem()), exception);
        }
    }

    private static String sanitize(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N} ()._-]+", "")
                .trim().replace(' ', '_');
        return sanitized.isBlank() ? "unnamed" : sanitized;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
