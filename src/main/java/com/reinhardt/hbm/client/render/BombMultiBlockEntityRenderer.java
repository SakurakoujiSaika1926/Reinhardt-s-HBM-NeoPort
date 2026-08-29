package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.BombMultiBlock;
import com.reinhardt.hbm.blockentity.BombMultiBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact RenderBombMulti transform, kept separate from the shared machine-facing mapping. */
public final class BombMultiBlockEntityRenderer implements BlockEntityRenderer<BombMultiBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/bomb_multi_world");

    public BombMultiBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(BombMultiBlockEntity bomb, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = bomb.getBlockState();
        Direction facing = state.hasProperty(BombMultiBlock.FACING)
                ? state.getValue(BombMultiBlock.FACING) : Direction.EAST;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(legacyYaw(facing)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state,
                packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BombMultiBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0D);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 90.0F;
            case WEST -> 180.0F;
            case NORTH -> 270.0F;
            default -> 0.0F;
        };
    }
}
