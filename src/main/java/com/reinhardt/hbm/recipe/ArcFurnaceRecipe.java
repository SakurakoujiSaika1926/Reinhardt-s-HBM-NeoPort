package com.reinhardt.hbm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.item.ScrapsItem;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public record ArcFurnaceRecipe(
        String group,
        Ingredient input,
        int count,
        ItemStack solidOutput,
        List<MaterialOutput> liquidOutputs,
        int duration
) implements Recipe<ArcFurnaceRecipe.Input> {
    public ArcFurnaceRecipe {
        solidOutput = solidOutput.copy();
        liquidOutputs = List.copyOf(liquidOutputs);
    }

    @Override
    public boolean matches(Input input, Level level) {
        for (int index = 0; index < input.size(); index++) {
            ItemStack stack = input.getItem(index);
            if (!stack.isEmpty() && this.input.test(stack) && stack.getCount() >= this.count) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return solidOutput.isEmpty() && !liquidOutputs.isEmpty()
                ? ScrapsItem.create(liquidOutputs.getFirst().stack(), true)
                : solidOutput.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return assemble(Input.EMPTY, registries);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(input);
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HbmRecipeTypes.ARC_FURNACE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return HbmRecipeTypes.ARC_FURNACE.get();
    }

    public boolean hasSolidOutput() {
        return !solidOutput.isEmpty();
    }

    public boolean hasLiquidOutput() {
        return !liquidOutputs.isEmpty();
    }

    public List<FoundryMaterialStack> liquidStacks() {
        return liquidOutputs.stream().map(MaterialOutput::stack).toList();
    }

    public static List<FoundryMaterialStack> dynamicOutputs(ItemStack stack) {
        return FoundryMaterial.smeltingMaterialsFromItem(stack).stream()
                .filter(output -> output.material().behavior() == FoundryMaterial.SmeltingBehavior.SMELTABLE)
                .toList();
    }

    public static ItemStack dynamicSolidOutput(ItemStack stack, Level level) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        SingleRecipeInput input = new SingleRecipeInput(stack);
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, input, level)
                .map(holder -> holder.value().assemble(input, level.registryAccess()))
                .filter(output -> isArcSmeltable(stack, output))
                .map(ItemStack::copy)
                .orElse(ItemStack.EMPTY);
    }

    public static boolean isArcSmeltable(ItemStack input, ItemStack output) {
        return hasArcMaterialTag(input)
                || hasArcMaterialTag(output)
                || output.is(net.minecraft.world.item.Items.BRICK)
                || output.is(net.minecraft.world.item.Items.NETHER_BRICK);
    }

    private static boolean hasArcMaterialTag(ItemStack stack) {
        return !stack.isEmpty() && stack.getTags()
                .map(net.minecraft.tags.TagKey::location)
                .filter(tag -> tag.getNamespace().equals("c") || tag.getNamespace().equals("forge"))
                .map(net.minecraft.resources.ResourceLocation::getPath)
                .anyMatch(path -> path.equals("ingots")
                        || path.startsWith("ingots/")
                        || path.equals("ores")
                        || path.startsWith("ores/")
                        || path.equals("plates")
                        || path.startsWith("plates/"));
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
                return new MaterialOutput(
                        FoundryMaterial.byName(buffer.readUtf()).orElseThrow(),
                        buffer.readVarInt()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, MaterialOutput value) {
                buffer.writeUtf(value.material.name());
                buffer.writeVarInt(value.amount);
            }
        };

        public FoundryMaterialStack stack() {
            return new FoundryMaterialStack(material, amount);
        }
    }

    public record Input(List<ItemStack> stacks) implements RecipeInput {
        public static final Input EMPTY = new Input(List.of());

        @Override
        public ItemStack getItem(int index) {
            return index >= 0 && index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return stacks.size();
        }
    }

    public static class Serializer implements RecipeSerializer<ArcFurnaceRecipe> {
        private static final MapCodec<ArcFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ArcFurnaceRecipe::group),
                Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(ArcFurnaceRecipe::input),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ArcFurnaceRecipe::count),
                ItemStack.STRICT_CODEC.optionalFieldOf("solid", ItemStack.EMPTY).forGetter(ArcFurnaceRecipe::solidOutput),
                MaterialOutput.CODEC.listOf().optionalFieldOf("liquid", List.of()).forGetter(ArcFurnaceRecipe::liquidOutputs),
                Codec.intRange(1, 20_000).optionalFieldOf("duration", 400).forGetter(ArcFurnaceRecipe::duration)
        ).apply(instance, ArcFurnaceRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ArcFurnaceRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ArcFurnaceRecipe decode(RegistryFriendlyByteBuf buffer) {
                String group = buffer.readUtf();
                Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                int count = buffer.readVarInt();
                ItemStack solid = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                int size = buffer.readVarInt();
                List<MaterialOutput> liquid = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    liquid.add(MaterialOutput.STREAM_CODEC.decode(buffer));
                }
                return new ArcFurnaceRecipe(group, input, count, solid, liquid, buffer.readVarInt());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ArcFurnaceRecipe recipe) {
                buffer.writeUtf(recipe.group);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
                buffer.writeVarInt(recipe.count);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.solidOutput);
                buffer.writeVarInt(recipe.liquidOutputs.size());
                for (MaterialOutput output : recipe.liquidOutputs) {
                    MaterialOutput.STREAM_CODEC.encode(buffer, output);
                }
                buffer.writeVarInt(recipe.duration);
            }
        };

        @Override
        public MapCodec<ArcFurnaceRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ArcFurnaceRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
