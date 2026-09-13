package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.entity.GlyphidEntity;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.pollution.HbmPollutionWorlds;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.Difficulty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Natural Scout and escort spawning from HBM 1.7.10's pollution handler. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class GlyphidSpawnEvents {
    private GlyphidSpawnEvents() {
    }

    @SubscribeEvent
    public static void onPlayerSleep(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isSleeping()) {
            return;
        }
        GlyphidEntity.setGuidanceTarget(player.blockPosition());
    }

    @SubscribeEvent
    public static void onNaturalSpawnPosition(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL) {
            EntityType<?> type = event.getEntity().getType();
            boolean legacyCreeper = type == HbmEntityTypes.PHOSGENE_CREEPER.get()
                    || type == HbmEntityTypes.VOLATILE_CREEPER.get()
                    || type == HbmEntityTypes.GOLD_CREEPER.get();
            if (legacyCreeper && (!(event.getLevel() instanceof ServerLevel level)
                    || level.dimension() != Level.OVERWORLD)) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
            if ((type == HbmEntityTypes.VOLATILE_CREEPER.get()
                    || type == HbmEntityTypes.GOLD_CREEPER.get())
                    && event.getY() > 40.0D) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
        }
        if (event.getSpawnType() != MobSpawnType.NATURAL
                || !(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD
                || level.getDifficulty() == Difficulty.PEACEFUL
                || event.getResult() == MobSpawnEvent.PositionCheck.Result.FAIL
                || !HbmConfig.glyphidNaturalScoutSpawn()) {
            return;
        }

        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        if (!level.canSeeSky(pos)
                || level.getMaxLocalRawBrightness(pos) > 7
                || level.random.nextInt(HbmConfig.GLYPHID_NATURAL_SCOUT_CHANCE.get()) != 0
                || HbmPollutionWorlds.get(level, pos, HbmPollutionType.SOOT)
                < HbmConfig.GLYPHID_NATURAL_SCOUT_THRESHOLD.get()) {
            return;
        }

        GlyphidEntity scout = new GlyphidEntity(HbmEntityTypes.GLYPHID.get(), level);
        scout.setVariant(GlyphidEntity.Variant.SCOUT);
        scout.moveTo(event.getX(), event.getY(), event.getZ(), level.random.nextFloat() * 360.0F, 0.0F);

        // PollutionHandler constructed the escort, then called
        // setLocationAndAngles on the scout a second time. Keep that random
        // draw and final scout yaw exactly, including the redundant call.
        GlyphidEntity digger = new GlyphidEntity(HbmEntityTypes.GLYPHID.get(), level);
        digger.setVariant(GlyphidEntity.Variant.DIGGER);
        scout.moveTo(event.getX(), event.getY(), event.getZ(), level.random.nextFloat() * 360.0F, 0.0F);
        digger.moveTo(event.getX(), event.getY(), event.getZ(), level.random.nextFloat() * 360.0F, 0.0F);
        if (scout.checkSpawnObstruction(level)) {
            level.addFreshEntity(scout);
        } else {
            scout.discard();
        }
        if (digger.checkSpawnObstruction(level)) {
            level.addFreshEntity(digger);
        } else {
            digger.discard();
        }
    }
}
