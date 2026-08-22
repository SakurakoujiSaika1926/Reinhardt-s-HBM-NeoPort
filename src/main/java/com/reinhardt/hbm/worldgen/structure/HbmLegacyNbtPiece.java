package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.registry.HbmWorldgenStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public final class HbmLegacyNbtPiece extends StructurePiece {
    private final String spawnName;
    private final String templateName;
    private final int rotation;
    private final int heightOffset;
    private final boolean conformToTerrain;
    private final String replacementProfile;

    public HbmLegacyNbtPiece(
            HbmLegacyStructureSelection.SelectedStructure selected,
            BlockPos origin,
            int rotation,
            int boundingMinY,
            int boundingMaxY
    ) {
        super(
                HbmWorldgenStructures.HBM_LEGACY_NBT_PIECE.get(),
                0,
                makeBoundingBox(selected.templateName(), origin, rotation, boundingMinY, boundingMaxY)
        );
        this.spawnName = selected.spawnName();
        this.templateName = selected.templateName();
        this.rotation = rotation & 3;
        this.heightOffset = selected.heightOffset();
        this.conformToTerrain = selected.conformToTerrain();
        this.replacementProfile = "";
        this.setOrientation(null);
    }

    HbmLegacyNbtPiece(
            String spawnName,
            String templateName,
            BlockPos origin,
            int rotation,
            int heightOffset,
            boolean conformToTerrain,
            BoundingBox boundingBox,
            String replacementProfile
    ) {
        super(HbmWorldgenStructures.HBM_LEGACY_NBT_PIECE.get(), 0, boundingBox);
        this.spawnName = spawnName;
        this.templateName = templateName;
        this.rotation = rotation & 3;
        this.heightOffset = heightOffset;
        this.conformToTerrain = conformToTerrain;
        this.replacementProfile = replacementProfile == null ? "" : replacementProfile;
        this.setOrientation(null);
    }

    public HbmLegacyNbtPiece(StructureTemplateManager ignored, CompoundTag tag) {
        super(HbmWorldgenStructures.HBM_LEGACY_NBT_PIECE.get(), tag);
        this.spawnName = tag.getString("SpawnName");
        this.templateName = tag.getString("TemplateName");
        this.rotation = tag.getInt("Rotation") & 3;
        this.heightOffset = tag.getInt("HeightOffset");
        this.conformToTerrain = tag.getBoolean("ConformToTerrain");
        this.replacementProfile = tag.getString("ReplacementProfile");
        this.setOrientation(null);
    }

    private static BoundingBox makeBoundingBox(String templateName, BlockPos origin, int rotation, int boundingMinY, int boundingMaxY) {
        HbmLegacyNbtTemplate template = HbmLegacyNbtTemplate.get(templateName);
        int minY = Math.min(boundingMinY, boundingMaxY);
        int maxY = Math.max(boundingMinY, boundingMaxY);
        return new BoundingBox(
                origin.getX(),
                minY,
                origin.getZ(),
                origin.getX() + template.rotatedSizeX(rotation) - 1,
                maxY,
                origin.getZ() + template.rotatedSizeZ(rotation) - 1
        );
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("SpawnName", this.spawnName);
        tag.putString("TemplateName", this.templateName);
        tag.putInt("Rotation", this.rotation);
        tag.putInt("HeightOffset", this.heightOffset);
        tag.putBoolean("ConformToTerrain", this.conformToTerrain);
        if (!this.replacementProfile.isEmpty()) {
            tag.putString("ReplacementProfile", this.replacementProfile);
        }
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
        HbmLegacyNbtTemplate.BlockReplacement replacement = HbmLegacyJigsawGenerator.replacement(this.replacementProfile);
        HbmLegacyNbtTemplate.get(this.templateName).place(
                level,
                this.boundingBox,
                box,
                this.rotation,
                this.conformToTerrain,
                this.heightOffset,
                random,
                replacement
        );
    }
}
