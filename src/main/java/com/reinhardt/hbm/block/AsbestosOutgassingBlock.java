package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7.10 {@code BlockOutgas} behaviour for asbestos blocks.
 *
 * Random-ticking variants release gas into one random adjacent air block;
 * walking over a random-ticking variant can release gas above the block and
 * always produces the old warning particles.  All variants replace
 * themselves with asbestos gas when harvested or destroyed by an explosion.
 */
public class AsbestosOutgassingBlock extends Block {
    private final boolean randomTicking;

    public AsbestosOutgassingBlock(Properties properties, boolean randomTicking) {
        super(randomTicking ? properties.randomTicks() : properties);
        this.randomTicking = randomTicking;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        // BlockOutgas selected exactly one of the six Forge directions on
        // every random tick; there was no additional 1/6 chance gate.
        Direction direction = Direction.values()[random.nextInt(6)];
        BlockPos target = pos.relative(direction);
        if (level.isEmptyBlock(target)) {
            level.setBlock(target, HbmBlocks.GAS_ASBESTOS.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!randomTicking || !(level instanceof ServerLevel serverLevel) || !level.isEmptyBlock(pos.above())) {
            return;
        }

        if (level.random.nextInt(10) == 0) {
            level.setBlock(pos.above(), HbmBlocks.GAS_ASBESTOS.get().defaultBlockState(), 3);
        }
        serverLevel.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + level.random.nextFloat(),
                pos.getY() + 1.1D,
                pos.getZ() + level.random.nextFloat(),
                5,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        replaceWithGas(level, pos);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        super.wasExploded(level, pos, explosion);
        replaceWithGas(level, pos);
    }

    private static void replaceWithGas(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            level.setBlock(pos, HbmBlocks.GAS_ASBESTOS.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
