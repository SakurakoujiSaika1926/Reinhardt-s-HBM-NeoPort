package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.item.RbmkComponentBlockItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.EnumMap;
import java.util.Map;

public final class RbmkComponentItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> MODELS = new EnumMap<>(RbmkComponentBlock.Kind.class);

    private static final ModelResourceLocation MINI_PANEL_BASE = MachineModelRenderer.standalone("block/rbmk_mini_panel_base");
    private static final ModelResourceLocation AUTOLOADER_BASE = MachineModelRenderer.standalone("block/rbmk_autoloader_base");
    private static final ModelResourceLocation AUTOLOADER_PISTON = MachineModelRenderer.standalone("block/rbmk_autoloader_piston");
    private static final ModelResourceLocation BUTTON_SOCKET = MachineModelRenderer.standalone("block/rbmk_button_socket");
    private static final ModelResourceLocation BUTTON_BUTTON = MachineModelRenderer.standalone("block/rbmk_button_button");
    private static final ModelResourceLocation GAUGE_BASE = MachineModelRenderer.standalone("block/rbmk_gauge_base");
    private static final ModelResourceLocation GAUGE_NEEDLE = MachineModelRenderer.standalone("block/rbmk_gauge_needle");
    private static final ModelResourceLocation INDICATOR_BASE = MachineModelRenderer.standalone("block/rbmk_indicator_base");
    private static final ModelResourceLocation INDICATOR_LIGHT = MachineModelRenderer.standalone("block/rbmk_indicator_light");
    private static final ModelResourceLocation LEVER_BASE = MachineModelRenderer.standalone("block/rbmk_lever_base");
    private static final ModelResourceLocation LEVER_HANDLE = MachineModelRenderer.standalone("block/rbmk_lever_handle");
    private static final ModelResourceLocation CRANE_CONSOLE_BODY = MachineModelRenderer.standalone("block/rbmk_crane_console_body");
    private static final ModelResourceLocation CRANE_CONSOLE_JOYSTICK = MachineModelRenderer.standalone("block/rbmk_crane_console_joystick");
    private static final ModelResourceLocation CRANE_CONSOLE_METER1 = MachineModelRenderer.standalone("block/rbmk_crane_console_meter1");
    private static final ModelResourceLocation CRANE_CONSOLE_METER2 = MachineModelRenderer.standalone("block/rbmk_crane_console_meter2");

    static {
        for (RbmkComponentBlock.Kind kind : RbmkComponentBlock.Kind.values()) {
            if (!kind.isColumn()) {
                MODELS.put(kind, MachineModelRenderer.standalone("block/rbmk_" + kind.getSerializedName()));
            }
        }
    }

    public RbmkComponentItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        MODELS.values().forEach(event::register);
        event.register(MINI_PANEL_BASE);
        event.register(AUTOLOADER_BASE);
        event.register(AUTOLOADER_PISTON);
        event.register(BUTTON_SOCKET);
        event.register(BUTTON_BUTTON);
        event.register(GAUGE_BASE);
        event.register(GAUGE_NEEDLE);
        event.register(INDICATOR_BASE);
        event.register(INDICATOR_LIGHT);
        event.register(LEVER_BASE);
        event.register(LEVER_HANDLE);
        event.register(CRANE_CONSOLE_BODY);
        event.register(CRANE_CONSOLE_JOYSTICK);
        event.register(CRANE_CONSOLE_METER1);
        event.register(CRANE_CONSOLE_METER2);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        RbmkComponentBlock.Kind kind = stack.getItem() instanceof RbmkComponentBlockItem item
                ? item.kind()
                : RbmkComponentBlock.Kind.CONSOLE;

        poseStack.pushPose();
        switch (kind) {
            case LOADER -> renderLoaderItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case STEAM_INLET -> renderSteamInletItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case STEAM_OUTLET -> renderSteamOutletItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case AUTOLOADER -> renderAutoloaderItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case CONSOLE -> renderConsoleItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case CRANE_CONSOLE -> renderCraneConsoleItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case DISPLAY -> renderDisplayItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case DISPLAY_BLANK -> renderBlankDisplayItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case GAUGE -> renderGaugeItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case GRAPH -> renderGraphItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case INDICATOR -> renderIndicatorItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case KEY_PAD -> renderKeyPadItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case LEVER -> renderLeverItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case NUMITRON -> renderNumitronItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case TERMINAL -> renderTerminalItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case BLANK -> { }
            case FUEL_ROD -> { }
            case FUEL_ROD_MOD -> { }
            case FUEL_ROD_REASIM -> { }
            case FUEL_ROD_REASIM_MOD -> { }
            case CONTROL -> { }
            case CONTROL_AUTO -> { }
            case CONTROL_MOD -> { }
            case CONTROL_REASIM -> { }
            case CONTROL_REASIM_AUTO -> { }
            case BOILER -> { }
            case HEATER -> { }
            case COOLER -> { }
            case OUTGASSER -> { }
            case STORAGE -> { }
            case MODERATOR -> { }
            case REFLECTOR -> { }
            case ABSORBER -> { }
        }
        poseStack.popPose();
    }

    // Intentionally keep every component's complete item pose independent.
    // The 1.7.10 implementations were owned by the individual block/TESR
    // paths, and folding equal values back into a Kind-driven helper has
    // repeatedly caused unrelated RBMK items to inherit the wrong transform.
    private static void renderLoaderItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_LOADER.get().defaultBlockState();
        applyLoaderItemPose(context, poseStack);
        renderModel(MODELS.get(RbmkComponentBlock.Kind.LOADER), state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyLoaderItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        } else if (context != ItemDisplayContext.GROUND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        float scale = context == ItemDisplayContext.GUI ? 0.78F : 0.52F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    private static void renderSteamInletItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_STEAM_INLET.get().defaultBlockState();
        applySteamInletItemPose(context, poseStack);
        renderModel(MODELS.get(RbmkComponentBlock.Kind.STEAM_INLET), state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applySteamInletItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        } else if (context != ItemDisplayContext.GROUND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        float scale = context == ItemDisplayContext.GUI ? 0.78F : 0.52F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    private static void renderSteamOutletItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_STEAM_OUTLET.get().defaultBlockState();
        applySteamOutletItemPose(context, poseStack);
        renderModel(MODELS.get(RbmkComponentBlock.Kind.STEAM_OUTLET), state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applySteamOutletItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        } else if (context != ItemDisplayContext.GROUND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        float scale = context == ItemDisplayContext.GUI ? 0.78F : 0.52F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    private static void renderAutoloaderItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_AUTOLOADER.get().defaultBlockState();
        applyAutoloaderItemPose(context, poseStack);
        renderModel(AUTOLOADER_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderModel(AUTOLOADER_PISTON, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyAutoloaderItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        } else if (context != ItemDisplayContext.GROUND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        float scale = context == ItemDisplayContext.GUI ? 0.12F : 0.09F;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, -4.5F, 0.0F);
    }

    private static void renderConsoleItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_CONSOLE.get().defaultBlockState();
        applyConsoleItemPose(context, poseStack);
        renderModel(MODELS.get(RbmkComponentBlock.Kind.CONSOLE), state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyConsoleItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(2.5F, 2.5F, 2.5F);
            return;
        }
        poseStack.translate(0.5F, 0.25F, 0.0F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void renderCraneConsoleItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_CRANE_CONSOLE.get().defaultBlockState();
        applyCraneConsoleItemPose(context, poseStack);
        renderCraneConsole(state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyCraneConsoleItemPose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(3.5F, 3.5F, 3.5F);
            return;
        }
        poseStack.translate(0.5F, 0.25F, 0.0F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void renderDisplayItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_DISPLAY.get().defaultBlockState();
        applyDisplayItemPose(context, poseStack);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderModel(MODELS.get(RbmkComponentBlock.Kind.DISPLAY), state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyDisplayItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void renderBlankDisplayItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_DISPLAY_BLANK.get().defaultBlockState();
        applyBlankDisplayItemPose(context, poseStack);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderModel(MODELS.get(RbmkComponentBlock.Kind.DISPLAY_BLANK), state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyBlankDisplayItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void renderGaugeItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_GAUGE.get().defaultBlockState();
        applyGaugeItemPose(context, poseStack);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.translate(-0.25F, -0.5F, -0.5F);
        renderModel(MINI_PANEL_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderGaugeContents(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyGaugeItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void renderGaugeContents(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        for (int slot = 0; slot < 4; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, (slot / 2) * -0.5D + 0.25D, (slot % 2) * -0.5D + 0.25D);
            renderModel(GAUGE_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.4375D, -0.125D);
            poseStack.mulPose(Axis.XN.rotationDegrees(-85.0F));
            poseStack.translate(0.0D, -0.4375D, 0.125D);
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(GAUGE_NEEDLE), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, 0xFF800000);
            poseStack.popPose();
            poseStack.popPose();
        }
    }

    private static void renderGraphItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_GRAPH.get().defaultBlockState();
        applyGraphItemPose(context, poseStack);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.translate(-0.25F, -0.5F, -0.5F);
        renderModel(MINI_PANEL_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderGraphContents(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyGraphItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void renderGraphContents(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ModelResourceLocation numitron = MODELS.get(RbmkComponentBlock.Kind.NUMITRON);
        for (int slot = 0; slot < 2; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, slot * -0.5D + 0.25D, 0.0D);
            renderModel(numitron, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderIndicatorItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_INDICATOR.get().defaultBlockState();
        applyIndicatorItemPose(context, poseStack);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.translate(-0.25F, -0.5F, -0.5F);
        renderModel(MINI_PANEL_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderIndicatorContents(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyIndicatorItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void renderIndicatorContents(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        for (int slot = 0; slot < 6; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, (slot / 2) * -0.3125D + 0.3125D, (slot % 2) * 0.5D - 0.25D);
            renderModel(INDICATOR_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(INDICATOR_LIGHT), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, (slot & 1) == 0 ? 0xFFFF0000 : 0xFFFFFF00);
            poseStack.popPose();
        }
    }

    private static void renderKeyPadItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_KEY_PAD.get().defaultBlockState();
        applyKeyPadItemPose(context, poseStack);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.translate(-0.25F, -0.5F, -0.5F);
        renderModel(MINI_PANEL_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderKeyPadContents(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyKeyPadItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void renderKeyPadContents(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        for (int slot = 0; slot < 4; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, (slot / 2) * -0.5D + 0.25D, (slot % 2) * -0.5D + 0.25D);
            renderModel(BUTTON_SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(BUTTON_BUTTON), poseStack, bufferSource, state, packedLight, packedOverlay, 0xFFA60000);
            poseStack.popPose();
        }
    }

    private static void renderLeverItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_LEVER.get().defaultBlockState();
        applyLeverItemPose(context, poseStack);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.translate(-0.25F, -0.5F, -0.5F);
        renderModel(MINI_PANEL_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderLeverContents(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyLeverItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void renderLeverContents(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        for (int slot = 0; slot < 2; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, 0.0D, slot * -0.5D + 0.25D);
            renderModel(LEVER_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
            renderModel(LEVER_HANDLE, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderNumitronItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_NUMITRON.get().defaultBlockState();
        applyNumitronItemPose(context, poseStack);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.translate(-0.25F, -0.5F, -0.5F);
        renderModel(MINI_PANEL_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderNumitronContents(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyNumitronItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void renderNumitronContents(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ModelResourceLocation numitron = MODELS.get(RbmkComponentBlock.Kind.NUMITRON);
        for (int slot = 0; slot < 2; slot++) {
            poseStack.pushPose();
            poseStack.translate(0.25D, slot * -0.5D + 0.25D, 0.0D);
            renderModel(numitron, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderTerminalItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_TERMINAL.get().defaultBlockState();
        applyTerminalItemPose(context, poseStack);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.translate(-0.25F, -0.5F, -0.5F);
        renderModel(MINI_PANEL_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderTerminalContents(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyTerminalItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> { }
        }
    }

    private static void renderTerminalContents(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.25D, 0.0D, 0.0D);
        renderModel(MODELS.get(RbmkComponentBlock.Kind.TERMINAL), state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderCraneConsole(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderModel(CRANE_CONSOLE_BODY, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderModel(CRANE_CONSOLE_JOYSTICK, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderModel(CRANE_CONSOLE_METER1, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderModel(CRANE_CONSOLE_METER2, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderModel(ModelResourceLocation location, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (location != null) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(location), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
    }
}
