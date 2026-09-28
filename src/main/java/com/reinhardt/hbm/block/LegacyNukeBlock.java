package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.blockentity.LegacyNukeBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public final class LegacyNukeBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    // The definition is selected by the registry entry and is not serialized
    // as block properties, so the codec must use a stable representative.
    private static final MapCodec<LegacyNukeBlock> CODEC = MapCodec.unit(() ->
            new LegacyNukeBlock(Properties.of(), LegacyNukeDefinition.CUSTOM));
    private static final VoxelShape SHAPE = Shapes.block();
    private final LegacyNukeDefinition definition;

    public LegacyNukeBlock(Properties properties, LegacyNukeDefinition definition) {
        super(properties);
        this.definition = definition;
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    public LegacyNukeDefinition definition() { return definition; }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return switch (this.definition) {
            case GADGET -> gadgetPlacement(context);
            case MAN -> manPlacement(context);
            case MIKE -> mikePlacement(context);
            case TSAR -> tsarPlacement(context);
            case FLEIJA -> fleijaPlacement(context);
            case PROTOTYPE -> prototypePlacement(context);
            case SOLINIUM -> soliniumPlacement(context);
            case N2 -> n2Placement(context);
            case CUSTOM -> customPlacement(context);
            case BALEFIRE -> balefirePlacement(context);
        };
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (this.definition) {
            case GADGET -> rotateGadget(state, rotation);
            case MAN -> rotateMan(state, rotation);
            case MIKE -> rotateMike(state, rotation);
            case TSAR -> rotateTsar(state, rotation);
            case FLEIJA -> rotateFleija(state, rotation);
            case PROTOTYPE -> rotatePrototype(state, rotation);
            case SOLINIUM -> rotateSolinium(state, rotation);
            case N2 -> rotateN2(state, rotation);
            case CUSTOM -> rotateCustom(state, rotation);
            case BALEFIRE -> rotateBalefire(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return switch (this.definition) {
            case GADGET -> mirrorGadget(state, mirror);
            case MAN -> mirrorMan(state, mirror);
            case MIKE -> mirrorMike(state, mirror);
            case TSAR -> mirrorTsar(state, mirror);
            case FLEIJA -> mirrorFleija(state, mirror);
            case PROTOTYPE -> mirrorPrototype(state, mirror);
            case SOLINIUM -> mirrorSolinium(state, mirror);
            case N2 -> mirrorN2(state, mirror);
            case CUSTOM -> mirrorCustom(state, mirror);
            case BALEFIRE -> mirrorBalefire(state, mirror);
        };
    }

    private BlockState gadgetPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState manPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState mikePlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState tsarPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState fleijaPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState prototypePlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState soliniumPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState n2Placement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState customPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private BlockState balefirePlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private static BlockState rotateGadget(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateMan(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateMike(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateTsar(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateFleija(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotatePrototype(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateSolinium(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateN2(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateCustom(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateBalefire(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorGadget(BlockState state, Mirror mirror) {
        return rotateGadget(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorMan(BlockState state, Mirror mirror) {
        return rotateMan(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorMike(BlockState state, Mirror mirror) {
        return rotateMike(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorTsar(BlockState state, Mirror mirror) {
        return rotateTsar(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorFleija(BlockState state, Mirror mirror) {
        return rotateFleija(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorPrototype(BlockState state, Mirror mirror) {
        return rotatePrototype(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorSolinium(BlockState state, Mirror mirror) {
        return rotateSolinium(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorN2(BlockState state, Mirror mirror) {
        return rotateN2(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorCustom(BlockState state, Mirror mirror) {
        return rotateCustom(state, mirror.getRotation(state.getValue(FACING)));
    }

    private static BlockState mirrorBalefire(BlockState state, Mirror mirror) {
        return rotateBalefire(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LegacyNukeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, buffer -> {
                buffer.writeBlockPos(pos);
                buffer.writeVarInt(definition.ordinal());
            });
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos fromPos, boolean moving) {
        if (!level.isClientSide && level.hasNeighborSignal(pos)
                && level.getBlockEntity(pos) instanceof LegacyNukeBlockEntity nuke && nuke.isReady()) {
            nuke.detonate();
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
}
