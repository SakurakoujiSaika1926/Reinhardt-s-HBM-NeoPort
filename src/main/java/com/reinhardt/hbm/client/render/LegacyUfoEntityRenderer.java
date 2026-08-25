package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyUfoEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Original UFO OBJ, its 2x legacy render scale, and the three layered beam passes. */
public final class LegacyUfoEntityRenderer extends EntityRenderer<LegacyUfoEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("entity/ufo");
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/ufo.png");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public LegacyUfoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(LegacyUfoEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.0D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 5.0F));
        poseStack.scale(2.0F, 2.0F, 2.0F);
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                RENDER_STATE, packedLight, OverlayTexture.NO_OVERLAY, TEXTURE, 0.0F, 0.0F);
        poseStack.popPose();

        if (entity.beamActive()) {
            renderBeam(entity, poseStack, bufferSource);
        }
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void renderBeam(LegacyUfoEntity entity, PoseStack poseStack, MultiBufferSource bufferSource) {
        double length = Math.max(0.0D, entity.getY() - entity.beamGroundY());
        if (length <= 0.0D) return;
        poseStack.pushPose();
        poseStack.translate(0.0D, -length, 0.0D);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        // RenderUFO used a solid spiral core plus two randomized wider passes.
        prism(consumer, poseStack.last(), length, 1.50F, 0x101020);
        prism(consumer, poseStack.last(), length, 0.125F, 0x202060);
        prism(consumer, poseStack.last(), length, 0.0625F, 0x5050C0);
        poseStack.popPose();
    }

    private static void prism(VertexConsumer consumer, PoseStack.Pose pose, double length, float radius, int color) {
        vertex(consumer, pose, -radius, 0.0D, -radius, color);
        vertex(consumer, pose, radius, 0.0D, -radius, color);
        vertex(consumer, pose, radius, length, -radius, color);
        vertex(consumer, pose, -radius, length, -radius, color);
        vertex(consumer, pose, radius, 0.0D, radius, color);
        vertex(consumer, pose, -radius, 0.0D, radius, color);
        vertex(consumer, pose, -radius, length, radius, color);
        vertex(consumer, pose, radius, length, radius, color);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, int color) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, 210)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyUfoEntity entity) {
        return TEXTURE;
    }
}
