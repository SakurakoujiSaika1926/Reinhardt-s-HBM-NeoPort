package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyCreeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;

/** The five 1.7.10 creepers use the vanilla creeper model with their own skin. */
public final class LegacyCreeperEntityRenderer<T extends LegacyCreeperEntity>
        extends MobRenderer<T, CreeperModel<T>> {
    private final ResourceLocation texture;
    private final ResourceLocation poweredTexture;
    private final float swellMod;

    public LegacyCreeperEntityRenderer(EntityRendererProvider.Context context,
                                        String textureName, String poweredTextureName) {
        this(context, textureName, poweredTextureName, 1.0F);
    }

    public LegacyCreeperEntityRenderer(EntityRendererProvider.Context context,
                                        String textureName, String poweredTextureName,
                                        float swellMod) {
        super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        texture = ReinhardtsHBM.id("textures/entity/" + textureName + ".png");
        poweredTexture = ReinhardtsHBM.id("textures/entity/" + poweredTextureName + ".png");
        this.swellMod = swellMod;
        addLayer(new PoweredLayer(this, poweredTexture));
    }

    /**
     * Direct conversion of RenderCreeperUniversal#preRenderCallback.
     * The nuclear creeper passes the old renderer's explicit swellMod=5;
     * every other variant keeps the renderer default of 1.
     */
    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        float swell = entity.getSwelling(partialTick);
        float flash = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;
        swell = Mth.clamp(swell, 0.0F, 1.0F);
        swell *= swell;
        swell *= swell;
        swell *= swell;
        swell *= swellMod;
        float horizontal = (1.0F + swell * 0.4F) * flash;
        float vertical = (1.0F + swell * 0.1F) / flash;
        poseStack.scale(horizontal, vertical, horizontal);
    }

    @Override
    public boolean shouldRender(T entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        double radius = visualRadius(entity);
        AABB bounds = EntityRenderBounds.radius(
                entity.getX(), entity.getY(), entity.getZ(), Math.nextUp(radius));
        if (EntityRenderBounds.shouldRender(entity, bounds, frustum,
                cameraX, cameraY, cameraZ)) {
            return true;
        }
        Entity holder = entity instanceof Leashable leashable ? leashable.getLeashHolder() : null;
        return holder != null
                && EntityRenderBounds.isWithinRenderDistance(
                        entity, bounds, cameraX, cameraY, cameraZ)
                && frustum.isVisible(holder.getBoundingBoxForCulling());
    }

    /**
     * Proved envelope of the current 1.21.1 CreeperModel layer under every
     * transform executed by LivingEntityRenderer and this renderer. Head and
     * leg rotations are covered around their authored pivots; the unrotated
     * body is evaluated corner by corner.
     */
    private double visualRadius(T entity) {
        // getSwelling(partialTick) is linear between these endpoints. The
        // sine term is therefore bounded exactly by |sin| <= 1 over the pass.
        double rawMaximum = Math.max(Math.abs(entity.getSwelling(0.0F)),
                Math.abs(entity.getSwelling(1.0F)));
        double clampedMaximum = Math.max(
                Mth.clamp(entity.getSwelling(0.0F), 0.0F, 1.0F),
                Mth.clamp(entity.getSwelling(1.0F), 0.0F, 1.0F));
        double poweredSwell = clampedMaximum;
        poweredSwell *= poweredSwell;
        poweredSwell *= poweredSwell;
        poweredSwell *= poweredSwell;
        poweredSwell *= Math.abs((double) swellMod);

        double flashMaximum = 1.0D + rawMaximum * 0.01D;
        double flashMinimum = 1.0D - rawMaximum * 0.01D;
        double horizontal = (1.0D + poweredSwell * 0.4D) * flashMaximum;
        double vertical = (1.0D + poweredSwell * 0.1D) / flashMinimum;

        double modelRadius = 0.0D;
        modelRadius = Math.max(modelRadius, animatedPartRadius(horizontal, vertical,
                0.0D, 6.0D, 0.0D, -4.0D, -8.0D, -4.0D, 8.0D, 8.0D, 8.0D));
        modelRadius = Math.max(modelRadius, fixedPartRadius(horizontal, vertical,
                0.0D, 6.0D, 0.0D, -4.0D, 0.0D, -2.0D, 8.0D, 12.0D, 4.0D));
        modelRadius = Math.max(modelRadius, animatedPartRadius(horizontal, vertical,
                -2.0D, 18.0D, 4.0D, -2.0D, 0.0D, -2.0D, 4.0D, 6.0D, 4.0D));
        modelRadius = Math.max(modelRadius, animatedPartRadius(horizontal, vertical,
                2.0D, 18.0D, 4.0D, -2.0D, 0.0D, -2.0D, 4.0D, 6.0D, 4.0D));
        modelRadius = Math.max(modelRadius, animatedPartRadius(horizontal, vertical,
                -2.0D, 18.0D, -4.0D, -2.0D, 0.0D, -2.0D, 4.0D, 6.0D, 4.0D));
        modelRadius = Math.max(modelRadius, animatedPartRadius(horizontal, vertical,
                2.0D, 18.0D, -4.0D, -2.0D, 0.0D, -2.0D, 4.0D, 6.0D, 4.0D));

        double radius = modelRadius * entity.getScale();
        if (entity.hasPose(Pose.SLEEPING)) {
            radius += Math.abs(entity.getEyeHeight(Pose.STANDING) - 0.1D);
        }
        if (LivingEntityRenderer.isEntityUpsideDown(entity)) {
            radius += entity.getBbHeight() + 0.1D;
        }
        return radius;
    }

    private static double animatedPartRadius(double horizontal, double vertical,
                                             double pivotX, double pivotY, double pivotZ,
                                             double x, double y, double z,
                                             double sizeX, double sizeY, double sizeZ) {
        double centerX = horizontal * pivotX / 16.0D;
        double centerY = vertical * (pivotY / 16.0D - 1.501D);
        double centerZ = horizontal * pivotZ / 16.0D;
        double centerRadius = Math.sqrt(centerX * centerX + centerY * centerY + centerZ * centerZ);
        double localRadius = furthestCornerRadius(x, y, z, sizeX, sizeY, sizeZ) / 16.0D;
        return centerRadius + Math.max(horizontal, vertical) * localRadius;
    }

    private static double fixedPartRadius(double horizontal, double vertical,
                                          double pivotX, double pivotY, double pivotZ,
                                          double x, double y, double z,
                                          double sizeX, double sizeY, double sizeZ) {
        double maximum = 0.0D;
        for (int xSide = 0; xSide < 2; xSide++) {
            double cornerX = xSide == 0 ? x : x + sizeX;
            for (int ySide = 0; ySide < 2; ySide++) {
                double cornerY = ySide == 0 ? y : y + sizeY;
                for (int zSide = 0; zSide < 2; zSide++) {
                    double cornerZ = zSide == 0 ? z : z + sizeZ;
                    double transformedX = horizontal * (pivotX + cornerX) / 16.0D;
                    double transformedY = vertical * ((pivotY + cornerY) / 16.0D - 1.501D);
                    double transformedZ = horizontal * (pivotZ + cornerZ) / 16.0D;
                    maximum = Math.max(maximum, Math.sqrt(
                            transformedX * transformedX
                                    + transformedY * transformedY
                                    + transformedZ * transformedZ));
                }
            }
        }
        return maximum;
    }

    private static double furthestCornerRadius(double x, double y, double z,
                                               double sizeX, double sizeY, double sizeZ) {
        double maximumSquared = 0.0D;
        for (int xSide = 0; xSide < 2; xSide++) {
            double cornerX = xSide == 0 ? x : x + sizeX;
            for (int ySide = 0; ySide < 2; ySide++) {
                double cornerY = ySide == 0 ? y : y + sizeY;
                for (int zSide = 0; zSide < 2; zSide++) {
                    double cornerZ = zSide == 0 ? z : z + sizeZ;
                    maximumSquared = Math.max(maximumSquared,
                            cornerX * cornerX + cornerY * cornerY + cornerZ * cornerZ);
                }
            }
        }
        return Math.sqrt(maximumSquared);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static final class PoweredLayer<T extends LegacyCreeperEntity>
            extends RenderLayer<T, CreeperModel<T>> {
        private final ResourceLocation texture;

        private PoweredLayer(RenderLayerParent<T, CreeperModel<T>> parent, ResourceLocation texture) {
            super(parent);
            this.texture = texture;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                           T entity, float limbSwing, float limbSwingAmount, float partialTick,
                           float ageInTicks, float netHeadYaw, float headPitch) {
            if (!entity.isPowered()) {
                return;
            }
            float scroll = (entity.tickCount + partialTick) * 0.01F;
            CreeperModel<T> parentModel = this.getParentModel();
            parentModel.renderToBuffer(poseStack,
                    bufferSource.getBuffer(RenderType.energySwirl(texture, scroll % 1.0F, scroll % 1.0F)),
                    0xF000F0, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), 0xFFFFFFFF);
        }
    }
}
