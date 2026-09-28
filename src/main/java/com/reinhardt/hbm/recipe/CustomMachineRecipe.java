package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** Data form of the latest 1.7.10 custom-machine recipe table. */
public record CustomMachineRecipe(
        String group,
        String machine,
        Ingredient ingredient,
        int inputCount,
        FluidInput inputFluid,
        ItemStack result,
        int duration,
        int power,
        String pollutionType,
        double pollutionAmount
) implements Recipe<CustomMachineRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        return this.machine.equals(input.machine())
                && !input.item().isEmpty()
                && input.item().getCount() >= this.inputCount
                && this.ingredient.test(input.item())
                && input.fluid().type() == this.inputFluid.fluid
                && input.fluid().pressure() == this.inputFluid.pressure
                && input.fluid().amount() >= this.inputFluid.amount;
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
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.CUSTOM_MACHINE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.CUSTOM_MACHINE.get();
    }

    public record FluidInput(HbmFluidDefinition fluid, int amount, int pressure) {
        private static final Codec<FluidInput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(value -> value.fluid.name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidInput::amount),
                Codec.intRange(0, 1_000_000).optionalFieldOf("pressure", 0).forGetter(FluidInput::pressure)
        ).apply(instance, FluidInput::fromName));

        private static FluidInput fromName(String fluidName, int amount, int pressure) {
            return new FluidInput(HbmFluids.byName(fluidName).orElse(HbmFluids.none()), amount, pressure);
        }
    }

    public record Input(String machine, ItemStack item, HbmFluidStack fluid) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? this.item : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static final class Serializer implements RecipeSerializer<CustomMachineRecipe> {
        private static final MapCodec<CustomMachineRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(CustomMachineRecipe::group),
                Codec.STRING.fieldOf("machine").forGetter(CustomMachineRecipe::machine),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CustomMachineRecipe::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(CustomMachineRecipe::inputCount),
                FluidInput.CODEC.fieldOf("input_fluid").forGetter(CustomMachineRecipe::inputFluid),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CustomMachineRecipe::result),
                Codec.intRange(1, 1_000_000).fieldOf("duration").forGetter(CustomMachineRecipe::duration),
                Codec.intRange(1, 1_000_000).fieldOf("power").forGetter(CustomMachineRecipe::power),
                Codec.STRING.optionalFieldOf("pollution_type", "").forGetter(CustomMachineRecipe::pollutionType),
                Codec.DOUBLE.optionalFieldOf("pollution_amount", 0.0D).forGetter(CustomMachineRecipe::pollutionAmount)
        ).apply(instance, CustomMachineRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CustomMachineRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CustomMachineRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                String machine = buffer.readUtf();
                Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                int inputCount = buffer.readVarInt();
                FluidInput fluid = new FluidInput(
                        HbmFluids.byName(buffer.readUtf()).orElse(HbmFluids.none()),
                        buffer.readVarInt(),
                        buffer.readVarInt()
                );
                ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
                int duration = buffer.readVarInt();
                int power = buffer.readVarInt();
                String pollutionType = buffer.readUtf();
                double pollutionAmount = buffer.readDouble();
                return new CustomMachineRecipe(group, machine, ingredient, inputCount, fluid, result,
                        duration, power, pollutionType, pollutionAmount);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CustomMachineRecipe recipe) {
                buffer.writeUtf(recipe.group);
                buffer.writeUtf(recipe.machine);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
                buffer.writeVarInt(recipe.inputCount);
                buffer.writeUtf(recipe.inputFluid.fluid.name());
                buffer.writeVarInt(recipe.inputFluid.amount);
                buffer.writeVarInt(recipe.inputFluid.pressure);
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                buffer.writeVarInt(recipe.duration);
                buffer.writeVarInt(recipe.power);
                buffer.writeUtf(recipe.pollutionType);
                buffer.writeDouble(recipe.pollutionAmount);
            }
        };

        @Override
        public MapCodec<CustomMachineRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CustomMachineRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
