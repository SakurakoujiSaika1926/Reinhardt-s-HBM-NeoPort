package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Item renderer for the old BAT models.
 *
 * The model bounds and centers are taken from the 1.7.10 OBJ assets. Keeping
 * these values here prevents the generic item auto-fit pass from changing the
 * scale when another OBJ is added or its bounds contain an offset.
 */
public final class LegacyTankItemRenderer extends BlockEntityWithoutLevelRenderer {
    public enum Kind {
        BAT9000,
        BIG_ASS_TANK
    }

    private static final ModelResourceLocation BAT9000_MODEL =
            MachineModelRenderer.standalone("block/machine_bat9000");
    private static final ModelResourceLocation BIG_ASS_TANK_MODEL =
            MachineModelRenderer.standalone("block/machine_bigasstank");

    private final Kind kind;

    public LegacyTankItemRenderer(Kind kind) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.kind = kind;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ModelResourceLocation model = this.kind == Kind.BAT9000 ? BAT9000_MODEL : BIG_ASS_TANK_MODEL;
        BlockState state = this.kind == Kind.BAT9000
                ? HbmBlocks.MACHINE_BAT9000.get().defaultBlockState()
                : HbmBlocks.MACHINE_BIGASSTANK.get().defaultBlockState();

        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            if (this.kind == Kind.BAT9000) {
                poseStack.translate(0.0F, -3.0F, 0.0F);
                poseStack.scale(2.0F, 2.0F, 2.0F);
            } else {
                poseStack.translate(0.0F, -1.0F, 0.0F);
                poseStack.scale(2.5F, 2.5F, 2.5F);
            }
        }
        if (this.kind == Kind.BIG_ASS_TANK) {
            // RenderBigAssTank#getRenderer.renderCommonWithStack().
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
