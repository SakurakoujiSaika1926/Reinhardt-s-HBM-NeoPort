package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.ChemicalFactoryBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class ChemicalFactoryBlockEntityRenderer implements BlockEntityRenderer<ChemicalFactoryBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_chemical_factory_base");
    private static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/machine_chemical_factory_frame");
    private static final ModelResourceLocation FAN_1 = MachineModelRenderer.standalone("block/machine_chemical_factory_fan1");
    private static final ModelResourceLocation FAN_2 = MachineModelRenderer.standalone("block/machine_chemical_factory_fan2");
    private static final ModelResourceLocation PLANE = MachineModelRenderer.standalone("block/machine_chemical_factory_plane_001");

    public ChemicalFactoryBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(FRAME);
        event.register(FAN_1);
        event.register(FAN_2);
        event.register(PLANE);
    }

    @Override
    public void render(ChemicalFactoryBlockEntity factory, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        factory.updateClientAnimation();
        BlockState state = factory.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        float anim = (float) factory.clientAnim(partialTick);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        rotateY(poseStack, legacyChemicalYaw(facing));

        renderPart(BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(PLANE, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (factory.clientFrame()) {
            renderPart(FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);
        }

        poseStack.pushPose();
        poseStack.translate(1.0D, 0.0D, 0.0D);
        rotateY(poseStack, -anim * 45.0F % 360.0F);
        poseStack.translate(-1.0D, 0.0D, 0.0D);
        renderPart(FAN_1, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-1.0D, 0.0D, 0.0D);
        rotateY(poseStack, -anim * 45.0F % 360.0F);
        poseStack.translate(1.0D, 0.0D, 0.0D);
        renderPart(FAN_2, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ChemicalFactoryBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(5.0D, 5.0D, 5.0D);
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static float legacyChemicalYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }

    private static void rotateY(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F)));
    }
}
