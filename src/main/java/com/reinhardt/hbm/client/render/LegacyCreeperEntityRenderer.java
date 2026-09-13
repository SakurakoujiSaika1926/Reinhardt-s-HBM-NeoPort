package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyCreeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** The five 1.7.10 creepers use the vanilla creeper model with their own skin. */
public final class LegacyCreeperEntityRenderer<T extends LegacyCreeperEntity>
        extends MobRenderer<T, CreeperModel<T>> {
    private final ResourceLocation texture;
    private final ResourceLocation poweredTexture;
    private final float swellMod;

    public LegacyCreeperEntityRenderer(EntityRendererProvider.Context context,
                                        String textureName, String poweredTextureName) {
        this(context, textureName, poweredTextureName, 1.0F);
    }

    public LegacyCreeperEntityRenderer(EntityRendererProvider.Context context,
                                        String textureName, String poweredTextureName,
                                        float swellMod) {
        super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        texture = ReinhardtsHBM.id("textures/entity/" + textureName + ".png");
        poweredTexture = ReinhardtsHBM.id("textures/entity/" + poweredTextureName + ".png");
        this.swellMod = swellMod;
        addLayer(new PoweredLayer(this, poweredTexture));
    }

    /**
     * Direct conversion of RenderCreeperUniversal#preRenderCallback.
     * The nuclear creeper passes the old renderer's explicit swellMod=5;
     * every other variant keeps the renderer default of 1.
     */
    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        float swell = entity.getSwelling(partialTick);
        float flash = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;
        swell = Mth.clamp(swell, 0.0F, 1.0F);
        swell *= swell;
        swell *= swell;
        swell *= swell;
        swell *= swellMod;
        float horizontal = (1.0F + swell * 0.4F) * flash;
        float vertical = (1.0F + swell * 0.1F) / flash;
        poseStack.scale(horizontal, vertical, horizontal);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static final class PoweredLayer<T extends LegacyCreeperEntity>
            extends RenderLayer<T, CreeperModel<T>> {
        private final ResourceLocation texture;

        private PoweredLayer(RenderLayerParent<T, CreeperModel<T>> parent, ResourceLocation texture) {
            super(parent);
            this.texture = texture;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                           T entity, float limbSwing, float limbSwingAmount, float partialTick,
                           float ageInTicks, float netHeadYaw, float headPitch) {
            if (!entity.isPowered()) {
                return;
            }
            float scroll = (entity.tickCount + partialTick) * 0.01F;
            CreeperModel<T> parentModel = this.getParentModel();
            parentModel.renderToBuffer(poseStack,
                    bufferSource.getBuffer(RenderType.energySwirl(texture, scroll % 1.0F, scroll % 1.0F)),
                    0xF000F0, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 0xFFFFFFFF);
        }
    }
}
