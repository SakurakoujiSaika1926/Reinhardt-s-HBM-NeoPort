package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Set;
import java.util.function.Supplier;

public final class LegacyHazardLiquidBlock extends LiquidBlock {
    private static final Vec3 STUCK_MULTIPLIER = new Vec3(0.25D, 0.05D, 0.25D);
    private static final DustParticleOptions SCHRAB_FOG = new DustParticleOptions(new Vector3f(0.0F, 0.7F, 0.7F), 1.25F);
    private static final Set<String> HAZMAT_SUIT_PREFIXES = Set.of(
            "hazmat", "hazmat_red", "hazmat_grey", "hazmat_paa", "liquidator",
            "schrabidium", "euphemium", "rpa", "fau", "dns"
    );

    private final Kind kind;

    public LegacyHazardLiquidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties, Kind kind) {
        super(fluid.get(), properties);
        this.kind = kind;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        reactToNeighbors(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        reactToNeighbors(level, pos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        entity.makeStuckInBlock(state, STUCK_MULTIPLIER);
        if (level.isClientSide) {
            return;
        }

        switch (this.kind) {
            case ACID -> entity.hurt(level.damageSources().source(HbmDamageTypes.ACID), 10_000.0F);
            case MUD -> {
                if (!(entity instanceof LivingEntity living) || !hasFullHazmat(living)) {
                    entity.hurt(level.damageSources().source(HbmDamageTypes.MUD_POISONING), 8.0F);
                }
            }
            case TOXIC, SCHRABIDIC -> irradiate(entity, 1.0F);
            case RAD_LAVA -> irradiate(entity, 5.0F);
            case SULFURIC_ACID -> corrode(level, entity);
        }
    }

    private static void corrode(Level level, Entity entity) {
        if (entity instanceof ItemEntity) {
            entity.setDeltaMovement(Vec3.ZERO);
            if (entity.tickCount % 20 == 0) {
                entity.hurt(level.damageSources().source(HbmDamageTypes.ACID), 0.5F);
            }
            if (entity.tickCount % 5 == 0 && level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY(), entity.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        } else {
            Vec3 motion = entity.getDeltaMovement();
            if (motion.y < -0.2D) {
                entity.setDeltaMovement(motion.x, motion.y * 0.5D, motion.z);
            }
            entity.hurt(level.damageSources().source(HbmDamageTypes.ACID), 5.0F);
        }
        if (entity.tickCount % 5 == 0) {
            level.playSound(null, entity.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 1.0F);
        }
    }

    private static void irradiate(Entity entity, float amount) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        HbmLivingRadiation radiation = HbmLivingRadiation.get(living);
        radiation.addEnvironmentRadiation(amount);
        if (!(living instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            radiation.addRadiation(amount);
        }
        HbmLivingRadiation.set(living, radiation);
    }

    private static boolean hasFullHazmat(LivingEntity living) {
        String prefix = null;
        for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            Item item = living.getItemBySlot(slot).getItem();
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(ReinhardtsHBM.MOD_ID)) {
                return false;
            }
            String path = id.getPath();
            String piecePrefix = HAZMAT_SUIT_PREFIXES.stream()
                    .filter(value -> path.startsWith(value + "_"))
                    .findFirst()
                    .orElse(null);
            if (piecePrefix == null || prefix != null && !prefix.equals(piecePrefix)) {
                return false;
            }
            prefix = piecePrefix;
        }
        return prefix != null;
    }

    private void reactToNeighbors(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        if (this.kind == Kind.ACID) {
            for (Direction direction : Direction.values()) {
                BlockPos target = pos.relative(direction);
                if (!level.getBlockState(target).is(this)) {
                    level.removeBlock(target, false);
                }
            }
            return;
        }
        if (this.kind == Kind.MUD) {
            for (Direction direction : Direction.values()) {
                BlockPos target = pos.relative(direction);
                BlockState targetState = level.getBlockState(target);
                if (!targetState.is(this) && !targetState.getFluidState().isEmpty()) {
                    level.removeBlock(target, false);
                }
            }
            return;
        }
        if (this.kind == Kind.TOXIC || this.kind == Kind.SCHRABIDIC) {
            for (Direction direction : Direction.values()) {
                BlockState target = level.getBlockState(pos.relative(direction));
                if (!target.is(this) && !target.getFluidState().isEmpty()) {
                    setById(level, pos, "sellafield_slaked");
                    return;
                }
            }
            return;
        }
        if (this.kind == Kind.RAD_LAVA) {
            for (Direction direction : Direction.values()) {
                BlockPos target = pos.relative(direction);
                BlockState replacement = radioactiveReaction(level, target);
                if (replacement != null) {
                    level.setBlock(target, replacement, Block.UPDATE_ALL);
                }
            }
        }
    }

    public void afterFluidTick(Level level, BlockPos pos, RandomSource random) {
        if (level.isClientSide || !level.getBlockState(pos).is(this)) {
            return;
        }
        switch (this.kind) {
            case ACID, TOXIC, SCHRABIDIC, SULFURIC_ACID -> reactToNeighbors(level, pos);
            case MUD -> {
                for (Direction direction : Direction.values()) {
                    erodeMudNeighbor(level, pos.relative(direction), random);
                }
            }
            case RAD_LAVA -> solidifyRadioactiveLava(level, pos, random);
        }
    }

    private static void erodeMudNeighbor(Level level, BlockPos target, RandomSource random) {
        BlockState state = level.getBlockState(target);
        if (state.is(Blocks.STONE) || state.is(Blocks.STONE_BRICKS) || state.is(Blocks.STONE_BRICK_STAIRS)
                || state.is(Blocks.STONE_SLAB)) {
            if (random.nextInt(20) == 0) level.setBlock(target, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.COBBLESTONE)) {
            if (random.nextInt(15) == 0) level.setBlock(target, Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.SANDSTONE)) {
            if (random.nextInt(5) == 0) level.setBlock(target, Blocks.SAND.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(BlockTags.TERRACOTTA)) {
            if (random.nextInt(10) == 0) level.setBlock(target, Blocks.CLAY.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(BlockTags.WOODEN_BUTTONS) || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.LOGS)
                || state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL)
                || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(Blocks.CACTUS) || state.is(Blocks.CAKE)
                || state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)
                || state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.SPONGE) || state.is(Blocks.WET_SPONGE)
                || state.is(Blocks.VINE) || state.is(Blocks.COBWEB)
                || state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN)
                || state.is(Blocks.MELON) || state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON)
                || state.getBlock().getExplosionResistance() < 1.2F) {
            level.removeBlock(target, false);
        }
    }

    private void solidifyRadioactiveLava(Level level, BlockPos pos, RandomSource random) {
        int lavaCount = 0;
        int basaltCount = 0;
        Block slaked = blockById("sellafield_slaked");
        for (Direction direction : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(direction));
            if (state.is(this)) lavaCount++;
            if (state.is(slaked)) basaltCount++;
        }
        if (((!level.getFluidState(pos).isSource() && lavaCount < 2) || random.nextInt(5) == 0 && lavaCount < 5)
                && !level.getBlockState(pos.below()).is(this)) {
            int roll = random.nextInt(400);
            BlockState above = level.getBlockState(pos.above(10));
            boolean canMakeGem = lavaCount + basaltCount == 6 && lavaCount < 3 && (above.is(slaked) || above.is(this));
            String result = roll < 2 ? "ore_sellafield_diamond"
                    : roll == 2 ? "ore_sellafield_emerald"
                    : roll < 20 && canMakeGem ? "ore_sellafield_radgem"
                    : "sellafield_slaked";
            setById(level, pos, result);
        }
    }

    private static BlockState radioactiveReaction(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getFluidState().is(FluidTags.WATER)) return Blocks.STONE.defaultBlockState();
        if (state.is(BlockTags.LOGS)) return blockById("waste_log").defaultBlockState();
        if (state.is(BlockTags.PLANKS)) return blockById("waste_planks").defaultBlockState();
        if (state.is(BlockTags.LEAVES)) return Blocks.FIRE.defaultBlockState();
        if (state.is(Blocks.DIAMOND_ORE)) return blockById("ore_sellafield_radgem").defaultBlockState();
        if (state.is(blockById("ore_uranium")) || state.is(blockById("ore_gneiss_uranium"))) {
            return blockById(level.random.nextInt(5) == 0
                    ? "ore_sellafield_schrabidium"
                    : "ore_sellafield_uranium_scorched").defaultBlockState();
        }
        return null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (this.kind == Kind.SCHRABIDIC) {
            level.addParticle(SCHRAB_FOG,
                    pos.getX() + 0.5D + random.nextDouble() * 2.0D - 1.0D,
                    pos.getY() + 0.5D + random.nextDouble() * 2.0D - 1.0D,
                    pos.getZ() + 0.5D + random.nextDouble() * 2.0D - 1.0D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    private static Block blockById(String id) {
        return BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id(id));
    }

    private static void setById(Level level, BlockPos pos, String id) {
        Block block = blockById(id);
        if (block != Blocks.AIR) {
            level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public enum Kind {
        MUD,
        ACID,
        TOXIC,
        SCHRABIDIC,
        RAD_LAVA,
        SULFURIC_ACID
    }
}
