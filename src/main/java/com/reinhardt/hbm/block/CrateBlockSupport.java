package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

final class CrateBlockSupport {
    private CrateBlockSupport() {
    }

    static boolean isCrowbar(ItemStack stack) {
        return stack.is(HbmItems.CROWBAR.get());
    }

    static ItemStack stack(String id) {
        return stack(id, 1);
    }

    static ItemStack stack(String id, int count) {
        ResourceLocation key = ReinhardtsHBM.id(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(key);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    static void open(Level level, BlockPos pos, List<ItemStack> drops) {
        for (ItemStack stack : drops) {
            drop(level, pos, stack);
        }
        level.removeBlock(pos, false);
        level.playSound(null, pos, HbmSoundEvents.CRATE_BREAK.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        double x = pos.getX() + level.random.nextFloat() * 0.8F + 0.1F;
        double y = pos.getY() + level.random.nextFloat() * 0.8F + 0.1F;
        double z = pos.getZ() + level.random.nextFloat() * 0.8F + 0.1F;
        ItemEntity entity = new ItemEntity(level, x, y, z, stack.copy());
        entity.setDeltaMovement(
                level.random.nextGaussian() * 0.05D,
                level.random.nextGaussian() * 0.05D + 0.2D,
                level.random.nextGaussian() * 0.05D
        );
        level.addFreshEntity(entity);
    }
}
