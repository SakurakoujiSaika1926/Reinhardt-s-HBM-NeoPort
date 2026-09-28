package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.PowerPylonBlockEntity;
import com.reinhardt.hbm.item.WiringRedCopperItem;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
    private static final VoxelShape CONNECTOR_SUPER_UP = box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape CONNECTOR_SUPER_DOWN = box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape CONNECTOR_SUPER_NORTH = box(5, 5, 0, 11, 11, 16);
    private static final VoxelShape CONNECTOR_SUPER_SOUTH = box(5, 5, 0, 11, 11, 16);
    private static final VoxelShape CONNECTOR_SUPER_EAST = box(0, 5, 5, 16, 11, 11);
    private static final VoxelShape CONNECTOR_SUPER_WEST = box(0, 5, 5, 16, 11, 11);
    private final Kind kind;

    public PowerPylonBlock(Properties properties, Kind kind) {
        super(LargeMachineBlock.nonOccludingMachineProperties(properties));
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
        Direction facing = switch (this.kind) {
            case RED_CONNECTOR -> redConnectorPlacementFacing(context);
            case CONNECTOR_RED_SUPER -> connectorRedSuperPlacementFacing(context);
            case RED_PYLON -> redPylonPlacementFacing(context);
            case RED_PYLON_STEEL -> redPylonSteelPlacementFacing(context);
            case RED_PYLON_MEDIUM_WOOD -> redPylonMediumWoodPlacementFacing(context);
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> redPylonMediumWoodTransformerPlacementFacing(context);
            case RED_PYLON_MEDIUM_STEEL -> redPylonMediumSteelPlacementFacing(context);
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> redPylonMediumSteelTransformerPlacementFacing(context);
            case RED_PYLON_LARGE -> redPylonLargePlacementFacing(context);
            case SUBSTATION -> substationPlacementFacing(context);
        };
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
        VoxelShape authoredShape = switch (this.kind) {
            case RED_CONNECTOR -> redConnectorShape(state.getValue(FACING));
            case CONNECTOR_RED_SUPER -> connectorRedSuperShape(state.getValue(FACING));
            default -> Shapes.block();
        };
        return authoredShape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** OBJ-rendered pylons/substations occupy large footprints but should not create invisible light blockers. */
    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (this.kind) {
            case RED_CONNECTOR -> rotateRedConnector(state, rotation);
            case CONNECTOR_RED_SUPER -> rotateConnectorRedSuper(state, rotation);
            case RED_PYLON -> rotateRedPylon(state, rotation);
            case RED_PYLON_STEEL -> rotateRedPylonSteel(state, rotation);
            case RED_PYLON_MEDIUM_WOOD -> rotateRedPylonMediumWood(state, rotation);
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> rotateRedPylonMediumWoodTransformer(state, rotation);
            case RED_PYLON_MEDIUM_STEEL -> rotateRedPylonMediumSteel(state, rotation);
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> rotateRedPylonMediumSteelTransformer(state, rotation);
            case RED_PYLON_LARGE -> rotateRedPylonLarge(state, rotation);
            case SUBSTATION -> rotateSubstation(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.kind) {
            case RED_CONNECTOR -> mirrorRedConnector(state, mirror);
            case CONNECTOR_RED_SUPER -> mirrorConnectorRedSuper(state, mirror);
            case RED_PYLON -> mirrorRedPylon(state, mirror);
            case RED_PYLON_STEEL -> mirrorRedPylonSteel(state, mirror);
            case RED_PYLON_MEDIUM_WOOD -> mirrorRedPylonMediumWood(state, mirror);
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> mirrorRedPylonMediumWoodTransformer(state, mirror);
            case RED_PYLON_MEDIUM_STEEL -> mirrorRedPylonMediumSteel(state, mirror);
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> mirrorRedPylonMediumSteelTransformer(state, mirror);
            case RED_PYLON_LARGE -> mirrorRedPylonLarge(state, mirror);
            case SUBSTATION -> mirrorSubstation(state, mirror);
        };
    }

    private static BlockState rotateRedConnector(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateConnectorRedSuper(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateRedPylon(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateRedPylonSteel(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateRedPylonMediumWood(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateRedPylonMediumWoodTransformer(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateRedPylonMediumSteel(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateRedPylonMediumSteelTransformer(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateRedPylonLarge(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSubstation(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorRedConnector(BlockState state, Mirror mirror) {
        return rotateRedConnector(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorConnectorRedSuper(BlockState state, Mirror mirror) {
        return rotateConnectorRedSuper(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorRedPylon(BlockState state, Mirror mirror) {
        return rotateRedPylon(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorRedPylonSteel(BlockState state, Mirror mirror) {
        return rotateRedPylonSteel(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorRedPylonMediumWood(BlockState state, Mirror mirror) {
        return rotateRedPylonMediumWood(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorRedPylonMediumWoodTransformer(BlockState state, Mirror mirror) {
        return rotateRedPylonMediumWoodTransformer(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorRedPylonMediumSteel(BlockState state, Mirror mirror) {
        return rotateRedPylonMediumSteel(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorRedPylonMediumSteelTransformer(BlockState state, Mirror mirror) {
        return rotateRedPylonMediumSteelTransformer(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorRedPylonLarge(BlockState state, Mirror mirror) {
        return rotateRedPylonLarge(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSubstation(BlockState state, Mirror mirror) {
        return rotateSubstation(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private boolean canPlaceFootprint(BlockPlaceContext context, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos corePos = corePos(context.getClickedPos(), facing);
        for (BlockPos testPos : occupiedPositions(corePos, facing)) {
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
        return switch (this.kind) {
            case RED_CONNECTOR -> redConnectorCorePos(placedPos, facing);
            case CONNECTOR_RED_SUPER -> connectorRedSuperCorePos(placedPos, facing);
            case RED_PYLON -> redPylonCorePos(placedPos, facing);
            case RED_PYLON_STEEL -> redPylonSteelCorePos(placedPos, facing);
            case RED_PYLON_MEDIUM_WOOD -> redPylonMediumWoodCorePos(placedPos, facing);
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> redPylonMediumWoodTransformerCorePos(placedPos, facing);
            case RED_PYLON_MEDIUM_STEEL -> redPylonMediumSteelCorePos(placedPos, facing);
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> redPylonMediumSteelTransformerCorePos(placedPos, facing);
            case RED_PYLON_LARGE -> redPylonLargeCorePos(placedPos, facing);
            case SUBSTATION -> substationCorePos(placedPos, facing);
        };
    }

    private void placeDummies(Level level, BlockPos corePos, BlockState state) {
        Direction facing = state.getValue(FACING);
        for (BlockPos pos : occupiedPositions(corePos, facing)) {
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
        Direction facing = state.getValue(FACING);
        List<BlockPos> positions = occupiedPositions(corePos, facing);
        LargeMachineBlock.pushEntitiesOutOfPositions(level, corePos, facing, positions, placer);
    }

    private void removeDummies(Level level, BlockPos corePos, BlockState state) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        Direction facing = state.getValue(FACING);
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos pos : occupiedPositions(corePos, facing)) {
                if (pos.equals(corePos)) {
                    continue;
                }
                if (level.getBlockState(pos).is(HbmBlocks.MACHINE_DUMMY.get())) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    private List<BlockPos> occupiedPositions(BlockPos corePos, Direction facing) {
        return switch (this.kind) {
            case RED_CONNECTOR -> redConnectorOccupiedPositions(corePos, facing);
            case CONNECTOR_RED_SUPER -> connectorRedSuperOccupiedPositions(corePos, facing);
            case RED_PYLON -> redPylonOccupiedPositions(corePos, facing);
            case RED_PYLON_STEEL -> redPylonSteelOccupiedPositions(corePos, facing);
            case RED_PYLON_MEDIUM_WOOD -> redPylonMediumWoodOccupiedPositions(corePos, facing);
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> redPylonMediumWoodTransformerOccupiedPositions(corePos, facing);
            case RED_PYLON_MEDIUM_STEEL -> redPylonMediumSteelOccupiedPositions(corePos, facing);
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> redPylonMediumSteelTransformerOccupiedPositions(corePos, facing);
            case RED_PYLON_LARGE -> redPylonLargeOccupiedPositions(corePos, facing);
            case SUBSTATION -> substationOccupiedPositions(corePos, facing);
        };
    }

    private static Direction redConnectorPlacementFacing(BlockPlaceContext context) {
        return context.getClickedFace();
    }

    private static Direction connectorRedSuperPlacementFacing(BlockPlaceContext context) {
        return context.getClickedFace();
    }

    private static Direction redPylonPlacementFacing(BlockPlaceContext context) {
        return context.getHorizontalDirection().getOpposite();
    }

    private static Direction redPylonSteelPlacementFacing(BlockPlaceContext context) {
        return context.getHorizontalDirection().getOpposite();
    }

    private static Direction redPylonMediumWoodPlacementFacing(BlockPlaceContext context) {
        return context.getHorizontalDirection().getOpposite();
    }

    private static Direction redPylonMediumWoodTransformerPlacementFacing(BlockPlaceContext context) {
        return context.getHorizontalDirection().getOpposite();
    }

    private static Direction redPylonMediumSteelPlacementFacing(BlockPlaceContext context) {
        return context.getHorizontalDirection().getOpposite();
    }

    private static Direction redPylonMediumSteelTransformerPlacementFacing(BlockPlaceContext context) {
        return context.getHorizontalDirection().getOpposite();
    }

    private static Direction redPylonLargePlacementFacing(BlockPlaceContext context) {
        // PylonLarge#getMetaForCore uses yaw * 4 / 180 (not the ordinary / 360
        // dummy-footprint direction).  Preserve its 45-degree placement sectors
        // because EAST/WEST select the legacy 45/135-degree tower poses.
        int legacyIndex = Mth.floor(context.getRotation() * 4.0F / 180.0F + 0.5D) & 3;
        return switch (legacyIndex) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> throw new IllegalStateException("Unexpected legacy large-pylon direction " + legacyIndex);
        };
    }

    private static Direction substationPlacementFacing(BlockPlaceContext context) {
        return context.getHorizontalDirection().getOpposite();
    }

    private static BlockPos redConnectorCorePos(BlockPos placedPos, Direction facing) {
        return placedPos;
    }

    private static BlockPos connectorRedSuperCorePos(BlockPos placedPos, Direction facing) {
        return placedPos;
    }

    private static BlockPos redPylonCorePos(BlockPos placedPos, Direction facing) {
        return placedPos;
    }

    private static BlockPos redPylonSteelCorePos(BlockPos placedPos, Direction facing) {
        return placedPos;
    }

    private static BlockPos redPylonMediumWoodCorePos(BlockPos placedPos, Direction facing) {
        return placedPos;
    }

    private static BlockPos redPylonMediumWoodTransformerCorePos(BlockPos placedPos, Direction facing) {
        return placedPos;
    }

    private static BlockPos redPylonMediumSteelCorePos(BlockPos placedPos, Direction facing) {
        return placedPos;
    }

    private static BlockPos redPylonMediumSteelTransformerCorePos(BlockPos placedPos, Direction facing) {
        return placedPos;
    }

    private static BlockPos redPylonLargeCorePos(BlockPos placedPos, Direction facing) {
        return placedPos;
    }

    private static BlockPos substationCorePos(BlockPos placedPos, Direction facing) {
        return switch (facing) {
            case NORTH -> placedPos.south();
            case SOUTH -> placedPos.north();
            case WEST -> placedPos.east();
            case EAST -> placedPos.west();
            case UP -> placedPos.below();
            case DOWN -> placedPos.above();
        };
    }

    private static List<BlockPos> redConnectorOccupiedPositions(BlockPos corePos, Direction facing) {
        return List.of();
    }

    private static List<BlockPos> connectorRedSuperOccupiedPositions(BlockPos corePos, Direction facing) {
        return List.of();
    }

    private static List<BlockPos> redPylonOccupiedPositions(BlockPos corePos, Direction facing) {
        List<BlockPos> positions = new ArrayList<>(5);
        for (int y = 0; y <= 4; y++) {
            positions.add(corePos.above(y));
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> redPylonSteelOccupiedPositions(BlockPos corePos, Direction facing) {
        List<BlockPos> positions = new ArrayList<>(5);
        for (int y = 0; y <= 4; y++) {
            positions.add(corePos.above(y));
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> redPylonMediumWoodOccupiedPositions(BlockPos corePos, Direction facing) {
        return switch (facing) {
            case NORTH -> redPylonMediumWoodVerticalPositions(corePos);
            case SOUTH -> redPylonMediumWoodVerticalPositions(corePos);
            case WEST -> redPylonMediumWoodVerticalPositions(corePos);
            case EAST -> redPylonMediumWoodVerticalPositions(corePos);
            case UP -> redPylonMediumWoodVerticalPositions(corePos);
            case DOWN -> redPylonMediumWoodVerticalPositions(corePos);
        };
    }

    private static List<BlockPos> redPylonMediumWoodVerticalPositions(BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>(7);
        for (int y = 0; y <= 6; y++) {
            positions.add(corePos.above(y));
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> redPylonMediumWoodTransformerOccupiedPositions(BlockPos corePos, Direction facing) {
        return switch (facing) {
            case NORTH -> redPylonMediumWoodTransformerVerticalPositions(corePos);
            case SOUTH -> redPylonMediumWoodTransformerVerticalPositions(corePos);
            case WEST -> redPylonMediumWoodTransformerVerticalPositions(corePos);
            case EAST -> redPylonMediumWoodTransformerVerticalPositions(corePos);
            case UP -> redPylonMediumWoodTransformerVerticalPositions(corePos);
            case DOWN -> redPylonMediumWoodTransformerVerticalPositions(corePos);
        };
    }

    private static List<BlockPos> redPylonMediumWoodTransformerVerticalPositions(BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>(7);
        for (int y = 0; y <= 6; y++) {
            positions.add(corePos.above(y));
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> redPylonMediumSteelOccupiedPositions(BlockPos corePos, Direction facing) {
        return switch (facing) {
            case NORTH -> redPylonMediumSteelVerticalPositions(corePos);
            case SOUTH -> redPylonMediumSteelVerticalPositions(corePos);
            case WEST -> redPylonMediumSteelVerticalPositions(corePos);
            case EAST -> redPylonMediumSteelVerticalPositions(corePos);
            case UP -> redPylonMediumSteelVerticalPositions(corePos);
            case DOWN -> redPylonMediumSteelVerticalPositions(corePos);
        };
    }

    private static List<BlockPos> redPylonMediumSteelVerticalPositions(BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>(7);
        for (int y = 0; y <= 6; y++) {
            positions.add(corePos.above(y));
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> redPylonMediumSteelTransformerOccupiedPositions(BlockPos corePos, Direction facing) {
        return switch (facing) {
            case NORTH -> redPylonMediumSteelTransformerVerticalPositions(corePos);
            case SOUTH -> redPylonMediumSteelTransformerVerticalPositions(corePos);
            case WEST -> redPylonMediumSteelTransformerVerticalPositions(corePos);
            case EAST -> redPylonMediumSteelTransformerVerticalPositions(corePos);
            case UP -> redPylonMediumSteelTransformerVerticalPositions(corePos);
            case DOWN -> redPylonMediumSteelTransformerVerticalPositions(corePos);
        };
    }

    private static List<BlockPos> redPylonMediumSteelTransformerVerticalPositions(BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>(7);
        for (int y = 0; y <= 6; y++) {
            positions.add(corePos.above(y));
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> redPylonLargeOccupiedPositions(BlockPos corePos, Direction facing) {
        return switch (facing) {
            case NORTH -> redPylonLargeNorthPositions(corePos);
            case SOUTH -> redPylonLargeSouthPositions(corePos);
            case WEST -> redPylonLargeWestPositions(corePos);
            case EAST -> redPylonLargeEastPositions(corePos);
            case UP -> redPylonLargeSouthPositions(corePos);
            case DOWN -> redPylonLargeSouthPositions(corePos);
        };
    }

    private static List<BlockPos> redPylonLargeNorthPositions(BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>(126);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 13; y++) {
                for (int z = -1; z <= 1; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> redPylonLargeSouthPositions(BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>(126);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 13; y++) {
                for (int z = -1; z <= 1; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> redPylonLargeWestPositions(BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>(126);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 13; y++) {
                for (int z = -1; z <= 1; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> redPylonLargeEastPositions(BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>(126);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 13; y++) {
                for (int z = -1; z <= 1; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> substationOccupiedPositions(BlockPos corePos, Direction facing) {
        return switch (facing) {
            case NORTH -> substationNorthPositions(corePos);
            case SOUTH -> substationSouthPositions(corePos);
            case WEST -> substationWestPositions(corePos);
            case EAST -> substationEastPositions(corePos);
            case UP -> substationSouthPositions(corePos);
            case DOWN -> substationSouthPositions(corePos);
        };
    }

    private static List<BlockPos> substationNorthPositions(BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>(75);
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -1; z <= 1; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> substationSouthPositions(BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>(75);
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -1; z <= 1; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> substationWestPositions(BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>(75);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -2; z <= 2; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
        }
        return List.copyOf(positions);
    }

    private static List<BlockPos> substationEastPositions(BlockPos corePos) {
        List<BlockPos> positions = new ArrayList<>(75);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -2; z <= 2; z++) {
                    positions.add(corePos.offset(x, y, z));
                }
            }
        }
        return List.copyOf(positions);
    }

    private static VoxelShape redConnectorShape(Direction facing) {
        return switch (facing) {
            case DOWN -> CONNECTOR_DOWN;
            case NORTH -> CONNECTOR_NORTH;
            case SOUTH -> CONNECTOR_SOUTH;
            case WEST -> CONNECTOR_WEST;
            case EAST -> CONNECTOR_EAST;
            default -> CONNECTOR_UP;
        };
    }

    private static VoxelShape connectorRedSuperShape(Direction facing) {
        return switch (facing) {
            case DOWN -> CONNECTOR_SUPER_DOWN;
            case NORTH -> CONNECTOR_SUPER_NORTH;
            case SOUTH -> CONNECTOR_SUPER_SOUTH;
            case WEST -> CONNECTOR_SUPER_WEST;
            case EAST -> CONNECTOR_SUPER_EAST;
            case UP -> CONNECTOR_SUPER_UP;
        };
    }

    public enum ConnectionType {
        SINGLE,
        TRIPLE,
        QUAD
    }

    public enum Kind {
        RED_CONNECTOR(true, ConnectionType.SINGLE, 10.0D),
        CONNECTOR_RED_SUPER(true, ConnectionType.SINGLE, 100.0D),
        RED_PYLON(false, ConnectionType.SINGLE, 25.0D),
        RED_PYLON_STEEL(false, ConnectionType.SINGLE, 25.0D),
        RED_PYLON_MEDIUM_WOOD(false, ConnectionType.TRIPLE, 45.0D),
        RED_PYLON_MEDIUM_WOOD_TRANSFORMER(false, ConnectionType.TRIPLE, 45.0D),
        RED_PYLON_MEDIUM_STEEL(false, ConnectionType.TRIPLE, 45.0D),
        RED_PYLON_MEDIUM_STEEL_TRANSFORMER(false, ConnectionType.TRIPLE, 45.0D),
        RED_PYLON_LARGE(false, ConnectionType.QUAD, 100.0D),
        SUBSTATION(false, ConnectionType.QUAD, 20.0D);

        private final boolean connector;
        private final ConnectionType connectionType;
        private final double maxWireLength;

        Kind(boolean connector, ConnectionType connectionType, double maxWireLength) {
            this.connector = connector;
            this.connectionType = connectionType;
            this.maxWireLength = maxWireLength;
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

        public boolean hasTransformer() {
            return this == RED_PYLON_MEDIUM_WOOD_TRANSFORMER || this == RED_PYLON_MEDIUM_STEEL_TRANSFORMER;
        }
    }
}
