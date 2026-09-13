package com.reinhardt.hbm.util;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyVariantItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Direct value table from HBM 1.7.10 {@code FuelHandler#getBurnTime}.
 * <p>
 * This intentionally contains only HBM's legacy furnace fuel overrides. Callers
 * that also need vanilla or other-mod fuels should fall back to
 * {@link ItemStack#getBurnTime(net.minecraft.world.item.crafting.RecipeType)}
 * when this returns {@code 0}.
 */
public final class LegacyFurnaceFuels {
    private static final int SINGLE = 200;

    private LegacyFurnaceFuels() {
    }

    public static int burnTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || !ReinhardtsHBM.MOD_ID.equals(id.getNamespace())) {
            return 0;
        }

        return switch (id.getPath()) {
            case "solid_fuel" -> SINGLE * 16;
            case "solid_fuel_presto" -> SINGLE * 40;
            case "solid_fuel_presto_triplet" -> SINGLE * 200;
            case "solid_fuel_bf" -> SINGLE * 160;
            case "solid_fuel_presto_bf" -> SINGLE * 400;
            case "solid_fuel_presto_triplet_bf" -> SINGLE * 2_000;
            case "rocket_fuel" -> SINGLE * 32;
            case "biomass", "block_scrap" -> SINGLE * 2;
            case "biomass_compressed" -> SINGLE * 4;
            case "powder_coal" -> SINGLE * 8;
            case "scrap" -> SINGLE / 4;
            case "dust" -> SINGLE / 8;
            case "powder_fire", "crystal_coal" -> 6_400;
            case "lignite", "powder_lignite" -> 1_200;
            case "coke" -> SINGLE * 16;
            case "block_coke" -> SINGLE * 160;
            case "book_guide" -> SINGLE;
            case "coal_infernal" -> 4_800;
            case "powder_sawdust" -> SINGLE / 2;
            case "briquette" -> briquetteBurnTime(stack);
            case "powder_ash" -> ashBurnTime(stack);
            default -> 0;
        };
    }

    private static int briquetteBurnTime(ItemStack stack) {
        return switch (variantId(stack)) {
            case "coal" -> SINGLE * 10;
            case "lignite" -> SINGLE * 8;
            case "wood" -> SINGLE * 2;
            default -> 0;
        };
    }

    private static int ashBurnTime(ItemStack stack) {
        return switch (variantId(stack)) {
            case "wood", "misc", "soot" -> SINGLE / 2;
            case "coal", "fly" -> SINGLE;
            default -> 0;
        };
    }

    private static String variantId(ItemStack stack) {
        return stack.getItem() instanceof LegacyVariantItem item ? item.variant(stack).id() : "";
    }
}
