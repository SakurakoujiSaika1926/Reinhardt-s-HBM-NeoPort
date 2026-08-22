package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.RadarScreenBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

/** 1.7.10 MachineRadarScreen: a SOUTH-basis 2 x 2 dummyable radar monitor. */
public final class RadarScreenBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.legacySouthBox(1, 0, 0, 0, 1, 0);

    public RadarScreenBlock(Properties properties) {
        super(properties, FOOTPRINT, Shapes.block(), RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!LargeMachineBlock.canPlaceLegacyFootprint(context, facing, FOOTPRINT)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadarScreenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.RADAR_SCREEN.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, entity) -> RadarScreenBlockEntity.tick(
                tickerLevel, tickerPos, tickerState, (RadarScreenBlockEntity) entity
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof RadarScreenBlockEntity screen
                && player instanceof ServerPlayer serverPlayer) {
            screen.openLinkedRadar(serverPlayer);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
