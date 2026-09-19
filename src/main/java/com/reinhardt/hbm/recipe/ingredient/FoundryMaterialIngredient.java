package com.reinhardt.hbm.recipe.ingredient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialShapes;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.item.FoundryShapeItem;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public final class FoundryMaterialIngredient implements ICustomIngredient {
    private static final Codec<FoundryShape> SHAPE_CODEC = Codec.STRING.comapFlatMap(
            key -> Arrays.stream(FoundryShape.values())
                    .filter(shape -> shape.key().equals(key))
                    .findFirst()
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown foundry material shape '" + key + "'")),
            FoundryShape::key
    );

    public static final MapCodec<FoundryMaterialIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            HolderSetCodec.create(Registries.ITEM, BuiltInRegistries.ITEM.holderByNameCodec(), false)
                    .fieldOf("items")
                    .forGetter(FoundryMaterialIngredient::items),
            SHAPE_CODEC.fieldOf("shape").forGetter(FoundryMaterialIngredient::shape),
            Codec.STRING.fieldOf("material").forGetter(FoundryMaterialIngredient::materialName)
    ).apply(instance, FoundryMaterialIngredient::new));

    private final HolderSet<Item> items;
    private final FoundryShape shape;
    private final String materialName;

    public FoundryMaterialIngredient(HolderSet<Item> items, FoundryShape shape, String materialName) {
        this.items = items;
        this.shape = shape;
        this.materialName = materialName;
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty() || !this.items.contains(stack.getItemHolder())) {
            return false;
        }
        if (!(stack.getItem() instanceof FoundryShapeItem shapeItem) || shapeItem.shape() != this.shape) {
            return false;
        }
        FoundryMaterial expected = material();
        return expected != null && shapeItem.material(stack) == expected;
    }

    @Override
    public Stream<ItemStack> getItems() {
        FoundryMaterial material = material();
        if (material == null || !FoundryMaterialShapes.supports(this.shape, material)) {
            return Stream.empty();
        }
        return this.items.stream()
                .map(holder -> holder.value())
                .filter(item -> item instanceof FoundryShapeItem shapeItem && shapeItem.shape() == this.shape)
                .map(item -> FoundryShapeItem.stackFor(item, material));
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return HbmRecipeTypes.FOUNDRY_MATERIAL_INGREDIENT.get();
    }

    public HolderSet<Item> items() {
        return this.items;
    }

    public FoundryShape shape() {
        return this.shape;
    }

    public String materialName() {
        return this.materialName;
    }

    private FoundryMaterial material() {
        return FoundryMaterial.byName(this.materialName).orElse(null);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof FoundryMaterialIngredient that)) {
            return false;
        }
        return Objects.equals(this.items, that.items)
                && this.shape == that.shape
                && Objects.equals(this.materialName, that.materialName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.items, this.shape, this.materialName);
    }
}
