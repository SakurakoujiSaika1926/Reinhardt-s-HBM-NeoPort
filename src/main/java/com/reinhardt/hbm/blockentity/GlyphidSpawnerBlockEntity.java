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
    private static final int SWARM_COOLDOWN = 120 * 20;
    private static final int BASE_SWARM_SIZE = 5;
    private static final double SWARM_SCALING = 1.2D;
    private static final int SOOT_STEP = 50;
    private static final int SPAWN_MAX = 50;
    private record SpawnChance(GlyphidEntity.Variant variant, int base, int sootModifier, int minimumSoot) {
    }

    // Same order as BlockGlyphidSpawner.spawnMap in 1.7.10.
    private static final SpawnChance[] CHANCES = {
            new SpawnChance(GlyphidEntity.Variant.NORMAL, 50, -45, 0),
            new SpawnChance(GlyphidEntity.Variant.BOMBARDIER, 20, -15, 1),
            new SpawnChance(GlyphidEntity.Variant.BRAWLER, 10, 30, 1),
            new SpawnChance(GlyphidEntity.Variant.DIGGER, -15, 25, 5),
            new SpawnChance(GlyphidEntity.Variant.BLASTER, -5, 40, 5),
            new SpawnChance(GlyphidEntity.Variant.BEHEMOTH, -30, 45, 10),
            new SpawnChance(GlyphidEntity.Variant.BRENDA, -50, 60, 20),
            new SpawnChance(GlyphidEntity.Variant.NUCLEAR, -50, 60, 50)
    };

    private boolean initialSpawn = true;

    public GlyphidSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.GLYPHID_SPAWNER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GlyphidSpawnerBlockEntity spawner) {
        if (level.isClientSide || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        if (!spawner.initialSpawn && level.getGameTime() % SWARM_COOLDOWN != 0) {
            return;
        }
        boolean firstSpawn = spawner.initialSpawn;
        spawner.initialSpawn = false;
        if (level instanceof ServerLevel serverLevel && countLoadedGlyphids(serverLevel) >= SPAWN_MAX) {
            return;
        }
        AABB nearbyBox = new AABB(pos).inflate(5.0D, 0.0D, 5.0D).expandTowards(1.0D, 7.0D, 1.0D);
        List<GlyphidEntity> nearby = level.getEntitiesOfClass(GlyphidEntity.class, nearbyBox);
        int subtype = Math.max(0, Math.min(2, state.getValue(com.reinhardt.hbm.block.LegacyVariantBlock.VARIANT)));
        if (nearby.size() > 3 && subtype != GlyphidEntity.TYPE_RADIOACTIVE) {
            return;
        }
        double soot = level instanceof ServerLevel serverLevel
                ? HbmPollutionData.get(serverLevel).get(pos, HbmPollutionType.SOOT)
                : 0.0D;
        int swarmAmount = (int) Math.min(BASE_SWARM_SIZE * Math.max(SWARM_SCALING * (soot / SOOT_STEP), 1.0D), 10.0D);
        List<GlyphidEntity> swarm = new ArrayList<>();
        int attempts = 100;
        while (swarm.size() <= swarmAmount && attempts-- >= 0) {
            for (SpawnChance chance : CHANCES) {
                int adjusted = (int) (chance.base + (chance.sootModifier
                        - chance.sootModifier / Math.max((soot + 1.0D) / 3.0D, 1.0D)));
                if (soot >= chance.minimumSoot && level.random.nextInt(100) <= adjusted) {
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
        if ((!firstSpawn || HbmConfig.GLYPHID_SCOUT_INITIAL_SPAWN.get())
                && subtype != GlyphidEntity.TYPE_RADIOACTIVE
                && level.random.nextInt(HbmConfig.glyphidScoutSwarmChance() + 1) == 0
                && soot >= HbmConfig.glyphidScoutSootThreshold()) {
            GlyphidEntity scout = new GlyphidEntity(
                    com.reinhardt.hbm.registry.HbmEntityTypes.GLYPHID.get(), level);
            scout.setSubtype(subtype);
            scout.setVariant(GlyphidEntity.Variant.SCOUT);
            spawn(level, pos, scout);
        }
        spawner.setChanged();
    }

    private static int countLoadedGlyphids(ServerLevel level) {
        int count = 0;
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (entity instanceof GlyphidEntity && entity.isAlive() && ++count >= SPAWN_MAX) {
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
        glyphid.discard();
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
