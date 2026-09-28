package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyBlockSpiderEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Exact ModelBlockSpider layout: animated OBJ legs around a rendered block state. */
public final class LegacyBlockSpiderEntityRenderer extends EntityRenderer<LegacyBlockSpiderEntity> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/blockspider.png");
    private static final ModelResourceLocation ODD_LEGS = ModelResourceLocation.standalone(
            ReinhardtsHBM.id("entity/blockspider_odd"));
    private static final ModelResourceLocation EVEN_LEGS = ModelResourceLocation.standalone(
            ReinhardtsHBM.id("entity/blockspider_even"));
    private static final LegacyEntityObjMesh BOUNDS_MODEL = LegacyEntityObjMesh.load(
            ReinhardtsHBM.id("models/obj/entity/blockspider.obj"));
    private static final double LEG_RADIUS = legRadius();
    private static final Map<BlockState, AABB> BODY_BOUNDS = new ConcurrentHashMap<>();

    public LegacyBlockSpiderEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 1.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ODD_LEGS);
        event.register(EVEN_LEGS);
    }

    static void invalidateBounds() {
        BODY_BOUNDS.clear();
    }

    @Override
    public void render(LegacyBlockSpiderEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float limbSwing = entity.walkAnimation.position(partialTick);
        float limbAmount = entity.walkAnimation.speed(partialTick);
        float legRotation = -Mth.cos(limbSwing * 0.6662F * 2.0F) * 0.4F
                * limbAmount * 57.3F;

        poseStack.pushPose();
        // These are ModelBlockSpider's literal per-entity transforms.
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0D, -1.5D, 0.0D);

        poseStack.pushPose();
        poseStack.translate(0.0D, legRotation * 0.005D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(legRotation));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(ODD_LEGS), poseStack,
                bufferSource, Blocks.STONE.defaultBlockState(), packedLight,
                OverlayTexture.NO_OVERLAY, TEXTURE, 0.0F, 0.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, legRotation * -0.005D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-legRotation));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(EVEN_LEGS), poseStack,
                bufferSource, Blocks.STONE.defaultBlockState(), packedLight,
                OverlayTexture.NO_OVERLAY, TEXTURE, 0.0F, 0.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.75D, 0.0D);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(entity.blockState(), poseStack,
                bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyBlockSpiderEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB legs = EntityRenderBounds.radius(
                entity.getX(), entity.getY(), entity.getZ(), LEG_RADIUS);
        AABB body = BODY_BOUNDS.computeIfAbsent(entity.blockState(),
                LegacyBlockSpiderEntityRenderer::captureBodyBounds)
                .move(entity.getX(), entity.getY(), entity.getZ());
        return EntityRenderBounds.shouldRender(entity, legs.minmax(body), frustum,
                cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyBlockSpiderEntity entity) {
        return TEXTURE;
    }

    private static double legRadius() {
        double authoredRadius = 0.0D;
        for (int index = 1; index <= 8; index++) {
            authoredRadius = Math.max(authoredRadius,
                    BOUNDS_MODEL.maximumGroupFaceRadius("Leg" + index));
        }
        // LivingEntity clamps walk input to [0,1]. The authored expression
        // therefore limits |legRotation| to .4*57.3 degrees and its only
        // translation to |rotation*.005| <= .1146. The fixed root translation
        // is 1.5; every other operation is a unit rotation.
        double radius = authoredRadius + 1.5D + 0.4D * 57.3D * 0.005D;
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma64));
    }

    private static AABB captureBodyBounds(BlockState state) {
        PoseStack bodyPose = new PoseStack();
        bodyPose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        bodyPose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        bodyPose.translate(0.0D, -1.5D, 0.0D);
        bodyPose.translate(0.0D, 0.75D, 0.0D);
        return BakedBlockGeometrySnapshot.build(state, List.of(bodyPose),
                net.minecraft.core.BlockPos.ZERO).bounds();
    }
}
