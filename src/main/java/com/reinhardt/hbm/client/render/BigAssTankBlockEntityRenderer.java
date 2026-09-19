package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.BigAssTankBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class BigAssTankBlockEntityRenderer implements LongRangeBlockEntityRenderer<BigAssTankBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_bat9000");
    private static final ResourceLocation DANGER_DIAMOND =
            ReinhardtsHBM.id("textures/models/misc/danger_diamond.png");

    public BigAssTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(BigAssTankBlockEntity tank, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = tank.getBlockState();
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        renderFluidWindows(tank, partialTick, poseStack, bufferSource, packedOverlay);
        renderHazardDiamonds(tank.tank().type(), poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BigAssTankBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 2.0D,
                pos.getY(),
                pos.getZ() - 2.0D,
                pos.getX() + 3.0D,
                pos.getY() + 5.0D,
                pos.getZ() + 3.0D
        );
    }

    private static void renderFluidWindows(BigAssTankBlockEntity tank, float partialTick,
                                           PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedOverlay) {
        HbmFluidDefinition fluid = tank.tank().type();
        if (fluid == null || fluid.isNone() || tank.tank().amount() <= 0 || tank.getLevel() == null) {
            return;
        }
        FluidTankBlockEntityRenderer.TextureChoice tankTexture = FluidTankBlockEntityRenderer.tankTexture(fluid);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(tankTexture.texture()));
        double height = tank.tank().amount() * 1.5D / tank.tank().capacity();
        double offset = 2.2D;
        double scroll = -((tank.getLevel().getGameTime() % 250L + partialTick) / 250.0D) % 1.0D;
        float minU = (float) scroll;
        float maxU = minU + 0.5F;
        float maxV = (float) (-height * 2.0D);
        PoseStack.Pose pose = poseStack.last();
        quadX(consumer, pose, -offset, 1.5D, -0.5D, 1.5D + height, 0.5D,
                minU, maxU, maxV, packedOverlay, false, tankTexture.color());
        quadX(consumer, pose, offset, 1.5D, -0.5D, 1.5D + height, 0.5D,
                minU, maxU, maxV, packedOverlay, true, tankTexture.color());
        quadZ(consumer, pose, -0.5D, 1.5D, -offset, 0.5D, 1.5D + height,
                minU, maxU, maxV, packedOverlay, false, tankTexture.color());
        quadZ(consumer, pose, -0.5D, 1.5D, offset, 0.5D, 1.5D + height,
                minU, maxU, maxV, packedOverlay, true, tankTexture.color());
    }

    private static void quadX(VertexConsumer consumer, PoseStack.Pose pose, double x, double minY,
                              double minZ, double maxY, double maxZ, float minU, float maxU,
                              float maxV, int overlay, boolean reverse, int argb) {
        if (!reverse) {
            vertex(consumer, pose, x, minY, minZ, minU, 0.0F, overlay, -1.0F, 0.0F, 0.0F, argb);
            vertex(consumer, pose, x, maxY, minZ, minU, maxV, overlay, -1.0F, 0.0F, 0.0F, argb);
            vertex(consumer, pose, x, maxY, maxZ, maxU, maxV, overlay, -1.0F, 0.0F, 0.0F, argb);
            vertex(consumer, pose, x, minY, maxZ, maxU, 0.0F, overlay, -1.0F, 0.0F, 0.0F, argb);
        } else {
            vertex(consumer, pose, x, minY, minZ, maxU, 0.0F, overlay, 1.0F, 0.0F, 0.0F, argb);
            vertex(consumer, pose, x, minY, maxZ, minU, 0.0F, overlay, 1.0F, 0.0F, 0.0F, argb);
            vertex(consumer, pose, x, maxY, maxZ, minU, maxV, overlay, 1.0F, 0.0F, 0.0F, argb);
            vertex(consumer, pose, x, maxY, minZ, maxU, maxV, overlay, 1.0F, 0.0F, 0.0F, argb);
        }
    }

    private static void quadZ(VertexConsumer consumer, PoseStack.Pose pose, double minX, double minY,
                              double z, double maxX, double maxY, float minU, float maxU,
                              float maxV, int overlay, boolean reverse, int argb) {
        if (!reverse) {
            vertex(consumer, pose, minX, minY, z, minU, 0.0F, overlay, 0.0F, 0.0F, -1.0F, argb);
            vertex(consumer, pose, minX, maxY, z, minU, maxV, overlay, 0.0F, 0.0F, -1.0F, argb);
            vertex(consumer, pose, maxX, maxY, z, maxU, maxV, overlay, 0.0F, 0.0F, -1.0F, argb);
            vertex(consumer, pose, maxX, minY, z, maxU, 0.0F, overlay, 0.0F, 0.0F, -1.0F, argb);
        } else {
            vertex(consumer, pose, minX, minY, z, maxU, 0.0F, overlay, 0.0F, 0.0F, 1.0F, argb);
            vertex(consumer, pose, maxX, minY, z, minU, 0.0F, overlay, 0.0F, 0.0F, 1.0F, argb);
            vertex(consumer, pose, maxX, maxY, z, minU, maxV, overlay, 0.0F, 0.0F, 1.0F, argb);
            vertex(consumer, pose, minX, maxY, z, maxU, maxV, overlay, 0.0F, 0.0F, 1.0F, argb);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z,
                               float u, float v, int overlay, float nx, float ny, float nz, int argb) {
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        int alpha = (argb >>> 24) & 0xFF;
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, alpha).setUv(u, v).setOverlay(overlay)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, nx, ny, nz);
    }

    private static void renderHazardDiamonds(HbmFluidDefinition fluid, PoseStack poseStack,
                                             MultiBufferSource bufferSource, int overlay) {
        if (fluid == null || fluid.isNone()) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(DANGER_DIAMOND));
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        for (int side = 0; side < 4; side++) {
            poseStack.pushPose();
            poseStack.translate(2.5D, 2.25D, 0.0D);
            poseStack.scale(1.0F, 0.75F, 0.75F);
            FluidBarrelBlockEntityRenderer.renderDiamond(fluid, poseStack, consumer,
                    LightTexture.FULL_BRIGHT, overlay);
            poseStack.popPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.popPose();
    }
}
