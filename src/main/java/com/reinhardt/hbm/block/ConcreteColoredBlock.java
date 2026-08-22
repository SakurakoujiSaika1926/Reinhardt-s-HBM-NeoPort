package com.reinhardt.hbm.block;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
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

import com.reinhardt.hbm.item.ConcreteColoredBlockItem;

import java.util.List;

public class ConcreteColoredBlock extends Block {
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);
    public static final String META_TAG = "meta";
    private final int maxMeta;

    public ConcreteColoredBlock(Properties properties) {
        this(properties, 15);
    }

    public ConcreteColoredBlock(Properties properties, int maxMeta) {
        super(properties);
        this.maxMeta = Mth.clamp(maxMeta, 0, 15);
        this.registerDefaultState(this.stateDefinition.any().setValue(META, 0));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(META, clampMeta(meta(context.getItemInHand())));
    }

    public static int meta(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains(META_TAG) ? Mth.clamp(tag.getInt(META_TAG), 0, 15) : 0;
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
        if (this.asItem() instanceof ConcreteColoredBlockItem item) {
            return ConcreteColoredBlockItem.stackFor(item, clampMeta(state.getValue(META)));
        }
        return new ItemStack(this);
    }

    private int clampMeta(int meta) {
        return Mth.clamp(meta, 0, this.maxMeta);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }
}
