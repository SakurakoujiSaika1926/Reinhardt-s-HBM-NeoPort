package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.HbmHeavyDoorBlockEntity;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorPartBlockEntity;
import com.reinhardt.hbm.door.HbmDoorDecl;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class HbmHeavyDoorBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final HbmDoorDecl decl;

    public HbmHeavyDoorBlock(Properties properties, HbmDoorDecl decl) {
        super(properties);
        this.decl = decl;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    public HbmDoorDecl decl() {
        return this.decl;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) {
            return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        }
        // BlockDummyable 1.7.10 stores the direction opposite the player's
        // facing (the multiblock extends toward the player).  Keep that exact
        // metadata/OBJ basis; using the raw placement direction rotates and
        // displaces every heavy-door model by 180 degrees.
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = corePosForClicked(context.getClickedPos(), facing, this.decl);
        if (!canPlaceAt(context.getLevel(), corePos, facing, this.decl, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    public static BlockPos corePosForClicked(BlockPos clickedPos, Direction facing, HbmDoorDecl decl) {
        int offset = -decl.blockOffset();
        return clickedPos.relative(facing, offset);
    }

    public static boolean canPlaceAt(Level level, BlockPos corePos, Direction facing, HbmDoorDecl decl, BlockPlaceContext context) {
        for (BlockPos worldPos : worldOffsets(decl, facing, corePos).keySet()) {
            if (worldPos.equals(corePos)) {
                continue;
            }
            if (!level.getBlockState(worldPos).canBeReplaced(context)) {
                return false;
            }
        }
        return level.getBlockState(corePos).canBeReplaced(context) || level.getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            placeParts(level, pos, facing, this.decl);
            LargeMachineBlock.pushEntitiesOutOfPositions(level, pos, facing, worldOffsets(this.decl, facing, pos).keySet(), placer);
        }
    }

    public static void placeParts(Level level, BlockPos corePos, Direction facing, HbmDoorDecl decl) {
        for (Map.Entry<BlockPos, BlockPos> entry : worldOffsets(decl, facing, corePos).entrySet()) {
            BlockPos worldPos = entry.getKey();
            if (worldPos.equals(corePos)) {
                continue;
            }
            BlockState partState = HbmBlocks.HEAVY_DOOR_PART.get()
                    .defaultBlockState()
                    .setValue(HbmHeavyDoorPartBlock.EXTRA, false);
            level.setBlock(worldPos, partState, Block.UPDATE_ALL);
            if (level.getBlockEntity(worldPos) instanceof HbmHeavyDoorPartBlockEntity part) {
                part.configure(corePos, entry.getValue());
            }
        }
    }

    public static Map<BlockPos, BlockPos> worldOffsets(HbmDoorDecl decl, Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addDimension(positions, corePos, facing, decl.dimensions());
        if (decl.extraDimensions() != null) {
            for (int[] dims : decl.extraDimensions()) {
                addDimension(positions, corePos, facing, new HbmDoorDecl.Dimensions(dims[0], dims[1], dims[2], dims[3], dims[4], dims[5]));
            }
        }
        return positions;
    }

    private static void addDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos, Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    BlockPos rotated = LegacyMachineGeometry.rotateLegacySouth(local, facing);
                    positions.putIfAbsent(corePos.offset(rotated), local);
                }
            }
        }
    }

    public static BlockPos rotateDoorOpening(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            default -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
        };
    }

    /**
     * Converts a dummy's world-relative position into the coordinate basis
     * consumed by the legacy DoorDecl#getBlockBound implementation.
     *
     * <p>The 1.7.10 code did not pass the footprint coordinate directly. It
     * applied {@code Rotation.getBlockRotation(dir).add(COUNTERCLOCKWISE_90)}
     * before selecting the open-door AABB. This extra quarter-turn is
     * intentionally different from the footprint transform and from the
     * opening-range transform above.</p>
     */
    public static BlockPos legacyCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            // Rotation.COUNTERCLOCKWISE_90: (x, z) -> (z, -x)
            case NORTH -> new BlockPos(
                    worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            // Rotation.CLOCKWISE_90: (x, z) -> (-z, x)
            case SOUTH -> new BlockPos(
                    -worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            // Rotation.CLOCKWISE_180: (x, z) -> (-x, -z)
            case EAST -> new BlockPos(
                    -worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            // Rotation.NONE
            case WEST, UP, DOWN -> worldRelative;
        };
    }

    public static boolean hasNeighborSignalAnywhere(Level level, BlockPos corePos, HbmHeavyDoorBlock doorBlock, Direction facing) {
        if (level.hasNeighborSignal(corePos)) {
            return true;
        }
        for (BlockPos pos : worldOffsets(doorBlock.decl, facing, corePos).keySet()) {
            if (!pos.equals(corePos) && level.hasNeighborSignal(pos)) {
                return true;
            }
        }
        return false;
    }

    public interface PrecomputedDoorPlacement {
        Direction hbmDoorFacing();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HbmHeavyDoorBlockEntity door) {
            door.toggle();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (ScrewdriverItem.isScrewdriver(stack) && player.isShiftKeyDown()) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof HbmHeavyDoorBlockEntity door) {
                door.cycleSkin(this.decl);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HbmHeavyDoorBlockEntity door) {
            door.toggle();
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HbmHeavyDoorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.HEAVY_DOOR.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> HbmHeavyDoorBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (HbmHeavyDoorBlockEntity) blockEntity
        );
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(this);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeAt(state, level, pos, false);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeAt(state, level, pos, true);
    }

    VoxelShape shapeAt(BlockState state, BlockGetter level, BlockPos pos, boolean collision) {
        if (!(level.getBlockEntity(pos) instanceof HbmHeavyDoorBlockEntity door)) {
            // A client can receive the block state one packet before its
            // block entity.  Returning a full cube here makes the camera
            // enter an apparently open door and triggers the inside-block
            // blackout.  The server keeps the legacy closed-door fallback;
            // the client remains passable until the authoritative BE arrives.
            if (level instanceof Level runtimeLevel && runtimeLevel.isClientSide) {
                return Shapes.empty();
            }
            return orientLegacyShape(this.decl.localShape(0, 0, 0, false, collision),
                    state.getValue(FACING));
        }
        boolean open = door.isOpenForCollision();
        VoxelShape localShape = this.decl.localShape(0, 0, 0, open, collision);
        return orientLegacyShape(localShape, state.getValue(FACING));
    }

    /**
     * Applies the exact per-facing AABB mapping used by the 1.7.10
     * BlockDoorGeneric#getBoundingBox switch.  This is intentionally an
     * explicit four-way mapping: the legacy code mirrors or swaps the local
     * box differently for each metadata direction.
     */
    public static VoxelShape orientLegacyShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(
                    1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(
                    1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(
                    box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            default -> Shapes.box(
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
        };
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HbmHeavyDoorBlockEntity door) {
            door.checkRedstoneNow();
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            removeParts(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    static void removeParts(Level level, BlockPos corePos, BlockState oldCoreState) {
        if (!(oldCoreState.getBlock() instanceof HbmHeavyDoorBlock doorBlock)) {
            return;
        }
        Direction facing = oldCoreState.getValue(FACING);
        HbmHeavyDoorPartBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos partPos : worldOffsets(doorBlock.decl(), facing, corePos).keySet()) {
                if (!partPos.equals(corePos)
                        && level.getBlockState(partPos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                        && level.getBlockEntity(partPos) instanceof HbmHeavyDoorPartBlockEntity part
                        && part.corePos().equals(corePos)) {
                    level.removeBlock(partPos, false);
                }
            }
        });
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
}
