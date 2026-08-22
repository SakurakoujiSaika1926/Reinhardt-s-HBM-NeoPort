package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FusionMachineBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FusionMachineBlock extends LargeMachineBlock implements EntityBlock {
    private final Kind kind;

    public FusionMachineBlock(Properties properties, VoxelShape shape, Kind kind) {
        super(properties, kind.footprint(), shape, RotationBasis.HBM_LEGACY_SOUTH);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    public Kind kind() {
        return this.kind;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return LargeMachineBlock.canPlaceLegacyFootprint(context, facing, this.kind.footprint())
                ? this.defaultBlockState().setValue(FACING, facing)
                : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        refreshPorts(level, pos, state);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionMachineBlockEntity(pos, state, this.kind);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.FUSION_MACHINE.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof FusionMachineBlockEntity fusion) {
                FusionMachineBlockEntity.tick(tickerLevel, pos, tickerState, fusion);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        if (blockEntity instanceof FusionMachineBlockEntity fusion) {
            fusion.printInfo(player);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            refreshPorts(level, pos, state);
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (removed) {
            refreshPorts(level, pos, state);
            PowerNetworkManager.markDirty(level);
        }
    }

    private void refreshPorts(Level level, BlockPos corePos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.SOUTH;
        for (FusionMachineBlockEntity.Port port : FusionMachineBlockEntity.portsFor(corePos, facing, this.kind)) {
            if (port.power()) {
                EnergyCableBlock.refreshConnections(level, port.connectorPos());
            }
            if (port.fluid()) {
                CatalyticCrackerBlock.refreshDuctsAtPort(level, port.proxyPos(), port.connectorPos());
            }
        }
    }

    public enum Kind {
        TORUS("fusion_torus", 7, torusFootprint(), PortRole.ALL),
        KLYSTRON("fusion_klystron", 3, klystronFootprint(), PortRole.POWER_FLUID),
        KLYSTRON_CREATIVE("fusion_klystron_creative", 3, klystronFootprint(), PortRole.NONE),
        BOILER("fusion_boiler", 4, legacyBox(3, 0, 4, 4, 1, 1).with(
                new BlockPos(1, 0, -1),
                new BlockPos(-1, 0, -1),
                new BlockPos(1, 0, 2),
                new BlockPos(-1, 0, 2)
        ), PortRole.FLUID),
        MHDT("fusion_mhdt", 7, mhdtFootprint(), PortRole.POWER_FLUID),
        BREEDER("fusion_breeder", 2, legacyBox(3, 0, 2, 2, 1, 1).with(
                new BlockPos(1, 0, 0),
                new BlockPos(-1, 0, 0),
                new BlockPos(1, 0, 1),
                new BlockPos(-1, 0, 1),
                new BlockPos(0, 2, 2)
        ), PortRole.FLUID),
        COLLECTOR("fusion_collector", 1, legacyBox(3, 0, 2, 1, 2, 2), PortRole.NONE),
        COUPLER("fusion_coupler", 0, legacyBox(3, 0, 1, 1, 1, 1), PortRole.NONE),
        PLASMA_FORGE("fusion_plasma_forge", 5, plasmaForgeFootprint(), PortRole.ALL);

        private final String id;
        private final int legacyOffset;
        private final Footprint footprint;
        private final PortRole portRole;

        Kind(String id, int legacyOffset, Footprint footprint, PortRole portRole) {
            this.id = id;
            this.legacyOffset = legacyOffset;
            this.footprint = footprint;
            this.portRole = portRole;
        }

        public String id() {
            return this.id;
        }

        public int legacyOffset() {
            return this.legacyOffset;
        }

        public Footprint footprint() {
            return this.footprint;
        }

        public boolean hasPowerPorts() {
            return this.portRole == PortRole.ALL || this.portRole == PortRole.POWER_FLUID;
        }

        public boolean hasFluidPorts() {
            return this.portRole == PortRole.ALL || this.portRole == PortRole.POWER_FLUID || this.portRole == PortRole.FLUID;
        }

        public List<LocalPort> localPorts() {
            Direction south = Direction.SOUTH;
            Direction rot = LegacyMachineGeometry.forgeRotateUp(south);
            return switch (this) {
                case TORUS -> torusPorts();
                case KLYSTRON -> List.of(
                        new LocalPort(local(south, 3, rot, 0, 2), south),
                        new LocalPort(local(south, 0, rot, 2, 0), rot),
                        new LocalPort(local(south, 0, rot, -2, 0), rot.getOpposite())
                );
                case KLYSTRON_CREATIVE -> List.of();
                case BOILER -> List.of(
                        new LocalPort(local(south, -1, rot, 1, 0), rot),
                        new LocalPort(local(south, -1, rot, -1, 0), rot.getOpposite()),
                        new LocalPort(local(south, 2, rot, 1, 0), rot),
                        new LocalPort(local(south, 2, rot, -1, 0), rot.getOpposite())
                );
                case MHDT -> List.of(
                        new LocalPort(local(south, 4, rot, 3, 0), rot),
                        new LocalPort(local(south, 4, rot, -3, 0), rot.getOpposite()),
                        new LocalPort(local(south, 7, rot, 0, 1), south)
                );
                case BREEDER -> List.of(
                        new LocalPort(local(south, 0, rot, 1, 0), rot),
                        new LocalPort(local(south, 0, rot, -1, 0), rot.getOpposite()),
                        new LocalPort(local(south, 1, rot, 1, 0), rot),
                        new LocalPort(local(south, 1, rot, -1, 0), rot.getOpposite()),
                        new LocalPort(local(south, 2, rot, 0, 2), south)
                );
                case COLLECTOR, COUPLER -> List.of();
                case PLASMA_FORGE -> {
                    ArrayList<LocalPort> ports = new ArrayList<>();
                    for (int i = -2; i <= 2; i++) {
                        ports.add(new LocalPort(local(south, 5, rot, i, 0), south));
                        ports.add(new LocalPort(local(south, -5, rot, i, 0), south.getOpposite()));
                    }
                    yield List.copyOf(ports);
                }
            };
        }

        public Component displayName() {
            return Component.translatable("block.reinhardtshbm." + this.id);
        }
    }

    private enum PortRole {
        NONE,
        FLUID,
        POWER_FLUID,
        ALL
    }

    private static Footprint legacyBox(int up, int down, int north, int south, int west, int east) {
        return Footprint.legacySouthBox(up, down, north, south, west, east);
    }

    private static Footprint klystronFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        addLegacyBox(offsets, 3, 0, 4, 3, 2, 2, BlockPos.ZERO);
        addLegacyBox(offsets, 4, -3, 4, 3, 1, 1, BlockPos.ZERO);
        offsets.add(new BlockPos(0, 2, 3));
        offsets.add(new BlockPos(2, 0, 0));
        offsets.add(new BlockPos(-2, 0, 0));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint mhdtFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        addLegacyBox(offsets, 2, 0, 6, 7, 2, 2, BlockPos.ZERO);
        addLegacyBox(offsets, 3, -2, 6, 2, 1, 1, BlockPos.ZERO);
        addLegacyBox(offsets, 3, -2, -6, 7, 1, 1, BlockPos.ZERO);
        addLegacyBox(offsets, 3, -2, -3, 5, 2, 2, BlockPos.ZERO);
        addLegacyBox(offsets, 4, -3, -3, 5, 1, 1, BlockPos.ZERO);
        addLegacyBox(offsets, 1, 0, 0, 1, 3, 3, new BlockPos(0, 0, 3));
        offsets.add(new BlockPos(3, 0, 4));
        offsets.add(new BlockPos(-3, 0, 4));
        offsets.add(new BlockPos(0, 1, 7));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint plasmaForgeFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        addLegacyBox(offsets, 2, 0, 2, 2, 5, 5, BlockPos.ZERO);
        addLegacyBox(offsets, 2, 0, 3, -2, 4, 4, BlockPos.ZERO);
        addLegacyBox(offsets, 2, 0, -2, 3, 4, 4, BlockPos.ZERO);
        addLegacyBox(offsets, 2, 0, 4, -3, 3, 3, BlockPos.ZERO);
        addLegacyBox(offsets, 2, 0, -3, 4, 3, 3, BlockPos.ZERO);
        addLegacyBox(offsets, 2, 0, 5, -4, 2, 2, BlockPos.ZERO);
        addLegacyBox(offsets, 2, 0, -4, 5, 2, 2, BlockPos.ZERO);
        addLegacyBox(offsets, 3, -2, 1, 1, 5, 5, BlockPos.ZERO);
        addLegacyBox(offsets, 4, -3, 0, 0, 4, 4, BlockPos.ZERO);
        for (int i = -2; i <= 2; i++) {
            offsets.add(new BlockPos(i, 0, 5));
            offsets.add(new BlockPos(i, 0, -5));
        }
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint torusFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y < 5; y++) {
            int layerIndex = y > 2 ? 4 - y : y;
            int[][] layer = TORUS_LAYOUT[layerIndex];
            for (int x = 0; x < layer.length; x++) {
                for (int z = 0; z < layer[x].length; z++) {
                    if (layer[x][z] > 0) {
                        offsets.add(new BlockPos(x - layer.length / 2, y, z - layer[x].length / 2));
                    }
                }
            }
        }
        for (LocalPort port : torusPorts()) {
            offsets.add(port.offset());
        }
        offsets.add(BlockPos.ZERO);
        return new Footprint(List.copyOf(offsets));
    }

    private static List<LocalPort> torusPorts() {
        return List.of(
                new LocalPort(new BlockPos(0, 4, 0), Direction.UP),
                new LocalPort(new BlockPos(6, 0, 0), Direction.DOWN),
                new LocalPort(new BlockPos(6, 4, 0), Direction.UP),
                new LocalPort(new BlockPos(6, 0, 2), Direction.DOWN),
                new LocalPort(new BlockPos(6, 4, 2), Direction.UP),
                new LocalPort(new BlockPos(6, 0, -2), Direction.DOWN),
                new LocalPort(new BlockPos(6, 4, -2), Direction.UP),
                new LocalPort(new BlockPos(-6, 0, 0), Direction.DOWN),
                new LocalPort(new BlockPos(-6, 4, 0), Direction.UP),
                new LocalPort(new BlockPos(-6, 0, 2), Direction.DOWN),
                new LocalPort(new BlockPos(-6, 4, 2), Direction.UP),
                new LocalPort(new BlockPos(-6, 0, -2), Direction.DOWN),
                new LocalPort(new BlockPos(-6, 4, -2), Direction.UP),
                new LocalPort(new BlockPos(0, 0, 6), Direction.DOWN),
                new LocalPort(new BlockPos(0, 4, 6), Direction.UP),
                new LocalPort(new BlockPos(2, 0, 6), Direction.DOWN),
                new LocalPort(new BlockPos(2, 4, 6), Direction.UP),
                new LocalPort(new BlockPos(-2, 0, 6), Direction.DOWN),
                new LocalPort(new BlockPos(-2, 4, 6), Direction.UP),
                new LocalPort(new BlockPos(0, 0, -6), Direction.DOWN),
                new LocalPort(new BlockPos(0, 4, -6), Direction.UP),
                new LocalPort(new BlockPos(2, 0, -6), Direction.DOWN),
                new LocalPort(new BlockPos(2, 4, -6), Direction.UP),
                new LocalPort(new BlockPos(-2, 0, -6), Direction.DOWN),
                new LocalPort(new BlockPos(-2, 4, -6), Direction.UP)
        );
    }

    private static BlockPos local(Direction facing, int alongFacing, Direction side, int alongSide, int y) {
        return new BlockPos(
                facing.getStepX() * alongFacing + side.getStepX() * alongSide,
                y,
                facing.getStepZ() * alongFacing + side.getStepZ() * alongSide
        );
    }

    private static void addLegacyBox(Set<BlockPos> offsets, int up, int down, int north, int south, int west, int east, BlockPos origin) {
        for (int y = -down; y <= up; y++) {
            for (int x = -west; x <= east; x++) {
                for (int z = -north; z <= south; z++) {
                    offsets.add(origin.offset(x, y, z));
                }
            }
        }
    }

    public record LocalPort(BlockPos offset, Direction face) {
        public LocalPort rotate(Direction facing) {
            return new LocalPort(
                    LegacyMachineGeometry.rotateLegacySouth(this.offset, facing),
                    LegacyMachineGeometry.rotateDirection(this.face, facing, LargeMachineBlock.RotationBasis.HBM_LEGACY_SOUTH)
            );
        }
    }

    public static List<BlockPos> absolutePorts(BlockPos corePos, Direction facing, Kind kind) {
        ArrayList<BlockPos> ports = new ArrayList<>(kind.localPorts().size());
        for (LocalPort local : kind.localPorts()) {
            ports.add(corePos.offset(local.rotate(facing).offset()));
        }
        return List.copyOf(ports);
    }

    public static void markPortDummies(Level level, BlockPos corePos, Direction facing, Kind kind) {
        for (BlockPos port : absolutePorts(corePos, facing, kind)) {
            if (level.getBlockState(port).is(HbmBlocks.MACHINE_DUMMY.get())
                    && level.getBlockEntity(port) instanceof MachineDummyBlockEntity dummy) {
                dummy.setCorePos(corePos);
            }
        }
    }

    public static final int[][][] TORUS_LAYOUT = {
            {
                    {0, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0},
                    {0, 0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0, 0},
                    {0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0},
                    {0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0},
                    {3, 1, 1, 1, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1, 3},
                    {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    {3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    {3, 1, 1, 1, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1, 3},
                    {0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0},
                    {0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0},
                    {0, 0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0, 0},
                    {0, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0}
            },
            {
                    {0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
                    {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                    {0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0},
                    {0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 0},
                    {1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1},
                    {1, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 1},
                    {3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
                    {3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
                    {3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
                    {1, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 1},
                    {1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1},
                    {0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 0},
                    {0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0},
                    {0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                    {0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0}
            },
            {
                    {0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
                    {0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0},
                    {0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0},
                    {0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0},
                    {1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 1},
                    {1, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 1},
                    {3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
                    {3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
                    {3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
                    {1, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 1},
                    {1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 1},
                    {0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0},
                    {0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0},
                    {0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0},
                    {0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0}
            }
    };
}
