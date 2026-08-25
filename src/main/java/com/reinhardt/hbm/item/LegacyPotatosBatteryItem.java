package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** 1.7.10 ItemPotatos: a battery that periodically talks while held. */
public final class LegacyPotatosBatteryItem extends FixedBatteryItem {
    private static final String TIMER_KEY = "potatos_timer";

    public LegacyPotatosBatteryItem(Item.Properties properties, long maxCharge, long chargeRate, long dischargeRate) {
        super(properties, maxCharge, chargeRate, dischargeRate);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (level.isClientSide || !selected || !(entity instanceof Player player) || hbmCharge(stack) <= 0L) {
            return;
        }

        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int timer = data.getInt(TIMER_KEY);
        if (timer > 0) {
            data.putInt(TIMER_KEY, timer - 1);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            return;
        }

        // ItemPotatos only speaks when it is the selected main-hand stack.
        if (player.getMainHandItem() != stack) {
            return;
        }

        float pitch = (float) hbmCharge(stack) / (float) hbmCapacity(stack) * 0.5F + 0.5F;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundEvents.POTATOS_RANDOM.get(),
                SoundSource.PLAYERS, 1.0F, pitch);
        data.putInt(TIMER_KEY, 200 + level.random.nextInt(100));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }
}
