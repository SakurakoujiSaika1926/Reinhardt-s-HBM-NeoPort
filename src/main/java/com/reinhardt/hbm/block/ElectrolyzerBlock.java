package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.ElectrolyzerBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public class ElectrolyzerBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final VoxelShape shape;

    public ElectrolyzerBlock(Properties properties, VoxelShape shape) {
        super(LargeMachineBlock.nonOccludingMachineProperties(properties));
        this.shape = shape;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        return canPlaceOldElectrolyzer(context, context.getClickedPos(), facing)
                ? defaultBlockState().setValue(FACING, facing)
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            placeOldElectrolyzerDummies(level, pos, state.getValue(FACING));
            LargeMachineBlock.pushEntitiesOutOfPositions(level, pos, state.getValue(FACING), oldElectrolyzerPositions(pos, state.getValue(FACING)), placer);
            refreshPorts(level, pos);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectrolyzerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.ELECTROLYZER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> ElectrolyzerBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (ElectrolyzerBlockEntity) blockEntity
        );
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
        if (blockEntity instanceof ElectrolyzerBlockEntity electrolyzer && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(electrolyzer, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeVarInt(electrolyzer.selectedGui());
            });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        if (removed && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        if (removed && !level.isClientSide) {
            removeOldElectrolyzerDummies(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (removed) {
            refreshPorts(level, pos, state);
        }
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
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private static void refreshPorts(Level level, BlockPos corePos) {
        refreshPorts(level, corePos, level.getBlockState(corePos));
    }

    private static void refreshPorts(Level level, BlockPos corePos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        for (ElectrolyzerBlockEntity.Port port : ElectrolyzerBlockEntity.portsFor(corePos, state)) {
            EnergyCableBlock.refreshConnections(level, port.connectorPos());
            CatalyticCrackerBlock.refreshDuctsAtPort(level, port.pos(), port.connectorPos());
        }
    }

    public static BlockPos legacyCorePos(BlockPos clickedPos, Direction facing) {
        return clickedPos.relative(facing, -5);
    }

    public static boolean canPlaceLegacyAt(Level level, BlockPos clickedPos, Direction facing, BlockPlaceContext context) {
        BlockPos corePos = legacyCorePos(clickedPos, facing);
        if (!level.getWorldBorder().isWithinBounds(corePos)) {
            return false;
        }
        if (!level.getBlockState(corePos).canBeReplaced(context)) {
            return false;
        }
        return canPlaceOldElectrolyzer(context, corePos, facing);
    }

    public void placeLegacyDummies(Level level, BlockPos corePos, Direction facing, @Nullable LivingEntity placer) {
        placeOldElectrolyzerDummies(level, corePos, facing);
        LargeMachineBlock.pushEntitiesOutOfPositions(level, corePos, facing, oldElectrolyzerPositions(corePos, facing), placer);
        refreshPorts(level, corePos);
    }

    private static boolean canPlaceOldElectrolyzer(BlockPlaceContext context, BlockPos corePos, Direction facing) {
        for (BlockPos pos : oldElectrolyzerPositions(corePos, facing)) {
            if (pos.equals(corePos)) {
                continue;
            }
            if (!context.getLevel().getBlockState(pos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private static void placeOldElectrolyzerDummies(Level level, BlockPos corePos, Direction facing) {
        for (BlockPos pos : oldElectrolyzerPositions(corePos, facing)) {
            if (pos.equals(corePos)) {
                continue;
            }
            level.setBlock(pos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    private static void removeOldElectrolyzerDummies(Level level, BlockPos corePos, Direction facing) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos pos : occupiedPositions(corePos, facing)) {
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

    public static Set<BlockPos> occupiedPositions(BlockPos corePos, Direction facing) {
        LinkedHashSet<BlockPos> authoredOffsets = new LinkedHashSet<>();
        addSouthBox(authoredOffsets, BlockPos.ZERO, 0, 0, 5, 5, 1, 3);
        addSouthBox(authoredOffsets, BlockPos.ZERO, 2, -1, 5, 5, 1, 1);
        addSouthBox(authoredOffsets, BlockPos.ZERO, 3, -3, 5, 5, 0, 0);
        addSouthBox(authoredOffsets, BlockPos.ZERO, 3, -1, 4, -4, -3, 3);
        addSouthBox(authoredOffsets, BlockPos.ZERO, 3, -1, 2, -2, -3, 3);
        addSouthBox(authoredOffsets, BlockPos.ZERO, 3, -1, 0, 0, -3, 3);
        addSouthBox(authoredOffsets, BlockPos.ZERO, 3, -1, -2, 2, -3, 3);
        addSouthBox(authoredOffsets, BlockPos.ZERO, 3, -1, -4, 4, -3, 3);
        addSouthBox(authoredOffsets, new BlockPos(0, 3, 4), 0, 0, 0, 0, -1, 2);
        addSouthBox(authoredOffsets, new BlockPos(0, 3, 2), 0, 0, 0, 0, -1, 2);
        addSouthBox(authoredOffsets, new BlockPos(0, 3, 0), 0, 0, 0, 0, -1, 2);
        addSouthBox(authoredOffsets, new BlockPos(0, 3, -2), 0, 0, 0, 0, -1, 2);
        addSouthBox(authoredOffsets, new BlockPos(0, 3, -4), 0, 0, 0, 0, -1, 2);
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
        for (BlockPos offset : authoredOffsets) {
            positions.add(corePos.offset(mapElectrolyzerOffset(facing, offset)).immutable());
        }
        return Set.copyOf(positions);
    }

    private static Set<BlockPos> oldElectrolyzerPositions(BlockPos corePos, Direction facing) {
        return occupiedPositions(corePos, facing);
    }

    private static void addSouthBox(Set<BlockPos> positions, BlockPos origin, int up, int down,
                                    int north, int south, int west, int east) {
        for (int x = -west; x <= east; x++) {
            for (int y = -down; y <= up; y++) {
                for (int z = -north; z <= south; z++) {
                    positions.add(origin.offset(x, y, z));
                }
            }
        }
    }

    public static BlockPos mapElectrolyzerOffset(Direction facing, BlockPos offset) {
        int x = offset.getX(); int y = offset.getY(); int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("Electrolyzer requires a horizontal facing: " + facing);
        };
    }

    public static Direction mapElectrolyzerFace(Direction facing, Direction face) {
        return switch (facing) {
            case SOUTH -> face;
            case EAST -> face == Direction.SOUTH ? Direction.EAST : face == Direction.NORTH ? Direction.WEST : face;
            case NORTH -> face == Direction.SOUTH ? Direction.NORTH : face == Direction.NORTH ? Direction.SOUTH : face;
            case WEST -> face == Direction.SOUTH ? Direction.WEST : face == Direction.NORTH ? Direction.EAST : face;
            case UP, DOWN -> throw new IllegalArgumentException("Electrolyzer requires a horizontal facing: " + facing);
        };
    }
}
