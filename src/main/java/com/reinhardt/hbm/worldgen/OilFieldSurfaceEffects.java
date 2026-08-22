package com.reinhardt.hbm.worldgen;

import com.reinhardt.hbm.block.DeadPlantBlock;
import com.reinhardt.hbm.block.MustardWillowFlowerBlock;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class OilFieldSurfaceEffects {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private OilFieldSurfaceEffects() {
    }

    public static void addOilDepositSurfaceSpot(WorldGenLevel level, RandomSource random, int centerX, int centerZ) {
        int spotCount = 150;
        int spotWidth = 7;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < spotCount; i++) {
            int offX = (int) (random.nextGaussian() * spotWidth);
            int offZ = (int) (random.nextGaussian() * spotWidth);
            int absX = centerX + offX;
            int absZ = centerZ + offZ;
            int startY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, absX, absZ);

            for (int y = startY; y >= level.getMinBuildHeight(); y--) {
                pos.set(absX, y, absZ);
                BlockState state = level.getBlockState(pos);

                if (isFullCube(level, pos, state)) {
                    for (int oy = 1; oy > -3; oy--) {
                        BlockPos subPos = pos.offset(0, oy, 0);
                        BlockState subState = level.getBlockState(subPos);
                        int distSq = offX * offX + offZ * offZ;
                        boolean inner = distSq < (spotWidth / 2) * (spotWidth / 2);

                        if (isGrassOrDirt(subState)) {
                            level.setBlock(subPos, inner ? HbmBlocks.DIRT_OILY.get().defaultBlockState() : HbmBlocks.DIRT_DEAD.get().defaultBlockState(), SET_FLAGS);

                            if (!inner && oy == 0 && random.nextInt(20) == 0) {
                                placeDeadPlant(level, random, subPos.above());
                            }
                            break;
                        } else if (isSandOrOilSand(subState)) {
                            level.setBlock(subPos, dirtySandFor(subState), SET_FLAGS);
                            break;
                        } else if (isStone(subState)) {
                            level.setBlock(subPos, HbmBlocks.STONE_CRACKED.get().defaultBlockState(), SET_FLAGS);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        addOilSeep(level, centerX, centerZ);
    }

    public static void generateBedrockOilSpot(WorldGenLevel level, RandomSource random, int centerX, int centerZ, int width, int count, boolean addRichPlants) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < count; i++) {
            int x = centerX + (int) (random.nextGaussian() * width);
            int z = centerZ + (int) (random.nextGaussian() * width);
            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

            for (int y = surfaceY; y > surfaceY - 4 && y > level.getMinBuildHeight(); y--) {
                pos.set(x, y - 1, z);
                BlockState belowState = level.getBlockState(pos);

                pos.set(x, y, z);
                BlockState groundState = level.getBlockState(pos);

                if (isOilImmunePlant(groundState)) {
                    continue;
                } else if (isFullCube(level, pos.set(x, y - 1, z), belowState)) {
                    pos.set(x, y, z);
                    contaminatePlant(level, random, pos, groundState);
                }

                if (isGrassOrDirt(groundState)) {
                    pos.set(x, y, z);
                    BlockState dirtState = random.nextInt(10) == 0 ? HbmBlocks.DIRT_OILY.get().defaultBlockState() : HbmBlocks.DIRT_DEAD.get().defaultBlockState();
                    level.setBlock(pos, dirtState, SET_FLAGS);

                    if (addRichPlants && random.nextInt(50) == 0) {
                        placeMustardWillow(level, pos.above());
                    }
                    break;
                } else if (isSandOrOilSand(groundState)) {
                    pos.set(x, y, z);
                    level.setBlock(pos, dirtySandFor(groundState), SET_FLAGS);
                    break;
                } else if (isStone(groundState)) {
                    pos.set(x, y, z);
                    level.setBlock(pos, HbmBlocks.STONE_CRACKED.get().defaultBlockState(), SET_FLAGS);
                    break;
                } else if (groundState.is(BlockTags.LEAVES)) {
                    pos.set(x, y, z);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), SET_FLAGS);
                    break;
                }
            }
        }
    }

    public static void generateRuntimeOilSpot(Level level, RandomSource random, int centerX, int centerZ, int width, int count, boolean addRichPlants) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < count; i++) {
            int x = centerX + (int) (random.nextGaussian() * width);
            int z = centerZ + (int) (random.nextGaussian() * width);
            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

            for (int y = surfaceY; y > surfaceY - 4 && y > level.getMinBuildHeight(); y--) {
                pos.set(x, y - 1, z);
                BlockState belowState = level.getBlockState(pos);

                pos.set(x, y, z);
                BlockState groundState = level.getBlockState(pos);

                if (isOilImmunePlant(groundState)) {
                    continue;
                } else if (isFullCube(level, pos.set(x, y - 1, z), belowState)) {
                    pos.set(x, y, z);
                    contaminatePlant(level, random, pos, groundState);
                }

                if (isGrassOrDirt(groundState)) {
                    pos.set(x, y, z);
                    BlockState dirtState = random.nextInt(10) == 0 ? HbmBlocks.DIRT_OILY.get().defaultBlockState() : HbmBlocks.DIRT_DEAD.get().defaultBlockState();
                    level.setBlock(pos, dirtState, SET_FLAGS);

                    if (addRichPlants && random.nextInt(50) == 0) {
                        placeMustardWillow(level, pos.above());
                    }
                    break;
                } else if (isSandOrOilSand(groundState)) {
                    pos.set(x, y, z);
                    level.setBlock(pos, dirtySandFor(groundState), SET_FLAGS);
                    break;
                } else if (isStone(groundState)) {
                    pos.set(x, y, z);
                    level.setBlock(pos, HbmBlocks.STONE_CRACKED.get().defaultBlockState(), SET_FLAGS);
                    break;
                } else if (groundState.is(BlockTags.LEAVES)) {
                    pos.set(x, y, z);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), SET_FLAGS);
                    break;
                }
            }
        }
    }

    private static void addOilSeep(WorldGenLevel level, int centerX, int centerZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 1; i < 6; i++) {
            Direction facing = Direction.from3DDataValue(i);
            int x = centerX + facing.getStepX();
            int z = centerZ + facing.getStepZ();
            int solids = 0;
            int startY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

            for (int y = startY; y >= level.getMinBuildHeight(); y--) {
                pos.set(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    continue;
                }
                if (!state.getFluidState().isEmpty()) {
                    break;
                }

                if (isFullCube(level, pos, state)) {
                    solids++;

                    if (i > 1) {
                        if (isStoneLikeForCracking(state)) {
                            level.setBlock(pos, HbmBlocks.STONE_CRACKED.get().defaultBlockState(), SET_FLAGS);
                        }
                        if (solids >= 4) {
                            break;
                        }
                    } else {
                        if (solids < 3) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), SET_FLAGS);
                        }
                        if (solids == 3) {
                            level.setBlock(pos, HbmBlocks.OIL_SPILL.get().defaultBlockState(), SET_FLAGS);
                        }
                        if (solids > 3 && solids < 7 && isStoneLikeForCracking(state)) {
                            level.setBlock(pos, HbmBlocks.STONE_CRACKED.get().defaultBlockState(), SET_FLAGS);
                        }
                        if (solids == 7) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void contaminatePlant(WorldGenLevel level, RandomSource random, BlockPos.MutableBlockPos pos, BlockState state) {
        if (!isPlant(state)) {
            return;
        }
        if (random.nextInt(10) == 0) {
            DeadPlantBlock.Kind kind = deadPlantKind(state);
            BlockState deadPlant = HbmBlocks.PLANT_DEAD.get().defaultBlockState().setValue(DeadPlantBlock.KIND, kind);
            if (deadPlant.canSurvive(level, pos)) {
                level.setBlock(pos, deadPlant, SET_FLAGS);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), SET_FLAGS);
            }
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), SET_FLAGS);
        }
    }

    private static void contaminatePlant(Level level, RandomSource random, BlockPos.MutableBlockPos pos, BlockState state) {
        if (!isPlant(state)) {
            return;
        }
        if (random.nextInt(10) == 0) {
            DeadPlantBlock.Kind kind = deadPlantKind(state);
            BlockState deadPlant = HbmBlocks.PLANT_DEAD.get().defaultBlockState().setValue(DeadPlantBlock.KIND, kind);
            if (deadPlant.canSurvive(level, pos)) {
                level.setBlock(pos, deadPlant, SET_FLAGS);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), SET_FLAGS);
            }
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), SET_FLAGS);
        }
    }

    private static void placeDeadPlant(WorldGenLevel level, RandomSource random, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) {
            return;
        }
        DeadPlantBlock.Kind kind = random.nextBoolean() ? DeadPlantBlock.Kind.GRASS : DeadPlantBlock.Kind.GENERIC;
        BlockState deadPlant = HbmBlocks.PLANT_DEAD.get().defaultBlockState().setValue(DeadPlantBlock.KIND, kind);
        if (deadPlant.canSurvive(level, pos)) {
            level.setBlock(pos, deadPlant, SET_FLAGS);
        }
    }

    private static void placeDeadPlant(Level level, RandomSource random, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) {
            return;
        }
        DeadPlantBlock.Kind kind = random.nextBoolean() ? DeadPlantBlock.Kind.GRASS : DeadPlantBlock.Kind.GENERIC;
        BlockState deadPlant = HbmBlocks.PLANT_DEAD.get().defaultBlockState().setValue(DeadPlantBlock.KIND, kind);
        if (deadPlant.canSurvive(level, pos)) {
            level.setBlock(pos, deadPlant, SET_FLAGS);
        }
    }

    private static void placeMustardWillow(WorldGenLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) {
            return;
        }
        BlockState willow = HbmBlocks.PLANT_FLOWER.get().defaultBlockState().setValue(MustardWillowFlowerBlock.META, MustardWillowFlowerBlock.CD0);
        if (willow.canSurvive(level, pos)) {
            level.setBlock(pos, willow, SET_FLAGS);
        }
    }

    private static void placeMustardWillow(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) {
            return;
        }
        BlockState willow = HbmBlocks.PLANT_FLOWER.get().defaultBlockState().setValue(MustardWillowFlowerBlock.META, MustardWillowFlowerBlock.CD0);
        if (willow.canSurvive(level, pos)) {
            level.setBlock(pos, willow, SET_FLAGS);
        }
    }

    private static DeadPlantBlock.Kind deadPlantKind(BlockState state) {
        if (state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)) {
            return DeadPlantBlock.Kind.FERN;
        }
        if (state.is(BlockTags.TALL_FLOWERS)) {
            return DeadPlantBlock.Kind.BIG_FLOWER;
        }
        if (state.is(BlockTags.FLOWERS)) {
            return DeadPlantBlock.Kind.FLOWER;
        }
        if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)) {
            return DeadPlantBlock.Kind.GRASS;
        }
        return DeadPlantBlock.Kind.GENERIC;
    }

    private static BlockState dirtySandFor(BlockState state) {
        return state.is(Blocks.RED_SAND) ? HbmBlocks.SAND_DIRTY_RED.get().defaultBlockState() : HbmBlocks.SAND_DIRTY.get().defaultBlockState();
    }

    private static boolean isFullCube(WorldGenLevel level, BlockPos pos, BlockState state) {
        return state.isCollisionShapeFullBlock(level, pos);
    }

    private static boolean isFullCube(Level level, BlockPos pos, BlockState state) {
        return state.isCollisionShapeFullBlock(level, pos);
    }

    private static boolean isGrassOrDirt(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL);
    }

    private static boolean isSandOrOilSand(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(HbmBlocks.ORE_OIL_SAND.get());
    }

    private static boolean isStone(BlockState state) {
        return state.is(Blocks.STONE);
    }

    private static boolean isStoneLikeForCracking(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE);
    }

    private static boolean isPlant(BlockState state) {
        return state.getBlock() instanceof BushBlock
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.TALL_FLOWERS)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN);
    }

    private static boolean isOilImmunePlant(BlockState state) {
        return state.is(HbmBlocks.PLANT_DEAD.get())
                || state.is(HbmBlocks.PLANT_FLOWER.get())
                || state.is(HbmBlocks.PLANT_TALL.get());
    }
}
