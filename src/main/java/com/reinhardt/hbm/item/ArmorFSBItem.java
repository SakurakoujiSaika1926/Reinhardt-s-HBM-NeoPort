package com.reinhardt.hbm.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import com.reinhardt.hbm.registry.HbmSoundEvents;

import java.util.List;
import java.util.Map;

public class ArmorFSBItem extends ArmorItem {
    private static final String DASH_COOLDOWN_TAG = "reinhardtshbm_dash_cooldown";
    private static final String DASH_STAMINA_TAG = "reinhardtshbm_dash_stamina";
    private static final int DASH_COOLDOWN_TICKS = 5;
    private static final int DASH_STAMINA_PER_USE = 30;
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    );

    private final String fsbGroup;
    private final boolean noHelmet;
    private final List<FullSetEffect> fullSetEffects;

    /**
     * Feature flags carried by the 1.7.10 ArmorFSB registry.  Keeping these
     * by suit group preserves the old cloneStats behaviour while allowing all
     * four armor slots to share one authoritative profile in 1.21.1.
     */
    private static final Map<String, FeatureProfile> FEATURES = Map.ofEntries(
            Map.entry("t51", new FeatureProfile(true, false, true, false, true, 0, 0)),
            Map.entry("steamsuit", new FeatureProfile(false, false, false, false, true, 0, 0)),
            Map.entry("dieselsuit", new FeatureProfile(true, true, false, false, false, 0, 0)),
            Map.entry("ajr", new FeatureProfile(true, false, true, false, true, 0, 0)),
            Map.entry("ajro", new FeatureProfile(true, false, true, false, true, 0, 0)),
            Map.entry("rpa", new FeatureProfile(true, false, true, false, true, 0, 0)),
            Map.entry("ncrpa", new FeatureProfile(true, false, true, false, true, 0, 0)),
            Map.entry("blackjack", new FeatureProfile(true, true, true, false, true, 0, 0)),
            Map.entry("hev", new FeatureProfile(false, false, true, true, false, 0, 0)),
            Map.entry("fau", new FeatureProfile(false, true, true, false, true, 0, 0)),
            Map.entry("dns", new FeatureProfile(true, true, true, false, true, 0, 0)),
            Map.entry("dnt", new FeatureProfile(false, true, false, false, true, 0, 0)),
            Map.entry("bismuth", new FeatureProfile(false, false, false, false, false, 0, 3)),
            Map.entry("taurun", new FeatureProfile(false, false, false, false, false, 1, 0)),
            Map.entry("trenchmaster", new FeatureProfile(true, false, false, false, false, 1, 0))
    );

    public ArmorFSBItem(
            String fsbGroup,
            Holder<ArmorMaterial> material,
            Type type,
            boolean noHelmet,
            List<FullSetEffect> fullSetEffects,
            Properties properties
    ) {
        super(material, type, properties);
        this.fsbGroup = fsbGroup;
        this.noHelmet = noHelmet;
        this.fullSetEffects = List.copyOf(fullSetEffects);
    }

    /**
     * Legacy powered suits are rendered by the client OBJ layers.  Hide the
     * vanilla humanoid armor model for these sets; their ArmorMaterials are
     * kept for item/repair metadata, not for the old entity geometry.
     */
    @Override
    public void initializeClient(
            java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer
    ) {
        if (!usesLegacyObjModel(this.fsbGroup)) {
            return;
        }
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    net.minecraft.world.entity.LivingEntity livingEntity,
                    net.minecraft.world.item.ItemStack itemStack,
                    net.minecraft.world.entity.EquipmentSlot equipmentSlot,
                    net.minecraft.client.model.HumanoidModel<?> original
            ) {
                original.setAllVisible(false);
                return original;
            }
        });
    }

    private static boolean usesLegacyObjModel(String group) {
        return switch (group) {
            case "t51", "bismuth", "steamsuit", "dieselsuit", "ajr", "ajro",
                    "rpa", "ncrpa", "blackjack", "envsuit", "hev", "fau",
                    "dns", "dnt", "taurun", "trenchmaster" -> true;
            default -> false;
        };
    }

    public static FullSetEffect effect(Holder<MobEffect> effect, int duration, int amplifier) {
        return new FullSetEffect(effect, duration, amplifier);
    }

    public static boolean hasFSBArmor(Player player) {
        return hasFSBArmor(player, true);
    }

    /** Matches 1.7.10's hasFSBArmorIgnoreCharge(), used by HEV battery blocks. */
    public static boolean hasFSBArmorIgnoringCharge(Player player) {
        return hasFSBArmor(player, false);
    }

    private static boolean hasFSBArmor(Player player, boolean requireEnabled) {
        if (player == null) {
            return false;
        }

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ArmorFSBItem chestplate)) {
            return false;
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (chestplate.noHelmet && slot == EquipmentSlot.HEAD) {
                continue;
            }

            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorFSBItem armor)) {
                return false;
            }
            if (!armor.fsbGroup.equals(chestplate.fsbGroup)) {
                return false;
            }
            if (requireEnabled && !armor.isArmorEnabled(stack)) {
                return false;
            }
        }
        return true;
    }

    public static String fullSetGroup(LivingEntity living) {
        String group = null;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = living.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ArmorFSBItem armor) || armor.noHelmet) {
                return "";
            }
            if (group == null) {
                group = armor.fsbGroup;
            } else if (!group.equals(armor.fsbGroup)) {
                return "";
            }
        }
        return group == null ? "" : group;
    }

    public static boolean hasFSBArmorHelmet(Player player) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        return chestStack.getItem() instanceof ArmorFSBItem chestplate
                && !chestplate.noHelmet
                && hasFSBArmor(player);
    }

    public static FeatureProfile features(LivingEntity entity) {
        String group = fullSetGroup(entity);
        return FEATURES.getOrDefault(group, FeatureProfile.NONE);
    }

    public static boolean hasFeature(LivingEntity entity, Feature feature) {
        FeatureProfile profile = features(entity);
        return switch (feature) {
            case VATS -> profile.vats();
            case THERMAL -> profile.thermal();
            case GEIGER_SOUND -> profile.geigerSound();
            case CUSTOM_GEIGER -> profile.customGeiger();
            case HARD_LANDING -> profile.hardLanding();
        };
    }

    /** Handles the 1.7.10 bismuth armour dash using the old stamina model. */
    public static void tickDash(Player player) {
        FeatureProfile profile = features(player);
        if (profile.dashCount() <= 0 || !hasFSBArmor(player)) {
            player.getPersistentData().remove(DASH_COOLDOWN_TAG);
            player.getPersistentData().remove(DASH_STAMINA_TAG);
            return;
        }
        var tag = player.getPersistentData();
        int max = profile.dashCount() * DASH_STAMINA_PER_USE;
        int stamina = Math.min(max, tag.getInt(DASH_STAMINA_TAG) + 1);
        tag.putInt(DASH_STAMINA_TAG, stamina);
        int cooldown = tag.getInt(DASH_COOLDOWN_TAG);
        if (cooldown > 0) {
            tag.putInt(DASH_COOLDOWN_TAG, cooldown - 1);
        }
    }

    /** Server-authoritative dash trigger (bound to the legacy V key). */
    public static void performDash(Player player) {
        FeatureProfile profile = features(player);
        if (!hasFSBArmor(player) || profile.dashCount() <= 0) {
            return;
        }
        var tag = player.getPersistentData();
        if (tag.getInt(DASH_COOLDOWN_TAG) > 0
                || tag.getInt(DASH_STAMINA_TAG) < DASH_STAMINA_PER_USE) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0.0D, look.z);
        if (direction.lengthSqr() < 1.0E-5D) {
            return;
        }
        direction = direction.normalize();
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x + direction.x, 0.0D, velocity.z + direction.z);
        player.fallDistance = 0.0F;
        tag.putInt(DASH_STAMINA_TAG, tag.getInt(DASH_STAMINA_TAG) - DASH_STAMINA_PER_USE);
        tag.putInt(DASH_COOLDOWN_TAG, DASH_COOLDOWN_TICKS);
        player.level().playSound(null, player.blockPosition(), HbmSoundEvents.WEAPON_ROCKET_FLAME.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static void tickFullSet(Player player) {
        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ArmorFSBItem chestplate) || !hasFSBArmor(player)) {
            return;
        }

        for (FullSetEffect effect : chestplate.fullSetEffects) {
            player.addEffect(new MobEffectInstance(
                    effect.effect(),
                    effect.duration(),
                    effect.amplifier(),
                    false,
                    false
            ));
        }
    }

    /**
     * Fuelled and powered 1.7.10 suits only enabled their set bonuses while
     * every equipped component still had usable fuel or charge.
     */
    public boolean isArmorEnabled(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this.fullSetEffects.isEmpty()) {
            return;
        }

        tooltip.add(Component.translatable("armor.reinhardtshbm.full_set_bonus").withStyle(ChatFormatting.GOLD));
        for (FullSetEffect effect : this.fullSetEffects) {
            tooltip.add(Component.literal("  ")
                    .append(Component.translatable(effect.effect().value().getDescriptionId()))
                    .withStyle(ChatFormatting.AQUA));
        }
        FeatureProfile profile = FEATURES.getOrDefault(this.fsbGroup, FeatureProfile.NONE);
        if (profile.vats()) tooltip.add(Component.translatable("armor.reinhardtshbm.vats").withStyle(ChatFormatting.RED));
        if (profile.thermal()) tooltip.add(Component.translatable("armor.reinhardtshbm.thermal").withStyle(ChatFormatting.RED));
        if (profile.geigerSound()) tooltip.add(Component.translatable("armor.reinhardtshbm.geiger").withStyle(ChatFormatting.GOLD));
        if (profile.customGeiger()) tooltip.add(Component.translatable("armor.reinhardtshbm.geiger_hud").withStyle(ChatFormatting.GOLD));
        if (profile.hardLanding()) tooltip.add(Component.translatable("armor.reinhardtshbm.hard_landing").withStyle(ChatFormatting.RED));
        if (profile.stepHeight() > 0) tooltip.add(Component.translatable("armor.reinhardtshbm.step", profile.stepHeight()).withStyle(ChatFormatting.BLUE));
        if (profile.dashCount() > 0) tooltip.add(Component.translatable("armor.reinhardtshbm.dash", profile.dashCount()).withStyle(ChatFormatting.AQUA));
    }

    public record FullSetEffect(Holder<MobEffect> effect, int duration, int amplifier) {
    }

    public enum Feature {
        VATS, THERMAL, GEIGER_SOUND, CUSTOM_GEIGER, HARD_LANDING
    }

    public record FeatureProfile(
            boolean vats,
            boolean thermal,
            boolean geigerSound,
            boolean customGeiger,
            boolean hardLanding,
            int stepHeight,
            int dashCount
    ) {
        public static final FeatureProfile NONE = new FeatureProfile(false, false, false, false, false, 0, 0);
    }
}
