package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.item.BlueprintFolderItem;
import com.reinhardt.hbm.item.FoundryShapeItem;
import com.reinhardt.hbm.item.LegacyConserveItem;
import com.reinhardt.hbm.item.LegacyVariantItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The 1.7.10 pedestal recipe table and its exact nine-position matcher.
 *
 * The old table contains a number of firearms which are intentionally retired
 * in 1.21.1. Those entries remain represented here for auditability, but an
 * entry is only executable when its output and every exact item input are
 * registered in the current game.
 */
public final class LegacyPedestalRecipes {
    private static final int SLOT_COUNT = 9;
    private static final List<Recipe> RECIPES = createRecipes();

    private LegacyPedestalRecipes() {
    }

    public static Optional<Recipe> find(Level level, BlockPos center) {
        if (!level.hasNeighborSignal(center)) {
            return Optional.empty();
        }

        ItemStack[] stacks = new ItemStack[SLOT_COUNT];
        int[][] offsets = {
                {-2, -2}, {0, -3}, {2, -2},
                {-3, 0}, {0, 0}, {3, 0},
                {-2, 2}, {0, 3}, {2, 2}
        };
        for (int i = 0; i < offsets.length; i++) {
            if (level.getBlockEntity(center.offset(offsets[i][0], 0, offsets[i][1]))
                    instanceof LegacyDisplayStandBlockEntity pedestal) {
                stacks[i] = pedestal.displayedItem();
            } else {
                stacks[i] = ItemStack.EMPTY;
            }
        }

        for (Recipe recipe : RECIPES) {
            if (!recipe.available() || !recipe.condition().matches(level)) {
                continue;
            }
            boolean matches = true;
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (!recipe.input()[i].matches(stacks[i])) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static void consume(Level level, BlockPos center, Recipe recipe) {
        int[][] offsets = {
                {-2, -2}, {0, -3}, {2, -2},
                {-3, 0}, {0, 0}, {3, 0},
                {-2, 2}, {0, 3}, {2, 2}
        };
        for (int i = 0; i < offsets.length; i++) {
            if (i == 4 || recipe.input()[i].empty()) {
                continue;
            }
            if (level.getBlockEntity(center.offset(offsets[i][0], 0, offsets[i][1]))
                    instanceof LegacyDisplayStandBlockEntity pedestal) {
                pedestal.clearDisplayedItem();
            }
        }
        if (level.getBlockEntity(center) instanceof LegacyDisplayStandBlockEntity pedestal) {
            pedestal.setDisplayedItem(recipe.result());
        }
    }

    private static List<Recipe> createRecipes() {
        List<Recipe> recipes = new ArrayList<>();

        // Exact 1.7.10 PedestalRecipes.registerDefaults() entries.
        recipes.add(recipe(item("gun_light_revolver_dani", 1),
                empty(), tag("plates/lead", 1), empty(),
                tag("plates/gold", 1), item("gun_light_revolver", 1), tag("plates/gold", 1),
                empty(), tag("plates/lead", 1), empty(), Condition.NONE, 0));
        recipes.add(recipe(item("gun_maresleg_broken", 1),
                item("barbed_wire", 1), tag("plates/weaponsteel", 1), item("barbed_wire", 1),
                tag("plates/weaponsteel", 1), item("gun_maresleg", 1), tag("plates/weaponsteel", 1),
                item("barbed_wire", 1), tag("plates/weaponsteel", 1), item("barbed_wire", 1), Condition.NONE, 0));
        recipes.add(recipe(item("gun_heavy_revolver_lilmac", 1),
                empty(), variant("weapon_mod_special", "scope", 1), empty(),
                item("powder_magic", 1), item("gun_heavy_revolver", 1), tag("plates/weaponsteel", 1),
                empty(), shape("part_grip", FoundryShape.GRIP, "bone", 1), item("minecraft:apple", 3), Condition.NONE, 0));
        recipes.add(recipe(item("gun_heavy_revolver_protege", 1),
                item("chain", 16), tag("crystals/cinnabar", 1), item("chain", 16),
                item("scrap_nuclear", 1), item("gun_heavy_revolver", 1), item("scrap_nuclear", 1),
                item("chain", 16), tag("crystals/cinnabar", 1), item("chain", 16), Condition.NONE, 0));
        recipes.add(recipe(item("gun_amat_subtlety", 1),
                tag("ingots/starmetal", 1), shape("plate_cast", FoundryShape.CAST_PLATE, "aluminium", 1), tag("ingots/starmetal", 1),
                shape("plate_cast", FoundryShape.CAST_PLATE, "aluminium", 1), item("gun_amat", 1), shape("plate_cast", FoundryShape.CAST_PLATE, "aluminium", 1),
                tag("ingots/starmetal", 1), shape("plate_cast", FoundryShape.CAST_PLATE, "aluminium", 1), tag("ingots/starmetal", 1), Condition.NONE, 0));
        recipes.add(recipe(item("gun_amat_penance", 1),
                tag("ingots/starmetal", 1), shape("plate_cast", FoundryShape.CAST_PLATE, "dura_steel", 1), tag("ingots/starmetal", 1),
                variant("weapon_mod_special", "silencer", 1), item("gun_amat", 1), variant("weapon_mod_special", "furniture_black", 1),
                tag("ingots/starmetal", 1), shape("plate_cast", FoundryShape.CAST_PLATE, "dura_steel", 1), tag("ingots/starmetal", 1), Condition.NONE, 0));
        recipes.add(recipe(item("gun_flamer_daybreaker", 1),
                shape("plate_cast", FoundryShape.CAST_PLATE, "gold", 1), conserve("slime", 1), shape("plate_cast", FoundryShape.CAST_PLATE, "gold", 1),
                tag("ingots/phosphorus", 1), item("gun_flamer", 1), tag("ingots/phosphorus", 1),
                shape("plate_cast", FoundryShape.CAST_PLATE, "gold", 1), item("stick_dynamite", 1), shape("plate_cast", FoundryShape.CAST_PLATE, "gold", 1), Condition.SUN, 0));
        recipes.add(recipe(item("gun_autoshotgun_sexy", 1),
                item("bolt_spike", 16), item("wild_p", 1), item("bolt_spike", 16),
                item("card_qos", 1), item("gun_autoshotgun", 1), item("card_aos", 1),
                item("bolt_spike", 16), tag("ingots/starmetal", 1), item("bolt_spike", 16), Condition.NONE, 0));
        recipes.add(recipe(item("gun_minigun_lacunae", 1),
                empty(), item("powder_magic", 4), empty(),
                variant("item_secret", "selenium_steel", 4), item("gun_minigun", 1), variant("item_secret", "selenium_steel", 4),
                empty(), item("powder_magic", 4), empty(), Condition.FULL_MOON, 0));
        recipes.add(recipe(item("gun_laser_pistol_morning_glory", 1),
                empty(), item("morning_glory", 1), empty(),
                variant("item_secret", "selenium_steel", 2), item("gun_laser_pistol", 1), variant("item_secret", "selenium_steel", 2),
                empty(), tag("gems/emerald", 16), empty(), Condition.NONE, 0));
        recipes.add(recipe(item("gun_folly", 1),
                variant("item_secret", "folly", 4), variant("item_secret", "controller", 2), variant("item_secret", "folly", 4),
                tag("ingots/bscco", 16), tag("blocks/starmetal", 64), tag("ingots/bscco", 16),
                variant("item_secret", "folly", 4), variant("item_secret", "controller", 2), variant("item_secret", "folly", 4), Condition.FULL_MOON, 1));
        recipes.add(recipe(item("gun_aberrator", 1),
                empty(), variant("item_secret", "aberrator", 1), empty(),
                variant("item_secret", "aberrator", 1), shape("part_mechanism", FoundryShape.MECHANISM, "saturnite", 4), variant("item_secret", "aberrator", 1),
                empty(), variant("item_secret", "aberrator", 1), empty(), Condition.NONE, 1));
        recipes.add(recipe(item("gun_aberrator_eott", 1),
                variant("item_secret", "aberrator", 1), variant("item_secret", "aberrator", 1), variant("item_secret", "aberrator", 1),
                variant("item_secret", "aberrator", 1), shape("part_mechanism", FoundryShape.MECHANISM, "saturnite", 16), variant("item_secret", "aberrator", 1),
                variant("item_secret", "aberrator", 1), variant("item_secret", "aberrator", 1), variant("item_secret", "aberrator", 1), Condition.GOOD_KARMA, 1));
        recipes.add(recipe(variant("ammo_secret", "folly_sm", 1),
                tag("ingots/starmetal", 1), item("powder_magic", 1), tag("ingots/starmetal", 1),
                item("powder_magic", 1), variant("chunk_ore", "moonstone", 1), item("powder_magic", 1),
                tag("ingots/starmetal", 1), item("powder_magic", 1), tag("ingots/starmetal", 1), Condition.FULL_MOON, 1));
        recipes.add(recipe(variant("ammo_secret", "folly_nuke", 1),
                tag("ingots/starmetal", 1), item("powder_magic", 1), tag("ingots/starmetal", 1),
                item("powder_magic", 1), variant("ammo_standard", "nuke_high", 4), item("powder_magic", 1),
                tag("ingots/starmetal", 1), item("powder_magic", 1), tag("ingots/starmetal", 1), Condition.FULL_MOON, 1));
        recipes.add(recipe(variant("ammo_secret", "p35_800", 5),
                empty(), empty(), empty(), empty(), variant("item_secret", "aberrator", 1), empty(), empty(), empty(), empty(), Condition.NONE, 1));
        recipes.add(recipe(variant("ammo_secret", "p35_800_bl", 10),
                empty(), empty(), empty(), empty(), variant("item_secret", "aberrator", 3), empty(), empty(), empty(), empty(), Condition.NONE, 1));

        return List.copyOf(recipes);
    }

    private static Recipe recipe(Spec output, Spec a, Spec b, Spec c, Spec d, Spec e, Spec f, Spec g, Spec h, Spec i,
                                 Condition condition, int set) {
        return new Recipe(output, new Spec[]{a, b, c, d, e, f, g, h, i}, condition, set);
    }

    private static Spec empty() {
        return new Spec(null, null, null, null, 0);
    }

    private static Spec item(String id, int count) {
        return new Spec(id, null, null, null, count);
    }

    private static Spec variant(String id, String variant, int count) {
        return new Spec(id, variant, null, null, count);
    }

    private static Spec tag(String path, int count) {
        return new Spec(null, null, path, null, count);
    }

    private static Spec shape(String id, FoundryShape shape, String material, int count) {
        return new Spec(id, null, null, new ShapeSpec(shape, material), count);
    }

    private static Spec conserve(String variant, int count) {
        return new Spec("canned_conserve", variant, null, null, count);
    }

    public record Recipe(Spec output, Spec[] input, Condition condition, int set) {
        boolean available() {
            return output.available() && java.util.Arrays.stream(input).allMatch(Spec::available);
        }

        ItemStack result() {
            return output.stack();
        }
    }

    private record ShapeSpec(FoundryShape shape, String material) {
    }

    private record Spec(String itemId, String variant, String tagPath, ShapeSpec shape, int count) {
        boolean empty() {
            return count == 0;
        }

        boolean available() {
            if (empty() || tagPath != null) {
                return true;
            }
            ResourceLocation id = resolve(itemId);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                return false;
            }
            if (shape != null) {
                Item item = BuiltInRegistries.ITEM.get(id);
                return item instanceof FoundryShapeItem foundry
                        && foundry.shape() == shape.shape()
                        && FoundryMaterial.byName(shape.material()).isPresent();
            }
            return true;
        }

        boolean matches(ItemStack stack) {
            if (empty()) {
                return stack.isEmpty();
            }
            if (stack.isEmpty() || stack.getCount() != count) {
                return false;
            }
            if (tagPath != null) {
                return stack.is(TagKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath("c", tagPath)));
            }
            ResourceLocation id = resolve(itemId);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || !stack.is(BuiltInRegistries.ITEM.get(id))) {
                return false;
            }
            if (shape != null) {
                if (!(stack.getItem() instanceof FoundryShapeItem foundry)
                        || foundry.shape() != shape.shape()) {
                    return false;
                }
                return foundry.material(stack) != null
                        && foundry.material(stack).name().equals(shape.material());
            }
            if (variant == null) {
                return true;
            }
            return variantOf(stack).equals(variant);
        }

