package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.entity.LegacyFallingNukeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.List;

/** Dedicated falling-custom-nuke pose; deliberately not routed through a generic rotation helper. */
public final class LegacyFallingNukeEntityRenderer extends EntityRenderer<LegacyFallingNukeEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/nuke_custom_world");
    private static volatile double modelRadius = Double.NaN;

    public LegacyFallingNukeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.5F;
    }

    @Override
    public void render(LegacyFallingNukeEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        applyFacingPose(poseStack, entity);
        poseStack.translate(-2.0D, 0.0D, 0.0D);
        float pitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTick;
        if (pitch < -80.0F) pitch = 0.0F;
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                Blocks.AIR.defaultBlockState(), packedLight, 0);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyFallingNukeEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        // The bomb pitches around the model origin after this fixed facing and
        // translation.  A sphere using the baked model's exact maximum vertex
        // radius therefore contains every authored vertex at every pitch while
        // remaining centred on the actual translated pivot.
        PoseStack fixedPose = new PoseStack();
        applyFacingPose(fixedPose, entity);
        fixedPose.translate(-2.0D, 0.0D, 0.0D);
        Vector3f pivot = fixedPose.last().pose().transformPosition(new Vector3f());
        double radius = visualRadius();
        AABB bounds = EntityRenderBounds.radius(
                entity.getX() + pivot.x,
                entity.getY() + pivot.y,
                entity.getZ() + pivot.z,
                radius);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum,
                cameraX, cameraY, cameraZ);
    }

    static void invalidateBounds() {
        modelRadius = Double.NaN;
    }

    private static synchronized double visualRadius() {
        if (Double.isFinite(modelRadius)) {
            return modelRadius;
        }
        PoseStack identity = new PoseStack();
        modelRadius = BakedBlockGeometrySnapshot.build(
                MachineModelRenderer.model(MODEL),
                Blocks.AIR.defaultBlockState(),
                List.of(identity),
                BlockPos.ZERO).maximumRadius();
        return modelRadius;
    }

    private static void applyFacingPose(PoseStack poseStack, LegacyFallingNukeEntity entity) {
        switch (entity.facing()) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalStateException("Falling custom nuke has vertical facing");
        }
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyFallingNukeEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
