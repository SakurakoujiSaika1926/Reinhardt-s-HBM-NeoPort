package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.item.StandardAmmoItem;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

public final class AmmoCrateBlock extends Block {
    public AmmoCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!CrateBlockSupport.isCrowbar(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            CrateBlockSupport.open(level, pos, createDrops(level.random));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static List<ItemStack> createDrops(RandomSource random) {
        List<ItemStack> drops = new ArrayList<>();
        add(drops, CrateBlockSupport.stack("cap_nuka", 12 + random.nextInt(21)));
        add(drops, CrateBlockSupport.stack("syringe_metal_stimpak", 1 + random.nextInt(3)));
        maybeAddAmmo(drops, random, StandardAmmoItem.StandardAmmoType.P9_SP, 16, 17);
        maybeAddAmmo(drops, random, StandardAmmoItem.StandardAmmoType.P9_FMJ, 16, 17);
        maybeAddAmmo(drops, random, StandardAmmoItem.StandardAmmoType.R556_SP, 16, 17);
        maybeAddAmmo(drops, random, StandardAmmoItem.StandardAmmoType.R556_FMJ, 16, 17);
        maybeAddAmmo(drops, random, StandardAmmoItem.StandardAmmoType.ROCKET_HE, 2, 3);
        if (random.nextInt(10) == 0) {
            add(drops, CrateBlockSupport.stack("syringe_metal_super", 2));
        }
        return drops;
    }

    private static void maybeAddAmmo(
            List<ItemStack> drops,
            RandomSource random,
            StandardAmmoItem.StandardAmmoType type,
            int minimum,
            int range
    ) {
        if (!random.nextBoolean()) {
            return;
        }
        ItemStack stack = LegacyVariantItem.stackFor(HbmItems.AMMO_STANDARD.get(), type.id());
        stack.setCount(minimum + random.nextInt(range));
        drops.add(stack);
    }

    private static void add(List<ItemStack> drops, ItemStack stack) {
        if (!stack.isEmpty()) {
            drops.add(stack);
        }
    }
}
