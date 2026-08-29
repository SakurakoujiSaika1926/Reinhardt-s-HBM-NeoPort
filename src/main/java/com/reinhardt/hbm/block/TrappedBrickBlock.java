package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** 1.7.10 TrappedBrick, including both contact and detector traps. */
public final class TrappedBrickBlock extends Block {
    public static final IntegerProperty TRAP = IntegerProperty.create("trap", 0, 14);
    public static final int MAX_TRAP = 14;

    public TrappedBrickBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(TRAP, 0));
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!level.isClientSide && entity instanceof Player && Trap.byId(state.getValue(TRAP)).contact) {
            trigger(level, pos, state, (Player) entity);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && Trap.byId(state.getValue(TRAP)).detector) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Trap trap = Trap.byId(state.getValue(TRAP));
        if (!trap.detector) {
            return;
        }
        AABB detector = detectorBounds(level, pos, trap);
        if (!level.getEntitiesOfClass(Player.class, detector).isEmpty()) {
            trigger(level, pos, state, null);
            return;
        }
        level.scheduleTick(pos, this, 1);
    }

    private static AABB detectorBounds(Level level, BlockPos pos, Trap trap) {
        return switch (trap) {
            case FALLING_ROCKS, SPIDERS -> new AABB(pos).inflate(1.0D).move(0.0D, -3.0D, 0.0D);
            case PILLAR -> new AABB(pos.getX() + 0.2D, pos.getY() - 3.0D, pos.getZ() + 0.2D,
                    pos.getX() + 0.8D, pos.getY(), pos.getZ() + 0.8D);
            case ARROW, FLAMING_ARROW, POISON_DART -> directionalDetector(level, pos);
            case ZOMBIE -> new AABB(pos).inflate(1.0D).move(0.0D, 1.0D, 0.0D);
            default -> new AABB(pos);
        };
    }

    private static AABB directionalDetector(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.isEmptyBlock(pos.relative(direction))) {
                double minX = pos.getX() + 0.4D;
                double minY = pos.getY() + 0.4D;
                double minZ = pos.getZ() + 0.4D;
                double maxX = pos.getX() + 0.6D;
                double maxY = pos.getY() + 0.6D;
                double maxZ = pos.getZ() + 0.6D;
                if (direction.getStepX() > 0) maxX += 3.0D;
                if (direction.getStepX() < 0) minX -= 3.0D;
                if (direction.getStepZ() > 0) maxZ += 3.0D;
                if (direction.getStepZ() < 0) minZ -= 3.0D;
                return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
            }
        }
        return new AABB(pos);
    }

    private static void trigger(Level level, BlockPos pos, BlockState state, Player player) {
        Trap trap = Trap.byId(state.getValue(TRAP));
        switch (trap) {
            case FIRE -> setIfReplaceable(level, pos.above(), Blocks.FIRE.defaultBlockState());
            case SPIKES -> {
                setIfReplaceable(level, pos.above(), HbmBlocks.SPIKES.get().defaultBlockState());
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos.above()))) {
                    target.hurt(level.damageSources().generic(), 10.0F);
                }
            }
            case MINE -> level.explode(null, pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D,
                    1.0F, Level.ExplosionInteraction.BLOCK);
            case WEB -> setIfReplaceable(level, pos.above(), Blocks.COBWEB.defaultBlockState());
            case RAD_CONVERSION -> convert(level, pos, HbmBlocks.BRICK_JUNGLE_OOZE.get());
            case MAGIC_CONVERSION -> convert(level, pos, HbmBlocks.BRICK_JUNGLE_MYSTIC.get());
            case SLOWNESS -> effect(player, MobEffects.MOVEMENT_SLOWDOWN, 300, 2);
            case WEAKNESS -> effect(player, MobEffects.WEAKNESS, 300, 2);
            case FALLING_ROCKS -> {
                for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
                    var rubble = new com.reinhardt.hbm.entity.MineRubbleEntity(level,
                            pos.getX() - 0.5D + x, pos.getY() - 0.5D, pos.getZ() - 0.5D + z,
                            new Vec3(0.0D, -0.15D, 0.0D));
                    level.addFreshEntity(rubble);
                }
            }
            case ARROW, FLAMING_ARROW, POISON_DART -> fireArrow(level, pos, trap);
            case PILLAR -> {
                for (int i = 0; i < 3; i++) {
                    level.setBlock(pos.below(i + 1), HbmBlocks.CONCRETE_PILLAR.get().defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            case ZOMBIE -> {
                Zombie zombie = EntityType.ZOMBIE.create(level);
                if (zombie != null) {
                    zombie.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 0.0F, 0.0F);
                    level.addFreshEntity(zombie);
                }
            }
            case SPIDERS -> {
                for (int i = 0; i < 3; i++) {
                    CaveSpider spider = EntityType.CAVE_SPIDER.create(level);
                    if (spider != null) {
                        spider.moveTo(pos.getX() + 0.5D, pos.getY() - 1.0D, pos.getZ() + 0.5D, 0.0F, 0.0F);
                        level.addFreshEntity(spider);
                    }
                }
            }
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.BLOCKS, 0.3F, 0.6F);
        level.setBlock(pos, HbmBlocks.BRICK_JUNGLE.get().defaultBlockState(), Block.UPDATE_ALL);
    }

    private static void fireArrow(Level level, BlockPos pos, Trap trap) {
        Direction direction = Direction.NORTH;
        for (Direction candidate : Direction.Plane.HORIZONTAL) {
            if (level.isEmptyBlock(pos.relative(candidate))) {
                direction = candidate;
                break;
            }
        }
        Arrow arrow = new Arrow(EntityType.ARROW, level);
        arrow.moveTo(pos.getX() + 0.5D + direction.getStepX(), pos.getY() + 0.5D,
                pos.getZ() + 0.5D + direction.getStepZ(), 0.0F, 0.0F);
        arrow.shoot(direction.getStepX(), 0.0D, direction.getStepZ(), 1.0F, 0.0F);
        if (trap == Trap.FLAMING_ARROW) arrow.setRemainingFireTicks(60);
        level.addFreshEntity(arrow);
    }

    private static void convert(Level level, BlockPos pos, Block target) {
        for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) for (int z = -3; z <= 3; z++) {
            if (level.random.nextBoolean()) continue;
            BlockPos candidate = pos.offset(x, y, z);
            Block block = level.getBlockState(candidate).getBlock();
            if (block == HbmBlocks.BRICK_JUNGLE.get() || block == HbmBlocks.BRICK_JUNGLE_CRACKED.get()
                    || block == HbmBlocks.BRICK_JUNGLE_LAVA.get()) {
                level.setBlock(candidate, target.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static void effect(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                               int duration, int amplifier) {
        if (player != null) player.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    private static void setIfReplaceable(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).canBeReplaced()) level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRAP);
    }

    private enum Trap {
        FALLING_ROCKS(false, true), FIRE(true, false), ARROW(false, true), SPIKES(true, false), MINE(true, false),
        WEB(true, false), FLAMING_ARROW(false, true), PILLAR(false, true), RAD_CONVERSION(true, false),
        MAGIC_CONVERSION(true, false), SLOWNESS(true, false), WEAKNESS(true, false), POISON_DART(false, true),
        ZOMBIE(false, true), SPIDERS(false, true);

        private final boolean contact;
        private final boolean detector;

        Trap(boolean contact, boolean detector) {
            this.contact = contact;
            this.detector = detector;
        }

        private static Trap byId(int id) {
            return values()[Math.max(0, Math.min(values().length - 1, id))];
        }
    }
}
