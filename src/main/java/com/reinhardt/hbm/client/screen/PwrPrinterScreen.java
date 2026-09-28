package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.PwrBlock;
import com.reinhardt.hbm.network.PwrPrinterOpenPayload;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modern equivalent of GUIScreenSlicePrinter.  It shows the original blocks
 * hidden behind an assembled PWR and can export the same one-image-per-layer
 * construction diagrams into .minecraft/printer.
 */
public final class PwrPrinterScreen extends Screen {
    private static final int LEGEND_WIDTH = 184;
    private static final int SCREEN_MARGIN = 18;
    private static final int MAGENTA = 0xFFFF00FF;
    private static final DateTimeFormatter DIRECTORY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    private final PwrPrinterOpenPayload snapshot;
    private final List<List<PwrPrinterOpenPayload.Part>> layers;
    private final Set<PwrBlock.Kind> usedKinds;
    private int layer;
    private Button previousButton;
    private Button nextButton;
    private Button exportButton;
    private boolean exporting;
    private boolean waitingForWrites;
    private Path exportDirectory;
    private final AtomicInteger pendingWrites = new AtomicInteger();
    private final AtomicBoolean exportFailed = new AtomicBoolean();

    private int gridLeft;
    private int gridTop;
    private int cellSize;
    private int displayColumns;
    private int displayRows;

    private PwrPrinterScreen(PwrPrinterOpenPayload snapshot) {
        super(Component.translatable("gui.reinhardtshbm.pwr_printer.title"));
        this.snapshot = snapshot;
        this.layers = new ArrayList<>(snapshot.sizeY());
        for (int y = 0; y < snapshot.sizeY(); y++) {
            this.layers.add(new ArrayList<>());
        }
        EnumSet<PwrBlock.Kind> kinds = EnumSet.noneOf(PwrBlock.Kind.class);
        for (PwrPrinterOpenPayload.Part part : snapshot.parts()) {
            this.layers.get(part.y()).add(part);
            kinds.add(part.kind());
        }
        this.usedKinds = Set.copyOf(kinds);
    }

    public static void open(PwrPrinterOpenPayload snapshot) {
        Minecraft.getInstance().setScreen(new PwrPrinterScreen(snapshot));
    }

