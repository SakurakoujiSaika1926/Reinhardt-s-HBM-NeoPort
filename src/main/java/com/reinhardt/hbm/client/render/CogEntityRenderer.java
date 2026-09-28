package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.CogEntity;
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

public class CogEntityRenderer extends EntityRenderer<CogEntity> {
    private static final ModelResourceLocation COG = MachineModelRenderer.standalone("block/machine_stirling_cog");
    private static final ModelResourceLocation COG_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_cog");
    private static volatile double ironRadius = Double.NaN;
    private static volatile double steelRadius = Double.NaN;

    public CogEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(COG);
        event.register(COG_STEEL);
    }

    @Override
    public void render(CogEntity cog, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        switch (Math.floorMod(cog.orientation(), 6)) {
            case 5 -> poseStack.mulPose(yaw(90.0F));
            case 2 -> poseStack.mulPose(yaw(180.0F));
            case 4 -> poseStack.mulPose(yaw(270.0F));
            default -> {
            }
        }

        poseStack.translate(0.0D, 0.0D, -1.0D);
        if (cog.orientation() < 6) {
            float angle = ((cog.tickCount + partialTick) * 12.0F) % 360.0F;
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(angle), 0.0F, 0.0F, -1.0F)));
        }
        poseStack.translate(0.0D, -1.375D, 0.0D);

        BlockState state = cog.meta() == 0
                ? HbmBlocks.MACHINE_STIRLING.get().defaultBlockState()
                : HbmBlocks.MACHINE_STIRLING_STEEL.get().defaultBlockState();
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(cog.meta() == 0 ? COG : COG_STEEL),
                poseStack,
                bufferSource,
                state,
                packedLight,
                0
        );

        poseStack.popPose();
        super.render(cog, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(CogEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        double radius = visualRadius(entity.meta() != 0);
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), radius),
                frustum, cameraX, cameraY, cameraZ);
    }

    static void invalidateBounds() {
        ironRadius = Double.NaN;
        steelRadius = Double.NaN;
    }

    private static double visualRadius(boolean steel) {
        double cached = steel ? steelRadius : ironRadius;
        if (Double.isFinite(cached)) return cached;
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0D, -1.375D, 0.0D);
        BlockState state = steel
                ? HbmBlocks.MACHINE_STIRLING_STEEL.get().defaultBlockState()
                : HbmBlocks.MACHINE_STIRLING.get().defaultBlockState();
        double local = BakedBlockGeometrySnapshot.build(
                MachineModelRenderer.model(steel ? COG_STEEL : COG), state,
                java.util.List.of(poseStack), net.minecraft.core.BlockPos.ZERO).maximumRadius();
        // The animated Z rotation preserves the captured local radius; the
        // preceding fixed -1 Z translation can add exactly one block.
        double radius = Math.nextUp(local + 1.0D);
        if (steel) steelRadius = radius;
        else ironRadius = radius;
        return radius;
    }

    @Override
    public ResourceLocation getTextureLocation(CogEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    private static Quaternionf yaw(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }
}
