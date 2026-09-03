package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.block.SawmillBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/** Renders the complete legacy OBJ assembly for machine items. */
public final class LegacyMachineItemRenderer extends BlockEntityWithoutLevelRenderer {
    public LegacyMachineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        String legacyId = legacyObjId(stack);
        if (legacyId != null) {
            poseStack.pushPose();
            applyItemRenderBasePose(context, poseStack);
            LegacyMachineBlockEntityRenderer.renderItemLegacy(legacyId,
                    HbmBlocks.MACHINE_ANNIHILATOR.get().defaultBlockState(), poseStack, bufferSource,
                    packedLight, packedOverlay, context == ItemDisplayContext.GUI);
            poseStack.popPose();
            return;
        }
        poseStack.pushPose();
        applyItemRenderBasePose(context, poseStack);
        if (stack.is(HbmBlocks.MACHINE_PYROOVEN.get().asItem())) {
            if (context == ItemDisplayContext.GUI) {
                poseStack.translate(0.0F, -1.0F, 0.0F);
                poseStack.scale(3.5F, 3.5F, 3.5F);
            }
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(yaw(90.0F));
            LegacyMachineBlockEntityRenderer.renderItemPyro(HbmBlocks.MACHINE_PYROOVEN.get().defaultBlockState(), poseStack,
                    bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_AUTOSAW.get().asItem())) {
            // RenderAutosaw#getRenderer: translate(0,-3.5,-3), scale 5,
            // then -90 degrees around Y. The model is anchored at the saw
            // base, so keep the original negative-Z inventory offset.
            if (context == ItemDisplayContext.GUI) {
                poseStack.translate(0.0F, -3.5F, -3.0F);
                poseStack.scale(5.0F, 5.0F, 5.0F);
            }
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(yaw(-90.0F));
            LegacyMachineBlockEntityRenderer.renderItemAutosaw(HbmBlocks.MACHINE_AUTOSAW.get().defaultBlockState(), poseStack,
                    bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_SAWMILL.get().asItem())) {
            // RenderSawmill: translate(0,-1.5), scale(3.25), rotateY(90).
            if (context == ItemDisplayContext.GUI) {
                poseStack.translate(0.0F, -1.5F, 0.0F);
                poseStack.scale(3.25F, 3.25F, 3.25F);
            }
            // RenderSawmill#getRenderer applies +90 degrees around Y.
            poseStack.mulPose(yaw(90.0F));
            LegacyMachineBlockEntityRenderer.renderItemSawmill(HbmBlocks.MACHINE_SAWMILL.get().defaultBlockState(), SawmillBlock.hasBlade(stack), poseStack,
                    bufferSource, packedLight, packedOverlay);
        } else if (stack.is(HbmBlocks.MACHINE_RTG_GREY.get().asItem())) {
            // RenderRTGBlock#renderInventoryBlock offsets its Gen group by
            // -0.5 on Y. It has no RTG-specific scale transform.
            poseStack.translate(0.0F, -0.5F, 0.0F);
            LegacyMachineBlockEntityRenderer.renderItemRtg(HbmBlocks.MACHINE_RTG_GREY.get().defaultBlockState(), poseStack,
                    bufferSource, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static Quaternionf yaw(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    /**
     * Exact modern equivalent of the common 1.7.10 ItemRenderBase pose.  The
     * individual machine branches below retain their own legacy transforms.
     */
    static void applyItemRenderBasePose(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            return;
        }

        poseStack.translate(0.5F, 0.25F, 0.0F);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static String legacyObjId(ItemStack stack) {
        if (stack.is(HbmBlocks.MACHINE_ANNIHILATOR.get().asItem())) return "machine_annihilator";
        if (stack.is(HbmBlocks.MACHINE_FORCEFIELD.get().asItem())) return "machine_forcefield";
        if (stack.is(HbmBlocks.MACHINE_MISSILE_ASSEMBLY.get().asItem())) return "machine_missile_assembly";
        if (stack.is(HbmBlocks.MACHINE_ORBUS.get().asItem())) return "machine_orbus";
        if (stack.is(HbmBlocks.MACHINE_PRECASS.get().asItem())) return "machine_precass";
        if (stack.is(HbmBlocks.MACHINE_RADAR.get().asItem())) return "machine_radar";
        if (stack.is(HbmBlocks.MACHINE_RADAR_LARGE.get().asItem())) return "machine_radar_large";
        if (stack.is(HbmBlocks.MACHINE_RADGEN.get().asItem())) return "machine_radgen";
        if (stack.is(HbmBlocks.MACHINE_RADIOLYSIS.get().asItem())) return "machine_radiolysis";
        if (stack.is(HbmBlocks.MACHINE_REACTOR_BREEDING.get().asItem())) return "machine_reactor_breeding";
        if (stack.is(HbmBlocks.MACHINE_TURBOFAN.get().asItem())) return "machine_turbofan";
        if (stack.is(HbmBlocks.MACHINE_THRESHER.get().asItem())) return "machine_thresher";
        if (stack.is(HbmBlocks.MACHINE_LPW2.get().asItem())) return "machine_lpw2";
        if (stack.is(HbmBlocks.MACHINE_TURBINEGAS.get().asItem())) return "machine_turbinegas";
        return null;
    }
}
