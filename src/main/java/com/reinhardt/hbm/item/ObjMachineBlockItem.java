package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.render.ObjMachineItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Block item that keeps placement vanilla while rendering the machine's complete OBJ assembly. */
public class ObjMachineBlockItem extends BlockItem {
    public ObjMachineBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        installRenderer(consumer);
    }

    public static void installRenderer(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final ObjMachineItemRenderer renderer = new ObjMachineItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }
}
