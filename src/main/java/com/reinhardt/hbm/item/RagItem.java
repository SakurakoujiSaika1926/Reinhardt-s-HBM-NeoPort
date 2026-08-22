package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Direct port of ItemRag: water soaks dropped rags and use creates a used rag. */
public final class RagItem extends Item {
    public RagItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity itemEntity) {
        Level level = itemEntity.level();
        BlockPos pos = itemEntity.blockPosition();
        if (!level.isClientSide && level.getFluidState(pos).is(FluidTags.WATER)) {
            itemEntity.setItem(new ItemStack(HbmItems.RAG_DAMP.get(), stack.getCount()));
            return true;
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            stack.shrink(1);
            ItemStack usedRag = new ItemStack(HbmItems.RAG_PISS.get());
            if (!player.getInventory().add(usedRag)) {
                player.drop(usedRag, false);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
