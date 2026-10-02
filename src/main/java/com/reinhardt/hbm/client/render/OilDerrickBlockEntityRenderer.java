package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.OilDerrickBlock;
import com.reinhardt.hbm.block.OilPumpjackBlock;
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
import net.neoforged.neoforge.client.event.ModelEvent;

public class OilDerrickBlockEntityRenderer implements LongRangeBlockEntityRenderer<OilDerrickBlockEntity> {
    static final ModelResourceLocation DERRICK_MODEL = MachineModelRenderer.standalone("block/machine_well_world");
    static final ModelResourceLocation PUMPJACK_BASE = MachineModelRenderer.standalone("block/machine_pumpjack_base");
    static final ModelResourceLocation PUMPJACK_ROTOR = MachineModelRenderer.standalone("block/machine_pumpjack_rotor");
    static final ModelResourceLocation PUMPJACK_HEAD = MachineModelRenderer.standalone("block/machine_pumpjack_head");
    static final ModelResourceLocation PUMPJACK_CARRIAGE = MachineModelRenderer.standalone("block/machine_pumpjack_carriage");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ResourceLocation ROD_TEXTURE = ReinhardtsHBM.id("textures/models/network/wire_greyscale.png");
    private static final AABB[] DERRICK_BOUNDS = buildDerrickBounds();
    private static final AABB[] PUMPJACK_BOUNDS = buildPumpjackBounds();

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
        if (state.is(HbmBlocks.MACHINE_PUMPJACK.get())) {
            renderPumpjack(derrick, partialTick, poseStack, bufferSource, packedLight, packedOverlay, state,
                    oilPumpjackFacing(state));
            return;
        }
        renderDerrick(poseStack, bufferSource, packedLight, packedOverlay, state, oilDerrickFacing(state));
    }

    private void renderDerrick(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, BlockState state, Direction facing) {
        poseStack.pushPose();
        applyDerrickTransform(poseStack, facing);
        STATIC_MESHES.render(DERRICK_MODEL, poseStack, state, packedLight, packedOverlay);
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
        applyPumpjackTransform(poseStack, facing);

        renderPart(PUMPJACK_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.5F, -5.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation - 90.0F));
        poseStack.translate(0.0F, -1.5F, 5.5F);
        renderPart(PUMPJACK_ROTOR, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 3.5F, -3.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(
                (float) (Math.toDegrees(Math.sin(Math.toRadians(rotation))) * 0.25D)));
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
        BlockState state = blockEntity.getBlockState();
        if (state.is(HbmBlocks.MACHINE_PUMPJACK.get())) {
            Direction facing = oilPumpjackFacing(state);
            /* Full 0..360 degree pump cycle. X and floor/max-Z come from Base;
             * the rotor's farthest face-used vertex is 1.0000006491277895
             * from (Y=1.5,Z=-5.5), and the procedural front rod reaches the
             * exact top at pRot=.25, radial offset=33.75 degrees. */
            return PUMPJACK_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
        }
        Direction facing = oilDerrickFacing(state);
        return DERRICK_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static void renderPart(
            ModelResourceLocation model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        STATIC_MESHES.render(model, poseStack, state, packedLight, packedOverlay);
    }

    private static void renderRods(float rotation, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(ROD_TEXTURE));
        double radians = Math.toRadians(rotation);
        double backAngle = -Math.sin(radians) * 0.25D;
        double backY = -2.0D * Math.sin(backAngle);
        double backZ = -2.0D * Math.cos(backAngle);
        double rotorAngle = -Math.toRadians(rotation - 90.0D);
        double rotorY = 0.5D * Math.cos(rotorAngle);
        double rotorZ = -0.5D * Math.sin(rotorAngle);

        for (int i = -1; i <= 1; i += 2) {
            double xi = 0.53125D * i;
            double y1 = 1.5D + rotorY;
            double z1 = -5.5D + rotorZ;
            double y2 = 3.5D + backY;
            double z2 = -3.5D + backZ;
            quad(
                    poseStack,
                    consumer,
                    xi, y1, z1 - 0.0625D,
                    xi, y1, z1 + 0.0625D,
                    xi, y2, z2 + 0.0625D,
                    xi, y2, z2 - 0.0625D,
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
            double frontPosY = Math.sin(pRot);
            double frontPosZ = Math.cos(pRot);

            double dist = 0.03125D;
            double radialLength = 2.5D + dist;
            double frontRadY = radialLength * Math.sin(pRot);
            double frontRadZ = radialLength * Math.cos(pRot);
            double cutlet = 360.0D / 32.0D;
            double initialAngle = -Math.toRadians(cutlet * -3.0D);
            double initialCos = Math.cos(initialAngle);
            double initialSin = Math.sin(initialAngle);
            double rotatedY = frontRadY * initialCos + frontRadZ * initialSin;
            frontRadZ = frontRadZ * initialCos - frontRadY * initialSin;
            frontRadY = rotatedY;
            double stepAngle = -Math.toRadians(cutlet);
            double stepCos = Math.cos(stepAngle);
            double stepSin = Math.sin(stepAngle);

            for (int j = 0; j < 4; j++) {
                double sumY1 = frontPosY + frontRadY;
                double sumZ1 = frontPosZ + frontRadZ;
                if (frontRadY < 0.0D) {
                    sumZ1 = 3.5D + dist * 0.5D;
                }

                rotatedY = frontRadY * stepCos + frontRadZ * stepSin;
                frontRadZ = frontRadZ * stepCos - frontRadY * stepSin;
                frontRadY = rotatedY;

                double sumY2 = frontPosY + frontRadY;
                double sumZ2 = frontPosZ + frontRadZ;
                if (frontRadY < 0.0D) {
                    sumZ2 = 3.5D + dist * 0.5D;
                }

                double xL = (width - pd) * i;
                double xR = (width + pd) * i;
                quad(
                        poseStack,
                        consumer,
                        xL, 3.5D + sumY1, -3.5D + sumZ1,
                        xR, 3.5D + sumY1, -3.5D + sumZ1,
                        xR, 3.5D + sumY2, -3.5D + sumZ2,
                        xL, 3.5D + sumY2, -3.5D + sumZ2,
                        51,
                        51,
                        51,
                        packedOverlay
                );
            }

            double sumY = frontPosY + frontRadY;
            double sumZ = frontPosZ + frontRadZ;
            if (frontRadY < 0.0D) {
                sumZ = 3.5D + dist * 0.5D;
            }

            double xR = (width + pd) * i;
            double xL = (width - pd) * i;
            quad(
                    poseStack,
                    consumer,
                    xR, 3.5D + sumY, -3.5D + sumZ,
                    xL, 3.5D + sumY, -3.5D + sumZ,
                    xL, 2.0D + height, 0.0D,
                    xR, 2.0D + height, 0.0D,
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
                p, height + 1.5D, p,
                -p, height + 1.5D, -p,
                -p, 0.75D, -p,
                p, 0.75D, p,
                51,
                51,
                51,
                packedOverlay
        );
        quad(
                poseStack,
                consumer,
                -p, height + 1.5D, p,
                p, height + 1.5D, -p,
                p, 0.75D, -p,
                -p, 0.75D, p,
                51,
                51,
                51,
                packedOverlay
        );
    }

    private static void quad(PoseStack poseStack, VertexConsumer consumer,
                             double ax, double ay, double az,
                             double bx, double by, double bz,
                             double cx, double cy, double cz,
                             double dx, double dy, double dz,
                             int r, int g, int bl, int packedOverlay) {
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, ax, ay, az, 0.0F, 0.0F, r, g, bl, packedOverlay);
        vertex(consumer, pose, bx, by, bz, 0.0F, 1.0F, r, g, bl, packedOverlay);
        vertex(consumer, pose, cx, cy, cz, 1.0F, 1.0F, r, g, bl, packedOverlay);
        vertex(consumer, pose, dx, dy, dz, 1.0F, 0.0F, r, g, bl, packedOverlay);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                               double x, double y, double z,
                               float u, float v, int r, int g, int b, int packedOverlay) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static AABB[] buildDerrickBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyDerrickTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.5F, 0.0F, -1.5F, 1.5F, 9.75F, 1.5F);
        }
        return bounds;
    }

    private static AABB[] buildPumpjackBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyPumpjackTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.5F, 0.0F, (float) -6.5000006491277895D,
                    1.5F, (float) 5.630673637163781D, 0.5F);
        }
        return bounds;
    }

    private static void applyDerrickTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Oil derrick facing must be horizontal: " + facing);
        }
    }

    private static void applyPumpjackTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Oil pumpjack facing must be horizontal: " + facing);
        }
    }

    private static Direction oilDerrickFacing(BlockState state) {
        if (!state.hasProperty(OilDerrickBlock.FACING)) {
            throw new IllegalStateException("Oil derrick block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(OilDerrickBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Oil derrick block state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static Direction oilPumpjackFacing(BlockState state) {
        if (!state.hasProperty(OilPumpjackBlock.FACING)) {
            throw new IllegalStateException("Oil pumpjack block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(OilPumpjackBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Oil pumpjack block state has a vertical facing: " + facing);
        }
        return facing;
    }

}
