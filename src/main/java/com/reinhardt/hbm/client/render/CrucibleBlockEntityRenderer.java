package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.CrucibleBlock;
import com.reinhardt.hbm.blockentity.CrucibleBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class CrucibleBlockEntityRenderer implements LongRangeBlockEntityRenderer<CrucibleBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_crucible_world");
    private static final ResourceLocation LAVA = ReinhardtsHBM.id("textures/models/machines/lava.png");

    public CrucibleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(CrucibleBlockEntity crucible, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = crucible.getBlockState();

        poseStack.pushPose();
        applyCruciblePose(poseStack, crucibleFacing(state));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(WORLD), poseStack, bufferSource, state, packedLight, packedOverlay);
        renderMoltenSurface(crucible, poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CrucibleBlockEntity blockEntity) {
        // The variable-height molten quad is wholly inside the +/-1.5 by 0..1.5 OBJ shell.
        return RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                -1.0D, 0.0D, -1.0D, 2.0D, 1.5D, 2.0D);
    }

    private static void renderMoltenSurface(CrucibleBlockEntity crucible, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        CrucibleBlockEntity.MoltenRenderSnapshot snapshot = crucible.moltenRenderSnapshot();
        int totalMass = snapshot.totalMass();
        int color = snapshot.color();
        if (totalMass <= 0) {
            return;
        }

        double level = 0.5D + ((double) totalMass / (double) (CrucibleBlockEntity.RECIPE_CAPACITY + CrucibleBlockEntity.WASTE_CAPACITY)) * 0.875D;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(LAVA));
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -1.0F, (float) level, -1.0F, 0.0F, 0.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, -1.0F, (float) level, 1.0F, 0.0F, 1.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, 1.0F, (float) level, 1.0F, 1.0F, 1.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, 1.0F, (float) level, -1.0F, 1.0F, 0.0F, r, g, b, packedOverlay);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, int r, int g, int b, int packedOverlay) {
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, 220)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static Direction crucibleFacing(BlockState state) {
        if (!state.hasProperty(CrucibleBlock.FACING)) {
            throw new IllegalStateException("Crucible state is missing its dedicated facing property");
        }
        return state.getValue(CrucibleBlock.FACING);
    }

    private static void applyCruciblePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Crucible facing must be horizontal: " + facing);
        }
    }
}
