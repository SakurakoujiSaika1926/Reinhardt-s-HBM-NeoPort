package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.DieselGeneratorBlock;
import com.reinhardt.hbm.blockentity.DieselGeneratorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class DieselGeneratorBlockEntityRenderer implements BlockEntityRenderer<DieselGeneratorBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_diesel_world");

    public DieselGeneratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(DieselGeneratorBlockEntity diesel, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = diesel.getBlockState();
        Direction facing = state.hasProperty(DieselGeneratorBlock.FACING) ? state.getValue(DieselGeneratorBlock.FACING) : Direction.NORTH;
        poseStack.pushPose();
        // The diesel generator OBJ is authored around a centered local origin (-0.5..0.5),
        // so rotating it like a normal 0..1 block model introduces a half-block offset.
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyDieselYaw(facing));
        if (diesel.running()) {
            poseStack.translate(
                    Math.sin((diesel.getLevel() == null ? 0L : diesel.getLevel().getGameTime()) / 2.5D) * 0.005D,
                    0.0D,
                    Math.sin((diesel.getLevel() == null ? 0L : diesel.getLevel().getGameTime()) / 5.0D) * 0.005D
            );
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(WORLD), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(DieselGeneratorBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0D);
    }

    private static float legacyDieselYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }
}
