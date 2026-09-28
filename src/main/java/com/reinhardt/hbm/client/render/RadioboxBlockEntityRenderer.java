package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.RadioboxBlock;
import com.reinhardt.hbm.blockentity.RadioboxBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

/** Direct port of the radiobox branch in 1.7.10 RenderDecoBlock. */
public final class RadioboxBlockEntityRenderer implements LongRangeBlockEntityRenderer<RadioboxBlockEntity> {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/block/radiobox.png");
    private static final AABB[][] LOCAL_BOUNDS = buildLocalBounds();

    private final LegacyRadioboxModel model;

    public RadioboxBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new LegacyRadioboxModel(context.bakeLayer(LegacyRadioboxModel.LAYER));
    }

    @Override
    public void render(RadioboxBlockEntity radiobox, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = radiobox.getBlockState();
        poseStack.pushPose();
        applyRadioboxPose(poseStack, state.getValue(RadioboxBlock.FACING));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.render(poseStack, consumer, packedLight, packedOverlay,
                state.getValue(RadioboxBlock.ACTIVE));
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RadioboxBlockEntity radiobox) {
        BlockState state = radiobox.getBlockState();
        Direction facing = state.getValue(RadioboxBlock.FACING);
        return LOCAL_BOUNDS[state.getValue(RadioboxBlock.ACTIVE) ? 1 : 0][facing.ordinal()]
                .move(radiobox.getBlockPos());
    }

    private static AABB[][] buildLocalBounds() {
        AABB[][] bounds = new AABB[2][Direction.values().length];
        for (int active = 0; active < 2; active++) {
            for (Direction facing : new Direction[] {
                    Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
            }) {
                bounds[active][facing.ordinal()] = buildLocalBounds(facing, active != 0);
            }
        }
        return bounds;
    }

    private static AABB buildLocalBounds(Direction facing, boolean active) {
        PoseStack poseStack = new PoseStack();
        applyRadioboxPose(poseStack, facing);

        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(BlockPos.ZERO);
        includePart(bounds, poseStack, -4.0F, 9.0F, -12.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 8.0F, 14.0F, 4.0F);
        includePart(bounds, poseStack, -3.5F, 9.5F, -12.5F, 0.0F,
                0.0F, 0.0F, 0.0F, 7.0F, 13.0F, 1.0F);
        includePart(bounds, poseStack, 4.0F, 16.0F, -10.0F,
                (float) Math.toRadians(active ? -160.0F : -20.0F),
                0.0F, -1.0F, -1.0F, 2.0F, 7.0F, 1.0F);
        return bounds.build();
    }

    /** Mirrors ModelPart child translation/X rotation and baked /16 cube vertices. */
    private static void includePart(RenderBounds.FloatBuilder bounds, PoseStack poseStack,
                                    float pivotX, float pivotY, float pivotZ, float xRot,
                                    float minX, float minY, float minZ,
                                    float maxX, float maxY, float maxZ) {
        poseStack.pushPose();
        poseStack.translate(pivotX / 16.0F, pivotY / 16.0F, pivotZ / 16.0F);
        if (xRot != 0.0F) {
            poseStack.mulPose(new Quaternionf().rotationZYX(0.0F, 0.0F, xRot));
        }
        bounds.includeBox(poseStack,
                minX / 16.0F, minY / 16.0F, minZ / 16.0F,
                maxX / 16.0F, maxY / 16.0F, maxZ / 16.0F);
        poseStack.popPose();
    }

    private static void applyRadioboxPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        switch (facing) {
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Radiobox facing must be horizontal: " + facing);
        }
        poseStack.translate(0.0F, 0.0F, 1.0F);
    }
}
