package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.ConcreteColoredBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** The 1.7.10 vinyl tile enum has exactly large and small variants. */
public final class VinylTileBlock extends Block {
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 1);

    public VinylTileBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(META, 0));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(META, meta(context.getItemInHand()));
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
        if (asItem() instanceof ConcreteColoredBlockItem item) {
            return ConcreteColoredBlockItem.stackFor(item, state.getValue(META));
        }
        return new ItemStack(this);
    }

    private static int meta(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(ConcreteColoredBlock.META_TAG)
                ? Mth.clamp(tag.getInt(ConcreteColoredBlock.META_TAG), 0, 1)
                : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }
}
