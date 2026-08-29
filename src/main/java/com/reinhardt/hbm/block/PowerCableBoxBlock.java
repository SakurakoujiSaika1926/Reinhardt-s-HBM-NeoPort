package com.reinhardt.hbm.block;

import com.reinhardt.hbm.item.PowerCableBoxBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** 1.7.10 PowerCableBox: five metadata sizes, each with its own collision envelope. */
public final class PowerCableBoxBlock extends EnergyCableBlock {
    public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 4);

    public PowerCableBoxBlock(Properties properties) {
        super(properties, 6.0D);
        registerDefaultState(defaultBlockState().setValue(SIZE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SIZE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int size = state.getValue(SIZE);
        double lower = 2.0D + size;
        double upper = 14.0D - size;
        boolean north = state.getValue(NORTH);
        boolean south = state.getValue(SOUTH);
        boolean east = state.getValue(EAST);
        boolean west = state.getValue(WEST);
        boolean up = state.getValue(UP);
        boolean down = state.getValue(DOWN);
        int mask = (east ? 32 : 0) | (west ? 16 : 0) | (up ? 8 : 0)
                | (down ? 4 : 0) | (south ? 2 : 0) | (north ? 1 : 0);

        // This is the exact branch selection from 1.7.10's PowerCableBox.
        if (mask == 0) {
            return box(lower, lower, lower, upper, upper, upper);
        }
        if (mask == 0b100000 || mask == 0b010000 || mask == 0b110000) {
            return box(0.0D, lower, lower, 16.0D, upper, upper);
        }
        if (mask == 0b001000 || mask == 0b000100 || mask == 0b001100) {
            return box(lower, 0.0D, lower, upper, 16.0D, upper);
        }
        if (mask == 0b000010 || mask == 0b000001 || mask == 0b000011) {
            return box(lower, lower, 0.0D, upper, upper, 16.0D);
        }

        VoxelShape shape = box(lower, lower, lower, upper, upper, upper);
        if (north) shape = Shapes.or(shape, box(lower, lower, 0.0D, upper, upper, lower));
        if (south) shape = Shapes.or(shape, box(lower, lower, upper, upper, upper, 16.0D));
        if (west) shape = Shapes.or(shape, box(0.0D, lower, lower, lower, upper, upper));
        if (east) shape = Shapes.or(shape, box(upper, lower, lower, 16.0D, upper, upper));
        if (up) shape = Shapes.or(shape, box(lower, upper, lower, upper, 16.0D, upper));
        if (down) shape = Shapes.or(shape, box(lower, 0.0D, lower, upper, lower, upper));
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    /** Preserve PowerCableBox's former metadata value on both drops and pick-block. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (asItem() instanceof PowerCableBoxBlockItem item) {
            return List.of(PowerCableBoxBlockItem.stackFor(item, state.getValue(SIZE)));
        }
        return super.getDrops(state, builder);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (asItem() instanceof PowerCableBoxBlockItem item) {
            return PowerCableBoxBlockItem.stackFor(item, state.getValue(SIZE));
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }
}
