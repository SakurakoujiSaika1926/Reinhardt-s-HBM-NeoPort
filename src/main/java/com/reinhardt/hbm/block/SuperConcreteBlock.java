package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Exact damage-state and decay behaviour of the 1.7.10 {@code BlockUberConcrete}. */
public final class SuperConcreteBlock extends Block {
    public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, 15);
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public SuperConcreteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DAMAGE, 0));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int damage = state.getValue(DAMAGE);
        if (random.nextInt(damage + 1) > 0) {
            return;
        }
        if (damage < 15) {
            level.setBlock(pos, state.setValue(DAMAGE, damage + 1), Block.UPDATE_ALL);
            return;
        }

        level.removeBlock(pos, false);
        BlockState debris = HbmBlocks.CONCRETE_SUPER_BROKEN.get().defaultBlockState();
        if (level.getBlockState(pos.below()).isAir()) {
            level.setBlock(pos, debris, Block.UPDATE_ALL);
            return;
        }

        Direction[] shuffled = HORIZONTAL.clone();
        for (int i = shuffled.length - 1; i > 0; i--) {
            int swap = random.nextInt(i + 1);
            Direction direction = shuffled[i];
            shuffled[i] = shuffled[swap];
            shuffled[swap] = direction;
        }
        for (Direction direction : shuffled) {
            BlockPos target = pos.relative(direction);
            if (!level.getBlockState(target).isAir() || !level.getBlockState(target.below()).isAir()) {
                continue;
            }
            level.setBlock(pos, debris, Block.UPDATE_ALL);
            FallingBlockEntity falling = FallingBlockEntity.fall(level, pos, debris);
            falling.setPos(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
            falling.time = 2;
            falling.dropItem = false;
            falling.setHurtsEntities(2.0F, 40);
            return;
        }

        level.setBlock(pos, debris, Block.UPDATE_ALL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DAMAGE);
    }
}
