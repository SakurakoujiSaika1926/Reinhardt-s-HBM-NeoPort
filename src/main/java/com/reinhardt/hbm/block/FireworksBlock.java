package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FireworksBlockEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Exact interaction contract of the 1.7.10 fireworks battery. */
public final class FireworksBlock extends Block implements EntityBlock {
    public FireworksBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FireworksBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.FIREWORKS.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, entity) ->
                FireworksBlockEntity.tick(tickerLevel, tickerPos, tickerState, (FireworksBlockEntity) entity);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof FireworksBlockEntity fireworks)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            if (stack.is(Items.GUNPOWDER)) {
                fireworks.addCharges(stack.getCount() * 3);
                stack.shrink(stack.getCount());
            } else if (stack.is(HbmItems.SULFUR.get())) {
                fireworks.addCharges(stack.getCount());
                stack.shrink(stack.getCount());
            } else {
                DyeColor dye = DyeColor.getColor(stack);
                if (dye != null) {
                    fireworks.setColor(dye.getFireworkColor());
                    stack.shrink(1);
                } else if (stack.is(Items.NAME_TAG)) {
                    fireworks.setMessage(stack.getHoverName().getString());
                    stack.shrink(1);
                } else {
                    showStatus(player, fireworks);
                }
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FireworksBlockEntity fireworks) {
            showStatus(player, fireworks);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void showStatus(Player player, FireworksBlockEntity fireworks) {
        player.sendSystemMessage(Component.translatable("block.reinhardtshbm.fireworks")
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable(
                "message.reinhardtshbm.fireworks.charges", fireworks.charges()).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable(
                "message.reinhardtshbm.fireworks.color", Integer.toHexString(fireworks.color()))
                .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable(
                "message.reinhardtshbm.fireworks.message", fireworks.message()).withStyle(ChatFormatting.YELLOW));
    }
}
