package com.reinhardt.hbm.worldgen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.MeteorOreBlock;
import com.reinhardt.hbm.item.MeteorOreBlockItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class MeteoriteGenerator {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private MeteoriteGenerator() {
    }

    public static void generate(LevelAccessor level, RandomSource random, BlockPos center, boolean safe, boolean allowSpecials, boolean damagingImpact) {
        if (damagingImpact && level instanceof net.minecraft.world.level.Level realLevel) {
            AABB box = new AABB(center).inflate(7.5D);
            for (Entity entity : realLevel.getEntities(null, box)) {
                entity.hurt(realLevel.damageSources().source(HbmDamageTypes.METEORITE), 1000.0F);
            }
        }

        if (allowSpecials && com.reinhardt.hbm.config.HbmConfig.ENABLE_SPECIAL_METEORS.get()) {
            switch (random.nextInt(300)) {
                case 0 -> {
                    generateBox(level, random, center, list(meteor()));
                    return;
                }
                case 1 -> {
                    List<BlockState> blocks = randomOre();
                    int size = blocks.size();
                    for (int i = 0; i < size; i++) {
                        blocks.add(broken());
                    }
                    generateSphere7x7(level, random, center, blocks, safe);
                    return;
                }
                case 2 -> {
                    List<BlockState> blocks = randomOre();
                    int size = blocks.size() / 2;
                    for (int i = 0; i < size; i++) {
                        blocks.add(broken());
                    }
                    generateSphere5x5(level, random, center, blocks, safe);
                    return;
                }
                case 3 -> {
                    generateBox(level, random, center, randomOre(), safe);
                    return;
                }
                case 4 -> {
                    if (level instanceof net.minecraft.world.level.Level realLevel) {
                        realLevel.explode(null, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D, 15.0F, !safe, safe
                                ? net.minecraft.world.level.Level.ExplosionInteraction.NONE
                                : net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
                    }
                    return;
                }
                case 5 -> {
                    generateSphere7x7(level, random, center, list(treasure(), broken()), safe);
                    return;
                }
                case 6 -> {
                    generateSphere5x5(level, random, center, list(treasure(), treasure(), broken()), safe);
                    return;
                }
                case 7 -> {
                    generateBox(level, random, center, list(treasure()), safe);
                    return;
                }
                case 8 -> {
                    generateSphere7x7(level, random, center, list(treasure()), safe);
                    generateSphere5x5(level, random, center, list(HbmBlocks.TOXIC_BLOCK.get().defaultBlockState()), safe);
                    return;
                }
                case 9 -> {
                    generateSphere9x9(level, random, center, list(broken()), safe);
                    generateSphere7x7(level, random, center, randomOre(), safe);
                    return;
                }
                case 10 -> {
                    generateSphere5x5(level, random, center, list(broken()), safe);
                    setBlock(level, center, state(ReinhardtsHBM.id("taint")), safe);
                    return;
                }
                case 12 -> {
                    if (level instanceof net.minecraft.world.level.Level realLevel) {
                        realLevel.explode(null, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D, 10.0F, !safe, safe
                                ? net.minecraft.world.level.Level.ExplosionInteraction.NONE
                                : net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
                    }
                    return;
                }
                default -> {
                }
            }
        }

        switch (random.nextInt(3)) {
            case 0 -> generateLarge(level, random, center, safe);
            case 1 -> generateMedium(level, random, center, safe);
            default -> generateSmall(level, random, center, safe);
        }
    }

    private static void generateLarge(LevelAccessor level, RandomSource random, BlockPos center, boolean safe) {
        int hull = random.nextInt(4);
        int outerPadding = hull == 2 ? 1 + random.nextInt(2) : hull == 3 ? 2 : 0;
        int innerPadding = random.nextInt(hull == 0 ? 3 : 2);
        int core = innerPadding > 0 ? 2 : random.nextInt(2);

        List<BlockState> hullL = hullList(hull);
        List<BlockState> opL = paddingList(outerPadding);
        List<BlockState> ipL = innerPaddingList(innerPadding);
        List<BlockState> coreL = coreList(core);

        switch (random.nextInt(5)) {
            case 0 -> {
                generateSphere7x7(level, random, center, hullL, safe);
                generateStar5x5(level, random, center, opL, safe);
                generateStar3x3(level, random, center, ipL, safe);
            }
            case 1 -> {
                generateSphere7x7(level, random, center, hullL, safe);
                generateSphere5x5(level, random, center, opL, safe);
                generateStar3x3(level, random, center, ipL, safe);
            }
            case 2 -> {
                generateSphere7x7(level, random, center, hullL, safe);
                generateSphere5x5(level, random, center, opL, safe);
                generateBox(level, random, center, ipL, safe);
            }
            case 3 -> {
                generateSphere7x7(level, random, center, hullL, safe);
                generateSphere5x5(level, random, center, opL, safe);
                generateBox(level, random, center, ipL, safe);
                generateStar3x3(level, random, center, randomOre(), safe);
            }
            default -> {
                generateSphere7x7(level, random, center, hullL, safe);
                generateSphere5x5(level, random, center, opL, safe);
                generateStar5x5(level, random, center, ipL, safe);
                generateStar3x3(level, random, center, randomOre(), safe);
            }
        }
        setBlock(level, center, pick(coreL, random), safe);
    }

    private static void generateMedium(LevelAccessor level, RandomSource random, BlockPos center, boolean safe) {
        int hull = random.nextInt(4);
        int outerPadding = hull == 2 ? 1 + random.nextInt(2) : hull == 3 ? 2 : 0;
        int innerPadding = random.nextInt(hull == 0 ? 3 : 2);
        int core = innerPadding > 0 ? 2 : random.nextInt(2);

        List<BlockState> hullL = hullList(hull);
        List<BlockState> opL = paddingList(outerPadding);
        List<BlockState> ipL = innerPaddingList(innerPadding);
        List<BlockState> coreL = coreList(core);
        List<BlockState> sCore = smallCoreList(core);

        int shape = random.nextInt(6);
        switch (shape) {
            case 0 -> generateSphere5x5(level, random, center, hullL, safe);
            case 1 -> {
                generateSphere5x5(level, random, center, hullL, safe);
                generateStar3x3(level, random, center, opL, safe);
            }
            case 2 -> {
                generateSphere5x5(level, random, center, hullL, safe);
                generateBox(level, random, center, opL, safe);
            }
            case 3 -> {
                generateSphere5x5(level, random, center, hullL, safe);
                generateBox(level, random, center, opL, safe);
                generateStar3x3(level, random, center, ipL, safe);
            }
            case 4 -> {
                generateSphere5x5(level, random, center, hullL, safe);
                generateBox(level, random, center, ipL, safe);
            }
            default -> {
                generateSphere5x5(level, random, center, hullL, safe);
                generateBox(level, random, center, ipL, safe);
                generateStar3x3(level, random, center, randomOre(), safe);
            }
        }
        setBlock(level, center, pick(shape == 0 ? sCore : coreL, random), safe);
    }

    private static void generateSmall(LevelAccessor level, RandomSource random, BlockPos center, boolean safe) {
        int hull = random.nextInt(4);
        int core = random.nextInt(3);
        generateBox(level, random, center, hullList(hull), safe);
        setBlock(level, center, pick(smallCoreList(core), random), safe);
    }

    private static List<BlockState> hullList(int hull) {
        return switch (hull) {
            case 0 -> list(molten());
            case 1 -> list(cobble());
            case 2 -> weightedTreasureBroken();
            default -> list(molten(), broken());
        };
    }

    private static List<BlockState> paddingList(int padding) {
        return switch (padding) {
            case 1 -> weightedTreasureBroken();
            case 2 -> list(cobble(), broken());
            default -> list(cobble());
        };
    }

    private static List<BlockState> innerPaddingList(int padding) {
        return switch (padding) {
            case 1 -> list(broken());
            case 2 -> list(cobble());
            default -> weightedTreasureBroken();
        };
    }

    private static List<BlockState> coreList(int core) {
        return switch (core) {
            case 1 -> list(treasure());
            case 2 -> randomOre();
            default -> list(meteor());
        };
    }

    private static List<BlockState> smallCoreList(int core) {
        return switch (core) {
            case 1 -> list(treasure());
            case 2 -> list(treasure(), meteor());
            default -> list(meteor());
        };
    }

    private static List<BlockState> weightedTreasureBroken() {
        List<BlockState> blocks = new ArrayList<>();
        for (int i = 0; i < 99; i++) {
            blocks.add(broken());
        }
        blocks.add(treasure());
        return blocks;
    }

    private static List<BlockState> randomOre() {
        List<BlockState> ores = new ArrayList<>();
        for (MeteorOreBlockItem.Type type : MeteorOreBlockItem.Type.values()) {
            ores.add(HbmBlocks.ORE_METEOR.get().defaultBlockState().setValue(MeteorOreBlock.VARIANT, type.ordinal()));
        }
        return ores;
    }

    private static void generateSphere7x7(LevelAccessor level, RandomSource random, BlockPos center, List<BlockState> set, boolean safe) {
        box(level, random, center, -3, -1, -1, 3, 1, 1, set, safe);
        box(level, random, center, -1, -3, -1, 1, 3, 1, set, safe);
        box(level, random, center, -1, -1, -3, 1, 1, 3, set, safe);
        box(level, random, center, -2, -2, -1, 2, 2, 1, set, safe);
        box(level, random, center, -1, -2, -2, 1, 2, 2, set, safe);
        box(level, random, center, -2, -1, -2, 2, 1, 2, set, safe);
    }

    private static void generateSphere5x5(LevelAccessor level, RandomSource random, BlockPos center, List<BlockState> set, boolean safe) {
        box(level, random, center, -2, -1, -1, 2, 1, 1, set, safe);
        box(level, random, center, -1, -2, -1, 1, 2, 1, set, safe);
        box(level, random, center, -1, -1, -2, 1, 1, 2, set, safe);
    }

    private static void generateSphere9x9(LevelAccessor level, RandomSource random, BlockPos center, List<BlockState> set, boolean safe) {
        box(level, random, center, -4, -1, -1, 4, 1, 1, set, safe);
        box(level, random, center, -1, -4, -1, 1, 4, 1, set, safe);
        box(level, random, center, -1, -1, -4, 1, 1, 4, set, safe);
        box(level, random, center, -1, -3, -3, 1, 3, 3, set, safe);
        box(level, random, center, -3, -1, -3, 3, 1, 3, set, safe);
        box(level, random, center, -3, -3, -1, 3, 3, 1, set, safe);
        box(level, random, center, -3, -2, -2, 3, 2, 2, set, safe);
        box(level, random, center, -2, -3, -2, 2, 3, 2, set, safe);
        box(level, random, center, -2, -2, -3, 2, 2, 3, set, safe);
    }

    private static void generateBox(LevelAccessor level, RandomSource random, BlockPos center, List<BlockState> set) {
        generateBox(level, random, center, set, false);
    }

    private static void generateBox(LevelAccessor level, RandomSource random, BlockPos center, List<BlockState> set, boolean safe) {
        box(level, random, center, -1, -1, -1, 1, 1, 1, set, safe);
    }

    private static void generateStar5x5(LevelAccessor level, RandomSource random, BlockPos center, List<BlockState> set, boolean safe) {
        generateBox(level, random, center, set, safe);
        setBlock(level, center.offset(2, 0, 0), pick(set, random), safe);
        setBlock(level, center.offset(-2, 0, 0), pick(set, random), safe);
        setBlock(level, center.offset(0, 2, 0), pick(set, random), safe);
        setBlock(level, center.offset(0, -2, 0), pick(set, random), safe);
        setBlock(level, center.offset(0, 0, 2), pick(set, random), safe);
        setBlock(level, center.offset(0, 0, -2), pick(set, random), safe);
    }

    private static void generateStar3x3(LevelAccessor level, RandomSource random, BlockPos center, List<BlockState> set, boolean safe) {
        setBlock(level, center, pick(set, random), safe);
        setBlock(level, center.offset(1, 0, 0), pick(set, random), safe);
        setBlock(level, center.offset(-1, 0, 0), pick(set, random), safe);
        setBlock(level, center.offset(0, 1, 0), pick(set, random), safe);
        setBlock(level, center.offset(0, -1, 0), pick(set, random), safe);
        setBlock(level, center.offset(0, 0, 1), pick(set, random), safe);
        setBlock(level, center.offset(0, 0, -1), pick(set, random), safe);
    }

    private static void box(LevelAccessor level, RandomSource random, BlockPos center, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, List<BlockState> set, boolean safe) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    setBlock(level, pos, pick(set, random), safe);
                }
            }
        }
    }

    private static void setBlock(LevelAccessor level, BlockPos pos, BlockState state, boolean safe) {
        if (state.isAir() || pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return;
        }
        BlockState target = level.getBlockState(pos);
        if (safe && !target.canBeReplaced() && !isMeteorReplaceable(target)) {
            return;
        }
        float hardness = target.getDestroySpeed(level, pos);
        if (hardness != -1.0F && hardness < 10_000.0F) {
            level.setBlock(pos, state, SET_FLAGS);
        }
    }

    private static boolean isMeteorReplaceable(BlockState state) {
        Block block = state.getBlock();
        return METEOR_REPLACEABLES.contains(block);
    }

    private static BlockState pick(List<BlockState> set, RandomSource random) {
        return set.get(random.nextInt(set.size()));
    }

    private static BlockState state(ResourceLocation id) {
        Block block = BuiltInRegistries.BLOCK.get(id);
        return block == Blocks.AIR ? Blocks.AIR.defaultBlockState() : block.defaultBlockState();
    }

    @SafeVarargs
    private static List<BlockState> list(BlockState... states) {
        return new ArrayList<>(List.of(states));
    }

    private static BlockState meteor() {
        return HbmBlocks.BLOCK_METEOR.get().defaultBlockState();
    }

    private static BlockState broken() {
        return HbmBlocks.BLOCK_METEOR_BROKEN.get().defaultBlockState();
    }

    private static BlockState cobble() {
        return HbmBlocks.BLOCK_METEOR_COBBLE.get().defaultBlockState();
    }

    private static BlockState molten() {
        return HbmBlocks.BLOCK_METEOR_MOLTEN.get().defaultBlockState();
    }

    private static BlockState treasure() {
        return HbmBlocks.BLOCK_METEOR_TREASURE.get().defaultBlockState();
    }

    private static final Set<Block> METEOR_REPLACEABLES = Set.of(
            HbmBlocks.BLOCK_METEOR.get(),
            HbmBlocks.BLOCK_METEOR_BROKEN.get(),
            HbmBlocks.BLOCK_METEOR_COBBLE.get(),
            HbmBlocks.BLOCK_METEOR_MOLTEN.get(),
            HbmBlocks.BLOCK_METEOR_TREASURE.get(),
            HbmBlocks.ORE_METEOR.get()
    );
}
