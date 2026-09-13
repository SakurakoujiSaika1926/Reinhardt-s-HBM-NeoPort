package com.reinhardt.hbm.worldgen;

import com.reinhardt.hbm.blockentity.DecoLootBlockEntity;
import com.reinhardt.hbm.blockentity.LanternBehemothBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.item.LegacyBookLoreItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Surface generation path from HbmWorldGen's fixed 1-in-2000 old-lantern roll. */
public final class LanternBehemothFeature extends Feature<NoneFeatureConfiguration> {
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    public LanternBehemothFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (level.getLevel().dimension() != Level.OVERWORLD
                || !HbmConfig.legacyDungeonGenerationEnabled(level.getLevel().getServer().getWorldData().worldGenOptions().generateStructures())) {
            return false;
        }
        BlockPos corePos = context.origin();
        BlockPos supportPos = corePos.below();
        if (!level.getBlockState(supportPos).isFaceSturdy(level, supportPos, Direction.UP)) {
            return false;
        }
        for (int y = 0; y <= 4; y++) {
            if (!level.getBlockState(corePos.above(y)).canBeReplaced()) {
                return false;
            }
        }

        level.setBlock(corePos, HbmBlocks.LANTERN_BEHEMOTH.get().defaultBlockState(), FLAGS);
        if (!(level.getBlockEntity(corePos) instanceof LanternBehemothBlockEntity lantern)) {
            return false;
        }
        lantern.setBroken(true);

        for (int y = 1; y <= 4; y++) {
            BlockPos dummyPos = corePos.above(y);
            level.setBlock(dummyPos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), FLAGS);
            if (level.getBlockEntity(dummyPos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }

        if (context.random().nextBoolean()) {
            placeBooklet(level, corePos.north(2));
        }
        return true;
    }

    private static void placeBooklet(WorldGenLevel level, BlockPos pos) {
        level.setBlock(pos, HbmBlocks.DECO_LOOT.get().defaultBlockState(), FLAGS);
        if (level.getBlockEntity(pos) instanceof DecoLootBlockEntity loot) {
            ItemStack book = LegacyBookLoreItem.create(
                    new ItemStack(HbmItems.BOOK_LORE.get()), "beacon", 12, 0x404040, 0xD637B3);
            loot.addItem(book, 0.0D, 0.0D, 0.0D);
        }
    }
}
