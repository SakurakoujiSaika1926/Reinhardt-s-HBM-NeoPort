package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FoundryOutletBlock;
import com.reinhardt.hbm.foundry.CrucibleAcceptor;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FoundryFlowBlockEntity extends BlockEntity implements CrucibleAcceptor, MachineInventory {
    private static final int CHANNEL_CAPACITY = FoundryShape.INGOT.q(2);
    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    @Nullable
    private FoundryMaterial type;
    private int amount;
    private int nextUpdate = 5;
    private Direction lastFlow;
    @Nullable
    private FoundryMaterial filter;
    private boolean invertFilter;
    private boolean invertRedstone;

    public FoundryFlowBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.FOUNDRY_FLOW.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FoundryFlowBlockEntity flow) {
        if (level.isClientSide || !flow.isChannel()) {
            return;
        }
        flow.tickChannel(level);
    }

    @Nullable
    public FoundryMaterial material() {
        return this.type;
    }

    public int amount() {
        return this.amount;
    }

    public int capacity() {
        return isChannel() ? CHANNEL_CAPACITY : 0;
    }

    public boolean isRedstoneInverted() {
        return this.invertRedstone;
    }

    public void toggleRedstoneInversion() {
        this.invertRedstone = !this.invertRedstone;
        setChangedAndSync();
    }

    public boolean setFilterFromScraps(ItemStack stack) {
        FoundryMaterialStack contents = ScrapsItem.contents(stack);
        if (contents == null) {
            return false;
        }
        this.filter = contents.material();
        setChangedAndSync();
        return true;
    }

    public void clearFilter() {
        this.filter = null;
        this.invertFilter = false;
        setChangedAndSync();
    }

    public boolean scrape(Player player) {
        if (this.type == null || this.amount <= 0) {
            return false;
        }
        giveOrDrop(player, ScrapsItem.create(new FoundryMaterialStack(this.type, this.amount), false));
        this.type = null;
        this.amount = 0;
        this.lastFlow = null;
        setChangedAndSync();
        return true;
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, double x, double y, double z, Direction side, FoundryMaterialStack stack) {
        if (!isChannel() || side != Direction.UP || !isCastable(stack)) {
            return false;
        }
        return connectedChannelAccepts(level, stack.material()) && standardCheck(stack);
    }

    @Override
    public FoundryMaterialStack pour(Level level, BlockPos pos, double x, double y, double z, Direction side, FoundryMaterialStack stack) {
        if (!canAcceptPartialPour(level, pos, x, y, z, side, stack)) {
            return stack;
        }
        return standardAdd(stack);
    }

    @Override
    public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, FoundryMaterialStack stack) {
        if (isChannel()) {
            return isCastable(stack) && connectedChannelAccepts(level, stack.material()) && standardCheck(stack);
        }
        return outletCanFlow(level, side, stack);
    }

    @Override
    public FoundryMaterialStack flow(Level level, BlockPos pos, Direction side, FoundryMaterialStack stack) {
        if (isChannel()) {
            if (!canAcceptPartialFlow(level, pos, side, stack)) {
                return stack;
            }
            this.lastFlow = side;
            return standardAdd(stack);
        }
        if (!outletCanFlow(level, side, stack)) {
            return stack;
        }
        return isSlagTap() ? flowSlag(level, stack) : flowOutlet(level, stack);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        if (this.type != null && this.amount > 0) {
            net.minecraft.world.Containers.dropItemStack(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    ScrapsItem.create(new FoundryMaterialStack(this.type, this.amount), false)
            );
        }
        this.type = null;
        this.amount = 0;
    }

    private void tickChannel(Level level) {
        normalize();
        this.nextUpdate--;
        if (this.nextUpdate > 0 || this.type == null || this.amount <= 0) {
            if (this.amount <= 0) {
                this.nextUpdate = 5;
                this.lastFlow = null;
            }
            return;
        }
        this.nextUpdate = 5;

        List<Direction> directions = shuffledDirections(level);
        if (tryFlowToAcceptors(level, directions)) {
            return;
        }
        equalizeWithChannels(level, directions);
    }

    private boolean tryFlowToAcceptors(Level level, List<Direction> directions) {
        for (Direction direction : directions) {
            BlockPos targetPos = this.worldPosition.relative(direction);
            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (!(blockEntity instanceof CrucibleAcceptor acceptor) || blockEntity instanceof FoundryFlowBlockEntity flow && flow.isChannel()) {
                continue;
            }
            FoundryMaterialStack stack = new FoundryMaterialStack(this.type, this.amount);
            Direction side = direction.getOpposite();
            if (!acceptor.canAcceptPartialFlow(level, targetPos, side, stack)) {
                continue;
            }
            FoundryMaterialStack left = acceptor.flow(level, targetPos, side, stack);
            int leftover = left == null ? 0 : left.amount();
            int transferred = this.amount - leftover;
            if (transferred <= 0) {
                continue;
            }
            this.amount = leftover;
            if (this.amount <= 0) {
                this.amount = 0;
                this.type = null;
            }
            setChangedAndSync();
            return true;
        }
        return false;
    }

    private void equalizeWithChannels(Level level, List<Direction> directions) {
        for (Direction direction : directions) {
            BlockEntity blockEntity = level.getBlockEntity(this.worldPosition.relative(direction));
            if (!(blockEntity instanceof FoundryFlowBlockEntity channel) || !channel.isChannel()) {
                continue;
            }
            if (channel.type != null && channel.type != this.type && channel.amount > 0) {
                continue;
            }
            channel.type = this.type;
            channel.lastFlow = direction.getOpposite();
            if (level.random.nextInt(5) == 0 || this.amount == 1) {
                int swap = this.amount;
                this.amount = channel.amount;
                channel.amount = swap;
            } else {
                int diff = this.amount - channel.amount;
                if (diff > 0) {
                    diff /= 2;
                    this.amount -= diff;
                    channel.amount += diff;
                }
            }
            normalize();
            channel.normalize();
            setChangedAndSync();
            channel.setChangedAndSync();
        }
    }

    private boolean outletCanFlow(Level level, Direction side, FoundryMaterialStack stack) {
        if (!isCastable(stack) || this.filter != null && (this.invertFilter ? this.filter == stack.material() : this.filter != stack.material())) {
            return false;
        }
        if (isClosed(level)) {
            return false;
        }
        Direction facing = getBlockState().hasProperty(FoundryOutletBlock.FACING)
                ? getBlockState().getValue(FoundryOutletBlock.FACING)
                : Direction.NORTH;
        if (side != facing.getOpposite()) {
            return false;
        }
        if (isSlagTap()) {
            BlockPos target = slagTarget(level);
            return target != null && canFlowSlagAt(level, target, stack.material());
        }
        PourTarget target = findPourTarget(level, 4);
        return target != null && target.acceptor().canAcceptPartialPour(level, target.pos(), target.pos().getX() + 0.5D, target.pos().getY(), target.pos().getZ() + 0.5D, Direction.UP, stack);
    }

    private FoundryMaterialStack flowOutlet(Level level, FoundryMaterialStack stack) {
        PourTarget target = findPourTarget(level, 4);
        if (target == null) {
            return stack;
        }
        FoundryMaterialStack left = target.acceptor().pour(level, target.pos(), target.pos().getX() + 0.5D, target.pos().getY(), target.pos().getZ() + 0.5D, Direction.UP, stack);
        setChangedAndSync();
        return left;
    }

    private FoundryMaterialStack flowSlag(Level level, FoundryMaterialStack stack) {
        BlockPos target = slagTarget(level);
        if (target == null) {
            return stack;
        }
        int leftover = flowIntoSlag(level, target, stack.material(), stack.amount());
        setChangedAndSync();
        return leftover <= 0 ? null : new FoundryMaterialStack(stack.material(), leftover);
    }

    private int flowIntoSlag(Level level, BlockPos target, FoundryMaterial material, int amount) {
        int remaining = amount;
        if (level.getBlockState(target).is(HbmBlocks.SLAG.get())
                && level.getBlockEntity(target) instanceof FoundrySlagBlockEntity slag
                && slag.canAccept(material)) {
            int accepted = slag.add(material, remaining);
            remaining -= accepted;
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.scheduleTick(target, HbmBlocks.SLAG.get(), 1);
            }
        } else if (level.getBlockState(target).canBeReplaced()) {
            level.setBlock(target, HbmBlocks.SLAG.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(target) instanceof FoundrySlagBlockEntity slag) {
                int accepted = slag.add(material, remaining);
                remaining -= accepted;
            }
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.scheduleTick(target, HbmBlocks.SLAG.get(), 1);
            }
        }

        BlockPos above = target.above();
        if (remaining > 0 && level.getBlockState(above).canBeReplaced()) {
            level.setBlock(above, HbmBlocks.SLAG.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(above) instanceof FoundrySlagBlockEntity slag) {
                int accepted = slag.add(material, remaining);
                remaining -= accepted;
            }
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.scheduleTick(above, HbmBlocks.SLAG.get(), 1);
            }
        }
        return remaining;
    }

    @Nullable
    private PourTarget findPourTarget(Level level, int range) {
        for (int offset = 1; offset <= range; offset++) {
            BlockPos targetPos = this.worldPosition.below(offset);
            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (blockEntity instanceof CrucibleAcceptor acceptor) {
                return new PourTarget(targetPos, acceptor);
            }
            if (!level.getBlockState(targetPos).isAir()) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    private BlockPos slagTarget(Level level) {
        for (int offset = 1; offset <= 15; offset++) {
            BlockPos target = this.worldPosition.below(offset);
            BlockState state = level.getBlockState(target);
            if (state.is(HbmBlocks.SLAG.get())) {
                return target;
            }
            if (state.isAir() || state.canBeReplaced()) {
                continue;
            }
            BlockPos above = target.above();
            return level.getBlockState(above).canBeReplaced() ? above : null;
        }
        return null;
    }

    private boolean canFlowSlagAt(Level level, BlockPos target, FoundryMaterial material) {
        BlockState state = level.getBlockState(target);
        if (state.is(HbmBlocks.SLAG.get()) && level.getBlockEntity(target) instanceof FoundrySlagBlockEntity slag) {
            if (slag.canAccept(material) && slag.remainingCapacity() > 0) {
                return true;
            }
            return level.getBlockState(target.above()).canBeReplaced();
        }
        return state.canBeReplaced();
    }

    private boolean connectedChannelAccepts(Level level, FoundryMaterial material) {
        List<BlockPos> open = new ArrayList<>();
        List<BlockPos> visited = new ArrayList<>();
        open.add(this.worldPosition);
        while (!open.isEmpty() && visited.size() < 128) {
            BlockPos current = open.remove(open.size() - 1);
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);
            BlockEntity blockEntity = level.getBlockEntity(current);
            if (blockEntity instanceof FoundryFlowBlockEntity channel && channel.isChannel()) {
                if (channel.type != null && channel.amount > 0 && channel.type != material) {
                    return false;
                }
                for (Direction direction : HORIZONTALS) {
                    BlockPos next = current.relative(direction);
                    if (!visited.contains(next) && level.getBlockState(next).is(HbmBlocks.FOUNDRY_CHANNEL.get())) {
                        open.add(next);
                    }
                }
            }
        }
        return true;
    }

    private FoundryMaterialStack standardAdd(FoundryMaterialStack stack) {
        this.type = stack.material();
        int accepted = Math.min(stack.amount(), CHANNEL_CAPACITY - this.amount);
        this.amount += accepted;
        setChangedAndSync();
        int leftover = stack.amount() - accepted;
        return leftover <= 0 ? null : new FoundryMaterialStack(stack.material(), leftover);
    }

    private boolean standardCheck(FoundryMaterialStack stack) {
        if (stack == null || stack.amount() <= 0) {
            return false;
        }
        if (this.type != null && this.amount > 0 && this.type != stack.material()) {
            return false;
        }
        return this.amount < CHANNEL_CAPACITY;
    }

    private boolean isClosed(Level level) {
        boolean powered = level.hasNeighborSignal(this.worldPosition);
        return this.invertRedstone ^ powered;
    }

    private boolean isChannel() {
        return getBlockState().is(HbmBlocks.FOUNDRY_CHANNEL.get());
    }

    private boolean isSlagTap() {
        return getBlockState().getBlock() instanceof FoundryOutletBlock outlet && outlet.kind() == FoundryOutletBlock.Kind.SLAGTAP;
    }

    private void normalize() {
        if (this.amount <= 0) {
            this.amount = 0;
            this.type = null;
        }
        if (this.amount > CHANNEL_CAPACITY && isChannel()) {
            this.amount = CHANNEL_CAPACITY;
        }
    }

    private List<Direction> shuffledDirections(Level level) {
        List<Direction> directions = new ArrayList<>(List.of(HORIZONTALS));
        for (int index = directions.size() - 1; index > 0; index--) {
            Collections.swap(directions, index, level.random.nextInt(index + 1));
        }
        if (this.lastFlow != null && directions.remove(this.lastFlow)) {
            directions.add(this.lastFlow);
        }
        return directions;
    }

    private static boolean isCastable(FoundryMaterialStack stack) {
        return stack != null && stack.amount() > 0 && stack.material().behavior() == FoundryMaterial.SmeltingBehavior.SMELTABLE;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.type != null) {
            tag.putString("Type", this.type.name());
        }
        if (this.filter != null) {
            tag.putString("Filter", this.filter.name());
        }
        tag.putInt("Amount", this.amount);
        tag.putInt("NextUpdate", this.nextUpdate);
        tag.putBoolean("InvertFilter", this.invertFilter);
        tag.putBoolean("InvertRedstone", this.invertRedstone);
        if (this.lastFlow != null) {
            tag.putString("LastFlow", this.lastFlow.getName());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.type = FoundryMaterial.byName(tag.getString("Type")).orElse(null);
        this.filter = FoundryMaterial.byName(tag.getString("Filter")).orElse(null);
        this.amount = tag.getInt("Amount");
        this.nextUpdate = tag.contains("NextUpdate") ? tag.getInt("NextUpdate") : 5;
        this.invertFilter = tag.getBoolean("InvertFilter");
        this.invertRedstone = tag.getBoolean("InvertRedstone");
        this.lastFlow = tag.contains("LastFlow") ? Direction.byName(tag.getString("LastFlow")) : null;
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

    private record PourTarget(BlockPos pos, CrucibleAcceptor acceptor) {
    }
}
