package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.ChargerBlock;
import com.reinhardt.hbm.blockentity.ChargerBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/** Direct model-part port of 1.7.10 RenderCharger. */
public final class ChargerBlockEntityRenderer implements LongRangeBlockEntityRenderer<ChargerBlockEntity> {
    private static final ModelResourceLocation BASE = part("charger_base");
    private static final ModelResourceLocation LEFT = part("charger_left");
    private static final ModelResourceLocation RIGHT = part("charger_right");
    private static final ModelResourceLocation SLIDE = part("charger_slide");
    private static final float[][] LIGHT_VERTICES = {
            {-0.266762F, 0.501450F, -0.023437F},
            {-0.269068F, 0.517861F, -0.016573F},
            {-0.270024F, 0.524659F, 0.0F},
            {-0.269068F, 0.517861F, 0.016573F},
            {-0.266762F, 0.501450F, 0.023438F},
            {-0.264455F, 0.485038F, 0.016573F},
            {-0.263500F, 0.478240F, 0.0F},
            {-0.264455F, 0.485038F, -0.016573F}
    };
    private static final int[][] LIGHT_TRIANGLES = {
            {2, 4, 6}, {0, 1, 2}, {2, 3, 4},
            {4, 5, 6}, {6, 7, 0}, {0, 2, 6}
    };
    private static final AABB[] LOCAL_BOUNDS = buildChargerBounds();

    public ChargerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** ItemRenderLibrary#charger renders exactly these two legacy OBJ groups. */
    static void renderItemAssembly(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SLIDE), poseStack, bufferSource,
                state, packedLight, packedOverlay);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(LEFT);
        event.register(RIGHT);
        event.register(SLIDE);
    }

    @Override
    public void render(ChargerBlockEntity charger, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = charger.getBlockState();
        poseStack.pushPose();
        applyChargerTransform(poseStack, chargerFacing(state));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource,
                state, packedLight, packedOverlay);

        double time = (charger.lastUsingTicks() +
                (charger.usingTicks() - charger.lastUsingTicks()) * partialTick) / (double) charger.delay();
        double extend = Math.min(1.0D, time * 2.0D);
        double swivel = Math.max(0.0D, (time - 0.5D) * 2.0D);

        poseStack.pushPose();
        poseStack.translate(-0.34375D, 0.25D, 0.0D);
        poseStack.mulPose(zRotation(10.0F));
        poseStack.translate(0.34375D, -0.25D, 0.0D);
        poseStack.translate(0.0D, -0.25D * extend, 0.0D);

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.28D, 0.0D);
        poseStack.mulPose(xRotation((float) (30.0D * swivel)));
        poseStack.translate(0.0D, -0.28D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LEFT), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.28D, 0.0D);
        poseStack.mulPose(xRotation((float) (-30.0D * swivel)));
        poseStack.translate(0.0D, -0.28D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RIGHT), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();

        // RenderCharger disables texturing and lighting for this orange lamp.
        // The original Light group has position/normal indices but no UVs, so
        // it must stay on this direct non-textured rendering path.
        renderLegacyLight(poseStack, bufferSource, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(-0.34375D, 0.25D, 0.0D);
        poseStack.mulPose(zRotation(10.0F));
        poseStack.translate(0.34375D, -0.25D, 0.0D);
        poseStack.translate(0.0D, -0.25D * extend, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SLIDE), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ChargerBlockEntity blockEntity) {
        /*
         * Face-used baked-float vertices were evaluated through the exact
         * renderer matrices.  The only global extrema outside Base/Light are
         * Slide at full extension (time=.5); interval extrema of both +/-30
         * degree arm sweeps are strictly inside this union.  In particular,
         * the fixed ten-degree Z rotation puts Slide at y=-0.007054962f, so a
         * vanilla unit box is not sufficient.
         */
        Direction facing = chargerFacing(blockEntity.getBlockState());
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static ModelResourceLocation part(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }

    private static Quaternionf xRotation(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 1.0F, 0.0F, 0.0F));
    }

    private static void renderLegacyLight(PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        for (int[] triangle : LIGHT_TRIANGLES) {
            for (int index : triangle) {
                float[] vertex = LIGHT_VERTICES[index];
                consumer.addVertex(pose, vertex[0], vertex[1], vertex[2])
                        .setColor(255, 191, 0, 255)
                        .setOverlay(packedOverlay)
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(pose, 0.9903F, 0.1392F, 0.0F);
            }
        }
    }

    private static Quaternionf zRotation(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 0.0F, 1.0F));
    }

    private static AABB[] buildChargerBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyChargerTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -0.5F, -0.007054962F, -0.1875F,
                    -0.23878747F, 0.75F, 0.1875F);
        }
        return bounds;
    }

    private static void applyChargerTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            // RenderCharger: fixed 90 degrees, then metadata 2/3/4/5 adds 0/180/90/270.
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Charger facing must be horizontal: " + facing);
        }
    }

    private static Direction chargerFacing(BlockState state) {
        if (!state.hasProperty(ChargerBlock.FACING)) {
            throw new IllegalStateException("Charger block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(ChargerBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Charger block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
