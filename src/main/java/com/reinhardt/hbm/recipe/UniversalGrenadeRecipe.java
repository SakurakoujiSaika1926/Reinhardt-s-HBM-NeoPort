package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.item.UniversalGrenadeItem;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Map;

/** Exact shapeless four-component grenade assembly rule from GrenadeCraftingHandler. */
public final class UniversalGrenadeRecipe extends CustomRecipe {
    public UniversalGrenadeRecipe() {
        super(CraftingBookCategory.MISC);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return state(input).valid();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        State state = state(input);
        return state.valid()
                ? UniversalGrenadeItem.make(state.shell(), state.filling(), state.fuze(), state.extra())
                : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return UniversalGrenadeItem.make(
                UniversalGrenadeItem.Shell.FRAG,
                UniversalGrenadeItem.Filling.HE,
                UniversalGrenadeItem.Fuze.S3,
                null
        );
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(ingredient(HbmItems.GRENADE_SHELL_ITEMS));
        ingredients.add(ingredient(HbmItems.GRENADE_FILLING_ITEMS));
        ingredients.add(ingredient(HbmItems.GRENADE_FUZE_ITEMS));
        ingredients.add(ingredient(HbmItems.GRENADE_EXTRA_ITEMS));
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.UNIVERSAL_GRENADE_SERIALIZER.get();
    }

    private static State state(CraftingInput input) {
        UniversalGrenadeItem.Shell shell = null;
        UniversalGrenadeItem.Filling filling = null;
        UniversalGrenadeItem.Fuze fuze = null;
        UniversalGrenadeItem.Extra extra = null;
        int count = 0;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            count++;
            String variant;
            if ((variant = variant(HbmItems.GRENADE_SHELL_ITEMS, stack)) != null && shell == null) {
                shell = UniversalGrenadeItem.Shell.byId(variant);
            } else if ((variant = variant(HbmItems.GRENADE_FILLING_ITEMS, stack)) != null && filling == null) {
                filling = UniversalGrenadeItem.Filling.byId(variant);
            } else if ((variant = variant(HbmItems.GRENADE_FUZE_ITEMS, stack)) != null && fuze == null) {
                fuze = UniversalGrenadeItem.Fuze.byId(variant);
            } else if ((variant = variant(HbmItems.GRENADE_EXTRA_ITEMS, stack)) != null && extra == null) {
                extra = UniversalGrenadeItem.Extra.byId(variant);
            } else {
                return State.INVALID;
            }
        }
        if (count < 3 || count > 4 || shell == null || filling == null || fuze == null || !filling.supports(shell)) {
            return State.INVALID;
        }
        return new State(shell, filling, fuze, extra);
    }

    private static Ingredient ingredient(Map<String, DeferredItem<Item>> items) {
        return Ingredient.of(items.values().stream().map(DeferredItem::get).toArray(Item[]::new));
    }

    private static String variant(Map<String, DeferredItem<Item>> items, ItemStack stack) {
        for (Map.Entry<String, DeferredItem<Item>> entry : items.entrySet()) {
            if (stack.is(entry.getValue().get())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private record State(
            UniversalGrenadeItem.Shell shell,
            UniversalGrenadeItem.Filling filling,
            UniversalGrenadeItem.Fuze fuze,
            UniversalGrenadeItem.Extra extra
    ) {
        private static final State INVALID = new State(null, null, null, null);
        private boolean valid() { return shell != null && filling != null && fuze != null; }
    }

    public static final class Serializer implements RecipeSerializer<UniversalGrenadeRecipe> {
        private static final MapCodec<UniversalGrenadeRecipe> CODEC = MapCodec.unit(UniversalGrenadeRecipe::new);
        private static final StreamCodec<RegistryFriendlyByteBuf, UniversalGrenadeRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override public UniversalGrenadeRecipe decode(RegistryFriendlyByteBuf buffer) { return new UniversalGrenadeRecipe(); }
            @Override public void encode(RegistryFriendlyByteBuf buffer, UniversalGrenadeRecipe recipe) { }
        };

        @Override public MapCodec<UniversalGrenadeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, UniversalGrenadeRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
