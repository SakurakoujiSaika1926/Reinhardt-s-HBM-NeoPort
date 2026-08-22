package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.WandJigsawBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.worldgen.structure.StructureWandBlockTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class WandJigsawBlock extends AbstractFacingWandBlock {
    public WandJigsawBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WandJigsawBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || isStructureWand(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof WandJigsawBlockEntity jigsaw && stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock() == HbmBlocks.WAND_AIR.get()
                    ? net.minecraft.world.level.block.Blocks.AIR
                    : blockItem.getBlock();
            if (canSetReplacement(stack, block)) {
                if (!level.isClientSide) {
                    jigsaw.setReplacement(block, StructureWandBlockTarget.legacyMeta(block, stack));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            openClientScreen(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void openClientScreen(BlockPos pos) {
        try {
            Class<?> hooks = Class.forName("com.reinhardt.hbm.client.WandClientHooks");
            hooks.getMethod("openJigsaw", BlockPos.class).invoke(null, pos);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
