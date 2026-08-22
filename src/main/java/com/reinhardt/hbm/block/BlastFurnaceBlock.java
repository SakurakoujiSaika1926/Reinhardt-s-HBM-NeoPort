package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.BlastFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
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

import javax.annotation.Nullable;

public class BlastFurnaceBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty EXTENDED = BooleanProperty.create("extended");

    private final boolean defaultLit;

    public BlastFurnaceBlock(Properties properties, boolean defaultLit) {
        super(properties);
        this.defaultLit = defaultLit;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, defaultLit)
                .setValue(EXTENDED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, this.defaultLit)
                .setValue(EXTENDED, isExtension(context.getLevel(), context.getClickedPos().above()));
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (direction == Direction.UP) {
            return state.setValue(EXTENDED, isExtension(level, neighborPos));
        }
        return state;
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlastFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTicker(blockEntityType, HbmBlockEntities.BLAST_FURNACE.get(), BlastFurnaceBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
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

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(HbmBlocks.MACHINE_DIFURNACE_OFF.get());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }

        Direction facing = state.getValue(FACING);
        double x0 = pos.getX() + 0.5D;
        double y0 = pos.getY() + 0.25D + random.nextDouble() * 0.375D;
        double z0 = pos.getZ() + 0.5D;
        double sideOff = 0.52D;
        double sideRand = random.nextDouble() * 0.5D - 0.25D;
        double xOff = random.nextDouble() * 0.375D + 0.3125D;
        double zOff = random.nextDouble() * 0.375D + 0.3125D;

        switch (facing) {
            case WEST -> level.addParticle(ParticleTypes.FLAME, x0 - sideOff, y0, z0 + sideRand, 0.0D, 0.0D, 0.0D);
            case EAST -> level.addParticle(ParticleTypes.FLAME, x0 + sideOff, y0, z0 + sideRand, 0.0D, 0.0D, 0.0D);
            case NORTH -> level.addParticle(ParticleTypes.FLAME, x0 + sideRand, y0, z0 - sideOff, 0.0D, 0.0D, 0.0D);
            case SOUTH -> level.addParticle(ParticleTypes.FLAME, x0 + sideRand, y0, z0 + sideOff, 0.0D, 0.0D, 0.0D);
            default -> {
            }
        }

        double smokeY = pos.getY() + (state.getValue(EXTENDED) ? 2.0D : 1.0D);
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + xOff, smokeY, pos.getZ() + zOff, 0.0D, 0.0D, 0.0D);
    }

    public static void refreshExtensionState(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(EXTENDED)) {
            boolean extended = isExtension(level, pos.above());
            if (state.getValue(EXTENDED) != extended) {
                level.setBlock(pos, state.setValue(EXTENDED, extended), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean isExtension(LevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos).is(HbmBlocks.MACHINE_DIFURNACE_EXT.get());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, EXTENDED);
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(
            BlockEntityType<A> actual,
            BlockEntityType<E> expected,
            BlockEntityTicker<? super E> ticker
    ) {
        return actual == expected ? (BlockEntityTicker<A>) ticker : null;
    }
}
