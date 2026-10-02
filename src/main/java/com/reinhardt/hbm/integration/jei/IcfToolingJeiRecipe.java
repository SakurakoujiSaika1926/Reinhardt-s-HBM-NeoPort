package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyVariantBlockItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record IcfToolingJeiRecipe(
        ItemStack input,
        List<List<ItemStack>> materials,
        ItemStack tool,
        ItemStack output
) {
    public static List<IcfToolingJeiRecipe> createAll() {
        return List.of(
                new IcfToolingJeiRecipe(
                        component(1),
                        List.of(List.of(material("plate_cast_bismuth_bronze", 1),
                                material("plate_cast_arsenic_bronze", 1))),
                        new ItemStack(HbmItems.BLOWTORCH.get()),
                        component(2)
                ),
                new IcfToolingJeiRecipe(
                        component(3),
                        List.of(
                                List.of(material("plate_cast_steel", 1)),
                                List.of(new ItemStack(HbmItems.BOLT_DURA_STEEL.get(), 4))
                        ),
                        new ItemStack(HbmItems.BOLTGUN.get()),
                        component(4)
                )
        );
    }

    private static ItemStack component(int variant) {
        LegacyVariantBlockItem item = (LegacyVariantBlockItem) HbmBlocks.ICF_COMPONENT.get().asItem();
        return LegacyVariantBlockItem.stackFor(item, variant);
    }

    private static ItemStack material(String path, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(path)), count);
    }
}
