package com.reinhardt.hbm.item;

import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;

/** Registered 1.7.10 food classes that have real effects beyond vanilla food. */
public final class LegacySpecialFoodItem extends Item {
    public enum Kind {
        BOMB_WAFFLE("bomb_waffle", 20, 0.6F, false),
        SCHNITZEL_VEGAN("schnitzel_vegan", 0, 0.6F, false),
        COTTON_CANDY("cotton_candy", 5, 0.6F, true),
        APPLE_LEAD("apple_lead", 5, 0.0F, true),
        APPLE_SCHRABIDIUM("apple_schrabidium", 20, 100.0F, true),
        TEM_FLAKES("tem_flakes", 0, 0.0F, true),
        PANCAKE("pancake", 20, 20.0F, true),
        MUCHO_MANGO("mucho_mango", 10, 0.6F, true),
        APPLE_EUPHEMIUM("apple_euphemium", 20, 100.0F, true);

        private final String id;
        private final int nutrition;
        private final float saturation;
        private final boolean alwaysEdible;

        Kind(String id, int nutrition, float saturation, boolean alwaysEdible) {
            this.id = id;
            this.nutrition = nutrition;
            this.saturation = saturation;
            this.alwaysEdible = alwaysEdible;
        }
    }

    private final Kind kind;

    private LegacySpecialFoodItem(Kind kind) {
        super(properties(kind));
        this.kind = kind;
    }

    public static LegacySpecialFoodItem fromLegacyId(String id) {
        for (Kind kind : Kind.values()) {
            if (kind.id.equals(id)) {
                return new LegacySpecialFoodItem(kind);
            }
        }
        throw new IllegalArgumentException("Unknown legacy special food: " + id);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind == Kind.PANCAKE && !canEatPancake(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal("Your teeth are too soft to eat this.").withStyle(ChatFormatting.YELLOW), false);
            }
            return InteractionResultHolder.fail(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        ItemStack result = super.finishUsingItem(stack, level, living);
        if (!level.isClientSide && living instanceof Player player) {
            applyEffect(player, stack);
        }
        return result;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return kind == Kind.MUCHO_MANGO ? 200 : super.getUseDuration(stack, entity);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.MUCHO_MANGO ? UseAnim.DRINK : super.getUseAnimation(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return kind == Kind.APPLE_EUPHEMIUM || (kind == Kind.APPLE_SCHRABIDIUM && variant(stack) == 2);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        switch (kind) {
            case TEM_FLAKES -> tooltip.add(Component.literal(switch (variant(stack)) {
                case 0 -> "Heals 2HP DISCOUNT FOOD OF TEM!!!";
                case 1 -> "Heals 2HP food of tem";
                default -> "Heals food of tem (expensiv)";
            }));
            case PANCAKE -> {
                tooltip.add(Component.literal("Can be eaten to recharge lunar cybernetic armor"));
                tooltip.add(Component.literal("Not for people with weak molars"));
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Half burnt and smells horrible"));
            }
            case MUCHO_MANGO -> tooltip.add(Component.literal("The Comically Large Can"));
            default -> {
            }
        }
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        int count = switch (kind) {
            case APPLE_LEAD, APPLE_SCHRABIDIUM, TEM_FLAKES -> 3;
            default -> 1;
        };
        for (int variant = 0; variant < count; variant++) {
            output.accept(stackFor(this, variant));
        }
    }

    public static ItemStack stackFor(Item item, int variant) {
        ItemStack stack = new ItemStack(item);
        if (variant > 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(variant));
        }
        if (item instanceof LegacySpecialFoodItem food
                && (food.kind == Kind.APPLE_LEAD || food.kind == Kind.APPLE_SCHRABIDIUM)) {
            stack.set(DataComponents.RARITY, switch (variant) {
                case 0 -> Rarity.UNCOMMON;
                case 1 -> Rarity.RARE;
                default -> Rarity.EPIC;
            });
        }
        return stack;
    }

    private void applyEffect(Player player, ItemStack stack) {
        switch (kind) {
            case BOMB_WAFFLE -> NukeExplosionManager.scheduleLegacyNuke(
                    (net.minecraft.server.level.ServerLevel) player.level(), player.getX(), player.getY() + 0.5D, player.getZ(), 20
            );
            case SCHNITZEL_VEGAN -> {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 10 * 20));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 30 * 20));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 3 * 60 * 20, 4));
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 3 * 20));
                player.igniteForSeconds(100.0F);
                player.setDeltaMovement(player.getDeltaMovement().add(0.0D, 2.0D, 0.0D));
                player.hurtMarked = true;
            }
            case COTTON_CANDY -> {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 15 * 20));
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 5 * 20));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 4));
            }
            case APPLE_LEAD -> {
                switch (variant(stack)) {
                    case 0 -> player.addEffect(new MobEffectInstance(HbmMobEffects.LEAD_POISONING, 15 * 20, 2));
                    case 1 -> player.addEffect(new MobEffectInstance(HbmMobEffects.LEAD_POISONING, 60 * 20, 4));
                    default -> player.hurt(player.damageSources().source(HbmDamageTypes.LEAD), 500.0F);
                }
            }
            case APPLE_SCHRABIDIUM -> applySchrabidiumApple(player, variant(stack));
            case TEM_FLAKES -> player.heal(2.0F);
            case PANCAKE -> chargeArmor(player);
            case MUCHO_MANGO -> player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200));
            case APPLE_EUPHEMIUM -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 120));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE));
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, Integer.MAX_VALUE, 120));
            }
        }
    }

    private static void applySchrabidiumApple(Player player, int variant) {
        if (variant == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 4));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000));
        } else if (variant == 1) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 4));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 4));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 4));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 1200, 2));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 2));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 1200, 4));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200, 9));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 4));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1200, 9));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Integer.MAX_VALUE, 4));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Integer.MAX_VALUE));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 9));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, Integer.MAX_VALUE, 4));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Integer.MAX_VALUE, 3));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, Integer.MAX_VALUE, 4));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, Integer.MAX_VALUE, 24));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 14));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, Integer.MAX_VALUE, 99));
        }
    }

    private static boolean canEatPancake(Player player) {
        return ArmorFSBItem.hasFSBArmor(player) && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(HbmItems.BJ_HELMET.get());
    }

    private static void chargeArmor(Player player) {
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.getItem() instanceof HbmChargeableItem battery) {
                battery.hbmSetCharge(armor, battery.hbmCapacity(armor));
            }
        }
    }

    private static int variant(ItemStack stack) {
        CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return Math.max(0, model == null ? 0 : model.value());
    }

    private static Properties properties(Kind kind) {
        net.minecraft.world.food.FoodProperties.Builder food = new net.minecraft.world.food.FoodProperties.Builder()
                .nutrition(kind.nutrition).saturationModifier(kind.saturation);
        if (kind.alwaysEdible) {
            food.alwaysEdible();
        }
        Properties properties = new Properties().food(food.build());
        return kind == Kind.APPLE_EUPHEMIUM ? properties.stacksTo(1).rarity(Rarity.EPIC) : properties;
    }
}
