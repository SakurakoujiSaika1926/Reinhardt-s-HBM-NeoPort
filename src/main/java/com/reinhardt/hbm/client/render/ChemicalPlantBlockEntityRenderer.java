package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.ChemicalPlantBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.ChemicalPlantBlockEntity;
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

public class ChemicalPlantBlockEntityRenderer implements BlockEntityRenderer<ChemicalPlantBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_chemical_plant_base");
    private static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/machine_chemical_plant_frame");
    private static final ModelResourceLocation SLIDER = MachineModelRenderer.standalone("block/machine_chemical_plant_slider");
    private static final ModelResourceLocation SPINNER = MachineModelRenderer.standalone("block/machine_chemical_plant_spinner");
    private static final ModelResourceLocation FLUID = MachineModelRenderer.standalone("block/machine_chemical_plant_fluid");

    public ChemicalPlantBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(FRAME);
        event.register(SLIDER);
        event.register(SPINNER);
        event.register(FLUID);
    }

    @Override
    public void render(ChemicalPlantBlockEntity chemicalPlant, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        chemicalPlant.updateClientAnimation();
        BlockState state = chemicalPlant.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        double anim = chemicalPlant.clientAnim(partialTick);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        rotateY(poseStack, legacyChemicalYaw(facing));

        renderPart(BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (chemicalPlant.clientFrame()) {
            renderPart(FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);
        }

        poseStack.pushPose();
        poseStack.translate(sps(anim * 0.125D) * 0.375D, 0.0D, 0.0D);
        renderPart(SLIDER, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        rotateY(poseStack, (float) ((anim * 15.0D) % 360.0D));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        renderPart(SPINNER, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        if (state.hasProperty(ChemicalPlantBlock.LIT) && state.getValue(ChemicalPlantBlock.LIT)) {
            MachineModelRenderer.renderUnculledTinted(
                    MachineModelRenderer.model(FLUID),
                    poseStack,
                    bufferSource,
                    state,
                    packedLight,
                    packedOverlay,
                    chemicalPlant.clientFluidColor()
            );
        }

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ChemicalPlantBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 4.0D, 3.0D);
    }

    private static void renderPart(
            ModelResourceLocation model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static double sps(double value) {
        return Math.sin(Math.PI * 0.5D * Math.cos(value));
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
