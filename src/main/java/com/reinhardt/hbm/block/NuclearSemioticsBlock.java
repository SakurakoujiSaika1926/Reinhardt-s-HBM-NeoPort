package com.reinhardt.hbm.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The ten-line nuclear semiotics warning on the marked concrete brick. */
public final class NuclearSemioticsBlock extends Block {
    private static final String[] WARNING = {
            "You should not have come here.",
            "This is not a place of honor. No great deed is commemorated here.",
            "Nothing of value is here.",
            "What is here is dangerous and repulsive.",
            "We considered ourselves a powerful culture. We harnessed the hidden fire, and used it for our own purposes.",
            "Then we saw the fire could burn within living things, unnoticed until it destroyed them.",
            "And we were afraid.",
            "We built great tombs to hold the fire for one hundred thousand years, after which it would no longer kill.",
            "If this place is opened, the fire will not be isolated from the world, and we will have failed to protect you.",
            "Leave this place and never come back."
    };

    public NuclearSemioticsBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return player.isShiftKeyDown()
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : showWarning(level, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        showWarning(level, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static ItemInteractionResult showWarning(Level level, Player player) {
        if (!level.isClientSide) {
            for (String line : WARNING) {
                player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.RED));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
