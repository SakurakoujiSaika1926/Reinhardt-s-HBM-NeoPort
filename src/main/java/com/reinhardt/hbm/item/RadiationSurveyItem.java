package com.reinhardt.hbm.item;

import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RadiationSurveyItem extends Item {
    private static final List<DeferredHolder<SoundEvent, SoundEvent>> GEIGER_SOUNDS = List.of(
            HbmSoundEvents.GEIGER_1,
            HbmSoundEvents.GEIGER_2,
            HbmSoundEvents.GEIGER_3,
            HbmSoundEvents.GEIGER_4,
            HbmSoundEvents.GEIGER_5,
            HbmSoundEvents.GEIGER_6
    );

    private final int indicatorColor;
    private final String descriptionKey;

    public RadiationSurveyItem(Properties properties, int indicatorColor, String descriptionKey) {
        super(properties);
        this.indicatorColor = indicatorColor;
        this.descriptionKey = descriptionKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reinhardtshbm." + descriptionKey + ".hint")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player) || level.getGameTime() % 5L != 0L) {
            return;
        }

        double received = receivedRadiation(player);
        if ("dosimeter".equals(this.descriptionKey)) {
            playDosimeter(level, player, received);
        } else {
            playGeiger(level, player, received);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!level.isClientSide) {
            if ("dosimeter".equals(this.descriptionKey)) {
                printDosimeterData(player);
            } else {
                printGeigerData(level, player);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void playGeiger(Level level, Player player, double received) {
        if (received > 1.0E-5D) {
            List<Integer> choices = new ArrayList<>(8);
            if (received < 1.0D) choices.add(0);
            if (received < 5.0D) choices.add(0);
            if (received < 10.0D) choices.add(1);
            if (received > 5.0D && received < 15.0D) choices.add(2);
            if (received > 10.0D && received < 20.0D) choices.add(3);
            if (received > 15.0D && received < 25.0D) choices.add(4);
            if (received > 20.0D && received < 30.0D) choices.add(5);
            if (received > 25.0D) choices.add(6);
            int chosen = choices.get(level.random.nextInt(choices.size()));
            if (chosen > 0) {
                playSound(level, player, chosen);
            }
        } else if (level.random.nextInt(100) == 0) {
            playSound(level, player, 1);
        }
    }

    private static void playDosimeter(Level level, Player player, double received) {
        if (received > 1.0E-5D) {
            List<Integer> choices = new ArrayList<>(4);
            if (received < 0.5D) choices.add(0);
            if (received < 1.0D) choices.add(1);
            if (received >= 0.5D && received < 2.0D) choices.add(2);
            if (received >= 1.0D && received >= 2.0D) choices.add(3);
            int chosen = choices.get(level.random.nextInt(choices.size()));
            if (chosen > 0) {
                playSound(level, player, chosen);
            }
        } else if (level.random.nextInt(100) == 0) {
            playSound(level, player, 1);
        }
    }

    private static void playSound(Level level, Player player, int oneBasedIndex) {
        int index = Math.max(1, Math.min(oneBasedIndex, GEIGER_SOUNDS.size())) - 1;
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                GEIGER_SOUNDS.get(index).get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    private static void printGeigerData(Level level, Player player) {
        HbmLivingRadiation data = HbmLivingRadiation.get(player);
        double body = data.getRadiation();
        double chunkRadiation = level instanceof ServerLevel serverLevel
                ? ChunkRadiationData.get(serverLevel).getRadiation(player.blockPosition())
                : data.getChunkRadiation();
        double environment = data.getRadiationBuffer();
        double received = receivedRadiation(player);

        player.sendSystemMessage(Component.literal("===== ")
                .append(Component.translatable("geiger.title"))
                .append(Component.literal(" ====="))
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("geiger.chunkRad")
                .append(Component.literal(" " + formatNumber(chunkRadiation) + " RAD/s").withStyle(radColor(chunkRadiation)))
                .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("geiger.envRad")
                .append(Component.literal(" " + formatNumber(environment) + " RAD/s").withStyle(radColor(environment)))
                .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("geiger.recievedRad")
                .append(Component.literal(" " + formatNumber(received) + " RAD/s").withStyle(radColor(received)))
                .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("geiger.playerRad")
                .append(Component.literal(" " + formatNumber(body) + " RAD").withStyle(bodyColor(body)))
                .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("geiger.playerRes")
                .append(Component.literal(" 0.000000% (0.00)").withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.YELLOW));
    }

    private static void printDosimeterData(Player player) {
        double received = receivedRadiation(player);
        boolean limit = false;
        if (received > 3.6D) {
            received = 3.6D;
            limit = true;
        }
        received = ((int) (1000.0D * received)) / 1000.0D;

        player.sendSystemMessage(Component.literal("===== ")
                .append(Component.translatable("dosimeter.title"))
                .append(Component.literal(" ====="))
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("geiger.recievedRad")
                .append(Component.literal(" " + (limit ? ">" : "") + formatFixed(received) + " RAD/s")
                        .withStyle(radColor(received)))
                .withStyle(ChatFormatting.YELLOW));
    }

    private static double receivedRadiation(Player player) {
        HbmLivingRadiation data = HbmLivingRadiation.get(player);
        return data.getRadiationBuffer() + data.getNeutron() * 20.0D;
    }

    private static String formatNumber(double value) {
        double abs = Math.abs(value);
        if (abs >= 1.0E6D || (abs > 0.0D && abs < 1.0E-3D)) {
            return String.format(Locale.ROOT, "%.3e", value);
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String formatFixed(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static ChatFormatting radColor(double rads) {
        if (rads == 0.0D) return ChatFormatting.GREEN;
        if (rads < 1.0D) return ChatFormatting.YELLOW;
        if (rads < 10.0D) return ChatFormatting.GOLD;
        if (rads < 100.0D) return ChatFormatting.RED;
        if (rads < 1000.0D) return ChatFormatting.DARK_RED;
        return ChatFormatting.DARK_GRAY;
    }

    private static ChatFormatting bodyColor(double rads) {
        if (rads < 200.0D) return ChatFormatting.GREEN;
        if (rads < 400.0D) return ChatFormatting.YELLOW;
        if (rads < 600.0D) return ChatFormatting.GOLD;
        if (rads < 800.0D) return ChatFormatting.RED;
        if (rads < 1000.0D) return ChatFormatting.DARK_RED;
        return ChatFormatting.DARK_GRAY;
    }

    public int getIndicatorColor() {
        return indicatorColor;
    }
}
