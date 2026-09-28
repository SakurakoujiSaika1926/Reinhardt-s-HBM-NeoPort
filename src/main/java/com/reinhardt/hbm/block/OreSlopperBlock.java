package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.OreSlopperBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class OreSlopperBlock extends LargeMachineBlock implements EntityBlock {
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    public static final Footprint FOOTPRINT = Footprint.box(-1, 1, 0, 3, -3, 3);
    private static final double[][] LEGACY_BOXES_1710 = {
            {-3.5D, 0.0D, -1.5D, 3.5D, 1.0D, 1.5D},
            {0.5D, 1.0D, -1.5D, 3.5D, 3.25D, 1.5D},
            {-2.25D, 1.0D, -1.5D, 0.25D, 3.25D, -0.75D},
            {-2.25D, 1.0D, 0.75D, 0.25D, 3.25D, 1.5D},
            {-2.25D, 1.0D, -1.5D, -2.0D, 3.25D, 1.5D},
            {0.0D, 1.0D, -1.5D, 0.25D, 3.25D, 1.5D},
            {-2.0D, 1.0D, -0.75D, 0.0D, 2.0D, 0.75D},
            {-3.25D, 1.0D, -1.0D, -2.25D, 3.0D, 1.0D}
    };

    public OreSlopperBlock(Properties properties, VoxelShape shape) {
        super(properties, FOOTPRINT, shape, OreSlopperBlock::mapOreSlopperOffset);
    }

    private static BlockPos mapOreSlopperOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case NORTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case SOUTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            default -> throw new IllegalArgumentException("Ore slopper facing must be horizontal: " + facing);
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        net.minecraft.core.Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OreSlopperBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForPart(state.getValue(FACING), BlockPos.ZERO);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForPart(state.getValue(FACING), BlockPos.ZERO);
    }

    public static VoxelShape shapeForPart(Direction facing, BlockPos offsetFromCore) {
        VoxelShape shape = Shapes.empty();
        for (double[] legacyBox : LEGACY_BOXES_1710) {
            // The old collision boxes use the BlockDummyable axes while ore_slopper.obj uses (-Z, Y, X).
            Bounds objAxes = new Bounds(
                    -legacyBox[5], legacyBox[1], legacyBox[0],
                    -legacyBox[2], legacyBox[4], legacyBox[3]
            );
            Bounds oriented = switch (facing) {
                case NORTH -> objAxes;
                case EAST -> new Bounds(-objAxes.maxZ, objAxes.minY, objAxes.minX,
                        -objAxes.minZ, objAxes.maxY, objAxes.maxX);
                case SOUTH -> new Bounds(-objAxes.maxX, objAxes.minY, -objAxes.maxZ,
                        -objAxes.minX, objAxes.maxY, -objAxes.minZ);
                case WEST -> new Bounds(objAxes.minZ, objAxes.minY, -objAxes.maxX,
                        objAxes.maxZ, objAxes.maxY, -objAxes.minX);
                case UP, DOWN -> throw new IllegalArgumentException("Ore slopper facing must be horizontal: " + facing);
            };
            shape = Shapes.or(shape, oriented.slice(offsetFromCore));
        }
        return shape.isEmpty() ? Shapes.empty() : shape;
    }

    private record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        private VoxelShape slice(BlockPos offset) {
            double localMinX = clamp(this.minX - offset.getX() + 0.5D);
            double localMaxX = clamp(this.maxX - offset.getX() + 0.5D);
            double localMinY = clamp(this.minY - offset.getY());
            double localMaxY = clamp(this.maxY - offset.getY());
            double localMinZ = clamp(this.minZ - offset.getZ() + 0.5D);
            double localMaxZ = clamp(this.maxZ - offset.getZ() + 0.5D);
            if (localMinX >= localMaxX || localMinY >= localMaxY || localMinZ >= localMaxZ) {
                return Shapes.empty();
            }
            return Shapes.box(localMinX, localMinY, localMinZ, localMaxX, localMaxY, localMaxZ);
        }

        private static double clamp(double value) {
            return Math.max(0.0D, Math.min(1.0D, value));
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof OreSlopperBlockEntity slopper) {
                OreSlopperBlockEntity.tick(tickerLevel, pos, tickerState, slopper);
            }
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
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
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
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

