package com.reinhardt.hbm.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Fixed-color replacement for the old sixteen-state ItemChemicalDye metadata item. */
public final class ChemicalDyeItem extends Item {
    private static final int[] COLORS = {
            1973019, 11743532, 3887386, 5320730, 2437522, 8073150, 2651799, 11250603,
            4408131, 14188952, 4312372, 14602026, 6719955, 12801229, 15435844, 15790320
    };

    private final int colorIndex;

    public ChemicalDyeItem(Properties properties, int colorIndex) {
        super(properties);
        this.colorIndex = Math.clamp(colorIndex, 0, COLORS.length - 1);
    }

    public static int tint(ItemStack stack, int tintIndex) {
        if (tintIndex != 1 || !(stack.getItem() instanceof ChemicalDyeItem dye)) {
            return 0xFFFFFFFF;
        }
        return 0xFF000000 | COLORS[dye.colorIndex];
    }
}
