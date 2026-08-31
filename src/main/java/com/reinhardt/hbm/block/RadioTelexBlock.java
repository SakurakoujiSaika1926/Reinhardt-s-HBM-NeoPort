package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.RadioTelexBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** 1.7.10 RadioTelex: a two-block-wide legacy dummyable machine. */
public final class RadioTelexBlock extends LargeMachineBlock implements EntityBlock {
    private static final Footprint FOOTPRINT = Footprint.legacySouthBox(0, 0, 0, 0, 1, 0);

    public RadioTelexBlock(Properties properties) {
        super(properties, FOOTPRINT, RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioTelexBlockEntity(pos, state);
    }
}
