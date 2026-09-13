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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Mask Man's original eight-part OBJ and health-dependent head. */
public final class LegacyMaskManEntityRenderer extends EntityRenderer<LegacyMaskManEntity> {
    private static final ResourceLocation MASKMAN_TEXTURE = ReinhardtsHBM.id("textures/entity/maskman.png");
    private static final ResourceLocation IOU_TEXTURE = ReinhardtsHBM.id("textures/entity/iou.png");
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
        // RenderMaskMan was constructed with a 1.0F shadow radius; only its
        // opacity was zeroed in the legacy renderer.
        shadowRadius = 1.0F;
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
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        poseStack.pushPose();
        // RenderLiving applies the entity yaw; ModelMaskMan then applies only
        // the three model transforms below. The OBJ is already authored in
        // legacy coordinates; only RenderLiving's fixed mirror is reproduced.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        // RenderLiving's fixed biped mirror is part of the 1.7.10 render
        // pipeline.  ModelMaskMan's 180-degree X rotation is authored on top
        // of this mirror; omitting it leaves the OBJ upside down and below the
        // entity origin.
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        // Exact RenderLivingBase.prepareScale baseline from the local
        // 1.7.10 source, applied before ModelMaskMan.render.
        poseStack.translate(0.0F, -1.5078125F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(swing * -0.1F));
        renderPart(TORSO, poseStack, bufferSource, packedLight, MASKMAN_TEXTURE);
        renderPart(L_LEG, poseStack, bufferSource, packedLight, -0.5F, 1.75F, -0.5F, swing, MASKMAN_TEXTURE);
        renderPart(R_LEG, poseStack, bufferSource, packedLight, -0.5F, 1.75F, 0.5F, -swing, MASKMAN_TEXTURE);
        renderPart(L_ARM, poseStack, bufferSource, packedLight, -0.5F, 3.75F, -1.5F, swing * 0.25F, MASKMAN_TEXTURE);
        renderPart(R_ARM, poseStack, bufferSource, packedLight, -0.5F, 3.75F, 1.5F, -swing * 0.25F, MASKMAN_TEXTURE);
        poseStack.pushPose();
        poseStack.translate(0.5F, 4.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.wrapDegrees(headYaw - bodyYaw)));
        ModelResourceLocation head = entity.getHealth() >= entity.getMaxHealth() / 2.0F ? HEAD : SKULL;
        renderPart(head, poseStack, bufferSource, packedLight, MASKMAN_TEXTURE);
        if (head == SKULL) {
            renderPart(IOU, poseStack, bufferSource, packedLight, IOU_TEXTURE);
        }
        poseStack.popPose();
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight,
                                   float x, float y, float z, float rotation, ResourceLocation texture) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        renderPart(model, poseStack, bufferSource, packedLight, texture);
        poseStack.popPose();
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, ResourceLocation texture) {
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(model), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY, texture, 0.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyMaskManEntity entity) {
        return MASKMAN_TEXTURE;
    }
}
