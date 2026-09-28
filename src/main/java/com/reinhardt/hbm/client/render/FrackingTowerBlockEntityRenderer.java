package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.FrackingTowerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class FrackingTowerBlockEntityRenderer implements LongRangeBlockEntityRenderer<FrackingTowerBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_fracking_tower");
    private static final ModelResourceLocation PORTS_MODEL = MachineModelRenderer.standalone("block/machine_fracking_tower_ports");
    private static final AABB LOCAL_BOUNDS = buildFrackingTowerBounds();

    public FrackingTowerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
        event.register(PORTS_MODEL);
    }

    @Override
    public void render(FrackingTowerBlockEntity tower, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = tower.getBlockState();
        poseStack.pushPose();
        applyFrackingTowerTransform(poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PORTS_MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FrackingTowerBlockEntity blockEntity) {
        /* Face-used fracking_tower.obj envelope under the fixed render pose.
         * The translated ports OBJ is wholly contained in this envelope. */
        return LOCAL_BOUNDS.move(blockEntity.getBlockPos());
    }

    private static AABB buildFrackingTowerBounds() {
        PoseStack poseStack = new PoseStack();
        applyFrackingTowerTransform(poseStack);
        return RenderBounds.transformedBox(net.minecraft.core.BlockPos.ZERO, poseStack,
                -3.5F, 0.0F, -3.55F, 3.5F, 24.5F, 3.5F);
    }

    private static void applyFrackingTowerTransform(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
    }
}
