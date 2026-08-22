package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SmallElectricBoilerBlockEntity extends SmallBoilerBlockEntity implements PowerEndpoint {
    public SmallElectricBoilerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.SMALL_ELECTRIC_BOILER.get(), pos, blockState);
    }

    @Override
    public boolean isElectric() {
        return true;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return List.of(
                this.worldPosition.relative(Direction.UP).immutable(),
                this.worldPosition.relative(Direction.DOWN).immutable(),
                this.worldPosition.relative(Direction.NORTH).immutable(),
                this.worldPosition.relative(Direction.SOUTH).immutable(),
                this.worldPosition.relative(Direction.WEST).immutable(),
                this.worldPosition.relative(Direction.EAST).immutable()
        );
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null) {
            PowerNetworkManager.markDirty(this.level);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null) {
            PowerNetworkManager.markDirty(this.level);
        }
        super.setRemoved();
    }
}
