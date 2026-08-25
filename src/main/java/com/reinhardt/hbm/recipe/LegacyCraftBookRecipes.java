package com.reinhardt.hbm.recipe;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyUnstableItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Exact ordered recipe tables from MagicRecipes and LemegetonRecipes. */
public final class LegacyCraftBookRecipes {
    private LegacyCraftBookRecipes() {
    }

    public static ItemStack magicResult(Container input) {
        List<ItemStack> stacks = new ArrayList<>(4);
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }

        if (matches(stacks, itemVariant("ingot_u238m2", 1), itemVariant("ingot_u238m2", 2), itemVariant("ingot_u238m2", 3))) {
            return item("ingot_u238m2");
        }
        if (matches(stacks, exact(Items.ENDER_PEARL), exact(Items.BLAZE_ROD), expectedItem("nugget_euphemium"))) {
            return item("rod_of_discord");
        }
        if (matches(stacks, expectedTag("ingots/steel"), expectedItem("egg_balefire_shard"))) {
            return item("balefire_and_steel");
        }
        if (matches(stacks, exact(Items.IRON_SHOVEL), exact(Items.BONE), expectedItem("ingot_starmetal"), expectedItem("ducttape"))) {
            return item("mysteryshovel");
        }
        if (matches(stacks, expectedItem("pellet_charged"), expectedItem("pellet_charged"), expectedItem("ingot_dineutronium"), expectedItem("ingot_dineutronium"))) {
            return item("ingot_electronium");
        }
        if (matches(stacks, expectedItem("gravel_diamond"), expectedItem("gravel_diamond"), expectedItem("gravel_diamond"), expectedItem("lead_gavel"))) {
            return item("diamond_gavel");
        }
        if (matches(stacks, expectedItem("shimmer_handle"), expectedItem("powder_dineutronium"), expectedItem("blades_desh"), expectedItem("diamond_gavel"))) {
            return item("mese_gavel");
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack lemegetonResult(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return firstTagMatch(input,
                "ingots/iron", "ingot_steel",
                "ingots/steel", "ingot_dura_steel",
                "ingots/dura_steel", "ingot_tcalloy",
                "ingots/tcalloy", "ingot_combine_steel",
                "ingots/combine_steel", "ingot_dineutronium",
                "ingots/titanium", "ingot_saturnite",
                "ingots/saturnite", "ingot_starmetal",
                "ingots/copper", "ingot_red_copper",
                "ingots/mining_grade", "ingot_desh",
                "ingots/desh", "ingot_bscco",
                "ingots/lead", "minecraft:gold_ingot",
                "ingots/gold", "ingot_bismuth",
                "ingots/bismuth", "ingot_osmiridium",
                "ingots/th232", "ingot_uranium",
                "ingots/uranium", "ingot_u238",
                "ingots/u238", "ingot_u235",
                "ingots/u235", "ingot_plutonium",
                "ingots/plutonium", "ingot_pu238",
                "ingots/pu238", "ingot_pu239",
                "ingots/pu239", "ingot_pu240",
                "ingots/pu240", "ingot_pu241",
                "ingots/pu241", "ingot_am241",
                "ingots/am241", "ingot_am242",
                "ingots/ra226", "ingot_polonium",
                "ingots/po210", "ingot_technetium",
                "ingots/polymer", "ingot_pc",
                "ingots/bakelite", "ingot_pvc",
                "ingots/latex", "ingot_rubber",
                "gems/coal", "ingot_graphite",
                "ingots/graphite", "minecraft:diamond",
                "gems/diamond", "ingot_cft",
                "dusts/fluorite", "gem_sodalite",
                "gems/sodalite", "gem_volcanic",
                "gems/volcanic", "gem_rad",
                "sands", "ingot_fiberglass",
                "ingots/fiberglass", "ingot_asbestos"
        );
    }

    private static ItemStack firstTagMatch(ItemStack input, String... tagOutputPairs) {
        for (int index = 0; index < tagOutputPairs.length; index += 2) {
            if (input.is(tag(tagOutputPairs[index]))) {
                return item(tagOutputPairs[index + 1]);
            }
        }
        if (input.is(item("gem_rad").getItem())) {
            return item("gem_alexandrite");
        }
        return ItemStack.EMPTY;
    }

    private static boolean matches(List<ItemStack> actual, Expected... expected) {
        if (actual.size() != expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if (!expected[index].matches(actual.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static Expected expectedItem(String id) {
        return stack -> stack.is(item(id).getItem());
    }

    private static Expected exact(Item item) {
        return stack -> stack.is(item);
    }

    private static Expected itemVariant(String id, int variant) {
        return stack -> stack.is(item(id).getItem()) && LegacyUnstableItem.variant(stack) == variant;
    }

    private static Expected expectedTag(String path) {
        return stack -> stack.is(tag(path));
    }

    private static ItemStack item(String id) {
        ResourceLocation location = id.indexOf(':') >= 0 ? ResourceLocation.parse(id) : ReinhardtsHBM.id(id);
        return new ItemStack(BuiltInRegistries.ITEM.get(location));
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    @FunctionalInterface
    private interface Expected {
        boolean matches(ItemStack stack);
    }
}
