package com.reinhardt.hbm.radiation;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight, source-local shielding for radiation emitters whose old 1.7.10
 * behavior could be contained by a shell.  This deliberately does not rebuild
 * the old per-section resistance field; each source samples a few rays and the
 * result is cached briefly.
 */
public final class RadiationShielding {
    private static final TagKey<Block> RADIATION_SHIELDS = TagKey.create(
            Registries.BLOCK,
            ReinhardtsHBM.id("radiation_shields"));
    private static final TagKey<Block> LEAD_STORAGE_BLOCKS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/lead"));

    private static final int CONTAINMENT_CACHE_TICKS = 80;
    private static final int CONTAINMENT_TRACE_STEPS = 18;
    private static final int DIRECT_TRACE_MAX_STEPS = 128;
    private static final int INVALIDATE_RADIUS = CONTAINMENT_TRACE_STEPS + 2;
    private static final double CONTAINMENT_ATTENUATION_SCALE = 90.0D;
    private static final double MIN_RELEVANT_FACTOR = 1.0E-4D;

    private static final int[][] CONTAINMENT_DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            {0, 1, 0}, {0, -1, 0},
            {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
            {1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1},
            {1, 1, 1}, {1, 1, -1}, {-1, 1, 1}, {-1, 1, -1},
            {1, -1, 1}, {1, -1, -1}, {-1, -1, 1}, {-1, -1, -1}
    };

    private static final Map<ContainmentKey, CachedContainment> CONTAINMENT_CACHE = new ConcurrentHashMap<>();

    private RadiationShielding() {
    }

    public static void incrementContainedRadiation(ServerLevel level, BlockPos source, double amount) {
        incrementContainedRadiation(level, source, amount, HbmRadiationConstants.CHUNK_RADIATION_MAX);
    }

    public static void incrementContainedRadiation(ServerLevel level, BlockPos source, double amount, double max) {
        if (!Double.isFinite(amount) || amount == 0.0D) {
            return;
        }
        double contained = amount * containmentFactor(level, source);
        if (Math.abs(contained) < HbmRadiationConstants.RAD_EPSILON) {
            return;
        }
        ChunkRadiationData.get(level).incrementRadiation(source, contained, max);
    }

    public static float attenuateDirectDose(ServerLevel level, BlockPos source, LivingEntity target, float dose) {
        if (dose <= 0.0F) {
            return 0.0F;
        }
        Vec3 from = Vec3.atCenterOf(source);
        Vec3 to = target.position().add(0.0D, target.getEyeHeight(), 0.0D);
        return attenuateDirectDose(level, from, to, dose);
    }

    public static float attenuateDirectDose(ServerLevel level, Vec3 from, Vec3 to, float dose) {
        if (dose <= 0.0F) {
            return 0.0F;
        }
        RayShield shield = trace(level, from, to, DIRECT_TRACE_MAX_STEPS);
        if (shield.hardBlocked()) {
            return 0.0F;
        }
        double resistance = Math.max(1.0D, shield.resistance());
        double attenuated = dose / resistance;
        return attenuated < 1.0E-6D ? 0.0F : (float) attenuated;
    }

    public static void invalidate(ServerLevel level, BlockPos pos) {
        if (CONTAINMENT_CACHE.isEmpty()) {
            return;
        }
        ResourceKey<Level> dimension = level.dimension();
        CONTAINMENT_CACHE.keySet().removeIf(key -> key.dimension().equals(dimension)
                && Math.abs(BlockPos.getX(key.packedPos()) - pos.getX()) <= INVALIDATE_RADIUS
                && Math.abs(BlockPos.getY(key.packedPos()) - pos.getY()) <= INVALIDATE_RADIUS
                && Math.abs(BlockPos.getZ(key.packedPos()) - pos.getZ()) <= INVALIDATE_RADIUS);
    }

    public static void invalidateAll(ServerLevel level) {
        if (CONTAINMENT_CACHE.isEmpty()) {
            return;
        }
        ResourceKey<Level> dimension = level.dimension();
        CONTAINMENT_CACHE.keySet().removeIf(key -> key.dimension().equals(dimension));
    }

    private static double containmentFactor(ServerLevel level, BlockPos source) {
        long now = level.getGameTime();
        ContainmentKey key = new ContainmentKey(level.dimension(), source.asLong());
        CachedContainment cached = CONTAINMENT_CACHE.get(key);
        if (cached != null && cached.expiresAt() >= now) {
            return cached.factor();
        }
        double factor = calculateContainmentFactor(level, source);
        CONTAINMENT_CACHE.put(key, new CachedContainment(factor, now + CONTAINMENT_CACHE_TICKS));
        return factor;
    }

