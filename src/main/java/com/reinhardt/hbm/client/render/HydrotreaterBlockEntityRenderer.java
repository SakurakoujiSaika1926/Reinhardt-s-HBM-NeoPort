package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.HydrotreaterBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class HydrotreaterBlockEntityRenderer implements LongRangeBlockEntityRenderer<HydrotreaterBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_hydrotreater");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();

    public HydrotreaterBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(HydrotreaterBlockEntity machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        // The legacy TESR anchors this OBJ at the center of its core block.
        poseStack.translate(0.5F, 0.0F, 0.5F);
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HydrotreaterBlockEntity machine) {
        // Face-used hydrotreater.obj vertices after the renderer's (+.5, 0, +.5) translation.
        return RenderBounds.localFloatVertices(machine.getBlockPos(),
                -1.0D, 0.0D, -1.0D, 2.0D, 7.0D, 2.0D);
    }
}
