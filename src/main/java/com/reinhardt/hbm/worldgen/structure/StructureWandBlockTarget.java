package com.reinhardt.hbm.worldgen.structure;

import com.reinhardt.hbm.block.ConcreteColoredBlock;
import com.reinhardt.hbm.block.FilingCabinetBlock;
import com.reinhardt.hbm.block.LegacyVariantBlock;
import com.reinhardt.hbm.block.VinylTileBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;

public final class StructureWandBlockTarget {
    private StructureWandBlockTarget() {
    }

    public static HbmStructureIO.LegacyBlockKey key(Block block, ItemStack stack) {
        return HbmStructureIO.keyFromBlock(block, legacyMeta(block, stack));
    }

    public static int legacyMeta(Block block, ItemStack stack) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (block instanceof ConcreteColoredBlock || block instanceof VinylTileBlock) {
            return data.copyTag().getInt(ConcreteColoredBlock.META_TAG);
        }

        if (block instanceof LegacyVariantBlock) {
            if (data.copyTag().contains("variant_index")) {
                return data.copyTag().getInt("variant_index");
            }
            CustomModelData modelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            return modelData == null ? 0 : modelData.value();
        }

        if (block instanceof FilingCabinetBlock) {
            return data.copyTag().getInt("variant");
        }

        CompoundMeta meta = fromCustomData(data);
        if (meta.present()) {
            return meta.value();
        }
        return Math.max(0, stack.getDamageValue());
    }

    private static CompoundMeta fromCustomData(CustomData data) {
        var tag = data.copyTag();
        if (tag.contains("meta")) {
            return new CompoundMeta(true, tag.getInt("meta"));
        }
        if (tag.contains("variant")) {
            try {
                return new CompoundMeta(true, Integer.parseInt(tag.getString("variant")));
            } catch (NumberFormatException ignored) {
                return new CompoundMeta(false, 0);
            }
        }
        return new CompoundMeta(false, 0);
    }

    private record CompoundMeta(boolean present, int value) {
    }
}
