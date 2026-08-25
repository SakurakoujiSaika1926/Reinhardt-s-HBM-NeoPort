package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Renders the real back-tesla OBJ for inventories and held stacks. */
public final class TeslaArmorModItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("armor/back_tesla");

    public TeslaArmorModItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(2.6F, 2.6F, 2.6F);
        } else {
            poseStack.scale(0.65F, 0.65F, 0.65F);
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }
}
