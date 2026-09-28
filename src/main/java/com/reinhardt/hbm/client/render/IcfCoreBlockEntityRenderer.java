package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.IcfCoreBlock;
import com.reinhardt.hbm.blockentity.IcfCoreBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Direct port of RenderICF: the authored OBJ uses the 1.7.10 metadata orientation. */
public final class IcfCoreBlockEntityRenderer implements LongRangeBlockEntityRenderer<IcfCoreBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/icf");
    private static final AABB[] LOCAL_BOUNDS = buildIcfCoreBounds();

    public IcfCoreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(IcfCoreBlockEntity core, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = core.getBlockState();
        Direction facing = icfCoreFacing(state);
        poseStack.pushPose();
        applyIcfCoreTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(IcfCoreBlockEntity core) {
        BlockState state = core.getBlockState();
        Direction facing = icfCoreFacing(state);
        return LOCAL_BOUNDS[facing.ordinal()].move(core.getBlockPos());
    }

    private static AABB[] buildIcfCoreBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyIcfCoreTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -5.875F, 0.0F, -8.5F, 5.875F, 6.372075F, 8.5F);
        }
        return bounds;
    }

    private static void applyIcfCoreTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("ICF core facing must be horizontal: " + facing);
        }
    }

    private static Direction icfCoreFacing(BlockState state) {
        if (!state.hasProperty(IcfCoreBlock.FACING)) {
            throw new IllegalStateException("ICF core state is missing its owned facing property: " + state);
        }
        return state.getValue(IcfCoreBlock.FACING);
    }
}
