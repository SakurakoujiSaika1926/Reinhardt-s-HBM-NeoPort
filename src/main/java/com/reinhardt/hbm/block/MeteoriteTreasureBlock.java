package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.HbmStructureLoot;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

/**
 * The 1.7.10 treasure meteorite block: mining it performs one to three
 * independent rolls against {@code POOL_METEORITE_TREASURE}.
 */
public final class MeteoriteTreasureBlock extends Block {
    private static final String TREASURE_POOL = "POOL_METEORITE_TREASURE";

    public MeteoriteTreasureBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (hasSilkTouch(params, tool)) {
            return List.of(new ItemStack(this));
        }

        RandomSource random = params.getLevel().getRandom();
        int rollCount = 1 + random.nextInt(3);
        List<ItemStack> drops = new ArrayList<>(rollCount);
        for (int i = 0; i < rollCount; i++) {
            ItemStack drop = HbmStructureLoot.randomStack(TREASURE_POOL, random);
            if (!drop.isEmpty()) {
                drops.add(drop);
            }
        }

        // 1.7.10 selected every loot stack first, then independently applied
        // the explosion survival chance to each selected stack.
        Float explosionRadius = params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
        if (explosionRadius != null) {
            float survivalChance = 1.0F / explosionRadius;
            drops.removeIf(drop -> random.nextFloat() > survivalChance);
        }
        return drops;
    }

    private static boolean hasSilkTouch(LootParams.Builder params, ItemStack tool) {
        if (tool == null || tool.isEmpty()) {
            return false;
        }
        return EnchantmentHelper.getItemEnchantmentLevel(
                params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.SILK_TOUCH),
                tool) > 0;
    }
}
