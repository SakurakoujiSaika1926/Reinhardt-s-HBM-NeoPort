package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.PowerMachineBlock;
import com.reinhardt.hbm.blockentity.WoodBurnerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class WoodBurnerBlockEntityRenderer implements BlockEntityRenderer<WoodBurnerBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_wood_burner_world");

    public WoodBurnerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(WoodBurnerBlockEntity burner, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = burner.getBlockState();
        Direction facing = state.hasProperty(PowerMachineBlock.FACING) ? state.getValue(PowerMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(legacyYaw(facing)), 0.0F, 1.0F, 0.0F)));
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(WORLD), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(WoodBurnerBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 1.0D, pos.getY(), pos.getZ() - 1.0D,
                pos.getX() + 2.0D, pos.getY() + 6.0D, pos.getZ() + 2.0D
        );
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            default -> 180.0F;
        };
    }
}
