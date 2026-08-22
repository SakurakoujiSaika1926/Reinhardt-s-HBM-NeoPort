package com.reinhardt.hbm.worldgen.structure;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmWorldgenStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public final class HbmLegacyNbtStructure extends Structure {
    public static final MapCodec<HbmLegacyNbtStructure> CODEC = Structure.simpleCodec(HbmLegacyNbtStructure::new);

    public HbmLegacyNbtStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!HbmConfig.GENERATE_HBM_STRUCTURES.get()) {
            return Optional.empty();
        }

        ChunkPos chunk = context.chunkPos();
        if (!isLegacyCandidateChunk(context, chunk)) {
            return Optional.empty();
        }

        int centerX = chunk.getMiddleBlockX();
        int centerZ = chunk.getMiddleBlockZ();
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(centerX),
                QuartPos.fromBlock(64),
                QuartPos.fromBlock(centerZ),
                context.randomState().sampler()
        );
        HbmLegacyStructureSelection.SelectedStructure selected = HbmLegacyStructureSelection.select(biome, context.random());
        if (selected == null) {
            return Optional.empty();
        }

        int rotation = context.random().nextInt(4);
        if (selected.procedural()) {
            long seed = context.random().nextLong();
            BlockPos origin = new BlockPos(centerX, 64, centerZ);
            boolean flatBiome = HbmLegacyProceduralPiece.isFlatBiome(biome);
            boolean hotDryBiome = HbmLegacyProceduralPiece.isHotDryBiome(biome);
            boolean badlandsBiome = HbmLegacyProceduralPiece.isBadlandsBiome(biome);
            return Optional.of(new GenerationStub(origin, pieces -> pieces.addPiece(new HbmLegacyProceduralPiece(
                    selected,
                    origin,
                    seed,
                    rotation,
                    flatBiome,
                    hotDryBiome,
                    badlandsBiome,
                    context.heightAccessor().getMinBuildHeight(),
                    context.heightAccessor().getMaxBuildHeight()
            ))));
        }

        HbmLegacyNbtTemplate template = HbmLegacyNbtTemplate.get(selected.templateName());
        int originX = chunk.getMinBlockX();
        int originZ = chunk.getMinBlockZ();
        int y = selected.conformToTerrain()
                ? Math.max(context.heightAccessor().getMinBuildHeight(), selected.minHeight())
                : terrainAlignedY(context, originX, originZ, template.rotatedSizeX(rotation), template.rotatedSizeZ(rotation), selected);
        int minY = selected.conformToTerrain() ? context.heightAccessor().getMinBuildHeight() : y;
        int maxY = selected.conformToTerrain()
                ? context.heightAccessor().getMaxBuildHeight() - 1
                : y + template.sizeY() - 1;
        BlockPos origin = new BlockPos(originX, y, originZ);

        if (HbmLegacyJigsawGenerator.canGenerate(selected)) {
            return Optional.of(new GenerationStub(origin, pieces -> {
                for (HbmLegacyNbtPiece piece : HbmLegacyJigsawGenerator.generate(selected, origin, rotation, context.random())) {
                    pieces.addPiece(piece);
                }
            }));
        }

        return Optional.of(new GenerationStub(origin, pieces -> pieces.addPiece(new HbmLegacyNbtPiece(selected, origin, rotation, minY, maxY))));
    }

    private static boolean isLegacyCandidateChunk(GenerationContext context, ChunkPos chunk) {
        int minChunks = Math.max(0, HbmConfig.HBM_STRUCTURE_MIN_CHUNKS.get());
        int maxChunks = Math.max(1, HbmConfig.HBM_STRUCTURE_MAX_CHUNKS.get());
        if (maxChunks <= minChunks) {
            maxChunks = minChunks + 1;
        }

        int x = chunk.x;
        int z = chunk.z;
        if (x < 0) {
            x -= maxChunks - 1;
        }
        if (z < 0) {
            z -= maxChunks - 1;
        }

        x /= maxChunks;
        z /= maxChunks;
        WorldgenRandom random = context.random();
        random.setSeed((long) x * 341873128712L + (long) z * 132897987541L + context.seed() + 996996996L);
        x *= maxChunks;
        z *= maxChunks;
        int spread = Math.max(1, maxChunks - minChunks);
        x += random.nextInt(spread);
        z += random.nextInt(spread);
        return chunk.x == x && chunk.z == z;
    }

    private static int terrainAlignedY(GenerationContext context, int originX, int originZ, int sizeX, int sizeZ, HbmLegacyStructureSelection.SelectedStructure selected) {
        ChunkGenerator generator = context.chunkGenerator();
        int samples = 0;
        int total = 0;
        for (int x = originX; x < originX + sizeX; x += Math.max(1, sizeX - 1)) {
            for (int z = originZ; z < originZ + sizeZ; z += Math.max(1, sizeZ - 1)) {
                total += generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                samples++;
            }
        }
        int average = samples == 0 ? 64 : total / samples;
        int y = average + selected.heightOffset();
        return Mth.clamp(y, selected.minHeight(), selected.maxHeight());
    }

    @Override
    public StructureType<?> type() {
        return HbmWorldgenStructures.HBM_LEGACY_NBT.get();
    }
}
