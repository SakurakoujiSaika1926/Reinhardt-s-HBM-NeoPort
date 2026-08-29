package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.CrashedBombBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Keeps the old metadata variants while rendering the actual dud OBJ in every item context. */
public final class CrashedBombBlockItem extends LegacyVariantBlockItem {
    public CrashedBombBlockItem(Block block, Properties properties) {
        super(block, properties, CrashedBombBlock.VARIANT, "block.reinhardtshbm.crashed_bomb",
                "balefire", "conventional", "nuke", "salted");
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }
}
