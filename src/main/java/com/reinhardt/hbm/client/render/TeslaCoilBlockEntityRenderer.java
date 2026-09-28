package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.blockentity.TeslaCoilBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Direct modern rendering route for 1.7.10 RenderTesla. */
public final class TeslaCoilBlockEntityRenderer implements LongRangeBlockEntityRenderer<TeslaCoilBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/tesla");
    private static final long JAVA_RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long JAVA_RANDOM_ADDEND = 0xBL;
    private static final long JAVA_RANDOM_MASK = (1L << 48) - 1L;
    /** CPU-only arc batches are weakly owned by the exact block entity that produced them. */
    private final Map<TeslaCoilBlockEntity, CachedArcGeometry> arcGeometry = new WeakHashMap<>();

    public TeslaCoilBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(TeslaCoilBlockEntity tesla, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        TeslaCoilBlockEntity.RenderSnapshot snapshot = tesla.renderSnapshot();
        BlockState state = snapshot.blockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();

        List<Vec3> targets = snapshot.targets();
        if (targets.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5D, TeslaCoilBlockEntity.EMITTER_OFFSET, 0.5D);
        int phase = (int) (snapshot.gameTime() % 1000L) + 1;
        CachedArcGeometry geometry = geometryFor(tesla, snapshot, phase);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        float[] vertices = geometry.vertices();
        for (int offset = 0; offset < vertices.length; offset += 3) {
            consumer.addVertex(pose, vertices[offset], vertices[offset + 1], vertices[offset + 2])
                    .setColor(0x40, 0x40, 0x40, 0xFF)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(pose, 0.0F, 1.0F, 0.0F);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TeslaCoilBlockEntity tesla) {
        return tesla.renderSnapshot().bounds();
    }

    private static void appendLegacyRandomBeam(FloatVertexBuilder output, Vec3 end,
                                               int phase, int segments) {
        double length = Math.sqrt(end.x * end.x + end.y * end.y + end.z * end.z);
        double axisX = end.x / length;
        double axisY = end.y / length;
        double axisZ = end.z / length;
        double sideX = axisZ;
        double sideY = 0.0D;
        double sideZ = -axisX;
        double sideLengthSqr = sideX * sideX + sideZ * sideZ;
        if (sideLengthSqr < 1.0E-5D) {
            sideX = 1.0D;
            sideZ = 0.0D;
        } else {
            double inverseSideLength = 1.0D / Math.sqrt(sideLengthSqr);
            sideX *= inverseSideLength;
            sideZ *= inverseSideLength;
        }
        double verticalX = sideY * axisZ - sideZ * axisY;
        double verticalY = sideZ * axisX - sideX * axisZ;
        double verticalZ = sideX * axisY - sideY * axisX;
        double inverseVerticalLength = 1.0D / Math.sqrt(
                verticalX * verticalX + verticalY * verticalY + verticalZ * verticalZ);
        verticalX *= inverseVerticalLength;
        verticalY *= inverseVerticalLength;
        verticalZ *= inverseVerticalLength;
        long randomState = (phase ^ JAVA_RANDOM_MULTIPLIER) & JAVA_RANDOM_MASK;
        double previousX = 0.0D;
        double previousY = 0.0D;
        double previousZ = 0.0D;
        int count = Math.max(1, segments);
        for (int index = 1; index <= count; index++) {
            double progress = index / (double) count;
            double pointX = end.x * progress;
            double pointY = end.y * progress;
            double pointZ = end.z * progress;
            if (index < count) {
                randomState = (randomState * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
                long high = randomState >>> 22;
                randomState = (randomState * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
                double sideOffset = (((high << 27) + (randomState >>> 21)) * 0x1.0p-53 - 0.5D) * 0.25D;
                randomState = (randomState * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
                high = randomState >>> 22;
                randomState = (randomState * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
                double verticalOffset = (((high << 27) + (randomState >>> 21)) * 0x1.0p-53 - 0.5D) * 0.25D;
                pointX += sideX * sideOffset + verticalX * verticalOffset;
                pointY += sideY * sideOffset + verticalY * verticalOffset;
                pointZ += sideZ * sideOffset + verticalZ * verticalOffset;
            }
            appendBeamQuad(output,
                    previousX, previousY, previousZ,
                    pointX, pointY, pointZ,
                    sideX * 0.03125D, sideY * 0.03125D, sideZ * 0.03125D,
                    verticalX * 0.03125D, verticalY * 0.03125D, verticalZ * 0.03125D);
            previousX = pointX;
            previousY = pointY;
            previousZ = pointZ;
        }
    }

    private static void appendBeamQuad(FloatVertexBuilder output,
                                       double startX, double startY, double startZ,
                                       double endX, double endY, double endZ,
                                       double sideX, double sideY, double sideZ,
                                       double verticalX, double verticalY, double verticalZ) {
        output.add(startX + sideX + verticalX, startY + sideY + verticalY, startZ + sideZ + verticalZ);
        output.add(startX - sideX + verticalX, startY - sideY + verticalY, startZ - sideZ + verticalZ);
        output.add(endX - sideX - verticalX, endY - sideY - verticalY, endZ - sideZ - verticalZ);
        output.add(endX + sideX - verticalX, endY + sideY - verticalY, endZ + sideZ - verticalZ);
        output.add(startX + verticalX, startY + verticalY, startZ + verticalZ);
        output.add(startX - verticalX, startY - verticalY, startZ - verticalZ);
        output.add(endX - verticalX, endY - verticalY, endZ - verticalZ);
        output.add(endX + verticalX, endY + verticalY, endZ + verticalZ);
    }

    private synchronized CachedArcGeometry geometryFor(
            TeslaCoilBlockEntity tesla, TeslaCoilBlockEntity.RenderSnapshot snapshot, int phase) {
        CachedArcGeometry previous = this.arcGeometry.get(tesla);
        if (previous != null
                && previous.phase() == phase
                && previous.emitter().equals(snapshot.emitter())
                && previous.targets().equals(snapshot.targets())) {
            return previous;
        }

        FloatVertexBuilder output = new FloatVertexBuilder(192);
        for (Vec3 target : snapshot.targets()) {
            Vec3 vector = target.subtract(snapshot.emitter());
            if (vector.lengthSqr() > 1.0E-5D) {
                appendLegacyRandomBeam(output, vector, phase,
                        (int) Math.ceil(vector.length() * 5.0D));
            }
        }
        CachedArcGeometry next = new CachedArcGeometry(
                snapshot.emitter(), List.copyOf(snapshot.targets()), phase, output.toArray());
        this.arcGeometry.put(tesla, next);
        return next;
    }

    private record CachedArcGeometry(Vec3 emitter, List<Vec3> targets, int phase, float[] vertices) {
    }

    private static final class FloatVertexBuilder {
        private float[] values;
        private int size;

        private FloatVertexBuilder(int expectedFloats) {
            this.values = new float[Math.max(3, expectedFloats)];
        }

        private void add(double x, double y, double z) {
            ensureCapacity(this.size + 3);
            this.values[this.size++] = (float) x;
            this.values[this.size++] = (float) y;
            this.values[this.size++] = (float) z;
        }

        private void ensureCapacity(int required) {
            if (required > this.values.length) {
                this.values = Arrays.copyOf(this.values, Math.max(required, this.values.length * 2));
            }
        }

        private float[] toArray() {
            return Arrays.copyOf(this.values, this.size);
        }
    }
}
