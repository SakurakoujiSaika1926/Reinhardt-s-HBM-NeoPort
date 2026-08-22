package com.reinhardt.hbm.worldgen.structure;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmWorldgenStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** The 1.7.10 ancient tomb, kept separate from the later weighted structure selector. */
public final class AncientTombStructure extends Structure {
    public static final MapCodec<AncientTombStructure> CODEC = Structure.simpleCodec(AncientTombStructure::new);

    public AncientTombStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!HbmConfig.GENERATE_HBM_STRUCTURES.get()) {
            return Optional.empty();
        }

        int chance = HbmConfig.ANCIENT_TOMB_SPAWN_CHANCE.get();
        if (chance <= 0) {
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
        Biome value = biome.value();
        if (value.getBaseTemperature() < 2.0F || value.getModifiedClimateSettings().downfall() > 0.0F) {
            return Optional.empty();
        }
        if (context.random().nextInt(chance) != 0) {
            return Optional.empty();
        }

        int surface = context.chunkGenerator().getFirstOccupiedHeight(
                centerX,
                centerZ,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );
        int pyramidBase = Math.max(surface, 35) - 5;
        long seed = context.random().nextLong();
        BlockPos origin = new BlockPos(centerX, pyramidBase, centerZ);
        return Optional.of(new GenerationStub(origin, pieces -> pieces.addPiece(new AncientTombPiece(
                origin,
                seed,
                context.heightAccessor().getMinBuildHeight(),
                context.heightAccessor().getMaxBuildHeight()
        ))));
    }

    @Override
    public StructureType<?> type() {
        return HbmWorldgenStructures.ANCIENT_TOMB.get();
    }
}
