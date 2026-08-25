package com.reinhardt.hbm.drone;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The old ModulePatternMatcher modes used by logistics requester filters. */
public record DroneItemMatcher(ItemStack pattern, String mode) {
    public static final String EXACT = "exact";
    public static final String WILDCARD = "wildcard";

    public DroneItemMatcher {
        pattern = pattern.copy();
        if (mode == null || mode.isBlank()) {
            mode = EXACT;
        }
    }

    public boolean matches(ItemStack candidate) {
        if (pattern.isEmpty() || candidate.isEmpty()) {
            return false;
        }
        if (EXACT.equals(mode)) {
            return ItemStack.isSameItemSameComponents(pattern, candidate);
        }
        if (WILDCARD.equals(mode)) {
            return pattern.getItem() == candidate.getItem();
        }
        ResourceLocation expected = ResourceLocation.tryParse(mode);
        if (expected == null) {
            return false;
        }
        return candidate.getTags().map(TagKey::location).anyMatch(expected::equals);
    }

    public static String defaultMode(ItemStack stack) {
        return EXACT;
    }
}
