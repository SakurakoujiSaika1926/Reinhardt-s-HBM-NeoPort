package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.WandLootBlockEntity;
import com.reinhardt.hbm.item.KeyPinItem;
import com.reinhardt.hbm.item.LockItem;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.worldgen.structure.StructureWandBlockTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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

public class WandLootBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public WandLootBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof WandLootBlockEntity loot) {
            loot.placedRotation = placer.getYRot();
            loot.sync();
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WandLootBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof WandLootBlockEntity loot) {
                WandLootBlockEntity.tick(tickerLevel, pos, tickerState, loot);
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            if (ScrewdriverItem.isScrewdriver(stack) || ScrewdriverItem.isHandDrill(stack) || ScrewdriverItem.isDefuser(stack)) {
                return useTool(stack, level, pos, player);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (ScrewdriverItem.isScrewdriver(stack) || ScrewdriverItem.isHandDrill(stack) || ScrewdriverItem.isDefuser(stack)) {
            return useTool(stack, level, pos, player);
        }
        if (level.getBlockEntity(pos) instanceof WandLootBlockEntity loot && stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (isLootableReplacement(level, pos, block)) {
                if (!level.isClientSide) {
                    loot.setReplacement(block, StructureWandBlockTarget.legacyMeta(block, stack));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (level.getBlockEntity(pos) instanceof WandLootBlockEntity loot && stack.getItem() instanceof LockItem lock) {
            if (!level.isClientSide) {
                loot.setLock(KeyPinItem.pins(stack), (float) lock.lockMod());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static ItemInteractionResult useTool(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof WandLootBlockEntity loot)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (ScrewdriverItem.isScrewdriver(stack)) {
                loot.adjustMin(player.isShiftKeyDown());
            } else if (ScrewdriverItem.isHandDrill(stack)) {
                loot.adjustMax(player.isShiftKeyDown());
            } else if (ScrewdriverItem.isDefuser(stack)) {
                loot.cyclePool(player.isShiftKeyDown());
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean isLootableReplacement(Level level, BlockPos pos, Block block) {
        if (block == HbmBlocks.DECO_LOOT.get()) {
            return true;
        }
        if (!(block instanceof EntityBlock entityBlock)) {
            return false;
        }
        BlockEntity blockEntity = entityBlock.newBlockEntity(pos, block.defaultBlockState());
        return blockEntity instanceof Container;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
