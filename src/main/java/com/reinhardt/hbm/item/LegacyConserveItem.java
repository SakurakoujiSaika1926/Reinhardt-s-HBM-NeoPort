package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyVortexEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * All 27 metadata states of 1.7.10 ItemConserve.  The variant is persisted in
 * custom data and mirrored to CustomModelData so recipes and renderers see the
 * same state.
 */
public final class LegacyConserveItem extends Item {
    private static final String VARIANT_TAG = "conserve";

    public enum Variant {
        BEEF(8, 0.75F), TUNA(4, 0.75F), MYSTERY(6, 0.5F), PASHTET(4, 0.5F),
        CHEESE(3, 1.0F), SLIME(15, 5.0F), MILK(5, 0.25F), ASS(6, 0.75F),
        PIZZA(8, 75.0F), TUBE(2, 0.25F), TOMATO(4, 0.5F), ASBESTOS(7, 1.0F),
        BHOLE(10, 1.0F), HOTDOGS(5, 0.75F), LEFTOVERS(1, 0.1F), YOGURT(3, 0.5F),
        STEW(5, 0.5F), CHINESE(6, 0.1F), OIL(3, 1.0F), FIST(6, 0.75F),
        SPAM(8, 1.0F), FRIED(10, 0.75F), NAPALM(6, 1.0F), DIESEL(6, 1.0F),
        KEROSENE(6, 1.0F), RECURSION(1, 1.0F), BARK(2, 1.0F);

        private final int nutrition;
        private final float saturation;

        Variant(int nutrition, float saturation) {
            this.nutrition = nutrition;
            this.saturation = saturation;
        }

        private String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public LegacyConserveItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm.canned_" + variant(stack).id());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.canEat(false)) {
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
            Variant variant = variant(stack);
            player.getFoodData().eat(variant.nutrition, variant.saturation);
            give(player, keyStack());
            switch (variant) {
                case BHOLE -> LegacyVortexEntity.spawn(level, player.position(), 0.5F, 0.01F, true);
                case RECURSION -> {
                    if (level.random.nextInt(10) > 0) {
                        give(player, stackFor(this, Variant.RECURSION));
                    }
                }
                case FIST -> player.hurt(player.damageSources().magic(), 2.0F);
                default -> {
                }
            }
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = "item.reinhardtshbm.canned_" + variant(stack).id() + ".desc";
        String text = Component.translatable(key).getString();
        if (!text.equals(key)) {
            for (String line : text.split("\\\\$")) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Variant variant : Variant.values()) {
            output.accept(stackFor(this, variant));
        }
    }

    public static ItemStack stackFor(Item item, Variant variant) {
        ItemStack stack = new ItemStack(item);
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        net.minecraft.nbt.CompoundTag tag = data.copyTag();
        tag.putString(VARIANT_TAG, variant.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(variant.ordinal()));
        return stack;
    }

    private Variant variant(ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(VARIANT_TAG)) {
            try {
                return Variant.valueOf(tag.getString(VARIANT_TAG).toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Fall through to the model component for stacks made by recipes.
            }
        }
        CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        int index = model == null ? 0 : model.value();
        return Variant.values()[Math.clamp(index, 0, Variant.values().length - 1)];
    }

    private static ItemStack keyStack() {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ReinhardtsHBM.MOD_ID, "can_key"));
        return item == net.minecraft.world.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
