package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.PlushieBlockEntity;
import com.reinhardt.hbm.item.PlushieBlockItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PlushieBlock extends Block implements EntityBlock {
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

    public PlushieBlock(Properties properties) {
        super(properties.sound(SoundType.WOOL));
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int rotation = Mth.floor((context.getRotation() + 180.0F) * 16.0F / 360.0F + 0.5F) & 15;
        return defaultBlockState().setValue(ROTATION, rotation);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PlushieBlockEntity plushie) {
            plushie.setType(PlushieBlockItem.type(stack));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlushieBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide && type == HbmBlockEntities.PLUSHIE.get()
                ? (tickerLevel, tickerPos, tickerState, entity) -> {
                    if (entity instanceof PlushieBlockEntity plushie) {
                        PlushieBlockEntity.clientTick(tickerLevel, tickerPos, tickerState, plushie);
                    }
                }
                : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        activate(level, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        activate(level, pos);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void activate(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PlushieBlockEntity plushie) {
            if (level.isClientSide) {
                plushie.squish();
            } else {
                level.playSound(null, pos, plushie.type() == PlushieType.HUNDUN
                                ? HbmSoundEvents.HUNDUN_HOWL.get() : HbmSoundEvents.PLUSHIE_SQUEAK.get(),
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        plushie.type() == PlushieType.HUNDUN ? 100.0F : 0.25F, 1.0F);
            }
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof PlushieBlockEntity plushie
                && asItem() instanceof PlushieBlockItem item) {
            return PlushieBlockItem.stackFor(item, plushie.type());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof PlushieBlockEntity plushie
                && asItem() instanceof PlushieBlockItem item) {
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                    PlushieBlockItem.stackFor(item, plushie.type()));
            drop.setDeltaMovement(0.0D, 0.0D, 0.0D);
            level.addFreshEntity(drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, (state.getValue(ROTATION) + rotation.ordinal() * 4) & 15);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }
}
