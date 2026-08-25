package com.reinhardt.hbm.worldgen;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.item.BedrockOreBaseItem;
import com.reinhardt.hbm.item.BedrockOreItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BedrockOreDepositFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public BedrockOreDepositFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public BedrockOreDepositFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!HbmConfig.GENERATE_BEDROCK_ORES.get()) {
            return false;
        }
        RandomSource random = context.random();
        int spawnRate = Math.max(1, HbmConfig.BEDROCK_ORE_SPAWN_RATE.get());
        if (random.nextInt(spawnRate) != 0) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        boolean nether = isNether(level);
        // 1.7.10 places deposits in the central two-by-two area of each generated chunk.
        int centerX = origin.getX() + random.nextInt(2) + 8;
        int centerZ = origin.getZ() + random.nextInt(2) + 8;
        boolean placed = generateBedrockOre(level, centerX, centerZ, random, nether);
        return placed;
    }

    private static boolean generateBedrockOre(WorldGenLevel level, int centerX, int centerZ, RandomSource random, boolean nether) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        double density = BedrockOreBaseItem.getAverageOreLevel(centerX, centerZ);
        ItemStack resource = nether
                ? weightedNetherResource(random)
                : new ItemStack(HbmItems.BEDROCK_ORE_BASE.get());
        HbmFluidStack acid = nether ? HbmFluidStack.EMPTY : boreFluid(density);
        int tier = nether ? 1 : boreTier(density);
        int color = netherResourceColor(resource);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                int y = level.getMinBuildHeight();
                pos.set(x, y, z);
                BlockState state = level.getBlockState(pos);
                if (!state.is(Blocks.BEDROCK)) {
                    continue;
                }
                if (dx == 0 && dz == 0 || random.nextBoolean()) {
                    level.setBlock(pos, HbmBlocks.ORE_BEDROCK_BLOCK.get().defaultBlockState(), SET_FLAGS);
                    if (level.getBlockEntity(pos) instanceof com.reinhardt.hbm.blockentity.BedrockOreBlockEntity ore) {
                        ore.configure(
                                resource.copy(),
                                acid,
                                color,
                                tier,
                                random.nextInt(10)
                        );
                    }
                    placed = true;
                }
            }
        }
        generateDepthRock(level, centerX, centerZ, nether);
        return placed;
    }

    private static void generateDepthRock(WorldGenLevel level, int centerX, int centerZ, boolean nether) {
        Block depthRock = nether ? HbmBlocks.STONE_DEPTH_NETHER.get() : HbmBlocks.STONE_DEPTH.get();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();
        for (int x = centerX - 3; x <= centerX + 3; x++) {
            for (int z = centerZ - 3; z <= centerZ + 3; z++) {
                for (int dy = 1; dy < 7; dy++) {
                    int y = minY + dy;
                    if (y >= level.getMaxBuildHeight()) {
                        continue;
                    }
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    boolean replace = dy < 3 || state.is(Blocks.BEDROCK);
                    if (replace && (state.is(Blocks.STONE) || state.is(Blocks.BEDROCK) || state.is(Blocks.NETHERRACK))) {
                        level.setBlock(pos, depthRock.defaultBlockState(), SET_FLAGS);
                    }
                }
            }
        }
    }

    private static ItemStack weightedNetherResource(RandomSource random) {
        int glowstoneWeight = Math.max(0, HbmConfig.BEDROCK_ORE_NETHER_GLOWSTONE_WEIGHT.get());
        int phosphorusWeight = Math.max(0, HbmConfig.BEDROCK_ORE_NETHER_PHOSPHORUS_WEIGHT.get());
        int quartzWeight = Math.max(0, HbmConfig.BEDROCK_ORE_NETHER_QUARTZ_WEIGHT.get());
        int totalWeight = glowstoneWeight + phosphorusWeight + quartzWeight;
        if (totalWeight <= 0) {
            return new ItemStack(Items.GLOWSTONE_DUST, 4);
        }
        int roll = random.nextInt(totalWeight);
        if (roll < glowstoneWeight) {
            return new ItemStack(Items.GLOWSTONE_DUST, 4);
        }
        if (roll < glowstoneWeight + phosphorusWeight) {
            return new ItemStack(item("powder_fire"), 4);
        }
        return new ItemStack(Items.QUARTZ, 4);
    }

    private static int netherResourceColor(ItemStack resource) {
        if (resource.is(HbmItems.BEDROCK_ORE_BASE.get())) {
            return 0xD78A16;
        }
        if (resource.is(Items.GLOWSTONE_DUST)) {
            return 0xF9FF4D;
        }
        if (resource.is(Items.QUARTZ)) {
            return 0xF0EFDD;
        }
        return 0xD7341F;
    }

    private static HbmFluidStack boreFluid(double density) {
        if (density > 1.5D) {
            return new HbmFluidStack(HbmFluids.byName("solvent").orElse(HbmFluids.none()), 2_000);
        }
        if (density > 1.0D) {
            return new HbmFluidStack(HbmFluids.byName("sulfuric_acid").orElse(HbmFluids.none()), 1_000);
        }
        if (density > 0.75D) {
            return new HbmFluidStack(HbmFluids.byName("water").orElse(HbmFluids.none()), 1_000);
        }
        return HbmFluidStack.EMPTY;
    }

    private static int boreTier(double density) {
        if (density > 1.5D) {
            return 4;
        }
        if (density > 1.0D) {
            return 3;
        }
        if (density > 0.75D) {
            return 2;
        }
        return 1;
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("reinhardtshbm", path));
    }

    private static boolean isNether(WorldGenLevel level) {
        Level realLevel = level.getLevel();
        return realLevel != null && realLevel.dimension() == Level.NETHER;
    }
}
