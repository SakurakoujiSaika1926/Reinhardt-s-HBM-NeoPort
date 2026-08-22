package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.BlastFurnaceBlock;
import com.reinhardt.hbm.blockentity.BlastFurnaceBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class BlastFurnaceBlockEntityRenderer implements BlockEntityRenderer<BlastFurnaceBlockEntity> {
    private static final ModelResourceLocation OFF = MachineModelRenderer.standalone("block/machine_difurnace_off");
    private static final ModelResourceLocation ON = MachineModelRenderer.standalone("block/machine_difurnace_on");

    public BlastFurnaceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(OFF);
        event.register(ON);
    }

    @Override
    public void render(BlastFurnaceBlockEntity furnace, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = furnace.getBlockState();
        Direction facing = state.hasProperty(BlastFurnaceBlock.FACING) ? state.getValue(BlastFurnaceBlock.FACING) : Direction.NORTH;
        boolean lit = state.hasProperty(BlastFurnaceBlock.LIT) && state.getValue(BlastFurnaceBlock.LIT);

        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyBlastFurnaceYaw(facing));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(lit ? ON : OFF), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BlastFurnaceBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 7.0D, 3.0D);
    }

    private static float legacyBlastFurnaceYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 0.0F;
        };
    }
}
