package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.entity.LegacyUndeadSoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Uses the exact vanilla zombie/skeleton texture pair selected by the old renderer. */
public final class LegacyUndeadSoldierEntityRenderer
        extends MobRenderer<LegacyUndeadSoldierEntity, HumanoidModel<LegacyUndeadSoldierEntity>> {
    private static final ResourceLocation ZOMBIE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    private static final ResourceLocation SKELETON = ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");

    public LegacyUndeadSoldierEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyUndeadSoldierEntity soldier) {
        return soldier.isSkeletonVariant() ? SKELETON : ZOMBIE;
    }
}
