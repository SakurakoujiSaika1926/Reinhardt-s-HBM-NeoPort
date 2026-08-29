package com.reinhardt.hbm.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public class LegacyVariantSlabBlock extends SlabBlock {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 7);
    private final int maxVariant;
    private final LegacyVariantStrengths.Strength[] strengths;

    public LegacyVariantSlabBlock(Properties properties) {
        this(properties, 7);
    }

    public LegacyVariantSlabBlock(Properties properties, int maxVariant) {
        this(properties, maxVariant, null);
    }

    public LegacyVariantSlabBlock(Properties properties, LegacyVariantStrengths.Strength[] strengths) {
        this(properties, strengths.length - 1, strengths);
    }

    public LegacyVariantSlabBlock(Properties properties, int maxVariant, @Nullable LegacyVariantStrengths.Strength[] strengths) {
        super(properties);
        this.maxVariant = Math.max(0, Math.min(7, maxVariant));
        this.strengths = strengths;
        this.registerDefaultState(this.defaultBlockState().setValue(VARIANT, 0));
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return this.strengths == null
                ? super.getExplosionResistance(state, level, pos, explosion)
                : LegacyVariantStrengths.strengthFor(state, this.strengths).resistance();
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (this.strengths == null) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        float hardness = LegacyVariantStrengths.strengthFor(state, this.strengths).hardness();
        if (hardness == -1.0F) {
            return 0.0F;
        }
        int divisor = net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(player, state, level, pos) ? 30 : 100;
        return player.getDigSpeed(state, pos) / hardness / (float) divisor;
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighborState,
                                     net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos,
                                     net.minecraft.core.BlockPos neighborPos) {
        return clampVariant(super.updateShape(state, direction, neighborState, level, pos, neighborPos));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VARIANT);
    }

    private BlockState clampVariant(BlockState state) {
        int variant = state.getValue(VARIANT);
        return variant > maxVariant ? state.setValue(VARIANT, maxVariant) : state;
    }

    public int maxVariant() {
        return this.maxVariant;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (this.asItem() instanceof com.reinhardt.hbm.item.LegacyVariantBlockItem item) {
            return List.of(com.reinhardt.hbm.item.LegacyVariantBlockItem.stackFor(item, state.getValue(VARIANT)));
        }
        return super.getDrops(state, params);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (this.asItem() instanceof com.reinhardt.hbm.item.LegacyVariantBlockItem item) {
            return com.reinhardt.hbm.item.LegacyVariantBlockItem.stackFor(item, state.getValue(VARIANT));
        }
        return super.getCloneItemStack(level, pos, state);
    }
}
