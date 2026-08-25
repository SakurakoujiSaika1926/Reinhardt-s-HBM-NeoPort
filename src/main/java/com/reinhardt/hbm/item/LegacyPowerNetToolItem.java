package com.reinhardt.hbm.item;

import com.reinhardt.hbm.power.PowerNetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Direct 1.7.10 ItemPowerNetTool diagnostic port. */
public final class LegacyPowerNetToolItem extends Item {
    private static final int DISPLAY_RADIUS = 20;

    public LegacyPowerNetToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        PowerNetworkManager.NetworkDiagnostics diagnostics = PowerNetworkManager.diagnostics(level, context.getClickedPos());
        if (diagnostics == null) {
            player.displayClientMessage(Component.translatable("item.reinhardtshbm.power_net_tool.no_network")
                    .withStyle(ChatFormatting.RED), false);
            return InteractionResult.FAIL;
        }

        player.displayClientMessage(Component.translatable("item.reinhardtshbm.power_net_tool.start", diagnostics.id())
                .withStyle(ChatFormatting.GOLD), false);
        player.displayClientMessage(Component.translatable("item.reinhardtshbm.power_net_tool.links", diagnostics.links())
                .withStyle(ChatFormatting.YELLOW), false);
        player.displayClientMessage(Component.translatable("item.reinhardtshbm.power_net_tool.providers", diagnostics.providers())
                .withStyle(ChatFormatting.YELLOW), false);
        player.displayClientMessage(Component.translatable("item.reinhardtshbm.power_net_tool.receivers", diagnostics.receivers())
                .withStyle(ChatFormatting.YELLOW), false);
        player.displayClientMessage(Component.translatable("item.reinhardtshbm.power_net_tool.end", diagnostics.id())
                .withStyle(ChatFormatting.GOLD), false);

        ServerLevel server = (ServerLevel) level;
        for (BlockPos pos : diagnostics.linkPositions()) {
            if (player.blockPosition().distManhattan(pos) <= DISPLAY_RADIUS) {
                server.sendParticles((ServerPlayer) player, DustParticleOptions.REDSTONE,
                        true, pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D,
                        3, 0.2D, 0.2D, 0.2D, 0.0D);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reinhardtshbm.power_net_tool.hint").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("item.reinhardtshbm.power_net_tool.radius", DISPLAY_RADIUS).withStyle(ChatFormatting.RED));
    }
}
