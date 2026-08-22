package com.reinhardt.hbm.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LegacyVariantBlock extends Block {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 7);
    private final int maxVariant;
    private final LegacyVariantStrengths.Strength[] strengths;

    public LegacyVariantBlock(Properties properties) {
        this(properties, 7);
    }

    public LegacyVariantBlock(Properties properties, int maxVariant) {
        this(properties, maxVariant, null);
    }

    public LegacyVariantBlock(Properties properties, LegacyVariantStrengths.Strength[] strengths) {
        this(properties, strengths.length - 1, strengths);
    }

    public LegacyVariantBlock(Properties properties, int maxVariant, @Nullable LegacyVariantStrengths.Strength[] strengths) {
        super(properties);
        this.maxVariant = Math.max(0, Math.min(7, maxVariant));
        this.strengths = strengths;
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0));
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
        return clampVariant(state);
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return clampVariant(super.rotate(state, rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return clampVariant(super.mirror(state, mirror));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (this.asItem() instanceof com.reinhardt.hbm.item.FusionComponentBlockItem item) {
            com.reinhardt.hbm.item.FusionComponentBlockItem.Type[] values =
                    com.reinhardt.hbm.item.FusionComponentBlockItem.Type.values();
            int variant = Math.max(0, Math.min(values.length - 1, state.getValue(VARIANT)));
            return List.of(com.reinhardt.hbm.item.FusionComponentBlockItem.stackFor(item, values[variant]));
        }
        return super.getDrops(state, params);
    }

    private BlockState clampVariant(BlockState state) {
        int variant = state.getValue(VARIANT);
        return variant > maxVariant ? state.setValue(VARIANT, maxVariant) : state;
    }

    public int maxVariant() {
        return this.maxVariant;
    }
}
