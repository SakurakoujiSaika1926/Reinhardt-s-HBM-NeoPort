package com.reinhardt.hbm.item;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.registry.HbmDataAttachments;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The shield fields from the old HbmPlayerProps capability.  The infusion
 * item, damage absorption and delayed recharge all use this one synced state.
 */
public final class HbmPlayerShield {
    public static final float CAP = 100.0F;
    private static final float MAX_EFFECTIVE_CAP = 125.0F;
    public static final StreamCodec<RegistryFriendlyByteBuf, HbmPlayerShield> STREAM_CODEC = StreamCodec.of(
            (buffer, data) -> buffer.writeNbt(data.save()),
            buffer -> {
                HbmPlayerShield data = new HbmPlayerShield();
                data.load(buffer.readNbt());
                return data;
            }
    );
    public static final Codec<HbmPlayerShield> CODEC = CompoundTag.CODEC.xmap(
            tag -> {
                HbmPlayerShield data = new HbmPlayerShield();
                data.load(tag);
                return data;
            },
            HbmPlayerShield::save
    );

    private float shield;
    private float maxShield;
    private int lastDamage;

    public static HbmPlayerShield get(Player player) {
        return player.getData(HbmDataAttachments.PLAYER_SHIELD);
    }

    public static void set(Player player, HbmPlayerShield shield) {
        player.setData(HbmDataAttachments.PLAYER_SHIELD, shield);
    }

    public static void addInfusion(Player player, float amount) {
        HbmPlayerShield data = get(player);
        data.maxShield = Math.min(CAP, data.maxShield + amount);
        data.shield = Math.min(effectiveMaxShield(player, data), data.shield + amount);
        set(player, data);
    }

    public static void tick(Player player) {
        HbmPlayerShield data = get(player);
        float effectiveMaxShield = effectiveMaxShield(player, data);
        if (data.shield < effectiveMaxShield && player.tickCount > data.lastDamage + 60) {
            int elapsed = player.tickCount - (data.lastDamage + 60);
            data.shield += Math.min(effectiveMaxShield - data.shield, 0.005F * elapsed);
        }
        if (data.shield > effectiveMaxShield) {
            data.shield = effectiveMaxShield;
        }
        set(player, data);
    }

    public static void absorb(Player player, LivingIncomingDamageEvent event) {
        HbmPlayerShield data = get(player);
        if (data.shield > 0.0F) {
            float reduced = Math.min(data.shield, event.getAmount());
            data.shield -= reduced;
            event.setAmount(event.getAmount() - reduced);
        }
        data.lastDamage = player.tickCount;
        set(player, data);
    }

    public float shield() {
        return shield;
    }

    public float maxShield() {
        return maxShield;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("shield", shield);
        tag.putFloat("max_shield", maxShield);
        tag.putInt("last_damage", lastDamage);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        shield = Math.max(0.0F, Math.min(MAX_EFFECTIVE_CAP, tag.getFloat("shield")));
        maxShield = Math.max(0.0F, Math.min(CAP, tag.getFloat("max_shield")));
        lastDamage = Math.max(0, tag.getInt("last_damage"));
    }

    private static float effectiveMaxShield(Player player, HbmPlayerShield data) {
        float max = data.maxShield;
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack insert = ArmorModHandler.pryMods(chestplate, player.registryAccess())[ArmorModHandler.KEVLAR];
        if (insert.getItem() instanceof LegacyShieldArmorModItem shieldMod) {
            max += shieldMod.shield();
        }
        return Math.min(MAX_EFFECTIVE_CAP, max);
    }
}
