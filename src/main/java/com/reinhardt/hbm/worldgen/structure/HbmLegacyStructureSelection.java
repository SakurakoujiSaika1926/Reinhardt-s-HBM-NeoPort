package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.config.HbmConfig;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

final class HbmLegacyStructureSelection {
    private HbmLegacyStructureSelection() {
    }

    static SelectedStructure select(Holder<Biome> biome, RandomSource random) {
        List<Candidate> candidates = new ArrayList<>();
        add(candidates, "spire", "spire", -1, 1, 128, false, HbmConfig.HBM_STRUCTURE_SPIRE_WEIGHT, HbmLegacyStructureSelection::isVeryFlatNonInvalid);
        add(candidates, "vertibird", "vertibird", -3, 1, 128, false, HbmConfig.HBM_STRUCTURE_VERTIBIRD_WEIGHT, HbmLegacyStructureSelection::isSandy);
        add(candidates, "crashed_vertibird", "crashed-vertibird", -10, 1, 128, false, HbmConfig.HBM_STRUCTURE_CRASHED_VERTIBIRD_WEIGHT, HbmLegacyStructureSelection::isSandy);
        add(candidates, "aircraft_carrier", "aircraft_carrier", -6, 1, 42, false, oceanWeight(HbmConfig.HBM_STRUCTURE_AIRCRAFT_CARRIER_WEIGHT), HbmLegacyStructureSelection::isOcean);
        add(candidates, "oil_rig", "oil_rig", -20, 11, 12, false, oceanWeight(HbmConfig.HBM_STRUCTURE_OIL_RIG_WEIGHT), HbmLegacyStructureSelection::isOcean);
        add(candidates, "lighthouse", "lighthouse", -40, 28, 29, false, oceanWeight(HbmConfig.HBM_STRUCTURE_LIGHTHOUSE_WEIGHT), HbmLegacyStructureSelection::isOceanOrBeach);
        add(candidates, "beached_patrol", "beached_patrol", -5, 58, 67, false, HbmConfig.HBM_STRUCTURE_BEACHED_PATROL_WEIGHT, HbmLegacyStructureSelection::isBeach);
        add(candidates, "dish", "dish", -10, 53, 65, false, HbmConfig.HBM_STRUCTURE_DISH_WEIGHT, HbmLegacyStructureSelection::isPlainsLike);
        add(candidates, "forestchem", "forest_chem", -9, 1, 128, false, HbmConfig.HBM_STRUCTURE_FOREST_CHEM_WEIGHT, HbmLegacyStructureSelection::isGentleNonInvalid);
        add(candidates, "plane1", "crashed_plane_1", -5, 1, 128, false, HbmConfig.HBM_STRUCTURE_PLANE1_WEIGHT, HbmLegacyStructureSelection::isGentleNonInvalid);
        add(candidates, "plane2", "crashed_plane_2", -8, 1, 128, false, HbmConfig.HBM_STRUCTURE_PLANE2_WEIGHT, HbmLegacyStructureSelection::isGentleNonInvalid);
        add(candidates, "desert_shack_1", "desert_shack_1", -7, 1, 128, false, HbmConfig.HBM_STRUCTURE_DESERT_SHACK1_WEIGHT, HbmLegacyStructureSelection::isSandy);
        add(candidates, "desert_shack_2", "desert_shack_2", -7, 1, 128, false, HbmConfig.HBM_STRUCTURE_DESERT_SHACK2_WEIGHT, HbmLegacyStructureSelection::isSandy);
        add(candidates, "desert_shack_3", "desert_shack_3", -5, 1, 128, false, HbmConfig.HBM_STRUCTURE_DESERT_SHACK3_WEIGHT, HbmLegacyStructureSelection::isSandy);
        add(candidates, "labolatory", "laboratory", -10, 53, 65, false, HbmConfig.HBM_STRUCTURE_LABORATORY_WEIGHT, HbmLegacyStructureSelection::isFlatBiome);
        add(candidates, "forest_post", "forest_post", -10, 1, 128, false, HbmConfig.HBM_STRUCTURE_FOREST_POST_WEIGHT, HbmLegacyStructureSelection::isGentleNonInvalid);
        add(candidates, "factory", "factory", -10, 1, 128, false, HbmConfig.HBM_STRUCTURE_FACTORY_WEIGHT, HbmLegacyStructureSelection::isFlatBiome);
        add(candidates, "crane", "crane", -9, 1, 128, false, HbmConfig.HBM_STRUCTURE_CRANE_WEIGHT, HbmLegacyStructureSelection::isFlatBiome);
        add(candidates, "broadcaster_tower", "broadcasting_tower", -9, 1, 128, false, HbmConfig.HBM_STRUCTURE_BROADCASTING_TOWER_WEIGHT, HbmLegacyStructureSelection::isFlatBiome);
        add(candidates, "radio", "radio_house", -6, 1, 128, false, HbmConfig.HBM_STRUCTURE_RADIO_WEIGHT, HbmLegacyStructureSelection::isFlatBiome);
        addProcedural(candidates, "features", HbmLegacyProceduralPiece.Kind.FEATURES, HbmConfig.HBM_STRUCTURE_FEATURES_WEIGHT, HbmLegacyStructureSelection::isProceduralBiome);
        addProcedural(candidates, "bunker", HbmLegacyProceduralPiece.Kind.BUNKER, HbmConfig.HBM_STRUCTURE_BUNKER_WEIGHT, HbmLegacyStructureSelection::isProceduralBiome);
        addJigsaw(candidates, "meteor_dungeon", "meteor/meteor-core", 0, 32, 32, false, HbmConfig.HBM_STRUCTURE_METEOR_DUNGEON_WEIGHT, HbmLegacyStructureSelection::isMeteorDungeonBiome, "start", 128, 128);
        add(candidates, "ruin1", "ntmruins_a", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_A_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin2", "ntmruins_b", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_B_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin3", "ntmruins_c", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_C_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin4", "ntmruins_d", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_D_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin5", "ntmruins_e", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_E_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin6", "ntmruins_f", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_F_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin7", "ntmruins_g", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_G_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin8", "ntmruins_h", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_H_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin9", "ntmruins_i", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_I_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        add(candidates, "ruin10", "ntmruins_j", -1, 1, 128, true, ruinWeight(HbmConfig.HBM_STRUCTURE_RUIN_J_WEIGHT), HbmLegacyStructureSelection::canSpawnRainRuin);
        addNull(candidates, HbmConfig.HBM_STRUCTURE_PLAINS_NULL_WEIGHT, HbmLegacyStructureSelection::isPlains);
        addNull(candidates, HbmConfig.HBM_STRUCTURE_OCEAN_NULL_WEIGHT, HbmLegacyStructureSelection::isOcean);

        int totalWeight = 0;
        for (Candidate candidate : candidates) {
            if (candidate.canSpawn.test(biome)) {
                totalWeight += Math.max(0, candidate.weight.getAsInt());
            }
        }
        if (totalWeight <= 0) {
            return null;
        }

        int selected = random.nextInt(totalWeight);
        for (Candidate candidate : candidates) {
            if (!candidate.canSpawn.test(biome)) {
                continue;
            }
            selected -= Math.max(0, candidate.weight.getAsInt());
            if (selected < 0) {
                return candidate.toSelected();
            }
        }
        return null;
    }

    private static void add(List<Candidate> candidates, String spawnName, String templateName, int heightOffset, int minHeight, int maxHeight, boolean conformToTerrain, IntSupplier weight, Predicate<Holder<Biome>> canSpawn) {
        candidates.add(new Candidate(spawnName, templateName, heightOffset, minHeight, maxHeight, conformToTerrain, weight, canSpawn));
    }

    private static void add(List<Candidate> candidates, String spawnName, String templateName, int heightOffset, int minHeight, int maxHeight, boolean conformToTerrain, ModConfigSpec.IntValue weight, Predicate<Holder<Biome>> canSpawn) {
        add(candidates, spawnName, templateName, heightOffset, minHeight, maxHeight, conformToTerrain, weight::get, canSpawn);
    }

    private static void addJigsaw(
            List<Candidate> candidates,
            String spawnName,
            String templateName,
            int heightOffset,
            int minHeight,
            int maxHeight,
            boolean conformToTerrain,
            ModConfigSpec.IntValue weight,
            Predicate<Holder<Biome>> canSpawn,
            String startPool,
            int sizeLimit,
            int rangeLimit
    ) {
        candidates.add(new Candidate(spawnName, templateName, heightOffset, minHeight, maxHeight, conformToTerrain, weight::get, canSpawn, true, startPool, sizeLimit, rangeLimit));
    }

    private static void addProcedural(List<Candidate> candidates, String spawnName, HbmLegacyProceduralPiece.Kind proceduralKind, ModConfigSpec.IntValue weight, Predicate<Holder<Biome>> canSpawn) {
        candidates.add(new Candidate(spawnName, "", 0, 1, 128, false, weight::get, canSpawn, false, "", 0, 0, proceduralKind));
    }

    private static void addNull(List<Candidate> candidates, IntSupplier weight, Predicate<Holder<Biome>> canSpawn) {
        candidates.add(new Candidate(null, null, 0, 1, 128, false, weight, canSpawn));
    }

    private static void addNull(List<Candidate> candidates, ModConfigSpec.IntValue weight, Predicate<Holder<Biome>> canSpawn) {
        addNull(candidates, weight::get, canSpawn);
    }

    private static IntSupplier oceanWeight(IntSupplier delegate) {
        return () -> HbmConfig.HBM_STRUCTURE_ENABLE_OCEAN.get() ? delegate.getAsInt() : 0;
    }

    private static IntSupplier oceanWeight(ModConfigSpec.IntValue delegate) {
        return oceanWeight(delegate::get);
    }

    private static IntSupplier ruinWeight(IntSupplier delegate) {
        return () -> HbmConfig.HBM_STRUCTURE_ENABLE_RUINS.get() ? delegate.getAsInt() : 0;
    }

    private static IntSupplier ruinWeight(ModConfigSpec.IntValue delegate) {
        return ruinWeight(delegate::get);
    }

    private static boolean isInvalidBiome(Holder<Biome> biome) {
        return biome.is(Biomes.THE_VOID) || biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER);
    }

