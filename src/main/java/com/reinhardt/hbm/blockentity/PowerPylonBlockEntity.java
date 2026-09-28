package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PowerPylonBlock;
import com.reinhardt.hbm.power.PowerGraphNode;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PowerPylonBlockEntity extends BlockEntity implements PowerGraphNode {
    private static final Set<PowerPylonBlockEntity> CLIENT_INSTANCES =
            Collections.synchronizedSet(Collections.newSetFromMap(new java.util.WeakHashMap<>()));
    private final List<BlockPos> connections = new ArrayList<>();
    private final Map<BlockPos, ConnectionRenderInfo> connectionRenderInfos = new HashMap<>();
    private final Set<BlockPos> loadedClientEndpoints = new HashSet<>();
    private final Set<AutoCloseable> clientRenderResources =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private int color;
    private int nextRenderRevision;
    private volatile RenderSnapshot renderSnapshot;

    public PowerPylonBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.POWER_PYLON.get(), pos, blockState);
        this.renderSnapshot = buildRenderSnapshot();
    }

    @Override
    public void setBlockState(BlockState state) {
        PowerPylonBlock.Kind previousKind = kind();
        Direction previousFacing = facing();
        super.setBlockState(state);
        if (this.renderSnapshot == null
                || previousKind == kind() && previousFacing == facing()) {
            return;
        }

        // LevelChunk retains a compatible block-entity instance when a block
        // state is replaced in place. Keep the immutable render snapshot and
        // every reciprocal endpoint's cached mount metadata in lockstep with
        // that live state; otherwise model, wires, and culling bounds disagree.
        syncAndDirtyGraph();
        if (this.level != null && !this.level.isClientSide) {
            for (BlockPos connection : this.connections) {
                if (this.level.getBlockEntity(connection) instanceof PowerPylonBlockEntity pylon
                        && pylon.hasConnection(this.worldPosition)
                        && pylon.rememberConnection(this.worldPosition, this)) {
                    pylon.refreshRenderSnapshot();
                    pylon.syncRenderInfoOnly();
                }
            }
        }
    }

    public PowerPylonBlock.Kind kind() {
        if (this.getBlockState().getBlock() instanceof PowerPylonBlock block) {
            return block.kind();
        }
        return PowerPylonBlock.Kind.RED_PYLON;
    }

    public List<BlockPos> connections() {
        return this.renderSnapshot.connectionPositions();
    }

    public int color() {
        return this.color;
    }

    public int renderRevision() {
        return this.renderSnapshot.revision();
    }

    public RenderSnapshot renderSnapshot() {
        return this.renderSnapshot;
    }

    public boolean setColorFrom(ItemStack stack) {
        DyeColor dyeColor = DyeColor.getColor(stack);
        if (dyeColor == null) {
            return false;
        }

        int newColor = dyeColor.getTextureDiffuseColor();
        if (newColor == this.color) {
            return false;
        }

        this.color = newColor;
        stack.shrink(1);
        syncAndDirtyGraph();
        return true;
    }

    public boolean hasConnection(BlockPos pos) {
        return this.connections.contains(pos.immutable());
    }

    public void addConnection(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (immutable.equals(this.worldPosition)) {
            return;
        }
        if (this.connections.contains(immutable)) {
            rememberLoadedConnection(immutable);
            return;
        }

        this.connections.add(immutable);
        rememberLoadedConnection(immutable);
        this.connections.sort(Comparator
                .comparingInt((BlockPos entry) -> entry.getX())
                .thenComparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getZ));
        syncAndDirtyGraph();
    }

    public void removeConnection(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        this.connectionRenderInfos.remove(immutable);
        this.loadedClientEndpoints.remove(immutable);
        if (this.connections.remove(immutable)) {
            syncAndDirtyGraph();
        }
    }

    public void disconnectAll() {
        if (this.level == null) {
            this.connections.clear();
            this.connectionRenderInfos.clear();
            this.loadedClientEndpoints.clear();
            refreshRenderSnapshot();
            return;
        }

        List<BlockPos> oldConnections = List.copyOf(this.connections);
        this.connections.clear();
        this.connectionRenderInfos.clear();
        this.loadedClientEndpoints.clear();
        for (BlockPos connection : oldConnections) {
            if (this.level.getBlockEntity(connection) instanceof PowerPylonBlockEntity pylon) {
                pylon.removeConnection(this.worldPosition);
            }
        }
        syncAndDirtyGraph();
    }

    public static int canConnect(PowerPylonBlockEntity first, PowerPylonBlockEntity second) {
        if (first.kind().connectionType() != second.kind().connectionType()) {
            return 1;
        }
        if (first == second || first.getBlockPos().equals(second.getBlockPos())) {
            return 2;
        }

        double maxLength = Math.min(first.kind().maxWireLength(), second.kind().maxWireLength());
        return first.connectionPoint().distanceTo(second.connectionPoint()) <= maxLength ? 0 : 3;
    }

    public Vec3 connectionPoint() {
        if (kind() == PowerPylonBlock.Kind.SUBSTATION) {
            // TileEntitySubstation overrides getConnectionPoint() instead of
            // using its first wire mount.
            return new Vec3(this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY() + 5.25D,
                    this.worldPosition.getZ() + 0.5D);
        }
        Vec3[] mounts = mountPositions();
        if (mounts.length == 0) {
            return Vec3.atCenterOf(this.worldPosition);
        }
        return mounts[0].add(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ());
    }

    public Vec3[] mountPositions() {
        return mountPositions(kind(), facing());
    }

    public ConnectionRenderInfo connectionRenderInfo(BlockPos connection) {
        BlockPos immutable = connection.immutable();
        for (RenderConnection renderConnection : this.renderSnapshot.connections()) {
            if (renderConnection.pos().equals(immutable)) {
                return renderConnection.remote();
            }
        }
        return null;
    }

    /** Math-free owner-local dispatch; no generic Kind+Direction transform is exposed. */
    private static Vec3[] mountPositions(PowerPylonBlock.Kind kind, Direction facing) {
        return switch (kind) {
            case RED_CONNECTOR -> redConnectorMounts(facing);
            case CONNECTOR_RED_SUPER -> connectorRedSuperMounts(facing);
            case RED_PYLON -> redPylonMounts(facing);
            case RED_PYLON_STEEL -> redPylonSteelMounts(facing);
            case RED_PYLON_MEDIUM_WOOD -> redPylonMediumWoodMounts(facing);
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> redPylonMediumWoodTransformerMounts(facing);
            case RED_PYLON_MEDIUM_STEEL -> redPylonMediumSteelMounts(facing);
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> redPylonMediumSteelTransformerMounts(facing);
            case RED_PYLON_LARGE -> redPylonLargeMounts(facing);
            case SUBSTATION -> substationMounts(facing);
        };
    }

    @Override
    public BlockPos getGraphPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return adjacentPowerPorts();
    }

    @Override
    public List<BlockPos> getRemotePowerLinks(Level level) {
        List<BlockPos> links = new ArrayList<>();
        boolean renderInfoChanged = false;
        for (BlockPos connection : this.connections) {
            if (level.getBlockEntity(connection) instanceof PowerPylonBlockEntity pylon
                    && pylon.hasConnection(this.worldPosition)
                    && pylon.kind().connectionType() == this.kind().connectionType()) {
                links.add(connection.immutable());
                renderInfoChanged |= rememberConnection(connection, pylon);
            }
        }
        if (renderInfoChanged) {
            refreshRenderSnapshot();
            if (!level.isClientSide) {
                syncRenderInfoOnly();
            }
        }
        return links;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level == null) {
            refreshRenderSnapshot();
            return;
        }

        if (this.level.isClientSide) {
            CLIENT_INSTANCES.add(this);
        }
        boolean changed = false;
        Set<BlockPos> loadedEndpoints = new HashSet<>();
        for (BlockPos connection : this.connections) {
            if (this.level.getBlockEntity(connection) instanceof PowerPylonBlockEntity pylon) {
                changed |= rememberConnection(connection, pylon);
                if (pylon.hasConnection(this.worldPosition)) {
                    boolean remoteChanged = pylon.rememberConnection(this.worldPosition, this);
                    if (this.level.isClientSide) {
                        loadedEndpoints.add(connection.immutable());
                        boolean endpointChanged = pylon.markClientEndpointLoaded(this.worldPosition, true);
                        if (remoteChanged && !endpointChanged) {
                            pylon.refreshRenderSnapshot();
                            pylon.markClientRenderSectionDirty();
                        }
                    } else if (remoteChanged) {
                        pylon.refreshRenderSnapshot();
                    }
                    if (remoteChanged && !this.level.isClientSide) {
                        pylon.syncRenderInfoOnly();
                    }
                }
            }
        }
        if (this.level.isClientSide) {
            this.loadedClientEndpoints.clear();
            this.loadedClientEndpoints.addAll(loadedEndpoints);
        }
        refreshRenderSnapshot();
        if (changed && !this.level.isClientSide) {
            syncRenderInfoOnly();
        }
    }

    public List<BlockPos> adjacentPowerPorts() {
        return switch (kind()) {
            case RED_CONNECTOR -> redConnectorAdjacentPowerPorts();
            case CONNECTOR_RED_SUPER -> connectorRedSuperAdjacentPowerPorts();
            case RED_PYLON -> redPylonAdjacentPowerPorts();
            case RED_PYLON_STEEL -> redPylonSteelAdjacentPowerPorts();
            case RED_PYLON_MEDIUM_WOOD -> List.of();
            case RED_PYLON_MEDIUM_WOOD_TRANSFORMER -> redPylonMediumWoodTransformerAdjacentPowerPorts();
            case RED_PYLON_MEDIUM_STEEL -> List.of();
            case RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> redPylonMediumSteelTransformerAdjacentPowerPorts();
            case RED_PYLON_LARGE -> List.of();
            case SUBSTATION -> substationAdjacentPorts();
        };
    }

    public AABB getRenderBoundingBox() {
        return this.renderSnapshot.bounds();
    }

    public Direction facing() {
        BlockState state = this.getBlockState();
        if (state.hasProperty(PowerPylonBlock.FACING)) {
            return state.getValue(PowerPylonBlock.FACING);
        }
        return Direction.SOUTH;
    }

    private static Vec3[] redConnectorMounts(Direction facing) {
        return switch (facing) {
            case DOWN -> new Vec3[]{new Vec3(0.5D, 0.5D, 0.5D)};
            case UP -> new Vec3[]{new Vec3(0.5D, 0.5D, 0.5D)};
            case NORTH -> new Vec3[]{new Vec3(0.5D, 0.5D, 0.5D)};
            case SOUTH -> new Vec3[]{new Vec3(0.5D, 0.5D, 0.5D)};
            case WEST -> new Vec3[]{new Vec3(0.5D, 0.5D, 0.5D)};
            case EAST -> new Vec3[]{new Vec3(0.5D, 0.5D, 0.5D)};
        };
    }

    private static Vec3[] connectorRedSuperMounts(Direction facing) {
        return switch (facing) {
            case DOWN -> new Vec3[]{new Vec3(0.5D, 0.125D, 0.5D)};
            case UP -> new Vec3[]{new Vec3(0.5D, 0.875D, 0.5D)};
            case NORTH -> new Vec3[]{new Vec3(0.5D, 0.5D, 0.125D)};
            case SOUTH -> new Vec3[]{new Vec3(0.5D, 0.5D, 0.875D)};
            case WEST -> new Vec3[]{new Vec3(0.125D, 0.5D, 0.5D)};
            case EAST -> new Vec3[]{new Vec3(0.875D, 0.5D, 0.5D)};
        };
    }

    private static Vec3[] redPylonMounts(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3[]{new Vec3(0.5D, 5.5D, 0.5D)};
            case SOUTH -> new Vec3[]{new Vec3(0.5D, 5.5D, 0.5D)};
            case WEST -> new Vec3[]{new Vec3(0.5D, 5.5D, 0.5D)};
            case EAST -> new Vec3[]{new Vec3(0.5D, 5.5D, 0.5D)};
            case UP -> new Vec3[]{new Vec3(0.5D, 5.5D, 0.5D)};
            case DOWN -> new Vec3[]{new Vec3(0.5D, 5.5D, 0.5D)};
        };
    }

    private static Vec3[] redPylonSteelMounts(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3[]{new Vec3(0.5D, 5.5D, 0.5D)};
            case SOUTH -> new Vec3[]{new Vec3(0.5D, 5.5D, 0.5D)};
            case WEST -> new Vec3[]{new Vec3(0.5D, 5.5D, 0.5D)};
            case EAST -> new Vec3[]{new Vec3(0.5D, 5.5D, 0.5D)};
            case UP -> new Vec3[]{new Vec3(0.5D, 5.5D, 0.5D)};
            case DOWN -> new Vec3[]{new Vec3(0.5D, 5.5D, 0.5D)};
        };
    }

    private static Vec3[] redPylonMediumWoodMounts(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, -0.5D), new Vec3(0.5D, 7.5D, -1.5D)};
            case SOUTH -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, 1.5D), new Vec3(0.5D, 7.5D, 2.5D)};
            case WEST -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(-0.5D, 7.5D, 0.5D), new Vec3(-1.5D, 7.5D, 0.5D)};
            case EAST -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(1.5D, 7.5D, 0.5D), new Vec3(2.5D, 7.5D, 0.5D)};
            case UP -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, 1.5D), new Vec3(0.5D, 7.5D, 2.5D)};
            case DOWN -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, 1.5D), new Vec3(0.5D, 7.5D, 2.5D)};
        };
    }

    private static Vec3[] redPylonMediumWoodTransformerMounts(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, -0.5D), new Vec3(0.5D, 7.5D, -1.5D)};
            case SOUTH -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, 1.5D), new Vec3(0.5D, 7.5D, 2.5D)};
            case WEST -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(-0.5D, 7.5D, 0.5D), new Vec3(-1.5D, 7.5D, 0.5D)};
            case EAST -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(1.5D, 7.5D, 0.5D), new Vec3(2.5D, 7.5D, 0.5D)};
            case UP -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, 1.5D), new Vec3(0.5D, 7.5D, 2.5D)};
            case DOWN -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, 1.5D), new Vec3(0.5D, 7.5D, 2.5D)};
        };
    }

    private static Vec3[] redPylonMediumSteelMounts(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, -0.5D), new Vec3(0.5D, 7.5D, -1.5D)};
            case SOUTH -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, 1.5D), new Vec3(0.5D, 7.5D, 2.5D)};
            case WEST -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(-0.5D, 7.5D, 0.5D), new Vec3(-1.5D, 7.5D, 0.5D)};
            case EAST -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(1.5D, 7.5D, 0.5D), new Vec3(2.5D, 7.5D, 0.5D)};
            case UP -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, 1.5D), new Vec3(0.5D, 7.5D, 2.5D)};
            case DOWN -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, 1.5D), new Vec3(0.5D, 7.5D, 2.5D)};
        };
    }

    private static Vec3[] redPylonMediumSteelTransformerMounts(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, -0.5D), new Vec3(0.5D, 7.5D, -1.5D)};
            case SOUTH -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, 1.5D), new Vec3(0.5D, 7.5D, 2.5D)};
            case WEST -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(-0.5D, 7.5D, 0.5D), new Vec3(-1.5D, 7.5D, 0.5D)};
            case EAST -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(1.5D, 7.5D, 0.5D), new Vec3(2.5D, 7.5D, 0.5D)};
            case UP -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, 1.5D), new Vec3(0.5D, 7.5D, 2.5D)};
            case DOWN -> new Vec3[]{new Vec3(0.5D, 7.5D, 0.5D), new Vec3(0.5D, 7.5D, 1.5D), new Vec3(0.5D, 7.5D, 2.5D)};
        };
    }

    private static Vec3[] redPylonLargeMounts(Direction facing) {
        // TileEntityPylonLarge rotates (3.375, 0, 0) by 0/45/90/135 degrees.
        // These literals retain 1.7.10 MathHelper's float sine-table edges; they
        // are not values inferred from the renderer's visually similar yaw.
        return switch (facing) {
            case NORTH -> new Vec3[]{
                    new Vec3(3.875D, 12.3125D, 0.5D), new Vec3(3.875D, 10.6875D, 0.5D),
                    new Vec3(-2.875D, 12.3125D, 0.5D), new Vec3(-2.875D, 10.6875D, 0.5D)};
            case WEST -> new Vec3[]{
                    new Vec3(2.88648534566164D, 12.3125D, -1.8864853456616402D),
                    new Vec3(2.88648534566164D, 10.6875D, -1.8864853456616402D),
                    new Vec3(-1.8864853456616402D, 12.3125D, 2.88648534566164D),
                    new Vec3(-1.8864853456616402D, 10.6875D, 2.88648534566164D)};
            case SOUTH -> new Vec3[]{
                    new Vec3(0.5000000000000004D, 12.3125D, -2.875D),
                    new Vec3(0.5000000000000004D, 10.6875D, -2.875D),
                    new Vec3(0.4999999999999996D, 12.3125D, 3.875D),
                    new Vec3(0.4999999999999996D, 10.6875D, 3.875D)};
            case EAST -> new Vec3[]{
                    new Vec3(-1.8864853456616402D, 12.3125D, -1.8864853456616402D),
                    new Vec3(-1.8864853456616402D, 10.6875D, -1.8864853456616402D),
                    new Vec3(2.88648534566164D, 12.3125D, 2.88648534566164D),
                    new Vec3(2.88648534566164D, 10.6875D, 2.88648534566164D)};
            case UP -> new Vec3[]{
                    new Vec3(3.875D, 12.3125D, 0.5D), new Vec3(3.875D, 10.6875D, 0.5D),
                    new Vec3(-2.875D, 12.3125D, 0.5D), new Vec3(-2.875D, 10.6875D, 0.5D)};
            case DOWN -> new Vec3[]{
                    new Vec3(3.875D, 12.3125D, 0.5D), new Vec3(3.875D, 10.6875D, 0.5D),
                    new Vec3(-2.875D, 12.3125D, 0.5D), new Vec3(-2.875D, 10.6875D, 0.5D)};
        };
    }

    private static Vec3[] substationMounts(Direction facing) {
        // WEST/EAST retain Vec3.rotateAroundY(+PI/2)'s negative-Z order and
        // MathHelper float sine-table edge values from 1.7.10.
        return switch (facing) {
            case NORTH -> new Vec3[]{new Vec3(1.0D, 5.25D, 0.5D), new Vec3(2.0D, 5.25D, 0.5D), new Vec3(0.0D, 5.25D, 0.5D), new Vec3(-1.0D, 5.25D, 0.5D)};
            case SOUTH -> new Vec3[]{new Vec3(1.0D, 5.25D, 0.5D), new Vec3(2.0D, 5.25D, 0.5D), new Vec3(0.0D, 5.25D, 0.5D), new Vec3(-1.0D, 5.25D, 0.5D)};
            case WEST -> new Vec3[]{new Vec3(0.5000000000000001D, 5.25D, 0.0D), new Vec3(0.5000000000000002D, 5.25D, -1.0D), new Vec3(0.49999999999999994D, 5.25D, 1.0D), new Vec3(0.49999999999999983D, 5.25D, 2.0D)};
            case EAST -> new Vec3[]{new Vec3(0.5000000000000001D, 5.25D, 0.0D), new Vec3(0.5000000000000002D, 5.25D, -1.0D), new Vec3(0.49999999999999994D, 5.25D, 1.0D), new Vec3(0.49999999999999983D, 5.25D, 2.0D)};
            case UP -> new Vec3[]{new Vec3(1.0D, 5.25D, 0.5D), new Vec3(2.0D, 5.25D, 0.5D), new Vec3(0.0D, 5.25D, 0.5D), new Vec3(-1.0D, 5.25D, 0.5D)};
            case DOWN -> new Vec3[]{new Vec3(1.0D, 5.25D, 0.5D), new Vec3(2.0D, 5.25D, 0.5D), new Vec3(0.0D, 5.25D, 0.5D), new Vec3(-1.0D, 5.25D, 0.5D)};
        };
    }

    private List<BlockPos> redConnectorAdjacentPowerPorts() {
        return List.of(this.worldPosition.relative(facing().getOpposite()));
    }

    private List<BlockPos> connectorRedSuperAdjacentPowerPorts() {
        return List.of(this.worldPosition.relative(facing().getOpposite()));
    }

    private List<BlockPos> redPylonAdjacentPowerPorts() {
        BlockPos pos = this.worldPosition;
        return List.of(pos.north(), pos.south(), pos.west(), pos.east());
    }

    private List<BlockPos> redPylonSteelAdjacentPowerPorts() {
        BlockPos pos = this.worldPosition;
        return List.of(pos.north(), pos.south(), pos.west(), pos.east());
    }

    private List<BlockPos> redPylonMediumWoodTransformerAdjacentPowerPorts() {
        return List.of(this.worldPosition.relative(facing().getOpposite()));
    }

    private List<BlockPos> redPylonMediumSteelTransformerAdjacentPowerPorts() {
        return List.of(this.worldPosition.relative(facing().getOpposite()));
    }

    private List<BlockPos> substationAdjacentPorts() {
        BlockPos pos = this.worldPosition;
        return List.of(
                pos.offset(2, 0, -1),
                pos.offset(2, 0, 1),
                pos.offset(-2, 0, -1),
                pos.offset(-2, 0, 1),
                pos.offset(-1, 0, 2),
                pos.offset(1, 0, 2),
                pos.offset(-1, 0, -2),
                pos.offset(1, 0, -2)
        );
    }

    private void syncAndDirtyGraph() {
        refreshClientEndpointStates();
        refreshRenderSnapshot();
        setChanged();
        if (this.level != null) {
            PowerNetworkManager.markDirty(this.level);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Color", this.color);
        ListTag list = new ListTag();
        for (BlockPos connection : this.connections) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("X", connection.getX());
            entry.putInt("Y", connection.getY());
            entry.putInt("Z", connection.getZ());
            ConnectionRenderInfo renderInfo = this.connectionRenderInfos.get(connection);
            if (renderInfo != null) {
                entry.putString("RemoteKind", renderInfo.kind().name());
                entry.putString("RemoteFacing", renderInfo.facing().getName());
            }
            list.add(entry);
        }
        tag.put("Connections", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.color = tag.getInt("Color");
        this.connections.clear();
        this.connectionRenderInfos.clear();
        ListTag list = tag.getList("Connections", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos connection = new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"));
            this.connections.add(connection);
            if (entry.contains("RemoteKind", Tag.TAG_STRING) && entry.contains("RemoteFacing", Tag.TAG_STRING)) {
                try {
                    PowerPylonBlock.Kind remoteKind = PowerPylonBlock.Kind.valueOf(entry.getString("RemoteKind"));
                    Direction remoteFacing = Direction.byName(entry.getString("RemoteFacing"));
                    if (remoteFacing != null) {
                        this.connectionRenderInfos.put(connection, new ConnectionRenderInfo(connection, remoteKind, remoteFacing));
                    }
                } catch (IllegalArgumentException ignored) {
                    // Preserve compatibility with saves if a future version removes or renames a pylon kind.
                }
            }
        }
        refreshClientEndpointStates();
        refreshRenderSnapshot();
        if (this.level != null && this.level.isClientSide) {
            markClientRenderSectionDirty();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private ConnectionRenderInfo selfRenderInfo() {
        return new ConnectionRenderInfo(this.worldPosition, kind(), facing());
    }

    private void rememberLoadedConnection(BlockPos connection) {
        if (this.level != null && this.level.getBlockEntity(connection) instanceof PowerPylonBlockEntity pylon) {
            boolean changed = rememberConnection(connection, pylon);
            boolean endpointChanged = this.level.isClientSide
                    && pylon.hasConnection(this.worldPosition)
                    && this.loadedClientEndpoints.add(connection.immutable());
            if (changed || endpointChanged) {
                refreshRenderSnapshot();
                if (this.level.isClientSide) {
                    markClientRenderSectionDirty();
                }
            }
        }
    }

    private boolean rememberConnection(BlockPos connection, PowerPylonBlockEntity pylon) {
        BlockPos immutable = connection.immutable();
        ConnectionRenderInfo updated = pylon.selfRenderInfo();
        ConnectionRenderInfo previous = this.connectionRenderInfos.put(immutable, updated);
        return !updated.equals(previous);
    }

    private void refreshClientEndpointStates() {
        if (this.level == null || !this.level.isClientSide) {
            this.loadedClientEndpoints.clear();
            return;
        }
        Set<BlockPos> previouslyLoaded = Set.copyOf(this.loadedClientEndpoints);
        Set<BlockPos> loaded = new HashSet<>();
        for (BlockPos connection : this.connections) {
            if (this.level.getBlockEntity(connection) instanceof PowerPylonBlockEntity pylon
                    && pylon.hasConnection(this.worldPosition)) {
                BlockPos immutable = connection.immutable();
                loaded.add(immutable);
                rememberConnection(connection, pylon);

                // Connection update packets for the two endpoints are not ordered as one
                // transaction.  Whichever endpoint receives the reciprocal connection
                // second must publish the half-span state to the already-updated peer as
                // well, otherwise one renderer retains a full cable while the other draws
                // its half and the overlap z-fights indefinitely.
                boolean remoteInfoChanged = pylon.rememberConnection(this.worldPosition, this);
                boolean remoteEndpointChanged = pylon.markClientEndpointLoaded(this.worldPosition, true);
                if (remoteInfoChanged && !remoteEndpointChanged) {
                    pylon.refreshRenderSnapshot();
                    pylon.markClientRenderSectionDirty();
                }
            }
        }
        for (BlockPos connection : previouslyLoaded) {
            if (!loaded.contains(connection)
                    && this.level.getBlockEntity(connection) instanceof PowerPylonBlockEntity pylon) {
                pylon.markClientEndpointLoaded(this.worldPosition, false);
            }
        }
        this.loadedClientEndpoints.clear();
        this.loadedClientEndpoints.addAll(loaded);
    }

    private void refreshRenderSnapshot() {
        this.renderSnapshot = buildRenderSnapshot();
    }

    private RenderSnapshot buildRenderSnapshot() {
        PowerPylonBlock.Kind localKind = kind();
        Direction localFacing = facing();
        Vec3[] localMounts = mountPositions(localKind, localFacing);
        List<RenderConnection> renderConnections = new ArrayList<>(this.connections.size());
        List<BlockPos> positions = new ArrayList<>(this.connections.size());
        List<PowerPylonWireLayout.WireQuad> wireQuads = new ArrayList<>();
        Object modelGeometryToken = null;
        long modelRevision = -1L;
        AABB bounds = new AABB(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(),
                this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ());
        // The common-side bridge deliberately has no reference to a client class. Its
        // provider is installed during client renderer registration; on a dedicated
        // server it remains absent and this immutable render-only token stays null.
        PowerPylonRenderGeometry.GeometryResult geometry =
                PowerPylonRenderGeometry.capture(this, this.getBlockState(), localKind, localFacing);
        if (geometry != null) {
            modelGeometryToken = geometry.geometryToken();
            modelRevision = geometry.revision();
            bounds = geometry.bounds();
        }

        for (BlockPos connection : this.connections) {
            BlockPos immutable = connection.immutable();
            positions.add(immutable);
            ConnectionRenderInfo remote = this.connectionRenderInfos.get(immutable);
            boolean halfSpan = this.loadedClientEndpoints.contains(immutable);
            List<Integer> mountMapping = new ArrayList<>();
            if (remote != null) {
                Vec3[] remoteMounts = remote.mountPositions();
                int lineCount = Math.min(localMounts.length, remoteMounts.length);
                int[] mapping = PowerPylonWireLayout.mountMapping(this, remote, lineCount);
                for (int value : mapping) {
                    mountMapping.add(value);
                }
                for (int line = 0; line < lineCount; line++) {
                    // Build in the same block-local coordinate system uploaded to the VBO.
                    // appendSaggingWire quantizes every emitted position to float once; its
                    // returned bounds and the renderer then consume those identical vertices.
                    Vec3 start = localMounts[line];
                    Vec3 fullEnd = remoteMounts[mapping[line]].add(
                            immutable.getX() - this.worldPosition.getX(),
                            immutable.getY() - this.worldPosition.getY(),
                            immutable.getZ() - this.worldPosition.getZ());
                    Vec3 renderEnd = halfSpan
                            ? start.add(fullEnd.subtract(start).scale(0.5D))
                            : fullEnd;
                    AABB localWireBounds = PowerPylonWireLayout.appendSaggingWire(
                            wireQuads, start, renderEnd, !halfSpan);
                    bounds = bounds.minmax(localWireBounds.move(this.worldPosition));
                }
            }
            renderConnections.add(new RenderConnection(immutable, remote, halfSpan, mountMapping));
        }

        return new RenderSnapshot(
                ++this.nextRenderRevision,
                this.getBlockState(),
                localKind,
                localFacing,
                this.color,
                modelGeometryToken,
                modelRevision,
                renderConnections,
                positions,
                wireQuads,
                bounds
        );
    }

    private boolean markClientEndpointLoaded(BlockPos connection, boolean loaded) {
        if (this.level == null || !this.level.isClientSide || !this.connections.contains(connection)) {
            return false;
        }
        BlockPos immutable = connection.immutable();
        boolean changed = loaded
                ? this.loadedClientEndpoints.add(immutable)
                : this.loadedClientEndpoints.remove(immutable);
        if (changed) {
            refreshRenderSnapshot();
            markClientRenderSectionDirty();
        }
        return changed;
    }

    private void markClientRenderSectionDirty() {
        if (this.level != null && this.level.isClientSide) {
            this.level.sendBlockUpdated(
                    this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Client model-bake hook: replaces bounds and the captured render geometry atomically. */
    public void refreshClientModelGeometryAfterBake() {
        if (this.level != null && this.level.isClientSide) {
            refreshRenderSnapshot();
            markClientRenderSectionDirty();
        }
    }

    public static void onClientChunkUnload(Level level, ChunkPos chunkPos) {
        List<PowerPylonBlockEntity> pylons;
        synchronized (CLIENT_INSTANCES) {
            pylons = List.copyOf(CLIENT_INSTANCES);
        }
        for (PowerPylonBlockEntity pylon : pylons) {
            if (pylon.level != level || pylon.isRemoved()) {
                continue;
            }
            for (BlockPos connection : pylon.renderSnapshot.connectionPositions()) {
                if (chunkPos.equals(new ChunkPos(connection))) {
                    pylon.markClientEndpointLoaded(connection, false);
                }
            }
        }
    }

    public void registerClientRenderResource(AutoCloseable resource) {
        this.clientRenderResources.add(resource);
    }

    public void unregisterClientRenderResource(AutoCloseable resource) {
        this.clientRenderResources.remove(resource);
    }

    private void closeClientRenderResources() {
        List<AutoCloseable> resources = List.copyOf(this.clientRenderResources);
        this.clientRenderResources.clear();
        for (AutoCloseable resource : resources) {
            closeResource(resource);
        }
    }

    private static void closeResource(@Nullable AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ignored) {
                // Client render resources are best-effort during teardown.
            }
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && this.level.isClientSide) {
            CLIENT_INSTANCES.remove(this);
            for (BlockPos connection : this.renderSnapshot.connectionPositions()) {
                if (this.level.getBlockEntity(connection) instanceof PowerPylonBlockEntity pylon) {
                    pylon.markClientEndpointLoaded(this.worldPosition, false);
                }
            }
        }
        closeClientRenderResources();
        super.setRemoved();
    }

    private void syncRenderInfoOnly() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public record ConnectionRenderInfo(BlockPos pos, PowerPylonBlock.Kind kind, Direction facing) {
        public ConnectionRenderInfo {
            pos = pos.immutable();
        }

        public Vec3[] mountPositions() {
            return PowerPylonBlockEntity.mountPositions(this.kind, this.facing);
        }
    }

    public record RenderConnection(BlockPos pos, @Nullable ConnectionRenderInfo remote,
                                   boolean halfSpan, List<Integer> mountMapping) {
        public RenderConnection {
            pos = pos.immutable();
            mountMapping = List.copyOf(mountMapping);
        }

        public int remoteMountIndex(int line) {
            return this.mountMapping.get(line);
        }
    }

    public record RenderSnapshot(int revision, BlockState blockState,
                                 PowerPylonBlock.Kind kind, Direction facing, int color,
                                 Object modelGeometryToken, long modelRevision,
                                 List<RenderConnection> connections,
                                 List<BlockPos> connectionPositions,
                                 List<PowerPylonWireLayout.WireQuad> wireQuads,
                                 AABB bounds) {
        public RenderSnapshot {
            connections = List.copyOf(connections);
            connectionPositions = List.copyOf(connectionPositions);
            wireQuads = List.copyOf(wireQuads);
        }
    }
}
