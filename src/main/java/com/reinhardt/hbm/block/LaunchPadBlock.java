package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LauncherBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LaunchPadBlock extends LargeMachineBlock implements EntityBlock {
    public enum Kind {
        SILO,
        LARGE,
        RUSTED
    }

    private static final List<Box> SILO_BOXES = List.of(
            new Box(-1.5D, 0.0D, -1.5D, -0.5D, 1.0D, -0.5D),
            new Box(0.5D, 0.0D, -1.5D, 1.5D, 1.0D, -0.5D),
            new Box(-1.5D, 0.0D, 0.5D, -0.5D, 1.0D, 1.5D),
            new Box(0.5D, 0.0D, 0.5D, 1.5D, 1.0D, 1.5D),
            new Box(-0.5D, 0.5D, -1.5D, 0.5D, 1.0D, 1.5D),
            new Box(-1.5D, 0.5D, -0.5D, 1.5D, 1.0D, 0.5D)
    );
    private static final List<Box> RUSTED_BOXES = List.of(
            new Box(-1.5D, 0.0D, -1.5D, -0.5D, 1.0D, -0.5D),
            new Box(0.5D, 0.0D, -1.5D, 1.5D, 1.0D, -0.5D),
            new Box(-1.5D, 0.0D, 0.5D, -0.5D, 1.0D, 1.5D),
            new Box(0.5D, 0.0D, 0.5D, 1.5D, 1.0D, 1.5D),
            new Box(-0.5D, 0.5D, -1.5D, 0.5D, 1.0D, 1.5D),
            new Box(-1.5D, 0.5D, -0.5D, 1.5D, 1.0D, 0.5D)
    );
    private static final List<Box> LARGE_BOXES = List.of(
            new Box(-4.5D, 0.0D, -4.5D, 4.5D, 1.0D, -0.5D),
            new Box(-4.5D, 0.0D, 0.5D, 4.5D, 1.0D, 4.5D),
            new Box(-4.5D, 0.875D, -0.5D, 4.5D, 1.0D, 0.5D)
    );

    private final Kind kind;

    public LaunchPadBlock(Properties properties, Kind kind) {
        super(properties,
                switch (kind) {
                    case SILO -> siloFootprint();
                    case LARGE -> largePadFootprint();
                    case RUSTED -> rustedPadFootprint();
                },
                switch (kind) {
                    case SILO -> siloShapeForPart(Direction.NORTH, BlockPos.ZERO);
                    case LARGE -> largePadShapeForPart(Direction.NORTH, BlockPos.ZERO);
                    case RUSTED -> rustedPadShapeForPart(Direction.NORTH, BlockPos.ZERO);
                },
                switch (kind) {
                    case SILO -> LaunchPadBlock::mapSiloOffset;
                    case LARGE -> LaunchPadBlock::mapLargePadOffset;
                    case RUSTED -> LaunchPadBlock::mapRustedPadOffset;
                });
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return switch (this.kind) {
            case SILO -> siloStateForPlacement(context);
            case LARGE -> largePadStateForPlacement(context);
            case RUSTED -> rustedPadStateForPlacement(context);
        };
    }

    private BlockState siloStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState largePadStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState rustedPadStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return switch (this.kind) {
            case SILO -> rotateSilo(state, rotation);
            case LARGE -> rotateLargePad(state, rotation);
            case RUSTED -> rotateRustedPad(state, rotation);
        };
    }

    private static BlockState rotateSilo(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateLargePad(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateRustedPad(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return switch (this.kind) {
            case SILO -> mirrorSilo(state, mirror);
            case LARGE -> mirrorLargePad(state, mirror);
            case RUSTED -> mirrorRustedPad(state, mirror);
        };
    }

    private static BlockState mirrorSilo(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorLargePad(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorRustedPad(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        if (!level.isClientSide) {
            switch (this.kind) {
                case SILO -> placeSiloDummies(level, pos, state.getValue(FACING), placer);
                case LARGE -> placeLargePadDummies(level, pos, state.getValue(FACING), placer);
                case RUSTED -> placeRustedPadDummies(level, pos, state.getValue(FACING), placer);
            }
        }
    }

    private void placeSiloDummies(Level level, BlockPos pos, Direction facing,
                                  net.minecraft.world.entity.LivingEntity placer) {
        placeDummies(level, pos, facing, this.machineFootprint(), LaunchPadBlock::mapSiloOffset);
        pushEntitiesOutOfFootprint(level, pos, facing, this.machineFootprint(), placer,
                LaunchPadBlock::mapSiloOffset);
    }

    private void placeLargePadDummies(Level level, BlockPos pos, Direction facing,
                                      net.minecraft.world.entity.LivingEntity placer) {
        placeDummies(level, pos, facing, this.machineFootprint(), LaunchPadBlock::mapLargePadOffset);
        pushEntitiesOutOfFootprint(level, pos, facing, this.machineFootprint(), placer,
                LaunchPadBlock::mapLargePadOffset);
    }

    private void placeRustedPadDummies(Level level, BlockPos pos, Direction facing,
                                       net.minecraft.world.entity.LivingEntity placer) {
        placeDummies(level, pos, facing, this.machineFootprint(), LaunchPadBlock::mapRustedPadOffset);
        pushEntitiesOutOfFootprint(level, pos, facing, this.machineFootprint(), placer,
                LaunchPadBlock::mapRustedPadOffset);
    }

    private static BlockPos mapSiloOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case NORTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(-z, y, x);
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(z, y, -x);
            case UP, DOWN -> throw new IllegalArgumentException("Launch silo requires a horizontal facing: " + facing);
        };
    }

    private static BlockPos mapLargePadOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case NORTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(-z, y, x);
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(z, y, -x);
            case UP, DOWN -> throw new IllegalArgumentException("Large launch pad requires a horizontal facing: " + facing);
        };
    }

    private static BlockPos mapRustedPadOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case NORTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(-z, y, x);
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(z, y, -x);
            case UP, DOWN -> throw new IllegalArgumentException("Rusted launch pad requires a horizontal facing: " + facing);
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        LauncherBlockEntity.Kind launcherKind = switch (this.kind) {
            case LARGE -> LauncherBlockEntity.Kind.PAD_LARGE;
            case RUSTED -> LauncherBlockEntity.Kind.PAD_RUSTED;
            default -> LauncherBlockEntity.Kind.PAD_SMALL;
        };
        return new LauncherBlockEntity(pos, state, launcherKind);
    }

    private static Footprint siloFootprint() {
        return Footprint.centered(1, 1, 1);
    }

    private static Footprint rustedPadFootprint() {
        return Footprint.centered(1, 1, 1);
    }

    private static Footprint largePadFootprint() {
        return Footprint.centered(4, 1, 4);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (this.kind) {
            case SILO -> siloShapeForPart(state.getValue(FACING), BlockPos.ZERO);
            case LARGE -> largePadShapeForPart(state.getValue(FACING), BlockPos.ZERO);
            case RUSTED -> rustedPadShapeForPart(state.getValue(FACING), BlockPos.ZERO);
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (this.kind) {
            case SILO -> siloShapeForPart(state.getValue(FACING), BlockPos.ZERO);
            case LARGE -> largePadShapeForPart(state.getValue(FACING), BlockPos.ZERO);
            case RUSTED -> rustedPadShapeForPart(state.getValue(FACING), BlockPos.ZERO);
        };
    }

    public static VoxelShape siloShapeForPart(Direction facing, BlockPos offsetFromCore) {
        VoxelShape shape = Shapes.empty();
        for (Box box : SILO_BOXES) {
            Box oriented = switch (facing) {
                // Exact BlockDummyable#getAABBRotationOffset result for legacy metadata 2/5/3/4.
                case NORTH -> new Box(-box.maxZ, box.minY, box.minX, -box.minZ, box.maxY, box.maxX);
                case EAST -> new Box(-box.maxX, box.minY, -box.maxZ, -box.minX, box.maxY, -box.minZ);
                case SOUTH -> new Box(box.minZ, box.minY, -box.maxX, box.maxZ, box.maxY, -box.minX);
                case WEST -> box;
                case UP, DOWN -> throw new IllegalArgumentException("Launch silo requires a horizontal facing: " + facing);
            };
            shape = Shapes.or(shape, oriented.slice(offsetFromCore));
        }
        return shape.isEmpty() ? Shapes.empty() : shape;
    }

    public static VoxelShape rustedPadShapeForPart(Direction facing, BlockPos offsetFromCore) {
        VoxelShape shape = Shapes.empty();
        for (Box box : RUSTED_BOXES) {
            Box oriented = switch (facing) {
                // Exact BlockDummyable#getAABBRotationOffset result for legacy metadata 2/5/3/4.
                case NORTH -> new Box(-box.maxZ, box.minY, box.minX, -box.minZ, box.maxY, box.maxX);
                case EAST -> new Box(-box.maxX, box.minY, -box.maxZ, -box.minX, box.maxY, -box.minZ);
                case SOUTH -> new Box(box.minZ, box.minY, -box.maxX, box.maxZ, box.maxY, -box.minX);
                case WEST -> box;
                case UP, DOWN -> throw new IllegalArgumentException("Rusted launch pad requires a horizontal facing: " + facing);
            };
            shape = Shapes.or(shape, oriented.slice(offsetFromCore));
        }
        return shape.isEmpty() ? Shapes.empty() : shape;
    }

    public static VoxelShape largePadShapeForPart(Direction facing, BlockPos offsetFromCore) {
        VoxelShape shape = Shapes.empty();
        for (Box box : LARGE_BOXES) {
            Box oriented = switch (facing) {
                // Exact BlockDummyable#getAABBRotationOffset result for legacy metadata 2/5/3/4.
                case NORTH -> new Box(-box.maxZ, box.minY, box.minX, -box.minZ, box.maxY, box.maxX);
                case EAST -> new Box(-box.maxX, box.minY, -box.maxZ, -box.minX, box.maxY, -box.minZ);
                case SOUTH -> new Box(box.minZ, box.minY, -box.maxX, box.maxZ, box.maxY, -box.minX);
                case WEST -> box;
                case UP, DOWN -> throw new IllegalArgumentException("Large launch pad requires a horizontal facing: " + facing);
            };
            shape = Shapes.or(shape, oriented.slice(offsetFromCore));
        }
        return shape.isEmpty() ? Shapes.empty() : shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && !state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof LauncherBlockEntity launcher) launcher.dropContents(level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                    net.minecraft.world.entity.player.Player player,
                                                                    net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return net.minecraft.world.InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LauncherBlockEntity launcher
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.openMenu(launcher, buffer -> buffer.writeBlockPos(pos));
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != com.reinhardt.hbm.registry.HbmBlockEntities.LAUNCHER.get()) return null;
        return (l, p, s, be) -> LauncherBlockEntity.tick(l, p, s, (LauncherBlockEntity) be);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block,
                                    BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LauncherBlockEntity launcher) launcher.updateRedstonePower(pos);
    }

    private record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        VoxelShape slice(BlockPos offset) {
            double minXLocal = clamp(this.minX - offset.getX() + 0.5D);
            double maxXLocal = clamp(this.maxX - offset.getX() + 0.5D);
            double minYLocal = clamp(this.minY - offset.getY());
            double maxYLocal = clamp(this.maxY - offset.getY());
            double minZLocal = clamp(this.minZ - offset.getZ() + 0.5D);
            double maxZLocal = clamp(this.maxZ - offset.getZ() + 0.5D);
            if (minXLocal >= maxXLocal || minYLocal >= maxYLocal || minZLocal >= maxZLocal) {
                return Shapes.empty();
            }
            return Shapes.box(minXLocal, minYLocal, minZLocal, maxXLocal, maxYLocal, maxZLocal);
        }

        private static double clamp(double value) {
            return Math.max(0.0D, Math.min(1.0D, value));
        }
    }
}
