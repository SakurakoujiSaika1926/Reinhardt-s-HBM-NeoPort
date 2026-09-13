package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.MiningLaserBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

public class MiningLaserBlock extends LegacyDirectionalBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape CORE_SHAPE = Shapes.block();

    public MiningLaserBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace() != net.minecraft.core.Direction.DOWN) {
            return null;
        }
        BlockPos corePos = legacyCorePos(context.getClickedPos());
        if (!canPlaceAt(context.getLevel(), corePos, context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(LIT, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MiningLaserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == HbmBlockEntities.MINING_LASER.get()
                ? (tickerLevel, pos, tickerState, blockEntity) -> MiningLaserBlockEntity.tick(
                        tickerLevel,
                        pos,
                        tickerState,
                        (MiningLaserBlockEntity) blockEntity
                )
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            placeDummies(level, pos);
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            if (!level.isClientSide) {
                removeDummies(level, pos);
            }
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            LargeMachineBlock.pushEntitiesOutOfPositions(level, pos, state.getValue(FACING), MiningLaserBlockEntity.multiblockPositions(pos), placer);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return CORE_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return CORE_SHAPE;
    }

    public static BlockPos legacyCorePos(BlockPos clickedPos) {
        return clickedPos.below();
    }

    public static boolean canPlaceAt(LevelReader level, BlockPos corePos, BlockPlaceContext context) {
        if (!level.getBlockState(corePos).canBeReplaced(context)) {
            return false;
        }
        for (BlockPos pos : MiningLaserBlockEntity.multiblockPositions(corePos)) {
            if (pos.equals(corePos)) {
                continue;
            }
            if (!level.getBlockState(pos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private static void placeDummies(Level level, BlockPos corePos) {
        for (BlockPos pos : MiningLaserBlockEntity.multiblockPositions(corePos)) {
            if (pos.equals(corePos)) {
                continue;
            }
            level.setBlock(pos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    private static void removeDummies(Level level, BlockPos corePos) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos pos : MiningLaserBlockEntity.multiblockPositions(corePos)) {
                if (pos.equals(corePos)) {
                    continue;
                }
                if (level.getBlockState(pos).is(HbmBlocks.MACHINE_DUMMY.get())
                        && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                        && dummy.getCorePos().equals(corePos)) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }
}
