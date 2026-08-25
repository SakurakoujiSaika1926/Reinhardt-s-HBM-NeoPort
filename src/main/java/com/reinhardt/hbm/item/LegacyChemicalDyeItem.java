package com.reinhardt.hbm.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/** Full sixteen-state ItemChemicalDye port, including its second render pass tint. */
public final class LegacyChemicalDyeItem extends LegacyVariantItem {
    private static final int[] COLORS = {
            1973019, 11743532, 3887386, 5320730, 2437522, 8073150, 2651799, 11250603,
            4408131, 14188952, 4312372, 14602026, 6719955, 12801229, 15435844, 15790320
    };
    private static final String[] NAMES = {
            "black", "red", "green", "brown", "blue", "purple", "cyan", "silver",
            "gray", "pink", "lime", "yellow", "lightblue", "magenta", "orange", "white"
    };

    public LegacyChemicalDyeItem(Properties properties) {
        super(properties, "chemical_dye", variants(NAMES));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm.chemical_dye." + NAMES[variantIndex(stack)]);
    }

    public static int tint(ItemStack stack, int tintIndex) {
        if (tintIndex != 1 || !(stack.getItem() instanceof LegacyChemicalDyeItem dye)) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | COLORS[dye.variantIndex(stack)];
    }

    private int variantIndex(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String id = tag.getString("variant");
        if (!id.isBlank()) {
            for (int index = 0; index < NAMES.length; index++) {
                if (NAMES[index].equals(id)) {
                    return index;
                }
            }
        }
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return Math.clamp(data == null ? 0 : data.value(), 0, NAMES.length - 1);
    }
}
