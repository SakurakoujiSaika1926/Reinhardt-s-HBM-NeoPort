package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.BatteryReddBlockEntity;
import com.reinhardt.hbm.client.sound.BatteryReddClientSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.Random;

public class BatteryReddBlockEntityRenderer implements BlockEntityRenderer<BatteryReddBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_battery_redd_base");
    private static final ModelResourceLocation WHEEL = MachineModelRenderer.standalone("block/machine_battery_redd_wheel");
    private static final ModelResourceLocation LIGHTS = MachineModelRenderer.standalone("block/machine_battery_redd_lights");
    private static final ModelResourceLocation PLASMA = MachineModelRenderer.standalone("block/machine_battery_redd_plasma");
    private static final ModelResourceLocation PLASMA_SPARKLE = MachineModelRenderer.standalone("block/machine_battery_redd_plasma_sparkle");

    public BatteryReddBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(WHEEL);
        event.register(LIGHTS);
        event.register(PLASMA);
        event.register(PLASMA_SPARKLE);
    }

    @Override
    public void render(BatteryReddBlockEntity battery, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BatteryReddClientSounds.tick(battery);
        BlockState state = battery.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));
        MachineModelRenderer.renderUnculledCutoutNoCull(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0F, 5.5F, 0.0F);
        float rotation = Mth.lerp(partialTick, battery.prevRotation, battery.rotation);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotation), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0F, -5.5F, 0.0F);
        MachineModelRenderer.renderUnculledCutoutNoCull(MachineModelRenderer.model(WHEEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculledCutoutNoCullFullBright(MachineModelRenderer.model(LIGHTS), poseStack, bufferSource, state, packedOverlay);
        float speed = Math.max(0.0F, Math.min(15.0F, battery.speed()));
        if (speed > 0.0F) {
            renderWheelStreaks(speed, poseStack, bufferSource);
            renderSparkle(battery, speed, poseStack, bufferSource, state, packedOverlay);
        }
        poseStack.popPose();
        if (speed > 0.0F) {
            renderZaps(battery, poseStack, bufferSource);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BatteryReddBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(6.0D, 5.0D, 6.0D);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 270.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static void renderSparkle(BatteryReddBlockEntity battery, float speed, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedOverlay) {
        long time = System.currentTimeMillis();
        float alpha = 0.45F + (float) Math.sin(time / 1000.0D) * 0.15F;
        float alphaMult = speed / 15.0F;
        int baseAlpha = Mth.clamp(Math.round(alpha * alphaMult * 255.0F), 0, 255);
        int brightAlpha = Mth.clamp(Math.round(0.75F * alphaMult * 255.0F), 0, 255);
        float mainOsc = (float) (sps(time / 1000.0D) % 1.0D);
        float sparkleSpin = (float) ((time / 250.0D * -1.0D) % 1.0D);
        float sparkleOsc = (float) (Math.sin(time / 1000.0D) * 0.5D % 1.0D);
        MachineModelRenderer.renderUnculledTintedUvEyes(MachineModelRenderer.model(PLASMA), poseStack, bufferSource, state, packedOverlay, (baseAlpha << 24) | 0xFF40BF, 0.0F, mainOsc);
        if (isNearEnoughForExtraEffects(battery, 100.0D)) {
            MachineModelRenderer.renderUnculledTintedUvEyes(MachineModelRenderer.model(PLASMA_SPARKLE), poseStack, bufferSource, state, packedOverlay, (brightAlpha << 24) | 0xFF80FF, sparkleSpin, sparkleOsc);
        }
    }

    private static double sps(double value) {
        return value - Math.cos(value * Math.PI) / Math.PI;
    }

    private static void renderWheelStreaks(float speed, PoseStack poseStack, MultiBufferSource bufferSource) {
        double span = speed * 0.75D;
        if (span <= 0.0D) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0D, 5.5D, 0.0D);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        double len = 4.25D;
        double width = 0.125D;
        for (int side = -1; side <= 1; side += 2) {
            double xOffset = 0.8125D * side;
            for (int i = 0; i < 8; i++) {
                double base = i * 45.0D;
                wheelStreakSegment(consumer, pose, xOffset, len, width, base, base + span, 191, 128);
                wheelStreakSegment(consumer, pose, xOffset, len, width, base + span, base + span * 2.0D, 128, 64);
                wheelStreakSegment(consumer, pose, xOffset, len, width, base + span * 2.0D, base + span * 3.0D, 64, 0);
            }
        }
        poseStack.popPose();
    }

    private static void wheelStreakSegment(VertexConsumer consumer, PoseStack.Pose pose, double x, double len, double width, double fromDegrees, double toDegrees, int fromAlpha, int toAlpha) {
        Vec3 from = wheelVector(fromDegrees);
        Vec3 to = wheelVector(toDegrees);
        lightningVertex(consumer, pose, x, from.y * len - from.y * width, from.z * len - from.z * width, 255, 255, 0, fromAlpha);
        lightningVertex(consumer, pose, x, from.y * len + from.y * width, from.z * len + from.z * width, 255, 255, 0, fromAlpha);
        lightningVertex(consumer, pose, x, to.y * len + to.y * width, to.z * len + to.z * width, 255, 255, 0, toAlpha);
        lightningVertex(consumer, pose, x, to.y * len - to.y * width, to.z * len - to.z * width, 255, 255, 0, toAlpha);
    }

    private static Vec3 wheelVector(double degrees) {
        double radians = Math.toRadians(degrees);
        return new Vec3(0.0D, Math.cos(radians), Math.sin(radians));
    }

    private static void renderZaps(BatteryReddBlockEntity battery, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (!isNearEnoughForExtraEffects(battery, 100.0D)) {
            return;
        }
        Level level = battery.getLevel();
        long time = level == null ? System.currentTimeMillis() / 50L : level.getGameTime();
        Random random = new Random(time / 5L);
        random.nextBoolean();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        int phase = (int) (System.currentTimeMillis() % 1000L) / 50;
        if (random.nextBoolean()) {
            renderLegacyZap(poseStack.last(), consumer, new Vec3(3.125D, 5.5D, 0.0D), new Vec3(-1.375D, -2.625D, 3.75D), phase);
        }
        if (random.nextBoolean()) {
            renderLegacyZap(poseStack.last(), consumer, new Vec3(-3.125D, 5.5D, 0.0D), new Vec3(1.375D, -2.625D, 3.75D), phase);
        }
        if (random.nextBoolean()) {
            renderLegacyZap(poseStack.last(), consumer, new Vec3(3.125D, 5.5D, 0.0D), new Vec3(-1.375D, -2.625D, -3.75D), phase);
        }
        if (random.nextBoolean()) {
            renderLegacyZap(poseStack.last(), consumer, new Vec3(-3.125D, 5.5D, 0.0D), new Vec3(1.375D, -2.625D, -3.75D), phase);
        }
    }

    private static void renderLegacyZap(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 skeleton, int phase) {
        renderLegacyBeam(pose, consumer, start, skeleton, phase, 15, 0.25D, 3, 0.0625D);
        renderLegacyBeam(pose, consumer, start, skeleton, phase, 1, 0.0D, 3, 0.0625D);
    }

    private static void renderLegacyBeam(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 skeleton, int phase, int segments, double size, int layers, double thickness) {
        Vec3 axisY = skeleton.normalize();
        Vec3 axisX = new Vec3(axisY.z, 0.0D, -axisY.x);
        if (axisX.lengthSqr() < 1.0E-5D) {
            axisX = new Vec3(1.0D, 0.0D, 0.0D);
        }
        axisX = axisX.normalize();
        Vec3 axisZ = axisX.cross(axisY).normalize();
        Random beamRandom = new Random(phase);
        double length = skeleton.length();
        double segmentLength = length / segments;
        Vec3 previous = null;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0D * beamRandom.nextFloat();
            angle += Math.PI * 2.0D * beamRandom.nextFloat();
            Vec3 spinner = axisX.scale(Math.cos(angle) * size).add(axisZ.scale(Math.sin(angle) * size));
            Vec3 point = start.add(axisY.scale(segmentLength * i)).add(spinner);
            if (previous != null) {
                renderLegacyBeamSegment(pose, consumer, previous, point, axisX, axisZ, layers, thickness);
            }
            previous = point;
        }
    }

    private static void renderLegacyBeamSegment(PoseStack.Pose pose, VertexConsumer consumer, Vec3 previous, Vec3 point, Vec3 axisX, Vec3 axisZ, int layers, double thickness) {
        double radiusStep = thickness / layers;
        for (int layer = 1; layer <= layers; layer++) {
            double interpolation = layers == 1 ? 1.0D : (layer - 1.0D) / (layers - 1.0D);
            int red = Mth.clamp((int) Math.round(64.0D + (0.0D - 64.0D) * interpolation), 0, 255);
            int green = Mth.clamp((int) Math.round(64.0D + (32.0D - 64.0D) * interpolation), 0, 255);
            int blue = 64;
            Vec3 x = axisX.scale(radiusStep * layer);
            Vec3 z = axisZ.scale(radiusStep * layer);
            zapQuad(consumer, pose, previous.add(x).add(z), previous.add(x).subtract(z), point.add(x).subtract(z), point.add(x).add(z), red, green, blue);
            zapQuad(consumer, pose, previous.subtract(x).add(z), previous.subtract(x).subtract(z), point.subtract(x).subtract(z), point.subtract(x).add(z), red, green, blue);
            zapQuad(consumer, pose, previous.add(x).add(z), previous.subtract(x).add(z), point.subtract(x).add(z), point.add(x).add(z), red, green, blue);
            zapQuad(consumer, pose, previous.add(x).subtract(z), previous.subtract(x).subtract(z), point.subtract(x).subtract(z), point.add(x).subtract(z), red, green, blue);
        }
    }

    private static void zapQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int red, int green, int blue) {
        lightningVertex(consumer, pose, a.x, a.y, a.z, red, green, blue, 255);
        lightningVertex(consumer, pose, b.x, b.y, b.z, red, green, blue, 255);
        lightningVertex(consumer, pose, c.x, c.y, c.z, red, green, blue, 255);
        lightningVertex(consumer, pose, d.x, d.y, d.z, red, green, blue, 255);
    }

    private static void lightningVertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, alpha)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static boolean isNearEnoughForExtraEffects(BatteryReddBlockEntity battery, double blocks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        return minecraft.player.distanceToSqr(
                battery.getBlockPos().getX() + 0.5D,
                battery.getBlockPos().getY() + 2.5D,
                battery.getBlockPos().getZ() + 0.5D
        ) < blocks * blocks;
    }
}
