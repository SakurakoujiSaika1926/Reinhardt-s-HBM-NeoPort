package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.WallChargeBlockEntity;
import com.reinhardt.hbm.blockentity.WallChargeExplosions;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Exact six-sided wall charge behavior from 1.7.10 BlockChargeBase. */
public final class WallChargeBlock extends BaseEntityBlock implements EntityBlock {
    private static final MapCodec<WallChargeBlock> CODEC = MapCodec.unit(() ->
            new WallChargeBlock(BlockBehaviour.Properties.of(), Kind.DYNAMITE));
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape UP = Block.box(0, 0, 0, 16, 6, 16);
    private static final VoxelShape DOWN = Block.box(0, 10, 0, 16, 16, 16);
    private static final VoxelShape NORTH = Block.box(0, 0, 10, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 6);
    private static final VoxelShape WEST = Block.box(10, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST = Block.box(0, 0, 0, 6, 16, 16);

    private final Kind kind;

    public WallChargeBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos supportPos = pos.relative(state.getValue(FACING).getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, state.getValue(FACING));
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && !state.canSurvive(level, pos)) {
            WallChargeExplosions.detonate((ServerLevel) level, pos, kind);
        }
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos,
                                net.minecraft.world.level.Explosion explosion) {
        if (!level.isClientSide && level instanceof ServerLevel server) {
            WallChargeExplosions.detonate(server, pos, kind);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            case UP -> UP;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WallChargeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == HbmBlockEntities.WALL_CHARGE.get()
                ? (tickerLevel, pos, tickerState, blockEntity) ->
                WallChargeBlockEntity.tick(tickerLevel, pos, tickerState, (WallChargeBlockEntity) blockEntity)
                : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (ScrewdriverItem.isDefuser(stack)) {
            if (!level.isClientSide) {
                WallChargeBlockEntity charge = charge(level, pos);
                if (charge != null && charge.started()) {
                    charge.disarm();
                } else {
                    level.removeBlock(pos, false);
                    popResource(level, pos, new ItemStack(this));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hitResult) {
        WallChargeBlockEntity charge = charge(level, pos);
        if (charge == null || charge.started()) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                if (charge.timer() > 0) {
                    charge.arm(charge.timer());
                    level.playSound(null, pos, HbmSoundEvents.CHARGE_START.get(),
                            net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            } else {
                charge.cycleTimer();
                level.playSound(null, pos, HbmSoundEvents.CHARGE_BEEP.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        return List.of();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            WallChargeExplosions.detonate((ServerLevel) level, pos, kind);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private WallChargeBlockEntity charge(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof WallChargeBlockEntity charge ? charge : null;
    }

    public enum Kind {
        DYNAMITE("charge_dynamite", 4.0F, true, false),
        MINER("charge_miner", 4.0F, true, true),
        C4("charge_c4", 15.0F, false, false),
        SEMTEX("charge_semtex", 10.0F, true, false);

        private final String id;
        private final float radius;
        private final boolean drops;
        private final boolean noHurt;

        Kind(String id, float radius, boolean drops, boolean noHurt) {
            this.id = id;
            this.radius = radius;
            this.drops = drops;
            this.noHurt = noHurt;
        }

        public String id() { return id; }
        public float radius() { return radius; }
        public boolean drops() { return drops; }
        public boolean noHurt() { return noHurt; }
    }
}
