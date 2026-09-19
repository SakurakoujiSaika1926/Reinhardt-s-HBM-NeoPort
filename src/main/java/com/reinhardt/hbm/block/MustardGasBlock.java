package com.reinhardt.hbm.block;

import com.reinhardt.hbm.pollution.HbmArmorProtection;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MustardGasBlock extends ChlorineGasBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 150);
    private static final int MAX_AGE = 150;
    public static final int TICK_INTERVAL = 10;
    private static final int LEGACY_MUSTARD_CLOUD_WIDTH = 20;
    private static final int LEGACY_MUSTARD_CLOUD_HEIGHT = 10;
    private static final int DRAIN_PLUME_REACH = LEGACY_MUSTARD_CLOUD_WIDTH / 2;
    private static final int DRAIN_PLUME_HEIGHT = LEGACY_MUSTARD_CLOUD_HEIGHT / 2;

    public MustardGasBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        applyCloudExposure(level, pos);
        int age = state.getValue(AGE) + TICK_INTERVAL;
        if (age >= MAX_AGE) {
            level.removeBlock(pos, false);
            return;
        }
        level.setBlock(pos, state.setValue(AGE, age), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, this, TICK_INTERVAL);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide || random.nextInt(4) != 0) {
            return;
        }
        Player viewer = level.getNearestPlayer(
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 64.0D,
                entity -> entity instanceof LivingEntity living
                        && living.getItemBySlot(EquipmentSlot.HEAD).is(HbmItems.ASHGLASSES.get())
        );
        if (viewer == null) {
            return;
        }
        Set<BlockPos> reachable = collectReachableCells(level, pos);
        if (reachable.isEmpty()) {
            return;
        }
        ArrayList<BlockPos> cells = new ArrayList<>(reachable);
        BlockPos particlePos = cells.get(random.nextInt(cells.size()));
        level.addParticle(HbmParticleTypes.MUSTARD_GAS_CLOUD.get(),
                particlePos.getX() + 0.35D + random.nextDouble() * 0.30D,
                particlePos.getY() + 0.25D + random.nextDouble() * 0.35D,
                particlePos.getZ() + 0.35D + random.nextDouble() * 0.30D,
                (random.nextDouble() - 0.5D) * 0.025D,
                (random.nextDouble() - 0.5D) * 0.012D,
                (random.nextDouble() - 0.5D) * 0.025D);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Mustard gas is handled by the scheduled single-source cloud scan.
        // Do not delegate to ChlorineGasBlock here; mustard and chlorine have different legacy toxin entries.
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    public static void applyCloudExposure(Level level, BlockPos pos) {
        Set<BlockPos> reachable = collectReachableCells(level, pos);
        AABB plume = cloudBounds(pos);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, plume, LivingEntity::isAlive)) {
            if (occupiesReachableCell(living, reachable)) {
                applyBlisteringEffects(level, living);
            }
        }
    }

    private static void applyBlisteringEffects(Level level, LivingEntity living) {
        boolean maskProtected = HbmArmorProtection.hasHeadProtection(
                living,
                HbmArmorProtection.HazardClass.GAS_BLISTERING,
                1
        );
        if (!maskProtected) {
            living.hurt(level.damageSources().source(HbmDamageTypes.CLOUD), 4.0F);
        }
        if (!maskProtected || !HbmArmorProtection.hasLegacyHazmatProtection(living)) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }
    }

    private static boolean occupiesReachableCell(LivingEntity living, Set<BlockPos> reachable) {
        AABB bounds = living.getBoundingBox();
        int minX = Mth.floor(bounds.minX);
        int minY = Mth.floor(bounds.minY);
        int minZ = Mth.floor(bounds.minZ);
        int maxX = Mth.floor(bounds.maxX - 1.0E-7D);
        int maxY = Mth.floor(bounds.maxY - 1.0E-7D);
        int maxZ = Mth.floor(bounds.maxZ - 1.0E-7D);
        for (BlockPos occupied : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (reachable.contains(occupied)) {
                return true;
            }
        }
        return false;
    }

    private static Set<BlockPos> collectReachableCells(Level level, BlockPos source) {
        Set<BlockPos> reachable = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        reachable.add(source);
        pending.add(source);
        while (!pending.isEmpty()) {
            BlockPos current = pending.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!withinCloudBounds(source, next) || reachable.contains(next) || !canGasOccupy(level, next)) {
                    continue;
                }
                reachable.add(next);
                pending.add(next);
            }
        }
        return reachable;
    }

    private static boolean withinCloudBounds(BlockPos source, BlockPos target) {
        return Math.abs(target.getX() - source.getX()) <= DRAIN_PLUME_REACH
                && Math.abs(target.getY() - source.getY()) <= DRAIN_PLUME_HEIGHT
                && Math.abs(target.getZ() - source.getZ()) <= DRAIN_PLUME_REACH;
    }

    private static boolean canGasOccupy(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir()
                || state.is(com.reinhardt.hbm.registry.HbmBlocks.MUSTARD_GAS.get())
                || (state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty());
    }

    private static AABB cloudBounds(BlockPos pos) {
        return new AABB(pos).inflate(DRAIN_PLUME_REACH, DRAIN_PLUME_HEIGHT, DRAIN_PLUME_REACH);
    }
}
