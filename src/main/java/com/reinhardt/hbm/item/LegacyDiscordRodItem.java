package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Direct port of ItemDiscord's unbounded, no-cost line-of-sight teleport. */
public final class LegacyDiscordRodItem extends Item {
    public LegacyDiscordRodItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(100.0D, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            return InteractionResultHolder.pass(stack);
        }

        Vec3 source = player.position();
        Direction side = blockHit.getDirection();
        Vec3 target = blockHit.getLocation().add(side.getStepX(), side.getStepY() - 1.0D, side.getStepZ());
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            player.unRide();
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHORUS_FRUIT_TELEPORT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            serverPlayer.teleportTo(target.x, target.y, target.z);
            serverLevel.playSound(null, target.x, target.y, target.z, SoundEvents.CHORUS_FRUIT_TELEPORT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            serverPlayer.fallDistance = 0.0F;
            spawnPortalParticles(serverLevel, source);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void spawnPortalParticles(ServerLevel level, Vec3 source) {
        for (int index = 0; index < 32; index++) {
            level.sendParticles(ParticleTypes.PORTAL,
                    source.x,
                    source.y + level.random.nextDouble() * 2.0D,
                    source.z,
                    1,
                    level.random.nextGaussian(),
                    0.0D,
                    level.random.nextGaussian(),
                    0.0D);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rod_of_discord.1").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rod_of_discord.2").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rod_of_discord.3").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rod_of_discord.4").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rod_of_discord.5").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.rod_of_discord.6").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }
}
