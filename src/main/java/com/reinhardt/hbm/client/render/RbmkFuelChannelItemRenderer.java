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
    private static final float MODEL_HEIGHT = 4.25F;
    private static final float GUI_MODEL_SCALE = 0.235F;
    private static final float NON_GUI_TARGET_SIZE = 0.95F;

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
        BlockState state = HbmBlocks.RBMK_ROD.get().defaultBlockState();

        poseStack.pushPose();
        applyLegacyInventoryTransform(context, poseStack);

        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, i, 0.0F);
            renderOneSegment(kind, i == 3, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
        if (kind.isControl()) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 3.0F, 0.5F);
            MachineModelRenderer.renderUnculled(
                    MachineModelRenderer.model(kind.isAutomaticControl() ? CONTROL_AUTO_LID : CONTROL_LID),
                    poseStack,
                    bufferSource,
                    state,
                    packedLight,
                    packedOverlay
            );
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void applyLegacyInventoryTransform(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
            poseStack.scale(GUI_MODEL_SCALE, GUI_MODEL_SCALE, GUI_MODEL_SCALE);
        } else {
            float scale = NON_GUI_TARGET_SIZE / MODEL_HEIGHT;
            poseStack.scale(scale, scale, scale);
        }
        poseStack.translate(-0.5F, -MODEL_HEIGHT * 0.5F, -0.5F);
    }

    private static void renderOneSegment(RbmkComponentBlock.Kind kind, boolean top, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ModelResourceLocation segment = SEGMENTS.get(kind);
        if (segment != null) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(segment), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        if (kind.acceptsFuel()) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            ModelResourceLocation inner = INNERS.get(kind);
            ModelResourceLocation cap = CAPS.get(kind);
            if (inner != null) {
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(inner), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            if (cap != null) {
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(cap), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            if (kind.acceptsFuel()) {
                MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(RODS), poseStack, bufferSource, state, packedLight, packedOverlay, 0xFF304825);
            }
            poseStack.popPose();
        }
        if (top) {
            ModelResourceLocation topModel = TOPS.get(kind);
            if (topModel != null) {
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(topModel), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
        }
    }
}
