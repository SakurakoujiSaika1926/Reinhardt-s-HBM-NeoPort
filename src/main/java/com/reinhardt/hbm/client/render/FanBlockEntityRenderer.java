package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.FanBlock;
import com.reinhardt.hbm.blockentity.FanBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class FanBlockEntityRenderer implements BlockEntityRenderer<FanBlockEntity> {
    private static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/fan_frame");
    private static final ModelResourceLocation BLADES = MachineModelRenderer.standalone("block/fan_blades");

    public FanBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(FRAME);
        event.register(BLADES);
    }

    @Override
    public void render(FanBlockEntity fan, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        BlockState state = fan.getBlockState();
        poseStack.pushPose();
        // The legacy renderer is invoked with (x + 0.5, y, z + 0.5).  OBJ
        // vertices are centered around the block origin, so retain that
        // center translation before applying the facing transform.
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.translate(0.0F, 0.5F, 0.0F);
        switch (state.getValue(FanBlock.FACING)) {
            case DOWN -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
            // 1.7.10 metadata 4 = WEST (+90 around Z), 5 = EAST (-90).
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
            case UP -> { }
        }
        poseStack.translate(0.0F, -0.5F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FRAME), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-fan.spin(partialTick)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BLADES), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FanBlockEntity fan) {
        return new AABB(fan.getBlockPos()).inflate(1.0D);
    }
}
