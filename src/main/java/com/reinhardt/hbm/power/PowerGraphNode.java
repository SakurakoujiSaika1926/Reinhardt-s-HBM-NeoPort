package com.reinhardt.hbm.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.List;

public interface PowerGraphNode {
    BlockPos getGraphPos();

    default List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        List<BlockPos> connectors = new ArrayList<>(Direction.values().length);
        BlockPos pos = getGraphPos();
        for (Direction direction : Direction.values()) {
            connectors.add(pos.relative(direction).immutable());
        }
        return List.copyOf(connectors);
    }

    default boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return getPowerConnectorPositions(level).contains(connectorPos);
    }

    List<BlockPos> getRemotePowerLinks(Level level);
}
