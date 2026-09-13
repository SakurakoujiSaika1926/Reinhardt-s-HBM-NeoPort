package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.FilingCabinetBlock;
import com.reinhardt.hbm.client.render.FilingCabinetItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Item subtype for the green and steel legacy filing cabinets. */
public final class FilingCabinetBlockItem extends ObjMachineBlockItem {
    private static final String VARIANT = "variant";

    public FilingCabinetBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public int variant(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        return Math.max(0, Math.min(1, tag.getInt(VARIANT)));
    }

    public static ItemStack stackFor(FilingCabinetBlockItem item, int variant) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = new CompoundTag();
        tag.putInt(VARIANT, Math.max(0, Math.min(1, variant)));
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        return stack;
    }

    public void addCreativeVariants(net.minecraft.world.item.CreativeModeTab.Output output) {
        output.accept(stackFor(this, 0));
        output.accept(stackFor(this, 1));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new FilingCabinetItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.reinhardtshbm.filing_cabinet." + (variant(stack) == 1 ? "steel" : "green"));
    }

    @Override
    protected net.minecraft.world.level.block.state.BlockState getPlacementState(net.minecraft.world.item.context.BlockPlaceContext context) {
        net.minecraft.world.level.block.state.BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(FilingCabinetBlock.MATERIAL, variant(context.getItemInHand()));
    }
}
