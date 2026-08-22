package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.MeteorOreBlockItem;
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

public class MeteorOreBlock extends Block {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 4);

    public MeteorOreBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return stackForState(state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(stackForState(state));
    }

    public static MeteorOreBlockItem.Type type(BlockState state) {
        MeteorOreBlockItem.Type[] values = MeteorOreBlockItem.Type.values();
        int variant = state.hasProperty(VARIANT) ? state.getValue(VARIANT) : 0;
        return values[Mth.clamp(variant, 0, values.length - 1)];
    }

    private ItemStack stackForState(BlockState state) {
        if (this.asItem() instanceof MeteorOreBlockItem item) {
            return MeteorOreBlockItem.stackFor(item, type(state));
        }
        return new ItemStack(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT);
    }
}
