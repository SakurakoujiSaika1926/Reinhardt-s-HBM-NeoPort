package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyBlockSpiderEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact ModelBlockSpider layout: animated OBJ legs around a rendered block state. */
public final class LegacyBlockSpiderEntityRenderer extends EntityRenderer<LegacyBlockSpiderEntity> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/blockspider.png");
    private static final ModelResourceLocation ODD_LEGS = ModelResourceLocation.standalone(
            ReinhardtsHBM.id("entity/blockspider_odd"));
    private static final ModelResourceLocation EVEN_LEGS = ModelResourceLocation.standalone(
            ReinhardtsHBM.id("entity/blockspider_even"));

    public LegacyBlockSpiderEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 1.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ODD_LEGS);
        event.register(EVEN_LEGS);
    }

    @Override
    public void render(LegacyBlockSpiderEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbAmount = entity.walkAnimation.speed(partialTick);
        float legRotation = -Mth.cos(limbSwing * 0.6662F * 2.0F) * 0.4F
                * limbAmount * 57.3F;

        poseStack.pushPose();
        // These are ModelBlockSpider's literal per-entity transforms.
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0D, -1.5D, 0.0D);

        poseStack.pushPose();
        poseStack.translate(0.0D, legRotation * 0.005D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(legRotation));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(ODD_LEGS), poseStack,
                bufferSource, Blocks.STONE.defaultBlockState(), packedLight,
                OverlayTexture.NO_OVERLAY, TEXTURE, 0.0F, 0.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, legRotation * -0.005D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-legRotation));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(EVEN_LEGS), poseStack,
                bufferSource, Blocks.STONE.defaultBlockState(), packedLight,
                OverlayTexture.NO_OVERLAY, TEXTURE, 0.0F, 0.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.75D, 0.0D);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(entity.blockState(), poseStack,
                bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyBlockSpiderEntity entity) {
        return TEXTURE;
    }
}
