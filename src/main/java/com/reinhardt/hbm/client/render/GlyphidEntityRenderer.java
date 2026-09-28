package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.GlyphidEntity;
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
import org.joml.Quaternionf;

import java.util.Map;

/** Direct port of RenderGlyphid's single OBJ assembly and animation transforms. */
public final class GlyphidEntityRenderer<T extends GlyphidEntity> extends EntityRenderer<T> {
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private static final ResourceLocation INFESTATION = ReinhardtsHBM.id("textures/entity/glyphid_infestation.png");
    private static final LegacyEntityObjMesh BOUNDS_MODEL = LegacyEntityObjMesh.load(
            ReinhardtsHBM.id("models/obj/entity/glyphid.obj"));
    private static final double ANIMATED_MODEL_RADIUS = animatedModelRadius();
    private static final Map<String, ModelResourceLocation> PARTS = Map.ofEntries(
            entry("Body"),
            entry("ArmorFront"), entry("ArmorLeft"), entry("ArmorRight"),
            entry("ArmLeftUpper"), entry("ArmLeftMid"), entry("ArmLeftLower"), entry("ArmLeftArmor"),
            entry("ArmRightUpper"), entry("ArmRightMid"), entry("ArmRightLower"), entry("ArmRightArmor"),
            entry("JawTop"), entry("JawLeft"), entry("JawRight"),
            entry("LegLeftUpper"), entry("LegLeftLower"), entry("LegRightUpper"), entry("LegRightLower")
    );

    private static Map.Entry<String, ModelResourceLocation> part(String name) {
        return entry(name);
    }

    private static Map.Entry<String, ModelResourceLocation> entry(String name) {
        return Map.entry(name, MachineModelRenderer.standalone("entity/glyphid_" + name.toLowerCase(java.util.Locale.ROOT)));
    }

