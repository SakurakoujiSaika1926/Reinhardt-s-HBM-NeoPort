package com.reinhardt.hbm.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Direct 1.7.10 ExplosionChaos.floater/move behavior used by dropped Xen crystals. */
public final class LegacyChaosEffects {
    private static final int RADIUS = 25;
    private static final int LIFT_HEIGHT = 75;

    private LegacyChaosEffects() {
    }

    public static void floatAndLift(ServerLevel level, BlockPos center) {
        floatBlocks(level, center, RADIUS, LIFT_HEIGHT);
        liftEntities(level, center, RADIUS, LIFT_HEIGHT);
    }

    private static void floatBlocks(ServerLevel level, BlockPos center, int radius, int height) {
        int radiusSquaredHalf = radius * radius / 2;
        for (int x = -radius; x < radius; x++) {
            int horizontalSquared = x * x;
            for (int y = -radius; y < radius; y++) {
                int verticalSquared = horizontalSquared + y * y;
                for (int z = -radius; z < radius; z++) {
                    if (verticalSquared + z * z >= radiusSquaredHalf) {
                        continue;
                    }
                    BlockPos source = center.offset(x, y, z);
                    BlockPos destination = source.above(height);
                    if (!level.isInWorldBounds(source) || !level.isInWorldBounds(destination)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(source);
                    if (state.isAir()) {
                        continue;
                    }
                    // Legacy code moved the block state only, discarding any block-entity data.
                    level.removeBlock(source, false);
                    level.setBlock(destination, state, 2);
                }
            }
        }
    }

    private static void liftEntities(ServerLevel level, BlockPos center, int radius, int height) {
        double reach = radius * 2.0D;
        AABB area = new AABB(
                center.getX() - radius - 1.0D, center.getY() - radius - 1.0D, center.getZ() - radius - 1.0D,
                center.getX() + radius + 1.0D, center.getY() + radius + 1.0D, center.getZ() + radius + 1.0D
        );
        Vec3 origin = Vec3.atLowerCornerOf(center);
        for (Entity entity : level.getEntities((Entity) null, area, Entity::isAlive)) {
            if (entity.position().distanceTo(origin) / reach > 1.0D) {
                continue;
            }
            Vec3 eyeOffset = new Vec3(entity.getX() - origin.x, entity.getEyeY() - origin.y, entity.getZ() - origin.z);
            if (eyeOffset.length() >= radius) {
                continue;
            }
            if (entity instanceof Sheep) {
                entity.setCustomName(Component.literal("jeb_"));
            } else if (entity instanceof LivingEntity) {
                entity.setCustomName(Component.literal(level.random.nextBoolean() ? "Dinnerbone" : "Grumm"));
            }
            entity.setPos(entity.getX(), entity.getY() + height, entity.getZ());
        }
    }
}
