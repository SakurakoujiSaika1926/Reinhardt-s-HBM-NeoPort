package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.RefuelerBlock;
import com.reinhardt.hbm.blockentity.RefuelerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Direct RenderRefueler OBJ route with the legacy liquid chamber kept as a separately tinted part. */
public final class RefuelerBlockEntityRenderer implements BlockEntityRenderer<RefuelerBlockEntity> {
    private static final ModelResourceLocation FUELER = MachineModelRenderer.standalone("block/refueler_body");
    private static final ModelResourceLocation FLUID = MachineModelRenderer.standalone("block/refueler_fluid");

    public RefuelerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(FUELER);
        event.register(FLUID);
    }

    @Override
    public void render(RefuelerBlockEntity refueler, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = refueler.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        orient(poseStack, state.getValue(RefuelerBlock.FACING));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FUELER), poseStack, bufferSource,
                state, packedLight, packedOverlay);

        float fill = Math.clamp(refueler.fillLevel(partialTick), 0.0F, 1.0F);
        if (fill > 0.0F && !refueler.tank().type().isNone()) {
            poseStack.pushPose();
            // The old clip-plane liquid keeps its bottom fixed at y=0.12 and moves only the surface.
            poseStack.translate(0.0D, 0.12D, 0.0D);
            poseStack.scale(1.0F, fill, 1.0F);
            poseStack.translate(0.0D, -0.12D, 0.0D);
            MachineModelRenderer.renderUnculledTintedTranslucent(MachineModelRenderer.model(FLUID), poseStack,
                    bufferSource, state, packedLight, packedOverlay, 0xBF000000 | refueler.tank().type().color());
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RefuelerBlockEntity refueler) {
        return new AABB(refueler.getBlockPos());
    }

    private static void orient(PoseStack poseStack, Direction facing) {
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        // Literal BlockRefueler / RenderRefueler metadata mapping.
        switch (facing) {
            case SOUTH -> {
            }
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> {
            }
        }
    }
}
