package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.SoyuzCapsuleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class SoyuzCapsuleEntityRenderer extends EntityRenderer<SoyuzCapsuleEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("entity/soyuz_lander");
    private static final LegacyEntityObjMesh BOUNDS_MODEL = LegacyEntityObjMesh.load(
            ReinhardtsHBM.id("models/obj/entity/soyuz_lander.obj"));
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private static final double SWAY_RADIUS = rotationSafeRadius(
            BOUNDS_MODEL.maximumFaceRadius(0.0D, 7.0D, 0.0D));

    public SoyuzCapsuleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(SoyuzCapsuleEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        double time = entity.level().getGameTime() + partialTick;
        float swayZ = Mth.sin((float) (time * 0.05D)) * 5.0F;
        float swayX = Mth.sin((float) (time * 0.05D + Math.PI * 0.5D)) * 5.0F;
        poseStack.translate(0.0D, 7.0D, 0.0D);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(swayZ));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(swayX));
        poseStack.translate(0.0D, -7.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, RENDER_STATE, packedLight, 0);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(SoyuzCapsuleEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        // render() rotates the authored mesh about the exact (0, 7, 0)
        // pivot.  Rotations preserve every vertex's distance from that pivot.
        AABB bounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY() + 7.0D, entity.getZ(), SWAY_RADIUS);
        return EntityRenderBounds.shouldRender(entity, bounds, frustum,
                cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(SoyuzCapsuleEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    private static double rotationSafeRadius(double radius) {
        double unitRoundoff = 0x1.0p-24;
        double gamma64 = 64.0D * unitRoundoff / (1.0D - 64.0D * unitRoundoff);
        return Math.nextUp(radius * (1.0D + gamma64));
    }
}
