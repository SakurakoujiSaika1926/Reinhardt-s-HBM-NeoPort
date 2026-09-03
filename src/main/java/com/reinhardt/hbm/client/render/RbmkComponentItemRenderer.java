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
        BlockState state = blockState(kind);

        poseStack.pushPose();
        if (kind == RbmkComponentBlock.Kind.DISPLAY || kind == RbmkComponentBlock.Kind.DISPLAY_BLANK) {
            renderLegacyMiniPanelItem(kind, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        if (isLegacyWirelessPanel(kind)) {
            renderLegacyWirelessPanelItem(kind, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        applyLegacyItemTransform(kind, context, poseStack);
        if (kind == RbmkComponentBlock.Kind.CONSOLE) {
            renderModel(MODELS.get(kind), state, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (kind == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            renderCraneConsole(state, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (kind == RbmkComponentBlock.Kind.AUTOLOADER) {
            renderModel(AUTOLOADER_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
            renderModel(AUTOLOADER_PISTON, state, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (kind.isMiniPanel()) {
            renderMiniPanelItem(kind, state, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            renderModel(MODELS.get(kind), state, poseStack, bufferSource, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void renderLegacyMiniPanelItem(
            RbmkComponentBlock.Kind kind,
            BlockState state,
            ItemDisplayContext context,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        // The old item renderer first applies Minecraft's block-item camera,
        // then RBMKMiniPanelBase and finally the subclass controls.
        applyLegacyMiniPanelCameraTransform(context, poseStack);

        poseStack.pushPose();
        // RBMKDisplay inherits RBMKMiniPanelBase#renderInventoryBlock. Use
        // the display block's [4,16] X bounds directly. The imported model's
        // face winding requires the local inverse of the old inventory turn.
        // The display model uses the opposite local face from the wireless
        // panel base after the legacy inventory transform. Keep this fix
        // local to the display item path; the wireless panel renderer has its
        // own legacy orientation below.
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderModel(MODELS.get(kind), state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        if (kind == RbmkComponentBlock.Kind.DISPLAY || kind == RbmkComponentBlock.Kind.DISPLAY_BLANK) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        renderMiniPanelContents(kind, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyLegacyMiniPanelCameraTransform(ItemDisplayContext context, PoseStack poseStack) {
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
            default -> {
            }
        }
    }

    private static void renderLegacyWirelessPanelItem(
            RbmkComponentBlock.Kind kind,
            BlockState state,
            ItemDisplayContext context,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        applyLegacyMiniPanelCameraTransform(context, poseStack);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(90.0F));
        poseStack.translate(-0.25F, -0.5F, -0.5F);
        renderModel(MINI_PANEL_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderMiniPanelContents(kind, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static boolean isLegacyWirelessPanel(RbmkComponentBlock.Kind kind) {
        return switch (kind) {
            case GAUGE, GRAPH, INDICATOR, KEY_PAD, LEVER, NUMITRON, TERMINAL -> true;
            default -> false;
        };
    }

    private static BlockState blockState(RbmkComponentBlock.Kind kind) {
        return switch (kind) {
            case AUTOLOADER -> HbmBlocks.RBMK_AUTOLOADER.get().defaultBlockState();
            case CONSOLE -> HbmBlocks.RBMK_CONSOLE.get().defaultBlockState();
            case CRANE_CONSOLE -> HbmBlocks.RBMK_CRANE_CONSOLE.get().defaultBlockState();
            case DISPLAY -> HbmBlocks.RBMK_DISPLAY.get().defaultBlockState();
            case DISPLAY_BLANK -> HbmBlocks.RBMK_DISPLAY_BLANK.get().defaultBlockState();
            case GAUGE -> HbmBlocks.RBMK_GAUGE.get().defaultBlockState();
            case GRAPH -> HbmBlocks.RBMK_GRAPH.get().defaultBlockState();
            case INDICATOR -> HbmBlocks.RBMK_INDICATOR.get().defaultBlockState();
            case KEY_PAD -> HbmBlocks.RBMK_KEY_PAD.get().defaultBlockState();
            case LEVER -> HbmBlocks.RBMK_LEVER.get().defaultBlockState();
            case NUMITRON -> HbmBlocks.RBMK_NUMITRON.get().defaultBlockState();
            case TERMINAL -> HbmBlocks.RBMK_TERMINAL.get().defaultBlockState();
            case STEAM_INLET -> HbmBlocks.RBMK_STEAM_INLET.get().defaultBlockState();
            case STEAM_OUTLET -> HbmBlocks.RBMK_STEAM_OUTLET.get().defaultBlockState();
            case LOADER -> HbmBlocks.RBMK_LOADER.get().defaultBlockState();
            default -> HbmBlocks.RBMK_LOADER.get().defaultBlockState();
        };
    }

    private static void applyLegacyItemTransform(RbmkComponentBlock.Kind kind, ItemDisplayContext context, PoseStack poseStack) {
        if (kind == RbmkComponentBlock.Kind.CONSOLE || kind == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
            if (context == ItemDisplayContext.GUI) {
                // ItemRenderLibrary has a separate inventory transform for
                // each console; these values are deliberately not shared
                // with the other RBMK component items.
                poseStack.translate(0.0F, -3.0F, 0.0F);
                float scale = kind == RbmkComponentBlock.Kind.CONSOLE ? 2.5F : 3.5F;
                poseStack.scale(scale, scale, scale);
            }
            return;
        }

        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        } else if (context != ItemDisplayContext.GROUND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }

        float scale = switch (kind) {
            case CONSOLE -> context == ItemDisplayContext.GUI ? 0.22F : 0.16F;
            case CRANE_CONSOLE -> context == ItemDisplayContext.GUI ? 0.72F : 0.5F;
            case AUTOLOADER -> context == ItemDisplayContext.GUI ? 0.12F : 0.09F;
            default -> context == ItemDisplayContext.GUI ? 0.78F : 0.52F;
        };
        poseStack.scale(scale, scale, scale);

        switch (kind) {
            case CONSOLE -> poseStack.translate(0.0F, -2.0F, 0.0F);
            case CRANE_CONSOLE -> poseStack.translate(-0.5F, -0.5F, 0.0F);
            case AUTOLOADER -> poseStack.translate(0.0F, -4.5F, 0.0F);
            default -> poseStack.translate(-0.5F, -0.5F, -0.5F);
        }
    }

    private static void renderMiniPanelItem(RbmkComponentBlock.Kind kind, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderModel(MINI_PANEL_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        renderMiniPanelContents(kind, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderMiniPanelContents(RbmkComponentBlock.Kind kind, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        switch (kind) {
            case GAUGE -> {
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
            case LEVER -> {
                for (int slot = 0; slot < 2; slot++) {
                    poseStack.pushPose();
                    poseStack.translate(0.25D, 0.0D, slot * -0.5D + 0.25D);
                    renderModel(LEVER_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
                    renderModel(LEVER_HANDLE, state, poseStack, bufferSource, packedLight, packedOverlay);
                    poseStack.popPose();
                }
            }
            case INDICATOR -> {
                for (int slot = 0; slot < 6; slot++) {
                    poseStack.pushPose();
                    poseStack.translate(0.25D, (slot / 2) * -0.3125D + 0.3125D, (slot % 2) * 0.5D - 0.25D);
                    renderModel(INDICATOR_BASE, state, poseStack, bufferSource, packedLight, packedOverlay);
                    MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(INDICATOR_LIGHT), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, (slot & 1) == 0 ? 0xFFFF0000 : 0xFFFFFF00);
                    poseStack.popPose();
                }
            }
            case KEY_PAD -> {
                for (int slot = 0; slot < 4; slot++) {
                    poseStack.pushPose();
                    poseStack.translate(0.25D, (slot / 2) * -0.5D + 0.25D, (slot % 2) * -0.5D + 0.25D);
                    renderModel(BUTTON_SOCKET, state, poseStack, bufferSource, packedLight, packedOverlay);
                    MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(BUTTON_BUTTON), poseStack, bufferSource, state, packedLight, packedOverlay, 0xFFA60000);
                    poseStack.popPose();
                }
            }
            case GRAPH, NUMITRON -> {
                ModelResourceLocation numitron = MODELS.get(RbmkComponentBlock.Kind.NUMITRON);
                for (int slot = 0; slot < 2; slot++) {
                    poseStack.pushPose();
                    poseStack.translate(0.25D, slot * -0.5D + 0.25D, 0.0D);
                    renderModel(numitron, state, poseStack, bufferSource, packedLight, packedOverlay);
                    poseStack.popPose();
                }
            }
            case TERMINAL -> {
                poseStack.pushPose();
                poseStack.translate(0.25D, 0.0D, 0.0D);
                renderModel(MODELS.get(RbmkComponentBlock.Kind.TERMINAL), state, poseStack, bufferSource, packedLight, packedOverlay);
                poseStack.popPose();
            }
            default -> {
            }
        }
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
