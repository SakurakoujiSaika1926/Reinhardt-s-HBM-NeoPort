package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.ChemicalFactoryBlock;
import com.reinhardt.hbm.blockentity.ChemicalFactoryBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ChemicalFactoryBlockEntityRenderer implements LongRangeBlockEntityRenderer<ChemicalFactoryBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_chemical_factory_base");
    private static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/machine_chemical_factory_frame");
    private static final ModelResourceLocation FAN_1 = MachineModelRenderer.standalone("block/machine_chemical_factory_fan1");
    private static final ModelResourceLocation FAN_2 = MachineModelRenderer.standalone("block/machine_chemical_factory_fan2");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final AABB[] LOCAL_BOUNDS = buildChemicalFactoryBounds();

    public ChemicalFactoryBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(FRAME);
        event.register(FAN_1);
        event.register(FAN_2);
    }

    @Override
    public void render(ChemicalFactoryBlockEntity factory, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        factory.updateClientAnimation();
        BlockState state = factory.getBlockState();
        Direction facing = chemicalFactoryFacing(state);
        float anim = (float) factory.clientAnim(partialTick);

        poseStack.pushPose();
        applyChemicalFactoryTransform(poseStack, facing);

        STATIC_MESHES.render(BASE, poseStack, state, packedLight, packedOverlay);
        if (factory.clientFrame()) {
            STATIC_MESHES.render(FRAME, poseStack, state, packedLight, packedOverlay);
        }

        poseStack.pushPose();
        poseStack.translate(1.0D, 0.0D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-anim * 45.0F % 360.0F));
        poseStack.translate(-1.0D, 0.0D, 0.0D);
        renderPart(FAN_1, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-1.0D, 0.0D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-anim * 45.0F % 360.0F));
        poseStack.translate(1.0D, 0.0D, 0.0D);
        renderPart(FAN_2, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ChemicalFactoryBlockEntity blockEntity) {
        Direction facing = chemicalFactoryFacing(blockEntity.getBlockState());
        /* RenderChemicalFactory draws only Base, optional Frame, Fan1 and Fan2.
         * Their face-referenced OBJ union is exactly
         * [-2.5, 0, -2.5]..[2.5, 3, 2.5]; both complete fan sweeps remain
         * inside Base.  Plane.001 is deliberately absent from the legacy
         * renderer and must not enlarge this envelope. */
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        STATIC_MESHES.render(model, poseStack, state, packedLight, packedOverlay);
    }

    private static AABB[] buildChemicalFactoryBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyChemicalFactoryTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -2.5F, 0.0F, -2.5F, 2.5F, 3.0F, 2.5F);
        }
        return bounds;
    }

    private static void applyChemicalFactoryTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            // RenderChemicalFactory's fixed 90 degrees plus metadata rotation.
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Chemical factory facing must be horizontal: " + facing);
        }
    }

    private static Direction chemicalFactoryFacing(BlockState state) {
        if (!state.hasProperty(ChemicalFactoryBlock.FACING)) {
            throw new IllegalStateException("Chemical factory state is missing its owned facing property: " + state);
        }
        return state.getValue(ChemicalFactoryBlock.FACING);
    }
}
