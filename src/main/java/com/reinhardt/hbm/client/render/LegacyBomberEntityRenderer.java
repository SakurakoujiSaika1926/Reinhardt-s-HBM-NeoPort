package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyBomberEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Old RenderBomber transforms, using the original Dornier and B-29 OBJ resources. */
public final class LegacyBomberEntityRenderer extends EntityRenderer<LegacyBomberEntity> {
    private static final ModelResourceLocation DORNIER = MachineModelRenderer.standalone("entity/bomber_dornier");
    private static final ModelResourceLocation B29 = MachineModelRenderer.standalone("entity/bomber_b29");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private static final LegacyEntityObjMesh DORNIER_BOUNDS_MODEL = LegacyEntityObjMesh.load(
            ReinhardtsHBM.id("models/obj/entity/bomber_dornier.obj"));
    private static final LegacyEntityObjMesh B29_BOUNDS_MODEL = LegacyEntityObjMesh.load(
            ReinhardtsHBM.id("models/obj/entity/bomber_b29.obj"));
    private static final double DORNIER_RADIUS = rotationSafeRadius(
            DORNIER_BOUNDS_MODEL.maximumFaceRadius(0.0D, 0.0D, 0.0D) * 5.0D);
    private static final double B29_RADIUS = rotationSafeRadius(
            B29_BOUNDS_MODEL.maximumFaceRadius(0.0D, 0.0D, 0.0D) * (30.0D / 3.1D));

    public LegacyBomberEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(DORNIER);
        event.register(B29);
    }

    @Override
    public void render(LegacyBomberEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getXRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) Math.sin((entity.tickCount + partialTick) * 0.05F) * 10.0F));
        boolean b29 = entity.style() >= 5;
        poseStack.scale(b29 ? 30.0F / 3.1F : 5.0F, b29 ? 30.0F / 3.1F : 5.0F, b29 ? 30.0F / 3.1F : 5.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(b29 ? 180.0F : -90.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(b29 ? B29 : DORNIER), poseStack,
                bufferSource, RENDER_STATE, packedLight, 0);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyBomberEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        double radius = entity.style() >= 5 ? B29_RADIUS : DORNIER_RADIUS;
        AABB bounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY(), entity.getZ(), radius);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum,
                cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyBomberEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    private static double rotationSafeRadius(double radius) {
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma64));
    }
}
