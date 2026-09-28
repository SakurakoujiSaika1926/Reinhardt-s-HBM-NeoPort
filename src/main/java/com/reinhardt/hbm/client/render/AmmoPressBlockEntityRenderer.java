package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.AmmoPressBlock;
import com.reinhardt.hbm.blockentity.AmmoPressBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class AmmoPressBlockEntityRenderer implements LongRangeBlockEntityRenderer<AmmoPressBlockEntity> {
    private static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/machine_ammo_press_frame_world");
    private static final ModelResourceLocation PRESS = MachineModelRenderer.standalone("block/machine_ammo_press_press_world");
    private static final ModelResourceLocation SHELLS = MachineModelRenderer.standalone("block/machine_ammo_press_shells_world");
    private static final ModelResourceLocation BULLETS = MachineModelRenderer.standalone("block/machine_ammo_press_bullets_world");
    private static final AABB[] LOCAL_BOUNDS = buildAmmoPressBounds();

    public AmmoPressBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(FRAME);
        event.register(PRESS);
        event.register(SHELLS);
        event.register(BULLETS);
    }

    @Override
    public void render(AmmoPressBlockEntity ammoPress, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ammoPress.getBlockState();
        Direction facing = ammoPressFacing(state);
        float lift = ammoPress.lift(partialTick);
        float press = ammoPress.press(partialTick);

        poseStack.pushPose();
        applyAmmoPressTransform(poseStack, facing);

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FRAME), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0F, -press * 0.25F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRESS), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, lift * 0.5F - 0.5F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SHELLS), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (ammoPress.showBullets()) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BULLETS), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AmmoPressBlockEntity blockEntity) {
        Direction facing = ammoPressFacing(blockEntity.getBlockState());
        // Frame bounds contain the complete press/lift travel and both ammunition meshes.
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildAmmoPressBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyAmmoPressTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -0.5F, 0.0F, -1.5F, 0.5F, 2.0F, 1.5F);
        }
        return bounds;
    }

    private static void applyAmmoPressTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Ammo press facing must be horizontal: " + facing);
        }
    }

    private static Direction ammoPressFacing(BlockState state) {
        if (!state.hasProperty(AmmoPressBlock.FACING)) {
            throw new IllegalStateException("Ammo press block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(AmmoPressBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Ammo press block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
