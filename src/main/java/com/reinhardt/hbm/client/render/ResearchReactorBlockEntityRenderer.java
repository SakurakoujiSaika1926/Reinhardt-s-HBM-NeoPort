package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.blockentity.ResearchReactorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ResearchReactorBlockEntityRenderer implements LongRangeBlockEntityRenderer<ResearchReactorBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_reactor_small_base");
    private static final ModelResourceLocation RODS = MachineModelRenderer.standalone("block/machine_reactor_small_rods");
    private static final AABB RENDER_BOUNDS = createResearchReactorLocalBounds();

    public ResearchReactorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(RODS);
    }

    @Override
    public void render(ResearchReactorBlockEntity reactor, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = reactor.getBlockState();
        poseStack.pushPose();
        applyResearchReactorPose(poseStack);
        STATIC_MESHES.render(BASE, poseStack, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0D, reactor.controlLevel(partialTick), 0.0D);
        STATIC_MESHES.render(RODS, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ResearchReactorBlockEntity blockEntity) {
        // Full 0..1 control travel: base y -0.5..3, rods y 1..3.3 translated up to one block.
        return RENDER_BOUNDS.move(blockEntity.getBlockPos());
    }

    private static void applyResearchReactorPose(PoseStack poseStack) {
        // RenderSmallReactor: translate to the block centre, then rotate the
        // complete base/rod assembly exactly 180 degrees around Y.
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    private static AABB createResearchReactorLocalBounds() {
        PoseStack poseStack = new PoseStack();
        applyResearchReactorPose(poseStack);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -0.5F, -0.5F, -0.5F, 0.5F, 4.3F, 0.5F);
    }
}
