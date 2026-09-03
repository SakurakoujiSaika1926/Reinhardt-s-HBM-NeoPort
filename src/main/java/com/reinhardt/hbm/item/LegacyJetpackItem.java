package com.reinhardt.hbm.item;

import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Fuelled jetpack armor modifier with the old four movement profiles. */
public final class LegacyJetpackItem extends ArmorModItem {
    public enum Profile { REGULAR, BREAK, VECTOR, BOOST }
    private static final String FUEL = "jetpack_fuel";
    private final String fuelName;
    private final int capacity;
    private final Profile profile;

    public LegacyJetpackItem(Properties properties, String fuelName, int capacity, Profile profile) {
        super(properties.stacksTo(1), ArmorModHandler.PLATE_ONLY, false, true, false, false);
        this.fuelName = fuelName;
        this.capacity = capacity;
        this.profile = profile;
    }

    public static int fuel(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getInt(FUEL);
    }

    public int capacity() {
        return this.capacity;
    }

    public String fuelName() {
        return this.fuelName;
    }

    public boolean acceptsFuel(String fluidName) {
        return this.fuelName.equals(fluidName);
    }

    /** Mirrors JetpackFueledBase#tryFill: returns the amount actually accepted. */
    public int fill(ItemStack stack, String fluidName, int amount) {
        if (amount <= 0 || !acceptsFuel(fluidName)) {
            return 0;
        }
        int accepted = Math.min(amount, Math.max(0, this.capacity - fuel(stack)));
        if (accepted > 0) {
            setFuel(stack, fuel(stack) + accepted);
        }
        return accepted;
    }

    @Override
    public void tickArmor(Player player, ItemStack armor) {
        ItemStack jetpack = ArmorModHandler.pryMods(armor, player.registryAccess())[ArmorModHandler.PLATE_ONLY];
        if (jetpack.isEmpty() || fuel(jetpack) <= 0) {
            return;
        }
        // The vanilla jump key is client-side state. Until the jetpack input
        // payload is installed, rising motion/flying is the authoritative
        // server-side activation signal and avoids inventing a permanent
        // always-on mode.
        boolean active = player.getAbilities().flying || player.isFallFlying()
                || player.getDeltaMovement().y > 0.0D;
        boolean autoHover = profile == Profile.BREAK && !player.onGround() && !player.isCrouching();
        if (!active && !autoHover) {
            return;
        }
        var velocity = player.getDeltaMovement();
        double max = switch (profile) {
            case REGULAR, BREAK -> 0.4D;
            case VECTOR -> 2.0D;
            case BOOST -> 5.0D;
        };
        double lift = switch (profile) {
            case REGULAR, BREAK, VECTOR -> 0.1D;
            case BOOST -> 0.25D;
        };
        if (active) {
            double y = Math.min(max, velocity.y + lift);
            if (profile == Profile.VECTOR || profile == Profile.BOOST) {
                var look = player.getLookAngle();
                velocity = velocity.add(look.scale(profile == Profile.BOOST ? 0.25D : 0.1D));
            }
            player.setDeltaMovement(velocity.x, y, velocity.z);
        } else if (profile == Profile.BREAK && velocity.y < 0.0D) {
            player.setDeltaMovement(velocity.x, Math.max(-0.1D, velocity.y + 0.1D), velocity.z);
        }
        player.fallDistance = 0.0F;
        if (player.tickCount % (profile == Profile.BOOST ? 1 : profile == Profile.VECTOR ? 3 : 5) == 0) {
            setFuel(jetpack, fuel(jetpack) - 1);
            ArmorModHandler.applyMod(armor, jetpack, player.registryAccess());
        }
    }

    public static void setFuel(ItemStack stack, int amount) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.putInt(FUEL, Math.max(0, amount));
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int descriptionLines = switch (this.profile) {
            case REGULAR -> 1;
            case BREAK -> 3;
            case VECTOR, BOOST -> 2;
        };
        String profileKey = "tooltip.reinhardtshbm.jetpack.profile." + this.profile.name().toLowerCase();
        for (int line = 1; line <= descriptionLines; line++) {
            tooltip.add(Component.translatable(profileKey + "." + line).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.jetpack.fuel",
                        Component.translatable("hbmfluid." + this.fuelName), fuel(stack), this.capacity)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
