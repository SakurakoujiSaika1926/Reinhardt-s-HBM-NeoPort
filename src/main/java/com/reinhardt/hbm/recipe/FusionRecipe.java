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

public record FusionRecipe(
        String group,
        long ignitionTemp,
        long outputTemp,
        double neutronFlux,
        int power,
        int duration,
        float r,
        float g,
        float b,
        ItemStack icon,
        List<FusionFluidStack> inputFluids,
        List<FusionFluidStack> outputFluids,
        Optional<ItemStack> outputItem
) implements Recipe<FusionRecipe.Input> {
    public FusionRecipe {
        icon = icon == null ? ItemStack.EMPTY : icon;
        inputFluids = List.copyOf(inputFluids);
        outputFluids = List.copyOf(outputFluids);
        outputItem = outputItem == null ? Optional.empty() : outputItem;
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (input.klystronEnergy() < this.ignitionTemp || input.power() < this.power) {
            return false;
        }
        if (this.inputFluids.size() > input.fluids().size()) {
            return false;
        }
        for (int index = 0; index < this.inputFluids.size(); index++) {
            FusionFluidStack required = this.inputFluids.get(index);
            HbmFluidStack held = input.fluids().get(index);
            if (held.isEmpty() || held.type() != required.fluid() || held.amount() < required.amount()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        if (this.outputItem.isPresent()) {
            return this.outputItem.get().copy();
        }
        if (!this.outputFluids.isEmpty()) {
            return FluidIconItem.forFluid(this.outputFluids.getFirst().fluid());
        }
        return ItemStack.EMPTY;
    }

    public ItemStack displayIcon() {
        if (!this.icon.isEmpty()) {
            return this.icon.copy();
        }
        ItemStack result = getResultItem(null);
        if (!result.isEmpty()) {
            return result;
        }
        if (!this.inputFluids.isEmpty()) {
            return FluidIconItem.forFluid(this.inputFluids.getFirst().fluid());
        }
        return new ItemStack(HbmItems.TEMPLATE_FOLDER.get());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.FUSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.FUSION.get();
    }

    public record FusionFluidStack(HbmFluidDefinition fluid, int amount) {
        private static final Codec<FusionFluidStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.fluid().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FusionFluidStack::amount)
        ).apply(instance, FusionFluidStack::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FusionFluidStack> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FusionFluidStack decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FusionFluidStack stack) {
                buffer.writeUtf(stack.fluid().name());
                buffer.writeVarInt(stack.amount());
            }
        };

        private static FusionFluidStack fromName(String name, int amount) {
            return new FusionFluidStack(HbmFluids.byName(name).orElse(HbmFluids.none()), amount);
        }
    }

    public record Input(List<HbmFluidStack> fluids, long klystronEnergy, long power) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    public static class Serializer implements RecipeSerializer<FusionRecipe> {
        private static final MapCodec<FusionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(FusionRecipe::group),
                Codec.LONG.fieldOf("ignition_temp").forGetter(FusionRecipe::ignitionTemp),
                Codec.LONG.fieldOf("output_temp").forGetter(FusionRecipe::outputTemp),
                Codec.DOUBLE.fieldOf("neutron_flux").forGetter(FusionRecipe::neutronFlux),
                Codec.intRange(1, 1_000_000).fieldOf("power").forGetter(FusionRecipe::power),
                Codec.intRange(1, 1_000_000).fieldOf("duration").forGetter(FusionRecipe::duration),
                Codec.FLOAT.optionalFieldOf("r", 0.7F).forGetter(FusionRecipe::r),
                Codec.FLOAT.optionalFieldOf("g", 0.7F).forGetter(FusionRecipe::g),
                Codec.FLOAT.optionalFieldOf("b", 1.0F).forGetter(FusionRecipe::b),
                ItemStack.STRICT_CODEC.optionalFieldOf("icon", ItemStack.EMPTY).forGetter(FusionRecipe::icon),
                FusionFluidStack.CODEC.listOf(1, 3).fieldOf("input_fluids").forGetter(FusionRecipe::inputFluids),
                FusionFluidStack.CODEC.listOf(0, 11).optionalFieldOf("output_fluids", List.of()).forGetter(FusionRecipe::outputFluids),
                ItemStack.STRICT_CODEC.optionalFieldOf("output_item").forGetter(FusionRecipe::outputItem)
        ).apply(instance, FusionRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FusionRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FusionRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                long ignitionTemp = buffer.readVarLong();
                long outputTemp = buffer.readVarLong();
                double neutronFlux = buffer.readDouble();
                int power = buffer.readVarInt();
                int duration = buffer.readVarInt();
                float r = buffer.readFloat();
                float g = buffer.readFloat();
                float b = buffer.readFloat();
                ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                List<FusionFluidStack> inputFluids = readFluidList(buffer);
                List<FusionFluidStack> outputFluids = readFluidList(buffer);
                Optional<ItemStack> outputItem = buffer.readBoolean() ? Optional.of(ItemStack.STREAM_CODEC.decode(buffer)) : Optional.empty();
                return new FusionRecipe(group, ignitionTemp, outputTemp, neutronFlux, power, duration, r, g, b, icon, inputFluids, outputFluids, outputItem);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FusionRecipe recipe) {
                buffer.writeUtf(recipe.group());
                buffer.writeVarLong(recipe.ignitionTemp());
                buffer.writeVarLong(recipe.outputTemp());
                buffer.writeDouble(recipe.neutronFlux());
                buffer.writeVarInt(recipe.power());
                buffer.writeVarInt(recipe.duration());
                buffer.writeFloat(recipe.r());
                buffer.writeFloat(recipe.g());
                buffer.writeFloat(recipe.b());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.icon());
                writeFluidList(buffer, recipe.inputFluids());
                writeFluidList(buffer, recipe.outputFluids());
                buffer.writeBoolean(recipe.outputItem().isPresent());
                recipe.outputItem().ifPresent(stack -> ItemStack.STREAM_CODEC.encode(buffer, stack));
            }

            private List<FusionFluidStack> readFluidList(RegistryFriendlyByteBuf buffer) {
                int count = buffer.readVarInt();
                ArrayList<FusionFluidStack> fluids = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    fluids.add(FusionFluidStack.STREAM_CODEC.decode(buffer));
                }
                return List.copyOf(fluids);
            }

            private void writeFluidList(RegistryFriendlyByteBuf buffer, List<FusionFluidStack> fluids) {
                buffer.writeVarInt(fluids.size());
                for (FusionFluidStack fluid : fluids) {
                    FusionFluidStack.STREAM_CODEC.encode(buffer, fluid);
                }
            }
        };

        @Override
        public MapCodec<FusionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FusionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
