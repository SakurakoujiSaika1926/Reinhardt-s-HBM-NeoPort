package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyWormBodyEntity;
import com.reinhardt.hbm.entity.LegacyWormHeadEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Old RenderWormHead/RenderWormBody transforms, rendered with their original OBJ geometry and texture maps. */
public class LegacyWormEntityRenderer<E extends Entity> extends EntityRenderer<E> {
    private static final ModelResourceLocation HEAD_MODEL = MachineModelRenderer.standalone("entity/bot_prime_head");
    private static final ModelResourceLocation BODY_MODEL = MachineModelRenderer.standalone("entity/bot_prime_body");
    private static final ResourceLocation HEAD_TEXTURE = ReinhardtsHBM.id("textures/entity/mark_zero_head.png");
    private static final ResourceLocation BODY_TEXTURE = ReinhardtsHBM.id("textures/entity/mark_zero_body.png");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private final boolean head;

    public LegacyWormEntityRenderer(EntityRendererProvider.Context context, boolean head) {
        super(context);
        this.head = head;
        shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(HEAD_MODEL);
        event.register(BODY_MODEL);
    }

    @Override
    public void render(E entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getXRot() - 90.0F));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(head ? HEAD_MODEL : BODY_MODEL), poseStack,
                bufferSource, RENDER_STATE, packedLight, OverlayTexture.NO_OVERLAY,
                head ? HEAD_TEXTURE : BODY_TEXTURE, 0.0F, 0.0F);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(E entity) {
        return head ? HEAD_TEXTURE : BODY_TEXTURE;
    }

    public static final class Head extends LegacyWormEntityRenderer<LegacyWormHeadEntity> {
        public Head(EntityRendererProvider.Context context) { super(context, true); }
    }

    public static final class Body extends LegacyWormEntityRenderer<LegacyWormBodyEntity> {
        public Body(EntityRendererProvider.Context context) { super(context, false); }
    }
}
