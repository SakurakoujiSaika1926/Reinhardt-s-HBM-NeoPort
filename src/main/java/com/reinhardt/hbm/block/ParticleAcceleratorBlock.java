package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.ParticleAcceleratorBlockEntity;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;

public class ParticleAcceleratorBlock extends LargeMachineBlock implements EntityBlock {
    private final Kind kind;

    public ParticleAcceleratorBlock(Properties properties, VoxelShape shape, Kind kind) {
        super(properties, footprint(kind), shape, switch (kind) {
            case SOURCE -> ParticleAcceleratorBlock::mapSourceOffset;
            case BEAMLINE -> ParticleAcceleratorBlock::mapBeamlineOffset;
            case RFC -> ParticleAcceleratorBlock::mapRfcOffset;
            case QUADRUPOLE -> ParticleAcceleratorBlock::mapQuadrupoleOffset;
            case DIPOLE -> ParticleAcceleratorBlock::mapDipoleOffset;
            case DETECTOR -> ParticleAcceleratorBlock::mapDetectorOffset;
        });
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return switch (this.kind) {
            case SOURCE -> sourceStateForPlacement(context);
            case BEAMLINE -> beamlineStateForPlacement(context);
            case RFC -> rfcStateForPlacement(context);
            case QUADRUPOLE -> quadrupoleStateForPlacement(context);
            case DIPOLE -> dipoleStateForPlacement(context);
            case DETECTOR -> detectorStateForPlacement(context);
        };
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return switch (this.kind) {
            case SOURCE -> rotateSource(state, rotation);
            case BEAMLINE -> rotateBeamline(state, rotation);
            case RFC -> rotateRfc(state, rotation);
            case QUADRUPOLE -> rotateQuadrupole(state, rotation);
            case DIPOLE -> rotateDipole(state, rotation);
            case DETECTOR -> rotateDetector(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return switch (this.kind) {
            case SOURCE -> mirrorSource(state, mirror);
            case BEAMLINE -> mirrorBeamline(state, mirror);
            case RFC -> mirrorRfc(state, mirror);
            case QUADRUPOLE -> mirrorQuadrupole(state, mirror);
            case DIPOLE -> mirrorDipole(state, mirror);
            case DETECTOR -> mirrorDetector(state, mirror);
        };
    }

    private BlockState sourceStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = sourceCorePos(context.getClickedPos());
        if (!context.getLevel().getWorldBorder().isWithinBounds(corePos)
                || !context.getLevel().getBlockState(corePos).canBeReplaced(context)) {
            return null;
        }
        for (BlockPos pos : sourcePositions(corePos, facing)) {
            if (!pos.equals(corePos) && (!context.getLevel().getWorldBorder().isWithinBounds(pos)
                    || !context.getLevel().getBlockState(pos).canBeReplaced(context))) {
                return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState beamlineStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = beamlineCorePos(context.getClickedPos());
        if (!context.getLevel().getWorldBorder().isWithinBounds(corePos)
                || !context.getLevel().getBlockState(corePos).canBeReplaced(context)) {
            return null;
        }
        for (BlockPos pos : beamlinePositions(corePos, facing)) {
            if (!pos.equals(corePos) && (!context.getLevel().getWorldBorder().isWithinBounds(pos)
                    || !context.getLevel().getBlockState(pos).canBeReplaced(context))) {
                return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState rfcStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = rfcCorePos(context.getClickedPos());
        if (!context.getLevel().getWorldBorder().isWithinBounds(corePos)
                || !context.getLevel().getBlockState(corePos).canBeReplaced(context)) {
            return null;
        }
        for (BlockPos pos : rfcPositions(corePos, facing)) {
            if (!pos.equals(corePos) && (!context.getLevel().getWorldBorder().isWithinBounds(pos)
                    || !context.getLevel().getBlockState(pos).canBeReplaced(context))) {
                return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState quadrupoleStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = quadrupoleCorePos(context.getClickedPos());
        if (!context.getLevel().getWorldBorder().isWithinBounds(corePos)
                || !context.getLevel().getBlockState(corePos).canBeReplaced(context)) {
            return null;
        }
        for (BlockPos pos : quadrupolePositions(corePos, facing)) {
            if (!pos.equals(corePos) && (!context.getLevel().getWorldBorder().isWithinBounds(pos)
                    || !context.getLevel().getBlockState(pos).canBeReplaced(context))) {
                return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState dipoleStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = dipoleCorePos(context.getClickedPos());
        if (!context.getLevel().getWorldBorder().isWithinBounds(corePos)
                || !context.getLevel().getBlockState(corePos).canBeReplaced(context)) {
            return null;
        }
        for (BlockPos pos : dipolePositions(corePos, facing)) {
            if (!pos.equals(corePos) && (!context.getLevel().getWorldBorder().isWithinBounds(pos)
                    || !context.getLevel().getBlockState(pos).canBeReplaced(context))) {
                return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState detectorStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = detectorCorePos(context.getClickedPos());
        if (!context.getLevel().getWorldBorder().isWithinBounds(corePos)
                || !context.getLevel().getBlockState(corePos).canBeReplaced(context)) {
            return null;
        }
        for (BlockPos pos : detectorPositions(corePos, facing)) {
            if (!pos.equals(corePos) && (!context.getLevel().getWorldBorder().isWithinBounds(pos)
                    || !context.getLevel().getBlockState(pos).canBeReplaced(context))) {
                return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState rotateSource(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateBeamline(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateRfc(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateQuadrupole(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateDipole(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateDetector(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState mirrorSource(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING,
                mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorBeamline(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING,
                mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorRfc(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING,
                mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorQuadrupole(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING,
                mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorDipole(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING,
                mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorDetector(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING,
                mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            placeLegacyDummies(level, pos, state.getValue(FACING), placer);
            refreshPorts(level, pos, state.getValue(FACING));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ParticleAcceleratorBlockEntity(pos, state, this.kind);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.PARTICLE_ACCELERATOR.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                ParticleAcceleratorBlockEntity.tick(tickerLevel, pos, tickerState, (ParticleAcceleratorBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isCrouching() || !this.kind.hasMenu()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (this.kind == Kind.BEAMLINE
                && ScrewdriverItem.isScrewdriver(stack)
                && level.getBlockEntity(pos) instanceof ParticleAcceleratorBlockEntity accelerator) {
            if (!level.isClientSide) {
                accelerator.toggleBeamlineWindow();
                level.playSound(null, pos, HbmSoundEvents.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 0.65F, 1.35F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            removeLegacyDummies(level, pos, state.getValue(FACING));
            refreshPorts(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static BlockPos legacyCorePos(BlockPos clickedPos, Kind kind) {
        return switch (kind) {
            case SOURCE -> sourceCorePos(clickedPos);
            case BEAMLINE -> beamlineCorePos(clickedPos);
            case RFC -> rfcCorePos(clickedPos);
            case QUADRUPOLE -> quadrupoleCorePos(clickedPos);
            case DIPOLE -> dipoleCorePos(clickedPos);
            case DETECTOR -> detectorCorePos(clickedPos);
        };
    }

    private static BlockPos sourceCorePos(BlockPos clickedPos) {
        return clickedPos.above(1);
    }

    private static BlockPos beamlineCorePos(BlockPos clickedPos) {
        return clickedPos;
    }

    private static BlockPos rfcCorePos(BlockPos clickedPos) {
        return clickedPos.above(1);
    }

    private static BlockPos quadrupoleCorePos(BlockPos clickedPos) {
        return clickedPos.above(1);
    }

    private static BlockPos dipoleCorePos(BlockPos clickedPos) {
        return clickedPos.above(1);
    }

    private static BlockPos detectorCorePos(BlockPos clickedPos) {
        return clickedPos.above(2);
    }

    public boolean canPlaceLegacy(Level level, BlockPos corePos, Direction facing, BlockPlaceContext context) {
        return canPlaceCurrentKind(level, corePos, facing, context);
    }

    public void placeLegacyDummies(Level level, BlockPos corePos, Direction facing, @Nullable LivingEntity placer) {
        List<BlockPos> positions = currentPositions(corePos, facing);
        for (BlockPos pos : positions) {
            if (pos.equals(corePos)) {
                continue;
            }
            level.setBlock(pos, HbmBlocks.MACHINE_DUMMY.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
        if (placer != null) {
            LargeMachineBlock.pushEntitiesOutOfPositions(level, corePos, facing, positions, placer);
        }
    }

    private void removeLegacyDummies(Level level, BlockPos corePos, Direction facing) {
        if (level.getBlockEntity(corePos) == null) {
            return;
        }
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos pos : currentPositions(corePos, facing)) {
                if (pos.equals(corePos)) {
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

    private boolean canPlaceCurrentKind(Level level, BlockPos corePos, Direction facing, BlockPlaceContext context) {
        if (!level.getWorldBorder().isWithinBounds(corePos) || !level.getBlockState(corePos).canBeReplaced(context)) {
            return false;
        }
        for (BlockPos pos : currentPositions(corePos, facing)) {
            if (pos.equals(corePos)) {
                continue;
            }
            if (!level.getWorldBorder().isWithinBounds(pos) || !level.getBlockState(pos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    private List<BlockPos> currentPositions(BlockPos corePos, Direction facing) {
        return switch (this.kind) {
            case SOURCE -> sourcePositions(corePos, facing);
            case BEAMLINE -> beamlinePositions(corePos, facing);
            case RFC -> rfcPositions(corePos, facing);
            case QUADRUPOLE -> quadrupolePositions(corePos, facing);
            case DIPOLE -> dipolePositions(corePos, facing);
            case DETECTOR -> detectorPositions(corePos, facing);
        };
    }

    private static Footprint footprint(Kind kind) {
        return switch (kind) {
            case SOURCE -> SourceFootprint.VALUE;
            case BEAMLINE -> BeamlineFootprint.VALUE;
            case RFC -> RfcFootprint.VALUE;
            case QUADRUPOLE -> QuadrupoleFootprint.VALUE;
            case DIPOLE -> DipoleFootprint.VALUE;
            case DETECTOR -> DetectorFootprint.VALUE;
        };
    }

    private static final class SourceFootprint {
        private static final Footprint VALUE = sourceFootprint();
    }

    private static final class BeamlineFootprint {
        private static final Footprint VALUE = beamlineFootprint();
    }

    private static final class RfcFootprint {
        private static final Footprint VALUE = rfcFootprint();
    }

    private static final class QuadrupoleFootprint {
        private static final Footprint VALUE = quadrupoleFootprint();
    }

    private static final class DipoleFootprint {
        private static final Footprint VALUE = dipoleFootprint();
    }

    private static final class DetectorFootprint {
        private static final Footprint VALUE = detectorFootprint();
    }

    private static Footprint sourceFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                for (int x = -4; x <= 4; x++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.add(new BlockPos(-4, 0, 0));
        offsets.add(new BlockPos(0, 0, 1));
        offsets.add(new BlockPos(-2, 0, 1));
        offsets.add(new BlockPos(2, 0, 1));
        offsets.add(new BlockPos(0, 0, -1));
        offsets.add(new BlockPos(-2, 0, -1));
        offsets.add(new BlockPos(2, 0, -1));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint beamlineFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 0; y++) {
            for (int z = 0; z <= 0; z++) {
                for (int x = -1; x <= 1; x++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint rfcFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                for (int x = -4; x <= 4; x++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.add(new BlockPos(-3, 1, 0));
        offsets.add(new BlockPos(3, 1, 0));
        offsets.add(new BlockPos(0, 1, 0));
        offsets.add(new BlockPos(-3, -1, 0));
        offsets.add(new BlockPos(3, -1, 0));
        offsets.add(new BlockPos(0, -1, 0));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint quadrupoleFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                for (int x = -1; x <= 1; x++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.add(new BlockPos(0, 0, 1));
        offsets.add(new BlockPos(0, 0, -1));
        offsets.add(new BlockPos(0, 1, 0));
        offsets.add(new BlockPos(0, -1, 0));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint dipoleFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                for (int x = -1; x <= 1; x++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.add(new BlockPos(1, -1, 0));
        offsets.add(new BlockPos(-1, -1, 0));
        offsets.add(new BlockPos(0, -1, 1));
        offsets.add(new BlockPos(0, -1, -1));
        offsets.add(new BlockPos(1, 1, 0));
        offsets.add(new BlockPos(-1, 1, 0));
        offsets.add(new BlockPos(0, 1, 1));
        offsets.add(new BlockPos(0, 1, -1));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint detectorFootprint() {
        LinkedHashSet<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = -2; y <= 2; y++) {
            for (int z = -2; z <= 2; z++) {
                for (int x = -4; x <= 4; x++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.add(new BlockPos(4, 0, 0));
        offsets.add(new BlockPos(4, 1, 0));
        offsets.add(new BlockPos(4, -1, 0));
        offsets.add(new BlockPos(4, 0, 1));
        offsets.add(new BlockPos(4, 0, -1));
        return new Footprint(List.copyOf(offsets));
    }

    private static List<BlockPos> sourcePositions(BlockPos corePos, Direction facing) {
        return SourceFootprint.VALUE.offsets().stream()
                .map(offset -> corePos.offset(mapSourceOffset(facing, offset)))
                .toList();
    }

    private static List<BlockPos> beamlinePositions(BlockPos corePos, Direction facing) {
        return BeamlineFootprint.VALUE.offsets().stream()
                .map(offset -> corePos.offset(mapBeamlineOffset(facing, offset)))
                .toList();
    }

    private static List<BlockPos> rfcPositions(BlockPos corePos, Direction facing) {
        return RfcFootprint.VALUE.offsets().stream()
                .map(offset -> corePos.offset(mapRfcOffset(facing, offset)))
                .toList();
    }

    private static List<BlockPos> quadrupolePositions(BlockPos corePos, Direction facing) {
        return QuadrupoleFootprint.VALUE.offsets().stream()
                .map(offset -> corePos.offset(mapQuadrupoleOffset(facing, offset)))
                .toList();
    }

    private static List<BlockPos> dipolePositions(BlockPos corePos, Direction facing) {
        return DipoleFootprint.VALUE.offsets().stream()
                .map(offset -> corePos.offset(mapDipoleOffset(facing, offset)))
                .toList();
    }

    private static List<BlockPos> detectorPositions(BlockPos corePos, Direction facing) {
        return DetectorFootprint.VALUE.offsets().stream()
                .map(offset -> corePos.offset(mapDetectorOffset(facing, offset)))
                .toList();
    }

    private static BlockPos mapSourceOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            default -> throw new IllegalArgumentException("PA source facing must be horizontal: " + facing);
        };
    }

    private static BlockPos mapBeamlineOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            default -> throw new IllegalArgumentException("PA beamline facing must be horizontal: " + facing);
        };
    }

    private static BlockPos mapRfcOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            default -> throw new IllegalArgumentException("PA RFC facing must be horizontal: " + facing);
        };
    }

    private static BlockPos mapQuadrupoleOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            default -> throw new IllegalArgumentException("PA quadrupole facing must be horizontal: " + facing);
        };
    }

    private static BlockPos mapDipoleOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            default -> throw new IllegalArgumentException("PA dipole facing must be horizontal: " + facing);
        };
    }

    private static BlockPos mapDetectorOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            default -> throw new IllegalArgumentException("PA detector facing must be horizontal: " + facing);
        };
    }

    private void refreshPorts(Level level, BlockPos corePos, Direction facing) {
        if (level.isClientSide) {
            return;
        }
        List<BlockPos> connectors = switch (this.kind) {
            case SOURCE -> ParticleAcceleratorBlockEntity.sourceConnectorPositions(corePos, facing);
            case BEAMLINE -> ParticleAcceleratorBlockEntity.beamlineConnectorPositions(corePos, facing);
            case RFC -> ParticleAcceleratorBlockEntity.rfcConnectorPositions(corePos, facing);
            case QUADRUPOLE -> ParticleAcceleratorBlockEntity.quadrupoleConnectorPositions(corePos, facing);
            case DIPOLE -> ParticleAcceleratorBlockEntity.dipoleConnectorPositions(corePos, facing);
            case DETECTOR -> ParticleAcceleratorBlockEntity.detectorConnectorPositions(corePos, facing);
        };
        for (BlockPos connector : connectors) {
            EnergyCableBlock.refreshConnections(level, connector);
            BlockState ductState = level.getBlockState(connector);
            if (ductState.getBlock() instanceof FluidDuctBlock duct) {
                duct.refreshConnections(level, connector);
            }
        }
    }

    public enum Kind {
        SOURCE("pa_source", true),
        BEAMLINE("pa_beamline", false),
        RFC("pa_rfc", true),
        QUADRUPOLE("pa_quadrupole", true),
        DIPOLE("pa_dipole", true),
        DETECTOR("pa_detector", true);

        private final String id;
        private final boolean hasMenu;

        Kind(String id, boolean hasMenu) {
            this.id = id;
            this.hasMenu = hasMenu;
        }

        public String id() {
            return this.id;
        }

        public boolean hasMenu() {
            return this.hasMenu;
        }
    }
}
