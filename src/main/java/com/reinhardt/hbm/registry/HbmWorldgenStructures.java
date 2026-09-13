package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.worldgen.structure.HbmLegacyNbtPiece;
import com.reinhardt.hbm.worldgen.structure.HbmLegacyNbtStructure;
import com.reinhardt.hbm.worldgen.structure.HbmLegacyProceduralPiece;
import com.reinhardt.hbm.worldgen.structure.HbmLegacyScatterPiece;
import com.reinhardt.hbm.worldgen.structure.HbmLegacyScatterStructure;
import com.reinhardt.hbm.worldgen.structure.AncientTombPiece;
import com.reinhardt.hbm.worldgen.structure.AncientTombStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HbmWorldgenStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, ReinhardtsHBM.MOD_ID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, ReinhardtsHBM.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<HbmLegacyNbtStructure>> HBM_LEGACY_NBT =
            STRUCTURE_TYPES.register("hbm_legacy_nbt", () -> () -> HbmLegacyNbtStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<HbmLegacyScatterStructure>> HBM_LEGACY_SCATTER =
            STRUCTURE_TYPES.register("hbm_legacy_scatter", () -> () -> HbmLegacyScatterStructure.CODEC);

    public static final DeferredHolder<StructureType<?>, StructureType<AncientTombStructure>> ANCIENT_TOMB =
            STRUCTURE_TYPES.register("ancient_tomb", () -> () -> AncientTombStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> HBM_LEGACY_NBT_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "hbm_legacy_nbt_piece",
                    () -> (context, tag) -> new HbmLegacyNbtPiece(context.structureTemplateManager(), tag)
            );

    public static final DeferredHolder<StructurePieceType, StructurePieceType> HBM_LEGACY_SCATTER_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "hbm_legacy_scatter_piece",
                    () -> (context, tag) -> new HbmLegacyScatterPiece(context.structureTemplateManager(), tag)
            );

    public static final DeferredHolder<StructurePieceType, StructurePieceType> ANCIENT_TOMB_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "ancient_tomb_piece",
                    () -> (context, tag) -> new AncientTombPiece(tag)
            );

    public static final DeferredHolder<StructurePieceType, StructurePieceType> HBM_LEGACY_PROCEDURAL_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "hbm_legacy_procedural_piece",
                    () -> (context, tag) -> new HbmLegacyProceduralPiece(tag)
            );

    private HbmWorldgenStructures() {
    }

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
        STRUCTURE_PIECE_TYPES.register(eventBus);
    }
}
