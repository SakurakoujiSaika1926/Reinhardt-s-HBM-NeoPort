package com.reinhardt.hbm.item;

import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 1.7.10 ItemUnstable: the normal variant detonates after 200 carried ticks.
 */
public final class LegacyUnstableItem extends Item {
    private static final String TIMER_TAG = "timer";
    private static final String LEGACY_VARIANT_TAG = "legacy_damage";
    private static final int DETONATION_RADIUS = 350;
    private static final int DETONATION_TIMER = 200;

    public LegacyUnstableItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (variant(stack) != 0) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int timer = timer(stack) + 1;
        if (timer < DETONATION_TIMER) {
            setTimer(stack, timer);
            return;
        }

        NukeExplosionManager.scheduleLegacyNuke(serverLevel, entity.getX(), entity.getY(), entity.getZ(), DETONATION_RADIUS);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(), HbmSoundEvents.ENTITY_OLD_EXPLOSION.get(),
                SoundSource.HOSTILE, 1.0F, 1.0F);
        entity.hurt(serverLevel.damageSources().explosion(null), Float.MAX_VALUE);
        stack.shrink(1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (variant(stack) != 0) {
            return;
        }
        int percentage = timer(stack) * 100 / DETONATION_TIMER;
        tooltip.add(Component.translatable("item.reinhardtshbm.ingot_u238m2.decay", percentage).withStyle(ChatFormatting.RED));
    }

    @Override
    public Component getName(ItemStack stack) {
        return switch (variant(stack)) {
            case 1 -> Component.literal("ELEMENTS");
            case 2 -> Component.literal("ARSENIC");
            case 3 -> Component.literal("VAULT");
            default -> super.getName(stack);
        };
    }

    public static ItemStack stackFor(Item item, int variant) {
        ItemStack stack = new ItemStack(item);
        if (variant > 0) {
            CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            data.putInt(LEGACY_VARIANT_TAG, variant);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        return stack;
    }

    public static int variant(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(LEGACY_VARIANT_TAG);
    }

    private static int timer(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(TIMER_TAG);
    }

    private static void setTimer(ItemStack stack, int timer) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TIMER_TAG, timer);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
