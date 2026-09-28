package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.SawbladeEntity;
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

/** Direct transform port of RenderSawblade. */
public final class SawbladeEntityRenderer extends EntityRenderer<SawbladeEntity> {
    private static final ModelResourceLocation BLADE = MachineModelRenderer.standalone("block/machine_sawmill_blade");
    private static volatile double visualRadius = Double.NaN;

    public SawbladeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BLADE);
    }

    @Override
    public void render(SawbladeEntity blade, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        switch (Math.floorMod(blade.orientation(), 6)) {
            case 5 -> poseStack.mulPose(yaw(90.0F));
            case 2 -> poseStack.mulPose(yaw(180.0F));
            case 4 -> poseStack.mulPose(yaw(270.0F));
            default -> {
            }
        }
        poseStack.translate(0.0D, 0.0D, -1.0D);
        if (blade.orientation() < 6) {
            float angle = ((blade.tickCount + partialTick) * 12.0F) % 360.0F;
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(angle), 0.0F, 0.0F, -1.0F)));
        }
        poseStack.translate(0.0D, -1.375D, 0.0D);
        BlockState state = HbmBlocks.MACHINE_SAWMILL.get().defaultBlockState();
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BLADE), poseStack, bufferSource, state, packedLight, 0);
        poseStack.popPose();
        super.render(blade, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(SawbladeEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return EntityRenderBounds.shouldRender(entity,
                EntityRenderBounds.radius(entity.getX(), entity.getY(), entity.getZ(), visualRadius()),
                frustum, cameraX, cameraY, cameraZ);
    }

    static void invalidateBounds() {
        visualRadius = Double.NaN;
    }

    private static double visualRadius() {
        double cached = visualRadius;
        if (Double.isFinite(cached)) return cached;
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0D, -1.375D, 0.0D);
        BlockState state = HbmBlocks.MACHINE_SAWMILL.get().defaultBlockState();
        double local = BakedBlockGeometrySnapshot.build(
                MachineModelRenderer.model(BLADE), state, java.util.List.of(poseStack),
                net.minecraft.core.BlockPos.ZERO).maximumRadius();
        visualRadius = Math.nextUp(local + 1.0D);
        return visualRadius;
    }

    @Override
    public ResourceLocation getTextureLocation(SawbladeEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    private static Quaternionf yaw(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }
}
