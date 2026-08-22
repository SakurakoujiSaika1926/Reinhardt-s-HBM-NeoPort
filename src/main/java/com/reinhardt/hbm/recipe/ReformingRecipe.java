package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
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

import java.util.List;

public record ReformingRecipe(
        String group,
        FluidIngredient input,
        FluidOutput output1,
        FluidOutput output2,
        FluidOutput output3,
        int power
) implements Recipe<ReformingRecipe.Input> {
    public static final int DEFAULT_POWER = 20_000;

    @Override
    public boolean matches(Input input, Level level) {
        return input.fluid() == this.input.fluid() && input.amount() >= this.input.amount();
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return FluidIconItem.forFluid(this.output1.fluid());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return FluidIconItem.forFluid(this.output1.fluid());
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
        return HbmRecipeTypes.REFORMING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.REFORMING.get();
    }

    public List<FluidOutput> outputs() {
        return List.of(this.output1, this.output2, this.output3);
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
                Codec.intRange(0, 1_000_000).fieldOf("amount").forGetter(FluidOutput::amount)
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

        public boolean isEmpty() {
            return this.fluid.isNone() || this.amount <= 0;
        }

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

    public static class Serializer implements RecipeSerializer<ReformingRecipe> {
        private static final MapCodec<ReformingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ReformingRecipe::group),
                FluidIngredient.CODEC.fieldOf("input").forGetter(ReformingRecipe::input),
                FluidOutput.CODEC.fieldOf("output1").forGetter(ReformingRecipe::output1),
                FluidOutput.CODEC.fieldOf("output2").forGetter(ReformingRecipe::output2),
                FluidOutput.CODEC.fieldOf("output3").forGetter(ReformingRecipe::output3),
                Codec.intRange(1, 1_000_000_000).optionalFieldOf("power", DEFAULT_POWER).forGetter(ReformingRecipe::power)
        ).apply(instance, ReformingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ReformingRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ReformingRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                FluidIngredient input = FluidIngredient.STREAM_CODEC.decode(buffer);
                FluidOutput output1 = FluidOutput.STREAM_CODEC.decode(buffer);
                FluidOutput output2 = FluidOutput.STREAM_CODEC.decode(buffer);
                FluidOutput output3 = FluidOutput.STREAM_CODEC.decode(buffer);
                int power = buffer.readVarInt();
                return new ReformingRecipe(group, input, output1, output2, output3, power);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ReformingRecipe recipe) {
                buffer.writeUtf(recipe.group());
                FluidIngredient.STREAM_CODEC.encode(buffer, recipe.input());
                FluidOutput.STREAM_CODEC.encode(buffer, recipe.output1());
                FluidOutput.STREAM_CODEC.encode(buffer, recipe.output2());
                FluidOutput.STREAM_CODEC.encode(buffer, recipe.output3());
                buffer.writeVarInt(recipe.power());
            }
        };

        @Override
        public MapCodec<ReformingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ReformingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
