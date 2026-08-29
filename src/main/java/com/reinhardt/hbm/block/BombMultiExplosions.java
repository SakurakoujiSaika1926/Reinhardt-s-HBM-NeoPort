package com.reinhardt.hbm.block;

import com.reinhardt.hbm.entity.ClusterSubmunitionEntity;
import com.reinhardt.hbm.entity.LegacyMistEntity;
import com.reinhardt.hbm.entity.LegacyProjectileUtil;
import com.reinhardt.hbm.entity.MineRubbleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Exact effect dispatch from BombMulti#igniteTestBomb in the 1.7.10 source. */
public final class BombMultiExplosions {
    private static final float BASE_STRENGTH = 8.0F;

    private BombMultiExplosions() {
    }

    public static void detonate(ServerLevel level, BlockPos pos, int firstType, int secondType) {
        float strength = BASE_STRENGTH;
        int clusters = 0;
        int fireRadius = 0;
        int poisonRadius = 0;
        int gasCloud = 0;

        int[] modifiers = {firstType, secondType};
        for (int type : modifiers) {
            switch (type) {
                case 1 -> strength += 1.0F;
                case 2 -> strength += 4.0F;
                case 3 -> clusters += 50;
                case 4 -> fireRadius += 10;
                case 5 -> poisonRadius += 15;
                case 6 -> gasCloud += 50;
                default -> {
                }
            }
        }

        // The legacy call was world.createExplosion(null, x, y, z, strength, true): integer block origin and fire.
        Vec3 center = new Vec3(pos.getX(), pos.getY(), pos.getZ());
        level.explode(null, center.x, center.y, center.z, strength, true, Level.ExplosionInteraction.BLOCK);

        int integralStrength = (int) strength;
        LegacyProjectileUtil.composeExplosionEffect(level, center,
                (int) (850.0D * (1.0D - Math.exp(-integralStrength / 15.0D)) + 15.0D),
                1.0F, 1.0F, 0.0F,
                0, 0, 0, 0.0F, 0.0F, 0.0F, 250.0F);
        spawnRubble(level, center, integralStrength / 10);
        LegacyProjectileUtil.spawnShrapnel(level, center, integralStrength / 3);

        if (clusters > 0) {
            ClusterSubmunitionEntity.spawn(level, Vec3.atCenterOf(pos), clusters);
        }
        if (fireRadius > 0) {
            LegacyProjectileUtil.igniteAllBlocksLegacy(level, pos, fireRadius);
        }
        if (poisonRadius > 0) {
            LegacyProjectileUtil.wasteNoSchrab(level, pos, poisonRadius);
        }
        if (gasCloud > 0) {
            level.addFreshEntity(new LegacyMistEntity(level,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    LegacyMistEntity.MistType.CHLORINE,
                    gasCloud * 15.0F / 50.0F,
                    gasCloud * 7.5F / 50.0F));
        }
    }

    private static void spawnRubble(ServerLevel level, Vec3 center, int count) {
        for (int index = 0; index < count; index++) {
            double vertical = 0.75D * (1 + ((count + level.random.nextInt(count * 5)) / 25));
            double lateral = 0.75D * (1 + (count / 50));
            level.addFreshEntity(new MineRubbleEntity(level, center.x, center.y, center.z,
                    new Vec3(level.random.nextGaussian() * lateral, vertical,
                            level.random.nextGaussian() * lateral)));
        }
    }
}
