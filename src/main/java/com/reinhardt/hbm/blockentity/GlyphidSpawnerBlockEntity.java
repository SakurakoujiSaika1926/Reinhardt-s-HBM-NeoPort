package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.entity.GlyphidEntity;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionData;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Server-side port of BlockGlyphidSpawner's initial and periodic swarm logic. */
public final class GlyphidSpawnerBlockEntity extends BlockEntity {
    private record SpawnChance(GlyphidEntity.Variant variant, int base, int sootModifier, int minimumSoot) {
    }

    private boolean initialSpawn = true;

    public GlyphidSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.GLYPHID_SPAWNER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GlyphidSpawnerBlockEntity spawner) {
        if (level.isClientSide || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        if (!spawner.initialSpawn && level.getGameTime() % HbmConfig.glyphidSwarmCooldown() != 0) {
            return;
        }
        spawner.initialSpawn = false;
        if (level instanceof ServerLevel serverLevel && countLoadedGlyphids(serverLevel) >= HbmConfig.GLYPHID_SPAWN_MAX.get()) {
            return;
        }
        AABB nearbyBox = new AABB(pos.getX() - 5.0D, pos.getY() + 1.0D, pos.getZ() - 5.0D,
                pos.getX() + 6.0D, pos.getY() + 7.0D, pos.getZ() + 6.0D);
        List<GlyphidEntity> nearby = level.getEntitiesOfClass(GlyphidEntity.class, nearbyBox);
        int subtype = Math.max(0, Math.min(2, state.getValue(com.reinhardt.hbm.block.LegacyVariantBlock.VARIANT)));
        if (nearby.size() > 3 && subtype != GlyphidEntity.TYPE_RADIOACTIVE) {
            return;
        }
        double soot = level instanceof ServerLevel serverLevel
                ? HbmPollutionData.get(serverLevel).get(pos, HbmPollutionType.SOOT)
                : 0.0D;
        int swarmAmount = (int) Math.min(HbmConfig.GLYPHID_BASE_SWARM_SIZE.get()
                * Math.max(HbmConfig.GLYPHID_SWARM_SCALING_MULTIPLIER.get()
                * (soot / HbmConfig.GLYPHID_SOOT_STEP.get()), 1.0D), 10.0D);
        List<GlyphidEntity> swarm = new ArrayList<>();
        int attempts = 100;
        java.util.Random random = new java.util.Random();
        while (swarm.size() <= swarmAmount && attempts-- >= 0) {
            for (SpawnChance chance : chances()) {
                int adjusted = (int) (chance.base + (chance.sootModifier
                        - chance.sootModifier / Math.max((soot + 1.0D) / 3.0D, 1.0D)));
                if (soot >= chance.minimumSoot && random.nextInt(100) <= adjusted) {
                    GlyphidEntity glyphid = new GlyphidEntity(com.reinhardt.hbm.registry.HbmEntityTypes.GLYPHID.get(), level);
                    glyphid.setSubtype(subtype);
                    glyphid.setVariant(chance.variant);
                    swarm.add(glyphid);
                }
            }
        }
        for (GlyphidEntity glyphid : swarm) {
            spawn(level, pos, glyphid);
        }
        if (subtype != GlyphidEntity.TYPE_RADIOACTIVE
                && level.random.nextInt(HbmConfig.glyphidScoutSwarmChance() + 1) == 0
                && soot >= HbmConfig.glyphidScoutSootThreshold()) {
            GlyphidEntity scout = new GlyphidEntity(
                    com.reinhardt.hbm.registry.HbmEntityTypes.GLYPHID.get(), level);
            scout.setSubtype(subtype);
            scout.setVariant(GlyphidEntity.Variant.SCOUT);
            spawn(level, pos, scout);
        }
    }

    private static int countLoadedGlyphids(ServerLevel level) {
        int count = 0;
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (entity instanceof GlyphidEntity && ++count >= HbmConfig.GLYPHID_SPAWN_MAX.get()) {
                return count;
            }
        }
        return count;
    }

    private static void spawn(Level level, BlockPos pos, GlyphidEntity glyphid) {
        double x = pos.getX() + 0.5D + glyphid.getRandom().nextGaussian() * 3.0D;
        double z = pos.getZ() + 0.5D + glyphid.getRandom().nextGaussian() * 3.0D;
        for (int i = 0; i < 7; i++) {
            glyphid.moveTo(x, pos.getY() - 2 + i, z, level.random.nextFloat() * 360.0F, 0.0F);
            if (glyphid.checkSpawnObstruction(level)) {
                level.addFreshEntity(glyphid);
                return;
            }
        }
    }

    /** Same BlockGlyphidSpawner.spawnMap order, with each 1.7.10 config tuple intact. */
    private static SpawnChance[] chances() {
        return new SpawnChance[]{
                chance(GlyphidEntity.Variant.NORMAL, HbmConfig.GLYPHID_GRUNT_CHANCE, "glyphidChance"),
                chance(GlyphidEntity.Variant.BOMBARDIER, HbmConfig.GLYPHID_BOMBARDIER_CHANCE, "bombardierChance"),
                chance(GlyphidEntity.Variant.BRAWLER, HbmConfig.GLYPHID_BRAWLER_CHANCE, "brawlerChance"),
                chance(GlyphidEntity.Variant.DIGGER, HbmConfig.GLYPHID_DIGGER_CHANCE, "diggerChance"),
                chance(GlyphidEntity.Variant.BLASTER, HbmConfig.GLYPHID_BLASTER_CHANCE, "blasterChance"),
                chance(GlyphidEntity.Variant.BEHEMOTH, HbmConfig.GLYPHID_BEHEMOTH_CHANCE, "behemothChance"),
                chance(GlyphidEntity.Variant.BRENDA, HbmConfig.GLYPHID_BRENDA_CHANCE, "brendaChance"),
                chance(GlyphidEntity.Variant.NUCLEAR, HbmConfig.GLYPHID_NUCLEAR_CHANCE, "johnsonChance")
        };
    }

    private static SpawnChance chance(GlyphidEntity.Variant variant,
                                      net.neoforged.neoforge.common.ModConfigSpec.ConfigValue<List<? extends Integer>> value,
                                      String key) {
        int[] tuple = HbmConfig.glyphidChance(value, key);
        return new SpawnChance(variant, tuple[0], tuple[1], tuple[2]);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("initialSpawn", initialSpawn);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        initialSpawn = tag.getBoolean("initialSpawn");
    }
}
