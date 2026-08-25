package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.ElectricFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.ShredderBlockEntity;
import com.reinhardt.hbm.blockentity.WoodBurnerBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class PowerMachineBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final MachineType type;

    public PowerMachineBlock(Properties properties, MachineType type) {
        super(properties);
        this.type = type;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, this.type == MachineType.ELECTRIC_FURNACE_ON);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (this.type) {
            case WOOD_BURNER -> new WoodBurnerBlockEntity(pos, state);
            case ELECTRIC_FURNACE, ELECTRIC_FURNACE_ON -> new ElectricFurnaceBlockEntity(pos, state);
            case SHREDDER -> new ShredderBlockEntity(pos, state);
        };
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide && this.type != MachineType.WOOD_BURNER) {
            return null;
        }

        return switch (this.type) {
            case WOOD_BURNER -> (tickerLevel, pos, tickerState, blockEntity) -> {
                if (blockEntity instanceof WoodBurnerBlockEntity woodBurner) {
                    WoodBurnerBlockEntity.tick(tickerLevel, pos, tickerState, woodBurner);
                }
            };
            case ELECTRIC_FURNACE, ELECTRIC_FURNACE_ON -> (tickerLevel, pos, tickerState, blockEntity) -> {
                if (blockEntity instanceof ElectricFurnaceBlockEntity electricFurnace) {
                    ElectricFurnaceBlockEntity.tick(tickerLevel, pos, tickerState, electricFurnace);
                }
            };
            case SHREDDER -> (tickerLevel, pos, tickerState, blockEntity) -> {
                if (blockEntity instanceof ShredderBlockEntity shredder) {
                    ShredderBlockEntity.tick(tickerLevel, pos, tickerState, shredder);
                }
            };
        };
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
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if ((this.type != MachineType.ELECTRIC_FURNACE && this.type != MachineType.ELECTRIC_FURNACE_ON)
                || !state.getValue(LIT)) {
            return;
        }

        Direction facing = state.getValue(FACING);
        double sideOffset = 0.52D;
        double sideRandom = random.nextFloat() * 0.6D - 0.3D;
        double y = pos.getY() + random.nextFloat() * 6.0D / 16.0D;
        double x = pos.getX() + 0.5D + facing.getStepX() * sideOffset
                + (facing.getAxis() == Direction.Axis.Z ? sideRandom : 0.0D);
        double z = pos.getZ() + 0.5D + facing.getStepZ() * sideOffset
                + (facing.getAxis() == Direction.Axis.X ? sideRandom : 0.0D);
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    public enum MachineType {
        WOOD_BURNER,
        ELECTRIC_FURNACE,
        ELECTRIC_FURNACE_ON,
        SHREDDER
    }
}
