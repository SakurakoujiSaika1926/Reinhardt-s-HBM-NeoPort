package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.CrystallizerBlock;
import com.reinhardt.hbm.blockentity.CrystallizerBlockEntity;
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

public class CrystallizerBlockEntityRenderer implements LongRangeBlockEntityRenderer<CrystallizerBlockEntity> {
    private static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/machine_crystallizer_body");
    private static final ModelResourceLocation SPINNER = MachineModelRenderer.standalone("block/machine_crystallizer_spinner");
    private static final ModelResourceLocation FLUID = MachineModelRenderer.standalone("block/machine_crystallizer_fluid");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final AABB[] LOCAL_BOUNDS = buildCrystallizerBounds();

    public CrystallizerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(SPINNER);
        event.register(FLUID);
    }

    @Override
    public void render(CrystallizerBlockEntity crystallizer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = crystallizer.getBlockState();
        Direction facing = crystallizerFacing(state);

        poseStack.pushPose();
        applyCrystallizerTransform(poseStack, facing);
        STATIC_MESHES.render(BODY, poseStack, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(crystallizer.clientAngle(partialTick)), 0.0F, 1.0F, 0.0F)));
        STATIC_MESHES.render(SPINNER, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        if (crystallizer.isWorking() && crystallizer.acidTank().amount() > 0 && !crystallizer.acidTank().type().isNone()) {
            MachineModelRenderer.renderUnculledTinted(
                    MachineModelRenderer.model(FLUID),
                    poseStack,
                    bufferSource,
                    state,
                    packedLight,
                    packedOverlay,
                    crystallizer.clientFluidColor()
            );
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CrystallizerBlockEntity blockEntity) {
        Direction facing = crystallizerFacing(blockEntity.getBlockState());
        /* Face-used component union. The body's +/-1.5 X/Z contains the spinner
         * at every yaw; the spinner alone supplies the exact 6.625 top. */
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildCrystallizerBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyCrystallizerTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.5F, 0.0F, -1.5F, 1.5F, 6.625F, 1.5F);
        }
        return bounds;
    }

    private static void applyCrystallizerTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Crystallizer facing must be horizontal: " + facing);
        }
    }

    private static Direction crystallizerFacing(BlockState state) {
        if (!state.hasProperty(CrystallizerBlock.FACING)) {
            throw new IllegalStateException("Crystallizer state is missing its owned facing property: " + state);
        }
        return state.getValue(CrystallizerBlock.FACING);
    }
}
