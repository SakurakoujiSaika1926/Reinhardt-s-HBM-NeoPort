package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.worldgen.structure.HbmLegacyNbtTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Direct 1.7.10 EntityBlockSpider (entity_taintcrawler) port. */
public final class LegacyBlockSpiderEntity extends Monster {
    private static final EntityDataAccessor<Integer> BLOCK_STATE =
            SynchedEntityData.defineId(LegacyBlockSpiderEntity.class, EntityDataSerializers.INT);

    public LegacyBlockSpiderEntity(EntityType<? extends LegacyBlockSpiderEntity> type, Level level) {
        super(type, level);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new RandomStrollGoal(this, 0.5D));
        // EntityBlockSpider used EntityAINearestAttackableTarget with
        // targetChance=0, so it evaluated the player target every tick rather
        // than inheriting the modern goal's ten-tick random interval.
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, Player.class, 0, true, false, null));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BLOCK_STATE, Block.getId(Blocks.STONE.defaultBlockState()));
    }

    /** Legacy makeBlock(Block, metadata) entry point retained for old callers. */
    public void makeBlock(Block block, int metadata) {
        // EntityBlockSpider stored the legacy block id and metadata, so a
        // metadata-bearing caller must not silently collapse to the modern
        // default state.  Reuse the same explicit 1.7.10 decoder used by
        // structure migration.
        makeBlock(HbmLegacyNbtTemplate.stateFromLegacyId(
                BuiltInRegistries.BLOCK.getKey(block).toString(), metadata));
    }

    public void makeBlock(BlockState state) {
        entityData.set(BLOCK_STATE, Block.getId(state));
        double health = Math.max(1.0D, state.getBlock().getExplosionResistance());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        setHealth(getMaxHealth());
    }

    public BlockState blockState() {
        BlockState state = Block.stateById(entityData.get(BLOCK_STATE));
        return state == null ? Blocks.STONE.defaultBlockState() : state;
    }

}
