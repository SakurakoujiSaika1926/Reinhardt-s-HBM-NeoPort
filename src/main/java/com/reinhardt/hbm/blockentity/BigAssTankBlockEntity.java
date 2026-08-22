package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidTrait;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class BigAssTankBlockEntity extends FluidTankBlockEntity {
    public static final int CAPACITY = 2_048_000;

    public BigAssTankBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.BIG_ASS_TANK.get(), pos, blockState, CAPACITY, "container.reinhardtshbm.bat9000");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BigAssTankBlockEntity tank) {
        FluidTankBlockEntity.tick(level, pos, state, tank);
        if (!tank.tank().type().isNone() && tank.tank().type().hasTrait(HbmFluidTrait.ANTIMATTER)) {
            level.destroyBlock(pos, false);
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 10.0F, true, Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override
    public List<Port> ports(LevelAccessor level) {
        // BAT9000 is the old deprecated tank. Its 1.7.10 getConPos() used
        // world axes directly even though the multiblock footprint rotated.
        return List.of(
                legacyPort(new BlockPos(1, 0, 3), Direction.SOUTH),
                legacyPort(new BlockPos(-1, 0, 3), Direction.SOUTH),
                legacyPort(new BlockPos(1, 0, -3), Direction.NORTH),
                legacyPort(new BlockPos(-1, 0, -3), Direction.NORTH),
                legacyPort(new BlockPos(3, 0, 1), Direction.EAST),
                legacyPort(new BlockPos(-3, 0, 1), Direction.WEST),
                legacyPort(new BlockPos(3, 0, -1), Direction.EAST),
                legacyPort(new BlockPos(-3, 0, -1), Direction.WEST)
        );
    }

    private Port legacyPort(BlockPos connectorOffset, Direction face) {
        BlockPos connector = this.worldPosition.offset(connectorOffset);
        return new Port(connector.relative(face.getOpposite()).immutable(), face);
    }
}
