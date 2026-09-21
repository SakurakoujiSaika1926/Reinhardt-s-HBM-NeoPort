package com.reinhardt.hbm.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.List;

public interface PowerGraphNode {
    BlockPos getGraphPos();

    default boolean isPowerGraphEnabled(LevelAccessor level) {
        return true;
    }

    default List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        List<BlockPos> connectors = new ArrayList<>(Direction.values().length);
        BlockPos pos = getGraphPos();
        for (Direction direction : Direction.values()) {
            connectors.add(pos.relative(direction).immutable());
        }
        return List.copyOf(connectors);
    }

    /** Directed graph edges used by one-way power devices. */
    default List<BlockPos> getPowerFlowPositions(LevelAccessor level) {
        return getPowerConnectorPositions(level);
    }

    /** Maximum amount of power this graph node may pass in one tick. */
    default long getPowerFlowLimit(LevelAccessor level) {
        return Long.MAX_VALUE;
    }

    /**
     * Marks nodes whose direction or throughput changes path reachability.
     * Ordinary cables and junctions can use the component-wide fast path;
     * diodes and transformer-like nodes opt into the directed solver.
     */
    default boolean requiresDirectedPowerRouting(LevelAccessor level) {
        return getPowerFlowLimit(level) != Long.MAX_VALUE;
    }

    /** Whether a directed path may enter this node from a connector. */
    default boolean canAcceptPowerFrom(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return canConnectPower(level, connectorPos, machineSide);
    }

    default boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return getPowerConnectorPositions(level).contains(connectorPos);
    }

    List<BlockPos> getRemotePowerLinks(Level level);
}
