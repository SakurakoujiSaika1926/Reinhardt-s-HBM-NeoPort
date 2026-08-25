package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.screen.ClayTabletScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

/** ItemClayTablet's stack-local clue seed and portable pedestal-recipe display. */
public final class LegacyClayTabletItem extends Item {
    public static final String SEED = "tabletSeed";

    public LegacyClayTabletItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && !data(stack).contains(SEED)) {
            CompoundTag data = data(stack);
            data.putLong(SEED, player.getRandom().nextLong());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        if (level.isClientSide && FMLEnvironment.dist.isClient()) {
            ClayTabletScreen.open(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static long seed(ItemStack stack) {
        return data(stack).getLong(SEED);
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
