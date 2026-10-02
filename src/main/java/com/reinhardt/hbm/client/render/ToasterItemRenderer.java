package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.ToasterBlock;
import com.reinhardt.hbm.item.ToasterBlockItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class ToasterItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation IRON = MachineModelRenderer.standalone("item/deco_toaster_iron");
    private static final ModelResourceLocation STEEL = MachineModelRenderer.standalone("item/deco_toaster_steel");
    private static final ModelResourceLocation WOOD = MachineModelRenderer.standalone("item/deco_toaster_wood");

    /*
     * toaster.obj has the exact bounds
     *   X [-0.125, 0.125], Y [0, 0.3125], Z [-0.3125, 0.25].
     * Its longest side is therefore 0.5625 blocks.  A raw block-item camera
     * pose renders that deliberately small world model at only 56.25% of a
     * normal item icon.  Normalize this renderer's own model to one item cube;
     * these values are toaster-specific and must not be shared with another
     * machine renderer.
     */
    private static final float MODEL_CENTER_X = 0.0F;
    private static final float MODEL_CENTER_Y = 0.15625F;
    private static final float MODEL_CENTER_Z = -0.03125F;
    private static final float MODEL_MAX_EXTENT = 0.5625F;
    private static final float GUI_SCALE = 0.625F / MODEL_MAX_EXTENT;
    private static final float THIRD_PERSON_SCALE = 0.375F / MODEL_MAX_EXTENT;
    private static final float FIRST_PERSON_SCALE = 0.4F / MODEL_MAX_EXTENT;
    private static final float GROUND_SCALE = 0.25F / MODEL_MAX_EXTENT;
    private static final float FIXED_SCALE = 0.5F / MODEL_MAX_EXTENT;

    public ToasterItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(IRON);
        event.register(STEEL);
        event.register(WOOD);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        int variant = ToasterBlockItem.variant(stack);
        ModelResourceLocation model = variant == 1 ? STEEL : variant == 2 ? WOOD : IRON;
        BlockState state = HbmBlocks.DECO_TOASTER.get().defaultBlockState()
                .setValue(ToasterBlock.VARIANT, variant);

        poseStack.pushPose();
        // ItemRenderer applies (-0.5, -0.5, -0.5) before invoking a BEWLR.
        // Cancel that framework transform first; everything below belongs only
        // to the toaster and is derived from the 1.7.10 RenderItem +
        // RenderToaster matrices.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                // Keep the legacy inventory angles, but fit this 0.5625-block
                // model to the same projected size as a normal block item.
                poseStack.scale(GUI_SCALE, -GUI_SCALE, -GUI_SCALE);
                poseStack.mulPose(Axis.XP.rotationDegrees(210.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                // RenderToaster was a normal 3-D block item.  These are the
                // exact 1.21.1 minecraft:block/block camera operations, copied
                // here because builtin/entity intentionally inherits none.
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(THIRD_PERSON_SCALE, THIRD_PERSON_SCALE, THIRD_PERSON_SCALE);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
                poseStack.scale(THIRD_PERSON_SCALE, THIRD_PERSON_SCALE, THIRD_PERSON_SCALE);
            }
            case FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(FIRST_PERSON_SCALE, FIRST_PERSON_SCALE, FIRST_PERSON_SCALE);
            }
            case FIRST_PERSON_LEFT_HAND -> {
                // block.json specifies +225 degrees for the left-hand entry;
                // ItemTransform applies the mandatory left-hand Y reflection.
                poseStack.mulPose(Axis.YP.rotationDegrees(-225.0F));
                poseStack.scale(FIRST_PERSON_SCALE, FIRST_PERSON_SCALE, FIRST_PERSON_SCALE);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(GROUND_SCALE, GROUND_SCALE, GROUND_SCALE);
            }
            case FIXED -> poseStack.scale(FIXED_SCALE, FIXED_SCALE, FIXED_SCALE);
            case HEAD, NONE -> {
                // minecraft:block/block has no transform for either context.
            }
        }
        // The old renderer rotated the toaster by -90 degrees.  Center the
        // measured model rather than retaining its world-space origin so the
        // normalized icon remains centered in every item display context.
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(-MODEL_CENTER_X, -MODEL_CENTER_Y, -MODEL_CENTER_Z);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
