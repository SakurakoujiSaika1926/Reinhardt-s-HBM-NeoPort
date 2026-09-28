package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyQuackosEntity;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.phys.AABB;

/** The old Quackos renderer is a chicken model scaled by exactly 25. */
public final class LegacyQuackosEntityRenderer extends ChickenRenderer {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/entity/duck.png");
    private static final double ADULT_MODEL_RADIUS = adultModelRadius();
    private static final double BABY_MODEL_RADIUS = babyModelRadius();

    public LegacyQuackosEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        // EntityQuackos#getShadowSize returned 7.5F in 1.7.10; the giant
        // chicken must retain that exact footprint instead of ChickenRenderer's
        // normal 0.3F shadow.
        shadowRadius = 7.5F;
    }

    @Override
    protected void scale(net.minecraft.world.entity.animal.Chicken entity,
                         PoseStack poseStack, float partialTick) {
        poseStack.scale(25.0F, 25.0F, 25.0F);
    }

    @Override
    public boolean shouldRender(Chicken entity, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        double modelRadius = entity.isBaby() ? BABY_MODEL_RADIUS : ADULT_MODEL_RADIUS;
        double radius = modelRadius * 25.0D * entity.getScale();
        if (entity.hasPose(Pose.SLEEPING)) {
            // LivingEntityRenderer applies this bed offset before entity scale.
            radius += Math.abs(entity.getEyeHeight(Pose.STANDING) - 0.1D);
        }
        if (LivingEntityRenderer.isEntityUpsideDown(entity)) {
            radius += entity.getBbHeight() + 0.1D;
        }
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

    @Override
    public ResourceLocation getTextureLocation(net.minecraft.world.entity.animal.Chicken entity) {
        return TEXTURE;
    }

    private static double adultModelRadius() {
        double radius = 0.0D;
        radius = Math.max(radius, adultPart(0, 15, -4, -2, -6, -2, 4, 6, 3));
        radius = Math.max(radius, adultPart(0, 15, -4, -2, -4, -4, 4, 2, 2));
        radius = Math.max(radius, adultPart(0, 15, -4, -1, -2, -3, 2, 2, 2));
        radius = Math.max(radius, adultPart(0, 16, 0, -3, -4, -3, 6, 8, 6));
        radius = Math.max(radius, adultPart(-2, 19, 1, -1, 0, -3, 3, 5, 3));
        radius = Math.max(radius, adultPart(1, 19, 1, -1, 0, -3, 3, 5, 3));
        radius = Math.max(radius, adultPart(-4, 13, 0, 0, 0, -3, 1, 4, 6));
        radius = Math.max(radius, adultPart(4, 13, 0, -1, 0, -3, 1, 4, 6));
        return Math.nextUp(radius);
    }

    private static double babyModelRadius() {
        double radius = 0.0D;
        // AgeableListModel's exact ChickenModel defaults: unscaled head at
        // (0,5,2)/16, and half-scale body at (0,24,0)/16.
        radius = Math.max(radius, babyHeadPart(0, 15, -4, -2, -6, -2, 4, 6, 3));
        radius = Math.max(radius, babyHeadPart(0, 15, -4, -2, -4, -4, 4, 2, 2));
        radius = Math.max(radius, babyHeadPart(0, 15, -4, -1, -2, -3, 2, 2, 2));
        radius = Math.max(radius, babyBodyPart(0, 16, 0, -3, -4, -3, 6, 8, 6));
        radius = Math.max(radius, babyBodyPart(-2, 19, 1, -1, 0, -3, 3, 5, 3));
        radius = Math.max(radius, babyBodyPart(1, 19, 1, -1, 0, -3, 3, 5, 3));
        radius = Math.max(radius, babyBodyPart(-4, 13, 0, 0, 0, -3, 1, 4, 6));
        radius = Math.max(radius, babyBodyPart(4, 13, 0, -1, 0, -3, 1, 4, 6));
        return Math.nextUp(radius);
    }

    private static double adultPart(double px, double py, double pz,
                                    double x, double y, double z,
                                    double dx, double dy, double dz) {
        return partRadius(px / 16.0D, py / 16.0D - 1.501D, pz / 16.0D,
                x / 16.0D, y / 16.0D, z / 16.0D,
                dx / 16.0D, dy / 16.0D, dz / 16.0D, 1.0D);
    }

    private static double babyHeadPart(double px, double py, double pz,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
        return partRadius(px / 16.0D, (py + 5.0D) / 16.0D - 1.501D, (pz + 2.0D) / 16.0D,
                x / 16.0D, y / 16.0D, z / 16.0D,
                dx / 16.0D, dy / 16.0D, dz / 16.0D, 1.0D);
    }

    private static double babyBodyPart(double px, double py, double pz,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
        return partRadius(px / 32.0D, (24.0D + py) / 32.0D - 1.501D, pz / 32.0D,
                x / 16.0D, y / 16.0D, z / 16.0D,
                dx / 16.0D, dy / 16.0D, dz / 16.0D, 0.5D);
    }

    private static double partRadius(double px, double py, double pz,
                                     double x, double y, double z,
                                     double dx, double dy, double dz,
                                     double localScale) {
        double localSquared = 0.0D;
        for (double vx : new double[] {x, x + dx}) {
            for (double vy : new double[] {y, y + dy}) {
                for (double vz : new double[] {z, z + dz}) {
                    localSquared = Math.max(localSquared, vx * vx + vy * vy + vz * vz);
                }
            }
        }
        return Math.sqrt(px * px + py * py + pz * pz)
                + localScale * Math.sqrt(localSquared);
    }
}
