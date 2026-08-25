package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.GlyphBlockItem;
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

/** 1.7.10 Enargite glyph brick: its sixteen metadata values remain distinct block states. */
public final class GlyphBlock extends Block {
    public static final IntegerProperty GLYPH = IntegerProperty.create("glyph", 0, 15);
    public static final String GLYPH_TAG = "glyph";

    public GlyphBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(GLYPH, 0));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(GLYPH, glyph(context.getItemInHand()));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return stackForState(state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(stackForState(state));
    }

    public static int glyph(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Mth.clamp(tag.getInt(GLYPH_TAG), 0, 15);
    }

    private ItemStack stackForState(BlockState state) {
        return this.asItem() instanceof GlyphBlockItem item
                ? GlyphBlockItem.stackFor(item, state.getValue(GLYPH))
                : new ItemStack(this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GLYPH);
    }
}
