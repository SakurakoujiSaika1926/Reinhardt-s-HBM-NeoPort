package com.reinhardt.hbm.block;

import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.entity.LegacyProjectileUtil;
import com.reinhardt.hbm.explosion.LegacyMukeExplosion;
import com.reinhardt.hbm.network.LandmineEffectPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LandmineExplosions {
    private LandmineExplosions() {
    }

    public static void detonate(ServerLevel level, BlockPos pos, LandmineBlock.LandmineType type) {
        Vec3 center = Vec3.atCenterOf(pos);
        switch (type) {
            case AP -> LegacyProjectileUtil.landmineExplosion(
                    level, center, 3.0F, HbmConfig.MINE_AP_DAMAGE.get().floatValue(),
                    0.5D, 5.0F, 0.2F, false, 16, false, 5, 1.0F, 0.5F);
            case HE -> LegacyProjectileUtil.landmineExplosion(
                    level, center, 4.0F, HbmConfig.MINE_HE_DAMAGE.get().floatValue(),
                    1.0D, 15.0F, 0.2F, true, 16, false, 15, 3.5F, 1.25F);
            case SHRAPNEL -> {
                LegacyProjectileUtil.landmineExplosion(
                        level, center, 3.0F, HbmConfig.MINE_SHRAP_DAMAGE.get().floatValue(),
                        0.5D, 0.0F, 0.0F, false, 16, false, 5, 1.0F, 0.5F);
                LegacyProjectileUtil.spawnShrapnelShower(level, center, new Vec3(0.0D, 1.0D, 0.0D), 45, 0.2D);
                LegacyProjectileUtil.spawnShrapnel(level, center, 5);
            }
            case NUCLEAR -> LegacyMukeExplosion.detonateLandmine(
                    level, center, HbmConfig.MINE_NUKE_DAMAGE.get().floatValue());
            case NAVAL -> detonateNaval(level, pos);
        }
    }

    private static void detonateNaval(ServerLevel level, BlockPos pos) {
        Vec3 legacyBlastCenter = new Vec3(pos.getX() + 5.0D, pos.getY() + 5.0D, pos.getZ() + 5.0D);
        LegacyProjectileUtil.landmineExplosion(
                level, legacyBlastCenter, 25.0F, HbmConfig.MINE_NAVAL_DAMAGE.get().floatValue(),
                0.5D, 5.0F, 0.2F, true, 32, true, 10, 1.0F, 0.5F);

        Vec3 effectCenter = Vec3.atCenterOf(pos);
        int foamCount = hasWaterAbove(level, pos) ? 60 : 0;
        LandmineEffectPayload payload = new LandmineEffectPayload(
                effectCenter.x, effectCenter.y, effectCenter.z, 30, foamCount);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(effectCenter) <= 250.0D * 250.0D) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
        for (int i = 0; i < 5; i++) {
            double motionY = 0.75D * (1.0D + (5 + level.random.nextInt(25)) / 25.0D);
            level.addFreshEntity(new com.reinhardt.hbm.entity.MineRubbleEntity(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    new Vec3(level.random.nextGaussian() * 0.75D, motionY, level.random.nextGaussian() * 0.75D)));
        }
    }

    private static boolean hasWaterAbove(ServerLevel level, BlockPos pos) {
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                BlockPos above = pos.offset(xOffset, 1, zOffset);
                if (level.getFluidState(above).is(FluidTags.WATER)) {
                    return true;
                }
            }
        }
        return false;
    }
}
