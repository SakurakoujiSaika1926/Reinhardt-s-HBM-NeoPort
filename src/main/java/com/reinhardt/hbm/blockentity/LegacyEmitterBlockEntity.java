package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LegacyEmitterBlock;
import com.reinhardt.hbm.entity.LegacyShrapnelEntity;
import com.reinhardt.hbm.item.ArmorFSBItem;
import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class LegacyEmitterBlockEntity extends BlockEntity {
    private final List<CloudTrace> clouds = new ArrayList<>();
    private int timer;

    public LegacyEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LEGACY_EMITTER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LegacyEmitterBlockEntity blockEntity) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)
                || !(state.getBlock() instanceof LegacyEmitterBlock emitter)) {
            return;
        }

        blockEntity.tickClouds(serverLevel);
        if (emitter.kind().isGeyser()) {
            blockEntity.tickGeyser(serverLevel, pos, state, emitter.kind());
        } else if (emitter.kind().isVent()) {
            blockEntity.tickVent(serverLevel, pos, emitter.kind());
        } else if (emitter.kind() == LegacyEmitterBlock.Kind.VENT_CHLORINE_SEAL
                && level.hasNeighborSignal(pos)) {
            blockEntity.spreadChlorine(serverLevel, pos);
        }
    }

    private void tickGeyser(ServerLevel level, BlockPos pos, BlockState state, LegacyEmitterBlock.Kind kind) {
        if (!level.isEmptyBlock(pos.above())) {
            return;
        }

        timer--;
        boolean wasActive = state.getValue(LegacyEmitterBlock.ACTIVE);
        if (timer <= 0) {
            timer = geyserDelay(level.random, kind, wasActive);
            level.setBlock(pos, state.setValue(LegacyEmitterBlock.ACTIVE, !wasActive), Block.UPDATE_CLIENTS);
        }

        // The old tile entity performs once more on the transition tick because it reads metadata before toggling.
        if (wasActive) {
            if (kind == LegacyEmitterBlock.Kind.GEYSER_CHLORINE) {
                for (int i = 0; i < 3; i++) {
                    addCloud(level, CloudKind.ORANGE,
                            pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D,
                            level.random.nextGaussian() * 0.45D,
                            timer * 0.3D,
                            level.random.nextGaussian() * 0.45D);
                }
            } else {
                eruptNether(level, pos);
            }
        }
    }

    private static int geyserDelay(RandomSource random, LegacyEmitterBlock.Kind kind, boolean active) {
        if (kind == LegacyEmitterBlock.Kind.GEYSER_CHLORINE) {
            return active ? 400 + random.nextInt(100) : 20;
        }
        return active ? 80 + random.nextInt(60) : random.nextBoolean() ? 300 : 450;
    }

    private void eruptNether(ServerLevel level, BlockPos pos) {
        AABB range = new AABB(pos).inflate(32.0D);
        if (level.getEntitiesOfClass(Player.class, range, Player::isAlive).isEmpty()) {
            return;
        }
        if (level.random.nextInt(3) == 0) {
            level.addFreshEntity(new LegacyShrapnelEntity(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 1.5D,
                    pos.getZ() + 0.5D,
                    new Vec3(
                            level.random.nextGaussian() * 0.05D,
                            0.5D + level.random.nextDouble() * timer * 0.01D,
                            level.random.nextGaussian() * 0.05D
                    ),
                    true
            ));
        }
        if ((timer & 1) == 0) {
            sendParticle(level, HbmParticleTypes.GEYSER_FIRE.get(),
                    pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                    level.random.nextGaussian() * 0.05D, 0.2D,
                    level.random.nextGaussian() * 0.05D);
        }
    }

    private void tickVent(ServerLevel level, BlockPos pos, LegacyEmitterBlock.Kind kind) {
        if (!level.hasNeighborSignal(pos)) {
            return;
        }
        double spread = kind == LegacyEmitterBlock.Kind.VENT_CHLORINE ? 1.5D
                : kind == LegacyEmitterBlock.Kind.VENT_CLOUD ? 1.75D : 2.0D;
        double x = level.random.nextGaussian() * spread;
        double y = level.random.nextGaussian() * spread;
        double z = level.random.nextGaussian() * spread;
        BlockPos spawnBlock = pos.offset((int) x, (int) y, (int) z);
        if (level.getBlockState(spawnBlock).isSolidRender(level, spawnBlock)) {
            return;
        }
        CloudKind cloudKind = switch (kind) {
            case VENT_CHLORINE -> CloudKind.CHLORINE;
            case VENT_CLOUD -> CloudKind.CLOUD;
            case VENT_PINK_CLOUD -> CloudKind.PINK;
            default -> throw new IllegalStateException("Unsupported vent kind " + kind);
        };
        addCloud(level, cloudKind,
                spawnBlock.getX(), spawnBlock.getY(), spawnBlock.getZ(),
                x * 0.5D, y * 0.5D, z * 0.5D);
    }

    private void addCloud(ServerLevel level, CloudKind kind, double x, double y, double z,
                          double motionX, double motionY, double motionZ) {
        int lifetime = switch (kind) {
            case CHLORINE -> 700 + level.random.nextInt(101);
            case CLOUD, PINK, ORANGE -> 900 + level.random.nextInt(301);
        };
        clouds.add(new CloudTrace(kind, x, y, z, motionX, motionY, motionZ, lifetime));

        ParticleOptions particle = switch (kind) {
            case CHLORINE -> HbmParticleTypes.LEGACY_CHLORINE_CLOUD.get();
            case CLOUD -> HbmParticleTypes.LEGACY_CLOUD.get();
            case PINK -> HbmParticleTypes.LEGACY_PINK_CLOUD.get();
            case ORANGE -> HbmParticleTypes.LEGACY_ORANGE_CLOUD.get();
        };
        for (int i = 0; i < 5; i++) {
            sendParticle(level, particle,
                    x + level.random.nextGaussian() * 0.15D,
                    y + level.random.nextGaussian() * 0.15D,
                    z + level.random.nextGaussian() * 0.15D,
                    motionX, motionY, motionZ);
        }
    }

    private static void sendParticle(ServerLevel level, ParticleOptions particle,
                                     double x, double y, double z,
                                     double motionX, double motionY, double motionZ) {
        level.sendParticles(particle, x, y, z, 0, motionX, motionY, motionZ, 1.0D);
    }

    private void tickClouds(ServerLevel level) {
        Iterator<CloudTrace> iterator = clouds.iterator();
        while (iterator.hasNext()) {
            CloudTrace cloud = iterator.next();
            if (!cloud.tick(level)) {
                iterator.remove();
            }
        }
    }

    private void spreadChlorine(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        for (int step = 0; step <= 50; step++) {
            BlockState state = level.getBlockState(cursor);
            if (state.canBeReplaced()) {
                level.setBlock(cursor, HbmBlocks.CHLORINE_GAS.get().defaultBlockState(), Block.UPDATE_ALL);
                state = level.getBlockState(cursor);
            }
            if (!state.is(HbmBlocks.CHLORINE_GAS.get()) && !state.is(HbmBlocks.VENT_CHLORINE_SEAL.get())) {
                return;
            }
            cursor.move(Direction.getRandom(level.random));
        }
    }

    private enum CloudKind {
        CHLORINE,
        CLOUD,
        PINK,
        ORANGE
    }

    private static final class CloudTrace {
        private final CloudKind kind;
        private final int lifetime;
        private double x;
        private double y;
        private double z;
        private double motionX;
        private double motionY;
        private double motionZ;
        private int age;

        private CloudTrace(CloudKind kind, double x, double y, double z,
                           double motionX, double motionY, double motionZ, int lifetime) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.lifetime = lifetime;
        }

        private boolean tick(ServerLevel level) {
            if (++age >= lifetime) {
                return false;
            }
            if (level.random.nextInt(50) == 0) {
                applyEffect(level);
            }

            if (kind == CloudKind.ORANGE) {
                motionX *= 0.86D;
                motionY = motionY * 0.86D - 0.1D;
                motionZ *= 0.86D;
            } else {
                motionX *= 0.76D;
                motionY *= 0.76D;
                motionZ *= 0.76D;
                if (level.isRainingAt(BlockPos.containing(x, y, z))) {
                    motionY -= 0.01D;
                }
            }

            for (int i = 0; i < 4; i++) {
                x += motionX * 0.25D;
                y += motionY * 0.25D;
                z += motionZ * 0.25D;
                BlockPos blockPos = BlockPos.containing(x, y, z);
                BlockState state = level.getBlockState(blockPos);
                boolean collided = kind == CloudKind.ORANGE
                        ? !state.isAir()
                        : state.isSolidRender(level, blockPos);
                if (!collided) {
                    continue;
                }
                x -= motionX * 0.25D;
                y -= motionY * 0.25D;
                z -= motionZ * 0.25D;
                if (kind == CloudKind.ORANGE) {
                    agentOrangeTerrain(level, blockPos);
                    return false;
                }
                motionX = 0.0D;
                motionY = 0.0D;
                motionZ = 0.0D;
                return level.random.nextInt(5) == 0;
            }
            return true;
        }

        private void applyEffect(ServerLevel level) {
            AABB bounds = new AABB(x - 2.0D, y - 2.0D, z - 2.0D, x + 2.0D, y + 2.0D, z + 2.0D);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, bounds, LivingEntity::isAlive)) {
                if (living.distanceToSqr(x, y, z) > 4.0D) {
                    continue;
                }
                if (kind == CloudKind.CHLORINE || kind == CloudKind.ORANGE) {
                    applyPoison(living);
                } else {
                    damageArmor(living);
                    if (kind == CloudKind.PINK || !hasFullHazmat(living)) {
                        living.hurt(level.damageSources().source(HbmDamageTypes.CLOUD), 5.0F);
                    }
                }
            }
        }

        private static void applyPoison(LivingEntity living) {
            if (HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.GAS_LUNG, 1)
                    || HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.GAS_BLISTERING, 1)) {
                return;
            }
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0));
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 20, 2));
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 1));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 * 20, 1));
            living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30 * 20, 2));
        }

        private static void damageArmor(LivingEntity living) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!slot.isArmor()) {
                    continue;
                }
                ItemStack stack = living.getItemBySlot(slot);
                if (!stack.isEmpty() && stack.isDamageableItem()) {
                    stack.hurtAndBreak(25, living, slot);
                }
            }
        }

        private static boolean hasFullHazmat(LivingEntity living) {
            String group = ArmorFSBItem.fullSetGroup(living);
            return group.startsWith("hazmat") || group.equals("liquidator")
                    || group.equals("schrabidium") || group.equals("euphemium")
                    || group.equals("rpa") || group.equals("fau") || group.equals("dns");
        }

        private static void agentOrangeTerrain(ServerLevel level, BlockPos center) {
            for (BlockPos target : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
                BlockState state = level.getBlockState(target);
                if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM)
                        || state.is(blockById("waste_earth")) || state.is(blockById("waste_mycelium"))) {
                    level.setBlock(target, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                } else if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)
                        || state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)
                        || state.is(BlockTags.SAPLINGS)
                        || state.is(Blocks.CACTUS) || state.is(Blocks.SPONGE)
                        || state.is(Blocks.WET_SPONGE) || state.is(Blocks.VINE)
                        || state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)) {
                    level.removeBlock(target, false);
                }
            }
        }

        private static Block blockById(String id) {
            ResourceLocation key = ReinhardtsHBM.id(id);
            return BuiltInRegistries.BLOCK.containsKey(key) ? BuiltInRegistries.BLOCK.get(key) : Blocks.AIR;
        }
    }
}
