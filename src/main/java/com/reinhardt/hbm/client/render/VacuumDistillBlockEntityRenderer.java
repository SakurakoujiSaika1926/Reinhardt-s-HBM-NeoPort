package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.VacuumDistillBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class VacuumDistillBlockEntityRenderer implements LongRangeBlockEntityRenderer<VacuumDistillBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_vacuum_distill");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();

    public VacuumDistillBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(VacuumDistillBlockEntity refinery, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = refinery.getBlockState();
        poseStack.pushPose();
        // RenderVacuumDistill has no metadata yaw: only the block-centre translation.
        poseStack.translate(0.5F, 0.0F, 0.5F);
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(VacuumDistillBlockEntity blockEntity) {
        // Exact face-used vacuum_distill.obj envelope under the fixed 180-degree render pose.
        return RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                -1.0D, 0.0D, -1.0D, 2.0D, 9.0D, 2.0D);
    }
}

