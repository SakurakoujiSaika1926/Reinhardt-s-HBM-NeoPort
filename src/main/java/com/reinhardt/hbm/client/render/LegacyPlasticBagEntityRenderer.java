package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyPlasticBagEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Plastic Bag uses the original transparent OBJ and disables face culling. */
public final class LegacyPlasticBagEntityRenderer extends EntityRenderer<LegacyPlasticBagEntity> {
    private static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(
            ReinhardtsHBM.id("entity/plasticbag"));
    private static final BlockState STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public LegacyPlasticBagEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(LegacyPlasticBagEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() + 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getXRot() - 90.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyPlasticBagEntity entity) {
        return ReinhardtsHBM.id("textures/entity/plasticbag.png");
    }
}
