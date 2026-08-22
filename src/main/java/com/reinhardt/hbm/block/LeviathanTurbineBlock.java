package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LeviathanTurbineBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LeviathanTurbineBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = createFootprint();

    public LeviathanTurbineBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, RotationBasis.MODERN_NORTH);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!LargeMachineBlock.canPlaceFootprint(context, facing, FOOTPRINT)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LeviathanTurbineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.LEVIATHAN_TURBINE.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> LeviathanTurbineBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (LeviathanTurbineBlockEntity) blockEntity
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return useOnPart(level, pos, pos, player);
    }

    public static InteractionResult useOnPart(Level level, BlockPos clickedPos, BlockPos corePos, Player player) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(corePos) instanceof LeviathanTurbineBlockEntity turbine)) {
            return InteractionResult.PASS;
        }
        if (!turbine.isLeverPart(clickedPos)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            if (turbine.cycleCompression(player)) {
                level.playSound(null, clickedPos, HbmSoundEvents.CHUNGUS_LEVER.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (removed) {
            PowerNetworkManager.markDirty(level);
        }
    }

    private static Footprint createFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        addBox(offsets, -2, 2, 0, 3, -3, 0);
        addBox(offsets, -1, 1, 4, 4, -3, 0);
        addBox(offsets, -1, 1, 0, 3, 1, 6);
        addBox(offsets, -1, 1, 0, 2, 7, 10);
        offsets.add(new BlockPos(0, 2, -4));
        return new Footprint(List.copyOf(offsets));
    }

    private static void addBox(
            Set<BlockPos> offsets,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ
    ) {
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
    }
}

