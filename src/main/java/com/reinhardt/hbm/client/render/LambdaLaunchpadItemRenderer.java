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

import java.util.List;

/** Exact ItemRenderBase pose and static Lambda pad parts. */
public final class LambdaLaunchpadItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation OBJ =
            ReinhardtsHBM.id("models/obj/machine/launchpad_lambda.obj");
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/models/legacy/launchpad_lambda.png");
    private static final LegacyEntityObjMesh MODEL = LegacyEntityObjMesh.load(OBJ);
    private static final List<String> LOWER = List.of("Silo", "DoorLeft", "DoorRight");
    private static final List<String> UPPER = List.of(
            "Erector", "Rotor",
            "PivotLower1", "ClampLower1", "PivotLower2", "ClampLower2",
            "PivotUpper1", "ClampUpper1", "PivotUpper2", "ClampUpper2");

    public LambdaLaunchpadItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        pose.pushPose();
        applyLegacyItemContext(context, pose);
        if (context == ItemDisplayContext.GUI) {
            pose.translate(0.0D, -3.5D, 0.0D);
            pose.scale(1.75F, 1.75F, 1.75F);
        }
        pose.scale(0.25F, 0.25F, 0.25F);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (String group : LOWER) {
            MODEL.renderGroup(group, pose, consumer, packedLight,
                    OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
        pose.translate(0.0D, 2.0D, 0.0D);
        for (String group : UPPER) {
            MODEL.renderGroup(group, pose, consumer, packedLight,
                    OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }
        pose.popPose();
    }

    private static void applyLegacyItemContext(ItemDisplayContext context, PoseStack pose) {
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
}
