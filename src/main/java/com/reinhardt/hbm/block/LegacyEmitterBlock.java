package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LegacyEmitterBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public final class LegacyEmitterBlock extends Block implements EntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final Kind kind;

    public LegacyEmitterBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    public Kind kind() {
        return kind;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LegacyEmitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (blockEntityType != HbmBlockEntities.LEGACY_EMITTER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> LegacyEmitterBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (LegacyEmitterBlockEntity) blockEntity
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public enum Kind {
        GEYSER_CHLORINE,
        GEYSER_NETHER,
        VENT_CHLORINE,
        VENT_CLOUD,
        VENT_PINK_CLOUD,
        VENT_CHLORINE_SEAL;

        public boolean isGeyser() {
            return this == GEYSER_CHLORINE || this == GEYSER_NETHER;
        }

        public boolean isVent() {
            return this == VENT_CHLORINE || this == VENT_CLOUD || this == VENT_PINK_CLOUD;
        }
    }
}
