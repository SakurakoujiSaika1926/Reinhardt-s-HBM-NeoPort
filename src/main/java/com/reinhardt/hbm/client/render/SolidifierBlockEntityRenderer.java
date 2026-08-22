package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.SolidifierBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class SolidifierBlockEntityRenderer implements BlockEntityRenderer<SolidifierBlockEntity> {
    private static final ModelResourceLocation MAIN = MachineModelRenderer.standalone("block/machine_solidifier");
    private static final ModelResourceLocation FLUID = MachineModelRenderer.standalone("block/machine_solidifier_fluid");
    private static final ModelResourceLocation GLASS = MachineModelRenderer.standalone("block/machine_solidifier_glass");
    private static final int GLASS_COLOR = 0x26BFFFFF;

    public SolidifierBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MAIN);
        event.register(FLUID);
        event.register(GLASS);
    }

    @Override
    public void render(SolidifierBlockEntity solidifier, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = solidifier.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MAIN), poseStack, bufferSource, state, packedLight, packedOverlay);

        if (solidifier.tank().amount() > 0 && !solidifier.tank().type().isNone()) {
            double height = (double) solidifier.tank().amount() / (double) solidifier.tank().capacity();
            poseStack.pushPose();
            poseStack.translate(0.0D, 1.25D, 0.0D);
            poseStack.scale(1.0F, (float) height, 1.0F);
            poseStack.translate(0.0D, -1.25D, 0.0D);
            MachineModelRenderer.renderUnculledTinted(
                    MachineModelRenderer.model(FLUID),
                    poseStack,
                    bufferSource,
                    state,
                    packedLight,
                    packedOverlay,
                    0xFF000000 | solidifier.tank().type().color()
            );
            poseStack.popPose();
        }

        MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(GLASS), poseStack, bufferSource, state, packedLight, packedOverlay, GLASS_COLOR);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SolidifierBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 5.0D, 3.0D);
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
