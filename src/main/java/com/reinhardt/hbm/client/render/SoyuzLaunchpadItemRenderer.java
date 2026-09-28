package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Exact static groups and ItemRenderBase transforms of RenderLaunchpadSoyuz. */
public final class SoyuzLaunchpadItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation OBJ =
            ReinhardtsHBM.id("models/obj/machine/launchpad_soyuz.obj");
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/models/legacy/launchpad_soyuz.png");
    private static final LegacyEntityObjMesh MODEL = LegacyEntityObjMesh.load(OBJ);

    public SoyuzLaunchpadItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        pose.pushPose();
        applyLegacyItemBase(context, pose);
        if (context == ItemDisplayContext.GUI) {
            pose.translate(6.0D, -1.5D, 0.0D);
            pose.scale(1.125F, 1.125F, 1.125F);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        pose.scale(0.25F, 0.25F, 0.25F);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        group("Launchpad", pose, consumer, packedLight);
        pose.translate(0.0D, 0.0D, 3.25D);
        for (int i = 1; i <= 5; i++) {
            group("Strut" + i, pose, consumer, packedLight);
        }
        pose.popPose();
    }

    private static void applyLegacyItemBase(ItemDisplayContext context, PoseStack pose) {
        if (context == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
            return;
        }
        if (context == ItemDisplayContext.GROUND) {
            pose.scale(1.5F, 1.5F, 1.5F);
        } else {
            pose.translate(0.5D, 0.25D, 0.0D);
        }
        pose.scale(0.25F, 0.25F, 0.25F);
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void group(String name, PoseStack pose, VertexConsumer consumer, int light) {
        MODEL.renderGroup(name, pose, consumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
