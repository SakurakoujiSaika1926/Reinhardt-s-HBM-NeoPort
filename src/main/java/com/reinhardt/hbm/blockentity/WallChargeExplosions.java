package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.WallChargeBlock;
import com.reinhardt.hbm.entity.LegacyProjectileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public final class WallChargeExplosions {
    private WallChargeExplosions() {
    }

    public static void detonate(ServerLevel level, BlockPos pos, WallChargeBlock.Kind kind) {
        Vec3 center = Vec3.atCenterOf(pos);
        level.removeBlock(pos, false);
        if (!kind.noHurt()) {
            level.explode(null, center.x, center.y, center.z, kind.radius(), false, Level.ExplosionInteraction.NONE);
        }
        destroyLegacyChargeBlocks(level, center, kind.radius(), kind.drops());
        LegacyProjectileUtil.composeExplosionEffect(level, center,
                kind == WallChargeBlock.Kind.DYNAMITE ? 15 : 10,
                kind == WallChargeBlock.Kind.DYNAMITE ? 3.0F : 2.5F,
                1.25F, 1.0F, 0, 0, 0, 0.0F, 0.0F, 0.0F, 64.0F);
    }

    private static void destroyLegacyChargeBlocks(ServerLevel level, Vec3 center, float radius, boolean drops) {
        Set<BlockPos> affected = new HashSet<>();
        int resolution = 32;
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                for (int k = 0; k < resolution; k++) {
                    if (i != 0 && i != resolution - 1 && j != 0 && j != resolution - 1 && k != 0 && k != resolution - 1) {
                        continue;
                    }
                    double dx = (double) i / (resolution - 1) * 2.0D - 1.0D;
                    double dy = (double) j / (resolution - 1) * 2.0D - 1.0D;
                    double dz = (double) k / (resolution - 1) * 2.0D - 1.0D;
                    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= length;
                    dy /= length;
                    dz /= length;
                    double x = center.x;
                    double y = center.y;
                    double z = center.z;
                    float power = radius * (0.7F + level.random.nextFloat() * 0.6F);
                    for (float step = 0.3F; power > 0.0F; power -= step * 0.75F) {
                        BlockPos blockPos = BlockPos.containing(x, y, z);
                        if (!level.isInWorldBounds(blockPos)) {
                            break;
                        }
                        BlockState state = level.getBlockState(blockPos);
                        if (!state.isAir()) {
                            power -= (state.getBlock().getExplosionResistance() + 0.3F) * step;
                            if (power > 0.0F && state.getDestroySpeed(level, blockPos) >= 0.0F) {
                                affected.add(blockPos.immutable());
                            }
                        }
                        x += dx * step;
                        y += dy * step;
                        z += dz * step;
                    }
                }
            }
        }
        Explosion context = new Explosion(level, null, center.x, center.y, center.z,
                radius, false, Explosion.BlockInteraction.KEEP);
        for (BlockPos blockPos : affected) {
            BlockState state = level.getBlockState(blockPos);
            if (state.isAir()) {
                continue;
            }
            if (drops) {
                level.destroyBlock(blockPos, true, null);
            } else {
                state.onExplosionHit(level, blockPos, context, (stack, dropPos) -> { });
                level.removeBlock(blockPos, false);
            }
        }
    }
}
