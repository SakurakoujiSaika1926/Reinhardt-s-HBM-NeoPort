package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.block.BobbleheadBlock;
import com.reinhardt.hbm.block.BobbleheadType;
import com.reinhardt.hbm.blockentity.BobbleheadBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmWorldgenStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

public final class HbmLegacyProceduralPiece extends StructurePiece {
    private static final int MAX_SURFACE_SIZE = 64;
    private static final int MAX_BUNKER_SIZE = 96;

    public enum Kind {
        FEATURES,
        BUNKER;

        static Kind byName(String name) {
            for (Kind kind : values()) {
                if (kind.name().equals(name)) {
                    return kind;
                }
            }
            return FEATURES;
        }
    }

    private final Kind kind;
    private final String spawnName;
    private final int originX;
    private final int originZ;
    private final long seed;
    private final int rotation;
    private final boolean flatBiome;
    private final boolean hotDryBiome;
    private final boolean badlandsBiome;

    public HbmLegacyProceduralPiece(
            HbmLegacyStructureSelection.SelectedStructure selected,
            BlockPos origin,
            long seed,
            int rotation,
            boolean flatBiome,
            boolean hotDryBiome,
            boolean badlandsBiome,
            int minBuildHeight,
            int maxBuildHeight
    ) {
        super(
                HbmWorldgenStructures.HBM_LEGACY_PROCEDURAL_PIECE.get(),
                0,
                makeBoundingBox(selected.proceduralKind(), origin, minBuildHeight, maxBuildHeight)
        );
        this.kind = selected.proceduralKind();
        this.spawnName = selected.spawnName();
        this.originX = origin.getX();
        this.originZ = origin.getZ();
        this.seed = seed;
        this.rotation = rotation & 3;
        this.flatBiome = flatBiome;
        this.hotDryBiome = hotDryBiome;
        this.badlandsBiome = badlandsBiome;
        this.setOrientation(null);
    }

    public HbmLegacyProceduralPiece(CompoundTag tag) {
        super(HbmWorldgenStructures.HBM_LEGACY_PROCEDURAL_PIECE.get(), tag);
        this.kind = Kind.byName(tag.getString("Kind"));
        this.spawnName = tag.getString("SpawnName");
        this.originX = tag.getInt("OriginX");
        this.originZ = tag.getInt("OriginZ");
        this.seed = tag.getLong("Seed");
        this.rotation = tag.getInt("Rotation") & 3;
        this.flatBiome = tag.getBoolean("FlatBiome");
        this.hotDryBiome = tag.getBoolean("HotDryBiome");
        this.badlandsBiome = tag.getBoolean("BadlandsBiome");
        this.setOrientation(null);
    }

    private static BoundingBox makeBoundingBox(Kind kind, BlockPos origin, int minBuildHeight, int maxBuildHeight) {
        int radius = kind == Kind.BUNKER ? MAX_BUNKER_SIZE : MAX_SURFACE_SIZE;
        int minY = kind == Kind.BUNKER ? Math.max(minBuildHeight, 1) : minBuildHeight;
        int maxY = kind == Kind.BUNKER ? Math.min(maxBuildHeight - 1, 96) : maxBuildHeight - 1;
        return new BoundingBox(
                origin.getX() - radius,
                minY,
                origin.getZ() - radius,
                origin.getX() + radius,
                maxY,
                origin.getZ() + radius
        );
    }

    static boolean isFlatBiome(net.minecraft.core.Holder<Biome> biome) {
        return !biome.is(BiomeTags.IS_HILL) && !biome.is(BiomeTags.IS_MOUNTAIN);
    }

    static boolean isHotDryBiome(net.minecraft.core.Holder<Biome> biome) {
        Biome value = biome.value();
        return value.getBaseTemperature() >= 1.0F && value.getModifiedClimateSettings().downfall() <= 0.0F;
    }

