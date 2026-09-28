package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.PneumaticStorageBlockEntity;
import com.reinhardt.hbm.pneumatic.PneumaticNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.player.Player;
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
import org.jetbrains.annotations.Nullable;

/** Directional pneumatic endpoint/storage housing. */
public class PneumaticStorageBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public enum Kind {
        ACCESS, CLUTTER, MONO, IMPORTER, EXPORTER
    }

    private final Kind kind;

    public PneumaticStorageBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return switch (this.kind) {
            case ACCESS -> accessStateForPlacement(context);
            case CLUTTER -> clutterStateForPlacement(context);
            case MONO -> monoStateForPlacement(context);
            case IMPORTER -> importerStateForPlacement(context);
            case EXPORTER -> exporterStateForPlacement(context);
        };
    }

    private BlockState accessStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState clutterStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState monoStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState importerStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState exporterStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (this.kind) {
            case ACCESS -> rotateAccess(state, rotation);
            case CLUTTER -> rotateClutter(state, rotation);
            case MONO -> rotateMono(state, rotation);
            case IMPORTER -> rotateImporter(state, rotation);
            case EXPORTER -> rotateExporter(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.kind) {
            case ACCESS -> mirrorAccess(state, mirror);
            case CLUTTER -> mirrorClutter(state, mirror);
            case MONO -> mirrorMono(state, mirror);
            case IMPORTER -> mirrorImporter(state, mirror);
            case EXPORTER -> mirrorExporter(state, mirror);
        };
    }

    private static BlockState rotateAccess(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateClutter(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateMono(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateImporter(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateExporter(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorAccess(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorClutter(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorMono(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorImporter(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorExporter(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PneumaticStorageBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.PNEUMATIC_STORAGE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) ->
                PneumaticStorageBlockEntity.tick(tickLevel, pos, tickState, (PneumaticStorageBlockEntity) entity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PneumaticStorageBlockEntity storage
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(storage, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeVarInt(kind.ordinal());
            });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PneumaticNetworkManager.invalidate(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof PneumaticStorageBlockEntity storage) {
                storage.dropRealContents(level, pos);
            }
            PneumaticNetworkManager.invalidate(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
