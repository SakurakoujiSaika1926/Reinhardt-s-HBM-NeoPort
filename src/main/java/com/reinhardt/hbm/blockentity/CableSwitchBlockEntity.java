package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CableDetectorBlock;
import com.reinhardt.hbm.block.CableSwitchBlock;
import com.reinhardt.hbm.power.PowerGraphNode;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** A redstone-controlled graph bridge, matching 1.7.10's node registration rule. */
public final class CableSwitchBlockEntity extends BlockEntity implements PowerGraphNode {
    public CableSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CABLE_SWITCH.get(), pos, state);
    }

    @Override
    public BlockPos getGraphPos() {
        return worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return List.of(
                worldPosition.relative(Direction.NORTH),
                worldPosition.relative(Direction.SOUTH),
                worldPosition.relative(Direction.EAST),
                worldPosition.relative(Direction.WEST),
                worldPosition.relative(Direction.UP),
                worldPosition.relative(Direction.DOWN)
        );
    }

    @Override
    public boolean isPowerGraphEnabled(LevelAccessor level) {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof CableSwitchBlock) {
            return state.getValue(CableSwitchBlock.POWERED);
        }
        if (state.getBlock() instanceof CableDetectorBlock) {
            return state.getValue(CableDetectorBlock.POWERED);
        }
        return false;
    }

    @Override
    public List<BlockPos> getRemotePowerLinks(net.minecraft.world.level.Level level) {
        return List.of();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }
}
