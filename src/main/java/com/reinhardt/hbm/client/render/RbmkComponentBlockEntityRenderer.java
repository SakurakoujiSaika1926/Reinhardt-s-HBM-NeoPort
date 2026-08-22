package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.EnumSet;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class RbmkComponentBlockEntityRenderer implements BlockEntityRenderer<RbmkComponentBlockEntity> {
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> MODELS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> SEGMENTS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> TOPS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> GLASS_TOPS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> FUEL_CAPS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> FUEL_INNERS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final ModelResourceLocation FUEL_RODS = MachineModelRenderer.standalone("block/rbmk_rod_rods");
    private static final ModelResourceLocation CONTROL_LID = MachineModelRenderer.standalone("block/rbmk_rods_lid");
    private static final ModelResourceLocation CONTROL_LID_RED = MachineModelRenderer.standalone("block/rbmk_rods_lid_red");
    private static final ModelResourceLocation CONTROL_LID_YELLOW = MachineModelRenderer.standalone("block/rbmk_rods_lid_yellow");
    private static final ModelResourceLocation CONTROL_LID_GREEN = MachineModelRenderer.standalone("block/rbmk_rods_lid_green");
    private static final ModelResourceLocation CONTROL_LID_BLUE = MachineModelRenderer.standalone("block/rbmk_rods_lid_blue");
    private static final ModelResourceLocation CONTROL_LID_PURPLE = MachineModelRenderer.standalone("block/rbmk_rods_lid_purple");
    private static final ModelResourceLocation CONTROL_AUTO_LID = MachineModelRenderer.standalone("block/rbmk_rods_lid_auto");
    private static final ModelResourceLocation AUTOLOADER_BASE = MachineModelRenderer.standalone("block/rbmk_autoloader_base");
    private static final ModelResourceLocation AUTOLOADER_PISTON = MachineModelRenderer.standalone("block/rbmk_autoloader_piston");
    private static final ModelResourceLocation BUTTON_SOCKET = MachineModelRenderer.standalone("block/rbmk_button_socket");
    private static final ModelResourceLocation BUTTON_BUTTON = MachineModelRenderer.standalone("block/rbmk_button_button");
    private static final ModelResourceLocation MINI_PANEL_BASE = MachineModelRenderer.standalone("block/rbmk_mini_panel_base");
    private static final ModelResourceLocation GAUGE_BASE = MachineModelRenderer.standalone("block/rbmk_gauge_base");
    private static final ModelResourceLocation GAUGE_NEEDLE = MachineModelRenderer.standalone("block/rbmk_gauge_needle");
    private static final ModelResourceLocation INDICATOR_BASE = MachineModelRenderer.standalone("block/rbmk_indicator_base");
    private static final ModelResourceLocation INDICATOR_LIGHT = MachineModelRenderer.standalone("block/rbmk_indicator_light");
    private static final ModelResourceLocation LEVER_BASE = MachineModelRenderer.standalone("block/rbmk_lever_base");
    private static final ModelResourceLocation LEVER_HANDLE = MachineModelRenderer.standalone("block/rbmk_lever_handle");
    private static final ModelResourceLocation CRANE_GIRDER = MachineModelRenderer.standalone("block/rbmk_crane_girder");
    private static final ModelResourceLocation CRANE_MAIN = MachineModelRenderer.standalone("block/rbmk_crane_main");
    private static final ModelResourceLocation CRANE_TUBE = MachineModelRenderer.standalone("block/rbmk_crane_tube");
    private static final ModelResourceLocation CRANE_CARRIAGE = MachineModelRenderer.standalone("block/rbmk_crane_carriage");
    private static final ModelResourceLocation CRANE_LIFT = MachineModelRenderer.standalone("block/rbmk_crane_lift");
    private static final ModelResourceLocation CRANE_CONSOLE_BODY = MachineModelRenderer.standalone("block/rbmk_crane_console_body");
    private static final ModelResourceLocation CRANE_CONSOLE_JOYSTICK = MachineModelRenderer.standalone("block/rbmk_crane_console_joystick");
    private static final ModelResourceLocation CRANE_CONSOLE_METER1 = MachineModelRenderer.standalone("block/rbmk_crane_console_meter1");
    private static final ModelResourceLocation CRANE_CONSOLE_METER2 = MachineModelRenderer.standalone("block/rbmk_crane_console_meter2");
    private static final ModelResourceLocation CRANE_CONSOLE_LAMP1 = MachineModelRenderer.standalone("block/rbmk_crane_console_lamp1");
    private static final ModelResourceLocation CRANE_CONSOLE_LAMP2 = MachineModelRenderer.standalone("block/rbmk_crane_console_lamp2");

    static {
        for (RbmkComponentBlock.Kind kind : RbmkComponentBlock.Kind.values()) {
            String base = "block/rbmk_" + kind.getSerializedName();
            MODELS.put(kind, MachineModelRenderer.standalone(base));
            if (kind.isColumn()) {
                SEGMENTS.put(kind, MachineModelRenderer.standalone(base + "_segment"));
                TOPS.put(kind, MachineModelRenderer.standalone(base + "_top"));
                if (!kind.isControl() && !kind.hasTopPipes()) {
                    GLASS_TOPS.put(kind, MachineModelRenderer.standalone(base + "_glass_top"));
                }
            }
            if (kind.acceptsFuel()) {
                FUEL_CAPS.put(kind, MachineModelRenderer.standalone(base + "_cap"));
                FUEL_INNERS.put(kind, MachineModelRenderer.standalone(base + "_inner"));
            }
        }
    }

    public RbmkComponentBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation model : MODELS.values()) {
            event.register(model);
        }
        for (ModelResourceLocation model : SEGMENTS.values()) {
            event.register(model);
        }
        for (ModelResourceLocation model : TOPS.values()) {
            event.register(model);
        }
        for (ModelResourceLocation model : GLASS_TOPS.values()) {
            event.register(model);
        }
        for (ModelResourceLocation model : FUEL_CAPS.values()) {
            event.register(model);
        }
        for (ModelResourceLocation model : FUEL_INNERS.values()) {
            event.register(model);
        }
        event.register(FUEL_RODS);
        event.register(CONTROL_LID);
        event.register(CONTROL_LID_RED);
        event.register(CONTROL_LID_YELLOW);
        event.register(CONTROL_LID_GREEN);
        event.register(CONTROL_LID_BLUE);
        event.register(CONTROL_LID_PURPLE);
        event.register(CONTROL_AUTO_LID);
        event.register(AUTOLOADER_BASE);
        event.register(AUTOLOADER_PISTON);
        event.register(BUTTON_SOCKET);
        event.register(BUTTON_BUTTON);
        event.register(MINI_PANEL_BASE);
        event.register(GAUGE_BASE);
        event.register(GAUGE_NEEDLE);
        event.register(INDICATOR_BASE);
        event.register(INDICATOR_LIGHT);
        event.register(LEVER_BASE);
        event.register(LEVER_HANDLE);
        event.register(CRANE_GIRDER);
        event.register(CRANE_MAIN);
        event.register(CRANE_TUBE);
        event.register(CRANE_CARRIAGE);
        event.register(CRANE_LIFT);
        event.register(CRANE_CONSOLE_BODY);
        event.register(CRANE_CONSOLE_JOYSTICK);
        event.register(CRANE_CONSOLE_METER1);
        event.register(CRANE_CONSOLE_METER2);
        event.register(CRANE_CONSOLE_LAMP1);
        event.register(CRANE_CONSOLE_LAMP2);
    }

    @Override
    public void render(RbmkComponentBlockEntity rbmk, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        RbmkComponentBlock.Kind kind = rbmk.kind();
        ModelResourceLocation location = MODELS.get(kind);
        if (location == null) {
            return;
        }
        BlockState state = rbmk.getBlockState();
        if (kind == RbmkComponentBlock.Kind.AUTOLOADER) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(AUTOLOADER_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.pushPose();
            poseStack.translate(0.0F, 4.0F - rbmk.autoloaderPiston(partialTick) * 4.0D, 0.0F);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(AUTOLOADER_PISTON), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
            poseStack.popPose();
            return;
        }
        if (!kind.isColumn()) {
            poseStack.pushPose();
            if (kind == RbmkComponentBlock.Kind.CONSOLE || kind == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
                poseStack.translate(0.5F, 0.0F, 0.5F);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rbmkConsoleYaw(state.getValue(RbmkComponentBlock.FACING))));
                poseStack.translate(0.5F, 0.0F, 0.0F);
            }
            if (kind == RbmkComponentBlock.Kind.CONSOLE) {
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(location), poseStack, bufferSource, state, packedLight, packedOverlay);
                renderConsoleBoard(rbmk, poseStack, bufferSource);
                renderConsoleScreens(rbmk, poseStack, bufferSource);
            } else if (kind == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
                renderCraneConsole(rbmk, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else if (kind == RbmkComponentBlock.Kind.DISPLAY) {
                renderMiniPanelBase(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderDisplayBoard(rbmk, poseStack, bufferSource);
            } else if (kind == RbmkComponentBlock.Kind.DISPLAY_BLANK) {
                renderMiniPanelBase(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else if (kind == RbmkComponentBlock.Kind.GAUGE) {
                renderMiniPanelBase(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderGaugePanel(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay, partialTick);
            } else if (kind == RbmkComponentBlock.Kind.GRAPH) {
                renderMiniPanelBase(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderGraphPanel(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else if (kind == RbmkComponentBlock.Kind.LEVER) {
                renderMiniPanelBase(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderLeverPanel(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay, partialTick);
            } else if (kind == RbmkComponentBlock.Kind.INDICATOR) {
                renderMiniPanelBase(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderIndicatorPanel(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else if (kind == RbmkComponentBlock.Kind.KEY_PAD) {
                renderMiniPanelBase(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderKeypadPanel(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else if (kind == RbmkComponentBlock.Kind.NUMITRON) {
                renderMiniPanelBase(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderNumitronPanel(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else if (kind == RbmkComponentBlock.Kind.TERMINAL) {
                renderMiniPanelBase(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
                renderTerminalPanel(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
            } else {
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(location), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            poseStack.popPose();
            if (kind == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
                renderCraneStructure(rbmk, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            return;
        }

        ModelResourceLocation segment = SEGMENTS.get(kind);
        ModelResourceLocation top = TOPS.get(kind);
        int height = RbmkComponentBlock.columnHeight(rbmk.getLevel());
        int topSegment = height - 1;
        for (int y = 0; y < height; y++) {
            ModelResourceLocation topModel = y == topSegment ? topModel(rbmk, kind, top) : null;
            poseStack.pushPose();
            poseStack.translate(0.0F, y, 0.0F);
            MachineModelRenderer.renderExceptDirections(
                    MachineModelRenderer.model(segment),
                    poseStack,
                    bufferSource,
                    state,
                    packedLight,
                    packedOverlay,
                    hiddenColumnFaces(rbmk, y, topSegment, topModel != null)
            );
            renderFuelChannelInsert(rbmk, kind, poseStack, bufferSource, state, packedLight, packedOverlay, partialTick);
            if (topModel != null) {
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(topModel), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            poseStack.popPose();
        }
        if (kind.acceptsFuel() && rbmk.hasFuelRod()) {
            renderFuelRods(rbmk, poseStack, bufferSource, state, packedLight, packedOverlay);
            renderCherenkov(rbmk, poseStack, bufferSource, packedOverlay);
        }
        if (kind.isControl()) {
            renderControlLid(rbmk, kind, poseStack, bufferSource, state, packedLight, packedOverlay, partialTick);
        }
    }

    private static void renderMiniPanelBase(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        MachineModelRenderer.orientYaw(poseStack, rbmkDisplayYaw(rbmk.getBlockState().getValue(RbmkComponentBlock.FACING)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MINI_PANEL_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderGaugePanel(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, float partialTick) {
        poseStack.pushPose();
        orientPanel(poseStack, rbmk);
        for (int slot = 0; slot < 4; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, (slot / 2) * -0.5D + 0.25D, (slot % 2) * -0.5D + 0.25D);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(GAUGE_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.pushPose();
            double value = slot == 0 ? rbmk.heat() : slot == 1 ? rbmk.lastFlux() : slot == 2 ? rbmk.controlLevel() * 100.0D : rbmk.redstoneLevel();
            double upper = slot == 0 ? Math.max(1.0D, rbmk.maxConsoleHeat()) : slot == 1 ? Math.max(1.0D, rbmk.lastFlux()) : slot == 2 ? 100.0D : 15.0D;
            double angle = Math.max(0.0D, Math.min(80.0D, value / upper * 50.0D));
            poseStack.translate(0.0D, 0.4375D, -0.125D);
            poseStack.mulPose(com.mojang.math.Axis.XN.rotationDegrees((float) angle - 85.0F));
            poseStack.translate(0.0D, -0.4375D, 0.125D);
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(GAUGE_NEEDLE), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, gaugeNeedleColor(slot));
            poseStack.popPose();
            drawPanelLabel(defaultGaugeLabel(slot), poseStack, bufferSource, 0.01D, 0.3125D, 0.0D, 0.4F, 0x00FF00);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderGraphPanel(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientPanel(poseStack, rbmk);
        for (int slot = 0; slot < 2; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, slot * -0.5D + 0.25D, 0.0D);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODELS.get(RbmkComponentBlock.Kind.NUMITRON)), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderNumitronPanel(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientPanel(poseStack, rbmk);
        for (int slot = 0; slot < 2; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, slot * -0.5D + 0.25D, 0.0D);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODELS.get(RbmkComponentBlock.Kind.NUMITRON)), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderTerminalPanel(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientPanel(poseStack, rbmk);
        poseStack.translate(0.25D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODELS.get(RbmkComponentBlock.Kind.TERMINAL)), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderLeverPanel(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, float partialTick) {
        poseStack.pushPose();
        orientPanel(poseStack, rbmk);
        for (int slot = 0; slot < 2; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, 0.0D, slot * -0.5D + 0.25D);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LEVER_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
            double progress = slot == 0 ? rbmk.redstoneLevel() / 15.0D : rbmk.controlLevel();
            poseStack.pushPose();
            poseStack.translate(0.125D, 0.5625D, 0.0D);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) (-180.0D * progress)));
            poseStack.translate(-0.125D, -0.5625D, 0.0D);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LEVER_HANDLE), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
            drawPanelLabel(slot == 0 ? "Signal" : "Control", poseStack, bufferSource, 0.01D, 0.0625D, 0.0D, 0.4F, 0x00FF00);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderIndicatorPanel(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientPanel(poseStack, rbmk);
        for (int slot = 0; slot < 6; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, (slot / 2) * -0.3125D + 0.3125D, (slot % 2) * -0.5D + 0.25D);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(INDICATOR_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
            boolean lit = slot == 0 ? rbmk.heat() > 0.0D : slot == 1 ? rbmk.lastFlux() > 0.0D : slot == 2 ? rbmk.controlLevel() > 0.0D : slot == 3 ? rbmk.redstoneLevel() > 0 : slot == 4 ? rbmk.waterAmount() > 0 : rbmk.steamAmount() > 0;
            int color = (slot & 1) == 0 ? 0xFFFF0000 : 0xFFFFFF00;
            if (!lit) {
                color = dimColor(color, 0.35F);
            }
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(INDICATOR_LIGHT), poseStack, bufferSource, state, lit ? LightTexture.FULL_BRIGHT : packedLight, packedOverlay, color);
            drawPanelLabel(defaultIndicatorLabel(slot), poseStack, bufferSource, 0.0725D, 0.5D, 0.0D, 0.3F, 0x000000);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderKeypadPanel(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        orientPanel(poseStack, rbmk);
        for (int slot = 0; slot < 4; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, (slot / 2) * -0.5D + 0.25D, (slot % 2) * -0.5D + 0.25D);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BUTTON_SOCKET), poseStack, bufferSource, state, packedLight, packedOverlay);
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(BUTTON_BUTTON), poseStack, bufferSource, state, packedLight, packedOverlay, keypadColor(slot));
            drawPanelLabel(Integer.toString(slot + 1), poseStack, bufferSource, 0.01D, 0.3125D, 0.0D, 0.4F, 0x00FF00);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void orientPanel(PoseStack poseStack, RbmkComponentBlockEntity rbmk) {
        BlockState state = rbmk.getBlockState();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rbmkDisplayYaw(state.getValue(RbmkComponentBlock.FACING))));
    }

    private static void drawPanelLabel(String text, PoseStack poseStack, MultiBufferSource bufferSource, double x, double y, double z, float maxWidth, int color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        int width = font.width(text);
        float scale = Math.min(0.0125F, maxWidth / Math.max(width, 1));
        drawPanelText(text, poseStack, bufferSource, 0.0D, 0.0D, 0.0D, scale, color, true);
        poseStack.popPose();
    }

    private static void drawPanelText(String text, PoseStack poseStack, MultiBufferSource bufferSource, double x, double y, double z, float scale, int color, boolean centered) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        int width = font.width(text);
        int height = font.lineHeight;
        poseStack.scale(scale, -scale, scale);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        font.drawInBatch(text, centered ? -width / 2.0F : 0.0F, -height / 2.0F, color, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static int gaugeNeedleColor(int slot) {
        return switch (slot) {
            case 0 -> 0xFF800000;
            case 1 -> 0xFF804000;
            case 2 -> 0xFF808000;
            default -> 0xFF000080;
        };
    }

    private static int keypadColor(int slot) {
        return switch (slot) {
            case 0 -> 0xFFFF0000;
            case 1 -> 0xFFFFFF00;
            case 2 -> 0xFF00FF00;
            default -> 0xFF0080FF;
        };
    }

    private static int dimColor(int argb, float multiplier) {
        int red = Math.round(((argb >> 16) & 0xFF) * multiplier);
        int green = Math.round(((argb >> 8) & 0xFF) * multiplier);
        int blue = Math.round((argb & 0xFF) * multiplier);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static String defaultGaugeLabel(int slot) {
        return switch (slot) {
            case 0 -> "Heat";
            case 1 -> "Flux";
            case 2 -> "Rod";
            default -> "Redstone";
        };
    }

    private static String defaultIndicatorLabel(int slot) {
        return switch (slot) {
            case 0 -> "Heat";
            case 1 -> "Flux";
            case 2 -> "Rod";
            case 3 -> "Signal";
            case 4 -> "Water";
            default -> "Steam";
        };
    }

    private static void renderControlLid(RbmkComponentBlockEntity rbmk, RbmkComponentBlock.Kind kind, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, float partialTick) {
        poseStack.pushPose();
        poseStack.translate(0.5D, RbmkComponentBlock.columnHeight(rbmk.getLevel()) - 1.0D + rbmk.controlLevel(partialTick), 0.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(controlLidModel(rbmk, kind)), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static ModelResourceLocation controlLidModel(RbmkComponentBlockEntity rbmk, RbmkComponentBlock.Kind kind) {
        if (kind.isAutomaticControl()) {
            return CONTROL_AUTO_LID;
        }
        return switch (rbmk.colorGroup()) {
            case 0 -> CONTROL_LID_RED;
            case 1 -> CONTROL_LID_YELLOW;
            case 2 -> CONTROL_LID_GREEN;
            case 3 -> CONTROL_LID_BLUE;
            case 4 -> CONTROL_LID_PURPLE;
            default -> CONTROL_LID;
        };
    }

    private static EnumSet<Direction> hiddenColumnFaces(RbmkComponentBlockEntity rbmk, int yOffset, int segments, boolean hasTopModel) {
        EnumSet<Direction> hidden = EnumSet.noneOf(Direction.class);
        if (yOffset > 0) {
            hidden.add(Direction.DOWN);
        }
        if (yOffset < segments || hasTopModel) {
            hidden.add(Direction.UP);
        }
        if (rbmk.getLevel() == null) {
            return hidden;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (rbmk.getLevel().getBlockEntity(rbmk.getBlockPos().relative(direction)) instanceof RbmkComponentBlockEntity other
                    && other.kind().isColumn()) {
                hidden.add(direction);
            }
        }
        return hidden;
    }

    private static void renderFuelChannelInsert(RbmkComponentBlockEntity rbmk, RbmkComponentBlock.Kind kind, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, float partialTick) {
        ModelResourceLocation cap = FUEL_CAPS.get(kind);
        ModelResourceLocation inner = FUEL_INNERS.get(kind);
        if (cap == null || inner == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(cap), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(inner), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderFuelRods(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        int color = rbmk.fuelChannelColor();
        int height = RbmkComponentBlock.columnHeight(rbmk.getLevel());
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        for (int y = 0; y < height; y++) {
            poseStack.pushPose();
            poseStack.translate(0.0D, y, 0.0D);
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(FUEL_RODS), poseStack, bufferSource, state, packedLight, packedOverlay, 0xFF000000 | color);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderCherenkov(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        if (rbmk.lastFlux() <= 5.0D) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        PoseStack.Pose pose = poseStack.last();
        double max = RbmkComponentBlock.columnHeight(rbmk.getLevel());
        for (double y = 0.75D; y <= max; y += 0.25D) {
            cherenkovVertex(consumer, pose, -0.5D, y, -0.5D, packedOverlay);
            cherenkovVertex(consumer, pose, -0.5D, y, 0.5D, packedOverlay);
            cherenkovVertex(consumer, pose, 0.5D, y, 0.5D, packedOverlay);
            cherenkovVertex(consumer, pose, 0.5D, y, -0.5D, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void cherenkovVertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, int packedOverlay) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(102, 230, 255, 26)
                .setUv(0.0F, 0.0F)
                .setOverlay(packedOverlay == 0 ? OverlayTexture.NO_OVERLAY : packedOverlay)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static ModelResourceLocation topModel(RbmkComponentBlockEntity rbmk, RbmkComponentBlock.Kind kind, ModelResourceLocation defaultTop) {
        if (kind.hasTopPipes() || kind.isControl()) {
            return defaultTop;
        }
        if (!rbmk.hasLid()) {
            return null;
        }
        if (rbmk.lidType() == RbmkComponentBlockEntity.LidType.GLASS) {
            return GLASS_TOPS.get(kind);
        }
        return defaultTop;
    }

    private static void renderCraneConsole(RbmkComponentBlockEntity rbmk, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRANE_CONSOLE_BODY), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.75D, 1.0D, 0.0D);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) rbmk.craneTiltFront(partialTick)));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) rbmk.craneTiltLeft(partialTick)));
        poseStack.translate(-0.75D, -1.015D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRANE_CONSOLE_JOYSTICK), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        double meterWave = Math.sin((System.currentTimeMillis() * 0.01D) % 360.0D) * 180.0D / Math.PI * 0.05D;
        renderCraneMeter(CRANE_CONSOLE_METER1, 0.75D, meterWave + 135.0D - 270.0D * rbmk.craneLoadedHeat(), poseStack, bufferSource, state, packedLight, packedOverlay);
        renderCraneMeter(CRANE_CONSOLE_METER2, 0.25D, meterWave + 135.0D - 270.0D * rbmk.craneLoadedEnrichment(), poseStack, bufferSource, state, packedLight, packedOverlay);

        int lamp1 = rbmk.craneIsLoading() ? 0xFFCCCC00 : rbmk.craneHasLoadedItem() ? 0xFF00FF00 : 0xFF001900;
        int lamp2 = rbmk.craneAboveValidTarget() ? 0xFF00FF00 : 0xFFFF0000;
        MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(CRANE_CONSOLE_LAMP1), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, lamp1);
        MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(CRANE_CONSOLE_LAMP2), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, lamp2);
    }

    private static void renderCraneMeter(ModelResourceLocation model, double z, double degrees, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.25D, z);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) degrees));
        poseStack.translate(0.0D, -1.25D, -z);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderCraneStructure(RbmkComponentBlockEntity rbmk, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (!rbmk.craneIsSetUp() || rbmk.craneCenter() == null) {
            return;
        }
        poseStack.pushPose();
        double cranePosX = -rbmk.getBlockPos().getX() + rbmk.craneCenter().getX();
        double cranePosY = -rbmk.getBlockPos().getY() + rbmk.craneCenter().getY() + 1.0D;
        double cranePosZ = -rbmk.getBlockPos().getZ() + rbmk.craneCenter().getZ();
        poseStack.translate(cranePosX, cranePosY, cranePosZ);
        poseStack.translate(0.5D, -1.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rbmkConsoleYaw(state.getValue(RbmkComponentBlock.FACING))));

        double posFront = rbmk.cranePosFront(partialTick);
        double posLeft = rbmk.cranePosLeft(partialTick);
        poseStack.translate(-posFront, 0.0D, posLeft);
        int craneRotationOffset = rbmk.craneRotationOffset();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(craneRotationOffset));

        poseStack.pushPose();
        int girderSpan = 0;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-craneRotationOffset));
        switch (Math.floorMod(craneRotationOffset, 360)) {
            case 0 -> {
                girderSpan = rbmk.craneSpanF() + rbmk.craneSpanB() + 1;
                poseStack.translate(posFront + rbmk.craneSpanB(), 0.0D, 0.0D);
            }
            case 90 -> {
                girderSpan = rbmk.craneSpanL() + rbmk.craneSpanR() + 1;
                poseStack.translate(0.0D, 0.0D, -posLeft - rbmk.craneSpanR());
            }
            case 180 -> {
                girderSpan = rbmk.craneSpanF() + rbmk.craneSpanB() + 1;
                poseStack.translate(posFront - rbmk.craneSpanF(), 0.0D, 0.0D);
            }
            case 270 -> {
                girderSpan = rbmk.craneSpanL() + rbmk.craneSpanR() + 1;
                poseStack.translate(0.0D, 0.0D, -posLeft + rbmk.craneSpanL());
            }
            default -> {
            }
        }
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(craneRotationOffset));
        for (int i = 0; i < girderSpan; i++) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRANE_GIRDER), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.translate(-1.0D, 0.0D, 0.0D);
        }
        poseStack.popPose();

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRANE_MAIN), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        int tubeCount = Math.max(0, rbmk.craneHeight() - 6);
        for (int i = 0; i < tubeCount; i++) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRANE_TUBE), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.translate(0.0D, 1.0D, 0.0D);
        }
        poseStack.translate(0.0D, -1.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRANE_CARRIAGE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.translate(0.0D, -3.25D * (1.0D - rbmk.craneProgress(partialTick)), 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRANE_LIFT), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static float rbmkConsoleYaw(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            case EAST -> 0.0F;
            default -> 0.0F;
        };
    }

    private static void renderConsoleScreens(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource) {
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(-0.42F, 3.5F, 1.75F);
        for (int slot = 0; slot < 6; slot++) {
            Component text = consoleScreenText(rbmk, slot);
            if (text == null) {
                continue;
            }
            poseStack.pushPose();
            if (slot % 2 == 1) {
                poseStack.translate(0.0F, 0.0F, -3.5F);
            }
            poseStack.translate(0.0F, -0.75F * (slot / 2), 0.0F);
            int width = font.width(text);
            int height = font.lineHeight;
            float scale = Math.min(0.03F, 0.8F / Math.max(width, 1));
            poseStack.scale(scale, -scale, scale);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            font.drawInBatch(
                    text,
                    -width / 2.0F,
                    -height / 2.0F,
                    0x00FF00,
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0,
                    LightTexture.FULL_BRIGHT
            );
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static Component consoleScreenText(RbmkComponentBlockEntity rbmk, int slot) {
        RbmkComponentBlockEntity.ConsoleScreenType type = RbmkComponentBlockEntity.ConsoleScreenType.byOrdinal(rbmk.consoleScreenType(slot));
        if (type == RbmkComponentBlockEntity.ConsoleScreenType.NONE) {
            return null;
        }
        String value = formatTenths(rbmk.consoleScreenDisplay(slot));
        return switch (type) {
            case COL_TEMP -> Component.translatable("rbmk.screen.temp", value);
            case FUEL_DEPLETION -> Component.translatable("rbmk.screen.depletion", value);
            case FUEL_POISON -> Component.translatable("rbmk.screen.xenon", value);
            case FUEL_TEMP -> Component.translatable("rbmk.screen.core", value);
            case ROD_EXTRACTION -> Component.translatable("rbmk.screen.rod", value);
            case NONE -> null;
        };
    }

    private static String formatTenths(int tenths) {
        return tenths % 10 == 0
                ? Integer.toString(tenths / 10)
                : String.format(Locale.ROOT, "%.1f", tenths / 10.0D);
    }

    private static void renderConsoleBoard(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose pose = poseStack.last();
        for (int index = 0; index < RbmkComponentBlockEntity.CONSOLE_COLUMN_COUNT; index++) {
            int kindOrdinal = rbmk.consoleKind(index);
            if (kindOrdinal < 0 || kindOrdinal >= RbmkComponentBlock.Kind.values().length) {
                continue;
            }
            double x = -0.3725D;
            double y = -(index / RbmkComponentBlockEntity.CONSOLE_GRID_SIZE) * 0.125D + 3.625D;
            double z = -(index % RbmkComponentBlockEntity.CONSOLE_GRID_SIZE) * 0.125D + 0.125D * 7.0D;
            int color = consoleBoardColor(rbmk, index);
            addConsoleBoardColumn(consumer, pose, x, y, z, color);
            RbmkComponentBlock.Kind kind = RbmkComponentBlock.Kind.values()[kindOrdinal];
            if (kind.acceptsFuel()) {
                int green = 64 + Math.min(191, Math.max(0, rbmk.consoleFuelDepletion(index) * 191 / 1000));
                addConsoleBoardDot(consumer, pose, x + 0.01D, y, z, 0, green, 0);
            } else if (kind.isControl()) {
                int level = Math.min(255, Math.max(0, rbmk.consoleControl(index) * 255 / 100));
                if (kind == RbmkComponentBlock.Kind.CONTROL_AUTO) {
                    addConsoleBoardDot(consumer, pose, x + 0.01D, y, z, level, 0, level);
                } else {
                    addConsoleBoardDot(consumer, pose, x + 0.01D, y, z, level, level, 0);
                }
            }
        }
    }

    private static void renderDisplayBoard(RbmkComponentBlockEntity rbmk, PoseStack poseStack, MultiBufferSource bufferSource) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rbmkDisplayYaw(rbmk.getBlockState().getValue(RbmkComponentBlock.FACING))));
        poseStack.translate(0.0D, 0.5D, 0.0D);
        poseStack.scale(1.0F, 8.0F / 7.0F, 8.0F / 7.0F);
        poseStack.translate(0.0D, -0.5D, 0.0D);
        PoseStack.Pose pose = poseStack.last();
        for (int index = 0; index < RbmkComponentBlockEntity.DISPLAY_COLUMN_COUNT; index++) {
            int kindOrdinal = rbmk.displayKind(index);
            if (kindOrdinal < 0 || kindOrdinal >= RbmkComponentBlock.Kind.values().length) {
                continue;
            }
            double x = 0.28125D;
            double y = -(index / RbmkComponentBlockEntity.DISPLAY_GRID_SIZE) * 0.125D + 0.875D;
            double z = -(index % RbmkComponentBlockEntity.DISPLAY_GRID_SIZE) * 0.125D + 0.125D * 3.0D;
            int color = displayBoardColor(rbmk, index);
            addConsoleBoardColumn(consumer, pose, x, y, z, color);
            RbmkComponentBlock.Kind kind = RbmkComponentBlock.Kind.values()[kindOrdinal];
            if (kind.acceptsFuel()) {
                int green = 64 + Math.min(191, Math.max(0, rbmk.displayFuelDepletion(index) * 191 / 1000));
                addConsoleBoardDot(consumer, pose, x + 0.01D, y, z, 0, green, 0);
            } else if (kind.isControl()) {
                int level = Math.min(255, Math.max(0, rbmk.displayControl(index) * 255 / 100));
                if (kind == RbmkComponentBlock.Kind.CONTROL_AUTO) {
                    addConsoleBoardDot(consumer, pose, x + 0.01D, y, z, level, 0, level);
                } else {
                    addConsoleBoardDot(consumer, pose, x + 0.01D, y, z, level, level, 0);
                }
            }
        }
        poseStack.popPose();
    }

    private static int consoleBoardColor(RbmkComponentBlockEntity rbmk, int index) {
        if (rbmk.consoleCraneIndicator(index) > 0) {
            return 0xFFFFFF00;
        }
        int group = rbmk.consoleColorGroup(index);
        if (group >= 0) {
            return switch (group) {
                case 0 -> 0xFFFF0000;
                case 1 -> 0xFFFFFF00;
                case 2 -> 0xFF008000;
                case 3 -> 0xFF0000FF;
                case 4 -> 0xFF8000FF;
                default -> 0xFFFFFFFF;
            };
        }
        int maxHeat = Math.max(1, rbmk.consoleMaxHeat(index));
        double heat = Math.max(0.0D, Math.min(1.0D, rbmk.consoleHeat(index) / (double) maxHeat));
        double base = 0.65D + (index % 2) * 0.05D;
        int red = (int) Math.round((base + (1.0D - base) * heat) * 255.0D);
        int gb = (int) Math.round(base * 255.0D);
        return 0xFF000000 | (red << 16) | (gb << 8) | gb;
    }

    private static int displayBoardColor(RbmkComponentBlockEntity rbmk, int index) {
        if (rbmk.displayCraneIndicator(index) > 0) {
            return 0xFFFFFF00;
        }
        int group = rbmk.displayColorGroup(index);
        if (group >= 0) {
            return switch (group) {
                case 0 -> 0xFFFF0000;
                case 1 -> 0xFFFFFF00;
                case 2 -> 0xFF008000;
                case 3 -> 0xFF0000FF;
                case 4 -> 0xFF8000FF;
                default -> 0xFFFFFFFF;
            };
        }
        int maxHeat = Math.max(1, rbmk.displayMaxHeat(index));
        double heat = Math.max(0.0D, Math.min(1.0D, rbmk.displayHeat(index) / (double) maxHeat));
        double base = 0.65D + (index % 2) * 0.05D;
        int red = (int) Math.round((base + (1.0D - base) * heat) * 255.0D);
        int gb = (int) Math.round(base * 255.0D);
        return 0xFF000000 | (red << 16) | (gb << 8) | gb;
    }

    private static void addConsoleBoardColumn(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, int argb) {
        double width = 0.0625D * 0.75D;
        addConsoleBoardVertex(consumer, pose, x, y + width, z - width, argb);
        addConsoleBoardVertex(consumer, pose, x, y + width, z + width, argb);
        addConsoleBoardVertex(consumer, pose, x, y - width, z + width, argb);
        addConsoleBoardVertex(consumer, pose, x, y - width, z - width, argb);
    }

    private static void addConsoleBoardDot(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, int red, int green, int blue) {
        double width = 0.03125D;
        double edge = 0.022097D;
        int argb = 0xFF000000 | (red << 16) | (green << 8) | blue;
        addConsoleBoardVertex(consumer, pose, x, y + width, z, argb);
        addConsoleBoardVertex(consumer, pose, x, y + edge, z + edge, argb);
        addConsoleBoardVertex(consumer, pose, x, y, z + width, argb);
        addConsoleBoardVertex(consumer, pose, x, y - edge, z + edge, argb);
        addConsoleBoardVertex(consumer, pose, x, y + edge, z - edge, argb);
        addConsoleBoardVertex(consumer, pose, x, y + width, z, argb);
        addConsoleBoardVertex(consumer, pose, x, y - edge, z - edge, argb);
        addConsoleBoardVertex(consumer, pose, x, y, z - width, argb);
        addConsoleBoardVertex(consumer, pose, x, y + width, z, argb);
        addConsoleBoardVertex(consumer, pose, x, y - edge, z + edge, argb);
        addConsoleBoardVertex(consumer, pose, x, y - width, z, argb);
        addConsoleBoardVertex(consumer, pose, x, y - edge, z - edge, argb);
    }

    private static void addConsoleBoardVertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, int argb) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(argb)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 1.0F, 0.0F, 0.0F);
    }

    private static float rbmkDisplayYaw(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            case EAST -> 0.0F;
            default -> 0.0F;
        };
    }

    @Override
    public AABB getRenderBoundingBox(RbmkComponentBlockEntity blockEntity) {
        if (blockEntity.kind() == RbmkComponentBlock.Kind.CONSOLE || blockEntity.kind() == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            return new AABB(blockEntity.getBlockPos()).inflate(2.0D, 0.0D, 2.0D).expandTowards(0.0D, 4.0D, 0.0D);
        }
        int height = blockEntity.kind() == RbmkComponentBlock.Kind.AUTOLOADER
                ? 9
                : blockEntity.kind().isColumn() ? RbmkComponentBlock.columnHeight(blockEntity.getLevel()) : 1;
        return new AABB(blockEntity.getBlockPos()).expandTowards(0.0D, height, 0.0D);
    }
}
