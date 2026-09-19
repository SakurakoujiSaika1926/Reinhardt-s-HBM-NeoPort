package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7.10/1.12.2 {@code BlockNetherCoal}: a glowing animated ore that burns
 * entities walking over it, emits flame/smoke particles from exposed faces and
 * leaves carbon monoxide gas when broken.
 */
public class NetherCoalOreBlock extends Block {
    public NetherCoalOreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        entity.igniteForSeconds(3.0F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN || !level.isEmptyBlock(pos.relative(direction))) {
                continue;
            }

            double x = pos.getX() + 0.5D + direction.getStepX() + random.nextDouble() - 0.5D;
            double y = pos.getY() + 0.5D + direction.getStepY() + random.nextDouble() - 0.5D;
            double z = pos.getZ() + 0.5D + direction.getStepZ() + random.nextDouble() - 0.5D;

            if (direction.getAxis() == Direction.Axis.X) {
                x = pos.getX() + 0.5D + direction.getStepX() * 0.5D + random.nextDouble() * 0.125D * direction.getStepX();
            }
            if (direction.getAxis() == Direction.Axis.Y) {
                y = pos.getY() + 0.5D + direction.getStepY() * 0.5D + random.nextDouble() * 0.125D * direction.getStepY();
            }
            if (direction.getAxis() == Direction.Axis.Z) {
                z = pos.getZ() + 0.5D + direction.getStepZ() * 0.5D + random.nextDouble() * 0.125D * direction.getStepZ();
            }

            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.1D, 0.0D);
        }
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        replaceWithMonoxide(level, pos);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        super.wasExploded(level, pos, explosion);
        replaceWithMonoxide(level, pos);
    }

    private static void replaceWithMonoxide(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            level.setBlock(pos, HbmBlocks.GAS_MONOXIDE.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
