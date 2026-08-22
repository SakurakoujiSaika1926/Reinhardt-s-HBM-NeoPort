package com.reinhardt.hbm.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayList;
import java.util.List;

public interface PowerEndpoint {
    BlockPos getPowerPos();

    default List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        List<BlockPos> connectors = new ArrayList<>(Direction.values().length);
        BlockPos pos = getPowerPos();
        for (Direction direction : Direction.values()) {
            connectors.add(pos.relative(direction).immutable());
        }
        return List.copyOf(connectors);
    }

    default boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return getPowerConnectorPositions(level).contains(connectorPos);
    }

    long getAvailableOutput();

    long getRequestedInput();

    default ConnectionPriority getPowerPriority() {
        return ConnectionPriority.NORMAL;
    }

    void applyPower(long usedOutput, long receivedInput);

    Component getPowerStatus();

    enum ConnectionPriority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST;

        public static final ConnectionPriority[] VALUES = new ConnectionPriority[]{LOWEST, LOW, NORMAL, HIGH, HIGHEST};
    }
}
