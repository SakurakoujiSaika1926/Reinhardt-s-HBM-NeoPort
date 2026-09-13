package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

final class LegacyEmpUtil {
    private LegacyEmpUtil() {
    }

    static void empBlast(ServerLevel level, int x, int y, int z, int bombStartStrength) {
        int r = bombStartStrength;
        int r2 = r * r;
        int r22 = r2 / 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xx = -r; xx < r; xx++) {
            int xx2 = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int yy2 = xx2 + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    if (yy2 + zz * zz >= r22) {
                        continue;
                    }
                    pos.set(x + xx, y + yy, z + zz);
                    emp(level, pos, level.random);
                }
            }
        }
    }

    static boolean emp(Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof PowerEndpoint endpoint)) {
            return false;
        }
        endpoint.applyPower(Long.MAX_VALUE, 0L);
        if (random.nextInt(5) == 0 && level instanceof ServerLevel server) {
            server.setBlock(pos, HbmBlocks.BLOCK_ELECTRICAL_SCRAP.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        return true;
    }

    static boolean isEmpTarget(BlockEntity entity) {
        return entity instanceof PowerEndpoint;
    }
}
