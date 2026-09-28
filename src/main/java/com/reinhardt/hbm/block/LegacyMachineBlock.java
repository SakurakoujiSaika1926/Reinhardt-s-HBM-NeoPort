package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.event.LegacyMobSpawnEvents;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

/**
 * Shared structural block for legacy 1.7.10 machines whose complete geometry
 * is supplied by an OBJ model. The footprint is the old BlockDummyable volume,
 * not an approximation of the rendered mesh.
 */
public class LegacyMachineBlock extends LargeMachineBlock implements EntityBlock {
    private static final DustParticleOptions FORCEFIELD_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), 1.0F);
    @Nullable
    private final Kind kind;

    public LegacyMachineBlock(BlockBehaviour.Properties properties, Footprint footprint, VoxelShape coreShape, Kind kind) {
        super(properties, footprint, coreShape, offsetMapper(kind));
        this.kind = kind;
    }

    protected LegacyMachineBlock(BlockBehaviour.Properties properties, Footprint footprint, VoxelShape coreShape,
                                 OffsetMapper offsetMapper) {
        super(properties, footprint, coreShape, offsetMapper);
        this.kind = null;
    }

    public enum Kind {
        ANNIHILATOR, AUTOCRAFTER, AUTOSAW, THRESHER, LPW2, FORCEFIELD,
        MISSILE_ASSEMBLY, ORBUS, PRECASS, PYROOVEN, RADAR, RADAR_LARGE,
        RADGEN, RADIOLYSIS, RTG_GREY, TELEPORTER, TURBOFAN
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return switch (this.kind) {
            case ANNIHILATOR -> annihilatorStateForPlacement(context);
            case AUTOCRAFTER -> autocrafterStateForPlacement(context);
            case AUTOSAW -> autosawStateForPlacement(context);
            case THRESHER -> thresherStateForPlacement(context);
            case LPW2 -> lpw2StateForPlacement(context);
            case FORCEFIELD -> forcefieldStateForPlacement(context);
            case MISSILE_ASSEMBLY -> missileAssemblyStateForPlacement(context);
            case ORBUS -> orbusStateForPlacement(context);
            case PRECASS -> precassStateForPlacement(context);
            case PYROOVEN -> pyroovenStateForPlacement(context);
            case RADAR -> radarStateForPlacement(context);
            case RADAR_LARGE -> largeRadarStateForPlacement(context);
            case RADGEN -> radgenStateForPlacement(context);
            case RADIOLYSIS -> radiolysisStateForPlacement(context);
            case RTG_GREY -> greyRtgStateForPlacement(context);
            case TELEPORTER -> teleporterStateForPlacement(context);
            case TURBOFAN -> turbofanStateForPlacement(context);
        };
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return switch (this.kind) {
            case ANNIHILATOR -> rotateAnnihilator(state, rotation);
            case AUTOCRAFTER -> rotateAutocrafter(state, rotation);
            case AUTOSAW -> rotateAutosaw(state, rotation);
            case THRESHER -> rotateThresher(state, rotation);
            case LPW2 -> rotateLpw2(state, rotation);
            case FORCEFIELD -> rotateForcefield(state, rotation);
            case MISSILE_ASSEMBLY -> rotateMissileAssembly(state, rotation);
            case ORBUS -> rotateOrbus(state, rotation);
            case PRECASS -> rotatePrecass(state, rotation);
            case PYROOVEN -> rotatePyrooven(state, rotation);
            case RADAR -> rotateRadar(state, rotation);
            case RADAR_LARGE -> rotateLargeRadar(state, rotation);
            case RADGEN -> rotateRadgen(state, rotation);
            case RADIOLYSIS -> rotateRadiolysis(state, rotation);
            case RTG_GREY -> rotateGreyRtg(state, rotation);
            case TELEPORTER -> rotateTeleporter(state, rotation);
            case TURBOFAN -> rotateTurbofan(state, rotation);
        };
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return switch (this.kind) {
            case ANNIHILATOR -> mirrorAnnihilator(state, mirror);
            case AUTOCRAFTER -> mirrorAutocrafter(state, mirror);
            case AUTOSAW -> mirrorAutosaw(state, mirror);
            case THRESHER -> mirrorThresher(state, mirror);
            case LPW2 -> mirrorLpw2(state, mirror);
            case FORCEFIELD -> mirrorForcefield(state, mirror);
            case MISSILE_ASSEMBLY -> mirrorMissileAssembly(state, mirror);
            case ORBUS -> mirrorOrbus(state, mirror);
            case PRECASS -> mirrorPrecass(state, mirror);
            case PYROOVEN -> mirrorPyrooven(state, mirror);
            case RADAR -> mirrorRadar(state, mirror);
            case RADAR_LARGE -> mirrorLargeRadar(state, mirror);
            case RADGEN -> mirrorRadgen(state, mirror);
            case RADIOLYSIS -> mirrorRadiolysis(state, mirror);
            case RTG_GREY -> mirrorGreyRtg(state, mirror);
            case TELEPORTER -> mirrorTeleporter(state, mirror);
            case TURBOFAN -> mirrorTurbofan(state, mirror);
        };
    }

    private BlockState annihilatorStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState autocrafterStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState autosawStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState thresherStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState lpw2StateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState forcefieldStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState missileAssemblyStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState orbusStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState precassStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState pyroovenStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState radarStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState largeRadarStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState radgenStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState radiolysisStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState greyRtgStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState teleporterStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState turbofanStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context) ? state : null;
    }

    private BlockState rotateAnnihilator(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateAutocrafter(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateAutosaw(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateThresher(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateLpw2(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateForcefield(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateMissileAssembly(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateOrbus(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotatePrecass(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotatePyrooven(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateRadar(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateLargeRadar(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateRadgen(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateRadiolysis(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateGreyRtg(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateTeleporter(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState rotateTurbofan(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    private BlockState mirrorAnnihilator(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorAutocrafter(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorAutosaw(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorThresher(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorLpw2(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorForcefield(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorMissileAssembly(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorOrbus(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorPrecass(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorPyrooven(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorRadar(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorLargeRadar(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorRadgen(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorRadiolysis(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorGreyRtg(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorTeleporter(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private BlockState mirrorTurbofan(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    private static OffsetMapper offsetMapper(Kind kind) {
        return switch (kind) {
            case ANNIHILATOR -> LegacyMachineBlock::mapAnnihilatorOffset;
            case AUTOCRAFTER -> LegacyMachineBlock::mapAutocrafterOffset;
            case AUTOSAW -> LegacyMachineBlock::mapAutosawOffset;
            case THRESHER -> LegacyMachineBlock::mapThresherOffset;
            case LPW2 -> LegacyMachineBlock::mapLpw2Offset;
            case FORCEFIELD -> LegacyMachineBlock::mapForcefieldOffset;
            case MISSILE_ASSEMBLY -> LegacyMachineBlock::mapMissileAssemblyOffset;
            case ORBUS -> LegacyMachineBlock::mapOrbusOffset;
            case PRECASS -> LegacyMachineBlock::mapPrecassOffset;
            case PYROOVEN -> LegacyMachineBlock::mapPyroovenOffset;
            case RADAR -> LegacyMachineBlock::mapRadarOffset;
            case RADAR_LARGE -> LegacyMachineBlock::mapLargeRadarOffset;
            case RADGEN -> LegacyMachineBlock::mapRadgenOffset;
            case RADIOLYSIS -> LegacyMachineBlock::mapRadiolysisOffset;
            case RTG_GREY -> LegacyMachineBlock::mapGreyRtgOffset;
            case TELEPORTER -> LegacyMachineBlock::mapTeleporterOffset;
            case TURBOFAN -> LegacyMachineBlock::mapTurbofanOffset;
        };
    }

    private static BlockPos mapAnnihilatorOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Annihilator", facing); };
    }
    private static BlockPos mapAutocrafterOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Autocrafter", facing); };
    }
    private static BlockPos mapAutosawOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Autosaw", facing); };
    }
    private static BlockPos mapThresherOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Thresher", facing); };
    }
    private static BlockPos mapLpw2Offset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("LPW2", facing); };
    }
    private static BlockPos mapForcefieldOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Forcefield", facing); };
    }
    private static BlockPos mapMissileAssemblyOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Missile assembly", facing); };
    }
    private static BlockPos mapOrbusOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Orbus", facing); };
    }
    private static BlockPos mapPrecassOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Precass", facing); };
    }
    private static BlockPos mapPyroovenOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Pyrooven", facing); };
    }
    private static BlockPos mapRadarOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Radar", facing); };
    }
    private static BlockPos mapLargeRadarOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Large radar", facing); };
    }
    private static BlockPos mapRadgenOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Radgen", facing); };
    }
    private static BlockPos mapRadiolysisOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Radiolysis", facing); };
    }
    private static BlockPos mapGreyRtgOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Grey RTG", facing); };
    }
    private static BlockPos mapTeleporterOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Teleporter", facing); };
    }
    private static BlockPos mapTurbofanOffset(Direction facing, BlockPos offset) {
        int x=offset.getX(), y=offset.getY(), z=offset.getZ(); return switch(facing) { case SOUTH -> new BlockPos(x,y,z); case EAST -> new BlockPos(z,y,-x); case NORTH -> new BlockPos(-x,y,-z); case WEST -> new BlockPos(-z,y,x); case UP,DOWN -> throw horizontal("Turbofan", facing); };
    }

    private static IllegalArgumentException horizontal(String machine, Direction facing) {
        return new IllegalArgumentException(machine + " requires a horizontal facing: " + facing);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LegacyMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != HbmBlockEntities.LEGACY_MACHINE.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, entity) -> {
            if (entity instanceof LegacyMachineBlockEntity machine) {
                LegacyMachineBlockEntity.tick(tickerLevel, tickerPos, tickerState, machine);
            }
        };
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!BuiltInRegistries.BLOCK.getKey(this).getPath().equals("machine_forcefield")
                || !(level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine)) {
            return;
        }
        if (machine.forcefieldCooldown() > 0) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.SMOKE,
                        pos.getX() + random.nextFloat(), pos.getY() + 2.0D,
                        pos.getZ() + random.nextFloat(), 0.0D, 0.0D, 0.0D);
            }
        } else if (machine.forcefieldRenderable()) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(machine.forcefieldColor() == 0xFF0000 ? ParticleTypes.LAVA : FORCEFIELD_DUST,
                        pos.getX() + random.nextFloat(), pos.getY() + 2.0D,
                        pos.getZ() + random.nextFloat(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        markFbiTarget(level, player, machine);
        if ((machine.machineId().equals("machine_radar") || machine.machineId().equals("machine_radar_large"))
                && pos.getY() < HbmConfig.RADAR_ALTITUDE.get()) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.reinhardtshbm.radar.altitude")
                        .withStyle(ChatFormatting.RED), false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // MachineOrbus uses crouching right-click as a non-GUI interaction.
        if (player.isCrouching()) {
            return machine.machineId().equals("machine_orbus")
                    ? InteractionResult.sidedSuccess(level.isClientSide)
                    : InteractionResult.PASS;
        }
        if (machine.handleEmptyHandInteraction(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!machine.hasMenu()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider menu && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menu, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine
                && (machine.machineId().equals("machine_radar") || machine.machineId().equals("machine_radar_large"))
                ? machine.radarRedPower() : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            net.minecraft.world.item.ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine
                && machine.handleItemInteraction(player, stack)) {
            markFbiTarget(level, player, machine);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.getBlockEntity(pos) instanceof LegacyMachineBlockEntity machine) {
            markFbiTarget(level, player, machine);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static void markFbiTarget(Level level, Player player, LegacyMachineBlockEntity machine) {
        if (!level.isClientSide && !player.isCrouching() && player instanceof ServerPlayer serverPlayer
                && (machine.machineId().equals("machine_missile_assembly")
                || machine.machineId().equals("machine_radiolysis"))) {
            LegacyMobSpawnEvents.markFbi(serverPlayer);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
