package com.reinhardt.hbm.item;

import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryShape;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class FoundryShapeItem extends Item {
    private static final String MATERIAL_ID = "material_id";
    private static final String MATERIAL = "material";
    private final FoundryShape shape;

    public FoundryShapeItem(Properties properties, FoundryShape shape) {
        super(properties);
        this.shape = shape;
    }

    @Override
    public Component getName(ItemStack stack) {
        FoundryMaterial material = material(stack);
        if (material == null) {
            return super.getName(stack);
        }
        return Component.translatable(
                "item.reinhardtshbm." + itemName(),
                Component.translatable(material.translationKey())
        );
    }

    public FoundryShape shape() {
        return this.shape;
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (FoundryMaterial material : FoundryMaterial.ordered()) {
            if (supports(this.shape, material)) {
                output.accept(stackFor(this, material));
            }
        }
    }

    public FoundryMaterial material(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(MATERIAL_ID)) {
            var materialById = FoundryMaterial.byId(tag.getInt(MATERIAL_ID));
            if (materialById.isPresent() && supports(this.shape, materialById.get())) {
                return materialById.get();
            }
        }
        var materialByName = FoundryMaterial.byName(tag.getString(MATERIAL));
        if (materialByName.isPresent() && supports(this.shape, materialByName.get())) {
            return materialByName.get();
        }
        return null;
    }

    public static ItemStack stackFor(Item item, FoundryMaterial material) {
        return stackFor(item, material, 1);
    }

    public static ItemStack stackFor(Item item, FoundryMaterial material, int count) {
        ItemStack stack = new ItemStack(item);
        stack.setCount(count);
        CompoundTag tag = new CompoundTag();
        tag.putInt(MATERIAL_ID, material.id());
        tag.putString(MATERIAL, material.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static boolean supports(FoundryShape shape, FoundryMaterial material) {
        if (material == null) {
            return false;
        }
        return com.reinhardt.hbm.foundry.FoundryMaterialShapes.supports(shape, material);
    }

    private String itemName() {
        return this.shape.key();
    }
}
