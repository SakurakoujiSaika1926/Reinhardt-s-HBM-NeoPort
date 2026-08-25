package com.reinhardt.hbm.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Direct port of ItemLootCrate's per-rarity reroll selection. */
public final class LegacyLootCrateItem extends Item {
    private final MissilePartItem.LootPool pool;

    public LegacyLootCrateItem(MissilePartItem.LootPool pool) {
        super(new Properties().stacksTo(1));
        this.pool = pool;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack crate = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(crate, true);
        }

        Item reward = choose(level);
        if (reward == null) {
            return InteractionResultHolder.fail(crate);
        }
        ItemStack stack = new ItemStack(reward);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        if (!player.getAbilities().instabuild) {
            crate.shrink(1);
        }
        return InteractionResultHolder.consume(crate);
    }

    private Item choose(Level level) {
        List<MissilePartItem> candidates = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof MissilePartItem part && part.belongsToLegacyLootPool(this.pool)) {
                candidates.add(part);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }

        // The original implementation retries indefinitely until the rarity roll succeeds.
        while (true) {
            MissilePartItem candidate = candidates.get(level.random.nextInt(candidates.size()));
            if (candidate.winsLegacyLootRoll(level.random)) {
                return candidate;
            }
        }
    }
}
