package com.reinhardt.hbm.explosion;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.advancement.HbmAdvancements;
import com.reinhardt.hbm.entity.NukeTorexEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.radiation.HbmRadiationWorlds;
import com.reinhardt.hbm.worldgen.NuclearFalloutTerrainEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class NukeExplosionManager {
    private static final int BOY_RADIUS = 120;
    private static final int MISSILE_RADIUS = 100;
    private static final int MK5_RAY_STEPS_PER_TICK = 3_072;
    private static final int MK5_BLOCK_UPDATES_PER_TICK = 256;
    private static final int MK5_DESTROY_BATCH_SIZE = 2_048;
    private static final long MK5_RAY_NANOS_PER_TICK = 500_000L;
    private static final long MK5_BLOCK_UPDATE_NANOS_PER_TICK = 500_000L;
    private static final int DAMAGE_TICKS = 1;
    private static final long CACHE_MISS = Long.MIN_VALUE;
    private static final long SAMPLE_AIR = 1L << 32;
    private static final long SAMPLE_FLUID_EMPTY = 1L << 33;
    private static final Map<net.minecraft.resources.ResourceLocation, Queue<NukeTask>> TASKS = new HashMap<>();
    private static final Map<net.minecraft.resources.ResourceLocation, Map<Long, Long>> FIELD_DISTURBERS = new HashMap<>();
    private static final ExecutorService PLANNER = Executors.newFixedThreadPool(
            Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 2)),
            task -> {
                Thread thread = new Thread(task, "RHbm-NukePlanner");
                thread.setDaemon(true);
                return thread;
            }
    );

    private NukeExplosionManager() {
    }

    public static void scheduleLittleBoy(ServerLevel level, double x, double y, double z) {
        scheduleLegacyNuke(level, x, y, z, BOY_RADIUS);
    }

    public static void scheduleMissileNuke(ServerLevel level, double x, double y, double z) {
        scheduleLegacyNuke(level, x, y, z, MISSILE_RADIUS);
    }

    public static void scheduleLegacyNuke(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, true, true, true);
    }

    /** 1.7.10 EntityMissileMicro: PARAMS_HIGH delegates to MK5 directly and
     * does not create the ordinary missile Torex. MK5 still creates fallout. */
    public static void scheduleMicroNuke(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, false, true, true);
    }

    /** 1.7.10 EntityMissileSchrabidium: MK3 Fleija explosion.  The Fleija
     * cloud is emitted explicitly; no standard Torex is substituted. */
    public static void scheduleFleijaNuke(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, false, false, false);
        sendFleijaCloud(level, x, y, z, radius);
    }

    /** EntityNukeExplosionMK5.statFac (ordinary nuclear/TX warheads). */
    public static void scheduleMk5Nuclear(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, true, true, true);
    }

    /** EntityNukeExplosionMK5.statFacNoRad used by custom N2. */
    public static void scheduleMk5NoRadiation(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, true, false, false);
    }

    /** EntityMissileMirv doubles missileRadius and uses the normal MK5/Torex pair. */
    public static void scheduleMirv(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius * 2, true, true, true);
    }

    /** EntityMissileDoomsday: MK5.moreFallout(100). */
    public static void scheduleDoomsday(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, true, true, true);
        NuclearFalloutTerrainEffects.scheduleDeferred(level, BlockPos.containing(x, y, z), 100, 20 * 20);
    }

    /** EntityMissileDoomsdayRusted uses missileRadius (not doubled) and the
     * same extra fallout modifier. */
    public static void scheduleRustedDoomsday(ServerLevel level, double x, double y, double z, int radius) {
        scheduleInternal(level, x, y, z, radius, true, true, true);
        NuclearFalloutTerrainEffects.scheduleDeferred(level, BlockPos.containing(x, y, z), 100, 20 * 20);
    }

    private static void scheduleInternal(ServerLevel level, double x, double y, double z, int radius,
                                          boolean spawnTorex, boolean deferredFallout, boolean radiation) {
        HbmAdvancements.awardAll(level, "manhattan");
        TASKS.computeIfAbsent(level.dimension().location(), unused -> new ArrayDeque<>())
                .add(new NukeTask(new Vec3(x, y, z), radius * 2, radius, radiation));
        playInitialSound(level, x, y, z);
        if (spawnTorex) {
            spawnTorex(level, x, y + 0.5D, z, radius);
        }
        sendInitialParticles(level, x, y, z);
        if (deferredFallout) {
            NuclearFalloutTerrainEffects.scheduleDeferred(level, BlockPos.containing(x, y, z), (int) (radius * 2.5D), 20 * 20);
        }
    }

    private static void sendFleijaCloud(ServerLevel level, double x, double y, double z, int radius) {
        int count = Math.max(64, radius * 10);
        level.sendParticles(com.reinhardt.hbm.registry.HbmParticleTypes.LEGACY_CLOUD.get(),
                x, y, z, count, radius * 0.05D, radius * 0.05D, radius * 0.05D, 0.35D);
    }

    /**
     * Applies the old custom-nuke stage priority before entering the bounded
     * destruction queue. The world mutation remains on the server thread, so
     * this method is safe to call from a block entity update.
     */
    public static void scheduleCustomNuke(ServerLevel level, double x, double y, double z,
                                          float tnt, float nuke, float hydro, float amat,
                                          float dirty, float schrab, float euph) {
        dirty = Math.min(dirty, 100.0F);
        if (euph > 0.0F) {
            BalefireExplosionManager.schedule(level, BlockPos.containing(x, y, z), 150);
            return;
        }
        if (schrab > 0.0F) {
            int strength = Math.min(250, Math.round(schrab + amat / 2.0F + hydro / 4.0F + nuke / 8.0F + tnt / 16.0F));
            scheduleLegacyNuke(level, x, y, z, Math.max(1, strength));
            return;
        }
        if (amat > 0.0F) {
            int strength = Math.min(350, Math.round(amat + hydro / 2.0F + nuke / 4.0F + tnt / 8.0F));
            scheduleLegacyNuke(level, x, y, z, Math.max(1, strength));
            return;
        }
        if (hydro > 0.0F) {
            int strength = Math.min(350, Math.round(hydro + nuke / 2.0F + tnt / 4.0F));
            scheduleLegacyNuke(level, x, y, z, Math.max(1, strength));
            return;
        }
        if (nuke > 0.0F) {
            int strength = Math.min(200, Math.round(nuke + tnt / 2.0F));
            scheduleLegacyNuke(level, x, y, z, Math.max(1, strength));
            return;
        }
        if (tnt >= 75.0F) {
            scheduleLegacyNuke(level, x, y, z, Math.max(1, Math.min(150, Math.round(tnt))));
        } else if (tnt > 0.0F) {
            level.explode(null, x, y, z, tnt, true, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        }
    }

    /** Registers the 1.7.10 MachineFieldDisturber exclusion point for FLEIJA detonations. */
    public static void registerFieldDisturber(ServerLevel level, BlockPos pos, int lifetimeTicks) {
        FIELD_DISTURBERS.computeIfAbsent(level.dimension().location(), ignored -> new HashMap<>())
                .put(pos.asLong(), level.getGameTime() + lifetimeTicks);
    }

    /** Matches EntityNukeExplosionMK3.statFacFleija's 300-block exclusion check. */
    public static boolean isFleijaSuppressed(ServerLevel level, BlockPos explosionCenter) {
        Map<Long, Long> fields = FIELD_DISTURBERS.get(level.dimension().location());
        if (fields == null || fields.isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        fields.entrySet().removeIf(entry -> entry.getValue() < now);
        if (fields.isEmpty()) {
            FIELD_DISTURBERS.remove(level.dimension().location());
            return false;
        }
        for (long serializedPos : fields.keySet()) {
            if (BlockPos.of(serializedPos).distSqr(explosionCenter) < 90_000.0D) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Queue<NukeTask> tasks = TASKS.get(level.dimension().location());
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        NukeTask task = tasks.peek();
        if (task.tick(level)) {
            tasks.remove();
        }
        if (tasks.isEmpty()) {
            TASKS.remove(level.dimension().location());
        }
    }

    private static void playInitialSound(ServerLevel level, double x, double y, double z) {
        level.playSound(null, x, y, z, HbmSoundEvents.ENTITY_OLD_EXPLOSION.get(), SoundSource.HOSTILE, 10000.0F, 0.5F + level.random.nextFloat() * 0.1F);
        level.playSound(null, x, y, z, HbmSoundEvents.RBMK_EXPLOSION.get(), SoundSource.HOSTILE, 10000.0F, 0.8F + level.random.nextFloat() * 0.05F);
    }

    private static void spawnTorex(ServerLevel level, double x, double y, double z, int radius) {
        NukeTorexEntity torex = new NukeTorexEntity(HbmEntityTypes.NUKE_TOREX.get(), level);
        torex.setPos(x, y, z);
        double scaledRadius = radius * 0.01D;
        double squirt = Math.sqrt(scaledRadius + 1.0D / ((scaledRadius + 2.0D) * (scaledRadius + 2.0D)))
                - 1.0D / (scaledRadius + 2.0D);
        torex.setScale(Mth.clamp((float) (squirt * 1.5D), 0.5F, 5.0F));
        level.addFreshEntity(torex);
    }

    private static void sendInitialParticles(ServerLevel level, double x, double y, double z) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) > 1000.0D * 1000.0D) {
                continue;
            }
            level.sendParticles(player, ParticleTypes.EXPLOSION_EMITTER, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(player, ParticleTypes.FLASH, true, x, y + 1.0D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, y + 8.0D, z, 120, 8.0D, 10.0D, 8.0D, 0.04D);
            level.sendParticles(player, ParticleTypes.LARGE_SMOKE, true, x, y + 2.0D, z, 96, 20.0D, 3.0D, 20.0D, 0.18D);
            level.sendParticles(player, ParticleTypes.FLAME, true, x, y + 1.0D, z, 80, 12.0D, 2.0D, 12.0D, 0.12D);
        }
    }

    private static final class NukeTask {
        private final Vec3 center;
        private final int strength;
        private final int length;
        private final boolean radiationEnabled;
        private final Long2LongOpenHashMap blockSampleCache = new Long2LongOpenHashMap();
        private final Queue<Long> pendingDestroy = new ArrayDeque<>();
        private final LongOpenHashSet queuedDestroy = new LongOpenHashSet();
        private final Map<Long, ShortOpenHashSet> sectionUpdates = new HashMap<>();
        private final Map<Long, Long> sectionLightSamples = new HashMap<>();
        private final CompletableFuture<List<RayDirection>> directions;
        private CompletableFuture<List<Long>> sortFuture;
        private List<RayDirection> loadedDirections = List.of();
        private RayScan activeRay;
        private int nextRay;
        private int damageTicks = DAMAGE_TICKS;
        private boolean raysDone;

        private NukeTask(Vec3 center, int strength, int length, boolean radiationEnabled) {
            this.center = center;
            this.strength = strength;
            this.length = length;
            this.radiationEnabled = radiationEnabled;
            this.blockSampleCache.defaultReturnValue(CACHE_MISS);
            this.directions = CompletableFuture.supplyAsync(() -> createDirections(length), PLANNER);
        }

        private boolean tick(ServerLevel level) {
            dealDamage(level);
            if (!this.raysDone) {
                scanRays(level, MK5_RAY_STEPS_PER_TICK);
            }
            collectSortedDestroyBatch();
            applyPendingDestroy(level, MK5_BLOCK_UPDATES_PER_TICK);
            flushSectionUpdates(level);
            return this.raysDone && this.pendingDestroy.isEmpty() && this.sortFuture == null && this.damageTicks <= 0;
        }

        private void dealDamage(ServerLevel level) {
            if (this.damageTicks-- <= 0) {
                return;
            }
            double damageRange = this.length * 2.0D;
            AABB area = new AABB(
                    this.center.x - damageRange,
                    this.center.y - damageRange,
                    this.center.z - damageRange,
                    this.center.x + damageRange,
                    this.center.y + damageRange,
                    this.center.z + damageRange
            );
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
                double distance = Math.sqrt(entity.distanceToSqr(this.center));
                if (distance > damageRange) {
                    continue;
                }
                double exposure = 1.0D - distance / damageRange;
                // Legacy nuclear blasts use HBM's nuclearBlast damage type,
                // rather than vanilla explosion, so the old death message is
                // preserved for scheduled (powered) creeper detonations too.
                entity.hurt(level.damageSources().source(HbmDamageTypes.NUCLEAR_BLAST),
                        (float) Math.max(4.0D, exposure * this.strength * 4.0D));
                applyPromptRadiation(level, entity, distance, damageRange);
            }
        }

        private void applyPromptRadiation(ServerLevel level, LivingEntity entity, double distance, double range) {
            if (!this.radiationEnabled || this.damageTicks >= 10 || this.strength < 150) {
                return;
            }
            if (com.reinhardt.hbm.radiation.RadiationEvents.isLegacyRadiationImmune(entity)) {
                return;
            }
            Vec3 target = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
            Vec3 ray = target.subtract(this.center);
            double lengthToEntity = ray.length();
            if (lengthToEntity <= 1.0D) {
                HbmLivingRadiation.get(entity).addRadiation(HbmLivingRadiation.MAX_RADIATION);
                return;
            }
            Vec3 step = ray.normalize();
            float resistance = 1.0F;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int i = 1; i < lengthToEntity; i++) {
                cursor.set(
                        (int) Math.floor(this.center.x + step.x * i),
                        (int) Math.floor(this.center.y + step.y * i),
                        (int) Math.floor(this.center.z + step.z * i)
                );
                if (!level.isLoaded(cursor) || cursor.getY() < level.getMinBuildHeight() || cursor.getY() >= level.getMaxBuildHeight()) {
                    continue;
                }
                resistance += Math.max(0.0F, level.getBlockState(cursor).getBlock().getExplosionResistance());
            }
            float rads = 2_500_000.0F / (this.damageTicks * 5.0F + 1.0F);
            rads /= resistance;
            rads /= Math.max(1.0F, (float) (distance * distance));
            HbmLivingRadiation.get(entity).addRadiation(Math.min(HbmLivingRadiation.MAX_RADIATION, rads));
        }

        private void scanRays(ServerLevel level, int maxSteps) {
            if (!this.directions.isDone()) {
                return;
            }
            if (this.directions.isCompletedExceptionally() || this.directions.isCancelled()) {
                ReinhardtsHBM.LOGGER.error("Nuke ray planner failed; cancelling terrain destruction at {}", this.center);
                this.raysDone = true;
                this.blockSampleCache.clear();
                this.pendingDestroy.clear();
                this.queuedDestroy.clear();
                this.sortFuture = null;
                return;
            }
            if (this.loadedDirections.isEmpty()) {
                this.loadedDirections = this.directions.join();
            }
            long deadline = System.nanoTime() + MK5_RAY_NANOS_PER_TICK;
            int steps = 0;
            while (steps < maxSteps) {
                if ((steps & 255) == 0 && System.nanoTime() >= deadline) {
                    return;
                }
                if (this.activeRay == null) {
                    if (this.nextRay >= this.loadedDirections.size()) {
                        this.raysDone = true;
                        this.blockSampleCache.clear();
                        flushDestroySort();
                        return;
                    }
                    this.activeRay = new RayScan(this.loadedDirections.get(this.nextRay++), this.strength, this.length);
                }
                steps += this.activeRay.step(level, this.center, this.blockSampleCache);
                if (this.activeRay.done()) {
                    this.activeRay = null;
                }
                if (this.pendingDestroy.size() >= MK5_DESTROY_BATCH_SIZE) {
                    flushDestroySort();
                }
            }
        }

        private void applyPendingDestroy(ServerLevel level, int maxUpdates) {
            int checked = 0;
            long deadline = System.nanoTime() + MK5_BLOCK_UPDATE_NANOS_PER_TICK;
            while (checked < maxUpdates && !this.pendingDestroy.isEmpty()) {
                if ((checked & 63) == 0 && System.nanoTime() >= deadline) {
                    return;
                }
                long pos = this.pendingDestroy.remove();
                this.queuedDestroy.remove(pos);
                fastDestroyCollected(level, pos);
                checked++;
            }
        }

        private void flushSectionUpdates(ServerLevel level) {
            if (this.sectionUpdates.isEmpty()) {
                return;
            }
            for (Map.Entry<Long, ShortOpenHashSet> entry : List.copyOf(this.sectionUpdates.entrySet())) {
                long sectionKey = entry.getKey();
                ShortSet positions = entry.getValue();
                if (positions.isEmpty()) {
                    this.sectionUpdates.remove(sectionKey);
                    this.sectionLightSamples.remove(sectionKey);
                    continue;
                }
                SectionPos sectionPos = SectionPos.of(sectionKey);
                LevelChunk chunk = level.getChunkSource().getChunkNow(sectionPos.chunk().x, sectionPos.chunk().z);
                if (chunk == null) {
                    continue;
                }
                int sectionIndex = level.getSectionIndex(sectionPos.minBlockY());
                if (sectionIndex < 0 || sectionIndex >= level.getSectionsCount()) {
                    this.sectionUpdates.remove(sectionKey);
                    this.sectionLightSamples.remove(sectionKey);
                    continue;
                }
                LevelChunkSection section = chunk.getSection(sectionIndex);
                ClientboundSectionBlocksUpdatePacket packet = new ClientboundSectionBlocksUpdatePacket(sectionPos, positions, section);
                for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(sectionPos.chunk(), false)) {
                    player.connection.send(packet);
                }
                Long sample = this.sectionLightSamples.get(sectionKey);
                if (sample != null) {
                    level.getChunkSource().getLightEngine().checkBlock(BlockPos.of(sample));
                }
                this.sectionUpdates.remove(sectionKey);
                this.sectionLightSamples.remove(sectionKey);
            }
        }

        private void flushDestroySort() {
            if (this.sortFuture != null || this.pendingDestroy.isEmpty()) {
                return;
            }
            List<Long> batch = new ArrayList<>(this.pendingDestroy.size());
            while (!this.pendingDestroy.isEmpty()) {
                batch.add(this.pendingDestroy.remove());
            }
            int centerChunkX = BlockPos.containing(this.center).getX() >> 4;
            int centerChunkZ = BlockPos.containing(this.center).getZ() >> 4;
            this.sortFuture = CompletableFuture.supplyAsync(() -> sortByChunkDistance(batch, centerChunkX, centerChunkZ), PLANNER);
        }

        private void collectSortedDestroyBatch() {
            if (this.sortFuture == null || !this.sortFuture.isDone()) {
                return;
            }
            if (this.sortFuture.isCompletedExceptionally() || this.sortFuture.isCancelled()) {
                ReinhardtsHBM.LOGGER.error("Nuke destruction sorter failed; continuing unsorted at {}", this.center);
                this.sortFuture = null;
                return;
            }
            this.pendingDestroy.addAll(this.sortFuture.join());
            this.sortFuture = null;
        }

        private List<Long> sortByChunkDistance(List<Long> batch, int centerChunkX, int centerChunkZ) {
            batch.sort((a, b) -> {
                BlockPos posA = BlockPos.of(a);
                BlockPos posB = BlockPos.of(b);
                int distA = Math.abs((posA.getX() >> 4) - centerChunkX) + Math.abs((posA.getZ() >> 4) - centerChunkZ);
                int distB = Math.abs((posB.getX() >> 4) - centerChunkX) + Math.abs((posB.getZ() >> 4) - centerChunkZ);
                return Integer.compare(distA, distB);
            });
            return batch;
        }

        private boolean destroyCollected(ServerLevel level, long packedPos, boolean tip) {
            BlockPos pos = BlockPos.of(packedPos);
            if (!level.isLoaded(pos)) {
                return false;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(Blocks.BEDROCK)) {
                return false;
            }
            if (state.getBlock().getExplosionResistance() >= 3_600_000.0F) {
                return false;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), tip ? 3 : 2);
            HbmRadiationWorlds.invalidateResistance(level, pos);
            return true;
        }

        private boolean fastDestroyCollected(ServerLevel level, long packedPos) {
            BlockPos pos = BlockPos.of(packedPos);
            LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
            if (chunk == null) {
                return false;
            }
            int sectionIndex = level.getSectionIndex(pos.getY());
            if (sectionIndex < 0 || sectionIndex >= level.getSectionsCount()) {
                return false;
            }
            LevelChunkSection section = chunk.getSection(sectionIndex);
            BlockState state = section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
            if (state.isAir() || state.is(Blocks.BEDROCK)) {
                return false;
            }
            if (state.getBlock().getExplosionResistance() >= 3_600_000.0F) {
                return false;
            }
            if (state.hasBlockEntity()) {
                chunk.removeBlockEntity(pos);
            }
            section.setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, Blocks.AIR.defaultBlockState(), false);
            chunk.setUnsaved(true);
            long sectionKey = SectionPos.asLong(
                    SectionPos.blockToSectionCoord(pos.getX()),
                    SectionPos.blockToSectionCoord(pos.getY()),
                    SectionPos.blockToSectionCoord(pos.getZ())
            );
            this.sectionUpdates.computeIfAbsent(sectionKey, unused -> new ShortOpenHashSet()).add(SectionPos.sectionRelativePos(pos));
            this.sectionLightSamples.putIfAbsent(sectionKey, pos.asLong());
            HbmRadiationWorlds.invalidateResistanceSection(level, sectionKey);
            return true;
        }

        private float masqueradeResistance(BlockState state) {
            if (state.is(Blocks.SANDSTONE)) {
                return Blocks.STONE.getExplosionResistance();
            }
            if (state.is(Blocks.OBSIDIAN)) {
                return Blocks.STONE.getExplosionResistance() * 3.0F;
            }
            return state.getBlock().getExplosionResistance();
        }

        private static long encodeSample(BlockSample sample) {
            long encoded = Float.floatToRawIntBits(sample.resistance()) & 0xffffffffL;
            if (sample.air()) {
                encoded |= SAMPLE_AIR;
            }
            if (sample.fluidEmpty()) {
                encoded |= SAMPLE_FLUID_EMPTY;
            }
            return encoded;
        }

        private static BlockSample decodeSample(long encoded) {
            float resistance = Float.intBitsToFloat((int) (encoded & 0xffffffffL));
            return new BlockSample(resistance, (encoded & SAMPLE_AIR) != 0L, (encoded & SAMPLE_FLUID_EMPTY) != 0L);
        }

        private BlockSample sampleBlock(ServerLevel level, BlockPos.MutableBlockPos cursor, Long2LongOpenHashMap cache, int x, int y, int z) {
            long key = BlockPos.asLong(x, y, z);
            long encoded = cache.get(key);
            if (encoded != CACHE_MISS) {
                return decodeSample(encoded);
            }
            BlockState state = Blocks.AIR.defaultBlockState();
            LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
            int sectionIndex = level.getSectionIndex(y);
            if (chunk != null && sectionIndex >= 0 && sectionIndex < level.getSectionsCount()) {
                state = chunk.getSection(sectionIndex).getBlockState(x & 15, y & 15, z & 15);
            }
            BlockSample sample = new BlockSample(masqueradeResistance(state), state.isAir(), state.getFluidState().isEmpty());
            cache.put(key, encodeSample(sample));
            return sample;
        }

        private record BlockSample(float resistance, boolean air, boolean fluidEmpty) {
        }

        private static List<RayDirection> createDirections(int radius) {
            int max = (int) (2.5D * Math.PI * Math.pow(radius, 2.0D));
            List<RayDirection> rays = new ArrayList<>(max);
            double gspX = Math.PI;
            double gspY = 0.0D;
            for (int gspNum = 1; gspNum <= max; gspNum++) {
                double dx = Math.sin(gspX) * Math.cos(gspY);
                double dz = Math.sin(gspX) * Math.sin(gspY);
                double dy = Math.cos(gspX);
                rays.add(new RayDirection(dx, dy, dz));
                if (gspNum < max) {
                    int k = gspNum + 1;
                    double hk = -1.0D + 2.0D * (k - 1.0D) / (max - 1.0D);
                    gspX = Math.acos(hk);
                    double longitude = gspY + 3.6D / Math.sqrt(max) / Math.sqrt(1.0D - hk * hk);
                    gspY = longitude % (Math.PI * 2.0D);
                }
            }
            return rays;
        }

        private record RayDirection(double x, double y, double z) {
        }

        private final class RayScan {
            private final RayDirection ray;
            private final int rayLength;
            private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            private float resistanceBudget;
            private int step;
            private boolean done;

            private RayScan(RayDirection ray, int strength, int length) {
                this.ray = ray;
                this.rayLength = (int) Math.ceil(strength);
                this.resistanceBudget = strength;
            }

            private int step(ServerLevel level, Vec3 center, Long2LongOpenHashMap cache) {
                if (this.done) {
                    return 0;
                }
                if (this.step > length || this.step >= this.rayLength) {
                    this.done = true;
                    return 0;
                }
                double x = center.x + this.ray.x * this.step;
                double y = center.y + this.ray.y * this.step;
                double z = center.z + this.ray.z * this.step;
                int ix = (int) Math.floor(x);
                int iy = (int) Math.floor(y);
                int iz = (int) Math.floor(z);
                if (iy < level.getMinBuildHeight() || iy >= level.getMaxBuildHeight()) {
                    this.done = true;
                    return 0;
                }
                this.cursor.set(ix, iy, iz);
                BlockSample sample = sampleBlock(level, this.cursor, cache, ix, iy, iz);
                double factor = 100.0D - (double) this.step / (double) this.rayLength * 100.0D;
                factor *= 0.07D;
                if (sample.fluidEmpty()) {
                    this.resistanceBudget -= Math.pow(sample.resistance(), 7.5D - factor);
                }
                if (this.resistanceBudget > 0.0F && !sample.air()) {
                    long packed = BlockPos.asLong(ix, iy, iz);
                    if (NukeTask.this.queuedDestroy.add(packed)) {
                        NukeTask.this.pendingDestroy.add(packed);
                    }
                }
                if (this.resistanceBudget <= 0.0F || this.step + 1 >= length || this.step + 1 >= this.rayLength) {
                    this.done = true;
                }
                this.step++;
                return 1;
            }

            private boolean done() {
                return this.done;
            }
        }
    }
}
