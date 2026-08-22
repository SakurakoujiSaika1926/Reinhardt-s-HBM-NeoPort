package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.ExcavatorBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class ExcavatorBlockEntityRenderer implements BlockEntityRenderer<ExcavatorBlockEntity> {
    private static final ModelResourceLocation MAIN = MachineModelRenderer.standalone("block/machine_excavator_main");
    private static final ModelResourceLocation CRUSHER_1 = MachineModelRenderer.standalone("block/machine_excavator_crusher1");
    private static final ModelResourceLocation CRUSHER_2 = MachineModelRenderer.standalone("block/machine_excavator_crusher2");
    private static final ModelResourceLocation DRILLBIT = MachineModelRenderer.standalone("block/machine_excavator_drillbit");
    private static final ModelResourceLocation SHAFT = MachineModelRenderer.standalone("block/machine_excavator_shaft");
    private static final ResourceLocation COBBLE = ReinhardtsHBM.id("textures/models/machines/cobblestone.png");
    private static final ResourceLocation GRAVEL = ReinhardtsHBM.id("textures/models/machines/gravel.png");

    public ExcavatorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MAIN);
        event.register(CRUSHER_1);
        event.register(CRUSHER_2);
        event.register(DRILLBIT);
        event.register(SHAFT);
    }

    @Override
    public void render(ExcavatorBlockEntity excavator, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = excavator.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        rotateY(poseStack, legacyYaw(facing));
        poseStack.translate(0.0F, -3.0F, 0.0F);
        renderPart(MAIN, poseStack, bufferSource, state, packedLight, packedOverlay);

        float crusher = excavator.crusherRotation(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.0F, 2.8125F);
        rotateX(poseStack, -crusher);
        poseStack.translate(0.0F, -2.0F, -2.8125F);
        renderPart(CRUSHER_1, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 2.0F, 2.1875F);
        rotateX(poseStack, crusher);
        poseStack.translate(0.0F, -2.0F, -2.1875F);
        renderPart(CRUSHER_2, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        rotateY(poseStack, -excavator.drillRotation(partialTick));
        float ext = excavator.drillExtension(partialTick);
        poseStack.translate(0.0F, -ext, 0.0F);
        renderPart(DRILLBIT, poseStack, bufferSource, state, packedLight, packedOverlay);
        while (ext >= -1.5F) {
            renderPart(SHAFT, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.translate(0.0F, 2.0F, 0.0F);
            ext -= 2.0F;
        }
        poseStack.popPose();

        if (excavator.chuteTimer() > 0) {
            renderChute(excavator, poseStack, bufferSource, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ExcavatorBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(5.0D, 8.0D, 5.0D);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case NORTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderChute(ExcavatorBlockEntity excavator, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        double speed = 250.0D;
        float dropU = (float) (-(System.currentTimeMillis() % (long) speed) / speed);
        float dropL = dropU + 4.0F;

        VertexConsumer cobble = bufferSource.getBuffer(RenderType.entityCutoutNoCull(COBBLE));
        renderDropColumn(poseStack, cobble, 0.125D, 0.125D, 1.0F, 1.0F, 3.0D, 2.0D, dropU, dropL, packedOverlay);

        boolean crushed = excavator.crusherEnabled();
        VertexConsumer output = bufferSource.getBuffer(RenderType.entityCutoutNoCull(crushed ? GRAVEL : COBBLE));
        renderDropColumn(poseStack, output, crushed ? 0.5D : 0.25D, 0.0625D, crushed ? 4.0F : 2.0F, 0.5F, 2.0D, 1.0D, dropU, dropL, packedOverlay);
    }

    private static void renderDropColumn(
            PoseStack poseStack,
            VertexConsumer consumer,
            double widthX,
            double widthZ,
            float faceUMax,
            float sideUMax,
            double top,
            double bottom,
            float dropU,
            float dropL,
            int packedOverlay
    ) {
        PoseStack.Pose pose = poseStack.last();
        quad(consumer, pose, widthX, top, 2.5D + widthZ, -widthX, top, 2.5D + widthZ, -widthX, bottom, 2.5D + widthZ, widthX, bottom, 2.5D + widthZ, 0.0F, dropU, faceUMax, dropL, packedOverlay);
        quad(consumer, pose, -widthX, top, 2.5D - widthZ, widthX, top, 2.5D - widthZ, widthX, bottom, 2.5D - widthZ, -widthX, bottom, 2.5D - widthZ, 0.0F, dropU, faceUMax, dropL, packedOverlay);
        quad(consumer, pose, -widthX, top, 2.5D + widthZ, -widthX, top, 2.5D - widthZ, -widthX, bottom, 2.5D - widthZ, -widthX, bottom, 2.5D + widthZ, 0.0F, dropU, sideUMax, dropL, packedOverlay);
        quad(consumer, pose, widthX, top, 2.5D - widthZ, widthX, top, 2.5D + widthZ, widthX, bottom, 2.5D + widthZ, widthX, bottom, 2.5D - widthZ, 0.0F, dropU, sideUMax, dropL, packedOverlay);
    }

    private static void quad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            double ax,
            double ay,
            double az,
            double bx,
            double by,
            double bz,
            double cx,
            double cy,
            double cz,
            double dx,
            double dy,
            double dz,
            float uMin,
            float vMin,
            float uMax,
            float vMax,
            int packedOverlay
    ) {
        vertex(consumer, pose, ax, ay, az, uMin, vMin, packedOverlay);
        vertex(consumer, pose, bx, by, bz, uMax, vMin, packedOverlay);
        vertex(consumer, pose, cx, cy, cz, uMax, vMax, packedOverlay);
        vertex(consumer, pose, dx, dy, dz, uMin, vMax, packedOverlay);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, float u, float v, int packedOverlay) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(0x00F000F0)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void rotateX(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 1.0F, 0.0F, 0.0F)));
    }

    private static void rotateY(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F)));
    }
}
