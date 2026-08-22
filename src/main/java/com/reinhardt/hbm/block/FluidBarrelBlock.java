package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FluidBarrelBlockEntity;
import com.reinhardt.hbm.blockentity.FluidTankBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.List;

public class FluidBarrelBlock extends Block implements EntityBlock {
    public static final BooleanProperty CONN_POS_X = BooleanProperty.create("conn_pos_x");
    public static final BooleanProperty CONN_NEG_X = BooleanProperty.create("conn_neg_x");
    public static final BooleanProperty CONN_POS_Z = BooleanProperty.create("conn_pos_z");
    public static final BooleanProperty CONN_NEG_Z = BooleanProperty.create("conn_neg_z");

    private static final VoxelShape BARREL_SHAPE = Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);
    private final Kind kind;

    public FluidBarrelBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(CONN_POS_X, false)
                .setValue(CONN_NEG_X, false)
                .setValue(CONN_POS_Z, false)
                .setValue(CONN_NEG_Z, false));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.kind.hasBlockEntity() ? new FluidBarrelBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != HbmBlockEntities.FLUID_BARREL.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof FluidBarrelBlockEntity barrel) {
                FluidBarrelBlockEntity.tick(tickerLevel, pos, tickerState, barrel);
            }
        };
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BARREL_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BARREL_SHAPE;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        refreshConnections(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        refreshConnections(level, pos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity barrel) {
            barrel.loadFromItem(stack);
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof FluidBarrelBlockEntity barrel) {
            ItemStack stack = new ItemStack(this);
            barrel.saveToItem(stack);
            return List.of(stack);
        }
        return super.getDrops(state, params);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            level.invalidateCapabilities(pos);
            refreshNeighborPipes(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!(stack.getItem() instanceof FluidIdentifierItem) || !player.isShiftKeyDown() || !this.kind.hasBlockEntity()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            tank.setType(FluidIdentifierItem.primary(stack));
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!this.kind.hasBlockEntity()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
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

    public static void refreshConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidBarrelBlock)) {
            return;
        }
        FluidTankBlockEntity tank = level.getBlockEntity(pos) instanceof FluidTankBlockEntity fluidTank ? fluidTank : null;
        boolean hasFluidType = tank != null && !tank.tank().type().isNone();
        BlockState next = state
                .setValue(CONN_POS_X, hasFluidType && canConnect(level, pos, Direction.EAST, tank))
                .setValue(CONN_NEG_X, hasFluidType && canConnect(level, pos, Direction.WEST, tank))
                .setValue(CONN_POS_Z, hasFluidType && canConnect(level, pos, Direction.SOUTH, tank))
                .setValue(CONN_NEG_Z, hasFluidType && canConnect(level, pos, Direction.NORTH, tank));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    private static boolean canConnect(LevelAccessor level, BlockPos pos, Direction direction, FluidTankBlockEntity tank) {
        BlockPos target = pos.relative(direction);
        if (level instanceof Level realLevel) {
            IFluidHandler handler = realLevel.getCapability(Capabilities.FluidHandler.BLOCK, target, direction.getOpposite());
            if (handler == null) {
                return false;
            }
            FluidStack probe = HbmFluids.toNeoStack(tank.tank().type(), 1);
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack stored = handler.getFluidInTank(i);
                if ((stored.isEmpty() || FluidStack.isSameFluidSameComponents(stored, probe)) && handler.isFluidValid(i, probe)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void refreshNeighborPipes(LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.getBlock() instanceof FluidDuctBlock duct) {
                duct.refreshConnections(level, neighbor);
            }
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return this.kind.hasBlockEntity();
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FluidBarrelBlockEntity barrel) || barrel.tank().amount() <= 0) {
            return 0;
        }
        double fraction = (double) barrel.tank().amount() / (double) Math.max(1, barrel.tank().capacity());
        return Math.min(15, (int) Math.floor(fraction * 15.0D) + 1);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONN_POS_X, CONN_NEG_X, CONN_POS_Z, CONN_NEG_Z);
    }

    public enum Kind {
        PLASTIC(12_000, true),
        CORRODED(6_000, false),
        STEEL(16_000, true),
        TCALLOY(24_000, true),
        ANTIMATTER(16_000, true);

        private final int capacity;
        private final boolean hasBlockEntity;

        Kind(int capacity, boolean hasBlockEntity) {
            this.capacity = capacity;
            this.hasBlockEntity = hasBlockEntity;
        }

        public int capacity() {
            return this.capacity;
        }

        public boolean hasBlockEntity() {
            return this.hasBlockEntity;
        }
    }
}
