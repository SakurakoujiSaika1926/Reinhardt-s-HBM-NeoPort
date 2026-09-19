package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.OilDerrickBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class OilDerrickBlockEntityRenderer implements LongRangeBlockEntityRenderer<OilDerrickBlockEntity> {
    static final ModelResourceLocation DERRICK_MODEL = MachineModelRenderer.standalone("block/machine_well_world");
    static final ModelResourceLocation PUMPJACK_BASE = MachineModelRenderer.standalone("block/machine_pumpjack_base");
    static final ModelResourceLocation PUMPJACK_ROTOR = MachineModelRenderer.standalone("block/machine_pumpjack_rotor");
    static final ModelResourceLocation PUMPJACK_HEAD = MachineModelRenderer.standalone("block/machine_pumpjack_head");
    static final ModelResourceLocation PUMPJACK_CARRIAGE = MachineModelRenderer.standalone("block/machine_pumpjack_carriage");
    private static final ResourceLocation ROD_TEXTURE = ReinhardtsHBM.id("textures/models/network/wire_greyscale.png");

    public OilDerrickBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(DERRICK_MODEL);
        event.register(PUMPJACK_BASE);
        event.register(PUMPJACK_ROTOR);
        event.register(PUMPJACK_HEAD);
        event.register(PUMPJACK_CARRIAGE);
    }

    @Override
    public void render(OilDerrickBlockEntity derrick, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = derrick.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        if (state.is(HbmBlocks.MACHINE_PUMPJACK.get())) {
            renderPumpjack(derrick, partialTick, poseStack, bufferSource, packedLight, packedOverlay, state, facing);
            return;
        }
        renderDerrick(poseStack, bufferSource, packedLight, packedOverlay, state, facing);
    }

    private void renderDerrick(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BlockState state, Direction facing) {
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyDerrickYaw(facing));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(DERRICK_MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderPumpjack(
            OilDerrickBlockEntity pumpjack,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BlockState state,
            Direction facing
    ) {
        float rotation = pumpjack.pumpjackRotation(partialTick);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        rotateY(poseStack, legacyPumpjackYaw(facing));

        renderPart(PUMPJACK_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.5F, -5.5F);
        rotateX(poseStack, rotation - 90.0F);
        poseStack.translate(0.0F, -1.5F, 5.5F);
        renderPart(PUMPJACK_ROTOR, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 3.5F, -3.5F);
        rotateX(poseStack, (float) (Math.toDegrees(Math.sin(Math.toRadians(rotation))) * 0.25D));
        poseStack.translate(0.0F, -3.5F, 3.5F);
        renderPart(PUMPJACK_HEAD, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, -Math.sin(Math.toRadians(rotation)), 0.0D);
        renderPart(PUMPJACK_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        renderRods(rotation, poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(OilDerrickBlockEntity blockEntity) {
        if (blockEntity.getBlockState().is(HbmBlocks.MACHINE_PUMPJACK.get())) {
            return new AABB(blockEntity.getBlockPos()).inflate(8.0D, 6.0D, 8.0D);
        }
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 10.0D, 3.0D);
    }

    private static void renderPart(
            ModelResourceLocation model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderRods(float rotation, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(ROD_TEXTURE));
        double radians = Math.toRadians(rotation);
        Vec3 backPos = rotateAroundX(new Vec3(0.0D, 0.0D, -2.0D), -Math.sin(radians) * 0.25D);
        Vec3 rot = rotateAroundX(new Vec3(0.0D, 0.5D, 0.0D), -Math.toRadians(rotation - 90.0D));

        for (int i = -1; i <= 1; i += 2) {
            double xi = 0.53125D * i;
            double y1 = 1.5D + rot.y;
            double z1 = -5.5D + rot.z;
            double y2 = 3.5D + backPos.y;
            double z2 = -3.5D + backPos.z;
            quad(
                    poseStack,
                    consumer,
                    new Vec3(xi, y1, z1 - 0.0625D),
                    new Vec3(xi, y1, z1 + 0.0625D),
                    new Vec3(xi, y2, z2 + 0.0625D),
                    new Vec3(xi, y2, z2 - 0.0625D),
                    128,
                    128,
                    128,
                    packedOverlay
            );
        }

        double pd = 0.03125D;
        double width = 0.25D;
        double height = -Math.sin(radians);
        for (int i = -1; i <= 1; i += 2) {
            double pRot = -Math.sin(radians) * 0.25D;
            Vec3 frontPos = rotateAroundX(new Vec3(0.0D, 0.0D, 1.0D), pRot);

            double dist = 0.03125D;
            Vec3 frontRad = rotateAroundX(new Vec3(0.0D, 0.0D, 2.5D + dist), pRot);
            double cutlet = 360.0D / 32.0D;
            frontRad = rotateAroundX(frontRad, -Math.toRadians(cutlet * -3.0D));

            for (int j = 0; j < 4; j++) {
                double sumY1 = frontPos.y + frontRad.y;
                double sumZ1 = frontPos.z + frontRad.z;
                if (frontRad.y < 0.0D) {
                    sumZ1 = 3.5D + dist * 0.5D;
                }

                frontRad = rotateAroundX(frontRad, -Math.toRadians(cutlet));

                double sumY2 = frontPos.y + frontRad.y;
                double sumZ2 = frontPos.z + frontRad.z;
                if (frontRad.y < 0.0D) {
                    sumZ2 = 3.5D + dist * 0.5D;
                }

                double xL = (width - pd) * i;
                double xR = (width + pd) * i;
                quad(
                        poseStack,
                        consumer,
                        new Vec3(xL, 3.5D + sumY1, -3.5D + sumZ1),
                        new Vec3(xR, 3.5D + sumY1, -3.5D + sumZ1),
                        new Vec3(xR, 3.5D + sumY2, -3.5D + sumZ2),
                        new Vec3(xL, 3.5D + sumY2, -3.5D + sumZ2),
                        51,
                        51,
                        51,
                        packedOverlay
                );
            }

            double sumY = frontPos.y + frontRad.y;
            double sumZ = frontPos.z + frontRad.z;
            if (frontRad.y < 0.0D) {
                sumZ = 3.5D + dist * 0.5D;
            }

            double xR = (width + pd) * i;
            double xL = (width - pd) * i;
            quad(
                    poseStack,
                    consumer,
                    new Vec3(xR, 3.5D + sumY, -3.5D + sumZ),
                    new Vec3(xL, 3.5D + sumY, -3.5D + sumZ),
                    new Vec3(xL, 2.0D + height, 0.0D),
                    new Vec3(xR, 2.0D + height, 0.0D),
                    51,
                    51,
                    51,
                    packedOverlay
            );
        }

        double p = 0.03125D;
        quad(
                poseStack,
                consumer,
                new Vec3(p, height + 1.5D, p),
                new Vec3(-p, height + 1.5D, -p),
                new Vec3(-p, 0.75D, -p),
                new Vec3(p, 0.75D, p),
                51,
                51,
                51,
                packedOverlay
        );
        quad(
                poseStack,
                consumer,
                new Vec3(-p, height + 1.5D, p),
                new Vec3(p, height + 1.5D, -p),
                new Vec3(p, 0.75D, -p),
                new Vec3(-p, 0.75D, p),
                51,
                51,
                51,
                packedOverlay
        );
    }

    private static void quad(PoseStack poseStack, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int r, int g, int bl, int packedOverlay) {
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, a, 0.0F, 0.0F, r, g, bl, packedOverlay);
        vertex(consumer, pose, b, 0.0F, 1.0F, r, g, bl, packedOverlay);
        vertex(consumer, pose, c, 1.0F, 1.0F, r, g, bl, packedOverlay);
        vertex(consumer, pose, d, 1.0F, 0.0F, r, g, bl, packedOverlay);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, float u, float v, int r, int g, int b, int packedOverlay) {
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static Vec3 rotateAroundX(Vec3 vec, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(vec.x, vec.y * cos + vec.z * sin, vec.z * cos - vec.y * sin);
    }

    private static float legacyDerrickYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            default -> 180.0F;
        };
    }

    private static float legacyPumpjackYaw(Direction facing) {
        return switch (facing) {
            case SOUTH -> 270.0F;
            case EAST -> 0.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }

    private static void rotateX(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 1.0F, 0.0F, 0.0F)));
    }

    private static void rotateY(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F)));
    }
}
