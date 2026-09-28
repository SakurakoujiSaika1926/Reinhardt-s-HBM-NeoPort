package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.MinerRocketEntity;
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

/** Unrotated RenderMinerRocket port with its separate original rocket texture. */
public final class MinerRocketEntityRenderer extends EntityRenderer<MinerRocketEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("entity/miner_rocket");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/models/legacy/miner_rocket.png");
    private static final LegacyEntityObjMesh BOUNDS_MODEL = LegacyEntityObjMesh.load(
            ReinhardtsHBM.id("models/obj/entity/miner_rocket.obj"));
    private static final AABB LOCAL_VISUAL_BOUNDS = buildLocalVisualBounds();

    public MinerRocketEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(MinerRocketEntity rocket, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                RENDER_STATE, packedLight, 0, TEXTURE, 0.0F, 0.0F);
        poseStack.popPose();
        super.render(rocket, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(MinerRocketEntity rocket, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(rocket,
                EntityRenderBounds.moveToEntity(LOCAL_VISUAL_BOUNDS, rocket),
                frustum, cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(MinerRocketEntity rocket) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    private static AABB buildLocalVisualBounds() {
        EntityRenderBounds.FloatBuilder bounds = new EntityRenderBounds.FloatBuilder();
        BOUNDS_MODEL.includeAllFacePositions(new PoseStack(), bounds);
        return bounds.buildLocal();
    }
}
