package com.reinhardt.hbm.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.RbmkFuelRodItem;
import com.reinhardt.hbm.menu.RbmkComponentMenu;
import com.reinhardt.hbm.network.RbmkAutoControlPayload;
import com.reinhardt.hbm.network.RbmkConsoleControlPayload;
import com.reinhardt.hbm.network.RbmkControlRodPayload;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class RbmkComponentScreen extends AbstractContainerScreen<RbmkComponentMenu> {
    private static final int WIDTH = 176;
    private static final int HEIGHT = 186;
    private static final int AUTOLOADER_HEIGHT = 182;
    private static final int CONSOLE_WIDTH = 244;
    private static final int CONSOLE_HEIGHT = 172;
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static final int[] CONTROL_GROUP_COLORS = {
            0xFFFF3030,
            0xFFFFFF30,
            0xFF30FF30,
            0xFF3090FF,
            0xFFFF70FF
    };
    private final boolean[] consoleSelection = new boolean[RbmkComponentBlockEntity.CONSOLE_COLUMN_COUNT];
    private EditBox consoleLevelField;
    private EditBox[] autoControlFields;
    private boolean az5CoverClosed = true;
    private long lastAz5Press;

    public RbmkComponentScreen(RbmkComponentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = menu.kind() == RbmkComponentBlock.Kind.CONSOLE ? CONSOLE_WIDTH : WIDTH;
        this.imageHeight = menu.kind() == RbmkComponentBlock.Kind.CONSOLE ? CONSOLE_HEIGHT : menu.kind() == RbmkComponentBlock.Kind.AUTOLOADER ? AUTOLOADER_HEIGHT : HEIGHT;
        this.inventoryLabelY = menu.kind() == RbmkComponentBlock.Kind.AUTOLOADER ? this.imageHeight - 96 + 2 : 92;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        if (this.menu.kind() == RbmkComponentBlock.Kind.CONSOLE) {
            this.consoleLevelField = new EditBox(this.font, this.leftPos + 9, this.topPos + 84, 35, 9, Component.empty());
            this.consoleLevelField.setTextColor(0x00FF00);
            this.consoleLevelField.setTextColorUneditable(0x008000);
            this.consoleLevelField.setBordered(false);
            this.consoleLevelField.setMaxLength(3);
            this.consoleLevelField.setValue("0");
            this.addRenderableWidget(this.consoleLevelField);
            return;
        }
        if (this.menu.kind().isControl()) {
            if (this.menu.kind().isAutomaticControl()) {
                initAutoControlWidgets();
            } else {
                initManualControlWidgets();
            }
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.BOILER) {
            this.addRenderableWidget(new ClickArea(this.leftPos + 33, this.topPos + 21, 20, 64, () -> {
                if (this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
                }
            }));
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.AUTOLOADER) {
            addAutoloaderButton(4, 74, 36);
            addAutoloaderButton(5, 90, 36);
        }
    }

    private void initManualControlWidgets() {
        for (int index = 0; index < 5; index++) {
            final int row = index;
            this.addRenderableWidget(new ClickArea(this.leftPos + 118, this.topPos + 26 + row * 11, 30, 10,
                    () -> sendManualControl(RbmkControlRodPayload.ACTION_SET_LEVEL, 100 - row * 25)));
            this.addRenderableWidget(new ClickArea(this.leftPos + 28, this.topPos + 26 + row * 11, 12, 10,
                    () -> sendManualControl(RbmkControlRodPayload.ACTION_ASSIGN_COLOR, row)));
        }
    }

    private void sendManualControl(int action, int value) {
        PacketDistributor.sendToServer(new RbmkControlRodPayload(this.menu.pos(), action, value));
    }

    private void initAutoControlWidgets() {
        this.autoControlFields = new EditBox[4];
        int[] values = {
                this.menu.autoLevelUpper(),
                this.menu.autoLevelLower(),
                this.menu.autoHeatUpper(),
                this.menu.autoHeatLower()
        };
        for (int index = 0; index < this.autoControlFields.length; index++) {
            EditBox field = new EditBox(this.font, this.leftPos + 30, this.topPos + 27 + 11 * index, 26, 6, Component.empty());
            field.setTextColor(0xFFFFFF);
            field.setTextColorUneditable(0xFFFFFF);
            field.setBordered(false);
            field.setMaxLength(index < 2 ? 3 : 4);
            field.setValue(String.valueOf(values[index]));
            this.autoControlFields[index] = field;
            this.addRenderableWidget(field);
        }
        for (int function = 0; function < 3; function++) {
            final int selectedFunction = function;
            this.addRenderableWidget(new ClickArea(this.leftPos + 61, this.topPos + 48 + function * 11, 22, 10,
                    () -> sendAutoControl(selectedFunction, false)));
        }
        this.addRenderableWidget(new ClickArea(this.leftPos + 28, this.topPos + 70, 30, 10,
                () -> sendAutoControl(this.menu.autoFunction(), true)));
    }

    private void addAutoloaderButton(int id, int x, int y) {
        this.addRenderableWidget(new ClickArea(this.leftPos + x, this.topPos + y, 12, 12, () -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }));
    }

    private void sendAutoControl(int function, boolean readFields) {
        int levelUpper = this.menu.autoLevelUpper();
        int levelLower = this.menu.autoLevelLower();
        int heatUpper = this.menu.autoHeatUpper();
        int heatLower = this.menu.autoHeatLower();
        if (readFields && this.autoControlFields != null && this.autoControlFields.length == 4) {
            levelUpper = parseAutoField(0, 100);
            levelLower = parseAutoField(1, 100);
            heatUpper = parseAutoField(2, 9999);
            heatLower = parseAutoField(3, 9999);
        }
        PacketDistributor.sendToServer(new RbmkAutoControlPayload(
                this.menu.pos(),
                function,
                levelUpper,
                levelLower,
                heatUpper,
                heatLower,
                readFields
        ));
    }

    private int parseAutoField(int index, int max) {
        if (this.autoControlFields == null || index < 0 || index >= this.autoControlFields.length) {
            return 0;
        }
        String value = this.autoControlFields[index].getValue().trim();
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            parsed = 0;
        }
        parsed = Math.max(0, Math.min(max, parsed));
        this.autoControlFields[index].setValue(String.valueOf(parsed));
        return parsed;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = texture();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(guiGraphics, texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.kind() == RbmkComponentBlock.Kind.CONSOLE) {
            renderConsoleGrid(guiGraphics, mouseX, mouseY);
            renderConsoleControls(guiGraphics);
        } else {
            renderRbmkOverlays(guiGraphics, texture);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu.kind() == RbmkComponentBlock.Kind.CONSOLE) {
            renderConsoleLabels(guiGraphics);
            return;
        }
        int titleColor = this.menu.kind() == RbmkComponentBlock.Kind.AUTOLOADER ? 0xFFFFFF : 4210752;
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, titleColor, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        if (this.menu.kind() == RbmkComponentBlock.Kind.AUTOLOADER) {
            String cycle = this.menu.autoloaderCycle() + "%";
            guiGraphics.drawString(this.font, cycle, this.imageWidth / 2 - this.font.width(cycle) / 2, 23, 0x00FF00, false);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.menu.kind() == RbmkComponentBlock.Kind.CONSOLE) {
            List<Component> tooltip = consoleTooltip(x, y);
            if (!tooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(this.font, tooltip, x, y);
                return;
            }
        } else {
            List<Component> tooltip = rbmkTooltip(x, y);
            if (!tooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(this.font, tooltip, x, y);
                return;
            }
        }
        super.renderTooltip(guiGraphics, x, y);
    }

    private void renderRbmkOverlays(GuiGraphics graphics, ResourceLocation texture) {
        if (this.menu.kind().acceptsFuel() && !this.menu.getSlot(0).getItem().isEmpty()) {
            renderFuelRodOverlay(graphics, texture);
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.BOILER) {
            renderBoilerOverlay(graphics, texture);
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.HEATER) {
            renderHeaterOverlay(graphics, texture);
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.OUTGASSER) {
            renderOutgasserOverlay(graphics, texture);
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.CONTROL
                || this.menu.kind() == RbmkComponentBlock.Kind.CONTROL_MOD
                || this.menu.kind() == RbmkComponentBlock.Kind.CONTROL_REASIM) {
            renderManualControlOverlay(graphics, texture);
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.CONTROL_AUTO
                || this.menu.kind() == RbmkComponentBlock.Kind.CONTROL_REASIM_AUTO) {
            renderAutoControlOverlay(graphics, texture);
        }
    }

    private void renderFuelRodOverlay(GuiGraphics graphics, ResourceLocation texture) {
        ItemStack rod = this.menu.getSlot(0).getItem();
        if (!(rod.getItem() instanceof RbmkFuelRodItem)) {
            return;
        }
        blit(graphics, texture, this.leftPos + 34, this.topPos + 21, 176, 0, 18, 67);
        int depletion = clampPixels((int) Math.round(RbmkFuelRodItem.depletion(rod) * 67.0D), 67);
        if (depletion > 0) {
            blit(graphics, texture, this.leftPos + 34, this.topPos + 21, 194, 0, 18, depletion);
        }
        int xenon = clampPixels((int) Math.round(RbmkFuelRodItem.xenon(rod) * 58.0D), 58);
        if (xenon > 0) {
            blit(graphics, texture, this.leftPos + 126, this.topPos + 82 - xenon, 212, 58 - xenon, 14, xenon);
        }
    }

    private void renderBoilerOverlay(GuiGraphics graphics, ResourceLocation texture) {
        int water = clampPixels(this.menu.water() * 58 / RbmkComponentBlockEntity.WATER_CAPACITY, 58);
        if (water > 0) {
            blit(graphics, texture, this.leftPos + 126, this.topPos + 82 - water, 176, 58 - water, 14, water);
        }
        int steam = clampPixels(this.menu.steam() * 22 / RbmkComponentBlockEntity.STEAM_CAPACITY, 24);
        if (steam > 0) {
            if (steam > 0) steam++;
            if (steam > 22) steam++;
            blit(graphics, texture, this.leftPos + 91, this.topPos + 65 - steam, 190, 24 - steam, 4, steam);
        }
        int steamU = switch (this.menu.steamCompression()) {
            case 1 -> 208;
            case 2 -> 222;
            case 3 -> 236;
            default -> 194;
        };
        blit(graphics, texture, this.leftPos + 36, this.topPos + 24, steamU, 0, 14, 58);
    }

    private void renderHeaterOverlay(GuiGraphics graphics, ResourceLocation texture) {
        drawFluid(graphics, 68, 82, 14, this.menu.heaterInput() * 58 / RbmkComponentBlockEntity.HEATER_TANK_CAPACITY, fluid("coolant"));
        drawFluid(graphics, 126, 82, 14, this.menu.heaterOutput() * 58 / RbmkComponentBlockEntity.HEATER_TANK_CAPACITY, fluid("coolant_hot"));
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(graphics, texture, this.leftPos + 72, this.topPos + 72, 176, 0, 10, 10);
        blit(graphics, texture, this.leftPos + 130, this.topPos + 72, 186, 0, 10, 10);
    }

    private void renderOutgasserOverlay(GuiGraphics graphics, ResourceLocation texture) {
        int progress = clampPixels(this.menu.outgasserProgress() * 13 / RbmkComponentBlockEntity.OUTGASSER_DURATION, 13);
        if (progress > 0) {
            blit(graphics, texture, this.leftPos + 82, this.topPos + 50, 176, 0, progress, 6);
        }
        int gas = clampPixels(this.menu.outgasserGas() * 42 / RbmkComponentBlockEntity.OUTGASSER_GAS_CAPACITY, 42);
        if (gas > 0) {
            blit(graphics, texture, this.leftPos + 115, this.topPos + 66 - gas, 188, 42 - gas, 10, gas);
        }
    }

    private void renderManualControlOverlay(GuiGraphics graphics, ResourceLocation texture) {
        int height = clampPixels(Math.round(56.0F * (1.0F - this.menu.controlLevel() / 1000.0F)), 56);
        if (height > 0) {
            blit(graphics, texture, this.leftPos + 75, this.topPos + 29, 176, 56 - height, 8, height);
        }
        int color = this.menu.colorGroup();
        if (color >= 0 && color < 5) {
            blit(graphics, texture, this.leftPos + 28, this.topPos + 26 + color * 11, 184, color * 10, 12, 10);
        }
    }

    private void renderAutoControlOverlay(GuiGraphics graphics, ResourceLocation texture) {
        int height = clampPixels(Math.round(56.0F * (1.0F - this.menu.controlLevel() / 1000.0F)), 56);
        if (height > 0) {
            blit(graphics, texture, this.leftPos + 124, this.topPos + 29, 176, 56 - height, 8, height);
        }
        int function = Math.floorMod(this.menu.autoFunction(), 3);
        blit(graphics, texture, this.leftPos + 59, this.topPos + 27, 184, function * 19, 26, 19);
    }

    private void renderConsoleGrid(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        RbmkComponentBlockEntity console = console();
        if (console == null) {
            return;
        }
        ResourceLocation texture = texture();
        int baseX = this.leftPos + 86;
        int baseY = this.topPos + 11;
        for (int index = 0; index < RbmkComponentBlockEntity.CONSOLE_COLUMN_COUNT; index++) {
            int kindOrdinal = console.consoleKind(index);
            if (kindOrdinal < 0 || kindOrdinal >= RbmkComponentBlock.Kind.values().length) {
                continue;
            }
            RbmkComponentBlock.Kind kind = RbmkComponentBlock.Kind.values()[kindOrdinal];
            int x = baseX + index % RbmkComponentBlockEntity.CONSOLE_GRID_SIZE * 10;
            int y = baseY + index / RbmkComponentBlockEntity.CONSOLE_GRID_SIZE * 10;
            blit(guiGraphics, texture, x, y, consoleColumnOffset(kind), 172, 10, 10);
            int maxHeat = Math.max(1, console.consoleMaxHeat(index));
            int heatHeight = clampPixels((int) Math.ceil((console.consoleHeat(index) - 20) * 10.0D / maxHeat), 10);
            if (heatHeight > 0) {
                blit(guiGraphics, texture, x, y + 10 - heatHeight, 0, 192 - heatHeight, 10, heatHeight);
            }
            if (kind.isControl()) {
                int colorGroup = console.consoleColorGroup(index);
                if (colorGroup >= 0 && colorGroup < CONTROL_GROUP_COLORS.length && isManualControlKind(kind)) {
                    blit(guiGraphics, texture, x, y, colorGroup * 10, 202, 10, 10);
                }
                int rodHeight = 8 - clampPixels((int) Math.ceil(console.consoleControl(index) * 8.0D / 100.0D), 8);
                if (rodHeight > 0) {
                    blit(guiGraphics, texture, x + 4, y + 1, 24, 183, 2, rodHeight);
                }
            } else if (kind.acceptsFuel() && console.consoleFuelMaxHeat(index) > 0) {
                // GUIRBMKConsole used the rod core's excess heat above the
                // 20°C baseline, exactly like the column heat strip.  Using
                // the raw core temperature makes a cold rod appear loaded.
                int coreHeight = clampPixels((int) Math.ceil(
                        (console.consoleFuelCoreHeat(index) - 20) * 8.0D
                                / Math.max(1, console.consoleFuelMaxHeat(index))), 8);
                // Legacy GUIRBMKConsole drew this strip from the rod's
                // enrichment (remaining fuel), not from spent fraction.
                int enrichment = 1000 - console.consoleFuelDepletion(index);
                int fuelHeight = clampPixels((int) Math.ceil(enrichment * 8.0D / 1000.0D), 8);
                // consoleFuelXenon is normalized xenon * 1000; old GUI used
                // the legacy percentage (xenon * 8 / 100).
                int xenon = clampPixels((int) Math.ceil(console.consoleFuelXenon(index) * 8.0D / 1000.0D), 8);
                if (coreHeight > 0) blit(guiGraphics, texture, x + 1, y + 9 - coreHeight, 11, 191 - coreHeight, 2, coreHeight);
                if (fuelHeight > 0) blit(guiGraphics, texture, x + 4, y + 9 - fuelHeight, 14, 191 - fuelHeight, 2, fuelHeight);
                if (xenon > 0) blit(guiGraphics, texture, x + 7, y + 9 - xenon, 17, 191 - xenon, 2, xenon);
            } else if (kind == RbmkComponentBlock.Kind.BOILER) {
                int water = clampPixels((int) Math.ceil(console.consoleWater(index) * 8.0D / Math.max(1, console.consoleMaxWater(index))), 8);
                int steam = clampPixels((int) Math.ceil(console.consoleSteam(index) * 8.0D / Math.max(1, console.consoleMaxSteam(index))), 8);
                if (water > 0) blit(guiGraphics, texture, x + 1, y + 9 - water, 41, 191 - water, 3, water);
                if (steam > 0) blit(guiGraphics, texture, x + 6, y + 9 - steam, 46, 191 - steam, 3, steam);
                int markerY = 1 + clampPixels(console.consoleSteamType(index), 3) * 2;
                blit(guiGraphics, texture, x + 4, y + markerY, 44, 183 + markerY - 1, 2, 2);
            } else if (kind == RbmkComponentBlock.Kind.HEATER) {
                int cold = clampPixels((int) Math.ceil(console.consoleHeaterInput(index) * 8.0D / Math.max(1, console.consoleHeaterMax(index))), 8);
                int hot = clampPixels((int) Math.ceil(console.consoleHeaterOutput(index) * 8.0D / Math.max(1, console.consoleHeaterMax(index))), 8);
                if (cold > 0) blit(guiGraphics, texture, x + 1, y + 9 - cold, 131, 191 - cold, 3, cold);
                if (hot > 0) blit(guiGraphics, texture, x + 6, y + 9 - hot, 136, 191 - hot, 3, hot);
            }
            if (this.consoleSelection[index]) {
                blit(guiGraphics, texture, x, y, 0, 192, 10, 10);
            }
            if (mouseX >= x && mouseX < x + 10 && mouseY >= y && mouseY < y + 10) {
                guiGraphics.fill(x, y, x + 10, y + 10, 0x66FFFFFF);
            }
        }
    }

    private void renderConsoleControls(GuiGraphics guiGraphics) {
        ResourceLocation texture = texture();
        RbmkComponentBlockEntity console = console();
        if (console != null) {
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 2; column++) {
                    int slot = row * 2 + column;
                    blit(guiGraphics, texture, this.leftPos + 6 + 40 * column, this.topPos + 8 + 21 * row,
                            console.consoleScreenType(slot) * 18, 238, 18, 18);
                }
            }
            renderFluxGraph(guiGraphics, console);
        }
        if (az5CoverClosed) {
            blit(guiGraphics, texture, this.leftPos + 30, this.topPos + 138, 228, 172, 28, 28);
        }
        for (int i = 0; i < CONTROL_GROUP_COLORS.length; i++) {
            int x = this.leftPos + 6 + i * 11;
            int y = this.topPos + 70;
            guiGraphics.fill(x, y, x + 10, y + 10, 0xFF101010);
            guiGraphics.fill(x + 1, y + 1, x + 9, y + 9, CONTROL_GROUP_COLORS[i]);
        }
    }

    private void renderConsoleLabels(GuiGraphics guiGraphics) {
        RbmkComponentBlockEntity console = console();
        if (console != null) {
            renderFluxScale(guiGraphics, console);
        }
    }

    private void renderFluxScale(GuiGraphics graphics, RbmkComponentBlockEntity console) {
        int highest = Integer.MIN_VALUE;
        int lowest = Integer.MAX_VALUE;
        for (int index = 0; index < console.consoleFluxHistorySize(); index++) {
            int value = console.consoleFluxHistory(index);
            highest = Math.max(highest, value);
            lowest = Math.min(lowest, value);
        }
        if (highest == Integer.MIN_VALUE) {
            highest = console.consoleTotalFlux();
            lowest = 0;
        }
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        graphics.drawString(this.font, Integer.toString(highest), (this.leftPos + 8) * 2, (this.topPos + 98) * 2, 0x00FF00, false);
        graphics.drawString(this.font, Integer.toString(highest), (this.leftPos + 80 - this.font.width(Integer.toString(highest)) / 2) * 2, (this.topPos + 98) * 2, 0x00FF00, false);
        graphics.drawString(this.font, Integer.toString(lowest), (this.leftPos + 8) * 2, (this.topPos + 129) * 2, 0x00FF00, false);
        graphics.drawString(this.font, Integer.toString(lowest), (this.leftPos + 80 - this.font.width(Integer.toString(lowest)) / 2) * 2, (this.topPos + 129) * 2, 0x00FF00, false);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.menu.kind() == RbmkComponentBlock.Kind.CONSOLE && handleConsoleClick((int) mouseX, (int) mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleConsoleClick(int mouseX, int mouseY, int button) {
        int gridX = this.leftPos + 86;
        int gridY = this.topPos + 11;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 2; column++) {
                int slot = row * 2 + column;
                if (inside(mouseX, mouseY, 6 + 40 * column, 8 + 21 * row, 18, 18)) {
                    PacketDistributor.sendToServer(new RbmkConsoleControlPayload(this.menu.pos(), RbmkConsoleControlPayload.ACTION_TOGGLE_SCREEN, slot, new int[0]));
                    return true;
                }
                if (inside(mouseX, mouseY, 24 + 40 * column, 8 + 21 * row, 18, 18)) {
                    PacketDistributor.sendToServer(new RbmkConsoleControlPayload(this.menu.pos(), RbmkConsoleControlPayload.ACTION_ASSIGN_SCREEN, slot, selectedIndices()));
                    return true;
                }
            }
        }
        if (mouseX >= gridX && mouseX < gridX + 150 && mouseY >= gridY && mouseY < gridY + 150) {
            int index = (mouseX - gridX) / 10 + (mouseY - gridY) / 10 * RbmkComponentBlockEntity.CONSOLE_GRID_SIZE;
            RbmkComponentBlockEntity console = console();
            if (console != null && console.consoleKind(index) >= 0) {
                this.consoleSelection[index] = !this.consoleSelection[index];
            }
            return true;
        }
        if (inside(mouseX, mouseY, 61, 70, 10, 10)) {
            selectAllControls();
            return true;
        }
        if (inside(mouseX, mouseY, 72, 70, 10, 10)) {
            java.util.Arrays.fill(this.consoleSelection, false);
            return true;
        }
        for (int color = 0; color < CONTROL_GROUP_COLORS.length; color++) {
            if (inside(mouseX, mouseY, 6 + color * 11, 70, 10, 10)) {
                if (button == 1) {
                    sendConsoleControl(RbmkConsoleControlPayload.ACTION_ASSIGN_COLOR, color);
                } else {
                    selectColorGroup(color);
                }
                return true;
            }
        }
        if (inside(mouseX, mouseY, 48, 82, 12, 12)) {
            sendConsoleControl(RbmkConsoleControlPayload.ACTION_SET_CONTROL, parseConsoleLevel());
            return true;
        }
        if (inside(mouseX, mouseY, 70, 82, 12, 12)) {
            sendConsoleControl(RbmkConsoleControlPayload.ACTION_CYCLE_COMPRESSOR, 0);
            return true;
        }
        if (inside(mouseX, mouseY, 30, 138, 28, 28)) {
            if (az5CoverClosed) {
                az5CoverClosed = false;
            } else if (lastAz5Press + 3000L < System.currentTimeMillis()) {
                lastAz5Press = System.currentTimeMillis();
                PacketDistributor.sendToServer(new RbmkConsoleControlPayload(this.menu.pos(), RbmkConsoleControlPayload.ACTION_AZ5, 0, new int[0]));
            }
            return true;
        }
        return false;
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        int left = this.leftPos + x;
        int top = this.topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private void selectAllControls() {
        java.util.Arrays.fill(this.consoleSelection, false);
        RbmkComponentBlockEntity console = console();
        if (console == null) {
            return;
        }
        for (int i = 0; i < this.consoleSelection.length; i++) {
            int kindOrdinal = console.consoleKind(i);
            if (kindOrdinal >= 0 && kindOrdinal < RbmkComponentBlock.Kind.values().length
                    && isManualControlKind(RbmkComponentBlock.Kind.values()[kindOrdinal])) {
                this.consoleSelection[i] = true;
            }
        }
    }

    private void selectColorGroup(int colorGroup) {
        java.util.Arrays.fill(this.consoleSelection, false);
        RbmkComponentBlockEntity console = console();
        if (console == null) {
            return;
        }
        for (int i = 0; i < this.consoleSelection.length; i++) {
            int kindOrdinal = console.consoleKind(i);
            if (kindOrdinal >= 0 && kindOrdinal < RbmkComponentBlock.Kind.values().length
                    && isManualControlKind(RbmkComponentBlock.Kind.values()[kindOrdinal])
                    && console.consoleColorGroup(i) == colorGroup) {
                this.consoleSelection[i] = true;
            }
        }
    }

    private void sendConsoleControl(int action, int value) {
        PacketDistributor.sendToServer(new RbmkConsoleControlPayload(this.menu.pos(), action, value, selectedIndices()));
    }

    private static boolean isManualControlKind(RbmkComponentBlock.Kind kind) {
        return kind == RbmkComponentBlock.Kind.CONTROL
                || kind == RbmkComponentBlock.Kind.CONTROL_MOD
                || kind == RbmkComponentBlock.Kind.CONTROL_REASIM;
    }

    private int[] selectedIndices() {
        int count = selectedCount();
        int[] indices = new int[count];
        int cursor = 0;
        for (int i = 0; i < this.consoleSelection.length; i++) {
            if (this.consoleSelection[i]) {
                indices[cursor++] = i;
            }
        }
        return indices;
    }

    private int selectedCount() {
        int count = 0;
        for (boolean selected : this.consoleSelection) {
            if (selected) {
                count++;
            }
        }
        return count;
    }

    private int parseConsoleLevel() {
        if (this.consoleLevelField == null) {
            return 0;
        }
        try {
            return Math.max(0, Math.min(100, Integer.parseInt(this.consoleLevelField.getValue())));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private List<Component> consoleTooltip(int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        RbmkComponentBlockEntity console = console();
        if (console == null) {
            return tooltip;
        }
        int baseX = this.leftPos + 86;
        int baseY = this.topPos + 11;
        if (mouseX < baseX || mouseX >= baseX + 150 || mouseY < baseY || mouseY >= baseY + 150) {
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 2; column++) {
                    int slot = row * 2 + column;
                    if (inside(mouseX, mouseY, 6 + 40 * column, 8 + 21 * row, 18, 18)) {
                        tooltip.add(Component.translatable("rbmk.console." + consoleScreenKey(console.consoleScreenType(slot)), slot + 1));
                        return tooltip;
                    }
                    if (inside(mouseX, mouseY, 24 + 40 * column, 8 + 21 * row, 18, 18)) {
                        tooltip.add(Component.translatable("rbmk.console.assign", slot + 1));
                        return tooltip;
                    }
                }
            }
            for (int color = 0; color < CONTROL_GROUP_COLORS.length; color++) {
                if (inside(mouseX, mouseY, 6 + color * 11, 70, 10, 10)) {
                    tooltip.add(Component.translatable("rbmk.console.color_group", color + 1));
                    tooltip.add(Component.translatable("rbmk.console.color_group.left"));
                    tooltip.add(Component.translatable("rbmk.console.color_group.right"));
                    return tooltip;
                }
            }
            if (inside(mouseX, mouseY, 61, 70, 10, 10)) {
                tooltip.add(Component.translatable("rbmk.console.select_all"));
                return tooltip;
            }
            if (inside(mouseX, mouseY, 72, 70, 10, 10)) {
                tooltip.add(Component.translatable("rbmk.console.deselect_all"));
                return tooltip;
            }
            if (inside(mouseX, mouseY, 70, 82, 12, 12)) {
                tooltip.add(Component.translatable("rbmk.console.compressor"));
                return tooltip;
            }
            if (inside(mouseX, mouseY, 30, 138, 28, 28)) {
                tooltip.add(Component.translatable("rbmk.console.az5"));
                return tooltip;
            }
            return tooltip;
        }
        int index = (mouseX - baseX) / 10 + (mouseY - baseY) / 10 * RbmkComponentBlockEntity.CONSOLE_GRID_SIZE;
        int kindOrdinal = console.consoleKind(index);
        if (kindOrdinal < 0 || kindOrdinal >= RbmkComponentBlock.Kind.values().length) {
            return tooltip;
        }
        RbmkComponentBlock.Kind kind = RbmkComponentBlock.Kind.values()[kindOrdinal];
        tooltip.add(Component.translatable("rbmk.console.column", kind.getSerializedName()));
        tooltip.add(Component.translatable("rbmk.heat", console.consoleHeat(index)));
        if (kind.acceptsFuel()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.flux", console.consoleFlux(index)));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.depletion", console.consoleFuelDepletion(index) / 10.0D));
            // Legacy RBMKColumn stored xenon as a 0..100 percentage.  The
            // modern scan stores the normalized item value at 1000 scale.
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.xenon", console.consoleFuelXenon(index) / 10.0D));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.core_temp", console.consoleFuelCoreHeat(index)));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.skin_temp", console.consoleFuelHullHeat(index)));
        } else if (kind == RbmkComponentBlock.Kind.BOILER) {
            tooltip.add(Component.translatable("rbmk.boiler.water", console.consoleWater(index), console.consoleMaxWater(index)));
            tooltip.add(Component.translatable("rbmk.boiler.steam", console.consoleSteam(index), console.consoleMaxSteam(index)));
        } else if (kind == RbmkComponentBlock.Kind.HEATER) {
            tooltip.add(Component.translatable("rbmk.heater.input", console.consoleHeaterInput(index), console.consoleHeaterMax(index)));
            tooltip.add(Component.translatable("rbmk.heater.output", console.consoleHeaterOutput(index), console.consoleHeaterMax(index)));
        }
        if (kind.isControl()) {
            tooltip.add(Component.translatable("rbmk.console.control", console.consoleControl(index)));
            if (isManualControlKind(kind)) {
                tooltip.add(Component.translatable("rbmk.console.color_group", console.consoleColorGroup(index) + 1));
            }
        }
        return tooltip;
    }

    private List<Component> rbmkTooltip(int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();
        if (this.menu.kind().acceptsFuel()) {
            if (isHovering(34, 21, 18, 67, mouseX, mouseY)) {
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.depletion",
                        Math.round(RbmkFuelRodItem.depletion(this.menu.getSlot(0).getItem()) * 1000.0F) / 10.0F));
            }
            if (isHovering(126, 24, 14, 58, mouseX, mouseY)) {
                tooltip.add(Component.translatable("tooltip.reinhardtshbm.rbmk_fuel.xenon",
                        Math.round(RbmkFuelRodItem.xenon(this.menu.getSlot(0).getItem()) * 100000.0F) / 1000.0F));
            }
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.BOILER) {
            if (isHovering(126, 24, 16, 56, mouseX, mouseY)) {
                tooltip.addAll(HbmFluidTooltip.forTank(fluid("water"), this.menu.water(), RbmkComponentBlockEntity.WATER_CAPACITY));
            } else if (isHovering(89, 39, 8, 28, mouseX, mouseY)) {
                tooltip.addAll(HbmFluidTooltip.forTank(steamFluid(), this.menu.steam(), RbmkComponentBlockEntity.STEAM_CAPACITY));
            } else if (isHovering(33, 21, 20, 64, mouseX, mouseY)) {
                tooltip.add(Component.translatable("rbmk.boiler.type", Component.translatable(steamFluid().translationKey())));
            }
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.HEATER) {
            if (isHovering(68, 24, 16, 58, mouseX, mouseY)) {
                tooltip.addAll(HbmFluidTooltip.forTank(fluid("coolant"), this.menu.heaterInput(), RbmkComponentBlockEntity.HEATER_TANK_CAPACITY));
            } else if (isHovering(126, 24, 16, 58, mouseX, mouseY)) {
                tooltip.addAll(HbmFluidTooltip.forTank(fluid("coolant_hot"), this.menu.heaterOutput(), RbmkComponentBlockEntity.HEATER_TANK_CAPACITY));
            }
        } else if (this.menu.kind() == RbmkComponentBlock.Kind.OUTGASSER) {
            if (isHovering(112, 21, 16, 48, mouseX, mouseY)) {
                tooltip.addAll(HbmFluidTooltip.forTank(fluid("tritium"), this.menu.outgasserGas(), RbmkComponentBlockEntity.OUTGASSER_GAS_CAPACITY));
            } else if (isHovering(82, 50, 13, 6, mouseX, mouseY)) {
                tooltip.add(Component.translatable("rbmk.outgasser.progress", this.menu.outgasserProgress(), RbmkComponentBlockEntity.OUTGASSER_DURATION));
            }
        } else if (this.menu.kind().isAutomaticControl()) {
            if (isHovering(124, 29, 16, 56, mouseX, mouseY)) {
                tooltip.add(Component.translatable("rbmk.control.level", percent(this.menu.controlLevel()) + "% -> " + percent(this.menu.targetControlLevel()) + "%"));
            } else if (isHovering(58, 26, 28, 19, mouseX, mouseY)) {
                tooltip.add(Component.translatable("rbmk.auto.function", autoFunctionName(this.menu.autoFunction())));
            } else if (isHovering(61, 48, 22, 10, mouseX, mouseY)) {
                tooltip.add(Component.translatable("rbmk.auto.select_linear"));
            } else if (isHovering(61, 59, 22, 10, mouseX, mouseY)) {
                tooltip.add(Component.translatable("rbmk.auto.select_quad"));
            } else if (isHovering(61, 70, 22, 10, mouseX, mouseY)) {
                tooltip.add(Component.translatable("rbmk.auto.select_inverse_quad"));
            } else if (isHovering(28, 26, 30, 10, mouseX, mouseY)) {
                tooltip.add(Component.translatable("rbmk.auto.level_upper"));
            } else if (isHovering(28, 37, 30, 10, mouseX, mouseY)) {
                tooltip.add(Component.translatable("rbmk.auto.level_lower"));
            } else if (isHovering(28, 48, 30, 10, mouseX, mouseY)) {
                tooltip.add(Component.translatable("rbmk.auto.heat_upper"));
            } else if (isHovering(28, 59, 30, 10, mouseX, mouseY)) {
                tooltip.add(Component.translatable("rbmk.auto.heat_lower"));
            } else if (isHovering(28, 70, 30, 10, mouseX, mouseY)) {
                tooltip.add(Component.translatable("rbmk.auto.save"));
            }
        } else if (this.menu.kind().isControl()) {
            tooltip.add(Component.translatable("rbmk.control.level", percent(this.menu.controlLevel()) + "% -> " + percent(this.menu.targetControlLevel()) + "%"));
        }
        return tooltip;
    }

    private static Component autoFunctionName(int function) {
        return switch (Math.floorMod(function, 3)) {
            case 1 -> Component.translatable("rbmk.auto.function_quad");
            case 2 -> Component.translatable("rbmk.auto.function_inverse_quad");
            default -> Component.translatable("rbmk.auto.function_linear");
        };
    }

    private RbmkComponentBlockEntity console() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        BlockEntity blockEntity = this.minecraft.level.getBlockEntity(this.menu.pos());
        return blockEntity instanceof RbmkComponentBlockEntity rbmk ? rbmk : null;
    }

    private void renderFluxGraph(GuiGraphics graphics, RbmkComponentBlockEntity console) {
        int highest = Integer.MIN_VALUE;
        int lowest = Integer.MAX_VALUE;
        int size = console.consoleFluxHistorySize();
        for (int index = 0; index < size; index++) {
            int value = console.consoleFluxHistory(index);
            highest = Math.max(highest, value);
            lowest = Math.min(lowest, value);
        }
        if (highest == Integer.MIN_VALUE || size < 2) {
            return;
        }
        int range = Math.max(1, highest - lowest);
        for (int index = 0; index < size - 1; index++) {
            int x1 = this.leftPos + 7 + (int) Math.round(index * 74.0D / size);
            int x2 = this.leftPos + 7 + (int) Math.round((index + 1) * 74.0D / size);
            int y1 = this.topPos + 127 - (int) Math.round((console.consoleFluxHistory(index) - lowest) * 24.0D / range);
            int y2 = this.topPos + 127 - (int) Math.round((console.consoleFluxHistory(index + 1) - lowest) * 24.0D / range);
            drawLine(graphics, x1, y1, x2, y2, 0xFF00FF00);
        }
    }

    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps <= 0) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        for (int step = 0; step <= steps; step++) {
            int x = x1 + (x2 - x1) * step / steps;
            int y = y1 + (y2 - y1) * step / steps;
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static int consoleColumnOffset(RbmkComponentBlock.Kind kind) {
        return switch (kind) {
            case FUEL_ROD, FUEL_ROD_MOD, FUEL_ROD_REASIM, FUEL_ROD_REASIM_MOD -> 10;
            case CONTROL, CONTROL_MOD, CONTROL_REASIM -> 20;
            case CONTROL_AUTO, CONTROL_REASIM_AUTO -> 30;
            case BOILER -> 40;
            case MODERATOR -> 50;
            case ABSORBER -> 60;
            case REFLECTOR -> 70;
            case OUTGASSER -> 80;
            case STORAGE -> 110;
            case COOLER -> 120;
            case HEATER -> 130;
            default -> 0;
        };
    }

    private static String consoleScreenKey(int type) {
        return switch (Math.floorMod(type, 6)) {
            case 1 -> "col_temp";
            case 2 -> "rod_extraction";
            case 3 -> "fuel_depletion";
            case 4 -> "fuel_poison";
            case 5 -> "fuel_temp";
            default -> "none";
        };
    }

    private static int consoleColor(RbmkComponentBlock.Kind kind, int heat) {
        if (kind.isControl()) {
            return 0xFF4CA3FF;
        }
        if (kind.acceptsFuel()) {
            int red = Math.min(255, 80 + heat / 5);
            return 0xFF000000 | red << 16 | 0x004020;
        }
        return switch (kind) {
            case BOILER -> 0xFF87D7FF;
            case HEATER -> 0xFFFFA13A;
            case COOLER -> 0xFF40D0D0;
            case MODERATOR -> 0xFFCFCFCF;
            case ABSORBER -> 0xFF343434;
            case REFLECTOR -> 0xFFE8E8A0;
            case OUTGASSER -> 0xFFB060FF;
            case STORAGE -> 0xFF8A6A40;
            default -> 0xFF606060;
        };
    }

    private ResourceLocation texture() {
        return switch (this.menu.kind()) {
            case AUTOLOADER -> ReinhardtsHBM.id("textures/gui/reactors/gui_rbmk_autoloader.png");
            case BOILER -> ReinhardtsHBM.id("textures/gui/reactors/gui_rbmk_boiler.png");
            case HEATER -> ReinhardtsHBM.id("textures/gui/reactors/gui_rbmk_heater.png");
            case OUTGASSER -> ReinhardtsHBM.id("textures/gui/reactors/gui_rbmk_outgasser.png");
            case STORAGE -> ReinhardtsHBM.id("textures/gui/reactors/gui_rbmk_storage.png");
            case CONSOLE -> ReinhardtsHBM.id("textures/gui/reactors/gui_rbmk_console.png");
            case CONTROL, CONTROL_MOD, CONTROL_REASIM -> ReinhardtsHBM.id("textures/gui/reactors/gui_rbmk_control.png");
            case CONTROL_AUTO, CONTROL_REASIM_AUTO -> ReinhardtsHBM.id("textures/gui/reactors/gui_rbmk_control_auto.png");
            default -> ReinhardtsHBM.id("textures/gui/reactors/gui_rbmk_element.png");
        };
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, HbmFluidDefinition fluid) {
        height = clampPixels(height, bottomY);
        if (height <= 0 || fluid.isNone()) {
            return;
        }
        ResourceLocation fluidTexture = ReinhardtsHBM.id("textures/gui/fluids/" + fluid.name() + ".png");
        if (this.minecraft == null || this.minecraft.getResourceManager().getResource(fluidTexture).isEmpty()) {
            graphics.fill(this.leftPos + x, this.topPos + bottomY - height, this.leftPos + x + width, this.topPos + bottomY, 0xFF000000 | fluid.color());
            return;
        }
        float red = ((fluid.color() >> 16) & 0xFF) / 255.0F;
        float green = ((fluid.color() >> 8) & 0xFF) / 255.0F;
        float blue = (fluid.color() & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        int top = bottomY - height;
        for (int tileY = 0; tileY < height; tileY += 16) {
            int tileHeight = Math.min(16, height - tileY);
            graphics.blit(fluidTexture, this.leftPos + x, this.topPos + top + tileY, 0, 16 - tileHeight, width, tileHeight, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean isHovering(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= this.leftPos + x && mouseX < this.leftPos + x + width
                && mouseY >= this.topPos + y && mouseY < this.topPos + y + height;
    }

    private void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        graphics.blit(texture, x, y, u, v, width, height, TEX_W, TEX_H);
    }

    private int clampPixels(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }

    private HbmFluidDefinition steamFluid() {
        return fluid(switch (this.menu.steamCompression()) {
            case 1 -> "hotsteam";
            case 2 -> "superhotsteam";
            case 3 -> "ultrahotsteam";
            default -> "steam";
        });
    }

    private String percent(int milli) {
        return Integer.toString(Math.round(milli / 10.0F));
    }

    private static final class ClickArea extends AbstractWidget {
        private final Runnable action;

        private ClickArea(int x, int y, int width, int height, Runnable action) {
            super(x, y, width, height, Component.empty());
            this.action = action;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }
}
