package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.CokeBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

public class CokeBlock extends Block {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

    public CokeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction face) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction face) {
        return 10;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return stackForState(state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(stackForState(state));
    }

    private ItemStack stackForState(BlockState state) {
        if (this.asItem() instanceof CokeBlockItem item) {
            CokeBlockItem.CokeType[] values = CokeBlockItem.CokeType.values();
            int variant = Mth.clamp(state.getValue(VARIANT), 0, values.length - 1);
            return CokeBlockItem.stackFor(item, values[variant]);
        }
        return new ItemStack(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }
}
