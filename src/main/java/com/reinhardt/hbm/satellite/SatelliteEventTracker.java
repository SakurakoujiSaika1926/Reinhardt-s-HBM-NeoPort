package com.reinhardt.hbm.satellite;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ephemeral event stores used by the detector and narrow-band scanning
 * satellites.  The 1.7.10 implementation also kept these events in static
 * memory rather than world saves.
 */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class SatelliteEventTracker {
    public static final int DETECTOR_LOW_DURATION = 15 * 20;
    public static final int DETECTOR_MEDIUM_DURATION = 20 / 2;
    public static final int DETECTOR_HIGH_DURATION = 60 * 20;

    private static final double LOW_INACCURACY = 10_000.0D;
    private static final double MEDIUM_INACCURACY = 2_500.0D;
    private static final double HIGH_INACCURACY = 500.0D;

    private static final Map<ResourceKey<Level>, List<RadiationBurst>> RADIATION = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, LinkedHashMap<BlockPos, RayEvent>> RAYS = new ConcurrentHashMap<>();

    private SatelliteEventTracker() {
    }

    public static void reportRadiation(ServerLevel level, int lifetime, BurstIntensity intensity,
                                       double x, double z) {
        double inaccuracy = switch (intensity) {
            case LOW -> LOW_INACCURACY;
            case MEDIUM -> MEDIUM_INACCURACY;
            case HIGH -> HIGH_INACCURACY;
        };
        int reportedX = (int) Math.floor(x)
                + (int) (Mth.clamp(level.random.nextGaussian(), -1.0D, 1.0D) * inaccuracy);
        int reportedZ = (int) Math.floor(z)
                + (int) (Mth.clamp(level.random.nextGaussian(), -1.0D, 1.0D) * inaccuracy);
        List<RadiationBurst> bursts = RADIATION.computeIfAbsent(level.dimension(), ignored -> new ArrayList<>());
        purgeRadiation(level, bursts);
        bursts.add(new RadiationBurst(level.getGameTime() + lifetime, intensity, reportedX, reportedZ));
    }

    public static List<RadiationBurst> radiationBursts(ServerLevel level) {
        List<RadiationBurst> bursts = RADIATION.computeIfAbsent(level.dimension(), ignored -> new ArrayList<>());
        purgeRadiation(level, bursts);
        return List.copyOf(bursts);
    }

    public static void reportRay(ServerLevel level, BlockPos position, String info, int lifetime) {
        LinkedHashMap<BlockPos, RayEvent> events = RAYS.computeIfAbsent(
                level.dimension(), ignored -> new LinkedHashMap<>());
        purgeRays(level, events);
        events.put(position.immutable(), new RayEvent(
                level.getGameTime() + lifetime, info, position.getX(), position.getZ()));
    }

    public static List<RayEvent> rayEvents(ServerLevel level, int centerX, int centerZ, int range) {
        LinkedHashMap<BlockPos, RayEvent> events = RAYS.computeIfAbsent(
                level.dimension(), ignored -> new LinkedHashMap<>());
        purgeRays(level, events);
        long rangeSquared = (long) range * range;
        return events.values().stream().filter(event -> {
            long deltaX = (long) event.x() - centerX;
            long deltaZ = (long) event.z() - centerZ;
            return deltaX * deltaX + deltaZ * deltaZ <= rangeSquared;
        }).toList();
    }

    private static void purgeRadiation(ServerLevel level, List<RadiationBurst> bursts) {
        long now = level.getGameTime();
        bursts.removeIf(burst -> now > burst.expiresOn());
    }

    private static void purgeRays(ServerLevel level, LinkedHashMap<BlockPos, RayEvent> events) {
        long now = level.getGameTime();
        events.entrySet().removeIf(entry -> now > entry.getValue().expiresOn());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            RADIATION.remove(level.dimension());
            RAYS.remove(level.dimension());
        }
    }

    public enum BurstIntensity {
        LOW,
        MEDIUM,
        HIGH
    }

    public record RadiationBurst(long expiresOn, BurstIntensity intensity, int x, int z) {
    }

    public record RayEvent(long expiresOn, String info, int x, int z) {
        public static final String INFO_ARC_FLASH = "ARC_FLASH";
        public static final String INFO_NUCLEAR = "NEUTRON_EMISSION";
        public static final String INFO_PARTICLE = "HIGH_ENERGY_PARTICLES";
        public static final String INFO_RADAR = "RADAR_WAVES";
        public static final String INFO_RADIO = "RADIO_WAVES";
    }
}
