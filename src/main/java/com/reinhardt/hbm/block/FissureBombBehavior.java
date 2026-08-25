package com.reinhardt.hbm.block;

import com.reinhardt.hbm.explosion.LegacyMukeExplosion;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 1.7.10 BlockFissureBomb post-blast bedrock conversion. */
public final class FissureBombBehavior {
    private FissureBombBehavior() {
    }

    public static void detonate(ServerLevel level, Entity source, Vec3 center) {
        LegacyMukeExplosion.detonateMediumMiniNuke(level, source, center);
        int range = 5;
        boolean crater = level.getBiome(BlockPos.containing(center)).unwrapKey()
                .map(key -> key.location().getPath().contains("crater"))
                .orElse(false);
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = BlockPos.containing(center).offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(HbmBlocks.ORE_BEDROCK_BLOCK.get())) {
                        level.setBlock(pos, HbmBlocks.ORE_VOLCANO.get().defaultBlockState()
                                .setValue(FissureBlock.CRATER, crater), net.minecraft.world.level.block.Block.UPDATE_ALL);
                    } else if (state.is(HbmBlocks.ORE_BEDROCK_OIL.get())) {
                        level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
                    }
                }
            }
        }
    }
}
