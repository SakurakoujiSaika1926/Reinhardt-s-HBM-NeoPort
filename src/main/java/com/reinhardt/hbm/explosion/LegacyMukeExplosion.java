package com.reinhardt.hbm.explosion;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyArtilleryShellEntity;
import com.reinhardt.hbm.entity.LegacyBulletEntity;
import com.reinhardt.hbm.entity.LegacyBossProjectileEntity;
import com.reinhardt.hbm.entity.LegacyHimarsRocketEntity;
import com.reinhardt.hbm.entity.LegacyProjectileUtil;
import com.reinhardt.hbm.entity.LegacyShrapnelEntity;
import com.reinhardt.hbm.entity.JeremyShellEntity;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.radiation.HbmRadiationWorlds;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

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
public final class LegacyMukeExplosion {
    private static final int MINI_BLAST_RADIUS = 20;
    private static final int MINI_KILL_RADIUS = 55;
    private static final int MINI_RESOLUTION = 64;
    private static final int MINI_SHRAPNEL_COUNT = 25;
    private static final int RAY_STEPS_PER_TICK = 24_576;
    private static final int BLOCKS_PER_TICK = 512;
    private static final long RAY_TIME_PER_TICK = 750_000L;
    private static final long BLOCK_TIME_PER_TICK = 750_000L;
    private static final Map<ResourceLocation, Queue<MiniNukeTask>> TASKS = new HashMap<>();
    private static final ExecutorService PLANNER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "RHbm-MukePlanner");
        thread.setDaemon(true);
        return thread;
    });
    private static final CompletableFuture<List<RayDirection>> MINI_DIRECTIONS =
            CompletableFuture.supplyAsync(() -> createCubeDirections(MINI_RESOLUTION), PLANNER);

    private LegacyMukeExplosion() {
    }

    public static void detonateW9(ServerLevel level, Entity source, Vec3 center, float fixedDamage) {
        applyW9CrossDamage(level, source, center, fixedDamage);
        incrementRadiation(level, center, 1.0F);
        sendMukeEffect(level, center);
    }

    public static void detonateMediumMiniNuke(ServerLevel level, Entity source, Vec3 center) {
        TASKS.computeIfAbsent(level.dimension().location(), unused -> new ArrayDeque<>())
                .add(new MiniNukeTask(center, MINI_BLAST_RADIUS, 0.0F, true));
        applyNuclearDamage(level, source, center, MINI_KILL_RADIUS, 250.0F);
        LegacyProjectileUtil.spawnShrapnel(level, center, MINI_SHRAPNEL_COUNT);
        incrementRadiation(level, center, 1.0F);
        sendMukeEffect(level, center);
    }

    /** Exact ExplosionNukeSmall.PARAMS_SAFE branch used by an unpowered nuclear creeper. */
    public static void detonateSafeMiniNuke(ServerLevel level, Entity source, Vec3 center) {
        applyNuclearDamage(level, source, center, 45.0D, 250.0F);
        LegacyProjectileUtil.spawnShrapnel(level, center, MINI_SHRAPNEL_COUNT);
        incrementRadiation(level, center, 2.0F / 3.0F);
        sendMukeEffect(level, center);
    }

    /** Nuclear creeper powered no-grief branch: the old damage-only radius-100 pass. */
    public static void detonateNuclearDamage(ServerLevel level, Entity source, Vec3 center, double radius) {
        applyNuclearDamage(level, source, center, radius, 250.0F);
        sendMukeEffect(level, center);
    }

    /** Exact non-block-damaging UFO rocket impact from ExplosionNukeGeneric.dealDamage. */
    public static void detonateUfoRocket(ServerLevel level, Entity source, Vec3 center) {
        applyNuclearDamage(level, source, center, 10.0D, 50.0F);
    }

    public static void detonateLandmine(ServerLevel level, Vec3 center, float damage) {
        TASKS.computeIfAbsent(level.dimension().location(), unused -> new ArrayDeque<>())
                .add(new MiniNukeTask(center, 10, 1.0F / 10.0F, false));
        LegacyProjectileUtil.landmineDamage(level, center, 10.0F, damage, 2.0D, 0.0F, 0.0F, 1.5F);
        incrementRadiation(level, center, 1.5F);
        sendMukeEffect(level, center);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Queue<MiniNukeTask> queue = TASKS.get(level.dimension().location());
        if (queue == null || queue.isEmpty()) {
            return;
        }
        if (queue.peek().tick(level)) {
            queue.remove();
        }
        if (queue.isEmpty()) {
            TASKS.remove(level.dimension().location());
        }
    }

    private static void applyW9CrossDamage(ServerLevel level, Entity source, Vec3 center, float fixedDamage) {
        double range = 30.0D;
        AABB area = new AABB(center, center).inflate(range + 1.0D);
        Vec3[] nodes = {
                center,
                center.add(0.0D, -2.0D, 0.0D),
                center.add(0.0D, 2.0D, 0.0D),
                center.add(0.0D, 0.0D, -2.0D),
                center.add(0.0D, 0.0D, 2.0D),
                center.add(-2.0D, 0.0D, 0.0D),
                center.add(2.0D, 0.0D, 0.0D)
        };
        for (Entity entity : level.getEntities((Entity) null, area, Entity::isAlive)) {
            double distance = distanceFromBox(center, entity.getBoundingBox());
            double scaledDistance = distance / range;
            if (scaledDistance > 1.0D) {
                continue;
            }
            Vec3 delta = new Vec3(entity.getX() - center.x, entity.getEyeY() - center.y, entity.getZ() - center.z);
            if (delta.lengthSqr() <= 0.0D) {
                continue;
            }
            double density = 0.0D;
            for (Vec3 node : nodes) {
                density = Math.max(density, Explosion.getSeenPercent(node, entity));
            }
            float damage = density < 0.125D ? 0.0F : (float) (fixedDamage * (1.0D - scaledDistance));
            if (damage > 0.0F) {
                LegacyProjectileUtil.hurtNoIFrame(
                        entity,
                        level.damageSources().source(HbmDamageTypes.NUCLEAR_BLAST, source, null),
                        damage
                );
            }
            double knockback = (1.0D - scaledDistance) * density;
            if (knockback > 0.0D && !isProjectile(entity)) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(delta.normalize().scale(knockback)));
                entity.hurtMarked = true;
            }
        }
    }

    private static void applyNuclearDamage(ServerLevel level, Entity source, Vec3 center, double radius, float maxDamage) {
        AABB area = new AABB(center, center).inflate(radius);
        for (Entity entity : level.getEntities((Entity) null, area, Entity::isAlive)) {
            if (isProjectile(entity) || entity instanceof Player player && player.isCreative()) {
                continue;
            }
            double distance = Math.sqrt(entity.distanceToSqr(center));
            if (distance > radius || isObstructed(level, center, entity)) {
                continue;
            }
            float damage = (float) (maxDamage * (radius - distance) / radius);
            boolean hurt;
            int oldInvulnerability = entity.invulnerableTime;
            entity.invulnerableTime = 0;
            hurt = entity.hurt(level.damageSources().source(HbmDamageTypes.NUCLEAR_BLAST, source, null), damage);
            entity.invulnerableTime = 0;
            if (oldInvulnerability < 0) {
                entity.invulnerableTime = oldInvulnerability;
            }
            entity.igniteForSeconds(5.0F);
            if (hurt) {
                Vec3 knockback = new Vec3(entity.getX() - center.x, entity.getEyeY() - center.y, entity.getZ() - center.z);
                if (knockback.lengthSqr() > 0.0D) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.normalize().scale(0.2D)));
                    entity.hurtMarked = true;
                }
            }
        }
    }

    private static boolean isObstructed(ServerLevel level, Vec3 center, Entity entity) {
        Vec3 target = new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
        return level.clip(new ClipContext(center, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType()
                != HitResult.Type.MISS;
    }

    private static boolean isProjectile(Entity entity) {
        return entity instanceof JeremyShellEntity
                || entity instanceof LegacyBulletEntity
                || entity instanceof LegacyBossProjectileEntity
                || entity instanceof LegacyArtilleryShellEntity
                || entity instanceof LegacyHimarsRocketEntity
                || entity instanceof LegacyShrapnelEntity;
    }

    private static double distanceFromBox(Vec3 center, AABB box) {
        double dx = box.minX <= center.x && box.maxX >= center.x
                ? 0.0D : Math.min(Math.abs(box.minX - center.x), Math.abs(box.maxX - center.x));
        double dy = box.minY <= center.y && box.maxY >= center.y
                ? 0.0D : Math.min(Math.abs(box.minY - center.y), Math.abs(box.maxY - center.y));
        double dz = box.minZ <= center.z && box.maxZ >= center.z
                ? 0.0D : Math.min(Math.abs(box.minZ - center.z), Math.abs(box.maxZ - center.z));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void incrementRadiation(ServerLevel level, Vec3 center, float multiplier) {
        ChunkRadiationData radiation = ChunkRadiationData.get(level);
        int y = Mth.floor(center.y);
        for (int xOffset = -2; xOffset <= 2; xOffset++) {
            for (int zOffset = -2; zOffset <= 2; zOffset++) {
                int distance = Math.abs(xOffset) + Math.abs(zOffset);
                if (distance >= 4) {
                    continue;
                }
                BlockPos pos = new BlockPos(
                        Mth.floor(center.x + xOffset * 16.0D),
                        y,
                        Mth.floor(center.z + zOffset * 16.0D)
                );
                radiation.incrementRadiation(pos, 50.0D / (distance + 1.0D) * multiplier);
            }
        }
    }

    public static void sendMukeEffect(ServerLevel level, Vec3 center) {
        level.playSound(null, center.x, center.y, center.z,
                HbmSoundEvents.WEAPON_MUKE_EXPLOSION.get(), SoundSource.BLOCKS, 15.0F, 1.0F);
        boolean balefire = level.random.nextInt(100) == 0;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) > 250.0D * 250.0D) {
                continue;
            }
            level.sendParticles(player,
                    balefire ? HbmParticleTypes.MUKE_FLASH_BALEFIRE.get() : HbmParticleTypes.MUKE_FLASH.get(),
                    true, center.x, center.y + 0.5D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(player, HbmParticleTypes.MUKE_WAVE.get(),
                    true, center.x, center.y + 0.5D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static List<RayDirection> createCubeDirections(int resolution) {
        List<RayDirection> directions = new ArrayList<>(resolution * resolution * 6);
        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {
                for (int z = 0; z < resolution; z++) {
                    if (x != 0 && x != resolution - 1
                            && y != 0 && y != resolution - 1
                            && z != 0 && z != resolution - 1) {
                        continue;
                    }
                    double dx = (double) ((float) x / (resolution - 1.0F) * 2.0F - 1.0F);
                    double dy = (double) ((float) y / (resolution - 1.0F) * 2.0F - 1.0F);
                    double dz = (double) ((float) z / (resolution - 1.0F) * 2.0F - 1.0F);
                    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    directions.add(new RayDirection(dx / length, dy / length, dz / length));
                }
            }
        }
        return List.copyOf(directions);
    }

    private record RayDirection(double x, double y, double z) {
    }

    private static final class MiniNukeTask {
        private final Vec3 center;
        private final int blastRadius;
        private final float dropChance;
        private final boolean placeFire;
        private final LongOpenHashSet affectedBlocks = new LongOpenHashSet();
        private List<RayDirection> directions = List.of();
        private int nextRay;
        private ActiveRay activeRay;
        private LongIterator destruction;
        private long[] firePositions;
        private int nextFire;
        private Explosion explosionContext;

        private MiniNukeTask(Vec3 center, int blastRadius, float dropChance, boolean placeFire) {
            this.center = center;
            this.blastRadius = blastRadius;
            this.dropChance = dropChance;
            this.placeFire = placeFire;
        }

        private boolean tick(ServerLevel level) {
            if (this.destruction == null) {
                scan(level);
                if (this.nextRay < this.directions.size() || this.activeRay != null || this.directions.isEmpty()) {
                    return false;
                }
                this.firePositions = this.affectedBlocks.toLongArray();
                this.destruction = this.affectedBlocks.iterator();
                this.explosionContext = new Explosion(level, null, this.center.x, this.center.y, this.center.z,
                        this.blastRadius, false, Explosion.BlockInteraction.DESTROY);
            }
            if (this.destruction.hasNext()) {
                destroy(level);
                return false;
            }
            if (this.placeFire && this.nextFire < this.firePositions.length) {
                placeFire(level);
                return false;
            }
            return true;
        }

        private void scan(ServerLevel level) {
            if (this.directions.isEmpty()) {
                if (!MINI_DIRECTIONS.isDone()) {
                    return;
                }
                this.directions = MINI_DIRECTIONS.join();
            }
            long deadline = System.nanoTime() + RAY_TIME_PER_TICK;
            int steps = 0;
            while (steps < RAY_STEPS_PER_TICK && System.nanoTime() < deadline) {
                if (this.activeRay == null) {
                    if (this.nextRay >= this.directions.size()) {
                        return;
                    }
                    this.activeRay = new ActiveRay(this.directions.get(this.nextRay++), level.random.nextFloat(), this.blastRadius);
                }
                this.activeRay.step(level, this.center, this.affectedBlocks);
                steps++;
                if (this.activeRay.done) {
                    this.activeRay = null;
                }
            }
        }

        private void destroy(ServerLevel level) {
            long deadline = System.nanoTime() + BLOCK_TIME_PER_TICK;
            int updates = 0;
            while (updates < BLOCKS_PER_TICK && this.destruction.hasNext() && System.nanoTime() < deadline) {
                BlockPos pos = BlockPos.of(this.destruction.nextLong());
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    state.onExplosionHit(level, pos, this.explosionContext, (stack, dropPos) -> {
                        if (this.dropChance > 0.0F && level.random.nextFloat() < this.dropChance) {
                            Block.popResource(level, dropPos, stack);
                        }
                    });
                    HbmRadiationWorlds.invalidateResistance(level, pos);
                }
                updates++;
            }
        }

        private void placeFire(ServerLevel level) {
            long deadline = System.nanoTime() + BLOCK_TIME_PER_TICK;
            int updates = 0;
            while (updates < BLOCKS_PER_TICK && this.nextFire < this.firePositions.length && System.nanoTime() < deadline) {
                BlockPos pos = BlockPos.of(this.firePositions[this.nextFire++]);
                if (level.random.nextInt(3) == 0
                        && level.getBlockState(pos).isAir()
                        && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                    level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
                }
                updates++;
            }
        }
    }

    private static final class ActiveRay {
        private static final float STEP = 0.3F;
        private final RayDirection direction;
        private float remainingPower;
        private double x;
        private double y;
        private double z;
        private boolean initialized;
        private boolean done;

        private ActiveRay(RayDirection direction, float randomPower, int blastRadius) {
            this.direction = direction;
            this.remainingPower = blastRadius * (0.7F + randomPower * 0.6F);
        }

        private void step(ServerLevel level, Vec3 center, LongOpenHashSet affectedBlocks) {
            if (!this.initialized) {
                this.x = center.x;
                this.y = center.y;
                this.z = center.z;
                this.initialized = true;
            }
            BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
            if (!level.isInWorldBounds(pos)) {
                this.done = true;
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                this.remainingPower -= (state.getBlock().getExplosionResistance() + 0.3F) * STEP;
                if (this.remainingPower > 0.0F) {
                    affectedBlocks.add(pos.asLong());
                }
            }
            this.x += this.direction.x * STEP;
            this.y += this.direction.y * STEP;
            this.z += this.direction.z * STEP;
            this.remainingPower -= STEP * 0.75F;
            if (this.remainingPower <= 0.0F) {
                this.done = true;
            }
        }
    }
}
