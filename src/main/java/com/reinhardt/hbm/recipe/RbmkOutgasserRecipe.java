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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record RbmkOutgasserRecipe(
        String group,
        Ingredient ingredient,
        ItemStack itemOutput,
        FluidOutput fluidOutput,
        boolean fusionOnly
) implements Recipe<RbmkOutgasserRecipe.Input> {
    public RbmkOutgasserRecipe {
        if (itemOutput.isEmpty()) {
            itemOutput = ItemStack.EMPTY;
        }
        if (fluidOutput == null || fluidOutput.isEmpty()) {
            fluidOutput = FluidOutput.EMPTY;
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        return !this.fusionOnly && !input.item().isEmpty() && this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.itemOutput.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.itemOutput;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.ingredient);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.RBMK_OUTGASSER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.RBMK_OUTGASSER.get();
    }

    public boolean hasItemOutput() {
        return !this.itemOutput.isEmpty();
    }

    public boolean hasFluidOutput() {
        return !this.fluidOutput.isEmpty();
    }

    public ItemStack fluidIcon() {
        return this.hasFluidOutput() ? FluidIconItem.forFluid(this.fluidOutput.fluid()) : ItemStack.EMPTY;
    }

    public record FluidOutput(HbmFluidDefinition fluid, int amount) {
        public static final FluidOutput EMPTY = new FluidOutput(HbmFluids.none(), 0);

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

        public boolean isEmpty() {
            return this.fluid.isNone() || this.amount <= 0;
        }
    }

    public record Input(ItemStack item) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? this.item : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    public static class Serializer implements RecipeSerializer<RbmkOutgasserRecipe> {
        private static final MapCodec<RbmkOutgasserRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(RbmkOutgasserRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(RbmkOutgasserRecipe::ingredient),
                ItemStack.STRICT_CODEC.optionalFieldOf("item_output", ItemStack.EMPTY).forGetter(RbmkOutgasserRecipe::itemOutput),
                FluidOutput.CODEC.optionalFieldOf("fluid_output", FluidOutput.EMPTY).forGetter(RbmkOutgasserRecipe::fluidOutput),
                Codec.BOOL.optionalFieldOf("fusion_only", false).forGetter(RbmkOutgasserRecipe::fusionOnly)
        ).apply(instance, RbmkOutgasserRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, RbmkOutgasserRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                RbmkOutgasserRecipe::group,
                Ingredient.CONTENTS_STREAM_CODEC,
                RbmkOutgasserRecipe::ingredient,
                ItemStack.OPTIONAL_STREAM_CODEC,
                RbmkOutgasserRecipe::itemOutput,
                FluidOutput.STREAM_CODEC,
                RbmkOutgasserRecipe::fluidOutput,
                ByteBufCodecs.BOOL,
                RbmkOutgasserRecipe::fusionOnly,
                RbmkOutgasserRecipe::new
        );

        @Override
        public MapCodec<RbmkOutgasserRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RbmkOutgasserRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
