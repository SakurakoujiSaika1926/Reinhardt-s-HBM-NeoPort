package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.NukeBoyBlock;
import com.reinhardt.hbm.blockentity.NukeBoyBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class NukeBoyBlockEntityRenderer implements BlockEntityRenderer<NukeBoyBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/nuke_boy_world");

    public NukeBoyBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(NukeBoyBlockEntity nukeBoy, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = nukeBoy.getBlockState();
        Direction facing = state.hasProperty(NukeBoyBlock.FACING) ? state.getValue(NukeBoyBlock.FACING) : Direction.SOUTH;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(legacyYaw(facing)));
        poseStack.translate(-2.0D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(NukeBoyBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 2.0D, 3.0D);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case WEST -> 90.0F;
            case SOUTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
    }
}
