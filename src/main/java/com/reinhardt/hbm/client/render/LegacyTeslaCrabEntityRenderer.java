package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyTeslaCrabEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Tesla Crab OBJ parts and the original leg swing. */
public final class LegacyTeslaCrabEntityRenderer extends EntityRenderer<LegacyTeslaCrabEntity> {
    private static final ModelResourceLocation BODY = model("teslacrab_body");
    private static final ModelResourceLocation FRONT = model("teslacrab_front");
    private static final ModelResourceLocation BACK = model("teslacrab_back");
    private static final BlockState STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public LegacyTeslaCrabEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    private static ModelResourceLocation model(String name) {
        return ModelResourceLocation.standalone(ReinhardtsHBM.id("entity/" + name));
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(FRONT);
        event.register(BACK);
    }

    @Override
    public void render(LegacyTeslaCrabEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BODY), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY);
        float swing = (float) (-(Math.cos(entity.walkAnimation.position(partialTick) * .6662F * 2.0F)
                * .4F) * entity.walkAnimation.speed(partialTick) * 57.3F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(swing));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FRONT), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-swing));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BACK), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        poseStack.popPose();
        LegacyCyberCrabBeamRenderer.render(poseStack, bufferSource, entity.targets(),
                entity.position(), 1.0D, entity.level().getGameTime());
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyTeslaCrabEntity entity) {
        return ReinhardtsHBM.id("textures/entity/teslacrab.png");
    }
}
