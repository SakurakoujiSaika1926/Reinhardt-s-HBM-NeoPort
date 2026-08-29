package com.reinhardt.hbm.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Construction boltgun retained because legacy multiblock assembly requires it. */
public final class BoltgunItem extends Item {
    public BoltgunItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!ToolConversion.isConvertible(context.getLevel(), context.getClickedPos(), ToolConversion.Tool.BOLT)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        return ToolConversion.convert(context.getLevel(), context.getClickedPos(), context.getPlayer(), ToolConversion.Tool.BOLT)
                ? InteractionResult.CONSUME
                : InteractionResult.PASS;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }
}
