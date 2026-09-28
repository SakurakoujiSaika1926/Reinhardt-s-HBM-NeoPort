package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PileMk2Block;
import com.reinhardt.hbm.block.PileMk2BrickBlock;
import com.reinhardt.hbm.entity.PileDebrisEntity;
import com.reinhardt.hbm.item.PileRodMk2Item;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Exact current-upstream Chicago Pile MK2 channel simulation, stored only on the core block. */
public final class PileMk2CoreBlockEntity extends BlockEntity {
    public static final int MAX_HEAT = 800;
    private static final ThreadLocal<Boolean> RESTORING_STRUCTURE = ThreadLocal.withInitial(() -> false);

    private Orientation orientation = Orientation.NEITHER;
    private Direction assemblyDirection = Direction.NORTH;
    private int height;
    private int width;
    private int depth;
    private int left;
    private int right;
    private int up;
    private BlockPos min = BlockPos.ZERO;
    private BlockPos max = BlockPos.ZERO;
    private boolean initialized;

    private final List<PileChannel> fuelChannels = new ArrayList<>();
    private final List<PileChannel> ventilationChannels = new ArrayList<>();
    private final List<PileChannel> controlChannels = new ArrayList<>();
    private PileSegment[] segments = new PileSegment[0];
    private double highestHeat;

