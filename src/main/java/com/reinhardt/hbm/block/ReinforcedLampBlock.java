package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** The legacy reinforced lamp is two registry blocks that toggle on redstone power. */
public final class ReinforcedLampBlock extends Block {
    private final boolean lit;

    public ReinforcedLampBlock(Properties properties, boolean lit) {
        super(properties);
        this.lit = lit;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            updatePoweredState(level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide) {
            updatePoweredState(level, pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.lit && !level.hasNeighborSignal(pos)) {
            setLampState(level, pos, false);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(blockFor(false));
    }

    private void updatePoweredState(Level level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered && !this.lit) {
            setLampState(level, pos, true);
        } else if (!powered && this.lit) {
            level.scheduleTick(pos, this, 4);
        }
    }

    private void setLampState(Level level, BlockPos pos, boolean targetLit) {
        Block target = blockFor(targetLit);
        if (target != this) {
            level.setBlock(pos, target.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private Block blockFor(boolean targetLit) {
        String id = targetLit ? "reinforced_lamp_on" : "reinforced_lamp_off";
        return BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id(id));
    }
}
