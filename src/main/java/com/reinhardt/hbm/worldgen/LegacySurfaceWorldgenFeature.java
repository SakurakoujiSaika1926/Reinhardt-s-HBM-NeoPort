package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.block.BobbleheadBlock;
import com.reinhardt.hbm.block.BobbleheadType;
import com.reinhardt.hbm.block.BroadcasterBlock;
import com.reinhardt.hbm.block.CrashedBombBlock;
import com.reinhardt.hbm.block.MustardWillowFlowerBlock;
import com.reinhardt.hbm.block.SafeBlock;
import com.reinhardt.hbm.blockentity.BobbleheadBlockEntity;
import com.reinhardt.hbm.blockentity.HbmStructureLoot;
import com.reinhardt.hbm.blockentity.LandmineBlockEntity;
import com.reinhardt.hbm.blockentity.SafeBlockEntity;
import com.reinhardt.hbm.blockentity.SoyuzCapsuleBlockEntity;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.worldgen.structure.HbmLegacyNbtTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The scattered overworld branch from HbmWorldGen.generateSurface in HBM 1.7.10.
 *
 * <p>Do not collapse the separate rolls into a shared rarity modifier: the old
 * generator made each branch consume its own random calls and several features
 * deliberately used different chunk coordinate offsets.</p>
 */
public final class LegacySurfaceWorldgenFeature extends Feature<NoneFeatureConfiguration> {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final int GAS_VEIN_SIZE = 32;
    private static final int BLUEPRINT_BOUNDS = 5000;
    private static final int BLUEPRINT_SPAWN_PROTECTION = 100;

    public LegacySurfaceWorldgenFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public LegacySurfaceWorldgenFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (level.getLevel() != null && level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int chunkX = origin.getX() & ~15;
        int chunkZ = origin.getZ() & ~15;
        Holder<Biome> biome = level.getBiome(new BlockPos(chunkX, 64, chunkZ));
        boolean placed = false;

        // 1.7.10 skipped plants during Tom impact.  The current port has no
        // TomSaveData equivalent yet, so only the already-existing branches are
        // mirrored here and no fake impact flag is introduced.
        placed |= placePlants(level, random, chunkX, chunkZ, biome);
        placed |= placeGasBubble(level, random, chunkX, chunkZ, HbmConfig.GAS_BUBBLE_SPAWN_RATE.get(),
                HbmBlocks.GAS_FLAMMABLE.get().defaultBlockState());
        placed |= placeGasBubble(level, random, chunkX, chunkZ, HbmConfig.EXPLOSIVE_GAS_BUBBLE_SPAWN_RATE.get(),
                HbmBlocks.GAS_EXPLOSIVE.get().defaultBlockState());

        boolean dungeons = dungeonGenerationEnabled(level);
        if (dungeons) {
            placed |= placeAntenna(level, random, chunkX, chunkZ, biome);
            placed |= placeDud(level, random, chunkX, chunkZ);
            placed |= placeBroadcaster(level, random, chunkX, chunkZ);
            placed |= placeApLandmine(level, random, chunkX, chunkZ);
            placed |= placeBosniaMine(level, random, chunkX, chunkZ);
            placed |= placeSoyuzCapsule(level, random, chunkX, chunkZ, biome);
            placed |= placePinkTree(level, random, chunkX, chunkZ);
            placed |= placeSafe(level, random, chunkX, chunkZ);
            placed |= placeArcticVault(level, random, chunkX, chunkZ);
        }

        placed |= placeStoneKeyhole(level, random, chunkX, chunkZ);
        placed |= placeBlueprintChest(level, random, chunkX, chunkZ);
        return placed;
    }

