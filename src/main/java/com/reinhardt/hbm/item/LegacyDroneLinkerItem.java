package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.DroneLinkable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Direct modern port of ItemDroneLinker: select one node, then link it to another. */
public final class LegacyDroneLinkerItem extends Item {
    public LegacyDroneLinkerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof DroneLinkable target)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos selected = stack.get(LegacyItemComponents.DRONE_LINK_ORIGIN.get());
        ServerPlayer player = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (selected == null) {
            stack.set(LegacyItemComponents.DRONE_LINK_ORIGIN.get(), context.getClickedPos().immutable());
            message(player, "item.reinhardtshbm.drone_linker.set_initial", ChatFormatting.AQUA);
            return InteractionResult.CONSUME;
        }

        if (context.getLevel().getBlockEntity(selected) instanceof DroneLinkable origin) {
            origin.setDroneTarget(target.dronePoint());
            message(player, "item.reinhardtshbm.drone_linker.link_set", ChatFormatting.AQUA);
        } else {
            message(player, "item.reinhardtshbm.drone_linker.previous_lost", ChatFormatting.RED);
        }
        stack.set(LegacyItemComponents.DRONE_LINK_ORIGIN.get(), context.getClickedPos().immutable());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && stack.has(LegacyItemComponents.DRONE_LINK_ORIGIN.get())) {
            stack.remove(LegacyItemComponents.DRONE_LINK_ORIGIN.get());
            message(player instanceof ServerPlayer serverPlayer ? serverPlayer : null,
                    "item.reinhardtshbm.drone_linker.position_cleared", ChatFormatting.GREEN);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos selected = stack.get(LegacyItemComponents.DRONE_LINK_ORIGIN.get());
        if (selected != null) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.drone_linker.position",
                    selected.getX(), selected.getY(), selected.getZ()).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private static void message(ServerPlayer player, String key, ChatFormatting color) {
        if (player != null) {
            player.sendSystemMessage(Component.translatable(key).withStyle(color));
        }
    }
}
