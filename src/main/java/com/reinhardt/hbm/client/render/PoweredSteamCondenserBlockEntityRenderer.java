package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.PoweredSteamCondenserBlock;
import com.reinhardt.hbm.blockentity.PoweredSteamCondenserBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class PoweredSteamCondenserBlockEntityRenderer implements LongRangeBlockEntityRenderer<PoweredSteamCondenserBlockEntity> {
    private static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/machine_condenser_powered_body");
    private static final ModelResourceLocation FAN1 = MachineModelRenderer.standalone("block/machine_condenser_powered_fan1");
    private static final ModelResourceLocation FAN2 = MachineModelRenderer.standalone("block/machine_condenser_powered_fan2");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final AABB NORTH_RENDER_BOUNDS = createCondenserLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_RENDER_BOUNDS = createCondenserLocalBounds(Direction.SOUTH);
    private static final AABB WEST_RENDER_BOUNDS = createCondenserLocalBounds(Direction.WEST);
    private static final AABB EAST_RENDER_BOUNDS = createCondenserLocalBounds(Direction.EAST);

    public PoweredSteamCondenserBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(FAN1);
        event.register(FAN2);
    }

    @Override
    public void render(PoweredSteamCondenserBlockEntity condenser, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = condenser.getBlockState();
        Direction facing = poweredSteamCondenserFacing(state);

        poseStack.pushPose();
        applyCondenserPose(poseStack, facing);

        STATIC_MESHES.render(BODY, poseStack, state, packedLight, packedOverlay);

        float spin = condenser.spin(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(spin));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        STATIC_MESHES.render(FAN1, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(Axis.XN.rotationDegrees(spin));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        STATIC_MESHES.render(FAN2, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PoweredSteamCondenserBlockEntity blockEntity) {
        Direction facing = poweredSteamCondenserFacing(blockEntity.getBlockState());
        return condenserRenderBounds(blockEntity.getBlockPos(), facing);
    }

    private static void applyCondenserPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Powered condenser facing must be horizontal: " + facing);
        }
    }

    static AABB condenserRenderBounds(BlockPos pos, Direction facing) {
        AABB localBounds = switch (facing) {
            case NORTH -> NORTH_RENDER_BOUNDS;
            case SOUTH -> SOUTH_RENDER_BOUNDS;
            case WEST -> WEST_RENDER_BOUNDS;
            case EAST -> EAST_RENDER_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Powered condenser facing must be horizontal: " + facing);
        };
        return localBounds.move(pos);
    }

    private static AABB createCondenserLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyCondenserPose(poseStack, facing);
        // The body's 1,242 face-used vertices dominate both fans' complete 0..360-degree sweeps.
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.625F, -0.125F, -3.5F,
                1.625F, 3.125F, 3.5F);
    }

    private static Direction poweredSteamCondenserFacing(BlockState state) {
        if (!state.hasProperty(PoweredSteamCondenserBlock.FACING)) {
            throw new IllegalStateException("Powered condenser block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(PoweredSteamCondenserBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Powered condenser block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
