package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.PileControlBlockEntity;
import com.reinhardt.hbm.blockentity.PileLoaderBlockEntity;
import com.reinhardt.hbm.blockentity.PileMk2CoreBlockEntity;
import com.reinhardt.hbm.blockentity.PileVentBlockEntity;
import com.reinhardt.hbm.item.PileRodMk2Item;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** One flattened block registration for each current-upstream Chicago Pile MK2 device. */
public final class PileMk2DeviceBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final Kind kind;

    public PileMk2DeviceBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (kind == Kind.CONTROL) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
        Direction clicked = context.getClickedFace();
        return defaultBlockState().setValue(FACING,
                clicked.getAxis().isHorizontal() ? clicked : Direction.NORTH);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) level.invalidateCapabilities(pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case LOADER -> new PileLoaderBlockEntity(pos, state);
            case VENT -> new PileVentBlockEntity(pos, state);
            case CONTROL -> new PileControlBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (kind == Kind.LOADER && type == HbmBlockEntities.PILE_MK2_LOADER.get()) {
            return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                    PileLoaderBlockEntity.tick(tickerLevel, tickerPos, tickerState,
                            (PileLoaderBlockEntity) blockEntity);
        }
        if (kind == Kind.VENT && type == HbmBlockEntities.PILE_MK2_VENT.get()) {
            return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                    PileVentBlockEntity.tick(tickerLevel, tickerPos, tickerState,
                            (PileVentBlockEntity) blockEntity);
        }
        if (kind == Kind.CONTROL && type == HbmBlockEntities.PILE_MK2_CONTROL.get()) {
            return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                    PileControlBlockEntity.tick(tickerLevel, tickerPos, tickerState,
                            (PileControlBlockEntity) blockEntity);
        }
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (ScrewdriverItem.isHandDrill(stack)) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            BlockPos entry = kind == Kind.CONTROL ? pos.below() : pos.relative(state.getValue(FACING).getOpposite());
            Direction inward = kind == Kind.CONTROL ? Direction.DOWN : state.getValue(FACING).getOpposite();
            PileMk2CoreBlockEntity core = PileMk2Block.findCore(level, entry);
            if (core == null) return ItemInteractionResult.FAIL;
            boolean changed = core.drillChannel(entry, inward, player);
            if (changed) ScrewdriverItem.damageTool(stack, level, player, hand);
            return changed ? ItemInteractionResult.CONSUME : ItemInteractionResult.FAIL;
        }

        if (kind == Kind.LOADER && level.getBlockEntity(pos) instanceof PileLoaderBlockEntity loader) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (loader.renderedLevel(1.0F) <= 0.0D && !stack.isEmpty()
                    && PileRodMk2Item.isPileRod(stack) && loader.loadingStack().isEmpty()) {
                loader.setItem(0, stack.copyWithCount(1));
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, pos, HbmSoundEvents.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.CONSUME;
            }
            loader.startLoading();
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hit) {
        if (kind != Kind.LOADER) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PileLoaderBlockEntity loader) {
            loader.startLoading();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            level.invalidateCapabilities(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (kind) {
            case LOADER -> state.setValue(FACING, switch (rotation) {
                case NONE -> state.getValue(FACING);
                case CLOCKWISE_90 -> state.getValue(FACING).getClockWise();
                case CLOCKWISE_180 -> state.getValue(FACING).getOpposite();
                case COUNTERCLOCKWISE_90 -> state.getValue(FACING).getCounterClockWise();
            });
            case VENT -> state.setValue(FACING, switch (rotation) {
                case NONE -> state.getValue(FACING);
                case CLOCKWISE_90 -> state.getValue(FACING).getClockWise();
                case CLOCKWISE_180 -> state.getValue(FACING).getOpposite();
                case COUNTERCLOCKWISE_90 -> state.getValue(FACING).getCounterClockWise();
            });
            case CONTROL -> state.setValue(FACING, switch (rotation) {
                case NONE -> state.getValue(FACING);
                case CLOCKWISE_90 -> state.getValue(FACING).getClockWise();
                case CLOCKWISE_180 -> state.getValue(FACING).getOpposite();
                case COUNTERCLOCKWISE_90 -> state.getValue(FACING).getCounterClockWise();
            });
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (kind) {
            case LOADER -> state.setValue(FACING, switch (mirror) {
                case NONE -> state.getValue(FACING);
                case LEFT_RIGHT -> switch (state.getValue(FACING)) {
                    case NORTH -> Direction.SOUTH;
                    case SOUTH -> Direction.NORTH;
                    case EAST -> Direction.EAST;
                    case WEST -> Direction.WEST;
                    case UP, DOWN -> throw new IllegalStateException("Pile loader facing must be horizontal");
                };
                case FRONT_BACK -> switch (state.getValue(FACING)) {
                    case NORTH -> Direction.NORTH;
                    case SOUTH -> Direction.SOUTH;
                    case EAST -> Direction.WEST;
                    case WEST -> Direction.EAST;
                    case UP, DOWN -> throw new IllegalStateException("Pile loader facing must be horizontal");
                };
            });
            case VENT -> state.setValue(FACING, switch (mirror) {
                case NONE -> state.getValue(FACING);
                case LEFT_RIGHT -> switch (state.getValue(FACING)) {
                    case NORTH -> Direction.SOUTH;
                    case SOUTH -> Direction.NORTH;
                    case EAST -> Direction.EAST;
                    case WEST -> Direction.WEST;
                    case UP, DOWN -> throw new IllegalStateException("Pile vent facing must be horizontal");
                };
                case FRONT_BACK -> switch (state.getValue(FACING)) {
                    case NORTH -> Direction.NORTH;
                    case SOUTH -> Direction.SOUTH;
                    case EAST -> Direction.WEST;
                    case WEST -> Direction.EAST;
                    case UP, DOWN -> throw new IllegalStateException("Pile vent facing must be horizontal");
                };
            });
            case CONTROL -> state.setValue(FACING, switch (mirror) {
                case NONE -> state.getValue(FACING);
                case LEFT_RIGHT -> switch (state.getValue(FACING)) {
                    case NORTH -> Direction.SOUTH;
                    case SOUTH -> Direction.NORTH;
                    case EAST -> Direction.EAST;
                    case WEST -> Direction.WEST;
                    case UP, DOWN -> throw new IllegalStateException("Pile control facing must be horizontal");
                };
                case FRONT_BACK -> switch (state.getValue(FACING)) {
                    case NORTH -> Direction.NORTH;
                    case SOUTH -> Direction.SOUTH;
                    case EAST -> Direction.WEST;
                    case WEST -> Direction.EAST;
                    case UP, DOWN -> throw new IllegalStateException("Pile control facing must be horizontal");
                };
            });
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public enum Kind {
        LOADER,
        VENT,
        CONTROL
    }
}
