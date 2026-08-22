package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class IndustrialBoilerBlockEntity extends HeatBoilerBlockEntity {
    private static final int MAX_HEAT = 12_800_000;

    public IndustrialBoilerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.INDUSTRIAL_BOILER.get(), pos, blockState, 64_000);
    }

    @Override
    protected int inputCapacity() {
        return 64_000;
    }

    @Override
    protected int maxHeat() {
        return MAX_HEAT;
    }

    @Override
    protected boolean canExplode() {
        return false;
    }

    @Override
    public List<Port> ports(LevelAccessor level) {
        return List.of(
                new Port(this.worldPosition.east(), Direction.EAST),
                new Port(this.worldPosition.west(), Direction.WEST),
                new Port(this.worldPosition.south(), Direction.SOUTH),
                new Port(this.worldPosition.north(), Direction.NORTH),
                new Port(this.worldPosition.above(4), Direction.UP)
        );
    }
}
