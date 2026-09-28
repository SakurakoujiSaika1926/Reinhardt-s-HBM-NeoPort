package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.ConveyorPressBlock;
import com.reinhardt.hbm.blockentity.ConveyorPressBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact object groups and metadata rotation table from RenderConveyorPress. */
public final class ConveyorPressBlockEntityRenderer implements LongRangeBlockEntityRenderer<ConveyorPressBlockEntity> {
    static final ModelResourceLocation PRESS = MachineModelRenderer.standalone("block/machine_conveyor_press_world");
    static final ModelResourceLocation PISTON = MachineModelRenderer.standalone("block/machine_conveyor_press_piston");
    static final ModelResourceLocation BELT = MachineModelRenderer.standalone("block/machine_conveyor_press_belt");
    private static final ResourceLocation BELT_TEXTURE = ReinhardtsHBM.id("textures/models/machines/conveyor_press_belt.png");
    private static final AABB[] LOCAL_BOUNDS = buildConveyorPressBounds();

    public ConveyorPressBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PRESS);
        event.register(PISTON);
        event.register(BELT);
    }

    @Override
    public void render(
            ConveyorPressBlockEntity press,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = press.getBlockState();
        Direction facing = conveyorPressFacing(state);

        poseStack.pushPose();
        applyConveyorPressTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(PRESS), poseStack, bufferSource, state, packedLight, packedOverlay);

        // 1.7.10 only rendered the movable piston when a stamp was installed.
        if (press.hasStamp()) {
            poseStack.pushPose();
            poseStack.translate(0.0D, -press.press(partialTick) * 0.75D, 0.0D);
            MachineModelRenderer.renderUnculled(
                    MachineModelRenderer.model(PISTON), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }

        long worldTime = press.getLevel() == null ? 0L : press.getLevel().getGameTime();
        float beltVOffset = ((worldTime % 16L) - 2L) / 16.0F;
        MachineModelRenderer.renderUnculledUv(
                MachineModelRenderer.model(BELT), poseStack, bufferSource, state, packedLight, packedOverlay,
                BELT_TEXTURE, 0.0F, beltVOffset);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ConveyorPressBlockEntity press) {
        Direction facing = conveyorPressFacing(press.getBlockState());
        // Press body contains the piston across its entire 0..0.75 travel and the belt.
        return LOCAL_BOUNDS[facing.ordinal()].move(press.getBlockPos());
    }

    static void renderItemParts(
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(PRESS), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(PISTON), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(BELT), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static AABB[] buildConveyorPressBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyConveyorPressTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -0.75F, 0.0F, -0.5F, 0.75F, 3.0F, 0.5F);
        }
        return bounds;
    }

    private static void applyConveyorPressTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Conveyor press facing must be horizontal: " + facing);
        }
    }

    private static Direction conveyorPressFacing(BlockState state) {
        if (!state.hasProperty(ConveyorPressBlock.FACING)) {
            throw new IllegalStateException("Conveyor press state is missing its owned facing property: " + state);
        }
        return state.getValue(ConveyorPressBlock.FACING);
    }
}
