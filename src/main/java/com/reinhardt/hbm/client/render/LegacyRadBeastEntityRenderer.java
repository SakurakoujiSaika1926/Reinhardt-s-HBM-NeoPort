package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyRadBeastEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Radiation Beast uses the old Blaze body and its dedicated texture. */
public final class LegacyRadBeastEntityRenderer
        extends MobRenderer<LegacyRadBeastEntity, BlazeModel<LegacyRadBeastEntity>> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/radbeast.png");
    private static final ResourceLocation MASK_TEXTURE = ReinhardtsHBM.id("textures/models/model_m65_blaze.png");

    public LegacyRadBeastEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new BlazeModel<>(context.bakeLayer(ModelLayers.BLAZE)), 0.5F);
        addLayer(new MaskLayer(this, new LegacyM65BlazeModel(context.bakeLayer(LegacyM65BlazeModel.LAYER))));
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyRadBeastEntity entity) {
        return TEXTURE;
    }

    /** EntityRADBeast#getBrightnessForRender always returned full-bright. */
    @Override
    protected int getBlockLightLevel(LegacyRadBeastEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(LegacyRadBeastEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        Entity victim = entity.getUnfortunateSoul();
        if (victim == null || entity.getY() <= 0.1D) {
            return;
        }
        Vec3 target = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D);
        if (victim instanceof Player) {
            target = target.subtract(0.0D, 1.5D, 0.0D);
        }
        Vec3 origin = entity.position();
        if (target.distanceTo(origin) < 200.0D) {
            LegacyCyberCrabBeamRenderer.render(poseStack, bufferSource, List.of(target),
                    origin, 1.25D, entity.level().getGameTime());
        }
    }

    private static final class MaskLayer extends RenderLayer<LegacyRadBeastEntity, BlazeModel<LegacyRadBeastEntity>> {
        private final LegacyM65BlazeModel maskModel;

        private MaskLayer(RenderLayerParent<LegacyRadBeastEntity, BlazeModel<LegacyRadBeastEntity>> parent,
                          LegacyM65BlazeModel maskModel) {
            super(parent);
            this.maskModel = maskModel;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                           LegacyRadBeastEntity entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            maskModel.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            poseStack.pushPose();
            // ModelM65Blaze explicitly scales the authored mask by 18/16 and then 1.01.
            float legacyScale = 18.0F / 16.0F * 1.01F;
            poseStack.scale(legacyScale, legacyScale, legacyScale);
            maskModel.renderToBuffer(poseStack,
                    bufferSource.getBuffer(RenderType.entityCutoutNoCull(MASK_TEXTURE)),
                    packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), -1);
            poseStack.popPose();
        }
    }
}
