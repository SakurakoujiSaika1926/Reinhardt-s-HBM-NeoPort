package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.AutocalBlock;
import com.reinhardt.hbm.blockentity.AutocalBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Direct RenderAUTOCAL orientation table, deliberately independent of generic rotation helpers. */
public final class AutocalBlockEntityRenderer implements LongRangeBlockEntityRenderer<AutocalBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/radio_autocal");
    private static final ResourceLocation TEXTURE = com.reinhardt.hbm.ReinhardtsHBM.id(
            "textures/models/machines/autocal.png"
    );
    private static final AABB[] LOCAL_BOUNDS = buildAutocalBounds();

    public AutocalBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(AutocalBlockEntity autocal, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = autocal.getBlockState();
        poseStack.pushPose();
        applyAutocalTransform(poseStack, autocalFacing(state));
        renderModel(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    static void renderModel(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay) {
        // RenderAUTOCAL binds autocal_tex directly before drawing this OBJ.
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay, TEXTURE, 0.0F, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(AutocalBlockEntity autocal) {
        Direction facing = autocalFacing(autocal.getBlockState());
        return LOCAL_BOUNDS[facing.ordinal()].move(autocal.getBlockPos());
    }

    private static AABB[] buildAutocalBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyAutocalTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -0.5F, 0.0F, -0.5F, 0.501F, 2.0F, 0.5F);
        }
        return bounds;
    }

    private static void applyAutocalTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Autocal facing must be horizontal: " + facing);
        }
    }

    private static Direction autocalFacing(BlockState state) {
        if (!state.hasProperty(AutocalBlock.FACING)) {
            throw new IllegalStateException("Autocal block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(AutocalBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Autocal block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
