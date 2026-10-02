package com.reinhardt.hbm.mixin.tom;

import com.reinhardt.hbm.world.TomImpactWorldgen;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scopes the old post-impact village palette replacement to village pieces. */
@Mixin(StructureStart.class)
public abstract class StructureStartMixin {
    @Inject(method = "placeInChunk", at = @At("HEAD"))
    private void reinhardtshbm$beginTomVillage(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            CallbackInfo callbackInfo
    ) {
        TomImpactWorldgen.beginStructure(
                level, ((StructureStart) (Object) this).getStructure(), random);
    }

    @Inject(method = "placeInChunk", at = @At("RETURN"))
    private void reinhardtshbm$endTomVillage(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            CallbackInfo callbackInfo
    ) {
        TomImpactWorldgen.endStructure();
    }
}
