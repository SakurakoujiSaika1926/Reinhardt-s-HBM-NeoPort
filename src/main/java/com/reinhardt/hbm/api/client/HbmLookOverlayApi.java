package com.reinhardt.hbm.api.client;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side extension point for HBM's crosshair look overlay.
 *
 * <p>Providers only describe overlay contents. HBM owns the render event and is
 * the sole renderer, so add-ons get the same placement and styling as built-in
 * machines. Providers are queried by descending priority and the first non-null
 * result is rendered.</p>
 */
public final class HbmLookOverlayApi {
    public static final int COLOR_WHITE = 0xFFFFFF;
    public static final int COLOR_GREEN = 0x00FF00;
    public static final int COLOR_RED = 0xFF5555;
    public static final int COLOR_YELLOW = 0xFFFF00;

    private static final Map<ResourceLocation, RegisteredProvider> PROVIDERS_BY_ID = new LinkedHashMap<>();
    private static final Set<ResourceLocation> FAILED_PROVIDERS = ConcurrentHashMap.newKeySet();
    private static volatile List<RegisteredProvider> providers = List.of();
    private static long registrationOrder;

    private HbmLookOverlayApi() {
    }

    /**
     * Registers a provider at the default priority of zero.
     *
     * @throws IllegalStateException when the id is already registered
     */
    public static void registerProvider(ResourceLocation id, Provider provider) {
        registerProvider(id, 0, provider);
    }

    /**
     * Registers a provider. Higher priorities are queried first; equal
     * priorities retain registration order.
     *
     * @throws IllegalStateException when the id is already registered
     */
    public static synchronized void registerProvider(ResourceLocation id, int priority, Provider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        if (PROVIDERS_BY_ID.containsKey(id)) {
            throw new IllegalStateException("Look-overlay provider already registered: " + id);
        }

        RegisteredProvider registered = new RegisteredProvider(id, priority, registrationOrder++, provider);
        PROVIDERS_BY_ID.put(id, registered);
        ArrayList<RegisteredProvider> next = new ArrayList<>(PROVIDERS_BY_ID.values());
        next.sort(Comparator.comparingInt(RegisteredProvider::priority)
                .reversed()
                .thenComparingLong(RegisteredProvider::order));
        providers = List.copyOf(next);
    }

    /**
     * Resolves the first overlay supplied for the current block hit.
     */
    @Nullable
    public static OverlayData findOverlay(Level level, BlockHitResult hit) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(hit, "hit");
        for (RegisteredProvider registered : providers) {
            try {
                OverlayData data = registered.provider().getOverlay(level, hit);
                if (data != null) {
                    return data;
                }
            } catch (RuntimeException exception) {
                if (FAILED_PROVIDERS.add(registered.id())) {
                    ReinhardtsHBM.LOGGER.error(
                            "Look-overlay provider {} failed; other providers will still be queried",
                            registered.id(),
                            exception
                    );
                }
            }
        }
        return null;
    }

    public static OverlayData overlay(Component title, List<OverlayLine> lines) {
        return new OverlayData(title, COLOR_YELLOW, lines);
    }

    public static OverlayLine line(String text, int color) {
        return new OverlayLine(text, color);
    }

    public static OverlayLine whiteLine(String text) {
        return OverlayLine.white(text);
    }

    public static String fluidText(String prefix, HbmFluidTank tank) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(tank, "tank");
        String name = Component.translatable(tank.type().translationKey()).getString();
        return prefix + name + ": " + tank.amount() + "/" + tank.capacity() + "mB";
    }

    public static OverlayLine fluidLine(String prefix, HbmFluidTank tank) {
        return OverlayLine.white(fluidText(prefix, tank));
    }

    public static String fluidNameText(String prefix, HbmFluidTank tank) {
        Objects.requireNonNull(prefix, "prefix");
        Objects.requireNonNull(tank, "tank");
        return prefix + Component.translatable(tank.type().translationKey()).getString();
    }

    public static OverlayLine fluidNameLine(String prefix, HbmFluidTank tank) {
        return OverlayLine.white(fluidNameText(prefix, tank));
    }

    public static String energyText(String prefix, long energy) {
        return Objects.requireNonNull(prefix, "prefix") + String.format(Locale.ROOT, "%,d HE", energy);
    }

    public static OverlayLine energyLine(String prefix, long energy, int color) {
        return new OverlayLine(energyText(prefix, energy), color);
    }

    public static String energyStorageText(String prefix, long energy, long capacity) {
        return Objects.requireNonNull(prefix, "prefix")
                + String.format(Locale.ROOT, "%,d / %,d HE", energy, capacity);
    }

    public static OverlayLine energyStorageLine(String prefix, long energy, long capacity, int color) {
        return new OverlayLine(energyStorageText(prefix, energy, capacity), color);
    }

    public static OverlayLine energyPercentLine(long energy, long capacity) {
        double percent = capacity <= 0L ? 0.0D : (double) energy * 100.0D / (double) capacity;
        int color = ((int) (0xFF - 0xFF * percent / 100.0D)) << 16
                | ((int) (0xFF * percent / 100.0D) << 8);
        return new OverlayLine(String.format(Locale.ROOT, "%.2f%%", percent), color);
    }

    public static String energyRateText(String prefix, long energyPerTick) {
        return Objects.requireNonNull(prefix, "prefix")
                + String.format(Locale.ROOT, "%,d HE/t", energyPerTick);
    }

    public static OverlayLine energyRateLine(String prefix, long energyPerTick, int color) {
        return new OverlayLine(energyRateText(prefix, energyPerTick), color);
    }

    public static String heatText(String prefix, long heat) {
        return Objects.requireNonNull(prefix, "prefix") + String.format(Locale.ROOT, "%,d TU", heat);
    }

    public static OverlayLine heatLine(String prefix, long heat) {
        return OverlayLine.white(heatText(prefix, heat));
    }

    public static OverlayLine heatLine(String prefix, long heat, int color) {
        return new OverlayLine(heatText(prefix, heat), color);
    }

    @FunctionalInterface
    public interface Provider {
        /**
         * Returns null when this provider does not own the hit.
         */
        @Nullable
        OverlayData getOverlay(Level level, BlockHitResult hit);
    }

    public record OverlayData(Component title, int titleColor, List<OverlayLine> lines) {
        public OverlayData {
            Objects.requireNonNull(title, "title");
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        }
    }

    public record OverlayLine(String text, int color) {
        public OverlayLine {
            Objects.requireNonNull(text, "text");
        }

        public static OverlayLine white(String text) {
            return new OverlayLine(text, COLOR_WHITE);
        }
    }

    private record RegisteredProvider(ResourceLocation id, int priority, long order, Provider provider) {
    }
}
