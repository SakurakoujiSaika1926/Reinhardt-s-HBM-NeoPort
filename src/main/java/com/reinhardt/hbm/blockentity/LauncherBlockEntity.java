package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LauncherBlockEntity extends BlockEntity {
    public enum Kind {
        PAD_SMALL,
        PAD_RUSTED,
        PAD_LARGE,
        COMPACT,
        TABLE,
        SOYUZ
    }

    private final Kind kind;

    public LauncherBlockEntity(BlockPos pos, BlockState blockState) {
        this(pos, blockState, Kind.PAD_SMALL);
    }

    public LauncherBlockEntity(BlockPos pos, BlockState blockState, Kind kind) {
        super(HbmBlockEntities.LAUNCHER.get(), pos, blockState);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }
}
