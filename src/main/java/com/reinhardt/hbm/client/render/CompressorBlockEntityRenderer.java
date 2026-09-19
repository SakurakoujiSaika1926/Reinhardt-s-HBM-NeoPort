package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.CompressorBlockEntity;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class CompressorBlockEntityRenderer implements LongRangeBlockEntityRenderer<CompressorBlockEntity> {
    private static final ModelResourceLocation NORMAL_BODY = MachineModelRenderer.standalone("block/machine_compressor_world");
    private static final ModelResourceLocation NORMAL_PUMP = MachineModelRenderer.standalone("block/machine_compressor_pump");
    private static final ModelResourceLocation NORMAL_FAN = MachineModelRenderer.standalone("block/machine_compressor_fan");
    private static final ModelResourceLocation COMPACT_BODY = MachineModelRenderer.standalone("block/machine_compressor_compact_world");
    private static final ModelResourceLocation COMPACT_FAN1 = MachineModelRenderer.standalone("block/machine_compressor_compact_fan1");
    private static final ModelResourceLocation COMPACT_FAN2 = MachineModelRenderer.standalone("block/machine_compressor_compact_fan2");

    public CompressorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(NORMAL_BODY);
        event.register(NORMAL_PUMP);
        event.register(NORMAL_FAN);
        event.register(COMPACT_BODY);
        event.register(COMPACT_FAN1);
        event.register(COMPACT_FAN2);
    }

    @Override
    public void render(CompressorBlockEntity compressor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = compressor.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;

        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(
                poseStack,
                LegacyMachineGeometry.legacyWavefrontYaw(facing, 270.0F)
        );
        if (compressor.kind() == CompressorBlockEntity.Kind.COMPACT) {
            renderCompact(compressor, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
        } else {
            renderNormal(compressor, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CompressorBlockEntity blockEntity) {
        if (blockEntity.kind() == CompressorBlockEntity.Kind.COMPACT) {
            return new AABB(
                    blockEntity.getBlockPos().getX() - 3.0D,
                    blockEntity.getBlockPos().getY(),
                    blockEntity.getBlockPos().getZ() - 3.0D,
                    blockEntity.getBlockPos().getX() + 4.0D,
                    blockEntity.getBlockPos().getY() + 3.0D,
                    blockEntity.getBlockPos().getZ() + 4.0D
            );
        }
        return new AABB(
                blockEntity.getBlockPos().getX() - 2.0D,
                blockEntity.getBlockPos().getY(),
                blockEntity.getBlockPos().getZ() - 2.0D,
                blockEntity.getBlockPos().getX() + 3.0D,
                blockEntity.getBlockPos().getY() + 9.0D,
                blockEntity.getBlockPos().getZ() + 3.0D
        );
    }

    private static void renderNormal(CompressorBlockEntity compressor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(NORMAL_BODY), poseStack, bufferSource, state, packedLight, packedOverlay);

        float lift = compressor.piston(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0D, lift * 3.0D - 3.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(NORMAL_PUMP), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        float fan = compressor.fanSpin(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(fan), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(NORMAL_FAN), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderCompact(CompressorBlockEntity compressor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(COMPACT_BODY), poseStack, bufferSource, state, packedLight, packedOverlay);

        float fan = compressor.fanSpin(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(fan), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(COMPACT_FAN1), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(fan), -1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(COMPACT_FAN2), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
