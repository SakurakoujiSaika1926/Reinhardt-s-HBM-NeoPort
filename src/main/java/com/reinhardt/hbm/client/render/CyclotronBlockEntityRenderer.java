package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.CyclotronBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class CyclotronBlockEntityRenderer implements LongRangeBlockEntityRenderer<CyclotronBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation BODY = MachineModelRenderer.standalone("block/machine_cyclotron_body");
    private static final ModelResourceLocation B1 = MachineModelRenderer.standalone("block/machine_cyclotron_b1");
    private static final ModelResourceLocation B1_FILLED = MachineModelRenderer.standalone("block/machine_cyclotron_b1_filled");
    private static final ModelResourceLocation B2 = MachineModelRenderer.standalone("block/machine_cyclotron_b2");
    private static final ModelResourceLocation B2_FILLED = MachineModelRenderer.standalone("block/machine_cyclotron_b2_filled");
    private static final ModelResourceLocation B3 = MachineModelRenderer.standalone("block/machine_cyclotron_b3");
    private static final ModelResourceLocation B3_FILLED = MachineModelRenderer.standalone("block/machine_cyclotron_b3_filled");
    private static final ModelResourceLocation B4 = MachineModelRenderer.standalone("block/machine_cyclotron_b4");
    private static final ModelResourceLocation B4_FILLED = MachineModelRenderer.standalone("block/machine_cyclotron_b4_filled");

    public CyclotronBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(B1);
        event.register(B1_FILLED);
        event.register(B2);
        event.register(B2_FILLED);
        event.register(B3);
        event.register(B3_FILLED);
        event.register(B4);
        event.register(B4_FILLED);
    }

    @Override
    public void render(CyclotronBlockEntity cyclotron, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = cyclotron.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        STATIC_MESHES.render(BODY, poseStack, state, packedLight, packedOverlay);
        STATIC_MESHES.render(cyclotron.getPlug(0) ? B1_FILLED : B1, poseStack, state, packedLight, packedOverlay);
        STATIC_MESHES.render(cyclotron.getPlug(1) ? B2_FILLED : B2, poseStack, state, packedLight, packedOverlay);
        STATIC_MESHES.render(cyclotron.getPlug(2) ? B3_FILLED : B3, poseStack, state, packedLight, packedOverlay);
        STATIC_MESHES.render(cyclotron.getPlug(3) ? B4_FILLED : B4, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CyclotronBlockEntity blockEntity) {
        // Body face bounds contain all four empty/filled plug variants.
        return RenderBounds.localFloatVertices(blockEntity.getBlockPos(),
                -2.0D, 0.0D, -2.0D, 3.0D, 3.75D, 3.0D);
    }
}
