package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.LiquefactorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class LiquefactorBlockEntityRenderer implements LongRangeBlockEntityRenderer<LiquefactorBlockEntity> {
    private static final ModelResourceLocation MAIN = MachineModelRenderer.standalone("block/machine_liquefactor");
    private static final ModelResourceLocation FLUID = MachineModelRenderer.standalone("block/machine_liquefactor_fluid");
    private static final ModelResourceLocation GLASS = MachineModelRenderer.standalone("block/machine_liquefactor_glass");
    private static final int GLASS_COLOR = 0x26BFFFFF;
    private static final AABB RENDER_BOUNDS = createLiquefactorLocalBounds();

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
        applyLiquefactorPose(poseStack);
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
        // Main OBJ is +/-1.5 by 0..4 after the +0.5 origin shift; fluid/glass are contained.
        return RENDER_BOUNDS.move(blockEntity.getBlockPos());
    }

    private static void applyLiquefactorPose(PoseStack poseStack) {
        // The 1.7.10 liquefactor renderer has no facing-dependent rotation.
        // Keep this transform owned by this renderer instead of routing it
        // through a shared machine-orientation helper.
        poseStack.translate(0.5D, 0.0D, 0.5D);
    }

    private static AABB createLiquefactorLocalBounds() {
        PoseStack poseStack = new PoseStack();
        applyLiquefactorPose(poseStack);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -1.5F, 0.0F, -1.5F, 1.5F, 4.0F, 1.5F);
    }
}
