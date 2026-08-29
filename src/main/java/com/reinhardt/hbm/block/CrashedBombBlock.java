package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.blockentity.CrashedBombBlockEntity;
import com.reinhardt.hbm.explosion.CrashedBombExplosions;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** 1.7.10 crashed bomb: a four-variant, unbreakable world-generation dud. */
public final class CrashedBombBlock extends Block implements EntityBlock {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, Type.values().length - 1);
    private static final MapCodec<CrashedBombBlock> CODEC = simpleCodec(CrashedBombBlock::new);

    public CrashedBombBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VARIANT, Type.BALEFIRE.ordinal()));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrashedBombBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (level.isClientSide || type != HbmBlockEntities.CRASHED_BOMB.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) -> {
            if (blockEntity instanceof CrashedBombBlockEntity bomb) {
                CrashedBombBlockEntity.tick(tickerLevel, tickerPos, tickerState, bomb);
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(HbmItems.DEFUSER.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            CrashedBombExplosions.dropDefusedContents(serverLevel, pos, type(state));
            serverLevel.removeBlock(pos, false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    public static Type type(BlockState state) {
        return Type.byOrdinal(state.hasProperty(VARIANT) ? state.getValue(VARIANT) : 0);
    }

    /** Called by old bomb callers and structure triggers; normal player interaction only defuses. */
    public static void detonate(ServerLevel level, BlockPos pos, BlockState state) {
        level.removeBlock(pos, false);
        CrashedBombExplosions.detonate(level, pos, type(state));
    }

    public enum Type {
        BALEFIRE,
        CONVENTIONAL,
        NUKE,
        SALTED;

        public static Type byOrdinal(int ordinal) {
            Type[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : BALEFIRE;
        }
    }
}
