package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.LegacyVariantBlockItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** Five legacy processed-sand metadata variants with vanilla falling-block behavior. */
public final class SandMixBlock extends FallingBlock {
    private static final MapCodec<SandMixBlock> CODEC = simpleCodec(SandMixBlock::new);
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 4);

    public SandMixBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(VARIANT);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (this.asItem() instanceof LegacyVariantBlockItem item) {
            return List.of(LegacyVariantBlockItem.stackFor(item, state.getValue(VARIANT)));
        }
        return super.getDrops(state, params);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (this.asItem() instanceof LegacyVariantBlockItem item) {
            return LegacyVariantBlockItem.stackFor(item, state.getValue(VARIANT));
        }
        return super.getCloneItemStack(level, pos, state);
    }
}
