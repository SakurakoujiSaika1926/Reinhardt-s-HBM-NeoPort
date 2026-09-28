package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.ClientRenderNeighborSnapshot;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
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
import java.util.Objects;

public class LargeMachineBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final Footprint footprint;
    private final VoxelShape shape;
    private final OffsetMapper offsetMapper;

    public static Properties nonOccludingMachineProperties(Properties properties) {
        return properties.noOcclusion()
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }

    public LargeMachineBlock(Properties properties, Footprint footprint, OffsetMapper offsetMapper) {
        this(properties, footprint, Shapes.block(), offsetMapper);
    }

    public LargeMachineBlock(Properties properties, Footprint footprint, VoxelShape shape, OffsetMapper offsetMapper) {
        super(nonOccludingMachineProperties(properties));
        this.footprint = Objects.requireNonNull(footprint);
        this.shape = Objects.requireNonNull(shape);
        this.offsetMapper = Objects.requireNonNull(offsetMapper);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            placeDummies(level, pos, state.getValue(FACING), this.footprint, this.offsetMapper);
            pushEntitiesOutOfFootprint(level, pos, state.getValue(FACING), this.footprint, placer, this.offsetMapper);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Create Aeronautics/Sable copies the complete footprint first and
        // then removes the source blocks with the vanilla moving flag.  That
        // source cleanup is relocation, not destruction of the multiblock.
        if (!movedByPiston && !state.is(newState.getBlock()) && !level.isClientSide) {
            removeDummies(level, pos, state.getValue(FACING), this.footprint, this.offsetMapper);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        ClientRenderNeighborSnapshot.refreshAt(level, pos);
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighborPos) {
        ClientRenderNeighborSnapshot.refreshAt(level, pos);
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
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public Footprint machineFootprint() {
        return this.footprint;
    }

    /**
     * Applies this concrete machine's authored offset mapping. The mapper is supplied by the
     * concrete block class; this base class deliberately contains no shared facing convention.
     */
    public BlockPos machineOffset(Direction facing, BlockPos authoredOffset) {
        return mappedOffset(this.offsetMapper, facing, authoredOffset);
    }

    /**
     * Returns the exact world positions occupied by this machine's collision
     * footprint. Render bounds and placement must use this same source of
     * truth instead of maintaining a second set of guessed dimensions.
     */
    public List<BlockPos> machineOccupiedPositions(BlockPos corePos, Direction facing) {
        java.util.ArrayList<BlockPos> positions = new java.util.ArrayList<>(this.footprint.offsets().size());
        for (BlockPos offset : this.footprint.offsets()) {
            positions.add(corePos.offset(machineOffset(facing, offset)).immutable());
        }
        return List.copyOf(positions);
    }

    public boolean canPlaceMachineFootprintAt(Level level, BlockPos corePos, Direction facing,
                                              BlockPlaceContext context) {
        return canPlaceFootprintAt(level, corePos, facing, this.footprint, context, this.offsetMapper);
    }

    public static boolean canPlaceFootprint(BlockPlaceContext context, BlockState state, Footprint footprint,
                                            OffsetMapper offsetMapper) {
        return canPlaceFootprint(context, state.getValue(FACING), footprint, offsetMapper);
    }

    public static boolean canPlaceFootprint(BlockPlaceContext context, Direction facing, Footprint footprint,
                                            OffsetMapper offsetMapper) {
        return canPlaceFootprintAt(context.getLevel(), context.getClickedPos(), facing, footprint, context,
                offsetMapper);
    }

    public static boolean canPlaceFootprintAt(Level level, BlockPos corePos, Direction facing, Footprint footprint,
                                              BlockPlaceContext context, OffsetMapper offsetMapper) {
        for (BlockPos offset : footprint.offsets()) {
            BlockPos pos = corePos.offset(mappedOffset(offsetMapper, facing, offset));
            if (pos.equals(corePos)) {
                continue;
            }
            if (!level.getBlockState(pos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    public static void placeDummies(Level level, BlockPos corePos, Direction facing, Footprint footprint,
                                    OffsetMapper offsetMapper) {
        for (BlockPos offset : footprint.offsets()) {
            BlockPos pos = corePos.offset(mappedOffset(offsetMapper, facing, offset));
            if (pos.equals(corePos)) {
                continue;
            }
            level.setBlock(pos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    public static void pushEntitiesOutOfFootprint(Level level, BlockPos corePos, Direction facing,
                                                  Footprint footprint, LivingEntity placer,
                                                  OffsetMapper offsetMapper) {
        java.util.ArrayList<BlockPos> positions = new java.util.ArrayList<>();
        for (BlockPos offset : footprint.offsets()) {
            positions.add(corePos.offset(mappedOffset(offsetMapper, facing, offset)));
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

    public static void removeDummies(Level level, BlockPos corePos, Direction facing, Footprint footprint,
                                     OffsetMapper offsetMapper) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos offset : footprint.offsets()) {
                BlockPos pos = corePos.offset(mappedOffset(offsetMapper, facing, offset));
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

    public static void removeDummiesOutsideFootprint(Level level, BlockPos corePos, Direction facing,
                                                     Footprint staleFootprint, Footprint currentFootprint,
                                                     OffsetMapper offsetMapper) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        java.util.HashSet<BlockPos> currentPositions = new java.util.HashSet<>();
        for (BlockPos offset : currentFootprint.offsets()) {
            currentPositions.add(corePos.offset(mappedOffset(offsetMapper, facing, offset)).immutable());
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos offset : staleFootprint.offsets()) {
                BlockPos pos = corePos.offset(mappedOffset(offsetMapper, facing, offset)).immutable();
                if (currentPositions.contains(pos)) {
                    continue;
                }
                if (level.getBlockState(pos).is(HbmBlocks.MACHINE_DUMMY.get())
                        && level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                        && dummy.getCorePos().equals(corePos)) {
                    level.removeBlock(pos, false);
                }
            }
        });
    }

    private static BlockPos mappedOffset(OffsetMapper offsetMapper, Direction facing, BlockPos authoredOffset) {
        BlockPos mapped = Objects.requireNonNull(offsetMapper, "offsetMapper")
                .map(facing, authoredOffset);
        return Objects.requireNonNull(mapped, "offsetMapper result").immutable();
    }

    @FunctionalInterface
    public interface OffsetMapper {
        BlockPos map(Direction facing, BlockPos authoredOffset);
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

    }
}
