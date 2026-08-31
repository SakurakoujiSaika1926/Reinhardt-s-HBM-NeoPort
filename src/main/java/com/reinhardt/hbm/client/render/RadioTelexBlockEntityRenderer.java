package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.RadioTelexBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Direct port of the 1.7.10 RenderTelex transform and texture binding. */
public final class RadioTelexBlockEntityRenderer implements BlockEntityRenderer<RadioTelexBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/radio_telex");
    private static final ResourceLocation TEXTURE = com.reinhardt.hbm.ReinhardtsHBM.id(
            "textures/models/machines/telex.png"
    );

    public RadioTelexBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(RadioTelexBlockEntity telex, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = telex.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (state.getValue(LargeMachineBlock.FACING)) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> {
            }
            default -> {
            }
        }
        renderModel(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    static void renderModel(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay, TEXTURE, 0.0F, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(RadioTelexBlockEntity telex) {
        return new AABB(telex.getBlockPos()).inflate(1.0D, 0.0D, 1.0D).expandTowards(0.0D, 1.0D, 0.0D);
    }
}
