package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.AshpitBlock;
import com.reinhardt.hbm.blockentity.AshpitBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Direct part-by-part port of 1.7.10 RenderAshpit. */
public final class AshpitBlockEntityRenderer implements LongRangeBlockEntityRenderer<AshpitBlockEntity> {
    private static final ModelResourceLocation MAIN = MachineModelRenderer.standalone("block/machine_ashpit_world");
    private static final ModelResourceLocation DOOR = MachineModelRenderer.standalone("block/machine_ashpit_door");
    private static final ModelResourceLocation INNER = MachineModelRenderer.standalone("block/machine_ashpit_inner");
    private static final ModelResourceLocation INNER_BURNING = MachineModelRenderer.standalone("block/machine_ashpit_inner_burning");
    private static final AABB[] LOCAL_BOUNDS = buildAshpitBounds();

    public AshpitBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MAIN);
        event.register(DOOR);
        event.register(INNER);
        event.register(INNER_BURNING);
    }

    @Override
    public void render(AshpitBlockEntity ashpit, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        BlockState state = ashpit.getBlockState();
        Direction facing = ashpitFacing(state);
        int worldLight = ashpit.getLevel() == null ? packedLight
                : LevelRenderer.getLightColor(ashpit.getLevel(), ashpit.getBlockPos());

        poseStack.pushPose();
        applyAshpitTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MAIN), poseStack, bufferSource, state, worldLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, ashpit.doorAngle(partialTick) * 0.75F / 135.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(DOOR), poseStack, bufferSource, state, worldLight, packedOverlay);
        poseStack.popPose();

        BakedModel inner = MachineModelRenderer.model(ashpit.isFull() ? INNER_BURNING : INNER);
        if (ashpit.isFull()) {
            MachineModelRenderer.renderUnculledFullBright(inner, poseStack, bufferSource, state, packedOverlay);
        } else {
            MachineModelRenderer.renderUnculled(inner, poseStack, bufferSource, state, worldLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AshpitBlockEntity ashpit) {
        Direction facing = ashpitFacing(ashpit.getBlockState());
        // Full 0..0.75 door slide; the door's Z maximum remains inside the main shell.
        return LOCAL_BOUNDS[facing.ordinal()].move(ashpit.getBlockPos());
    }

    private static AABB[] buildAshpitBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyAshpitTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.5F, 0.0F, -1.5F, 1.6875F, 1.0F, 1.5F);
        }
        return bounds;
    }

    private static void applyAshpitTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Ashpit facing must be horizontal: " + facing);
        }
    }

    private static Direction ashpitFacing(BlockState state) {
        if (!state.hasProperty(AshpitBlock.FACING)) {
            throw new IllegalStateException("Ashpit state is missing its owned facing property: " + state);
        }
        return state.getValue(AshpitBlock.FACING);
    }
}
