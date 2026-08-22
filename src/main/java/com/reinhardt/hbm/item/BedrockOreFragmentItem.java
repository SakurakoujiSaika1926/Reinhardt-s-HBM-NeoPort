package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class BedrockOreFragmentItem extends Item {
    private static final String MATERIAL_ID = "material_id";
    private static final String MATERIAL = "material";

    private static final List<MaterialRef> MATERIALS = List.of(
            material(2600, "iron"),
            material(2900, "copper"),
            material(2200, "titanium"),
            material(2902, "bauxite"),
            material(2903, "cryolite"),
            material(1701, "chlorocalcite"),
            material(300, "lithium"),
            material(1100, "sodium"),
            material(7400, "tungsten"),
            material(8200, "lead"),
            material(7900, "gold"),
            material(400, "beryllium"),
            material(8300, "bismuth"),
            material(7300, "tantalium"),
            material(2700, "cobalt"),
            material(20000, "rareearth"),
            material(500, "boron"),
            material(5700, "lanthanium"),
            material(4100, "niobium"),
            material(6000, "neodymium"),
            material(3800, "strontium"),
            material(4000, "zirconium"),
            material(9200, "uranium"),
            material(9032, "thorium"),
            material(8826, "radium"),
            material(8410, "polonium"),
            material(4399, "technetium"),
            material(9238, "u238"),
            material(600, "coal"),
            material(1600, "sulfur"),
            material(601, "lignite"),
            material(700, "kno"),
            material(900, "fluorite"),
            material(1500, "phosphorus"),
            material(1400, "silicon"),
            material(1, "redstone"),
            material(8001, "cinnabar"),
            material(1101, "sodalite"),
            material(1401, "asbestos"),
            material(1430, "diamond"),
            material(401, "emerald"),
            material(501, "borax"),
            material(1702, "molysite")
    );

    public BedrockOreFragmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        MaterialRef material = material(stack);
        return Component.translatable("item.reinhardtshbm.bedrock_ore_fragment", Component.translatable("hbmmat." + material.name()));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        creativeVariants().forEach(output::accept);
    }

    public static List<ItemStack> creativeVariants() {
        return MATERIALS.stream()
                .map(material -> stackFor(HbmItems.BEDROCK_ORE_FRAGMENT, material, 1))
                .toList();
    }

    public static ItemStack stackFor(Supplier<? extends Item> item, int materialId, String material, int count) {
        return stackFor(item, new MaterialRef(materialId, material), count);
    }

    private static ItemStack stackFor(Supplier<? extends Item> item, MaterialRef material, int count) {
        ItemStack stack = new ItemStack(item.get(), count);
        CompoundTag tag = new CompoundTag();
        tag.putInt(MATERIAL_ID, material.id());
        tag.putString(MATERIAL, material.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private MaterialRef material(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int id = tag.getInt(MATERIAL_ID);
        String name = tag.getString(MATERIAL);
        for (MaterialRef material : MATERIALS) {
            if (material.id() == id || material.name().equals(name)) {
                return material;
            }
        }
        return name == null || name.isBlank() ? MATERIALS.getFirst() : new MaterialRef(id, name.toLowerCase(Locale.ROOT));
    }

    private static MaterialRef material(int id, String name) {
        return new MaterialRef(id, name);
    }

    private record MaterialRef(int id, String name) {
    }
}
