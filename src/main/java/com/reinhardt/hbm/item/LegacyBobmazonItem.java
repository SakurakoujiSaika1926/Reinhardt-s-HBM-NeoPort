package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.screen.BobmazonScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/** 1.7.10 ItemCatalog, split into the public and hidden catalog variants. */
public final class LegacyBobmazonItem extends Item {
    private final boolean hidden;

    public LegacyBobmazonItem(Properties properties, boolean hidden) {
        super(properties.stacksTo(1));
        this.hidden = hidden;
    }

    public boolean hidden() {
        return hidden;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && FMLEnvironment.dist.isClient()) {
            BobmazonScreen.open(hidden);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (hidden) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.bobmazon_hidden.1").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.bobmazon_hidden.2").withStyle(ChatFormatting.GRAY));
        }
    }
}
