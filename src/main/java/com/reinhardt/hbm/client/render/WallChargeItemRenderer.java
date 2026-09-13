package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exact item route for the four {@code BlockChargeBase} variants.
 *
 * <p>1.7.10 rendered these through one {@code RenderBlockRotated} handler:
 * dynamite/miner used {@code charge_dynamite.obj}, while C4/semtex used
 * {@code charge_c4.obj}. Its only target-local operation was Y=-90 degrees,
 * Z=-90 degrees, then Y=0.375. The context matrices below are written here,
 * rather than delegated to a shared helper, because this route must preserve
 * the old block-item pipeline without any automatic fitting.</p>
 */
public final class WallChargeItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation DYNAMITE = model("block/charge_dynamite_world");
    private static final ModelResourceLocation MINER = model("block/charge_miner_world");
    private static final ModelResourceLocation C4 = model("block/charge_c4_world");
    private static final ModelResourceLocation SEMTEX = model("block/charge_semtex_world");

    public WallChargeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            throw new IllegalArgumentException("Wall-charge renderer received a non-block item: " + stack.getItem());
        }

        Block block = blockItem.getBlock();
        ModelResourceLocation model = modelFor(block);
        BlockState state = block.defaultBlockState();

        poseStack.pushPose();
        // The old RenderItem block route applies these literal context poses
        // before RenderBlockRotated receives the matrix.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            case NONE, HEAD -> throw new IllegalArgumentException(
                    "1.7.10 RenderBlockRotated has no " + context + " item context");
        }

        // RenderBlockRotated#renderInventoryBlock, in the source order.
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static ModelResourceLocation modelFor(Block block) {
        if (block == HbmBlocks.CHARGE_DYNAMITE.get()) return DYNAMITE;
        if (block == HbmBlocks.CHARGE_MINER.get()) return MINER;
        if (block == HbmBlocks.CHARGE_C4.get()) return C4;
        if (block == HbmBlocks.CHARGE_SEMTEX.get()) return SEMTEX;
        throw new IllegalArgumentException("Wall-charge renderer received an unsupported block: " + block);
    }

    private static ModelResourceLocation model(String path) {
        return MachineModelRenderer.standalone(path);
    }
}
