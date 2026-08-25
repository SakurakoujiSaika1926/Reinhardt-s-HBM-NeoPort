package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The original ItemEnergy family. These were not food items in 1.7.10: they
 * applied their specific effects after the drink animation and returned the
 * matching can, bottle and cap items.
 */
public class LegacyEnergyDrinkItem extends Item {
    private static final Set<String> BOTTLES = Set.of(
            "bottle_nuka", "bottle_cherry", "bottle_quantum", "bottle_sparkle", "bottle_rad",
            "bottle2_korl", "bottle2_fritz"
    );
    private static final Map<String, String> CONTAINERS = Map.ofEntries(
            Map.entry("can_smart", "can_empty"),
            Map.entry("can_creature", "can_empty"),
            Map.entry("can_redbomb", "can_empty"),
            Map.entry("can_mrsugar", "can_empty"),
            Map.entry("can_overcharge", "can_empty"),
            Map.entry("can_luna", "can_empty"),
            Map.entry("can_bepis", "can_empty"),
            Map.entry("can_breen", "can_empty"),
            Map.entry("can_mug", "can_empty"),
            Map.entry("bottle_nuka", "bottle_empty"),
            Map.entry("bottle_cherry", "bottle_empty"),
            Map.entry("bottle_quantum", "bottle_empty"),
            Map.entry("bottle_sparkle", "bottle_empty"),
            Map.entry("bottle_rad", "bottle_empty"),
            Map.entry("bottle2_korl", "bottle2_empty"),
            Map.entry("bottle2_fritz", "bottle2_empty")
    );
    private static final Map<String, String> CAPS = Map.ofEntries(
            Map.entry("can_smart", "ring_pull"),
            Map.entry("can_creature", "ring_pull"),
            Map.entry("can_redbomb", "ring_pull"),
            Map.entry("can_mrsugar", "ring_pull"),
            Map.entry("can_overcharge", "ring_pull"),
            Map.entry("can_luna", "ring_pull"),
            Map.entry("can_bepis", "ring_pull"),
            Map.entry("can_breen", "ring_pull"),
            Map.entry("can_mug", "ring_pull"),
            Map.entry("bottle_nuka", "cap_nuka"),
            Map.entry("bottle_cherry", "cap_nuka"),
            Map.entry("bottle_quantum", "cap_quantum"),
            Map.entry("bottle_sparkle", "cap_sparkle"),
            Map.entry("bottle_rad", "cap_rad"),
            Map.entry("bottle2_korl", "cap_korl"),
            Map.entry("bottle2_fritz", "cap_fritz")
    );

    private final String id;

    public LegacyEnergyDrinkItem(Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.hasEffect(HbmMobEffects.POTION_SICKNESS)) {
            return InteractionResultHolder.fail(stack);
        }
        if (BOTTLES.contains(id) && !hasItem(player, "bottle_opener")) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (!(living instanceof Player player)) {
            return stack;
        }
        if (!level.isClientSide) {
            if (player instanceof FakePlayer) {
                level.explode(player, player.getX(), player.getY(), player.getZ(), 5.0F, true, Level.ExplosionInteraction.TNT);
            } else {
                player.addEffect(new MobEffectInstance(HbmMobEffects.POTION_SICKNESS, 5 * 20));
                applyLegacyEffects(level, player);
            }
        }
        if (player.getAbilities().instabuild) {
            return stack;
        }

        stack.shrink(1);
        give(player, itemStack(CAPS.get(id)));
        ItemStack container = itemStack(CONTAINERS.get(id));
        if (!container.isEmpty()) {
            if (stack.isEmpty()) {
                return container;
            }
            give(player, container);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        switch (id) {
            case "can_smart" -> tooltip.add(Component.literal("Cheap and full of bubbles"));
            case "can_creature" -> tooltip.add(Component.literal("Basically gasoline in a tin can"));
            case "can_redbomb" -> tooltip.add(Component.literal("Liquefied explosives"));
            case "can_mrsugar" -> tooltip.add(Component.literal("An intellectual drink, for the chosen ones!"));
            case "can_overcharge" -> tooltip.add(Component.literal("Possible side effects include heart attacks, seizures or zombification"));
            case "can_luna" -> tooltip.add(Component.literal("Contains actual selenium and star metal. Tastes like night."));
            case "can_bepis" -> tooltip.add(Component.literal("beppp"));
            case "can_breen" -> {
                tooltip.add(Component.literal("Don't drink the water. They put something in it, to make you forget."));
                tooltip.add(Component.literal("I don't even know how I got here."));
            }
            case "chocolate_milk" -> {
                tooltip.add(Component.literal("Regular chocolate milk. Safe to drink."));
                tooltip.add(Component.literal("Totally not made from nitroglycerine."));
            }
            case "bottle_nuka" -> tooltip.add(Component.literal("Contains about 210 kcal and 1500 mSv."));
            case "bottle_cherry" -> tooltip.add(Component.literal("Now with severe radiation poisoning in every seventh bottle!"));
            case "bottle_quantum" -> tooltip.add(Component.literal("Comes with a colorful mix of over 70 isotopes!"));
            case "bottle2_korl" -> tooltip.add(Component.literal("Contains actual orange juice!"));
            case "bottle2_fritz" -> tooltip.add(Component.literal("moremore caffeine"));
            case "bottle_sparkle" -> tooltip.add(Component.literal("The most delicious beverage in the wasteland!"));
            case "bottle_rad" -> tooltip.add(Component.literal("Tastes like radish and radiation."));
            default -> {
            }
        }
        if (BOTTLES.contains(id)) {
            tooltip.add(Component.literal("[Requires bottle opener]").withStyle(ChatFormatting.GRAY));
        }
    }

