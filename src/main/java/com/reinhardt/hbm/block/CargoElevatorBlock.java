package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.CargoElevatorBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class CargoElevatorBlock extends Block implements EntityBlock {
    public static final BooleanProperty CORE = BooleanProperty.create("core");
    private static final ThreadLocal<Boolean> BUILDING = ThreadLocal.withInitial(() -> false);

    public CargoElevatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CORE, true));
    }

    /**
     * BlockDummyable#getOffset() is one: the clicked position is the edge of
     * the initial 3x3 platform and the core is one block toward the player.
     * Keep the same yaw quantisation as the 1.7.10 implementation.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos clicked = context.getClickedPos();
        BlockPos core = corePos(clicked, context.getPlayer());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos target = core.offset(x, 0, z);
                if (!target.equals(clicked) && !context.getLevel().getBlockState(target).canBeReplaced(context)) {
                    return null;
                }
            }
        }
        return defaultBlockState().setValue(CORE, true);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof Player player)) {
            return;
        }

        BlockPos core = corePos(pos, player);
        setBuilding(true);
        try {
            level.removeBlock(pos, false);
            level.setBlock(core, defaultBlockState().setValue(CORE, true), Block.UPDATE_ALL);
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }
                    BlockPos target = core.offset(x, 0, z);
                    level.setBlock(target, defaultBlockState().setValue(CORE, false), Block.UPDATE_ALL);
                    if (level.getBlockEntity(target) instanceof CargoElevatorBlockEntity segment) {
                        segment.setCorePos(core);
                    }
                }
            }
        } finally {
            setBuilding(false);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoElevatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.CARGO_ELEVATOR.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                CargoElevatorBlockEntity.tick(tickerLevel, tickerPos, tickerState, (CargoElevatorBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity elevator) {
            elevator.core().ifPresent(core -> core.toggleElevator());
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(this.asItem()) && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity elevator) {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            boolean extended = elevator.core().map(core -> core.addLayer(stack, player)).orElse(false);
            return extended ? ItemInteractionResult.CONSUME : ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !BUILDING.get() && level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity elevator) {
            elevator.core().ifPresent(core -> core.removeStructure());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                   BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide && !BUILDING.get() && !state.getValue(CORE)
                && (!(level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity segment)
                || segment.core().isEmpty())) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CargoElevatorBlockEntity segment) {
            CargoElevatorBlockEntity core = segment.core().orElse(segment.isCore() ? segment : null);
            if (core != null) {
                int count = core.height() + 1;
                List<ItemStack> drops = new ArrayList<>();
                while (count > 0) {
                    int stackSize = Math.min(count, 64);
                    drops.add(new ItemStack(this, stackSize));
                    count -= stackSize;
                }
                return drops;
            }
        }
        return super.getDrops(state, params);
    }

    private static BlockPos corePos(BlockPos clicked, @Nullable LivingEntity placer) {
        if (placer == null) {
            return clicked;
        }
        int quadrant = Mth.floor(placer.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        Direction facing = switch (quadrant) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
        return clicked.relative(facing.getOpposite());
    }

    public static void setBuilding(boolean building) { BUILDING.set(building); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CargoElevatorBlockEntity.shapeFor(level, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CargoElevatorBlockEntity.shapeFor(level, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CORE);
    }
}
