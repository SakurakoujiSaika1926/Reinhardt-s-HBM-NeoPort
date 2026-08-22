package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
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

public record CrucibleRecipe(
        String group,
        int frequency,
        ItemStack icon,
        List<MaterialIngredient> input,
        List<MaterialIngredient> output
) implements Recipe<CrucibleRecipe.Input> {
    public CrucibleRecipe {
        input = List.copyOf(input);
        output = List.copyOf(output);
    }

    @Override
    public boolean matches(Input input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.icon.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.CRUCIBLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.CRUCIBLE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    public int inputAmount() {
        return this.input.stream().mapToInt(MaterialIngredient::amount).sum();
    }

    public int quantaIn(List<FoundryMaterialStack> stacks, FoundryMaterial material) {
        int sum = 0;
        for (FoundryMaterialStack stack : stacks) {
            if (stack.material() == material) {
                sum += stack.amount();
            }
        }
        return sum;
    }

    public boolean references(FoundryMaterial material) {
        return this.input.stream().anyMatch(entry -> entry.material() == material)
                || this.output.stream().anyMatch(entry -> entry.material() == material);
    }

    public record MaterialIngredient(FoundryMaterial material, int amount) {
        private static final Codec<MaterialIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("material").xmap(
                        name -> FoundryMaterial.byName(name).orElseThrow(() -> new IllegalArgumentException("Unknown foundry material: " + name)),
                        FoundryMaterial::name
                ).forGetter(MaterialIngredient::material),
                Codec.intRange(1, 1_000_000).fieldOf("amount").forGetter(MaterialIngredient::amount)
        ).apply(instance, MaterialIngredient::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MaterialIngredient> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public MaterialIngredient decode(RegistryFriendlyByteBuf buffer) {
                FoundryMaterial material = FoundryMaterial.byName(buffer.readUtf())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown foundry material in packet"));
                return new MaterialIngredient(material, buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, MaterialIngredient ingredient) {
                buffer.writeUtf(ingredient.material().name());
                buffer.writeVarInt(ingredient.amount());
            }
        };

        public FoundryMaterialStack stack() {
            return new FoundryMaterialStack(this.material, this.amount);
        }
    }

    public record Input() implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }
    }

    public static class Serializer implements RecipeSerializer<CrucibleRecipe> {
        private static final MapCodec<CrucibleRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(CrucibleRecipe::group),
                Codec.intRange(1, 20_000).fieldOf("frequency").forGetter(CrucibleRecipe::frequency),
                ItemStack.STRICT_CODEC.fieldOf("icon").forGetter(CrucibleRecipe::icon),
                MaterialIngredient.CODEC.listOf().fieldOf("input").forGetter(CrucibleRecipe::input),
                MaterialIngredient.CODEC.listOf().fieldOf("output").forGetter(CrucibleRecipe::output)
        ).apply(instance, CrucibleRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public CrucibleRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                int frequency = buffer.readVarInt();
                ItemStack icon = ItemStack.STREAM_CODEC.decode(buffer);
                List<MaterialIngredient> input = readList(buffer);
                List<MaterialIngredient> output = readList(buffer);
                return new CrucibleRecipe(group, frequency, icon, input, output);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, CrucibleRecipe recipe) {
                buffer.writeUtf(recipe.group());
                buffer.writeVarInt(recipe.frequency());
                ItemStack.STREAM_CODEC.encode(buffer, recipe.icon());
                writeList(buffer, recipe.input());
                writeList(buffer, recipe.output());
            }

            private List<MaterialIngredient> readList(RegistryFriendlyByteBuf buffer) {
                int size = buffer.readVarInt();
                List<MaterialIngredient> values = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    values.add(MaterialIngredient.STREAM_CODEC.decode(buffer));
                }
                return List.copyOf(values);
            }

            private void writeList(RegistryFriendlyByteBuf buffer, List<MaterialIngredient> values) {
                buffer.writeVarInt(values.size());
                for (MaterialIngredient value : values) {
                    MaterialIngredient.STREAM_CODEC.encode(buffer, value);
                }
            }
        };

        @Override
        public MapCodec<CrucibleRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
