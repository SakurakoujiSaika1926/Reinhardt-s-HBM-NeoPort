package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.SatelliteGroundStationBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Literal RenderSatLink transform with independently rendered Base, Rotor and Dish groups. */
public final class SatelliteGroundStationBlockEntityRenderer
        implements LongRangeBlockEntityRenderer<SatelliteGroundStationBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_satlink_base");
    private static final ModelResourceLocation ROTOR = MachineModelRenderer.standalone("block/machine_satlink_rotor");
    private static final ModelResourceLocation DISH = MachineModelRenderer.standalone("block/machine_satlink_dish");

    /*
     * The dish OBJ bounds are X/Z +/-1.5 and Y 7.125..8.875. Its pivot is
     * (0, 7.375, 0), so every permitted rotor/lift pose lies inside the exact
     * rotation-invariant sphere sqrt(1.5^2 + 1.5^2 + 1.5^2).
     */
    private static final double MOVING_RADIUS = Math.sqrt(6.75D);

    public SatelliteGroundStationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(ROTOR);
        event.register(DISH);
    }

    @Override
    public void render(SatelliteGroundStationBlockEntity station, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = station.getBlockState();
        if (!state.is(HbmBlocks.MACHINE_SATLINK.get())) {
            return;
        }
        Direction facing = facing(state);
        poseStack.pushPose();
        applyWorldPose(poseStack, facing);
        renderPart(BASE, poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.mulPose(Axis.YP.rotationDegrees(station.rotation(partialTick)));
        renderPart(ROTOR, poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 7.375F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(station.lift(partialTick)));
        poseStack.translate(0.0F, -7.375F, 0.0F);
        renderPart(DISH, poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Literal ItemRenderBase/RenderSatLink inventory and held-item transform. */
    public static void renderItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers,
                                  int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_SATLINK.get().defaultBlockState();
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -5.0F, 0.0F);
            poseStack.scale(3.5F, 3.5F, 3.5F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        renderPart(BASE, poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
        renderPart(ROTOR, poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 7.375F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        poseStack.translate(0.0F, -7.375F, 0.0F);
        renderPart(DISH, poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SatelliteGroundStationBlockEntity station) {
        Direction facing = facing(station.getBlockState());
        double centerX = facing == Direction.NORTH || facing == Direction.WEST ? 1.0D : 0.0D;
        double centerZ = facing == Direction.NORTH || facing == Direction.EAST ? 1.0D : 0.0D;
        return RenderBounds.localAnalytic(
                station.getBlockPos(),
                centerX - MOVING_RADIUS, 0.0D, centerZ - MOVING_RADIUS,
                centerX + MOVING_RADIUS, 7.375D + MOVING_RADIUS, centerZ + MOVING_RADIUS);
    }

    private static void applyWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        switch (facing) {
            case SOUTH -> poseStack.translate(0.5F, 0.0F, 0.5F);
            case NORTH -> poseStack.translate(-0.5F, 0.0F, -0.5F);
            case EAST -> poseStack.translate(0.5F, 0.0F, -0.5F);
            case WEST -> poseStack.translate(-0.5F, 0.0F, 0.5F);
            case UP, DOWN -> throw new IllegalStateException("Ground-station facing is vertical: " + facing);
        }
    }

    private static Direction facing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("Ground-station state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(LargeMachineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Ground-station facing is vertical: " + facing);
        }
        return facing;
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack,
                                   MultiBufferSource buffers, BlockState state,
                                   int packedLight, int packedOverlay) {
        STATIC_MESHES.render(model, poseStack, state, packedLight, packedOverlay);
    }
}
