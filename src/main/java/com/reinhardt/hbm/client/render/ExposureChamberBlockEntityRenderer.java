package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.ExposureChamberBlockEntity;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.Random;

public class ExposureChamberBlockEntityRenderer implements BlockEntityRenderer<ExposureChamberBlockEntity> {
    private static final ModelResourceLocation CHAMBER = MachineModelRenderer.standalone("block/machine_exposure_chamber_chamber");
    private static final ModelResourceLocation MAGNETS = MachineModelRenderer.standalone("block/machine_exposure_chamber_magnets");
    private static final ModelResourceLocation CORE = MachineModelRenderer.standalone("block/machine_exposure_chamber_core");

    public ExposureChamberBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(CHAMBER);
        event.register(MAGNETS);
        event.register(CORE);
    }

    @Override
    public void render(ExposureChamberBlockEntity chamber, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = chamber.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        float rotation = chamber.rotation(partialTick);

        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, LegacyMachineGeometry.legacyWavefrontYaw(facing, 270.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CHAMBER), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotation), 0.0F, 1.0F, 0.0F)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MAGNETS), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        if (chamber.isOn()) {
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotation / 2.0F), 0.0F, 1.0F, 0.0F)));
            double bob = Math.sin(((chamber.getLevel() == null ? 0 : chamber.getLevel().getGameTime()) + partialTick) * 0.125D) * 0.0625D;
            poseStack.translate(0.0D, bob, 0.0D);
            MachineModelRenderer.renderUnculledFullBright(MachineModelRenderer.model(CORE), poseStack, bufferSource, state, packedOverlay);
            poseStack.popPose();
            renderBeams(chamber, poseStack, bufferSource);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ExposureChamberBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(9.0D, 5.0D, 9.0D);
    }
    private static void renderBeams(ExposureChamberBlockEntity chamber, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (!isNearEnoughForEffects(chamber, 96.0D)) {
            return;
        }
        long gameTime = chamber.getLevel() == null ? 0L : chamber.getLevel().getGameTime();
        int duration = 8;
        Random random = new Random(gameTime / duration);
        int color = gameTime % duration >= duration / 2 ? 0x80D0FF : 0xFFFFFF;
        int phase = (int) (System.currentTimeMillis() % 1000L) / 50;
        random.nextInt(2);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        if (random.nextInt(2) == 0) {
            renderRandomLineBeam(pose, consumer, new Vec3(0.0D, 3.675D, -7.5D), new Vec3(0.0D, 0.0D, 5.0D), color, 0xFFFFFF, phase, 15, 0.125D);
        }
        if (random.nextInt(2) == 0) {
            renderRandomLineBeam(pose, consumer, new Vec3(1.1875D, 2.5D, -7.5D), new Vec3(0.0D, 0.0D, 5.0D), color, 0xFFFFFF, phase, 15, 0.125D);
        }
        if (random.nextInt(2) == 0) {
            renderRandomLineBeam(pose, consumer, new Vec3(-1.1875D, 2.5D, -7.5D), new Vec3(0.0D, 0.0D, 5.0D), color, 0xFFFFFF, phase, 15, 0.125D);
        }

        renderRandomLineBeam(pose, consumer, new Vec3(0.0D, 1.75D, 0.0D), new Vec3(0.0D, 1.5D, 0.0D), 0x80D0FF, 0xFFFFFF, phase, 10, 0.125D);
        renderRandomLineBeam(pose, consumer, new Vec3(0.0D, 1.75D, 0.0D), new Vec3(0.0D, 1.5D, 0.0D), 0x8080FF, 0xFFFFFF, phase + 5, 10, 0.125D);
        renderSpiralLineBeam(pose, consumer, new Vec3(0.0D, 2.5D, 0.0D), new Vec3(0.0D, 0.0D, -1.0D), 0xFFFF80, 0xFFFFFF, (int) (System.currentTimeMillis() % 360L), 15, 0.125D);
        renderSpiralLineBeam(pose, consumer, new Vec3(0.0D, 2.5D, 0.0D), new Vec3(0.0D, 0.0D, -1.0D), 0xFF8080, 0xFFFFFF, (int) (System.currentTimeMillis() % 360L) + 180, 15, 0.125D);
    }

    private static void renderRandomLineBeam(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 skeleton, int outerColor, int innerColor, int phase, int segments, double size) {
        renderLineBeam(pose, consumer, start, skeleton, outerColor, innerColor, phase, segments, size, true);
    }

    private static void renderSpiralLineBeam(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 skeleton, int outerColor, int innerColor, int phase, int segments, double size) {
        renderLineBeam(pose, consumer, start, skeleton, outerColor, innerColor, phase, segments, size, false);
    }

    private static void renderLineBeam(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 skeleton, int outerColor, int innerColor, int phase, int segments, double size, boolean randomWave) {
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
        for (int index = 0; index <= segments; index++) {
            double angle;
            if (randomWave) {
                angle = Math.PI * 2.0D * beamRandom.nextFloat();
                angle += Math.PI * 2.0D * beamRandom.nextFloat();
            } else {
                angle = Math.toRadians(phase + 45.0D * index);
            }
            Vec3 spinner = axisX.scale(Math.cos(angle) * size).add(axisZ.scale(Math.sin(angle) * size));
            Vec3 point = start.add(axisY.scale(segmentLength * index)).add(spinner);
            if (previous != null) {
                renderBeamSegment(pose, consumer, previous, point, axisX, axisZ, 0.025D, outerColor);
            }
            previous = point;
        }
        renderBeamSegment(pose, consumer, start, start.add(skeleton), axisX, axisZ, 0.0125D, innerColor);
    }

    private static void renderBeamSegment(PoseStack.Pose pose, VertexConsumer consumer, Vec3 previous, Vec3 point, Vec3 axisX, Vec3 axisZ, double thickness, int color) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        Vec3 x = axisX.scale(thickness);
        Vec3 z = axisZ.scale(thickness);
        beamQuad(consumer, pose, previous.add(x).add(z), previous.add(x).subtract(z), point.add(x).subtract(z), point.add(x).add(z), red, green, blue);
        beamQuad(consumer, pose, previous.subtract(x).add(z), previous.subtract(x).subtract(z), point.subtract(x).subtract(z), point.subtract(x).add(z), red, green, blue);
        beamQuad(consumer, pose, previous.add(x).add(z), previous.subtract(x).add(z), point.subtract(x).add(z), point.add(x).add(z), red, green, blue);
        beamQuad(consumer, pose, previous.add(x).subtract(z), previous.subtract(x).subtract(z), point.subtract(x).subtract(z), point.add(x).subtract(z), red, green, blue);
    }

    private static void beamQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int red, int green, int blue) {
        beamVertex(consumer, pose, a, red, green, blue);
        beamVertex(consumer, pose, b, red, green, blue);
        beamVertex(consumer, pose, c, red, green, blue);
        beamVertex(consumer, pose, d, red, green, blue);
    }

    private static void beamVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, int red, int green, int blue) {
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(red, green, blue, 255)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static boolean isNearEnoughForEffects(ExposureChamberBlockEntity chamber, double blocks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        return minecraft.player.distanceToSqr(
                chamber.getBlockPos().getX() + 0.5D,
                chamber.getBlockPos().getY() + 1.5D,
                chamber.getBlockPos().getZ() + 0.5D
        ) < Mth.square(blocks);
    }
}
