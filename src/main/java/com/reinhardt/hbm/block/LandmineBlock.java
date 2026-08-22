package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.blockentity.LandmineBlockEntity;
import com.reinhardt.hbm.item.DefuserItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

public final class LandmineBlock extends BaseEntityBlock implements EntityBlock {
    private static final MapCodec<LandmineBlock> CODEC = MapCodec.unit(() ->
            new LandmineBlock(Properties.of(), LandmineType.AP));

    private final LandmineType type;

    public LandmineBlock(Properties properties, LandmineType type) {
        super(properties);
        this.type = type;
    }

    public LandmineType type() {
        return this.type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LandmineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        return level.isClientSide ? null
                : createTickerHelper(blockEntityType, HbmBlockEntities.LANDMINE.get(),
                (serverLevel, pos, blockState, landmine) -> {
                    if (serverLevel instanceof ServerLevel server) {
                        LandmineBlockEntity.tick(server, pos, blockState, landmine);
                    }
                });
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState support = level.getBlockState(pos.below());
        return support.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                || support.getBlock() instanceof FenceBlock;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide) {
            return;
        }
        if (level.hasNeighborSignal(pos) || !state.canSurvive(level, pos)) {
            detonate(level, pos);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            detonate(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
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
        if (!(stack.getItem() instanceof DefuserItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            level.removeBlock(pos, false);
            ItemEntity item = new ItemEntity(
                    serverLevel,
                    pos.getX() + serverLevel.random.nextFloat() * 0.6D + 0.2D,
                    pos.getY() + serverLevel.random.nextFloat() * 0.2D + 1.0D,
                    pos.getZ() + serverLevel.random.nextFloat() * 0.6D + 0.2D,
                    new ItemStack(this)
            );
            item.setDeltaMovement(
                    serverLevel.random.nextGaussian() * 0.05D,
                    serverLevel.random.nextGaussian() * 0.05D + 0.2D,
                    serverLevel.random.nextGaussian() * 0.05D
            );
            serverLevel.addFreshEntity(item);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public void detonate(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || !level.getBlockState(pos).is(this)) {
            return;
        }
        level.removeBlock(pos, false);
        this.type.detonate(serverLevel, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.type.shape();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.type.shape();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    public enum LandmineType {
        AP(1.5D, 1.0D, Shapes.box(5.0D / 16.0D, 0.0D, 5.0D / 16.0D, 11.0D / 16.0D, 1.0D / 16.0D, 11.0D / 16.0D)),
        HE(2.0D, 5.0D, Shapes.box(4.0D / 16.0D, 0.0D, 4.0D / 16.0D, 12.0D / 16.0D, 2.0D / 16.0D, 12.0D / 16.0D)),
        SHRAPNEL(1.5D, 1.0D, Shapes.box(5.0D / 16.0D, 0.0D, 5.0D / 16.0D, 11.0D / 16.0D, 1.0D / 16.0D, 11.0D / 16.0D)),
        NUCLEAR(2.5D, 1.0D, Shapes.box(5.0D / 16.0D, 0.0D, 4.0D / 16.0D, 11.0D / 16.0D, 6.0D / 16.0D, 12.0D / 16.0D)),
        NAVAL(2.5D, 1.0D, Shapes.block());

        private final double range;
        private final double height;
        private final VoxelShape shape;

        LandmineType(double range, double height, VoxelShape shape) {
            this.range = range;
            this.height = height;
            this.shape = shape;
        }

        public double range() {
            return this.range;
        }

        public double height() {
            return this.height;
        }

        public VoxelShape shape() {
            return this.shape;
        }

        private void detonate(ServerLevel level, BlockPos pos) {
            LandmineExplosions.detonate(level, pos, this);
        }
    }
}
