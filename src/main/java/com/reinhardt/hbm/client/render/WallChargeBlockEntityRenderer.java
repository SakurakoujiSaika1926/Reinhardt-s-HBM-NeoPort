package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.WallChargeBlock;
import com.reinhardt.hbm.blockentity.WallChargeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Renders charge geometry and the old green MM:SS display without a second block model. */
public final class WallChargeBlockEntityRenderer implements BlockEntityRenderer<WallChargeBlockEntity> {
    private static final ModelResourceLocation DYNAMITE = model("block/charge_dynamite_world");
    private static final ModelResourceLocation C4 = model("block/charge_c4_world");

    public WallChargeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(DYNAMITE);
        event.register(C4);
    }

    @Override
    public void render(WallChargeBlockEntity charge, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = charge.getBlockState();
        Direction facing = state.getValue(WallChargeBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        applyLegacyFacing(poseStack, facing);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        ModelResourceLocation model = charge.kind() == WallChargeBlock.Kind.C4
                || charge.kind() == WallChargeBlock.Kind.SEMTEX ? C4 : DYNAMITE;
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        renderTimer(charge, facing, poseStack, bufferSource);
    }

    private static void renderTimer(WallChargeBlockEntity charge, Direction facing, PoseStack poseStack,
                                    MultiBufferSource bufferSource) {
        String text = charge.minutes() + ":" + charge.seconds();
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        applyLegacyFacing(poseStack, facing);
        poseStack.translate(-0.05F, -0.185F, 0.15F);
        poseStack.scale(0.0125F, -0.0125F, 0.0125F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
        font.drawInBatch(text, 0.0F, 0.0F, 0x00FF00, false, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static void applyLegacyFacing(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
            case NORTH -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
            }
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
            case UP -> {
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(WallChargeBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos());
    }

    private static ModelResourceLocation model(String path) {
        return MachineModelRenderer.standalone(path);
    }
}
