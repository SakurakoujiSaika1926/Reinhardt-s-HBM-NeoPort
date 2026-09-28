package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.MeteorEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class MeteorEntityRenderer extends EntityRenderer<MeteorEntity> {
    private static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("reinhardtshbm", "block/block_meteor_molten"));
    private static final BlockState RENDER_STATE = HbmBlocks.BLOCK_METEOR_MOLTEN.get().defaultBlockState();
    // The rendered block cube is centered then scaled by five; every possible
    // spin remains inside its exact corner radius.
    private static final double VISUAL_RADIUS = Math.nextUp(2.5D * Math.sqrt(3.0D));

    public MeteorEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(MeteorEntity meteor, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0D), 1.0F, 0.0F, 0.0F)));
        float spin = ((meteor.tickCount % 360) + partialTick) * 10.0F;
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(spin), 1.0F, 1.0F, 1.0F)));
        poseStack.scale(5.0F, 5.0F, 5.0F);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        MachineModelRenderer.renderUnculledFullBright(MachineModelRenderer.model(MODEL), poseStack, bufferSource, RENDER_STATE, 0);
        poseStack.popPose();
        super.render(meteor, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(MeteorEntity meteor, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB bounds = EntityRenderBounds.radius(
                meteor.getX(), meteor.getY(), meteor.getZ(), VISUAL_RADIUS);
        return EntityRenderBounds.shouldRender(meteor, bounds, frustum,
                cameraX, cameraY, cameraZ);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