    private void applyLegacyEffects(Level level, Player player) {
        switch (id) {
            case "can_smart", "can_overcharge" -> effects(player, 30 * 20,
                    effect(MobEffects.MOVEMENT_SPEED, 1), effect(MobEffects.DAMAGE_RESISTANCE, 2), effect(MobEffects.DAMAGE_BOOST, 0));
            case "can_creature" -> effects(player, 30 * 20,
                    effect(MobEffects.MOVEMENT_SPEED, 0), effect(MobEffects.DAMAGE_RESISTANCE, 2), effect(MobEffects.REGENERATION, 1));
            case "can_redbomb" -> effects(player, 30 * 20,
                    effect(MobEffects.MOVEMENT_SPEED, 0), effect(MobEffects.ABSORPTION, 2), effect(MobEffects.JUMP, 1));
            case "can_mrsugar" -> effects(player, 30 * 20,
                    effect(MobEffects.MOVEMENT_SPEED, 0), effect(MobEffects.DIG_SPEED, 1), effect(MobEffects.JUMP, 2));
            case "can_luna" -> effects(player, 30 * 20,
                    effect(MobEffects.MOVEMENT_SPEED, 1), effect(MobEffects.DAMAGE_RESISTANCE, 2), effect(MobEffects.DAMAGE_BOOST, 1), effect(MobEffects.REGENERATION, 2));
            case "can_bepis" -> effects(player, 30 * 20,
                    effect(MobEffects.MOVEMENT_SPEED, 3), effect(MobEffects.DAMAGE_RESISTANCE, 3));
            case "can_breen" -> player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 30 * 20, 0));
            case "can_mug" -> effects(player, 3 * 60 * 20,
                    effect(MobEffects.DAMAGE_RESISTANCE, 2), effect(MobEffects.REGENERATION, 2, 60 * 20));
            case "chocolate_milk" -> level.explode(player, player.getX(), player.getY(), player.getZ(), 50.0F, true, Level.ExplosionInteraction.TNT);
            case "bottle_nuka" -> {
                player.heal(4.0F);
                effects(player, 30 * 20, effect(MobEffects.MOVEMENT_SPEED, 1), effect(MobEffects.DIG_SPEED, 1));
                radiation(player, 5.0F);
            }
            case "bottle_cherry" -> {
                player.heal(6.0F);
                effects(player, 30 * 20, effect(MobEffects.MOVEMENT_SPEED, 0), effect(MobEffects.JUMP, 2));
                radiation(player, 5.0F);
            }
            case "bottle_quantum" -> {
                player.heal(10.0F);
                effects(player, 30 * 20, effect(MobEffects.MOVEMENT_SPEED, 1), effect(MobEffects.DAMAGE_RESISTANCE, 2), effect(MobEffects.DAMAGE_BOOST, 1));
                radiation(player, 15.0F);
            }
            case "bottle2_korl" -> {
                player.heal(6.0F);
                effects(player, 30 * 20, effect(MobEffects.MOVEMENT_SPEED, 1), effect(MobEffects.DIG_SPEED, 2), effect(MobEffects.DAMAGE_BOOST, 2));
            }
            case "bottle2_fritz" -> {
                player.heal(6.0F);
                effects(player, 30 * 20, effect(MobEffects.MOVEMENT_SPEED, 1), effect(MobEffects.DAMAGE_RESISTANCE, 2), effect(MobEffects.JUMP, 2));
            }
            case "bottle_sparkle" -> {
                player.heal(10.0F);
                effects(player, 120 * 20, effect(MobEffects.MOVEMENT_SPEED, 1), effect(MobEffects.DAMAGE_RESISTANCE, 2), effect(MobEffects.DAMAGE_BOOST, 2), effect(MobEffects.DIG_SPEED, 1));
                radiation(player, 5.0F);
            }
            case "bottle_rad" -> {
                player.heal(10.0F);
                effects(player, 120 * 20, effect(MobEffects.MOVEMENT_SPEED, 1), effect(MobEffects.DAMAGE_RESISTANCE, 2), effect(MobEffects.FIRE_RESISTANCE, 0), effect(MobEffects.DAMAGE_BOOST, 4), effect(MobEffects.DIG_SPEED, 1));
                radiation(player, 15.0F);
            }
            case "coffee", "coffee_radium" -> {
                player.heal(10.0F);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60 * 20, 2));
                if (id.equals("coffee_radium")) {
                    radiation(player, 500.0F);
                }
            }
            default -> {
            }
        }
    }

    private static MobEffectInstance effect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
        return new MobEffectInstance(effect, 30 * 20, amplifier);
    }

    private static MobEffectInstance effect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier, int duration) {
        return new MobEffectInstance(effect, duration, amplifier);
    }

    private static void effects(Player player, int duration, MobEffectInstance... effects) {
        for (MobEffectInstance effect : effects) {
            player.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration() == 30 * 20 ? duration : effect.getDuration(), effect.getAmplifier()));
        }
    }

    private static void radiation(Player player, float amount) {
        HbmLivingRadiation radiation = HbmLivingRadiation.get(player);
        radiation.addRadiation(amount);
        HbmLivingRadiation.set(player, radiation);
    }

    private static boolean hasItem(Player player, String id) {
        Item expected = item(id);
        return !expected.equals(net.minecraft.world.item.Items.AIR) && player.getInventory().items.stream().anyMatch(stack -> stack.is(expected));
    }

    private static ItemStack itemStack(String id) {
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = item(id);
        return item.equals(net.minecraft.world.item.Items.AIR) ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ReinhardtsHBM.MOD_ID, id));
    }

    private static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
