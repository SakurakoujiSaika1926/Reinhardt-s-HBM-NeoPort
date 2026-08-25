package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.AshpitBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

public class AshpitBlock extends LargeMachineBlock implements EntityBlock {
    public AshpitBlock(Properties properties) {
        // MachineAshpit is a BlockDummyable machine authored in the old SOUTH
        // basis. Its 3x3 footprint must rotate with that same basis.
        super(properties, Footprint.centered(1, 1, 1), Shapes.block(), RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // The original renderer draws Main, Door and Inner as separate OBJ
        // groups. Rendering the block model as well duplicates the shell.
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof AshpitBlockEntity ashpit) {
            ashpit.startOpen(serverPlayer);
            serverPlayer.openMenu(ashpit, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AshpitBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof AshpitBlockEntity ashpit) {
                AshpitBlockEntity.tick(tickerLevel, pos, tickerState, ashpit);
            }
        };
    }
}

