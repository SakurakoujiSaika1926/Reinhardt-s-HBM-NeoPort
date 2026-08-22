package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.RotaryFurnaceBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class RotaryFurnaceBlockEntityRenderer implements BlockEntityRenderer<RotaryFurnaceBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_rotary_furnace");

    public RotaryFurnaceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(RotaryFurnaceBlockEntity furnace, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = furnace.getBlockState();
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(state.getValue(LargeMachineBlock.FACING)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RotaryFurnaceBlockEntity blockEntity) {
        return new AABB(
                blockEntity.getBlockPos().getX() - 3.0D,
                blockEntity.getBlockPos().getY(),
                blockEntity.getBlockPos().getZ() - 3.0D,
                blockEntity.getBlockPos().getX() + 4.0D,
                blockEntity.getBlockPos().getY() + 5.0D,
                blockEntity.getBlockPos().getZ() + 4.0D
        );
    }

    private static float legacyYaw(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }
}
