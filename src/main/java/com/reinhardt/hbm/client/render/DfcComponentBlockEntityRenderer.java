package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.DfcComponentBlock;
import com.reinhardt.hbm.blockentity.DfcEmitterBlockEntity;
import com.reinhardt.hbm.blockentity.DfcInventoryBlockEntity;
import com.reinhardt.hbm.blockentity.DfcInjectorBlockEntity;
import com.reinhardt.hbm.blockentity.DfcReceiverBlockEntity;
import com.reinhardt.hbm.blockentity.DfcStabilizerBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.List;

public class DfcComponentBlockEntityRenderer<T extends BlockEntity> implements LongRangeBlockEntityRenderer<T> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation EMITTER = MachineModelRenderer.standalone("block/dfc_emitter");
    private static final ModelResourceLocation RECEIVER = MachineModelRenderer.standalone("block/dfc_receiver");
    private static final ModelResourceLocation INJECTOR = MachineModelRenderer.standalone("block/dfc_injector");
    private static final ModelResourceLocation STABILIZER = MachineModelRenderer.standalone("block/dfc_stabilizer");
    private static final List<ModelResourceLocation> MODELS = List.of(
            EMITTER, RECEIVER, INJECTOR, STABILIZER);

    public DfcComponentBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(EMITTER);
        event.register(RECEIVER);
        event.register(INJECTOR);
        event.register(STABILIZER);
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ModelResourceLocation model = model(blockEntity);
        if (model == null || !(blockEntity instanceof DfcInventoryBlockEntity dfc)) {
            return;
        }

        DfcInventoryBlockEntity.BeamRenderSnapshot snapshot = dfc.beamRenderSnapshot();
        if (!(snapshot.geometryToken() instanceof BakedModelGeneration generation)) {
            return;
        }
        BlockState state = snapshot.blockState();
        Direction facing = snapshot.facing();
        int beamLength = snapshot.length();
        if (blockEntity instanceof DfcEmitterBlockEntity) {
            poseStack.pushPose();
            applyEmitterPose(poseStack, facing);
            STATIC_MESHES.render(generation.model(EMITTER), poseStack,
                    state, packedLight, packedOverlay);
            poseStack.popPose();
        } else if (blockEntity instanceof DfcReceiverBlockEntity) {
            poseStack.pushPose();
            applyReceiverPose(poseStack, facing);
            STATIC_MESHES.render(generation.model(RECEIVER), poseStack,
                    state, packedLight, packedOverlay);
            poseStack.popPose();
        } else if (blockEntity instanceof DfcInjectorBlockEntity) {
            poseStack.pushPose();
            applyInjectorPose(poseStack, facing);
            STATIC_MESHES.render(generation.model(INJECTOR), poseStack,
                    state, packedLight, packedOverlay);
            poseStack.popPose();
        } else if (blockEntity instanceof DfcStabilizerBlockEntity) {
            poseStack.pushPose();
            applyStabilizerPose(poseStack, facing);
            STATIC_MESHES.render(generation.model(STABILIZER), poseStack,
                    state, packedLight, packedOverlay);
            poseStack.popPose();
        }

        if (blockEntity instanceof DfcEmitterBlockEntity && beamLength > 0) {
            renderBeam(poseStack, bufferSource, facing, beamLength, snapshot.color(), 0.07D);
        } else if (blockEntity instanceof DfcInjectorBlockEntity && beamLength > 0) {
            renderBeam(poseStack, bufferSource, facing, beamLength, snapshot.color(), 0.035D);
        } else if (blockEntity instanceof DfcStabilizerBlockEntity && beamLength > 0) {
            renderBeam(poseStack, bufferSource, facing, beamLength, snapshot.color(), 0.04D);
        }
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        return blockEntity instanceof DfcInventoryBlockEntity dfc
                ? dfc.beamRenderSnapshot().bounds()
                : new AABB(blockEntity.getBlockPos());
    }

    static ModelResourceLocation model(BlockEntity blockEntity) {
        if (blockEntity instanceof DfcEmitterBlockEntity) {
            return EMITTER;
        }
        if (blockEntity instanceof DfcReceiverBlockEntity) {
            return RECEIVER;
        }
        if (blockEntity instanceof DfcInjectorBlockEntity) {
            return INJECTOR;
        }
        if (blockEntity instanceof DfcStabilizerBlockEntity) {
            return STABILIZER;
        }
        return null;
    }

    static List<ModelResourceLocation> models() {
        return MODELS;
    }

    static void applyEmitterPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        switch (facing) {
            case DOWN -> {
                poseStack.translate(0.0D, 0.5D, -0.5D);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            }
            case UP -> {
                poseStack.translate(0.0D, 0.5D, 0.5D);
                poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
            }
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> {
            }
        }
    }

    static void applyReceiverPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        switch (facing) {
            case DOWN -> {
                poseStack.translate(0.0D, 0.5D, -0.5D);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            }
            case UP -> {
                poseStack.translate(0.0D, 0.5D, 0.5D);
                poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
            }
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> {
            }
        }
    }

    static void applyInjectorPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        switch (facing) {
            case DOWN -> {
                poseStack.translate(0.0D, 0.5D, -0.5D);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            }
            case UP -> {
                poseStack.translate(0.0D, 0.5D, 0.5D);
                poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
            }
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> {
            }
        }
    }

    static void applyStabilizerPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        switch (facing) {
            case DOWN -> {
                poseStack.translate(0.0D, 0.5D, -0.5D);
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            }
            case UP -> {
                poseStack.translate(0.0D, 0.5D, 0.5D);
                poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));
            }
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> {
            }
        }
    }

    private static void renderBeam(PoseStack poseStack, MultiBufferSource bufferSource, Direction direction, int length, int argb, double thickness) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        PoseStack.Pose pose = poseStack.last();
        double endX = 0.5D + direction.getStepX() * length;
        double endY = 0.5D + direction.getStepY() * length;
        double endZ = 0.5D + direction.getStepZ() * length;
        beamQuad(consumer, pose, 0.5D, 0.5D, 0.5D, endX, endY, endZ, thickness, argb);
        beamQuad(consumer, pose, 0.5D, 0.5D + thickness, 0.5D,
                endX, endY + thickness, endZ, thickness * 0.5D, argb);
    }

    private static void beamQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                 double startX, double startY, double startZ,
                                 double endX, double endY, double endZ,
                                 double thickness, int argb) {
        double directionX = endX - startX;
        double directionZ = endZ - startZ;
        double axisX = directionZ;
        double axisZ = -directionX;
        double axisLengthSqr = axisX * axisX + axisZ * axisZ;
        if (axisLengthSqr < 1.0E-5D) {
            axisX = thickness;
            axisZ = 0.0D;
        } else {
            double scale = thickness / Math.sqrt(axisLengthSqr);
            axisX *= scale;
            axisZ *= scale;
        }
        beamVertex(consumer, pose, startX + axisX, startY + thickness, startZ + axisZ, argb);
        beamVertex(consumer, pose, startX - axisX, startY + thickness, startZ - axisZ, argb);
        beamVertex(consumer, pose, endX - axisX, endY - thickness, endZ - axisZ, argb);
        beamVertex(consumer, pose, endX + axisX, endY - thickness, endZ + axisZ, argb);
    }

    private static void beamVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                   double x, double y, double z, int argb) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
