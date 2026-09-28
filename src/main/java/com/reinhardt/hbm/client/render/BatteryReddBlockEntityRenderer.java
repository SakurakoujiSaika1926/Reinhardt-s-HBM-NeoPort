package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.block.BatteryReddBlock;
import com.reinhardt.hbm.blockentity.BatteryReddBlockEntity;
import com.reinhardt.hbm.client.sound.BatteryReddClientSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Quaternionf;

public class BatteryReddBlockEntityRenderer implements LongRangeBlockEntityRenderer<BatteryReddBlockEntity> {
    private static final long JAVA_RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long JAVA_RANDOM_ADDEND = 0xBL;
    private static final long JAVA_RANDOM_MASK = (1L << 48) - 1L;
    /*
     * The rotating OBJ union is dominated by fensu2_wheel's face-used baked
     * vertex (y=4.622091F,z=4.413535F), whose radius about y=5.5 is the value
     * below.  gamma(32) bounds the float quaternion/matrix/translation path;
     * nextUp converts that analytic continuous sweep to an outward float.
     */
    private static final float WHEEL_SWEEP_EXTENT = wheelSweepExtent();
    private static final AABB[] LOCAL_BOUNDS = buildLocalBounds();
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
        Direction facing = batteryReddFacing(state);
        poseStack.pushPose();
        applyBatteryReddTransform(poseStack, facing);
        MachineModelRenderer.renderUnculledCutoutNoCull(MachineModelRenderer.model(BASE), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0F, 5.5F, 0.0F);
        float rotation = Mth.lerp(partialTick, battery.prevRotation, battery.rotation);
        poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(rotation)));
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
        BlockState state = blockEntity.getBlockState();
        Direction facing = batteryReddFacing(state);
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildLocalBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {
                Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        }) {
            /*
             * Base faces supply y=0..10.25.  Exhaustively evaluating the
             * four legacy zap skeletons for all 20 legal phase seeds gives
             * x=-3.4346895F..3.4346895F and z inside +/-3.9587097F.  The
             * continuous wheel sweep therefore supplies the final Z extent.
             */
            PoseStack poseStack = new PoseStack();
            applyBatteryReddTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    BlockPos.ZERO, poseStack,
                    -3.4346895F, 0.0F, -WHEEL_SWEEP_EXTENT,
                    3.4346895F, 10.25F, WHEEL_SWEEP_EXTENT);
        }
        return bounds;
    }

    private static float wheelSweepExtent() {
        double radius = Math.nextUp(4.500001863689191D);
        double unitRoundoff = 0x1.0p-24;
        double gamma32 = 32.0D * unitRoundoff / (1.0D - 32.0D * unitRoundoff);
        double matrixError = (5.5D + radius) * gamma32;
        return Math.nextUp((float) (radius + matrixError));
    }

    private static void applyBatteryReddTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            // RenderBatteryREDD metadata 2/3/4/5: 270/90/0/180 degrees.
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case WEST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("REDD battery facing must be horizontal: " + facing);
        }
    }

    private static Direction batteryReddFacing(BlockState state) {
        if (!state.hasProperty(BatteryReddBlock.FACING)) {
            throw new IllegalStateException("REDD battery state is missing its owned facing property: " + state);
        }
        return state.getValue(BatteryReddBlock.FACING);
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
        double fromRadians = Math.toRadians(fromDegrees);
        double fromY = Math.cos(fromRadians);
        double fromZ = Math.sin(fromRadians);
        double toRadians = Math.toRadians(toDegrees);
        double toY = Math.cos(toRadians);
        double toZ = Math.sin(toRadians);
        lightningVertex(consumer, pose, x, fromY * len - fromY * width, fromZ * len - fromZ * width, 255, 255, 0, fromAlpha);
        lightningVertex(consumer, pose, x, fromY * len + fromY * width, fromZ * len + fromZ * width, 255, 255, 0, fromAlpha);
        lightningVertex(consumer, pose, x, toY * len + toY * width, toZ * len + toZ * width, 255, 255, 0, toAlpha);
        lightningVertex(consumer, pose, x, toY * len - toY * width, toZ * len - toZ * width, 255, 255, 0, toAlpha);
    }

    private static void renderZaps(BatteryReddBlockEntity battery, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (!isNearEnoughForExtraEffects(battery, 100.0D)) {
            return;
        }
        Level level = battery.getLevel();
        long time = level == null ? System.currentTimeMillis() / 50L : level.getGameTime();
        long randomState = javaRandomInitialState(time / 5L);
        randomState = javaRandomNextState(randomState); // Legacy code discarded its first nextBoolean().
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        int phase = (int) (System.currentTimeMillis() % 1000L) / 50;
        randomState = javaRandomNextState(randomState);
        if (javaRandomBoolean(randomState)) {
            renderLegacyZap(poseStack.last(), consumer, 3.125D, 5.5D, 0.0D, -1.375D, -2.625D, 3.75D, phase);
        }
        randomState = javaRandomNextState(randomState);
        if (javaRandomBoolean(randomState)) {
            renderLegacyZap(poseStack.last(), consumer, -3.125D, 5.5D, 0.0D, 1.375D, -2.625D, 3.75D, phase);
        }
        randomState = javaRandomNextState(randomState);
        if (javaRandomBoolean(randomState)) {
            renderLegacyZap(poseStack.last(), consumer, 3.125D, 5.5D, 0.0D, -1.375D, -2.625D, -3.75D, phase);
        }
        randomState = javaRandomNextState(randomState);
        if (javaRandomBoolean(randomState)) {
            renderLegacyZap(poseStack.last(), consumer, -3.125D, 5.5D, 0.0D, 1.375D, -2.625D, -3.75D, phase);
        }
    }

    private static long javaRandomInitialState(long seed) {
        return (seed ^ JAVA_RANDOM_MULTIPLIER) & JAVA_RANDOM_MASK;
    }

    private static long javaRandomNextState(long state) {
        return (state * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
    }

    private static boolean javaRandomBoolean(long state) {
        return (state >>> 47) != 0L;
    }

    private static float javaRandomFloat(long state) {
        return (float) (state >>> 24) / 16777216.0F;
    }

    private static void renderLegacyZap(PoseStack.Pose pose, VertexConsumer consumer,
                                        double startX, double startY, double startZ,
                                        double skeletonX, double skeletonY, double skeletonZ,
                                        int phase) {
        renderLegacyBeam(pose, consumer, startX, startY, startZ, skeletonX, skeletonY, skeletonZ, phase, 15, 0.25D, 3, 0.0625D);
        renderLegacyBeam(pose, consumer, startX, startY, startZ, skeletonX, skeletonY, skeletonZ, phase, 1, 0.0D, 3, 0.0625D);
    }

    private static void renderLegacyBeam(PoseStack.Pose pose, VertexConsumer consumer,
                                         double startX, double startY, double startZ,
                                         double skeletonX, double skeletonY, double skeletonZ,
                                         int phase, int segments, double size, int layers, double thickness) {
        double length = Math.sqrt(skeletonX * skeletonX + skeletonY * skeletonY + skeletonZ * skeletonZ);
        double axisYX = skeletonX / length;
        double axisYY = skeletonY / length;
        double axisYZ = skeletonZ / length;

        double axisXX = axisYZ;
        double axisXY = 0.0D;
        double axisXZ = -axisYX;
        double axisXLengthSqr = axisXX * axisXX + axisXZ * axisXZ;
        if (axisXLengthSqr < 1.0E-5D) {
            axisXX = 1.0D;
            axisXZ = 0.0D;
        } else {
            double inverseAxisXLength = 1.0D / Math.sqrt(axisXLengthSqr);
            axisXX *= inverseAxisXLength;
            axisXZ *= inverseAxisXLength;
        }

        double axisZX = axisXY * axisYZ - axisXZ * axisYY;
        double axisZY = axisXZ * axisYX - axisXX * axisYZ;
        double axisZZ = axisXX * axisYY - axisXY * axisYX;
        double inverseAxisZLength = 1.0D / Math.sqrt(axisZX * axisZX + axisZY * axisZY + axisZZ * axisZZ);
        axisZX *= inverseAxisZLength;
        axisZY *= inverseAxisZLength;
        axisZZ *= inverseAxisZLength;

        long randomState = javaRandomInitialState(phase);
        double segmentLength = length / segments;
        double previousX = 0.0D;
        double previousY = 0.0D;
        double previousZ = 0.0D;
        for (int i = 0; i <= segments; i++) {
            randomState = javaRandomNextState(randomState);
            double angle = Math.PI * 2.0D * javaRandomFloat(randomState);
            randomState = javaRandomNextState(randomState);
            angle += Math.PI * 2.0D * javaRandomFloat(randomState);
            double radialX = Math.cos(angle) * size;
            double radialZ = Math.sin(angle) * size;
            double spinnerX = axisXX * radialX + axisZX * radialZ;
            double spinnerY = axisXY * radialX + axisZY * radialZ;
            double spinnerZ = axisXZ * radialX + axisZZ * radialZ;
            double distance = segmentLength * i;
            double pointX = startX + axisYX * distance + spinnerX;
            double pointY = startY + axisYY * distance + spinnerY;
            double pointZ = startZ + axisYZ * distance + spinnerZ;
            if (i > 0) {
                renderLegacyBeamSegment(pose, consumer,
                        previousX, previousY, previousZ,
                        pointX, pointY, pointZ,
                        axisXX, axisXY, axisXZ,
                        axisZX, axisZY, axisZZ,
                        layers, thickness);
            }
            previousX = pointX;
            previousY = pointY;
            previousZ = pointZ;
        }
    }

    private static void renderLegacyBeamSegment(PoseStack.Pose pose, VertexConsumer consumer,
                                                double previousX, double previousY, double previousZ,
                                                double pointX, double pointY, double pointZ,
                                                double axisXX, double axisXY, double axisXZ,
                                                double axisZX, double axisZY, double axisZZ,
                                                int layers, double thickness) {
        double radiusStep = thickness / layers;
        for (int layer = 1; layer <= layers; layer++) {
            double interpolation = layers == 1 ? 1.0D : (layer - 1.0D) / (layers - 1.0D);
            int red = Mth.clamp((int) Math.round(64.0D + (0.0D - 64.0D) * interpolation), 0, 255);
            int green = Mth.clamp((int) Math.round(64.0D + (32.0D - 64.0D) * interpolation), 0, 255);
            int blue = 64;
            double radius = radiusStep * layer;
            double xX = axisXX * radius;
            double xY = axisXY * radius;
            double xZ = axisXZ * radius;
            double zX = axisZX * radius;
            double zY = axisZY * radius;
            double zZ = axisZZ * radius;
            zapQuad(consumer, pose,
                    previousX + xX + zX, previousY + xY + zY, previousZ + xZ + zZ,
                    previousX + xX - zX, previousY + xY - zY, previousZ + xZ - zZ,
                    pointX + xX - zX, pointY + xY - zY, pointZ + xZ - zZ,
                    pointX + xX + zX, pointY + xY + zY, pointZ + xZ + zZ,
                    red, green, blue);
            zapQuad(consumer, pose,
                    previousX - xX + zX, previousY - xY + zY, previousZ - xZ + zZ,
                    previousX - xX - zX, previousY - xY - zY, previousZ - xZ - zZ,
                    pointX - xX - zX, pointY - xY - zY, pointZ - xZ - zZ,
                    pointX - xX + zX, pointY - xY + zY, pointZ - xZ + zZ,
                    red, green, blue);
            zapQuad(consumer, pose,
                    previousX + xX + zX, previousY + xY + zY, previousZ + xZ + zZ,
                    previousX - xX + zX, previousY - xY + zY, previousZ - xZ + zZ,
                    pointX - xX + zX, pointY - xY + zY, pointZ - xZ + zZ,
                    pointX + xX + zX, pointY + xY + zY, pointZ + xZ + zZ,
                    red, green, blue);
            zapQuad(consumer, pose,
                    previousX + xX - zX, previousY + xY - zY, previousZ + xZ - zZ,
                    previousX - xX - zX, previousY - xY - zY, previousZ - xZ - zZ,
                    pointX - xX - zX, pointY - xY - zY, pointZ - xZ - zZ,
                    pointX + xX - zX, pointY + xY - zY, pointZ + xZ - zZ,
                    red, green, blue);
        }
    }

    private static void zapQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                double ax, double ay, double az,
                                double bx, double by, double bz,
                                double cx, double cy, double cz,
                                double dx, double dy, double dz,
                                int red, int green, int blue) {
        lightningVertex(consumer, pose, ax, ay, az, red, green, blue, 255);
        lightningVertex(consumer, pose, bx, by, bz, red, green, blue, 255);
        lightningVertex(consumer, pose, cx, cy, cz, red, green, blue, 255);
        lightningVertex(consumer, pose, dx, dy, dz, red, green, blue, 255);
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
