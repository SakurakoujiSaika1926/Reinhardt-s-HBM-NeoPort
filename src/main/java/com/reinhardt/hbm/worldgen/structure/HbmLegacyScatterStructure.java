package com.reinhardt.hbm.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmWorldgenStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Spawns the 1.7.10 structures that existed only as Java WorldGenerator source
 * rather than NBT files.  They are modern structures, not configured features,
 * because these old schematics routinely cross chunk boundaries and a feature's
 * WorldGenRegion write radius would clip them.
 */
public final class HbmLegacyScatterStructure extends Structure {
    public static final MapCodec<HbmLegacyScatterStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            Codec.STRING.fieldOf("legacy_scatter").forGetter(HbmLegacyScatterStructure::legacyScatterName)
    ).apply(instance, HbmLegacyScatterStructure::new));

    private final Kind kind;

    public HbmLegacyScatterStructure(StructureSettings settings, String legacyScatterName) {
        super(settings);
        this.kind = Kind.require(legacyScatterName);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!HbmConfig.legacyDungeonGenerationEnabled(HbmConfig.GENERATE_HBM_STRUCTURES.get())) {
            return Optional.empty();
        }

        ChunkPos chunk = context.chunkPos();
        int centerX = chunk.getMiddleBlockX();
        int centerZ = chunk.getMiddleBlockZ();
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(centerX),
                QuartPos.fromBlock(64),
                QuartPos.fromBlock(centerZ),
                context.randomState().sampler()
        );
        if (!this.kind.canSpawnIn(biome)) {
            return Optional.empty();
        }

        RandomSource random = chunkRandom(context.seed(), chunk, this.kind);
        int rate = this.kind.spawnRate();
        if (rate <= 0 || random.nextInt(rate) != 0) {
            return Optional.empty();
        }

        int x = chunk.getMinBlockX() + random.nextInt(16);
        int z = chunk.getMinBlockZ() + random.nextInt(16);
        int y = this.kind.randomY()
                ? random.nextInt(256)
                : firstFreeHeight(context, x, z);
        BlockPos origin = new BlockPos(x, y, z);
        long templateSeed = random.nextLong();

        return Optional.of(new GenerationStub(origin, pieces -> pieces.addPiece(new HbmLegacyScatterPiece(
                this.kind,
                origin,
                templateSeed,
                context.heightAccessor().getMinBuildHeight(),
                context.heightAccessor().getMaxBuildHeight()
        ))));
    }

    private static int firstFreeHeight(GenerationContext context, int x, int z) {
        ChunkGenerator generator = context.chunkGenerator();
        return generator.getFirstFreeHeight(
                x,
                z,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );
    }

    static RandomSource chunkRandom(long worldSeed, ChunkPos chunk, Kind kind) {
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setSeed((long) chunk.x * 341873128712L + (long) chunk.z * 132897987541L + worldSeed + kind.seedSalt());
        return random;
    }

    public static List<String> listStructureNames() {
        return Arrays.stream(Kind.values())
                .map(Kind::serializedName)
                .toList();
    }

    public static String selectedNameAt(net.minecraft.server.level.ServerLevel level, ChunkPos chunk, String name) {
        Kind kind = Kind.byName(name);
        if (kind == null || !HbmConfig.legacyDungeonGenerationEnabled(HbmConfig.GENERATE_HBM_STRUCTURES.get())) {
            return null;
        }
        int centerX = chunk.getMiddleBlockX();
        int centerZ = chunk.getMiddleBlockZ();
        Holder<Biome> biome = level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(centerX),
                QuartPos.fromBlock(64),
                QuartPos.fromBlock(centerZ),
                level.getChunkSource().randomState().sampler()
        );
        if (!kind.canSpawnIn(biome)) {
            return null;
        }
        RandomSource random = chunkRandom(level.getSeed(), chunk, kind);
        int rate = kind.spawnRate();
        return rate > 0 && random.nextInt(rate) == 0 ? kind.serializedName() : null;
    }

    private String legacyScatterName() {
        return this.kind.serializedName();
    }

    @Override
    public StructureType<?> type() {
        return HbmWorldgenStructures.HBM_LEGACY_SCATTER.get();
    }

    public enum Kind {
        DESERT_ATOM("desert_atom", "desert_atom", false, 0, 500_001L) {
            @Override
            int spawnRate() {
                return HbmConfig.ATOM_STRUCTURE_SPAWN_RATE.get();
            }

            @Override
            boolean canSpawnIn(Holder<Biome> biome) {
                return isHotDryNoLightning(biome);
            }

            @Override
            boolean validPlacement(WorldGenLevel level, BlockPos origin) {
                return validSurface(level, origin.offset(20, 0, 16), true, true);
            }
        },
        LIBRARY_DUNGEON("library_dungeon", "library_dungeon", true, 0, 500_002L) {
            @Override
            int spawnRate() {
                return HbmConfig.LIBRARY_DUNGEON_SPAWN_RATE.get();
            }

            @Override
            boolean validPlacement(WorldGenLevel level, BlockPos origin) {
                return validLibraryCorner(level, origin)
                        && validLibraryCorner(level, origin.offset(8, 0, 0))
                        && validLibraryCorner(level, origin.offset(8, 0, 10))
                        && validLibraryCorner(level, origin.offset(0, 0, 10));
            }
        },
        SPACESHIP("spaceship", "spaceship", false, 1, 500_003L) {
            @Override
            int spawnRate() {
                return HbmConfig.SPACESHIP_STRUCTURE_SPAWN_RATE.get();
            }

            @Override
            boolean validPlacement(WorldGenLevel level, BlockPos origin) {
                return validSurface(level, origin, false, false)
                        && validSurface(level, origin.offset(12, 0, 0), false, false)
                        && validSurface(level, origin.offset(0, 0, 23), false, false)
                        && validSurface(level, origin.offset(12, 0, 23), false, false);
            }
        },
        WASTE_TANK("waste_tank", "waste_tank", false, 0, 500_004L) {
            @Override
            int spawnRate() {
                return HbmConfig.WASTE_TANK_STRUCTURE_SPAWN_RATE.get();
            }

            @Override
            boolean canSpawnIn(Holder<Biome> biome) {
                return isHotDryNoLightning(biome);
            }

            @Override
            boolean validPlacement(WorldGenLevel level, BlockPos origin) {
                return validSurface(level, origin, true, false)
                        && validSurface(level, origin.offset(4, 0, 0), true, false)
                        && validSurface(level, origin.offset(4, 0, 6), true, false)
                        && validSurface(level, origin.offset(0, 0, 6), true, false);
            }
        },
        JUNGLE_DUNGEON("jungle_dungeon", "jungle_dungeon", false, 0, 500_005L) {
            @Override
            int spawnRate() {
                return HbmConfig.JUNGLE_DUNGEON_SPAWN_RATE.get();
            }

            @Override
            boolean canSpawnIn(Holder<Biome> biome) {
                return biome.is(BiomeTags.IS_JUNGLE);
            }

            @Override
            boolean validPlacement(WorldGenLevel level, BlockPos origin) {
                return true;
            }
        };

        private final String serializedName;
        private final String templateName;
        private final boolean randomY;
        private final int surfaceYOffset;
        private final long seedSalt;

        Kind(String serializedName, String templateName, boolean randomY, int surfaceYOffset, long seedSalt) {
            this.serializedName = serializedName;
            this.templateName = templateName;
            this.randomY = randomY;
            this.surfaceYOffset = surfaceYOffset;
            this.seedSalt = seedSalt;
        }

        public String serializedName() {
            return serializedName;
        }

        public String templateName() {
            return templateName;
        }

        boolean randomY() {
            return randomY;
        }

        boolean surfaceAligned() {
            return !randomY;
        }

        int surfaceYOffset() {
            return surfaceYOffset;
        }

        long seedSalt() {
            return seedSalt;
        }

        boolean canSpawnIn(Holder<Biome> biome) {
            return true;
        }

        abstract int spawnRate();

        abstract boolean validPlacement(WorldGenLevel level, BlockPos origin);

        static Kind require(String name) {
            Kind kind = byName(name);
            if (kind == null) {
                throw new IllegalArgumentException("Unknown legacy HBM scatter structure: " + name);
            }
            return kind;
        }

        public static Kind byName(String name) {
            String normalized = name.toLowerCase(Locale.ROOT);
            for (Kind kind : values()) {
                if (kind.serializedName.equals(normalized)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private static boolean isHotDryNoLightning(Holder<Biome> biome) {
        Biome value = biome.value();
        return !value.hasPrecipitation() && value.getBaseTemperature() >= 1.5F;
    }

    private static boolean validLibraryCorner(WorldGenLevel level, BlockPos pos) {
        return pos.getY() - 1 > 4
                && isSolidMaterial(level, pos.above(8))
                && isSolidMaterial(level, pos.below());
    }

    private static boolean isSolidMaterial(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty()
                && state.isCollisionShapeFullBlock(level, pos);
    }

    private static boolean validSurface(WorldGenLevel level, BlockPos pos, boolean requireAirAbove, boolean atomClayAllowed) {
        if (requireAirAbove && !level.getBlockState(pos).isAir()) {
            return false;
        }

        BlockState check = level.getBlockState(pos.below());
        BlockState below = level.getBlockState(pos.below(2));
        if (validSurfaceBlock(check, atomClayAllowed)) {
            return true;
        }
        if (check.is(Blocks.SNOW) && validSurfaceBlock(below, atomClayAllowed)) {
            return true;
        }
        return isLegacyPlantMaterial(check) && validSurfaceBlock(below, atomClayAllowed);
    }

    private static boolean validSurfaceBlock(BlockState state, boolean atomClayAllowed) {
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.SAND)
                || state.is(Blocks.SANDSTONE) || state.is(Blocks.STONE)) {
            return true;
        }
        return atomClayAllowed && state.is(BlockTags.TERRACOTTA);
    }

    private static boolean isLegacyPlantMaterial(BlockState state) {
        Block block = state.getBlock();
        return block instanceof BushBlock
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH);
    }
}
