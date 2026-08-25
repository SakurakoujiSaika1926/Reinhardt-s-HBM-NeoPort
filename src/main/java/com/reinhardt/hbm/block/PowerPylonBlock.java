package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.PowerPylonBlockEntity;
import com.reinhardt.hbm.item.WiringRedCopperItem;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
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

import java.util.ArrayList;
import java.util.List;

public class PowerPylonBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final ThreadLocal<Boolean> RELOCATING = ThreadLocal.withInitial(() -> false);
    private static final double F = 1.0D / 16.0D;
    private static final VoxelShape CONNECTOR_UP = box(5, 0, 5, 11, 11, 11);
    private static final VoxelShape CONNECTOR_DOWN = box(5, 5, 5, 11, 16, 11);
    private static final VoxelShape CONNECTOR_SOUTH = box(5, 5, 0, 11, 11, 11);
    private static final VoxelShape CONNECTOR_NORTH = box(5, 5, 5, 11, 11, 16);
    private static final VoxelShape CONNECTOR_EAST = box(0, 5, 5, 11, 11, 11);
    private static final VoxelShape CONNECTOR_WEST = box(5, 5, 5, 16, 11, 11);
    private final Kind kind;

    public PowerPylonBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = this.kind.connector()
                ? context.getClickedFace()
                : context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        if (!canPlaceFootprint(context, state)) {
            return null;
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            BlockPos corePos = corePos(pos, facing);
            if (!corePos.equals(pos)) {
                RELOCATING.set(true);
                try {
                    level.removeBlock(pos, false);
                    level.setBlock(corePos, state, Block.UPDATE_ALL);
                } finally {
                    RELOCATING.set(false);
                }
            }
            placeDummies(level, corePos, state);
            pushEntitiesOutOfFootprint(level, corePos, state, placer);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerPylonBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return null;
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
        if (stack.getItem() instanceof WiringRedCopperItem) {
            return WiringRedCopperItem.useItemOnBlock(stack, level, player, pos);
        }
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PowerPylonBlockEntity pylon && pylon.setColorFrom(stack)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.containerMenu.broadcastChanges();
            }
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
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
        if (!state.is(newState.getBlock()) && !RELOCATING.get()) {
            if (level.getBlockEntity(pos) instanceof PowerPylonBlockEntity pylon) {
                pylon.disconnectAll();
            }
            if (!level.isClientSide) {
                removeDummies(level, pos, state);
            }
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (this.kind.connector()) {
            return connectorShape(state.getValue(FACING));
        }
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
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

    private boolean canPlaceFootprint(BlockPlaceContext context, BlockState state) {
        if (this.kind.footprintOffsets().isEmpty()) {
            return true;
        }
        Direction facing = state.getValue(FACING);
        BlockPos corePos = corePos(context.getClickedPos(), facing);
        for (BlockPos offset : this.kind.footprintOffsets()) {
            BlockPos testPos = corePos.offset(rotate(offset, facing));
            if (testPos.equals(context.getClickedPos())) {
                continue;
            }
            if (!context.getLevel().getBlockState(testPos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private BlockPos corePos(BlockPos placedPos, Direction facing) {
        return this.kind == Kind.SUBSTATION
                ? placedPos.relative(facing, -1)
                : placedPos;
    }

    private void placeDummies(Level level, BlockPos corePos, BlockState state) {
        Direction facing = state.getValue(FACING);
        for (BlockPos offset : this.kind.footprintOffsets()) {
            BlockPos pos = corePos.offset(rotate(offset, facing));
            if (pos.equals(corePos)) {
                continue;
            }
            level.setBlock(pos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    private void pushEntitiesOutOfFootprint(Level level, BlockPos corePos, BlockState state, LivingEntity placer) {
        if (this.kind.footprintOffsets().isEmpty()) {
            return;
        }
        Direction facing = state.getValue(FACING);
        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos offset : this.kind.footprintOffsets()) {
            positions.add(corePos.offset(rotate(offset, facing)));
        }
        LargeMachineBlock.pushEntitiesOutOfPositions(level, corePos, facing, positions, placer);
    }

    private void removeDummies(Level level, BlockPos corePos, BlockState state) {
        Direction facing = state.getValue(FACING);
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos offset : this.kind.footprintOffsets()) {
                BlockPos pos = corePos.offset(rotate(offset, facing));
                if (pos.equals(corePos)) {
                    continue;
                }
                if (level.getBlockState(pos).is(HbmBlocks.MACHINE_DUMMY.get())) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    private static BlockPos rotate(BlockPos pos, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            default -> pos;
        };
    }

    private static VoxelShape connectorShape(Direction facing) {
        return switch (facing) {
            case DOWN -> CONNECTOR_DOWN;
            case NORTH -> CONNECTOR_NORTH;
            case SOUTH -> CONNECTOR_SOUTH;
            case WEST -> CONNECTOR_WEST;
            case EAST -> CONNECTOR_EAST;
            default -> CONNECTOR_UP;
        };
    }

    public enum ConnectionType {
        SINGLE,
        TRIPLE,
        QUAD
    }

    public enum Kind {
        RED_CONNECTOR(true, ConnectionType.SINGLE, 10.0D, List.of()),
        CONNECTOR_RED_SUPER(true, ConnectionType.SINGLE, 100.0D, List.of()),
        RED_PYLON(false, ConnectionType.SINGLE, 25.0D, List.of()),
        RED_PYLON_MEDIUM_WOOD(false, ConnectionType.TRIPLE, 45.0D, footprint(6, 0, 0, 0, 0, 0)),
        RED_PYLON_MEDIUM_WOOD_TRANSFORMER(false, ConnectionType.TRIPLE, 45.0D, footprint(6, 0, 0, 0, 0, 0)),
        RED_PYLON_MEDIUM_STEEL(false, ConnectionType.TRIPLE, 45.0D, footprint(6, 0, 0, 0, 0, 0)),
        RED_PYLON_MEDIUM_STEEL_TRANSFORMER(false, ConnectionType.TRIPLE, 45.0D, footprint(6, 0, 0, 0, 0, 0)),
        RED_PYLON_LARGE(false, ConnectionType.QUAD, 100.0D, footprint(13, 0, 1, 1, 1, 1)),
        SUBSTATION(false, ConnectionType.QUAD, 20.0D, substationFootprint());

        private final boolean connector;
        private final ConnectionType connectionType;
        private final double maxWireLength;
        private final List<BlockPos> footprintOffsets;

        Kind(boolean connector, ConnectionType connectionType, double maxWireLength, List<BlockPos> footprintOffsets) {
            this.connector = connector;
            this.connectionType = connectionType;
            this.maxWireLength = maxWireLength;
            this.footprintOffsets = footprintOffsets;
        }

        public boolean connector() {
            return this.connector;
        }

        public ConnectionType connectionType() {
            return this.connectionType;
        }

        public double maxWireLength() {
            return this.maxWireLength;
        }

        public List<BlockPos> footprintOffsets() {
            return this.footprintOffsets;
        }

        public boolean hasTransformer() {
            return this == RED_PYLON_MEDIUM_WOOD_TRANSFORMER || this == RED_PYLON_MEDIUM_STEEL_TRANSFORMER;
        }
    }

    private static List<BlockPos> footprint(int up, int down, int north, int south, int west, int east) {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -west; x <= east; x++) {
            for (int y = -down; y <= up; y++) {
                for (int z = -north; z <= south; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static List<BlockPos> substationFootprint() {
        List<BlockPos> offsets = new ArrayList<>(footprint(4, 0, 1, 1, 2, 2));
        offsets.add(new BlockPos(1, 0, 1));
        offsets.add(new BlockPos(1, 0, -1));
        offsets.add(new BlockPos(-1, 0, 1));
        offsets.add(new BlockPos(-1, 0, -1));
        return List.copyOf(offsets);
    }
}
