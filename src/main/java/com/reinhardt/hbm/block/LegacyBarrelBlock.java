package com.reinhardt.hbm.block;

import com.reinhardt.hbm.entity.TimedExplosiveEntity;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.worldgen.NuclearFalloutTerrainEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Legacy 1.7.10 explosive, cryogenic, tainted, and radioactive barrel behavior. */
public class LegacyBarrelBlock extends Block {
    public static final BooleanProperty IGNITED = BooleanProperty.create("ignited");
    private static final VoxelShape BARREL_SHAPE = Shapes.box(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);
    private static final int LEGACY_POP_FUSE = 100;
    private static final int LEGACY_FLAMMABILITY = 15;
    private static final int LEGACY_FIRE_SPREAD_SPEED = 2;
    private final Kind kind;

    public LegacyBarrelBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(this.stateDefinition.any().setValue(IGNITED, false));
    }

    public boolean detonatesWhenShot() {
        return this.kind == Kind.RED || this.kind == Kind.PINK;
    }

    public void detonateOnShot(ServerLevel level, BlockPos pos, Entity source) {
        if (!level.getBlockState(pos).is(this)) {
            return;
        }
        level.removeBlock(pos, false);
        detonateAt(level, Vec3.atLowerCornerOf(pos), pos, null);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BARREL_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BARREL_SHAPE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && oldState.getBlock() != state.getBlock()) {
            if (this.kind.radioactive()) {
                level.scheduleTick(pos, this, 20);
            }
            if (this.kind.flammable() && touchesFire(level, pos)) {
                level.removeBlock(pos, false);
                primeFromBlock((ServerLevel) level, pos, null);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && this.kind.flammable() && !state.getValue(IGNITED) && touchesFire(level, pos)) {
            level.removeBlock(pos, false);
            primeFromBlock((ServerLevel) level, pos, null);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.kind.radioactive()) {
            ChunkRadiationData.get(level).incrementRadiation(pos, this.kind.radiation());
            level.scheduleTick(pos, this, 20);
        }
        if (state.getValue(IGNITED)) {
            level.removeBlock(pos, false);
            primeFromBlock(level, pos, null);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (this.kind.radioactive()) {
            level.addParticle(HbmParticleTypes.RADIATION_FOG.get(),
                    pos.getX() + 0.25D + random.nextDouble() * 0.5D,
                    pos.getY() + 1.1D,
                    pos.getZ() + 0.25D + random.nextDouble() * 0.5D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        if (level instanceof ServerLevel server && this.kind.primesWhenExploded()) {
            level.removeBlock(pos, false);
            primeFromBlock(server, pos, explosion == null ? null : explosion.getIndirectSourceEntity());
            return;
        }
        if (level instanceof ServerLevel server && this.kind != Kind.VITRIFIED) {
            level.removeBlock(pos, false);
            detonateAt(server, Vec3.atCenterOf(pos), pos, null);
            return;
        }
        super.onBlockExploded(state, level, pos, explosion);
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
        if (!(level instanceof ServerLevel server) || !this.kind.flammable()) {
            return;
        }
        level.removeBlock(pos, false);
        primeFromBlock(server, pos, igniter);
    }

    public boolean canDropFromExplosion(net.minecraft.world.level.Explosion explosion) {
        return false;
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return this.kind.flammable();
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return this.kind.flammable() ? LEGACY_FLAMMABILITY : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return this.kind.flammable() ? LEGACY_FIRE_SPREAD_SPEED : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IGNITED);
    }

    private static boolean touchesFire(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.FIRE)) {
                return true;
            }
        }
        return false;
    }

    private void primeFromBlock(ServerLevel level, BlockPos pos, @Nullable Entity owner) {
        TimedExplosiveEntity.Kind timedKind = this.kind.timedKind();
        if (timedKind == null) {
            detonateAt(level, Vec3.atCenterOf(pos), pos, owner);
            return;
        }
        TimedExplosiveEntity entity = new TimedExplosiveEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                owner,
                level.random.nextInt(LEGACY_POP_FUSE) + LEGACY_POP_FUSE / 2,
                timedKind
        );
        level.addFreshEntity(entity);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public static void detonatePrimed(ServerLevel level, Vec3 position, TimedExplosiveEntity.Kind timedKind, @Nullable Entity source) {
        Kind kind = Kind.byTimedKind(timedKind);
        if (kind == null) {
            return;
        }
        detonateAt(level, position, BlockPos.containing(position), source, kind);
    }

    private void detonateAt(ServerLevel level, Vec3 position, BlockPos blockPos, @Nullable Entity source) {
        detonateAt(level, position, blockPos, source, this.kind);
    }

    private static void detonateAt(ServerLevel level, Vec3 position, BlockPos blockPos, @Nullable Entity source, Kind kind) {
        switch (kind) {
            case RED, PINK -> level.explode(source, position.x, position.y, position.z, 2.5F, true, Level.ExplosionInteraction.TNT);
            case LOX -> {
                level.explode(source, position.x, position.y, position.z, 1.0F, false, Level.ExplosionInteraction.NONE);
                freeze(level, blockPos);
            }
            case TAINT -> {
                level.explode(source, position.x, position.y, position.z, 1.0F, false, Level.ExplosionInteraction.NONE);
                spreadTaint(level, blockPos);
            }
            case YELLOW -> detonateYellow(level, position, blockPos, source);
            case VITRIFIED -> {
                // 1.7.10 only gave this variant passive radiation; it must not inherit the yellow barrel detonation.
            }
        }
    }

    private static void freeze(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(8.0D);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.distanceToSqr(center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D) < 49.0D)) {
            BlockPos base = living.blockPosition();
            for (int x = base.getX() - 2; x <= base.getX(); x++) {
                for (int y = base.getY(); y <= base.getY() + 2; y++) {
                    for (int z = base.getZ() - 1; z <= base.getZ() + 1; z++) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.ICE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2 * 60 * 20, 4));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90 * 20, 2));
            living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 3 * 60 * 20, 2));
        }
    }

    private static void spreadTaint(ServerLevel level, BlockPos center) {
        for (int i = 0; i < 100; i++) {
            BlockPos target = center.offset(level.random.nextInt(9) - 4, level.random.nextInt(9) - 4, level.random.nextInt(9) - 4);
            BlockState state = level.getBlockState(target);
            if (!state.isAir() && state.isSolidRender(level, target)) {
                level.setBlock(target, HbmBlocks.TAINT.get().defaultBlockState().setValue(TaintBlock.AGE, 4 + level.random.nextInt(3)), Block.UPDATE_ALL);
            }
        }
    }

    private static void detonateYellow(ServerLevel level, Vec3 position, BlockPos pos, @Nullable Entity source) {
        if (level.random.nextInt(3) == 0) {
            level.setBlock(pos, HbmBlocks.TOXIC_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        } else {
            level.explode(source, position.x, position.y, position.z, 12.0F, true, Level.ExplosionInteraction.TNT);
        }
        NuclearFalloutTerrainEffects.schedule(level, pos, 35);
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos target = pos.offset(x, y, z);
                    if (level.random.nextInt(5) == 0 && level.getBlockState(target).isAir()) {
                        level.setBlock(target, HbmBlocks.GAS_RADON_DENSE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        ChunkRadiationData.get(level).incrementRadiation(pos, 35.0D);
    }

    public enum Kind {
        RED(true, 0.0D),
        PINK(true, 0.0D),
        LOX(false, 0.0D),
        TAINT(false, 0.0D),
        YELLOW(false, 5.0D),
        VITRIFIED(false, 0.5D);

        private final boolean flammable;
        private final double radiation;

        Kind(boolean flammable, double radiation) {
            this.flammable = flammable;
            this.radiation = radiation;
        }

        boolean flammable() {
            return this.flammable;
        }

        boolean radioactive() {
            return this.radiation > 0.0D;
        }

        double radiation() {
            return this.radiation;
        }

        boolean primesWhenExploded() {
            return this != VITRIFIED;
        }

        @Nullable
        TimedExplosiveEntity.Kind timedKind() {
            return switch (this) {
                case RED -> TimedExplosiveEntity.Kind.RED_BARREL;
                case PINK -> TimedExplosiveEntity.Kind.PINK_BARREL;
                case LOX -> TimedExplosiveEntity.Kind.LOX_BARREL;
                case TAINT -> TimedExplosiveEntity.Kind.TAINT_BARREL;
                case YELLOW -> TimedExplosiveEntity.Kind.YELLOW_BARREL;
                case VITRIFIED -> null;
            };
        }

        @Nullable
        static Kind byTimedKind(TimedExplosiveEntity.Kind timedKind) {
            return switch (timedKind) {
                case RED_BARREL -> RED;
                case PINK_BARREL -> PINK;
                case LOX_BARREL -> LOX;
                case TAINT_BARREL -> TAINT;
                case YELLOW_BARREL -> YELLOW;
                default -> null;
            };
        }
    }
}
