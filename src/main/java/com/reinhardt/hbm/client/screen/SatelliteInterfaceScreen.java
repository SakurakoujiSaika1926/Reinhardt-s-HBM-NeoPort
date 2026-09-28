package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.reinhardt.hbm.item.LegacySatelliteToolItem;
import com.reinhardt.hbm.item.SatelliteChipItem;
import com.reinhardt.hbm.network.SatellitePanelClickPayload;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The old 216x216 satellite control panel.
 *
 * <p>The map remains a client-side surface scan, as it was in 1.7.10. One
 * 200-pixel row is sampled every 25 ms and uploaded as one dynamic texture;
 * it never requests an unloaded client chunk.</p>
 */
public final class SatelliteInterfaceScreen extends Screen {
    private static final int PANEL_SIZE = 216;
    private static final int MAP_SIZE = 200;
    private static final int MAP_OFFSET = 8;
    private static final int MAP_RADIUS = MAP_SIZE / 2;
    private static final long SCAN_INTERVAL_MILLIS = 25L;
    private static final int EMPTY_PIXEL = 0xFF000000;
    private static final int GRID_PIXEL = 0xFF004000;
    private static final int CENTER_PIXEL = 0xFF00FF00;

    private final int frequency;
    private final boolean laserPanel;
    private final boolean connected;
    private final InteractionHand hand;

    private int leftPos;
    private int topPos;
    private int centerX;
    private int centerZ;
    private int scanRow;
    private long lastScanMillis;
    private DynamicTexture mapTexture;
    private ResourceLocation mapTextureLocation;

