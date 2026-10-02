package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FluidPipeBlockEntity;
import com.reinhardt.hbm.blockentity.ClientRenderNeighborSnapshot;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

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

    private static final VoxelShape MK2_CENTER = Shapes.box(0.3125D, 0.3125D, 0.3125D, 0.6875D, 0.6875D, 0.6875D);
    private static final VoxelShape MK2_NORTH_ARM = Shapes.box(0.3125D, 0.3125D, 0.0D, 0.6875D, 0.6875D, 0.3125D);
    private static final VoxelShape MK2_SOUTH_ARM = Shapes.box(0.3125D, 0.3125D, 0.6875D, 0.6875D, 0.6875D, 1.0D);
    private static final VoxelShape MK2_WEST_ARM = Shapes.box(0.0D, 0.3125D, 0.3125D, 0.3125D, 0.6875D, 0.6875D);
    private static final VoxelShape MK2_EAST_ARM = Shapes.box(0.6875D, 0.3125D, 0.3125D, 1.0D, 0.6875D, 0.6875D);
    private static final VoxelShape MK2_UP_ARM = Shapes.box(0.3125D, 0.6875D, 0.3125D, 0.6875D, 1.0D, 0.6875D);
    private static final VoxelShape MK2_DOWN_ARM = Shapes.box(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.3125D, 0.6875D);
    private static final VoxelShape MK2_ISOLATED_COLLISION = Shapes.or(MK2_NORTH_ARM, MK2_SOUTH_ARM, MK2_WEST_ARM, MK2_EAST_ARM, MK2_UP_ARM, MK2_DOWN_ARM);

    private static final VoxelShape PAINTABLE_CENTER = Shapes.box(0.3125D, 0.3125D, 0.3125D, 0.6875D, 0.6875D, 0.6875D);
    private static final VoxelShape PAINTABLE_NORTH_ARM = Shapes.box(0.3125D, 0.3125D, 0.0D, 0.6875D, 0.6875D, 0.3125D);
    private static final VoxelShape PAINTABLE_SOUTH_ARM = Shapes.box(0.3125D, 0.3125D, 0.6875D, 0.6875D, 0.6875D, 1.0D);
    private static final VoxelShape PAINTABLE_WEST_ARM = Shapes.box(0.0D, 0.3125D, 0.3125D, 0.3125D, 0.6875D, 0.6875D);
    private static final VoxelShape PAINTABLE_EAST_ARM = Shapes.box(0.6875D, 0.3125D, 0.3125D, 1.0D, 0.6875D, 0.6875D);
    private static final VoxelShape PAINTABLE_UP_ARM = Shapes.box(0.3125D, 0.6875D, 0.3125D, 0.6875D, 1.0D, 0.6875D);
    private static final VoxelShape PAINTABLE_DOWN_ARM = Shapes.box(0.3125D, 0.0D, 0.3125D, 0.6875D, 0.3125D, 0.6875D);
    private static final VoxelShape PAINTABLE_ISOLATED_COLLISION = Shapes.or(PAINTABLE_NORTH_ARM, PAINTABLE_SOUTH_ARM, PAINTABLE_WEST_ARM, PAINTABLE_EAST_ARM, PAINTABLE_UP_ARM, PAINTABLE_DOWN_ARM);

    private static final VoxelShape BOX_CENTER = Shapes.box(0.0625D, 0.0625D, 0.0625D, 0.9375D, 0.9375D, 0.9375D);
    private static final VoxelShape BOX_CORE = Shapes.box(0.125D, 0.125D, 0.125D, 0.875D, 0.875D, 0.875D);
    private static final VoxelShape BOX_NORTH_ARM = Shapes.box(0.125D, 0.125D, 0.0D, 0.875D, 0.875D, 0.125D);
    private static final VoxelShape BOX_SOUTH_ARM = Shapes.box(0.125D, 0.125D, 0.875D, 0.875D, 0.875D, 1.0D);
    private static final VoxelShape BOX_WEST_ARM = Shapes.box(0.0D, 0.125D, 0.125D, 0.125D, 0.875D, 0.875D);
    private static final VoxelShape BOX_EAST_ARM = Shapes.box(0.875D, 0.125D, 0.125D, 1.0D, 0.875D, 0.875D);
    private static final VoxelShape BOX_UP_ARM = Shapes.box(0.125D, 0.875D, 0.125D, 0.875D, 1.0D, 0.875D);
    private static final VoxelShape BOX_DOWN_ARM = Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 0.125D, 0.875D);

    private static final VoxelShape EXHAUST_CENTER = Shapes.box(0.0625D, 0.0625D, 0.0625D, 0.9375D, 0.9375D, 0.9375D);
    private static final VoxelShape EXHAUST_CORE = Shapes.box(0.125D, 0.125D, 0.125D, 0.875D, 0.875D, 0.875D);
    private static final VoxelShape EXHAUST_NORTH_ARM = Shapes.box(0.125D, 0.125D, 0.0D, 0.875D, 0.875D, 0.125D);
    private static final VoxelShape EXHAUST_SOUTH_ARM = Shapes.box(0.125D, 0.125D, 0.875D, 0.875D, 0.875D, 1.0D);
    private static final VoxelShape EXHAUST_WEST_ARM = Shapes.box(0.0D, 0.125D, 0.125D, 0.125D, 0.875D, 0.875D);
    private static final VoxelShape EXHAUST_EAST_ARM = Shapes.box(0.875D, 0.125D, 0.125D, 1.0D, 0.875D, 0.875D);
    private static final VoxelShape EXHAUST_UP_ARM = Shapes.box(0.125D, 0.875D, 0.125D, 0.875D, 1.0D, 0.875D);
    private static final VoxelShape EXHAUST_DOWN_ARM = Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 0.125D, 0.875D);

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
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || this.kind != Kind.COUNTER_VALVE) {
            return null;
        }
        BlockEntityTicker<FluidPipeBlockEntity> ticker = FluidPipeBlockEntity::serverTick;
        return (BlockEntityTicker<T>) ticker;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (this.kind) {
            case MK2 -> mk2SelectionShape(state);
            case BOX -> boxSelectionShape(state);
            case EXHAUST -> exhaustSelectionShape(state);
            case GAUGE -> gaugeSelectionShape(state);
            case PAINTABLE -> paintableSelectionShape(state);
            case PAINTABLE_EXHAUST -> paintableExhaustSelectionShape(state);
            case SOLID -> solidSelectionShape(state);
            case SOLID_SEALED -> solidSealedSelectionShape(state);
            case VALVE -> valveSelectionShape(state);
            case SWITCH -> switchSelectionShape(state);
            case COUNTER_VALVE -> counterValveSelectionShape(state);
        };
    }

    private static VoxelShape mk2SelectionShape(BlockState state) {
        int mask = (state.getValue(EAST) ? 32 : 0)
                | (state.getValue(WEST) ? 16 : 0)
                | (state.getValue(UP) ? 8 : 0)
                | (state.getValue(DOWN) ? 4 : 0)
                | (state.getValue(SOUTH) ? 2 : 0)
                | (state.getValue(NORTH) ? 1 : 0);
        if (mask == 0) {
            return Shapes.block();
        }
        if (mask == 0b100000 || mask == 0b010000 || mask == 0b110000) {
            return Shapes.box(0.0D, 0.3125D, 0.3125D, 1.0D, 0.6875D, 0.6875D);
        }
        if (mask == 0b001000 || mask == 0b000100 || mask == 0b001100) {
            return Shapes.box(0.3125D, 0.0D, 0.3125D, 0.6875D, 1.0D, 0.6875D);
        }
        if (mask == 0b000010 || mask == 0b000001 || mask == 0b000011) {
            return Shapes.box(0.3125D, 0.3125D, 0.0D, 0.6875D, 0.6875D, 1.0D);
        }
        return Shapes.box(
                state.getValue(WEST) ? 0.0D : 0.3125D,
                state.getValue(DOWN) ? 0.0D : 0.3125D,
                state.getValue(NORTH) ? 0.0D : 0.3125D,
                state.getValue(EAST) ? 1.0D : 0.6875D,
                state.getValue(UP) ? 1.0D : 0.6875D,
                state.getValue(SOUTH) ? 1.0D : 0.6875D
        );
    }

    private static VoxelShape paintableSelectionShape(BlockState state) {
        int mask = (state.getValue(EAST) ? 32 : 0)
                | (state.getValue(WEST) ? 16 : 0)
                | (state.getValue(UP) ? 8 : 0)
                | (state.getValue(DOWN) ? 4 : 0)
                | (state.getValue(SOUTH) ? 2 : 0)
                | (state.getValue(NORTH) ? 1 : 0);
        if (mask == 0) {
            return Shapes.block();
        }
        if (mask == 0b100000 || mask == 0b010000 || mask == 0b110000) {
            return Shapes.box(0.0D, 0.3125D, 0.3125D, 1.0D, 0.6875D, 0.6875D);
        }
        if (mask == 0b001000 || mask == 0b000100 || mask == 0b001100) {
            return Shapes.box(0.3125D, 0.0D, 0.3125D, 0.6875D, 1.0D, 0.6875D);
        }
        if (mask == 0b000010 || mask == 0b000001 || mask == 0b000011) {
            return Shapes.box(0.3125D, 0.3125D, 0.0D, 0.6875D, 0.6875D, 1.0D);
        }
        return Shapes.box(
                state.getValue(WEST) ? 0.0D : 0.3125D,
                state.getValue(DOWN) ? 0.0D : 0.3125D,
                state.getValue(NORTH) ? 0.0D : 0.3125D,
                state.getValue(EAST) ? 1.0D : 0.6875D,
                state.getValue(UP) ? 1.0D : 0.6875D,
                state.getValue(SOUTH) ? 1.0D : 0.6875D
        );
    }

    private static VoxelShape boxSelectionShape(BlockState state) {
        int mask = (state.getValue(EAST) ? 32 : 0)
                | (state.getValue(WEST) ? 16 : 0)
                | (state.getValue(UP) ? 8 : 0)
                | (state.getValue(DOWN) ? 4 : 0)
                | (state.getValue(SOUTH) ? 2 : 0)
                | (state.getValue(NORTH) ? 1 : 0);
        int count = Integer.bitCount(mask);
        if (mask == 0) return BOX_CENTER;
        if (mask == 0b100000 || mask == 0b010000 || mask == 0b110000) return Shapes.box(0.0D, 0.125D, 0.125D, 1.0D, 0.875D, 0.875D);
        if (mask == 0b001000 || mask == 0b000100 || mask == 0b001100) return Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);
        if (mask == 0b000010 || mask == 0b000001 || mask == 0b000011) return Shapes.box(0.125D, 0.125D, 0.0D, 0.875D, 0.875D, 1.0D);
        double lower = count == 2 ? 0.125D : 0.0625D;
        double upper = count == 2 ? 0.875D : 0.9375D;
        return Shapes.box(
                state.getValue(WEST) ? 0.0D : lower,
                state.getValue(DOWN) ? 0.0D : lower,
                state.getValue(NORTH) ? 0.0D : lower,
                state.getValue(EAST) ? 1.0D : upper,
                state.getValue(UP) ? 1.0D : upper,
                state.getValue(SOUTH) ? 1.0D : upper
        );
    }

    private static VoxelShape exhaustSelectionShape(BlockState state) {
        int mask = (state.getValue(EAST) ? 32 : 0)
                | (state.getValue(WEST) ? 16 : 0)
                | (state.getValue(UP) ? 8 : 0)
                | (state.getValue(DOWN) ? 4 : 0)
                | (state.getValue(SOUTH) ? 2 : 0)
                | (state.getValue(NORTH) ? 1 : 0);
        int count = Integer.bitCount(mask);
        if (mask == 0) return EXHAUST_CENTER;
        if (mask == 0b100000 || mask == 0b010000 || mask == 0b110000) return Shapes.box(0.0D, 0.125D, 0.125D, 1.0D, 0.875D, 0.875D);
        if (mask == 0b001000 || mask == 0b000100 || mask == 0b001100) return Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);
        if (mask == 0b000010 || mask == 0b000001 || mask == 0b000011) return Shapes.box(0.125D, 0.125D, 0.0D, 0.875D, 0.875D, 1.0D);
        double lower = count == 2 ? 0.125D : 0.0625D;
        double upper = count == 2 ? 0.875D : 0.9375D;
        return Shapes.box(
                state.getValue(WEST) ? 0.0D : lower,
                state.getValue(DOWN) ? 0.0D : lower,
                state.getValue(NORTH) ? 0.0D : lower,
                state.getValue(EAST) ? 1.0D : upper,
                state.getValue(UP) ? 1.0D : upper,
                state.getValue(SOUTH) ? 1.0D : upper
        );
    }

    private static VoxelShape gaugeSelectionShape(BlockState state) { return Shapes.block(); }
    private static VoxelShape paintableExhaustSelectionShape(BlockState state) { return Shapes.block(); }
    private static VoxelShape solidSelectionShape(BlockState state) { return Shapes.block(); }
    private static VoxelShape solidSealedSelectionShape(BlockState state) { return Shapes.block(); }
    private static VoxelShape valveSelectionShape(BlockState state) { return Shapes.block(); }
    private static VoxelShape switchSelectionShape(BlockState state) { return Shapes.block(); }
    private static VoxelShape counterValveSelectionShape(BlockState state) { return Shapes.block(); }

    private static VoxelShape mk2CollisionShape(BlockState state) {
        VoxelShape shape = MK2_CENTER;
        boolean connected = false;
        if (state.getValue(NORTH)) {
            shape = Shapes.or(shape, MK2_NORTH_ARM);
            connected = true;
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.or(shape, MK2_SOUTH_ARM);
            connected = true;
        }
        if (state.getValue(WEST)) {
            shape = Shapes.or(shape, MK2_WEST_ARM);
            connected = true;
        }
        if (state.getValue(EAST)) {
            shape = Shapes.or(shape, MK2_EAST_ARM);
            connected = true;
        }
        if (state.getValue(UP)) {
            shape = Shapes.or(shape, MK2_UP_ARM);
            connected = true;
        }
        if (state.getValue(DOWN)) {
            shape = Shapes.or(shape, MK2_DOWN_ARM);
            connected = true;
        }
        return connected ? shape : MK2_ISOLATED_COLLISION;
    }

    private static VoxelShape paintableCollisionShape(BlockState state) {
        VoxelShape shape = PAINTABLE_CENTER;
        boolean connected = false;
        if (state.getValue(NORTH)) {
            shape = Shapes.or(shape, PAINTABLE_NORTH_ARM);
            connected = true;
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.or(shape, PAINTABLE_SOUTH_ARM);
            connected = true;
        }
        if (state.getValue(WEST)) {
            shape = Shapes.or(shape, PAINTABLE_WEST_ARM);
            connected = true;
        }
        if (state.getValue(EAST)) {
            shape = Shapes.or(shape, PAINTABLE_EAST_ARM);
            connected = true;
        }
        if (state.getValue(UP)) {
            shape = Shapes.or(shape, PAINTABLE_UP_ARM);
            connected = true;
        }
        if (state.getValue(DOWN)) {
            shape = Shapes.or(shape, PAINTABLE_DOWN_ARM);
            connected = true;
        }
        return connected ? shape : PAINTABLE_ISOLATED_COLLISION;
    }

    private static VoxelShape boxCollisionShape(BlockState state) {
        boolean north = state.getValue(NORTH);
        boolean south = state.getValue(SOUTH);
        boolean east = state.getValue(EAST);
        boolean west = state.getValue(WEST);
        boolean up = state.getValue(UP);
        boolean down = state.getValue(DOWN);
        int mask = (east ? 32 : 0) | (west ? 16 : 0) | (up ? 8 : 0) | (down ? 4 : 0) | (south ? 2 : 0) | (north ? 1 : 0);
        int count = (east ? 1 : 0) + (west ? 1 : 0) + (up ? 1 : 0) + (down ? 1 : 0) + (south ? 1 : 0) + (north ? 1 : 0);

        if (mask == 0) {
            return BOX_CENTER;
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

    private static VoxelShape exhaustCollisionShape(BlockState state) {
        boolean north = state.getValue(NORTH);
        boolean south = state.getValue(SOUTH);
        boolean east = state.getValue(EAST);
        boolean west = state.getValue(WEST);
        boolean up = state.getValue(UP);
        boolean down = state.getValue(DOWN);
        int mask = (east ? 32 : 0) | (west ? 16 : 0) | (up ? 8 : 0) | (down ? 4 : 0) | (south ? 2 : 0) | (north ? 1 : 0);
        int count = (east ? 1 : 0) + (west ? 1 : 0) + (up ? 1 : 0) + (down ? 1 : 0) + (south ? 1 : 0) + (north ? 1 : 0);

        if (mask == 0) return EXHAUST_CENTER;
        if (mask == 0b100000 || mask == 0b010000 || mask == 0b110000) return Shapes.box(0.0D, 0.125D, 0.125D, 1.0D, 0.875D, 0.875D);
        if (mask == 0b001000 || mask == 0b000100 || mask == 0b001100) return Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);
        if (mask == 0b000010 || mask == 0b000001 || mask == 0b000011) return Shapes.box(0.125D, 0.125D, 0.0D, 0.875D, 0.875D, 1.0D);

        VoxelShape shape = count == 2 ? EXHAUST_CORE : EXHAUST_CENTER;
        if (north) shape = Shapes.or(shape, EXHAUST_NORTH_ARM);
        if (south) shape = Shapes.or(shape, EXHAUST_SOUTH_ARM);
        if (west) shape = Shapes.or(shape, EXHAUST_WEST_ARM);
        if (east) shape = Shapes.or(shape, EXHAUST_EAST_ARM);
        if (up) shape = Shapes.or(shape, EXHAUST_UP_ARM);
        if (down) shape = Shapes.or(shape, EXHAUST_DOWN_ARM);
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (this.kind) {
            case MK2 -> mk2CollisionShape(state);
            case BOX -> boxCollisionShape(state);
            case EXHAUST -> exhaustCollisionShape(state);
            case GAUGE -> gaugeCollisionShape(state);
            case PAINTABLE -> paintableCollisionShape(state);
            case PAINTABLE_EXHAUST -> paintableExhaustCollisionShape(state);
            case SOLID -> solidCollisionShape(state);
            case SOLID_SEALED -> solidSealedCollisionShape(state);
            case VALVE -> valveCollisionShape(state);
            case SWITCH -> switchCollisionShape(state);
            case COUNTER_VALVE -> counterValveCollisionShape(state);
        };
    }

    private static VoxelShape gaugeCollisionShape(BlockState state) { return Shapes.block(); }
    private static VoxelShape paintableExhaustCollisionShape(BlockState state) { return Shapes.block(); }
    private static VoxelShape solidCollisionShape(BlockState state) { return Shapes.block(); }
    private static VoxelShape solidSealedCollisionShape(BlockState state) { return Shapes.block(); }
    private static VoxelShape valveCollisionShape(BlockState state) { return Shapes.block(); }
    private static VoxelShape switchCollisionShape(BlockState state) { return Shapes.block(); }
    private static VoxelShape counterValveCollisionShape(BlockState state) { return Shapes.block(); }

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
            refreshAdjacentRenderSnapshots(level, pos);
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
        if (this.kind != Kind.VALVE && this.kind != Kind.COUNTER_VALVE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            setOpen(level, pos, !state.getValue(OPEN), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void refreshConnections(LevelAccessor level, BlockPos pos) {
        // Connection properties are persisted and synchronized by the server.
        // Recomputing them on a client or while a chunk is still assembling can
        // observe a dummy before its remote machine core exists and permanently
        // turn a working endpoint into a visually disconnected pipe.
        if (level instanceof Level actualLevel && actualLevel.isClientSide) {
            return;
        }
        switch (this.kind) {
            case MK2 -> refreshMk2Connections(level, pos);
            case BOX -> refreshBoxConnections(level, pos);
            case EXHAUST -> refreshExhaustConnections(level, pos);
            case GAUGE -> refreshGaugeConnections(level, pos);
            case PAINTABLE -> refreshPaintableConnections(level, pos);
            case PAINTABLE_EXHAUST -> refreshPaintableExhaustConnections(level, pos);
            case SOLID -> refreshSolidConnections(level, pos);
            case SOLID_SEALED -> refreshSolidSealedConnections(level, pos);
            case VALVE -> refreshValveConnections(level, pos);
            case SWITCH -> refreshSwitchConnections(level, pos);
            case COUNTER_VALVE -> refreshCounterValveConnections(level, pos);
        }
    }

    private static void refreshMk2Connections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock)) return;
        BlockEntity entity = level.getBlockEntity(pos);
        HbmFluidDefinition type = entity instanceof FluidPipeBlockEntity pipe ? pipe.type() : HbmFluids.none();
        BlockState next = state
                .setValue(NORTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.NORTH, type))
                .setValue(SOUTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.SOUTH, type))
                .setValue(EAST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.EAST, type))
                .setValue(WEST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.WEST, type))
                .setValue(UP, HbmFluidNetworks.canPipeConnect(level, pos, Direction.UP, type))
                .setValue(DOWN, HbmFluidNetworks.canPipeConnect(level, pos, Direction.DOWN, type));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            refreshAdjacentRenderSnapshots(level, pos);
        }
    }

    private static void refreshBoxConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock)) return;
        BlockEntity entity = level.getBlockEntity(pos);
        HbmFluidDefinition type = entity instanceof FluidPipeBlockEntity pipe ? pipe.type() : HbmFluids.none();
        BlockState next = state
                .setValue(NORTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.NORTH, type))
                .setValue(SOUTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.SOUTH, type))
                .setValue(EAST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.EAST, type))
                .setValue(WEST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.WEST, type))
                .setValue(UP, HbmFluidNetworks.canPipeConnect(level, pos, Direction.UP, type))
                .setValue(DOWN, HbmFluidNetworks.canPipeConnect(level, pos, Direction.DOWN, type));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            refreshAdjacentRenderSnapshots(level, pos);
        }
    }

    private static void refreshExhaustConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock)) return;
        HbmFluidDefinition type = FluidPipeBlockEntity.defaultLegacyExhaustFluid();
        BlockState next = state
                .setValue(NORTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.NORTH, type))
                .setValue(SOUTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.SOUTH, type))
                .setValue(EAST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.EAST, type))
                .setValue(WEST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.WEST, type))
                .setValue(UP, HbmFluidNetworks.canPipeConnect(level, pos, Direction.UP, type))
                .setValue(DOWN, HbmFluidNetworks.canPipeConnect(level, pos, Direction.DOWN, type));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            refreshAdjacentRenderSnapshots(level, pos);
        }
    }

    private static void refreshGaugeConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock)) return;
        BlockEntity entity = level.getBlockEntity(pos);
        HbmFluidDefinition type = entity instanceof FluidPipeBlockEntity pipe ? pipe.type() : HbmFluids.none();
        BlockState next = state
                .setValue(NORTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.NORTH, type))
                .setValue(SOUTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.SOUTH, type))
                .setValue(EAST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.EAST, type))
                .setValue(WEST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.WEST, type))
                .setValue(UP, HbmFluidNetworks.canPipeConnect(level, pos, Direction.UP, type))
                .setValue(DOWN, HbmFluidNetworks.canPipeConnect(level, pos, Direction.DOWN, type));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            refreshAdjacentRenderSnapshots(level, pos);
        }
    }

    private static void refreshPaintableConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock)) return;
        BlockEntity entity = level.getBlockEntity(pos);
        HbmFluidDefinition type = entity instanceof FluidPipeBlockEntity pipe ? pipe.type() : HbmFluids.none();
        BlockState next = state
                .setValue(NORTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.NORTH, type))
                .setValue(SOUTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.SOUTH, type))
                .setValue(EAST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.EAST, type))
                .setValue(WEST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.WEST, type))
                .setValue(UP, HbmFluidNetworks.canPipeConnect(level, pos, Direction.UP, type))
                .setValue(DOWN, HbmFluidNetworks.canPipeConnect(level, pos, Direction.DOWN, type));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            refreshAdjacentRenderSnapshots(level, pos);
        }
    }

    private static void refreshPaintableExhaustConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock)) return;
        HbmFluidDefinition type = FluidPipeBlockEntity.defaultLegacyExhaustFluid();
        BlockState next = state
                .setValue(NORTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.NORTH, type))
                .setValue(SOUTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.SOUTH, type))
                .setValue(EAST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.EAST, type))
                .setValue(WEST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.WEST, type))
                .setValue(UP, HbmFluidNetworks.canPipeConnect(level, pos, Direction.UP, type))
                .setValue(DOWN, HbmFluidNetworks.canPipeConnect(level, pos, Direction.DOWN, type));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            refreshAdjacentRenderSnapshots(level, pos);
        }
    }

    private static void refreshSolidConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock)) return;
        BlockEntity entity = level.getBlockEntity(pos);
        HbmFluidDefinition type = entity instanceof FluidPipeBlockEntity pipe ? pipe.type() : HbmFluids.none();
        BlockState next = state
                .setValue(NORTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.NORTH, type))
                .setValue(SOUTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.SOUTH, type))
                .setValue(EAST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.EAST, type))
                .setValue(WEST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.WEST, type))
                .setValue(UP, HbmFluidNetworks.canPipeConnect(level, pos, Direction.UP, type))
                .setValue(DOWN, HbmFluidNetworks.canPipeConnect(level, pos, Direction.DOWN, type));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            refreshAdjacentRenderSnapshots(level, pos);
        }
    }

    private static void refreshSolidSealedConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock)) return;
        BlockEntity entity = level.getBlockEntity(pos);
        HbmFluidDefinition type = entity instanceof FluidPipeBlockEntity pipe ? pipe.type() : HbmFluids.none();
        BlockState next = state
                .setValue(NORTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.NORTH, type))
                .setValue(SOUTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.SOUTH, type))
                .setValue(EAST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.EAST, type))
                .setValue(WEST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.WEST, type))
                .setValue(UP, HbmFluidNetworks.canPipeConnect(level, pos, Direction.UP, type))
                .setValue(DOWN, HbmFluidNetworks.canPipeConnect(level, pos, Direction.DOWN, type));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            refreshAdjacentRenderSnapshots(level, pos);
        }
    }

    private static void refreshValveConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock)) return;
        BlockEntity entity = level.getBlockEntity(pos);
        HbmFluidDefinition type = entity instanceof FluidPipeBlockEntity pipe ? pipe.type() : HbmFluids.none();
        BlockState next = state
                .setValue(NORTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.NORTH, type))
                .setValue(SOUTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.SOUTH, type))
                .setValue(EAST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.EAST, type))
                .setValue(WEST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.WEST, type))
                .setValue(UP, HbmFluidNetworks.canPipeConnect(level, pos, Direction.UP, type))
                .setValue(DOWN, HbmFluidNetworks.canPipeConnect(level, pos, Direction.DOWN, type));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            refreshAdjacentRenderSnapshots(level, pos);
        }
    }

    private static void refreshSwitchConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock)) return;
        BlockEntity entity = level.getBlockEntity(pos);
        HbmFluidDefinition type = entity instanceof FluidPipeBlockEntity pipe ? pipe.type() : HbmFluids.none();
        BlockState next = state
                .setValue(NORTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.NORTH, type))
                .setValue(SOUTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.SOUTH, type))
                .setValue(EAST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.EAST, type))
                .setValue(WEST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.WEST, type))
                .setValue(UP, HbmFluidNetworks.canPipeConnect(level, pos, Direction.UP, type))
                .setValue(DOWN, HbmFluidNetworks.canPipeConnect(level, pos, Direction.DOWN, type));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            refreshAdjacentRenderSnapshots(level, pos);
        }
    }

    private static void refreshCounterValveConnections(LevelAccessor level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidDuctBlock)) return;
        BlockEntity entity = level.getBlockEntity(pos);
        HbmFluidDefinition type = entity instanceof FluidPipeBlockEntity pipe ? pipe.type() : HbmFluids.none();
        BlockState next = state
                .setValue(NORTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.NORTH, type))
                .setValue(SOUTH, HbmFluidNetworks.canPipeConnect(level, pos, Direction.SOUTH, type))
                .setValue(EAST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.EAST, type))
                .setValue(WEST, HbmFluidNetworks.canPipeConnect(level, pos, Direction.WEST, type))
                .setValue(UP, HbmFluidNetworks.canPipeConnect(level, pos, Direction.UP, type))
                .setValue(DOWN, HbmFluidNetworks.canPipeConnect(level, pos, Direction.DOWN, type));
        if (next != state) {
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
            refreshAdjacentRenderSnapshots(level, pos);
        }
    }

    private static void refreshAdjacentRenderSnapshots(LevelAccessor level, BlockPos pipePos) {
        for (Direction direction : Direction.values()) {
            ClientRenderNeighborSnapshot.refreshAtOrDummyCore(level, pipePos.relative(direction));
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

    public void setOpen(Level level, BlockPos pos, boolean open, boolean playSound) {
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
        MK2(true),
        BOX(true),
        EXHAUST(true),
        GAUGE(true),
        PAINTABLE(true),
        PAINTABLE_EXHAUST(true),
        SOLID(true),
        SOLID_SEALED(true),
        VALVE(false),
        SWITCH(false),
        COUNTER_VALVE(false);

        private final boolean initialOpen;

        Kind(boolean initialOpen) {
            this.initialOpen = initialOpen;
        }

        public boolean initialOpen() {
            return initialOpen;
        }

        public boolean isExhaust() {
            return this == EXHAUST || this == PAINTABLE_EXHAUST;
        }
    }
}
