package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.PipeAnchorBlock;
import com.reinhardt.hbm.blockentity.PipeAnchorBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/** Exact 1.7.10 RenderPipeAnchor geometry and connection-line transforms. */
public final class PipeAnchorBlockEntityRenderer implements BlockEntityRenderer<PipeAnchorBlockEntity> {
    private static final ModelResourceLocation ANCHOR = part("pipe_anchor");
    private static final ModelResourceLocation PIPE = part("pipe_anchor_pipe");
    private static final ModelResourceLocation RING = part("pipe_anchor_ring");

    public PipeAnchorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ANCHOR);
        event.register(PIPE);
        event.register(RING);
    }

    @Override
    public void render(PipeAnchorBlockEntity anchor, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = anchor.getBlockState();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        rotateAnchor(poseStack, state.getValue(PipeAnchorBlock.FACING));
        poseStack.translate(0.0D, -0.5D, 0.0D);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(ANCHOR), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        if (anchor.getLevel() == null) {
            return;
        }
        HbmFluidDefinition type = anchor.type();
        for (BlockPos link : anchor.networkLinks()) {
            if (!(anchor.getLevel().getBlockEntity(link) instanceof PipeAnchorBlockEntity other)
                    || type != other.type() || !isDominant(anchor.getBlockPos(), other.getBlockPos())) {
                continue;
            }

            Vec3 first = connectionPoint(anchor.getBlockPos());
            Vec3 second = connectionPoint(other.getBlockPos());
            double dx = second.x - first.x;
            double dy = second.y - first.y;
            double dz = second.z - first.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double yaw = Math.toDegrees(Math.atan2(dx, dz));
            double pitch = Math.toDegrees(Math.atan2(dy, horizontal));
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

            poseStack.pushPose();
            poseStack.translate(0.5D, 0.5D, 0.5D);
            poseStack.mulPose(yQuaternion((float) yaw));
            poseStack.mulPose(new Quaternionf(new AxisAngle4f(
                    (float) Math.toRadians(90.0D - pitch), 1.0F, 0.0F, 0.0F)));

            poseStack.pushPose();
            poseStack.scale(1.0F, (float) length, 1.0F);
            poseStack.translate(0.0D, -0.5D, 0.0D);
            MachineModelRenderer.renderUnculledTinted(
                    MachineModelRenderer.model(PIPE), poseStack, bufferSource, state, packedLight, packedOverlay,
                    lightenColor(type.color()));
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(0.0D, length / 2.0D - 1.5D, 0.0D);
            MachineModelRenderer.renderUnculled(
                    MachineModelRenderer.model(RING), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(PipeAnchorBlockEntity anchor) {
        return new AABB(anchor.getBlockPos()).inflate(10.0D);
    }

    private static ModelResourceLocation part(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }

    private static Vec3 connectionPoint(BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }

    private static boolean isDominant(BlockPos first, BlockPos second) {
        if (first.getX() != second.getX()) {
            return first.getX() < second.getX();
        }
        if (first.getY() != second.getY()) {
            return first.getY() < second.getY();
        }
        if (first.getZ() != second.getZ()) {
            return first.getZ() < second.getZ();
        }
        return false;
    }

    private static void rotateAnchor(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(MachineModelRenderer.xQuaternion(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(MachineModelRenderer.xQuaternion(90.0F));
                poseStack.mulPose(MachineModelRenderer.zQuaternion(180.0F));
            }
            case SOUTH -> poseStack.mulPose(MachineModelRenderer.xQuaternion(90.0F));
            case WEST -> {
                poseStack.mulPose(MachineModelRenderer.xQuaternion(90.0F));
                poseStack.mulPose(MachineModelRenderer.zQuaternion(90.0F));
            }
            case EAST -> {
                poseStack.mulPose(MachineModelRenderer.xQuaternion(90.0F));
                poseStack.mulPose(MachineModelRenderer.zQuaternion(270.0F));
            }
        }
    }

    private static Quaternionf yQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    private static int lightenColor(int color) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        red = (int) (red + (255 - red) * 0.25D);
        green = (int) (green + (255 - green) * 0.25D);
        blue = (int) (blue + (255 - blue) * 0.25D);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
