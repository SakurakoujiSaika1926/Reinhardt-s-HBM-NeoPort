package com.reinhardt.hbm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.particle.LegacyExplosionCloudParticle;
import com.reinhardt.hbm.client.particle.LegacySmallExplosionParticle;
import com.reinhardt.hbm.client.particle.MukeWaveParticle;
import com.reinhardt.hbm.network.LegacyExplosionEffectPayload;
import com.reinhardt.hbm.network.LegacySmallExplosionEffectPayload;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class LegacyExplosionClientEffects {
    private static final double SPEED_OF_SOUND = 17.15D * 0.5D;
    private static final List<DebrisCluster> DEBRIS = new ArrayList<>();
    private static long debrisSequence;

    private LegacyExplosionClientEffects() {
    }

    public static void accept(LegacyExplosionEffectPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        RandomSource random = level.random;
        double distance = minecraft.player.position().distanceTo(new Vec3(payload.x(), payload.y(), payload.z()));
        if (distance <= payload.soundRange()) {
            SoundEvent sound = distance <= payload.soundRange() * 0.4D
                    ? HbmSoundEvents.WEAPON_EXPLOSION_LARGE_NEAR.get()
                    : HbmSoundEvents.WEAPON_EXPLOSION_LARGE_FAR.get();
            SimpleSoundInstance instance = new SimpleSoundInstance(
                    sound,
                    SoundSource.BLOCKS,
                    1000.0F,
                    0.9F + random.nextFloat() * 0.2F,
                    RandomSource.create(),
                    payload.x(),
                    payload.y(),
                    payload.z()
            );
            minecraft.getSoundManager().playDelayed(instance, (int) (distance / SPEED_OF_SOUND));
        }

        Particle wave = minecraft.particleEngine.createParticle(
                HbmParticleTypes.MUKE_WAVE.get(),
                payload.x(),
                payload.y() + 2.0D,
                payload.z(),
                0.0D,
                0.0D,
                0.0D
        );
        if (wave instanceof MukeWaveParticle mukeWave) {
            mukeWave.configure(payload.waveScale(), (int) (25.0F * payload.waveScale() / 45.0F));
        }

        for (int i = 0; i < payload.cloudCount(); i++) {
            Particle cloud = minecraft.particleEngine.createParticle(
                    HbmParticleTypes.LEGACY_EXPLOSION_CLOUD.get(),
                    payload.x(),
                    payload.y(),
                    payload.z(),
                    random.nextGaussian() * 0.5D * payload.cloudSpeedMultiplier(),
                    random.nextDouble() * 3.0D * payload.cloudSpeedMultiplier(),
                    random.nextGaussian() * 0.5D * payload.cloudSpeedMultiplier()
            );
            if (cloud instanceof LegacyExplosionCloudParticle explosionCloud) {
                explosionCloud.configure(payload.cloudScale(), 70 + random.nextInt(20));
            }
        }

        if (payload.debrisSize() <= 0) {
            return;
        }
        for (int i = 0; i < payload.debrisCount(); i++) {
            double offsetX = random.nextGaussian() * payload.debrisHorizontalDeviation();
            double offsetZ = random.nextGaussian() * payload.debrisHorizontalDeviation();
            int sampleX = Mth.floor(payload.x() + offsetX + 0.5D);
            int sampleY = Mth.floor(payload.y() + payload.debrisVerticalOffset() + 0.5D);
            int sampleZ = Mth.floor(payload.z() + offsetZ + 0.5D);
            Vec3 motion = rotateY(
                    rotateZ(
                            new Vec3(payload.debrisVelocity(), 0.0D, 0.0D),
                            -Math.toRadians(45.0D + random.nextFloat() * 25.0D)
                    ),
                    random.nextDouble() * Math.PI * 2.0D
            );
            DebrisCluster cluster = DebrisCluster.sample(
                    level,
                    new Vec3(payload.x(), payload.y(), payload.z()),
                    motion.scale(3.0D),
                    new BlockPos(sampleX, sampleY, sampleZ),
                    payload.debrisSize(),
                    payload.debrisRetry(),
                    random,
                    debrisSequence++
            );
            if (!cluster.blocks.isEmpty()) {
                DEBRIS.add(cluster);
            }
        }
    }

    public static void acceptSmall(LegacySmallExplosionEffectPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        RandomSource random = level.random;
        Vec3 center = new Vec3(payload.x(), payload.y(), payload.z());
        double distance = minecraft.player.position().distanceTo(center);
        if (distance <= 200.0D) {
            SoundEvent sound = distance <= 80.0D
                    ? HbmSoundEvents.WEAPON_EXPLOSION_SMALL_NEAR.get()
                    : HbmSoundEvents.WEAPON_EXPLOSION_SMALL_FAR.get();
            SimpleSoundInstance instance = new SimpleSoundInstance(
                    sound,
                    SoundSource.BLOCKS,
                    100.0F,
                    0.9F + random.nextFloat() * 0.2F,
                    RandomSource.create(),
                    payload.x(), payload.y(), payload.z());
            minecraft.getSoundManager().playDelayed(instance, (int) (distance / SPEED_OF_SOUND));
        }

        for (int i = 0; i < payload.cloudCount(); i++) {
            Particle particle = minecraft.particleEngine.createParticle(
                    HbmParticleTypes.LEGACY_SMALL_EXPLOSION.get(),
                    payload.x(), payload.y(), payload.z(),
                    random.nextGaussian() * payload.cloudSpeedMultiplier(),
                    0.0D,
                    random.nextGaussian() * payload.cloudSpeedMultiplier());
            if (particle instanceof LegacySmallExplosionParticle explosion) {
                explosion.configure(payload.cloudScale());
            }
        }

        BlockPos origin = BlockPos.containing(payload.x(), payload.y(), payload.z());
        BlockState debrisState = null;
        BlockPos debrisPos = origin;
        for (Direction direction : Direction.values()) {
            BlockPos candidate = origin.relative(direction);
            BlockState state = level.getBlockState(candidate);
            if (!state.isAir()) {
                debrisState = state;
                debrisPos = candidate;
                break;
            }
        }
        if (debrisState == null) {
            return;
        }
        for (int i = 0; i < 15; i++) {
            TerrainParticle debris = new TerrainParticle(
                    level,
                    payload.x(), payload.y() + 0.1D, payload.z(),
                    random.nextGaussian() * 0.2D,
                    0.5D + random.nextDouble() * 0.7D,
                    random.nextGaussian() * 0.2D,
                    debrisState,
                    debrisPos);
            debris.scale(2.0F);
            debris.setLifetime(50 + random.nextInt(20));
            minecraft.particleEngine.add(debris);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        Iterator<DebrisCluster> iterator = DEBRIS.iterator();
        while (iterator.hasNext()) {
            DebrisCluster cluster = iterator.next();
            if (level == null || cluster.level != level || !cluster.tick()) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || DEBRIS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        for (DebrisCluster cluster : DEBRIS) {
            if (cluster.level != level) {
                continue;
            }
            Vec3 renderPos = cluster.previousPosition.lerp(cluster.position, partialTick);
            poseStack.pushPose();
            poseStack.translate(renderPos.x - camera.x, renderPos.y - camera.y, renderPos.z - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, cluster.previousPitch, cluster.pitch)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, cluster.previousYaw, cluster.yaw)));
            poseStack.translate(-cluster.size / 2.0D, -cluster.size / 2.0D, -cluster.size / 2.0D);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(renderPos));
            for (Map.Entry<BlockPos, BlockState> entry : cluster.blocks.entrySet()) {
                BlockPos local = entry.getKey();
                poseStack.pushPose();
                poseStack.translate(local.getX(), local.getY(), local.getZ());
                minecraft.getBlockRenderer().renderSingleBlock(
                        entry.getValue(),
                        poseStack,
                        buffers,
                        light,
                        OverlayTexture.NO_OVERLAY
                );
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    private static Vec3 rotateZ(Vec3 vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vec.x * cos - vec.y * sin, vec.x * sin + vec.y * cos, vec.z);
    }

    private static Vec3 rotateY(Vec3 vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vec.x * cos + vec.z * sin, vec.y, vec.z * cos - vec.x * sin);
    }

    private static final class DebrisCluster {
        private final ClientLevel level;
        private final Map<BlockPos, BlockState> blocks;
        private final int size;
        private final float pitchStep;
        private final float yawStep;
        private final boolean leavesFlameTrail;
        private Vec3 position;
        private Vec3 previousPosition;
        private Vec3 motion;
        private float pitch;
        private float previousPitch;
        private float yaw;
        private float previousYaw;
        private int age;

        private DebrisCluster(
                ClientLevel level,
                Vec3 position,
                Vec3 motion,
                Map<BlockPos, BlockState> blocks,
                int size,
                float pitchStep,
                float yawStep,
                boolean leavesFlameTrail
        ) {
            this.level = level;
            this.position = position;
            this.previousPosition = position;
            this.motion = motion;
            this.blocks = blocks;
            this.size = size;
            this.pitchStep = pitchStep;
            this.yawStep = yawStep;
            this.leavesFlameTrail = leavesFlameTrail;
        }

        private static DebrisCluster sample(
                ClientLevel level,
                Vec3 position,
                Vec3 motion,
                BlockPos sampleOrigin,
                int size,
                int retries,
                RandomSource random,
                long sequence
        ) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            int middle = size / 2 - 1;
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    for (int z = 0; z < 2; z++) {
                        putSample(blocks, new BlockPos(middle + x, middle + y, middle + z),
                                level.getBlockState(sampleOrigin.offset(x, y, z)));
                    }
                }
            }
            for (int layer = 2; layer <= size / 2; layer++) {
                for (int attempt = 0; attempt < retries; attempt++) {
                    int x = -layer + random.nextInt(layer * 2 + 1);
                    int y = -layer + random.nextInt(layer * 2 + 1);
                    int z = -layer + random.nextInt(layer * 2 + 1);
                    BlockPos local = new BlockPos(middle + x, middle + y, middle + z);
                    if (!hasStoredNeighbor(blocks, local)) {
                        continue;
                    }
                    putSample(blocks, local, level.getBlockState(sampleOrigin.offset(x, y, z)));
                }
            }
            java.util.Random stable = new java.util.Random(sequence);
            return new DebrisCluster(level, position, motion, blocks, size,
                    stable.nextFloat() * 10.0F, stable.nextFloat() * 10.0F,
                    sequence % 3L == 0L);
        }

        private static void putSample(Map<BlockPos, BlockState> blocks, BlockPos local, BlockState state) {
            if (!state.isAir()) {
                blocks.put(local, state);
            }
        }

        private static boolean hasStoredNeighbor(Map<BlockPos, BlockState> blocks, BlockPos pos) {
            return blocks.containsKey(pos.east())
                    || blocks.containsKey(pos.west())
                    || blocks.containsKey(pos.above())
                    || blocks.containsKey(pos.below())
                    || blocks.containsKey(pos.south())
                    || blocks.containsKey(pos.north());
        }

        private boolean tick() {
            this.previousPosition = this.position;
            this.previousPitch = this.pitch;
            this.previousYaw = this.yaw;
            this.pitch += this.pitchStep;
            this.yaw += this.yawStep;
            if (this.leavesFlameTrail) {
                Particle flame = Minecraft.getInstance().particleEngine.createParticle(
                        HbmParticleTypes.METEOR_TAIL.get(),
                        this.position.x,
                        this.position.y,
                        this.position.z,
                        0.0D,
                        0.0D,
                        0.0D
                );
                if (flame instanceof com.reinhardt.hbm.client.particle.MeteorTailParticle rocketFlame) {
                    rocketFlame.configure(Math.max(this.size, 6) / 16.0F, 50);
                }
            }
            this.motion = this.motion.add(0.0D, -0.15D, 0.0D);
            Vec3 next = this.position.add(this.motion);
            if (this.age > 5) {
                AABB collision = new AABB(next.x - 0.1D, next.y - 0.1D, next.z - 0.1D,
                        next.x + 0.1D, next.y + 0.1D, next.z + 0.1D);
                if (!this.level.noCollision(collision)) {
                    return false;
                }
            }
            this.position = next;
            return ++this.age < 100 && this.position.y > this.level.getMinBuildHeight() - 16;
        }
    }
}
