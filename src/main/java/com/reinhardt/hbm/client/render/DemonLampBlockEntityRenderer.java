package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.DemonLampBlock;
import com.reinhardt.hbm.blockentity.DemonLampBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** RenderDemonLamp's six-face transform and pair of additive blue radiation cones. */
public final class DemonLampBlockEntityRenderer implements BlockEntityRenderer<DemonLampBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/lamp_demon");
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/models/machines/demon_lamp.png");

    public DemonLampBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(DemonLampBlockEntity lamp, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = lamp.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        orient(poseStack, state.getValue(DemonLampBlock.FACING));
        poseStack.translate(0.0D, -0.5D, 0.0D);
        renderModel(state, poseStack, bufferSource, packedLight, packedOverlay);
        renderCones(poseStack, bufferSource.getBuffer(RenderType.lightning()));
        poseStack.popPose();
    }

    /** Literal ItemRenderLibrary transform for lamp_demon. */
    public static void renderItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int packedLight, int packedOverlay) {
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(8.0F, 8.0F, 8.0F);
        }
        renderModel(com.reinhardt.hbm.registry.HbmBlocks.LAMP_DEMON.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(DemonLampBlockEntity lamp) {
        return new AABB(lamp.getBlockPos()).inflate(15.0D);
    }

    private static void orient(PoseStack poseStack, Direction facing) {
        // Literal RenderDemonLamp metadata transforms; no shared orientation table is used.
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            case UP -> {
            }
            case NORTH -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            }
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case WEST -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }
            case EAST -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(270.0F));
            }
        }
    }

    private static void renderModel(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        // RenderDemonLamp binds its texture directly. Doing the same avoids
        // the OBJ material atlas route that made this model disappear.
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay, TEXTURE, 0.0F, 0.0F);
    }

    private static void renderCones(PoseStack poseStack, VertexConsumer consumer) {
        PoseStack.Pose pose = poseStack.last();
        for (int cone = 0; cone < 2; cone++) {
            double nearY = 0.5D + cone * 0.125D;
            double farY = nearY + (cone == 0 ? -0.5D : 0.5D);
            for (int segment = 0; segment < 16; segment++) {
                double start = Math.PI * 2.0D * segment / 16.0D;
                double end = Math.PI * 2.0D * (segment + 1) / 16.0D;
                vertex(consumer, pose, Math.cos(start) * 0.375D, nearY, Math.sin(start) * 0.375D, 64);
                vertex(consumer, pose, Math.cos(start) * 15.0D, farY, Math.sin(start) * 15.0D, 0);
                vertex(consumer, pose, Math.cos(end) * 15.0D, farY, Math.sin(end) * 15.0D, 0);
                vertex(consumer, pose, Math.cos(end) * 0.375D, nearY, Math.sin(end) * 0.375D, 64);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, int alpha) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(0, 191, 255, alpha)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
