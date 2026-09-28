package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.blockentity.DecorationEmitterBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import java.awt.Color;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

/** NeoForge vertex port of the 1.7.10 RenderEmitter / BeamPronter visual modes. */
public final class DecorationEmitterBlockEntityRenderer implements LongRangeBlockEntityRenderer<DecorationEmitterBlockEntity> {
    private static final long JAVA_RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long JAVA_RANDOM_ADDEND = 0xBL;
    private static final long JAVA_RANDOM_MASK = (1L << 48) - 1L;
    /** Effect-one CPU batches are weakly scoped to the emitter instance and its two-tick seed. */
    private final Map<DecorationEmitterBlockEntity, EffectOneBatch> effectOneGeometry = new WeakHashMap<>();

    public DecorationEmitterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DecorationEmitterBlockEntity emitter, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        DecorationEmitterBlockEntity.RenderSnapshot snapshot = emitter.renderSnapshot();
        if (!snapshot.hasFacing()) {
            return;
        }
        int length = snapshot.beam() - 1;
        if (length <= 0) {
            return;
        }

        Direction facing = snapshot.facing();
        long time = snapshot.gameTime();
        int color = snapshot.color() == 0
                ? Color.HSBtoRGB((time + partialTick) / 50.0F, 0.5F, 0.25F) & 0x00FFFFFF
                : snapshot.color();
        int inner = darken(color, 0.85F);
        int outer = darken(color, 0.1F);
        double directionX = facing.getStepX();
        double directionY = facing.getStepY();
        double directionZ = facing.getStepZ();
        double startX = 0.5D + directionX * 0.5D;
        double startY = 0.5D + directionY * 0.5D;
        double startZ = 0.5D + directionZ * 0.5D;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());

        renderSolidBeam(consumer, poseStack.last(), startX, startY, startZ,
                directionX, directionY, directionZ, length, snapshot.girth(), outer, inner);
        switch (snapshot.effect()) {
            case 1 -> {
                EffectOneBatch batch = effectOneBatch(emitter, snapshot,
                        startX, startY, startZ, directionX, directionY, directionZ,
                        length, (int) time / 2);
                replayEffectOne(consumer, poseStack.last(), batch, outer, inner);
            }
            case 2, 3 -> {
                int waves = snapshot.effect() == 2 ? 2 : 3;
                int segments = Math.max((int) (length / snapshot.girth() / 2.0F), 1);
                int startAngle = (int) ((time + partialTick) * -10.0F) % 360;
                for (int wave = 0; wave < waves; wave++) {
                    renderWave(consumer, poseStack.last(), startX, startY, startZ,
                            directionX, directionY, directionZ, length,
                            startAngle + wave * (360 / waves), segments,
                            snapshot.girth() * 2.0F, snapshot.girth() * 0.1F, outer, inner);
                }
            }
            case 4 -> renderPlasmaPulse(consumer, poseStack.last(),
                    startX, startY, startZ, directionX, directionY, directionZ,
                    snapshot.beam(), time, snapshot.girth() * 5.0F, color);
            default -> {
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(DecorationEmitterBlockEntity emitter) {
        return emitter.renderSnapshot().bounds();
    }

    private static void renderSolidBeam(VertexConsumer consumer, PoseStack.Pose pose,
                                        double startX, double startY, double startZ,
                                        double directionX, double directionY, double directionZ,
                                        int length, float girth, int outer, int inner) {
        double endX = startX + directionX * length;
        double endY = startY + directionY * length;
        double endZ = startZ + directionZ * length;
        renderTube(consumer, pose, startX, startY, startZ, endX, endY, endZ, girth, outer, 0x70);
        renderTube(consumer, pose, startX, startY, startZ, endX, endY, endZ,
                girth * 0.45D, inner, 0xE0);
    }

    private static void renderWave(VertexConsumer consumer, PoseStack.Pose pose,
                                   double startX, double startY, double startZ,
                                   double directionX, double directionY, double directionZ,
                                   int length, int seed, int segments, float size, float thickness,
                                   int outer, int inner) {
        double basisUX;
        double basisUY = 0.0D;
        double basisUZ;
        if (Math.abs(directionY) > 0.9D) {
            basisUX = 1.0D;
            basisUZ = 0.0D;
        } else {
            basisUX = -directionZ;
            basisUZ = directionX;
        }
        double basisVX = directionY * basisUZ - directionZ * basisUY;
        double basisVY = directionZ * basisUX - directionX * basisUZ;
        double basisVZ = directionX * basisUY - directionY * basisUX;
        double previousX = 0.0D;
        double previousY = 0.0D;
        double previousZ = 0.0D;
        for (int index = 0; index <= segments; index++) {
            double angle = Math.toRadians(seed + 45.0D * index);
            double radialU = Math.cos(angle) * size;
            double radialV = Math.sin(angle) * size;
            double distance = (double) length * index / segments;
            double currentX = startX + directionX * distance + basisUX * radialU + basisVX * radialV;
            double currentY = startY + directionY * distance + basisUY * radialU + basisVY * radialV;
            double currentZ = startZ + directionZ * distance + basisUZ * radialU + basisVZ * radialV;
            if (index > 0) {
                renderTube(consumer, pose,
                        previousX, previousY, previousZ, currentX, currentY, currentZ,
                        thickness, outer, 0x70);
                renderTube(consumer, pose,
                        previousX, previousY, previousZ, currentX, currentY, currentZ,
                        thickness * 0.45D, inner, 0xE0);
            }
            previousX = currentX;
            previousY = currentY;
            previousZ = currentZ;
        }
    }

    private synchronized EffectOneBatch effectOneBatch(
            DecorationEmitterBlockEntity emitter,
            DecorationEmitterBlockEntity.RenderSnapshot snapshot,
            double startX, double startY, double startZ,
            double directionX, double directionY, double directionZ,
            int length, int seed) {
        EffectOneBatch previous = this.effectOneGeometry.get(emitter);
        int girthBits = Float.floatToIntBits(snapshot.girth());
        if (previous != null
                && previous.facing() == snapshot.facing()
                && previous.length() == length
                && previous.girthBits() == girthBits
                && previous.seed() == seed) {
            return previous;
        }

        StyledVertexBuilder output = new StyledVertexBuilder(192);
        appendWave(output, startX, startY, startZ, directionX, directionY, directionZ, length, seed,
                Math.max((int) (length / snapshot.girth() / 2.0F), 1),
                snapshot.girth() * 2.0F, snapshot.girth() * 0.1F);
        appendWave(output, startX, startY, startZ, directionX, directionY, directionZ, length, seed + 15,
                Math.max((int) (length / snapshot.girth() / 4.0F), 1),
                snapshot.girth() * 2.0F, snapshot.girth() * 0.1F);
        EffectOneBatch next = new EffectOneBatch(
                snapshot.facing(), length, girthBits, seed,
                output.positions(), output.styles());
        this.effectOneGeometry.put(emitter, next);
        return next;
    }

    private static void appendWave(
            StyledVertexBuilder output,
            double startX, double startY, double startZ,
            double directionX, double directionY, double directionZ,
            int length, int seed, int segments, float size, float thickness) {
        double basisUX;
        double basisUY = 0.0D;
        double basisUZ;
        if (Math.abs(directionY) > 0.9D) {
            basisUX = 1.0D;
            basisUZ = 0.0D;
        } else {
            basisUX = -directionZ;
            basisUZ = directionX;
        }
        double basisVX = directionY * basisUZ - directionZ * basisUY;
        double basisVY = directionZ * basisUX - directionX * basisUZ;
        double basisVZ = directionX * basisUY - directionY * basisUX;
        long randomState = javaRandomInitialState(seed);
        double previousX = 0.0D;
        double previousY = 0.0D;
        double previousZ = 0.0D;
        for (int index = 0; index <= segments; index++) {
            randomState = javaRandomNextState(randomState);
            long high = randomState >>> 22;
            randomState = javaRandomNextState(randomState);
            double randomA = ((high << 27) + (randomState >>> 21)) * 0x1.0p-53;
            randomState = javaRandomNextState(randomState);
            high = randomState >>> 22;
            randomState = javaRandomNextState(randomState);
            double randomB = ((high << 27) + (randomState >>> 21)) * 0x1.0p-53;
            double angle = randomA * Math.PI * 2.0D + randomB * Math.PI * 2.0D;
            double radialU = Math.cos(angle) * size;
            double radialV = Math.sin(angle) * size;
            double distance = (double) length * index / segments;
            double currentX = startX + directionX * distance + basisUX * radialU + basisVX * radialV;
            double currentY = startY + directionY * distance + basisUY * radialU + basisVY * radialV;
            double currentZ = startZ + directionZ * distance + basisUZ * radialU + basisVZ * radialV;
            if (index > 0) {
                appendTube(output, previousX, previousY, previousZ,
                        currentX, currentY, currentZ, thickness, (byte) 0);
                appendTube(output, previousX, previousY, previousZ,
                        currentX, currentY, currentZ, thickness * 0.45D, (byte) 1);
            }
            previousX = currentX;
            previousY = currentY;
            previousZ = currentZ;
        }
    }

    private static void appendTube(
            StyledVertexBuilder output,
            double startX, double startY, double startZ,
            double endX, double endY, double endZ,
            double radius, byte style) {
        double axisX = endX - startX;
        double axisY = endY - startY;
        double axisZ = endZ - startZ;
        double axisLengthSqr = axisX * axisX + axisY * axisY + axisZ * axisZ;
        if (axisLengthSqr < 1.0E-8D) {
            return;
        }
        double inverseAxisLength = 1.0D / Math.sqrt(axisLengthSqr);
        double directionX = axisX * inverseAxisLength;
        double directionY = axisY * inverseAxisLength;
        double directionZ = axisZ * inverseAxisLength;
        double normalizedLength = Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
        directionX /= normalizedLength;
        directionY /= normalizedLength;
        directionZ /= normalizedLength;
        double uX;
        double uY = 0.0D;
        double uZ;
        if (Math.abs(directionY) > 0.9D) {
            uX = 1.0D;
            uZ = 0.0D;
        } else {
            uX = -directionZ;
            uZ = directionX;
            double inverseULength = 1.0D / Math.sqrt(uX * uX + uZ * uZ);
            uX *= inverseULength;
            uZ *= inverseULength;
        }
        double vX = directionY * uZ - directionZ * uY;
        double vY = directionZ * uX - directionX * uZ;
        double vZ = directionX * uY - directionY * uX;
        double inverseVLength = 1.0D / Math.sqrt(vX * vX + vY * vY + vZ * vZ);
        vX *= inverseVLength;
        vY *= inverseVLength;
        vZ *= inverseVLength;
        uX *= radius;
        uY *= radius;
        uZ *= radius;
        vX *= radius;
        vY *= radius;
        vZ *= radius;
        appendQuad(output, style,
                startX + uX + vX, startY + uY + vY, startZ + uZ + vZ,
                startX - uX + vX, startY - uY + vY, startZ - uZ + vZ,
                endX - uX + vX, endY - uY + vY, endZ - uZ + vZ,
                endX + uX + vX, endY + uY + vY, endZ + uZ + vZ);
        appendQuad(output, style,
                startX - uX - vX, startY - uY - vY, startZ - uZ - vZ,
                startX + uX - vX, startY + uY - vY, startZ + uZ - vZ,
                endX + uX - vX, endY + uY - vY, endZ + uZ - vZ,
                endX - uX - vX, endY - uY - vY, endZ - uZ - vZ);
        appendQuad(output, style,
                startX + uX - vX, startY + uY - vY, startZ + uZ - vZ,
                startX + uX + vX, startY + uY + vY, startZ + uZ + vZ,
                endX + uX + vX, endY + uY + vY, endZ + uZ + vZ,
                endX + uX - vX, endY + uY - vY, endZ + uZ - vZ);
        appendQuad(output, style,
                startX - uX + vX, startY - uY + vY, startZ - uZ + vZ,
                startX - uX - vX, startY - uY - vY, startZ - uZ - vZ,
                endX - uX - vX, endY - uY - vY, endZ - uZ - vZ,
                endX - uX + vX, endY - uY + vY, endZ - uZ + vZ);
    }

    private static void appendQuad(
            StyledVertexBuilder output, byte style,
            double firstX, double firstY, double firstZ,
            double secondX, double secondY, double secondZ,
            double thirdX, double thirdY, double thirdZ,
            double fourthX, double fourthY, double fourthZ) {
        output.add(firstX, firstY, firstZ, style);
        output.add(secondX, secondY, secondZ, style);
        output.add(thirdX, thirdY, thirdZ, style);
        output.add(fourthX, fourthY, fourthZ, style);
    }

    private static void replayEffectOne(
            VertexConsumer consumer, PoseStack.Pose pose, EffectOneBatch batch,
            int outer, int inner) {
        float[] positions = batch.positions();
        byte[] styles = batch.styles();
        for (int vertexIndex = 0, offset = 0; vertexIndex < styles.length; vertexIndex++, offset += 3) {
            boolean innerVertex = styles[vertexIndex] != 0;
            int color = innerVertex ? inner : outer;
            int alpha = innerVertex ? 0xE0 : 0x70;
            consumer.addVertex(pose, positions[offset], positions[offset + 1], positions[offset + 2])
                    .setColor((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, alpha)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
        }
    }

    private static void renderPlasmaPulse(VertexConsumer consumer, PoseStack.Pose pose,
                                          double startX, double startY, double startZ,
                                          double directionX, double directionY, double directionZ,
                                          int beam, long time, float scale, int color) {
        int distance = (int) ((time / 5L) % beam);
        double centerX = startX + directionX * distance;
        double centerY = startY + directionY * distance;
        double centerZ = startZ + directionZ * distance;
        renderTube(consumer, pose,
                centerX - directionX * 0.08D, centerY - directionY * 0.08D, centerZ - directionZ * 0.08D,
                centerX + directionX * 0.08D, centerY + directionY * 0.08D, centerZ + directionZ * 0.08D,
                scale, color, 0xD0);
    }

    private static void renderTube(VertexConsumer consumer, PoseStack.Pose pose,
                                   double startX, double startY, double startZ,
                                   double endX, double endY, double endZ,
                                   double radius, int rgb, int alpha) {
        double axisX = endX - startX;
        double axisY = endY - startY;
        double axisZ = endZ - startZ;
        double axisLengthSqr = axisX * axisX + axisY * axisY + axisZ * axisZ;
        if (axisLengthSqr < 1.0E-8D) {
            return;
        }
        double inverseAxisLength = 1.0D / Math.sqrt(axisLengthSqr);
        double directionX = axisX * inverseAxisLength;
        double directionY = axisY * inverseAxisLength;
        double directionZ = axisZ * inverseAxisLength;
        double normalizedLength = Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
        directionX /= normalizedLength;
        directionY /= normalizedLength;
        directionZ /= normalizedLength;
        double uX;
        double uY = 0.0D;
        double uZ;
        if (Math.abs(directionY) > 0.9D) {
            uX = 1.0D;
            uZ = 0.0D;
        } else {
            uX = -directionZ;
            uZ = directionX;
            double inverseULength = 1.0D / Math.sqrt(uX * uX + uZ * uZ);
            uX *= inverseULength;
            uZ *= inverseULength;
        }
        double vX = directionY * uZ - directionZ * uY;
        double vY = directionZ * uX - directionX * uZ;
        double vZ = directionX * uY - directionY * uX;
        double inverseVLength = 1.0D / Math.sqrt(vX * vX + vY * vY + vZ * vZ);
        vX *= inverseVLength;
        vY *= inverseVLength;
        vZ *= inverseVLength;
        uX *= radius;
        uY *= radius;
        uZ *= radius;
        vX *= radius;
        vY *= radius;
        vZ *= radius;
        quad(consumer, pose,
                startX + uX + vX, startY + uY + vY, startZ + uZ + vZ,
                startX - uX + vX, startY - uY + vY, startZ - uZ + vZ,
                endX - uX + vX, endY - uY + vY, endZ - uZ + vZ,
                endX + uX + vX, endY + uY + vY, endZ + uZ + vZ,
                rgb, alpha);
        quad(consumer, pose,
                startX - uX - vX, startY - uY - vY, startZ - uZ - vZ,
                startX + uX - vX, startY + uY - vY, startZ + uZ - vZ,
                endX + uX - vX, endY + uY - vY, endZ + uZ - vZ,
                endX - uX - vX, endY - uY - vY, endZ - uZ - vZ,
                rgb, alpha);
        quad(consumer, pose,
                startX + uX - vX, startY + uY - vY, startZ + uZ - vZ,
                startX + uX + vX, startY + uY + vY, startZ + uZ + vZ,
                endX + uX + vX, endY + uY + vY, endZ + uZ + vZ,
                endX + uX - vX, endY + uY - vY, endZ + uZ - vZ,
                rgb, alpha);
        quad(consumer, pose,
                startX - uX + vX, startY - uY + vY, startZ - uZ + vZ,
                startX - uX - vX, startY - uY - vY, startZ - uZ - vZ,
                endX - uX - vX, endY - uY - vY, endZ - uZ - vZ,
                endX - uX + vX, endY - uY + vY, endZ - uZ + vZ,
                rgb, alpha);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             double firstX, double firstY, double firstZ,
                             double secondX, double secondY, double secondZ,
                             double thirdX, double thirdY, double thirdZ,
                             double fourthX, double fourthY, double fourthZ,
                             int rgb, int alpha) {
        vertex(consumer, pose, firstX, firstY, firstZ, rgb, alpha);
        vertex(consumer, pose, secondX, secondY, secondZ, rgb, alpha);
        vertex(consumer, pose, thirdX, thirdY, thirdZ, rgb, alpha);
        vertex(consumer, pose, fourthX, fourthY, fourthZ, rgb, alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               double x, double y, double z, int rgb, int alpha) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
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

    private static long javaRandomInitialState(long seed) {
        return (seed ^ JAVA_RANDOM_MULTIPLIER) & JAVA_RANDOM_MASK;
    }

    private static long javaRandomNextState(long state) {
        return (state * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
    }

    private record EffectOneBatch(Direction facing, int length, int girthBits, int seed,
                                  float[] positions, byte[] styles) {
    }

    private static final class StyledVertexBuilder {
        private float[] positions;
        private byte[] styles;
        private int vertexCount;

        private StyledVertexBuilder(int expectedVertices) {
            int capacity = Math.max(1, expectedVertices);
            this.positions = new float[capacity * 3];
            this.styles = new byte[capacity];
        }

        private void add(double x, double y, double z, byte style) {
            ensureCapacity(this.vertexCount + 1);
            int offset = this.vertexCount * 3;
            this.positions[offset] = (float) x;
            this.positions[offset + 1] = (float) y;
            this.positions[offset + 2] = (float) z;
            this.styles[this.vertexCount] = style;
            this.vertexCount++;
        }

        private void ensureCapacity(int requiredVertices) {
            if (requiredVertices > this.styles.length) {
                int capacity = Math.max(requiredVertices, this.styles.length * 2);
                this.positions = Arrays.copyOf(this.positions, capacity * 3);
                this.styles = Arrays.copyOf(this.styles, capacity);
            }
        }

        private float[] positions() {
            return Arrays.copyOf(this.positions, this.vertexCount * 3);
        }

        private byte[] styles() {
            return Arrays.copyOf(this.styles, this.vertexCount);
        }
    }
}
