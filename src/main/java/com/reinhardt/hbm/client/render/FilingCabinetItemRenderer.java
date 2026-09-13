package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.FilingCabinetBlock;
import com.reinhardt.hbm.item.FilingCabinetBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact 1.7.10 RenderFileCabinet item path, kept out of the shared fit renderer. */
public final class FilingCabinetItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation GREEN =
            MachineModelRenderer.standalone("block/filing_cabinet_green_item");
    private static final ModelResourceLocation STEEL =
            MachineModelRenderer.standalone("block/filing_cabinet_steel_item");

    public FilingCabinetItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(GREEN);
        event.register(STEEL);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof FilingCabinetBlockItem cabinet)) {
            return;
        }

        int variant = cabinet.variant(stack);
        ModelResourceLocation location = variant == 1 ? STEEL : GREEN;
        BakedModel model = MachineModelRenderer.model(location);
        BlockState state = cabinet.getBlock().defaultBlockState()
                .setValue(FilingCabinetBlock.MATERIAL, variant);

        poseStack.pushPose();
        switch (context) {
            case GUI -> {
                // ItemRenderBase#INVENTORY, followed by RenderFileCabinet's
                // renderInventory() transform.
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                poseStack.translate(0.0F, 11.3F, -11.3F);
                poseStack.translate(-1.0F, 0.5F, -1.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(4.0F, 4.0F, 4.0F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                // ItemRenderBase#EQUIPPED.
                poseStack.translate(0.5F, 0.25F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                // ItemRenderBase#EQUIPPED_FIRST_PERSON.
                poseStack.translate(0.5F, 0.25F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case GROUND -> {
                // ItemRenderBase#ENTITY: 1.5 * 0.25, then its Y rotation.
                poseStack.scale(0.375F, 0.375F, 0.375F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case NONE, HEAD, FIXED -> throw new IllegalArgumentException(
                    "RenderFileCabinet had no 1.7.10 ItemRenderType for " + context);
        }

        // RenderFileCabinet#getRenderer().renderCommonWithStack().
        poseStack.translate(0.0F, -1.25F, 0.0F);
        poseStack.scale(2.75F, 2.75F, 2.75F);
        MachineModelRenderer.renderUnculledCutoutNoCull(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
