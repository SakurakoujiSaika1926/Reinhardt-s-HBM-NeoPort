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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public record MixerRecipe(
        String group,
        FluidStackDef output,
        Optional<FluidStackDef> input1,
        Optional<FluidStackDef> input2,
        Optional<CountedIngredient> solidInput,
        int duration
) implements Recipe<MixerRecipe.Input> {
    @Override
    public boolean matches(Input input, Level level) {
        if (input.outputFluid() != this.output.fluid()) {
            return false;
        }
        if (this.input1.isPresent() && !matchesFluid(input.input1(), this.input1.get())) {
            return false;
        }
        if (this.input2.isPresent() && !matchesFluid(input.input2(), this.input2.get())) {
            return false;
        }
        return this.solidInput.map(ingredient -> ingredient.matches(input.solid())).orElse(true);
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return FluidIconItem.forFluid(this.output.fluid());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return FluidIconItem.forFluid(this.output.fluid());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        this.solidInput.ifPresent(input -> ingredients.add(input.ingredient()));
        return ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.MIXER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.MIXER.get();
    }

    private static boolean matchesFluid(HbmFluidStack held, FluidStackDef required) {
        return !held.isEmpty()
                && held.type() == required.fluid()
                && held.pressure() == required.pressure()
                && held.amount() >= required.amount();
    }

    public record Input(HbmFluidDefinition outputFluid, HbmFluidStack input1, HbmFluidStack input2, ItemStack solid) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? this.solid : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public record FluidStackDef(HbmFluidDefinition fluid, int amount, int pressure) {
        public static final Codec<FluidStackDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.fluid().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidStackDef::amount),
                Codec.intRange(0, 1_000_000).optionalFieldOf("pressure", 0).forGetter(FluidStackDef::pressure)
        ).apply(instance, FluidStackDef::fromName));

        public static final StreamCodec<RegistryFriendlyByteBuf, FluidStackDef> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidStackDef decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidStackDef stack) {
                buffer.writeUtf(stack.fluid().name());
                buffer.writeVarInt(stack.amount());
                buffer.writeVarInt(stack.pressure());
            }
        };

        public HbmFluidStack stack() {
            return new HbmFluidStack(this.fluid, this.amount, this.pressure);
        }

        private static FluidStackDef fromName(String fluid, int amount, int pressure) {
            return new FluidStackDef(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount, pressure);
        }
    }

    public record CountedIngredient(Ingredient ingredient, int count) {
        public static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(CountedIngredient::count)
        ).apply(instance, CountedIngredient::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CountedIngredient decode(RegistryFriendlyByteBuf buffer) {
                return new CountedIngredient(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CountedIngredient input) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, input.ingredient());
                buffer.writeVarInt(input.count());
            }
        };

        public boolean matches(ItemStack stack) {
            return stack.getCount() >= this.count && this.ingredient.test(stack);
        }
    }

    public static class Serializer implements RecipeSerializer<MixerRecipe> {
        private static final MapCodec<MixerRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(MixerRecipe::group),
                FluidStackDef.CODEC.fieldOf("output").forGetter(MixerRecipe::output),
                FluidStackDef.CODEC.optionalFieldOf("input1").forGetter(MixerRecipe::input1),
                FluidStackDef.CODEC.optionalFieldOf("input2").forGetter(MixerRecipe::input2),
                CountedIngredient.CODEC.optionalFieldOf("solid_input").forGetter(MixerRecipe::solidInput),
                Codec.intRange(1, 20_000).fieldOf("duration").forGetter(MixerRecipe::duration)
        ).apply(instance, MixerRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, Optional<FluidStackDef>> OPTIONAL_FLUID =
                ByteBufCodecs.optional(FluidStackDef.STREAM_CODEC);
        private static final StreamCodec<RegistryFriendlyByteBuf, Optional<CountedIngredient>> OPTIONAL_INGREDIENT =
                ByteBufCodecs.optional(CountedIngredient.STREAM_CODEC);

        private static final StreamCodec<RegistryFriendlyByteBuf, MixerRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                MixerRecipe::group,
                FluidStackDef.STREAM_CODEC,
                MixerRecipe::output,
                OPTIONAL_FLUID,
                MixerRecipe::input1,
                OPTIONAL_FLUID,
                MixerRecipe::input2,
                OPTIONAL_INGREDIENT,
                MixerRecipe::solidInput,
                ByteBufCodecs.VAR_INT,
                MixerRecipe::duration,
                MixerRecipe::new
        );

        @Override
        public MapCodec<MixerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MixerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
