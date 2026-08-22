package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.CyclotronBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class CyclotronBlockEntityRenderer implements BlockEntityRenderer<CyclotronBlockEntity> {
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
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BODY), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(cyclotron.getPlug(0) ? B1_FILLED : B1), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(cyclotron.getPlug(1) ? B2_FILLED : B2), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(cyclotron.getPlug(2) ? B3_FILLED : B3), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(cyclotron.getPlug(3) ? B4_FILLED : B4), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CyclotronBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(4.0D, 5.0D, 4.0D);
    }
}
