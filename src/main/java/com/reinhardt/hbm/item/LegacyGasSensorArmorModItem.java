package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Direct scan radius and cadence from 1.7.10 ItemModSensor. */
public final class LegacyGasSensorArmorModItem extends ArmorModItem {
    public LegacyGasSensorArmorModItem(Item.Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (entity instanceof Player player) {
            detect(player);
        }
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        detect(player);
    }

    private static void detect(Player player) {
        Level level = player.level();
        if (level.isClientSide || level.getGameTime() % 20L != 0L) {
            return;
        }

        BlockPos origin = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        boolean poison = false;
        boolean explosive = false;
        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = origin.offset(x * 2, y * 2, z * 2);
                    if (level.getBlockState(pos).is(HbmBlocks.GAS_FLAMMABLE.get())
                            || level.getBlockState(pos).is(HbmBlocks.GAS_EXPLOSIVE.get())) {
                        explosive = true;
                    }
                    if (level.getBlockState(pos).is(HbmBlocks.GAS_ASBESTOS.get())
                            || level.getBlockState(pos).is(HbmBlocks.GAS_COAL.get())
                            || level.getBlockState(pos).is(HbmBlocks.GAS_RADON.get())
                            || level.getBlockState(pos).is(HbmBlocks.GAS_MONOXIDE.get())
                            || level.getBlockState(pos).is(HbmBlocks.GAS_RADON_DENSE.get())
                            || level.getBlockState(pos).is(HbmBlocks.CHLORINE_GAS.get())) {
                        poison = true;
                    }
                }
            }
        }

        if (explosive) {
            level.playSound(null, player.blockPosition(), HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        } else if (poison) {
            level.playSound(null, player.blockPosition(), HbmSoundEvents.TECH_BOOP.get(), SoundSource.PLAYERS, 2.0F, 1.5F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.armor_mod.gas_sensor")
                .withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
