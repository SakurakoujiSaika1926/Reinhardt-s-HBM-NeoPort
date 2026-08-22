package com.reinhardt.hbm.block;

import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RbmkDebrisBlock extends Block {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 15);

    private final Kind kind;

    public RbmkDebrisBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && oldState.getBlock() != state.getBlock()) {
            if (kind == Kind.BURNING && level.random.nextInt(3) == 0) {
                flame(level, pos, level.random, false);
            }
            if (kind.hasScheduledBehavior()) {
                level.scheduleTick(pos, this, tickDelay(level.random));
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        switch (kind) {
            case BURNING -> tickBurning(state, level, pos, random);
            case RADIATING -> tickRadiating(state, level, pos, random);
            case DIGAMMA -> tickDigamma(level, pos);
            default -> {
            }
        }
    }

    private void tickBurning(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0) {
            flame(level, pos, random, true);
        }
        Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos target = pos.relative(direction);
        if (random.nextInt(10) == 0 && level.isEmptyBlock(target)) {
            level.setBlock(target, HbmBlocks.GAS_MELTDOWN.get().defaultBlockState(), 3);
        }
        int cooldownChance = isFoamOrBoron(level.getBlockState(target)) ? 10 : 100;
        if (random.nextInt(cooldownChance) == 0) {
            level.setBlock(pos, HbmBlocks.PRIBRIS.get().defaultBlockState(), 3);
        } else {
            level.scheduleTick(pos, this, tickDelay(random));
        }
    }

    private void tickRadiating(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        radiate(level, pos);
        if (random.nextInt(5) == 0) {
            flame(level, pos, random, true);
        }
        Direction direction = Direction.values()[random.nextInt(Direction.values().length)];
        BlockPos target = pos.relative(direction);
        if (random.nextInt(10) == 0 && level.isEmptyBlock(target)) {
            level.setBlock(target, HbmBlocks.GAS_MELTDOWN.get().defaultBlockState(), 3);
        }
        int cooldownChance = isBoron(level.getBlockState(target)) ? 25 : 1000;
        if (random.nextInt(cooldownChance) == 0) {
            int age = state.getValue(AGE);
            if (age < 15) {
                level.setBlock(pos, state.setValue(AGE, age + 1), 2);
                level.scheduleTick(pos, this, tickDelay(random));
            } else {
                level.setBlock(pos, HbmBlocks.PRIBRIS_BURNING.get().defaultBlockState(), 3);
            }
        } else {
            level.scheduleTick(pos, this, tickDelay(random));
        }
    }

    private static boolean isFoamOrBoron(BlockState state) {
        return state.is(HbmBlocks.FOAM_LAYER.get())
                || state.is(HbmBlocks.BLOCK_FOAM.get())
                || isBoron(state);
    }

    private static boolean isBoron(BlockState state) {
        return state.is(HbmBlocks.SAND_BORON_LAYER.get())
                || state.is(HbmBlocks.SAND_BORON.get());
    }

    private void tickDigamma(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos target = pos.relative(direction);
            BlockState state = level.getBlockState(target);
            if ((state.getBlock() instanceof RbmkDebrisBlock debris && debris.kind != Kind.DIGAMMA)
                    || state.is(HbmBlocks.CORIUM_BLOCK.get())
                    || state.is(HbmBlocks.BLOCK_CORIUM.get())) {
                level.setBlock(target, HbmBlocks.PRIBRIS_DIGAMMA.get().defaultBlockState(), 3);
            }
        }
        level.scheduleTick(pos, this, 2);
    }

    private void radiate(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(100.0D);
        DamageSources damageSources = level.damageSources();
        Vec3 center = Vec3.atCenterOf(pos);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            Vec3 eye = entity.position().add(0.0D, entity.getEyeHeight(), 0.0D);
            double distance = Math.max(1.0D, eye.distanceTo(center));
            double resistance = Math.max(1.0D, shieldingResistance(level, center, eye, distance));
            float dose = (float) (1_000_000.0D / resistance / (distance * distance));
            HbmLivingRadiation data = HbmLivingRadiation.get(entity);
            data.addEnvironmentRadiation(dose);
            if (!(entity instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                data.addRadiation(dose);
            }
            HbmLivingRadiation.set(entity, data);
            if (distance < 5.0D) {
                entity.hurt(damageSources.inFire(), 100.0F);
            }
        }
    }

    private static double shieldingResistance(ServerLevel level, Vec3 center, Vec3 eye, double distance) {
        Vec3 direction = eye.subtract(center).normalize();
        double resistance = 0.0D;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 1; i < distance; i++) {
            int x = (int) Math.floor(center.x + direction.x * i);
            int y = (int) Math.floor(center.y + direction.y * i);
            int z = (int) Math.floor(center.z + direction.z * i);
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir()) {
                resistance += state.getBlock().getExplosionResistance();
            }
        }
        return resistance;
    }

    private static void flame(Level level, BlockPos pos, RandomSource random, boolean sound) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(HbmParticleTypes.RBMK_FIRE.get(),
                    pos.getX() + 0.25D + random.nextDouble() * 0.5D,
                    pos.getY() + 1.75D,
                    pos.getZ() + 0.25D + random.nextDouble() * 0.5D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (sound) {
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if ((kind == Kind.BURNING || kind == Kind.RADIATING) && random.nextInt(3) == 0) {
            level.addParticle(HbmParticleTypes.RBMK_FIRE.get(),
                    pos.getX() + 0.25D + random.nextDouble() * 0.5D,
                    pos.getY() + 1.1D + random.nextDouble() * 0.65D,
                    pos.getZ() + 0.25D + random.nextDouble() * 0.5D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!level.isClientSide() && kind.hasScheduledBehavior()) {
            level.scheduleTick(pos, this, tickDelay(level.getRandom()));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private int tickDelay(RandomSource random) {
        return switch (kind) {
            case RADIATING -> 20 + random.nextInt(20);
            case DIGAMMA -> 2;
            case BURNING -> 100 + random.nextInt(20);
            default -> 100;
        };
    }

    public enum Kind {
        NORMAL,
        BURNING,
        RADIATING,
        DIGAMMA;

        private boolean hasScheduledBehavior() {
            return this != NORMAL;
        }
    }
}
