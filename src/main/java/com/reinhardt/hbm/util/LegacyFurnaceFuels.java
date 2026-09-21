package com.reinhardt.hbm.util;

import com.reinhardt.hbm.ReinhardtsHBM;
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

        String path = id.getPath();
        if (path.startsWith("coke_")) {
            return SINGLE * 16;
        }
        if (path.startsWith("briquette_")) {
            return briquetteBurnTime(path);
        }
        if (path.startsWith("powder_ash_")) {
            return ashBurnTime(path);
        }

        return switch (path) {
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
            case "block_coke" -> SINGLE * 160;
            case "book_guide" -> SINGLE;
            case "coal_infernal" -> 4_800;
            case "powder_sawdust" -> SINGLE / 2;
            default -> 0;
        };
    }

    private static int briquetteBurnTime(String path) {
        return switch (path) {
            case "briquette_coal" -> SINGLE * 10;
            case "briquette_lignite" -> SINGLE * 8;
            case "briquette_wood" -> SINGLE * 2;
            default -> 0;
        };
    }

    private static int ashBurnTime(String path) {
        return switch (path) {
            case "powder_ash_wood", "powder_ash_misc", "powder_ash_soot" -> SINGLE / 2;
            case "powder_ash_coal", "powder_ash_fly" -> SINGLE;
            default -> 0;
        };
    }
}
