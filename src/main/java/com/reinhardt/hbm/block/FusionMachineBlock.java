package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.FusionMachineBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
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
        super(properties, kind.footprint(), shape, offsetMapper(kind));
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    public Kind kind() {
        return this.kind;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return switch (this.kind) {
            case TORUS -> torusStateForPlacement(context);
            case KLYSTRON -> klystronStateForPlacement(context);
            case KLYSTRON_CREATIVE -> creativeKlystronStateForPlacement(context);
            case BOILER -> fusionBoilerStateForPlacement(context);
            case MHDT -> mhdtStateForPlacement(context);
            case BREEDER -> fusionBreederStateForPlacement(context);
            case COLLECTOR -> collectorStateForPlacement(context);
            case COUPLER -> couplerStateForPlacement(context);
            case PLASMA_FORGE -> plasmaForgeStateForPlacement(context);
        };
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return switch (this.kind) {
            case TORUS -> rotateTorus(state, rotation);
            case KLYSTRON -> rotateKlystron(state, rotation);
            case KLYSTRON_CREATIVE -> rotateCreativeKlystron(state, rotation);
            case BOILER -> rotateFusionBoiler(state, rotation);
            case MHDT -> rotateMhdt(state, rotation);
            case BREEDER -> rotateFusionBreeder(state, rotation);
            case COLLECTOR -> rotateCollector(state, rotation);
            case COUPLER -> rotateCoupler(state, rotation);
            case PLASMA_FORGE -> rotatePlasmaForge(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return switch (this.kind) {
            case TORUS -> mirrorTorus(state, mirror);
            case KLYSTRON -> mirrorKlystron(state, mirror);
            case KLYSTRON_CREATIVE -> mirrorCreativeKlystron(state, mirror);
            case BOILER -> mirrorFusionBoiler(state, mirror);
            case MHDT -> mirrorMhdt(state, mirror);
            case BREEDER -> mirrorFusionBreeder(state, mirror);
            case COLLECTOR -> mirrorCollector(state, mirror);
            case COUPLER -> mirrorCoupler(state, mirror);
            case PLASMA_FORGE -> mirrorPlasmaForge(state, mirror);
        };
    }

    private BlockState torusStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState klystronStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState creativeKlystronStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState fusionBoilerStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState mhdtStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState fusionBreederStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState collectorStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState couplerStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private BlockState plasmaForgeStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    private static BlockState rotateTorus(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateKlystron(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateCreativeKlystron(
            BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateFusionBoiler(
            BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateMhdt(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateFusionBreeder(
            BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateCollector(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotateCoupler(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState rotatePlasmaForge(
            BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorTorus(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorKlystron(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorCreativeKlystron(
            BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorFusionBoiler(
            BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorMhdt(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorFusionBreeder(
            BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorCollector(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorCoupler(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static BlockState mirrorPlasmaForge(
            BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
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
        if (removed && !movedByPiston) {
            refreshPorts(level, pos, state);
            PowerNetworkManager.markDirty(level);
        }
    }

    private void refreshPorts(Level level, BlockPos corePos, BlockState state) {
        if (level.isClientSide) {
            return;
        }
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.SOUTH;
        for (FusionMachineBlockEntity.Port port : portsAt(corePos, facing)) {
            if (port.power()) {
                EnergyCableBlock.refreshConnections(level, port.connectorPos());
            }
            if (port.fluid()) {
                CatalyticCrackerBlock.refreshDuctsAtPort(level, port.proxyPos(), port.connectorPos());
            }
        }
    }

    private List<FusionMachineBlockEntity.Port> portsAt(BlockPos corePos, Direction facing) {
        return switch (this.kind) {
            case TORUS -> FusionMachineBlockEntity.torusPortsFor(corePos, facing);
            case KLYSTRON -> FusionMachineBlockEntity.klystronPortsFor(corePos, facing);
            case KLYSTRON_CREATIVE -> FusionMachineBlockEntity.creativeKlystronPortsFor(corePos, facing);
            case BOILER -> FusionMachineBlockEntity.boilerPortsFor(corePos, facing);
            case MHDT -> FusionMachineBlockEntity.mhdtPortsFor(corePos, facing);
            case BREEDER -> FusionMachineBlockEntity.breederPortsFor(corePos, facing);
            case COLLECTOR -> FusionMachineBlockEntity.collectorPortsFor(corePos, facing);
            case COUPLER -> FusionMachineBlockEntity.couplerPortsFor(corePos, facing);
            case PLASMA_FORGE -> FusionMachineBlockEntity.plasmaForgePortsFor(corePos, facing);
        };
    }

    public enum Kind {
        TORUS("fusion_torus", 7, PortRole.ALL),
        KLYSTRON("fusion_klystron", 3, PortRole.POWER_FLUID),
        KLYSTRON_CREATIVE("fusion_klystron_creative", 3, PortRole.NONE),
        BOILER("fusion_boiler", 4, PortRole.FLUID),
        MHDT("fusion_mhdt", 7, PortRole.POWER_FLUID),
        BREEDER("fusion_breeder", 2, PortRole.FLUID),
        COLLECTOR("fusion_collector", 1, PortRole.NONE),
        COUPLER("fusion_coupler", 0, PortRole.NONE),
        PLASMA_FORGE("fusion_plasma_forge", 5, PortRole.ALL);

        private final String id;
        private final int legacyOffset;
        private final PortRole portRole;

        Kind(String id, int legacyOffset, PortRole portRole) {
            this.id = id;
            this.legacyOffset = legacyOffset;
            this.portRole = portRole;
        }

        public String id() {
            return this.id;
        }

        public int legacyOffset() {
            return this.legacyOffset;
        }

        public Footprint footprint() {
            return switch (this) {
                case TORUS -> TorusFootprint.VALUE;
                case KLYSTRON -> KlystronFootprint.VALUE;
                case KLYSTRON_CREATIVE -> CreativeKlystronFootprint.VALUE;
                case BOILER -> BoilerFootprint.VALUE;
                case MHDT -> MhdtFootprint.VALUE;
                case BREEDER -> BreederFootprint.VALUE;
                case COLLECTOR -> CollectorFootprint.VALUE;
                case COUPLER -> CouplerFootprint.VALUE;
                case PLASMA_FORGE -> PlasmaForgeFootprint.VALUE;
            };
        }

        public boolean hasPowerPorts() {
            return this.portRole == PortRole.ALL || this.portRole == PortRole.POWER_FLUID;
        }

        public boolean hasFluidPorts() {
            return this.portRole == PortRole.ALL || this.portRole == PortRole.POWER_FLUID || this.portRole == PortRole.FLUID;
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

    private static final class TorusFootprint {
        private static final Footprint VALUE = torusFootprint();
    }

    private static final class KlystronFootprint {
        private static final Footprint VALUE = klystronFootprint();
    }

    private static final class CreativeKlystronFootprint {
        private static final Footprint VALUE = creativeKlystronFootprint();
    }

    private static final class BoilerFootprint {
        private static final Footprint VALUE = boilerFootprint();
    }

    private static final class MhdtFootprint {
        private static final Footprint VALUE = mhdtFootprint();
    }

    private static final class BreederFootprint {
        private static final Footprint VALUE = breederFootprint();
    }

    private static final class CollectorFootprint {
        private static final Footprint VALUE = collectorFootprint();
    }

    private static final class CouplerFootprint {
        private static final Footprint VALUE = couplerFootprint();
    }

    private static final class PlasmaForgeFootprint {
        private static final Footprint VALUE = plasmaForgeFootprint();
    }

    private static OffsetMapper offsetMapper(Kind kind) {
        return switch (kind) {
            case TORUS -> FusionMachineBlock::mapTorusOffset;
            case KLYSTRON -> FusionMachineBlock::mapKlystronOffset;
            case KLYSTRON_CREATIVE -> FusionMachineBlock::mapCreativeKlystronOffset;
            case BOILER -> FusionMachineBlock::mapBoilerOffset;
            case MHDT -> FusionMachineBlock::mapMhdtOffset;
            case BREEDER -> FusionMachineBlock::mapBreederOffset;
            case COLLECTOR -> FusionMachineBlock::mapCollectorOffset;
            case COUPLER -> FusionMachineBlock::mapCouplerOffset;
            case PLASMA_FORGE -> FusionMachineBlock::mapPlasmaForgeOffset;
        };
    }

    public static BlockPos mapTorusOffset(Direction facing, BlockPos offset) {
        int x = offset.getX(); int y = offset.getY(); int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z); case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z); case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("Fusion torus requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos mapTorusStructureOffset(Direction facing, BlockPos offset) {
        return mapTorusOffset(facing, offset);
    }

    public static BlockPos mapKlystronOffset(Direction facing, BlockPos offset) {
        int x = offset.getX(); int y = offset.getY(); int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z); case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z); case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("Fusion klystron requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos mapCreativeKlystronOffset(Direction facing, BlockPos offset) {
        int x = offset.getX(); int y = offset.getY(); int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z); case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z); case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("Creative fusion klystron requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos mapBoilerOffset(Direction facing, BlockPos offset) {
        int x = offset.getX(); int y = offset.getY(); int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z); case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z); case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("Fusion boiler requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos mapMhdtOffset(Direction facing, BlockPos offset) {
        int x = offset.getX(); int y = offset.getY(); int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z); case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z); case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("Fusion MHDT requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos mapBreederOffset(Direction facing, BlockPos offset) {
        int x = offset.getX(); int y = offset.getY(); int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z); case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z); case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("Fusion breeder requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos mapCollectorOffset(Direction facing, BlockPos offset) {
        int x = offset.getX(); int y = offset.getY(); int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z); case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z); case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("Fusion collector requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos mapCouplerOffset(Direction facing, BlockPos offset) {
        int x = offset.getX(); int y = offset.getY(); int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z); case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z); case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("Fusion coupler requires a horizontal facing: " + facing);
        };
    }

    public static BlockPos mapPlasmaForgeOffset(Direction facing, BlockPos offset) {
        int x = offset.getX(); int y = offset.getY(); int z = offset.getZ();
        return switch (facing) {
            case SOUTH -> new BlockPos(x, y, z); case EAST -> new BlockPos(z, y, -x);
            case NORTH -> new BlockPos(-x, y, -z); case WEST -> new BlockPos(-z, y, x);
            case UP, DOWN -> throw new IllegalArgumentException("Fusion plasma forge requires a horizontal facing: " + facing);
        };
    }

    private static Footprint klystronFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 3; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -4; z <= 3; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 3; y <= 4; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -4; z <= 3; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.add(new BlockPos(0, 2, 3));
        offsets.add(new BlockPos(2, 0, 0));
        offsets.add(new BlockPos(-2, 0, 0));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint creativeKlystronFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 3; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -4; z <= 3; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 3; y <= 4; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -4; z <= 3; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.add(new BlockPos(0, 2, 3));
        offsets.add(new BlockPos(2, 0, 0));
        offsets.add(new BlockPos(-2, 0, 0));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint boilerFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -4; z <= 4; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.add(new BlockPos(1, 0, -1));
        offsets.add(new BlockPos(-1, 0, -1));
        offsets.add(new BlockPos(1, 0, 2));
        offsets.add(new BlockPos(-1, 0, 2));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint mhdtFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 2; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -6; z <= 7; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 2; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -6; z <= 2; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 2; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = 6; z <= 7; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 2; y <= 3; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = 3; z <= 5; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 3; y <= 4; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = 3; z <= 5; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        BlockPos shiftedCore = new BlockPos(0, 0, 3);
        for (int y = 0; y <= 1; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = 0; z <= 1; z++) {
                    offsets.add(shiftedCore.offset(x, y, z));
                }
            }
        }
        offsets.add(new BlockPos(3, 0, 4));
        offsets.add(new BlockPos(-3, 0, 4));
        offsets.add(new BlockPos(0, 1, 7));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint breederFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -2; z <= 2; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.add(new BlockPos(1, 0, 0));
        offsets.add(new BlockPos(-1, 0, 0));
        offsets.add(new BlockPos(1, 0, 1));
        offsets.add(new BlockPos(-1, 0, 1));
        offsets.add(new BlockPos(0, 2, 2));
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint collectorFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 3; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 1; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint couplerFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        return new Footprint(List.copyOf(offsets));
    }

    private static Footprint plasmaForgeFootprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        for (int y = 0; y <= 2; y++) {
            for (int x = -5; x <= 5; x++) {
                for (int z = -2; z <= 2; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 0; y <= 2; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -3; z <= -2; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 0; y <= 2; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = 2; z <= 3; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 0; y <= 2; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -4; z <= -3; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 0; y <= 2; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = 3; z <= 4; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 0; y <= 2; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -5; z <= -4; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 0; y <= 2; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = 4; z <= 5; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 2; y <= 3; y++) {
            for (int x = -5; x <= 5; x++) {
                for (int z = -1; z <= 1; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        for (int y = 3; y <= 4; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = 0; z <= 0; z++) {
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
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
        for (LocalPort port : torusLocalPorts()) {
            offsets.add(port.offset());
        }
        offsets.add(BlockPos.ZERO);
        return new Footprint(List.copyOf(offsets));
    }

    public static List<LocalPort> torusLocalPorts() {
        return List.of(
                new LocalPort(new BlockPos(0, 0, 0), Direction.DOWN),
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

    public static List<LocalPort> klystronLocalPorts() {
        return List.of(
                new LocalPort(new BlockPos(0, 2, 3), Direction.SOUTH),
                new LocalPort(new BlockPos(-2, 0, 0), Direction.WEST),
                new LocalPort(new BlockPos(2, 0, 0), Direction.EAST)
        );
    }

    public static List<LocalPort> creativeKlystronLocalPorts() {
        return List.of();
    }

    public static List<LocalPort> boilerLocalPorts() {
        return List.of(
                new LocalPort(new BlockPos(-1, 0, -1), Direction.WEST),
                new LocalPort(new BlockPos(1, 0, -1), Direction.EAST),
                new LocalPort(new BlockPos(-1, 0, 2), Direction.WEST),
                new LocalPort(new BlockPos(1, 0, 2), Direction.EAST)
        );
    }

    public static List<LocalPort> mhdtLocalPorts() {
        return List.of(
                new LocalPort(new BlockPos(-3, 0, 4), Direction.WEST),
                new LocalPort(new BlockPos(3, 0, 4), Direction.EAST),
                new LocalPort(new BlockPos(0, 1, 7), Direction.SOUTH)
        );
    }

    public static List<LocalPort> breederLocalPorts() {
        return List.of(
                new LocalPort(new BlockPos(-1, 0, 0), Direction.WEST),
                new LocalPort(new BlockPos(1, 0, 0), Direction.EAST),
                new LocalPort(new BlockPos(-1, 0, 1), Direction.WEST),
                new LocalPort(new BlockPos(1, 0, 1), Direction.EAST),
                new LocalPort(new BlockPos(0, 2, 2), Direction.SOUTH)
        );
    }

    public static List<LocalPort> collectorLocalPorts() {
        return List.of();
    }

    public static List<LocalPort> couplerLocalPorts() {
        return List.of();
    }

    public static List<LocalPort> plasmaForgeLocalPorts() {
        return List.of(
                new LocalPort(new BlockPos(2, 0, 5), Direction.SOUTH),
                new LocalPort(new BlockPos(2, 0, -5), Direction.NORTH),
                new LocalPort(new BlockPos(1, 0, 5), Direction.SOUTH),
                new LocalPort(new BlockPos(1, 0, -5), Direction.NORTH),
                new LocalPort(new BlockPos(0, 0, 5), Direction.SOUTH),
                new LocalPort(new BlockPos(0, 0, -5), Direction.NORTH),
                new LocalPort(new BlockPos(-1, 0, 5), Direction.SOUTH),
                new LocalPort(new BlockPos(-1, 0, -5), Direction.NORTH),
                new LocalPort(new BlockPos(-2, 0, 5), Direction.SOUTH),
                new LocalPort(new BlockPos(-2, 0, -5), Direction.NORTH)
        );
    }

    public static LocalPort rotateTorusPort(LocalPort port, Direction facing) {
        return new LocalPort(mapTorusOffset(facing, port.offset()), mapTorusPortFace(facing, port.face()));
    }

    public static LocalPort rotateKlystronPort(LocalPort port, Direction facing) {
        return new LocalPort(mapKlystronOffset(facing, port.offset()), mapKlystronPortFace(facing, port.face()));
    }

    public static LocalPort rotateCreativeKlystronPort(LocalPort port, Direction facing) {
        return new LocalPort(mapCreativeKlystronOffset(facing, port.offset()), mapCreativeKlystronPortFace(facing, port.face()));
    }

    public static LocalPort rotateBoilerPort(LocalPort port, Direction facing) {
        return new LocalPort(mapBoilerOffset(facing, port.offset()), mapBoilerPortFace(facing, port.face()));
    }

    public static LocalPort rotateMhdtPort(LocalPort port, Direction facing) {
        return new LocalPort(mapMhdtOffset(facing, port.offset()), mapMhdtPortFace(facing, port.face()));
    }

    public static LocalPort rotateBreederPort(LocalPort port, Direction facing) {
        return new LocalPort(mapBreederOffset(facing, port.offset()), mapBreederPortFace(facing, port.face()));
    }

    public static LocalPort rotateCollectorPort(LocalPort port, Direction facing) {
        return new LocalPort(mapCollectorOffset(facing, port.offset()), mapCollectorPortFace(facing, port.face()));
    }

    public static LocalPort rotateCouplerPort(LocalPort port, Direction facing) {
        return new LocalPort(mapCouplerOffset(facing, port.offset()), mapCouplerPortFace(facing, port.face()));
    }

    public static LocalPort rotatePlasmaForgePort(LocalPort port, Direction facing) {
        return new LocalPort(mapPlasmaForgeOffset(facing, port.offset()), mapPlasmaForgePortFace(facing, port.face()));
    }

    private static Direction mapTorusPortFace(Direction facing, Direction face) {
        return switch (facing) {
            case SOUTH -> face;
            case EAST -> switch (face) { case SOUTH -> Direction.EAST; case EAST -> Direction.NORTH; case NORTH -> Direction.WEST; case WEST -> Direction.SOUTH; default -> face; };
            case NORTH -> switch (face) { case SOUTH -> Direction.NORTH; case EAST -> Direction.WEST; case NORTH -> Direction.SOUTH; case WEST -> Direction.EAST; default -> face; };
            case WEST -> switch (face) { case SOUTH -> Direction.WEST; case WEST -> Direction.NORTH; case NORTH -> Direction.EAST; case EAST -> Direction.SOUTH; default -> face; };
            case UP, DOWN -> throw new IllegalArgumentException("Fusion torus requires a horizontal facing: " + facing);
        };
    }

    private static Direction mapKlystronPortFace(Direction facing, Direction face) {
        return switch (facing) {
            case SOUTH -> face;
            case EAST -> switch (face) { case SOUTH -> Direction.EAST; case EAST -> Direction.NORTH; case NORTH -> Direction.WEST; case WEST -> Direction.SOUTH; default -> face; };
            case NORTH -> switch (face) { case SOUTH -> Direction.NORTH; case EAST -> Direction.WEST; case NORTH -> Direction.SOUTH; case WEST -> Direction.EAST; default -> face; };
            case WEST -> switch (face) { case SOUTH -> Direction.WEST; case WEST -> Direction.NORTH; case NORTH -> Direction.EAST; case EAST -> Direction.SOUTH; default -> face; };
            case UP, DOWN -> throw new IllegalArgumentException("Fusion klystron requires a horizontal facing: " + facing);
        };
    }

    private static Direction mapCreativeKlystronPortFace(Direction facing, Direction face) {
        return switch (facing) {
            case SOUTH -> face;
            case EAST -> switch (face) { case SOUTH -> Direction.EAST; case EAST -> Direction.NORTH; case NORTH -> Direction.WEST; case WEST -> Direction.SOUTH; default -> face; };
            case NORTH -> switch (face) { case SOUTH -> Direction.NORTH; case EAST -> Direction.WEST; case NORTH -> Direction.SOUTH; case WEST -> Direction.EAST; default -> face; };
            case WEST -> switch (face) { case SOUTH -> Direction.WEST; case WEST -> Direction.NORTH; case NORTH -> Direction.EAST; case EAST -> Direction.SOUTH; default -> face; };
            case UP, DOWN -> throw new IllegalArgumentException("Creative fusion klystron requires a horizontal facing: " + facing);
        };
    }

    private static Direction mapBoilerPortFace(Direction facing, Direction face) {
        return switch (facing) {
            case SOUTH -> face;
            case EAST -> switch (face) { case SOUTH -> Direction.EAST; case EAST -> Direction.NORTH; case NORTH -> Direction.WEST; case WEST -> Direction.SOUTH; default -> face; };
            case NORTH -> switch (face) { case SOUTH -> Direction.NORTH; case EAST -> Direction.WEST; case NORTH -> Direction.SOUTH; case WEST -> Direction.EAST; default -> face; };
            case WEST -> switch (face) { case SOUTH -> Direction.WEST; case WEST -> Direction.NORTH; case NORTH -> Direction.EAST; case EAST -> Direction.SOUTH; default -> face; };
            case UP, DOWN -> throw new IllegalArgumentException("Fusion boiler requires a horizontal facing: " + facing);
        };
    }

    private static Direction mapMhdtPortFace(Direction facing, Direction face) {
        return switch (facing) {
            case SOUTH -> face;
            case EAST -> switch (face) { case SOUTH -> Direction.EAST; case EAST -> Direction.NORTH; case NORTH -> Direction.WEST; case WEST -> Direction.SOUTH; default -> face; };
            case NORTH -> switch (face) { case SOUTH -> Direction.NORTH; case EAST -> Direction.WEST; case NORTH -> Direction.SOUTH; case WEST -> Direction.EAST; default -> face; };
            case WEST -> switch (face) { case SOUTH -> Direction.WEST; case WEST -> Direction.NORTH; case NORTH -> Direction.EAST; case EAST -> Direction.SOUTH; default -> face; };
            case UP, DOWN -> throw new IllegalArgumentException("Fusion MHDT requires a horizontal facing: " + facing);
        };
    }

    private static Direction mapBreederPortFace(Direction facing, Direction face) {
        return switch (facing) {
            case SOUTH -> face;
            case EAST -> switch (face) { case SOUTH -> Direction.EAST; case EAST -> Direction.NORTH; case NORTH -> Direction.WEST; case WEST -> Direction.SOUTH; default -> face; };
            case NORTH -> switch (face) { case SOUTH -> Direction.NORTH; case EAST -> Direction.WEST; case NORTH -> Direction.SOUTH; case WEST -> Direction.EAST; default -> face; };
            case WEST -> switch (face) { case SOUTH -> Direction.WEST; case WEST -> Direction.NORTH; case NORTH -> Direction.EAST; case EAST -> Direction.SOUTH; default -> face; };
            case UP, DOWN -> throw new IllegalArgumentException("Fusion breeder requires a horizontal facing: " + facing);
        };
    }

    private static Direction mapCollectorPortFace(Direction facing, Direction face) {
        return switch (facing) {
            case SOUTH -> face;
            case EAST -> switch (face) { case SOUTH -> Direction.EAST; case EAST -> Direction.NORTH; case NORTH -> Direction.WEST; case WEST -> Direction.SOUTH; default -> face; };
            case NORTH -> switch (face) { case SOUTH -> Direction.NORTH; case EAST -> Direction.WEST; case NORTH -> Direction.SOUTH; case WEST -> Direction.EAST; default -> face; };
            case WEST -> switch (face) { case SOUTH -> Direction.WEST; case WEST -> Direction.NORTH; case NORTH -> Direction.EAST; case EAST -> Direction.SOUTH; default -> face; };
            case UP, DOWN -> throw new IllegalArgumentException("Fusion collector requires a horizontal facing: " + facing);
        };
    }

    private static Direction mapCouplerPortFace(Direction facing, Direction face) {
        return switch (facing) {
            case SOUTH -> face;
            case EAST -> switch (face) { case SOUTH -> Direction.EAST; case EAST -> Direction.NORTH; case NORTH -> Direction.WEST; case WEST -> Direction.SOUTH; default -> face; };
            case NORTH -> switch (face) { case SOUTH -> Direction.NORTH; case EAST -> Direction.WEST; case NORTH -> Direction.SOUTH; case WEST -> Direction.EAST; default -> face; };
            case WEST -> switch (face) { case SOUTH -> Direction.WEST; case WEST -> Direction.NORTH; case NORTH -> Direction.EAST; case EAST -> Direction.SOUTH; default -> face; };
            case UP, DOWN -> throw new IllegalArgumentException("Fusion coupler requires a horizontal facing: " + facing);
        };
    }

    private static Direction mapPlasmaForgePortFace(Direction facing, Direction face) {
        return switch (facing) {
            case SOUTH -> face;
            case EAST -> switch (face) { case SOUTH -> Direction.EAST; case EAST -> Direction.NORTH; case NORTH -> Direction.WEST; case WEST -> Direction.SOUTH; default -> face; };
            case NORTH -> switch (face) { case SOUTH -> Direction.NORTH; case EAST -> Direction.WEST; case NORTH -> Direction.SOUTH; case WEST -> Direction.EAST; default -> face; };
            case WEST -> switch (face) { case SOUTH -> Direction.WEST; case WEST -> Direction.NORTH; case NORTH -> Direction.EAST; case EAST -> Direction.SOUTH; default -> face; };
            case UP, DOWN -> throw new IllegalArgumentException("Fusion plasma forge requires a horizontal facing: " + facing);
        };
    }

    public record LocalPort(BlockPos offset, Direction face) {
    }

    public static List<BlockPos> absoluteTorusPorts(BlockPos corePos, Direction facing) {
        ArrayList<BlockPos> ports = new ArrayList<>(torusLocalPorts().size());
        for (LocalPort local : torusLocalPorts()) {
            ports.add(corePos.offset(rotateTorusPort(local, facing).offset()));
        }
        return List.copyOf(ports);
    }

    public static void markTorusPortDummies(Level level, BlockPos corePos, Direction facing) {
        for (BlockPos port : absoluteTorusPorts(corePos, facing)) {
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
