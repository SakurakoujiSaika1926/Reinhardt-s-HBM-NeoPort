package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.GasCentrifugeBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class GasCentrifugeBlockEntityRenderer implements BlockEntityRenderer<GasCentrifugeBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_gascent");

    public GasCentrifugeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(GasCentrifugeBlockEntity centrifuge, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = centrifuge.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(GasCentrifugeBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0D, 4.0D, 2.0D);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 180.0F;
            case SOUTH -> 90.0F;
            case WEST -> 0.0F;
            default -> 270.0F;
        };
    }
}
