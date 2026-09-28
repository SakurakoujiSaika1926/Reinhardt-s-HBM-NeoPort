package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.LeviathanTurbineBlock;
import com.reinhardt.hbm.blockentity.LeviathanTurbineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class LeviathanTurbineBlockEntityRenderer implements LongRangeBlockEntityRenderer<LeviathanTurbineBlockEntity> {
    private static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/machine_chungus_body");
    private static final ModelResourceLocation LEVER = MachineModelRenderer.standalone("block/machine_chungus_lever");
    private static final ModelResourceLocation BLADES = MachineModelRenderer.standalone("block/machine_chungus_blades");
    private static final AABB NORTH_BOUNDS = buildLocalRenderBounds(Direction.NORTH);
    private static final AABB SOUTH_BOUNDS = buildLocalRenderBounds(Direction.SOUTH);
    private static final AABB WEST_BOUNDS = buildLocalRenderBounds(Direction.WEST);
    private static final AABB EAST_BOUNDS = buildLocalRenderBounds(Direction.EAST);

    public LeviathanTurbineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(LEVER);
        event.register(BLADES);
    }

    @Override
    public void render(LeviathanTurbineBlockEntity turbine, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = turbine.getBlockState();

        poseStack.pushPose();
        applyLeviathanTurbinePose(poseStack, leviathanTurbineFacing(state));

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BODY), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 4.5D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(turbine.leverAngle()), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, 0.0D, -4.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LEVER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 2.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(turbine.rotor(partialTick)), 0.0F, 0.0F, -1.0F)));
        poseStack.translate(0.0D, -2.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BLADES), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LeviathanTurbineBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = leviathanTurbineFacing(state);
        return localRenderBounds(facing).move(blockEntity.getBlockPos());
    }

    private static AABB localRenderBounds(Direction facing) {
        return switch (facing) {
            case EAST -> EAST_BOUNDS;
            case SOUTH -> SOUTH_BOUNDS;
            case WEST -> WEST_BOUNDS;
            case NORTH -> NORTH_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Leviathan turbine facing must be horizontal: " + facing);
        };
    }

    private static AABB buildLocalRenderBounds(Direction facing) {
        PoseStack pose = new PoseStack();
        applyLeviathanTurbinePose(pose, facing);

        /*
         * Face-referenced baked-float Body vertices are exactly
         * [-2.5,0,-7.5]..[2.75,5,7.4940009117126465].  This is also the exact
         * union of every rendered group: Blades' full Z-axis sweep about
         * (0,2.5) has radius 2.260393663808342 and Z 3.75..5.25, while Lever's
         * four legal angles {15,5,-5,-15} remain X 2.53125..2.71875,
         * Y 0.10456453796722599..1.8514614609221631 and
         * Z 3.8989495455277186..5.037018405187175.  All are strictly inside
         * Body, so no dynamic field or guessed animation margin is required.
         */
        return RenderBounds.transformedBox(net.minecraft.core.BlockPos.ZERO, pose,
                -2.5F, 0.0F, -7.5F,
                2.75F, 5.0F, 7.494001F);
    }

    private static Direction leviathanTurbineFacing(BlockState state) {
        if (!state.hasProperty(LeviathanTurbineBlock.FACING)) {
            throw new IllegalStateException("Leviathan turbine state is missing its dedicated facing property");
        }
        return state.getValue(LeviathanTurbineBlock.FACING);
    }

    private static void applyLeviathanTurbinePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Leviathan turbine facing must be horizontal: " + facing);
        }
        poseStack.translate(0.0D, 0.0D, -3.0D);
    }
}
