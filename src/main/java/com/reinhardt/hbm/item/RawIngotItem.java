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

public class RawIngotItem extends Item {
    private static final String MATERIAL_ID = "material_id";
    private static final String MATERIAL = "material";
    private static final Set<String> LEGACY_RAW_INGOT_MATERIALS = Set.of(
            "redstone",
            "neodymium",
            "borax",
            "sodium",
            "strontium",
            "slag"
    );

    public RawIngotItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        FoundryMaterial material = material(stack);
        if (material == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.reinhardtshbm.ingot_raw", Component.translatable(material.translationKey()));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (FoundryMaterial material : FoundryMaterial.ordered()) {
            if (supports(material)) {
                output.accept(stackFor(this, material));
            }
        }
    }

    public static boolean supports(FoundryMaterial material) {
        return material != null && LEGACY_RAW_INGOT_MATERIALS.contains(material.name());
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
        ItemStack stack = new ItemStack(item, count);
        CompoundTag tag = new CompoundTag();
        tag.putInt(MATERIAL_ID, material.id());
        tag.putString(MATERIAL, material.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static int tint(ItemStack stack, int tintIndex) {
        if (!(stack.getItem() instanceof RawIngotItem rawIngot)) {
            return 0xFFFFFFFF;
        }
        FoundryMaterial material = rawIngot.material(stack);
        return material == null ? 0xFFFFFFFF : 0xFF000000 | material.moltenColor();
    }
}
