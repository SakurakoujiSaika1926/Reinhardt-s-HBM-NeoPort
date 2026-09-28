package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyBobmazonEntity;
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

/** Renderer for the original miner-rocket-derived Bobmazon delivery pod. */
public final class LegacyBobmazonEntityRenderer extends EntityRenderer<LegacyBobmazonEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("entity/bobmazon_drop");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/models/legacy/bobmazon.png");
    private static final LegacyEntityObjMesh BOUNDS_MODEL = LegacyEntityObjMesh.load(
            ReinhardtsHBM.id("models/obj/entity/bobmazon_drop.obj"));
    private static final AABB LOCAL_VISUAL_BOUNDS = buildLocalVisualBounds();
    public LegacyBobmazonEntityRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = 0.0F; }
    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) { event.register(MODEL); }
    @Override public void render(LegacyBobmazonEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // RenderMinerRocket applies this exact entity-specific flip for Bobmazon.
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                RENDER_STATE, packedLight, 0, TEXTURE, 0.0F, 0.0F);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }
    @Override public boolean shouldRender(LegacyBobmazonEntity entity, Frustum frustum,
                                          double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.moveToEntity(LOCAL_VISUAL_BOUNDS, entity),
                frustum, cameraX, cameraY, cameraZ);
    }
    @Override public ResourceLocation getTextureLocation(LegacyBobmazonEntity entity) { return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS; }

    private static AABB buildLocalVisualBounds() {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        EntityRenderBounds.FloatBuilder bounds = new EntityRenderBounds.FloatBuilder();
        BOUNDS_MODEL.includeAllFacePositions(poseStack, bounds);
        return bounds.buildLocal();
    }
}
