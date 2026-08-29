package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.SpotlightBeamBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Direct port of the 1.7.10 tritium lamp's redstone-inverted behavior. */
public final class TritiumLampBlock extends Block {
    private final boolean lit;
    private final boolean blue;

    public TritiumLampBlock(Properties properties, boolean lit, boolean blue) {
        super(properties);
        this.lit = lit;
        this.blue = blue;
    }

    public boolean isLit() {
        return lit;
    }

    public int beamLength() {
        return 8;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            updatePower(level, pos);
            updateBeam(level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide) {
            updatePower(level, pos);
            updateBeam(level, pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (lit && !level.hasNeighborSignal(pos)) {
            setLampState(level, pos, false);
        }
    }

    private void updatePower(Level level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        if (!lit && powered) {
            setLampState(level, pos, true);
        } else if (lit && !powered) {
            level.scheduleTick(pos, this, 4);
        }
    }

    private void setLampState(Level level, BlockPos pos, boolean targetLit) {
        Block target = targetLit ? onBlock() : offBlock();
        if (target != this) {
            level.setBlock(pos, target.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private Block onBlock() {
        return blue ? HbmBlocks.LAMP_TRITIUM_BLUE_ON.get() : HbmBlocks.LAMP_TRITIUM_GREEN_ON.get();
    }

    private Block offBlock() {
        return blue ? HbmBlocks.LAMP_TRITIUM_BLUE_OFF.get() : HbmBlocks.LAMP_TRITIUM_GREEN_OFF.get();
    }

    private void updateBeam(Level level, BlockPos pos) {
        if (!lit) {
            return;
        }
        for (Direction direction : Direction.values()) {
            SpotlightBeamBlockEntity.propagate(level, pos, direction, beamLength());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            for (Direction direction : Direction.values()) {
                SpotlightBeamBlockEntity.unpropagate(level, pos, direction);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(offBlock());
    }
}
