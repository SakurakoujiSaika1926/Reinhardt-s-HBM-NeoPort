package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.LiquefactorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class LiquefactorBlockEntityRenderer implements BlockEntityRenderer<LiquefactorBlockEntity> {
    private static final ModelResourceLocation MAIN = MachineModelRenderer.standalone("block/machine_liquefactor");
    private static final ModelResourceLocation FLUID = MachineModelRenderer.standalone("block/machine_liquefactor_fluid");
    private static final ModelResourceLocation GLASS = MachineModelRenderer.standalone("block/machine_liquefactor_glass");
    private static final int GLASS_COLOR = 0x26BFFFFF;

    public LiquefactorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MAIN);
        event.register(FLUID);
        event.register(GLASS);
    }

    @Override
    public void render(LiquefactorBlockEntity liquefactor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = liquefactor.getBlockState();

        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MAIN), poseStack, bufferSource, state, packedLight, packedOverlay);

        if (liquefactor.tank().amount() > 0 && !liquefactor.tank().type().isNone()) {
            double height = (double) liquefactor.tank().amount() / (double) liquefactor.tank().capacity();
            poseStack.pushPose();
            poseStack.translate(0.0D, 1.0D, 0.0D);
            poseStack.scale(1.0F, (float) height, 1.0F);
            poseStack.translate(0.0D, -1.0D, 0.0D);
            MachineModelRenderer.renderUnculledTinted(
                    MachineModelRenderer.model(FLUID),
                    poseStack,
                    bufferSource,
                    state,
                    packedLight,
                    packedOverlay,
                    0xFF000000 | liquefactor.tank().type().color()
            );
            poseStack.popPose();
        }

        MachineModelRenderer.renderUnculledTinted(MachineModelRenderer.model(GLASS), poseStack, bufferSource, state, packedLight, packedOverlay, GLASS_COLOR);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LiquefactorBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 5.0D, 3.0D);
    }
}
