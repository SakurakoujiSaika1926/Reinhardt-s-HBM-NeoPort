package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.registry.HbmWorldgenStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class HbmLegacyScatterPiece extends StructurePiece {
    private final String kindName;
    private final BlockPos origin;
    private final long seed;

    public HbmLegacyScatterPiece(HbmLegacyScatterStructure.Kind kind, BlockPos origin, long seed, int minBuildHeight, int maxBuildHeight) {
        super(HbmWorldgenStructures.HBM_LEGACY_SCATTER_PIECE.get(), 0, makeBoundingBox(kind, origin, minBuildHeight, maxBuildHeight));
        this.kindName = kind.serializedName();
        this.origin = origin;
        this.seed = seed;
        this.setOrientation(null);
    }

    public HbmLegacyScatterPiece(StructureTemplateManager ignored, CompoundTag tag) {
        super(HbmWorldgenStructures.HBM_LEGACY_SCATTER_PIECE.get(), tag);
        this.kindName = tag.getString("Kind");
        this.origin = new BlockPos(tag.getInt("OriginX"), tag.getInt("OriginY"), tag.getInt("OriginZ"));
        this.seed = tag.getLong("Seed");
        this.setOrientation(null);
    }

    private static BoundingBox makeBoundingBox(HbmLegacyScatterStructure.Kind kind, BlockPos origin, int minBuildHeight, int maxBuildHeight) {
        if (kind == HbmLegacyScatterStructure.Kind.JUNGLE_DUNGEON) {
            return HbmLegacyJungleDungeonGenerator.boundingBox(origin, minBuildHeight, maxBuildHeight);
        }
        HbmLegacyScatterTemplate template = HbmLegacyScatterTemplate.get(kind.templateName());
        return new BoundingBox(
                origin.getX() + template.minX(),
                minBuildHeight,
                origin.getZ() + template.minZ(),
                origin.getX() + template.maxX(),
                maxBuildHeight - 1,
                origin.getZ() + template.maxZ()
        );
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Kind", this.kindName);
        tag.putInt("OriginX", this.origin.getX());
        tag.putInt("OriginY", this.origin.getY());
        tag.putInt("OriginZ", this.origin.getZ());
        tag.putLong("Seed", this.seed);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pos
    ) {
        HbmLegacyScatterStructure.Kind kind = HbmLegacyScatterStructure.Kind.byName(this.kindName);
        if (kind == null) {
            throw new IllegalStateException("Unknown legacy scatter structure kind: " + this.kindName);
        }

        BlockPos validationOrigin = validationOrigin(level, kind, this.origin);
        if (!kind.validPlacement(level, validationOrigin)) {
            return;
        }
        BlockPos placementOrigin = validationOrigin.offset(0, kind.surfaceYOffset(), 0);
        if (kind == HbmLegacyScatterStructure.Kind.JUNGLE_DUNGEON) {
            HbmLegacyJungleDungeonGenerator.generate(level, box, this.seed, validationOrigin);
            return;
        }
        HbmLegacyScatterTemplate.get(kind.templateName()).place(level, placementOrigin, box, this.seed);
    }

    private static BlockPos validationOrigin(WorldGenLevel level, HbmLegacyScatterStructure.Kind kind, BlockPos startOrigin) {
        if (!kind.surfaceAligned()) {
            return startOrigin;
        }
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, startOrigin.getX(), startOrigin.getZ());
        return new BlockPos(startOrigin.getX(), y, startOrigin.getZ());
    }
}
