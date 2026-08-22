package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.IronFurnaceBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.SteelFurnaceBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class LegacyFurnaceBlock extends LargeMachineBlock implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final Footprint IRON_FOOTPRINT = Footprint.legacySouthBox(1, 0, 1, 0, 1, 0);
    private static final Footprint STEEL_FOOTPRINT = Footprint.legacySouthBox(1, 0, 1, 1, 1, 1);

    private final Kind kind;

    public LegacyFurnaceBlock(Properties properties, Kind kind, VoxelShape shape) {
        super(properties, kind == Kind.IRON ? IRON_FOOTPRINT : STEEL_FOOTPRINT, shape, RotationBasis.HBM_LEGACY_SOUTH);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.SOUTH)
                .setValue(LIT, false));
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Footprint footprint = this.kind == Kind.IRON ? IRON_FOOTPRINT : STEEL_FOOTPRINT;
        if (!LargeMachineBlock.canPlaceLegacyFootprint(context, facing, footprint)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(LIT, false);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.kind == Kind.IRON ? new IronFurnaceBlockEntity(pos, state) : new SteelFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (this.kind == Kind.IRON) {
            return createTicker(blockEntityType, HbmBlockEntities.IRON_FURNACE.get(), IronFurnaceBlockEntity::tick);
        }
        return createTicker(blockEntityType, HbmBlockEntities.STEEL_FURNACE.get(), SteelFurnaceBlockEntity::tick);
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
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(this.kind == Kind.IRON ? HbmBlocks.FURNACE_IRON.get() : HbmBlocks.FURNACE_STEEL.get());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        if (this.kind == Kind.IRON) {
            animateIron(level, pos, state.getValue(FACING), random);
        } else {
            animateSteel(level, pos, state.getValue(FACING), random);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    private static void animateIron(Level level, BlockPos pos, Direction facing, RandomSource random) {
        Direction side = facing.getClockWise();
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;

        double smokeOffset = random.nextBoolean() ? 1.0D : 0.5D;
        level.addParticle(
                ParticleTypes.SMOKE,
                centerX - facing.getStepX() * smokeOffset - side.getStepX() * 0.1875D,
                pos.getY() + 2.0D,
                centerZ - facing.getStepZ() * smokeOffset - side.getStepZ() * 0.1875D,
                0.0D,
                0.01D,
                0.0D
        );

        if (random.nextInt(5) == 0) {
            level.addParticle(
                    ParticleTypes.FLAME,
                    centerX + facing.getStepX() * 0.25D + side.getStepX() * random.nextDouble(),
                    pos.getY() + 0.25D + random.nextDouble() * 0.25D,
                    centerZ + facing.getStepZ() * 0.25D + side.getStepZ() * random.nextDouble(),
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    private static void animateSteel(Level level, BlockPos pos, Direction facing, RandomSource random) {
        Direction side = facing.getClockWise();
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;

        level.addParticle(
                ParticleTypes.SMOKE,
                centerX - facing.getStepX() * 1.125D - side.getStepX() * 0.75D,
                pos.getY() + 2.625D,
                centerZ - facing.getStepZ() * 1.125D - side.getStepZ() * 0.75D,
                0.0D,
                0.05D,
                0.0D
        );
        if (random.nextInt(20) == 0) {
            level.addParticle(
                    ParticleTypes.CLOUD,
                    centerX + facing.getStepX() * 0.75D,
                    pos.getY() + 2.0D,
                    centerZ + facing.getStepZ() * 0.75D,
                    0.0D,
                    0.05D,
                    0.0D
            );
        }
        if (random.nextInt(15) == 0) {
            level.addParticle(
                    ParticleTypes.LAVA,
                    centerX + facing.getStepX() * 1.5D + side.getStepX() * (random.nextDouble() - 0.5D),
                    pos.getY() + 0.75D,
                    centerZ + facing.getStepZ() * 1.5D + side.getStepZ() * (random.nextDouble() - 0.5D),
                    facing.getStepX() * 0.5D,
                    0.05D,
                    facing.getStepZ() * 0.5D
            );
        }
        if (random.nextDouble() < 0.05D) {
            level.playLocalSound(centerX, pos.getY() + 0.5D, centerZ, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 0.8F + random.nextFloat() * 0.3F, false);
        }
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(
            BlockEntityType<A> actual,
            BlockEntityType<E> expected,
            BlockEntityTicker<? super E> ticker
    ) {
        return actual == expected ? (BlockEntityTicker<A>) ticker : null;
    }

    public enum Kind {
        IRON,
        STEEL
    }
}

