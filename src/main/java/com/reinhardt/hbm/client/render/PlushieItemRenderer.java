package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.PlushieType;
import com.reinhardt.hbm.item.PlushieBlockItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class PlushieItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final IClientItemExtensions EXTENSION = new IClientItemExtensions() {
        private final PlushieItemRenderer renderer = new PlushieItemRenderer();

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return renderer;
        }
    };

    public PlushieItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static IClientItemExtensions clientExtension() {
        return EXTENSION;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        PlushieType type = PlushieBlockItem.type(stack);
        poseStack.pushPose();
        // ItemRenderBase applied this common transform before the plushie's
        // own inventory/hand branch.  Keep it explicit here so the legacy
        // per-type scales below are not applied to raw OBJ coordinates.
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -6.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.25F, 0.0F);
        if (type == PlushieType.YOMI) {
            poseStack.scale(1.25F, 1.25F, 1.25F);
        } else if (type == PlushieType.NUMBER_NINE) {
            poseStack.translate(0.0F, 0.25F, 0.25F);
            poseStack.scale(1.25F, 1.25F, 1.25F);
        } else if (type == PlushieType.HUNDUN) {
            poseStack.translate(0.5F, 0.5F, 0.0F);
            poseStack.scale(1.25F, 1.25F, 1.25F);
        } else if (type == PlushieType.DERG) {
            poseStack.scale(1.5F, 1.5F, 1.5F);
        }
        PlushieBlockEntityRenderer.renderItem(type, HbmBlocks.PLUSHIE.get().defaultBlockState(), poseStack,
                bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
