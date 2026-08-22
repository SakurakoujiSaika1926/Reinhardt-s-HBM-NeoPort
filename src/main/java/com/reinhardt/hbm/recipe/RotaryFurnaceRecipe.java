package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.item.ScrapsItem;
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

public record RotaryFurnaceRecipe(
        String group,
        List<CountedIngredient> inputs,
        FluidInput fluid,
        MaterialOutput output,
        int duration,
        int steam
) implements Recipe<RotaryFurnaceRecipe.Input> {
    public RotaryFurnaceRecipe {
        inputs = List.copyOf(inputs);
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (input.size() < 3) {
            return false;
        }
        java.util.ArrayList<CountedIngredient> remaining = new java.util.ArrayList<>(this.inputs);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            boolean matched = false;
            for (int index = 0; index < remaining.size(); index++) {
                if (remaining.get(index).matches(stack)) {
                    remaining.remove(index);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return ScrapsItem.create(this.output.stack(), true);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ScrapsItem.create(this.output.stack(), true);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (CountedIngredient input : this.inputs) {
            ingredients.add(input.ingredient());
        }
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.ROTARY_FURNACE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.ROTARY_FURNACE.get();
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    public boolean hasFluid() {
        return this.fluid != null && !this.fluid.isEmpty();
    }

    public HbmFluidStack fluidStack() {
        return hasFluid() ? new HbmFluidStack(this.fluid.fluid(), this.fluid.amount(), 0) : HbmFluidStack.EMPTY;
    }

    public ItemStack fluidIcon() {
        return hasFluid() ? FluidIconItem.forFluid(this.fluid.fluid()) : ItemStack.EMPTY;
    }

    public record CountedIngredient(Ingredient ingredient, int count) {
        private static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(CountedIngredient::count)
        ).apply(instance, CountedIngredient::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC = new StreamCodec<>() {
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
            return this.ingredient.test(stack) && stack.getCount() >= this.count;
        }
    }

    public record Input(ItemStack first, ItemStack second, ItemStack third) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> this.first;
                case 1 -> this.second;
                case 2 -> this.third;
                default -> ItemStack.EMPTY;
            };
        }

        @Override
        public int size() {
            return 3;
        }
    }

    public record FluidInput(HbmFluidDefinition fluid, int amount) {
        public static final FluidInput EMPTY = new FluidInput(HbmFluids.none(), 0);

        private static final Codec<FluidInput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("fluid").forGetter(stack -> stack.fluid().name()),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(FluidInput::amount)
        ).apply(instance, FluidInput::fromName));

        private static final StreamCodec<RegistryFriendlyByteBuf, FluidInput> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public FluidInput decode(RegistryFriendlyByteBuf buffer) {
                return fromName(buffer.readUtf(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidInput input) {
                buffer.writeUtf(input.fluid().name());
                buffer.writeVarInt(input.amount());
            }
        };

        public boolean isEmpty() {
            return this.fluid.isNone() || this.amount <= 0;
        }

        private static FluidInput fromName(String fluid, int amount) {
            return new FluidInput(HbmFluids.byName(fluid).orElse(HbmFluids.none()), amount);
        }
    }

    public record MaterialOutput(FoundryMaterial material, int amount) {
        private static final Codec<MaterialOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("material").xmap(
                        name -> FoundryMaterial.byName(name).orElseThrow(() -> new IllegalArgumentException("Unknown foundry material: " + name)),
                        FoundryMaterial::name
                ).forGetter(MaterialOutput::material),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(MaterialOutput::amount)
        ).apply(instance, MaterialOutput::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MaterialOutput> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public MaterialOutput decode(RegistryFriendlyByteBuf buffer) {
                FoundryMaterial material = FoundryMaterial.byName(buffer.readUtf())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown foundry material in packet"));
                return new MaterialOutput(material, buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, MaterialOutput output) {
                buffer.writeUtf(output.material().name());
                buffer.writeVarInt(output.amount());
            }
        };

        public FoundryMaterialStack stack() {
            return new FoundryMaterialStack(this.material, this.amount);
        }
    }

    public static class Serializer implements RecipeSerializer<RotaryFurnaceRecipe> {
        private static final MapCodec<RotaryFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(RotaryFurnaceRecipe::group),
                CountedIngredient.CODEC.listOf().fieldOf("inputs").forGetter(RotaryFurnaceRecipe::inputs),
                FluidInput.CODEC.optionalFieldOf("fluid", FluidInput.EMPTY).forGetter(RotaryFurnaceRecipe::fluid),
                MaterialOutput.CODEC.fieldOf("output").forGetter(RotaryFurnaceRecipe::output),
                Codec.intRange(1, 20_000).fieldOf("duration").forGetter(RotaryFurnaceRecipe::duration),
                Codec.intRange(0, 20_000).fieldOf("steam").forGetter(RotaryFurnaceRecipe::steam)
        ).apply(instance, RotaryFurnaceRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, RotaryFurnaceRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public RotaryFurnaceRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                int size = buffer.readVarInt();
                java.util.ArrayList<CountedIngredient> inputs = new java.util.ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    inputs.add(CountedIngredient.STREAM_CODEC.decode(buffer));
                }
                FluidInput fluid = FluidInput.STREAM_CODEC.decode(buffer);
                MaterialOutput output = MaterialOutput.STREAM_CODEC.decode(buffer);
                return new RotaryFurnaceRecipe(group, inputs, fluid, output, buffer.readVarInt(), buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, RotaryFurnaceRecipe recipe) {
                buffer.writeUtf(recipe.group());
                buffer.writeVarInt(recipe.inputs().size());
                for (CountedIngredient input : recipe.inputs()) {
                    CountedIngredient.STREAM_CODEC.encode(buffer, input);
                }
                FluidInput.STREAM_CODEC.encode(buffer, recipe.fluid());
                MaterialOutput.STREAM_CODEC.encode(buffer, recipe.output());
                buffer.writeVarInt(recipe.duration());
                buffer.writeVarInt(recipe.steam());
            }
        };

        @Override
        public MapCodec<RotaryFurnaceRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RotaryFurnaceRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
