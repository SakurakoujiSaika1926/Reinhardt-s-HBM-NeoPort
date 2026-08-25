package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.blockentity.TeslaCoilBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Random;

/** Direct modern rendering route for 1.7.10 RenderTesla. */
public final class TeslaCoilBlockEntityRenderer implements BlockEntityRenderer<TeslaCoilBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/tesla");

    public TeslaCoilBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(TeslaCoilBlockEntity tesla, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = tesla.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();

        if (tesla.targets().isEmpty()) {
            return;
        }
        Vec3 origin = tesla.emitter();
        poseStack.pushPose();
        poseStack.translate(0.5D, TeslaCoilBlockEntity.EMITTER_OFFSET, 0.5D);
        int phase = (int) ((tesla.getLevel() == null ? 0L : tesla.getLevel().getGameTime()) % 1000L) + 1;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        for (Vec3 target : tesla.targets()) {
            Vec3 vector = target.subtract(origin);
            if (vector.lengthSqr() > 1.0E-5D) {
                renderLegacyRandomBeam(pose, consumer, vector, phase, (int) Math.ceil(vector.length() * 5.0D));
            }
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TeslaCoilBlockEntity tesla) {
        return new AABB(tesla.getBlockPos()).inflate(TeslaCoilBlockEntity.RANGE);
    }

    private static void renderLegacyRandomBeam(PoseStack.Pose pose, VertexConsumer consumer, Vec3 end,
                                               int phase, int segments) {
        Vec3 axis = end.normalize();
        Vec3 side = new Vec3(axis.z, 0.0D, -axis.x);
        if (side.lengthSqr() < 1.0E-5D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize();
        Vec3 vertical = side.cross(axis).normalize();
        Random random = new Random(phase);
        Vec3 previous = Vec3.ZERO;
        int count = Math.max(1, segments);
        for (int index = 1; index <= count; index++) {
            double progress = index / (double) count;
            Vec3 point = end.scale(progress);
            if (index < count) {
                point = point.add(side.scale((random.nextDouble() - 0.5D) * 0.25D))
                        .add(vertical.scale((random.nextDouble() - 0.5D) * 0.25D));
            }
            beamQuad(consumer, pose, previous, point, side.scale(0.03125D), vertical.scale(0.03125D));
            previous = point;
        }
    }

    private static void beamQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end,
                                 Vec3 side, Vec3 vertical) {
        vertex(consumer, pose, start.add(side).add(vertical));
        vertex(consumer, pose, start.subtract(side).add(vertical));
        vertex(consumer, pose, end.subtract(side).subtract(vertical));
        vertex(consumer, pose, end.add(side).subtract(vertical));
        vertex(consumer, pose, start.add(vertical));
        vertex(consumer, pose, start.subtract(vertical));
        vertex(consumer, pose, end.subtract(vertical));
        vertex(consumer, pose, end.add(vertical));
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 point) {
        consumer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(0x40, 0x40, 0x40, 0xFF)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