    private static boolean placePlants(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ, Holder<Biome> biome) {
        boolean placed = false;
        if (biome.is(BiomeTags.IS_FOREST) && random.nextInt(16) == 0) {
            placed |= generateFlowerPatch(level, random, chunkX, chunkZ,
                    flower(MustardWillowFlowerBlock.FOXGLOVE));
        }
        if (isDarkForest(biome) && random.nextInt(8) == 0) {
            placed |= generateFlowerPatch(level, random, chunkX, chunkZ,
                    flower(MustardWillowFlowerBlock.NIGHTSHADE));
        }
        if (biome.is(BiomeTags.IS_JUNGLE) && random.nextInt(8) == 0) {
            placed |= generateFlowerPatch(level, random, chunkX, chunkZ,
                    flower(MustardWillowFlowerBlock.TOBACCO));
        }
        if (random.nextInt(64) == 0) {
            placed |= generateFlowerPatch(level, random, chunkX, chunkZ,
                    flower(MustardWillowFlowerBlock.WEED));
        }
        if (biome.is(BiomeTags.IS_RIVER) && random.nextInt(4) == 0) {
            placed |= generateFlowerPatch(level, random, chunkX, chunkZ,
                    HbmBlocks.PLANT_REEDS.get().defaultBlockState());
        }
        if (biome.is(BiomeTags.IS_BEACH) && random.nextInt(8) == 0) {
            placed |= generateFlowerPatch(level, random, chunkX, chunkZ,
                    HbmBlocks.PLANT_REEDS.get().defaultBlockState());
        }
        return placed;
    }

    private static BlockState flower(int meta) {
        return HbmBlocks.PLANT_FLOWER.get().defaultBlockState().setValue(MustardWillowFlowerBlock.META, meta);
    }

