package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.DecorationEmitterBlock;
import com.reinhardt.hbm.blockentity.DecorationEmitterBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.Random;

/** NeoForge vertex port of the 1.7.10 RenderEmitter / BeamPronter visual modes. */
public final class DecorationEmitterBlockEntityRenderer implements BlockEntityRenderer<DecorationEmitterBlockEntity> {
    public DecorationEmitterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DecorationEmitterBlockEntity emitter, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (emitter.getLevel() == null || !emitter.getBlockState().hasProperty(DecorationEmitterBlock.FACING)) {
            return;
        }
        int length = emitter.beam() - 1;
        if (length <= 0) {
            return;
        }

        Direction facing = emitter.getBlockState().getValue(DecorationEmitterBlock.FACING);
        long time = emitter.getLevel().getGameTime();
        int color = emitter.color() == 0
                ? Color.HSBtoRGB((time + partialTick) / 50.0F, 0.5F, 0.25F) & 0x00FFFFFF
                : emitter.color();
        int inner = darken(color, 0.85F);
        int outer = darken(color, 0.1F);
        Vec3 start = new Vec3(0.5D, 0.5D, 0.5D).add(
                facing.getStepX() * 0.5D,
                facing.getStepY() * 0.5D,
                facing.getStepZ() * 0.5D
        );
        Vec3 direction = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        renderSolidBeam(consumer, poseStack.last(), start, direction, length, emitter.girth(), outer, inner);
        switch (emitter.effect()) {
            case 1 -> {
                renderWave(consumer, poseStack.last(), start, direction, length, Wave.RANDOM,
                        (int) time / 2, Math.max((int) (length / emitter.girth() / 2.0F), 1),
                        emitter.girth() * 2.0F, emitter.girth() * 0.1F, outer, inner);
                renderWave(consumer, poseStack.last(), start, direction, length, Wave.RANDOM,
                        (int) time / 2 + 15, Math.max((int) (length / emitter.girth() / 4.0F), 1),
                        emitter.girth() * 2.0F, emitter.girth() * 0.1F, outer, inner);
            }
            case 2, 3 -> {
                int waves = emitter.effect() == 2 ? 2 : 3;
                int segments = Math.max((int) (length / emitter.girth() / 2.0F), 1);
                int startAngle = (int) ((time + partialTick) * -10.0F) % 360;
                for (int wave = 0; wave < waves; wave++) {
                    renderWave(consumer, poseStack.last(), start, direction, length, Wave.SPIRAL,
                            startAngle + wave * (360 / waves), segments,
                            emitter.girth() * 2.0F, emitter.girth() * 0.1F, outer, inner);
                }
            }
            case 4 -> renderPlasmaPulse(consumer, poseStack.last(), start, direction, emitter.beam(), time,
                    emitter.girth() * 5.0F, color);
            default -> {
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(DecorationEmitterBlockEntity emitter) {
        return new AABB(emitter.getBlockPos()).inflate(102.0D);
    }

    private static void renderSolidBeam(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 direction,
                                        int length, float girth, int outer, int inner) {
        Vec3 end = start.add(direction.scale(length));
        renderTube(consumer, pose, start, end, girth, outer, 0x70);
        renderTube(consumer, pose, start, end, girth * 0.45D, inner, 0xE0);
    }

    private static void renderWave(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 direction,
                                   int length, Wave wave, int seed, int segments, float size, float thickness,
                                   int outer, int inner) {
        Basis basis = Basis.of(direction);
        Random random = new Random(seed);
        Vec3 previous = null;
        for (int index = 0; index <= segments; index++) {
            double angle = wave == Wave.SPIRAL
                    ? Math.toRadians(seed + 45.0D * index)
                    : random.nextDouble() * Math.PI * 2.0D + random.nextDouble() * Math.PI * 2.0D;
            Vec3 offset = basis.u.scale(Math.cos(angle) * size).add(basis.v.scale(Math.sin(angle) * size));
            Vec3 current = start.add(direction.scale((double) length * index / segments)).add(offset);
            if (previous != null) {
                renderTube(consumer, pose, previous, current, thickness, outer, 0x70);
                renderTube(consumer, pose, previous, current, thickness * 0.45D, inner, 0xE0);
            }
            previous = current;
        }
    }

    private static void renderPlasmaPulse(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 direction,
                                          int beam, long time, float scale, int color) {
        int distance = (int) ((time / 5L) % beam);
        Vec3 center = start.add(direction.scale(distance));
        renderTube(consumer, pose, center.subtract(direction.scale(0.08D)), center.add(direction.scale(0.08D)),
                scale, color, 0xD0);
    }

    private static void renderTube(VertexConsumer consumer, PoseStack.Pose pose, Vec3 start, Vec3 end,
                                   double radius, int rgb, int alpha) {
        Vec3 axis = end.subtract(start);
        if (axis.lengthSqr() < 1.0E-8D) {
            return;
        }
        Basis basis = Basis.of(axis.normalize());
        Vec3 u = basis.u.scale(radius);
        Vec3 v = basis.v.scale(radius);
        quad(consumer, pose, start.add(u).add(v), start.subtract(u).add(v), end.subtract(u).add(v), end.add(u).add(v), rgb, alpha);
        quad(consumer, pose, start.subtract(u).subtract(v), start.add(u).subtract(v), end.add(u).subtract(v), end.subtract(u).subtract(v), rgb, alpha);
        quad(consumer, pose, start.add(u).subtract(v), start.add(u).add(v), end.add(u).add(v), end.add(u).subtract(v), rgb, alpha);
        quad(consumer, pose, start.subtract(u).add(v), start.subtract(u).subtract(v), end.subtract(u).subtract(v), end.subtract(u).add(v), rgb, alpha);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 first, Vec3 second, Vec3 third,
                             Vec3 fourth, int rgb, int alpha) {
        vertex(consumer, pose, first, rgb, alpha);
        vertex(consumer, pose, second, rgb, alpha);
        vertex(consumer, pose, third, rgb, alpha);
        vertex(consumer, pose, fourth, rgb, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 position, int rgb, int alpha) {
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor((rgb >>> 16) & 0xFF, (rgb >>> 8) & 0xFF, rgb & 0xFF, alpha)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static int darken(int color, float multiplier) {
        int red = (int) (((color >>> 16) & 0xFF) * multiplier);
        int green = (int) (((color >>> 8) & 0xFF) * multiplier);
        int blue = (int) ((color & 0xFF) * multiplier);
        return (red << 16) | (green << 8) | blue;
    }

    private enum Wave {
        RANDOM,
        SPIRAL
    }

    private record Basis(Vec3 u, Vec3 v) {
        private static Basis of(Vec3 direction) {
            Vec3 unit = direction.normalize();
            Vec3 u = Math.abs(unit.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D)
                    : new Vec3(-unit.z, 0.0D, unit.x).normalize();
            return new Basis(u, unit.cross(u).normalize());
        }
    }
}
