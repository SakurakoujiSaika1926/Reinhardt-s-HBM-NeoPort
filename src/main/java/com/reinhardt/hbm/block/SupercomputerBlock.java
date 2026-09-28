package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.SupercomputerBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Exact multi-volume footprint and five combo ports of 1.7.10 MachineSuperComputer. */
public final class SupercomputerBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = exactLegacyFootprint();
    private static final List<AuthoredPort> AUTHORED_PORTS = List.of(
            new AuthoredPort(new BlockPos(0, 0, 8), Direction.SOUTH),
            new AuthoredPort(new BlockPos(1, 0, 7), Direction.EAST),
            new AuthoredPort(new BlockPos(-1, 0, 7), Direction.WEST),
            new AuthoredPort(new BlockPos(1, 0, 5), Direction.EAST),
            new AuthoredPort(new BlockPos(-1, 0, 5), Direction.WEST)
    );

    public SupercomputerBlock(Properties properties) {
        super(properties, FOOTPRINT, Shapes.block(), SupercomputerBlock::mapAuthoredOffset);
    }

    /** The OBJ and legacy multiblock dimensions are authored facing south. */
    public static BlockPos mapAuthoredOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            case UP, DOWN -> throw new IllegalArgumentException(
                    "Supercomputer requires a horizontal facing: " + facing);
        };
    }

    public static List<BlockPos> portPositions(BlockPos corePos, Direction facing) {
        return capabilityPorts(corePos, facing).stream()
                .map(CapabilityPort::host)
                .toList();
    }

    /**
     * Literal conversion of TileEntityMachineSuperComputer#getPorts.  The
     * host is the old TileEntityProxyCombo block; the connector is the
     * adjacent block in the PortDef face direction.
     */
    public static List<CapabilityPort> capabilityPorts(BlockPos corePos, Direction facing) {
        return AUTHORED_PORTS.stream().map(port -> {
            BlockPos host = corePos.offset(mapAuthoredOffset(facing, port.offset())).immutable();
            Direction face = mapAuthoredPortFace(facing, port.face());
            return new CapabilityPort(host, host.relative(face).immutable(), face);
        }).toList();
    }

    /** Dedicated supercomputer port rotation; do not replace with a shared machine rotation helper. */
    private static Direction mapAuthoredPortFace(Direction facing, Direction authoredFace) {
        return switch (facing) {
            case SOUTH -> authoredFace;
            case NORTH -> switch (authoredFace) {
                case SOUTH -> Direction.NORTH;
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                default -> throw invalidAuthoredPortFace(authoredFace);
            };
            case EAST -> switch (authoredFace) {
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                case WEST -> Direction.SOUTH;
                default -> throw invalidAuthoredPortFace(authoredFace);
            };
            case WEST -> switch (authoredFace) {
                case SOUTH -> Direction.WEST;
                case EAST -> Direction.SOUTH;
                case WEST -> Direction.NORTH;
                default -> throw invalidAuthoredPortFace(authoredFace);
            };
            case UP, DOWN -> throw new IllegalArgumentException(
                    "Supercomputer requires a horizontal facing: " + facing);
        };
    }

    private static IllegalArgumentException invalidAuthoredPortFace(Direction face) {
        return new IllegalArgumentException("Unexpected authored supercomputer port face: " + face);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction facing = state.getValue(FACING);
        Direction rotated = switch (rotation) {
            case NONE -> facing;
            case CLOCKWISE_90 -> switch (facing) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                case UP, DOWN -> throw new IllegalStateException("Supercomputer facing became vertical: " + facing);
            };
            case CLOCKWISE_180 -> facing.getOpposite();
            case COUNTERCLOCKWISE_90 -> switch (facing) {
                case NORTH -> Direction.WEST;
                case WEST -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                case UP, DOWN -> throw new IllegalStateException("Supercomputer facing became vertical: " + facing);
            };
        };
        return state.setValue(FACING, rotated);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);
        Direction mirrored = switch (mirror) {
            case NONE -> facing;
            case LEFT_RIGHT -> switch (facing) {
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                case EAST, WEST -> facing;
                case UP, DOWN -> throw new IllegalStateException("Supercomputer facing became vertical: " + facing);
            };
            case FRONT_BACK -> switch (facing) {
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                case NORTH, SOUTH -> facing;
                case UP, DOWN -> throw new IllegalStateException("Supercomputer facing became vertical: " + facing);
            };
        };
        return state.setValue(FACING, mirrored);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupercomputerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.SUPERCOMPUTER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> SupercomputerBlockEntity.tick(
                tickerLevel, pos, tickerState, (SupercomputerBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider provider
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
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
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
                inventory.dropContents(level, pos);
            }
            PowerNetworkManager.markDirty(level);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static float legacyRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case EAST -> 90.0F;
            case NORTH -> 180.0F;
            case WEST -> 270.0F;
            case UP, DOWN -> throw new IllegalStateException("Supercomputer facing is vertical: " + facing);
        };
    }

    private static Footprint exactLegacyFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        addBox(offsets, -3, 3, 0, 5, -3, 3);
        // MultiblockHandlerXR interprets a negative DOWN value as
        // minY = centerY - down.  Therefore {6,-6,...} and {7,-7,...}
        // are one-block-thick roof planes, not ranges extending below the core.
        addBox(offsets, -1, 1, 6, 6, -3, 3);
        addBox(offsets, -3, 3, 6, 6, -1, 1);
        addBox(offsets, -1, 1, 7, 7, -1, 1);
        // {-3,8} in the NORTH/SOUTH fields resolves to Z+3 through Z+8.
        addBox(offsets, -1, 1, 0, 2, 3, 8);
        return new Footprint(List.copyOf(new ArrayList<>(offsets)));
    }

    private static void addBox(LinkedHashSet<BlockPos> offsets,
                               int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
    }

    private record AuthoredPort(BlockPos offset, Direction face) {
    }

    public record CapabilityPort(BlockPos host, BlockPos connector, Direction face) {
    }
}
