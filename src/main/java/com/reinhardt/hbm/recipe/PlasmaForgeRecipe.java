package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record PlasmaForgeRecipe(
        String group,
        List<String> blueprintPools,
        Optional<String> autoSwitchGroup,
        int duration,
        int power,
        long ignitionTemp,
        ItemStack icon,
        List<CountedIngredient> inputItems,
        List<PlasmaFluidStack> inputFluids,
        ItemStack result
) implements Recipe<PlasmaForgeRecipe.Input> {
    public static final int ITEM_LIMIT = 12;
    public static final int FLUID_LIMIT = 1;

    public PlasmaForgeRecipe {
        blueprintPools = List.copyOf(blueprintPools);
        autoSwitchGroup = autoSwitchGroup == null ? Optional.empty() : autoSwitchGroup.filter(value -> !value.isBlank());
        inputItems = List.copyOf(inputItems);
        inputFluids = List.copyOf(inputFluids);
        icon = icon == null ? ItemStack.EMPTY : icon;
        result = result == null ? ItemStack.EMPTY : result;
    }

    @Override
    public boolean matches(Input input, Level level) {
        return canCraft(input.items(), input.fluids(), this.inputItems, this.inputFluids);
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (CountedIngredient ingredient : this.inputItems) {
            list.add(ingredient.ingredient());
        }
        return list;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.PLASMA_FORGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.PLASMA_FORGE.get();
    }

    public ItemStack displayIcon() {
        if (!this.icon.isEmpty()) {
            return this.icon.copy();
        }
        if (!this.result.isEmpty()) {
            return this.result.copy();
        }
        if (!this.inputFluids.isEmpty()) {
            return FluidIconItem.forFluid(this.inputFluids.getFirst().type());
        }
        return new ItemStack(HbmItems.TEMPLATE_FOLDER.get());
    }

    public boolean isVisibleForPool(Optional<String> installedPool) {
        return this.blueprintPools.isEmpty() || installedPool.filter(this.blueprintPools::contains).isPresent();
    }

    public static boolean canCraft(
            List<ItemStack> itemStacks,
            List<HbmFluidStack> fluidStacks,
            List<CountedIngredient> itemInputs,
            List<PlasmaFluidStack> fluidInputs
    ) {
        if (itemInputs.size() > itemStacks.size() || fluidInputs.size() > fluidStacks.size()) {
            return false;
        }

        for (int slot = 0; slot < itemInputs.size(); slot++) {
            CountedIngredient ingredient = itemInputs.get(slot);
            ItemStack stack = itemStacks.get(slot);
            if (stack.isEmpty() || stack.getCount() < ingredient.count() || !ingredient.ingredient().test(stack)) {
                return false;
            }
        }

        for (int index = 0; index < fluidInputs.size(); index++) {
            PlasmaFluidStack required = fluidInputs.get(index);
            HbmFluidStack held = fluidStacks.get(index);
            if (held.isEmpty()
                    || held.type() != required.type()
                    || held.pressure() != required.pressure()
                    || held.amount() < required.amount()) {
                return false;
            }
        }
        return true;
    }

    public record CountedIngredient(Ingredient ingredient, int count) {
        private static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
                Codec.intRange(1, 64).fieldOf("count").forGetter(CountedIngredient::count)
        ).apply(instance, CountedIngredient::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CountedIngredient decode(RegistryFriendlyByteBuf buffer) {
                Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                int count = buffer.readVarInt();
                return new CountedIngredient(ingredient, count);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CountedIngredient countedIngredient) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, countedIngredient.ingredient());
                buffer.writeVarInt(countedIngredient.count());
            }
        };
    }

    public record PlasmaFluidStack(HbmFluidDefinition type, int amount, int pressure) {
        private static final Codec<PlasmaFluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.type().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(PlasmaFluidStack::amount),
                Codec.intRange(0, 1_000_000).optionalFieldOf("pressure", 0).forGetter(PlasmaFluidStack::pressure)
        ).apply(instance, PlasmaFluidStack::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, PlasmaFluidStack> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public PlasmaFluidStack decode(RegistryFriendlyByteBuf buffer) {
                HbmFluidDefinition fluid = HbmFluids.byName(buffer.readUtf()).orElse(HbmFluids.none());
                int amount = buffer.readVarInt();
                int pressure = buffer.readVarInt();
                return new PlasmaFluidStack(fluid, amount, pressure);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, PlasmaFluidStack stack) {
                buffer.writeUtf(stack.type().name());
                buffer.writeVarInt(stack.amount());
                buffer.writeVarInt(stack.pressure());
            }
        };

        private static PlasmaFluidStack fromName(String name, int amount, int pressure) {
            HbmFluidDefinition fluid = HbmFluids.byName(name).orElse(HbmFluids.none());
            return new PlasmaFluidStack(fluid, amount, pressure);
        }

        public boolean isEmpty() {
            return this.type.isNone() || this.amount <= 0;
        }
    }

    public record Input(List<ItemStack> items, List<HbmFluidStack> fluids) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index >= 0 && index < this.items.size() ? this.items.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return this.items.size();
        }
    }

    public static class Serializer implements RecipeSerializer<PlasmaForgeRecipe> {
        private static final MapCodec<PlasmaForgeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(PlasmaForgeRecipe::group),
                Codec.STRING.listOf().optionalFieldOf("blueprint_pools", List.of()).forGetter(PlasmaForgeRecipe::blueprintPools),
                Codec.STRING.optionalFieldOf("auto_switch_group").forGetter(PlasmaForgeRecipe::autoSwitchGroup),
                Codec.intRange(1, 1_000_000).fieldOf("duration").forGetter(PlasmaForgeRecipe::duration),
                Codec.intRange(1, 100_000_000).fieldOf("power").forGetter(PlasmaForgeRecipe::power),
                Codec.LONG.fieldOf("ignition_temp").forGetter(PlasmaForgeRecipe::ignitionTemp),
                ItemStack.STRICT_CODEC.optionalFieldOf("icon", ItemStack.EMPTY).forGetter(PlasmaForgeRecipe::icon),
                CountedIngredient.CODEC.listOf(0, ITEM_LIMIT).optionalFieldOf("input_items", List.of()).forGetter(PlasmaForgeRecipe::inputItems),
                PlasmaFluidStack.CODEC.listOf(0, FLUID_LIMIT).optionalFieldOf("input_fluids", List.of()).forGetter(PlasmaForgeRecipe::inputFluids),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(PlasmaForgeRecipe::result)
        ).apply(instance, PlasmaForgeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PlasmaForgeRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public PlasmaForgeRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                List<String> blueprintPools = readStringList(buffer);
                Optional<String> autoSwitchGroup = buffer.readBoolean() ? Optional.of(buffer.readUtf()) : Optional.empty();
                int duration = buffer.readVarInt();
                int power = buffer.readVarInt();
                long ignitionTemp = buffer.readVarLong();
                ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                List<CountedIngredient> inputItems = readList(buffer, CountedIngredient.STREAM_CODEC);
                List<PlasmaFluidStack> inputFluids = readList(buffer, PlasmaFluidStack.STREAM_CODEC);
                ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
                return new PlasmaForgeRecipe(group, blueprintPools, autoSwitchGroup, duration, power, ignitionTemp, icon, inputItems, inputFluids, result);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, PlasmaForgeRecipe recipe) {
                buffer.writeUtf(recipe.group());
                writeStringList(buffer, recipe.blueprintPools());
                buffer.writeBoolean(recipe.autoSwitchGroup().isPresent());
                recipe.autoSwitchGroup().ifPresent(buffer::writeUtf);
                buffer.writeVarInt(recipe.duration());
                buffer.writeVarInt(recipe.power());
                buffer.writeVarLong(recipe.ignitionTemp());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.icon());
                writeList(buffer, recipe.inputItems(), CountedIngredient.STREAM_CODEC);
                writeList(buffer, recipe.inputFluids(), PlasmaFluidStack.STREAM_CODEC);
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result());
            }
        };

        @Override
        public MapCodec<PlasmaForgeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PlasmaForgeRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static <T> List<T> readList(RegistryFriendlyByteBuf buffer, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
            int size = buffer.readVarInt();
            List<T> values = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                values.add(codec.decode(buffer));
            }
            return List.copyOf(values);
        }

        private static <T> void writeList(RegistryFriendlyByteBuf buffer, List<T> values, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
            buffer.writeVarInt(values.size());
            for (T value : values) {
                codec.encode(buffer, value);
            }
        }

        private static List<String> readStringList(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<String> values = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                values.add(buffer.readUtf());
            }
            return List.copyOf(values);
        }

        private static void writeStringList(RegistryFriendlyByteBuf buffer, List<String> values) {
            buffer.writeVarInt(values.size());
            for (String value : values) {
                buffer.writeUtf(value);
            }
        }
    }
}
