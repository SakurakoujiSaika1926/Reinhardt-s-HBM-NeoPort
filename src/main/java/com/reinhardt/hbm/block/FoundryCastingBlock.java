package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FoundryCastingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class FoundryCastingBlock extends Block implements EntityBlock {
    private static final VoxelShape MOLD_SHAPE = Shapes.or(
            Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D),
            Shapes.box(0.0D, 0.125D, 0.0D, 1.0D, 0.5D, 0.125D),
            Shapes.box(0.0D, 0.125D, 0.875D, 1.0D, 0.5D, 1.0D),
            Shapes.box(0.0D, 0.125D, 0.125D, 0.125D, 0.5D, 0.875D),
            Shapes.box(0.875D, 0.125D, 0.125D, 1.0D, 0.5D, 0.875D)
    );
    private static final VoxelShape BASIN_SHAPE = Shapes.or(
            Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D),
            Shapes.box(0.0D, 0.125D, 0.0D, 1.0D, 1.0D, 0.125D),
            Shapes.box(0.0D, 0.125D, 0.875D, 1.0D, 1.0D, 1.0D),
            Shapes.box(0.0D, 0.125D, 0.125D, 0.125D, 1.0D, 0.875D),
            Shapes.box(0.875D, 0.125D, 0.125D, 1.0D, 1.0D, 0.875D)
    );

    private final Kind kind;

    public FoundryCastingBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundryCastingBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : (blockEntityType == com.reinhardt.hbm.registry.HbmBlockEntities.FOUNDRY_CASTING.get()
                ? (lvl, pos, st, be) -> FoundryCastingBlockEntity.tick(lvl, pos, st, (FoundryCastingBlockEntity) be)
                : null);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity casting && casting.hasOutput()) {
            if (!level.isClientSide && casting.extractOutput(player)) {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity casting)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(ItemTags.SHOVELS)) {
            if (!level.isClientSide && casting.scrape(player)) {
                level.playSound(null, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (casting.canInstallMold(stack)) {
            if (!level.isClientSide && casting.installMold(stack, player)) {
                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.9F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Keep the legacy breakBlock contract: the installed mold, finished
        // casting and any remaining metal are recovered before vanilla removes
        // the block entity. onRemove remains as the fallback for explosions and
        // non-player block replacement; dropContents clears its own state, so
        // the two paths cannot duplicate drops.
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity casting) {
            casting.dropContents(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity casting) {
            casting.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity casting && casting.isFull() && random.nextInt(3) == 0) {
            level.addParticle(
                    ParticleTypes.SMOKE,
                    pos.getX() + 0.25D + random.nextDouble() * 0.5D,
                    pos.getY() + (this.kind == Kind.BASIN ? 1.0D : 0.5D),
                    pos.getZ() + 0.25D + random.nextDouble() * 0.5D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.kind == Kind.BASIN ? BASIN_SHAPE : MOLD_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.kind == Kind.BASIN ? BASIN_SHAPE : MOLD_SHAPE;
    }

    public enum Kind {
        MOLD(0),
        BASIN(1);

        private final int moldSize;

        Kind(int moldSize) {
            this.moldSize = moldSize;
        }

        public int moldSize() {
            return this.moldSize;
        }
    }
}
