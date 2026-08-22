package com.reinhardt.hbm.block;

import com.reinhardt.hbm.entity.ConveyorMovingItem;
import com.reinhardt.hbm.item.ConveyorWandItem;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.blockentity.ConveyorPressBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** Direct 1.7.10 conveyor family port; FACING is the old input direction. */
public final class ConveyorBlock extends Block {
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final IntegerProperty CURVE = IntegerProperty.create("curve", 0, 2);
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");

    private static final VoxelShape BELT_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);
    private static final VoxelShape LIFT_TOP_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
    private final Kind kind;

    public ConveyorBlock(Kind kind) {
        super(Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CURVE, 0)
                .setValue(BOTTOM, true)
                .setValue(TOP, false)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false));
    }

    public Kind kind() { return this.kind; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return refreshVisualState(
                this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()),
                context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return refreshVisualState(state, level, pos);
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
        // Block invokes this method from its constructor, before ConveyorBlock.kind is assigned.
        builder.add(FACING, CURVE, BOTTOM, TOP, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(level, pos);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(level, pos);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.empty(); }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ItemEntity item) || item.tickCount <= 10 || item.isRemoved()) return;
        ConveyorMovingItem moving = new ConveyorMovingItem(level, item.getItem().copy());
        Vec3 snap = closestSnappingPosition(level, pos, state, item.position());
        moving.setPos(snap.x, snap.y, snap.z);
        level.addFreshEntity(moving);
        item.discard();
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ConveyorWandItem.ConveyorType type = switch (this.kind) {
            case EXPRESS -> ConveyorWandItem.ConveyorType.EXPRESS;
            case DOUBLE -> ConveyorWandItem.ConveyorType.DOUBLE;
            case TRIPLE -> ConveyorWandItem.ConveyorType.TRIPLE;
            default -> ConveyorWandItem.ConveyorType.REGULAR;
        };
        return List.of(ConveyorWandItem.stackFor(HbmItems.CONVEYOR_WAND, type));
    }

    public Direction inputDirection(BlockState state) {
        return switch (this.kind) {
            case LIFT -> Direction.DOWN;
            case CHUTE -> Direction.UP;
            default -> state.getValue(FACING);
        };
    }

    public Direction outputDirection(BlockState state) {
        return switch (this.kind) {
            case LIFT -> Direction.UP;
            case CHUTE -> Direction.DOWN;
            default -> {
                Direction primary = state.getValue(FACING).getOpposite();
                int curve = curve(state);
                yield curve == 1 ? LegacyMachineGeometry.forgeRotateDown(primary)
                        : curve == 2 ? LegacyMachineGeometry.forgeRotateUp(primary) : primary;
            }
        };
    }

    public Direction travelDirection(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        if (this.kind == Kind.LIFT) return liftTop(level, pos) ? state.getValue(FACING) : Direction.DOWN;
        if (this.kind == Kind.CHUTE) {
            if (isConveyorAt(level, pos.below()) || itemPos.y > pos.getY() + 0.25D) return Direction.UP;
            return state.getValue(FACING);
        }
        Direction primary = state.getValue(FACING);
        int curve = curve(state);
        if (curve > 0) {
            int lane = curve - 1;
            Direction secondary = LegacyMachineGeometry.forgeRotateUp(primary);
            double pivotX = pos.getX() + 0.5D - (-primary.getStepX() * 0.5D + secondary.getStepX() * (0.5D - lane));
            double pivotZ = pos.getZ() + 0.5D - (-primary.getStepZ() * 0.5D + secondary.getStepZ() * (0.5D - lane));
            if (Math.abs(itemPos.x - pivotX) + Math.abs(itemPos.z - pivotZ) >= 1.0D) return lane == 0 ? secondary.getOpposite() : secondary;
        }
        return primary;
    }

    public Vec3 closestSnappingPosition(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        if (this.kind == Kind.LIFT && !liftTop(level, pos)) return new Vec3(pos.getX() + 0.5D, itemPos.y, pos.getZ() + 0.5D);
        if (this.kind == Kind.CHUTE && (isConveyorAt(level, pos.below()) || itemPos.y > pos.getY() + 0.25D)) {
            return new Vec3(pos.getX() + 0.5D, itemPos.y, pos.getZ() + 0.5D);
        }
        Direction direction = travelDirection(level, pos, state, itemPos);
        double x = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1.0D);
        double z = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1.0D);
        double snappedX = direction.getStepX() != 0 ? x : pos.getX() + 0.5D;
        double snappedZ = direction.getStepZ() != 0 ? z : pos.getZ() + 0.5D;
        if (this.kind == Kind.DOUBLE) {
            if (direction.getStepX() != 0) snappedZ += z > snappedZ ? 0.25D : -0.25D;
            if (direction.getStepZ() != 0) snappedX += x > snappedX ? 0.25D : -0.25D;
        } else if (this.kind == Kind.TRIPLE) {
            if (direction.getStepX() != 0) snappedZ += z > snappedZ + 0.15D ? 0.3125D : z < snappedZ - 0.15D ? -0.3125D : 0.0D;
            if (direction.getStepZ() != 0) snappedX += x > snappedX + 0.15D ? 0.3125D : x < snappedX - 0.15D ? -0.3125D : 0.0D;
        }
        return new Vec3(snappedX, pos.getY() + 0.25D, snappedZ);
    }

    public Vec3 travelLocation(Level level, BlockPos pos, BlockState state, Vec3 itemPos, double speed) {
        if (this.kind == Kind.EXPRESS) speed *= 3.0D;
        if (this.kind == Kind.CHUTE) {
            if (isConveyorAt(level, pos.below())) speed *= 5.0D;
            else if (itemPos.y > pos.getY() + 0.25D) speed *= 3.0D;
        }
        Direction direction = travelDirection(level, pos, state, itemPos);
        Vec3 snap = closestSnappingPosition(level, pos, state, itemPos);
        Vec3 destination = snap.subtract(direction.getStepX() * speed, direction.getStepY() * speed, direction.getStepZ() * speed);
        Vec3 delta = destination.subtract(itemPos);
        return delta.length() <= 1.0E-6D ? destination : itemPos.add(delta.scale(speed / delta.length()));
    }

    public static boolean configureWithScrewdriver(Level level, BlockPos pos, Player player, ItemStack stack,
                                                    net.minecraft.world.InteractionHand hand) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ConveyorBlock conveyor)) return false;
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            if (!player.isShiftKeyDown()) {
                level.setBlock(pos, state.setValue(FACING, LegacyMachineGeometry.forgeRotateUp(facing)), Block.UPDATE_ALL);
            } else if (conveyor.kind == Kind.LIFT) {
                level.setBlock(pos, HbmBlocks.CONVEYOR_CHUTE.get().defaultBlockState().setValue(FACING, facing), Block.UPDATE_ALL);
            } else if (conveyor.kind == Kind.CHUTE) {
                level.setBlock(pos, HbmBlocks.CONVEYOR.get().defaultBlockState().setValue(FACING, facing).setValue(CURVE, 0), Block.UPDATE_ALL);
            } else {
                int curve = curve(state);
                if (conveyor.kind == Kind.REGULAR && curve == 2) {
                    level.setBlock(pos, HbmBlocks.CONVEYOR_LIFT.get().defaultBlockState().setValue(FACING, facing), Block.UPDATE_ALL);
                } else {
                    level.setBlock(pos, state.setValue(CURVE, (curve + 1) % 3), Block.UPDATE_ALL);
                }
            }
            ScrewdriverItem.damageTool(stack, level, player, hand);
        }
        return true;
    }

    private VoxelShape shapeFor(BlockGetter level, BlockPos pos) {
        if (this.kind == Kind.LIFT) return liftTop(level, pos) ? LIFT_TOP_SHAPE : Shapes.block();
        return this.kind == Kind.CHUTE ? Shapes.block() : BELT_SHAPE;
    }

    private static boolean liftTop(BlockGetter level, BlockPos pos) {
        boolean bottom = !isConveyorAt(level, pos.below());
        return !isConveyorAt(level, pos.above()) && !bottom;
    }

    public static boolean isConveyor(BlockState state) { return state.getBlock() instanceof ConveyorBlock; }

    private static boolean isConveyorAt(BlockGetter level, BlockPos pos) {
        if (isConveyor(level.getBlockState(pos))) return true;
        return level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && dummy.core() instanceof ConveyorPressBlockEntity press
                && press.isBeltPosition(pos);
    }

    private BlockState refreshVisualState(BlockState state, BlockGetter level, BlockPos pos) {
        if (this.kind == Kind.LIFT) {
            boolean bottom = !isConveyorAt(level, pos.below());
            boolean top = !bottom && !isConveyorAt(level, pos.above());
            return state.setValue(BOTTOM, bottom).setValue(TOP, top);
        }
        if (this.kind == Kind.CHUTE) {
            return state.setValue(BOTTOM, !isConveyorAt(level, pos.below()))
                    .setValue(NORTH, isConveyorAt(level, pos.north()))
                    .setValue(EAST, isConveyorAt(level, pos.east()))
                    .setValue(SOUTH, isConveyorAt(level, pos.south()))
                    .setValue(WEST, isConveyorAt(level, pos.west()));
        }
        return state;
    }
    private static int curve(BlockState state) { return state.hasProperty(CURVE) ? state.getValue(CURVE) : 0; }

    public enum Kind {
        REGULAR(true), EXPRESS(true), DOUBLE(true), TRIPLE(true), LIFT(false), CHUTE(false);
        private final boolean bendable;
        Kind(boolean bendable) { this.bendable = bendable; }
        public boolean bendable() { return bendable; }
    }
}
