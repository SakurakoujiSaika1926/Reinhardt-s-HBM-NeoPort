package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmMobEffects;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Spreading IMP residue copied from the 1.7.10 BlockTaint update rules. */
public class TaintBlock extends Block {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 15);
    private static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.75D, 1.0D);

    public TaintBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age >= 15) {
            return;
        }
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (Math.abs(x) + Math.abs(y) + Math.abs(z) > 4 || random.nextFloat() > 0.25F) {
                        continue;
                    }
                    BlockPos target = pos.offset(x, y, z);
                    BlockState targetState = level.getBlockState(target);
                    if (targetState.isAir() || targetState.is(Blocks.BEDROCK)) {
                        continue;
                    }
                    int targetAge = age + (hasAirNeighbor(level, target) ? 1 : 3);
                    if (targetAge > 15 || targetState.is(this) && targetState.getValue(AGE) >= targetAge) {
                        continue;
                    }
                    level.setBlock(target, defaultBlockState().setValue(AGE, targetAge), Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
        if (!level.isClientSide && entity instanceof LivingEntity living && level.random.nextInt(50) == 0) {
            int amplifier = 15 - state.getValue(AGE);
            living.addEffect(new MobEffectInstance(HbmMobEffects.TAINT, 15 * 20, amplifier));
        }
        if (!level.isClientSide && entity.getClass() == Creeper.class) {
            com.reinhardt.hbm.entity.LegacyTaintedCreeperEntity creeper =
                    new com.reinhardt.hbm.entity.LegacyTaintedCreeperEntity(HbmEntityTypes.TAINTED_CREEPER.get(), level);
            creeper.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
            entity.discard();
            level.addFreshEntity(creeper);
        }
        if (!level.isClientSide && entity instanceof com.reinhardt.hbm.entity.LegacyTeslaCrabEntity) {
            com.reinhardt.hbm.entity.LegacyTaintCrabEntity crab =
                    new com.reinhardt.hbm.entity.LegacyTaintCrabEntity(HbmEntityTypes.TAINT_CRAB.get(), level);
            crab.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
            entity.discard();
            level.addFreshEntity(crab);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    private static boolean hasAirNeighbor(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isAir()) {
                return true;
            }
        }
        return false;
    }
}
