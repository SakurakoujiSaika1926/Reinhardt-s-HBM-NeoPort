package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.ChemicalPlantBlock;
import com.reinhardt.hbm.blockentity.ChemicalPlantBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ChemicalPlantBlockEntityRenderer implements LongRangeBlockEntityRenderer<ChemicalPlantBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_chemical_plant_base");
    private static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/machine_chemical_plant_frame");
    private static final ModelResourceLocation SLIDER = MachineModelRenderer.standalone("block/machine_chemical_plant_slider");
    private static final ModelResourceLocation SPINNER = MachineModelRenderer.standalone("block/machine_chemical_plant_spinner");
    private static final ModelResourceLocation FLUID = MachineModelRenderer.standalone("block/machine_chemical_plant_fluid");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();

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
        double anim = chemicalPlant.clientAnim(partialTick);

        poseStack.pushPose();
        applyChemicalPlantPose(poseStack, chemicalPlantFacing(state));

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
        poseStack.mulPose(Axis.YP.rotationDegrees((float) ((anim * 15.0D) % 360.0D)));
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
        // Base/frame face bounds contain the +/-0.375 slider and full spinner rotation.
        return RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                -1.0D, 0.0D, -1.0D, 2.0D, 3.0D, 2.0D);
    }

    private static void renderPart(
            ModelResourceLocation model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        STATIC_MESHES.render(model, poseStack, state, packedLight, packedOverlay);
    }

    private static double sps(double value) {
        return Math.sin(Math.PI * 0.5D * Math.cos(value));
    }

    private static Direction chemicalPlantFacing(BlockState state) {
        if (!state.hasProperty(ChemicalPlantBlock.FACING)) {
            throw new IllegalStateException("Chemical plant state is missing its dedicated facing property");
        }
        return state.getValue(ChemicalPlantBlock.FACING);
    }

    private static void applyChemicalPlantPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Chemical plant facing must be horizontal: " + facing);
        }
    }
}
