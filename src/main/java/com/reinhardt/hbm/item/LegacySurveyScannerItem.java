package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.BedrockOreBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Spatial 1.7.10 survey scan for legacy oil, depth-rock and bedrock resources. */
public final class LegacySurveyScannerItem extends Item {
    public LegacySurveyScannerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            scan(level, player);
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void scan(Level level, Player player) {
        BlockPos base = player.blockPosition();
        boolean oil = false;
        boolean coltan = false;
        boolean bedrockOil = false;
        boolean depth = false;
        boolean schist = false;
        BedrockOreBlockEntity bedrockOre = null;

        int topY = Math.min(level.getMaxBuildHeight() - 1, base.getY() + 15);
        int bottomY = level.getMinBuildHeight();
        for (int offsetX = -5; offsetX <= 5; offsetX++) {
            for (int offsetZ = -5; offsetZ <= 5; offsetZ++) {
                int x = base.getX() + offsetX * 5;
                int z = base.getZ() + offsetZ * 5;
                for (int y = topY; y > bottomY; y -= 2) {
                    Block block = level.getBlockState(new BlockPos(x, y, z)).getBlock();
                    oil |= block == HbmBlocks.ORE_OIL.get();
                    coltan |= block == HbmBlocks.ORE_COLTAN.get();
                    bedrockOil |= block == HbmBlocks.ORE_BEDROCK_OIL.get();
                    depth |= block == HbmBlocks.STONE_DEPTH.get() || block == HbmBlocks.STONE_DEPTH_NETHER.get();
                    schist |= block == HbmBlocks.STONE_GNEISS.get();
                }

                BlockPos orePos = new BlockPos(base.getX() + offsetX * 2, bottomY, base.getZ() + offsetZ * 2);
                if (level.getBlockEntity(orePos) instanceof BedrockOreBlockEntity ore) {
                    bedrockOre = ore;
                }
            }
        }

        if (oil) inform(player, "message.reinhardtshbm.survey_scanner.oil", ChatFormatting.BLACK);
        if (bedrockOil) inform(player, "message.reinhardtshbm.survey_scanner.bedrock_oil", ChatFormatting.BLACK);
        if (coltan) inform(player, "message.reinhardtshbm.survey_scanner.coltan", ChatFormatting.GOLD);
        if (depth) inform(player, "message.reinhardtshbm.survey_scanner.depth", ChatFormatting.GRAY);
        if (schist) inform(player, "message.reinhardtshbm.survey_scanner.schist", ChatFormatting.DARK_AQUA);
        if (bedrockOre != null) {
            player.displayClientMessage(Component.translatable("message.reinhardtshbm.survey_scanner.bedrock_ore", bedrockOre.resource().getHoverName())
                    .withStyle(ChatFormatting.RED), false);
        }
    }

    private static void inform(Player player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), false);
    }
}
