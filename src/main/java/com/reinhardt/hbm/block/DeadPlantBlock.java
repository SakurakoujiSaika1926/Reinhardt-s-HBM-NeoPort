package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

public class DeadPlantBlock extends BushBlock {
    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    private static final MapCodec<DeadPlantBlock> CODEC = simpleCodec(DeadPlantBlock::new);
    private static final VoxelShape SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    public DeadPlantBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(KIND, Kind.GRASS));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(HbmBlocks.DIRT_OILY.get())
                || state.is(HbmBlocks.DIRT_DEAD.get())
                || state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(KIND);
    }

    public enum Kind implements StringRepresentable {
        GRASS,
        GENERIC,
        FLOWER,
        FERN,
        BIG_FLOWER;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
