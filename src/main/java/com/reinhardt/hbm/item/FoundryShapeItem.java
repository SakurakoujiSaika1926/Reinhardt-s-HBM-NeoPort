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

import java.util.Set;

public class FoundryShapeItem extends Item {
    private static final String MATERIAL_ID = "material_id";
    private static final String MATERIAL = "material";
    private static final Set<String> CAST_PLATE_MATERIALS = Set.of(
            "iron",
            "gold",
            "schrabidium",
            "schrabidate",
            "titanium",
            "copper",
            "tungsten",
            "aluminium",
            "lead",
            "zirconium",
            "osmiridium",
            "steel",
            "advanced_alloy",
            "dura_steel",
            "desh",
            "starmetal",
            "ferrouranium",
            "tcalloy",
            "cdalloy",
            "bismuth_bronze",
            "arsenic_bronze",
            "combine_steel",
            "weaponsteel",
            "saturnite"
    );
    private static final Set<String> FINE_WIRE_MATERIALS = Set.of(
            "carbon",
            "gold",
            "schrabidium",
            "copper",
            "tungsten",
            "aluminium",
            "lead",
            "zirconium",
            "steel",
            "red_copper",
            "advanced_alloy",
            "magnetized_tungsten"
    );
    private static final Set<String> DENSE_WIRE_MATERIALS = Set.of(
            "gold",
            "schrabidium",
            "schrabidate",
            "titanium",
            "copper",
            "tungsten",
            "red_copper",
            "advanced_alloy",
            "starmetal",
            "dineutronium",
            "neodymium",
            "niobium",
            "bscco",
            "magnetized_tungsten"
    );
    private static final Set<String> WELDED_PLATE_MATERIALS = Set.of(
            "iron",
            "steel",
            "copper",
            "titanium",
            "zirconium",
            "aluminium",
            "tungsten",
            "tcalloy",
            "cdalloy",
            "combine_steel",
            "osmiridium"
    );
    private static final Set<String> SHELL_MATERIALS = Set.of(
            "titanium",
            "copper",
            "aluminium",
            "steel",
            "weaponsteel",
            "saturnite"
    );
    private static final Set<String> MECHANISM_MATERIALS = Set.of(
            "gunmetal",
            "weaponsteel",
            "saturnite"
    );

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
            if (material.behavior() == FoundryMaterial.SmeltingBehavior.SMELTABLE && supports(this.shape, material)) {
                output.accept(stackFor(this, material));
            }
        }
    }

    public FoundryMaterial material(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return FoundryMaterial.byId(tag.getInt(MATERIAL_ID))
                .or(() -> FoundryMaterial.byName(tag.getString(MATERIAL)))
                .orElse(null);
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
        String name = material.name();
        return switch (shape) {
            case CAST_PLATE -> CAST_PLATE_MATERIALS.contains(name);
            case WIRE -> FINE_WIRE_MATERIALS.contains(name);
            case DENSE_WIRE -> DENSE_WIRE_MATERIALS.contains(name);
            case WELDED_PLATE -> WELDED_PLATE_MATERIALS.contains(name);
            case SHELL -> SHELL_MATERIALS.contains(name);
            case MECHANISM -> MECHANISM_MATERIALS.contains(name);
            default -> true;
        };
    }

    private String itemName() {
        return switch (this.shape) {
            case CAST_PLATE -> "plate_cast";
            case WELDED_PLATE -> "plate_welded";
            case WIRE -> "wire_fine";
            case DENSE_WIRE -> "wire_dense";
            case MECHANISM -> "part_mechanism";
            default -> this.shape.key();
        };
    }
}
