package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.blockentity.SolarMirrorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class SolarMirrorBlockEntityRenderer implements BlockEntityRenderer<SolarMirrorBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/solar_mirror_base");
    private static final ModelResourceLocation MIRROR = MachineModelRenderer.standalone("block/solar_mirror_mirror");

    public SolarMirrorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(MIRROR);
    }

    @Override
    public void render(SolarMirrorBlockEntity mirror, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = mirror.getBlockState();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0D, 1.0D, 0.0D);
        Rotation rotation = targetRotation(mirror);
        if (rotation.valid()) {
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotation.yaw()), 0.0F, 1.0F, 0.0F)));
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotation.pitch()), 0.0F, 0.0F, 1.0F)));
        }
        poseStack.translate(0.0D, -1.0D, 0.0D);

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MIRROR), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (mirror.isOn() && rotation.valid()) {
            renderBeam(poseStack, bufferSource, rotation.distance());
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SolarMirrorBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(25.0D);
    }

    private static Rotation targetRotation(SolarMirrorBlockEntity mirror) {
        BlockPos target = mirror.target();
        BlockPos pos = mirror.getBlockPos();
        int dx = target.getX() - pos.getX();
        int dy = target.getY() - pos.getY();
        int dz = target.getZ() - pos.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (target.getY() < pos.getY() || distance <= 0.0001D) {
            return Rotation.invalid();
        }
        double pitch = Math.toDegrees(-Math.asin((dy + 0.5D) / distance)) + 90.0D;
        double yaw = Math.toDegrees(-Math.atan2(dz, dx)) + 180.0D;
        return new Rotation(true, yaw, pitch, distance);
    }

    private static void renderBeam(PoseStack poseStack, MultiBufferSource bufferSource, double distance) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        float minAlpha = 2.0F / 255.0F;
        float maxAlpha = 2.0F / 255.0F;
        quad(consumer, pose, 0.5F, 1.0625F, 0.5F, 0.5F, 1.0625F, -0.5F, 0.5F, (float) distance, -0.5F, 0.5F, (float) distance, 0.5F, maxAlpha, minAlpha);
        quad(consumer, pose, -0.5F, 1.0625F, 0.5F, -0.5F, 1.0625F, -0.5F, -0.5F, (float) distance, -0.5F, -0.5F, (float) distance, 0.5F, maxAlpha, minAlpha);
        quad(consumer, pose, 0.5F, 1.0625F, 0.5F, -0.5F, 1.0625F, 0.5F, -0.5F, (float) distance, 0.5F, 0.5F, (float) distance, 0.5F, maxAlpha, minAlpha);
        quad(consumer, pose, 0.5F, 1.0625F, -0.5F, -0.5F, 1.0625F, -0.5F, -0.5F, (float) distance, -0.5F, 0.5F, (float) distance, -0.5F, maxAlpha, minAlpha);
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz,
            float startAlpha,
            float endAlpha
    ) {
        vertex(consumer, pose, ax, ay, az, startAlpha);
        vertex(consumer, pose, bx, by, bz, startAlpha);
        vertex(consumer, pose, cx, cy, cz, endAlpha);
        vertex(consumer, pose, dx, dy, dz, endAlpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float alpha) {
        consumer.addVertex(pose, x, y, z).setColor(255, 255, 255, Math.round(alpha * 255.0F));
    }

    private record Rotation(boolean valid, double yaw, double pitch, double distance) {
        private static Rotation invalid() {
            return new Rotation(false, 0.0D, 0.0D, 0.0D);
        }
    }
}
