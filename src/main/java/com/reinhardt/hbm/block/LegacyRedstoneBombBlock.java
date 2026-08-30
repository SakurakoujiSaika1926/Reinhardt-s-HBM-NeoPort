package com.reinhardt.hbm.block;

import com.reinhardt.hbm.explosion.LegacyChaosEffects;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Exact redstone trigger semantics for BombFloat, EMP Device and Flame War in a Box. */
public final class LegacyRedstoneBombBlock extends Block {
    public enum Kind {
        FLOAT,
        EMP,
        FLAME_WAR
    }

    private final Kind kind;

    public LegacyRedstoneBombBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && level.hasNeighborSignal(pos)) {
            detonate((ServerLevel) level, pos);
        }
    }

    private void detonate(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(this)) {
            return;
        }
        level.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS,
                5.0F, 0.9F + level.random.nextFloat() * 0.2F);
        level.removeBlock(pos, false);

        switch (kind) {
            case FLOAT -> LegacyChaosEffects.floatAndLift(level, pos, 15, 50);
            case EMP -> dischargeEmp(level, pos);
            case FLAME_WAR -> flameWar(level, pos, level.random);
        }
    }

    /**
     * 1.7.10 iterated every loaded position in the EMP sphere, cleared HBM energy
     * handlers, then converted one fifth of them into electrical scrap.  The modern
     * power endpoint owns its stored energy, so applyPower with a saturated output
     * is the direct equivalent without touching unrelated block entities.
     */
    private static void dischargeEmp(ServerLevel level, BlockPos origin) {
        int radius = 50;
        int radiusSquaredHalf = radius * radius / 2;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x < radius; x++) {
            int xSquared = x * x;
            for (int y = -radius; y < radius; y++) {
                int xySquared = xSquared + y * y;
                for (int z = -radius; z < radius; z++) {
                    if (xySquared + z * z >= radiusSquaredHalf) {
                        continue;
                    }
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockEntity entity = level.getBlockEntity(cursor);
                    if (!(entity instanceof PowerEndpoint endpoint)) {
                        continue;
                    }
                    endpoint.applyPower(Long.MAX_VALUE, 0L);
                    if (level.random.nextInt(5) == 0) {
                        level.setBlock(cursor, HbmBlocks.BLOCK_ELECTRICAL_SCRAP.get().defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private static void flameWar(ServerLevel level, BlockPos origin, RandomSource random) {
        for (int index = 0; index < 150; index++) {
            level.explode(null,
                    origin.getX() + random.nextInt(51) - 25,
                    origin.getY() + random.nextInt(11) - 5,
                    origin.getZ() + random.nextInt(51) - 25,
                    4.0F,
                    Level.ExplosionInteraction.BLOCK);
        }
        level.explode(null, origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D,
                15.0F, Level.ExplosionInteraction.BLOCK);
    }
}
