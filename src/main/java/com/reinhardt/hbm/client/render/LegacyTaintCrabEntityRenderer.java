package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyTaintCrabEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Taint Crab OBJ parts and the original leg swing. */
public final class LegacyTaintCrabEntityRenderer extends EntityRenderer<LegacyTaintCrabEntity> {
    private static final ModelResourceLocation BODY = model("taintcrab_body");
    private static final ModelResourceLocation LEGS1 = model("taintcrab_legs1");
    private static final ModelResourceLocation LEGS2 = model("taintcrab_legs2");
    private static final BlockState STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public LegacyTaintCrabEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        // RenderTaintCrab used RenderLiving's 1.0F shadow size and only
        // disabled shadow opacity.
        shadowRadius = 1.0F;
    }

    private static ModelResourceLocation model(String name) {
        return ModelResourceLocation.standalone(ReinhardtsHBM.id("entity/" + name));
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(LEGS1);
        event.register(LEGS2);
    }

    @Override
    public void render(LegacyTaintCrabEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        // RenderLivingBase's 1.7.10 rotate/scale/prepare pipeline.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        // RenderLiving's fixed biped mirror was applied before ModelTaintCrab
        // in 1.7.10; reproduce that exact pipeline layer.
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.5078125F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(BODY), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY,
                ReinhardtsHBM.id("textures/entity/taintcrab.png"), 0.0F, 0.0F);
        float swing = (float) (-(Math.cos(entity.walkAnimation.position(partialTick) * .6662F * 2.0F)
                * .4F) * entity.walkAnimation.speed(partialTick) * 57.3F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(swing));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(LEGS1), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY,
                ReinhardtsHBM.id("textures/entity/taintcrab.png"), 0.0F, 0.0F);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-swing));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(LEGS2), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY,
                ReinhardtsHBM.id("textures/entity/taintcrab.png"), 0.0F, 0.0F);
        poseStack.popPose();
        poseStack.popPose();
        LegacyCyberCrabBeamRenderer.render(poseStack, bufferSource, entity.targets(),
                entity.position(), 1.25D, entity.level().getGameTime());
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyTaintCrabEntity entity) {
        return ReinhardtsHBM.id("textures/entity/taintcrab.png");
    }
}
