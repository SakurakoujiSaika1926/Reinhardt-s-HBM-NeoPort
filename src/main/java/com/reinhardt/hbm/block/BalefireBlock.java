package com.reinhardt.hbm.block;

import com.reinhardt.hbm.radiation.HbmLivingHazards;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The old HBM balefire block, including its long-range fire spread rules. */
public final class BalefireBlock extends BaseFireBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 15);
    public static final MapCodec<BalefireBlock> CODEC = simpleCodec(BalefireBlock::new);

    public BalefireBlock(Properties properties) {
        super(properties, 1.0F);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<BalefireBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canBurn(BlockState state) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, fireTickRate() + level.random.nextInt(10));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            return;
        }
        if (!this.canSurvive(state, level, pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        int age = state.getValue(AGE);
        if (age < 15) {
            level.scheduleTick(pos, this, fireTickRate() + random.nextInt(10));
        }

        if (!canNeighborBurn(level, pos)
                && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        if (age >= 15) {
            return;
        }

        tryCatchFire(level, pos.east(), 500, random, age, Direction.WEST);
        tryCatchFire(level, pos.west(), 500, random, age, Direction.EAST);
        tryCatchFire(level, pos.north(), 500, random, age, Direction.SOUTH);
        tryCatchFire(level, pos.south(), 500, random, age, Direction.NORTH);
        tryCatchFire(level, pos.above(), 300, random, age, Direction.DOWN);
        tryCatchFire(level, pos.below(), 300, random, age, Direction.UP);

        // HBM deliberately scans a 7 x 6 x 7 volume instead of vanilla's local fire scan.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 4; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    BlockPos candidate = pos.offset(dx, dy, dz);
                    BlockState candidateState = level.getBlockState(candidate);
                    if (candidateState.is(this) && candidateState.getValue(AGE) > age + 1) {
                        level.setBlock(candidate, defaultBlockState().setValue(AGE, age + 1), 3);
                        continue;
                    }
                    if (!level.isEmptyBlock(candidate)) {
                        continue;
                    }

                    int fireLimit = dy > 1 ? 100 + (dy - 1) * 100 : 100;
                    int spread = neighborEncouragement(level, candidate);
                    int adjusted = (spread + 40 + level.getDifficulty().getId() * 7) / (age + 30);
                    if (spread > 0 && adjusted > 0 && random.nextInt(fireLimit) <= adjusted) {
                        level.setBlock(candidate, defaultBlockState().setValue(AGE, age + 1), 3);
                    }
                }
            }
        }
    }

    private void tryCatchFire(Level level, BlockPos pos, int chance, RandomSource random, int age, Direction face) {
        BlockState targetState = level.getBlockState(pos);
        int flammability = targetState.getFlammability(level, pos, face);
        if (flammability <= 0 || random.nextInt(chance) >= flammability) {
            return;
        }

        level.setBlock(pos, defaultBlockState().setValue(AGE, age + 1), 3);
        targetState.onCaughtFire(level, pos, face, null);
    }

    private static boolean canNeighborBurn(LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.getBlockState(neighbor).isFlammable(level, neighbor, direction.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    private static int neighborEncouragement(LevelReader level, BlockPos pos) {
        if (!level.isEmptyBlock(pos)) {
            return 0;
        }
        int best = 0;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            best = Math.max(best, level.getBlockState(neighbor)
                    .getFlammability(level, neighbor, direction.getOpposite()));
        }
        return best;
    }

    private static int fireTickRate() {
        return 30;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.setRemainingFireTicks(200);
        if (entity instanceof LivingEntity living) {
            HbmLivingHazards hazards = HbmLivingHazards.get(living);
            hazards.extendBalefire(100);
            HbmLivingHazards.set(living, hazards);
        }
    }
}
