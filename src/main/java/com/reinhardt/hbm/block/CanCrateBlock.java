package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.LegacyConserveItem;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
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

public final class CanCrateBlock extends Block {
    private static final List<String> EXTRA_CANS = List.of(
            "can_smart", "can_creature", "can_redbomb", "can_mrsugar", "can_overcharge",
            "can_luna", "can_breen", "can_bepis", "pudding"
    );

    public CanCrateBlock(Properties properties) {
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
            List<ItemStack> pool = createPool();
            List<ItemStack> drops = new ArrayList<>();
            if (!pool.isEmpty()) {
                int count = 5 + level.random.nextInt(4);
                for (int i = 0; i < count; i++) {
                    drops.add(pool.get(level.random.nextInt(pool.size())).copy());
                }
            }
            CrateBlockSupport.open(level, pos, drops);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static List<ItemStack> createPool() {
        List<ItemStack> pool = new ArrayList<>();
        for (LegacyConserveItem.Variant variant : LegacyConserveItem.Variant.values()) {
            pool.add(LegacyConserveItem.stackFor(HbmItems.CANNED_CONSERVE.get(), variant));
        }
        for (String id : EXTRA_CANS) {
            ItemStack stack = CrateBlockSupport.stack(id);
            if (!stack.isEmpty()) {
                pool.add(stack);
            }
        }
        return pool;
    }
}
