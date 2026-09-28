package com.reinhardt.hbm.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shared visibility policy for renderers whose geometry extends beyond the
 * render section containing the owning block entity.
 *
 * <p>Only an instance whose finite AABB actually crosses its owning 16-block
 * render section is registered as global. NeoForge still frustum-tests those
 * instances against that AABB, so this prevents section-edge disappearance
 * without putting every machine of the same type on the global render list.
 * Distance is measured from the camera to the closest point of the authored
 * AABB. The view-distance ceiling follows the client's effective chunk render
 * distance instead of {@link BlockEntityRenderer}'s fixed 64-block default.</p>
 */
interface LongRangeBlockEntityRenderer<T extends BlockEntity> extends BlockEntityRenderer<T> {
    @Override
    default int getViewDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
    }

    @Override
    default boolean shouldRenderOffScreen(T blockEntity) {
        AABB bounds = getRenderBoundingBox(blockEntity);
        if (bounds == null) {
            return false;
        }

        BlockPos owner = blockEntity.getBlockPos();
        int ownerSectionX = SectionPos.blockToSectionCoord(owner.getX());
        int ownerSectionY = SectionPos.blockToSectionCoord(owner.getY());
        int ownerSectionZ = SectionPos.blockToSectionCoord(owner.getZ());
        return SectionPos.blockToSectionCoord(Mth.floor(bounds.minX)) != ownerSectionX
                || SectionPos.blockToSectionCoord(Mth.floor(inclusiveMaximum(bounds.minX, bounds.maxX))) != ownerSectionX
                || SectionPos.blockToSectionCoord(Mth.floor(bounds.minY)) != ownerSectionY
                || SectionPos.blockToSectionCoord(Mth.floor(inclusiveMaximum(bounds.minY, bounds.maxY))) != ownerSectionY
                || SectionPos.blockToSectionCoord(Mth.floor(bounds.minZ)) != ownerSectionZ
                || SectionPos.blockToSectionCoord(Mth.floor(inclusiveMaximum(bounds.minZ, bounds.maxZ))) != ownerSectionZ;
    }

    @Override
    default boolean shouldRender(T blockEntity, Vec3 cameraPosition) {
        AABB bounds = getRenderBoundingBox(blockEntity);
        if (bounds == null) {
            return Vec3.atCenterOf(blockEntity.getBlockPos())
                    .closerThan(cameraPosition, (double) getViewDistance());
        }

        double dx = axisDistance(cameraPosition.x, bounds.minX, bounds.maxX);
        double dy = axisDistance(cameraPosition.y, bounds.minY, bounds.maxY);
        double dz = axisDistance(cameraPosition.z, bounds.minZ, bounds.maxZ);
        double distance = getViewDistance();
        return dx * dx + dy * dy + dz * dz < distance * distance;
    }

    private static double axisDistance(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0D;
    }

    /** AABB maxima are exclusive; keep an exact section boundary in the section below it. */
    private static double inclusiveMaximum(double min, double max) {
        return max > min ? Math.nextDown(max) : max;
    }
}
