package com.reinhardt.hbm.machine;

import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.item.LegacyVariantItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Exact 1.7.10 default AnnihilatorRecipes milestones, guarded by enable528. */
public final class AnnihilatorRecipes {
    private static final ResourceLocation STEEL = id("c:ingots/steel");
    private static final ResourceLocation PLASTIC = id("c:ingots/plastic");
    private static final ResourceLocation RUBBER = id("c:ingots/rubber");
    private static final ResourceLocation FERROURANIUM = id("c:ingots/ferrouranium");
    private static final ResourceLocation STRONTIUM = id("c:dusts/strontium");
    private static final ResourceLocation HARDPLASTIC = id("c:ingots/hardplastic");
    private static final ResourceLocation RESISTANT_ALLOY = id("c:ingots/resistant_alloy");

    private static final List<Milestone> MILESTONES = List.of(
            tag(STEEL, 256, "528.steel"),
            item("billet_silicon", 256, "528.chip"),
            item("nugget_bismuth", 128, "528.chip_bismoid"),
            item("pellet_charged", 1024, "528.chip_quantum"),
            item("billet_uranium", 256, "528.gascent"),
            tag(PLASTIC, 512, "528.plastic"),
            tag(RUBBER, 512, "528.rubber"),
            tag(FERROURANIUM, 1024, "528.ferrouranium"),
            tag(STRONTIUM, 256, "528.strontium"),
            tag(HARDPLASTIC, 1024, "528.hardplastic"),
            tag(RESISTANT_ALLOY, 1024, "528.tcalloy"),
            item("powder_chlorophyte", 1024, "528.chlorophyte"),
            variant("ammo_standard", "bmg50_fmj", 256, "528.bmg"),
            variant("ammo_arty", "ammo_arty", 128, "528.arty"),
            item("circuit_controller", 128, "528.controller")
    );

    private AnnihilatorRecipes() {
    }

    public static List<String> keysFor(ItemStack stack) {
        List<String> keys = new ArrayList<>();
        if (stack.isEmpty()) {
            return keys;
        }
        keys.add(itemKey(stack.getItem()));
        if (stack.getItem() instanceof LegacyVariantItem item) {
            keys.add(variantKey(stack.getItem(), item.variant(stack).id()));
        }
        stack.getTags().map(TagKey::location).map(AnnihilatorRecipes::tagKey).forEach(keys::add);
        return keys;
    }

    public static String monitorKey(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        if (stack.getItem() instanceof FluidIdentifierItem) {
            return fluidKey(FluidIdentifierItem.primary(stack).name());
        }
        if (stack.getItem() instanceof LegacyVariantItem item) {
            return variantKey(stack.getItem(), item.variant(stack).id());
        }
        return itemKey(stack.getItem());
    }

    public static String itemKey(Item item) {
        return "item:" + BuiltInRegistries.ITEM.getKey(item);
    }

    public static String variantKey(Item item, String variant) {
        return "variant:" + BuiltInRegistries.ITEM.getKey(item) + "#" + variant;
    }

    public static String fluidKey(String fluid) {
        return "fluid:" + fluid;
    }

    public static String tagKey(ResourceLocation tag) {
        return "tag:" + tag;
    }

    public static Payout highestPayout(ItemStack stack, AnnihilatorSavedData data, String pool, int amount, boolean alwaysPayout) {
        if (!HbmConfig.ENABLE_EXPENSIVE_MODE.get() || stack.isEmpty() || amount <= 0) {
            return null;
        }
        List<MatchedMilestone> matches = new ArrayList<>();
        for (Milestone milestone : MILESTONES) {
            if (milestone.matches(stack)) {
                String key = milestone.counterKey(stack);
                BigInteger current = data.count(pool, key);
                BigInteger previous = current.subtract(BigInteger.valueOf(amount));
                if ((alwaysPayout || previous.compareTo(milestone.amount()) < 0) && current.compareTo(milestone.amount()) >= 0) {
                    matches.add(new MatchedMilestone(milestone, milestone.priority()));
                }
            }
        }
        return matches.stream()
                .max(Comparator.comparingInt(MatchedMilestone::priority))
                .map(match -> new Payout(match.milestone().pool(), match.milestone().amount()))
                .orElse(null);
    }

    public static void incrementAll(ItemStack stack, AnnihilatorSavedData data, String pool, int amount) {
        for (String key : keysFor(stack)) {
            data.increment(pool, key, amount);
        }
    }

    public static void incrementFluid(String fluid, AnnihilatorSavedData data, String pool, int amount) {
        data.increment(pool, fluidKey(fluid), amount);
    }

    private static Milestone item(String itemId, long amount, String pool) {
        return new Milestone(Kind.ITEM, id("reinhardtshbm:" + itemId), "", BigInteger.valueOf(amount), pool);
    }

    private static Milestone tag(ResourceLocation tag, long amount, String pool) {
        return new Milestone(Kind.TAG, tag, "", BigInteger.valueOf(amount), pool);
    }

    private static Milestone variant(String itemId, String variant, long amount, String pool) {
        return new Milestone(Kind.VARIANT, id("reinhardtshbm:" + itemId), variant, BigInteger.valueOf(amount), pool);
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.parse(value);
    }

    public record Payout(String blueprintPool, BigInteger milestone) {
        public ItemStack stack() {
            return com.reinhardt.hbm.item.BlueprintItem.stackFor(blueprintPool);
        }
    }

    private record MatchedMilestone(Milestone milestone, int priority) {
    }

    private enum Kind { ITEM, VARIANT, TAG }

    private record Milestone(Kind kind, ResourceLocation target, String variant, BigInteger amount, String pool) {
        private boolean matches(ItemStack stack) {
            return switch (kind) {
                case ITEM -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(target);
                case VARIANT -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(target)
                        && stack.getItem() instanceof LegacyVariantItem item
                        && item.variant(stack).id().equals(variant);
                case TAG -> stack.is(TagKey.create(Registries.ITEM, target));
            };
        }

        private String counterKey(ItemStack stack) {
            return switch (kind) {
                case ITEM -> itemKey(stack.getItem());
                case VARIANT -> variantKey(stack.getItem(), variant);
                case TAG -> tagKey(target);
            };
        }

        private int priority() {
            return switch (kind) {
                case ITEM -> 1;
                case VARIANT -> 2;
                case TAG -> 3;
            };
        }
    }
}
