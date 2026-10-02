package com.reinhardt.hbm.world;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayDeque;
import java.util.Deque;

/** Exact generation-time hooks used by the 1.7.10 TOM aftermath. */
public final class TomImpactWorldgen {
    private static final TagKey<PlacedFeature> ACTIVE_SUPPRESSED = TagKey.create(
            Registries.PLACED_FEATURE, ReinhardtsHBM.id("tom_active_suppressed"));
    private static final TagKey<PlacedFeature> SETTLED_SPARSE = TagKey.create(
            Registries.PLACED_FEATURE, ReinhardtsHBM.id("tom_settled_sparse"));

    private static final ThreadLocal<Deque<VillageContext>> VILLAGE_CONTEXT =
            ThreadLocal.withInitial(ArrayDeque::new);

    private TomImpactWorldgen() {
    }

    /**
     * Mirrors ModEventHandlerImpact#postImpactDecoration.  The caller applies
     * the 1/9 roll to every final placement position, not once per feature.
     */
    public static DecorationPolicy decorationPolicy(PlacementContext context) {
        if (context.topFeature().isEmpty()) {
            return DecorationPolicy.ALLOW;
        }
        WorldGenLevel worldGenLevel = context.getLevel();
        TomImpactSavedData data = TomImpactSavedData.cached(worldGenLevel.getLevel());
        if (data == null || !data.impact()) {
            return DecorationPolicy.ALLOW;
        }

        Registry<PlacedFeature> registry = worldGenLevel.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        PlacedFeature feature = context.topFeature().orElseThrow();
        if (data.dust() > 0.0F || data.fire() > 0.0F) {
            return registry.wrapAsHolder(feature).is(ACTIVE_SUPPRESSED)
                    ? DecorationPolicy.DENY
                    : DecorationPolicy.ALLOW;
        }
        return registry.wrapAsHolder(feature).is(SETTLED_SPARSE)
                ? DecorationPolicy.ONE_IN_NINE
                : DecorationPolicy.ALLOW;
    }

    /** Opens a scoped village placement context for StructureTemplate. */
    public static void beginStructure(WorldGenLevel level, Structure structure, RandomSource random) {
        TomImpactSavedData data = TomImpactSavedData.cached(level.getLevel());
        boolean affected = false;
        if (data != null && data.impact()) {
            Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            affected = registry.wrapAsHolder(structure).is(StructureTags.VILLAGE);
        }
        VILLAGE_CONTEXT.get().push(new VillageContext(affected, random));
    }

    public static void endStructure() {
        Deque<VillageContext> stack = VILLAGE_CONTEXT.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            VILLAGE_CONTEXT.remove();
        }
    }

    /** Mirrors BiomeEvent.GetVillageBlockID from the latest 1.7.10 source. */
    public static StructureTemplate.StructureBlockInfo transformVillageBlock(
            StructureTemplate.StructureBlockInfo info
    ) {
        Deque<VillageContext> stack = VILLAGE_CONTEXT.get();
        if (stack.isEmpty() || !stack.peek().affected()) {
            return info;
        }

        BlockState state = info.state();
        BlockState replacement = state;
        if (removedVillageMaterial(state)) {
            replacement = Blocks.AIR.defaultBlockState();
        } else if (state.is(Blocks.COBBLESTONE) || state.is(BlockTags.STONE_BRICKS)) {
            if (stack.peek().random().nextInt(3) == 1) {
                replacement = Blocks.GRAVEL.defaultBlockState();
            }
        } else if (isSandstone(state)) {
            if (stack.peek().random().nextInt(3) == 1) {
                replacement = Blocks.SAND.defaultBlockState();
            }
        } else if (state.is(Blocks.FARMLAND)) {
            replacement = Blocks.DIRT.defaultBlockState();
        }

        if (replacement == state) {
            return info;
        }
        return new StructureTemplate.StructureBlockInfo(
                info.pos(), replacement, replacement.isAir() ? null : info.nbt());
    }

    private static boolean removedVillageMaterial(BlockState state) {
        SoundType sound = state.getSoundType();
        return isWood(sound)
                || sound == SoundType.GLASS
                || sound == SoundType.WOOL
                || state.getBlock() instanceof LadderBlock
                || state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof ChestBlock
                || state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof BedBlock
                || state.getBlock() instanceof SlabBlock
                || state.getFluidState().is(FluidTags.WATER);
    }

    private static boolean isWood(SoundType sound) {
        return sound == SoundType.WOOD
                || sound == SoundType.CHERRY_WOOD
                || sound == SoundType.BAMBOO_WOOD
                || sound == SoundType.NETHER_WOOD
                || sound == SoundType.HANGING_SIGN
                || sound == SoundType.CHERRY_WOOD_HANGING_SIGN
                || sound == SoundType.BAMBOO_WOOD_HANGING_SIGN
                || sound == SoundType.NETHER_WOOD_HANGING_SIGN
                || sound == SoundType.CHISELED_BOOKSHELF;
    }

    private static boolean isSandstone(BlockState state) {
        return state.is(Blocks.SANDSTONE)
                || state.is(Blocks.CHISELED_SANDSTONE)
                || state.is(Blocks.CUT_SANDSTONE)
                || state.is(Blocks.SMOOTH_SANDSTONE);
    }

    public enum DecorationPolicy {
        ALLOW,
        DENY,
        ONE_IN_NINE
    }

    private record VillageContext(boolean affected, RandomSource random) {
    }
}
