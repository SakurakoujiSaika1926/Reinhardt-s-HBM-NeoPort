package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import com.reinhardt.hbm.blockentity.ClientRenderNeighborSnapshot;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.event.LegacyMobSpawnEvents;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RbmkComponentBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int DEFAULT_COLUMN_HEIGHT = 4;
    /** Legacy RBMKAutoloader dimensions {8, 0, 0, 0, 0, 0}: core plus eight blocks above it. */
    public static final int AUTOLOADER_TOP_OFFSET = 8;
    private static final VoxelShape FULL_BLOCK = Shapes.block();
    /** Legacy bounds -0.125..0.125 around the block centre for autoloader Y=0..3. */
    private static final VoxelShape AUTOLOADER_LOWER_COLUMN = Block.box(
            6.0D, 0.0D, 6.0D,
            10.0D, 16.0D, 10.0D
    );
    private static final VoxelShape PIPED_COLUMN_SHAPE = Shapes.or(
            FULL_BLOCK,
            Block.box(1.0D, 16.0D, 1.0D, 7.0D, 18.0D, 7.0D),
            Block.box(9.0D, 16.0D, 1.0D, 15.0D, 18.0D, 7.0D),
            Block.box(1.0D, 16.0D, 9.0D, 7.0D, 18.0D, 15.0D),
            Block.box(9.0D, 16.0D, 9.0D, 15.0D, 18.0D, 15.0D)
    );
    private static final VoxelShape COLUMN_WITH_LID_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 20.0D, 16.0D);
    private static final VoxelShape MINI_PANEL_SHAPE_NORTH = Block.box(0.0D, 0.0D, 4.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape MINI_PANEL_SHAPE_SOUTH = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 12.0D);
    private static final VoxelShape MINI_PANEL_SHAPE_WEST = Block.box(4.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape MINI_PANEL_SHAPE_EAST = Block.box(0.0D, 0.0D, 0.0D, 12.0D, 16.0D, 16.0D);
    private static final int[] RBMK_CONSOLE_MAIN_DIMS = new int[] {3, 0, 0, 0, 2, 2};
    private static final int[] RBMK_CONSOLE_EXTENSION_DIMS = new int[] {0, 0, 0, 1, 2, 2};
    private static final int[] RBMK_CRANE_CONSOLE_MAIN_DIMS = new int[] {1, 0, 0, 0, 1, 1};
    private static final int[] RBMK_CRANE_CONSOLE_EXTENSION_DIMS = new int[] {0, 0, 0, 1, 1, 1};

    private final Kind kind;

    public RbmkComponentBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return switch (this.kind) {
            case BLANK -> blankStateForPlacement(context);
            case FUEL_ROD -> fuelRodStateForPlacement(context);
            case FUEL_ROD_MOD -> moderatedFuelRodStateForPlacement(context);
            case FUEL_ROD_REASIM -> reasimFuelRodStateForPlacement(context);
            case FUEL_ROD_REASIM_MOD -> moderatedReasimFuelRodStateForPlacement(context);
            case CONTROL -> controlStateForPlacement(context);
            case CONTROL_AUTO -> automaticControlStateForPlacement(context);
            case CONTROL_MOD -> moderatedControlStateForPlacement(context);
            case CONTROL_REASIM -> reasimControlStateForPlacement(context);
            case CONTROL_REASIM_AUTO -> automaticReasimControlStateForPlacement(context);
            case BOILER -> boilerStateForPlacement(context);
            case HEATER -> heaterStateForPlacement(context);
            case COOLER -> coolerStateForPlacement(context);
            case LOADER -> loaderStateForPlacement(context);
            case STEAM_INLET -> steamInletStateForPlacement(context);
            case STEAM_OUTLET -> steamOutletStateForPlacement(context);
            case OUTGASSER -> outgasserStateForPlacement(context);
            case STORAGE -> storageStateForPlacement(context);
            case MODERATOR -> moderatorStateForPlacement(context);
            case REFLECTOR -> reflectorStateForPlacement(context);
            case ABSORBER -> absorberStateForPlacement(context);
            case AUTOLOADER -> autoloaderStateForPlacement(context);
            case CONSOLE -> consoleStateForPlacement(context);
            case CRANE_CONSOLE -> craneConsoleStateForPlacement(context);
            case DISPLAY -> displayStateForPlacement(context);
            case DISPLAY_BLANK -> blankDisplayStateForPlacement(context);
            case GAUGE -> gaugeStateForPlacement(context);
            case GRAPH -> graphStateForPlacement(context);
            case INDICATOR -> indicatorStateForPlacement(context);
            case KEY_PAD -> keyPadStateForPlacement(context);
            case LEVER -> leverStateForPlacement(context);
            case NUMITRON -> numitronStateForPlacement(context);
            case TERMINAL -> terminalStateForPlacement(context);
        };
    }

    private BlockState blankStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState fuelRodStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState moderatedFuelRodStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState reasimFuelRodStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState moderatedReasimFuelRodStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState controlStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState automaticControlStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState moderatedControlStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState reasimControlStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState automaticReasimControlStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState boilerStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState heaterStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState coolerStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState loaderStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState steamInletStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState steamOutletStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState outgasserStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState storageStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState moderatorStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState reflectorStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState absorberStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceColumn(context, state) ? state : null;
    }

    private BlockState autoloaderStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceAutoloaderStructure(context) ? state : null;
    }

    private BlockState consoleStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceOldDummyFootprint(context, state) ? state : null;
    }

    private BlockState craneConsoleStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return canPlaceOldDummyFootprint(context, state) ? state : null;
    }

    private BlockState displayStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState blankDisplayStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState gaugeStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState graphStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState indicatorStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState keyPadStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState leverStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState numitronStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState terminalStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && kind.isColumn()) {
            placeColumnDummies(level, pos);
            java.util.ArrayList<BlockPos> occupied = new java.util.ArrayList<>();
            for (int y = 0; y < columnHeight(level); y++) {
                occupied.add(pos.above(y));
            }
            occupied.add(pos.above(columnHeight(level)));
            LargeMachineBlock.pushEntitiesOutOfPositions(level, pos, state.getValue(FACING), occupied, placer);
        }
        if (!level.isClientSide && kind == Kind.AUTOLOADER) {
            placeAutoloaderDummies(level, pos);
            java.util.ArrayList<BlockPos> occupied = new java.util.ArrayList<>();
            for (int y = 0; y <= AUTOLOADER_TOP_OFFSET; y++) {
                occupied.add(pos.above(y));
            }
            LargeMachineBlock.pushEntitiesOutOfPositions(level, pos, state.getValue(FACING), occupied, placer);
        }
        if (!level.isClientSide && kind.hasOldDummyFootprint()) {
            java.util.Set<BlockPos> occupied = oldDummyFootprint(pos, state.getValue(FACING), kind);
            placeOldDummyFootprint(level, pos, occupied);
            LargeMachineBlock.pushEntitiesOutOfPositions(level, pos, state.getValue(FACING), occupied, placer);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return kind == Kind.LEVER || kind == Kind.KEY_PAD || kind == Kind.GAUGE || kind == Kind.NUMITRON;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof RbmkComponentBlockEntity rbmk ? rbmk.redstoneLevel() : 0;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return kind.glassLike ? 0 : super.getLightBlock(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    private VoxelShape shapeFor(BlockState state) {
        return switch (kind) {
            case BLANK -> blankShape(state);
            case FUEL_ROD -> fuelRodShape(state);
            case FUEL_ROD_MOD -> moderatedFuelRodShape(state);
            case FUEL_ROD_REASIM -> reasimFuelRodShape(state);
            case FUEL_ROD_REASIM_MOD -> moderatedReasimFuelRodShape(state);
            case CONTROL -> controlShape(state);
            case CONTROL_AUTO -> automaticControlShape(state);
            case CONTROL_MOD -> moderatedControlShape(state);
            case CONTROL_REASIM -> reasimControlShape(state);
            case CONTROL_REASIM_AUTO -> automaticReasimControlShape(state);
            case BOILER -> boilerShape(state);
            case HEATER -> heaterShape(state);
            case COOLER -> coolerShape(state);
            case LOADER -> loaderShape(state);
            case STEAM_INLET -> steamInletShape(state);
            case STEAM_OUTLET -> steamOutletShape(state);
            case OUTGASSER -> outgasserShape(state);
            case STORAGE -> storageShape(state);
            case MODERATOR -> moderatorShape(state);
            case REFLECTOR -> reflectorShape(state);
            case ABSORBER -> absorberShape(state);
            case AUTOLOADER -> autoloaderShape(state);
            case CONSOLE -> consoleShape(state);
            case CRANE_CONSOLE -> craneConsoleShape(state);
            case DISPLAY -> displayShape(state);
            case DISPLAY_BLANK -> blankDisplayShape(state);
            case GAUGE -> gaugeShape(state);
            case GRAPH -> graphShape(state);
            case INDICATOR -> indicatorShape(state);
            case KEY_PAD -> keyPadShape(state);
            case LEVER -> leverShape(state);
            case NUMITRON -> numitronShape(state);
            case TERMINAL -> terminalShape(state);
        };
    }

    public static VoxelShape columnSegmentShape(Kind kind, int yOffset, int columnHeight, boolean hasLid) {
        return switch (kind) {
            case BLANK -> blankColumnSegmentShape(yOffset, columnHeight, hasLid);
            case FUEL_ROD -> fuelRodColumnSegmentShape(yOffset, columnHeight, hasLid);
            case FUEL_ROD_MOD -> moderatedFuelRodColumnSegmentShape(yOffset, columnHeight, hasLid);
            case FUEL_ROD_REASIM -> reasimFuelRodColumnSegmentShape(yOffset, columnHeight, hasLid);
            case FUEL_ROD_REASIM_MOD -> moderatedReasimFuelRodColumnSegmentShape(yOffset, columnHeight, hasLid);
            case CONTROL -> controlColumnSegmentShape(yOffset, columnHeight, hasLid);
            case CONTROL_AUTO -> automaticControlColumnSegmentShape(yOffset, columnHeight, hasLid);
            case CONTROL_MOD -> moderatedControlColumnSegmentShape(yOffset, columnHeight, hasLid);
            case CONTROL_REASIM -> reasimControlColumnSegmentShape(yOffset, columnHeight, hasLid);
            case CONTROL_REASIM_AUTO -> automaticReasimControlColumnSegmentShape(yOffset, columnHeight, hasLid);
            case BOILER -> boilerColumnSegmentShape(yOffset, columnHeight, hasLid);
            case HEATER -> heaterColumnSegmentShape(yOffset, columnHeight, hasLid);
            case COOLER -> coolerColumnSegmentShape(yOffset, columnHeight, hasLid);
            case LOADER -> loaderColumnSegmentShape(yOffset, columnHeight, hasLid);
            case STEAM_INLET -> steamInletColumnSegmentShape(yOffset, columnHeight, hasLid);
            case STEAM_OUTLET -> steamOutletColumnSegmentShape(yOffset, columnHeight, hasLid);
            case OUTGASSER -> outgasserColumnSegmentShape(yOffset, columnHeight, hasLid);
            case STORAGE -> storageColumnSegmentShape(yOffset, columnHeight, hasLid);
            case MODERATOR -> moderatorColumnSegmentShape(yOffset, columnHeight, hasLid);
            case REFLECTOR -> reflectorColumnSegmentShape(yOffset, columnHeight, hasLid);
            case ABSORBER -> absorberColumnSegmentShape(yOffset, columnHeight, hasLid);
            case AUTOLOADER -> autoloaderColumnSegmentShape(yOffset, columnHeight, hasLid);
            case CONSOLE -> consoleColumnSegmentShape(yOffset, columnHeight, hasLid);
            case CRANE_CONSOLE -> craneConsoleColumnSegmentShape(yOffset, columnHeight, hasLid);
            case DISPLAY -> displayColumnSegmentShape(yOffset, columnHeight, hasLid);
            case DISPLAY_BLANK -> blankDisplayColumnSegmentShape(yOffset, columnHeight, hasLid);
            case GAUGE -> gaugeColumnSegmentShape(yOffset, columnHeight, hasLid);
            case GRAPH -> graphColumnSegmentShape(yOffset, columnHeight, hasLid);
            case INDICATOR -> indicatorColumnSegmentShape(yOffset, columnHeight, hasLid);
            case KEY_PAD -> keyPadColumnSegmentShape(yOffset, columnHeight, hasLid);
            case LEVER -> leverColumnSegmentShape(yOffset, columnHeight, hasLid);
            case NUMITRON -> numitronColumnSegmentShape(yOffset, columnHeight, hasLid);
            case TERMINAL -> terminalColumnSegmentShape(yOffset, columnHeight, hasLid);
        };
    }

    private static VoxelShape blankShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape fuelRodShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape moderatedFuelRodShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape reasimFuelRodShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape moderatedReasimFuelRodShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape controlShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape automaticControlShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape moderatedControlShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape reasimControlShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape automaticReasimControlShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape boilerShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape heaterShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape coolerShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape loaderShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape steamInletShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape steamOutletShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape outgasserShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape storageShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape moderatorShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape reflectorShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape absorberShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape autoloaderShape(BlockState state) {
        return AUTOLOADER_LOWER_COLUMN;
    }

    private static VoxelShape consoleShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape craneConsoleShape(BlockState state) {
        return FULL_BLOCK;
    }

    private static VoxelShape displayShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> MINI_PANEL_SHAPE_SOUTH;
            case EAST -> MINI_PANEL_SHAPE_EAST;
            case WEST -> MINI_PANEL_SHAPE_WEST;
            default -> MINI_PANEL_SHAPE_NORTH;
        };
    }

    private static VoxelShape blankDisplayShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> MINI_PANEL_SHAPE_SOUTH;
            case EAST -> MINI_PANEL_SHAPE_EAST;
            case WEST -> MINI_PANEL_SHAPE_WEST;
            default -> MINI_PANEL_SHAPE_NORTH;
        };
    }

    private static VoxelShape gaugeShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> MINI_PANEL_SHAPE_SOUTH;
            case EAST -> MINI_PANEL_SHAPE_EAST;
            case WEST -> MINI_PANEL_SHAPE_WEST;
            default -> MINI_PANEL_SHAPE_NORTH;
        };
    }

    private static VoxelShape graphShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> MINI_PANEL_SHAPE_SOUTH;
            case EAST -> MINI_PANEL_SHAPE_EAST;
            case WEST -> MINI_PANEL_SHAPE_WEST;
            default -> MINI_PANEL_SHAPE_NORTH;
        };
    }

    private static VoxelShape indicatorShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> MINI_PANEL_SHAPE_SOUTH;
            case EAST -> MINI_PANEL_SHAPE_EAST;
            case WEST -> MINI_PANEL_SHAPE_WEST;
            default -> MINI_PANEL_SHAPE_NORTH;
        };
    }

    private static VoxelShape keyPadShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> MINI_PANEL_SHAPE_SOUTH;
            case EAST -> MINI_PANEL_SHAPE_EAST;
            case WEST -> MINI_PANEL_SHAPE_WEST;
            default -> MINI_PANEL_SHAPE_NORTH;
        };
    }

    private static VoxelShape leverShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> MINI_PANEL_SHAPE_SOUTH;
            case EAST -> MINI_PANEL_SHAPE_EAST;
            case WEST -> MINI_PANEL_SHAPE_WEST;
            default -> MINI_PANEL_SHAPE_NORTH;
        };
    }

    private static VoxelShape numitronShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> MINI_PANEL_SHAPE_SOUTH;
            case EAST -> MINI_PANEL_SHAPE_EAST;
            case WEST -> MINI_PANEL_SHAPE_WEST;
            default -> MINI_PANEL_SHAPE_NORTH;
        };
    }

    private static VoxelShape terminalShape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> MINI_PANEL_SHAPE_SOUTH;
            case EAST -> MINI_PANEL_SHAPE_EAST;
            case WEST -> MINI_PANEL_SHAPE_WEST;
            default -> MINI_PANEL_SHAPE_NORTH;
        };
    }

    private static VoxelShape blankColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight && hasLid) {
            return COLUMN_WITH_LID_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape fuelRodColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight && hasLid) {
            return COLUMN_WITH_LID_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape moderatedFuelRodColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight && hasLid) {
            return COLUMN_WITH_LID_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape reasimFuelRodColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight && hasLid) {
            return COLUMN_WITH_LID_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape moderatedReasimFuelRodColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight && hasLid) {
            return COLUMN_WITH_LID_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape controlColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight) {
            return hasLid ? COLUMN_WITH_LID_SHAPE : PIPED_COLUMN_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape automaticControlColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight) {
            return hasLid ? COLUMN_WITH_LID_SHAPE : PIPED_COLUMN_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape moderatedControlColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight) {
            return hasLid ? COLUMN_WITH_LID_SHAPE : PIPED_COLUMN_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape reasimControlColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight) {
            return hasLid ? COLUMN_WITH_LID_SHAPE : PIPED_COLUMN_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape automaticReasimControlColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight) {
            return hasLid ? COLUMN_WITH_LID_SHAPE : PIPED_COLUMN_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape boilerColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight) {
            return hasLid ? COLUMN_WITH_LID_SHAPE : PIPED_COLUMN_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape heaterColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight) {
            return hasLid ? COLUMN_WITH_LID_SHAPE : PIPED_COLUMN_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape coolerColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight) {
            return hasLid ? COLUMN_WITH_LID_SHAPE : PIPED_COLUMN_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape loaderColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape steamInletColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape steamOutletColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape outgasserColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight && hasLid) {
            return COLUMN_WITH_LID_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape storageColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight && hasLid) {
            return COLUMN_WITH_LID_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape moderatorColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight && hasLid) {
            return COLUMN_WITH_LID_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape reflectorColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight && hasLid) {
            return COLUMN_WITH_LID_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape absorberColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        if (yOffset > 0 && yOffset == columnHeight && hasLid) {
            return COLUMN_WITH_LID_SHAPE;
        }
        return FULL_BLOCK;
    }

    private static VoxelShape autoloaderColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return autoloaderSegmentShape(yOffset);
    }

    /**
     * Exact per-block collision cut from the two legacy RBMKAutoloader AABBs:
     * Y=0..3 is a centred 0.25 x 0.25 column, Y=4..8 occupies the full block.
     */
    public static VoxelShape autoloaderSegmentShape(int yOffset) {
        if (yOffset < 0 || yOffset > AUTOLOADER_TOP_OFFSET) {
            return Shapes.empty();
        }
        return yOffset <= 3 ? AUTOLOADER_LOWER_COLUMN : FULL_BLOCK;
    }

    private static VoxelShape consoleColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape craneConsoleColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape displayColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape blankDisplayColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape gaugeColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape graphColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape indicatorColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape keyPadColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape leverColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape numitronColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    private static VoxelShape terminalColumnSegmentShape(int yOffset, int columnHeight, boolean hasLid) {
        return FULL_BLOCK;
    }

    public static int columnHeight() {
        try {
            return Math.max(2, Math.min(16, HbmConfig.RBMK_COLUMN_HEIGHT.get()));
        } catch (IllegalStateException ignored) {
            return DEFAULT_COLUMN_HEIGHT;
        }
    }

    public static int columnHeight(BlockGetter level) {
        return columnHeight();
    }

    private boolean canPlaceColumn(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos corePos = context.getClickedPos();
        for (int y = 1; y < columnHeight(level); y++) {
            if (!level.getBlockState(corePos.above(y)).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private static void placeColumnDummies(Level level, BlockPos corePos) {
        for (int y = 1; y < columnHeight(level); y++) {
            BlockPos dummyPos = corePos.above(y);
            level.setBlock(dummyPos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), 3);
            if (level.getBlockEntity(dummyPos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    private static void removeColumnDummies(Level level, BlockPos corePos, BlockState coreState) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (int y = 1; y <= columnHeight(level); y++) {
                BlockPos dummyPos = corePos.above(y);
                if (level.getBlockState(dummyPos).is(HbmBlocks.MACHINE_DUMMY.get())
                        && level.getBlockEntity(dummyPos) instanceof MachineDummyBlockEntity dummy
                        && dummy.getCorePos().equals(corePos)) {
                    level.removeBlock(dummyPos, false);
                }
            }
        });
    }

    private static boolean canPlaceAutoloaderStructure(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos corePos = context.getClickedPos();
        for (int y = 1; y <= AUTOLOADER_TOP_OFFSET; y++) {
            if (!level.getBlockState(corePos.above(y)).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private static void placeAutoloaderDummies(Level level, BlockPos corePos) {
        for (int y = 1; y <= AUTOLOADER_TOP_OFFSET; y++) {
            BlockPos dummyPos = corePos.above(y);
            level.setBlock(dummyPos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), 3);
            if (level.getBlockEntity(dummyPos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    private static void removeAutoloaderDummies(Level level, BlockPos corePos) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (int y = 1; y <= AUTOLOADER_TOP_OFFSET; y++) {
                BlockPos dummyPos = corePos.above(y);
                if (level.getBlockState(dummyPos).is(HbmBlocks.MACHINE_DUMMY.get())
                        && level.getBlockEntity(dummyPos) instanceof MachineDummyBlockEntity dummy
                        && dummy.getCorePos().equals(corePos)) {
                    level.removeBlock(dummyPos, false);
                }
            }
        });
    }

    private boolean canPlaceOldDummyFootprint(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos corePos = context.getClickedPos();
        for (BlockPos occupiedPos : oldDummyFootprint(corePos, state.getValue(FACING), kind)) {
            if (occupiedPos.equals(corePos)) {
                continue;
            }
            if (!level.getBlockState(occupiedPos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private static void placeOldDummyFootprint(Level level, BlockPos corePos, java.util.Set<BlockPos> occupied) {
        for (BlockPos dummyPos : occupied) {
            if (dummyPos.equals(corePos)) {
                continue;
            }
            level.setBlock(dummyPos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), 3);
            if (level.getBlockEntity(dummyPos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    private static void removeOldDummyFootprint(Level level, BlockPos corePos, Direction facing, Kind kind,
                                                BlockState coreState) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos dummyPos : oldDummyFootprint(corePos, facing, kind)) {
                if (dummyPos.equals(corePos)) {
                    continue;
                }
                if (level.getBlockState(dummyPos).is(HbmBlocks.MACHINE_DUMMY.get())
                        && level.getBlockEntity(dummyPos) instanceof MachineDummyBlockEntity dummy
                        && dummy.getCorePos().equals(corePos)) {
                    level.removeBlock(dummyPos, false);
                }
            }
        });
    }

    private static java.util.Set<BlockPos> oldDummyFootprint(BlockPos corePos, Direction facing, Kind kind) {
        java.util.LinkedHashSet<BlockPos> positions = new java.util.LinkedHashSet<>();
        switch (kind) {
            case CONSOLE -> {
                addConsoleDimension(positions, corePos, facing, RBMK_CONSOLE_MAIN_DIMS);
                addConsoleDimension(positions, corePos, facing, RBMK_CONSOLE_EXTENSION_DIMS);
            }
            case CRANE_CONSOLE -> {
                addCraneConsoleDimension(positions, corePos, facing, RBMK_CRANE_CONSOLE_MAIN_DIMS);
                addCraneConsoleDimension(positions, corePos, facing, RBMK_CRANE_CONSOLE_EXTENSION_DIMS);
            }
            default -> positions.add(corePos);
        }
        return positions;
    }

    private static void addConsoleDimension(java.util.Set<BlockPos> positions, BlockPos corePos,
                                            Direction facing, int[] dims) {
        switch (facing) {
            case NORTH -> addResolvedDimension(positions, corePos, dims[0], dims[1], dims[3], dims[2], dims[5], dims[4]);
            case EAST -> addResolvedDimension(positions, corePos, dims[0], dims[1], dims[5], dims[4], dims[2], dims[3]);
            case SOUTH -> addResolvedDimension(positions, corePos, dims[0], dims[1], dims[2], dims[3], dims[4], dims[5]);
            case WEST -> addResolvedDimension(positions, corePos, dims[0], dims[1], dims[4], dims[5], dims[3], dims[2]);
            case UP, DOWN -> throw new IllegalArgumentException("RBMK console requires a horizontal facing: " + facing);
        }
    }

    private static void addCraneConsoleDimension(java.util.Set<BlockPos> positions, BlockPos corePos,
                                                 Direction facing, int[] dims) {
        switch (facing) {
            case NORTH -> addResolvedDimension(positions, corePos, dims[0], dims[1], dims[3], dims[2], dims[5], dims[4]);
            case EAST -> addResolvedDimension(positions, corePos, dims[0], dims[1], dims[5], dims[4], dims[2], dims[3]);
            case SOUTH -> addResolvedDimension(positions, corePos, dims[0], dims[1], dims[2], dims[3], dims[4], dims[5]);
            case WEST -> addResolvedDimension(positions, corePos, dims[0], dims[1], dims[4], dims[5], dims[3], dims[2]);
            case UP, DOWN -> throw new IllegalArgumentException("RBMK crane console requires a horizontal facing: " + facing);
        }
    }

    private static void addResolvedDimension(java.util.Set<BlockPos> positions, BlockPos corePos,
                                             int up, int down, int negativeZ, int positiveZ,
                                             int negativeX, int positiveX) {
        for (int dx = -negativeX; dx <= positiveX; dx++) {
            for (int dy = -down; dy <= up; dy++) {
                for (int dz = -negativeZ; dz <= positiveZ; dz++) {
                    positions.add(corePos.offset(dx, dy, dz));
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RbmkComponentBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof RbmkComponentBlockEntity rbmk) {
                RbmkComponentBlockEntity.tick(tickerLevel, pos, tickerState, rbmk);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof RbmkComponentBlockEntity rbmk)) {
            return InteractionResult.PASS;
        }
        // TileEntityCraneConsole did not implement onBlockActivated or expose
        // a GUI in 1.7.10; empty-hand clicks therefore fell through to the
        // vanilla Block result instead of opening a null menu or printing
        // generic RBMK info.
        if (rbmk.kind() == Kind.CRANE_CONSOLE) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        markFbiTarget(player);
        if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            if (kind.hasMenu() && !player.isShiftKeyDown()) {
                serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
                return InteractionResult.CONSUME;
            }
        }
        // RBMKBase.openInv returned true for a sneaking click without opening
        // a menu or mutating the component.
        if (player.isShiftKeyDown()) {
            return InteractionResult.CONSUME;
        }
        if (rbmk.handleEmptyHand(player)) {
            return InteractionResult.CONSUME;
        }
        if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        rbmk.printInfo(player);
        return InteractionResult.CONSUME;
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
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!level.isClientSide) {
            markFbiTarget(player);
        }
        if (blockEntity instanceof RbmkComponentBlockEntity rbmk
                && rbmk.handleItemUse(player, hand, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private void markFbiTarget(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (kind == Kind.STORAGE || kind.acceptsFuel() || kind == Kind.CONSOLE && !player.isShiftKeyDown()) {
            LegacyMobSpawnEvents.markFbi(serverPlayer);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RbmkComponentBlockEntity rbmk && rbmk.triggerBreakMeltdown(level, pos)) {
                super.onRemove(state, level, pos, newState, movedByPiston);
                return;
            }
            if (blockEntity instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            if (blockEntity instanceof RbmkComponentBlockEntity rbmk) {
                ItemStack lid = rbmk.removeLidStack();
                if (!level.isClientSide && !lid.isEmpty()) {
                    BlockPos lidPos = pos.above(columnHeight(level));
                    Block.popResource(level, lidPos, lid);
                }
            }
            if (!level.isClientSide && kind.isColumn()) {
                removeColumnDummies(level, pos, state);
            }
            if (!level.isClientSide && kind == Kind.AUTOLOADER) {
                removeAutoloaderDummies(level, pos);
            }
            if (!level.isClientSide && kind.hasOldDummyFootprint()) {
                removeOldDummyFootprint(level, pos, state.getValue(FACING), kind, state);
            }
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
        return switch (this.kind) {
            case BLANK -> rotateBlank(state, rotation);
            case FUEL_ROD -> rotateFuelRod(state, rotation);
            case FUEL_ROD_MOD -> rotateModeratedFuelRod(state, rotation);
            case FUEL_ROD_REASIM -> rotateReasimFuelRod(state, rotation);
            case FUEL_ROD_REASIM_MOD -> rotateModeratedReasimFuelRod(state, rotation);
            case CONTROL -> rotateControl(state, rotation);
            case CONTROL_AUTO -> rotateAutomaticControl(state, rotation);
            case CONTROL_MOD -> rotateModeratedControl(state, rotation);
            case CONTROL_REASIM -> rotateReasimControl(state, rotation);
            case CONTROL_REASIM_AUTO -> rotateAutomaticReasimControl(state, rotation);
            case BOILER -> rotateBoiler(state, rotation);
            case HEATER -> rotateHeater(state, rotation);
            case COOLER -> rotateCooler(state, rotation);
            case LOADER -> rotateLoader(state, rotation);
            case STEAM_INLET -> rotateSteamInlet(state, rotation);
            case STEAM_OUTLET -> rotateSteamOutlet(state, rotation);
            case OUTGASSER -> rotateOutgasser(state, rotation);
            case STORAGE -> rotateStorage(state, rotation);
            case MODERATOR -> rotateModerator(state, rotation);
            case REFLECTOR -> rotateReflector(state, rotation);
            case ABSORBER -> rotateAbsorber(state, rotation);
            case AUTOLOADER -> rotateAutoloader(state, rotation);
            case CONSOLE -> rotateConsole(state, rotation);
            case CRANE_CONSOLE -> rotateCraneConsole(state, rotation);
            case DISPLAY -> rotateDisplay(state, rotation);
            case DISPLAY_BLANK -> rotateBlankDisplay(state, rotation);
            case GAUGE -> rotateGauge(state, rotation);
            case GRAPH -> rotateGraph(state, rotation);
            case INDICATOR -> rotateIndicator(state, rotation);
            case KEY_PAD -> rotateKeyPad(state, rotation);
            case LEVER -> rotateLever(state, rotation);
            case NUMITRON -> rotateNumitron(state, rotation);
            case TERMINAL -> rotateTerminal(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.kind) {
            case BLANK -> mirrorBlank(state, mirror);
            case FUEL_ROD -> mirrorFuelRod(state, mirror);
            case FUEL_ROD_MOD -> mirrorModeratedFuelRod(state, mirror);
            case FUEL_ROD_REASIM -> mirrorReasimFuelRod(state, mirror);
            case FUEL_ROD_REASIM_MOD -> mirrorModeratedReasimFuelRod(state, mirror);
            case CONTROL -> mirrorControl(state, mirror);
            case CONTROL_AUTO -> mirrorAutomaticControl(state, mirror);
            case CONTROL_MOD -> mirrorModeratedControl(state, mirror);
            case CONTROL_REASIM -> mirrorReasimControl(state, mirror);
            case CONTROL_REASIM_AUTO -> mirrorAutomaticReasimControl(state, mirror);
            case BOILER -> mirrorBoiler(state, mirror);
            case HEATER -> mirrorHeater(state, mirror);
            case COOLER -> mirrorCooler(state, mirror);
            case LOADER -> mirrorLoader(state, mirror);
            case STEAM_INLET -> mirrorSteamInlet(state, mirror);
            case STEAM_OUTLET -> mirrorSteamOutlet(state, mirror);
            case OUTGASSER -> mirrorOutgasser(state, mirror);
            case STORAGE -> mirrorStorage(state, mirror);
            case MODERATOR -> mirrorModerator(state, mirror);
            case REFLECTOR -> mirrorReflector(state, mirror);
            case ABSORBER -> mirrorAbsorber(state, mirror);
            case AUTOLOADER -> mirrorAutoloader(state, mirror);
            case CONSOLE -> mirrorConsole(state, mirror);
            case CRANE_CONSOLE -> mirrorCraneConsole(state, mirror);
            case DISPLAY -> mirrorDisplay(state, mirror);
            case DISPLAY_BLANK -> mirrorBlankDisplay(state, mirror);
            case GAUGE -> mirrorGauge(state, mirror);
            case GRAPH -> mirrorGraph(state, mirror);
            case INDICATOR -> mirrorIndicator(state, mirror);
            case KEY_PAD -> mirrorKeyPad(state, mirror);
            case LEVER -> mirrorLever(state, mirror);
            case NUMITRON -> mirrorNumitron(state, mirror);
            case TERMINAL -> mirrorTerminal(state, mirror);
        };
    }

    private static BlockState rotateBlank(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateFuelRod(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateModeratedFuelRod(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateReasimFuelRod(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateModeratedReasimFuelRod(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateControl(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateAutomaticControl(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateModeratedControl(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateReasimControl(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateAutomaticReasimControl(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateBoiler(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateHeater(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateCooler(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateLoader(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSteamInlet(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSteamOutlet(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateOutgasser(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateStorage(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateModerator(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateReflector(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateAbsorber(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateAutoloader(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateConsole(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateCraneConsole(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateDisplay(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateBlankDisplay(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateGauge(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateGraph(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateIndicator(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateKeyPad(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateLever(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateNumitron(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateTerminal(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorBlank(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorFuelRod(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorModeratedFuelRod(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorReasimFuelRod(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorModeratedReasimFuelRod(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorControl(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorAutomaticControl(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorModeratedControl(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorReasimControl(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorAutomaticReasimControl(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorBoiler(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorHeater(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorCooler(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorLoader(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSteamInlet(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSteamOutlet(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorOutgasser(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorStorage(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorModerator(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorReflector(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorAbsorber(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorAutoloader(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorConsole(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorCraneConsole(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorDisplay(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorBlankDisplay(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorGauge(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorGraph(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorIndicator(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorKeyPad(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorLever(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorNumitron(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorTerminal(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public enum Kind implements StringRepresentable {
        BLANK("blank", true, false, false, false),
        FUEL_ROD("rod", true, false, false, false),
        FUEL_ROD_MOD("rod_mod", true, false, false, false),
        FUEL_ROD_REASIM("rod_reasim", true, false, false, false),
        FUEL_ROD_REASIM_MOD("rod_reasim_mod", true, false, false, false),
        CONTROL("control", true, true, false, false),
        CONTROL_AUTO("control_auto", true, true, false, false),
        CONTROL_MOD("control_mod", true, true, false, false),
        CONTROL_REASIM("control_reasim", true, true, false, false),
        CONTROL_REASIM_AUTO("control_reasim_auto", true, true, false, false),
        BOILER("boiler", true, false, true, false),
        HEATER("heater", true, false, true, false),
        COOLER("cooler", true, false, true, false),
        LOADER("loader", false, false, true, false),
        STEAM_INLET("steam_inlet", false, false, true, false),
        STEAM_OUTLET("steam_outlet", false, false, true, false),
        OUTGASSER("outgasser", true, false, false, false),
        STORAGE("storage", true, false, false, false),
        MODERATOR("moderator", true, false, false, false),
        REFLECTOR("reflector", true, false, false, false),
        ABSORBER("absorber", true, false, false, false),
        AUTOLOADER("autoloader", false, false, false, false),
        CONSOLE("console", false, false, false, true),
        CRANE_CONSOLE("crane_console", false, false, false, true),
        DISPLAY("display", false, false, false, true),
        DISPLAY_BLANK("display_blank", false, false, false, true),
        GAUGE("gauge", false, false, false, true),
        GRAPH("graph", false, false, false, true),
        INDICATOR("indicator", false, false, false, true),
        KEY_PAD("key_pad", false, false, false, true),
        LEVER("lever", false, false, false, true),
        NUMITRON("numitron", false, false, false, true),
        TERMINAL("terminal", false, false, false, true);

        private final String serializedName;
        private final boolean column;
        private final boolean control;
        private final boolean fluid;
        private final boolean console;
        private final boolean glassLike;

        Kind(String serializedName, boolean column, boolean control, boolean fluid, boolean console) {
            this.serializedName = serializedName;
            this.column = column;
            this.control = control;
            this.fluid = fluid;
            this.console = console;
            this.glassLike = false;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public boolean isColumn() {
            return column;
        }

        public boolean isControl() {
            return control;
        }

        public boolean isAutomaticControl() {
            return this == CONTROL_AUTO || this == CONTROL_REASIM_AUTO;
        }

        public boolean hasFluid() {
            return fluid;
        }

        public boolean isConsole() {
            return console;
        }

        public boolean acceptsFuel() {
            return this == FUEL_ROD
                    || this == FUEL_ROD_MOD
                    || this == FUEL_ROD_REASIM
                    || this == FUEL_ROD_REASIM_MOD;
        }

        public boolean hasMenu() {
            return acceptsFuel()
                    || isControl()
                    || this == BOILER
                    || this == HEATER
                    || this == OUTGASSER
                    || this == STORAGE
                    || this == AUTOLOADER
                    || this == CONSOLE;
        }

        public boolean hasTopPipes() {
            return isControl()
                    || this == BOILER
                    || this == HEATER
                    || this == COOLER;
        }

        public boolean isMiniPanel() {
            return this == DISPLAY
                    || this == DISPLAY_BLANK
                    || this == GAUGE
                    || this == GRAPH
                    || this == INDICATOR
                    || this == KEY_PAD
                    || this == LEVER
                    || this == NUMITRON
                    || this == TERMINAL;
        }

        public boolean hasOldDummyFootprint() {
            return this == CONSOLE || this == CRANE_CONSOLE;
        }
    }
}
