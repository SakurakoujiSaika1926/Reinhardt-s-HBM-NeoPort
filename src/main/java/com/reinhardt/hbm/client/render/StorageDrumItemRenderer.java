package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact 1.7.10 ItemRenderLibrary path for the Nuclear Waste Disposal Drum. */
public final class StorageDrumItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation DRUM = MachineModelRenderer.standalone("block/machine_storage_drum");

    public StorageDrumItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(DRUM);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ((net.minecraft.world.item.BlockItem) stack.getItem()).getBlock().defaultBlockState();
        poseStack.pushPose();
        switch (context) {
            case GUI -> {
                // ItemRenderBase#INVENTORY, expressed here only for the
                // storage drum, followed by its ItemRenderLibrary entry.
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                poseStack.translate(0.0F, 11.3F, -11.3F);
                poseStack.translate(0.0F, -3.0F, 0.0F);
                poseStack.scale(5.0F, 5.0F, 5.0F);
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
                    "Storage drum had no 1.7.10 ItemRenderType for " + context);
        }
        // RenderStorageDrum's renderCommon.
        poseStack.scale(2.0F, 2.0F, 2.0F);
        // storage_drum.obj was translated +0.5 on X/Z only for modern world
        // block space. The 1.7.10 item mesh was centered on X/Z=0.
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(DRUM), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