    private SatelliteInterfaceScreen(int frequency, String satelliteKind, boolean offhand) {
        super(Component.translatable("gui.reinhardtshbm.satellite_interface"));
        this.frequency = frequency;
        this.connected = !satelliteKind.isEmpty();
        this.laserPanel = "laser".equals(satelliteKind);
        this.hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            this.centerX = (int) minecraft.player.getX();
            this.centerZ = (int) minecraft.player.getZ();
        }
    }

    public static void open(int frequency, String satelliteKind, boolean offhand) {
        Minecraft.getInstance().setScreen(new SatelliteInterfaceScreen(
                frequency, satelliteKind, offhand));
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - PANEL_SIZE) / 2;
        this.topPos = (this.height - PANEL_SIZE) / 2;
        if (this.mapTexture == null) {
            this.mapTexture = new DynamicTexture(MAP_SIZE, MAP_SIZE, true);
            this.mapTextureLocation = this.minecraft.getTextureManager().register(
                    "reinhardtshbm_satellite_map", this.mapTexture);
            clearMap();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        if (this.laserPanel) {
            progressScan();
        }
        renderPanel(graphics);

        if (!this.connected) {
            renderStatus(graphics,
                    Component.translatable("gui.reinhardtshbm.satellite_interface.not_linked"),
                    0xFFB00000);
        } else if (!this.laserPanel) {
            renderStatus(graphics,
                    Component.translatable("gui.reinhardtshbm.satellite_interface.no_service"),
                    0xFF9B7800);
        } else if (insideMap(mouseX, mouseY)) {
            int worldX = this.centerX + mouseX - this.leftPos - MAP_OFFSET - MAP_RADIUS;
            int worldZ = this.centerZ + mouseY - this.topPos - MAP_OFFSET - MAP_RADIUS;
            graphics.renderTooltip(this.font, Component.literal(worldX + " / " + worldZ), mouseX, mouseY);
        }
    }

    private void renderPanel(GuiGraphics graphics) {
        int right = this.leftPos + PANEL_SIZE;
        int bottom = this.topPos + PANEL_SIZE;
        graphics.fill(this.leftPos, this.topPos, right, bottom, 0xFF303030);
        graphics.fill(this.leftPos + 1, this.topPos + 1, right - 1, bottom - 1, 0xFF909090);
        graphics.fill(this.leftPos + 3, this.topPos + 3, right - 3, bottom - 3, 0xFF202020);
        graphics.fill(this.leftPos + 5, this.topPos + 5, right - 5, bottom - 5, 0xFFD0D0D0);
        graphics.fill(this.leftPos + 7, this.topPos + 7, right - 7, bottom - 7, 0xFF101010);
        if (this.mapTextureLocation != null) {
            graphics.blit(this.mapTextureLocation,
                    this.leftPos + MAP_OFFSET, this.topPos + MAP_OFFSET,
                    0.0F, 0.0F, MAP_SIZE, MAP_SIZE, MAP_SIZE, MAP_SIZE);
        }
    }

    private void renderStatus(GuiGraphics graphics, Component text, int background) {
        int textWidth = this.font.width(text);
        int x = (this.width - textWidth) / 2;
        int y = (this.height - this.font.lineHeight) / 2;
        graphics.fill(x - 2, y - 2, x + textWidth + 2, y + this.font.lineHeight + 1, background);
        graphics.drawString(this.font, text, x, y, 0xFFFFFFFF, false);
    }

    private void progressScan() {
        long now = System.currentTimeMillis();
        if (now < this.lastScanMillis + SCAN_INTERVAL_MILLIS) {
            return;
        }
        this.lastScanMillis = now;
        scanCurrentRow();
        this.scanRow = (this.scanRow + 1) % MAP_SIZE;
    }

    private void scanCurrentRow() {
        if (this.minecraft == null || this.minecraft.level == null || this.mapTexture == null) {
            return;
        }
        NativeImage pixels = this.mapTexture.getPixels();
        if (pixels == null) {
            return;
        }

        int worldZ = this.centerZ + this.scanRow - MAP_RADIUS;
        BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();
        for (int mapX = 0; mapX < MAP_SIZE; mapX++) {
            int worldX = this.centerX + mapX - MAP_RADIUS;
            int pixel = basePixel(mapX, this.scanRow);
            if (this.minecraft.level.getChunkSource().getChunk(
                    worldX >> 4, worldZ >> 4, ChunkStatus.FULL, false) != null) {
                int surfaceY = this.minecraft.level.getHeight(
                        Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
                samplePos.set(worldX, surfaceY, worldZ);
                BlockState state = this.minecraft.level.getBlockState(samplePos);
                int rgb = state.getMapColor(this.minecraft.level, samplePos).col;
                if (rgb != 0) {
                    pixel = FastColor.ABGR32.fromArgb32(0xFF000000 | rgb);
                }
            }
            pixels.setPixelRGBA(mapX, this.scanRow, pixel);
        }
        this.mapTexture.upload();
    }

    private void clearMap() {
        if (this.mapTexture == null) {
            return;
        }
        NativeImage pixels = this.mapTexture.getPixels();
        if (pixels == null) {
            return;
        }
        for (int z = 0; z < MAP_SIZE; z++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                pixels.setPixelRGBA(x, z, basePixel(x, z));
            }
        }
        this.scanRow = 0;
        this.lastScanMillis = 0L;
        this.mapTexture.upload();
    }

    private static int basePixel(int x, int z) {
        if (x == MAP_RADIUS && z == MAP_RADIUS) {
            return CENTER_PIXEL;
        }
        return x % 50 == 0 || z % 50 == 0 ? GRID_PIXEL : EMPTY_PIXEL;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.laserPanel && insideMap(mouseX, mouseY)) {
            int worldX = this.centerX + (int) mouseX - this.leftPos - MAP_OFFSET - MAP_RADIUS;
            int worldZ = this.centerZ + (int) mouseY - this.topPos - MAP_OFFSET - MAP_RADIUS;
            PacketDistributor.sendToServer(new SatellitePanelClickPayload(
                    worldX, worldZ, this.frequency, this.hand == InteractionHand.OFF_HAND));
            if (this.minecraft != null) {
                this.minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(HbmSoundEvents.TECH_BLEEP.get(), 1.0F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean insideMap(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + MAP_OFFSET
                && mouseX < this.leftPos + MAP_OFFSET + MAP_SIZE
                && mouseY >= this.topPos + MAP_OFFSET
                && mouseY < this.topPos + MAP_OFFSET + MAP_SIZE;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null) {
            if (keyCode == 256 || this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                onClose();
                return true;
            }
            if (this.minecraft.options.keyUp.matches(keyCode, scanCode)) {
                this.centerZ -= 50;
                clearMap();
                return true;
            }
            if (this.minecraft.options.keyDown.matches(keyCode, scanCode)) {
                this.centerZ += 50;
                clearMap();
                return true;
            }
            if (this.minecraft.options.keyLeft.matches(keyCode, scanCode)) {
                this.centerX -= 50;
                clearMap();
                return true;
            }
            if (this.minecraft.options.keyRight.matches(keyCode, scanCode)) {
                this.centerX += 50;
                clearMap();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        ItemStack held = this.minecraft.player.getItemInHand(this.hand);
        if (!(held.getItem() instanceof LegacySatelliteToolItem tool)
                || !tool.isInterface()
                || SatelliteChipItem.frequency(held) != this.frequency) {
            onClose();
        }
    }

    @Override
    public void removed() {
        if (this.minecraft != null && this.mapTextureLocation != null) {
            this.minecraft.getTextureManager().release(this.mapTextureLocation);
            this.mapTextureLocation = null;
            this.mapTexture = null;
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
