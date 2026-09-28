package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.item.RbmkFuelChannelBlockItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.EnumMap;
import java.util.Map;

public final class RbmkFuelChannelItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> SEGMENTS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> TOPS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> CAPS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final Map<RbmkComponentBlock.Kind, ModelResourceLocation> INNERS = new EnumMap<>(RbmkComponentBlock.Kind.class);
    private static final ModelResourceLocation RODS = MachineModelRenderer.standalone("block/rbmk_rod_rods");
    private static final ModelResourceLocation CONTROL_LID = MachineModelRenderer.standalone("block/rbmk_rods_lid");
    private static final ModelResourceLocation CONTROL_AUTO_LID = MachineModelRenderer.standalone("block/rbmk_rods_lid_auto");

    static {
        for (RbmkComponentBlock.Kind kind : RbmkComponentBlock.Kind.values()) {
            if (!kind.isColumn()) {
                continue;
            }
            String base = "block/rbmk_" + kind.getSerializedName();
            SEGMENTS.put(kind, MachineModelRenderer.standalone(base + "_segment"));
            TOPS.put(kind, MachineModelRenderer.standalone(base + "_top"));
            if (kind.acceptsFuel()) {
                CAPS.put(kind, MachineModelRenderer.standalone(base + "_cap"));
                INNERS.put(kind, MachineModelRenderer.standalone(base + "_inner"));
            }
        }
    }

    public RbmkFuelChannelItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        SEGMENTS.values().forEach(event::register);
        TOPS.values().forEach(event::register);
        CAPS.values().forEach(event::register);
        INNERS.values().forEach(event::register);
        event.register(RODS);
        event.register(CONTROL_LID);
        event.register(CONTROL_AUTO_LID);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        RbmkComponentBlock.Kind kind = stack.getItem() instanceof RbmkFuelChannelBlockItem item ? item.kind() : RbmkComponentBlock.Kind.BLANK;

        poseStack.pushPose();
        switch (kind) {
            case BLANK -> renderBlankItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case FUEL_ROD -> renderFuelRodItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case FUEL_ROD_MOD -> renderModeratedFuelRodItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case FUEL_ROD_REASIM -> renderReasimFuelRodItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case FUEL_ROD_REASIM_MOD -> renderModeratedReasimFuelRodItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case CONTROL -> renderControlItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case CONTROL_AUTO -> renderAutomaticControlItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case CONTROL_MOD -> renderModeratedControlItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case CONTROL_REASIM -> renderReasimControlItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case CONTROL_REASIM_AUTO -> renderAutomaticReasimControlItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case BOILER -> renderBoilerItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case HEATER -> renderHeaterItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case COOLER -> renderCoolerItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case OUTGASSER -> renderOutgasserItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case STORAGE -> renderStorageItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case MODERATOR -> renderModeratorItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case REFLECTOR -> renderReflectorItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case ABSORBER -> renderAbsorberItem(context, poseStack, bufferSource, packedLight, packedOverlay);
            case LOADER -> { }
            case STEAM_INLET -> { }
            case STEAM_OUTLET -> { }
            case AUTOLOADER -> { }
            case CONSOLE -> { }
            case CRANE_CONSOLE -> { }
            case DISPLAY -> { }
            case DISPLAY_BLANK -> { }
            case GAUGE -> { }
            case GRAPH -> { }
            case INDICATOR -> { }
            case KEY_PAD -> { }
            case LEVER -> { }
            case NUMITRON -> { }
            case TERMINAL -> { }
        }
        poseStack.popPose();
    }

    // RenderRBMKRod, RenderRBMKControl and the passive-column renderers were
    // separate 1.7.10 inventory paths. Keep both the camera pose and the
    // column assembly explicit per Kind even while their current values match.
    private static void renderBlankItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyBlankItemPose(context, poseStack);
        renderBlankColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyBlankItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderFuelRodItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyFuelRodItemPose(context, poseStack);
        renderFuelRodColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyFuelRodItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderModeratedFuelRodItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyModeratedFuelRodItemPose(context, poseStack);
        renderModeratedFuelRodColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyModeratedFuelRodItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderReasimFuelRodItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyReasimFuelRodItemPose(context, poseStack);
        renderReasimFuelRodColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyReasimFuelRodItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderModeratedReasimFuelRodItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyModeratedReasimFuelRodItemPose(context, poseStack);
        renderModeratedReasimFuelRodColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyModeratedReasimFuelRodItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderControlItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyControlItemPose(context, poseStack);
        renderControlColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyControlItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderAutomaticControlItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyAutomaticControlItemPose(context, poseStack);
        renderAutomaticControlColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyAutomaticControlItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderModeratedControlItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyModeratedControlItemPose(context, poseStack);
        renderModeratedControlColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyModeratedControlItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderReasimControlItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyReasimControlItemPose(context, poseStack);
        renderReasimControlColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyReasimControlItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderAutomaticReasimControlItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyAutomaticReasimControlItemPose(context, poseStack);
        renderAutomaticReasimControlColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyAutomaticReasimControlItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderBoilerItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyBoilerItemPose(context, poseStack);
        renderBoilerColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyBoilerItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderHeaterItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyHeaterItemPose(context, poseStack);
        renderHeaterColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyHeaterItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderCoolerItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyCoolerItemPose(context, poseStack);
        renderCoolerColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyCoolerItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderOutgasserItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyOutgasserItemPose(context, poseStack);
        renderOutgasserColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyOutgasserItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderStorageItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyStorageItemPose(context, poseStack);
        renderStorageColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyStorageItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderModeratorItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyModeratorItemPose(context, poseStack);
        renderModeratorColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyModeratorItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderReflectorItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyReflectorItemPose(context, poseStack);
        renderReflectorColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyReflectorItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderAbsorberItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        applyAbsorberItemPose(context, poseStack);
        renderAbsorberColumnGeometry(poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void applyAbsorberItemPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(0.235F, 0.235F, 0.235F);
        } else {
            float scale = 0.95F / 4.25F;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -4.25F * 0.5F, -0.5F);
    }

    private static void renderBlankColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.BLANK);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.BLANK);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderFuelRodColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.FUEL_ROD);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.FUEL_ROD);
        ModelResourceLocation inner = INNERS.get(RbmkComponentBlock.Kind.FUEL_ROD);
        ModelResourceLocation cap = CAPS.get(RbmkComponentBlock.Kind.FUEL_ROD);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            renderResolvedModel(inner, state, poseStack, bufferSource, packedLight, packedOverlay);
            renderResolvedModel(cap, state, poseStack, bufferSource, packedLight, packedOverlay);
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(RODS), poseStack, bufferSource, state, packedLight, packedOverlay, 0xFF304825);
            poseStack.popPose();
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderModeratedFuelRodColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.FUEL_ROD_MOD);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.FUEL_ROD_MOD);
        ModelResourceLocation inner = INNERS.get(RbmkComponentBlock.Kind.FUEL_ROD_MOD);
        ModelResourceLocation cap = CAPS.get(RbmkComponentBlock.Kind.FUEL_ROD_MOD);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            renderResolvedModel(inner, state, poseStack, bufferSource, packedLight, packedOverlay);
            renderResolvedModel(cap, state, poseStack, bufferSource, packedLight, packedOverlay);
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(RODS), poseStack, bufferSource, state, packedLight, packedOverlay, 0xFF304825);
            poseStack.popPose();
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderReasimFuelRodColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.FUEL_ROD_REASIM);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.FUEL_ROD_REASIM);
        ModelResourceLocation inner = INNERS.get(RbmkComponentBlock.Kind.FUEL_ROD_REASIM);
        ModelResourceLocation cap = CAPS.get(RbmkComponentBlock.Kind.FUEL_ROD_REASIM);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            renderResolvedModel(inner, state, poseStack, bufferSource, packedLight, packedOverlay);
            renderResolvedModel(cap, state, poseStack, bufferSource, packedLight, packedOverlay);
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(RODS), poseStack, bufferSource, state, packedLight, packedOverlay, 0xFF304825);
            poseStack.popPose();
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderModeratedReasimFuelRodColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD);
        ModelResourceLocation inner = INNERS.get(RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD);
        ModelResourceLocation cap = CAPS.get(RbmkComponentBlock.Kind.FUEL_ROD_REASIM_MOD);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            renderResolvedModel(inner, state, poseStack, bufferSource, packedLight, packedOverlay);
            renderResolvedModel(cap, state, poseStack, bufferSource, packedLight, packedOverlay);
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(RODS), poseStack, bufferSource, state, packedLight, packedOverlay, 0xFF304825);
            poseStack.popPose();
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderControlColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.CONTROL);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.CONTROL);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 3.0F, 0.5F);
        renderResolvedModel(CONTROL_LID, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderAutomaticControlColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.CONTROL_AUTO);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.CONTROL_AUTO);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 3.0F, 0.5F);
        renderResolvedModel(CONTROL_AUTO_LID, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderModeratedControlColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.CONTROL_MOD);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.CONTROL_MOD);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 3.0F, 0.5F);
        renderResolvedModel(CONTROL_LID, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderReasimControlColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.CONTROL_REASIM);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.CONTROL_REASIM);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 3.0F, 0.5F);
        renderResolvedModel(CONTROL_LID, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderAutomaticReasimControlColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.CONTROL_REASIM_AUTO);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.CONTROL_REASIM_AUTO);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 3.0F, 0.5F);
        renderResolvedModel(CONTROL_AUTO_LID, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderBoilerColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.BOILER);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.BOILER);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderHeaterColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.HEATER);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.HEATER);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderCoolerColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.COOLER);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.COOLER);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderOutgasserColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.OUTGASSER);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.OUTGASSER);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderStorageColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.STORAGE);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.STORAGE);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderModeratorColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.MODERATOR);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.MODERATOR);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderReflectorColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.REFLECTOR);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.REFLECTOR);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderAbsorberColumnGeometry(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();
        ModelResourceLocation segment = SEGMENTS.get(RbmkComponentBlock.Kind.ABSORBER);
        ModelResourceLocation top = TOPS.get(RbmkComponentBlock.Kind.ABSORBER);
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderResolvedModel(segment, state, poseStack, bufferSource, packedLight, packedOverlay);
            if (i == 3) renderResolvedModel(top, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderResolvedModel(ModelResourceLocation location, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (location != null) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(location), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
    }
}
