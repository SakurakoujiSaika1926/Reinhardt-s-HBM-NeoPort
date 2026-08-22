package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

public class LargeMachineBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final Footprint footprint;
    private final VoxelShape shape;
    private final RotationBasis rotationBasis;

    public LargeMachineBlock(Properties properties, Footprint footprint) {
        this(properties, footprint, Shapes.block());
    }

    public LargeMachineBlock(Properties properties, Footprint footprint, VoxelShape shape) {
        this(properties, footprint, shape, RotationBasis.MODERN_NORTH);
    }

    public LargeMachineBlock(Properties properties, Footprint footprint, RotationBasis rotationBasis) {
        this(properties, footprint, Shapes.block(), rotationBasis);
    }

    public LargeMachineBlock(Properties properties, Footprint footprint, VoxelShape shape, RotationBasis rotationBasis) {
        super(properties);
        this.footprint = footprint;
        this.shape = shape;
        this.rotationBasis = rotationBasis;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        if (!canPlaceFootprint(context, state)) {
            return null;
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            placeDummies(level, pos, state.getValue(FACING), this.footprint, this.rotationBasis);
            pushEntitiesOutOfFootprint(level, pos, state.getValue(FACING), this.footprint, placer, this.rotationBasis);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            removeDummies(level, pos, state.getValue(FACING), this.footprint, this.rotationBasis);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shape;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public Footprint machineFootprint() {
        return this.footprint;
    }

    public RotationBasis machineRotationBasis() {
        return this.rotationBasis;
    }

    public static boolean canPlaceFootprint(BlockPlaceContext context, BlockState state, Footprint footprint) {
        return canPlaceFootprint(context, state.getValue(FACING), footprint);
    }

    public static boolean canPlaceFootprint(BlockPlaceContext context, Direction facing, Footprint footprint) {
        return canPlaceFootprintAt(context.getLevel(), context.getClickedPos(), facing, footprint, context);
    }

    public static boolean canPlaceFootprintAt(Level level, BlockPos corePos, Direction facing, Footprint footprint, BlockPlaceContext context) {
        return canPlaceFootprintAt(level, corePos, facing, footprint, context, RotationBasis.MODERN_NORTH);
    }

    public static boolean canPlaceLegacyFootprint(BlockPlaceContext context, Direction facing, Footprint footprint) {
        return canPlaceFootprintAt(context.getLevel(), context.getClickedPos(), facing, footprint, context, RotationBasis.HBM_LEGACY_SOUTH);
    }

    public static boolean canPlaceFootprintAt(Level level, BlockPos corePos, Direction facing, Footprint footprint, BlockPlaceContext context, RotationBasis rotationBasis) {
        for (BlockPos offset : footprint.offsets()) {
            BlockPos pos = corePos.offset(rotate(offset, facing, rotationBasis));
            if (pos.equals(corePos)) {
                continue;
            }
            if (!level.getBlockState(pos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    public static void placeDummies(Level level, BlockPos corePos, Direction facing, Footprint footprint) {
        placeDummies(level, corePos, facing, footprint, RotationBasis.MODERN_NORTH);
    }

    public static void placeDummies(Level level, BlockPos corePos, Direction facing, Footprint footprint, RotationBasis rotationBasis) {
        for (BlockPos offset : footprint.offsets()) {
            BlockPos pos = corePos.offset(rotate(offset, facing, rotationBasis));
            if (pos.equals(corePos)) {
                continue;
            }
            level.setBlock(pos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    public static void pushEntitiesOutOfFootprint(Level level, BlockPos corePos, Direction facing, Footprint footprint, LivingEntity placer) {
        pushEntitiesOutOfFootprint(level, corePos, facing, footprint, placer, RotationBasis.MODERN_NORTH);
    }

    public static void pushEntitiesOutOfFootprint(Level level, BlockPos corePos, Direction facing, Footprint footprint, LivingEntity placer, RotationBasis rotationBasis) {
        java.util.ArrayList<BlockPos> positions = new java.util.ArrayList<>();
        for (BlockPos offset : footprint.offsets()) {
            positions.add(corePos.offset(rotate(offset, facing, rotationBasis)));
        }
        pushEntitiesOutOfPositions(level, corePos, facing, positions, placer);
    }

    public static void pushEntitiesOutOfPositions(Level level, BlockPos corePos, Direction fallbackDirection, Iterable<BlockPos> positions, LivingEntity placer) {
        if (level.isClientSide) {
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean any = false;

        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
            any = true;
        }

        if (!any) {
            return;
        }

        AABB footprintBox = new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
        double centerX = (footprintBox.minX + footprintBox.maxX) * 0.5D;
        double centerZ = (footprintBox.minZ + footprintBox.maxZ) * 0.5D;

        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                footprintBox.inflate(0.001D),
                entity -> !entity.isSpectator() && entity.getBoundingBox().intersects(footprintBox)
        )) {
            Vec3 push = pushVector(entity, placer, fallbackDirection, centerX, centerZ);
            double margin = Math.max(0.75D, entity.getBbWidth() * 0.5D + 0.35D);
            double targetX = entity.getX();
            double targetZ = entity.getZ();

            if (Math.abs(push.x) >= Math.abs(push.z)) {
                targetX = push.x >= 0.0D ? footprintBox.maxX + margin : footprintBox.minX - margin;
                targetZ = Mth.clamp(targetZ + push.z * 0.65D, footprintBox.minZ - margin, footprintBox.maxZ + margin);
            } else {
                targetZ = push.z >= 0.0D ? footprintBox.maxZ + margin : footprintBox.minZ - margin;
                targetX = Mth.clamp(targetX + push.x * 0.65D, footprintBox.minX - margin, footprintBox.maxX + margin);
            }

            entity.setPos(targetX, entity.getY(), targetZ);
            entity.setDeltaMovement(entity.getDeltaMovement().add(push.x * 0.45D, 0.08D, push.z * 0.45D));
        }
    }

    public static void removeDummies(Level level, BlockPos corePos, Direction facing, Footprint footprint) {
        removeDummies(level, corePos, facing, footprint, RotationBasis.MODERN_NORTH);
    }

    public static void removeDummies(Level level, BlockPos corePos, Direction facing, Footprint footprint, RotationBasis rotationBasis) {
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos offset : footprint.offsets()) {
                BlockPos pos = corePos.offset(rotate(offset, facing, rotationBasis));
                if (pos.equals(corePos)) {
                    continue;
                }
                if (level.getBlockState(pos).is(HbmBlocks.MACHINE_DUMMY.get())) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    private static Vec3 pushVector(LivingEntity entity, LivingEntity placer, Direction fallbackDirection, double centerX, double centerZ) {
        double x;
        double z;
        if (entity == placer && fallbackDirection.getAxis().isHorizontal()) {
            x = fallbackDirection.getStepX();
            z = fallbackDirection.getStepZ();
        } else {
            x = entity.getX() - centerX;
            z = entity.getZ() - centerZ;
            if (x * x + z * z < 1.0E-4D && fallbackDirection.getAxis().isHorizontal()) {
                x = fallbackDirection.getStepX();
                z = fallbackDirection.getStepZ();
            }
        }

        double length = Math.sqrt(x * x + z * z);
        if (length < 1.0E-4D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return new Vec3(x / length, 0.0D, z / length);
    }

    public static void removeDummiesOutsideFootprint(Level level, BlockPos corePos, Direction facing, Footprint staleFootprint, Footprint currentFootprint) {
        removeDummiesOutsideFootprint(level, corePos, facing, staleFootprint, currentFootprint, RotationBasis.MODERN_NORTH);
    }

    public static void removeDummiesOutsideFootprint(Level level, BlockPos corePos, Direction facing, Footprint staleFootprint, Footprint currentFootprint, RotationBasis rotationBasis) {
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos offset : staleFootprint.offsets()) {
                if (currentFootprint.offsets().contains(offset)) {
                    continue;
                }
                BlockPos pos = corePos.offset(rotate(offset, facing, rotationBasis));
                if (level.getBlockState(pos).is(HbmBlocks.MACHINE_DUMMY.get())
                        && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                        && dummy.getCorePos().equals(corePos)) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    private boolean canPlaceFootprint(BlockPlaceContext context, BlockState state) {
        return canPlaceFootprintAt(context.getLevel(), context.getClickedPos(), state.getValue(FACING), this.footprint, context, this.rotationBasis);
    }

    private static BlockPos rotate(BlockPos pos, Direction facing, RotationBasis rotationBasis) {
        return LegacyMachineGeometry.rotate(pos, facing, rotationBasis);
    }

    public enum RotationBasis {
        MODERN_NORTH,
        HBM_LEGACY_SOUTH
    }

    public record Footprint(List<BlockPos> offsets) {
        public Footprint with(BlockPos... extraOffsets) {
            java.util.LinkedHashSet<BlockPos> merged = new java.util.LinkedHashSet<>(this.offsets);
            java.util.Collections.addAll(merged, extraOffsets);
            return new Footprint(List.copyOf(merged));
        }

        public static Footprint centered(int radiusX, int height, int radiusZ) {
            java.util.ArrayList<BlockPos> offsets = new java.util.ArrayList<>();
            for (int y = 0; y < height; y++) {
                for (int x = -radiusX; x <= radiusX; x++) {
                    for (int z = -radiusZ; z <= radiusZ; z++) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
            return new Footprint(List.copyOf(offsets));
        }

        public static Footprint fromOffsets(BlockPos... offsets) {
            return new Footprint(List.of(offsets));
        }

        public static Footprint box(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            java.util.ArrayList<BlockPos> offsets = new java.util.ArrayList<>();
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        offsets.add(new BlockPos(x, y, z));
                    }
                }
            }
            return new Footprint(List.copyOf(offsets));
        }

        /**
         * Mirrors 1.7.10 BlockDummyable#getDimensions():
         * {up, down, north, south, west, east} with SOUTH as the unrotated basis.
         */
        public static Footprint legacySouthBox(int up, int down, int north, int south, int west, int east) {
            return box(-west, east, -down, up, -north, south);
        }
    }
}
