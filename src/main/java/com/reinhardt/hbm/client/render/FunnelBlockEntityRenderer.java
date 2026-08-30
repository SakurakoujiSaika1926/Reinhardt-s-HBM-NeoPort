package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.FunnelBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact single-block origin handling from 1.7.10 MachineFunnel#renderWorld. */
public final class FunnelBlockEntityRenderer implements BlockEntityRenderer<FunnelBlockEntity> {
    private static final ModelResourceLocation TOP = part("machine_funnel_top");
    private static final ModelResourceLocation BOTTOM = part("machine_funnel_bottom");
    private static final ModelResourceLocation SIDE = part("machine_funnel_side");

    public FunnelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(TOP);
        event.register(BOTTOM);
        event.register(SIDE);
    }

    @Override
    public void render(FunnelBlockEntity funnel, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = funnel.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        renderPart(TOP, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(BOTTOM, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(SIDE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FunnelBlockEntity funnel) {
        return new AABB(funnel.getBlockPos());
    }

    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // MachineFunnel is an IRenderBlock in 1.7.10, not an ItemRenderBase
        // renderer. Its mesh is centered on X/Z around its own origin, so map
        // that origin to the modern block-item model space before applying the
        // only inventory transform used by the legacy renderer.
        poseStack.translate(0.5F, 0.0F, 0.5F);
        if (context == ItemDisplayContext.GUI) {
            // MachineFunnel#renderInventory
            poseStack.translate(0.0F, -0.5F, 0.0F);
        }
        BlockState state = com.reinhardt.hbm.registry.HbmBlocks.MACHINE_FUNNEL.get().defaultBlockState();
        renderPart(TOP, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(BOTTOM, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(SIDE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack,
                                   MultiBufferSource bufferSource, BlockState state,
                                   int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack,
                bufferSource, state, packedLight, packedOverlay);
    }

    private static ModelResourceLocation part(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }
}
