package com.reinhardt.hbm.item;

import com.reinhardt.hbm.config.HbmConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 1.7.10 RTG pellet state. Depletion belongs to the pellet stack because a
 * generator may hold pellets with different remaining lifetimes.
 */
public final class RtgPelletItem extends Item {
    private static final String DEPLETION_TAG = "rtg_depletion";
    private static final String DEPLETED_MATERIAL_TAG = "rtg_depleted_material";

    private final int heat;
    private final int decayingHeat;
    private final long maxLifespan;
    private final String depletedMaterial;
    private final Supplier<? extends Item> depletedPellet;

    public RtgPelletItem(
            int heat,
            int decayingHeat,
            long maxLifespan,
            String depletedMaterial,
            Supplier<? extends Item> depletedPellet
    ) {
        super(new Properties().stacksTo(1));
        this.heat = Math.max(0, heat);
        this.decayingHeat = Math.max(0, decayingHeat);
        this.maxLifespan = Math.max(1L, maxLifespan);
        this.depletedMaterial = Objects.requireNonNull(depletedMaterial, "depletedMaterial");
        this.depletedPellet = Objects.requireNonNull(depletedPellet, "depletedPellet");
    }

    public int heat(ItemStack stack) {
        int baseHeat = HbmConfig.rtgDecay() ? this.decayingHeat : this.heat;
        if (!HbmConfig.scaleRtgPower()) {
            return baseHeat;
        }
        return (int) Math.ceil(baseHeat * (double) remainingLifespan(stack) / this.maxLifespan);
    }

    public long remainingLifespan(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(DEPLETION_TAG) ? Math.max(0L, tag.getLong(DEPLETION_TAG)) : this.maxLifespan;
    }

    /** Advances the exact old per-tick pellet depletion state. */
    public ItemStack burnTick(ItemStack stack) {
        if (!HbmConfig.rtgDecay()) {
            return stack;
        }
        long remaining = remainingLifespan(stack);
        // ItemRTGPellet.handleDecay() first permits the zero-lifetime tick,
        // then replaces the pellet on the following tick.
        if (remaining <= 0L) {
            return depletedStack();
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(DEPLETION_TAG, remaining - 1L);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private ItemStack depletedStack() {
        ItemStack depleted = new ItemStack(this.depletedPellet.get());
        CompoundTag tag = depleted.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(DEPLETED_MATERIAL_TAG, this.depletedMaterial);
        depleted.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return depleted;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return remainingLifespan(stack) < this.maxLifespan;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * remainingLifespan(stack) / this.maxLifespan);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x47C35A;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc"));
        tooltip.add(Component.translatable("item.reinhardtshbm.rtg_pellet.heat", heat(stack)));
        tooltip.add(Component.translatable("item.reinhardtshbm.rtg_pellet.decays_to",
                depletedStack().getHoverName()));
        tooltip.add(Component.translatable("item.reinhardtshbm.rtg_pellet.remaining", remainingLifespan(stack), this.maxLifespan));
    }
}
