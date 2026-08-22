package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class OilDetectorItem extends Item {
    public OilDetectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        BlockPos base = player.blockPosition();
        boolean direct = scanColumn(level, base.getX(), base.getY(), base.getZ());
        boolean nearby = direct
                || scanColumn(level, base.getX() + 5, base.getY(), base.getZ())
                || scanColumn(level, base.getX() - 5, base.getY(), base.getZ())
                || scanColumn(level, base.getX(), base.getY(), base.getZ() + 5)
                || scanColumn(level, base.getX(), base.getY(), base.getZ() - 5)
                || scanColumn(level, base.getX() + 10, base.getY(), base.getZ(), 10)
                || scanColumn(level, base.getX() - 10, base.getY(), base.getZ(), 10)
                || scanColumn(level, base.getX(), base.getY(), base.getZ() + 10, 10)
                || scanColumn(level, base.getX(), base.getY(), base.getZ() - 10, 10)
                || scanColumn(level, base.getX() + 5, base.getY(), base.getZ() + 5)
                || scanColumn(level, base.getX() - 5, base.getY(), base.getZ() + 5)
                || scanColumn(level, base.getX() + 5, base.getY(), base.getZ() - 5)
                || scanColumn(level, base.getX() - 5, base.getY(), base.getZ() - 5);

        if (!level.isClientSide) {
            Component message = direct
                    ? Component.translatable("item.reinhardtshbm.oil_detector.bullseye").withStyle(ChatFormatting.DARK_GREEN)
                    : nearby
                    ? Component.translatable("item.reinhardtshbm.oil_detector.detected").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("item.reinhardtshbm.oil_detector.noOil").withStyle(ChatFormatting.RED);
            player.displayClientMessage(message, false);
        }

        player.swing(usedHand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.reinhardtshbm.oil_detector.desc1"));
        tooltipComponents.add(Component.translatable("item.reinhardtshbm.oil_detector.desc2"));
    }

    private static boolean scanColumn(Level level, int x, int y, int z) {
        return scanColumn(level, x, y, z, 5);
    }

    private static boolean scanColumn(Level level, int x, int y, int z, int minY) {
        Block target = HbmBlocks.ORE_OIL.get();
        for (int i = y + 15; i > minY; i--) {
            if (level.getBlockState(new BlockPos(x, i, z)).is(target)) {
                return true;
            }
        }
        return false;
    }
}
