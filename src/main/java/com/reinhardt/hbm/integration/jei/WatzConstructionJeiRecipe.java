package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record WatzConstructionJeiRecipe(
        List<ItemStack> materials,
        ItemStack tool,
        ItemStack core,
        ItemStack result
) {
    public static WatzConstructionJeiRecipe create() {
        return new WatzConstructionJeiRecipe(
                List.of(
                        new ItemStack(HbmBlocks.WATZ_END.get(), 48),
                        new ItemStack(HbmItems.BOLT_DURA_STEEL.get(), 64),
                        new ItemStack(HbmItems.BOLT_DURA_STEEL.get(), 64),
                        new ItemStack(HbmItems.BOLT_DURA_STEEL.get(), 64),
                        new ItemStack(HbmBlocks.WATZ_ELEMENT.get(), 36),
                        new ItemStack(HbmBlocks.WATZ_COOLER.get(), 26)
                ),
                new ItemStack(HbmItems.BOLTGUN.get()),
                new ItemStack(HbmBlocks.STRUCT_WATZ_CORE.get()),
                new ItemStack(HbmBlocks.WATZ.get())
        );
    }
}
