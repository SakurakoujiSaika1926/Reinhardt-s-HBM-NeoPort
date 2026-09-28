package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyFbiDroneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.List;

/** FBI Drone uses the original quadcopter OBJ, not a humanoid placeholder. */
public final class LegacyFbiDroneEntityRenderer extends EntityRenderer<LegacyFbiDroneEntity> {
    private static final long JAVA_RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long JAVA_RANDOM_ADDEND = 0xBL;
    private static final long JAVA_RANDOM_MASK = (1L << 48) - 1L;
    private static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(
            ReinhardtsHBM.id("entity/quadcopter"));
    private static final BlockState STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private static volatile double visualRadius = Double.NaN;

    public LegacyFbiDroneEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(LegacyFbiDroneEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        applyBasePose(poseStack);
        // RenderDrone seeded this fixed yaw from the entity id on every frame.
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (firstJavaRandomDouble(entity.getId()) * 360.0D)));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY,
                ReinhardtsHBM.id("textures/entity/quadcopter.png"), 0.0F, 0.0F);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(LegacyFbiDroneEntity entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        AABB bounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY(), entity.getZ(), visualRadius());
        return EntityRenderBounds.shouldRender(
                entity, bounds, frustum, cameraX, cameraY, cameraZ);
    }

    static void invalidateBounds() {
        visualRadius = Double.NaN;
    }

    private static double visualRadius() {
        double cached = visualRadius;
        if (Double.isFinite(cached)) {
            return cached;
        }
        PoseStack poseStack = new PoseStack();
        applyBasePose(poseStack);
        double radius = BakedBlockGeometrySnapshot.build(
                MachineModelRenderer.model(MODEL), STATE, List.of(poseStack),
                net.minecraft.core.BlockPos.ZERO).maximumRadius();
        // The per-entity Y rotation is norm-preserving, so the exact captured
        // radius covers every deterministic yaw without a universal pose.
        visualRadius = Math.nextUp(radius);
        return visualRadius;
    }

    private static void applyBasePose(PoseStack poseStack) {
        poseStack.translate(0.0F, 0.25F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyFbiDroneEntity entity) {
        return ReinhardtsHBM.id("textures/entity/quadcopter.png");
    }

    private static double firstJavaRandomDouble(long seed) {
        long state = (seed ^ JAVA_RANDOM_MULTIPLIER) & JAVA_RANDOM_MASK;
        state = (state * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
        long high = state >>> 22;
        state = (state * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
        return ((high << 27) + (state >>> 21)) * 0x1.0p-53D;
    }
}
