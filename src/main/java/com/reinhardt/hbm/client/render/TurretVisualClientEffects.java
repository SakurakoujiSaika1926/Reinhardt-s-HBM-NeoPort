package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.network.TurretCasingEffectPayload;
import com.reinhardt.hbm.network.TurretMuzzleFlashPayload;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class TurretVisualClientEffects {
    private static final float MODEL_SCALE = 0.05F;
    private static final ModelResourceLocation STRAIGHT = MachineModelRenderer.standalone("block/effect_casing_straight");
    private static final ModelResourceLocation BOTTLENECK = MachineModelRenderer.standalone("block/effect_casing_bottleneck");
    private static final List<SpentCasingEffect> CASINGS = new ArrayList<>();

    private TurretVisualClientEffects() {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(STRAIGHT);
        event.register(BOTTLENECK);
    }

    public static void accept(TurretCasingEffectPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        CasingConfig config = CasingConfig.byId(payload.casingKind());
        if (level == null || config == null) {
            return;
        }
        CASINGS.add(new SpentCasingEffect(level, payload, config));
    }

    public static void accept(TurretMuzzleFlashPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Particle primary = minecraft.particleEngine.createParticle(
                ParticleTypes.EXPLOSION,
                payload.x(), payload.y(), payload.z(),
                payload.size(), 0.0D, 0.0D
        );
        if (primary != null) {
            float brightness = 1.0F - level.random.nextFloat() * 0.2F;
            primary.setColor(brightness, 0.9F * brightness, 0.5F * brightness);
        }
        for (int index = 0; index < payload.count(); index++) {
            Particle secondary = minecraft.particleEngine.createParticle(
                    ParticleTypes.POOF,
                    payload.x(), payload.y(), payload.z(),
                    1.0D, 0.0D, 0.0D
            );
            if (secondary != null) {
                float brightness = 1.0F - level.random.nextFloat() * 0.5F;
                secondary.setColor(0.5F * brightness, 0.5F * brightness, 0.5F * brightness);
                secondary.scale(index + 1.0F);
            }
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        Iterator<SpentCasingEffect> iterator = CASINGS.iterator();
        while (iterator.hasNext()) {
            SpentCasingEffect casing = iterator.next();
            if (level == null || casing.level != level || !casing.tick()) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || CASINGS.isEmpty()) {
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
        for (SpentCasingEffect casing : CASINGS) {
            if (casing.level != level) {
                continue;
            }
            Vec3 position = casing.previousPosition.lerp(casing.position, partialTick);
            float pitch = Mth.lerp(partialTick, casing.previousPitch, casing.pitch);
            float yaw = Mth.lerp(partialTick, casing.previousYaw, casing.yaw);
            poseStack.pushPose();
            poseStack.translate(
                    position.x - camera.x,
                    position.y - camera.y - casing.height * 0.25D + casing.config.scaleY * 0.01D,
                    position.z - camera.z
            );
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
            poseStack.scale(
                    MODEL_SCALE * casing.config.scaleX,
                    MODEL_SCALE * casing.config.scaleY,
                    MODEL_SCALE * casing.config.scaleZ
            );
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(position));
            MachineModelRenderer.renderUnculledTinted(
                    MachineModelRenderer.model(casing.config.model),
                    poseStack,
                    buffers,
                    Blocks.STONE.defaultBlockState(),
                    light,
                    OverlayTexture.NO_OVERLAY,
                    0xFF000000 | casing.config.color
            );
            poseStack.popPose();
        }
        VertexConsumer smokeConsumer = buffers.getBuffer(RenderType.lightning());
        for (SpentCasingEffect casing : CASINGS) {
            if (casing.level != level || casing.smokeNodes.size() < 2) {
                continue;
            }
            Vec3 position = casing.previousPosition.lerp(casing.position, partialTick);
            casing.updateSmokeRenderDelta(position);
            double width = casing.config.scaleX * 0.5D * MODEL_SCALE;
            Vec3 side = new Vec3(width, 0.0D, 0.0D).yRot((float) Math.toRadians(-event.getCamera().getYRot()));
            float timeAlpha = Math.max(0.0F, 1.0F - (float) casing.age / (float) casing.maxSmokeGen);
            poseStack.pushPose();
            poseStack.translate(
                    position.x - camera.x,
                    position.y - camera.y - casing.height * 0.25D,
                    position.z - camera.z
            );
            PoseStack.Pose pose = poseStack.last();
            for (int index = 0; index < casing.smokeNodes.size() - 1; index++) {
                SmokeNode node = casing.smokeNodes.get(index);
                SmokeNode past = casing.smokeNodes.get(index + 1);
                int nodeAlpha = Mth.clamp((int) (node.alpha * timeAlpha * 255.0D), 0, 255);
                int pastAlpha = Mth.clamp((int) (past.alpha * timeAlpha * 255.0D), 0, 255);
                smokeQuad(smokeConsumer, pose, node.position, past.position, side, nodeAlpha, pastAlpha);
                smokeQuad(smokeConsumer, pose, node.position, past.position, side.scale(-1.0D), nodeAlpha, pastAlpha);
            }
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    private static void smokeQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 node, Vec3 past,
                                  Vec3 side, int nodeAlpha, int pastAlpha) {
        smokeVertex(consumer, pose, node, nodeAlpha);
        smokeVertex(consumer, pose, node.add(side), 0);
        smokeVertex(consumer, pose, past.add(side), 0);
        smokeVertex(consumer, pose, past, pastAlpha);
    }

    private static void smokeVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 position, int alpha) {
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(255, 255, 255, alpha)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static final class SpentCasingEffect {
        private final ClientLevel level;
        private final CasingConfig config;
        private final double width;
        private final double height;
        private Vec3 position;
        private Vec3 previousPosition;
        private Vec3 motion;
        private float pitch;
        private float previousPitch;
        private float yaw;
        private float previousYaw;
        private float momentumPitch;
        private float momentumYaw;
        private final boolean smoking;
        private final int maxSmokeGen;
        private final double smokeLift;
        private final int smokeNodeLife;
        private final List<SmokeNode> smokeNodes = new ArrayList<>();
        private Vec3 previousSmokeRenderPosition;
        private int age;

        private SpentCasingEffect(ClientLevel level, TurretCasingEffectPayload payload, CasingConfig config) {
            this.level = level;
            this.config = config;
            this.position = new Vec3(payload.x(), payload.y(), payload.z());
            this.previousPosition = this.position;
            this.motion = new Vec3(payload.motionX(), payload.motionY(), payload.motionZ());
            this.pitch = payload.rotationPitch();
            this.previousPitch = this.pitch;
            this.yaw = payload.rotationYaw();
            this.previousYaw = this.yaw;
            this.momentumPitch = payload.momentumPitch();
            this.momentumYaw = payload.momentumYaw();
            this.smoking = payload.smoking();
            this.maxSmokeGen = payload.smokeLife();
            this.smokeLift = payload.smokeLift();
            this.smokeNodeLife = payload.smokeNodeLife();
            this.width = 2.0D * MODEL_SCALE * Math.max(config.scaleX, config.scaleZ);
            this.height = MODEL_SCALE * config.scaleY;
        }

        private boolean tick() {
            if (this.age++ >= this.config.maxAge) {
                return false;
            }
            this.previousPosition = this.position;
            this.motion = this.motion.add(0.0D, -0.04D, 0.0D);
            Vec3 requested = this.motion;
            Vec3 accepted = collide(requested);
            this.position = this.position.add(accepted);

            boolean collidedX = Math.abs(requested.x - accepted.x) > 1.0E-7D;
            boolean collidedY = Math.abs(requested.y - accepted.y) > 1.0E-7D;
            boolean collidedZ = Math.abs(requested.z - accepted.z) > 1.0E-7D;
            double motionX = collidedX ? requested.x * -0.25D : requested.x;
            double motionY = collidedY ? requested.y * -0.5D : requested.y;
            double motionZ = collidedZ ? requested.z * -0.25D : requested.z;

            if (collidedX) {
                this.momentumYaw = Math.abs(this.momentumYaw) > 1.0E-7F
                        ? this.momentumYaw * -0.75F
                        : (float) this.level.random.nextGaussian() * 10.0F * this.config.bounceYaw;
            }
            if (collidedY) {
                boolean rotationFromSpeed = Math.abs(motionY) > 0.04D;
                if (rotationFromSpeed || Math.abs(this.momentumPitch) > 1.0E-7F) {
                    this.momentumPitch *= -0.75F;
                    if (rotationFromSpeed) {
                        float multiplier = Mth.clamp((float) (requested.y / 0.2D), -1.0F, 1.0F);
                        this.momentumPitch += (float) this.level.random.nextGaussian() * 10.0F * this.config.bouncePitch * multiplier;
                        this.momentumYaw += (float) this.level.random.nextGaussian() * 10.0F * this.config.bounceYaw * multiplier;
                    }
                }
            }
            if (collidedZ) {
                this.momentumYaw = Math.abs(this.momentumYaw) > 1.0E-7F
                        ? this.momentumYaw * -0.75F
                        : (float) this.level.random.nextGaussian() * 10.0F * this.config.bounceYaw;
            }

            this.motion = new Vec3(motionX * 0.98D, motionY * 0.98D, motionZ * 0.98D);
            boolean onGround = collidedY && requested.y < 0.0D;
            if (onGround) {
                this.motion = new Vec3(this.motion.x * 0.7D, this.motion.y, this.motion.z * 0.7D);
                this.pitch = Math.round(this.pitch / 180.0F) * 180.0F;
                this.momentumYaw *= 0.7F;
            }
            if (collidedY && Math.abs(requested.y) >= 0.2D) {
                this.level.playLocalSound(
                        this.position.x, this.position.y, this.position.z,
                        this.config.sound(), SoundSource.PLAYERS,
                        this.config.soundSize == SoundSize.LARGE ? 1.0F : 0.5F,
                        1.0F + this.level.random.nextFloat() * 0.2F,
                        false
                );
            }

            if (this.age > this.maxSmokeGen && !this.smokeNodes.isEmpty()) {
                this.smokeNodes.clear();
            }
            if (this.smoking && this.age <= this.maxSmokeGen) {
                double fade = this.smokeNodeLife > 0 ? 1.0D / (double) this.smokeNodeLife : 1.0D;
                for (SmokeNode node : this.smokeNodes) {
                    node.position = node.position.add(
                            this.level.random.nextGaussian() * 0.001D,
                            this.smokeLift * MODEL_SCALE,
                            this.level.random.nextGaussian() * 0.001D
                    );
                    node.alpha = Math.max(0.0D, node.alpha - fade);
                }
                boolean inFluid = !this.level.getFluidState(BlockPos.containing(this.position)).isEmpty();
                if (this.age < this.maxSmokeGen || inFluid) {
                    this.smokeNodes.add(new SmokeNode(Vec3.ZERO, this.smokeNodes.isEmpty() ? 0.0D : 1.0D));
                }
            }

            this.previousPitch = this.pitch;
            this.previousYaw = this.yaw;
            this.pitch += this.momentumPitch;
            this.yaw += this.momentumYaw;
            if (Math.abs(this.previousPitch - this.pitch) > 180.0F) {
                this.previousPitch += this.previousPitch < this.pitch ? 360.0F : -360.0F;
            }
            if (Math.abs(this.previousYaw - this.yaw) > 180.0F) {
                this.previousYaw += this.previousYaw < this.yaw ? 360.0F : -360.0F;
            }
            return true;
        }

        private void updateSmokeRenderDelta(Vec3 renderPosition) {
            if (this.previousSmokeRenderPosition == null) {
                this.previousSmokeRenderPosition = renderPosition;
                return;
            }
            Vec3 delta = this.previousSmokeRenderPosition.subtract(renderPosition);
            for (SmokeNode node : this.smokeNodes) {
                node.position = node.position.add(delta);
            }
            this.previousSmokeRenderPosition = renderPosition;
        }

        private Vec3 collide(Vec3 requested) {
            AABB box = new AABB(
                    this.position.x - this.width * 0.5D,
                    this.position.y - this.height * 0.5D,
                    this.position.z - this.width * 0.5D,
                    this.position.x + this.width * 0.5D,
                    this.position.y + this.height * 0.5D,
                    this.position.z + this.width * 0.5D
            );
            AABB swept = box.expandTowards(requested);
            List<VoxelShape> collisions = StreamSupport.stream(this.level.getBlockCollisions(null, swept).spliterator(), false).toList();
            double y = Shapes.collide(Direction.Axis.Y, box, collisions, requested.y);
            box = box.move(0.0D, y, 0.0D);
            double x = Shapes.collide(Direction.Axis.X, box, collisions, requested.x);
            box = box.move(x, 0.0D, 0.0D);
            double z = Shapes.collide(Direction.Axis.Z, box, collisions, requested.z);
            return new Vec3(x, y, z);
        }
    }

    private static final class SmokeNode {
        private Vec3 position;
        private double alpha;

        private SmokeNode(Vec3 position, double alpha) {
            this.position = position;
            this.alpha = alpha;
        }
    }

    private enum SoundSize {
        SMALL,
        MEDIUM,
        LARGE
    }

    private enum CasingConfig {
        FRIENDLY(BOTTLENECK, 0.8F, 0.8F, 0.8F, 0xEBC35E, SoundSize.SMALL, 1.0F, 1.0F, 240),
        BMG50(BOTTLENECK, 1.5F, 1.5F, 1.5F, 0xEBC35E, SoundSize.MEDIUM, 1.0F, 1.0F, 240),
        P9(STRAIGHT, 1.0F, 1.0F, 0.75F, 0xEBC35E, SoundSize.SMALL, 1.0F, 1.0F, 240),
        SHELL_240(BOTTLENECK, 7.5F, 7.5F, 7.5F, 0xEBC35E, SoundSize.LARGE, 0.02F, 0.05F, 240),
        ARTY_16(STRAIGHT, 15.0F, 15.0F, 10.0F, 0xD89128, SoundSize.LARGE, 1.0F, 0.5F, 300),
        ARTY_16_PHOS(STRAIGHT, 15.0F, 15.0F, 10.0F, 0xC8C8C8, SoundSize.LARGE, 1.0F, 0.5F, 300),
        ARTY_16_NUKE(STRAIGHT, 15.0F, 15.0F, 10.0F, 0x495443, SoundSize.LARGE, 1.0F, 0.5F, 300),
        HOWARD(STRAIGHT, 1.5F, 1.5F, 1.5F, 0xEBC35E, SoundSize.MEDIUM, 1.0F, 0.5F, 60),
        FRIENDLY_STEEL(BOTTLENECK, 0.8F, 0.8F, 0.8F, 0x3E3E3E, SoundSize.SMALL, 1.0F, 1.0F, 240),
        BMG50_STEEL(BOTTLENECK, 1.5F, 1.5F, 1.5F, 0x3E3E3E, SoundSize.MEDIUM, 1.0F, 1.0F, 240),
        P9_STEEL(STRAIGHT, 1.0F, 1.0F, 0.75F, 0x3E3E3E, SoundSize.SMALL, 1.0F, 1.0F, 240);

        private final ModelResourceLocation model;
        private final float scaleX;
        private final float scaleY;
        private final float scaleZ;
        private final int color;
        private final SoundSize soundSize;
        private final float bounceYaw;
        private final float bouncePitch;
        private final int maxAge;

        CasingConfig(ModelResourceLocation model, float scaleX, float scaleY, float scaleZ, int color,
                     SoundSize soundSize, float bounceYaw, float bouncePitch, int maxAge) {
            this.model = model;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.color = color;
            this.soundSize = soundSize;
            this.bounceYaw = bounceYaw;
            this.bouncePitch = bouncePitch;
            this.maxAge = maxAge;
        }

        private SoundEvent sound() {
            return switch (this.soundSize) {
                case SMALL -> HbmSoundEvents.WEAPON_CASING_SMALL.get();
                case MEDIUM -> HbmSoundEvents.WEAPON_CASING_MEDIUM.get();
                case LARGE -> HbmSoundEvents.WEAPON_CASING_LARGE.get();
            };
        }

        private static CasingConfig byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : null;
        }
    }
}
