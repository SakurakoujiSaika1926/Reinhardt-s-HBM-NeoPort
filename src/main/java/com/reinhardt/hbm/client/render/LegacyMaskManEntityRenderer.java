package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyMaskManEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Mask Man's original eight-part OBJ and health-dependent head. */
public final class LegacyMaskManEntityRenderer extends EntityRenderer<LegacyMaskManEntity> {
    private static final ModelResourceLocation TORSO = model("maskman_torso");
    private static final ModelResourceLocation L_ARM = model("maskman_larm");
    private static final ModelResourceLocation R_ARM = model("maskman_rarm");
    private static final ModelResourceLocation L_LEG = model("maskman_lleg");
    private static final ModelResourceLocation R_LEG = model("maskman_rleg");
    private static final ModelResourceLocation HEAD = model("maskman_head");
    private static final ModelResourceLocation SKULL = model("maskman_skull");
    private static final ModelResourceLocation IOU = model("maskman_iou");
    private static final BlockState STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public LegacyMaskManEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    private static ModelResourceLocation model(String name) {
        return ModelResourceLocation.standalone(ReinhardtsHBM.id("entity/" + name));
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(TORSO);
        event.register(L_ARM);
        event.register(R_ARM);
        event.register(L_LEG);
        event.register(R_LEG);
        event.register(HEAD);
        event.register(SKULL);
        event.register(IOU);
    }

    @Override
    public void render(LegacyMaskManEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbAmount = entity.walkAnimation.speed(partialTick);
        float swing = (float) Math.toDegrees(Math.cos(limbSwing / 2.0F + Math.PI) * 1.4F * limbAmount * 0.5F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TORSO), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY);
        renderPart(L_LEG, poseStack, bufferSource, packedLight, -0.5F, 1.75F, -0.5F, swing, false);
        renderPart(R_LEG, poseStack, bufferSource, packedLight, -0.5F, 1.75F, 0.5F, -swing, false);
        renderPart(L_ARM, poseStack, bufferSource, packedLight, -0.5F, 3.75F, -1.5F, swing * 0.25F, false);
        renderPart(R_ARM, poseStack, bufferSource, packedLight, -0.5F, 3.75F, 1.5F, -swing * 0.25F, false);
        poseStack.pushPose();
        poseStack.translate(0.5F, 4.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYHeadRot()));
        ModelResourceLocation head = entity.getHealth() >= entity.getMaxHealth() / 2.0F ? HEAD : SKULL;
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(head), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY);
        if (head == SKULL) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(IOU), poseStack, bufferSource,
                    STATE, packedLight, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight,
                                   float x, float y, float z, float rotation, boolean unused) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyMaskManEntity entity) {
        return ReinhardtsHBM.id("textures/entity/maskman.png");
    }
}