    private static double calculateContainmentFactor(ServerLevel level, BlockPos source) {
        Vec3 center = Vec3.atCenterOf(source);
        double total = 0.0D;
        for (int[] direction : CONTAINMENT_DIRECTIONS) {
            RayShield shield = traceDirection(level, center, direction);
            if (shield.hardBlocked()) {
                continue;
            }
            double factor = Math.exp(-shield.resistance() / CONTAINMENT_ATTENUATION_SCALE);
            total += factor < MIN_RELEVANT_FACTOR ? 0.0D : factor;
        }
        return total / CONTAINMENT_DIRECTIONS.length;
    }

    private static RayShield traceDirection(ServerLevel level, Vec3 from, int[] direction) {
        double length = Math.sqrt(direction[0] * direction[0] + direction[1] * direction[1] + direction[2] * direction[2]);
        double stepX = direction[0] / length;
        double stepY = direction[1] / length;
        double stepZ = direction[2] / length;
        double resistance = 0.0D;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int step = 1; step <= CONTAINMENT_TRACE_STEPS; step++) {
            cursor.set(
                    (int) Math.floor(from.x + stepX * step),
                    (int) Math.floor(from.y + stepY * step),
                    (int) Math.floor(from.z + stepZ * step));
            if (!level.isLoaded(cursor)) {
                return new RayShield(false, resistance);
            }
            BlockState state = level.getBlockState(cursor);
            ShieldResult shield = shieldResult(state);
            if (shield.hardBlocked()) {
                return new RayShield(true, 0.0D);
            }
            resistance += shield.resistance();
        }
        return new RayShield(false, resistance);
    }

    private static RayShield trace(ServerLevel level, Vec3 from, Vec3 to, int maxSteps) {
        Vec3 delta = to.subtract(from);
        double distance = delta.length();
        if (distance <= 1.0E-6D) {
            return new RayShield(false, 1.0D);
        }
        Vec3 direction = delta.scale(1.0D / distance);
        int steps = Math.min(maxSteps, Math.max(1, (int) Math.ceil(distance)));
        double resistance = 1.0D;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int step = 1; step < steps; step++) {
            cursor.set(
                    (int) Math.floor(from.x + direction.x * step),
                    (int) Math.floor(from.y + direction.y * step),
                    (int) Math.floor(from.z + direction.z * step));
            if (!level.isLoaded(cursor)) {
                continue;
            }
            BlockState state = level.getBlockState(cursor);
            ShieldResult shield = shieldResult(state);
            if (shield.hardBlocked()) {
                return new RayShield(true, 0.0D);
            }
            resistance += shield.resistance();
        }
        return new RayShield(false, resistance);
    }

    private static ShieldResult shieldResult(BlockState state) {
        if (state.isAir()) {
            return ShieldResult.NONE;
        }
        if (isLeadHardShield(state)) {
            return ShieldResult.HARD_BLOCKED;
        }

        double resistance = Math.max(0.0D, state.getBlock().getExplosionResistance());
        if (state.is(RADIATION_SHIELDS)) {
            resistance = Math.max(resistance, 180.0D);
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath();
        if (path.contains("cmb")) {
            resistance = Math.max(resistance, 500.0D);
        } else if (path.contains("ducrete") || path.contains("concrete_super")) {
            resistance = Math.max(resistance, 300.0D);
        } else if (path.contains("reinforced")) {
            resistance = Math.max(resistance, 220.0D);
        }
        return resistance <= 0.0D ? ShieldResult.NONE : new ShieldResult(false, resistance);
    }

    private static boolean isLeadHardShield(BlockState state) {
        if (state.is(LEAD_STORAGE_BLOCKS)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return ReinhardtsHBM.MOD_ID.equals(id.getNamespace())
                && ("block_lead".equals(id.getPath()) || "glass_lead".equals(id.getPath()));
    }

    private record ContainmentKey(ResourceKey<Level> dimension, long packedPos) {
    }

    private record CachedContainment(double factor, long expiresAt) {
    }

    private record RayShield(boolean hardBlocked, double resistance) {
    }

    private record ShieldResult(boolean hardBlocked, double resistance) {
        private static final ShieldResult NONE = new ShieldResult(false, 0.0D);
        private static final ShieldResult HARD_BLOCKED = new ShieldResult(true, 0.0D);
    }
}
