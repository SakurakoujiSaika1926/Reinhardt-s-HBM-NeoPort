package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyVariantBlockItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Latest-1.7.10 ICF construction bill: 407 placed components plus post-processing materials. */
public record IcfConstructionJeiRecipe(
        List<List<ItemStack>> materials,
        List<ItemStack> tools,
        ItemStack core,
        ItemStack result
) {
    public static IcfConstructionJeiRecipe create() {
        return new IcfConstructionJeiRecipe(
                List.of(
                        List.of(component(0, 50)),
                        List.of(component(3, 240)),
                        List.of(new ItemStack(HbmItems.BOLT_DURA_STEEL.get(), 960)),
                        List.of(material("plate_cast_steel", 240)),
                        List.of(component(1, 117)),
                        List.of(material("plate_cast_bismuth_bronze", 117),
                                material("plate_cast_arsenic_bronze", 117))
                ),
                List.of(new ItemStack(HbmItems.BLOWTORCH.get()), new ItemStack(HbmItems.BOLTGUN.get())),
                new ItemStack(HbmBlocks.STRUCT_ICF_CORE.get()),
                new ItemStack(HbmBlocks.ICF.get())
        );
    }

    private static ItemStack component(int variant, int count) {
        LegacyVariantBlockItem item = (LegacyVariantBlockItem) HbmBlocks.ICF_COMPONENT.get().asItem();
        ItemStack stack = LegacyVariantBlockItem.stackFor(item, variant);
        stack.setCount(count);
        return stack;
    }

    private static ItemStack material(String path, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(path)), count);
    }
}
