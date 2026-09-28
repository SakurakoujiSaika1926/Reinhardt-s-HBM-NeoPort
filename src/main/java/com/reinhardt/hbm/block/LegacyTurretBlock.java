package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.blockentity.LegacyTurretBlockEntity;
import com.reinhardt.hbm.blockentity.ClientRenderNeighborSnapshot;
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
import net.minecraft.world.level.LevelReader;
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
        return switch (this.type) {
            case FRIENDLY -> friendlyStateForPlacement(context);
            case FRITZ -> fritzStateForPlacement(context);
            case HOWARD -> howardStateForPlacement(context);
            case HOWARD_DAMAGED -> howardDamagedStateForPlacement(context);
            case MAXWELL -> maxwellStateForPlacement(context);
            case RICHARD -> richardStateForPlacement(context);
            case TAUON -> tauonStateForPlacement(context);
            case ARTY -> artyStateForPlacement(context);
            case HIMARS -> himarsStateForPlacement(context);
            case SENTRY -> sentryStateForPlacement(context);
            case SENTRY_DAMAGED -> sentryDamagedStateForPlacement(context);
        };
    }

    private BlockState friendlyStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos pos : friendlyPositions(context.getClickedPos(), facing))
            if (!pos.equals(context.getClickedPos()) && !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    private BlockState fritzStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos pos : fritzPositions(context.getClickedPos(), facing))
            if (!pos.equals(context.getClickedPos()) && !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    private BlockState howardStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos pos : howardPositions(context.getClickedPos(), facing))
            if (!pos.equals(context.getClickedPos()) && !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    private BlockState howardDamagedStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos pos : howardDamagedPositions(context.getClickedPos(), facing))
            if (!pos.equals(context.getClickedPos()) && !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    private BlockState maxwellStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos pos : maxwellPositions(context.getClickedPos(), facing))
            if (!pos.equals(context.getClickedPos()) && !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    private BlockState richardStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos pos : richardPositions(context.getClickedPos(), facing))
            if (!pos.equals(context.getClickedPos()) && !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    private BlockState tauonStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos pos : tauonPositions(context.getClickedPos(), facing))
            if (!pos.equals(context.getClickedPos()) && !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    private BlockState artyStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos pos : artyPositions(context.getClickedPos(), facing))
            if (!pos.equals(context.getClickedPos()) && !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    private BlockState himarsStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos pos : himarsPositions(context.getClickedPos(), facing))
            if (!pos.equals(context.getClickedPos()) && !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    private BlockState sentryStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos pos : sentryPositions(context.getClickedPos(), facing))
            if (!pos.equals(context.getClickedPos()) && !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    private BlockState sentryDamagedStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (BlockPos pos : sentryDamagedPositions(context.getClickedPos(), facing))
            if (!pos.equals(context.getClickedPos()) && !context.getLevel().getBlockState(pos).canBeReplaced(context)) return null;
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
            placeDummies(level, pos, state.getValue(FACING), state);
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
                removeDummies(level, pos, state.getValue(FACING), state);
            }
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        ClientRenderNeighborSnapshot.refreshAt(level, pos);
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighborPos) {
        ClientRenderNeighborSnapshot.refreshAt(level, pos);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (this.type) {
            case FRIENDLY -> rotateFriendly(state, rotation);
            case FRITZ -> rotateFritz(state, rotation);
            case HOWARD -> rotateHoward(state, rotation);
            case HOWARD_DAMAGED -> rotateHowardDamaged(state, rotation);
            case MAXWELL -> rotateMaxwell(state, rotation);
            case RICHARD -> rotateRichard(state, rotation);
            case TAUON -> rotateTauon(state, rotation);
            case ARTY -> rotateArty(state, rotation);
            case HIMARS -> rotateHimars(state, rotation);
            case SENTRY -> rotateSentry(state, rotation);
            case SENTRY_DAMAGED -> rotateSentryDamaged(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.type) {
            case FRIENDLY -> mirrorFriendly(state, mirror);
            case FRITZ -> mirrorFritz(state, mirror);
            case HOWARD -> mirrorHoward(state, mirror);
            case HOWARD_DAMAGED -> mirrorHowardDamaged(state, mirror);
            case MAXWELL -> mirrorMaxwell(state, mirror);
            case RICHARD -> mirrorRichard(state, mirror);
            case TAUON -> mirrorTauon(state, mirror);
            case ARTY -> mirrorArty(state, mirror);
            case HIMARS -> mirrorHimars(state, mirror);
            case SENTRY -> mirrorSentry(state, mirror);
            case SENTRY_DAMAGED -> mirrorSentryDamaged(state, mirror);
        };
    }

    private static BlockState rotateFriendly(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateFritz(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateHoward(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateHowardDamaged(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateMaxwell(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateRichard(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateTauon(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateArty(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateHimars(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateSentry(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateSentryDamaged(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }

    private static BlockState mirrorFriendly(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorFritz(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorHoward(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorHowardDamaged(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorMaxwell(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorRichard(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorTauon(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorArty(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorHimars(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorSentry(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorSentryDamaged(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape authoredShape = switch (type) {
            case FRIENDLY -> HALF_SHAPE;
            case FRITZ -> HALF_SHAPE;
            case HOWARD -> HALF_SHAPE;
            case HOWARD_DAMAGED -> HALF_SHAPE;
            case MAXWELL -> HALF_SHAPE;
            case RICHARD -> HALF_SHAPE;
            case TAUON -> HALF_SHAPE;
            case ARTY -> Shapes.block();
            case HIMARS -> Shapes.block();
            case SENTRY -> Shapes.block();
            case SENTRY_DAMAGED -> Shapes.block();
        };
        return authoredShape;
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

    private void placeDummies(Level level, BlockPos corePos, Direction facing, BlockState coreState) {
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

    private void removeDummies(Level level, BlockPos corePos, Direction facing, BlockState coreState) {
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
        return switch (type) {
            case FRIENDLY -> friendlyPositions(corePos, facing);
            case FRITZ -> fritzPositions(corePos, facing);
            case HOWARD -> howardPositions(corePos, facing);
            case HOWARD_DAMAGED -> howardDamagedPositions(corePos, facing);
            case MAXWELL -> maxwellPositions(corePos, facing);
            case RICHARD -> richardPositions(corePos, facing);
            case TAUON -> tauonPositions(corePos, facing);
            case ARTY -> artyPositions(corePos, facing);
            case HIMARS -> himarsPositions(corePos, facing);
            case SENTRY -> sentryPositions(corePos, facing);
            case SENTRY_DAMAGED -> sentryDamagedPositions(corePos, facing);
        };
    }

    private static List<BlockPos> friendlyPositions(BlockPos corePos, Direction facing) {
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

    private static List<BlockPos> fritzPositions(BlockPos corePos, Direction facing) {
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

    private static List<BlockPos> howardPositions(BlockPos corePos, Direction facing) {
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

    private static List<BlockPos> howardDamagedPositions(BlockPos corePos, Direction facing) {
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

    private static List<BlockPos> maxwellPositions(BlockPos corePos, Direction facing) {
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

    private static List<BlockPos> richardPositions(BlockPos corePos, Direction facing) {
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

    private static List<BlockPos> tauonPositions(BlockPos corePos, Direction facing) {
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

    private static List<BlockPos> artyPositions(BlockPos corePos, Direction facing) {
        ArrayList<BlockPos> positions = new ArrayList<>(32);
        switch (facing) {
            case SOUTH -> {
                for (int x = -2; x <= 1; x++) for (int y = 0; y <= 1; y++) for (int z = -2; z <= 1; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
            case NORTH -> {
                for (int x = -1; x <= 2; x++) for (int y = 0; y <= 1; y++) for (int z = -1; z <= 2; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
            case EAST -> {
                for (int x = -2; x <= 1; x++) for (int y = 0; y <= 1; y++) for (int z = -1; z <= 2; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
            default -> {
                for (int x = -1; x <= 2; x++) for (int y = 0; y <= 1; y++) for (int z = -2; z <= 1; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
        }
        return positions;
    }

    private static List<BlockPos> himarsPositions(BlockPos corePos, Direction facing) {
        ArrayList<BlockPos> positions = new ArrayList<>(32);
        switch (facing) {
            case SOUTH -> {
                for (int x = -2; x <= 1; x++) for (int y = 0; y <= 1; y++) for (int z = -2; z <= 1; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
            case NORTH -> {
                for (int x = -1; x <= 2; x++) for (int y = 0; y <= 1; y++) for (int z = -1; z <= 2; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
            case EAST -> {
                for (int x = -2; x <= 1; x++) for (int y = 0; y <= 1; y++) for (int z = -1; z <= 2; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
            default -> {
                for (int x = -1; x <= 2; x++) for (int y = 0; y <= 1; y++) for (int z = -2; z <= 1; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
        }
        return positions;
    }

    private static List<BlockPos> sentryPositions(BlockPos corePos, Direction facing) {
        return List.of(corePos);
    }

    private static List<BlockPos> sentryDamagedPositions(BlockPos corePos, Direction facing) {
        return List.of(corePos);
    }
}
