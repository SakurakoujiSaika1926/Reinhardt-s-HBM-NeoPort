package com.reinhardt.hbm.block;

import com.reinhardt.hbm.entity.ConveyorMovingItem;
import com.reinhardt.hbm.item.ConveyorWandItem;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.blockentity.ConveyorPressBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
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

    private static final VoxelShape REGULAR_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);
    private static final VoxelShape EXPRESS_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);
    private static final VoxelShape DOUBLE_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);
    private static final VoxelShape TRIPLE_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);
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
        return switch (this.kind) {
            case REGULAR -> regularPlacementState(context);
            case EXPRESS -> expressPlacementState(context);
            case DOUBLE -> doublePlacementState(context);
            case TRIPLE -> triplePlacementState(context);
            case LIFT -> liftPlacementState(context);
            case CHUTE -> chutePlacementState(context);
        };
    }

    private BlockState regularPlacementState(BlockPlaceContext context) {
        return regularVisualState(this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()), context.getLevel(), context.getClickedPos());
    }

    private BlockState expressPlacementState(BlockPlaceContext context) {
        return expressVisualState(this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()), context.getLevel(), context.getClickedPos());
    }

    private BlockState doublePlacementState(BlockPlaceContext context) {
        return doubleVisualState(this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()), context.getLevel(), context.getClickedPos());
    }

    private BlockState triplePlacementState(BlockPlaceContext context) {
        return tripleVisualState(this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()), context.getLevel(), context.getClickedPos());
    }

    private BlockState liftPlacementState(BlockPlaceContext context) {
        return liftVisualState(this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()), context.getLevel(), context.getClickedPos());
    }

    private BlockState chutePlacementState(BlockPlaceContext context) {
        return chuteVisualState(this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return switch (this.kind) {
            case REGULAR -> regularVisualState(state, level, pos);
            case EXPRESS -> expressVisualState(state, level, pos);
            case DOUBLE -> doubleVisualState(state, level, pos);
            case TRIPLE -> tripleVisualState(state, level, pos);
            case LIFT -> liftVisualState(state, level, pos);
            case CHUTE -> chuteVisualState(state, level, pos);
        };
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (this.kind) {
            case REGULAR -> rotateRegular(state, rotation);
            case EXPRESS -> rotateExpress(state, rotation);
            case DOUBLE -> rotateDouble(state, rotation);
            case TRIPLE -> rotateTriple(state, rotation);
            case LIFT -> rotateLift(state, rotation);
            case CHUTE -> rotateChute(state, rotation);
        };
    }

    private static BlockState rotateRegular(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateExpress(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateDouble(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateTriple(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateLift(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    private static BlockState rotateChute(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.kind) {
            case REGULAR -> mirrorRegular(state, mirror);
            case EXPRESS -> mirrorExpress(state, mirror);
            case DOUBLE -> mirrorDouble(state, mirror);
            case TRIPLE -> mirrorTriple(state, mirror);
            case LIFT -> mirrorLift(state, mirror);
            case CHUTE -> mirrorChute(state, mirror);
        };
    }

    private static BlockState mirrorRegular(BlockState state, Mirror mirror) { return rotateRegular(state, mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorExpress(BlockState state, Mirror mirror) { return rotateExpress(state, mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorDouble(BlockState state, Mirror mirror) { return rotateDouble(state, mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorTriple(BlockState state, Mirror mirror) { return rotateTriple(state, mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorLift(BlockState state, Mirror mirror) { return rotateLift(state, mirror.getRotation(state.getValue(FACING))); }
    private static BlockState mirrorChute(BlockState state, Mirror mirror) { return rotateChute(state, mirror.getRotation(state.getValue(FACING))); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // Block invokes this method from its constructor, before ConveyorBlock.kind is assigned.
        builder.add(FACING, CURVE, BOTTOM, TOP, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (this.kind) {
            case REGULAR -> regularSelectionShape(state, level, pos);
            case EXPRESS -> expressSelectionShape(state, level, pos);
            case DOUBLE -> doubleSelectionShape(state, level, pos);
            case TRIPLE -> tripleSelectionShape(state, level, pos);
            case LIFT -> liftSelectionShape(state, level, pos);
            case CHUTE -> chuteSelectionShape(state, level, pos);
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (this.kind) {
            case REGULAR -> regularCollisionShape(state, level, pos);
            case EXPRESS -> expressCollisionShape(state, level, pos);
            case DOUBLE -> doubleCollisionShape(state, level, pos);
            case TRIPLE -> tripleCollisionShape(state, level, pos);
            case LIFT -> liftCollisionShape(state, level, pos);
            case CHUTE -> chuteCollisionShape(state, level, pos);
        };
    }

    private static VoxelShape regularSelectionShape(BlockState state, BlockGetter level, BlockPos pos) { return REGULAR_SHAPE; }
    private static VoxelShape expressSelectionShape(BlockState state, BlockGetter level, BlockPos pos) { return EXPRESS_SHAPE; }
    private static VoxelShape doubleSelectionShape(BlockState state, BlockGetter level, BlockPos pos) { return DOUBLE_SHAPE; }
    private static VoxelShape tripleSelectionShape(BlockState state, BlockGetter level, BlockPos pos) { return TRIPLE_SHAPE; }
    private static VoxelShape liftSelectionShape(BlockState state, BlockGetter level, BlockPos pos) { return liftTop(level, pos) ? LIFT_TOP_SHAPE : Shapes.block(); }
    private static VoxelShape chuteSelectionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.block(); }

    private static VoxelShape regularCollisionShape(BlockState state, BlockGetter level, BlockPos pos) { return REGULAR_SHAPE; }
    private static VoxelShape expressCollisionShape(BlockState state, BlockGetter level, BlockPos pos) { return EXPRESS_SHAPE; }
    private static VoxelShape doubleCollisionShape(BlockState state, BlockGetter level, BlockPos pos) { return DOUBLE_SHAPE; }
    private static VoxelShape tripleCollisionShape(BlockState state, BlockGetter level, BlockPos pos) { return TRIPLE_SHAPE; }
    private static VoxelShape liftCollisionShape(BlockState state, BlockGetter level, BlockPos pos) { return liftTop(level, pos) ? LIFT_TOP_SHAPE : Shapes.block(); }
    private static VoxelShape chuteCollisionShape(BlockState state, BlockGetter level, BlockPos pos) { return Shapes.block(); }

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
            case REGULAR -> regularInputDirection(state);
            case EXPRESS -> expressInputDirection(state);
            case DOUBLE -> doubleInputDirection(state);
            case TRIPLE -> tripleInputDirection(state);
            case LIFT -> liftInputDirection(state);
            case CHUTE -> chuteInputDirection(state);
        };
    }

    private static Direction regularInputDirection(BlockState state) { return state.getValue(FACING); }
    private static Direction expressInputDirection(BlockState state) { return state.getValue(FACING); }
    private static Direction doubleInputDirection(BlockState state) { return state.getValue(FACING); }
    private static Direction tripleInputDirection(BlockState state) { return state.getValue(FACING); }
    private static Direction liftInputDirection(BlockState state) { return Direction.DOWN; }
    private static Direction chuteInputDirection(BlockState state) { return Direction.UP; }

    public Direction outputDirection(BlockState state) {
        return switch (this.kind) {
            case REGULAR -> regularOutputDirection(state);
            case EXPRESS -> expressOutputDirection(state);
            case DOUBLE -> doubleOutputDirection(state);
            case TRIPLE -> tripleOutputDirection(state);
            case LIFT -> liftOutputDirection(state);
            case CHUTE -> chuteOutputDirection(state);
        };
    }

    private static Direction regularOutputDirection(BlockState state) {
        Direction primary = state.getValue(FACING).getOpposite();
        int curve = state.hasProperty(CURVE) ? state.getValue(CURVE) : 0;
        return curve == 1 ? regularCurveOneOutputDirection(primary)
                : curve == 2 ? regularCurveTwoOutputDirection(primary) : primary;
    }

    private static Direction regularCurveOneOutputDirection(Direction primary) {
        return switch (primary) {
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            default -> throw new IllegalArgumentException("Regular conveyor curve requires a horizontal direction: " + primary);
        };
    }

    private static Direction regularCurveTwoOutputDirection(Direction primary) {
        return switch (primary) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Regular conveyor curve requires a horizontal direction: " + primary);
        };
    }

    private static Direction expressOutputDirection(BlockState state) {
        Direction primary = state.getValue(FACING).getOpposite();
        int curve = state.hasProperty(CURVE) ? state.getValue(CURVE) : 0;
        return curve == 1 ? expressCurveOneOutputDirection(primary)
                : curve == 2 ? expressCurveTwoOutputDirection(primary) : primary;
    }

    private static Direction expressCurveOneOutputDirection(Direction primary) {
        return switch (primary) {
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            default -> throw new IllegalArgumentException("Express conveyor curve requires a horizontal direction: " + primary);
        };
    }

    private static Direction expressCurveTwoOutputDirection(Direction primary) {
        return switch (primary) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Express conveyor curve requires a horizontal direction: " + primary);
        };
    }

    private static Direction doubleOutputDirection(BlockState state) {
        Direction primary = state.getValue(FACING).getOpposite();
        int curve = state.hasProperty(CURVE) ? state.getValue(CURVE) : 0;
        return curve == 1 ? doubleCurveOneOutputDirection(primary)
                : curve == 2 ? doubleCurveTwoOutputDirection(primary) : primary;
    }

    private static Direction doubleCurveOneOutputDirection(Direction primary) {
        return switch (primary) {
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            default -> throw new IllegalArgumentException("Double conveyor curve requires a horizontal direction: " + primary);
        };
    }

    private static Direction doubleCurveTwoOutputDirection(Direction primary) {
        return switch (primary) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Double conveyor curve requires a horizontal direction: " + primary);
        };
    }

    private static Direction tripleOutputDirection(BlockState state) {
        Direction primary = state.getValue(FACING).getOpposite();
        int curve = state.hasProperty(CURVE) ? state.getValue(CURVE) : 0;
        return curve == 1 ? tripleCurveOneOutputDirection(primary)
                : curve == 2 ? tripleCurveTwoOutputDirection(primary) : primary;
    }

    private static Direction tripleCurveOneOutputDirection(Direction primary) {
        return switch (primary) {
            case NORTH -> Direction.WEST;
            case EAST -> Direction.NORTH;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            default -> throw new IllegalArgumentException("Triple conveyor curve requires a horizontal direction: " + primary);
        };
    }

    private static Direction tripleCurveTwoOutputDirection(Direction primary) {
        return switch (primary) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Triple conveyor curve requires a horizontal direction: " + primary);
        };
    }

    private static Direction liftOutputDirection(BlockState state) { return Direction.UP; }
    private static Direction chuteOutputDirection(BlockState state) { return Direction.DOWN; }

    public Direction travelDirection(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        return switch (this.kind) {
            case REGULAR -> regularTravelDirection(level, pos, state, itemPos);
            case EXPRESS -> expressTravelDirection(level, pos, state, itemPos);
            case DOUBLE -> doubleTravelDirection(level, pos, state, itemPos);
            case TRIPLE -> tripleTravelDirection(level, pos, state, itemPos);
            case LIFT -> liftTravelDirection(level, pos, state, itemPos);
            case CHUTE -> chuteTravelDirection(level, pos, state, itemPos);
        };
    }

    private static Direction regularTravelDirection(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        Direction primary = state.getValue(FACING);
        int curve = state.hasProperty(CURVE) ? state.getValue(CURVE) : 0;
        if (curve > 0) {
            int lane = curve - 1;
            Direction secondary = regularCurvePivotSide(primary);
            double pivotX = pos.getX() + 0.5D - (-primary.getStepX() * 0.5D + secondary.getStepX() * (0.5D - lane));
            double pivotZ = pos.getZ() + 0.5D - (-primary.getStepZ() * 0.5D + secondary.getStepZ() * (0.5D - lane));
            if (Math.abs(itemPos.x - pivotX) + Math.abs(itemPos.z - pivotZ) >= 1.0D) return lane == 0 ? secondary.getOpposite() : secondary;
        }
        return primary;
    }

    private static Direction regularCurvePivotSide(Direction primary) {
        return switch (primary) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Regular conveyor travel requires a horizontal direction: " + primary);
        };
    }

    private static Direction expressTravelDirection(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        Direction primary = state.getValue(FACING);
        int curve = state.hasProperty(CURVE) ? state.getValue(CURVE) : 0;
        if (curve > 0) {
            int lane = curve - 1;
            Direction secondary = expressCurvePivotSide(primary);
            double pivotX = pos.getX() + 0.5D - (-primary.getStepX() * 0.5D + secondary.getStepX() * (0.5D - lane));
            double pivotZ = pos.getZ() + 0.5D - (-primary.getStepZ() * 0.5D + secondary.getStepZ() * (0.5D - lane));
            if (Math.abs(itemPos.x - pivotX) + Math.abs(itemPos.z - pivotZ) >= 1.0D) return lane == 0 ? secondary.getOpposite() : secondary;
        }
        return primary;
    }

    private static Direction expressCurvePivotSide(Direction primary) {
        return switch (primary) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Express conveyor travel requires a horizontal direction: " + primary);
        };
    }

    private static Direction doubleTravelDirection(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        Direction primary = state.getValue(FACING);
        int curve = state.hasProperty(CURVE) ? state.getValue(CURVE) : 0;
        if (curve > 0) {
            int lane = curve - 1;
            Direction secondary = doubleCurvePivotSide(primary);
            double pivotX = pos.getX() + 0.5D - (-primary.getStepX() * 0.5D + secondary.getStepX() * (0.5D - lane));
            double pivotZ = pos.getZ() + 0.5D - (-primary.getStepZ() * 0.5D + secondary.getStepZ() * (0.5D - lane));
            if (Math.abs(itemPos.x - pivotX) + Math.abs(itemPos.z - pivotZ) >= 1.0D) return lane == 0 ? secondary.getOpposite() : secondary;
        }
        return primary;
    }

    private static Direction doubleCurvePivotSide(Direction primary) {
        return switch (primary) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Double conveyor travel requires a horizontal direction: " + primary);
        };
    }

    private static Direction tripleTravelDirection(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        Direction primary = state.getValue(FACING);
        int curve = state.hasProperty(CURVE) ? state.getValue(CURVE) : 0;
        if (curve > 0) {
            int lane = curve - 1;
            Direction secondary = tripleCurvePivotSide(primary);
            double pivotX = pos.getX() + 0.5D - (-primary.getStepX() * 0.5D + secondary.getStepX() * (0.5D - lane));
            double pivotZ = pos.getZ() + 0.5D - (-primary.getStepZ() * 0.5D + secondary.getStepZ() * (0.5D - lane));
            if (Math.abs(itemPos.x - pivotX) + Math.abs(itemPos.z - pivotZ) >= 1.0D) return lane == 0 ? secondary.getOpposite() : secondary;
        }
        return primary;
    }

    private static Direction tripleCurvePivotSide(Direction primary) {
        return switch (primary) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Triple conveyor travel requires a horizontal direction: " + primary);
        };
    }

    private static Direction liftTravelDirection(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        return liftTop(level, pos) ? state.getValue(FACING) : Direction.DOWN;
    }

    private static Direction chuteTravelDirection(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        if (acceptsChuteOutput(level, pos.below()) || itemPos.y > pos.getY() + 0.25D) return Direction.UP;
        return state.getValue(FACING);
    }

    public Vec3 closestSnappingPosition(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        return switch (this.kind) {
            case REGULAR -> regularSnappingPosition(level, pos, state, itemPos);
            case EXPRESS -> expressSnappingPosition(level, pos, state, itemPos);
            case DOUBLE -> doubleSnappingPosition(level, pos, state, itemPos);
            case TRIPLE -> tripleSnappingPosition(level, pos, state, itemPos);
            case LIFT -> liftSnappingPosition(level, pos, state, itemPos);
            case CHUTE -> chuteSnappingPosition(level, pos, state, itemPos);
        };
    }

    private static Vec3 regularSnappingPosition(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        Direction direction = regularTravelDirection(level, pos, state, itemPos);
        double x = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1.0D);
        double z = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1.0D);
        double snappedX = direction.getStepX() != 0 ? x : pos.getX() + 0.5D;
        double snappedZ = direction.getStepZ() != 0 ? z : pos.getZ() + 0.5D;
        return new Vec3(snappedX, pos.getY() + 0.25D, snappedZ);
    }

    private static Vec3 expressSnappingPosition(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        Direction direction = expressTravelDirection(level, pos, state, itemPos);
        double x = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1.0D);
        double z = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1.0D);
        double snappedX = direction.getStepX() != 0 ? x : pos.getX() + 0.5D;
        double snappedZ = direction.getStepZ() != 0 ? z : pos.getZ() + 0.5D;
        return new Vec3(snappedX, pos.getY() + 0.25D, snappedZ);
    }

    private static Vec3 doubleSnappingPosition(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        Direction direction = doubleTravelDirection(level, pos, state, itemPos);
        double x = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1.0D);
        double z = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1.0D);
        double snappedX = direction.getStepX() != 0 ? x : pos.getX() + 0.5D;
        double snappedZ = direction.getStepZ() != 0 ? z : pos.getZ() + 0.5D;
        if (direction.getStepX() != 0) snappedZ += z > snappedZ ? 0.25D : -0.25D;
        if (direction.getStepZ() != 0) snappedX += x > snappedX ? 0.25D : -0.25D;
        return new Vec3(snappedX, pos.getY() + 0.25D, snappedZ);
    }

    private static Vec3 tripleSnappingPosition(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        Direction direction = tripleTravelDirection(level, pos, state, itemPos);
        double x = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1.0D);
        double z = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1.0D);
        double snappedX = direction.getStepX() != 0 ? x : pos.getX() + 0.5D;
        double snappedZ = direction.getStepZ() != 0 ? z : pos.getZ() + 0.5D;
        if (direction.getStepX() != 0) snappedZ += z > snappedZ + 0.15D ? 0.3125D : z < snappedZ - 0.15D ? -0.3125D : 0.0D;
        if (direction.getStepZ() != 0) snappedX += x > snappedX + 0.15D ? 0.3125D : x < snappedX - 0.15D ? -0.3125D : 0.0D;
        return new Vec3(snappedX, pos.getY() + 0.25D, snappedZ);
    }

    private static Vec3 liftSnappingPosition(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        if (!liftTop(level, pos)) return new Vec3(pos.getX() + 0.5D, itemPos.y, pos.getZ() + 0.5D);
        Direction direction = liftTravelDirection(level, pos, state, itemPos);
        double x = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1.0D);
        double z = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1.0D);
        double snappedX = direction.getStepX() != 0 ? x : pos.getX() + 0.5D;
        double snappedZ = direction.getStepZ() != 0 ? z : pos.getZ() + 0.5D;
        return new Vec3(snappedX, pos.getY() + 0.25D, snappedZ);
    }

    private static Vec3 chuteSnappingPosition(Level level, BlockPos pos, BlockState state, Vec3 itemPos) {
        if (acceptsChuteOutput(level, pos.below()) || itemPos.y > pos.getY() + 0.25D) {
            return new Vec3(pos.getX() + 0.5D, itemPos.y, pos.getZ() + 0.5D);
        }
        Direction direction = chuteTravelDirection(level, pos, state, itemPos);
        double x = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1.0D);
        double z = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1.0D);
        double snappedX = direction.getStepX() != 0 ? x : pos.getX() + 0.5D;
        double snappedZ = direction.getStepZ() != 0 ? z : pos.getZ() + 0.5D;
        return new Vec3(snappedX, pos.getY() + 0.25D, snappedZ);
    }

    public Vec3 travelLocation(Level level, BlockPos pos, BlockState state, Vec3 itemPos, double speed) {
        return switch (this.kind) {
            case REGULAR -> regularTravelLocation(level, pos, state, itemPos, speed);
            case EXPRESS -> expressTravelLocation(level, pos, state, itemPos, speed);
            case DOUBLE -> doubleTravelLocation(level, pos, state, itemPos, speed);
            case TRIPLE -> tripleTravelLocation(level, pos, state, itemPos, speed);
            case LIFT -> liftTravelLocation(level, pos, state, itemPos, speed);
            case CHUTE -> chuteTravelLocation(level, pos, state, itemPos, speed);
        };
    }

    private static Vec3 regularTravelLocation(Level level, BlockPos pos, BlockState state, Vec3 itemPos, double speed) {
        Direction direction = regularTravelDirection(level, pos, state, itemPos);
        Vec3 snap = regularSnappingPosition(level, pos, state, itemPos);
        Vec3 destination = snap.subtract(direction.getStepX() * speed, direction.getStepY() * speed, direction.getStepZ() * speed);
        Vec3 delta = destination.subtract(itemPos);
        return delta.length() <= 1.0E-6D ? destination : itemPos.add(delta.scale(speed / delta.length()));
    }

    private static Vec3 expressTravelLocation(Level level, BlockPos pos, BlockState state, Vec3 itemPos, double speed) {
        speed *= 3.0D;
        Direction direction = expressTravelDirection(level, pos, state, itemPos);
        Vec3 snap = expressSnappingPosition(level, pos, state, itemPos);
        Vec3 destination = snap.subtract(direction.getStepX() * speed, direction.getStepY() * speed, direction.getStepZ() * speed);
        Vec3 delta = destination.subtract(itemPos);
        return delta.length() <= 1.0E-6D ? destination : itemPos.add(delta.scale(speed / delta.length()));
    }

    private static Vec3 doubleTravelLocation(Level level, BlockPos pos, BlockState state, Vec3 itemPos, double speed) {
        Direction direction = doubleTravelDirection(level, pos, state, itemPos);
        Vec3 snap = doubleSnappingPosition(level, pos, state, itemPos);
        Vec3 destination = snap.subtract(direction.getStepX() * speed, direction.getStepY() * speed, direction.getStepZ() * speed);
        Vec3 delta = destination.subtract(itemPos);
        return delta.length() <= 1.0E-6D ? destination : itemPos.add(delta.scale(speed / delta.length()));
    }

    private static Vec3 tripleTravelLocation(Level level, BlockPos pos, BlockState state, Vec3 itemPos, double speed) {
        Direction direction = tripleTravelDirection(level, pos, state, itemPos);
        Vec3 snap = tripleSnappingPosition(level, pos, state, itemPos);
        Vec3 destination = snap.subtract(direction.getStepX() * speed, direction.getStepY() * speed, direction.getStepZ() * speed);
        Vec3 delta = destination.subtract(itemPos);
        return delta.length() <= 1.0E-6D ? destination : itemPos.add(delta.scale(speed / delta.length()));
    }

    private static Vec3 liftTravelLocation(Level level, BlockPos pos, BlockState state, Vec3 itemPos, double speed) {
        Direction direction = liftTravelDirection(level, pos, state, itemPos);
        Vec3 snap = liftSnappingPosition(level, pos, state, itemPos);
        Vec3 destination = snap.subtract(direction.getStepX() * speed, direction.getStepY() * speed, direction.getStepZ() * speed);
        Vec3 delta = destination.subtract(itemPos);
        return delta.length() <= 1.0E-6D ? destination : itemPos.add(delta.scale(speed / delta.length()));
    }

    private static Vec3 chuteTravelLocation(Level level, BlockPos pos, BlockState state, Vec3 itemPos, double speed) {
        if (acceptsChuteOutput(level, pos.below())) speed *= 5.0D;
        else if (itemPos.y > pos.getY() + 0.25D) speed *= 3.0D;
        Direction direction = chuteTravelDirection(level, pos, state, itemPos);
        Vec3 snap = chuteSnappingPosition(level, pos, state, itemPos);
        Vec3 destination = snap.subtract(direction.getStepX() * speed, direction.getStepY() * speed, direction.getStepZ() * speed);
        Vec3 delta = destination.subtract(itemPos);
        return delta.length() <= 1.0E-6D ? destination : itemPos.add(delta.scale(speed / delta.length()));
    }

    public static boolean configureWithScrewdriver(Level level, BlockPos pos, Player player, ItemStack stack,
                                                    net.minecraft.world.InteractionHand hand) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ConveyorBlock conveyor)) return false;
        if (!level.isClientSide) {
            switch (conveyor.kind) {
                case REGULAR -> configureRegular(level, pos, state, player.isShiftKeyDown());
                case EXPRESS -> configureExpress(level, pos, state, player.isShiftKeyDown());
                case DOUBLE -> configureDouble(level, pos, state, player.isShiftKeyDown());
                case TRIPLE -> configureTriple(level, pos, state, player.isShiftKeyDown());
                case LIFT -> configureLift(level, pos, state, player.isShiftKeyDown());
                case CHUTE -> configureChute(level, pos, state, player.isShiftKeyDown());
            }
            refreshVisualStateAt(level, pos);
            ScrewdriverItem.damageTool(stack, level, player, hand);
        }
        return true;
    }

    private static void configureRegular(Level level, BlockPos pos, BlockState state, boolean shifted) {
        Direction facing = state.getValue(FACING);
        if (!shifted) {
            level.setBlock(pos, state.setValue(FACING, nextRegularScrewdriverFacing(facing)), Block.UPDATE_ALL);
            return;
        }
        int curve = state.hasProperty(CURVE) ? state.getValue(CURVE) : 0;
        if (curve == 2) level.setBlock(pos, HbmBlocks.CONVEYOR_LIFT.get().defaultBlockState().setValue(FACING, facing), Block.UPDATE_ALL);
        else level.setBlock(pos, state.setValue(CURVE, (curve + 1) % 3), Block.UPDATE_ALL);
    }

    private static Direction nextRegularScrewdriverFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Regular conveyor screwdriver rotation requires a horizontal facing: " + facing);
        };
    }

    private static void configureExpress(Level level, BlockPos pos, BlockState state, boolean shifted) {
        Direction facing = state.getValue(FACING);
        if (!shifted) level.setBlock(pos, state.setValue(FACING, nextExpressScrewdriverFacing(facing)), Block.UPDATE_ALL);
        else {
            int curve = state.hasProperty(CURVE) ? state.getValue(CURVE) : 0;
            level.setBlock(pos, state.setValue(CURVE, (curve + 1) % 3), Block.UPDATE_ALL);
        }
    }

    private static Direction nextExpressScrewdriverFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Express conveyor screwdriver rotation requires a horizontal facing: " + facing);
        };
    }

    private static void configureDouble(Level level, BlockPos pos, BlockState state, boolean shifted) {
        Direction facing = state.getValue(FACING);
        if (!shifted) level.setBlock(pos, state.setValue(FACING, nextDoubleScrewdriverFacing(facing)), Block.UPDATE_ALL);
        else {
            int curve = state.hasProperty(CURVE) ? state.getValue(CURVE) : 0;
            level.setBlock(pos, state.setValue(CURVE, (curve + 1) % 3), Block.UPDATE_ALL);
        }
    }

    private static Direction nextDoubleScrewdriverFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Double conveyor screwdriver rotation requires a horizontal facing: " + facing);
        };
    }

    private static void configureTriple(Level level, BlockPos pos, BlockState state, boolean shifted) {
        Direction facing = state.getValue(FACING);
        if (!shifted) level.setBlock(pos, state.setValue(FACING, nextTripleScrewdriverFacing(facing)), Block.UPDATE_ALL);
        else {
            int curve = state.hasProperty(CURVE) ? state.getValue(CURVE) : 0;
            level.setBlock(pos, state.setValue(CURVE, (curve + 1) % 3), Block.UPDATE_ALL);
        }
    }

    private static Direction nextTripleScrewdriverFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Triple conveyor screwdriver rotation requires a horizontal facing: " + facing);
        };
    }

    private static void configureLift(Level level, BlockPos pos, BlockState state, boolean shifted) {
        Direction facing = state.getValue(FACING);
        if (!shifted) level.setBlock(pos, state.setValue(FACING, nextLiftScrewdriverFacing(facing)), Block.UPDATE_ALL);
        else level.setBlock(pos, HbmBlocks.CONVEYOR_CHUTE.get().defaultBlockState().setValue(FACING, facing), Block.UPDATE_ALL);
    }

    private static Direction nextLiftScrewdriverFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Lift conveyor screwdriver rotation requires a horizontal facing: " + facing);
        };
    }

    private static void configureChute(Level level, BlockPos pos, BlockState state, boolean shifted) {
        Direction facing = state.getValue(FACING);
        if (!shifted) level.setBlock(pos, state.setValue(FACING, nextChuteScrewdriverFacing(facing)), Block.UPDATE_ALL);
        else level.setBlock(pos, HbmBlocks.CONVEYOR.get().defaultBlockState().setValue(FACING, facing).setValue(CURVE, 0), Block.UPDATE_ALL);
    }

    private static Direction nextChuteScrewdriverFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> throw new IllegalArgumentException("Chute conveyor screwdriver rotation requires a horizontal facing: " + facing);
        };
    }

    private static boolean liftTop(BlockGetter level, BlockPos pos) {
        boolean bottom = !isConveyorAt(level, pos.below());
        return !isConveyorAt(level, pos.above()) && !bottom && !isLegacyEnterableAt(level, pos.above());
    }

    public static boolean isConveyor(BlockState state) { return state.getBlock() instanceof ConveyorBlock; }

    private static boolean isConveyorAt(BlockGetter level, BlockPos pos) {
        if (isConveyor(level.getBlockState(pos))) return true;
        if (level.getBlockState(pos).getBlock() instanceof CraneMachineBlock crane
                && (crane.kind() == CraneMachineBlock.Kind.PARTITIONER
                || crane.kind() == CraneMachineBlock.Kind.SPLITTER)) return true;
        return level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && dummy.core() instanceof ConveyorPressBlockEntity press
                && press.isBeltPosition(pos);
    }

    /** Blocks that implemented IEnterableBlock in 1.7.10. */
    private static boolean isLegacyEnterableAt(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockState(pos).getBlock() instanceof CraneMachineBlock crane)) return false;
        return switch (crane.kind()) {
            case BOXER, INSERTER, PARTITIONER, ROUTER, SPLITTER, UNBOXER -> true;
            case EXTRACTOR, GRABBER -> false;
        };
    }

    private static boolean acceptsChuteOutput(BlockGetter level, BlockPos pos) {
        return isConveyorAt(level, pos) || isLegacyEnterableAt(level, pos);
    }

    private BlockState refreshVisualState(BlockState state, BlockGetter level, BlockPos pos) {
        return switch (this.kind) {
            case REGULAR -> regularVisualState(state, level, pos);
            case EXPRESS -> expressVisualState(state, level, pos);
            case DOUBLE -> doubleVisualState(state, level, pos);
            case TRIPLE -> tripleVisualState(state, level, pos);
            case LIFT -> liftVisualState(state, level, pos);
            case CHUTE -> chuteVisualState(state, level, pos);
        };
    }

    private static BlockState regularVisualState(BlockState state, BlockGetter level, BlockPos pos) { return state; }
    private static BlockState expressVisualState(BlockState state, BlockGetter level, BlockPos pos) { return state; }
    private static BlockState doubleVisualState(BlockState state, BlockGetter level, BlockPos pos) { return state; }
    private static BlockState tripleVisualState(BlockState state, BlockGetter level, BlockPos pos) { return state; }

    private static BlockState liftVisualState(BlockState state, BlockGetter level, BlockPos pos) {
        boolean bottom = !isConveyorAt(level, pos.below());
        boolean top = !bottom && !isConveyorAt(level, pos.above()) && !isLegacyEnterableAt(level, pos.above());
        return state.setValue(BOTTOM, bottom).setValue(TOP, top);
    }

    private static BlockState chuteVisualState(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(BOTTOM, !acceptsChuteOutput(level, pos.below()))
                .setValue(NORTH, isConveyorAt(level, pos.north()))
                .setValue(EAST, isConveyorAt(level, pos.east()))
                .setValue(SOUTH, isConveyorAt(level, pos.south()))
                .setValue(WEST, isConveyorAt(level, pos.west()));
    }

    public static void refreshVisualStateAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ConveyorBlock conveyor)) return;
        BlockState refreshed = conveyor.refreshVisualState(state, level, pos);
        if (!refreshed.equals(state)) level.setBlock(pos, refreshed, Block.UPDATE_CLIENTS);
    }
    public enum Kind {
        REGULAR(true), EXPRESS(true), DOUBLE(true), TRIPLE(true), LIFT(false), CHUTE(false);
        private final boolean bendable;
        Kind(boolean bendable) { this.bendable = bendable; }
        public boolean bendable() { return bendable; }
    }
}
