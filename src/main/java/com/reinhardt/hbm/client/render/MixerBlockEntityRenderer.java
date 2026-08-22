package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.MixerBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class MixerBlockEntityRenderer implements BlockEntityRenderer<MixerBlockEntity> {
    private static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/machine_mixer_world");
    private static final ModelResourceLocation BLADE = MachineModelRenderer.standalone("block/machine_mixer_blade");
    private static final ModelResourceLocation FLUID = MachineModelRenderer.standalone("block/machine_mixer_fluid");

    public MixerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(BLADE);
        event.register(FLUID);
    }

    @Override
    public void render(MixerBlockEntity mixer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = mixer.getBlockState();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BODY), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(mixer.rotation(partialTick)), 0.0F, -1.0F, 0.0F)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BLADE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        int totalFill = mixer.inputTank1().amount() + mixer.inputTank2().amount() + mixer.outputTank().amount();
        int totalCapacity = mixer.inputTank1().capacity() + mixer.inputTank2().capacity() + mixer.outputTank().capacity();
        HbmFluidDefinition renderFluid = mixer.outputTank().amount() > 0 ? mixer.outputTank().type() : mixer.configuredOutput();
        if (totalFill > 0 && totalCapacity > 0 && !renderFluid.isNone()) {
            int argb = 0xBF000000 | renderFluid.color();
            poseStack.pushPose();
            poseStack.translate(0.0D, 1.0D, 0.0D);
            poseStack.scale(1.0F, Math.min(0.99F, totalFill / (float) totalCapacity * 0.99F), 1.0F);
            poseStack.translate(0.0D, -1.0D, 0.0D);
            MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(FLUID), poseStack, bufferSource, state, packedLight, packedOverlay, argb);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(MixerBlockEntity blockEntity) {
        return new AABB(
                blockEntity.getBlockPos().getX(),
                blockEntity.getBlockPos().getY(),
                blockEntity.getBlockPos().getZ(),
                blockEntity.getBlockPos().getX() + 1.0D,
                blockEntity.getBlockPos().getY() + 3.0D,
                blockEntity.getBlockPos().getZ() + 1.0D
        );
    }
}
