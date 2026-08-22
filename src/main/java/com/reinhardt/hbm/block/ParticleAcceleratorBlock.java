package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.ParticleAcceleratorBlockEntity;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
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
import java.util.Set;

public class ParticleAcceleratorBlock extends LargeMachineBlock implements EntityBlock {
    private final Kind kind;

    public ParticleAcceleratorBlock(Properties properties, VoxelShape shape, Kind kind) {
        super(properties, new Footprint(List.of(BlockPos.ZERO)), shape, RotationBasis.HBM_LEGACY_SOUTH);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos corePos = legacyCorePos(context.getClickedPos(), this.kind);
        return canPlaceAt(context.getLevel(), corePos, facing, this.kind, context)
                ? defaultBlockState().setValue(FACING, facing)
                : null;
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
        return clickedPos.above(kind.heightOffset());
    }

    public boolean canPlaceLegacy(Level level, BlockPos corePos, Direction facing, BlockPlaceContext context) {
        return canPlaceAt(level, corePos, facing, this.kind, context);
    }

    public void placeLegacyDummies(Level level, BlockPos corePos, Direction facing, @Nullable LivingEntity placer) {
        List<BlockPos> positions = positions(corePos, facing, this.kind);
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
        MachineDummyBlock.runWithoutCoreDestroy(() -> {
            for (BlockPos pos : positions(corePos, facing, this.kind)) {
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

    private static boolean canPlaceAt(Level level, BlockPos corePos, Direction facing, Kind kind, BlockPlaceContext context) {
        if (!level.getWorldBorder().isWithinBounds(corePos) || !level.getBlockState(corePos).canBeReplaced(context)) {
            return false;
        }
        for (BlockPos pos : positions(corePos, facing, kind)) {
            if (pos.equals(corePos)) {
                continue;
            }
            if (!level.getWorldBorder().isWithinBounds(pos) || !level.getBlockState(pos).canBeReplaced(context)) {
                return false;
            }
        }
        return true;
    }

    public static List<BlockPos> positions(BlockPos corePos, Direction facing, Kind kind) {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        int[] dim = kind.dimensions();
        addLegacyBox(offsets, BlockPos.ZERO, dim[0], dim[1], dim[2], dim[3], dim[4], dim[5]);
        for (BlockPos extra : kind.extraOffsets(facing)) {
            offsets.add(extra);
        }
        return offsets.stream()
                .map(offset -> corePos.offset(LegacyMachineGeometry.rotateLegacySouth(offset, facing)))
                .toList();
    }

    private static void addLegacyBox(Set<BlockPos> offsets, BlockPos origin, int up, int down, int north, int south, int west, int east) {
        for (int y = -down; y <= up; y++) {
            for (int z = -north; z <= south; z++) {
                for (int x = -west; x <= east; x++) {
                    offsets.add(origin.offset(x, y, z));
                }
            }
        }
    }

    private static void refreshPorts(Level level, BlockPos corePos, Direction facing) {
        if (level.isClientSide) {
            return;
        }
        for (BlockPos connector : ParticleAcceleratorBlockEntity.connectorPositions(corePos, facing)) {
            EnergyCableBlock.refreshConnections(level, connector);
            BlockState ductState = level.getBlockState(connector);
            if (ductState.getBlock() instanceof FluidDuctBlock duct) {
                duct.refreshConnections(level, connector);
            }
        }
    }

    public enum Kind {
        SOURCE("pa_source", 1, new int[]{1, 1, 1, 1, 4, 4}, true),
        BEAMLINE("pa_beamline", 0, new int[]{0, 0, 0, 0, 1, 1}, false),
        RFC("pa_rfc", 1, new int[]{1, 1, 1, 1, 4, 4}, true),
        QUADRUPOLE("pa_quadrupole", 1, new int[]{1, 1, 1, 1, 1, 1}, true),
        DIPOLE("pa_dipole", 1, new int[]{1, 1, 1, 1, 1, 1}, true),
        DETECTOR("pa_detector", 2, new int[]{2, 2, 2, 2, 4, 4}, true);

        private final String id;
        private final int heightOffset;
        private final int[] dimensions;
        private final boolean hasMenu;

        Kind(String id, int heightOffset, int[] dimensions, boolean hasMenu) {
            this.id = id;
            this.heightOffset = heightOffset;
            this.dimensions = dimensions;
            this.hasMenu = hasMenu;
        }

        public String id() {
            return this.id;
        }

        public int heightOffset() {
            return this.heightOffset;
        }

        public int[] dimensions() {
            return this.dimensions.clone();
        }

        public boolean hasMenu() {
            return this.hasMenu;
        }

        private List<BlockPos> extraOffsets(Direction unrotatedFacing) {
            Direction dir = Direction.SOUTH;
            Direction rot = LegacyMachineGeometry.forgeRotateUp(dir);
            return switch (this) {
                case SOURCE -> List.of(
                        offset(rot, 4, 0),
                        offset(dir, 1, 0),
                        offset(dir, 1, rot, 2, 0),
                        offset(dir, 1, rot, -2, 0),
                        offset(dir, -1, 0),
                        offset(dir, -1, rot, 2, 0),
                        offset(dir, -1, rot, -2, 0)
                );
                case RFC -> {
                    Direction side = rot;
                    yield List.of(
                            offset(side, 3, 1),
                            offset(side, -3, 1),
                            new BlockPos(0, 1, 0),
                            offset(side, 3, -1),
                            offset(side, -3, -1),
                            new BlockPos(0, -1, 0)
                    );
                }
                case QUADRUPOLE -> List.of(
                        offset(dir, 1, 0),
                        offset(dir, -1, 0),
                        new BlockPos(0, 1, 0),
                        new BlockPos(0, -1, 0)
                );
                case DIPOLE -> List.of(
                        new BlockPos(1, -1, 0),
                        new BlockPos(-1, -1, 0),
                        new BlockPos(0, -1, 1),
                        new BlockPos(0, -1, -1),
                        new BlockPos(1, 1, 0),
                        new BlockPos(-1, 1, 0),
                        new BlockPos(0, 1, 1),
                        new BlockPos(0, 1, -1)
                );
                case DETECTOR -> List.of(
                        offset(rot, -4, 0),
                        offset(rot, -4, 1),
                        offset(rot, -4, -1),
                        offset(rot, -4, dir, 1, 0),
                        offset(rot, -4, dir, -1, 0)
                );
                case BEAMLINE -> List.of();
            };
        }

        private static BlockPos offset(Direction first, int firstAmount, int y) {
            return new BlockPos(first.getStepX() * firstAmount, y, first.getStepZ() * firstAmount);
        }

        private static BlockPos offset(Direction first, int firstAmount, Direction second, int secondAmount, int y) {
            return new BlockPos(
                    first.getStepX() * firstAmount + second.getStepX() * secondAmount,
                    y,
                    first.getStepZ() * firstAmount + second.getStepZ() * secondAmount
            );
        }
    }
}
