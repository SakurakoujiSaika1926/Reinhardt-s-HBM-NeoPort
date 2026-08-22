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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PowerPylonBlockEntity extends BlockEntity implements PowerGraphNode {
    private final List<BlockPos> connections = new ArrayList<>();
    private int color;

    public PowerPylonBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.POWER_PYLON.get(), pos, blockState);
    }

    public PowerPylonBlock.Kind kind() {
        if (this.getBlockState().getBlock() instanceof PowerPylonBlock block) {
            return block.kind();
        }
        return PowerPylonBlock.Kind.RED_PYLON;
    }

    public List<BlockPos> connections() {
        return List.copyOf(this.connections);
    }

    public int color() {
        return this.color;
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
        if (immutable.equals(this.worldPosition) || this.connections.contains(immutable)) {
            return;
        }

        this.connections.add(immutable);
        this.connections.sort(Comparator
                .comparingInt((BlockPos entry) -> entry.getX())
                .thenComparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getZ));
        syncAndDirtyGraph();
    }

    public void removeConnection(BlockPos pos) {
        if (this.connections.remove(pos.immutable())) {
            syncAndDirtyGraph();
        }
    }

    public void disconnectAll() {
        if (this.level == null) {
            this.connections.clear();
            return;
        }

        List<BlockPos> oldConnections = List.copyOf(this.connections);
        this.connections.clear();
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
        PowerPylonBlock.Kind kind = kind();
        Direction facing = facing();

        return switch (kind) {
            case RED_CONNECTOR -> new Vec3[]{new Vec3(0.5D, 0.5D, 0.5D)};
            case CONNECTOR_RED_SUPER -> new Vec3[]{new Vec3(0.5D, 0.875D, 0.5D)};
            case RED_PYLON -> new Vec3[]{new Vec3(0.5D, 5.4D, 0.5D)};
            case RED_PYLON_MEDIUM_WOOD,
                 RED_PYLON_MEDIUM_WOOD_TRANSFORMER,
                 RED_PYLON_MEDIUM_STEEL,
                 RED_PYLON_MEDIUM_STEEL_TRANSFORMER -> mediumPylonMounts(facing);
            case RED_PYLON_LARGE -> largePylonMounts(facing);
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
        for (BlockPos connection : this.connections) {
            if (level.getBlockEntity(connection) instanceof PowerPylonBlockEntity pylon
                    && pylon.hasConnection(this.worldPosition)
                    && pylon.kind().connectionType() == this.kind().connectionType()) {
                links.add(connection.immutable());
            }
        }
        return links;
    }

    public List<BlockPos> adjacentPowerPorts() {
        List<BlockPos> ports = new ArrayList<>();
        PowerPylonBlock.Kind kind = kind();
        if (kind == PowerPylonBlock.Kind.RED_PYLON) {
            for (Direction direction : Direction.values()) {
                ports.add(this.worldPosition.relative(direction));
            }
        } else if (kind.hasTransformer()) {
            ports.add(this.worldPosition.relative(facing().getOpposite()));
        } else if (kind == PowerPylonBlock.Kind.RED_CONNECTOR || kind == PowerPylonBlock.Kind.CONNECTOR_RED_SUPER) {
            ports.add(this.worldPosition.relative(facing().getOpposite()));
        } else if (kind == PowerPylonBlock.Kind.SUBSTATION) {
            ports.addAll(substationAdjacentPorts());
        }
        return ports;
    }

    public AABB getRenderBoundingBox() {
        AABB box = new AABB(this.worldPosition).inflate(1.0D, 2.0D, 1.0D);
        for (Vec3 mount : mountPositions()) {
            box = box.minmax(new AABB(this.worldPosition.getX() + mount.x, this.worldPosition.getY() + mount.y, this.worldPosition.getZ() + mount.z,
                    this.worldPosition.getX() + mount.x, this.worldPosition.getY() + mount.y, this.worldPosition.getZ() + mount.z));
        }
        if (this.level != null) {
            for (BlockPos connection : this.connections) {
                if (this.level.getBlockEntity(connection) instanceof PowerPylonBlockEntity pylon) {
                    for (Vec3 mount : pylon.mountPositions()) {
                        box = box.minmax(new AABB(connection.getX() + mount.x, connection.getY() + mount.y - 2.5D, connection.getZ() + mount.z,
                                connection.getX() + mount.x, connection.getY() + mount.y, connection.getZ() + mount.z));
                    }
                } else {
                    box = box.minmax(new AABB(connection).inflate(1.0D));
                }
            }
        }
        return box;
    }

    private Direction facing() {
        BlockState state = this.getBlockState();
        if (state.hasProperty(PowerPylonBlock.FACING)) {
            return state.getValue(PowerPylonBlock.FACING);
        }
        return Direction.SOUTH;
    }

    private Vec3[] largePylonMounts(Direction facing) {
        double topOff = 0.75D + 0.0625D;
        double sideOff = 3.375D;
        Vec3 side = switch (facing) {
            case WEST -> rotateY(sideOff, 0.0D, Math.PI * 0.25D);
            case SOUTH -> rotateY(sideOff, 0.0D, Math.PI * 0.5D);
            case EAST -> rotateY(sideOff, 0.0D, Math.PI * 0.75D);
            default -> new Vec3(sideOff, 0.0D, 0.0D);
        };

        return new Vec3[]{
                new Vec3(0.5D + side.x, 11.5D + topOff, 0.5D + side.z),
                new Vec3(0.5D + side.x, 11.5D - topOff, 0.5D + side.z),
                new Vec3(0.5D - side.x, 11.5D + topOff, 0.5D - side.z),
                new Vec3(0.5D - side.x, 11.5D - topOff, 0.5D - side.z)
        };
    }

    private Vec3[] mediumPylonMounts(Direction facing) {
        double height = 7.5D;
        return new Vec3[]{
                new Vec3(0.5D, height, 0.5D),
                new Vec3(0.5D + facing.getStepX(), height, 0.5D + facing.getStepZ()),
                new Vec3(0.5D + facing.getStepX() * 2.0D, height, 0.5D + facing.getStepZ() * 2.0D)
        };
    }

    private Vec3[] substationMounts(Direction facing) {
        double topOff = 5.25D;
        Vec3 side = (facing == Direction.EAST || facing == Direction.WEST)
                ? new Vec3(0.0D, 0.0D, 1.0D)
                : new Vec3(1.0D, 0.0D, 0.0D);

        return new Vec3[]{
                new Vec3(0.5D + side.x * 0.5D, topOff, 0.5D + side.z * 0.5D),
                new Vec3(0.5D + side.x * 1.5D, topOff, 0.5D + side.z * 1.5D),
                new Vec3(0.5D - side.x * 0.5D, topOff, 0.5D - side.z * 0.5D),
                new Vec3(0.5D - side.x * 1.5D, topOff, 0.5D - side.z * 1.5D)
        };
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

    private Vec3 rotateY(double x, double z, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(x * cos + z * sin, 0.0D, z * cos - x * sin);
    }

    private void syncAndDirtyGraph() {
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
            list.add(entry);
        }
        tag.put("Connections", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.color = tag.getInt("Color");
        this.connections.clear();
        ListTag list = tag.getList("Connections", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            this.connections.add(new BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z")));
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
}
