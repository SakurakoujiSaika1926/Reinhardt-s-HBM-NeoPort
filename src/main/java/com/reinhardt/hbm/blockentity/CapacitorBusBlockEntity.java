package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.CapacitorBusBlock;
import com.reinhardt.hbm.power.PowerGraphNode;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Direction-only graph node; it deliberately has no inventory or dummy shape. */
public final class CapacitorBusBlockEntity extends BlockEntity implements PowerGraphNode {
    public CapacitorBusBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CAPACITOR_BUS.get(), pos, state);
    }

    @Override
    public BlockPos getGraphPos() {
        return worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        Direction facing = getBlockState().getValue(CapacitorBusBlock.FACING);
        return List.of(worldPosition.relative(facing), worldPosition.relative(facing.getOpposite()));
    }

    @Override
    public List<BlockPos> getPowerFlowPositions(LevelAccessor level) {
        return List.of(worldPosition.relative(getBlockState().getValue(CapacitorBusBlock.FACING)));
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return getPowerConnectorPositions(level).contains(connectorPos);
    }

    @Override
    public boolean canAcceptPowerFrom(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        Direction facing = getBlockState().getValue(CapacitorBusBlock.FACING);
        return connectorPos.equals(worldPosition.relative(facing.getOpposite()));
    }

    @Override
    public boolean requiresDirectedPowerRouting(LevelAccessor level) {
        return true;
    }

    @Override
    public List<BlockPos> getRemotePowerLinks(Level level) {
        return List.of();
    }
}
