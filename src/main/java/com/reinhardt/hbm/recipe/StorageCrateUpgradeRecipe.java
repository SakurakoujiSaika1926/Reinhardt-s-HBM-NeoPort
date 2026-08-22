package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class StorageCrateUpgradeRecipe extends CustomRecipe {
    private final Target target;

    public StorageCrateUpgradeRecipe(Target target) {
        super(CraftingBookCategory.MISC);
        this.target = target;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return this.target.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack source = findSteelCrate(input);
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(this.target.result().asItem());
        CustomData data = source.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            result.set(DataComponents.CUSTOM_DATA, data);
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(this.target.result().asItem());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.target.ingredients();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return switch (this.target) {
            case DESH -> HbmRecipeTypes.CRATE_DESH_UPGRADE_SERIALIZER.get();
            case TUNGSTEN -> HbmRecipeTypes.CRATE_TUNGSTEN_UPGRADE_SERIALIZER.get();
        };
    }

    private static ItemStack findSteelCrate(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(HbmBlocks.CRATE_STEEL.asItem())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public enum Target {
        DESH,
        TUNGSTEN;

        private boolean matches(CraftingInput input) {
            if (input.width() != 3 || input.height() != 3) {
                return false;
            }
            return switch (this) {
                case DESH -> matchesDesh(input);
                case TUNGSTEN -> matchesTungsten(input);
            };
        }

        private boolean matchesDesh(CraftingInput input) {
            return isEmpty(input, 0, 0)
                    && isDeshPlate(input, 1, 0)
                    && isEmpty(input, 2, 0)
                    && isDeshPlate(input, 0, 1)
                    && is(input, 1, 1, HbmBlocks.CRATE_STEEL.asItem())
                    && isDeshPlate(input, 2, 1)
                    && isEmpty(input, 0, 2)
                    && isDeshPlate(input, 1, 2)
                    && isEmpty(input, 2, 2);
        }

        private boolean matchesTungsten(CraftingInput input) {
            return isTungstenBlock(input, 0, 0)
                    && isCopperCastPlate(input, 1, 0)
                    && isTungstenBlock(input, 2, 0)
                    && isCopperCastPlate(input, 0, 1)
                    && is(input, 1, 1, HbmBlocks.CRATE_STEEL.asItem())
                    && isCopperCastPlate(input, 2, 1)
                    && isTungstenBlock(input, 0, 2)
                    && isCopperCastPlate(input, 1, 2)
                    && isTungstenBlock(input, 2, 2);
        }

        private NonNullList<Ingredient> ingredients() {
            NonNullList<Ingredient> ingredients = NonNullList.create();
            switch (this) {
                case DESH -> {
                    ingredients.add(Ingredient.of(HbmBlocks.CRATE_STEEL.asItem()));
                    ingredients.add(Ingredient.of(item("plate_desh")));
                }
                case TUNGSTEN -> {
                    ingredients.add(Ingredient.of(HbmBlocks.CRATE_STEEL.asItem()));
                    ingredients.add(Ingredient.of(item("block_tungsten")));
                    ingredients.add(Ingredient.of(com.reinhardt.hbm.registry.HbmItems.PLATE_CAST.get()));
                }
            }
            return ingredients;
        }

        private net.minecraft.world.level.ItemLike result() {
            return switch (this) {
                case DESH -> HbmBlocks.CRATE_DESH.get();
                case TUNGSTEN -> HbmBlocks.CRATE_TUNGSTEN.get();
            };
        }

        private static boolean isEmpty(CraftingInput input, int x, int y) {
            return input.getItem(x + y * input.width()).isEmpty();
        }

        private static boolean is(CraftingInput input, int x, int y, net.minecraft.world.item.Item item) {
            return input.getItem(x + y * input.width()).is(item);
        }

        private static boolean isDeshPlate(CraftingInput input, int x, int y) {
            return is(input, x, y, item("plate_desh"));
        }

        private static boolean isTungstenBlock(CraftingInput input, int x, int y) {
            return is(input, x, y, item("block_tungsten"));
        }

        private static boolean isCopperCastPlate(CraftingInput input, int x, int y) {
            ItemStack stack = input.getItem(x + y * input.width());
            if (!stack.is(com.reinhardt.hbm.registry.HbmItems.PLATE_CAST.get())) {
                return false;
            }
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data == null) {
                return false;
            }
            String material = data.copyTag().getString("material");
            int materialId = data.copyTag().getInt("material_id");
            return "copper".equals(material) || materialId == 2900;
        }

        private static net.minecraft.world.item.Item item(String path) {
            return BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(path));
        }
    }

    public static class DeshSerializer implements RecipeSerializer<StorageCrateUpgradeRecipe> {
        private static final MapCodec<StorageCrateUpgradeRecipe> CODEC = MapCodec.unit(() -> new StorageCrateUpgradeRecipe(Target.DESH));
        private static final StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> STREAM_CODEC = createStreamCodec(Target.DESH);

        @Override
        public MapCodec<StorageCrateUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class TungstenSerializer implements RecipeSerializer<StorageCrateUpgradeRecipe> {
        private static final MapCodec<StorageCrateUpgradeRecipe> CODEC = MapCodec.unit(() -> new StorageCrateUpgradeRecipe(Target.TUNGSTEN));
        private static final StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> STREAM_CODEC = createStreamCodec(Target.TUNGSTEN);

        @Override
        public MapCodec<StorageCrateUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    private static StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> createStreamCodec(Target target) {
        return new StreamCodec<>() {
            @Override
            public StorageCrateUpgradeRecipe decode(RegistryFriendlyByteBuf buffer) {
                return new StorageCrateUpgradeRecipe(target);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, StorageCrateUpgradeRecipe recipe) {
            }
        };
    }
}
