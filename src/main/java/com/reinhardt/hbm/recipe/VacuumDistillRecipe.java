package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.registry.HbmFluids;
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

public record VacuumDistillRecipe(
        String group,
        FluidIngredient input,
        List<FluidOutput> outputs,
        ItemStack byproduct,
        int byproductInterval,
        int power
) implements Recipe<VacuumDistillRecipe.Input> {
    public static final int OUTPUT_LIMIT = 4;

    public VacuumDistillRecipe {
        outputs = List.copyOf(outputs);
        if (outputs.size() > OUTPUT_LIMIT) {
            throw new IllegalArgumentException("Refinery recipes support at most " + OUTPUT_LIMIT + " fluid outputs");
        }
        byproductInterval = Math.max(1, byproductInterval);
        power = Math.max(1, power);
    }

    @Override
    public boolean matches(Input input, Level level) {
        return input.fluid() == this.input.fluid() && input.amount() >= this.input.amount();
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.byproduct.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.byproduct;
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
        return HbmRecipeTypes.VACUUM_DISTILL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.VACUUM_DISTILL.get();
    }

    public ItemStack displayIcon() {
        return FluidIconItem.forFluid(this.input.fluid());
    }

    public HbmFluidStack inputStack() {
        return new HbmFluidStack(this.input.fluid(), this.input.amount(), 0);
    }

    public record FluidIngredient(HbmFluidDefinition fluid, int amount) {
        private static final Codec<FluidIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.fluid().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidIngredient::amount)
        ).apply(instance, FluidIngredient::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidIngredient decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidIngredient ingredient) {
                buffer.writeUtf(ingredient.fluid().name());
                buffer.writeVarInt(ingredient.amount());
            }
        };

        private static FluidIngredient fromName(String fluid, int amount) {
            return new FluidIngredient(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount);
        }
    }

    public record FluidOutput(HbmFluidDefinition fluid, int amount) {
        private static final Codec<FluidOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.fluid().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidOutput::amount)
        ).apply(instance, FluidOutput::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidOutput> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidOutput decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidOutput output) {
                buffer.writeUtf(output.fluid().name());
                buffer.writeVarInt(output.amount());
            }
        };

        private static FluidOutput fromName(String fluid, int amount) {
            return new FluidOutput(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount);
        }
    }

    public record Input(HbmFluidDefinition fluid, int amount) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    public static class Serializer implements RecipeSerializer<VacuumDistillRecipe> {
        private static final MapCodec<VacuumDistillRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(VacuumDistillRecipe::group),
                FluidIngredient.CODEC.fieldOf("input").forGetter(VacuumDistillRecipe::input),
                FluidOutput.CODEC.listOf().fieldOf("outputs").forGetter(VacuumDistillRecipe::outputs),
                ItemStack.STRICT_CODEC.optionalFieldOf("byproduct", ItemStack.EMPTY).forGetter(VacuumDistillRecipe::byproduct),
                Codec.intRange(1, 1_000_000).optionalFieldOf("byproduct_interval", 100).forGetter(VacuumDistillRecipe::byproductInterval),
                Codec.intRange(1, 1_000_000).optionalFieldOf("power", 5).forGetter(VacuumDistillRecipe::power)
        ).apply(instance, VacuumDistillRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, VacuumDistillRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public VacuumDistillRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                FluidIngredient input = FluidIngredient.STREAM_CODEC.decode(buffer);
                int outputCount = buffer.readVarInt();
                List<FluidOutput> outputs = new ArrayList<>(outputCount);
                for (int index = 0; index < outputCount; index++) {
                    outputs.add(FluidOutput.STREAM_CODEC.decode(buffer));
                }
                ItemStack byproduct = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                int byproductInterval = buffer.readVarInt();
                int power = buffer.readVarInt();
                return new VacuumDistillRecipe(group, input, outputs, byproduct, byproductInterval, power);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, VacuumDistillRecipe recipe) {
                buffer.writeUtf(recipe.group());
                FluidIngredient.STREAM_CODEC.encode(buffer, recipe.input());
                buffer.writeVarInt(recipe.outputs().size());
                for (FluidOutput output : recipe.outputs()) {
                    FluidOutput.STREAM_CODEC.encode(buffer, output);
                }
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.byproduct());
                buffer.writeVarInt(recipe.byproductInterval());
                buffer.writeVarInt(recipe.power());
            }
        };

        @Override
        public MapCodec<VacuumDistillRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, VacuumDistillRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}

