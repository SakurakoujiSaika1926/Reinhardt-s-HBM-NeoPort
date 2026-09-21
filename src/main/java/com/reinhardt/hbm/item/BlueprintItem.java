package com.reinhardt.hbm.item;

import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class BlueprintItem extends Item {
    public static final String POOL_PREFIX_ALT = "alt.";
    public static final String POOL_PREFIX_DISCOVER = "discover.";
    public static final String POOL_PREFIX_SECRET = "secret.";
    public static final String POOL_PREFIX_528 = "528.";

    private static final String POOL_TAG = "pool";
    private static final List<String> CREATIVE_POOLS = List.of(
            POOL_PREFIX_ALT + ".xenonoxy",
            POOL_PREFIX_DISCOVER,
            POOL_PREFIX_ALT + ".birkeland",
            POOL_PREFIX_528 + "plastic",
            POOL_PREFIX_528 + "hardplastic",
            POOL_PREFIX_528 + "tcalloy",
            POOL_PREFIX_528 + "chlorophyte",
            POOL_PREFIX_528 + "chip_quantum"
    );

    public BlueprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        Optional<String> pool = pool(stack);
        if (level.isClientSide || pool.isEmpty() || pool.get().startsWith(POOL_PREFIX_SECRET)) {
            return InteractionResultHolder.pass(stack);
        }

        boolean creative = player.getAbilities().instabuild;
        if (!creative && !player.getInventory().contains(new ItemStack(Items.PAPER))) {
            return InteractionResultHolder.pass(stack);
        }

        if (!creative && !consumePaper(player)) {
            return InteractionResultHolder.pass(stack);
        }

        ItemStack copy = stack.copy();
        copy.setCount(1);
        if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
        player.swing(usedHand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Optional<String> pool = pool(stack);
        if (pool.isEmpty()) {
            return;
        }
        if (pool.get().startsWith(POOL_PREFIX_SECRET)) {
            tooltip.add(Component.translatable("item.reinhardtshbm.blueprints.cannot_copy").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.reinhardtshbm.blueprints.copy").withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("item.reinhardtshbm.blueprints.pool", pool.get()).withStyle(ChatFormatting.GRAY));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (String pool : CREATIVE_POOLS) {
            output.accept(stackFor(pool));
        }
    }

    public static boolean isBlueprint(ItemStack stack) {
        return !stack.isEmpty() && stack.is(HbmItems.BLUEPRINTS.get());
    }

    public static Optional<String> pool(ItemStack stack) {
        if (!isBlueprint(stack)) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(POOL_TAG)) {
            return Optional.empty();
        }
        String pool = tag.getString(POOL_TAG);
        return pool.isBlank() ? Optional.empty() : Optional.of(pool);
    }

    /**
     * Legacy {@code GenericRecipe#setPools528} only attached its blueprint pool while 528 mode was
     * enabled. Datapack recipes keep that pool metadata so the same recipe can be inspected on both
     * sides of a config reload, therefore the visibility check must preserve the legacy conditional
     * behavior here instead of treating every {@code 528.*} pool as an unconditional requirement.
     */
    public static boolean isRecipeVisibleForPool(List<String> recipePools, Optional<String> installedPool) {
        if (recipePools.isEmpty()) {
            return true;
        }
        if (!HbmConfig.ENABLE_528_MODE.get()
                && recipePools.stream().allMatch(pool -> pool.startsWith(POOL_PREFIX_528))) {
            return true;
        }
        return installedPool.filter(recipePools::contains).isPresent();
    }

    public static ItemStack stackFor(String pool) {
        ItemStack stack = new ItemStack(HbmItems.BLUEPRINTS.get());
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(POOL_TAG, pool);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(modelData(pool)));
        return stack;
    }

    private static int modelData(String pool) {
        if (pool.startsWith(POOL_PREFIX_DISCOVER)) {
            return 1;
        }
        if (pool.startsWith(POOL_PREFIX_SECRET)) {
            return 2;
        }
        if (pool.startsWith(POOL_PREFIX_528)) {
            return 3;
        }
        return 0;
    }

    private static boolean consumePaper(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.PAPER)) {
                player.getInventory().removeItem(slot, 1);
                return true;
            }
        }
        return false;
    }
}