    public GlyphidEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        PARTS.values().forEach(event::register);
    }

    @Override
    public void render(T glyphid, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // RenderLivingBase.rotateCorpse in 1.7.10 rotated the complete model
        // by the interpolated body yaw before ModelGlyphid.render(). This
        // renderer intentionally owns that exact transform because it does
        // not inherit LivingEntityRenderer's rotation pipeline.
        float bodyYaw = Mth.rotLerp(partialTick, glyphid.yBodyRotO, glyphid.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        // RenderLiving applies this model-space conversion before ModelGlyphid.render().
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0D, -1.5078125D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        float scale = (float) glyphid.modelScale();
        poseStack.scale(scale, scale, scale);
        float walkCycle = glyphid.walkAnimation.position(partialTick);
        float biteProgress = Mth.lerp(partialTick, glyphid.oAttackAnim, glyphid.attackAnim);
        renderAssembly(glyphid, walkCycle, biteProgress, poseStack, bufferSource, packedLight, texture(glyphid));
        if (glyphid.getSubtype() == GlyphidEntity.TYPE_INFECTED) {
            renderAssembly(glyphid, walkCycle, biteProgress, poseStack, bufferSource, packedLight, INFESTATION);
        }
        poseStack.popPose();
        super.render(glyphid, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        // The two fixed legacy-pipeline translations are outside the
        // per-variant scale; all authored joint motion is contained in the
        // precomputed model-space sphere below.
        double radius = 1.5078125D + 1.5D
                + entity.modelScale() * ANIMATED_MODEL_RADIUS;
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), radius),
                frustum, cameraX, cameraY, cameraZ);
    }

    private static void renderAssembly(GlyphidEntity glyphid, float walkCycle, float biteProgress,
                                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                       ResourceLocation texture) {
        double cy0 = Math.sin(walkCycle % (Math.PI * 2.0D));
        double cy1 = Math.sin(walkCycle % (Math.PI * 2.0D) - Math.PI * 0.5D);
        double cy2 = Math.sin(walkCycle % (Math.PI * 2.0D) - Math.PI);
        double cy3 = Math.sin(walkCycle % (Math.PI * 2.0D) - Math.PI * 0.75D);
        double bite = Mth.clamp((float) Math.sin(biteProgress * Math.PI * 2.0D - Math.PI * 0.5D), 0.0F, 1.0F) * 20.0D;
        double headTilt = Math.sin(biteProgress * Math.PI) * 30.0D;

        renderPart("Body", texture, poseStack, bufferSource, packedLight);
        if (glyphid.hasArmor(0)) renderPart("ArmorFront", texture, poseStack, bufferSource, packedLight);
        if (glyphid.hasArmor(1)) renderPart("ArmorLeft", texture, poseStack, bufferSource, packedLight);
        if (glyphid.hasArmor(2)) renderPart("ArmorRight", texture, poseStack, bufferSource, packedLight);

        poseStack.pushPose();
        poseStack.translate(0.25D, 0.625D, 0.0625D);
        poseStack.mulPose(Axis.YP.rotationDegrees(10.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (35.0D + cy1 * 20.0D)));
        poseStack.translate(-0.25D, -0.625D, -0.0625D);
        renderPart("ArmLeftUpper", texture, poseStack, bufferSource, packedLight);
        poseStack.translate(0.25D, 0.625D, 0.4375D);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (-75.0D - cy1 * 20.0D + cy0 * 20.0D)));
        poseStack.translate(-0.25D, -0.625D, -0.4375D);
        renderPart("ArmLeftMid", texture, poseStack, bufferSource, packedLight);
        poseStack.translate(0.25D, 0.625D, 0.9375D);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (90.0D - cy0 * 45.0D)));
        poseStack.translate(-0.25D, -0.625D, -0.9375D);
        renderPart("ArmLeftLower", texture, poseStack, bufferSource, packedLight);
        if (glyphid.hasArmor(3)) renderPart("ArmLeftArmor", texture, poseStack, bufferSource, packedLight);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.25D, 0.625D, 0.0625D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-10.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (35.0D + cy2 * 20.0D)));
        poseStack.translate(0.25D, -0.625D, -0.0625D);
        renderPart("ArmRightUpper", texture, poseStack, bufferSource, packedLight);
        poseStack.translate(-0.25D, 0.625D, 0.4375D);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (-75.0D - cy2 * 20.0D + cy3 * 20.0D)));
        poseStack.translate(0.25D, -0.625D, -0.4375D);
        renderPart("ArmRightMid", texture, poseStack, bufferSource, packedLight);
        poseStack.translate(-0.25D, 0.625D, 0.9375D);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (90.0D - cy3 * 45.0D)));
        poseStack.translate(0.25D, -0.625D, -0.9375D);
        renderPart("ArmRightLower", texture, poseStack, bufferSource, packedLight);
        if (glyphid.hasArmor(4)) renderPart("ArmRightArmor", texture, poseStack, bufferSource, packedLight);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.5D, 0.25D);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) headTilt));
        poseStack.translate(0.0D, -0.5D, -0.25D);
        poseStack.pushPose();
        pivot(poseStack, 0.0D, 0.5D, 0.25D, Axis.XP.rotationDegrees((float) -bite));
        renderPart("JawTop", texture, poseStack, bufferSource, packedLight);
        poseStack.popPose();
        poseStack.pushPose();
        pivot(poseStack, 0.0D, 0.5D, 0.25D, Axis.YP.rotationDegrees((float) bite));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) bite));
        renderPart("JawLeft", texture, poseStack, bufferSource, packedLight);
        poseStack.popPose();
        poseStack.pushPose();
        pivot(poseStack, 0.0D, 0.5D, 0.25D, Axis.YP.rotationDegrees((float) -bite));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) bite));
        renderPart("JawRight", texture, poseStack, bufferSource, packedLight);
        poseStack.popPose();
        poseStack.popPose();

        double steppy = 15.0D;
        double bend = 60.0D;
        for (int i = 0; i < 3; i++) {
            double c0 = cy0 * (i == 1 ? -1.0D : 1.0D);
            double c1 = cy1 * (i == 1 ? -1.0D : 1.0D);
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.25D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (i * 30.0D - 15.0D + c0 * 7.5D)));
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) (steppy + c1 * steppy)));
            poseStack.translate(0.0D, -0.25D, 0.0D);
            renderPart("LegLeftUpper", texture, poseStack, bufferSource, packedLight);
            poseStack.translate(0.5625D, 0.25D, 0.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) (-bend - c1 * steppy)));
            poseStack.translate(-0.5625D, -0.25D, 0.0D);
            renderPart("LegLeftLower", texture, poseStack, bufferSource, packedLight);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(0.0D, 0.25D, 0.0D);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (i * 30.0D - 45.0D + c0 * 7.5D)));
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) (-steppy + c1 * steppy)));
            poseStack.translate(0.0D, -0.25D, 0.0D);
            renderPart("LegRightUpper", texture, poseStack, bufferSource, packedLight);
            poseStack.translate(-0.5625D, 0.25D, 0.0D);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) (bend - c1 * steppy)));
            poseStack.translate(0.5625D, -0.25D, 0.0D);
            renderPart("LegRightLower", texture, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }
    }

    private static void pivot(PoseStack poseStack, double x, double y, double z, Quaternionf rotation) {
        poseStack.translate(x, y, z);
        poseStack.mulPose(rotation);
        poseStack.translate(-x, -y, -z);
    }

    private static void renderPart(String name, ResourceLocation texture, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight) {
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(PARTS.get(name)), poseStack, bufferSource,
                RENDER_STATE, packedLight, OverlayTexture.NO_OVERLAY, texture, 0.0F, 0.0F);
    }

    private static ResourceLocation texture(GlyphidEntity glyphid) {
        return ReinhardtsHBM.id("textures/entity/" + glyphid.textureName() + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(GlyphidEntity glyphid) {
        return texture(glyphid);
    }

    private static double animatedModelRadius() {
        double rawRadius = BOUNDS_MODEL.maximumFaceRadius(0.0D, 0.0D, 0.0D);
        double armPivots = 2.0D * (
                length(0.25D, 0.625D, 0.0625D)
                        + length(0.25D, 0.625D, 0.4375D)
                        + length(0.25D, 0.625D, 0.9375D));
        double jawPivots = 4.0D * length(0.0D, 0.5D, 0.25D);
        double legPivots = 2.0D * (
                length(0.0D, 0.25D, 0.0D)
                        + length(0.5625D, 0.25D, 0.0D));
        // A pivot conjugation T(p) R T(-p) can increase distance from the
        // model origin by at most 2|p|. These are every nested pivot chain in
        // renderAssembly; sine-driven angles themselves cannot change radii.
        double radius = rawRadius + Math.max(armPivots, Math.max(jawPivots, legPivots));
        double unitRoundoff = 0x1.0p-24;
        double gamma128 = 128.0D * unitRoundoff / (1.0D - 128.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma128));
    }

    private static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
