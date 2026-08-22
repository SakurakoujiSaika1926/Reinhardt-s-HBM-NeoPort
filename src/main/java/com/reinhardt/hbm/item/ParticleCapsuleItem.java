package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ParticleCapsuleItem extends Item {
    private final boolean returnsEmptyCapsule;

    public ParticleCapsuleItem(Properties properties, boolean returnsEmptyCapsule) {
        super(properties);
        this.returnsEmptyCapsule = returnsEmptyCapsule;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return this.returnsEmptyCapsule;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
        return this.returnsEmptyCapsule ? new ItemStack(HbmItems.PARTICLE_EMPTY.get()) : ItemStack.EMPTY;
    }
}
