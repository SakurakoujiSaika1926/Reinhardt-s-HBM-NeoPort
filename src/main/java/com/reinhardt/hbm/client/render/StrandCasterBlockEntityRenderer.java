package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.StrandCasterBlock;
import com.reinhardt.hbm.blockentity.StrandCasterBlockEntity;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.item.FoundryMoldItem;
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

public class StrandCasterBlockEntityRenderer implements LongRangeBlockEntityRenderer<StrandCasterBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_strand_caster");
    private static final ModelResourceLocation PLATE = MachineModelRenderer.standalone("block/machine_strand_caster_plate");
    private static final ResourceLocation LAVA = ReinhardtsHBM.id("textures/models/machines/lava_gray.png");
    private static final AABB NORTH_BOUNDS = buildLocalRenderBounds(Direction.NORTH);
    private static final AABB SOUTH_BOUNDS = buildLocalRenderBounds(Direction.SOUTH);
    private static final AABB WEST_BOUNDS = buildLocalRenderBounds(Direction.WEST);
    private static final AABB EAST_BOUNDS = buildLocalRenderBounds(Direction.EAST);

    public StrandCasterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(PLATE);
    }

    @Override
    public void render(StrandCasterBlockEntity caster, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = caster.getBlockState();

        poseStack.pushPose();
        applyStrandCasterPose(poseStack, strandCasterFacing(state));

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        renderDynamicMolten(caster, poseStack, bufferSource, state, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(StrandCasterBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = strandCasterFacing(state);
        return localRenderBounds(facing).move(blockEntity.getBlockPos());
    }

    private static AABB localRenderBounds(Direction facing) {
        return switch (facing) {
            case NORTH -> NORTH_BOUNDS;
            case SOUTH -> SOUTH_BOUNDS;
            case WEST -> WEST_BOUNDS;
            case EAST -> EAST_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Strand caster facing must be horizontal: " + facing);
        };
    }

    private static AABB buildLocalRenderBounds(Direction facing) {
        PoseStack pose = new PoseStack();
        applyStrandCasterPose(pose, facing);
        // caster face vertices are [-1,0,-6]..[1,3,1]. The optional plate's
        // complete authored translation has Z [-5.25,1.9], while the molten
        // quad is inside this union. Apply the exact renderer PoseStack.
        return RenderBounds.transformedBox(net.minecraft.core.BlockPos.ZERO, pose,
                -1.0F, 0.0F, -6.0F,
                1.0F, 3.0F, 1.9000001F);
    }

    private static void renderDynamicMolten(
            StrandCasterBlockEntity caster,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedOverlay
    ) {
        FoundryMaterial material = caster.material();
        FoundryMoldItem.Mold mold = caster.getInstalledMold();
        if (material == null || mold == null || caster.amount() <= 0) {
            return;
        }
        int color = 0xFF000000 | material.moltenColor();
        double offset = ((double) caster.amount() / (double) mold.cost()) * 0.375D;

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, Math.max(-offset + 3.4D, 0.0D));
        MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(PLATE), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, color);
        poseStack.popPose();

        double level = ((double) caster.amount() / (double) caster.getCapacity()) * 0.675D;
        renderMoltenSurface(poseStack, bufferSource, packedOverlay, color, 2.3D + level);
    }

    private static void renderMoltenSurface(PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay, int color, double y) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(LAVA));
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -0.9F, (float) y, -0.999F, 0.0F, 0.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, -0.9F, (float) y, 0.999F, 0.0F, 1.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, 0.9F, (float) y, 0.999F, 1.0F, 1.0F, r, g, b, packedOverlay);
        vertex(consumer, pose, 0.9F, (float) y, -0.999F, 1.0F, 0.0F, r, g, b, packedOverlay);
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

    private static Direction strandCasterFacing(BlockState state) {
        if (!state.hasProperty(StrandCasterBlock.FACING)) {
            throw new IllegalStateException("Strand caster state is missing its dedicated facing property");
        }
        return state.getValue(StrandCasterBlock.FACING);
    }

    private static void applyStrandCasterPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case NORTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Strand caster facing must be horizontal: " + facing);
        }
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }
}