    private static boolean isDarkForest(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.equals(Biomes.DARK_FOREST)).orElse(false);
    }

    private static boolean generateFlowerPatch(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ, BlockState state) {
        int centerX = chunkX + random.nextInt(16) + 8;
        int centerZ = chunkZ + random.nextInt(16) + 8;
        int centerY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centerX, centerZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int attempt = 0; attempt < 64; attempt++) {
            pos.set(centerX + random.nextInt(8) - random.nextInt(8),
                    centerY + random.nextInt(4) - random.nextInt(4),
                    centerZ + random.nextInt(8) - random.nextInt(8));
            if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
                continue;
            }
            if (level.isEmptyBlock(pos) && state.canSurvive(level, pos)) {
                level.setBlock(pos, state, FLAGS);
                placed = true;
            }
        }
        return placed;
    }

    private static boolean placeGasBubble(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ, int rate, BlockState gas) {
        if (rate <= 0 || random.nextInt(rate) != 0) {
            return false;
        }
        int x = chunkX + random.nextInt(16);
        int y = 30 + random.nextInt(10);
        int z = chunkZ + random.nextInt(16);
        return generateMinable(level, random, x, y, z, gas, GAS_VEIN_SIZE, Blocks.STONE.defaultBlockState());
    }

    private static boolean placeAntenna(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ, Holder<Biome> biome) {
        int rate = HbmConfig.ANTENNA_STRUCTURE_SPAWN_RATE.get();
        if (rate <= 0 || !isAntennaBiome(biome) || random.nextInt(rate) != 0) {
            return false;
        }
        int x = chunkX + random.nextInt(16);
        int z = chunkZ + random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        return generateAntenna(level, random, x, y, z);
    }

    private static boolean isAntennaBiome(Holder<Biome> biome) {
        Biome value = biome.value();
        return value.getBaseTemperature() >= 0.4F
                && value.getModifiedClimateSettings().downfall() <= 0.6F;
    }

    private static boolean generateAntenna(WorldGenLevel level, RandomSource random, int x, int y, int z) {
        BlockPos spawnCheck = new BlockPos(x + 1, y, z + 1);
        if (!isAntennaPlacementValid(level, spawnCheck)
                || y < level.getMinBuildHeight()
                || y + 20 >= level.getMaxBuildHeight()) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= 20; dy++) {
            for (int dx = 0; dx <= 2; dx++) {
                for (int dz = 0; dz <= 2; dz++) {
                    cursor.set(x + dx, y + dy, z + dz);
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), FLAGS);
                }
            }
        }

        setLegacy(level, cursor, x + 1, y, z, "reinhardtshbm:steel_poles", 2);
        setLegacy(level, cursor, x, y, z + 1, "reinhardtshbm:steel_poles", 4);
        setLegacy(level, cursor, x + 1, y, z + 1, "reinhardtshbm:deco_steel", 0);
        setLegacy(level, cursor, x + 2, y, z + 1, "reinhardtshbm:tape_recorder", 5);
        setLegacy(level, cursor, x + 1, y, z + 2, "reinhardtshbm:steel_poles", 3);
        setAntennaChest(level, random, x + 2, y, z + 2);

        setLegacy(level, cursor, x + 1, y + 1, z, "reinhardtshbm:steel_poles", 2);
        setLegacy(level, cursor, x, y + 1, z + 1, "reinhardtshbm:steel_poles", 4);
        setLegacy(level, cursor, x + 1, y + 1, z + 1, "reinhardtshbm:deco_steel", 0);
        setLegacy(level, cursor, x + 2, y + 1, z + 1, "reinhardtshbm:tape_recorder", 5);
        setLegacy(level, cursor, x + 1, y + 1, z + 2, "reinhardtshbm:steel_poles", 3);

        setLegacy(level, cursor, x + 1, y + 2, z, "reinhardtshbm:deco_steel", 0);
        setLegacy(level, cursor, x, y + 2, z + 1, "reinhardtshbm:deco_steel", 0);
        setLegacy(level, cursor, x + 1, y + 2, z + 1, "reinhardtshbm:deco_steel", 0);
        setLegacy(level, cursor, x + 2, y + 2, z + 1, "reinhardtshbm:deco_steel", 0);
        setLegacy(level, cursor, x + 1, y + 2, z + 2, "reinhardtshbm:deco_steel", 0);

        for (int dy = 3; dy <= 12; dy++) {
            setLegacy(level, cursor, x + 1, y + dy, z + 1, "reinhardtshbm:steel_poles", 4);
        }
        setLegacy(level, cursor, x + 1, y + 13, z + 1, "reinhardtshbm:pole_satellite_receiver", 3);
        for (int dy = 14; dy <= 16; dy++) {
            setLegacy(level, cursor, x + 1, y + dy, z + 1, "reinhardtshbm:steel_poles", 4);
        }
        setLegacy(level, cursor, x + 1, y + 17, z + 1, "reinhardtshbm:pole_satellite_receiver", 2);
        setLegacy(level, cursor, x + 1, y + 18, z + 1, "reinhardtshbm:pole_satellite_receiver", 4);
        setLegacy(level, cursor, x + 1, y + 19, z + 1, "reinhardtshbm:steel_poles", 4);
        setLegacy(level, cursor, x + 1, y + 20, z + 1, "reinhardtshbm:pole_top", 4);
        return true;
    }

    private static boolean isAntennaPlacementValid(WorldGenLevel level, BlockPos pos) {
        if (!level.isEmptyBlock(pos)) {
            return false;
        }
        BlockState support = level.getBlockState(pos.below());
        if (isAntennaBase(support)) {
            return true;
        }
        return (support.is(Blocks.SNOW) || support.getBlock() instanceof BushBlock)
                && isAntennaBase(level.getBlockState(pos.below(2)));
    }

    private static boolean isAntennaBase(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.STONE)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND);
    }

    private static void setLegacy(WorldGenLevel level, BlockPos.MutableBlockPos cursor, int x, int y, int z, String blockId, int meta) {
        cursor.set(x, y, z);
        level.setBlock(cursor, HbmLegacyNbtTemplate.stateFromLegacyId(blockId, meta), FLAGS);
    }

    private static void setAntennaChest(WorldGenLevel level, RandomSource random, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState chest = Blocks.CHEST.defaultBlockState();
        if (chest.hasProperty(ChestBlock.FACING)) {
            chest = chest.setValue(ChestBlock.FACING, Direction.EAST);
        }
        level.setBlock(pos, chest, FLAGS);
        if (level.getBlockEntity(pos) instanceof Container container) {
            HbmStructureLoot.fillContainer(container, "POOL_ANTENNA", 8, 9, random);
        }
    }

    private static boolean placeArcticVault(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        int rate = HbmConfig.ARCTIC_VAULT_SPAWN_RATE.get();
        if (rate <= 0 || random.nextInt(rate) != 0) {
            return false;
        }
        int x = chunkX + random.nextInt(16);
        int z = chunkZ + random.nextInt(16);
        int y = 16 + random.nextInt(32) - 1;
        Holder<Biome> biome = level.getBiome(new BlockPos(x, y, z));
        if (biome.value().getBaseTemperature() >= 0.2F
                || !isArcticVaultRock(level.getBlockState(new BlockPos(x, y, z)))) {
            return false;
        }
        buildArcticVault(level, random, x, y, z);
        return true;
    }

    private static boolean isArcticVaultRock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    private static void buildArcticVault(WorldGenLevel level, RandomSource random, int x, int y, int z) {
        LegacyBlockChoice stoneBrick = new LegacyBlockChoice("minecraft:stonebrick", 0);
        LegacyBlockChoice crackedStoneBrick = new LegacyBlockChoice("minecraft:stonebrick", 2);
        LegacyBlockChoice air = new LegacyBlockChoice("minecraft:air", 0);
        LegacyBlockChoice web = new LegacyBlockChoice("minecraft:web", 0);
        LegacyBlockChoice[] brick = {stoneBrick, crackedStoneBrick};
        LegacyBlockChoice[] webOrAir = {air, air, web};
        LegacyBlockChoice[] crates = {
                new LegacyBlockChoice("reinhardtshbm:crate", 0),
                new LegacyBlockChoice("reinhardtshbm:crate_metal", 0),
                new LegacyBlockChoice("reinhardtshbm:crate_ammo", 0),
                new LegacyBlockChoice("reinhardtshbm:crate_can", 0),
                new LegacyBlockChoice("reinhardtshbm:crate_jungle", 0)
        };

        fillLegacyBox(level, random, x - 5, y, z - 5, 11, 1, 11, brick);
        fillLegacyBox(level, random, x - 5, y + 6, z - 5, 11, 1, 11, brick);
        fillLegacyBox(level, random, x - 5, y + 1, z - 5, 11, 5, 1, brick);
        fillLegacyBox(level, random, x - 5, y + 1, z + 5, 11, 5, 1, brick);
        fillLegacyBox(level, random, x - 5, y + 1, z - 5, 1, 5, 11, brick);
        fillLegacyBox(level, random, x + 5, y + 1, z - 5, 1, 5, 11, brick);
        fillBox(level, x - 4, y + 1, z - 4, 9, 3, 9, Blocks.AIR.defaultBlockState());
        fillBox(level, x - 4, y + 1, z - 4, 9, 1, 9, Blocks.SNOW.defaultBlockState());
        fillLegacyBox(level, x - 2, y + 1, z - 2, 5, 2, 1, "reinhardtshbm:tape_recorder", 3);
        fillBox(level, x - 2, y + 3, z - 2, 5, 1, 1, Blocks.SNOW.defaultBlockState());
        fillLegacyBox(level, x - 2, y + 1, z + 2, 5, 2, 1, "reinhardtshbm:tape_recorder", 2);
        fillBox(level, x - 2, y + 3, z + 2, 5, 1, 1, Blocks.SNOW.defaultBlockState());
        fillLegacyBox(level, random, x - 4, y + 4, z - 4, 9, 2, 9, webOrAir);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 15; i++) {
            int ix = x - 4 + random.nextInt(10);
            int iz = z - 4 + random.nextInt(10);
            cursor.set(ix, y + 1, iz);
            if (!level.getBlockState(cursor).is(Blocks.SNOW)) {
                continue;
            }
            if (i == 0) {
                BlockState state = HbmBlocks.BOBBLEHEAD.get().defaultBlockState()
                        .setValue(BobbleheadBlock.ROTATION, random.nextInt(16));
                level.setBlock(cursor, state, FLAGS);
                if (level.getBlockEntity(cursor) instanceof BobbleheadBlockEntity bobble) {
                    bobble.setType(BobbleheadType.byOrdinal(random.nextInt(BobbleheadType.values().length - 1) + 1));
                }
            } else {
                LegacyBlockChoice crate = crates[random.nextInt(crates.length)];
                level.setBlock(cursor, HbmLegacyNbtTemplate.stateFromLegacyId(crate.id(), crate.meta()), FLAGS);
                level.setBlock(cursor.above(), Blocks.SNOW.defaultBlockState(), FLAGS);
            }
        }

        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        BlockPos surface = new BlockPos(x, surfaceY, z);
        if (canPlaceOnTop(level, surface.below())) {
            level.setBlock(surface, HbmLegacyNbtTemplate.stateFromLegacyId("reinhardtshbm:tape_recorder", 0), FLAGS);
        }
    }

    private static void fillBox(WorldGenLevel level, int x, int y, int z, int sx, int sy, int sz, BlockState state) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int ix = x; ix < x + sx; ix++) {
            for (int iy = y; iy < y + sy; iy++) {
                if (iy < level.getMinBuildHeight() || iy >= level.getMaxBuildHeight()) {
                    continue;
                }
                for (int iz = z; iz < z + sz; iz++) {
                    cursor.set(ix, iy, iz);
                    level.setBlock(cursor, state, FLAGS);
                }
            }
        }
    }

    private static void fillLegacyBox(WorldGenLevel level, int x, int y, int z, int sx, int sy, int sz, String blockId, int meta) {
        fillBox(level, x, y, z, sx, sy, sz, HbmLegacyNbtTemplate.stateFromLegacyId(blockId, meta));
    }

    private static void fillLegacyBox(WorldGenLevel level, RandomSource random, int x, int y, int z, int sx, int sy, int sz, LegacyBlockChoice[] choices) {
        if (choices.length == 0) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int ix = x; ix < x + sx; ix++) {
            for (int iy = y; iy < y + sy; iy++) {
                if (iy < level.getMinBuildHeight() || iy >= level.getMaxBuildHeight()) {
                    continue;
                }
                for (int iz = z; iz < z + sz; iz++) {
                    LegacyBlockChoice choice = choices[random.nextInt(choices.length)];
                    cursor.set(ix, iy, iz);
                    level.setBlock(cursor, HbmLegacyNbtTemplate.stateFromLegacyId(choice.id(), choice.meta()), FLAGS);
                }
            }
        }
    }

    private record LegacyBlockChoice(String id, int meta) {
    }

    private static boolean placeDud(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        int rate = HbmConfig.DUD_STRUCTURE_SPAWN_RATE.get();
        if (rate <= 0 || random.nextInt(rate) != 0) {
            return false;
        }
        int x = chunkX + 8 + random.nextInt(16);
        int z = chunkZ + 8 + random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (!isDudPlacementValid(level, pos)) {
            return false;
        }
        level.setBlock(pos, HbmBlocks.CRASHED_BOMB.get().defaultBlockState()
                .setValue(CrashedBombBlock.VARIANT, random.nextInt(CrashedBombBlock.Type.values().length)), FLAGS);
        return true;
    }

    private static boolean isDudPlacementValid(WorldGenLevel level, BlockPos pos) {
        if (!level.isEmptyBlock(pos)) {
            return false;
        }
        BlockState support = level.getBlockState(pos.below());
        if (isDudBase(support)) {
            return true;
        }
        return (support.is(Blocks.SNOW) || support.getBlock() instanceof BushBlock)
                && isDudBase(level.getBlockState(pos.below(2)));
    }

    private static boolean isDudBase(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.STONE)
                || state.is(Blocks.SAND)
                || state.is(Blocks.RED_SAND)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE);
    }

    private static boolean placeBroadcaster(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        int rate = HbmConfig.BROADCASTER_SPAWN_RATE.get();
        if (rate <= 0 || random.nextInt(rate) != 0) {
            return false;
        }
        int x = chunkX + random.nextInt(16);
        int z = chunkZ + random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (!canPlaceOnTop(level, pos.below()) || !level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        level.setBlock(pos, HbmBlocks.BROADCASTER_PC.get().defaultBlockState()
                .setValue(BroadcasterBlock.FACING, randomLegacyFacing(random)), FLAGS);
        return true;
    }

    private static boolean placeApLandmine(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        int rate = HbmConfig.LANDMINE_SPAWN_RATE.get();
        if (!HbmConfig.ENABLE_WORLDGEN_LANDMINES.get() || rate <= 0 || random.nextInt(rate) != 0) {
            return false;
        }
        int x = chunkX + random.nextInt(16) + 8;
        int z = chunkZ + random.nextInt(16) + 8;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        for (int g = y + 2; g >= y; g--) {
            BlockPos pos = new BlockPos(x, g, z);
            if (canPlaceOnTop(level, pos.below())) {
                return placeMine(level, pos, HbmBlocks.MINE_AP.get().defaultBlockState());
            }
        }
        return false;
    }

    private static boolean placeBosniaMine(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        if (!HbmConfig.ENABLE_528_BOSNIA_MINES.get() || random.nextInt(16) != 0) {
            return false;
        }
        int x = chunkX + random.nextInt(16);
        int z = chunkZ + random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        return canPlaceOnTop(level, pos.below()) && placeMine(level, pos, HbmBlocks.MINE_HE.get().defaultBlockState());
    }

    private static boolean placeMine(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        level.setBlock(pos, state, FLAGS);
        if (level.getBlockEntity(pos) instanceof LandmineBlockEntity mine) {
            mine.setWaitingForPlayer(true);
        }
        return true;
    }

    private static boolean placeSoyuzCapsule(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ, Holder<Biome> biome) {
        int rate = HbmConfig.SOYUZ_CAPSULE_SPAWN_RATE.get();
        if (rate <= 0 || !biome.is(BiomeTags.IS_BEACH) || random.nextInt(rate) != 0) {
            return false;
        }
        int x = chunkX + random.nextInt(16);
        int z = chunkZ + random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 4;
        BlockPos pos = new BlockPos(x, y, z);
        if (!canPlaceOnTop(level, pos.above())) {
            return false;
        }
        level.setBlock(pos, HbmBlocks.SOYUZ_CAPSULE.get().defaultBlockState(), FLAGS);
        if (level.getBlockEntity(pos) instanceof SoyuzCapsuleBlockEntity capsule) {
            capsule.setItem(random.nextInt(capsule.getContainerSize()), new ItemStack(HbmItems.RECORD_GLASS.get()));
        }
        return true;
    }

    private static boolean placePinkTree(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        int rate = HbmConfig.PINK_TREE_SPAWN_RATE.get();
        if (rate <= 0 || random.nextInt(rate) != 0) {
            return false;
        }
        int x = chunkX + random.nextInt(16);
        int z = chunkZ + random.nextInt(16);
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getMinBuildHeight(), z);
        int maxY = Math.min(level.getMaxBuildHeight(), 256);
        for (int y = 0; y < maxY; y++) {
            if (y < level.getMinBuildHeight()) {
                continue;
            }
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.OAK_LOG)) {
                BlockState pink = HbmBlocks.PINK_LOG.get().defaultBlockState();
                if (state.hasProperty(RotatedPillarBlock.AXIS) && pink.hasProperty(RotatedPillarBlock.AXIS)) {
                    pink = pink.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
                }
                level.setBlock(pos, pink, FLAGS);
                placed = true;
            }
        }
        return placed;
    }

    private static boolean placeSafe(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        int rate = HbmConfig.VAULT_SPAWN_RATE.get();
        if (!HbmConfig.ENABLE_WORLDGEN_VAULTS.get() || rate <= 0 || random.nextInt(rate) != 0) {
            return false;
        }
        int x = chunkX + random.nextInt(16);
        int z = chunkZ + random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (!canPlaceOnTop(level, pos.below()) || !level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        level.setBlock(pos, HbmBlocks.SAFE.get().defaultBlockState()
                .setValue(SafeBlock.FACING, randomLegacyFacing(random)), FLAGS);
        if (level.getBlockEntity(pos) instanceof SafeBlockEntity safe) {
            switch (random.nextInt(10)) {
                case 0, 1, 2, 3 -> {
                    safe.setLockMod(1.0D);
                    HbmStructureLoot.fillContainer(safe, "POOL_VAULT_RUSTY", 3, 7, random);
                }
                case 4, 5, 6 -> {
                    safe.setLockMod(0.1D);
                    HbmStructureLoot.fillContainer(safe, "POOL_VAULT_STANDARD", 2, 5, random);
                }
                case 7, 8 -> {
                    safe.setLockMod(0.02D);
                    HbmStructureLoot.fillContainer(safe, "POOL_VAULT_REINFORCED", 1, 4, random);
                }
                case 9 -> {
                    safe.setLockMod(0.0D);
                    HbmStructureLoot.fillContainer(safe, "POOL_VAULT_UNBREAKABLE", 1, 3, random);
                }
            }
            safe.setPins(random.nextInt(999) + 1);
            safe.lock();
            if (random.nextInt(10) < 3) {
                safe.fillWithSpiders();
            }
        }
        return true;
    }

    private static boolean placeStoneKeyhole(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        int rate = HbmConfig.STONE_KEYHOLE_SPAWN_RATE.get();
        if (rate <= 0 || random.nextInt(rate) != 0) {
            return false;
        }
        int x = chunkX + random.nextInt(16) + 8;
        int y = 6 + random.nextInt(13);
        int z = chunkZ + random.nextInt(16) + 8;
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos).is(Blocks.STONE)) {
            return false;
        }
        level.setBlock(pos, HbmBlocks.STONE_KEYHOLE.get().defaultBlockState(), FLAGS);
        return true;
    }

    private static boolean placeBlueprintChest(WorldGenLevel level, RandomSource random, int chunkX, int chunkZ) {
        if (Math.abs(chunkX) < BLUEPRINT_SPAWN_PROTECTION && Math.abs(chunkZ) < BLUEPRINT_SPAWN_PROTECTION) {
            return false;
        }
        if (random.nextInt(20) < 10) {
            return false;
        }
        int cX = Math.abs(chunkX) % BLUEPRINT_BOUNDS;
        int cZ = Math.abs(chunkZ) % BLUEPRINT_BOUNDS;
        if (cX >= 16 || cZ >= 16) {
            return false;
        }

        int x = chunkX + 8;
        int z = chunkZ + 8;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - random.nextInt(2);
        BlockPos chestPos = new BlockPos(x, y, z);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), FLAGS);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int a = x - 1; a <= x + 1; a++) {
            for (int b = y - 1; b <= y + 1; b++) {
                if (b < level.getMinBuildHeight() || b >= level.getMaxBuildHeight()) {
                    continue;
                }
                for (int c = z - 1; c <= z + 1; c++) {
                    if (a == x && b == y && c == z) {
                        continue;
                    }
                    cursor.set(a, b, c);
                    level.setBlock(cursor, Blocks.OBSIDIAN.defaultBlockState(), FLAGS);
                }
            }
        }

        if (level.getBlockEntity(chestPos) instanceof Container container) {
            HbmStructureLoot.fillContainer(container, "POOL_BLUEPRINTS", 50, 51, random);
        }
        return true;
    }

    private static boolean generateMinable(WorldGenLevel level, RandomSource random, int centerX, int centerY, int centerZ,
                                           BlockState generatedState, int amount, BlockState replaceState) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        float angle = random.nextFloat() * (float) Math.PI;
        double startX = centerX + 8.0D + Mth.sin(angle) * amount / 8.0F;
        double endX = centerX + 8.0D - Mth.sin(angle) * amount / 8.0F;
        double startZ = centerZ + 8.0D + Mth.cos(angle) * amount / 8.0F;
        double endZ = centerZ + 8.0D - Mth.cos(angle) * amount / 8.0F;
        double startY = centerY + random.nextInt(3) - 2;
        double endY = centerY + random.nextInt(3) - 2;
        boolean placed = false;

        for (int i = 0; i <= amount; i++) {
            double x = startX + (endX - startX) * i / amount;
            double y = startY + (endY - startY) * i / amount;
            double z = startZ + (endZ - startZ) * i / amount;
            double radiusRand = random.nextDouble() * amount / 16.0D;
            double horizontal = (Mth.sin(i * (float) Math.PI / amount) + 1.0F) * radiusRand + 1.0D;
            double vertical = (Mth.sin(i * (float) Math.PI / amount) + 1.0F) * radiusRand + 1.0D;
            int minX = Mth.floor(x - horizontal / 2.0D);
            int minY = Mth.floor(y - vertical / 2.0D);
            int minZ = Mth.floor(z - horizontal / 2.0D);
            int maxX = Mth.floor(x + horizontal / 2.0D);
            int maxY = Mth.floor(y + vertical / 2.0D);
            int maxZ = Mth.floor(z + horizontal / 2.0D);

            for (int px = minX; px <= maxX; px++) {
                double dx = (px + 0.5D - x) / (horizontal / 2.0D);
                if (dx * dx >= 1.0D) {
                    continue;
                }
                for (int py = minY; py <= maxY; py++) {
                    if (py < level.getMinBuildHeight() || py >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    double dy = (py + 0.5D - y) / (vertical / 2.0D);
                    if (dx * dx + dy * dy >= 1.0D) {
                        continue;
                    }
                    for (int pz = minZ; pz <= maxZ; pz++) {
                        double dz = (pz + 0.5D - z) / (horizontal / 2.0D);
                        if (dx * dx + dy * dy + dz * dz >= 1.0D) {
                            continue;
                        }
                        pos.set(px, py, pz);
                        if (level.getBlockState(pos).is(replaceState.getBlock())) {
                            level.setBlock(pos, generatedState, FLAGS);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    private static boolean dungeonGenerationEnabled(WorldGenLevel level) {
        if (level.getLevel() == null || level.getLevel().getServer() == null) {
            return true;
        }
        return HbmConfig.legacyDungeonGenerationEnabled(level.getLevel().getServer().getWorldData().worldGenOptions().generateStructures());
    }

    private static boolean canPlaceOnTop(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
    }

    private static Direction randomLegacyFacing(RandomSource random) {
        return switch (random.nextInt(4) + 2) {
            case 2 -> Direction.NORTH;
            case 3 -> Direction.SOUTH;
            case 4 -> Direction.WEST;
            default -> Direction.EAST;
        };
    }
}
