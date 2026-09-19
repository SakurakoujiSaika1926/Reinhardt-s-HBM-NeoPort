package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.item.WatzPelletItem;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

public record WatzJeiRecipe(ItemStack input, ItemStack output) {
    public static List<WatzJeiRecipe> createAll() {
        return Arrays.stream(WatzPelletItem.Type.values())
                .map(type -> new WatzJeiRecipe(
                        WatzPelletItem.stack(type, HbmItems.WATZ_PELLET.get()),
                        WatzPelletItem.stack(type, HbmItems.WATZ_PELLET_DEPLETED.get())
                ))
                .toList();
    }
}
