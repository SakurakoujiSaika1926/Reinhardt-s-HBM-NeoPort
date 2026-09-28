package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.HexafluorideTankBlock;
import com.reinhardt.hbm.blockentity.HexafluorideTankBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact RenderUF6Tank / RenderPuF6Tank world transforms using the original tank.obj mesh. */
public final class HexafluorideTankBlockEntityRenderer implements LongRangeBlockEntityRenderer<HexafluorideTankBlockEntity> {
    private static final ModelResourceLocation UF6_MODEL = MachineModelRenderer.standalone("block/machine_uf6_tank_world");
    private static final ModelResourceLocation PUF6_MODEL = MachineModelRenderer.standalone("block/machine_puf6_tank_world");
    private static final AABB[] UF6_LOCAL_BOUNDS = buildUf6TankBounds();
    private static final AABB[] PUF6_LOCAL_BOUNDS = buildPuf6TankBounds();

    public HexafluorideTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(UF6_MODEL);
        event.register(PUF6_MODEL);
    }

    @Override
    public void render(HexafluorideTankBlockEntity tank, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = tank.getBlockState();
        if (state.is(HbmBlocks.MACHINE_PUF6_TANK.get())) {
            renderPuf6Model(state, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            renderUf6Model(state, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    static void renderUf6Model(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                               int packedLight, int packedOverlay) {
        Direction facing = uf6TankFacing(state);
        poseStack.pushPose();
        applyUf6TankTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(UF6_MODEL),
                poseStack, bufferSource, state, packedLight, packedOverlay
        );
        poseStack.popPose();
    }

    static void renderPuf6Model(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {
        Direction facing = puf6TankFacing(state);
        poseStack.pushPose();
        applyPuf6TankTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(PUF6_MODEL),
                poseStack, bufferSource, state, packedLight, packedOverlay
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HexafluorideTankBlockEntity tank) {
        BlockState state = tank.getBlockState();
        if (state.is(HbmBlocks.MACHINE_PUF6_TANK.get())) {
            Direction facing = puf6TankFacing(state);
            return PUF6_LOCAL_BOUNDS[facing.ordinal()].move(tank.getBlockPos());
        }
        Direction facing = uf6TankFacing(state);
        return UF6_LOCAL_BOUNDS[facing.ordinal()].move(tank.getBlockPos());
    }

    private static AABB[] buildUf6TankBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyUf6TankTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -0.5F, 0.0F, -0.5125F, 0.5F, 2.0F, 0.5F);
        }
        return bounds;
    }

    private static AABB[] buildPuf6TankBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyPuf6TankTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -0.5F, 0.0F, -0.5125F, 0.5F, 2.0F, 0.5F);
        }
        return bounds;
    }

    private static void applyUf6TankTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> { }
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("UF6 tank facing must be horizontal: " + facing);
        }
    }

    private static void applyPuf6TankTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> { }
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("PuF6 tank facing must be horizontal: " + facing);
        }
    }

    private static Direction uf6TankFacing(BlockState state) {
        if (!state.hasProperty(HexafluorideTankBlock.FACING)) {
            throw new IllegalStateException("UF6 tank state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(HexafluorideTankBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("UF6 tank state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction puf6TankFacing(BlockState state) {
        if (!state.hasProperty(HexafluorideTankBlock.FACING)) {
            throw new IllegalStateException("PuF6 tank state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(HexafluorideTankBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("PuF6 tank state has a vertical facing: " + facing);
        }
        return facing;
    }
}
