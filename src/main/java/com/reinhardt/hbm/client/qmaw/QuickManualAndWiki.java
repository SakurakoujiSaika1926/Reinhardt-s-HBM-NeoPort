package com.reinhardt.hbm.client.qmaw;

import net.minecraft.world.item.ItemStack;

import java.util.Map;

/** One resource-pack supplied QMAW page. */
public record QuickManualAndWiki(
        String name,
        ItemStack icon,
        Map<String, String> titles,
        Map<String, String> contents,
        boolean noIndex
) {
    public QuickManualAndWiki {
        icon = icon == null ? ItemStack.EMPTY : icon.copy();
        titles = Map.copyOf(titles);
        contents = Map.copyOf(contents);
    }

    public String title(String language) {
        return localized(titles, language, "Missing Localization!");
    }

    public String content(String language) {
        return localized(contents, language, "Missing Localization!");
    }

    private static String localized(Map<String, String> values, String language, String fallback) {
        String exact = values.get(language);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(language)) {
                return entry.getValue();
            }
        }
        String english = values.get("en_US");
        if (english != null) {
            return english;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("en_US")) {
                return entry.getValue();
            }
        }
        return fallback;
    }
}