    @Override
    protected void init() {
        int controlsY = this.height - 28;
        this.previousButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.reinhardtshbm.pwr_printer.previous"),
                button -> changeLayer(-1)
        ).bounds(this.width / 2 - 156, controlsY, 96, 20).build());
        this.nextButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.reinhardtshbm.pwr_printer.next"),
                button -> changeLayer(1)
        ).bounds(this.width / 2 - 52, controlsY, 96, 20).build());
        this.exportButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.reinhardtshbm.pwr_printer.export"),
                button -> beginExport()
        ).bounds(this.width / 2 + 52, controlsY, 112, 20).build());
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (exporting) {
            renderExportFrame(graphics);
            return;
        }

        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(8, 8, this.width - 8, this.height - 36, 0xD0101010);
        graphics.renderOutline(8, 8, this.width - 16, this.height - 44, 0xFF808080);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("gui.reinhardtshbm.pwr_printer.layer", this.layer + 1, this.snapshot.sizeY()),
                (this.width - LEGEND_WIDTH) / 2,
                29,
                0xFFFFFF55
        );

        DiagramLayout layout = layout(false);
        drawGridBackground(graphics, layout);
        PwrPrinterOpenPayload.Part hovered = drawLayer(graphics, layout, mouseX, mouseY, true);
        drawLegend(graphics);
        graphics.drawString(
                this.font,
                Component.translatable("gui.reinhardtshbm.pwr_printer.controls"),
                16,
                this.height - 45,
                0xFFB0B0B0,
                false
        );
        super.render(graphics, mouseX, mouseY, partialTick);

        if (hovered != null) {
            graphics.renderTooltip(this.font, List.of(
                    nameFor(hovered.kind()),
                    Component.translatable(
                            "gui.reinhardtshbm.pwr_printer.position",
                            hovered.x(), hovered.y(), hovered.z()
                    )
            ), java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private void renderExportFrame(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, MAGENTA);
        DiagramLayout layout = layout(true);
        drawLayer(graphics, layout, Integer.MIN_VALUE, Integer.MIN_VALUE, false);
        graphics.flush();
        captureLayer(this.layer);

        if (this.layer + 1 < this.snapshot.sizeY()) {
            this.layer++;
            updateButtons();
            return;
        }

        this.exporting = false;
        this.waitingForWrites = true;
        this.layer = 0;
        updateButtons();
    }

    private DiagramLayout layout(boolean export) {
        boolean quarterTurn = this.snapshot.inward() == Direction.WEST || this.snapshot.inward() == Direction.EAST;
        this.displayColumns = quarterTurn ? this.snapshot.sizeZ() : this.snapshot.sizeX();
        this.displayRows = quarterTurn ? this.snapshot.sizeX() : this.snapshot.sizeZ();

        int left = export ? SCREEN_MARGIN : 20;
        int right = export ? this.width - SCREEN_MARGIN : this.width - LEGEND_WIDTH - 18;
        int top = export ? SCREEN_MARGIN : 45;
        int bottom = export ? this.height - SCREEN_MARGIN : this.height - 55;
        int availableWidth = Math.max(1, right - left);
        int availableHeight = Math.max(1, bottom - top);
        this.cellSize = Math.max(1, Math.min(32,
                Math.min(availableWidth / Math.max(1, displayColumns), availableHeight / Math.max(1, displayRows))));
        int gridWidth = displayColumns * cellSize;
        int gridHeight = displayRows * cellSize;
        this.gridLeft = left + (availableWidth - gridWidth) / 2;
        this.gridTop = top + (availableHeight - gridHeight) / 2;
        return new DiagramLayout(gridLeft, gridTop, cellSize, displayColumns, displayRows);
    }

    private void drawGridBackground(GuiGraphics graphics, DiagramLayout layout) {
        int right = layout.left() + layout.columns() * layout.cell();
        int bottom = layout.top() + layout.rows() * layout.cell();
        graphics.fill(layout.left() - 2, layout.top() - 2, right + 2, bottom + 2, 0xFF606060);
        graphics.fill(layout.left(), layout.top(), right, bottom, 0xFF181818);
        if (layout.cell() >= 4) {
            for (int column = 0; column <= layout.columns(); column++) {
                int x = layout.left() + column * layout.cell();
                graphics.vLine(x, layout.top(), bottom, 0xFF303030);
            }
            for (int row = 0; row <= layout.rows(); row++) {
                int y = layout.top() + row * layout.cell();
                graphics.hLine(layout.left(), right, y, 0xFF303030);
            }
        }
    }

    private PwrPrinterOpenPayload.Part drawLayer(
            GuiGraphics graphics,
            DiagramLayout layout,
            int mouseX,
            int mouseY,
            boolean interactive
    ) {
        PwrPrinterOpenPayload.Part hovered = null;
        for (PwrPrinterOpenPayload.Part part : this.layers.get(this.layer)) {
            Cell cell = displayCell(part);
            int x = layout.left() + cell.column() * layout.cell();
            int y = layout.top() + cell.row() * layout.cell();
            int color = colorFor(part.kind());
            graphics.fill(x, y, x + layout.cell(), y + layout.cell(), color);
            if (layout.cell() >= 8) {
                renderPartIcon(graphics, part.kind(), x, y, layout.cell());
            }
            if (layout.cell() >= 3) {
                graphics.renderOutline(x, y, layout.cell(), layout.cell(), 0xFFC0C0C0);
            }
            if (interactive && mouseX >= x && mouseX < x + layout.cell()
                    && mouseY >= y && mouseY < y + layout.cell()) {
                hovered = part;
                graphics.renderOutline(x, y, layout.cell(), layout.cell(), 0xFFFFFF00);
            }
        }
        return hovered;
    }

    private Cell displayCell(PwrPrinterOpenPayload.Part part) {
        return switch (this.snapshot.inward()) {
            case WEST -> new Cell(this.snapshot.sizeZ() - 1 - part.z(), part.x());
            case SOUTH -> new Cell(this.snapshot.sizeX() - 1 - part.x(), this.snapshot.sizeZ() - 1 - part.z());
            case EAST -> new Cell(part.z(), this.snapshot.sizeX() - 1 - part.x());
            default -> new Cell(part.x(), part.z());
        };
    }

    private void renderPartIcon(GuiGraphics graphics, PwrBlock.Kind kind, int x, int y, int cell) {
        ItemStack stack = stackFor(kind);
        if (stack.isEmpty()) {
            return;
        }
        float scale = Math.max(0.25F, (cell - 2.0F) / 16.0F);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 1.0F, y + 1.0F, 100.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    private void drawLegend(GuiGraphics graphics) {
        int left = this.width - LEGEND_WIDTH + 8;
        int y = 47;
        graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.pwr_printer.legend"), left, y - 14, 0xFFFFFFFF, false);
        for (PwrBlock.Kind kind : PwrBlock.Kind.values()) {
            if (!this.usedKinds.contains(kind)) {
                continue;
            }
            graphics.fill(left, y, left + 16, y + 16, colorFor(kind));
            renderPartIcon(graphics, kind, left, y, 16);
            graphics.renderOutline(left, y, 16, 16, 0xFFB0B0B0);
            graphics.drawString(this.font, nameFor(kind), left + 21, y + 4, 0xFFE0E0E0, false);
            y += 19;
        }
    }

    private void changeLayer(int delta) {
        if (this.exporting || this.waitingForWrites) {
            return;
        }
        this.layer = Mth.clamp(this.layer + delta, 0, this.snapshot.sizeY() - 1);
        updateButtons();
    }

    private void updateButtons() {
        if (this.previousButton == null) {
            return;
        }
        boolean idle = !this.exporting && !this.waitingForWrites;
        this.previousButton.active = idle && this.layer > 0;
        this.nextButton.active = idle && this.layer + 1 < this.snapshot.sizeY();
        this.exportButton.active = idle;
        this.exportButton.setMessage(Component.translatable(
                this.waitingForWrites
                        ? "gui.reinhardtshbm.pwr_printer.saving"
                        : "gui.reinhardtshbm.pwr_printer.export"
        ));
    }

    private void beginExport() {
        if (this.exporting || this.waitingForWrites || this.minecraft == null) {
            return;
        }
        try {
            Path printerRoot = this.minecraft.gameDirectory.toPath().resolve("printer");
            this.exportDirectory = uniqueDirectory(printerRoot, DIRECTORY_FORMAT.format(LocalDateTime.now()));
            Files.createDirectories(this.exportDirectory);
        } catch (IOException exception) {
            ReinhardtsHBM.LOGGER.warn("Unable to create PWR printer output directory", exception);
            if (this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.translatable("gui.reinhardtshbm.pwr_printer.export_failed", exception.getMessage()),
                        false
                );
            }
            return;
        }
        this.layer = 0;
        this.exportFailed.set(false);
        this.pendingWrites.set(0);
        this.exporting = true;
        updateButtons();
    }

    private static Path uniqueDirectory(Path parent, String baseName) {
        Path candidate = parent.resolve(baseName);
        int suffix = 2;
        while (Files.exists(candidate)) {
            candidate = parent.resolve(baseName + "_" + suffix++);
        }
        return candidate;
    }

    private void captureLayer(int exportedLayer) {
        if (this.minecraft == null || this.exportDirectory == null) {
            return;
        }
        NativeImage image = Screenshot.takeScreenshot(this.minecraft.getMainRenderTarget());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                if ((pixel & 0x00FFFFFF) == 0x00FF00FF) {
                    image.setPixelRGBA(x, y, 0);
                }
            }
        }

        Path target = this.exportDirectory.resolve("slice_" + exportedLayer + ".png");
        this.pendingWrites.incrementAndGet();
        Util.ioPool().execute(() -> {
            try (image) {
                image.writeToFile(target);
            } catch (IOException exception) {
                this.exportFailed.set(true);
                ReinhardtsHBM.LOGGER.warn("Unable to write PWR construction diagram {}", target, exception);
            } finally {
                this.pendingWrites.decrementAndGet();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (this.waitingForWrites && this.pendingWrites.get() == 0) {
            completeExport();
        }
    }

    private void completeExport() {
        if (this.minecraft == null) {
            return;
        }
        this.waitingForWrites = false;
        updateButtons();
        if (this.minecraft.player != null) {
            Component message = this.exportFailed.get()
                    ? Component.translatable("gui.reinhardtshbm.pwr_printer.export_failed", this.exportDirectory)
                    : Component.translatable("gui.reinhardtshbm.pwr_printer.saved", this.exportDirectory);
            this.minecraft.player.displayClientMessage(message, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0D) {
            changeLayer(scrollY > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_LEFT) {
            changeLayer(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN || keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_RIGHT) {
            changeLayer(1);
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) && !this.exporting) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Component nameFor(PwrBlock.Kind kind) {
        return Component.translatable("block.reinhardtshbm." + switch (kind) {
            case BLOCK -> "pwr_block";
            case CASING -> "pwr_casing";
            case CHANNEL -> "pwr_channel";
            case CONTROL -> "pwr_control";
            case CONTROLLER -> "pwr_controller";
            case FUEL -> "pwr_fuelrod";
            case HEATEX -> "pwr_heatex";
            case HEATSINK -> "pwr_heatsink";
            case NEUTRON_SOURCE -> "pwr_neutron_source";
            case PORT -> "pwr_port";
            case REFLECTOR -> "pwr_reflector";
        });
    }

    private static ItemStack stackFor(PwrBlock.Kind kind) {
        return switch (kind) {
            case BLOCK -> ItemStack.EMPTY;
            case CASING -> new ItemStack(HbmBlocks.PWR_CASING.get());
            case CHANNEL -> new ItemStack(HbmBlocks.PWR_CHANNEL.get());
            case CONTROL -> new ItemStack(HbmBlocks.PWR_CONTROL.get());
            case CONTROLLER -> new ItemStack(HbmBlocks.PWR_CONTROLLER.get());
            case FUEL -> new ItemStack(HbmBlocks.PWR_FUELROD.get());
            case HEATEX -> new ItemStack(HbmBlocks.PWR_HEATEX.get());
            case HEATSINK -> new ItemStack(HbmBlocks.PWR_HEATSINK.get());
            case NEUTRON_SOURCE -> new ItemStack(HbmBlocks.PWR_NEUTRON_SOURCE.get());
            case PORT -> new ItemStack(HbmBlocks.PWR_PORT.get());
            case REFLECTOR -> new ItemStack(HbmBlocks.PWR_REFLECTOR.get());
        };
    }

    private static int colorFor(PwrBlock.Kind kind) {
        return switch (kind) {
            case BLOCK -> 0xFFAA00AA;
            case CASING -> 0xFF707780;
            case CHANNEL -> 0xFF2476A8;
            case CONTROL -> 0xFFC8A43C;
            case CONTROLLER -> 0xFF4EA55B;
            case FUEL -> 0xFFB24A38;
            case HEATEX -> 0xFF4DB8B0;
            case HEATSINK -> 0xFF49688F;
            case NEUTRON_SOURCE -> 0xFF8A55C2;
            case PORT -> 0xFFD08030;
            case REFLECTOR -> 0xFFB5B5C8;
        };
    }

    private record Cell(int column, int row) {
    }

    private record DiagramLayout(int left, int top, int cell, int columns, int rows) {
    }
}
