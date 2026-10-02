package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.HbmHeavyDoorBlockEntity;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorPartBlockEntity;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.door.HbmDoorDecl;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
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
        return switch (this.decl) {
            case FIRE_DOOR -> fireDoorStateForPlacement(context);
            case TRANSITION_SEAL -> transitionSealStateForPlacement(context);
            case SLIDING_BLAST_DOOR -> slidingBlastDoorStateForPlacement(context);
            case SLIDING_BLAST_DOOR_2 -> slidingBlastDoor2StateForPlacement(context);
            case SLIDING_GATE_DOOR -> slidingGateDoorStateForPlacement(context);
            case QE_SLIDING_DOOR -> qeSlidingDoorStateForPlacement(context);
            case QE_CONTAINMENT -> qeContainmentStateForPlacement(context);
            case SLIDING_SEAL_DOOR -> slidingSealDoorStateForPlacement(context);
            case SECURE_ACCESS_DOOR -> secureAccessDoorStateForPlacement(context);
            case ROUND_AIRLOCK_DOOR -> roundAirlockDoorStateForPlacement(context);
            case LARGE_VEHICLE_DOOR -> largeVehicleDoorStateForPlacement(context);
            case VAULT_DOOR -> vaultDoorStateForPlacement(context);
            case WATER_DOOR -> waterDoorStateForPlacement(context);
            case CARGO_DOOR -> cargoDoorStateForPlacement(context);
            case SILO_HATCH -> siloHatchStateForPlacement(context);
            case SILO_HATCH_LARGE -> siloHatchLargeStateForPlacement(context);
        };
    }

    private BlockState fireDoorStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) {
            return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        }
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = fireDoorCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : fireDoorWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState transitionSealStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = transitionSealCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : transitionSealWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState slidingBlastDoorStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = slidingBlastDoorCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : slidingBlastDoorWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState slidingBlastDoor2StateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = slidingBlastDoor2CorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : slidingBlastDoor2WorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState slidingGateDoorStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = slidingGateDoorCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : slidingGateDoorWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState qeSlidingDoorStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = qeSlidingDoorCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : qeSlidingDoorWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState qeContainmentStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = qeContainmentCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : qeContainmentWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState slidingSealDoorStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = slidingSealDoorCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : slidingSealDoorWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState secureAccessDoorStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = secureAccessDoorCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : secureAccessDoorWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState roundAirlockDoorStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = roundAirlockDoorCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : roundAirlockDoorWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState largeVehicleDoorStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = largeVehicleDoorCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : largeVehicleDoorWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState vaultDoorStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = vaultDoorCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : vaultDoorWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState waterDoorStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = waterDoorCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : waterDoorWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState cargoDoorStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = cargoDoorCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : cargoDoorWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState siloHatchStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = siloHatchCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : siloHatchWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    private BlockState siloHatchLargeStateForPlacement(BlockPlaceContext context) {
        if (context instanceof PrecomputedDoorPlacement precomputed) return this.defaultBlockState().setValue(FACING, precomputed.hbmDoorFacing());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = siloHatchLargeCorePosForClicked(context.getClickedPos(), facing);
        for (BlockPos worldPos : siloHatchLargeWorldOffsets(facing, corePos).keySet()) {
            if (!worldPos.equals(corePos) && !context.getLevel().getBlockState(worldPos).canBeReplaced(context)) return null;
        }
        return context.getLevel().getBlockState(corePos).canBeReplaced(context)
                || context.getLevel().getBlockState(corePos).is(HbmBlocks.HEAVY_DOOR_PART.get())
                ? this.defaultBlockState().setValue(FACING, facing) : null;
    }

    public static BlockPos corePosForClicked(BlockPos clickedPos, Direction facing, HbmDoorDecl decl) {
        return switch (decl) {
            case FIRE_DOOR -> fireDoorCorePosForClicked(clickedPos, facing);
            case TRANSITION_SEAL -> transitionSealCorePosForClicked(clickedPos, facing);
            case SLIDING_BLAST_DOOR -> slidingBlastDoorCorePosForClicked(clickedPos, facing);
            case SLIDING_BLAST_DOOR_2 -> slidingBlastDoor2CorePosForClicked(clickedPos, facing);
            case SLIDING_GATE_DOOR -> slidingGateDoorCorePosForClicked(clickedPos, facing);
            case QE_SLIDING_DOOR -> qeSlidingDoorCorePosForClicked(clickedPos, facing);
            case QE_CONTAINMENT -> qeContainmentCorePosForClicked(clickedPos, facing);
            case SLIDING_SEAL_DOOR -> slidingSealDoorCorePosForClicked(clickedPos, facing);
            case SECURE_ACCESS_DOOR -> secureAccessDoorCorePosForClicked(clickedPos, facing);
            case ROUND_AIRLOCK_DOOR -> roundAirlockDoorCorePosForClicked(clickedPos, facing);
            case LARGE_VEHICLE_DOOR -> largeVehicleDoorCorePosForClicked(clickedPos, facing);
            case VAULT_DOOR -> vaultDoorCorePosForClicked(clickedPos, facing);
            case WATER_DOOR -> waterDoorCorePosForClicked(clickedPos, facing);
            case CARGO_DOOR -> cargoDoorCorePosForClicked(clickedPos, facing);
            case SILO_HATCH -> siloHatchCorePosForClicked(clickedPos, facing);
            case SILO_HATCH_LARGE -> siloHatchLargeCorePosForClicked(clickedPos, facing);
        };
    }

    private static BlockPos fireDoorCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos transitionSealCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos slidingBlastDoorCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos slidingBlastDoor2CorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos slidingGateDoorCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos qeSlidingDoorCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos qeContainmentCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos slidingSealDoorCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos secureAccessDoorCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos roundAirlockDoorCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos largeVehicleDoorCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos vaultDoorCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos waterDoorCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos cargoDoorCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos;
    }

    private static BlockPos siloHatchCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos.relative(facing, -2);
    }

    private static BlockPos siloHatchLargeCorePosForClicked(BlockPos clickedPos, Direction facing) {
        return clickedPos.relative(facing, -3);
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
        return switch (decl) {
            case FIRE_DOOR -> fireDoorWorldOffsets(facing, corePos);
            case TRANSITION_SEAL -> transitionSealWorldOffsets(facing, corePos);
            case SLIDING_BLAST_DOOR -> slidingBlastDoorWorldOffsets(facing, corePos);
            case SLIDING_BLAST_DOOR_2 -> slidingBlastDoor2WorldOffsets(facing, corePos);
            case SLIDING_GATE_DOOR -> slidingGateDoorWorldOffsets(facing, corePos);
            case QE_SLIDING_DOOR -> qeSlidingDoorWorldOffsets(facing, corePos);
            case QE_CONTAINMENT -> qeContainmentWorldOffsets(facing, corePos);
            case SLIDING_SEAL_DOOR -> slidingSealDoorWorldOffsets(facing, corePos);
            case SECURE_ACCESS_DOOR -> secureAccessDoorWorldOffsets(facing, corePos);
            case ROUND_AIRLOCK_DOOR -> roundAirlockDoorWorldOffsets(facing, corePos);
            case LARGE_VEHICLE_DOOR -> largeVehicleDoorWorldOffsets(facing, corePos);
            case VAULT_DOOR -> vaultDoorWorldOffsets(facing, corePos);
            case WATER_DOOR -> waterDoorWorldOffsets(facing, corePos);
            case CARGO_DOOR -> cargoDoorWorldOffsets(facing, corePos);
            case SILO_HATCH -> siloHatchWorldOffsets(facing, corePos);
            case SILO_HATCH_LARGE -> siloHatchLargeWorldOffsets(facing, corePos);
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
    private static Map<BlockPos, BlockPos> fireDoorWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addFireDoorDimension(positions, corePos, facing, HbmDoorDecl.FIRE_DOOR.dimensions());
        return positions;
    }

    private static void addFireDoorDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapFireDoorFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapFireDoorFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("fire door requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> transitionSealWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addTransitionSealDimension(positions, corePos, facing, HbmDoorDecl.TRANSITION_SEAL.dimensions());
        return positions;
    }

    private static void addTransitionSealDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapTransitionSealFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapTransitionSealFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("transition seal requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> slidingBlastDoorWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addSlidingBlastDoorDimension(positions, corePos, facing, HbmDoorDecl.SLIDING_BLAST_DOOR.dimensions());
        return positions;
    }

    private static void addSlidingBlastDoorDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapSlidingBlastDoorFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapSlidingBlastDoorFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("sliding blast door requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> slidingBlastDoor2WorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addSlidingBlastDoor2Dimension(positions, corePos, facing, HbmDoorDecl.SLIDING_BLAST_DOOR_2.dimensions());
        return positions;
    }

    private static void addSlidingBlastDoor2Dimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapSlidingBlastDoor2FootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapSlidingBlastDoor2FootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("sliding blast door 2 requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> slidingGateDoorWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addSlidingGateDoorDimension(positions, corePos, facing, HbmDoorDecl.SLIDING_GATE_DOOR.dimensions());
        return positions;
    }

    private static void addSlidingGateDoorDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapSlidingGateDoorFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapSlidingGateDoorFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("sliding gate door requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> qeSlidingDoorWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addQeSlidingDoorDimension(positions, corePos, facing, HbmDoorDecl.QE_SLIDING_DOOR.dimensions());
        return positions;
    }

    private static void addQeSlidingDoorDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapQeSlidingDoorFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapQeSlidingDoorFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("QE sliding door requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> qeContainmentWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addQeContainmentDimension(positions, corePos, facing, HbmDoorDecl.QE_CONTAINMENT.dimensions());
        return positions;
    }

    private static void addQeContainmentDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapQeContainmentFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapQeContainmentFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("QE containment door requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> slidingSealDoorWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addSlidingSealDoorDimension(positions, corePos, facing, HbmDoorDecl.SLIDING_SEAL_DOOR.dimensions());
        return positions;
    }

    private static void addSlidingSealDoorDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapSlidingSealDoorFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapSlidingSealDoorFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("sliding seal door requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> secureAccessDoorWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addSecureAccessDoorDimension(positions, corePos, facing, HbmDoorDecl.SECURE_ACCESS_DOOR.dimensions());
        return positions;
    }

    private static void addSecureAccessDoorDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapSecureAccessDoorFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapSecureAccessDoorFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("secure access door requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> roundAirlockDoorWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addRoundAirlockDoorDimension(positions, corePos, facing, HbmDoorDecl.ROUND_AIRLOCK_DOOR.dimensions());
        return positions;
    }

    private static void addRoundAirlockDoorDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapRoundAirlockDoorFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapRoundAirlockDoorFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("round airlock door requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> largeVehicleDoorWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addLargeVehicleDoorDimension(positions, corePos, facing, HbmDoorDecl.LARGE_VEHICLE_DOOR.dimensions());
        return positions;
    }

    private static void addLargeVehicleDoorDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapLargeVehicleDoorFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapLargeVehicleDoorFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("large vehicle door requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> vaultDoorWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addVaultDoorDimension(positions, corePos, facing, HbmDoorDecl.VAULT_DOOR.dimensions());
        for (int[] dims : HbmDoorDecl.VAULT_DOOR.extraDimensions()) {
            addVaultDoorDimension(positions, corePos, facing,
                    new HbmDoorDecl.Dimensions(dims[0], dims[1], dims[2], dims[3], dims[4], dims[5]));
        }
        return positions;
    }

    private static void addVaultDoorDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapVaultDoorFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapVaultDoorFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("vault door requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> waterDoorWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addWaterDoorDimension(positions, corePos, facing, HbmDoorDecl.WATER_DOOR.dimensions());
        return positions;
    }

    private static void addWaterDoorDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapWaterDoorFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapWaterDoorFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("water door requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> cargoDoorWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addCargoDoorDimension(positions, corePos, facing, HbmDoorDecl.CARGO_DOOR.dimensions());
        return positions;
    }

    private static void addCargoDoorDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                              Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapCargoDoorFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapCargoDoorFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("cargo door requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> siloHatchWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addSiloHatchDimension(positions, corePos, facing, HbmDoorDecl.SILO_HATCH.dimensions());
        return positions;
    }

    private static void addSiloHatchDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapSiloHatchFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapSiloHatchFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("silo hatch requires a horizontal facing: " + facing);
        };
    }

    private static Map<BlockPos, BlockPos> siloHatchLargeWorldOffsets(Direction facing, BlockPos corePos) {
        LinkedHashMap<BlockPos, BlockPos> positions = new LinkedHashMap<>();
        addSiloHatchLargeDimension(positions, corePos, facing, HbmDoorDecl.SILO_HATCH_LARGE.dimensions());
        return positions;
    }

    private static void addSiloHatchLargeDimension(Map<BlockPos, BlockPos> positions, BlockPos corePos,
                                               Direction facing, HbmDoorDecl.Dimensions dims) {
        for (int y = -dims.down(); y <= dims.up(); y++) {
            for (int x = -dims.west(); x <= dims.east(); x++) {
                for (int z = -dims.north(); z <= dims.south(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    positions.putIfAbsent(corePos.offset(mapSiloHatchLargeFootprintOffset(facing, local)), local);
                }
            }
        }
    }

    private static BlockPos mapSiloHatchLargeFootprintOffset(Direction facing, BlockPos offset) {
        int x = offset.getX();
        int y = offset.getY();
        int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z);
            case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("large silo hatch requires a horizontal facing: " + facing);
        };
    }


    public static BlockPos fireDoorOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("fire door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos fireDoorCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("fire door requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientFireDoorShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("fire door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos transitionSealOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("transition seal requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos transitionSealCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("transition seal requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientTransitionSealShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("transition seal requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos slidingBlastDoorOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("sliding blast door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos slidingBlastDoorCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("sliding blast door requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientSlidingBlastDoorShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("sliding blast door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos slidingBlastDoor2OpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("sliding blast door 2 requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos slidingBlastDoor2CollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("sliding blast door 2 requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientSlidingBlastDoor2Shape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("sliding blast door 2 requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos slidingGateDoorOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("sliding gate door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos slidingGateDoorCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("sliding gate door requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientSlidingGateDoorShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("sliding gate door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos qeSlidingDoorOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("QE sliding door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos qeSlidingDoorCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("QE sliding door requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientQeSlidingDoorShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("QE sliding door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos qeContainmentOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("QE containment door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos qeContainmentCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("QE containment door requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientQeContainmentShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("QE containment door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos slidingSealDoorOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("sliding seal door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos slidingSealDoorCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("sliding seal door requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientSlidingSealDoorShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("sliding seal door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos secureAccessDoorOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("secure access door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos secureAccessDoorCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("secure access door requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientSecureAccessDoorShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("secure access door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos roundAirlockDoorOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("round airlock door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos roundAirlockDoorCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("round airlock door requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientRoundAirlockDoorShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("round airlock door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos largeVehicleDoorOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("large vehicle door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos largeVehicleDoorCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("large vehicle door requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientLargeVehicleDoorShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("large vehicle door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos vaultDoorOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("vault door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos vaultDoorCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("vault door requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientVaultDoorShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("vault door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos waterDoorOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("water door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos waterDoorCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("water door requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientWaterDoorShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("water door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos cargoDoorOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("cargo door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos cargoDoorCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("cargo door requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientCargoDoorShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("cargo door requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos siloHatchOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("silo hatch requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos siloHatchCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("silo hatch requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientSiloHatchShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("silo hatch requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos siloHatchLargeOpeningOffset(BlockPos local, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-local.getZ(), local.getY(), local.getX());
            case NORTH -> local;
            case WEST -> new BlockPos(local.getZ(), local.getY(), -local.getX());
            case SOUTH -> new BlockPos(-local.getX(), local.getY(), -local.getZ());
            case UP, DOWN -> throw new IllegalArgumentException("large silo hatch requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos siloHatchLargeCollisionLocalOffset(BlockPos worldRelative, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(worldRelative.getZ(), worldRelative.getY(), -worldRelative.getX());
            case SOUTH -> new BlockPos(-worldRelative.getZ(), worldRelative.getY(), worldRelative.getX());
            case EAST -> new BlockPos(-worldRelative.getX(), worldRelative.getY(), -worldRelative.getZ());
            case WEST -> worldRelative;
            case UP, DOWN -> throw new IllegalArgumentException("large silo hatch requires a horizontal facing: " + facing);
        };
    }

    public static VoxelShape orientSiloHatchLargeShape(VoxelShape localShape, Direction facing) {
        if (localShape.isEmpty()) {
            return Shapes.empty();
        }
        AABB box = localShape.bounds();
        return switch (facing) {
            case NORTH -> Shapes.box(1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ);
            case WEST -> Shapes.box(1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX);
            case EAST -> Shapes.box(box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX);
            case SOUTH -> Shapes.box(box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ);
            case UP, DOWN -> throw new IllegalArgumentException("large silo hatch requires a horizontal facing: " + facing);
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
        if (!level.isClientSide && allowsManualToggle()
                && level.getBlockEntity(pos) instanceof HbmHeavyDoorBlockEntity door) {
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
        if (!level.isClientSide && allowsManualToggle()
                && level.getBlockEntity(pos) instanceof HbmHeavyDoorBlockEntity door) {
            door.toggle();
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean allowsManualToggle() {
        return !HbmConfig.ULTRA_LARP_MODE.get() || this.decl == HbmDoorDecl.WATER_DOOR;
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
            VoxelShape localShape = this.decl.localShape(0, 0, 0, false, collision);
            Direction facing = state.getValue(FACING);
            return switch (this.decl) {
                case FIRE_DOOR -> orientFireDoorShape(localShape, facing);
                case TRANSITION_SEAL -> orientTransitionSealShape(localShape, facing);
                case SLIDING_BLAST_DOOR -> orientSlidingBlastDoorShape(localShape, facing);
                case SLIDING_BLAST_DOOR_2 -> orientSlidingBlastDoor2Shape(localShape, facing);
                case SLIDING_GATE_DOOR -> orientSlidingGateDoorShape(localShape, facing);
                case QE_SLIDING_DOOR -> orientQeSlidingDoorShape(localShape, facing);
                case QE_CONTAINMENT -> orientQeContainmentShape(localShape, facing);
                case SLIDING_SEAL_DOOR -> orientSlidingSealDoorShape(localShape, facing);
                case SECURE_ACCESS_DOOR -> orientSecureAccessDoorShape(localShape, facing);
                case ROUND_AIRLOCK_DOOR -> orientRoundAirlockDoorShape(localShape, facing);
                case LARGE_VEHICLE_DOOR -> orientLargeVehicleDoorShape(localShape, facing);
                case VAULT_DOOR -> orientVaultDoorShape(localShape, facing);
                case WATER_DOOR -> orientWaterDoorShape(localShape, facing);
                case CARGO_DOOR -> orientCargoDoorShape(localShape, facing);
                case SILO_HATCH -> orientSiloHatchShape(localShape, facing);
                case SILO_HATCH_LARGE -> orientSiloHatchLargeShape(localShape, facing);
            };
        }
        boolean open = door.isOpenForCollision();
        VoxelShape localShape = this.decl.localShape(0, 0, 0, open, collision);
        Direction facing = state.getValue(FACING);
        return switch (this.decl) {
            case FIRE_DOOR -> orientFireDoorShape(localShape, facing);
            case TRANSITION_SEAL -> orientTransitionSealShape(localShape, facing);
            case SLIDING_BLAST_DOOR -> orientSlidingBlastDoorShape(localShape, facing);
            case SLIDING_BLAST_DOOR_2 -> orientSlidingBlastDoor2Shape(localShape, facing);
            case SLIDING_GATE_DOOR -> orientSlidingGateDoorShape(localShape, facing);
            case QE_SLIDING_DOOR -> orientQeSlidingDoorShape(localShape, facing);
            case QE_CONTAINMENT -> orientQeContainmentShape(localShape, facing);
            case SLIDING_SEAL_DOOR -> orientSlidingSealDoorShape(localShape, facing);
            case SECURE_ACCESS_DOOR -> orientSecureAccessDoorShape(localShape, facing);
            case ROUND_AIRLOCK_DOOR -> orientRoundAirlockDoorShape(localShape, facing);
            case LARGE_VEHICLE_DOOR -> orientLargeVehicleDoorShape(localShape, facing);
            case VAULT_DOOR -> orientVaultDoorShape(localShape, facing);
            case WATER_DOOR -> orientWaterDoorShape(localShape, facing);
            case CARGO_DOOR -> orientCargoDoorShape(localShape, facing);
            case SILO_HATCH -> orientSiloHatchShape(localShape, facing);
            case SILO_HATCH_LARGE -> orientSiloHatchLargeShape(localShape, facing);
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
        return switch (this.decl) {
            case FIRE_DOOR -> rotateFireDoor(state, rotation);
            case TRANSITION_SEAL -> rotateTransitionSeal(state, rotation);
            case SLIDING_BLAST_DOOR -> rotateSlidingBlastDoor(state, rotation);
            case SLIDING_BLAST_DOOR_2 -> rotateSlidingBlastDoor2(state, rotation);
            case SLIDING_GATE_DOOR -> rotateSlidingGateDoor(state, rotation);
            case QE_SLIDING_DOOR -> rotateQeSlidingDoor(state, rotation);
            case QE_CONTAINMENT -> rotateQeContainment(state, rotation);
            case SLIDING_SEAL_DOOR -> rotateSlidingSealDoor(state, rotation);
            case SECURE_ACCESS_DOOR -> rotateSecureAccessDoor(state, rotation);
            case ROUND_AIRLOCK_DOOR -> rotateRoundAirlockDoor(state, rotation);
            case LARGE_VEHICLE_DOOR -> rotateLargeVehicleDoor(state, rotation);
            case VAULT_DOOR -> rotateVaultDoor(state, rotation);
            case WATER_DOOR -> rotateWaterDoor(state, rotation);
            case CARGO_DOOR -> rotateCargoDoor(state, rotation);
            case SILO_HATCH -> rotateSiloHatch(state, rotation);
            case SILO_HATCH_LARGE -> rotateSiloHatchLarge(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.decl) {
            case FIRE_DOOR -> mirrorFireDoor(state, mirror);
            case TRANSITION_SEAL -> mirrorTransitionSeal(state, mirror);
            case SLIDING_BLAST_DOOR -> mirrorSlidingBlastDoor(state, mirror);
            case SLIDING_BLAST_DOOR_2 -> mirrorSlidingBlastDoor2(state, mirror);
            case SLIDING_GATE_DOOR -> mirrorSlidingGateDoor(state, mirror);
            case QE_SLIDING_DOOR -> mirrorQeSlidingDoor(state, mirror);
            case QE_CONTAINMENT -> mirrorQeContainment(state, mirror);
            case SLIDING_SEAL_DOOR -> mirrorSlidingSealDoor(state, mirror);
            case SECURE_ACCESS_DOOR -> mirrorSecureAccessDoor(state, mirror);
            case ROUND_AIRLOCK_DOOR -> mirrorRoundAirlockDoor(state, mirror);
            case LARGE_VEHICLE_DOOR -> mirrorLargeVehicleDoor(state, mirror);
            case VAULT_DOOR -> mirrorVaultDoor(state, mirror);
            case WATER_DOOR -> mirrorWaterDoor(state, mirror);
            case CARGO_DOOR -> mirrorCargoDoor(state, mirror);
            case SILO_HATCH -> mirrorSiloHatch(state, mirror);
            case SILO_HATCH_LARGE -> mirrorSiloHatchLarge(state, mirror);
        };
    }

    private static BlockState rotateFireDoor(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateTransitionSeal(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSlidingBlastDoor(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSlidingBlastDoor2(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSlidingGateDoor(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateQeSlidingDoor(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateQeContainment(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSlidingSealDoor(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSecureAccessDoor(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateRoundAirlockDoor(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateLargeVehicleDoor(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateVaultDoor(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateWaterDoor(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateCargoDoor(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSiloHatch(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSiloHatchLarge(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorFireDoor(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorTransitionSeal(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSlidingBlastDoor(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSlidingBlastDoor2(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSlidingGateDoor(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorQeSlidingDoor(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorQeContainment(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSlidingSealDoor(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSecureAccessDoor(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorRoundAirlockDoor(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorLargeVehicleDoor(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorVaultDoor(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorWaterDoor(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorCargoDoor(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSiloHatch(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSiloHatchLarge(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
