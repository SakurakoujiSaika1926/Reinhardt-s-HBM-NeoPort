package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.entity.LegacyUndeadSoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

/** Uses the exact vanilla zombie/skeleton texture pair selected by the old renderer. */
public final class LegacyUndeadSoldierEntityRenderer
        extends MobRenderer<LegacyUndeadSoldierEntity, HumanoidModel<LegacyUndeadSoldierEntity>> {
    private static final ResourceLocation ZOMBIE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    private static final ResourceLocation SKELETON = ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
    private final HumanoidModel<LegacyUndeadSoldierEntity> zombieModel;
    private final LegacyUndeadSoldierModel skeletonModel;

    public LegacyUndeadSoldierEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        this.zombieModel = this.model;
        this.skeletonModel = new LegacyUndeadSoldierModel(context.bakeLayer(LegacyUndeadSoldierModel.LAYER));
        // RenderBiped rendered the soldier's four Taurus armor stacks. Keep
        // the vanilla armor layer instead of silently dropping equipped armor.
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public void render(LegacyUndeadSoldierEntity soldier, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // RenderUndeadSoldier.preRenderCallback selected the complete model
        // per synchronized TYPE value; no universal model or transform.
        this.model = soldier.isSkeletonVariant() ? skeletonModel : zombieModel;
        super.render(soldier, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyUndeadSoldierEntity soldier) {
        return soldier.isSkeletonVariant() ? SKELETON : ZOMBIE;
    }
}
