package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.CatalyticReformerBlock;
import com.reinhardt.hbm.blockentity.CatalyticReformerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class CatalyticReformerBlockEntityRenderer implements LongRangeBlockEntityRenderer<CatalyticReformerBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_catalytic_reformer");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final AABB[] LOCAL_BOUNDS = buildCatalyticReformerBounds();

    public CatalyticReformerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(CatalyticReformerBlockEntity reformer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = reformer.getBlockState();
        Direction facing = catalyticReformerFacing(state);

        poseStack.pushPose();
        applyCatalyticReformerTransform(poseStack, facing);
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CatalyticReformerBlockEntity blockEntity) {
        Direction facing = catalyticReformerFacing(blockEntity.getBlockState());
        // Face-used catalytic_reformer.obj vertices under the render pose.
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildCatalyticReformerBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyCatalyticReformerTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.5F, 0.0F, -2.5F, 1.5F, 7.0F, 2.5F);
        }
        return bounds;
    }

    private static void applyCatalyticReformerTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Catalytic reformer facing must be horizontal: " + facing);
        }
    }

    private static Direction catalyticReformerFacing(BlockState state) {
        if (!state.hasProperty(CatalyticReformerBlock.FACING)) {
            throw new IllegalStateException("Catalytic reformer state is missing its owned facing property: " + state);
        }
        return state.getValue(CatalyticReformerBlock.FACING);
    }
}
