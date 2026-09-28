package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LegacyFurnaceBlock;
import com.reinhardt.hbm.blockentity.IronFurnaceBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class IronFurnaceBlockEntityRenderer implements LongRangeBlockEntityRenderer<IronFurnaceBlockEntity> {
    private static final ModelResourceLocation MAIN = MachineModelRenderer.standalone("block/furnace_iron_main");
    private static final ModelResourceLocation ON = MachineModelRenderer.standalone("block/furnace_iron_on");
    private static final ModelResourceLocation OFF = MachineModelRenderer.standalone("block/furnace_iron_off");
    private static final AABB[] LOCAL_BOUNDS = buildIronFurnaceBounds();

    public IronFurnaceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MAIN);
        event.register(ON);
        event.register(OFF);
    }

    @Override
    public void render(IronFurnaceBlockEntity furnace, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = furnace.getBlockState();
        Direction facing = ironFurnaceFacing(state);
        boolean lit = state.hasProperty(LegacyFurnaceBlock.LIT) && state.getValue(LegacyFurnaceBlock.LIT);

        poseStack.pushPose();
        applyIronFurnaceTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MAIN), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (lit) {
            MachineModelRenderer.renderUnculledFullBright(MachineModelRenderer.model(ON), poseStack, bufferSource, state, packedOverlay);
        } else {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(OFF), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(IronFurnaceBlockEntity blockEntity) {
        Direction facing = ironFurnaceFacing(blockEntity.getBlockState());
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildIronFurnaceBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyIronFurnaceTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.0F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F);
        }
        return bounds;
    }

    private static void applyIronFurnaceTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Iron furnace facing must be horizontal: " + facing);
        }
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    private static Direction ironFurnaceFacing(BlockState state) {
        if (!state.hasProperty(LegacyFurnaceBlock.FACING)) {
            throw new IllegalStateException("Iron furnace state is missing its owned facing property: " + state);
        }
        return state.getValue(LegacyFurnaceBlock.FACING);
    }
}