        ItemStack stack() {
            if (empty()) {
                return ItemStack.EMPTY;
            }
            ResourceLocation id = resolve(itemId);
            if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                return ItemStack.EMPTY;
            }
            Item item = BuiltInRegistries.ITEM.get(id);
            ItemStack stack;
            if (shape != null && item instanceof FoundryShapeItem) {
                stack = FoundryShapeItem.stackFor(item, FoundryMaterial.get(shape.material()), count);
            } else if (item instanceof LegacyVariantItem && variant != null) {
                stack = LegacyVariantItem.stackFor(item, variant);
                stack.setCount(count);
            } else if (item instanceof LegacyConserveItem && variant != null) {
                stack = LegacyConserveItem.stackFor(item, LegacyConserveItem.Variant.valueOf(variant.toUpperCase(java.util.Locale.ROOT)));
                stack.setCount(count);
            } else if (item instanceof BlueprintFolderItem && variant != null && variant.equals("secret")) {
                stack = BlueprintFolderItem.stackFor(BlueprintFolderItem.Variant.SECRET);
                stack.setCount(count);
            } else {
                stack = new ItemStack(item, count);
            }
            return stack;
        }

        private static String variantOf(ItemStack stack) {
            if (stack.getItem() instanceof LegacyVariantItem legacy) {
                return legacy.variant(stack).id();
            }
            if (stack.getItem() instanceof LegacyConserveItem) {
                return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString("conserve");
            }
            if (stack.getItem() instanceof BlueprintFolderItem) {
                return "secret";
            }
            return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getString("variant");
        }

        private static ResourceLocation resolve(String id) {
            if (id == null) {
                return null;
            }
            return id.indexOf(':') >= 0
                    ? ResourceLocation.tryParse(id)
                    : ResourceLocation.fromNamespaceAndPath("reinhardtshbm", id);
        }
    }

    enum Condition {
        NONE {
            @Override boolean matches(Level level) { return true; }
        },
        FULL_MOON {
            @Override boolean matches(Level level) {
                return isNightWindow(level) && level.getMoonPhase() == 0;
            }
        },
        NEW_MOON {
            @Override boolean matches(Level level) {
                return isNightWindow(level) && level.getMoonPhase() == 4;
            }
        },
        SUN {
            @Override boolean matches(Level level) {
                float angle = level.getTimeOfDay(0.0F);
                return angle <= 0.15F || angle >= 0.85F;
            }
        },
        GOOD_KARMA {
            @Override boolean matches(Level level) { return false; }
        },
        BAD_KARMA {
            @Override boolean matches(Level level) { return false; }
        };

        abstract boolean matches(Level level);

        private static boolean isNightWindow(Level level) {
            float angle = level.getTimeOfDay(0.0F);
            return angle >= 0.35F && angle <= 0.65F;
        }
    }
}
