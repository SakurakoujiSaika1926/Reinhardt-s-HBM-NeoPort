package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** Exact 1.7.10 MachineSawmill collision boxes and missing-blade item state. */
public final class SawmillBlock extends LegacyMachineBlock {
    private static final String BLADE_MISSING_TAG = "sawmill_blade_missing";
    private static final Bounds[] LEGACY_BOUNDS = {
            // MachineSawmill#bounding, converted from center-relative x/z to block coordinates.
            new Bounds(-1.0D, 0.0D, -1.0D, 2.0D, 1.0D, 2.0D),
            new Bounds(-0.75D, 1.0D, 0.0D, -0.125D, 1.875D, 1.0D),
            new Bounds(-0.125D, 1.0D, -0.5D, 1.875D, 2.0D, 1.5D)
    };

    public SawmillBlock(BlockBehaviour.Properties properties, Footprint footprint) {
        super(properties, footprint, Shapes.empty());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForPart(state.getValue(FACING), BlockPos.ZERO);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForPart(state.getValue(FACING), BlockPos.ZERO);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity sawmill) {
            sawmill.setSawmillHasBlade(hasBlade(stack));
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        ItemStack drop = new ItemStack(this);
        if (blockEntity instanceof LegacyMachineBlockEntity sawmill && !sawmill.sawmillHasBlade()) {
            markBladeMissing(drop);
        }
        return List.of(drop);
    }

    /** Called by the common dummy block to return the clipped legacy AABBs for one structural cell. */
    public static VoxelShape shapeForPart(Direction facing, BlockPos relativePart) {
        VoxelShape shape = Shapes.empty();
        for (Bounds bounds : LEGACY_BOUNDS) {
            Bounds rotated = bounds.rotate(facing);
            double minX = Math.max(rotated.minX, relativePart.getX());
            double minY = Math.max(rotated.minY, relativePart.getY());
            double minZ = Math.max(rotated.minZ, relativePart.getZ());
            double maxX = Math.min(rotated.maxX, relativePart.getX() + 1.0D);
            double maxY = Math.min(rotated.maxY, relativePart.getY() + 1.0D);
            double maxZ = Math.min(rotated.maxZ, relativePart.getZ() + 1.0D);
            if (maxX > minX && maxY > minY && maxZ > minZ) {
                shape = Shapes.or(shape, Shapes.box(
                        minX - relativePart.getX(), minY - relativePart.getY(), minZ - relativePart.getZ(),
                        maxX - relativePart.getX(), maxY - relativePart.getY(), maxZ - relativePart.getZ()
                ));
            }
        }
        return shape;
    }

    public static boolean hasBlade(ItemStack stack) {
        return !stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(BLADE_MISSING_TAG);
    }

    public static void markBladeMissing(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(BLADE_MISSING_TAG, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        private Bounds rotate(Direction facing) {
            return switch (facing) {
                case EAST -> new Bounds(minZ, minY, 1.0D - maxX, maxZ, maxY, 1.0D - minX);
                case NORTH -> new Bounds(1.0D - maxX, minY, 1.0D - maxZ, 1.0D - minX, maxY, 1.0D - minZ);
                case WEST -> new Bounds(1.0D - maxZ, minY, minX, 1.0D - minZ, maxY, maxX);
                default -> this;
            };
        }
    }
}
