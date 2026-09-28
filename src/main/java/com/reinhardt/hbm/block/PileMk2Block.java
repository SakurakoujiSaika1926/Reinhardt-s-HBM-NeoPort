package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.PileMk2CoreBlockEntity;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** The formed, no-drop block family used by the current-upstream Chicago Pile MK2. */
public final class PileMk2Block extends Block implements EntityBlock {
    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);

    public PileMk2Block(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KIND, Kind.DUMMY));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(KIND) == Kind.CORE ? new PileMk2CoreBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(KIND) != Kind.CORE) {
            return null;
        }
        if (type != HbmBlockEntities.PILE_MK2_CORE.get()) return null;
        return (tickerLevel, tickerPos, tickerState, blockEntity) ->
                PileMk2CoreBlockEntity.tick(tickerLevel, tickerPos, tickerState,
                        (PileMk2CoreBlockEntity) blockEntity);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!ScrewdriverItem.isHandDrill(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (state.getValue(KIND) == Kind.CORE) {
            if (!level.isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.reinhardtshbm.pile_mk2.intersect_core"), true);
            }
            return ItemInteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        PileMk2CoreBlockEntity core = findCore(level, pos);
        if (core == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.reinhardtshbm.pile_mk2.no_core"), true);
            return ItemInteractionResult.FAIL;
        }
        boolean changed = core.drillChannel(pos, hit.getDirection().getOpposite(), player);
        if (changed) {
            ScrewdriverItem.damageTool(stack, level, player, hand);
        }
        return changed ? ItemInteractionResult.CONSUME : ItemInteractionResult.FAIL;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !movedByPiston && !level.isClientSide
                && !PileMk2CoreBlockEntity.isRestoringStructure()) {
            PileMk2CoreBlockEntity core = level.getBlockEntity(pos) instanceof PileMk2CoreBlockEntity direct
                    ? direct : findCore(level, pos);
            if (core != null) {
                core.dismantle(true);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Nullable
    public static PileMk2CoreBlockEntity findCore(Level level, BlockPos partPos) {
        if (level.getBlockEntity(partPos) instanceof PileMk2CoreBlockEntity direct && direct.contains(partPos)) {
            return direct;
        }
        int range = PileMk2BrickBlock.MAX_HORIZONTAL_SIZE - 1;
        int verticalRange = PileMk2BrickBlock.MAX_VERTICAL_SIZE - 1;
        for (int y = -verticalRange; y <= verticalRange; y++) {
            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos candidatePos = partPos.offset(x, y, z);
                    if (!level.hasChunkAt(candidatePos)) continue;
                    BlockState candidateState = level.getBlockState(candidatePos);
                    if (!(candidateState.getBlock() instanceof PileMk2Block)
                            || candidateState.getValue(KIND) != Kind.CORE) continue;
                    if (level.getBlockEntity(candidatePos) instanceof PileMk2CoreBlockEntity core
                            && core.contains(partPos)) {
                        return core;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND);
    }

    public enum Kind implements StringRepresentable {
        DUMMY("dummy"),
        CORE("core"),
        CHANNEL("channel"),
        FUEL_IN("fuel_in"),
        FUEL_OUT("fuel_out"),
        AIR_IN("air_in"),
        AIR_OUT("air_out"),
        CONTROL("control"),
        EDGE("edge");

        private final String serializedName;

        Kind(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
