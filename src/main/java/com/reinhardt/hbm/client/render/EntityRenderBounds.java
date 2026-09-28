package com.reinhardt.hbm.client.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/** Finite, model-derived culling envelopes for non-block entity renderers. */
final class EntityRenderBounds {
    private EntityRenderBounds() {
    }

    static boolean shouldRender(Entity entity, AABB worldBounds, Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        return shouldRender(entity, worldBounds, worldBounds, frustum,
                cameraX, cameraY, cameraZ);
    }

    /**
     * Uses one finite envelope for the submitted vertices and a separately
     * proved envelope for view-distance sizing.  This is required for legacy
     * effects such as the death blast: its 250-block beam participates in
     * frustum culling, while 1.7.10 explicitly limits the entity to a squared
     * render distance of 25,000 instead of deriving a 16,000-block distance
     * from that beam length.
     */
    static boolean shouldRender(Entity entity, AABB worldBounds, AABB distanceBounds,
                                Frustum frustum,
                                double cameraX, double cameraY, double cameraZ) {
        // LevelRenderer renders every entity at lerp(xOld, current, partialTick),
        // while EntityRenderer#shouldRender is called without partialTick.  The
        // union below is therefore the exact finite envelope of every possible
        // render origin for this frame.  Testing only worldBounds at the current
        // position clips fast projectiles and debris at a frustum edge.
        AABB sweptBounds = includePreviousRenderOrigin(entity, worldBounds);
        return isWithinRenderDistance(entity, distanceBounds, cameraX, cameraY, cameraZ)
                && frustum.isVisible(sweptBounds.inflate(0.5D));
    }

    /**
     * The distance half of {@link #shouldRender}.  Living renderers also use
     * this for Minecraft's leash-holder visibility fallback: falling back to
     * {@link Entity#shouldRender(double, double, double)} there would silently
     * replace a proved large-model distance with the entity's small collision
     * box distance.
     */
    static boolean isWithinRenderDistance(Entity entity, AABB worldBounds,
                                          double cameraX, double cameraY, double cameraZ) {
        double distanceSquared = distanceSquaredToRenderOriginSegment(
                entity, cameraX, cameraY, cameraZ);

        // Entity#shouldRenderAtSqrDistance uses AABB#getSize * 64 * the
        // configured view scale. Preserve every entity-specific override,
        // but also run that exact vanilla rule against the proven visual box
        // instead of the often much smaller collision box.
        // Motion between ticks changes the render origin, not the size of the
        // rendered object. Using a swept box here would turn a fast
        // projectile's travelled distance into an artificial view distance.
        double visualSize = Math.max(entity.getBoundingBox().getSize(), worldBounds.getSize());
        double visualDistance = visualSize * 64.0D * Entity.getViewScale();
        return entity.shouldRenderAtSqrDistance(distanceSquared)
                || distanceSquared < visualDistance * visualDistance;
    }

    /**
     * Exact minimum camera distance to the linear render-origin interpolation
     * used by LevelRenderer for partial ticks in [0, 1].
     */
    private static double distanceSquaredToRenderOriginSegment(
            Entity entity, double cameraX, double cameraY, double cameraZ) {
        double startX = entity.xOld;
        double startY = entity.yOld;
        double startZ = entity.zOld;
        double segmentX = entity.getX() - startX;
        double segmentY = entity.getY() - startY;
        double segmentZ = entity.getZ() - startZ;
        double lengthSquared = segmentX * segmentX + segmentY * segmentY + segmentZ * segmentZ;
        if (lengthSquared == 0.0D) {
            double dx = startX - cameraX;
            double dy = startY - cameraY;
            double dz = startZ - cameraZ;
            return dx * dx + dy * dy + dz * dz;
        }

        double projection = ((cameraX - startX) * segmentX
                + (cameraY - startY) * segmentY
                + (cameraZ - startZ) * segmentZ) / lengthSquared;
        double partialTick = Math.max(0.0D, Math.min(1.0D, projection));
        double dx = startX + segmentX * partialTick - cameraX;
        double dy = startY + segmentY * partialTick - cameraY;
        double dz = startZ + segmentZ * partialTick - cameraZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private static AABB includePreviousRenderOrigin(Entity entity, AABB currentBounds) {
        double previousOffsetX = entity.xOld - entity.getX();
        double previousOffsetY = entity.yOld - entity.getY();
        double previousOffsetZ = entity.zOld - entity.getZ();
        if (previousOffsetX == 0.0D && previousOffsetY == 0.0D && previousOffsetZ == 0.0D) {
            return currentBounds;
        }
        return currentBounds.minmax(currentBounds.move(
                previousOffsetX, previousOffsetY, previousOffsetZ));
    }

    static AABB moveToEntity(AABB localBounds, Entity entity) {
        return localBounds.move(entity.getX(), entity.getY(), entity.getZ());
    }

    static AABB radius(double centerX, double centerY, double centerZ, double radius) {
        double finiteRadius = Math.max(0.0D, radius);
        return new AABB(
                centerX - finiteRadius, centerY - finiteRadius, centerZ - finiteRadius,
                centerX + finiteRadius, centerY + finiteRadius, centerZ + finiteRadius);
    }

    /** Accumulates the same float coordinates submitted by OBJ render paths. */
    static final class FloatBuilder {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;
        private double maximumRadiusSquared;

        void include(Vector3f point) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
            maximumRadiusSquared = Math.max(maximumRadiusSquared,
                    (double) point.x * point.x
                            + (double) point.y * point.y
                            + (double) point.z * point.z);
        }

        double maximumRadius() {
            return Math.nextUp(Math.sqrt(maximumRadiusSquared));
        }

        AABB buildLocal() {
            if (!Float.isFinite(minX)) {
                return new AABB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            // Widen by one representable double step so the envelope also
            // contains the final float-to-double conversion at frustum tests.
            return new AABB(
                    Math.nextDown((double) minX), Math.nextDown((double) minY), Math.nextDown((double) minZ),
                    Math.nextUp((double) maxX), Math.nextUp((double) maxY), Math.nextUp((double) maxZ));
        }
    }
}
