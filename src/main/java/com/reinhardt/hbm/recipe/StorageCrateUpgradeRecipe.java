package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

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
        ItemStack source = findSource(input);
        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(this.target.result().asItem());
        copyContainerData(source, result, registries);
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
            case SAFE -> HbmRecipeTypes.SAFE_SERIALIZER.get();
            case DESH -> HbmRecipeTypes.CRATE_DESH_UPGRADE_SERIALIZER.get();
            case TUNGSTEN -> HbmRecipeTypes.CRATE_TUNGSTEN_UPGRADE_SERIALIZER.get();
            case MASS_STORAGE -> HbmRecipeTypes.MASS_STORAGE_SERIALIZER.get();
            case MASS_STORAGE_DESH -> HbmRecipeTypes.MASS_STORAGE_DESH_SERIALIZER.get();
            case MASS_STORAGE_RESISTANT -> HbmRecipeTypes.MASS_STORAGE_RESISTANT_SERIALIZER.get();
        };
    }

    private ItemStack findSource(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (this.target.source().test(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private void copyContainerData(ItemStack source, ItemStack result, HolderLookup.Provider registries) {
        CustomData data = source.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }

        CompoundTag root = data.copyTag();
        if (this.target == Target.SAFE && root.contains("crate_data")) {
            CompoundTag crateData = root.getCompound("crate_data");
            NonNullList<ItemStack> contents = NonNullList.withSize(15, ItemStack.EMPTY);
            for (int slot = 0; slot < contents.size(); slot++) {
                contents.set(slot, ItemStack.parseOptional(registries, crateData.getCompound("slot" + slot)));
            }
            CompoundTag safeData = new CompoundTag();
            net.minecraft.world.ContainerHelper.saveAllItems(safeData, contents, registries);
            safeData.putInt("lock", crateData.getInt("lock"));
            safeData.putBoolean("isLocked", crateData.getBoolean("isLocked"));
            safeData.putDouble("lockMod", crateData.contains("lockMod") ? crateData.getDouble("lockMod") : 0.1D);
            safeData.putBoolean("cheesable", !crateData.contains("cheesable") || crateData.getBoolean("cheesable"));
            root.remove("crate_data");
            root.put("safe_data", safeData);
        } else if (this.target == Target.MASS_STORAGE_DESH || this.target == Target.MASS_STORAGE_RESISTANT) {
            result.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            return;
        } else if (this.target == Target.MASS_STORAGE) {
            return;
        }
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public enum Target {
        SAFE,
        DESH,
        TUNGSTEN,
        MASS_STORAGE,
        MASS_STORAGE_DESH,
        MASS_STORAGE_RESISTANT;

        private boolean matches(CraftingInput input) {
            if (input.width() != 3 || input.height() != 3) {
                return false;
            }
            return switch (this) {
                case SAFE -> matchesSafe(input);
                case DESH -> matchesDesh(input);
                case TUNGSTEN -> matchesTungsten(input);
                case MASS_STORAGE -> matchesMassStorage(input);
                case MASS_STORAGE_DESH -> matchesMassStorageDesh(input);
                case MASS_STORAGE_RESISTANT -> matchesMassStorageResistant(input);
            };
        }

        private boolean matchesSafe(CraftingInput input) {
            return isEmpty(input, 0, 0) && isPlate(input, 1, 0, "lead") && isEmpty(input, 2, 0)
                    && isPlate(input, 0, 1, "titanium") && is(input, 1, 1, HbmBlocks.CRATE_STEEL.asItem()) && isPlate(input, 2, 1, "titanium")
                    && isEmpty(input, 0, 2) && isPlate(input, 1, 2, "lead") && isEmpty(input, 2, 2);
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

        private boolean matchesMassStorage(CraftingInput input) {
            return isEmpty(input, 0, 0) && is(input, 1, 0, HbmItems.CIRCUIT_VACUUM_TUBE.get()) && isEmpty(input, 2, 0)
                    && isTag(input, 0, 1, "c:ingots/titanium") && is(input, 1, 1, HbmBlocks.CRATE_STEEL.asItem()) && isTag(input, 2, 1, "c:ingots/titanium")
                    && isEmpty(input, 0, 2) && isTag(input, 1, 2, "c:ingots/titanium") && isEmpty(input, 2, 2);
        }

        private boolean matchesMassStorageDesh(CraftingInput input) {
            return isEmpty(input, 0, 0) && is(input, 1, 0, HbmItems.CIRCUIT_CHIP.get()) && isEmpty(input, 2, 0)
                    && isTag(input, 0, 1, "c:ingots/desh") && is(input, 1, 1, HbmBlocks.MASS_STORAGE_IRON.asItem()) && isTag(input, 2, 1, "c:ingots/desh")
                    && isEmpty(input, 0, 2) && isTag(input, 1, 2, "c:ingots/desh") && isEmpty(input, 2, 2);
        }

        private boolean matchesMassStorageResistant(CraftingInput input) {
            return isEmpty(input, 0, 0) && is(input, 1, 0, HbmItems.CIRCUIT_ADVANCED.get()) && isEmpty(input, 2, 0)
                    && isTag(input, 0, 1, "c:ingots/resistant_alloy") && is(input, 1, 1, HbmBlocks.MASS_STORAGE_DESH.asItem()) && isTag(input, 2, 1, "c:ingots/resistant_alloy")
                    && isEmpty(input, 0, 2) && isTag(input, 1, 2, "c:ingots/resistant_alloy") && isEmpty(input, 2, 2);
        }

        private NonNullList<Ingredient> ingredients() {
            NonNullList<Ingredient> ingredients = NonNullList.create();
            switch (this) {
                case SAFE -> {
                    ingredients.add(Ingredient.of(HbmBlocks.CRATE_STEEL.asItem()));
                    ingredients.add(Ingredient.of(tag("c:plates/lead")));
                    ingredients.add(Ingredient.of(tag("c:plates/titanium")));
                }
                case DESH -> {
                    ingredients.add(Ingredient.of(HbmBlocks.CRATE_STEEL.asItem()));
                    ingredients.add(Ingredient.of(item("plate_desh")));
                }
                case TUNGSTEN -> {
                    ingredients.add(Ingredient.of(HbmBlocks.CRATE_STEEL.asItem()));
                    ingredients.add(Ingredient.of(item("block_tungsten")));
                    ingredients.add(Ingredient.of(com.reinhardt.hbm.registry.HbmItems.PLATE_CAST.get()));
                }
                case MASS_STORAGE -> {
                    ingredients.add(Ingredient.of(HbmBlocks.CRATE_STEEL.asItem()));
                    ingredients.add(Ingredient.of(HbmItems.CIRCUIT_VACUUM_TUBE.get()));
                    ingredients.add(Ingredient.of(tag("c:ingots/titanium")));
                }
                case MASS_STORAGE_DESH -> {
                    ingredients.add(Ingredient.of(HbmBlocks.MASS_STORAGE_IRON.asItem()));
                    ingredients.add(Ingredient.of(HbmItems.CIRCUIT_CHIP.get()));
                    ingredients.add(Ingredient.of(tag("c:ingots/desh")));
                }
                case MASS_STORAGE_RESISTANT -> {
                    ingredients.add(Ingredient.of(HbmBlocks.MASS_STORAGE_DESH.asItem()));
                    ingredients.add(Ingredient.of(HbmItems.CIRCUIT_ADVANCED.get()));
                    ingredients.add(Ingredient.of(tag("c:ingots/resistant_alloy")));
                }
            }
            return ingredients;
        }

        private net.minecraft.world.level.ItemLike result() {
            return switch (this) {
                case SAFE -> HbmBlocks.SAFE.get();
                case DESH -> HbmBlocks.CRATE_DESH.get();
                case TUNGSTEN -> HbmBlocks.CRATE_TUNGSTEN.get();
                case MASS_STORAGE -> HbmBlocks.MASS_STORAGE_IRON.get();
                case MASS_STORAGE_DESH -> HbmBlocks.MASS_STORAGE_DESH.get();
                case MASS_STORAGE_RESISTANT -> HbmBlocks.MASS_STORAGE.get();
            };
        }

        private Predicate<ItemStack> source() {
            return switch (this) {
                case SAFE, MASS_STORAGE, DESH, TUNGSTEN -> stack -> stack.is(HbmBlocks.CRATE_STEEL.asItem());
                case MASS_STORAGE_DESH -> stack -> stack.is(HbmBlocks.MASS_STORAGE_IRON.asItem());
                case MASS_STORAGE_RESISTANT -> stack -> stack.is(HbmBlocks.MASS_STORAGE_DESH.asItem());
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

        private static boolean isPlate(CraftingInput input, int x, int y, String material) {
            return isTag(input, x, y, "c:plates/" + material);
        }

        private static boolean isTag(CraftingInput input, int x, int y, String id) {
            return input.getItem(x + y * input.width()).is(tag(id));
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

        private static TagKey<net.minecraft.world.item.Item> tag(String id) {
            ResourceLocation location = id.indexOf(':') >= 0 ? ResourceLocation.parse(id) : ReinhardtsHBM.id(id);
            return TagKey.create(Registries.ITEM, location);
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

    public static class SafeSerializer implements RecipeSerializer<StorageCrateUpgradeRecipe> {
        private static final MapCodec<StorageCrateUpgradeRecipe> CODEC = MapCodec.unit(() -> new StorageCrateUpgradeRecipe(Target.SAFE));
        private static final StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> STREAM_CODEC = createStreamCodec(Target.SAFE);
        @Override public MapCodec<StorageCrateUpgradeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> streamCodec() { return STREAM_CODEC; }
    }

    public static class MassStorageSerializer implements RecipeSerializer<StorageCrateUpgradeRecipe> {
        private static final MapCodec<StorageCrateUpgradeRecipe> CODEC = MapCodec.unit(() -> new StorageCrateUpgradeRecipe(Target.MASS_STORAGE));
        private static final StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> STREAM_CODEC = createStreamCodec(Target.MASS_STORAGE);
        @Override public MapCodec<StorageCrateUpgradeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> streamCodec() { return STREAM_CODEC; }
    }

    public static class MassStorageDeshSerializer implements RecipeSerializer<StorageCrateUpgradeRecipe> {
        private static final MapCodec<StorageCrateUpgradeRecipe> CODEC = MapCodec.unit(() -> new StorageCrateUpgradeRecipe(Target.MASS_STORAGE_DESH));
        private static final StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> STREAM_CODEC = createStreamCodec(Target.MASS_STORAGE_DESH);
        @Override public MapCodec<StorageCrateUpgradeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> streamCodec() { return STREAM_CODEC; }
    }

    public static class MassStorageResistantSerializer implements RecipeSerializer<StorageCrateUpgradeRecipe> {
        private static final MapCodec<StorageCrateUpgradeRecipe> CODEC = MapCodec.unit(() -> new StorageCrateUpgradeRecipe(Target.MASS_STORAGE_RESISTANT));
        private static final StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> STREAM_CODEC = createStreamCodec(Target.MASS_STORAGE_RESISTANT);
        @Override public MapCodec<StorageCrateUpgradeRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, StorageCrateUpgradeRecipe> streamCodec() { return STREAM_CODEC; }
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