    static boolean isBadlandsBiome(net.minecraft.core.Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_BADLANDS);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Kind", this.kind.name());
        tag.putString("SpawnName", this.spawnName);
        tag.putInt("OriginX", this.originX);
        tag.putInt("OriginZ", this.originZ);
        tag.putLong("Seed", this.seed);
        tag.putInt("Rotation", this.rotation);
        tag.putBoolean("FlatBiome", this.flatBiome);
        tag.putBoolean("HotDryBiome", this.hotDryBiome);
        tag.putBoolean("BadlandsBiome", this.badlandsBiome);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource ignored,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            BlockPos pos
    ) {
        RandomSource random = RandomSource.create(this.seed);
        if (this.kind == Kind.BUNKER) {
            BunkerGenerator.generate(level, chunkBox, random, this.originX, this.originZ, this.rotation);
            return;
        }
        FeatureGenerator.generate(level, chunkBox, random, this.originX, this.originZ, this.rotation, this.flatBiome, this.hotDryBiome, this.badlandsBiome);
    }

    private static final class FeatureGenerator {
        private FeatureGenerator() {
        }

        static void generate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int originX, int originZ, int rotation, boolean flatBiome, boolean hotDryBiome, boolean badlandsBiome) {
            if (flatBiome && random.nextInt(10) == 0) {
                placeSiloSurfaceMarker(level, chunkBox, random, originX, originZ, rotation);
                return;
            }
            if (hotDryBiome && !badlandsBiome) {
                if (random.nextBoolean()) {
                    placeDesertHouseOne(level, chunkBox, random, originX, originZ, rotation);
                } else {
                    placeDesertHouseTwo(level, chunkBox, random, originX, originZ, rotation);
                }
                return;
            }

            switch (random.nextInt(6)) {
                case 0 -> placeLabTwo(level, chunkBox, random, originX, originZ, rotation);
                case 1 -> placeLabOne(level, chunkBox, random, originX, originZ, rotation);
                case 2 -> placeOffice(level, chunkBox, random, originX, originZ, rotation, false);
                case 3 -> placeOffice(level, chunkBox, random, originX, originZ, rotation, true);
                default -> placeRuralHouse(level, chunkBox, random, originX, originZ, rotation);
            }
        }

        private static void placeDesertHouseOne(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int originX, int originZ, int rotation) {
            Placement p = Placement.surface(level, originX, originZ, rotation, 10, 5, 7);
            p.foundation(level, chunkBox, "minecraft:sandstone", 0, 0, 0, 9, 6, -1);
            p.fillSandstone(level, chunkBox, random, 0, 0, 0, 9, 0, 0);
            p.fillSandstone(level, chunkBox, random, 0, 1, 0, 1, 1, 0);
            p.set(level, chunkBox, 2, 1, 0, "minecraft:oak_fence", 0);
            p.fillSandstone(level, chunkBox, random, 3, 1, 0, 5, 1, 0);
            p.set(level, chunkBox, 6, 1, 0, "minecraft:oak_fence", 0);
            p.set(level, chunkBox, 7, 1, 0, "minecraft:oak_fence", 0);
            p.fillSandstone(level, chunkBox, random, 8, 1, 0, 9, 1, 0);
            p.fillSandstone(level, chunkBox, random, 0, 2, 0, 7, 2, 0);
            p.fillSandstone(level, chunkBox, random, 0, 0, 0, 0, 1, 6);
            p.set(level, chunkBox, 0, 2, 1, "minecraft:sandstone_slab", 0);
            p.fill(level, chunkBox, 0, 2, 3, 0, 2, 6, "minecraft:sandstone_slab", 0);
            p.fillSandstone(level, chunkBox, random, 1, 0, 6, 1, 1, 6);
            p.fillSandstone(level, chunkBox, random, 3, 0, 6, 9, 1, 6);
            p.fillSandstone(level, chunkBox, random, 1, 2, 6, 3, 2, 6);
            p.fill(level, chunkBox, 4, 2, 6, 5, 2, 6, "minecraft:sandstone_slab", 0);
            p.set(level, chunkBox, 7, 2, 6, "minecraft:sandstone_slab", 0);
            p.fillSandstone(level, chunkBox, random, 9, 0, 0, 9, 0, 6);
            p.fillRandom(level, chunkBox, random, 0.65F, 9, 1, 1, 9, 1, 5, "minecraft:sand", 0);
            p.fillSandstone(level, chunkBox, random, 4, 0, 1, 4, 1, 3);
            p.set(level, chunkBox, 4, 0, 4, "reinhardtshbm:reinforced_sand", 0);

            p.set(level, chunkBox, 1, 0, 1, "reinhardtshbm:crate_weapon", 0);
            p.set(level, chunkBox, 3, 0, 1, "minecraft:chest", 0);
            p.fill(level, chunkBox, 5, 0, 1, 6, 0, 1, "reinhardtshbm:crate", 0);
            p.set(level, chunkBox, 7, 0, 1, "minecraft:sand", 0);
            if (random.nextFloat() <= 0.25F) {
                p.set(level, chunkBox, 8, 0, 1, "reinhardtshbm:crate_metal", 0);
            }
            p.fillRandom(level, chunkBox, random, 0.25F, 1, 0, 2, 3, 0, 5, "minecraft:sand", 0);
            p.fillRandom(level, chunkBox, random, 0.25F, 5, 0, 2, 8, 0, 5, "minecraft:sand", 0);
        }

        private static void placeDesertHouseTwo(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int originX, int originZ, int rotation) {
            Placement p = Placement.surface(level, originX, originZ, rotation, 16, 6, 10);
            p.foundation(level, chunkBox, "minecraft:sandstone", 0, 0, 0, 6, 9, -1);
            p.foundation(level, chunkBox, "minecraft:sandstone", 0, 9, 0, 15, 9, -1);
            p.fill(level, chunkBox, 1, 1, 1, 5, 3, 8, "minecraft:air", 0);
            p.fillSandstone(level, chunkBox, random, 0, 0, 0, 6, 1, 0);
            p.fillSandstone(level, chunkBox, random, 0, 2, 0, 1, 2, 0);
            p.set(level, chunkBox, 2, 2, 0, "minecraft:oak_fence", 0);
            p.fillSandstone(level, chunkBox, random, 3, 2, 0, 3, 2, 0);
            p.set(level, chunkBox, 4, 2, 0, "minecraft:oak_fence", 0);
            p.fillSandstone(level, chunkBox, random, 5, 2, 0, 6, 2, 0);
            p.fillSandstone(level, chunkBox, random, 0, 3, 0, 6, 3, 0);
            p.fillSandstone(level, chunkBox, random, 0, 0, 1, 0, 3, 9);
            p.fillSandstone(level, chunkBox, random, 1, 0, 9, 6, 1, 9);
            p.fillSandstone(level, chunkBox, random, 1, 2, 9, 1, 2, 9);
            p.fill(level, chunkBox, 2, 2, 9, 4, 2, 9, "minecraft:oak_fence", 0);
            p.fillSandstone(level, chunkBox, random, 5, 2, 9, 6, 2, 9);
            p.fillSandstone(level, chunkBox, random, 1, 3, 9, 6, 3, 9);
            p.fillSandstone(level, chunkBox, random, 6, 0, 8, 6, 3, 8);
            p.fillSandstone(level, chunkBox, random, 6, 0, 7, 6, 0, 7);
            p.fillSandstone(level, chunkBox, random, 6, 3, 7, 6, 3, 7);
            p.fillSandstone(level, chunkBox, random, 6, 0, 1, 6, 3, 6);
            p.fill(level, chunkBox, 1, 0, 1, 5, 0, 8, "minecraft:sandstone", 0);
            p.fill(level, chunkBox, 1, 4, 0, 5, 4, 9, "minecraft:sandstone", 0);
            p.fill(level, chunkBox, 0, 4, 0, 0, 4, 9, "minecraft:sandstone_slab", 0);
            p.fill(level, chunkBox, 6, 4, 0, 6, 4, 9, "minecraft:sandstone_slab", 0);
            p.fill(level, chunkBox, 2, 5, 0, 4, 5, 0, "minecraft:sandstone_slab", 0);
            p.fill(level, chunkBox, 3, 5, 1, 3, 5, 2, "minecraft:sandstone_slab", 0);
            p.fill(level, chunkBox, 3, 5, 4, 3, 5, 6, "minecraft:sandstone_slab", 0);
            p.set(level, chunkBox, 3, 5, 8, "minecraft:sandstone_slab", 0);
            p.fill(level, chunkBox, 2, 5, 9, 4, 5, 9, "minecraft:sandstone_slab", 0);

            p.fillSandstone(level, chunkBox, random, 9, 0, 0, 15, 0, 0);
            p.fillSandstone(level, chunkBox, random, 9, 1, 0, 13, 1, 0);
            p.fillSandstone(level, chunkBox, random, 9, 2, 0, 9, 2, 0);
            p.set(level, chunkBox, 9, 2, 0, "minecraft:sandstone_slab", 0);
            p.set(level, chunkBox, 12, 2, 0, "minecraft:sandstone_slab", 0);
            p.fillSandstone(level, chunkBox, random, 9, 0, 1, 9, 3, 1);
            p.fillSandstone(level, chunkBox, random, 9, 0, 2, 9, 0, 2);
            p.fillSandstone(level, chunkBox, random, 9, 3, 2, 9, 3, 8);
            p.set(level, chunkBox, 9, 4, 2, "minecraft:sandstone_slab", 0);
            p.fill(level, chunkBox, 9, 4, 4, 9, 4, 7, "minecraft:sandstone_slab", 0);
            p.fillSandstone(level, chunkBox, random, 9, 0, 3, 9, 1, 9);
            p.fillSandstone(level, chunkBox, random, 9, 2, 3, 9, 2, 3);
            p.set(level, chunkBox, 9, 2, 4, "minecraft:oak_fence", 0);
            p.fillSandstone(level, chunkBox, random, 9, 2, 5, 9, 2, 5);
            p.fill(level, chunkBox, 9, 2, 6, 9, 2, 7, "minecraft:oak_fence", 0);
            p.fillSandstone(level, chunkBox, random, 9, 2, 8, 9, 2, 9);
            p.fillSandstone(level, chunkBox, random, 10, 0, 9, 15, 1, 9);
            p.fillSandstone(level, chunkBox, random, 10, 2, 9, 10, 2, 9);
            p.fillSandstone(level, chunkBox, random, 14, 2, 9, 15, 2, 9);
            p.fillSandstone(level, chunkBox, random, 15, 0, 1, 15, 0, 8);
            p.fillSandstone(level, chunkBox, random, 15, 1, 3, 15, 1, 3);
            p.fill(level, chunkBox, 15, 1, 4, 15, 1, 5, "minecraft:sandstone_slab", 0);
            p.fillSandstone(level, chunkBox, random, 15, 1, 6, 15, 1, 8);
            p.set(level, chunkBox, 15, 1, 8, "minecraft:sandstone_slab", 0);
            p.fill(level, chunkBox, 10, 0, 1, 14, 0, 8, "minecraft:sandstone", 0);

            p.set(level, chunkBox, 1, 1, 1, "reinhardtshbm:heat_boiler", 4);
            p.fill(level, chunkBox, 1, 2, 1, 1, 3, 1, "reinhardtshbm:deco_pipe_quad_rusted", 0);
            p.set(level, chunkBox, 1, 5, 1, "reinhardtshbm:deco_pipe_rim_rusted", 0);
            p.set(level, chunkBox, 2, 1, 3, "reinhardtshbm:crate", 0);
            p.set(level, chunkBox, 1, 1, 5, "reinhardtshbm:crate_can", 0);
            p.set(level, chunkBox, 1, 1, 7, "minecraft:chest", 0);
            p.fill(level, chunkBox, 4, 1, 8, 5, 1, 8, "reinhardtshbm:crate", 0);
            int eastMeta = p.getDecoMeta(4);
            p.fillDirect(level, chunkBox, 5, 1, 4, 5, 3, 4, "reinhardtshbm:steel_scaffold", eastMeta < 4 ? 0 : 8);
            p.fillDirect(level, chunkBox, 5, 1, 6, 5, 3, 6, "reinhardtshbm:steel_scaffold", eastMeta < 4 ? 0 : 8);
            p.setDirect(level, chunkBox, 5, 1, 5, "reinhardtshbm:steel_grate", 7);
            p.set(level, chunkBox, 5, 2, 5, "reinhardtshbm:crate_weapon", 0);

            p.set(level, chunkBox, 10, 1, 1, "minecraft:chest", 0);
            p.placeRandomBobble(level, chunkBox, random, 10, 1, 4);
            p.fillRandom(level, chunkBox, random, 0.25F, 11, 1, 1, 14, 1, 8, "minecraft:sand", 0);
        }

        private static void placeLabOne(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int originX, int originZ, int rotation) {
            Placement p = Placement.surface(level, originX, originZ, rotation, 10, 5, 8);
            p.foundation(level, chunkBox, "minecraft:stone_bricks", 0, 0, 0, 9, 6, -1);
            p.foundation(level, chunkBox, "minecraft:stone_bricks", 0, 3, 6, 9, 7, -1);
            p.setIfReplaceableOrAir(level, chunkBox, 2, 0, 6, "minecraft:stone_brick_stairs", p.getStairMeta(0));

            p.fill(level, chunkBox, 1, 1, 1, 8, 3, 4, "minecraft:air", 0);
            p.fill(level, chunkBox, 4, 1, 4, 8, 4, 6, "minecraft:air", 0);
            p.fill(level, chunkBox, 3, 1, 6, 3, 2, 6, "minecraft:air", 0);

            int pillarMeta = p.getPillarMeta(8);
            p.fill(level, chunkBox, 0, 0, 0, 0, 3, 0, "reinhardtshbm:concrete_pillar", 0);
            p.fill(level, chunkBox, 9, 0, 0, 9, 3, 0, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 0, 0, 1, 0, 0, 4, "reinhardtshbm:concrete_pillar", pillarMeta);
            p.fillDirect(level, chunkBox, 9, 0, 1, 9, 0, 6, "reinhardtshbm:concrete_pillar", pillarMeta);
            p.fill(level, chunkBox, 0, 0, 5, 0, 3, 5, "reinhardtshbm:concrete_pillar", 0);
            p.fill(level, chunkBox, 3, 0, 5, 3, 3, 5, "reinhardtshbm:concrete_pillar", 0);
            p.fill(level, chunkBox, 3, 0, 7, 3, 3, 7, "reinhardtshbm:concrete_pillar", 0);
            p.fill(level, chunkBox, 9, 0, 7, 9, 3, 7, "reinhardtshbm:concrete_pillar", 0);

            p.fillConcreteBricks(level, chunkBox, random, 1, 0, 0, 8, 3, 0);
            p.fillConcreteBricks(level, chunkBox, random, 0, 4, 0, 9, 4, 0);
            p.fillConcreteBricks(level, chunkBox, random, 0, 1, 1, 0, 3, 4);
            p.fillConcreteBricks(level, chunkBox, random, 0, 4, 0, 0, 4, 5);
            p.fillConcreteBricks(level, chunkBox, random, 1, 0, 5, 2, 4, 5);
            p.set(level, chunkBox, 3, 4, 5, "reinhardtshbm:brick_concrete_broken", 0);
            p.fillConcreteBricks(level, chunkBox, random, 3, 3, 6, 3, 4, 6);
            p.fillConcreteBricks(level, chunkBox, random, 4, 0, 7, 8, 1, 7);
            p.fillConcreteBricks(level, chunkBox, random, 4, 2, 7, 4, 3, 7);
            p.fillConcreteBricks(level, chunkBox, random, 8, 2, 7, 8, 3, 7);
            p.fillRandom(level, chunkBox, random, 0.75F, 5, 2, 7, 7, 3, 7, "minecraft:glass_pane", 0);
            p.fillConcreteBricks(level, chunkBox, random, 3, 4, 7, 9, 4, 7);
            p.fillConcreteBricks(level, chunkBox, random, 9, 1, 1, 9, 4, 6);

            p.fillLabTiles(level, chunkBox, random, 1, 0, 1, 8, 0, 4);
            p.fillLabTiles(level, chunkBox, random, 4, 0, 5, 8, 0, 6);
            p.set(level, chunkBox, 3, 0, 6, "reinhardtshbm:tile_lab_cracked", 0);
            p.fill(level, chunkBox, 1, 3, 1, 1, 4, 4, "reinhardtshbm:reinforced_glass", 0);
            p.fill(level, chunkBox, 2, 4, 1, 8, 4, 4, "reinhardtshbm:brick_light", 0);
            p.fill(level, chunkBox, 4, 4, 5, 8, 4, 6, "reinhardtshbm:brick_light", 0);

            p.fill(level, chunkBox, 1, 1, 1, 1, 1, 4, "minecraft:podzol", 0);
            int westDecoMeta = p.getDecoMeta(5);
            p.fillDirect(level, chunkBox, 2, 1, 1, 2, 1, 4, "reinhardtshbm:steel_wall", westDecoMeta);
            p.fillDirect(level, chunkBox, 2, 3, 1, 2, 3, 4, "reinhardtshbm:steel_wall", westDecoMeta);
            for (int i = 0; i < 4; i++) {
                p.setDirect(level, chunkBox, 1, 2, 1 + i, "reinhardtshbm:plant_flower", i);
            }
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_office", 2, true, false, 3, 1, 6);

            int northDecoMeta = p.getDecoMeta(3);
            p.fillDirect(level, chunkBox, 5, 3, 1, 8, 3, 1, "reinhardtshbm:steel_scaffold", westDecoMeta < 4 ? 0 : 8);
            p.fillDirect(level, chunkBox, 5, 3, 2, 8, 3, 2, "reinhardtshbm:steel_wall", northDecoMeta);
            p.setDirect(level, chunkBox, 5, 1, 1, "reinhardtshbm:machine_electric_furnace_off", northDecoMeta);
            p.setDirect(level, chunkBox, 5, 2, 1, "reinhardtshbm:machine_microwave", northDecoMeta);
            p.set(level, chunkBox, 6, 1, 1, "reinhardtshbm:deco_titanium", 0);
            p.set(level, chunkBox, 7, 1, 1, "reinhardtshbm:machine_shredder", 0);
            p.set(level, chunkBox, 8, 1, 1, "reinhardtshbm:deco_titanium", 0);
            p.fill(level, chunkBox, 5, 1, 3, 8, 1, 3, "reinhardtshbm:deco_titanium", 0);
            p.set(level, chunkBox, 6, 2, 3, "reinhardtshbm:deco_loot", 0);
            p.set(level, chunkBox, 8, 1, 5, "reinhardtshbm:crate_can", 0);
            p.set(level, chunkBox, 8, 1, 6, "reinhardtshbm:crate_iron", 0);
        }

        private static void placeLabTwo(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int originX, int originZ, int rotation) {
            Placement p = Placement.surface(level, originX, originZ, rotation, 13, 12, 9).shiftedY(-7);
            p.foundation(level, chunkBox, "minecraft:stone_bricks", 0, 0, 0, 12, 6, 6);
            p.foundation(level, chunkBox, "minecraft:stone_bricks", 0, 0, 7, 6, 8, 6);
            p.setIfReplaceableOrAir(level, chunkBox, 9, 7, 7, "minecraft:stone_brick_stairs", p.getStairMeta(2));
            p.setIfReplaceableOrAir(level, chunkBox, 10, 7, 7, "minecraft:stone_brick_stairs", p.getStairMeta(2));

            p.fill(level, chunkBox, 1, 7, 1, 11, 11, 5, "minecraft:air", 0);
            p.fill(level, chunkBox, 1, 7, 6, 5, 11, 7, "minecraft:air", 0);
            p.fill(level, chunkBox, 9, 8, 6, 10, 9, 6, "minecraft:air", 0);
            p.fill(level, chunkBox, 5, 5, 1, 6, 6, 2, "minecraft:air", 0);
            p.fill(level, chunkBox, 2, 1, 2, 10, 3, 6, "minecraft:air", 0);

            p.fillSuperConcrete(level, chunkBox, random, 0, 7, 0, 12, 11, 0);
            p.fillSuperConcrete(level, chunkBox, random, 0, 7, 0, 0, 11, 8);
            p.fillSuperConcrete(level, chunkBox, random, 1, 7, 8, 5, 7, 8);
            p.fill(level, chunkBox, 1, 8, 8, 1, 10, 8, "reinhardtshbm:reinforced_glass", 0);
            p.fillSuperConcrete(level, chunkBox, random, 2, 7, 8, 2, 10, 8);
            p.fill(level, chunkBox, 3, 8, 8, 3, 10, 8, "reinhardtshbm:reinforced_glass", 0);
            p.fillSuperConcrete(level, chunkBox, random, 4, 7, 8, 4, 10, 8);
            p.fill(level, chunkBox, 5, 8, 8, 5, 10, 8, "reinhardtshbm:reinforced_glass", 0);
            p.fillSuperConcrete(level, chunkBox, random, 1, 11, 8, 5, 11, 8);
            p.fillSuperConcrete(level, chunkBox, random, 6, 7, 7, 6, 11, 8);
            p.fillSuperConcrete(level, chunkBox, random, 6, 7, 6, 7, 9, 6);
            p.fill(level, chunkBox, 6, 10, 6, 7, 10, 6, "reinhardtshbm:concrete_super_broken", 0);
            p.fillSuperConcrete(level, chunkBox, random, 8, 7, 6, 12, 7, 6);
            p.fillSuperConcrete(level, chunkBox, random, 8, 8, 6, 8, 11, 6);
            p.fillSuperConcrete(level, chunkBox, random, 9, 10, 6, 10, 11, 6);
            p.fillSuperConcrete(level, chunkBox, random, 11, 7, 6, 12, 11, 6);
            p.fillSuperConcrete(level, chunkBox, random, 12, 7, 1, 12, 7, 5);
            p.fill(level, chunkBox, 12, 8, 5, 12, 10, 5, "reinhardtshbm:reinforced_glass", 0);
            p.fillSuperConcrete(level, chunkBox, random, 12, 8, 4, 12, 10, 4);
            p.fill(level, chunkBox, 12, 8, 3, 12, 10, 3, "reinhardtshbm:reinforced_glass", 0);
            p.fillSuperConcrete(level, chunkBox, random, 12, 8, 2, 12, 10, 2);
            p.fill(level, chunkBox, 12, 8, 1, 12, 10, 1, "reinhardtshbm:reinforced_glass", 0);
            p.fillSuperConcrete(level, chunkBox, random, 12, 11, 1, 12, 11, 5);

            p.fill(level, chunkBox, 1, 0, 1, 11, 3, 1, "reinhardtshbm:reinforced_stone", 0);
            p.fill(level, chunkBox, 1, 0, 2, 1, 3, 6, "reinhardtshbm:reinforced_stone", 0);
            p.fill(level, chunkBox, 1, 0, 7, 11, 3, 7, "reinhardtshbm:reinforced_stone", 0);
            p.fill(level, chunkBox, 11, 0, 2, 11, 3, 6, "reinhardtshbm:reinforced_stone", 0);
            p.fill(level, chunkBox, 6, 0, 3, 6, 3, 6, "reinhardtshbm:reinforced_stone", 0);

            p.fillLabTiles(level, chunkBox, random, 1, 7, 1, 3, 7, 7);
            p.fillLabTiles(level, chunkBox, random, 4, 7, 6, 5, 7, 7);
            p.fillLabTiles(level, chunkBox, random, 8, 7, 1, 11, 7, 5);
            p.fillLabTiles(level, chunkBox, random, 9, 7, 6, 10, 7, 6);
            p.fill(level, chunkBox, 4, 7, 1, 7, 7, 1, "reinhardtshbm:tile_lab_broken", 0);
            p.set(level, chunkBox, 4, 7, 2, "reinhardtshbm:tile_lab_broken", 0);
            p.fill(level, chunkBox, 4, 7, 3, 4, 7, 5, "reinhardtshbm:tile_lab_cracked", 0);
            p.set(level, chunkBox, 5, 7, 3, "reinhardtshbm:tile_lab_broken", 0);
            p.fill(level, chunkBox, 5, 7, 4, 5, 7, 5, "reinhardtshbm:tile_lab_cracked", 0);
            p.set(level, chunkBox, 6, 7, 4, "reinhardtshbm:tile_lab_broken", 0);
            p.set(level, chunkBox, 6, 7, 5, "reinhardtshbm:tile_lab_cracked", 0);
            p.fill(level, chunkBox, 7, 7, 2, 7, 7, 3, "reinhardtshbm:tile_lab_broken", 0);
            p.fill(level, chunkBox, 7, 7, 4, 7, 7, 5, "reinhardtshbm:tile_lab_cracked", 0);

            p.fill(level, chunkBox, 1, 11, 1, 2, 11, 7, "reinhardtshbm:brick_light", 0);
            p.fill(level, chunkBox, 3, 11, 6, 4, 11, 7, "reinhardtshbm:brick_light", 0);
            p.fill(level, chunkBox, 9, 11, 1, 11, 11, 5, "reinhardtshbm:brick_light", 0);
            p.fill(level, chunkBox, 3, 11, 1, 8, 11, 1, "reinhardtshbm:waste_planks", 0);
            p.fill(level, chunkBox, 3, 11, 2, 4, 11, 2, "reinhardtshbm:waste_planks", 0);
            p.fill(level, chunkBox, 7, 11, 2, 8, 11, 2, "reinhardtshbm:waste_planks", 0);
            p.fill(level, chunkBox, 3, 11, 3, 3, 11, 5, "reinhardtshbm:waste_planks", 0);
            p.fill(level, chunkBox, 4, 11, 4, 4, 11, 5, "reinhardtshbm:waste_planks", 0);
            p.fill(level, chunkBox, 5, 11, 6, 5, 11, 7, "reinhardtshbm:waste_planks", 0);
            p.fill(level, chunkBox, 8, 11, 3, 8, 11, 5, "reinhardtshbm:waste_planks", 0);

            p.fillLabTiles(level, chunkBox, random, 2, 0, 2, 5, 0, 6);
            p.fillLabTiles(level, chunkBox, random, 6, 0, 2, 6, 0, 3);
            p.fillLabTiles(level, chunkBox, random, 7, 0, 2, 10, 0, 6);
            p.fillConcreteBricks(level, chunkBox, random, 1, 4, 1, 11, 4, 7);

            int eastMeta = p.getDecoMeta(4);
            int westMeta = p.getDecoMeta(5);
            int northMeta = p.getDecoMeta(3);
            int southMeta = p.getDecoMeta(2);
            p.setDirect(level, chunkBox, 6, 9, 3, "reinhardtshbm:crashed_bomb", southMeta);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_office", 1, true, false, 9, 8, 6);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_office", 1, true, false, 10, 8, 6);
            p.fill(level, chunkBox, 1, 8, 8, 1, 10, 8, "reinhardtshbm:reinforced_glass", 0);
            p.fill(level, chunkBox, 1, 8, 1, 1, 10, 1, "reinhardtshbm:deco_steel", 0);
            p.fillDirect(level, chunkBox, 1, 8, 2, 1, 9, 3, "reinhardtshbm:steel_grate", 7);
            p.setDirect(level, chunkBox, 1, 10, 2, "reinhardtshbm:tape_recorder", westMeta);
            p.set(level, chunkBox, 1, 10, 3, "reinhardtshbm:steel_beam", 0);
            p.fill(level, chunkBox, 1, 8, 6, 1, 10, 6, "reinhardtshbm:deco_pipe_framed_rusted", 0);
            p.fillDirect(level, chunkBox, 8, 8, 1, 8, 10, 1, "reinhardtshbm:steel_wall", eastMeta);
            p.fillDirect(level, chunkBox, 9, 10, 1, 10, 10, 1, "reinhardtshbm:steel_grate", 0);
            p.fillDirect(level, chunkBox, 9, 9, 1, 10, 9, 1, "reinhardtshbm:tape_recorder", northMeta);
            p.fill(level, chunkBox, 9, 8, 1, 10, 8, 1, "reinhardtshbm:deco_steel", 0);
            p.fillDirect(level, chunkBox, 11, 8, 1, 11, 10, 1, "reinhardtshbm:steel_wall", westMeta);
            p.fillDirect(level, chunkBox, 2, 1, 2, 2, 1, 6, "reinhardtshbm:steel_grate", 7);
            p.set(level, chunkBox, 2, 1, 2, "reinhardtshbm:vitrified_barrel", 0);
            p.fillDirect(level, chunkBox, 3, 1, 2, 3, 3, 2, "reinhardtshbm:steel_wall", westMeta);
            p.fillDirect(level, chunkBox, 3, 1, 4, 3, 3, 4, "reinhardtshbm:steel_wall", westMeta);
            p.fillDirect(level, chunkBox, 3, 1, 6, 3, 3, 6, "reinhardtshbm:steel_wall", westMeta);
            p.set(level, chunkBox, 4, 1, 6, "reinhardtshbm:crate", 0);
            p.set(level, chunkBox, 4, 2, 6, "reinhardtshbm:crate_lead", 0);
            p.set(level, chunkBox, 5, 1, 6, "reinhardtshbm:crate_iron", 0);
            p.fill(level, chunkBox, 4, 1, 5, 5, 1, 5, "reinhardtshbm:crate_lead", 0);
            p.fill(level, chunkBox, 7, 1, 6, 7, 3, 6, "reinhardtshbm:deco_steel", 0);
            p.fillDirect(level, chunkBox, 8, 1, 6, 10, 1, 6, "reinhardtshbm:steel_grate", 7);
            p.fillDirect(level, chunkBox, 8, 2, 6, 9, 2, 6, "reinhardtshbm:tape_recorder", southMeta);
            p.set(level, chunkBox, 10, 2, 6, "reinhardtshbm:steel_beam", 0);
            p.fill(level, chunkBox, 8, 3, 6, 10, 3, 6, "reinhardtshbm:steel_roof", 0);
            p.set(level, chunkBox, 10, 1, 3, "reinhardtshbm:crate_iron", 0);
        }

        private static void placeOffice(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int originX, int originZ, int rotation, boolean corner) {
            if (corner) {
                placeLargeOfficeCorner(level, chunkBox, random, originX, originZ, rotation);
            } else {
                placeLargeOffice(level, chunkBox, random, originX, originZ, rotation);
            }
        }

        private static void placeLargeOffice(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int originX, int originZ, int rotation) {
            Placement p = Placement.surface(level, originX, originZ, rotation, 15, 6, 13);

            p.foundation(level, chunkBox, "minecraft:stone_bricks", 0, 5, 0, 14, 1, -1);
            p.foundation(level, chunkBox, "minecraft:stone_bricks", 0, 0, 2, 14, 7, -1);
            p.foundation(level, chunkBox, "minecraft:stone_bricks", 0, 0, 8, 8, 12, 0);
            p.foundation(level, chunkBox, "minecraft:stone_bricks", 0, 9, 8, 14, 12, -1);

            p.fill(level, chunkBox, 1, 1, 3, 4, 3, 6, "minecraft:air", 0);
            p.fill(level, chunkBox, 6, 1, 1, 13, 3, 6, "minecraft:air", 0);
            p.fill(level, chunkBox, 10, 1, 7, 13, 3, 11, "minecraft:air", 0);

            p.fillDirect(level, chunkBox, 0, 0, 2, 0, 4, 2, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 5, 0, 0, 5, 4, 0, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 14, 0, 0, 14, 4, 0, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 0, 0, 7, 0, 3, 7, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 0, 0, 12, 0, 3, 12, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 3, 0, 12, 3, 3, 12, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 6, 0, 12, 6, 3, 12, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 9, 0, 12, 9, 3, 12, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 9, 0, 7, 9, 3, 7, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 14, 0, 12, 14, 4, 12, "reinhardtshbm:concrete_pillar", 0);

            p.fillConcreteBricks(level, chunkBox, random, 1, 0, 2, 5, 4, 2);
            p.fillConcreteBricks(level, chunkBox, random, 5, 0, 1, 5, 4, 1);
            p.fillConcreteBricks(level, chunkBox, random, 6, 0, 0, 13, 1, 0);
            p.fillConcreteBricks(level, chunkBox, random, 6, 2, 0, 6, 2, 0);
            p.fillConcreteBricks(level, chunkBox, random, 9, 2, 0, 10, 2, 0);
            p.fillConcreteBricks(level, chunkBox, random, 13, 2, 0, 13, 2, 0);
            p.fillConcreteBricks(level, chunkBox, random, 6, 3, 0, 13, 4, 0);
            p.fillConcreteBricks(level, chunkBox, random, 14, 0, 1, 14, 1, 11);
            p.fillConcreteBricks(level, chunkBox, random, 14, 2, 1, 14, 2, 2);
            p.fillConcreteBricks(level, chunkBox, random, 14, 2, 5, 14, 2, 7);
            p.fillConcreteBricks(level, chunkBox, random, 14, 2, 10, 14, 2, 11);
            p.fillConcreteBricks(level, chunkBox, random, 14, 3, 1, 14, 4, 11);
            p.fillConcreteBricks(level, chunkBox, random, 0, 4, 12, 13, 4, 12);
            p.fillConcreteBricks(level, chunkBox, random, 10, 0, 12, 13, 1, 12);
            p.fillConcreteBricks(level, chunkBox, random, 10, 2, 12, 10, 2, 12);
            p.fillConcreteBricks(level, chunkBox, random, 13, 2, 12, 13, 2, 12);
            p.fillConcreteBricks(level, chunkBox, random, 10, 3, 12, 13, 3, 12);
            p.fillConcreteBricks(level, chunkBox, random, 9, 0, 8, 9, 3, 11);
            p.fillConcreteBricks(level, chunkBox, random, 1, 0, 7, 8, 0, 7);
            p.fillConcreteBricks(level, chunkBox, random, 1, 1, 7, 1, 2, 7);
            p.fillConcreteBricks(level, chunkBox, random, 4, 1, 7, 8, 3, 7);
            p.fillConcreteBricks(level, chunkBox, random, 1, 3, 7, 3, 3, 7);
            p.fillConcreteBricks(level, chunkBox, random, 0, 4, 3, 0, 4, 11);
            p.fillConcreteBricks(level, chunkBox, random, 0, 0, 3, 0, 1, 6);
            p.fillConcreteBricks(level, chunkBox, random, 0, 2, 3, 0, 3, 3);
            p.fillConcreteBricks(level, chunkBox, random, 0, 2, 6, 0, 3, 6);
            p.fillConcreteBricks(level, chunkBox, random, 5, 1, 3, 5, 3, 5);
            p.fillConcreteBricks(level, chunkBox, random, 5, 3, 6, 5, 3, 6);

            p.fillRandomDirect(level, chunkBox, random, 0.85F, 0, 5, 2, 5, 5, 2, "minecraft:smooth_stone_slab", 0);
            p.fillRandomDirect(level, chunkBox, random, 0.85F, 5, 5, 1, 5, 5, 1, "minecraft:smooth_stone_slab", 0);
            p.fillRandomDirect(level, chunkBox, random, 0.85F, 5, 5, 0, 14, 5, 0, "minecraft:smooth_stone_slab", 0);
            p.fillRandomDirect(level, chunkBox, random, 0.85F, 14, 5, 1, 14, 5, 12, "minecraft:smooth_stone_slab", 0);
            p.fillRandomDirect(level, chunkBox, random, 0.85F, 0, 5, 12, 13, 5, 12, "minecraft:smooth_stone_slab", 0);
            p.fillRandomDirect(level, chunkBox, random, 0.85F, 0, 5, 3, 0, 5, 11, "minecraft:smooth_stone_slab", 0);

            p.fill(level, chunkBox, 1, 0, 3, 4, 0, 6, "minecraft:green_wool", 0);
            p.fill(level, chunkBox, 5, 0, 3, 5, 0, 6, "reinhardtshbm:brick_light", 0);
            p.fill(level, chunkBox, 6, 0, 1, 13, 0, 6, "reinhardtshbm:brick_light", 0);
            p.fill(level, chunkBox, 10, 0, 7, 13, 0, 11, "reinhardtshbm:brick_light", 0);
            p.fill(level, chunkBox, 6, 4, 1, 13, 4, 2, "reinhardtshbm:brick_light", 0);
            p.fill(level, chunkBox, 1, 4, 3, 13, 4, 11, "reinhardtshbm:brick_light", 0);

            p.fill(level, chunkBox, 9, 1, 3, 11, 1, 6, "minecraft:light_gray_carpet", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 0, 2, 4, 0, 3, 5, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 7, 2, 0, 8, 2, 0, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 11, 2, 0, 12, 2, 0, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 14, 2, 3, 14, 2, 4, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 14, 2, 8, 14, 2, 9, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 11, 2, 12, 12, 2, 12, "minecraft:glass_pane", 0);

            int stairMetaE = p.getStairMeta(1);
            int stairMetaN = p.getStairMeta(2);
            int stairMetaS = p.getStairMeta(3);
            int stairMetaWU = p.getStairMeta(0) | 4;
            int stairMetaEU = stairMetaE | 4;
            int stairMetaNU = stairMetaN | 4;
            int stairMetaSU = stairMetaS | 4;
            p.setDirect(level, chunkBox, 1, 1, 4, "minecraft:spruce_stairs", stairMetaEU);
            p.setDirect(level, chunkBox, 2, 1, 4, "minecraft:spruce_stairs", stairMetaNU);
            p.setDirect(level, chunkBox, 3, 1, 4, "minecraft:spruce_stairs", stairMetaWU);
            p.setDirect(level, chunkBox, 2, 1, 3, "minecraft:oak_stairs", stairMetaS);
            p.setDirect(level, chunkBox, 1, 2, 4, "reinhardtshbm:deco_computer", p.getDecoModelMeta(0));

            p.setDirect(level, chunkBox, 7, 1, 3, "minecraft:oak_stairs", stairMetaS);
            p.setDirect(level, chunkBox, 6, 1, 4, "minecraft:spruce_stairs", stairMetaEU);
            p.setDirect(level, chunkBox, 7, 1, 4, "minecraft:spruce_stairs", stairMetaWU);
            p.set(level, chunkBox, 8, 1, 4, "minecraft:spruce_planks", 0);
            p.setDirect(level, chunkBox, 7, 2, 4, "reinhardtshbm:deco_computer", p.getDecoModelMeta(0));
            p.set(level, chunkBox, 8, 2, 4, "minecraft:flower_pot", 0);

            p.setDirect(level, chunkBox, 10, 1, 1, "minecraft:spruce_stairs", stairMetaEU);
            p.fillDirect(level, chunkBox, 11, 1, 1, 13, 1, 1, "minecraft:spruce_stairs", stairMetaSU);
            p.setDirect(level, chunkBox, 13, 1, 2, "minecraft:spruce_stairs", stairMetaNU);
            p.setDirect(level, chunkBox, 13, 1, 3, "minecraft:spruce_stairs", stairMetaSU);
            p.setDirect(level, chunkBox, 13, 1, 4, "minecraft:spruce_stairs", stairMetaWU);
            p.setDirect(level, chunkBox, 13, 1, 5, "minecraft:spruce_stairs", stairMetaNU);
            p.setDirect(level, chunkBox, 11, 1, 2, "minecraft:oak_stairs", stairMetaN);
            p.setDirect(level, chunkBox, 12, 1, 4, "minecraft:oak_stairs", stairMetaE);
            p.setDirect(level, chunkBox, 11, 2, 1, "reinhardtshbm:deco_computer", p.getDecoModelMeta(1));
            p.setDirect(level, chunkBox, 13, 2, 5, "reinhardtshbm:deco_computer", p.getDecoModelMeta(2));
            p.set(level, chunkBox, 13, 2, 3, "minecraft:flower_pot", 0);
            p.setDirect(level, chunkBox, 13, 2, 2, "reinhardtshbm:radiorec", p.getDecoMeta(5));

            p.setDirect(level, chunkBox, 10, 1, 8, "minecraft:spruce_stairs", stairMetaEU);
            p.setDirect(level, chunkBox, 11, 1, 8, "minecraft:spruce_stairs", stairMetaWU);
            p.setDirect(level, chunkBox, 10, 1, 9, "minecraft:oak_stairs", stairMetaN);
            p.setDirect(level, chunkBox, 10, 2, 8, "reinhardtshbm:deco_computer", p.getDecoModelMeta(1));

            p.setDirect(level, chunkBox, 13, 1, 9, "minecraft:spruce_stairs", stairMetaSU);
            p.setDirect(level, chunkBox, 13, 1, 10, "minecraft:spruce_stairs", stairMetaWU);
            p.setDirect(level, chunkBox, 13, 1, 11, "minecraft:spruce_stairs", stairMetaNU);
            p.setDirect(level, chunkBox, 11, 1, 11, "minecraft:oak_stairs", stairMetaE);
            p.setDirect(level, chunkBox, 13, 2, 11, "reinhardtshbm:deco_computer", p.getDecoModelMeta(2));

            p.fillRandom(level, chunkBox, random, 0.25F, 1, 3, 3, 4, 3, 6, "minecraft:cobweb", 0);
            p.fillRandom(level, chunkBox, random, 0.25F, 6, 3, 1, 13, 3, 6, "minecraft:cobweb", 0);
            p.fillRandom(level, chunkBox, random, 0.25F, 10, 3, 7, 13, 3, 11, "minecraft:cobweb", 0);

            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_office", 3, false, 2, 1, 7);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_office", 3, true, 3, 1, 7);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_office", 0, false, 5, 1, 6);
            p.setDirect(level, chunkBox, 10, 1, 11, "reinhardtshbm:filing_cabinet", p.getDecoModelMeta(0));
            p.setDirect(level, chunkBox, 6, 1, 1, "reinhardtshbm:safe", p.getDecoMeta(3));
        }

        private static void placeLargeOfficeCorner(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int originX, int originZ, int rotation) {
            Placement p = Placement.surface(level, originX, originZ, rotation, 12, 16, 15);
            int pillarMetaWE = p.getPillarMeta(4);
            int pillarMetaNS = p.getPillarMeta(8);

            p.foundation(level, chunkBox, "minecraft:stone_bricks", 0, 4, 0, 11, 2, -1);
            p.foundation(level, chunkBox, "minecraft:stone_bricks", 0, 1, 3, 11, 13, -1);
            p.foundation(level, chunkBox, "minecraft:stone_bricks", 0, 0, 14, 4, 14, -1);
            p.foundation(level, chunkBox, "minecraft:stone_bricks", 0, 0, 10, 0, 13, -1);

            p.fill(level, chunkBox, 1, 1, 11, 3, 12, 13, "minecraft:air", 0);
            p.fill(level, chunkBox, 4, 1, 4, 10, 12, 12, "minecraft:air", 0);
            p.fill(level, chunkBox, 2, 1, 4, 3, 12, 10, "minecraft:air", 0);
            p.fill(level, chunkBox, 5, 1, 1, 10, 12, 2, "minecraft:air", 0);
            p.fill(level, chunkBox, 5, 13, 1, 8, 14, 2, "minecraft:air", 0);

            p.fillDirect(level, chunkBox, 1, 0, 3, 5, 0, 3, "reinhardtshbm:concrete_pillar", 0);
            p.setDirect(level, chunkBox, 6, 0, 3, "reinhardtshbm:concrete_pillar", pillarMetaWE);
            p.fillDirect(level, chunkBox, 7, 0, 3, 10, 0, 3, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 4, 0, 0, 4, 0, 2, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 5, 0, 0, 11, 0, 0, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 11, 1, 0, 11, 12, 0, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 1, 1, 3, 1, 12, 3, "reinhardtshbm:concrete_pillar", 0);
            p.fillConcreteBricks(level, chunkBox, random, 4, 1, 0, 10, 12, 0);
            p.fillConcreteBricks(level, chunkBox, random, 4, 13, 0, 9, 13, 0);
            p.fillConcreteBricks(level, chunkBox, random, 4, 14, 0, 8, 14, 0);
            p.fillConcreteBricks(level, chunkBox, random, 4, 15, 0, 7, 15, 0);
            p.fillConcreteBricks(level, chunkBox, random, 2, 1, 3, 5, 2, 3);
            p.fillConcreteBricks(level, chunkBox, random, 7, 1, 3, 10, 2, 3);
            p.fillConcreteBricks(level, chunkBox, random, 2, 3, 3, 10, 7, 3);
            p.fillConcreteBricks(level, chunkBox, random, 2, 8, 3, 9, 10, 3);
            p.fillConcreteBricks(level, chunkBox, random, 2, 11, 3, 10, 12, 3);
            p.fillConcreteBricks(level, chunkBox, random, 4, 13, 3, 4, 14, 3);
            p.fillConcreteBricks(level, chunkBox, random, 6, 13, 3, 9, 13, 3);
            p.fillConcreteBricks(level, chunkBox, random, 6, 14, 3, 8, 14, 3);
            p.fillConcreteBricks(level, chunkBox, random, 4, 15, 3, 7, 15, 3);
            p.fillConcreteBricks(level, chunkBox, random, 4, 1, 1, 4, 15, 2);

            p.fillDirect(level, chunkBox, 11, 0, 1, 11, 0, 12, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 11, 0, 13, 11, 12, 13, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 11, 1, 3, 11, 7, 3, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 11, 4, 1, 11, 4, 2, "reinhardtshbm:concrete_pillar", pillarMetaNS);
            p.fillDirect(level, chunkBox, 11, 8, 1, 11, 8, 12, "reinhardtshbm:concrete_pillar", pillarMetaNS);
            p.fillDirect(level, chunkBox, 11, 9, 3, 11, 11, 3, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 11, 12, 1, 11, 12, 12, "reinhardtshbm:concrete_pillar", pillarMetaNS);
            p.fillConcreteBricks(level, chunkBox, random, 11, 1, 1, 11, 3, 2);
            p.fillConcreteBricks(level, chunkBox, random, 11, 5, 1, 11, 7, 2);
            p.fillConcreteBricks(level, chunkBox, random, 11, 9, 1, 11, 11, 2);
            p.fillConcreteBricks(level, chunkBox, random, 11, 1, 4, 11, 7, 12);
            p.fillConcreteBricks(level, chunkBox, random, 11, 9, 4, 11, 9, 12);
            p.fillConcreteBricks(level, chunkBox, random, 11, 10, 4, 11, 10, 4);
            p.fillConcreteBricks(level, chunkBox, random, 11, 10, 8, 11, 10, 8);
            p.fillConcreteBricks(level, chunkBox, random, 11, 10, 12, 11, 10, 12);
            p.fillConcreteBricks(level, chunkBox, random, 11, 11, 4, 11, 11, 12);

            p.fillDirect(level, chunkBox, 4, 0, 13, 10, 0, 13, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 4, 0, 14, 4, 12, 14, "reinhardtshbm:concrete_pillar", 0);
            p.setDirect(level, chunkBox, 3, 0, 14, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 1, 0, 14, 2, 0, 14, "reinhardtshbm:concrete_pillar", pillarMetaWE);
            p.fillDirect(level, chunkBox, 0, 0, 14, 0, 12, 14, "reinhardtshbm:concrete_pillar", 0);
            p.fillConcreteBricks(level, chunkBox, random, 4, 1, 13, 10, 1, 13);
            p.fillConcreteBricks(level, chunkBox, random, 10, 2, 13, 10, 3, 13);
            p.fillDirect(level, chunkBox, 9, 2, 13, 9, 3, 13, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 6, 2, 13, 6, 3, 13, "reinhardtshbm:concrete_pillar", 0);
            p.fillConcreteBricks(level, chunkBox, random, 4, 2, 13, 5, 3, 13);
            p.fillConcreteBricks(level, chunkBox, random, 4, 4, 13, 10, 5, 13);
            p.fillConcreteBricks(level, chunkBox, random, 10, 6, 13, 10, 7, 13);
            p.fillConcreteBricks(level, chunkBox, random, 4, 6, 13, 5, 7, 13);
            p.fillConcreteBricks(level, chunkBox, random, 4, 8, 13, 10, 9, 13);
            p.fillConcreteBricks(level, chunkBox, random, 10, 10, 13, 10, 11, 13);
            p.fillConcreteBricks(level, chunkBox, random, 4, 10, 13, 5, 11, 13);
            p.fillConcreteBricks(level, chunkBox, random, 4, 12, 13, 10, 12, 13);
            p.fillConcreteBricks(level, chunkBox, random, 3, 1, 14, 3, 2, 14);
            p.fillConcreteBricks(level, chunkBox, random, 1, 3, 14, 3, 5, 14);
            p.fillConcreteBricks(level, chunkBox, random, 1, 8, 14, 3, 9, 14);
            p.fillConcreteBricks(level, chunkBox, random, 1, 12, 14, 3, 12, 14);

            p.fillDirect(level, chunkBox, 0, 0, 12, 0, 0, 13, "reinhardtshbm:concrete_pillar", pillarMetaNS);
            p.setDirect(level, chunkBox, 0, 0, 11, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 0, 0, 10, 0, 12, 10, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 1, 0, 4, 1, 0, 10, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 1, 0, 3, 1, 12, 3, "reinhardtshbm:concrete_pillar", 0);
            p.fillConcreteBricks(level, chunkBox, random, 0, 1, 11, 0, 2, 11);
            p.fillConcreteBricks(level, chunkBox, random, 0, 3, 11, 0, 5, 13);
            p.fillConcreteBricks(level, chunkBox, random, 0, 8, 11, 0, 9, 13);
            p.fillConcreteBricks(level, chunkBox, random, 0, 12, 11, 0, 12, 13);
            p.fillConcreteBricks(level, chunkBox, random, 1, 1, 4, 1, 1, 10);
            p.fillConcreteBricks(level, chunkBox, random, 1, 2, 9, 1, 3, 10);
            p.fillDirect(level, chunkBox, 1, 2, 8, 1, 3, 8, "reinhardtshbm:concrete_pillar", 0);
            p.fillDirect(level, chunkBox, 1, 2, 5, 1, 3, 5, "reinhardtshbm:concrete_pillar", 0);
            p.fillConcreteBricks(level, chunkBox, random, 1, 2, 4, 1, 3, 4);
            p.fillConcreteBricks(level, chunkBox, random, 1, 4, 4, 1, 5, 10);
            p.fillConcreteBricks(level, chunkBox, random, 1, 6, 9, 1, 7, 10);
            p.fillConcreteBricks(level, chunkBox, random, 1, 6, 4, 1, 7, 4);
            p.fillConcreteBricks(level, chunkBox, random, 1, 8, 4, 1, 9, 10);
            p.fillConcreteBricks(level, chunkBox, random, 1, 10, 9, 1, 11, 10);
            p.fillConcreteBricks(level, chunkBox, random, 1, 10, 4, 1, 11, 4);
            p.fillConcreteBricks(level, chunkBox, random, 1, 12, 4, 1, 12, 10);

            p.fillDirect(level, chunkBox, 5, 0, 1, 10, 0, 2, "reinhardtshbm:concrete_pillar", pillarMetaWE);
            p.setDirect(level, chunkBox, 6, 0, 3, "reinhardtshbm:concrete_pillar", pillarMetaWE);
            p.fill(level, chunkBox, 2, 0, 4, 10, 0, 10, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 3, 0, 11, 10, 0, 11, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 4, 0, 12, 10, 0, 12, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 1, 0, 11, 2, 0, 13, "minecraft:gray_wool", 0);
            p.fill(level, chunkBox, 3, 0, 12, 3, 0, 13, "minecraft:gray_wool", 0);
            p.fillDirect(level, chunkBox, 5, 4, 1, 5, 4, 3, "reinhardtshbm:concrete_pillar", pillarMetaNS);
            p.fill(level, chunkBox, 2, 4, 4, 10, 4, 12, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 1, 4, 11, 1, 4, 13, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 2, 4, 13, 3, 4, 13, "minecraft:spruce_planks", 0);
            p.fillDirect(level, chunkBox, 10, 8, 1, 10, 8, 3, "reinhardtshbm:concrete_pillar", pillarMetaNS);
            p.fill(level, chunkBox, 2, 8, 4, 10, 8, 12, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 1, 8, 11, 1, 8, 13, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 2, 8, 13, 3, 8, 13, "minecraft:spruce_planks", 0);
            p.fillDirect(level, chunkBox, 5, 12, 1, 5, 12, 2, "reinhardtshbm:concrete_pillar", pillarMetaNS);
            p.fill(level, chunkBox, 10, 12, 1, 10, 12, 2, "reinhardtshbm:asphalt", 0);
            p.fill(level, chunkBox, 9, 13, 1, 9, 13, 2, "reinhardtshbm:asphalt", 0);
            p.fill(level, chunkBox, 8, 14, 1, 8, 14, 2, "reinhardtshbm:asphalt", 0);
            p.fill(level, chunkBox, 5, 15, 1, 7, 15, 2, "reinhardtshbm:asphalt", 0);
            p.fill(level, chunkBox, 2, 12, 4, 10, 12, 12, "reinhardtshbm:brick_light", 0);
            p.fill(level, chunkBox, 1, 12, 11, 1, 12, 13, "reinhardtshbm:brick_light", 0);
            p.fill(level, chunkBox, 2, 12, 13, 3, 12, 13, "reinhardtshbm:brick_light", 0);

            int east = p.getStairMeta(1);
            int west = p.getStairMeta(0);
            int north = p.getStairMeta(2);
            int south = p.getStairMeta(3);
            int eastUp = east | 4;
            int westUp = west | 4;
            int northUp = north | 4;
            int southUp = south | 4;
            p.setDirect(level, chunkBox, 9, 1, 1, "minecraft:oak_stairs", east);
            p.setDirect(level, chunkBox, 8, 1, 1, "minecraft:oak_stairs", westUp);
            p.setDirect(level, chunkBox, 8, 2, 1, "minecraft:oak_stairs", east);
            p.setDirect(level, chunkBox, 7, 2, 1, "minecraft:oak_stairs", westUp);
            p.setDirect(level, chunkBox, 7, 3, 1, "minecraft:oak_stairs", east);
            p.setDirect(level, chunkBox, 6, 3, 1, "minecraft:oak_stairs", westUp);
            p.setDirect(level, chunkBox, 6, 4, 1, "minecraft:oak_stairs", east);
            p.setDirect(level, chunkBox, 6, 4, 2, "minecraft:oak_stairs", eastUp);
            p.setDirect(level, chunkBox, 6, 5, 2, "minecraft:oak_stairs", west);
            p.setDirect(level, chunkBox, 7, 5, 2, "minecraft:oak_stairs", eastUp);
            p.setDirect(level, chunkBox, 7, 6, 2, "minecraft:oak_stairs", west);
            p.setDirect(level, chunkBox, 8, 6, 2, "minecraft:oak_stairs", eastUp);
            p.setDirect(level, chunkBox, 8, 7, 2, "minecraft:oak_stairs", west);
            p.setDirect(level, chunkBox, 9, 7, 2, "minecraft:oak_stairs", eastUp);
            p.setDirect(level, chunkBox, 9, 8, 2, "minecraft:oak_stairs", west);
            p.setDirect(level, chunkBox, 9, 8, 1, "minecraft:oak_stairs", westUp);
            p.setDirect(level, chunkBox, 9, 9, 1, "minecraft:oak_stairs", east);
            p.setDirect(level, chunkBox, 8, 9, 1, "minecraft:oak_stairs", westUp);
            p.setDirect(level, chunkBox, 8, 10, 1, "minecraft:oak_stairs", east);
            p.setDirect(level, chunkBox, 7, 10, 1, "minecraft:oak_stairs", westUp);
            p.setDirect(level, chunkBox, 7, 11, 1, "minecraft:oak_stairs", east);
            p.setDirect(level, chunkBox, 6, 11, 1, "minecraft:oak_stairs", westUp);
            p.setDirect(level, chunkBox, 6, 12, 1, "minecraft:oak_stairs", east);

            p.fillRandom(level, chunkBox, random, 0.75F, 11, 10, 5, 11, 10, 7, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 11, 10, 9, 11, 10, 11, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 7, 2, 13, 8, 3, 13, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 6, 6, 13, 9, 7, 13, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 6, 10, 13, 9, 11, 13, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 1, 6, 14, 3, 7, 14, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 1, 10, 14, 3, 11, 14, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 0, 6, 11, 0, 7, 13, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 0, 10, 11, 0, 11, 13, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 1, 2, 6, 1, 3, 7, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 1, 6, 5, 1, 7, 8, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.75F, 1, 10, 5, 1, 11, 8, "minecraft:glass_pane", 0);

            p.fillRandomDirect(level, chunkBox, random, 0.85F, 10, 13, 0, 10, 13, 0, "minecraft:smooth_stone_slab", 0);
            p.fillRandomDirect(level, chunkBox, random, 0.85F, 11, 13, 0, 11, 13, 13, "minecraft:smooth_stone_slab", 0);
            p.fillRandomDirect(level, chunkBox, random, 0.85F, 4, 13, 13, 10, 13, 13, "minecraft:smooth_stone_slab", 0);
            p.fillRandomDirect(level, chunkBox, random, 0.85F, 0, 13, 14, 4, 13, 14, "minecraft:smooth_stone_slab", 0);
            p.fillRandomDirect(level, chunkBox, random, 0.85F, 0, 13, 10, 0, 13, 13, "minecraft:smooth_stone_slab", 0);
            p.fillRandomDirect(level, chunkBox, random, 0.85F, 1, 13, 3, 1, 13, 10, "minecraft:smooth_stone_slab", 0);
            p.fillRandomDirect(level, chunkBox, random, 0.85F, 2, 13, 3, 3, 13, 3, "minecraft:smooth_stone_slab", 0);

            p.fillConcreteBricks(level, chunkBox, random, 4, 5, 12, 4, 6, 12);
            p.fillConcreteBricks(level, chunkBox, random, 4, 5, 10, 4, 6, 10);
            p.fillConcreteBricks(level, chunkBox, random, 4, 7, 10, 4, 7, 12);
            p.fillConcreteBricks(level, chunkBox, random, 2, 5, 10, 3, 7, 10);
            p.fillConcreteBricks(level, chunkBox, random, 4, 9, 10, 4, 11, 12);
            p.fillConcreteBricks(level, chunkBox, random, 2, 11, 10, 3, 11, 10);
            p.fillConcreteBricks(level, chunkBox, random, 2, 9, 10, 2, 10, 10);

            p.placeDoor(level, chunkBox, random, "minecraft:oak_door", 3, false, 1, 1, 14);
            p.placeDoor(level, chunkBox, random, "minecraft:oak_door", 3, true, 2, 1, 14);
            p.placeDoor(level, chunkBox, random, "minecraft:oak_door", 0, false, 0, 1, 12);
            p.placeDoor(level, chunkBox, random, "minecraft:oak_door", 0, true, 0, 1, 13);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_office", 3, false, 6, 1, 3);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_office", 3, false, 5, 5, 3);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_office", 2, false, 4, 5, 11);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_office", 3, false, 10, 9, 3);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_office", 1, false, 3, 9, 10);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_metal", 3, false, 5, 13, 3);

            p.setDirect(level, chunkBox, 2, 1, 5, "minecraft:oak_stairs", east);
            p.setDirect(level, chunkBox, 2, 1, 7, "minecraft:oak_stairs", south);
            p.setDirect(level, chunkBox, 2, 1, 8, "minecraft:oak_stairs", east);
            p.setDirect(level, chunkBox, 2, 1, 9, "minecraft:oak_stairs", north);
            p.setDirect(level, chunkBox, 8, 1, 4, "minecraft:dark_oak_stairs", southUp);
            p.fillDirect(level, chunkBox, 8, 1, 5, 8, 1, 7, "minecraft:dark_oak_slab", 8);
            p.setDirect(level, chunkBox, 8, 1, 8, "minecraft:dark_oak_stairs", northUp);
            p.setDirect(level, chunkBox, 9, 1, 8, "minecraft:dark_oak_stairs", westUp);
            p.setDirect(level, chunkBox, 9, 1, 5, "minecraft:oak_stairs", west);
            p.set(level, chunkBox, 9, 1, 4, "minecraft:flower_pot", 0);
            p.set(level, chunkBox, 8, 2, 7, "minecraft:flower_pot", 0);
            p.setDirect(level, chunkBox, 6, 1, 12, "minecraft:oak_stairs", east);
            p.setDirect(level, chunkBox, 7, 1, 12, "minecraft:oak_stairs", north);
            p.setDirect(level, chunkBox, 8, 1, 12, "minecraft:oak_stairs", west);
            p.setDirect(level, chunkBox, 10, 1, 11, "minecraft:dark_oak_stairs", southUp);
            p.setDirect(level, chunkBox, 10, 1, 12, "minecraft:dark_oak_stairs", northUp);
            p.set(level, chunkBox, 10, 2, 11, "minecraft:flower_pot", 0);

            p.setDirect(level, chunkBox, 4, 5, 4, "minecraft:dark_oak_stairs", westUp);
            p.setDirect(level, chunkBox, 3, 5, 4, "minecraft:dark_oak_slab", 8);
            p.setDirect(level, chunkBox, 2, 5, 4, "minecraft:dark_oak_stairs", eastUp);
            p.setDirect(level, chunkBox, 3, 5, 5, "minecraft:oak_stairs", north);
            p.setDirect(level, chunkBox, 3, 6, 4, "reinhardtshbm:deco_computer", p.getDecoModelMeta(1));
            p.setDirect(level, chunkBox, 10, 5, 4, "minecraft:dark_oak_stairs", southUp);
            p.setDirect(level, chunkBox, 10, 5, 5, "minecraft:dark_oak_slab", 8);
            p.setDirect(level, chunkBox, 10, 5, 6, "minecraft:dark_oak_stairs", northUp);
            p.setDirect(level, chunkBox, 8, 5, 6, "minecraft:oak_stairs", north);
            p.setDirect(level, chunkBox, 10, 6, 6, "reinhardtshbm:deco_computer", p.getDecoModelMeta(2));
            p.setDirect(level, chunkBox, 1, 6, 11, "reinhardtshbm:machine_microwave", p.getDecoMeta(5));

            p.setDirect(level, chunkBox, 8, 9, 4, "minecraft:dark_oak_stairs", westUp);
            p.setDirect(level, chunkBox, 7, 9, 4, "minecraft:dark_oak_stairs", eastUp);
            p.set(level, chunkBox, 9, 9, 4, "minecraft:flower_pot", 0);
            p.setDirect(level, chunkBox, 5, 9, 5, "minecraft:dark_oak_stairs", southUp);
            p.setDirect(level, chunkBox, 5, 9, 6, "minecraft:dark_oak_stairs", westUp);
            p.fillDirect(level, chunkBox, 3, 9, 6, 4, 9, 6, "minecraft:dark_oak_slab", 8);
            p.setDirect(level, chunkBox, 2, 9, 6, "minecraft:dark_oak_stairs", eastUp);
            p.setDirect(level, chunkBox, 3, 9, 5, "minecraft:oak_stairs", south);
            p.setDirect(level, chunkBox, 3, 10, 6, "reinhardtshbm:deco_computer", p.getDecoModelMeta(0));
            p.setDirect(level, chunkBox, 7, 10, 10, "reinhardtshbm:deco_computer", p.getDecoModelMeta(1));
            p.setDirect(level, chunkBox, 6, 9, 11, "reinhardtshbm:tape_recorder", p.getDecoMeta(5));
            p.placeRandomBobble(level, chunkBox, random, 5, 10, 11);

            p.setDirect(level, chunkBox, 9, 1, 7, "reinhardtshbm:filing_cabinet", p.getDecoModelMeta(3));
            p.setDirect(level, chunkBox, 7, 5, 4, "reinhardtshbm:filing_cabinet", p.getDecoModelMeta(1));
            p.setDirect(level, chunkBox, 10, 5, 7, "reinhardtshbm:filing_cabinet", p.getDecoModelMeta(2));
            p.setDirect(level, chunkBox, 10, 5, 12, "reinhardtshbm:filing_cabinet", p.getDecoModelMeta(0));
            p.setDirect(level, chunkBox, 2, 9, 5, "reinhardtshbm:filing_cabinet", p.getDecoModelMeta(0));
            p.setDirect(level, chunkBox, 1, 9, 13, "reinhardtshbm:safe", p.getDecoMeta(2));
            p.setDirect(level, chunkBox, 2, 9, 13, "reinhardtshbm:filing_cabinet", p.getDecoModelMeta(0));
            p.setDirect(level, chunkBox, 3, 9, 13, "reinhardtshbm:filing_cabinet", p.getDecoModelMeta(0));
            p.set(level, chunkBox, 6, 13, 11, "reinhardtshbm:deco_loot", 0);
            p.set(level, chunkBox, 1, 10, 11, "reinhardtshbm:deco_loot", 0);
            p.set(level, chunkBox, 5, 13, 9, "minecraft:flower_pot", 0);
            p.set(level, chunkBox, 7, 13, 11, "minecraft:flower_pot", 0);
        }

        private static void placeRuralHouse(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int originX, int originZ, int rotation) {
            Placement p = Placement.surface(level, originX, originZ, rotation, 15, 9, 15);
            p.fill(level, chunkBox, 9, 1, 3, 12, 4, 8, "minecraft:air", 0);
            p.fill(level, chunkBox, 5, 1, 2, 8, 3, 8, "minecraft:air", 0);
            p.fill(level, chunkBox, 2, 1, 5, 4, 3, 8, "minecraft:air", 0);
            p.fill(level, chunkBox, 2, 1, 10, 7, 3, 12, "minecraft:air", 0);

            p.fill(level, chunkBox, 1, 0, 4, 4, 0, 4, "reinhardtshbm:concrete_colored_ext", 0);
            p.fill(level, chunkBox, 4, 0, 2, 4, 0, 3, "reinhardtshbm:concrete_colored_ext", 0);
            p.fill(level, chunkBox, 4, 0, 1, 9, 0, 1, "reinhardtshbm:concrete_colored_ext", 0);
            p.fill(level, chunkBox, 9, 0, 2, 10, 0, 2, "reinhardtshbm:concrete_colored_ext", 0);
            p.set(level, chunkBox, 12, 0, 2, "reinhardtshbm:concrete_colored_ext", 0);
            p.fill(level, chunkBox, 13, 0, 2, 13, 0, 9, "reinhardtshbm:concrete_colored_ext", 0);
            p.fill(level, chunkBox, 5, 0, 9, 12, 0, 9, "reinhardtshbm:concrete_colored_ext", 0);
            p.fill(level, chunkBox, 2, 0, 9, 3, 0, 9, "reinhardtshbm:concrete_colored_ext", 0);
            p.set(level, chunkBox, 8, 0, 10, "reinhardtshbm:concrete_colored_ext", 0);
            p.fill(level, chunkBox, 8, 0, 12, 8, 0, 13, "reinhardtshbm:concrete_colored_ext", 0);
            p.fill(level, chunkBox, 1, 0, 13, 7, 0, 13, "reinhardtshbm:concrete_colored_ext", 0);
            p.fill(level, chunkBox, 1, 0, 5, 1, 0, 12, "reinhardtshbm:concrete_colored_ext", 0);
            p.foundation(level, chunkBox, "reinhardtshbm:concrete_colored_ext", 0, 1, 10, 8, 13, -1);
            p.foundation(level, chunkBox, "reinhardtshbm:concrete_colored_ext", 0, 1, 4, 3, 9, -1);
            p.foundation(level, chunkBox, "reinhardtshbm:concrete_colored_ext", 0, 4, 1, 13, 9, -1);

            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 2, 3, 2, 3, 0);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 3, 2, 3, 2, 0);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 3, 0, 3, 0, -1);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 5, 0, 5, 0, 0);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 8, 0, 8, 0, 0);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 10, 0, 10, 0, -1);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 14, 1, 14, 1, -1);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 14, 3, 14, 3, -1);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 14, 5, 14, 6, 0);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 14, 8, 14, 8, -1);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 14, 10, 14, 10, -1);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 9, 14, 9, 14, -1);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 7, 14, 7, 14, -1);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 4, 14, 5, 14, 0);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 2, 14, 2, 14, -1);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 0, 14, 0, 14, -1);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 0, 13, 0, 13, 0);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 0, 11, 0, 11, 0);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 0, 9, 0, 9, -1);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 0, 6, 0, 7, 0);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 0, 4, 0, 4, 0);
            p.foundation(level, chunkBox, "minecraft:oak_log", 0, 0, 3, 0, 4, -1);

            p.fill(level, chunkBox, 1, 1, 4, 4, 4, 4, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 2, 5, 4, 7, 5, 4, "minecraft:bricks", 0);
            p.set(level, chunkBox, 3, 6, 4, "minecraft:bricks", 0);
            p.set(level, chunkBox, 6, 6, 4, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 4, 7, 4, 5, 7, 4, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 4, 1, 1, 4, 4, 3, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 5, 1, 1, 8, 1, 1, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 5, 4, 1, 8, 4, 1, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 9, 1, 1, 9, 4, 2, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 10, 1, 2, 10, 3, 2, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 12, 1, 2, 13, 3, 2, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 10, 4, 2, 13, 4, 2, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 9, 5, 2, 12, 5, 2, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 10, 6, 2, 11, 6, 2, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 13, 1, 3, 13, 1, 8, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 13, 3, 3, 13, 4, 8, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 13, 1, 9, 13, 4, 9, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 9, 1, 9, 12, 1, 9, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 9, 4, 9, 12, 5, 9, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 10, 6, 9, 11, 6, 9, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 8, 1, 9, 8, 4, 10, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 8, 1, 12, 8, 3, 13, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 8, 4, 11, 8, 4, 13, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 7, 1, 13, 7, 3, 13, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 3, 1, 13, 6, 1, 13, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 2, 4, 13, 7, 5, 13, "minecraft:bricks", 0);
            p.set(level, chunkBox, 6, 6, 13, "minecraft:bricks", 0);
            p.set(level, chunkBox, 3, 6, 13, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 4, 7, 13, 5, 7, 13, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 2, 1, 13, 2, 3, 13, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 1, 1, 13, 1, 4, 13, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 1, 1, 5, 1, 1, 12, "minecraft:bricks", 0);
            p.set(level, chunkBox, 1, 2, 9, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 1, 3, 5, 1, 3, 12, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 2, 1, 9, 3, 3, 9, "minecraft:bricks", 0);
            p.fill(level, chunkBox, 5, 1, 9, 7, 3, 9, "minecraft:bricks", 0);

            p.fill(level, chunkBox, 5, 2, 1, 5, 3, 1, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 8, 2, 1, 8, 3, 1, "minecraft:spruce_planks", 0);
            p.set(level, chunkBox, 11, 3, 2, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 13, 2, 3, 13, 2, 4, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 13, 2, 7, 13, 2, 8, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 12, 2, 9, 12, 3, 9, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 9, 2, 9, 9, 3, 9, "minecraft:spruce_planks", 0);
            p.set(level, chunkBox, 8, 3, 11, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 6, 2, 13, 6, 3, 13, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 3, 2, 13, 3, 3, 13, "minecraft:spruce_planks", 0);
            p.set(level, chunkBox, 1, 2, 12, "minecraft:spruce_planks", 0);
            p.set(level, chunkBox, 1, 2, 10, "minecraft:spruce_planks", 0);
            p.set(level, chunkBox, 1, 2, 8, "minecraft:spruce_planks", 0);
            p.set(level, chunkBox, 1, 2, 5, "minecraft:spruce_planks", 0);
            p.set(level, chunkBox, 4, 3, 9, "minecraft:spruce_planks", 0);

            int logW = p.getPillarMeta(4);
            int logN = p.getPillarMeta(8);
            int stairW = p.getStairMeta(0);
            int stairE = p.getStairMeta(1);
            int stairN = p.getStairMeta(2);
            int stairS = p.getStairMeta(3);

            p.fill(level, chunkBox, 0, 0, 3, 0, 3, 3, "minecraft:oak_log", 0);
            p.fillDirect(level, chunkBox, 1, 4, 3, 3, 4, 3, "minecraft:oak_log", logW);
            p.fillDirect(level, chunkBox, 3, 4, 1, 3, 4, 2, "minecraft:oak_log", logN);
            p.setDirect(level, chunkBox, 1, 3, 3, "minecraft:spruce_slab", 8);
            p.setDirect(level, chunkBox, 3, 3, 1, "minecraft:spruce_slab", 8);
            p.fill(level, chunkBox, 1, 1, 3, 2, 1, 3, "minecraft:spruce_slab", 0);
            p.fill(level, chunkBox, 3, 1, 1, 3, 1, 3, "minecraft:spruce_slab", 0);
            p.fill(level, chunkBox, 3, 0, 0, 3, 3, 0, "minecraft:oak_log", 0);
            p.fill(level, chunkBox, 4, 1, 0, 9, 1, 0, "minecraft:spruce_slab", 0);
            p.setDirect(level, chunkBox, 4, 3, 0, "minecraft:spruce_slab", 8);
            p.setDirect(level, chunkBox, 9, 3, 0, "minecraft:spruce_slab", 8);
            p.fill(level, chunkBox, 10, 0, 0, 10, 3, 0, "minecraft:oak_log", 0);
            p.fillDirect(level, chunkBox, 10, 4, 1, 13, 4, 1, "minecraft:oak_log", logW);
            p.fill(level, chunkBox, 14, 0, 1, 14, 3, 1, "minecraft:oak_log", 0);
            p.fill(level, chunkBox, 14, 0, 3, 14, 3, 3, "minecraft:oak_log", 0);
            p.fill(level, chunkBox, 14, 0, 8, 14, 3, 8, "minecraft:oak_log", 0);
            p.fill(level, chunkBox, 14, 0, 10, 14, 3, 10, "minecraft:oak_log", 0);
            p.set(level, chunkBox, 14, 1, 2, "minecraft:spruce_slab", 0);
            p.fill(level, chunkBox, 14, 1, 4, 14, 1, 7, "minecraft:spruce_slab", 0);
            p.set(level, chunkBox, 14, 1, 9, "minecraft:spruce_slab", 0);
            p.setDirect(level, chunkBox, 14, 3, 2, "minecraft:spruce_slab", 8);
            p.setDirect(level, chunkBox, 14, 3, 4, "minecraft:spruce_slab", 8);
            p.setDirect(level, chunkBox, 14, 3, 7, "minecraft:spruce_slab", 8);
            p.setDirect(level, chunkBox, 14, 3, 9, "minecraft:spruce_slab", 8);
            p.fillDirect(level, chunkBox, 9, 4, 10, 13, 4, 10, "minecraft:oak_log", logW);
            p.setDirect(level, chunkBox, 13, 3, 10, "minecraft:spruce_slab", 8);
            p.fill(level, chunkBox, 9, 0, 14, 9, 3, 14, "minecraft:oak_log", 0);
            p.fill(level, chunkBox, 7, 0, 14, 7, 3, 14, "minecraft:oak_log", 0);
            p.fill(level, chunkBox, 2, 0, 14, 2, 3, 14, "minecraft:oak_log", 0);
            p.fill(level, chunkBox, 0, 0, 14, 0, 3, 14, "minecraft:oak_log", 0);
            p.fillDirect(level, chunkBox, 1, 4, 14, 8, 4, 14, "minecraft:oak_log", logW);
            p.set(level, chunkBox, 8, 1, 14, "minecraft:spruce_slab", 0);
            p.fill(level, chunkBox, 3, 1, 14, 6, 1, 14, "minecraft:spruce_slab", 0);
            p.set(level, chunkBox, 1, 1, 14, "minecraft:spruce_slab", 0);
            p.setDirect(level, chunkBox, 8, 3, 14, "minecraft:spruce_slab", 8);
            p.setDirect(level, chunkBox, 1, 3, 14, "minecraft:spruce_slab", 8);
            p.fill(level, chunkBox, 0, 0, 9, 0, 3, 9, "minecraft:oak_log", 0);
            p.fill(level, chunkBox, 0, 1, 10, 0, 1, 13, "minecraft:spruce_slab", 0);
            p.fill(level, chunkBox, 0, 1, 4, 0, 1, 8, "minecraft:spruce_slab", 0);
            p.setDirect(level, chunkBox, 0, 3, 13, "minecraft:spruce_slab", 8);
            p.setDirect(level, chunkBox, 0, 3, 10, "minecraft:spruce_slab", 8);
            p.setDirect(level, chunkBox, 0, 3, 8, "minecraft:spruce_slab", 8);
            p.setDirect(level, chunkBox, 0, 3, 4, "minecraft:spruce_slab", 8);

            p.set(level, chunkBox, 11, 0, 2, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 9, 0, 3, 12, 0, 8, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 5, 0, 2, 8, 0, 8, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 2, 0, 5, 4, 0, 8, "minecraft:spruce_planks", 0);
            p.set(level, chunkBox, 4, 0, 9, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 2, 0, 10, 7, 0, 12, "minecraft:spruce_planks", 0);
            p.set(level, chunkBox, 8, 0, 11, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 13, 1, 0, 14, 1, 0, "minecraft:oak_fence", 0);
            p.fill(level, chunkBox, 10, 0, 1, 13, 0, 1, "minecraft:oak_planks", 0);
            p.fillDirect(level, chunkBox, 11, 0, 0, 12, 0, 0, "minecraft:spruce_stairs", stairN);
            p.fill(level, chunkBox, 13, 0, 0, 14, 0, 0, "minecraft:spruce_planks", 0);
            p.fill(level, chunkBox, 12, 0, 10, 13, 0, 10, "minecraft:oak_planks", 0);
            p.fill(level, chunkBox, 9, 0, 10, 11, 0, 11, "minecraft:oak_planks", 0);
            p.fill(level, chunkBox, 9, 0, 12, 10, 0, 12, "minecraft:oak_planks", 0);
            p.set(level, chunkBox, 9, 0, 13, "minecraft:oak_planks", 0);
            for (int i = 0; i < 3; i++) {
                p.fill(level, chunkBox, 10 + i, 0, 13 - i, 11 + i, 0, 13 - i, "minecraft:spruce_planks", 0);
                p.fill(level, chunkBox, 10 + i, 1, 13 - i, 11 + i, 1, 13 - i, "minecraft:oak_fence", 0);
            }

            p.fillDirect(level, chunkBox, 12, 4, 3, 12, 4, 8, "minecraft:oak_stairs", stairW | 4);
            p.fill(level, chunkBox, 12, 5, 3, 12, 5, 8, "minecraft:oak_planks", 0);
            p.fill(level, chunkBox, 10, 5, 3, 11, 6, 8, "minecraft:oak_planks", 0);
            p.fill(level, chunkBox, 9, 5, 3, 9, 5, 8, "minecraft:oak_planks", 0);
            p.fillDirect(level, chunkBox, 9, 4, 3, 9, 4, 8, "minecraft:oak_stairs", stairE | 4);
            p.fill(level, chunkBox, 8, 4, 5, 8, 4, 8, "minecraft:oak_planks", 0);
            p.fill(level, chunkBox, 5, 4, 2, 8, 4, 4, "minecraft:oak_planks", 0);
            p.fill(level, chunkBox, 1, 4, 5, 7, 4, 12, "minecraft:oak_planks", 0);

            p.setDirect(level, chunkBox, 1, 5, 3, "minecraft:spruce_stairs", stairW);
            p.setDirect(level, chunkBox, 2, 6, 3, "minecraft:spruce_stairs", stairW);
            p.setDirect(level, chunkBox, 3, 6, 3, "minecraft:spruce_stairs", stairE | 4);
            p.setDirect(level, chunkBox, 3, 7, 3, "minecraft:spruce_stairs", stairW);
            p.setDirect(level, chunkBox, 4, 7, 3, "minecraft:spruce_stairs", stairE | 4);
            p.fill(level, chunkBox, 4, 8, 3, 5, 8, 3, "minecraft:spruce_slab", 0);
            p.setDirect(level, chunkBox, 5, 7, 3, "minecraft:spruce_stairs", stairW | 4);
            p.setDirect(level, chunkBox, 6, 7, 3, "minecraft:spruce_stairs", stairE);
            p.setDirect(level, chunkBox, 6, 6, 3, "minecraft:spruce_stairs", stairW | 4);
            p.setDirect(level, chunkBox, 7, 6, 3, "minecraft:spruce_stairs", stairE);
            p.fill(level, chunkBox, 2, 5, 3, 3, 5, 3, "minecraft:spruce_planks", 0);
            p.set(level, chunkBox, 3, 5, 2, "minecraft:spruce_planks", 0);
            p.set(level, chunkBox, 3, 5, 1, "minecraft:spruce_slab", 0);
            p.fillDirect(level, chunkBox, 3, 4, 0, 14, 4, 0, "minecraft:spruce_stairs", stairN);
            p.setDirect(level, chunkBox, 8, 5, 1, "minecraft:spruce_stairs", stairW);
            p.set(level, chunkBox, 9, 5, 1, "minecraft:spruce_planks", 0);
            p.set(level, chunkBox, 10, 5, 1, "minecraft:spruce_slab", 0);
            p.setDirect(level, chunkBox, 9, 6, 1, "minecraft:spruce_stairs", stairW);
            p.setDirect(level, chunkBox, 10, 6, 1, "minecraft:spruce_stairs", stairE | 4);
            p.fill(level, chunkBox, 10, 7, 1, 11, 7, 1, "minecraft:spruce_slab", 0);
            p.setDirect(level, chunkBox, 11, 6, 1, "minecraft:spruce_stairs", stairW | 4);
            p.setDirect(level, chunkBox, 12, 6, 1, "minecraft:spruce_stairs", stairE);
            p.setDirect(level, chunkBox, 12, 5, 1, "minecraft:spruce_stairs", stairW | 4);
            p.setDirect(level, chunkBox, 13, 5, 1, "minecraft:spruce_stairs", stairE);
            p.fillDirect(level, chunkBox, 14, 4, 1, 14, 4, 10, "minecraft:spruce_stairs", stairE);
            p.setDirect(level, chunkBox, 13, 5, 10, "minecraft:spruce_stairs", stairE);
            p.setDirect(level, chunkBox, 12, 5, 10, "minecraft:spruce_stairs", stairW | 4);
            p.setDirect(level, chunkBox, 12, 6, 10, "minecraft:spruce_stairs", stairE);
            p.setDirect(level, chunkBox, 11, 6, 10, "minecraft:spruce_stairs", stairW | 4);
            p.fill(level, chunkBox, 10, 7, 10, 11, 7, 10, "minecraft:spruce_slab", 0);
            p.setDirect(level, chunkBox, 10, 6, 10, "minecraft:spruce_stairs", stairE | 4);
            p.setDirect(level, chunkBox, 9, 6, 10, "minecraft:spruce_stairs", stairW);
            p.setDirect(level, chunkBox, 9, 5, 10, "minecraft:spruce_stairs", stairE | 4);
            p.fillDirect(level, chunkBox, 9, 4, 11, 9, 4, 14, "minecraft:spruce_stairs", stairE);
            p.setDirect(level, chunkBox, 8, 5, 14, "minecraft:spruce_stairs", stairE);
            p.setDirect(level, chunkBox, 7, 5, 14, "minecraft:spruce_stairs", stairW | 4);
            p.setDirect(level, chunkBox, 7, 6, 14, "minecraft:spruce_stairs", stairE);
            p.setDirect(level, chunkBox, 6, 6, 14, "minecraft:spruce_stairs", stairW | 4);
            p.setDirect(level, chunkBox, 6, 7, 14, "minecraft:spruce_stairs", stairE);
            p.setDirect(level, chunkBox, 5, 7, 14, "minecraft:spruce_stairs", stairW | 4);
            p.fill(level, chunkBox, 4, 8, 14, 5, 8, 14, "minecraft:spruce_slab", 0);
            p.setDirect(level, chunkBox, 4, 7, 14, "minecraft:spruce_stairs", stairE | 4);
            p.setDirect(level, chunkBox, 3, 7, 14, "minecraft:spruce_stairs", stairW);
            p.setDirect(level, chunkBox, 3, 6, 14, "minecraft:spruce_stairs", stairE | 4);
            p.setDirect(level, chunkBox, 2, 6, 14, "minecraft:spruce_stairs", stairW);
            p.setDirect(level, chunkBox, 2, 5, 14, "minecraft:spruce_stairs", stairE | 4);
            p.setDirect(level, chunkBox, 1, 5, 14, "minecraft:spruce_stairs", stairW);
            p.fillDirect(level, chunkBox, 0, 4, 3, 0, 4, 14, "minecraft:spruce_stairs", stairW);
            for (int z = 6; z <= 11; z += 5) {
                for (int i = 0; i < 3; i++) {
                    p.setDirect(level, chunkBox, 2 + i, 5 + i, z, "minecraft:spruce_stairs", stairE | 4);
                    p.setDirect(level, chunkBox, 7 - i, 5 + i, z, "minecraft:spruce_stairs", stairW | 4);
                }
            }

            p.fill(level, chunkBox, 4, 5, 1, 7, 5, 1, "minecraft:oak_slab", 0);
            p.fillBrokenRoofBlocks(level, chunkBox, random, 4, 5, 2, 7, 5, 3);
            p.fillBrokenRoofBlocks(level, chunkBox, random, 8, 5, 2, 8, 5, 10);
            p.fillBrokenRoofStairs(level, chunkBox, random, 9, 6, 2, 9, 6, 9, stairW);
            p.fillRandom(level, chunkBox, random, 0.8F, 10, 7, 2, 11, 7, 9, "minecraft:oak_slab", 0);
            p.fillBrokenRoofStairs(level, chunkBox, random, 12, 6, 2, 12, 6, 9, stairE);
            p.fillBrokenRoofStairs(level, chunkBox, random, 13, 5, 2, 13, 5, 9, stairE);
            p.fillBrokenRoofStairs(level, chunkBox, random, 8, 5, 11, 8, 5, 13, stairE);
            p.fillBrokenRoofStairs(level, chunkBox, random, 7, 6, 4, 7, 6, 13, stairE);
            p.fillBrokenRoofStairs(level, chunkBox, random, 6, 7, 4, 6, 7, 7, stairE);
            p.fillBrokenRoofStairs(level, chunkBox, random, 6, 7, 11, 6, 7, 13, stairE);
            p.fill(level, chunkBox, 4, 8, 4, 5, 8, 5, "minecraft:oak_slab", 0);
            p.set(level, chunkBox, 5, 8, 6, "minecraft:oak_slab", 0);
            p.set(level, chunkBox, 4, 8, 11, "minecraft:oak_slab", 0);
            p.fill(level, chunkBox, 4, 8, 12, 5, 8, 13, "minecraft:oak_slab", 0);
            p.fillBrokenRoofStairs(level, chunkBox, random, 3, 7, 4, 3, 7, 6, stairW);
            p.fillBrokenRoofStairs(level, chunkBox, random, 3, 7, 10, 3, 7, 13, stairW);
            p.fillBrokenRoofStairs(level, chunkBox, random, 2, 6, 4, 2, 6, 13, stairW);
            p.fillBrokenRoofStairs(level, chunkBox, random, 1, 5, 4, 1, 5, 13, stairW);

            int metaN = p.getDecoMeta(3);
            int metaE = p.getDecoMeta(4);
            p.fillRandom(level, chunkBox, random, 0.05F, 12, 3, 3, 12, 3, 8, "minecraft:cobweb", 0);
            p.fillRandom(level, chunkBox, random, 0.05F, 10, 4, 3, 11, 4, 8, "minecraft:cobweb", 0);
            p.fillRandom(level, chunkBox, random, 0.05F, 5, 3, 2, 8, 3, 2, "minecraft:cobweb", 0);
            p.fillRandom(level, chunkBox, random, 0.05F, 5, 3, 3, 9, 3, 8, "minecraft:cobweb", 0);
            p.fillRandom(level, chunkBox, random, 0.05F, 2, 3, 5, 4, 3, 8, "minecraft:cobweb", 0);
            p.fillRandom(level, chunkBox, random, 0.05F, 2, 3, 10, 7, 3, 12, "minecraft:cobweb", 0);
            p.placeDoor(level, chunkBox, random, "minecraft:oak_door", 1, false, false, 11, 1, 2);
            p.placeDoor(level, chunkBox, random, "minecraft:oak_door", 1, false, random.nextBoolean(), 4, 1, 9);
            p.placeDoor(level, chunkBox, random, "minecraft:oak_door", 2, false, random.nextBoolean(), 8, 1, 11);
            p.fillRandom(level, chunkBox, random, 0.5F, 6, 2, 1, 7, 3, 1, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.5F, 13, 2, 5, 13, 2, 6, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.5F, 10, 2, 9, 11, 3, 9, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.5F, 4, 2, 13, 5, 3, 13, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.5F, 1, 2, 11, 1, 2, 11, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.5F, 1, 2, 6, 1, 2, 7, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.5F, 4, 6, 4, 5, 6, 4, "minecraft:glass_pane", 0);
            p.fillRandom(level, chunkBox, random, 0.5F, 4, 6, 13, 5, 6, 13, "minecraft:glass_pane", 0);
            p.setTrapdoor(level, chunkBox, 6, 4, 10, p.getDecoModelMeta(4) >> 2);
            p.fillLadder(level, chunkBox, 6, 2, 10, 6, 3, 10, metaN);

            p.setDirect(level, chunkBox, 12, 1, 5, "minecraft:oak_stairs", stairN | 4);
            p.setDirect(level, chunkBox, 12, 1, 6, "minecraft:oak_slab", 8);
            p.setDirect(level, chunkBox, 12, 1, 7, "minecraft:oak_stairs", stairS | 4);
            p.fillDirect(level, chunkBox, 9, 1, 4, 9, 1, 5, "minecraft:dark_oak_stairs", stairE | 4);
            p.fillDirect(level, chunkBox, 8, 1, 4, 8, 1, 5, "minecraft:dark_oak_slab", 8);
            p.fillDirect(level, chunkBox, 7, 1, 4, 7, 1, 5, "minecraft:dark_oak_stairs", stairW | 4);
            p.setDirect(level, chunkBox, 8, 1, 2, "minecraft:dark_oak_stairs", stairS | 4);
            p.setDirect(level, chunkBox, 7, 1, 2, "minecraft:dark_oak_stairs", stairW);
            p.setDirect(level, chunkBox, 6, 1, 2, "minecraft:dark_oak_stairs", stairS);
            p.fillDirect(level, chunkBox, 5, 1, 2, 5, 1, 3, "minecraft:dark_oak_stairs", stairE);
            p.setDirect(level, chunkBox, 5, 1, 4, "minecraft:dark_oak_stairs", stairN);
            p.setDirect(level, chunkBox, 10, 1, 5, "minecraft:oak_stairs", stairW);
            p.setDirect(level, chunkBox, 8, 1, 6, "minecraft:oak_stairs", stairN);
            p.setDirect(level, chunkBox, 9, 1, 8, "minecraft:oak_stairs", stairE);
            p.setDirect(level, chunkBox, 9, 2, 8, "minecraft:oak_stairs", stairE | 4);
            p.fill(level, chunkBox, 8, 1, 8, 8, 2, 8, "minecraft:bookshelf", 0);
            p.setDirect(level, chunkBox, 7, 1, 8, "minecraft:oak_stairs", stairW);
            p.setDirect(level, chunkBox, 7, 2, 8, "minecraft:oak_stairs", stairW | 4);
            p.fill(level, chunkBox, 7, 3, 8, 9, 3, 8, "minecraft:spruce_slab", 0);
            p.set(level, chunkBox, 4, 1, 5, "minecraft:smooth_stone", 0);
            p.setDirect(level, chunkBox, 3, 1, 5, random.nextBoolean() ? "reinhardtshbm:machine_electric_furnace_off" : "minecraft:furnace", metaN);
            p.fill(level, chunkBox, 2, 1, 5, 2, 1, 6, "minecraft:smooth_stone", 0);
            p.set(level, chunkBox, 2, 1, 7, "minecraft:water_cauldron", 2);
            p.set(level, chunkBox, 2, 1, 8, "minecraft:smooth_stone", 0);
            p.set(level, chunkBox, 4, 3, 5, "minecraft:smooth_stone", 0);
            p.set(level, chunkBox, 3, 3, 5, "minecraft:redstone_lamp", 0);
            p.set(level, chunkBox, 2, 3, 5, "minecraft:smooth_stone", 0);
            p.setDirect(level, chunkBox, 3, 3, 6, "reinhardtshbm:steel_wall", metaN);
            p.setDirect(level, chunkBox, 8, 2, 2, "reinhardtshbm:radiorec", p.getDecoMeta(2));
            p.set(level, chunkBox, 7, 2, 4, "minecraft:flower_pot", 0);
            p.fill(level, chunkBox, 2, 1, 12, 3, 1, 12, "minecraft:bookshelf", 0);
            p.setDirect(level, chunkBox, 4, 1, 12, "minecraft:oak_stairs", stairE | 4);
            p.setDirect(level, chunkBox, 5, 1, 12, "minecraft:oak_slab", 8);
            p.setDirect(level, chunkBox, 6, 1, 12, "minecraft:oak_stairs", stairW | 4);
            p.fill(level, chunkBox, 7, 1, 12, 7, 2, 12, "minecraft:bookshelf", 0);
            p.set(level, chunkBox, 5, 1, 11, "minecraft:dark_oak_slab", 0);
            p.placeBed(level, chunkBox, 1, 3, 1, 10);
            p.set(level, chunkBox, 4, 2, 12, "minecraft:flower_pot", 0);
            p.setDirect(level, chunkBox, 5, 2, 12, "reinhardtshbm:deco_computer", p.getDecoModelMeta(0));
            p.fillDirect(level, chunkBox, 4, 5, 5, 5, 5, 5, "minecraft:dark_oak_stairs", stairS | 4);
            p.set(level, chunkBox, 4, 5, 6, "minecraft:spruce_slab", 0);
            p.set(level, chunkBox, 7, 5, 7, "reinhardtshbm:crate_can", 0);
            p.set(level, chunkBox, 2, 5, 9, "reinhardtshbm:crate_can", 0);
            p.set(level, chunkBox, 3, 5, 11, "reinhardtshbm:crate_can", 0);
            if (random.nextBoolean()) {
                p.setDirect(level, chunkBox, 7, 5, 9, "reinhardtshbm:machine_diesel", metaE);
            }
            p.set(level, chunkBox, 6, 5, 12, random.nextBoolean() ? "reinhardtshbm:crate_weapon" : "reinhardtshbm:crate", 0);
            p.setDirect(level, chunkBox, 7, 1, 10, "reinhardtshbm:filing_cabinet", p.getDecoModelMeta(2));
            p.setDirect(level, chunkBox, 7, 5, 5, "minecraft:chest", metaE);
            p.set(level, chunkBox, 3, 2, 12, "reinhardtshbm:deco_loot", 0);
            p.set(level, chunkBox, 5, 6, 5, "reinhardtshbm:deco_loot", 0);
            p.placeRandomBobble(level, chunkBox, random, 5, 5, 12);
        }

        private static void placeSiloSurfaceMarker(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int originX, int originZ, int rotation) {
            Placement p = Placement.surfaceArea(level, originX, originZ, rotation, 43, 30, 27, 13, 2, 29, 18, 25);
            int stairW = p.getStairMeta(0);
            int stairE = p.getStairMeta(1);
            int stairN = p.getStairMeta(2);
            int stairS = p.getStairMeta(3);

            p.fill(level, chunkBox, 13, 26, 2, 42, 36, 20, "minecraft:air", 0);
            p.foundation(level, chunkBox, "reinhardtshbm:concrete_colored_ext", 0, 13, 2, 42, 20, 24);

            p.fill(level, chunkBox, 13, 25, 2, 42, 25, 4, "reinhardtshbm:asphalt", 0);
            p.fill(level, chunkBox, 13, 25, 5, 34, 25, 9, "reinhardtshbm:asphalt", 0);
            p.fill(level, chunkBox, 13, 25, 10, 14, 25, 18, "reinhardtshbm:asphalt", 0);
            p.fill(level, chunkBox, 24, 25, 10, 35, 25, 12, "reinhardtshbm:asphalt", 0);
            p.fill(level, chunkBox, 24, 25, 13, 26, 25, 18, "reinhardtshbm:asphalt", 0);
            p.fill(level, chunkBox, 13, 25, 19, 42, 25, 20, "reinhardtshbm:asphalt", 0);
            p.fill(level, chunkBox, 40, 25, 5, 42, 25, 18, "reinhardtshbm:asphalt", 0);
            p.fill(level, chunkBox, 39, 25, 10, 39, 25, 12, "reinhardtshbm:asphalt", 0);
            p.fill(level, chunkBox, 15, 25, 10, 23, 25, 10, "reinhardtshbm:concrete_colored_ext", 5);
            p.fill(level, chunkBox, 15, 25, 11, 15, 25, 17, "reinhardtshbm:concrete_colored_ext", 5);
            p.fill(level, chunkBox, 15, 25, 18, 23, 25, 18, "reinhardtshbm:concrete_colored_ext", 5);
            p.fill(level, chunkBox, 23, 25, 11, 23, 25, 17, "reinhardtshbm:concrete_colored_ext", 5);
            p.set(level, chunkBox, 16, 25, 11, "reinhardtshbm:concrete_colored_ext", 5);
            p.set(level, chunkBox, 22, 25, 11, "reinhardtshbm:concrete_colored_ext", 5);
            p.set(level, chunkBox, 22, 25, 17, "reinhardtshbm:concrete_colored_ext", 5);
            p.fillConcreteBricks(level, chunkBox, random, 27, 25, 13, 39, 25, 18);
            p.fill(level, chunkBox, 36, 25, 4, 38, 25, 4, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 35, 25, 5, 39, 25, 9, "reinhardtshbm:concrete_smooth", 0);

            p.fill(level, chunkBox, 13, 26, 2, 13, 28, 2, "reinhardtshbm:deco_steel", 0);
            p.fill(level, chunkBox, 42, 26, 2, 42, 28, 2, "reinhardtshbm:deco_steel", 0);
            p.fill(level, chunkBox, 13, 26, 20, 13, 28, 20, "reinhardtshbm:deco_steel", 0);
            p.fill(level, chunkBox, 42, 26, 20, 42, 28, 20, "reinhardtshbm:deco_steel", 0);

            p.fill(level, chunkBox, 38, 26, 2, 41, 27, 2, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 34, 26, 2, 36, 27, 2, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 30, 26, 2, 31, 27, 2, "reinhardtshbm:fence_metal", 0);
            p.set(level, chunkBox, 28, 27, 2, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 22, 26, 2, 28, 26, 2, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 23, 27, 2, 26, 27, 2, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 18, 26, 2, 20, 26, 2, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 14, 26, 2, 16, 26, 2, "reinhardtshbm:fence_metal", 0);
            p.set(level, chunkBox, 14, 27, 2, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 38, 28, 2, 41, 28, 2, "reinhardtshbm:barbed_wire", 5);
            p.fill(level, chunkBox, 35, 28, 2, 36, 28, 2, "reinhardtshbm:barbed_wire", 5);
            p.fill(level, chunkBox, 23, 28, 2, 25, 28, 2, "reinhardtshbm:barbed_wire", 5);
            p.set(level, chunkBox, 14, 28, 2, "reinhardtshbm:barbed_wire", 5);
            p.fill(level, chunkBox, 13, 26, 3, 13, 27, 4, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 13, 26, 5, 13, 26, 6, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 13, 26, 9, 13, 27, 9, "reinhardtshbm:fence_metal", 0);
            p.set(level, chunkBox, 13, 26, 11, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 13, 26, 12, 13, 27, 19, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 13, 28, 3, 13, 28, 4, "reinhardtshbm:barbed_wire", 2);
            p.fill(level, chunkBox, 13, 28, 15, 13, 28, 19, "reinhardtshbm:barbed_wire", 2);
            p.fill(level, chunkBox, 42, 26, 3, 42, 27, 4, "reinhardtshbm:fence_metal", 0);
            p.set(level, chunkBox, 42, 26, 7, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 42, 26, 9, 42, 26, 12, "reinhardtshbm:fence_metal", 0);
            p.set(level, chunkBox, 42, 26, 14, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 42, 26, 15, 42, 27, 19, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 42, 28, 3, 42, 28, 4, "reinhardtshbm:barbed_wire", 3);
            p.fill(level, chunkBox, 42, 28, 15, 42, 28, 19, "reinhardtshbm:barbed_wire", 3);
            p.fill(level, chunkBox, 14, 26, 20, 17, 27, 20, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 18, 26, 20, 22, 26, 20, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 20, 27, 20, 21, 27, 20, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 24, 26, 20, 25, 26, 20, "reinhardtshbm:fence_metal", 0);
            p.set(level, chunkBox, 27, 26, 20, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 29, 26, 20, 32, 27, 20, "reinhardtshbm:fence_metal", 0);
            p.set(level, chunkBox, 33, 26, 20, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 35, 26, 20, 37, 26, 20, "reinhardtshbm:fence_metal", 0);
            p.set(level, chunkBox, 36, 27, 20, "reinhardtshbm:fence_metal", 0);
            p.set(level, chunkBox, 39, 26, 20, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 40, 26, 20, 41, 27, 20, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 14, 28, 20, 17, 28, 20, "reinhardtshbm:barbed_wire", 4);
            p.fill(level, chunkBox, 29, 28, 20, 32, 28, 20, "reinhardtshbm:barbed_wire", 4);
            p.fill(level, chunkBox, 40, 28, 20, 41, 28, 20, "reinhardtshbm:barbed_wire", 4);

            p.set(level, chunkBox, 27, 26, 13, "reinhardtshbm:concrete_pillar", 0);
            p.set(level, chunkBox, 32, 26, 13, "reinhardtshbm:concrete_pillar", 0);
            p.set(level, chunkBox, 27, 26, 18, "reinhardtshbm:concrete_pillar", 0);
            p.set(level, chunkBox, 32, 26, 18, "reinhardtshbm:concrete_pillar", 0);
            p.fillConcreteBricks(level, chunkBox, random, 28, 26, 14, 31, 26, 17);
            p.fillConcreteStairs(level, chunkBox, random, 28, 26, 13, 31, 26, 13, stairN);
            p.fillConcreteStairs(level, chunkBox, random, 27, 26, 14, 27, 26, 17, stairW);
            p.fillConcreteStairs(level, chunkBox, random, 28, 26, 18, 31, 26, 18, stairS);
            p.fill(level, chunkBox, 27, 27, 13, 32, 27, 13, "reinhardtshbm:concrete_slab", 1);
            p.fill(level, chunkBox, 27, 27, 14, 27, 27, 17, "reinhardtshbm:concrete_slab", 1);
            p.fill(level, chunkBox, 27, 27, 18, 32, 27, 18, "reinhardtshbm:concrete_slab", 1);
            p.fill(level, chunkBox, 32, 27, 14, 32, 27, 17, "reinhardtshbm:concrete_slab", 1);
            p.set(level, chunkBox, 29, 27, 15, "reinhardtshbm:turret_howard_damaged", 0);

            p.set(level, chunkBox, 34, 26, 13, "reinhardtshbm:concrete_pillar", 0);
            p.set(level, chunkBox, 39, 26, 13, "reinhardtshbm:concrete_pillar", 0);
            p.set(level, chunkBox, 34, 26, 18, "reinhardtshbm:concrete_pillar", 0);
            p.set(level, chunkBox, 39, 26, 18, "reinhardtshbm:concrete_pillar", 0);
            p.fillConcreteBricks(level, chunkBox, random, 35, 26, 13, 38, 26, 13);
            p.fillConcreteBricks(level, chunkBox, random, 32, 26, 15, 34, 26, 17);
            p.fillConcreteStairs(level, chunkBox, random, 35, 26, 18, 38, 26, 18, stairS);
            p.fillConcreteStairs(level, chunkBox, random, 39, 26, 14, 39, 26, 15, stairE);
            p.fillDestroyedBricks(level, chunkBox, random, 35, 26, 14, 38, 26, 17);
            p.fill(level, chunkBox, 33, 27, 15, 33, 27, 17, "reinhardtshbm:concrete_slab", 1);
            p.set(level, chunkBox, 34, 27, 17, "reinhardtshbm:concrete_slab", 1);
            p.fill(level, chunkBox, 34, 27, 18, 36, 27, 18, "reinhardtshbm:concrete_slab", 1);
            p.fill(level, chunkBox, 37, 27, 13, 39, 27, 13, "reinhardtshbm:concrete_slab", 1);
            p.set(level, chunkBox, 39, 27, 14, "reinhardtshbm:concrete_slab", 1);
            p.set(level, chunkBox, 37, 25, 15, "reinhardtshbm:deco_steel", 0);
            p.set(level, chunkBox, 37, 26, 15, "reinhardtshbm:deco_pipe_rim_rusted", 0);
            p.set(level, chunkBox, 36, 25, 16, "reinhardtshbm:deco_steel", 0);
            p.set(level, chunkBox, 36, 26, 16, "reinhardtshbm:deco_pipe_quad_rusted", 0);

            p.fillConcreteBricks(level, chunkBox, random, 35, 26, 5, 39, 28, 5);
            p.fillConcreteBricks(level, chunkBox, random, 35, 26, 6, 35, 28, 9);
            p.fillConcreteBricks(level, chunkBox, random, 39, 26, 6, 39, 28, 9);
            p.fillConcreteBricks(level, chunkBox, random, 36, 26, 9, 38, 28, 10);
            p.fillConcreteBricks(level, chunkBox, random, 36, 27, 11, 38, 27, 11);
            p.fillConcreteBricks(level, chunkBox, random, 36, 26, 12, 38, 26, 12);
            p.fillConcreteStairs(level, chunkBox, random, 36, 28, 11, 38, 28, 11, stairS);
            p.fillConcreteStairs(level, chunkBox, random, 36, 27, 12, 38, 27, 12, stairS);
            p.fill(level, chunkBox, 36, 29, 5, 38, 29, 9, "reinhardtshbm:concrete", 0);
            p.fillDirect(level, chunkBox, 35, 29, 5, 35, 29, 9, "reinhardtshbm:concrete_stairs", stairW);
            p.fillDirect(level, chunkBox, 36, 29, 10, 38, 29, 10, "reinhardtshbm:concrete_stairs", stairS);
            p.fillDirect(level, chunkBox, 39, 29, 5, 39, 29, 9, "reinhardtshbm:concrete_stairs", stairE);
            p.set(level, chunkBox, 35, 27, 7, "minecraft:iron_bars", 0);
            p.set(level, chunkBox, 39, 27, 7, "minecraft:iron_bars", 0);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_metal", 1, false, random.nextBoolean(), 37, 26, 5);

            for (int j = 4; j <= 8; j += 2) {
                p.set(level, chunkBox, 20, 26, j, "reinhardtshbm:steel_beam", 2);
                p.fill(level, chunkBox, 16, 26, j, 16, 27, j, "reinhardtshbm:steel_beam", 3);
            }
            p.fill(level, chunkBox, 16, 28, 4, 17, 28, 8, "reinhardtshbm:brick_concrete_slab", 0);
            p.fill(level, chunkBox, 18, 27, 4, 19, 27, 8, "reinhardtshbm:brick_concrete_slab", 8);
            p.fill(level, chunkBox, 20, 27, 4, 20, 27, 8, "reinhardtshbm:brick_concrete_slab", 0);
            p.fill(level, chunkBox, 16, 28, 6, 17, 28, 6, "reinhardtshbm:brick_concrete_slab", 5);
            p.fill(level, chunkBox, 18, 27, 6, 19, 27, 6, "reinhardtshbm:brick_concrete_slab", 13);
            p.set(level, chunkBox, 20, 27, 6, "reinhardtshbm:brick_concrete_slab", 5);

            p.fillSiloSupplies(level, chunkBox, random, 27, 26, 7, 29, 26, 9);
            p.fillSiloSupplies(level, chunkBox, random, 17, 26, 4, 19, 26, 8);
            p.set(level, chunkBox, 32, 26, 5, "reinhardtshbm:barrel_corroded", 0);
            p.fillDestroyedBricks(level, chunkBox, random, 32, 26, 7, 32, 26, 7);
            p.set(level, chunkBox, 31, 26, 9, "reinhardtshbm:barrel_corroded", 0);
            p.fillDestroyedBricks(level, chunkBox, random, 31, 26, 11, 32, 26, 11);
            p.fillDestroyedBricks(level, chunkBox, random, 34, 26, 11, 34, 26, 11);
            p.fillDestroyedBricks(level, chunkBox, random, 41, 26, 17, 41, 26, 17);
            p.set(level, chunkBox, 37, 26, 19, "reinhardtshbm:concrete_slab", 1);

            p.set(level, chunkBox, 19, 26, 14, "reinhardtshbm:silo_hatch_large", 2);
            p.set(level, chunkBox, 16, 25, 17, "reinhardtshbm:radio_torch_receiver", 1);
            p.setDirect(level, chunkBox, 36, 26, 17, "minecraft:chest", 2);

            p.fill(level, chunkBox, 37, 26, 9, 37, 27, 10, "minecraft:air", 0);
            p.set(level, chunkBox, 37, 25, 10, "minecraft:air", 0);
            p.fill(level, chunkBox, 37, 24, 11, 37, 26, 11, "minecraft:air", 0);
            p.fill(level, chunkBox, 37, 23, 12, 37, 25, 12, "minecraft:air", 0);
            p.fill(level, chunkBox, 37, 21, 13, 37, 24, 14, "minecraft:air", 0);
            for (int i = 0; i < 5; i++) {
                p.fillConcreteBricks(level, chunkBox, random, 36, 24 - i, 9 + i, 38, 24 - i, 9 + i);
                p.setDirect(level, chunkBox, 37, 25 - i, 9 + i, "reinhardtshbm:concrete_smooth_stairs", stairS);
            }
            for (int i = 36; i <= 38; i += 2) {
                p.fillConcreteBricks(level, chunkBox, random, i, 26, 11, i, 26, 11);
                p.fillConcreteBricks(level, chunkBox, random, i, 25, 10, i, 25, 12);
                p.fillConcreteBricks(level, chunkBox, random, i, 24, 10, i, 24, 15);
                p.fillConcreteBricks(level, chunkBox, random, i, 23, 11, i, 23, 15);
                p.fill(level, chunkBox, i, 22, 12, i, 22, 15, "reinhardtshbm:concrete_colored", 11);
                p.fillConcreteBricks(level, chunkBox, random, i, 21, 13, i, 21, 15);
            }
            p.fill(level, chunkBox, 36, 20, 14, 38, 20, 15, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 36, 21, 14, 36, 22, 14, "minecraft:air", 0);

            p.set(level, chunkBox, 36, 23, 17, "minecraft:air", 0);
            p.fill(level, chunkBox, 34, 21, 13, 35, 23, 19, "minecraft:air", 0);
            p.fill(level, chunkBox, 33, 21, 13, 33, 23, 15, "minecraft:air", 0);
            p.fill(level, chunkBox, 29, 21, 16, 31, 23, 19, "minecraft:air", 0);
            p.fill(level, chunkBox, 29, 21, 12, 32, 23, 15, "minecraft:air", 0);
            p.fill(level, chunkBox, 28, 21, 10, 32, 23, 11, "minecraft:air", 0);
            p.fill(level, chunkBox, 27, 21, 7, 31, 23, 9, "minecraft:air", 0);
            p.fill(level, chunkBox, 27, 21, 5, 30, 23, 6, "minecraft:air", 0);
            p.fill(level, chunkBox, 27, 21, 4, 29, 23, 4, "minecraft:air", 0);
            p.fill(level, chunkBox, 27, 21, 3, 28, 23, 3, "minecraft:air", 0);
            p.fill(level, chunkBox, 26, 22, 7, 26, 23, 8, "minecraft:air", 0);
            p.fill(level, chunkBox, 25, 22, 7, 25, 23, 7, "minecraft:air", 0);
            p.fill(level, chunkBox, 24, 21, 2, 26, 23, 6, "minecraft:air", 0);
            p.fill(level, chunkBox, 22, 21, 5, 23, 23, 5, "minecraft:air", 0);
            p.fill(level, chunkBox, 16, 21, 1, 23, 23, 4, "minecraft:air", 0);

            for (int y = 20; y <= 24; y += 4) {
                p.fill(level, chunkBox, 15, y, 0, 23, y, 5, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 24, y, 1, 26, y, 6, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 25, y, 7, 26, y, 7, "reinhardtshbm:concrete_smooth", 0);
                p.set(level, chunkBox, 26, y, 8, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 27, y, 2, 28, y, 6, "reinhardtshbm:concrete_smooth", 0);
                p.set(level, chunkBox, 29, y, 3, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 29, y, 4, 30, y, 4, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 29, y, 5, 31, y, 6, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 27, y, 7, 32, y, 9, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 28, y, 10, 33, y, 20, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 34, y, 12, 35, y, 15, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 34, y, 16, 37, y, 20, "reinhardtshbm:concrete_smooth", 0);
            }

            for (int y = 21; y <= 23; y += 2) {
                p.fillConcreteBricks(level, chunkBox, random, 15, y, 0, 23, y, 0);
                p.fillConcreteBricks(level, chunkBox, random, 24, y, 1, 26, y, 1);
                p.fillConcreteBricks(level, chunkBox, random, 27, y, 2, 28, y, 2);
                p.fillConcreteBricks(level, chunkBox, random, 29, y, 3, 29, y, 3);
                p.fillConcreteBricks(level, chunkBox, random, 30, y, 4, 30, y, 4);
                p.fillConcreteBricks(level, chunkBox, random, 31, y, 5, 31, y, 6);
                p.fillConcreteBricks(level, chunkBox, random, 32, y, 7, 32, y, 9);
                p.fillConcreteBricks(level, chunkBox, random, 33, y, 10, 33, y, 12);
                p.fillConcreteBricks(level, chunkBox, random, 34, y, 12, 35, y, 12);
            }
            p.fill(level, chunkBox, 15, 22, 0, 23, 22, 0, "reinhardtshbm:concrete_colored", 11);
            p.fill(level, chunkBox, 24, 22, 1, 26, 22, 1, "reinhardtshbm:concrete_colored", 11);
            p.fill(level, chunkBox, 27, 22, 2, 28, 22, 2, "reinhardtshbm:concrete_colored", 11);
            p.set(level, chunkBox, 29, 22, 3, "reinhardtshbm:concrete_colored", 11);
            p.set(level, chunkBox, 30, 22, 4, "reinhardtshbm:concrete_colored", 11);
            p.fill(level, chunkBox, 31, 22, 5, 31, 22, 6, "reinhardtshbm:concrete_colored", 11);
            p.fill(level, chunkBox, 32, 22, 7, 32, 22, 9, "reinhardtshbm:concrete_colored", 11);
            p.fill(level, chunkBox, 33, 22, 10, 33, 22, 12, "reinhardtshbm:concrete_colored", 11);
            p.fill(level, chunkBox, 34, 22, 12, 35, 22, 12, "reinhardtshbm:concrete_colored", 11);

            p.fillConcreteBricks(level, chunkBox, random, 15, 21, 1, 15, 21, 4);
            p.fill(level, chunkBox, 15, 22, 1, 15, 22, 4, "reinhardtshbm:concrete_colored", 11);
            p.fillConcreteBricks(level, chunkBox, random, 15, 23, 1, 15, 23, 4);
            for (int y = 20; y <= 23; y += 3) {
                p.fillConcreteBricks(level, chunkBox, random, 15, y, 6, 16, y + 1, 6);
                p.fillConcreteBricks(level, chunkBox, random, 22, y, 6, 23, y + 1, 6);
                p.fillConcreteBricks(level, chunkBox, random, 24, y, 7, 24, y + 1, 7);
                p.fillConcreteBricks(level, chunkBox, random, 25, y, 8, 25, y + 1, 8);
                p.fillConcreteBricks(level, chunkBox, random, 26, y, 9, 26, y + 1, 9);
                p.fillConcreteBricks(level, chunkBox, random, 27, y, 10, 27, y + 1, 11);
                p.fillConcreteBricks(level, chunkBox, random, 27, y, 17, 27, y + 1, 18);
            }
            p.fillConcreteBricks(level, chunkBox, random, 15, 21, 5, 18, 21, 5);
            p.fillConcreteBricks(level, chunkBox, random, 20, 21, 5, 21, 21, 5);
            p.fillConcreteBricks(level, chunkBox, random, 15, 23, 5, 21, 23, 5);
            p.fillConcreteBricks(level, chunkBox, random, 28, 21, 12, 28, 21, 13);
            p.fillConcreteBricks(level, chunkBox, random, 28, 21, 15, 28, 21, 20);
            p.fillConcreteBricks(level, chunkBox, random, 28, 23, 12, 28, 23, 20);
            p.fill(level, chunkBox, 15, 22, 6, 16, 22, 6, "reinhardtshbm:concrete_colored", 11);
            p.set(level, chunkBox, 22, 22, 6, "reinhardtshbm:concrete_colored", 11);
            p.set(level, chunkBox, 23, 22, 6, "reinhardtshbm:reinforced_glass", 0);
            p.set(level, chunkBox, 24, 22, 7, "reinhardtshbm:reinforced_glass", 0);
            p.set(level, chunkBox, 25, 22, 8, "reinhardtshbm:reinforced_glass", 0);
            p.set(level, chunkBox, 26, 22, 9, "reinhardtshbm:reinforced_glass", 0);
            p.set(level, chunkBox, 27, 22, 10, "reinhardtshbm:reinforced_glass", 0);
            p.set(level, chunkBox, 27, 22, 11, "reinhardtshbm:concrete_colored", 11);
            p.fill(level, chunkBox, 27, 22, 17, 27, 22, 18, "reinhardtshbm:concrete_colored", 11);
            p.fill(level, chunkBox, 15, 22, 5, 18, 22, 5, "reinhardtshbm:concrete_colored", 11);
            p.fill(level, chunkBox, 20, 22, 5, 21, 22, 5, "reinhardtshbm:concrete_colored", 11);
            p.fill(level, chunkBox, 28, 22, 12, 28, 22, 13, "reinhardtshbm:concrete_colored", 11);
            p.fill(level, chunkBox, 28, 22, 15, 28, 22, 20, "reinhardtshbm:concrete_colored", 11);

            p.fillConcreteBricks(level, chunkBox, random, 29, 21, 20, 36, 21, 20);
            p.fill(level, chunkBox, 29, 22, 20, 36, 22, 20, "reinhardtshbm:concrete_colored", 11);
            p.fillConcreteBricks(level, chunkBox, random, 29, 23, 20, 36, 23, 20);
            p.fillConcreteBricks(level, chunkBox, random, 37, 21, 15, 37, 21, 20);
            p.fill(level, chunkBox, 37, 22, 15, 37, 22, 20, "reinhardtshbm:concrete_colored", 11);
            p.fillConcreteBricks(level, chunkBox, random, 37, 23, 15, 37, 23, 20);
            p.fillConcreteBricks(level, chunkBox, random, 37, 24, 15, 37, 24, 15);
            p.fillConcreteBricks(level, chunkBox, random, 32, 21, 16, 32, 21, 19);
            p.fill(level, chunkBox, 32, 22, 16, 32, 22, 19, "reinhardtshbm:concrete_colored", 11);
            p.fillConcreteBricks(level, chunkBox, random, 32, 23, 16, 32, 23, 19);

            p.fillConcreteStairs(level, chunkBox, random, 24, 23, 2, 26, 23, 2, stairS | 4);
            p.fillConcreteStairs(level, chunkBox, random, 27, 23, 3, 28, 23, 3, stairS | 4);
            p.fillConcreteStairs(level, chunkBox, random, 30, 23, 5, 30, 23, 6, stairW | 4);
            p.fillConcreteStairs(level, chunkBox, random, 31, 23, 7, 31, 23, 9, stairW | 4);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 1, true, random.nextBoolean(), 19, 21, 5);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 2, false, random.nextBoolean(), 28, 21, 14);

            int decoN = p.getDecoMeta(3);
            int decoE = p.getDecoMeta(4);
            int decoW = p.getDecoMeta(5);
            int pillarWE = p.getPillarMeta(4);
            int pillarNS = p.getPillarMeta(8);
            int decoModelN = p.getDecoModelMeta(0);
            int decoModelW = p.getDecoModelMeta(2);
            int decoModelE = p.getDecoModelMeta(3);

            p.fill(level, chunkBox, 33, 21, 19, 33, 23, 19, "reinhardtshbm:deco_steel", 0);
            p.fill(level, chunkBox, 33, 21, 17, 33, 23, 17, "reinhardtshbm:deco_steel", 0);
            p.setDirect(level, chunkBox, 33, 21, 18, "reinhardtshbm:tape_recorder", decoW);
            p.setDirect(level, chunkBox, 33, 22, 18, "reinhardtshbm:deco_crt", p.getCrtMeta(1) | 8);
            p.setDirect(level, chunkBox, 33, 23, 18, "reinhardtshbm:tape_recorder", decoW);
            p.fillDirect(level, chunkBox, 33, 21, 16, 33, 23, 16, "reinhardtshbm:tape_recorder", decoW);
            p.setDirect(level, chunkBox, 34, 21, 19, "reinhardtshbm:reinforced_stone_stairs", stairE | 4);
            p.setDirect(level, chunkBox, 34, 21, 18, "reinhardtshbm:brick_concrete_slab", 8);
            p.set(level, chunkBox, 34, 22, 18, "minecraft:heavy_weighted_pressure_plate", 0);
            p.setDirect(level, chunkBox, 36, 21, 16, "reinhardtshbm:capacitor_copper", decoE);
            p.set(level, chunkBox, 36, 21, 17, "reinhardtshbm:deco_steel", 0);
            p.set(level, chunkBox, 36, 21, 19, "reinhardtshbm:hadron_coil_alloy", 0);
            p.fillDirect(level, chunkBox, 36, 22, 16, 36, 23, 16, "reinhardtshbm:tape_recorder", decoE);
            p.setDirect(level, chunkBox, 36, 22, 17, "reinhardtshbm:deco_computer", decoModelW);
            p.fillDirect(level, chunkBox, 36, 21, 18, 36, 23, 18, "reinhardtshbm:tape_recorder", decoE);
            p.fillDirect(level, chunkBox, 36, 22, 19, 36, 23, 19, "reinhardtshbm:deco_crt", p.getCrtMeta(3) | 12);

            p.fill(level, chunkBox, 32, 21, 11, 32, 22, 11, "reinhardtshbm:deco_pipe_framed_green_rusted", 0);
            p.setDirect(level, chunkBox, 32, 23, 10, "reinhardtshbm:deco_pipe_framed_green_rusted", pillarNS);
            p.set(level, chunkBox, 32, 23, 11, "reinhardtshbm:deco_steel", 0);
            p.fillDirect(level, chunkBox, 32, 23, 12, 32, 23, 15, "reinhardtshbm:deco_pipe_framed_green_rusted", pillarNS);
            p.set(level, chunkBox, 30, 21, 16, "reinhardtshbm:turret_sentry_damaged", 0);

            p.fill(level, chunkBox, 27, 21, 9, 28, 21, 9, "reinhardtshbm:deco_steel", 0);
            p.set(level, chunkBox, 26, 21, 8, "reinhardtshbm:deco_beryllium", 0);
            p.fill(level, chunkBox, 25, 21, 7, 26, 21, 7, "reinhardtshbm:deco_steel", 0);
            p.fill(level, chunkBox, 24, 21, 5, 24, 21, 6, "reinhardtshbm:deco_steel", 0);
            p.setDirect(level, chunkBox, 28, 22, 9, "reinhardtshbm:deco_computer", decoModelN);
            p.setLeverOnFloor(level, chunkBox, 26, 22, 8, Direction.NORTH);
            p.setLeverOnFloor(level, chunkBox, 25, 22, 7, Direction.NORTH);
            p.setDirect(level, chunkBox, 28, 21, 7, "minecraft:oak_stairs", stairS);
            p.setDirect(level, chunkBox, 27, 21, 5, "minecraft:oak_stairs", stairW);
            p.setDirect(level, chunkBox, 30, 21, 5, "reinhardtshbm:tape_recorder", decoE);
            p.set(level, chunkBox, 27, 21, 3, "minecraft:flower_pot", 0);
            p.set(level, chunkBox, 25, 22, 2, "minecraft:flower_pot", 0);
            p.setDirect(level, chunkBox, 25, 21, 5, "reinhardtshbm:radio_telex", decoW);
            p.set(level, chunkBox, 26, 20, 8, "reinhardtshbm:radio_torch_sender", 0);
            p.set(level, chunkBox, 25, 20, 7, "reinhardtshbm:radio_torch_sender", 0);

            p.setDirect(level, chunkBox, 23, 23, 1, "reinhardtshbm:deco_pipe_framed_green_rusted", pillarWE);
            p.fillDirect(level, chunkBox, 16, 23, 1, 19, 23, 1, "reinhardtshbm:deco_pipe_framed_green_rusted", pillarWE);
            p.set(level, chunkBox, 20, 21, 1, "reinhardtshbm:deco_steel", 0);
            p.setDirect(level, chunkBox, 20, 22, 1, "reinhardtshbm:capacitor_copper", decoN);
            p.setDirect(level, chunkBox, 21, 21, 1, "reinhardtshbm:reinforced_stone_stairs", stairS | 4);
            p.setDirect(level, chunkBox, 21, 22, 1, "reinhardtshbm:deco_crt", p.getCrtMeta(2) | 4);
            p.set(level, chunkBox, 22, 21, 1, "reinhardtshbm:deco_steel", 0);
            p.setDirect(level, chunkBox, 22, 22, 1, "reinhardtshbm:capacitor_copper", decoN);
            p.fill(level, chunkBox, 20, 23, 1, 22, 23, 1, "reinhardtshbm:deco_steel", 0);
            p.set(level, chunkBox, 23, 21, 1, "reinhardtshbm:hev_battery_block", 0);
            p.setDirect(level, chunkBox, 18, 21, 2, "minecraft:oak_stairs", stairW);
            p.fill(level, chunkBox, 16, 21, 1, 16, 21, 3, "reinhardtshbm:deco_steel", 0);
            p.setDirect(level, chunkBox, 16, 22, 2, "reinhardtshbm:deco_computer", decoModelE);
            p.set(level, chunkBox, 16, 22, 3, "minecraft:flower_pot", 0);
            p.placeRandomBobble(level, chunkBox, random, 16, 22, 4);

            p.setDirect(level, chunkBox, 31, 21, 17, "reinhardtshbm:filing_cabinet", decoModelW);
            p.setDirect(level, chunkBox, 31, 21, 18, "reinhardtshbm:filing_cabinet", decoModelW);
            p.setDirect(level, chunkBox, 31, 21, 19, "reinhardtshbm:filing_cabinet", decoModelW);
            p.setDirect(level, chunkBox, 31, 22, 17, "reinhardtshbm:filing_cabinet", decoModelW);
            p.setDirect(level, chunkBox, 31, 22, 19, "reinhardtshbm:filing_cabinet", decoModelW);
            p.setDirect(level, chunkBox, 29, 21, 19, "reinhardtshbm:crate_steel", 2);
            p.setDirect(level, chunkBox, 29, 21, 18, "reinhardtshbm:filing_cabinet", decoModelE);
            p.setDirect(level, chunkBox, 29, 21, 17, "reinhardtshbm:filing_cabinet", decoModelE);
            p.setDirect(level, chunkBox, 31, 21, 8, "reinhardtshbm:filing_cabinet", decoModelW);
            p.setDirect(level, chunkBox, 25, 21, 2, "reinhardtshbm:crate_steel", 3);
            p.setDirect(level, chunkBox, 23, 21, 5, "reinhardtshbm:filing_cabinet", decoModelN);
            p.setDirect(level, chunkBox, 16, 21, 4, "reinhardtshbm:safe", decoW);

            placeSiloMainShaft(level, chunkBox, random, p, stairW, stairE, stairN, stairS);
        }

        private static void placeSiloMainShaft(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, Placement p,
                                               int stairW, int stairE, int stairN, int stairS) {
            int decoN = p.getDecoMeta(3);
            int decoS = p.getDecoMeta(2);
            int decoE = p.getDecoMeta(4);
            int decoW = p.getDecoMeta(5);
            int pillarWE = p.getPillarMeta(4);
            int pillarNS = p.getPillarMeta(8);
            int decoModelN = p.getDecoModelMeta(0);
            int decoModelE = p.getDecoModelMeta(3);
            int decoModelW = p.getDecoModelMeta(2);

            // Silo top.
            p.fill(level, chunkBox, 17, 21, 6, 21, 23, 6, "minecraft:air", 0);
            p.fill(level, chunkBox, 15, 21, 7, 23, 23, 10, "minecraft:air", 0);
            p.fill(level, chunkBox, 24, 21, 8, 24, 23, 10, "minecraft:air", 0);
            p.fill(level, chunkBox, 25, 21, 9, 25, 23, 10, "minecraft:air", 0);
            p.fill(level, chunkBox, 26, 21, 10, 26, 23, 10, "minecraft:air", 0);
            p.fill(level, chunkBox, 23, 21, 11, 26, 23, 17, "minecraft:air", 0);
            p.fill(level, chunkBox, 27, 21, 12, 27, 23, 16, "minecraft:air", 0);
            p.fill(level, chunkBox, 26, 21, 18, 26, 23, 18, "minecraft:air", 0);
            p.fill(level, chunkBox, 25, 21, 18, 25, 23, 19, "minecraft:air", 0);
            p.fill(level, chunkBox, 24, 21, 18, 24, 23, 20, "minecraft:air", 0);
            p.fill(level, chunkBox, 15, 21, 18, 23, 23, 21, "minecraft:air", 0);
            p.fill(level, chunkBox, 17, 21, 22, 21, 23, 22, "minecraft:air", 0);
            p.fill(level, chunkBox, 14, 21, 18, 14, 23, 20, "minecraft:air", 0);
            p.fill(level, chunkBox, 13, 21, 18, 13, 23, 19, "minecraft:air", 0);
            p.fill(level, chunkBox, 12, 21, 18, 12, 23, 18, "minecraft:air", 0);
            p.fill(level, chunkBox, 12, 21, 11, 15, 23, 17, "minecraft:air", 0);
            p.fill(level, chunkBox, 11, 21, 12, 11, 23, 16, "minecraft:air", 0);
            p.fill(level, chunkBox, 12, 21, 10, 12, 23, 10, "minecraft:air", 0);
            p.fill(level, chunkBox, 13, 21, 9, 13, 23, 10, "minecraft:air", 0);
            p.fill(level, chunkBox, 14, 21, 8, 14, 23, 10, "minecraft:air", 0);

            p.fill(level, chunkBox, 13, 20, 9, 13, 20, 11, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 14, 20, 8, 14, 20, 9, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 15, 20, 7, 16, 20, 8, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 17, 20, 6, 21, 20, 7, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 22, 20, 7, 23, 20, 8, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 24, 20, 8, 24, 20, 9, "reinhardtshbm:concrete_smooth", 0);
            p.set(level, chunkBox, 25, 20, 9, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 25, 20, 10, 26, 20, 11, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 26, 20, 12, 27, 20, 16, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 25, 20, 17, 26, 20, 18, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 24, 20, 19, 25, 20, 19, "reinhardtshbm:concrete_smooth", 0);
            p.set(level, chunkBox, 24, 20, 20, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 22, 20, 20, 23, 20, 21, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 17, 20, 21, 21, 20, 22, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 15, 20, 20, 16, 20, 21, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 14, 20, 19, 14, 20, 20, "reinhardtshbm:concrete_smooth", 0);

            p.fill(level, chunkBox, 14, 20, 10, 15, 20, 18, "reinhardtshbm:steel_grate", 7);
            p.fill(level, chunkBox, 13, 20, 12, 13, 20, 16, "reinhardtshbm:steel_grate", 7);
            p.fill(level, chunkBox, 17, 20, 8, 21, 20, 8, "reinhardtshbm:steel_grate", 7);
            p.fill(level, chunkBox, 15, 20, 9, 23, 20, 9, "reinhardtshbm:steel_grate", 7);
            p.fill(level, chunkBox, 16, 20, 10, 22, 20, 10, "reinhardtshbm:steel_grate", 7);
            p.fill(level, chunkBox, 23, 20, 10, 24, 20, 18, "reinhardtshbm:steel_grate", 7);
            p.fill(level, chunkBox, 25, 20, 12, 25, 20, 16, "reinhardtshbm:steel_grate", 7);
            p.fill(level, chunkBox, 22, 20, 19, 23, 20, 19, "reinhardtshbm:steel_grate", 7);
            p.fill(level, chunkBox, 15, 20, 19, 16, 20, 19, "reinhardtshbm:steel_grate", 7);
            p.fill(level, chunkBox, 16, 20, 18, 22, 20, 18, "reinhardtshbm:steel_grate", 7);

            p.fill(level, chunkBox, 11, 24, 12, 11, 24, 16, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 12, 24, 10, 15, 24, 18, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 13, 24, 9, 15, 24, 9, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 14, 24, 8, 15, 24, 8, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 13, 24, 19, 15, 24, 19, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 14, 24, 20, 15, 24, 20, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 17, 24, 6, 21, 24, 6, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 15, 24, 7, 23, 24, 7, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 16, 24, 8, 22, 24, 10, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 27, 24, 12, 27, 24, 16, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 23, 24, 10, 26, 24, 18, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 23, 24, 9, 25, 24, 9, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 23, 24, 8, 24, 24, 8, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 23, 24, 19, 25, 24, 19, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 23, 24, 20, 24, 24, 20, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 17, 24, 22, 21, 24, 22, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 15, 24, 21, 23, 24, 21, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 16, 24, 18, 22, 24, 20, "reinhardtshbm:concrete_smooth", 0);

            p.fillConcreteBricks(level, chunkBox, random, 14, 20, 7, 14, 24, 7);
            p.fillConcreteBricks(level, chunkBox, random, 13, 20, 8, 13, 24, 8);
            p.fillConcreteBricks(level, chunkBox, random, 12, 21, 9, 12, 24, 9);
            p.fillConcreteBricks(level, chunkBox, random, 11, 21, 10, 11, 24, 11);
            p.fillConcreteBricks(level, chunkBox, random, 10, 21, 12, 10, 24, 16);
            p.fillConcreteBricks(level, chunkBox, random, 11, 21, 17, 11, 24, 18);
            p.fillConcreteBricks(level, chunkBox, random, 12, 21, 19, 12, 24, 19);
            p.fillConcreteBricks(level, chunkBox, random, 13, 21, 20, 13, 24, 20);
            p.fillConcreteBricks(level, chunkBox, random, 14, 20, 21, 14, 24, 21);
            p.fillConcreteBricks(level, chunkBox, random, 15, 20, 22, 16, 24, 22);
            p.fillConcreteBricks(level, chunkBox, random, 17, 20, 23, 21, 24, 23);
            p.fillConcreteBricks(level, chunkBox, random, 22, 20, 22, 23, 24, 22);
            p.fillConcreteBricks(level, chunkBox, random, 24, 20, 21, 24, 24, 21);
            p.fillConcreteBricks(level, chunkBox, random, 25, 20, 20, 25, 24, 20);
            p.fillConcreteBricks(level, chunkBox, random, 26, 20, 19, 26, 24, 19);

            // Silo center.
            p.fill(level, chunkBox, 17, 2, 12, 21, 25, 16, "minecraft:air", 0);
            for (int y = 5; y <= 17; y += 4) {
                if (((y - 5) / 4) % 2 == 0) {
                    p.fill(level, chunkBox, 17, y, 8, 20, y + 3, 9, "minecraft:air", 0);
                    p.fill(level, chunkBox, 17, y, 10, 21, y + 2, 10, "minecraft:air", 0);
                    p.fill(level, chunkBox, 17, y, 18, 21, y + 2, 20, "minecraft:air", 0);
                } else {
                    p.fill(level, chunkBox, 18, y, 19, 21, y + 3, 20, "minecraft:air", 0);
                    p.fill(level, chunkBox, 17, y, 18, 21, y + 2, 18, "minecraft:air", 0);
                    p.fill(level, chunkBox, 17, y, 8, 21, y + 2, 10, "minecraft:air", 0);
                }
                p.fill(level, chunkBox, 22, y, 10, 22, y + 2, 10, "minecraft:air", 0);
                p.fill(level, chunkBox, 22, y, 9, 23, y + 2, 9, "minecraft:air", 0);
                p.fill(level, chunkBox, 23, y, 10, 24, y + 2, 18, "minecraft:air", 0);
                p.fill(level, chunkBox, 25, y, 12, 25, y + 2, 16, "minecraft:air", 0);
                p.fill(level, chunkBox, 22, y, 19, 23, y + 2, 19, "minecraft:air", 0);
                p.fill(level, chunkBox, 22, y, 18, 22, y + 2, 18, "minecraft:air", 0);
                p.fill(level, chunkBox, 16, y, 18, 16, y + 2, 18, "minecraft:air", 0);
                p.fill(level, chunkBox, 15, y, 19, 16, y + 2, 19, "minecraft:air", 0);
                p.fill(level, chunkBox, 14, y, 10, 15, y + 2, 18, "minecraft:air", 0);
                p.fill(level, chunkBox, 13, y, 12, 13, y + 2, 16, "minecraft:air", 0);
                p.fill(level, chunkBox, 15, y, 9, 16, y + 2, 9, "minecraft:air", 0);
                p.fill(level, chunkBox, 16, y, 10, 16, y + 2, 10, "minecraft:air", 0);
            }
            for (int y = 6; y <= 22; y += 4) {
                p.fill(level, chunkBox, 16, y, 11, 18, y + 1, 11, "minecraft:air", 0);
                p.fill(level, chunkBox, 16, y, 12, 16, y + 1, 13, "minecraft:air", 0);
                p.fill(level, chunkBox, 16, y, 15, 16, y + 1, 16, "minecraft:air", 0);
                p.fill(level, chunkBox, 16, y, 17, 18, y + 1, 17, "minecraft:air", 0);
                p.fill(level, chunkBox, 20, y, 17, 22, y + 1, 17, "minecraft:air", 0);
                p.fill(level, chunkBox, 22, y, 15, 22, y + 1, 16, "minecraft:air", 0);
                p.fill(level, chunkBox, 22, y, 12, 22, y + 1, 13, "minecraft:air", 0);
                p.fill(level, chunkBox, 20, y, 11, 22, y + 1, 11, "minecraft:air", 0);
            }

            p.fillConcreteBricks(level, chunkBox, random, 22, 24, 17, 22, 24, 17);
            p.fillConcreteBricks(level, chunkBox, random, 17, 24, 17, 21, 25, 17);
            p.fillConcreteBricks(level, chunkBox, random, 16, 24, 17, 16, 24, 17);
            p.fillConcreteBricks(level, chunkBox, random, 16, 24, 12, 16, 25, 16);
            p.fillConcreteBricks(level, chunkBox, random, 16, 24, 11, 16, 24, 11);
            p.fillConcreteBricks(level, chunkBox, random, 17, 24, 11, 21, 25, 11);
            p.fillConcreteBricks(level, chunkBox, random, 22, 24, 11, 22, 24, 11);
            p.fillConcreteBricks(level, chunkBox, random, 22, 24, 12, 22, 25, 16);
            p.fillConcreteBricks(level, chunkBox, random, 19, 5, 11, 19, 23, 11);
            p.fillConcreteBricks(level, chunkBox, random, 22, 5, 14, 22, 23, 14);
            p.fillConcreteBricks(level, chunkBox, random, 19, 5, 17, 19, 23, 17);
            p.fillConcreteBricks(level, chunkBox, random, 16, 5, 14, 16, 23, 14);

            for (int y = 8; y <= 20; y += 4) {
                for (int x = 16; x <= 22; x += 6) {
                    p.fill(level, chunkBox, x, y, 15, x, y, 16, "reinhardtshbm:steel_grate", 7);
                    p.fill(level, chunkBox, x, y, 12, x, y, 13, "reinhardtshbm:steel_grate", 7);
                    p.fill(level, chunkBox, x, y + 1, 15, x, y + 1, 16, "reinhardtshbm:fence_metal", 0);
                    p.fill(level, chunkBox, x, y + 1, 12, x, y + 1, 13, "reinhardtshbm:fence_metal", 0);
                }
                for (int z = 11; z <= 17; z += 6) {
                    p.fill(level, chunkBox, 16, y, z, 18, y, z, "reinhardtshbm:steel_grate", 7);
                    p.fill(level, chunkBox, 20, y, z, 22, y, z, "reinhardtshbm:steel_grate", 7);
                    p.fill(level, chunkBox, 16, y + 1, z, 18, y + 1, z, "reinhardtshbm:fence_metal", 0);
                    p.fill(level, chunkBox, 20, y + 1, z, 22, y + 1, z, "reinhardtshbm:fence_metal", 0);
                }
            }

            for (int y = 8; y <= 16; y += 4) {
                p.fill(level, chunkBox, 15, y, 11, 15, y, 17, "reinhardtshbm:concrete", 0);
                p.fill(level, chunkBox, 16, y, 10, 22, y, 10, "reinhardtshbm:concrete", 0);
                p.fill(level, chunkBox, 23, y, 11, 23, y, 17, "reinhardtshbm:concrete", 0);
                p.fill(level, chunkBox, 16, y, 18, 22, y, 18, "reinhardtshbm:concrete", 0);
                p.fill(level, chunkBox, 15, y, 9, 16, y, 9, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 14, y, 10, 15, y, 10, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 14, y, 11, 14, y, 17, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 13, y, 12, 13, y, 16, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 14, y, 18, 15, y, 18, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 15, y, 19, 16, y, 19, "reinhardtshbm:concrete_smooth", 0);

                if ((y / 4) % 2 == 0) {
                    p.fill(level, chunkBox, 20, y, 19, 21, y, 20, "reinhardtshbm:concrete_smooth", 0);
                    p.fill(level, chunkBox, 19, y, 19, 19, y + 1, 20, "reinhardtshbm:concrete_smooth", 0);
                    p.fill(level, chunkBox, 18, y, 19, 18, y + 2, 20, "reinhardtshbm:concrete_smooth", 0);
                    p.fill(level, chunkBox, 17, y, 19, 17, y + 3, 20, "reinhardtshbm:concrete_smooth", 0);
                    for (int i = 0; i < 4; i++) {
                        p.fillDirect(level, chunkBox, 20 - i, y + 1 + i, 19, 20 - i, y + 1 + i, 20, "reinhardtshbm:concrete_smooth_stairs", stairE);
                    }
                } else {
                    p.fill(level, chunkBox, 17, y, 8, 18, y, 9, "reinhardtshbm:concrete_smooth", 0);
                    p.fill(level, chunkBox, 19, y, 8, 19, y + 1, 9, "reinhardtshbm:concrete_smooth", 0);
                    p.fill(level, chunkBox, 20, y, 8, 20, y + 2, 9, "reinhardtshbm:concrete_smooth", 0);
                    p.fill(level, chunkBox, 21, y, 8, 21, y + 3, 9, "reinhardtshbm:concrete_smooth", 0);
                    for (int i = 0; i < 4; i++) {
                        p.fillDirect(level, chunkBox, 18 + i, y + 1 + i, 8, 18 + i, y + 1 + i, 9, "reinhardtshbm:concrete_smooth_stairs", stairW);
                    }
                }

                p.fill(level, chunkBox, 22, y, 9, 23, y, 9, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 23, y, 10, 24, y, 10, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 24, y, 11, 24, y, 17, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 25, y, 12, 25, y, 16, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 23, y, 18, 24, y, 18, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 22, y, 19, 23, y, 19, "reinhardtshbm:concrete_smooth", 0);
            }

            p.fillConcreteBricks(level, chunkBox, random, 17, 5, 7, 21, 19, 7);
            p.fillConcreteBricks(level, chunkBox, random, 15, 4, 8, 16, 19, 8);
            p.fillConcreteBricks(level, chunkBox, random, 14, 4, 9, 14, 19, 9);
            p.fillConcreteBricks(level, chunkBox, random, 13, 4, 10, 13, 19, 11);
            p.fillConcreteBricks(level, chunkBox, random, 12, 5, 12, 12, 19, 16);
            p.fillConcreteBricks(level, chunkBox, random, 13, 4, 17, 13, 19, 18);
            p.fillConcreteBricks(level, chunkBox, random, 14, 4, 19, 14, 19, 19);
            p.fillConcreteBricks(level, chunkBox, random, 15, 4, 20, 16, 19, 20);
            p.fillConcreteBricks(level, chunkBox, random, 17, 5, 21, 21, 19, 21);
            p.fillConcreteBricks(level, chunkBox, random, 22, 4, 20, 23, 19, 20);
            p.fillConcreteBricks(level, chunkBox, random, 24, 4, 19, 24, 19, 19);
            p.fillConcreteBricks(level, chunkBox, random, 25, 4, 17, 25, 19, 18);
            p.fillConcreteBricks(level, chunkBox, random, 26, 5, 12, 26, 19, 16);
            p.fillConcreteBricks(level, chunkBox, random, 25, 4, 10, 25, 19, 11);
            p.fillConcreteBricks(level, chunkBox, random, 24, 4, 9, 24, 19, 9);
            p.fillConcreteBricks(level, chunkBox, random, 22, 4, 8, 23, 19, 8);

            // Silo exhaust and launch pit.
            p.fill(level, chunkBox, 17, 0, 7, 21, 0, 21, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 18, 1, 7, 20, 1, 7, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 17, 1, 7, 17, 1, 12, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 21, 1, 7, 21, 1, 12, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 18, 1, 11, 18, 1, 12, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 20, 1, 11, 20, 1, 12, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 18, 1, 21, 20, 1, 21, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 17, 1, 16, 17, 1, 21, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 21, 1, 16, 21, 1, 21, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 18, 1, 16, 18, 1, 17, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 20, 1, 16, 20, 1, 17, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 12, 0, 12, 16, 0, 16, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 22, 0, 12, 26, 0, 16, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 12, 1, 13, 12, 1, 15, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 12, 1, 16, 16, 1, 16, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 12, 1, 12, 16, 1, 12, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 16, 1, 15, 17, 1, 15, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 16, 1, 13, 17, 1, 13, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 26, 1, 13, 26, 1, 15, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 22, 1, 16, 26, 1, 16, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 22, 1, 12, 26, 1, 12, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 21, 1, 15, 22, 1, 15, "reinhardtshbm:concrete_colored", 7);
            p.fill(level, chunkBox, 21, 1, 13, 22, 1, 13, "reinhardtshbm:concrete_colored", 7);

            p.fill(level, chunkBox, 18, 2, 21, 20, 3, 21, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 21, 2, 17, 21, 3, 21, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 22, 2, 16, 26, 3, 16, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 26, 2, 13, 26, 3, 15, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 22, 2, 12, 26, 3, 12, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 21, 2, 7, 21, 3, 11, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 18, 2, 7, 20, 3, 7, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 17, 2, 7, 17, 3, 11, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 12, 2, 12, 16, 3, 12, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 12, 2, 13, 12, 3, 15, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 12, 2, 16, 16, 3, 16, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 17, 2, 17, 17, 2, 21, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 17, 4, 17, 21, 4, 21, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 22, 4, 17, 23, 4, 19, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 24, 4, 17, 24, 4, 18, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 22, 4, 12, 26, 4, 16, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 24, 4, 10, 24, 4, 11, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 22, 4, 9, 23, 4, 11, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 17, 4, 7, 21, 4, 11, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 15, 4, 9, 16, 4, 11, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 14, 4, 10, 14, 4, 11, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 12, 4, 12, 16, 4, 16, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 14, 4, 17, 14, 4, 18, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 15, 4, 17, 16, 4, 19, "reinhardtshbm:concrete_smooth", 0);

            p.fill(level, chunkBox, 19, 5, 8, 19, 5, 9, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 20, 5, 8, 20, 6, 9, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 21, 5, 8, 21, 7, 9, "reinhardtshbm:concrete_smooth", 0);
            for (int i = 0; i < 4; i++) {
                p.fillDirect(level, chunkBox, 18 + i, 5 + i, 8, 18 + i, 5 + i, 9, "reinhardtshbm:concrete_smooth_stairs", stairW);
            }

            p.set(level, chunkBox, 18, 5, 11, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 20, 5, 11, 22, 5, 11, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 22, 5, 12, 22, 5, 13, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 22, 5, 15, 22, 5, 17, "reinhardtshbm:fence_metal", 0);
            p.set(level, chunkBox, 20, 5, 17, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 16, 5, 17, 18, 5, 17, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 16, 5, 15, 16, 5, 16, "reinhardtshbm:fence_metal", 0);
            p.fill(level, chunkBox, 16, 5, 11, 16, 5, 13, "reinhardtshbm:fence_metal", 0);
            p.set(level, chunkBox, 21, 5, 17, "minecraft:air", 0);
            p.set(level, chunkBox, 17, 5, 11, "minecraft:air", 0);

            p.fillDirect(level, chunkBox, 17, 2, 12, 17, 4, 12, "reinhardtshbm:ladder_steel", decoN);
            p.fillDirect(level, chunkBox, 21, 2, 16, 21, 4, 16, "reinhardtshbm:ladder_steel", decoS);

            p.fill(level, chunkBox, 18, 1, 13, 20, 1, 15, "reinhardtshbm:launch_pad_rusted", 0);
            p.set(level, chunkBox, 19, 0, 14, "reinhardtshbm:radio_torch_receiver", 3);

            p.fill(level, chunkBox, 18, 1, 8, 20, 3, 10, "minecraft:air", 0);
            p.fill(level, chunkBox, 18, 2, 11, 20, 3, 11, "minecraft:air", 0);
            p.fill(level, chunkBox, 19, 1, 11, 19, 1, 12, "minecraft:air", 0);
            p.fill(level, chunkBox, 19, 1, 16, 19, 1, 17, "minecraft:air", 0);
            p.fill(level, chunkBox, 18, 2, 17, 20, 3, 17, "minecraft:air", 0);
            p.fill(level, chunkBox, 18, 1, 18, 20, 3, 20, "minecraft:air", 0);
            p.fill(level, chunkBox, 13, 1, 13, 15, 3, 15, "minecraft:air", 0);
            p.fill(level, chunkBox, 16, 2, 13, 16, 3, 15, "minecraft:air", 0);
            p.fill(level, chunkBox, 16, 1, 14, 17, 1, 14, "minecraft:air", 0);
            p.fill(level, chunkBox, 21, 1, 14, 22, 1, 14, "minecraft:air", 0);
            p.fill(level, chunkBox, 22, 2, 13, 22, 3, 15, "minecraft:air", 0);
            p.fill(level, chunkBox, 23, 1, 13, 25, 3, 15, "minecraft:air", 0);

            // Red sector.
            p.fill(level, chunkBox, 2, 17, 9, 11, 18, 11, "minecraft:air", 0);
            p.fill(level, chunkBox, 2, 19, 10, 11, 19, 10, "minecraft:air", 0);
            p.fill(level, chunkBox, 2, 17, 13, 11, 18, 15, "minecraft:air", 0);
            p.fill(level, chunkBox, 2, 19, 14, 11, 19, 14, "minecraft:air", 0);
            p.fill(level, chunkBox, 8, 17, 17, 12, 18, 25, "minecraft:air", 0);
            p.fill(level, chunkBox, 9, 19, 17, 11, 19, 25, "minecraft:air", 0);
            p.fill(level, chunkBox, 2, 17, 17, 6, 18, 21, "minecraft:air", 0);
            p.fill(level, chunkBox, 3, 19, 17, 5, 19, 21, "minecraft:air", 0);
            p.fill(level, chunkBox, 2, 17, 22, 3, 18, 25, "minecraft:air", 0);
            p.fill(level, chunkBox, 3, 19, 22, 3, 19, 25, "minecraft:air", 0);
            p.fill(level, chunkBox, 5, 17, 23, 6, 19, 25, "minecraft:air", 0);
            p.fill(level, chunkBox, 1, 20, 8, 12, 20, 16, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 1, 20, 17, 13, 20, 26, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 2, 16, 7, 11, 16, 7, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 1, 16, 8, 12, 16, 11, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 1, 16, 12, 11, 16, 16, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 1, 16, 17, 12, 16, 18, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 1, 16, 19, 13, 16, 26, "reinhardtshbm:concrete_smooth", 0);
            p.set(level, chunkBox, 12, 16, 14, "reinhardtshbm:concrete_smooth", 0);
            p.fillConcreteBricks(level, chunkBox, random, 2, 17, 7, 11, 19, 7);
            p.fillConcreteBricks(level, chunkBox, random, 11, 17, 8, 12, 17, 8);
            p.fillConcreteBricks(level, chunkBox, random, 8, 17, 8, 8, 17, 8);
            p.fillConcreteBricks(level, chunkBox, random, 5, 17, 8, 5, 17, 8);
            p.fillConcreteBricks(level, chunkBox, random, 1, 17, 8, 2, 17, 8);
            p.fillConcreteBricks(level, chunkBox, random, 1, 19, 8, 12, 19, 8);
            p.fill(level, chunkBox, 1, 18, 8, 2, 18, 8, "reinhardtshbm:concrete_colored", 14);
            p.set(level, chunkBox, 5, 18, 8, "reinhardtshbm:concrete_colored", 14);
            p.set(level, chunkBox, 8, 18, 8, "reinhardtshbm:concrete_colored", 14);
            p.fill(level, chunkBox, 11, 18, 8, 12, 18, 8, "reinhardtshbm:concrete_colored", 14);
            p.fill(level, chunkBox, 1, 18, 9, 1, 18, 25, "reinhardtshbm:concrete_colored", 14);
            p.fill(level, chunkBox, 1, 18, 26, 13, 18, 26, "reinhardtshbm:concrete_colored", 14);
            p.fill(level, chunkBox, 13, 18, 17, 13, 18, 25, "reinhardtshbm:concrete_colored", 14);
            p.fill(level, chunkBox, 12, 18, 9, 12, 18, 16, "reinhardtshbm:concrete_colored", 14);
            p.fill(level, chunkBox, 13, 18, 10, 13, 18, 11, "reinhardtshbm:concrete_colored", 14);
            p.fill(level, chunkBox, 2, 18, 12, 11, 18, 12, "reinhardtshbm:concrete_colored", 14);
            p.fill(level, chunkBox, 2, 18, 16, 11, 18, 16, "reinhardtshbm:concrete_colored", 14);
            p.fill(level, chunkBox, 7, 18, 17, 7, 18, 25, "reinhardtshbm:concrete_colored", 14);
            p.fill(level, chunkBox, 4, 18, 22, 6, 18, 22, "reinhardtshbm:concrete_colored", 14);
            p.fill(level, chunkBox, 4, 18, 23, 4, 18, 25, "reinhardtshbm:concrete_colored", 14);
            for (int y = 17; y <= 19; y += 2) {
                p.fillConcreteBricks(level, chunkBox, random, 1, y, 9, 1, y, 25);
                p.fillConcreteBricks(level, chunkBox, random, 1, y, 26, 13, y, 26);
                p.fillConcreteBricks(level, chunkBox, random, 13, y, 19, 13, y, 25);
                p.fillConcreteBricks(level, chunkBox, random, 12, y, 9, 12, y, 11);
                p.fillConcreteBricks(level, chunkBox, random, 2, y, 12, 11, y, 12);
                p.fillConcreteBricks(level, chunkBox, random, 2, y, 16, 11, y, 16);
                p.fillConcreteBricks(level, chunkBox, random, 7, y, 17, 7, y, 25);
                p.fillConcreteBricks(level, chunkBox, random, 4, y, 22, 6, y, 22);
                p.fillConcreteBricks(level, chunkBox, random, 4, y, 23, 4, y, 25);
            }
            p.fillConcreteStairs(level, chunkBox, random, 2, 19, 9, 11, 19, 9, stairS | 4);
            p.fillConcreteStairs(level, chunkBox, random, 2, 19, 13, 11, 19, 13, stairS | 4);
            p.fillConcreteStairs(level, chunkBox, random, 2, 19, 11, 11, 19, 11, stairN | 4);
            p.fillConcreteStairs(level, chunkBox, random, 2, 19, 15, 11, 19, 15, stairN | 4);
            p.fillConcreteStairs(level, chunkBox, random, 12, 19, 17, 12, 19, 25, stairW | 4);
            p.fillConcreteStairs(level, chunkBox, random, 6, 19, 17, 6, 19, 21, stairW | 4);
            p.fillConcreteStairs(level, chunkBox, random, 8, 19, 17, 8, 19, 25, stairE | 4);
            p.fillConcreteStairs(level, chunkBox, random, 2, 19, 17, 2, 19, 25, stairE | 4);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 2, false, random.nextBoolean(), 12, 17, 14);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 3, false, random.nextBoolean(), 10, 17, 12);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 1, false, random.nextBoolean(), 10, 17, 16);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 1, false, random.nextBoolean(), 4, 17, 16);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_metal", 0, false, random.nextBoolean(), 4, 17, 24);
            p.setDirect(level, chunkBox, 12, 17, 17, "reinhardtshbm:reinforced_stone_stairs", stairW | 4);
            p.set(level, chunkBox, 12, 17, 18, "minecraft:cauldron", 0);
            p.fillDirect(level, chunkBox, 12, 17, 19, 12, 17, 20, "reinhardtshbm:reinforced_stone_stairs", stairW | 4);
            p.setDirect(level, chunkBox, 12, 17, 21, "reinhardtshbm:machine_electric_furnace_off", decoE);
            p.setDirect(level, chunkBox, 12, 18, 17, "reinhardtshbm:deco_toaster", p.getCrtMeta(3) | 4);
            p.setLeverOnWall(level, chunkBox, 12, 18, 18, 2, true);
            p.setDirect(level, chunkBox, 12, 18, 19, "reinhardtshbm:machine_microwave", decoE);
            p.set(level, chunkBox, 12, 18, 20, "reinhardtshbm:hev_battery_block", 0);
            p.setDirect(level, chunkBox, 8, 17, 17, "minecraft:oak_stairs", stairS);
            p.fillDirect(level, chunkBox, 8, 17, 19, 9, 17, 19, "reinhardtshbm:reinforced_stone_stairs", stairS | 4);
            p.fillDirect(level, chunkBox, 8, 17, 20, 9, 17, 20, "reinhardtshbm:reinforced_stone_stairs", stairN | 4);
            p.setDirect(level, chunkBox, 8, 17, 22, "minecraft:oak_stairs", stairN);
            p.setDirect(level, chunkBox, 10, 17, 23, "minecraft:oak_stairs", stairE);
            p.setDirect(level, chunkBox, 11, 17, 23, "minecraft:oak_stairs", stairS);
            p.setDirect(level, chunkBox, 12, 17, 23, "minecraft:oak_stairs", stairW);
            p.fillDirect(level, chunkBox, 10, 17, 25, 12, 17, 25, "reinhardtshbm:reinforced_stone_stairs", stairN | 4);
            p.setDirect(level, chunkBox, 11, 18, 25, "reinhardtshbm:deco_crt", p.getCrtMeta(0));
            p.set(level, chunkBox, 6, 17, 17, "reinhardtshbm:reinforced_stone", 0);
            p.fill(level, chunkBox, 6, 17, 18, 6, 17, 20, "minecraft:cauldron", 0);
            p.set(level, chunkBox, 6, 17, 21, "reinhardtshbm:reinforced_stone", 0);
            for (int i = 0; i < 3; i++) {
                p.setLeverOnWall(level, chunkBox, 6, 18, 18 + i, 2, true);
            }
            p.setDirect(level, chunkBox, 6, 17, 24, "minecraft:hopper", decoW);
            p.setTrapdoor(level, chunkBox, 6, 18, 24, decoModelW >> 2);
            for (int x = 3; x <= 7; x += 2) {
                p.setDirect(level, chunkBox, x, 17, 11, "reinhardtshbm:reinforced_stone_stairs", stairN | 4);
            }
            for (int x = 4; x <= 10; x += 3) {
                for (int y = 17; y <= 18; y++) {
                    p.placeBed(level, chunkBox, 1, x, y, 8);
                }
            }
            p.setDirect(level, chunkBox, 8, 17, 25, "reinhardtshbm:crate_steel", 2);
            p.setDirect(level, chunkBox, 2, 17, 11, "reinhardtshbm:crate_steel", 2);
            p.setDirect(level, chunkBox, 4, 17, 11, "reinhardtshbm:crate_steel", 2);
            p.setDirect(level, chunkBox, 6, 17, 11, "reinhardtshbm:crate_steel", 2);
            p.setDirect(level, chunkBox, 8, 17, 11, "reinhardtshbm:crate_steel", 2);
            p.fillMines(level, chunkBox, random, 2, 17, 9, 11, 17, 11);
            p.fillMines(level, chunkBox, random, 9, 17, 17, 11, 17, 24);
            p.fillMines(level, chunkBox, random, 5, 17, 23, 6, 17, 25);

            // Yellow sector.
            p.fill(level, chunkBox, 27, 13, 13, 33, 14, 15, "minecraft:air", 0);
            p.fill(level, chunkBox, 27, 15, 14, 33, 15, 14, "minecraft:air", 0);
            p.fill(level, chunkBox, 27, 13, 17, 33, 14, 21, "minecraft:air", 0);
            p.fill(level, chunkBox, 27, 15, 18, 33, 15, 20, "minecraft:air", 0);
            p.fill(level, chunkBox, 27, 13, 9, 29, 14, 11, "minecraft:air", 0);
            p.fill(level, chunkBox, 28, 15, 9, 28, 15, 11, "minecraft:air", 0);
            p.fill(level, chunkBox, 31, 13, 9, 33, 14, 11, "minecraft:air", 0);
            p.fill(level, chunkBox, 32, 15, 9, 32, 15, 11, "minecraft:air", 0);
            for (int y = 12; y <= 16; y += 4) {
                p.fill(level, chunkBox, 26, y, 17, 26, y, 22, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 27, y, 8, 34, y, 22, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 26, y, 8, 26, y, 11, "reinhardtshbm:concrete_smooth", 0);
            }
            p.set(level, chunkBox, 26, 12, 14, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 26, 14, 8, 34, 14, 8, "reinhardtshbm:concrete_colored", 4);
            p.fill(level, chunkBox, 34, 14, 9, 34, 14, 21, "reinhardtshbm:concrete_colored", 4);
            p.fill(level, chunkBox, 26, 14, 22, 34, 14, 22, "reinhardtshbm:concrete_colored", 4);
            p.fill(level, chunkBox, 26, 14, 9, 26, 14, 21, "reinhardtshbm:concrete_colored", 4);
            p.fill(level, chunkBox, 25, 14, 17, 25, 14, 18, "reinhardtshbm:concrete_colored", 4);
            p.fill(level, chunkBox, 25, 14, 10, 25, 14, 11, "reinhardtshbm:concrete_colored", 4);
            p.fill(level, chunkBox, 27, 14, 16, 33, 14, 16, "reinhardtshbm:concrete_colored", 4);
            p.fill(level, chunkBox, 27, 14, 12, 33, 14, 12, "reinhardtshbm:concrete_colored", 4);
            p.fill(level, chunkBox, 30, 14, 9, 30, 14, 11, "reinhardtshbm:concrete_colored", 4);
            for (int y = 13; y <= 15; y += 2) {
                p.fillConcreteBricks(level, chunkBox, random, 26, y, 8, 34, y, 8);
                p.fillConcreteBricks(level, chunkBox, random, 34, y, 9, 34, y, 21);
                p.fillConcreteBricks(level, chunkBox, random, 26, y, 22, 34, y, 22);
                p.fillConcreteBricks(level, chunkBox, random, 26, y, 15, 26, y, 21);
                p.fillConcreteBricks(level, chunkBox, random, 26, y, 9, 26, y, 13);
                p.fillConcreteBricks(level, chunkBox, random, 27, y, 16, 33, y, 16);
                p.fillConcreteBricks(level, chunkBox, random, 27, y, 12, 33, y, 12);
                p.fillConcreteBricks(level, chunkBox, random, 30, y, 9, 30, y, 11);
            }
            p.fillConcreteStairs(level, chunkBox, random, 27, 15, 21, 33, 15, 21, stairN | 4);
            p.fillConcreteStairs(level, chunkBox, random, 27, 15, 15, 33, 15, 15, stairN | 4);
            p.fillConcreteStairs(level, chunkBox, random, 27, 15, 17, 33, 15, 17, stairS | 4);
            p.fillConcreteStairs(level, chunkBox, random, 27, 15, 13, 33, 15, 13, stairS | 4);
            p.fillConcreteStairs(level, chunkBox, random, 33, 15, 9, 33, 15, 11, stairW | 4);
            p.fillConcreteStairs(level, chunkBox, random, 29, 15, 9, 29, 15, 11, stairW | 4);
            p.fillConcreteStairs(level, chunkBox, random, 31, 15, 9, 31, 15, 11, stairE | 4);
            p.fillConcreteStairs(level, chunkBox, random, 27, 15, 9, 27, 15, 11, stairE | 4);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 0, false, random.nextBoolean(), 26, 13, 14);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 3, false, random.nextBoolean(), 28, 13, 12);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 3, false, random.nextBoolean(), 32, 13, 12);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 1, false, random.nextBoolean(), 32, 13, 16);
            p.set(level, chunkBox, 27, 13, 9, "reinhardtshbm:crate_ammo", 0);
            p.set(level, chunkBox, 27, 13, 10, "reinhardtshbm:crate_can", 0);
            p.set(level, chunkBox, 27, 14, 9, "reinhardtshbm:crate_can", 0);
            p.set(level, chunkBox, 28, 13, 9, "reinhardtshbm:crate_can", 0);
            p.set(level, chunkBox, 29, 13, 9, "reinhardtshbm:barrel_corroded", 0);
            p.set(level, chunkBox, 31, 13, 9, "reinhardtshbm:crate_can", 0);
            p.setDirect(level, chunkBox, 31, 13, 11, "reinhardtshbm:deco_computer", decoModelE);
            p.set(level, chunkBox, 33, 13, 11, "reinhardtshbm:crate_can", 0);
            p.set(level, chunkBox, 33, 13, 17, "reinhardtshbm:machine_transformer", 0);
            p.fillSiloSupplies(level, chunkBox, random, 33, 13, 18, 33, 13, 20);
            p.setDirect(level, chunkBox, 31, 13, 21, "reinhardtshbm:anvil_iron", decoN);
            p.fill(level, chunkBox, 28, 13, 18, 29, 13, 20, "minecraft:oak_planks", 0);
            p.set(level, chunkBox, 29, 13, 19, "minecraft:crafting_table", 0);
            p.setDirect(level, chunkBox, 28, 14, 19, "reinhardtshbm:radiorec", decoE);
            p.setDirect(level, chunkBox, 28, 13, 17, "reinhardtshbm:deco_toaster", p.getCrtMeta(1));
            p.setDirect(level, chunkBox, 32, 13, 9, "reinhardtshbm:crate_steel", 2);
            p.setDirect(level, chunkBox, 33, 13, 9, "reinhardtshbm:safe", decoN);
            p.setDirect(level, chunkBox, 33, 13, 21, "reinhardtshbm:crate_steel", 2);
            p.fillMines(level, chunkBox, random, 27, 13, 13, 33, 13, 15);

            // Green sector.
            p.fill(level, chunkBox, 1, 9, 13, 11, 10, 15, "minecraft:air", 0);
            p.fill(level, chunkBox, 1, 11, 14, 8, 11, 14, "minecraft:air", 0);
            p.fill(level, chunkBox, 1, 9, 7, 6, 10, 11, "minecraft:air", 0);
            p.fill(level, chunkBox, 1, 11, 8, 6, 11, 10, "minecraft:air", 0);
            p.fill(level, chunkBox, 7, 9, 7, 11, 10, 7, "minecraft:air", 0);
            p.fill(level, chunkBox, 7, 9, 11, 11, 10, 11, "minecraft:air", 0);
            p.fill(level, chunkBox, 2, 9, 17, 4, 11, 23, "minecraft:air", 0);
            p.fill(level, chunkBox, 5, 9, 17, 5, 9, 18, "minecraft:air", 0);
            p.fill(level, chunkBox, 5, 9, 22, 5, 9, 23, "minecraft:air", 0);
            p.fill(level, chunkBox, 1, 9, 17, 1, 9, 18, "minecraft:air", 0);
            p.fill(level, chunkBox, 1, 9, 22, 1, 9, 23, "minecraft:air", 0);
            p.fill(level, chunkBox, 7, 9, 17, 11, 10, 23, "minecraft:air", 0);
            p.fill(level, chunkBox, 8, 11, 17, 10, 11, 23, "minecraft:air", 0);
            p.set(level, chunkBox, 12, 8, 14, "reinhardtshbm:concrete_smooth", 0);
            for (int y = 8; y <= 12; y += 4) {
                p.fill(level, chunkBox, 0, y, 6, 12, y, 11, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 0, y, 12, 11, y, 16, "reinhardtshbm:concrete_smooth", 0);
                p.fill(level, chunkBox, 0, y, 17, 12, y, 24, "reinhardtshbm:concrete_smooth", 0);
            }
            p.fill(level, chunkBox, 0, 10, 6, 12, 10, 6, "reinhardtshbm:concrete_colored", 13);
            p.fill(level, chunkBox, 0, 10, 7, 0, 10, 23, "reinhardtshbm:concrete_colored", 13);
            p.fill(level, chunkBox, 0, 10, 24, 12, 10, 24, "reinhardtshbm:concrete_colored", 13);
            p.fill(level, chunkBox, 12, 10, 7, 12, 10, 23, "reinhardtshbm:concrete_colored", 13);
            p.fill(level, chunkBox, 13, 10, 17, 13, 10, 18, "reinhardtshbm:concrete_colored", 13);
            p.fill(level, chunkBox, 13, 10, 10, 13, 10, 11, "reinhardtshbm:concrete_colored", 13);
            p.fill(level, chunkBox, 1, 10, 12, 11, 10, 12, "reinhardtshbm:concrete_colored", 13);
            p.fill(level, chunkBox, 1, 10, 16, 11, 10, 16, "reinhardtshbm:concrete_colored", 13);
            p.fill(level, chunkBox, 6, 10, 17, 6, 10, 23, "reinhardtshbm:concrete_colored", 13);
            for (int y = 9; y <= 11; y += 2) {
                p.fillConcreteBricks(level, chunkBox, random, 0, y, 6, 12, y, 6);
                p.fillConcreteBricks(level, chunkBox, random, 0, y, 7, 0, y, 23);
                p.fillConcreteBricks(level, chunkBox, random, 0, y, 24, 12, y, 24);
                p.fillConcreteBricks(level, chunkBox, random, 12, y, 17, 12, y, 23);
                p.fillConcreteBricks(level, chunkBox, random, 12, y, 7, 12, y, 11);
                p.fillConcreteBricks(level, chunkBox, random, 1, y, 12, 11, y, 12);
                p.fillConcreteBricks(level, chunkBox, random, 1, y, 16, 11, y, 16);
                p.fillConcreteBricks(level, chunkBox, random, 6, y, 17, 6, y, 23);
            }
            p.fillConcreteStairs(level, chunkBox, random, 1, 11, 7, 11, 11, 7, stairS | 4);
            p.fillConcreteStairs(level, chunkBox, random, 1, 11, 13, 11, 11, 13, stairS | 4);
            p.fillConcreteStairs(level, chunkBox, random, 1, 11, 11, 11, 11, 11, stairN | 4);
            p.fillConcreteStairs(level, chunkBox, random, 1, 11, 15, 11, 11, 15, stairN | 4);
            p.fillConcreteStairs(level, chunkBox, random, 11, 11, 17, 11, 11, 23, stairW | 4);
            p.fillConcreteStairs(level, chunkBox, random, 5, 11, 17, 5, 11, 18, stairW | 4);
            p.fillConcreteStairs(level, chunkBox, random, 5, 11, 22, 5, 11, 23, stairW | 4);
            p.fillConcreteStairs(level, chunkBox, random, 7, 11, 17, 7, 11, 23, stairE | 4);
            p.fillConcreteStairs(level, chunkBox, random, 1, 11, 17, 1, 11, 18, stairE | 4);
            p.fillConcreteStairs(level, chunkBox, random, 1, 11, 22, 1, 11, 23, stairE | 4);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 2, false, random.nextBoolean(), 12, 9, 14);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 1, false, random.nextBoolean(), 9, 9, 16);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 1, false, random.nextBoolean(), 3, 9, 16);
            p.placeDoor(level, chunkBox, random, "reinhardtshbm:door_bunker", 3, false, random.nextBoolean(), 3, 9, 12);
            p.fillDirect(level, chunkBox, 17, 11, 14, 18, 11, 14, "reinhardtshbm:deco_pipe_quad_rusted", pillarWE);
            p.set(level, chunkBox, 16, 11, 14, "reinhardtshbm:deco_steel", 0);
            p.fillDirect(level, chunkBox, 13, 11, 14, 15, 11, 14, "reinhardtshbm:deco_pipe_quad_rusted", pillarWE);
            p.set(level, chunkBox, 12, 11, 14, "reinhardtshbm:deco_steel", 0);
            p.fillDirect(level, chunkBox, 10, 11, 14, 11, 11, 14, "reinhardtshbm:deco_pipe_quad_rusted", pillarWE);
            p.set(level, chunkBox, 9, 11, 14, "reinhardtshbm:deco_steel", 0);
            p.setDirect(level, chunkBox, 9, 11, 15, "reinhardtshbm:deco_pipe_quad_rusted", pillarNS);
            p.set(level, chunkBox, 9, 11, 16, "reinhardtshbm:deco_steel", 0);
            p.fillDirect(level, chunkBox, 9, 11, 17, 9, 11, 19, "reinhardtshbm:deco_pipe_quad_rusted", pillarNS);
            p.set(level, chunkBox, 9, 11, 20, "reinhardtshbm:fluid_duct_gauge", 0);
            p.fillDirect(level, chunkBox, 9, 11, 21, 9, 11, 22, "reinhardtshbm:deco_pipe_quad_rusted", pillarNS);
            p.set(level, chunkBox, 9, 11, 23, "reinhardtshbm:deco_steel", 0);
            p.fill(level, chunkBox, 9, 9, 23, 9, 10, 23, "reinhardtshbm:deco_pipe_framed_rusted", 0);
            p.set(level, chunkBox, 9, 8, 23, "reinhardtshbm:deco_steel", 0);
            p.setDirect(level, chunkBox, 10, 11, 20, "reinhardtshbm:deco_pipe_quad_rusted", pillarWE);
            p.set(level, chunkBox, 11, 11, 20, "reinhardtshbm:deco_steel", 0);
            p.fill(level, chunkBox, 11, 9, 20, 11, 10, 20, "reinhardtshbm:deco_pipe_framed_rusted", 0);
            p.set(level, chunkBox, 11, 8, 20, "reinhardtshbm:deco_steel", 0);
            p.setDirect(level, chunkBox, 8, 11, 20, "reinhardtshbm:deco_pipe_quad_rusted", pillarWE);
            p.set(level, chunkBox, 7, 11, 20, "reinhardtshbm:deco_steel", 0);
            p.fill(level, chunkBox, 7, 9, 20, 7, 10, 20, "reinhardtshbm:deco_pipe_framed_rusted", 0);
            p.set(level, chunkBox, 7, 8, 20, "reinhardtshbm:deco_steel", 0);
            p.fill(level, chunkBox, 8, 8, 18, 10, 8, 22, "reinhardtshbm:deco_lead", 0);
            p.set(level, chunkBox, 7, 9, 17, "reinhardtshbm:lox_barrel", 0);
            p.set(level, chunkBox, 11, 9, 19, "reinhardtshbm:pink_barrel", 0);
            p.set(level, chunkBox, 11, 9, 22, "reinhardtshbm:pink_barrel", 0);
            p.fill(level, chunkBox, 11, 9, 23, 11, 10, 23, "reinhardtshbm:pink_barrel", 0);
            p.set(level, chunkBox, 10, 9, 23, "reinhardtshbm:pink_barrel", 0);
            p.fill(level, chunkBox, 7, 9, 23, 8, 9, 23, "reinhardtshbm:lox_barrel", 0);
            p.fill(level, chunkBox, 7, 9, 21, 7, 9, 22, "reinhardtshbm:lox_barrel", 0);
            for (int x = 1; x <= 5; x += 4) {
                p.fillDirect(level, chunkBox, x, 10, 17, x, 10, 18, "reinhardtshbm:deco_pipe_quad_red", pillarNS);
                p.fillDirect(level, chunkBox, x, 10, 22, x, 10, 23, "reinhardtshbm:deco_pipe_quad_red", pillarNS);
                p.fill(level, chunkBox, x, 9, 19, x, 9, 21, "reinhardtshbm:deco_lead", 0);
                p.fillDirect(level, chunkBox, x, 10, 19, x, 10, 21, "reinhardtshbm:capacitor_copper", x == 1 ? decoW : decoE);
                p.fill(level, chunkBox, x, 11, 19, x, 11, 21, "reinhardtshbm:deco_lead", 0);
            }
            p.set(level, chunkBox, 1, 9, 11, "reinhardtshbm:barrel_corroded", 0);
            p.fill(level, chunkBox, 1, 9, 8, 1, 9, 9, "reinhardtshbm:barrel_corroded", 0);
            p.fill(level, chunkBox, 1, 9, 7, 1, 10, 7, "reinhardtshbm:barrel_corroded", 0);
            p.set(level, chunkBox, 2, 9, 7, "reinhardtshbm:barrel_corroded", 0);
            p.fill(level, chunkBox, 7, 9, 10, 11, 9, 10, "reinhardtshbm:deco_lead", 0);
            p.fill(level, chunkBox, 7, 10, 10, 11, 10, 10, "reinhardtshbm:hadron_coil_alloy", 0);
            p.fill(level, chunkBox, 7, 11, 10, 11, 11, 10, "reinhardtshbm:deco_lead", 0);
            p.fill(level, chunkBox, 7, 9, 9, 11, 9, 9, "reinhardtshbm:hadron_coil_alloy", 0);
            p.fill(level, chunkBox, 8, 10, 9, 11, 10, 9, "reinhardtshbm:deco_red_copper", 0);
            p.setDirect(level, chunkBox, 7, 10, 9, "reinhardtshbm:red_cable_gauge", decoE);
            p.fill(level, chunkBox, 7, 11, 9, 11, 11, 9, "reinhardtshbm:hadron_coil_alloy", 0);
            p.fill(level, chunkBox, 7, 9, 8, 11, 9, 8, "reinhardtshbm:deco_lead", 0);
            p.fill(level, chunkBox, 7, 10, 8, 11, 10, 8, "reinhardtshbm:hadron_coil_alloy", 0);
            p.fill(level, chunkBox, 7, 11, 8, 11, 11, 8, "reinhardtshbm:deco_lead", 0);
            p.setDirect(level, chunkBox, 4, 9, 7, "reinhardtshbm:crate_steel", 2);
            p.fillMines(level, chunkBox, random, 1, 9, 7, 6, 9, 11);
            p.fillMines(level, chunkBox, random, 8, 9, 17, 10, 9, 22);

            // Black sector.
            p.fill(level, chunkBox, 27, 5, 13, 31, 6, 15, "minecraft:air", 0);
            p.fill(level, chunkBox, 27, 7, 14, 31, 7, 14, "minecraft:air", 0);
            p.fill(level, chunkBox, 28, 2, 11, 31, 3, 15, "minecraft:air", 0);
            p.fill(level, chunkBox, 28, 0, 11, 31, 0, 15, "minecraft:dirt", 0);
            p.fillRandom(level, chunkBox, random, 0.5F, 28, 0, 11, 31, 0, 15, "minecraft:podzol", 0);
            p.fill(level, chunkBox, 27, 4, 11, 31, 4, 15, "reinhardtshbm:concrete_smooth", 0);
            p.fill(level, chunkBox, 27, 8, 12, 32, 8, 16, "reinhardtshbm:concrete_smooth", 0);
            p.fillConcreteBricks(level, chunkBox, random, 27, 0, 10, 32, 4, 10);
            p.fillConcreteBricks(level, chunkBox, random, 27, 5, 12, 32, 5, 12);
            p.fill(level, chunkBox, 27, 6, 12, 32, 6, 12, "reinhardtshbm:concrete_colored", 15);
            p.fillConcreteBricks(level, chunkBox, random, 27, 7, 12, 32, 7, 12);
            p.fillConcreteBricks(level, chunkBox, random, 32, 0, 11, 32, 4, 12);
            p.fillConcreteBricks(level, chunkBox, random, 32, 0, 13, 32, 5, 15);
            p.fill(level, chunkBox, 32, 6, 13, 32, 6, 15, "reinhardtshbm:concrete_colored", 15);
            p.fillConcreteBricks(level, chunkBox, random, 32, 7, 13, 32, 7, 15);
            p.fillConcreteBricks(level, chunkBox, random, 27, 0, 16, 32, 5, 16);
            p.fill(level, chunkBox, 27, 6, 16, 32, 6, 16, "reinhardtshbm:concrete_colored", 15);
            p.fillConcreteBricks(level, chunkBox, random, 27, 7, 16, 32, 7, 16);
            p.fillConcreteBricks(level, chunkBox, random, 27, 0, 11, 27, 3, 15);
            p.fillConcreteStairs(level, chunkBox, random, 27, 7, 15, 31, 7, 15, stairN | 4);
            p.fillConcreteStairs(level, chunkBox, random, 27, 7, 13, 31, 7, 13, stairS | 4);
            p.fill(level, chunkBox, 28, 1, 11, 31, 1, 15, "minecraft:water", 0);
            p.fill(level, chunkBox, 26, 5, 14, 26, 6, 14, "reinhardtshbm:concrete_smooth", 0);
            p.fillDirect(level, chunkBox, 31, 2, 15, 31, 4, 15, "reinhardtshbm:ladder_steel", decoE);
            p.fillRandom(level, chunkBox, random, 0.15F, 27, 5, 13, 30, 6, 15, "minecraft:cobweb", 0);
            p.fillRandom(level, chunkBox, random, 0.15F, 31, 6, 13, 31, 6, 15, "minecraft:cobweb", 0);
            p.fillRandom(level, chunkBox, random, 0.15F, 27, 7, 14, 31, 7, 14, "minecraft:cobweb", 0);
            p.fillRandom(level, chunkBox, random, 0.15F, 28, 2, 11, 31, 2, 15, "reinhardtshbm:plant_reeds", 0);
            p.fillDirect(level, chunkBox, 28, 3, 12, 28, 3, 15, "reinhardtshbm:deco_pipe_framed_green_rusted", pillarNS);
            p.set(level, chunkBox, 28, 3, 11, "reinhardtshbm:deco_steel", 0);
            p.set(level, chunkBox, 28, 2, 11, "reinhardtshbm:deco_pipe_rim_green_rusted", 0);
            p.set(level, chunkBox, 28, 0, 11, "reinhardtshbm:deco_steel", 0);
            p.fill(level, chunkBox, 31, 1, 11, 31, 1, 12, "reinhardtshbm:deco_beryllium", 0);
            p.fillDirect(level, chunkBox, 31, 2, 11, 31, 2, 12, "reinhardtshbm:tape_recorder", decoE);
            p.set(level, chunkBox, 30, 2, 11, "reinhardtshbm:hev_battery_block", 0);
            p.setDirect(level, chunkBox, 31, 5, 13, "reinhardtshbm:safe", decoE);
            p.setDirect(level, chunkBox, 31, 5, 14, "reinhardtshbm:crate_steel", 2);
            p.setDirect(level, chunkBox, 31, 5, 15, "reinhardtshbm:safe", decoE);
            p.setDirect(level, chunkBox, 30, 1, 11, "reinhardtshbm:crate_iron", 2);
            p.fillMines(level, chunkBox, random, 27, 5, 13, 30, 5, 15);
        }
    }

    private static final class BunkerGenerator {
        private static final int SIZE_LIMIT_MIN = 7;
        private static final int SIZE_LIMIT_SPREAD = 6;
        private static final int DISTANCE_LIMIT = 40;

        private BunkerGenerator() {
        }

        static void generate(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int originX, int originZ, int rotation) {
            BunkerStart start = new BunkerStart(originX, originZ, rotation, 64, 7 + random.nextInt(SIZE_LIMIT_SPREAD));
            start.generate(random);
            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, originX, originZ);
            int targetTop = Math.max(level.getMinBuildHeight() + 16, Math.min(surfaceY - 8, 50));
            start.offsetY(targetTop - start.minY());
            for (BunkerRoom room : start.rooms) {
                room.place(level, chunkBox, random);
            }
        }

        private static final class BunkerStart {
            private final List<BunkerRoom> rooms = new ArrayList<>();
            private final List<BunkerRoom> queue = new ArrayList<>();
            private final List<Weight> weights = new ArrayList<>();
            private final int originX;
            private final int originZ;
            private final int sizeLimit;

            private BunkerStart(int originX, int originZ, int rotation, int y, int sizeLimit) {
                this.originX = originX;
                this.originZ = originZ;
                this.sizeLimit = sizeLimit;
                weights.add(new Weight(6, 3, RoomType.CORRIDOR));
                weights.add(new Weight(5, 4, RoomType.BEDROOM_L));
                weights.add(new Weight(10, 3, RoomType.FUN_JUNCTION));
                weights.add(new Weight(5, 2, RoomType.BATHROOM_L));
                weights.add(new Weight(7, 2, RoomType.LABORATORY));
                weights.add(new Weight(5, 1, RoomType.POWER_ROOM));
                add(new BunkerRoom(RoomType.START, originX, y, originZ, rotation, 8, 6, 8));
            }

            private void generate(RandomSource random) {
                while (!queue.isEmpty()) {
                    BunkerRoom room = queue.remove(random.nextInt(queue.size()));
                    room.build(this, random);
                }
            }

            private int minY() {
                int min = Integer.MAX_VALUE;
                for (BunkerRoom room : rooms) {
                    min = Math.min(min, room.box.minY());
                }
                return min == Integer.MAX_VALUE ? 64 : min;
            }

            private void offsetY(int amount) {
                for (BunkerRoom room : rooms) {
                    room.offsetY(amount);
                }
            }

            private boolean tryAdd(BunkerRoom caller, RandomSource random, int anchorX, int anchorY, int anchorZ, int facing) {
                if (rooms.size() > sizeLimit) {
                    return false;
                }
                if (Math.abs(anchorX - originX) > DISTANCE_LIMIT || Math.abs(anchorZ - originZ) > DISTANCE_LIMIT) {
                    return false;
                }
                int total = 0;
                for (Weight weight : weights) {
                    if (weight.canSpawn()) {
                        total += weight.weight;
                    }
                }
                if (total <= 0) {
                    return false;
                }
                for (int attempt = 0; attempt < 5; attempt++) {
                    int value = random.nextInt(total);
                    for (Weight weight : weights) {
                        if (!weight.canSpawn()) {
                            continue;
                        }
                        value -= weight.weight;
                        if (value >= 0) {
                            continue;
                        }
                        BunkerRoom room = BunkerRoom.create(weight.type, anchorX, anchorY, anchorZ, facing, random);
                        if (room == null || intersects(room.box)) {
                            break;
                        }
                        weight.spawned++;
                        if (!weight.canSpawn()) {
                            weights.remove(weight);
                        }
                        add(room);
                        return true;
                    }
                }
                return false;
            }

            private boolean intersects(BoundingBox box) {
                for (BunkerRoom room : rooms) {
                    if (room.box.intersects(box)) {
                        return true;
                    }
                }
                return false;
            }

            private void add(BunkerRoom room) {
                rooms.add(room);
                queue.add(room);
            }
        }

        private enum RoomType {
            START,
            CORRIDOR,
            BEDROOM_L,
            FUN_JUNCTION,
            BATHROOM_L,
            LABORATORY,
            POWER_ROOM
        }

        private static final class Weight {
            private final int weight;
            private final int limit;
            private final RoomType type;
            private int spawned;

            private Weight(int weight, int limit, RoomType type) {
                this.weight = weight;
                this.limit = limit;
                this.type = type;
            }

            private boolean canSpawn() {
                return limit < 0 || spawned < limit;
            }
        }

        private static final class BunkerRoom {
            private final RoomType type;
            private BoundingBox box;
            private final int facing;
            private final int width;
            private final int height;
            private final int depth;
            private final int decoA;
            private final int decoB;
            private boolean pathA;
            private boolean pathB;
            private boolean pathC;

            private BunkerRoom(RoomType type, int x, int y, int z, int facing, int width, int height, int depth) {
                this.type = type;
                this.facing = facing & 3;
                this.width = width;
                this.height = height;
                this.depth = depth;
                this.box = boxFor(x, y, z, 0, 0, 0, width, height, depth, this.facing);
                this.decoA = 0;
                this.decoB = 0;
            }

            private BunkerRoom(RoomType type, BoundingBox box, int facing, int width, int height, int depth, RandomSource random) {
                this.type = type;
                this.box = box;
                this.facing = facing & 3;
                this.width = width;
                this.height = height;
                this.depth = depth;
                this.decoA = random.nextInt(6);
                this.decoB = random.nextInt(6);
            }

            private static BunkerRoom create(RoomType type, int x, int y, int z, int facing, RandomSource random) {
                return switch (type) {
                    case CORRIDOR -> create(type, x, y, z, facing, -3, -1, 0, 6, 6, 7, random);
                    case BEDROOM_L -> create(type, x, y, z, facing, -9, -1, 0, 10, 6, 11, random);
                    case FUN_JUNCTION -> create(type, x, y, z, facing, -4, -1, 0, 8, 6, 12, random);
                    case BATHROOM_L -> create(type, x, y, z, facing, -3, -1, 0, 9, 6, 11, random);
                    case LABORATORY -> create(type, x, y, z, facing, -6, -1, 0, 9, 6, 13, random);
                    case POWER_ROOM -> create(type, x, y, z, facing, -4, -1, 0, 12, 6, 12, random);
                    default -> null;
                };
            }

            private static BunkerRoom create(RoomType type, int x, int y, int z, int facing, int offX, int offY, int offZ, int width, int height, int depth, RandomSource random) {
                BoundingBox box = boxFor(x, y, z, offX, offY, offZ, width, height, depth, facing);
                if (box.minY() <= 10) {
                    return null;
                }
                return new BunkerRoom(type, box, facing, width, height, depth, random);
            }

            private void build(BunkerStart start, RandomSource random) {
                switch (type) {
                    case START -> {
                        pathA = nextEast(start, random, 5, 1);
                        pathB = nextAntiNormal(start, random, 4, 1);
                        pathC = nextWest(start, random, 3, 1);
                    }
                    case CORRIDOR -> pathA = nextNormal(start, random, 3, 1);
                    case BEDROOM_L -> pathA = nextWest(start, random, 9, 1);
                    case FUN_JUNCTION -> {
                        pathA = nextNormal(start, random, 4, 1);
                        pathB = nextEast(start, random, 3, 1);
                        pathC = nextWest(start, random, 3, 1);
                    }
                    case BATHROOM_L -> pathA = nextNormal(start, random, 3, 1);
                    case LABORATORY -> {
                        pathA = nextWest(start, random, 3, 1);
                        pathB = nextNormal(start, random, 6, 1);
                    }
                    case POWER_ROOM -> pathA = nextEast(start, random, 4, 1);
                }
            }

            private void place(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
                Placement p = Placement.absolute(this.box, this.facing, this.width, this.depth);
                p.fill(level, chunkBox, 0, 0, 0, width - 1, height - 1, depth - 1, "reinhardtshbm:brick_concrete", 0);
                p.fill(level, chunkBox, 1, 1, 1, width - 2, 3, depth - 2, "minecraft:air", 0);
                p.fill(level, chunkBox, 1, 0, 1, width - 2, 0, depth - 2, "reinhardtshbm:vinyl_tile", 1);
                p.fill(level, chunkBox, 1, 4, 1, width - 2, 4, depth - 2, "reinhardtshbm:vinyl_tile", 0);
                p.fill(level, chunkBox, 0, 5, 0, width - 1, 5, depth - 1, "reinhardtshbm:reinforced_stone", 0);
                placeLights(p, level, chunkBox, random);
                placeDecorations(p, level, chunkBox, random);
                clearDoors(p, level, chunkBox);
            }

            private void placeLights(Placement p, WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
                int midX = Math.max(2, width / 2);
                int midZ = Math.max(2, depth / 2);
                p.set(level, chunkBox, midX, 5, midZ, "reinhardtshbm:reinforced_lamp_off", 0);
                p.set(level, chunkBox, midX, 4, midZ, "reinhardtshbm:fan", 0);
                if (width > 9 || depth > 9) {
                    p.set(level, chunkBox, Math.max(2, width - 3), 5, Math.max(2, depth - 3), "reinhardtshbm:reinforced_lamp_off", 0);
                    p.set(level, chunkBox, Math.max(2, width - 3), 4, Math.max(2, depth - 3), "reinhardtshbm:fan", 0);
                }
            }

            private void placeDecorations(Placement p, WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
                switch (type) {
                    case START -> {
                        p.set(level, chunkBox, 3, 1, depth - 2, "reinhardtshbm:deco_tungsten", 0);
                        p.set(level, chunkBox, 4, 1, depth - 2, "minecraft:chest", 3);
                        p.set(level, chunkBox, 5, 1, depth - 2, "reinhardtshbm:deco_tungsten", 0);
                        p.set(level, chunkBox, 4, 2, 4, "reinhardtshbm:deco_computer", 1);
                    }
                    case CORRIDOR -> {
                        decorateNook(p, level, chunkBox, 1, decoA);
                        decorateNook(p, level, chunkBox, width - 2, decoB);
                    }
                    case BEDROOM_L -> {
                        p.set(level, chunkBox, 5, 1, 1, "minecraft:red_bed", 1);
                        p.set(level, chunkBox, 5, 1, 3, "minecraft:red_bed", 1);
                        p.set(level, chunkBox, 3, 1, 6, "minecraft:red_bed", 2);
                        p.set(level, chunkBox, 1, 1, 6, "minecraft:red_bed", 2);
                        p.set(level, chunkBox, 8, 1, 5, "minecraft:note_block", 0);
                        p.set(level, chunkBox, 8, 1, 7, "minecraft:chest", 4);
                        p.set(level, chunkBox, 3, 1, 9, "reinhardtshbm:filing_cabinet", 0);
                    }
                    case FUN_JUNCTION -> {
                        p.fill(level, chunkBox, 1, 1, 1, 3, 1, 1, "minecraft:oak_stairs", 0);
                        p.set(level, chunkBox, 1, 1, 3, "minecraft:oak_fence", 0);
                        p.set(level, chunkBox, 1, 2, 3, "minecraft:oak_pressure_plate", 0);
                        p.set(level, chunkBox, 6, 1, 3, "minecraft:chest", 4);
                        p.set(level, chunkBox, 1, 2, 9, "reinhardtshbm:deco_computer", 3);
                        p.set(level, chunkBox, 6, 2, 9, "reinhardtshbm:tape_recorder", 4);
                    }
                    case BATHROOM_L -> {
                        for (int z = 2; z <= 6; z += 4) {
                            p.set(level, chunkBox, 1, 1, z, "minecraft:cauldron", random.nextInt(4));
                            p.set(level, chunkBox, 1, 1, z + 1, "reinhardtshbm:concrete_slab", 8);
                            p.set(level, chunkBox, 5, 1, z, "reinhardtshbm:door_metal", 0);
                        }
                        p.set(level, chunkBox, 4, 2, 9, "reinhardtshbm:fan", 2);
                    }
                    case LABORATORY -> {
                        p.fill(level, chunkBox, 1, 1, 5, 1, 3, 5, "reinhardtshbm:deco_tungsten", 0);
                        p.set(level, chunkBox, 1, 2, 6, "reinhardtshbm:deco_computer", 3);
                        p.set(level, chunkBox, 3, 1, 4, "minecraft:chest", 2);
                        p.set(level, chunkBox, 7, 1, 10, "minecraft:chest", 4);
                        p.set(level, chunkBox, 7, 2, 3, "reinhardtshbm:deco_red_copper", 0);
                    }
                    case POWER_ROOM -> {
                        p.fill(level, chunkBox, 7, 2, 6, 9, 2, 6, "reinhardtshbm:reinforced_glass", 0);
                        p.set(level, chunkBox, 8, 1, 1, "reinhardtshbm:deco_red_copper", 0);
                        p.set(level, chunkBox, 8, 2, 1, "reinhardtshbm:concrete_colored_ext", 5);
                        p.set(level, chunkBox, 8, 3, 1, "reinhardtshbm:deco_red_copper", 0);
                        p.set(level, chunkBox, 1, 2, 1, "reinhardtshbm:machine_transformer", 0);
                        p.set(level, chunkBox, 1, 2, 3, "reinhardtshbm:machine_battery", 4);
                        p.set(level, chunkBox, 1, 1, 7, "minecraft:chest", 4);
                    }
                }
            }

            private void decorateNook(Placement p, WorldGenLevel level, BoundingBox chunkBox, int x, int deco) {
                switch (deco) {
                    case 1 -> {
                        p.set(level, chunkBox, x, 1, 2, "reinhardtshbm:concrete_smooth_stairs", 7);
                        p.set(level, chunkBox, x, 2, 2, "reinhardtshbm:deco_computer", 1);
                    }
                    case 2 -> p.fill(level, chunkBox, x, 1, 2, x, 1, 4, "minecraft:oak_stairs", 3);
                    case 3 -> {
                        p.fill(level, chunkBox, x, 1, 2, x, 1, 4, "reinhardtshbm:concrete_smooth_stairs", 7);
                        p.set(level, chunkBox, x, 2, 2, "minecraft:flower_pot", 0);
                    }
                    case 4 -> {
                        p.fill(level, chunkBox, x, 1, 1, x, 3, 1, "reinhardtshbm:deco_tungsten", 0);
                        p.set(level, chunkBox, x, 2, 3, "reinhardtshbm:deco_computer", x < width / 2 ? 3 : 2);
                    }
                    case 5 -> {
                        p.set(level, chunkBox, x, 1, 1, "minecraft:oak_fence", 0);
                        p.set(level, chunkBox, x, 2, 1, "minecraft:oak_pressure_plate", 0);
                        p.set(level, chunkBox, x, 2, 3, "reinhardtshbm:radiorec", x < width / 2 ? 4 : 5);
                    }
                    default -> {
                        p.set(level, chunkBox, x, 1, 2, "minecraft:oak_stairs", 3);
                        p.set(level, chunkBox, x, 1, 4, "minecraft:oak_stairs", 2);
                        p.set(level, chunkBox, x, 1, 3, "minecraft:oak_fence", 0);
                        p.set(level, chunkBox, x, 2, 3, "minecraft:oak_pressure_plate", 0);
                    }
                }
            }

            private void clearDoors(Placement p, WorldGenLevel level, BoundingBox chunkBox) {
                int mid = Math.max(2, width / 2);
                p.set(level, chunkBox, mid - 1, 1, 0, "reinhardtshbm:door_bunker", 1);
                p.set(level, chunkBox, mid, 1, 0, "reinhardtshbm:door_bunker", 1);
                if (pathA) {
                    p.fill(level, chunkBox, mid - 1, 1, depth - 1, mid, 2, depth - 1, "minecraft:air", 0);
                }
                if (pathB) {
                    p.fill(level, chunkBox, width - 1, 1, Math.max(2, depth / 2 - 1), width - 1, 2, Math.max(2, depth / 2), "minecraft:air", 0);
                }
                if (pathC) {
                    p.fill(level, chunkBox, 0, 1, Math.max(2, depth / 2 - 1), 0, 2, Math.max(2, depth / 2), "minecraft:air", 0);
                }
            }

            private boolean nextNormal(BunkerStart start, RandomSource random, int offset, int offsetY) {
                return switch (facing) {
                    case 1 -> start.tryAdd(this, random, box.minX() - 1, box.minY() + offsetY, box.minZ() + offset, facing);
                    case 2 -> start.tryAdd(this, random, box.maxX() - offset, box.minY() + offsetY, box.minZ() - 1, facing);
                    case 3 -> start.tryAdd(this, random, box.maxX() + 1, box.minY() + offsetY, box.maxZ() - offset, facing);
                    default -> start.tryAdd(this, random, box.minX() + offset, box.minY() + offsetY, box.maxZ() + 1, facing);
                };
            }

            private boolean nextAntiNormal(BunkerStart start, RandomSource random, int offset, int offsetY) {
                return switch (facing) {
                    case 1 -> start.tryAdd(this, random, box.maxX() + 1, box.minY() + offsetY, box.maxZ() - offset, 3);
                    case 2 -> start.tryAdd(this, random, box.minX() + offset, box.minY() + offsetY, box.maxZ() + 1, 0);
                    case 3 -> start.tryAdd(this, random, box.minX() - 1, box.minY() + offsetY, box.minZ() + offset, 1);
                    default -> start.tryAdd(this, random, box.maxX() - offset, box.minY() + offsetY, box.minZ() - 1, 2);
                };
            }

            private boolean nextWest(BunkerStart start, RandomSource random, int offset, int offsetY) {
                return switch (facing) {
                    case 1 -> start.tryAdd(this, random, box.maxX() - offset, box.minY() + offsetY, box.minZ() - 1, 2);
                    case 2 -> start.tryAdd(this, random, box.maxX() + 1, box.minY() + offsetY, box.maxZ() - offset, 3);
                    case 3 -> start.tryAdd(this, random, box.minX() + offset, box.minY() + offsetY, box.maxZ() + 1, 0);
                    default -> start.tryAdd(this, random, box.minX() - 1, box.minY() + offsetY, box.minZ() + offset, 1);
                };
            }

            private boolean nextEast(BunkerStart start, RandomSource random, int offset, int offsetY) {
                return switch (facing) {
                    case 1 -> start.tryAdd(this, random, box.minX() + offset, box.minY() + offsetY, box.maxZ() + 1, 0);
                    case 2 -> start.tryAdd(this, random, box.minX() - 1, box.minY() + offsetY, box.minZ() + offset, 1);
                    case 3 -> start.tryAdd(this, random, box.maxX() - offset, box.minY() + offsetY, box.minZ() - 1, 2);
                    default -> start.tryAdd(this, random, box.maxX() + 1, box.minY() + offsetY, box.maxZ() - offset, 3);
                };
            }

            private void offsetY(int amount) {
                this.box = new BoundingBox(box.minX(), box.minY() + amount, box.minZ(), box.maxX(), box.maxY() + amount, box.maxZ());
            }
        }

        private static BoundingBox boxFor(int posX, int posY, int posZ, int offsetX, int offsetY, int offsetZ, int width, int height, int depth, int facing) {
            return switch (facing & 3) {
                case 1 -> new BoundingBox(posX - depth + 1 - offsetZ, posY + offsetY, posZ + offsetX, posX - offsetZ, posY + height - 1 + offsetY, posZ + width - 1 + offsetX);
                case 2 -> new BoundingBox(posX - width + 1 - offsetX, posY + offsetY, posZ - depth + 1 - offsetZ, posX - offsetX, posY + height - 1 + offsetY, posZ + offsetZ);
                case 3 -> new BoundingBox(posX + offsetZ, posY + offsetY, posZ - width + 1 - offsetX, posX + depth - 1 + offsetZ, posY + height - 1 + offsetY, posZ - offsetX);
                default -> new BoundingBox(posX + offsetX, posY + offsetY, posZ + offsetZ, posX + width - 1 + offsetX, posY + height - 1 + offsetY, posZ + depth - 1 + offsetZ);
            };
        }
    }

    private static final class Placement {
        private final int originX;
        private final int originY;
        private final int originZ;
        private final int rotation;
        private final int sizeX;
        private final int sizeZ;

        private Placement(int originX, int originY, int originZ, int rotation, int sizeX, int sizeZ) {
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.rotation = rotation & 3;
            this.sizeX = sizeX;
            this.sizeZ = sizeZ;
        }

        static Placement surface(WorldGenLevel level, int originX, int originZ, int rotation, int sizeX, int sizeY, int sizeZ) {
            int y = averageHeight(level, originX, originZ, sizeX, sizeZ, rotation) - 1;
            return new Placement(originX, y, originZ, rotation, sizeX, sizeZ);
        }

        static Placement surfaceArea(WorldGenLevel level, int originX, int originZ, int rotation, int sizeX, int sizeY, int sizeZ,
                                     int areaMinX, int areaMinZ, int areaSizeX, int areaSizeZ, int localSurfaceY) {
            int y = averageHeightArea(level, originX, originZ, sizeX, sizeZ, rotation, areaMinX, areaMinZ, areaSizeX, areaSizeZ)
                    - 1 - localSurfaceY;
            return new Placement(originX, y, originZ, rotation, sizeX, sizeZ);
        }

        static Placement absolute(BoundingBox box, int rotation, int sizeX, int sizeZ) {
            return new Placement(box.minX(), box.minY(), box.minZ(), rotation, sizeX, sizeZ);
        }

        Placement shiftedY(int amount) {
            return new Placement(originX, originY + amount, originZ, rotation, sizeX, sizeZ);
        }

        void set(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, String blockId, int meta) {
            BlockPos pos = pos(x, y, z);
            if (!chunkBox.isInside(pos) || pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
                return;
            }
            BlockState state = state(blockId, meta).rotate(rotation());
            level.setBlock(pos, state, 2);
        }

        void setIfReplaceableOrAir(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, String blockId, int meta) {
            BlockPos pos = pos(x, y, z);
            if (!chunkBox.isInside(pos) || pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
                return;
            }
            BlockState existing = level.getBlockState(pos);
            if (existing.isAir() || !existing.getFluidState().isEmpty() || !existing.isSolidRender(level, pos)) {
                level.setBlock(pos, state(blockId, meta).rotate(rotation()), 2);
            }
        }

        void setDirect(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, String blockId, int meta) {
            setState(level, chunkBox, x, y, z, state(blockId, meta));
        }

        void setState(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, BlockState state) {
            BlockPos pos = pos(x, y, z);
            if (!chunkBox.isInside(pos) || pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
                return;
            }
            level.setBlock(pos, state, 2);
        }

        void fill(WorldGenLevel level, BoundingBox chunkBox, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String blockId, int meta) {
            int x0 = Math.min(minX, maxX);
            int x1 = Math.max(minX, maxX);
            int y0 = Math.min(minY, maxY);
            int y1 = Math.max(minY, maxY);
            int z0 = Math.min(minZ, maxZ);
            int z1 = Math.max(minZ, maxZ);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        set(level, chunkBox, x, y, z, blockId, meta);
                    }
                }
            }
        }

        void fillDirect(WorldGenLevel level, BoundingBox chunkBox, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String blockId, int meta) {
            int x0 = Math.min(minX, maxX);
            int x1 = Math.max(minX, maxX);
            int y0 = Math.min(minY, maxY);
            int y1 = Math.max(minY, maxY);
            int z0 = Math.min(minZ, maxZ);
            int z1 = Math.max(minZ, maxZ);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        setDirect(level, chunkBox, x, y, z, blockId, meta);
                    }
                }
            }
        }

        void fillRandom(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, float chance, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String blockId, int meta) {
            int x0 = Math.min(minX, maxX);
            int x1 = Math.max(minX, maxX);
            int y0 = Math.min(minY, maxY);
            int y1 = Math.max(minY, maxY);
            int z0 = Math.min(minZ, maxZ);
            int z1 = Math.max(minZ, maxZ);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        if (random.nextFloat() <= chance) {
                            set(level, chunkBox, x, y, z, blockId, meta);
                        }
                    }
                }
            }
        }

        void fillRandomDirect(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, float chance, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String blockId, int meta) {
            int x0 = Math.min(minX, maxX);
            int x1 = Math.max(minX, maxX);
            int y0 = Math.min(minY, maxY);
            int y1 = Math.max(minY, maxY);
            int z0 = Math.min(minZ, maxZ);
            int z1 = Math.max(minZ, maxZ);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        if (random.nextFloat() <= chance) {
                            setDirect(level, chunkBox, x, y, z, blockId, meta);
                        }
                    }
                }
            }
        }

        void fillConcreteBricks(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            fillSelected(level, chunkBox, random, minX, minY, minZ, maxX, maxY, maxZ, Placement::concreteBricks);
        }

        void fillConcreteStairs(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int stairMeta) {
            int x0 = Math.min(minX, maxX);
            int x1 = Math.max(minX, maxX);
            int y0 = Math.min(minY, maxY);
            int y1 = Math.max(minY, maxY);
            int z0 = Math.min(minZ, maxZ);
            int z1 = Math.max(minZ, maxZ);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        setState(level, chunkBox, x, y, z, concreteStairs(random, stairMeta));
                    }
                }
            }
        }

        void fillDestroyedBricks(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            fillSelected(level, chunkBox, random, minX, minY, minZ, maxX, maxY, maxZ, Placement::destroyedBricks);
        }

        void fillSiloSupplies(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            fillSelected(level, chunkBox, random, minX, minY, minZ, maxX, maxY, maxZ, Placement::siloSupplies);
        }

        void fillMines(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            int x0 = Math.min(minX, maxX);
            int x1 = Math.max(minX, maxX);
            int y0 = Math.min(minY, maxY);
            int y1 = Math.max(minY, maxY);
            int z0 = Math.min(minZ, maxZ);
            int z1 = Math.max(minZ, maxZ);
            BlockState mine = state("reinhardtshbm:mine_ap", 0);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        if (random.nextInt(15) != 0) {
                            continue;
                        }
                        BlockPos pos = pos(x, y, z);
                        if (!chunkBox.isInside(pos) || pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
                            continue;
                        }
                        if (level.getBlockState(pos).isAir() && !level.getBlockState(pos.below()).isAir()) {
                            level.setBlock(pos, mine, 2);
                            if (level.getBlockEntity(pos) instanceof com.reinhardt.hbm.blockentity.LandmineBlockEntity landmine) {
                                landmine.setWaitingForPlayer(true);
                            }
                        }
                    }
                }
            }
        }

        void fillSandstone(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            fillSelected(level, chunkBox, random, minX, minY, minZ, maxX, maxY, maxZ, Placement::sandstone);
        }

        void fillLabTiles(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            fillSelected(level, chunkBox, random, minX, minY, minZ, maxX, maxY, maxZ, Placement::labTiles);
        }

        void fillSuperConcrete(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            fillSelected(level, chunkBox, random, minX, minY, minZ, maxX, maxY, maxZ, Placement::superConcrete);
        }

        private void fillSelected(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockSelector selector) {
            int x0 = Math.min(minX, maxX);
            int x1 = Math.max(minX, maxX);
            int y0 = Math.min(minY, maxY);
            int y1 = Math.max(minY, maxY);
            int z0 = Math.min(minZ, maxZ);
            int z1 = Math.max(minZ, maxZ);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        setState(level, chunkBox, x, y, z, selector.select(random));
                    }
                }
            }
        }

        void fillBrokenRoofStairs(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int stairMeta) {
            int x0 = Math.min(minX, maxX);
            int x1 = Math.max(minX, maxX);
            int y0 = Math.min(minY, maxY);
            int y1 = Math.max(minY, maxY);
            int z0 = Math.min(minZ, maxZ);
            int z1 = Math.max(minZ, maxZ);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        float chance = random.nextFloat();
                        if (chance < 0.7F) {
                            setDirect(level, chunkBox, x, y, z, "minecraft:oak_stairs", stairMeta);
                        } else if (chance < 0.97F) {
                            set(level, chunkBox, x, y, z, "minecraft:oak_slab", 0);
                        } else {
                            set(level, chunkBox, x, y, z, "minecraft:air", 0);
                        }
                    }
                }
            }
        }

        void fillBrokenRoofBlocks(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            int x0 = Math.min(minX, maxX);
            int x1 = Math.max(minX, maxX);
            int y0 = Math.min(minY, maxY);
            int y1 = Math.max(minY, maxY);
            int z0 = Math.min(minZ, maxZ);
            int z1 = Math.max(minZ, maxZ);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        float chance = random.nextFloat();
                        if (chance < 0.6F) {
                            set(level, chunkBox, x, y, z, "minecraft:oak_planks", 0);
                        } else if (chance < 0.8F) {
                            setDirect(level, chunkBox, x, y, z, "minecraft:oak_stairs", random.nextInt(4));
                        } else {
                            set(level, chunkBox, x, y, z, "minecraft:oak_slab", 0);
                        }
                    }
                }
            }
        }

        void foundation(WorldGenLevel level, BoundingBox chunkBox, String blockId, int meta, int minX, int minZ, int maxX, int maxZ, int localY) {
            int x0 = Math.min(minX, maxX);
            int x1 = Math.max(minX, maxX);
            int z0 = Math.min(minZ, maxZ);
            int z1 = Math.max(minZ, maxZ);
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    BlockPos start = pos(x, localY, z);
                    for (int y = start.getY(); y > level.getMinBuildHeight() && y >= start.getY() - 15; y--) {
                        BlockPos current = new BlockPos(start.getX(), y, start.getZ());
                        if (!chunkBox.isInside(current)) {
                            continue;
                        }
                        BlockState existing = level.getBlockState(current);
                        if (!existing.isAir() && existing.getFluidState().isEmpty() && existing.isSolidRender(level, current)) {
                            break;
                        }
                        level.setBlock(current, state(blockId, meta).rotate(rotation()), 2);
                    }
                }
            }
        }

        void setTrapdoor(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, int decoModelMeta) {
            int legacyFacing = decoModelMeta;
            Direction facing = switch (legacyFacing & 3) {
                case 1 -> Direction.SOUTH;
                case 2 -> Direction.WEST;
                case 3 -> Direction.EAST;
                default -> Direction.NORTH;
            };
            BlockState state = Blocks.OAK_TRAPDOOR.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            }
            if (state.hasProperty(BlockStateProperties.OPEN)) {
                state = state.setValue(BlockStateProperties.OPEN, false);
            }
            if (state.hasProperty(BlockStateProperties.HALF)) {
                state = state.setValue(BlockStateProperties.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM);
            }
            if (state.hasProperty(BlockStateProperties.POWERED)) {
                state = state.setValue(BlockStateProperties.POWERED, false);
            }
            setState(level, chunkBox, x, y, z, state);
        }

        void setLeverOnFloor(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, Direction facing) {
            BlockState state = Blocks.LEVER.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
                state = state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
            }
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation().rotate(facing));
            }
            if (state.hasProperty(BlockStateProperties.POWERED)) {
                state = state.setValue(BlockStateProperties.POWERED, false);
            }
            setState(level, chunkBox, x, y, z, state);
        }

        void setLeverOnWall(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, int dirMeta, boolean powered) {
            Direction facing = switch (getDecoMeta(switch (dirMeta) {
                case 1 -> 5;
                case 2 -> 4;
                case 3 -> 3;
                case 4 -> 2;
                default -> 3;
            })) {
                case 2 -> Direction.SOUTH;
                case 4 -> Direction.WEST;
                case 5 -> Direction.EAST;
                default -> Direction.NORTH;
            };
            BlockState state = Blocks.LEVER.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
                state = state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL);
            }
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            }
            if (state.hasProperty(BlockStateProperties.POWERED)) {
                state = state.setValue(BlockStateProperties.POWERED, powered);
            }
            setState(level, chunkBox, x, y, z, state);
        }

        void fillLadder(WorldGenLevel level, BoundingBox chunkBox, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int decoMeta) {
            Direction facing = switch (decoMeta) {
                case 2 -> Direction.SOUTH;
                case 4 -> Direction.EAST;
                case 5 -> Direction.WEST;
                default -> Direction.NORTH;
            };
            BlockState state = Blocks.LADDER.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            }
            int x0 = Math.min(minX, maxX);
            int x1 = Math.max(minX, maxX);
            int y0 = Math.min(minY, maxY);
            int y1 = Math.max(minY, maxY);
            int z0 = Math.min(minZ, maxZ);
            int z1 = Math.max(minZ, maxZ);
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        setState(level, chunkBox, x, y, z, state);
                    }
                }
            }
        }

        void placeBed(WorldGenLevel level, BoundingBox chunkBox, int meta, int x, int y, int z) {
            int headX = x;
            int headZ = z;
            switch (meta & 3) {
                case 1 -> headX--;
                case 2 -> headZ--;
                case 3 -> headX++;
                default -> headZ++;
            }
            int rotatedMeta = rotateBedMeta(meta);
            Direction facing = switch (rotatedMeta & 3) {
                case 1 -> Direction.WEST;
                case 2 -> Direction.NORTH;
                case 3 -> Direction.EAST;
                default -> Direction.SOUTH;
            };
            BlockState foot = Blocks.RED_BED.defaultBlockState();
            BlockState head = Blocks.RED_BED.defaultBlockState();
            if (foot.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                foot = foot.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                head = head.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            }
            if (foot.hasProperty(BlockStateProperties.BED_PART)) {
                foot = foot.setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
                head = head.setValue(BlockStateProperties.BED_PART, BedPart.HEAD);
            }
            if (foot.hasProperty(BlockStateProperties.OCCUPIED)) {
                foot = foot.setValue(BlockStateProperties.OCCUPIED, false);
                head = head.setValue(BlockStateProperties.OCCUPIED, false);
            }
            setState(level, chunkBox, x, y, z, foot);
            setState(level, chunkBox, headX, y, headZ, head);
        }

        void placeDoor(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, String blockId, int dirMeta, boolean opensRight, int x, int y, int z) {
            placeDoor(level, chunkBox, random, blockId, dirMeta, opensRight, random.nextBoolean(), x, y, z);
        }

        void placeDoor(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, String blockId, int dirMeta, boolean opensRight, boolean isOpen, int x, int y, int z) {
            dirMeta = rotateDoorMeta(dirMeta);
            BlockState base = state(blockId, dirMeta);
            BlockState lower = applyDoorProperties(base, dirMeta, false, opensRight, isOpen);
            BlockState upper = applyDoorProperties(base, dirMeta, true, opensRight, isOpen);
            setState(level, chunkBox, x, y, z, lower);
            setState(level, chunkBox, x, y + 1, z, upper);
        }

        void placeRandomBobble(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int x, int y, int z) {
            BlockPos pos = pos(x, y, z);
            if (!chunkBox.isInside(pos) || pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
                return;
            }
            BlockState state = HbmBlocks.BOBBLEHEAD.get().defaultBlockState()
                    .setValue(BobbleheadBlock.ROTATION, random.nextInt(16));
            level.setBlock(pos, state, 2);
            if (level.getBlockEntity(pos) instanceof BobbleheadBlockEntity bobble) {
                bobble.setType(BobbleheadType.byOrdinal(random.nextInt(24) + 1));
            }
        }

        int getPillarMeta(int meta) {
            return (rotation & 1) != 0 ? meta ^ 12 : meta;
        }

        int getDecoMeta(int meta) {
            return switch (rotation) {
                case 1 -> switch (meta) {
                    case 2 -> 5;
                    case 3 -> 4;
                    case 4 -> 2;
                    case 5 -> 3;
                    default -> meta;
                };
                case 2 -> switch (meta) {
                    case 2 -> 3;
                    case 3 -> 2;
                    case 4 -> 5;
                    case 5 -> 4;
                    default -> meta;
                };
                case 3 -> switch (meta) {
                    case 2 -> 4;
                    case 3 -> 5;
                    case 4 -> 3;
                    case 5 -> 2;
                    default -> meta;
                };
                default -> meta;
            };
        }

        int getDecoModelMeta(int meta) {
            return switch (rotation) {
                case 1 -> (((meta & 3) < 2) ? meta ^ 3 : meta ^ 2) << 2;
                case 2 -> (meta ^ 1) << 2;
                case 3 -> (((meta & 3) < 2) ? meta ^ 2 : meta ^ 3) << 2;
                default -> meta << 2;
            };
        }

        int getCrtMeta(int meta) {
            return (meta + rotation) & 3;
        }

        int getStairMeta(int meta) {
            return switch (rotation) {
                case 1 -> (meta & 3) < 2 ? meta ^ 2 : meta ^ 3;
                case 2 -> meta ^ 1;
                case 3 -> (meta & 3) < 2 ? meta ^ 3 : meta ^ 2;
                default -> meta;
            };
        }

        private BlockPos pos(int x, int y, int z) {
            int rx = switch (rotation) {
                case 1 -> sizeZ - 1 - z;
                case 2 -> sizeX - 1 - x;
                case 3 -> z;
                default -> x;
            };
            int rz = switch (rotation) {
                case 1 -> x;
                case 2 -> sizeZ - 1 - z;
                case 3 -> sizeX - 1 - x;
                default -> z;
            };
            return new BlockPos(originX + rx, originY + y, originZ + rz);
        }

        private int rotateDoorMeta(int dirMeta) {
            return switch (rotation) {
                case 1 -> (dirMeta + 1) & 3;
                case 2 -> dirMeta ^ 2;
                case 3 -> (dirMeta + 3) & 3;
                default -> dirMeta;
            };
        }

        private int rotateBedMeta(int meta) {
            return switch (rotation) {
                case 1 -> (meta + 1) & 3;
                case 2 -> meta ^ 2;
                case 3 -> (meta + 3) & 3;
                default -> meta;
            };
        }

        private BlockState applyDoorProperties(BlockState state, int dirMeta, boolean upper, boolean opensRight, boolean open) {
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, doorFacing(dirMeta));
            }
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                state = state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
            }
            if (state.hasProperty(BlockStateProperties.DOOR_HINGE)) {
                state = state.setValue(BlockStateProperties.DOOR_HINGE, opensRight ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
            }
            if (state.hasProperty(BlockStateProperties.OPEN)) {
                state = state.setValue(BlockStateProperties.OPEN, open);
            }
            if (state.hasProperty(BlockStateProperties.POWERED)) {
                state = state.setValue(BlockStateProperties.POWERED, false);
            }
            return state;
        }

        private Direction doorFacing(int dirMeta) {
            return switch (dirMeta & 3) {
                case 0 -> Direction.EAST;
                case 1 -> Direction.SOUTH;
                case 2 -> Direction.WEST;
                default -> Direction.NORTH;
            };
        }

        private net.minecraft.world.level.block.Rotation rotation() {
            return switch (rotation) {
                case 1 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
                case 2 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
                case 3 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
                default -> net.minecraft.world.level.block.Rotation.NONE;
            };
        }

        private static int averageHeight(WorldGenLevel level, int originX, int originZ, int sizeX, int sizeZ, int rotation) {
            int footprintX = (rotation & 1) != 0 ? sizeZ : sizeX;
            int footprintZ = (rotation & 1) != 0 ? sizeX : sizeZ;
            int total = 0;
            int samples = 0;
            for (int x = 0; x < footprintX; x++) {
                for (int z = 0; z < footprintZ; z++) {
                    total += level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, originX + x, originZ + z);
                    samples++;
                }
            }
            return samples == 0 ? 64 : total / samples;
        }

        private static int averageHeightArea(WorldGenLevel level, int originX, int originZ, int sizeX, int sizeZ, int rotation,
                                             int areaMinX, int areaMinZ, int areaSizeX, int areaSizeZ) {
            int total = 0;
            int samples = 0;
            for (int x = areaMinX; x < areaMinX + areaSizeX; x++) {
                for (int z = areaMinZ; z < areaMinZ + areaSizeZ; z++) {
                    int rx = switch (rotation & 3) {
                        case 1 -> sizeZ - 1 - z;
                        case 2 -> sizeX - 1 - x;
                        case 3 -> z;
                        default -> x;
                    };
                    int rz = switch (rotation & 3) {
                        case 1 -> x;
                        case 2 -> sizeZ - 1 - z;
                        case 3 -> sizeX - 1 - x;
                        default -> z;
                    };
                    total += level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, originX + rx, originZ + rz);
                    samples++;
                }
            }
            return samples == 0 ? 64 : total / samples;
        }

        private static BlockState concreteBricks(RandomSource random) {
            float chance = random.nextFloat();
            if (chance < 0.4F) {
                return state("reinhardtshbm:brick_concrete", 0);
            }
            if (chance < 0.7F) {
                return state("reinhardtshbm:brick_concrete_mossy", 0);
            }
            if (chance < 0.9F) {
                return state("reinhardtshbm:brick_concrete_cracked", 0);
            }
            return state("reinhardtshbm:brick_concrete_broken", 0);
        }

        private static BlockState concreteStairs(RandomSource random, int meta) {
            float chance = random.nextFloat();
            if (chance < 0.4F) {
                return state("reinhardtshbm:brick_concrete_stairs", meta);
            }
            if (chance < 0.7F) {
                return state("reinhardtshbm:brick_concrete_mossy_stairs", meta);
            }
            if (chance < 0.9F) {
                return state("reinhardtshbm:brick_concrete_cracked_stairs", meta);
            }
            return state("reinhardtshbm:brick_concrete_broken_stairs", meta);
        }

        private static BlockState destroyedBricks(RandomSource random) {
            float chance = random.nextFloat();
            if (chance < 0.3F) {
                return state("reinhardtshbm:brick_concrete_slab", concreteSlabVariant(random));
            }
            if (chance < 0.6F) {
                return concreteStairs(random, random.nextInt(4));
            }
            if (chance < 0.9F) {
                return concreteBricks(random);
            }
            return Blocks.AIR.defaultBlockState();
        }

        private static BlockState siloSupplies(RandomSource random) {
            float chance = random.nextFloat();
            if (chance < 0.2F) {
                return state("reinhardtshbm:barrel_corroded", 0);
            }
            if (chance < 0.4F) {
                return state("reinhardtshbm:crate_can", 0);
            }
            if (chance < 0.45F) {
                return state("reinhardtshbm:red_barrel", 0);
            }
            if (chance < 0.5F) {
                return state("reinhardtshbm:pink_barrel", 0);
            }
            return Blocks.AIR.defaultBlockState();
        }

        private static int concreteSlabVariant(RandomSource random) {
            float chance = random.nextFloat();
            if (chance >= 0.4F && chance < 0.7F) {
                return 1;
            }
            if (chance < 0.9F) {
                return 2;
            }
            return 3;
        }

        private static BlockState sandstone(RandomSource random) {
            float chance = random.nextFloat();
            if (chance > 0.6F) {
                return state("minecraft:sandstone", 0);
            }
            if (chance < 0.5F) {
                return state("reinhardtshbm:reinforced_sand", 0);
            }
            return state("minecraft:sand", 0);
        }

        private static BlockState labTiles(RandomSource random) {
            float chance = random.nextFloat();
            if (chance < 0.5F) {
                return state("reinhardtshbm:tile_lab", 0);
            }
            if (chance < 0.9F) {
                return state("reinhardtshbm:tile_lab_cracked", 0);
            }
            return state("reinhardtshbm:tile_lab_broken", 0);
        }

        private static BlockState superConcrete(RandomSource random) {
            return state("reinhardtshbm:concrete_super", random.nextInt(6) + 10);
        }

        private static BlockState state(String blockId, int meta) {
            ResourceLocation id = ResourceLocation.tryParse(blockId);
            if (id != null && "minecraft".equals(id.getNamespace())) {
                return HbmLegacyNbtTemplate.stateFromLegacy(blockId, meta);
            }
            if (blockId.equals("minecraft:air")) {
                return Blocks.AIR.defaultBlockState();
            }
            return HbmLegacyNbtTemplate.stateFromLegacyId(blockId, meta);
        }

        @FunctionalInterface
        private interface BlockSelector {
            BlockState select(RandomSource random);
        }
    }
}
