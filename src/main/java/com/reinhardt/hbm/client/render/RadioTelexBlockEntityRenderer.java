package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.RadioTelexBlock;
import com.reinhardt.hbm.blockentity.RadioTelexBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Direct port of the 1.7.10 RenderTelex transform and texture binding. */
public final class RadioTelexBlockEntityRenderer implements LongRangeBlockEntityRenderer<RadioTelexBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/radio_telex");
    private static final ResourceLocation TEXTURE = com.reinhardt.hbm.ReinhardtsHBM.id(
            "textures/models/machines/telex.png"
    );
    private static final AABB NORTH_BOUNDS = createTelexLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_BOUNDS = createTelexLocalBounds(Direction.SOUTH);
    private static final AABB WEST_BOUNDS = createTelexLocalBounds(Direction.WEST);
    private static final AABB EAST_BOUNDS = createTelexLocalBounds(Direction.EAST);

    public RadioTelexBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(RadioTelexBlockEntity telex, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = telex.getBlockState();
        poseStack.pushPose();
        applyTelexPose(poseStack, radioTelexFacing(state));
        renderModel(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    static void renderModel(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay, TEXTURE, 0.0F, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(RadioTelexBlockEntity telex) {
        Direction facing = radioTelexFacing(telex.getBlockState());
        AABB local = switch (facing) {
            case NORTH -> NORTH_BOUNDS;
            case SOUTH -> SOUTH_BOUNDS;
            case WEST -> WEST_BOUNDS;
            case EAST -> EAST_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Radio telex facing must be horizontal: " + facing);
        };
        return local.move(telex.getBlockPos());
    }

    private static void applyTelexPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Radio telex facing must be horizontal: " + facing);
        }
    }

    private static AABB createTelexLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyTelexPose(poseStack, facing);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -0.5F, 0.0F, -0.5F, 0.625F, 1.8125F, 1.5F);
    }

    private static Direction radioTelexFacing(BlockState state) {
        if (!state.hasProperty(RadioTelexBlock.FACING)) {
            throw new IllegalStateException("Radio telex block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(RadioTelexBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Radio telex block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
