package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.DeuteriumTowerBlock;
import com.reinhardt.hbm.blockentity.DeuteriumTowerBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class DeuteriumTowerBlockEntityRenderer implements LongRangeBlockEntityRenderer<DeuteriumTowerBlockEntity> {
    private static final ModelResourceLocation TOWER = MachineModelRenderer.standalone("block/machine_deuterium_tower_world");
    private static final AABB[] LOCAL_BOUNDS = buildLocalBounds();

    public DeuteriumTowerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(TOWER);
    }

    @Override
    public void render(DeuteriumTowerBlockEntity extractor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = extractor.getBlockState();
        if (!state.is(HbmBlocks.MACHINE_DEUTERIUM_TOWER.get())) {
            return;
        }

        Direction facing = deuteriumTowerFacing(state);
        poseStack.pushPose();
        applyDeuteriumTowerWorldPose(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TOWER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Literal ItemRenderLibrary transform for machine_deuterium_tower. */
    public static void renderItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_DEUTERIUM_TOWER.get().defaultBlockState();
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -5.0F, 0.0F);
            poseStack.scale(3.0F, 3.0F, 3.0F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TOWER), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(DeuteriumTowerBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = deuteriumTowerFacing(state);
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildLocalBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        }) {
            PoseStack poseStack = new PoseStack();
            applyDeuteriumTowerWorldPose(poseStack, facing);
            // Face-used baked-float OBJ bounds, transformed by the renderer's exact matrix sequence.
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    BlockPos.ZERO, poseStack,
                    -1.0F, 0.0F, -1.0F,
                    1.0F, 10.57664F, 1.000001F
            );
        }
        return bounds;
    }

    private static void applyDeuteriumTowerWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        switch (facing) {
            case NORTH -> poseStack.translate(0.0F, 0.0F, -1.0F);
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.translate(1.0F, 0.0F, 0.0F);
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.translate(1.0F, 0.0F, -1.0F);
            }
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Deuterium tower facing must be horizontal: " + facing);
        }
    }

    private static Direction deuteriumTowerFacing(BlockState state) {
        if (!state.hasProperty(DeuteriumTowerBlock.FACING)) {
            throw new IllegalStateException("Deuterium tower state is missing its owned facing property: " + state);
        }
        return state.getValue(DeuteriumTowerBlock.FACING);
    }
}