    public PileMk2CoreBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PILE_MK2_CORE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PileMk2CoreBlockEntity core) {
        if (level.isClientSide || !core.initialized) {
            return;
        }
        core.runSimulation();
        core.handleVentilation();
        core.handleMeltdown();
        core.setChanged();
        if (level.getGameTime() % 25L == 0L) {
            core.sync();
        }
    }

    public static boolean isRestoringStructure() {
        return RESTORING_STRUCTURE.get();
    }

    public void setupStructure(Direction inward, int up, int down, int left, int right, int depth,
                               BlockPos min, BlockPos max) {
        if (!inward.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Chicago Pile assembly direction must be horizontal: " + inward);
        }
        this.assemblyDirection = inward;
        this.orientation = Orientation.fromDirection(inward);
        this.height = up + 1 + down;
        this.width = left + 1 + right;
        this.depth = depth;
        this.left = left;
        this.right = right;
        this.up = up;
        this.min = min.immutable();
        this.max = max.immutable();
        this.initialized = true;
        this.segments = new PileSegment[this.width];
        setChanged();
        sync();
    }

    public boolean contains(BlockPos pos) {
        return initialized
                && pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public double highestHeat() {
        return highestHeat;
    }

    @Nullable
    public PileChannel getFuelChannel(BlockPos entry) {
        return getChannel(entry, fuelChannels);
    }

    @Nullable
    public PileChannel getVentilationChannel(BlockPos entry) {
        return getChannel(entry, ventilationChannels);
    }

    @Nullable
    public PileChannel getControlChannel(BlockPos entry) {
        return getChannel(entry, controlChannels);
    }

    public int getFuelChannelNumber(PileChannel channel) {
        return fuelChannels.indexOf(channel);
    }

    public int getVentilationChannelNumber(PileChannel channel) {
        return ventilationChannels.indexOf(channel);
    }

    public int getControlChannelNumber(PileChannel channel) {
        return controlChannels.indexOf(channel);
    }

    @Nullable
    private static PileChannel getChannel(BlockPos entry, List<PileChannel> channels) {
        for (PileChannel channel : channels) {
            if (channel.entry.equals(entry)) return channel;
        }
        return null;
    }

    public boolean drillChannel(BlockPos start, Direction inward, Player player) {
        if (level == null || level.isClientSide || !initialized) {
            return false;
        }
        ChannelType type = ChannelType.fromDirection(inward, orientation);
        int size = switch (type) {
            case CONTROL -> height;
            case FUEL -> depth;
            case VENTILATION -> width;
        };
        List<PileChannel> channels = channels(type);
        PileMk2Block.Kind startKind = kindAt(start);

        if (startKind == PileMk2Block.Kind.FUEL_IN
                || startKind == PileMk2Block.Kind.AIR_IN
                || startKind == PileMk2Block.Kind.CONTROL) {
            for (int index = 0; index < channels.size(); index++) {
                PileChannel channel = channels.get(index);
                if (channel.entry.equals(start) && channel.direction == inward) {
                    if (channel.type == ChannelType.FUEL) channel.ejectAll();
                    channels.remove(index);
                    for (int step = 0; step < size; step++) {
                        level.setBlock(start.relative(inward, step), defaultPart(PileMk2Block.Kind.DUMMY),
                                Block.UPDATE_ALL);
                    }
                    level.playSound(null, start, SoundEvents.NOTE_BLOCK_PLING.value(),
                            SoundSource.BLOCKS, 1.0F, 0.75F);
                    recalculateSegments();
                    setChanged();
                    sync();
                    return true;
                }
            }
        }

        boolean error = false;
        for (int step = 0; step < size; step++) {
            BlockPos check = start.relative(inward, step);
            BlockState state = level.getBlockState(check);
            if (!(state.getBlock() instanceof PileMk2Block)) {
                error(player, "message.reinhardtshbm.pile_mk2.foreign", check);
                error = true;
                continue;
            }
            PileMk2Block.Kind kind = state.getValue(PileMk2Block.KIND);
            if (kind == PileMk2Block.Kind.EDGE) {
                error(player, "message.reinhardtshbm.pile_mk2.intersect_edge", check);
                error = true;
            } else if (kind == PileMk2Block.Kind.CORE) {
                error(player, "message.reinhardtshbm.pile_mk2.intersect_core_at", check);
                error = true;
            } else if (kind == PileMk2Block.Kind.CHANNEL) {
                error(player, "message.reinhardtshbm.pile_mk2.intersect_channel", check);
                error = true;
            } else if (kind != PileMk2Block.Kind.DUMMY) {
                error(player, "message.reinhardtshbm.pile_mk2.intersect_io", check);
                error = true;
            }
        }
        if (error) {
            return false;
        }

        for (int step = 0; step < size; step++) {
            PileMk2Block.Kind kind;
            if (step == 0) {
                kind = switch (type) {
                    case FUEL -> PileMk2Block.Kind.FUEL_IN;
                    case VENTILATION -> PileMk2Block.Kind.AIR_IN;
                    case CONTROL -> PileMk2Block.Kind.CONTROL;
                };
            } else if (step == size - 1) {
                kind = switch (type) {
                    case FUEL -> PileMk2Block.Kind.FUEL_OUT;
                    case VENTILATION -> PileMk2Block.Kind.AIR_OUT;
                    case CONTROL -> PileMk2Block.Kind.CONTROL;
                };
            } else {
                kind = PileMk2Block.Kind.CHANNEL;
            }
            level.setBlock(start.relative(inward, step), defaultPart(kind), Block.UPDATE_ALL);
        }

        channels.add(new PileChannel(start, inward, size, type));
        level.playSound(null, start, SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.BLOCKS, 1.0F, 1.25F);
        recalculateSegments();
        setChanged();
        sync();
        return true;
    }

    private List<PileChannel> channels(ChannelType type) {
        return switch (type) {
            case FUEL -> fuelChannels;
            case VENTILATION -> ventilationChannels;
            case CONTROL -> controlChannels;
        };
    }

    private PileMk2Block.Kind kindAt(BlockPos pos) {
        BlockState state = level == null ? null : level.getBlockState(pos);
        return state != null && state.getBlock() instanceof PileMk2Block
                ? state.getValue(PileMk2Block.KIND) : PileMk2Block.Kind.DUMMY;
    }

    private static BlockState defaultPart(PileMk2Block.Kind kind) {
        return HbmBlocks.PILE_BLOCK.get().defaultBlockState().setValue(PileMk2Block.KIND, kind);
    }

    private static void error(Player player, String key, BlockPos pos) {
        player.displayClientMessage(Component.translatable(key, pos.getX(), pos.getY(), pos.getZ()), true);
    }

    private void runSimulation() {
        for (PileChannel channel : fuelChannels) {
            if (channel.length <= 0) continue;
            double producedNeutrons = 0.0D;
            for (int slot = 0; slot < channel.rods.length; slot++) {
                ItemStack stack = channel.rods[slot];
                if (!PileRodMk2Item.isPileRod(stack)) continue;
                double neutrons = PileRodMk2Item.reactivity(stack,
                        channel.incomingNeutrons / channel.length);
                producedNeutrons += neutrons;
                channel.heat += neutrons * PileRodMk2Item.heatPerNeutron(stack);
                channel.rods[slot] = PileRodMk2Item.react(stack, neutrons);
            }
            channel.outgoingNeutrons = producedNeutrons;
            channel.incomingNeutrons = 0.0D;
        }

        for (PileSegment segment : segments) {
            if (segment == null || segment.type != ChannelType.FUEL) continue;
            double outgoing = 0.0D;
            for (PileChannel channel : segment.channels) outgoing += channel.outgoingNeutrons;
            for (PileChannel channel : segment.channels) channel.incomingNeutrons += outgoing;
        }

        for (int index = 1; index < segments.length - 1; index++) {
            PileSegment segment = segments[index];
            if (segment == null || segment.type != ChannelType.FUEL) continue;
            double outgoing = 0.0D;
            for (PileChannel channel : segment.channels) outgoing += channel.outgoingNeutrons;

            double multiplier = 1.0D;
            for (int leftIndex = index - 1; leftIndex >= 1; leftIndex--) {
                PileSegment neighbor = segments[leftIndex];
                if (neighbor == null) continue;
                multiplier *= neighbor.neutronMultiplier(this);
                if (neighbor.type == ChannelType.FUEL) {
                    for (PileChannel channel : neighbor.channels) {
                        channel.incomingNeutrons += outgoing * multiplier;
                    }
                }
            }

            multiplier = 1.0D;
            for (int rightIndex = index + 1; rightIndex < segments.length - 1; rightIndex++) {
                PileSegment neighbor = segments[rightIndex];
                if (neighbor == null) continue;
                multiplier *= neighbor.neutronMultiplier(this);
                if (neighbor.type == ChannelType.FUEL) {
                    for (PileChannel channel : neighbor.channels) {
                        channel.incomingNeutrons += outgoing * multiplier;
                    }
                }
            }
        }
    }

    private void handleVentilation() {
        if (level == null) return;
        for (PileChannel channel : ventilationChannels) {
            if (channel.air <= 0) continue;
            double airCapacity = (double) channel.air / PileChannel.MAX_AIR;
            for (PileChannel fuel : fuelChannels) {
                if (Math.abs(fuel.entry.getY() - channel.entry.getY()) <= 1) {
                    fuel.heat *= 1.0D - airCapacity * 0.05D;
                }
            }
            channel.air -= (int) Math.ceil(airCapacity * 5.0D);

            if (level.getGameTime() % 3L == 0L && level instanceof ServerLevel serverLevel) {
                double x = channel.entry.getX() + 0.5D
                        + channel.direction.getStepX() * (width - 0.375D);
                double y = channel.entry.getY() + 0.5D;
                double z = channel.entry.getZ() + 0.5D
                        + channel.direction.getStepZ() * (width - 0.375D);
                serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z,
                        1, 0.08D * airCapacity, 0.05D, 0.08D * airCapacity, 0.0025D);
            }
        }

        for (PileChannel channel : fuelChannels) {
            channel.heat *= 0.999D;
            if (channel.heat < 20.0D) channel.heat = 20.0D;
        }
    }

    private void handleMeltdown() {
        if (level == null) return;
        highestHeat = 0.0D;
        for (PileChannel channel : fuelChannels) {
            if (channel.heat > highestHeat) highestHeat = channel.heat;
        }
        if (highestHeat <= MAX_HEAT || fuelChannels.isEmpty()) {
            return;
        }

        double averageX = 0.0D;
        double averageZ = 0.0D;
        for (PileChannel channel : fuelChannels) {
            averageX += channel.entry.getX() + 0.5D
                    + channel.direction.getStepX() * (channel.length - 1) / 2.0D;
            averageZ += channel.entry.getZ() + 0.5D
                    + channel.direction.getStepZ() * (channel.length - 1) / 2.0D;
        }
        averageX /= fuelChannels.size();
        averageZ /= fuelChannels.size();
        double explosionY = worldPosition.getY() + up;

        dismantle(false);
        level.explode(null, averageX, explosionY, averageZ, 15.0F, true,
                Level.ExplosionInteraction.BLOCK);
        PileDebrisEntity.spawnMeltdownFragments(level,
                new net.minecraft.world.phys.Vec3(averageX, explosionY + 1.0D, averageZ));
    }

    private void recalculateSegments() {
        segments = new PileSegment[Math.max(0, width)];
        for (PileChannel channel : fuelChannels) addToSegment(channel, ChannelType.FUEL);
        for (PileChannel channel : controlChannels) addToSegment(channel, ChannelType.CONTROL);
    }

    private void addToSegment(PileChannel channel, ChannelType expectedType) {
        int index = getChannelVerticalIndex(channel);
        if (index < 0 || index >= segments.length) return;
        if (segments[index] == null) {
            segments[index] = new PileSegment(expectedType);
        }
        if (segments[index].type == expectedType) {
            segments[index].channels.add(channel);
        }
    }

    private int getChannelVerticalIndex(PileChannel channel) {
        Direction channelRight = channel.direction.getClockWise();
        int deltaX = (channel.entry.getX() - worldPosition.getX()) * channelRight.getStepX();
        int deltaZ = (channel.entry.getZ() - worldPosition.getZ()) * channelRight.getStepZ();
        int signedDistance = deltaX == 0 ? deltaZ : deltaX;
        return signedDistance + left;
    }

    public void dismantle(boolean ejectFuel) {
        if (level == null || level.isClientSide || !initialized || isRestoringStructure()) {
            return;
        }
        if (ejectFuel) {
            for (PileChannel channel : fuelChannels) channel.ejectAll();
        }
        initialized = false;
        RESTORING_STRUCTURE.set(true);
        try {
            for (BlockPos restore : BlockPos.betweenClosed(min, max)) {
                level.setBlock(restore, HbmBlocks.PILE_BRICK.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        } finally {
            RESTORING_STRUCTURE.set(false);
        }
        fuelChannels.clear();
        ventilationChannels.clear();
        controlChannels.clear();
        segments = new PileSegment[0];
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Initialized", initialized);
        tag.putString("Orientation", orientation.name());
        tag.putString("AssemblyDirection", assemblyDirection.getName());
        tag.putInt("Height", height);
        tag.putInt("Width", width);
        tag.putInt("Depth", depth);
        tag.putInt("Left", left);
        tag.putInt("Right", right);
        tag.putInt("Up", up);
        tag.putLong("Min", min.asLong());
        tag.putLong("Max", max.asLong());
        tag.putDouble("HighestHeat", highestHeat);
        tag.put("FuelChannels", saveChannels(fuelChannels, registries));
        tag.put("VentilationChannels", saveChannels(ventilationChannels, registries));
        tag.put("ControlChannels", saveChannels(controlChannels, registries));
    }

    private static ListTag saveChannels(List<PileChannel> channels, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (PileChannel channel : channels) list.add(channel.save(registries));
        return list;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        initialized = tag.getBoolean("Initialized");
        try {
            orientation = Orientation.valueOf(tag.getString("Orientation"));
        } catch (IllegalArgumentException ignored) {
            orientation = Orientation.NEITHER;
        }
        Direction loadedDirection = Direction.byName(tag.getString("AssemblyDirection"));
        assemblyDirection = loadedDirection != null && loadedDirection.getAxis().isHorizontal()
                ? loadedDirection : Direction.NORTH;
        height = tag.getInt("Height");
        width = tag.getInt("Width");
        depth = tag.getInt("Depth");
        left = tag.getInt("Left");
        right = tag.getInt("Right");
        up = tag.getInt("Up");
        min = tag.contains("Min") ? BlockPos.of(tag.getLong("Min")) : worldPosition;
        max = tag.contains("Max") ? BlockPos.of(tag.getLong("Max")) : worldPosition;
        highestHeat = tag.getDouble("HighestHeat");
        fuelChannels.clear();
        ventilationChannels.clear();
        controlChannels.clear();
        loadChannels(tag.getList("FuelChannels", Tag.TAG_COMPOUND), fuelChannels, registries);
        loadChannels(tag.getList("VentilationChannels", Tag.TAG_COMPOUND), ventilationChannels, registries);
        loadChannels(tag.getList("ControlChannels", Tag.TAG_COMPOUND), controlChannels, registries);
        recalculateSegments();
    }

    private void loadChannels(ListTag list, List<PileChannel> output, HolderLookup.Provider registries) {
        for (int index = 0; index < list.size(); index++) {
            PileChannel channel = loadChannel(list.getCompound(index), registries);
            if (channel != null) output.add(channel);
        }
    }

    @Nullable
    private PileChannel loadChannel(CompoundTag tag, HolderLookup.Provider registries) {
        Direction direction = Direction.byName(tag.getString("Direction"));
        ChannelType type;
        try {
            type = ChannelType.valueOf(tag.getString("Type"));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        int maximumLength = Math.max(PileMk2BrickBlock.MAX_HORIZONTAL_SIZE,
                PileMk2BrickBlock.MAX_VERTICAL_SIZE);
        int length = Mth.clamp(tag.getInt("Length"), 1, maximumLength);
        if (direction == null) return null;
        PileChannel channel = new PileChannel(BlockPos.of(tag.getLong("Entry")), direction, length, type);
        channel.heat = tag.getDouble("Heat");
        channel.outgoingNeutrons = tag.getDouble("Outgoing");
        channel.incomingNeutrons = tag.getDouble("Incoming");
        channel.air = Mth.clamp(tag.getInt("Air"), 0, PileChannel.MAX_AIR);
        channel.control = Mth.clamp(tag.getDouble("Control"), 0.0D, 1.0D);
        ListTag rods = tag.getList("Rods", Tag.TAG_COMPOUND);
        for (int index = 0; index < rods.size(); index++) {
            CompoundTag itemTag = rods.getCompound(index);
            int slot = itemTag.getInt("Slot");
            if (slot >= 0 && slot < channel.rods.length) {
                channel.rods[slot] = ItemStack.parseOptional(registries,
                        itemTag.getCompound("Stack"));
            }
        }
        return channel;
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

    private void sync() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public enum Orientation {
        NORTH_SOUTH,
        EAST_WEST,
        NEITHER;

        static Orientation fromDirection(Direction direction) {
            if (direction == Direction.NORTH || direction == Direction.SOUTH) return NORTH_SOUTH;
            if (direction == Direction.EAST || direction == Direction.WEST) return EAST_WEST;
            return NEITHER;
        }
    }

    public enum ChannelType {
        FUEL,
        VENTILATION,
        CONTROL;

        static ChannelType fromDirection(Direction direction, Orientation orientation) {
            if (direction.getAxis().isVertical()) return CONTROL;
            return Orientation.fromDirection(direction) == orientation ? FUEL : VENTILATION;
        }
    }

    public final class PileChannel {
        public static final int MAX_AIR = 1_000;

        private final BlockPos entry;
        private final Direction direction;
        private final int length;
        private final ChannelType type;
        private final ItemStack[] rods;
        private double heat;
        private double outgoingNeutrons;
        private double incomingNeutrons;
        private int air;
        private double control = 1.0D;

        private PileChannel(BlockPos entry, Direction direction, int length, ChannelType type) {
            this.entry = entry.immutable();
            this.direction = direction;
            this.length = length;
            this.type = type;
            this.rods = new ItemStack[length];
            java.util.Arrays.fill(this.rods, ItemStack.EMPTY);
        }

        public ItemStack lastRod() {
            return rods.length == 0 ? ItemStack.EMPTY : rods[rods.length - 1];
        }

        public double heat() {
            return heat;
        }

        public int air() {
            return air;
        }

        public void addAir(int amount) {
            air = Mth.clamp(air + Math.max(0, amount), 0, MAX_AIR);
            PileMk2CoreBlockEntity.this.setChanged();
        }

        public double control() {
            return control;
        }

        public void setControl(double value) {
            control = Mth.clamp(value, 0.0D, 1.0D);
            PileMk2CoreBlockEntity.this.setChanged();
        }

        public void loadItem(ItemStack incoming) {
            if (incoming.isEmpty()) return;
            ItemStack moving = incoming;
            if (rods.length <= 0) {
                dropItem(moving, -1);
                return;
            }
            for (int slot = 0; slot < rods.length; slot++) {
                if (rods[slot].isEmpty()) {
                    rods[slot] = moving;
                    PileMk2CoreBlockEntity.this.setChanged();
                    return;
                }
                ItemStack previous = rods[slot];
                rods[slot] = moving;
                moving = previous;
            }
            dropItem(moving, length);
            PileMk2CoreBlockEntity.this.setChanged();
        }

        private void ejectAll() {
            for (int slot = 0; slot < rods.length; slot++) {
                dropItem(rods[slot], length);
                rods[slot] = ItemStack.EMPTY;
            }
        }

        private void dropItem(ItemStack stack, int channelDepth) {
            if (stack.isEmpty() || level == null) return;
            ItemStack dropped = stack.copy();
            PileRodMk2Item.clearDepletion(dropped);
            BlockPos dropPos = entry.relative(direction, channelDepth);
            level.addFreshEntity(new ItemEntity(level,
                    dropPos.getX() + 0.5D, dropPos.getY() + 0.5D, dropPos.getZ() + 0.5D, dropped));
        }

        private CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Entry", entry.asLong());
            tag.putString("Direction", direction.getName());
            tag.putInt("Length", length);
            tag.putString("Type", type.name());
            tag.putDouble("Heat", heat);
            tag.putDouble("Outgoing", outgoingNeutrons);
            tag.putDouble("Incoming", incomingNeutrons);
            tag.putInt("Air", air);
            tag.putDouble("Control", control);
            ListTag itemList = new ListTag();
            for (int slot = 0; slot < rods.length; slot++) {
                if (rods[slot].isEmpty()) continue;
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", slot);
                itemTag.put("Stack", rods[slot].saveOptional(registries));
                itemList.add(itemTag);
            }
            tag.put("Rods", itemList);
            return tag;
        }

    }

    private static final class PileSegment {
        private final List<PileChannel> channels = new ArrayList<>();
        private final ChannelType type;

        private PileSegment(ChannelType type) {
            this.type = type;
        }

        private double neutronMultiplier(PileMk2CoreBlockEntity core) {
            if (type != ChannelType.CONTROL) return 1.0D;
            int size = core.depth - 1;
            if (size < 3) return 0.0D;
            double total = 0.0D;
            for (PileChannel channel : channels) total += channel.control;
            return Mth.clamp(total / size, 0.0D, 0.5D);
        }
    }
}
