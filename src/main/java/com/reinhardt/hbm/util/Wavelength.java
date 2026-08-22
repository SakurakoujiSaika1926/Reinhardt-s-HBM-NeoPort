package com.reinhardt.hbm.util;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum Wavelength implements StringRepresentable {
    NULL("null", "wavelengths.name.null", "wavelengths.waveRange.null", 0x010101, 0x010101, ChatFormatting.WHITE),
    IR("ir", "wavelengths.name.ir", "wavelengths.waveRange.ir", 0xBB1010, 0xCC4040, ChatFormatting.RED),
    VISIBLE("visible", "wavelengths.name.visible", "wavelengths.waveRange.visible", 0, 0, ChatFormatting.GREEN),
    UV("uv", "wavelengths.name.uv", "wavelengths.waveRange.uv", 0x0A1FC4, 0x00EFFF, ChatFormatting.AQUA),
    GAMMA("gamma", "wavelengths.name.gamma", "wavelengths.waveRange.gamma", 0x150560, 0xEF00FF, ChatFormatting.LIGHT_PURPLE),
    DRX("drx", "wavelengths.name.drx", "wavelengths.waveRange.drx", 0xFF0000, 0xFF0000, ChatFormatting.DARK_RED);

    private final String serializedName;
    private final String translationKey;
    private final String rangeKey;
    private final int renderedBeamColor;
    private final int guiColor;
    private final ChatFormatting textColor;

    Wavelength(String serializedName, String translationKey, String rangeKey, int renderedBeamColor, int guiColor, ChatFormatting textColor) {
        this.serializedName = serializedName;
        this.translationKey = translationKey;
        this.rangeKey = rangeKey;
        this.renderedBeamColor = renderedBeamColor;
        this.guiColor = guiColor;
        this.textColor = textColor;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public String rangeKey() {
        return this.rangeKey;
    }

    public int renderedBeamColor() {
        return this.renderedBeamColor;
    }

    public int guiColor(long gameTime) {
        if (this == VISIBLE) {
            return java.awt.Color.HSBtoRGB(gameTime / 50.0F, 0.5F, 1.0F) & 0xFFFFFF;
        }
        return this.guiColor;
    }

    public ChatFormatting textColor() {
        return this.textColor;
    }

    public static Wavelength byName(String name) {
        if (name == null || name.isBlank()) {
            return NULL;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        for (Wavelength wavelength : values()) {
            if (wavelength.serializedName.equals(normalized) || wavelength.name().equalsIgnoreCase(name)) {
                return wavelength;
            }
        }
        return NULL;
    }
}
