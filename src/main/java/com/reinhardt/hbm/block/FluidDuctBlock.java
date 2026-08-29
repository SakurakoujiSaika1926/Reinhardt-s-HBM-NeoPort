package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FluidPipeBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class FluidDuctBlock extends Block implements EntityBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    private static final VoxelShape CENTER = Shapes.box(0.3125D, 0.3125D, 0.3125D, 0.6875D, 0.6875D, 0.6875D);
    private static final VoxelShape NORTH_ARM = Shapes.box(0.3125D, 0.3125D, 0.0D, 0.6875D, 0.6875D, 0.3125D);
    private static final VoxelShape SOUTH_ARM = Shapes.box(0.3125D, 0.3125D, 0.6875D, 0.6875D, 0.6875D, 1.0D);
    private static final VoxelShape WEST_ARM = Shapes.box(0.0D, 0.3125D, 0.3125D, 0.3125D, 0.6875D, 0.6875D);
    private static final VoxelShape EAST_ARM = Shapes.box(0.6875D, 0.3125D, 0.3125D, 1.0D, 0.6875D, 0.6875D);
    private static final VoxelShape UP_ARM = Shapes.box(0.3125D, 0.6875D, 0.3125D, 0.6875D, 1.0D, 0.6875D);
    private static final VoxelShape DOWN_ARM = Shapes.box(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.3125D, 0.6875D);
    private static final VoxelShape ISOLATED = Shapes.or(CENTER, NORTH_ARM, SOUTH_ARM, WEST_ARM, EAST_ARM, UP_ARM, DOWN_ARM);
    private static final VoxelShape BOX_CENTER = Shapes.box(0.0625D, 0.0625D, 0.0625D, 0.9375D, 0.9375D, 0.9375D);
    private static final VoxelShape BOX_CORE = Shapes.box(0.125D, 0.125D, 0.125D, 0.875D, 0.875D, 0.875D);
    private static final VoxelShape BOX_NORTH_ARM = Shapes.box(0.125D, 0.125D, 0.0D, 0.875D, 0.875D, 0.125D);
    private static final VoxelShape BOX_SOUTH_ARM = Shapes.box(0.125D, 0.125D, 0.875D, 0.875D, 0.875D, 1.0D);
    private static final VoxelShape BOX_WEST_ARM = Shapes.box(0.0D, 0.125D, 0.125D, 0.125D, 0.875D, 0.875D);
    private static final VoxelShape BOX_EAST_ARM = Shapes.box(0.875D, 0.125D, 0.125D, 1.0D, 0.875D, 0.875D);
    private static final VoxelShape BOX_UP_ARM = Shapes.box(0.125D, 0.875D, 0.125D, 0.875D, 1.0D, 0.875D);
    private static final VoxelShape BOX_DOWN_ARM = Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 0.125D, 0.875D);
    private static final VoxelShape BOX_ISOLATED = BOX_CENTER;

    private final Kind kind;

    public FluidDuctBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(OPEN, kind.initialOpen()));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (this.kind.fullBlockShape()) {
            return Shapes.block();
        }
        if (this.kind == Kind.EXHAUST) {
            return getBoxDuctShape(state);
        }
        VoxelShape shape = CENTER;
        boolean connected = false;
        if (state.getValue(NORTH)) {
            shape = Shapes.or(shape, NORTH_ARM);
            connected = true;
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.or(shape, SOUTH_ARM);
            connected = true;
        }
        if (state.getValue(WEST)) {
            shape = Shapes.or(shape, WEST_ARM);
            connected = true;
        }
        if (state.getValue(EAST)) {
            shape = Shapes.or(shape, EAST_ARM);
            connected = true;
        }
        if (state.getValue(UP)) {
            shape = Shapes.or(shape, UP_ARM);
            connected = true;
        }
        if (state.getValue(DOWN)) {
            shape = Shapes.or(shape, DOWN_ARM);
            connected = true;
        }
        return connected ? shape : ISOLATED;
    }

    private static VoxelShape getBoxDuctShape(BlockState state) {
        boolean north = state.getValue(NORTH);
        boolean south = state.getValue(SOUTH);
        boolean east = state.getValue(EAST);
        boolean west = state.getValue(WEST);
        boolean up = state.getValue(UP);
        boolean down = state.getValue(DOWN);
        int mask = (east ? 32 : 0) | (west ? 16 : 0) | (up ? 8 : 0) | (down ? 4 : 0) | (south ? 2 : 0) | (north ? 1 : 0);
        int count = (east ? 1 : 0) + (west ? 1 : 0) + (up ? 1 : 0) + (down ? 1 : 0) + (south ? 1 : 0) + (north ? 1 : 0);

        if (mask == 0) {
            return BOX_ISOLATED;
        }
        if (mask == 0b100000 || mask == 0b010000 || mask == 0b110000) {
            return Shapes.box(0.0D, 0.125D, 0.125D, 1.0D, 0.875D, 0.875D);
        }
        if (mask == 0b001000 || mask == 0b000100 || mask == 0b001100) {
            return Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);
        }
        if (mask == 0b000010 || mask == 0b000001 || mask == 0b000011) {
            return Shapes.box(0.125D, 0.125D, 0.0D, 0.875D, 0.875D, 1.0D);
        }

        VoxelShape shape = count == 2 ? BOX_CORE : BOX_CENTER;
        if (north) {
            shape = Shapes.or(shape, BOX_NORTH_ARM);
        }
        if (south) {
            shape = Shapes.or(shape, BOX_SOUTH_ARM);
        }
        if (west) {
            shape = Shapes.or(shape, BOX_WEST_ARM);
        }
        if (east) {
            shape = Shapes.or(shape, BOX_EAST_ARM);
        }
        if (up) {
            shape = Shapes.or(shape, BOX_UP_ARM);
        }
        if (down) {
            shape = Shapes.or(shape, BOX_DOWN_ARM);
        }
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            pipe.setOpen(state.getValue(OPEN));
        }
        refreshConnections(level, pos);
        refreshNeighborPipes(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            level.invalidateCapabilities(pos);
            refreshNeighborPipes(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (this.kind == Kind.SWITCH) {
            setOpen(level, pos, level.hasNeighborSignal(pos), false);
        }
        refreshConnections(level, pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return FluidIdentifierItem.applyToPipe(stack, level, pos, player)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (this.kind != Kind.VALVE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            setOpen(level, pos, !state.getValue(OPEN), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void refreshConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock)) {
            return;
        }

        HbmFluidDefinition type = connectionType(level, pos, this.kind);
        BlockState next = state
                .setValue(NORTH, canConnect(level, pos, Direction.NORTH, type))
                .setValue(SOUTH, canConnect(level, pos, Direction.SOUTH, type))
                .setValue(EAST, canConnect(level, pos, Direction.EAST, type))
                .setValue(WEST, canConnect(level, pos, Direction.WEST, type))
                .setValue(UP, canConnect(level, pos, Direction.UP, type))
                .setValue(DOWN, canConnect(level, pos, Direction.DOWN, type));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    private static void refreshNeighborPipes(LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighbor);
            if (neighborState.getBlock() instanceof FluidDuctBlock duct) {
                duct.refreshConnections(level, neighbor);
            }
        }
    }

    private void setOpen(Level level, BlockPos pos, boolean open, boolean playSound) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(OPEN) || state.getValue(OPEN) == open) {
            return;
        }
        level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
            pipe.setOpen(open);
        }
        if (playSound) {
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, open ? 1.0F : 0.85F);
        }
        refreshConnections(level, pos);
        refreshNeighborPipes(level, pos);
    }

    private static boolean canConnect(LevelAccessor level, BlockPos pos, Direction direction, HbmFluidDefinition type) {
        return HbmFluidNetworks.canPipeConnect(level, pos, direction, type);
    }

    private static HbmFluidDefinition connectionType(LevelAccessor level, BlockPos pos, Kind kind) {
        if (kind.isExhaust()) {
            return FluidPipeBlockEntity.defaultLegacyExhaustFluid();
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof FluidPipeBlockEntity pipe) {
            return pipe.type();
        }
        return HbmFluids.none();
    }

    public static int changeTypeRecursively(Level level, BlockPos start, HbmFluidDefinition previousType, HbmFluidDefinition nextType, int limit) {
        if (previousType == nextType || limit <= 0) {
            return 0;
        }

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        int changed = 0;

        while (!queue.isEmpty() && visited.size() < limit) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) || pipe.isExhaustPipe() || pipe.type() != previousType) {
                continue;
            }
            pipe.setType(nextType);
            changed++;
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (!visited.contains(neighbor)
                        && level.getBlockEntity(neighbor) instanceof FluidPipeBlockEntity neighborPipe
                        && !neighborPipe.isExhaustPipe()) {
                    queue.addLast(neighbor);
                }
            }
            for (BlockPos link : pipe.networkLinks()) {
                if (!visited.contains(link)) {
                    queue.addLast(link.immutable());
                }
            }
        }
        return changed;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN, OPEN);
    }

    public enum Kind {
        MK2(false, true),
        BOX(true, true),
        EXHAUST(false, true),
        GAUGE(false, true),
        PAINTABLE(false, true),
        PAINTABLE_EXHAUST(true, true),
        SOLID(true, true),
        SOLID_SEALED(true, true),
        VALVE(true, false),
        SWITCH(true, false),
        COUNTER_VALVE(true, false);

        private final boolean fullBlockShape;
        private final boolean initialOpen;

        Kind(boolean fullBlockShape, boolean initialOpen) {
            this.fullBlockShape = fullBlockShape;
            this.initialOpen = initialOpen;
        }

        public boolean fullBlockShape() {
            return fullBlockShape;
        }

        public boolean initialOpen() {
            return initialOpen;
        }

        public boolean isExhaust() {
            return this == EXHAUST || this == PAINTABLE_EXHAUST;
        }
    }
}