    private static boolean isVeryFlatNonInvalid(Holder<Biome> biome) {
        return !isInvalidBiome(biome) && !biome.is(BiomeTags.IS_MOUNTAIN) && !biome.is(BiomeTags.IS_HILL);
    }

    private static boolean isGentleNonInvalid(Holder<Biome> biome) {
        return !isInvalidBiome(biome) && !biome.is(BiomeTags.IS_MOUNTAIN);
    }

    private static boolean isFlatBiome(Holder<Biome> biome) {
        return !isInvalidBiome(biome)
                && (isPlainsLike(biome) || biome.is(Biomes.DESERT) || biome.is(Biomes.SNOWY_PLAINS))
                && !biome.is(BiomeTags.IS_MOUNTAIN)
                && !biome.is(BiomeTags.IS_HILL);
    }

    private static boolean isSandy(Holder<Biome> biome) {
        return !isInvalidBiome(biome) && (biome.is(Biomes.DESERT) || biome.is(BiomeTags.IS_BADLANDS) || biome.is(BiomeTags.IS_BEACH));
    }

    private static boolean isOcean(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_OCEAN);
    }

    private static boolean isBeach(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_BEACH);
    }

    private static boolean isOceanOrBeach(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH);
    }

    private static boolean isPlains(Holder<Biome> biome) {
        return biome.is(Biomes.PLAINS);
    }

    private static boolean isPlainsLike(Holder<Biome> biome) {
        return biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS);
    }

    private static boolean canSpawnRainRuin(Holder<Biome> biome) {
        return !isInvalidBiome(biome) && biome.value().hasPrecipitation();
    }

    private static boolean isMeteorDungeonBiome(Holder<Biome> biome) {
        return !isInvalidBiome(biome);
    }

    private static boolean isProceduralBiome(Holder<Biome> biome) {
        return !isInvalidBiome(biome);
    }

    private record Candidate(
            String spawnName,
            String templateName,
            int heightOffset,
            int minHeight,
            int maxHeight,
            boolean conformToTerrain,
            IntSupplier weight,
            Predicate<Holder<Biome>> canSpawn,
            boolean jigsaw,
            String startPool,
            int sizeLimit,
            int rangeLimit,
            HbmLegacyProceduralPiece.Kind proceduralKind
    ) {
        private Candidate(String spawnName, String templateName, int heightOffset, int minHeight, int maxHeight, boolean conformToTerrain, IntSupplier weight, Predicate<Holder<Biome>> canSpawn) {
            this(spawnName, templateName, heightOffset, minHeight, maxHeight, conformToTerrain, weight, canSpawn, false, "", 0, 0, null);
        }

        private Candidate(String spawnName, String templateName, int heightOffset, int minHeight, int maxHeight, boolean conformToTerrain, IntSupplier weight, Predicate<Holder<Biome>> canSpawn, boolean jigsaw, String startPool, int sizeLimit, int rangeLimit) {
            this(spawnName, templateName, heightOffset, minHeight, maxHeight, conformToTerrain, weight, canSpawn, jigsaw, startPool, sizeLimit, rangeLimit, null);
        }

        SelectedStructure toSelected() {
            if (templateName == null) {
                return null;
            }
            return new SelectedStructure(spawnName, templateName, heightOffset, minHeight, maxHeight, conformToTerrain, jigsaw, startPool, sizeLimit, rangeLimit, proceduralKind);
        }
    }

    record SelectedStructure(
            String spawnName,
            String templateName,
            int heightOffset,
            int minHeight,
            int maxHeight,
            boolean conformToTerrain,
            boolean jigsaw,
            String startPool,
            int sizeLimit,
            int rangeLimit,
            HbmLegacyProceduralPiece.Kind proceduralKind
    ) {
        boolean procedural() {
            return proceduralKind != null;
        }
    }
}
