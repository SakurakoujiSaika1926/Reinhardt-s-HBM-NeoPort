package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.VendingMachineBlock;
import com.reinhardt.hbm.item.LegacyConserveItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stateless tile entity retained for the old render volume and reward pools. */
public final class VendingMachineBlockEntity extends BlockEntity {
    private static final Reward[] SODA = {
            new Reward(() -> stack("bottle_nuka"), 10),
            new Reward(() -> stack("bottle_cherry"), 5),
            new Reward(() -> stack("bottle_quantum"), 1),
            new Reward(() -> stack("can_bepis"), 10),
            new Reward(() -> stack("can_luna"), 10),
            new Reward(() -> stack("can_mug"), 10),
            new Reward(() -> stack("can_breen"), 1)
    };
    private static final Reward[] SNACKS = {
            new Reward(() -> new ItemStack(HbmItems.DEFINITELYFOOD.get()), 10),
            new Reward(() -> LegacyConserveItem.stackFor(HbmItems.CANNED_CONSERVE.get(), LegacyConserveItem.Variant.BEEF), 5),
            new Reward(() -> LegacyConserveItem.stackFor(HbmItems.CANNED_CONSERVE.get(), LegacyConserveItem.Variant.TUBE), 5),
            new Reward(() -> new ItemStack(HbmItems.TWINKIE.get()), 10),
            new Reward(() -> new ItemStack(HbmItems.CHOCOLATE.get()), 10)
    };

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.VENDING_MACHINE.get(), pos, state);
    }

    public boolean snacks() {
        return getBlockState().getValue(VendingMachineBlock.VARIANT) == 1;
    }

    public ItemStack dispense(RandomSource random) {
        Reward[] rewards = snacks() ? SNACKS : SODA;
        int total = 0;
        for (Reward reward : rewards) {
            total += reward.weight();
        }
        int selected = random.nextInt(total);
        for (Reward reward : rewards) {
            selected -= reward.weight();
            if (selected < 0) {
                return reward.stack().get();
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private record Reward(java.util.function.Supplier<ItemStack> stack, int weight) {
    }
}
