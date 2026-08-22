package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import com.reinhardt.hbm.config.HbmConfig;
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
    private static final VoxelShape FULL_BLOCK = Shapes.block();
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
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        if (kind.isColumn() && !canPlaceColumn(context, state)) {
            return null;
        }
        if (kind.hasOldDummyFootprint() && !canPlaceOldDummyFootprint(context, state)) {
            return null;
        }
        return state;
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
        if (kind.isMiniPanel()) {
            return switch (state.getValue(FACING)) {
                case SOUTH -> MINI_PANEL_SHAPE_SOUTH;
                case EAST -> MINI_PANEL_SHAPE_EAST;
                case WEST -> MINI_PANEL_SHAPE_WEST;
                default -> MINI_PANEL_SHAPE_NORTH;
            };
        }
        if (kind.isColumn()) {
            return FULL_BLOCK;
        }
        return FULL_BLOCK;
    }

    public static VoxelShape columnSegmentShape(Kind kind, int yOffset, int columnHeight, boolean hasLid) {
        if (!kind.isColumn() || yOffset <= 0) {
            return FULL_BLOCK;
        }
        if (yOffset == columnHeight) {
            if (hasLid) {
                return COLUMN_WITH_LID_SHAPE;
            }
            if (kind.hasTopPipes()) {
                return PIPED_COLUMN_SHAPE;
            }
        }
        return FULL_BLOCK;
    }

    public static int columnHeight(BlockGetter level) {
        try {
            return Math.max(2, Math.min(16, HbmConfig.RBMK_COLUMN_HEIGHT.get()));
        } catch (IllegalStateException ignored) {
            return DEFAULT_COLUMN_HEIGHT;
        }
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

    private static void removeColumnDummies(Level level, BlockPos corePos) {
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

    private static void removeOldDummyFootprint(Level level, BlockPos corePos, Direction facing, Kind kind) {
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
                addOldDimension(positions, corePos, facing, RBMK_CONSOLE_MAIN_DIMS);
                addOldDimension(positions, corePos, facing, RBMK_CONSOLE_EXTENSION_DIMS);
            }
            case CRANE_CONSOLE -> {
                addOldDimension(positions, corePos, facing, RBMK_CRANE_CONSOLE_MAIN_DIMS);
                addOldDimension(positions, corePos, facing, RBMK_CRANE_CONSOLE_EXTENSION_DIMS);
            }
            default -> positions.add(corePos);
        }
        return positions;
    }

    private static void addOldDimension(java.util.Set<BlockPos> positions, BlockPos corePos, Direction facing, int[] dims) {
        int[] rotated = rotateOldDimension(dims, facing);
        for (int dx = -rotated[4]; dx <= rotated[5]; dx++) {
            for (int dy = -rotated[1]; dy <= rotated[0]; dy++) {
                for (int dz = -rotated[2]; dz <= rotated[3]; dz++) {
                    positions.add(corePos.offset(dx, dy, dz));
                }
            }
        }
    }

    private static int[] rotateOldDimension(int[] dims, Direction facing) {
        return switch (facing) {
            case NORTH -> new int[] {dims[0], dims[1], dims[3], dims[2], dims[5], dims[4]};
            case EAST -> new int[] {dims[0], dims[1], dims[5], dims[4], dims[2], dims[3]};
            case WEST -> new int[] {dims[0], dims[1], dims[4], dims[5], dims[3], dims[2]};
            default -> dims;
        };
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
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            if (kind.hasMenu() && !player.isShiftKeyDown()) {
                serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
                return InteractionResult.CONSUME;
            }
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
        if (blockEntity instanceof RbmkComponentBlockEntity rbmk
                && rbmk.handleItemUse(player, hand, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
                    Block.popResource(level, pos.above(columnHeight(level)), lid);
                }
            }
            if (!level.isClientSide && kind.isColumn()) {
                removeColumnDummies(level, pos);
            }
            if (!level.isClientSide && kind.hasOldDummyFootprint()) {
                removeOldDummyFootprint(level, pos, state.getValue(FACING), kind);
            }
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
