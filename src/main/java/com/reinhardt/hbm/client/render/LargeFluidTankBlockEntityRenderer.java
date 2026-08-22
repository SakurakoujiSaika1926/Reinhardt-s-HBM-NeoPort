package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LargeFluidTankBlock;
import com.reinhardt.hbm.blockentity.LargeFluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class LargeFluidTankBlockEntityRenderer implements BlockEntityRenderer<LargeFluidTankBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_bigasstank");
    private static final ResourceLocation DANGER_DIAMOND =
            ReinhardtsHBM.id("textures/models/misc/danger_diamond.png");

    public LargeFluidTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(LargeFluidTankBlockEntity tank, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = tank.getBlockState();
        Direction facing = state.hasProperty(LargeFluidTankBlock.FACING)
                ? state.getValue(LargeFluidTankBlock.FACING)
                : Direction.SOUTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        if (tank.tilted()) {
            poseStack.translate(0.0D, -1.0D, 0.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(5.0F));
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(legacyYaw(facing)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        renderFluidWindows(tank, partialTick, poseStack, bufferSource, packedOverlay);
        renderHazardDiamonds(tank.tank().type(), poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LargeFluidTankBlockEntity tank) {
        BlockPos pos = tank.getBlockPos();
        // The 1.7.10 renderer covers y..y+5; the upper bound is exclusive.
        return new AABB(pos.getX() - 6.0D, pos.getY(), pos.getZ() - 6.0D,
                pos.getX() + 7.0D, pos.getY() + 5.0D, pos.getZ() + 7.0D);
    }

    private static void renderFluidWindows(LargeFluidTankBlockEntity tank, float partialTick,
                                           PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedOverlay) {
        HbmFluidDefinition fluid = tank.tank().type();
        if (fluid == null || fluid.isNone() || tank.tank().amount() <= 0) {
            return;
        }
        FluidTankBlockEntityRenderer.TextureChoice tankTexture = FluidTankBlockEntityRenderer.tankTexture(fluid);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(tankTexture.texture()));
        double height = tank.tank().amount() * 1.5D / tank.tank().capacity();
        double offset = 5.9375D;
        double scroll = -((tank.getLevel().getGameTime() % 250L + partialTick) / 250.0D) % 1.0D;
        float minU = (float) scroll;
        float maxU = minU + 0.5F;
        float maxV = (float) (-height);
        PoseStack.Pose pose = poseStack.last();
        fluidQuad(consumer, pose, -offset, 1.75D, -0.25D, 1.75D + height, 0.25D,
                minU, maxU, maxV, packedOverlay, false, tankTexture.color());
        fluidQuad(consumer, pose, offset, 1.75D, -0.25D, 1.75D + height, 0.25D,
                minU, maxU, maxV, packedOverlay, true, tankTexture.color());
    }

    private static void fluidQuad(VertexConsumer consumer, PoseStack.Pose pose, double x, double minY,
                                  double minZ, double maxY, double maxZ, float minU, float maxU,
                                  float maxV, int overlay, boolean reverse, int argb) {
        if (!reverse) {
            fluidVertex(consumer, pose, x, minY, minZ, minU, 0.0F, overlay, -1.0F, argb);
            fluidVertex(consumer, pose, x, maxY, minZ, minU, maxV, overlay, -1.0F, argb);
            fluidVertex(consumer, pose, x, maxY, maxZ, maxU, maxV, overlay, -1.0F, argb);
            fluidVertex(consumer, pose, x, minY, maxZ, maxU, 0.0F, overlay, -1.0F, argb);
        } else {
            fluidVertex(consumer, pose, x, minY, minZ, maxU, 0.0F, overlay, 1.0F, argb);
            fluidVertex(consumer, pose, x, minY, maxZ, minU, 0.0F, overlay, 1.0F, argb);
            fluidVertex(consumer, pose, x, maxY, maxZ, minU, maxV, overlay, 1.0F, argb);
            fluidVertex(consumer, pose, x, maxY, minZ, maxU, maxV, overlay, 1.0F, argb);
        }
    }

    private static void fluidVertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y,
                                    double z, float u, float v, int overlay, float normalX, int argb) {
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        int alpha = (argb >>> 24) & 0xFF;
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, normalX, 0.0F, 0.0F);
    }

    private static void renderHazardDiamonds(HbmFluidDefinition fluid, PoseStack poseStack,
                                             MultiBufferSource bufferSource, int overlay) {
        if (fluid == null || fluid.isNone()) {
            return;
        }
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(DANGER_DIAMOND));
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(22.5F));
        for (int side = 0; side < 2; side++) {
            poseStack.pushPose();
            poseStack.translate(5.5D, 2.0D, 0.0D);
            FluidBarrelBlockEntityRenderer.renderDiamond(fluid, poseStack, consumer,
                    LightTexture.FULL_BRIGHT, overlay);
            poseStack.popPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        }
        poseStack.popPose();
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 270.0F;
            case EAST -> 180.0F;
            case SOUTH -> 90.0F;
            case WEST -> 0.0F;
            default -> 0.0F;
        };
    }

    private record BlockPosBounds(net.minecraft.core.BlockPos min, net.minecraft.core.BlockPos max) {
        static BlockPosBounds around(net.minecraft.core.BlockPos pos, int west, int down, int north,
                                     int east, int up, int south) {
            return new BlockPosBounds(pos.offset(-west, -down, -north), pos.offset(east + 1, up + 1, south + 1));
        }

        AABB box() {
            return new AABB(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        }
    }
}
