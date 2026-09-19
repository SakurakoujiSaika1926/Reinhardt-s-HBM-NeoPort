package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.blockentity.LegacyTurretBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyTurretType;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LegacyTurretBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private static final MapCodec<LegacyTurretBlock> CODEC = simpleCodec(properties -> new LegacyTurretBlock(properties, LegacyTurretType.FRIENDLY));
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape HALF_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    private static final ThreadLocal<Boolean> SUPPRESS_AUTOMATIC_DUMMIES = ThreadLocal.withInitial(() -> false);
    private final LegacyTurretType type;

    public LegacyTurretBlock(Properties properties, LegacyTurretType type) {
        super(LargeMachineBlock.nonOccludingMachineProperties(properties));
        this.type = type;
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public LegacyTurretType type() {
        return type;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!canPlaceAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LegacyTurretBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return blockEntityType == HbmBlockEntities.LEGACY_TURRET.get()
                ? (tickerLevel, pos, tickerState, blockEntity) -> LegacyTurretBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (LegacyTurretBlockEntity) blockEntity
        )
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || !this.type.hasMenu()) {
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
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!SUPPRESS_AUTOMATIC_DUMMIES.get() && !level.isClientSide && !state.is(oldState.getBlock())) {
            placeDummies(level, pos, state.getValue(FACING));
            PowerNetworkManager.markDirty(level);
        }
    }

    /**
     * A 1.7.10 NBTStructure writes every saved turret part itself.  Suppress
     * normal item-placement expansion while that exact saved layout is loaded.
     */
    public static void runWithoutAutomaticDummies(Runnable action) {
        boolean previous = SUPPRESS_AUTOMATIC_DUMMIES.get();
        SUPPRESS_AUTOMATIC_DUMMIES.set(true);
        try {
            action.run();
        } finally {
            SUPPRESS_AUTOMATIC_DUMMIES.set(previous);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            LargeMachineBlock.pushEntitiesOutOfPositions(level, pos, state.getValue(FACING), occupiedPositions(pos, state.getValue(FACING), this.type), placer);
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
                removeDummies(level, pos, state.getValue(FACING));
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return type.layout() == LegacyTurretType.Layout.NT ? HALF_SHAPE : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public boolean canPlaceAt(Level level, BlockPos corePos, Direction facing, BlockPlaceContext context) {
        for (BlockPos pos : occupiedPositions(corePos, facing, this.type)) {
            if (pos.equals(corePos)) {
                continue;
            }
            if (!level.getBlockState(pos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private void placeDummies(Level level, BlockPos corePos, Direction facing) {
        if (!this.type.hasDummies()) {
            return;
        }
        for (BlockPos pos : occupiedPositions(corePos, facing, this.type)) {
            if (pos.equals(corePos)) {
                continue;
            }
            level.setBlock(pos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    private void removeDummies(Level level, BlockPos corePos, Direction facing) {
        if (!this.type.hasDummies()) {
            return;
        }
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos pos : occupiedPositions(corePos, facing, this.type)) {
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

    public static List<BlockPos> occupiedPositions(BlockPos corePos, Direction facing, LegacyTurretType type) {
        if (type.layout() == LegacyTurretType.Layout.SENTRY) {
            return List.of(corePos);
        }
        if (type.layout() == LegacyTurretType.Layout.NT) {
            return ntPositions(corePos, facing);
        }
        return artilleryPositions(corePos, facing);
    }

    private static List<BlockPos> ntPositions(BlockPos corePos, Direction facing) {
        ArrayList<BlockPos> positions = new ArrayList<>(4);
        switch (facing) {
            case NORTH -> {
                positions.add(corePos);
                positions.add(corePos.offset(1, 0, 0));
                positions.add(corePos.offset(0, 0, 1));
                positions.add(corePos.offset(1, 0, 1));
            }
            case SOUTH -> {
                positions.add(corePos);
                positions.add(corePos.offset(-1, 0, 0));
                positions.add(corePos.offset(0, 0, -1));
                positions.add(corePos.offset(-1, 0, -1));
            }
            case EAST -> {
                positions.add(corePos);
                positions.add(corePos.offset(-1, 0, 0));
                positions.add(corePos.offset(0, 0, 1));
                positions.add(corePos.offset(-1, 0, 1));
            }
            default -> {
                positions.add(corePos);
                positions.add(corePos.offset(1, 0, 0));
                positions.add(corePos.offset(0, 0, -1));
                positions.add(corePos.offset(1, 0, -1));
            }
        }
        return positions;
    }

    private static List<BlockPos> artilleryPositions(BlockPos corePos, Direction facing) {
        int[] dim = rotateDimensions(new int[]{1, 0, 2, 1, 2, 1}, facing);
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int x = -dim[4]; x <= dim[5]; x++) {
            for (int y = -dim[1]; y <= dim[0]; y++) {
                for (int z = -dim[2]; z <= dim[3]; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
        }
        return positions;
    }

    private static int[] rotateDimensions(int[] dim, Direction facing) {
        return switch (facing) {
            case SOUTH -> dim;
            case NORTH -> new int[]{dim[0], dim[1], dim[3], dim[2], dim[5], dim[4]};
            case EAST -> new int[]{dim[0], dim[1], dim[5], dim[4], dim[2], dim[3]};
            case WEST -> new int[]{dim[0], dim[1], dim[4], dim[5], dim[3], dim[2]};
            default -> dim;
        };
    }
}
