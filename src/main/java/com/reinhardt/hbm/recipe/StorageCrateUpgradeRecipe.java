package com.reinhardt.hbm.recipe;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Fixed shaped recipes which preserve the stored contents of the upgraded container.
 *
 * <p>The 1.7.10 implementation extended {@code ShapedOreRecipe}. Keeping this as a real
 * shaped recipe is important beyond presentation: JEI can expose the crafting-table
 * route, recipe transfer retains the exact 3x3 layout, and automation sees every repeated
 * ingredient instead of one de-duplicated material entry.</p>
 */
public class StorageCrateUpgradeRecipe extends ShapedRecipe {
    private final Target target;

    public StorageCrateUpgradeRecipe(Target target) {
        super("", CraftingBookCategory.MISC, target.pattern(), new ItemStack(target.result().asItem()));
        this.target = target;
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

        private ShapedRecipePattern pattern() {
            return switch (this) {
                case SAFE -> ShapedRecipePattern.of(Map.of(
                        'L', Ingredient.of(tag("c:plates/lead")),
                        'A', Ingredient.of(tag("c:plates/titanium")),
                        'C', Ingredient.of(HbmBlocks.CRATE_STEEL.asItem())
                ), "LAL", "ACA", "LAL");
                case DESH -> ShapedRecipePattern.of(Map.of(
                        'D', Ingredient.of(tag("c:plates/desh")),
                        'S', Ingredient.of(HbmBlocks.CRATE_STEEL.asItem())
                ), " D ", "DSD", " D ");
                case TUNGSTEN -> ShapedRecipePattern.of(Map.of(
                        'B', Ingredient.of(tag("c:storage_blocks/tungsten")),
                        'P', Ingredient.of(tag("c:plates/cast_copper")),
                        'C', Ingredient.of(HbmBlocks.CRATE_STEEL.asItem())
                ), "BPB", "PCP", "BPB");
                case MASS_STORAGE -> ShapedRecipePattern.of(Map.of(
                        'L', Ingredient.of(HbmItems.CIRCUIT_VACUUM_TUBE.get()),
                        'I', Ingredient.of(tag("c:ingots/titanium")),
                        'C', Ingredient.of(HbmBlocks.CRATE_STEEL.asItem())
                ), " L ", "ICI", " I ");
                case MASS_STORAGE_DESH -> ShapedRecipePattern.of(Map.of(
                        'C', Ingredient.of(HbmItems.CIRCUIT_CHIP.get()),
                        'P', Ingredient.of(tag("c:ingots/desh")),
                        'M', Ingredient.of(HbmBlocks.MASS_STORAGE_IRON.asItem())
                ), " C ", "PMP", " P ");
                case MASS_STORAGE_RESISTANT -> ShapedRecipePattern.of(Map.of(
                        'C', Ingredient.of(HbmItems.CIRCUIT_ADVANCED.get()),
                        'P', Ingredient.of(tag("c:ingots/resistant_alloy")),
                        'M', Ingredient.of(HbmBlocks.MASS_STORAGE_DESH.asItem())
                ), " C ", "PMP", " P ");
            };
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

        private static TagKey<net.minecraft.world.item.Item> tag(String id) {
            return TagKey.create(Registries.ITEM, ResourceLocation.parse(id));
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
