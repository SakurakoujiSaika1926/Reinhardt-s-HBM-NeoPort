package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.PaintableCableBlockEntity;
import com.reinhardt.hbm.item.ScrewdriverItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Direct modern port of BlockCablePaintable's paint, screwdriver and defuser behavior. */
public final class PaintableEnergyCableBlock extends EnergyCableBlock implements EntityBlock {
    public PaintableEnergyCableBlock(Properties properties) {
        super(properties, 8.0D);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PaintableCableBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof PaintableCableBlockEntity cable)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (ScrewdriverItem.isScrewdriver(stack)) {
            if (!level.isClientSide && cable.paintBlock() != null) {
                cable.setPaintBlock(null);
                ScrewdriverItem.damageTool(stack, level, player, hand);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (ScrewdriverItem.isDefuser(stack)) {
            if (!level.isClientSide) {
                cable.togglePortVisible();
                ScrewdriverItem.damageTool(stack, level, player, hand);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.getItem() instanceof BlockItem blockItem && cable.paintBlock() == null
                && isAllowedPaint(level, pos, blockItem.getBlock())) {
            if (!level.isClientSide) {
                cable.setPaintBlock(blockItem.getBlock());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (int line = 1; line <= 4; line++) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.red_cable_paintable." + line)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private boolean isAllowedPaint(Level level, BlockPos pos, Block block) {
        if (block == this || block == Blocks.GRASS_BLOCK || block.defaultBlockState().getRenderShape() != RenderShape.MODEL) {
            return false;
        }
        return block.defaultBlockState().isSolidRender(level, pos);
    }
}
