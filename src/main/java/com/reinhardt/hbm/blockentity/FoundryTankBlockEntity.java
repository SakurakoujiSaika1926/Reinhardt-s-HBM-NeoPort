package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.foundry.CrucibleAcceptor;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FoundryTankBlockEntity extends BlockEntity implements CrucibleAcceptor, MachineInventory {
    public static final int CAPACITY = FoundryShape.BLOCK.q(4);
    private static final Direction[] HORIZONTALS = {
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    @Nullable
    private FoundryMaterial type;
    private int amount;
    private int nextUpdate;

    public FoundryTankBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.FOUNDRY_TANK.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FoundryTankBlockEntity tank) {
        if (level.isClientSide) {
            return;
        }
        tank.normalize();
        tank.nextUpdate--;
        if (tank.nextUpdate > 0 || tank.type == null || tank.amount <= 0) {
            return;
        }

        tank.nextUpdate = level.random.nextInt(6) + 5;
        if (tank.moveDown(level)) {
            return;
        }

        List<Direction> directions = tank.shuffledDirections(level);
        if (tank.flowToAcceptor(level, directions)) {
            return;
        }
        tank.equalize(level, directions);
    }

    @Nullable
    public FoundryMaterial material() {
        return this.type;
    }

    public int amount() {
        return this.amount;
    }

    public int capacity() {
        return CAPACITY;
    }

    public boolean scrape(Player player) {
        if (this.type == null || this.amount <= 0) {
            return false;
        }
        giveOrDrop(player, ScrapsItem.create(new FoundryMaterialStack(this.type, this.amount), false));
        this.type = null;
        this.amount = 0;
        setChangedAndSync();
        return true;
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, double x, double y, double z,
                                        Direction side, FoundryMaterialStack stack) {
        return side == Direction.UP && standardCheck(stack);
    }

    @Override
    public FoundryMaterialStack pour(Level level, BlockPos pos, double x, double y, double z,
                                     Direction side, FoundryMaterialStack stack) {
        if (!canAcceptPartialPour(level, pos, x, y, z, side, stack)) {
            return stack;
        }
        return standardAdd(stack);
    }

    @Override
    public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side,
                                        FoundryMaterialStack stack) {
        return false;
    }

    @Override
    public FoundryMaterialStack flow(Level level, BlockPos pos, Direction side,
                                     FoundryMaterialStack stack) {
        return stack;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        if (this.type != null && this.amount > 0) {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D,
                    pos.getZ() + 0.5D,
                    ScrapsItem.create(new FoundryMaterialStack(this.type, this.amount), false));
        }
        this.type = null;
        this.amount = 0;
    }

    private boolean moveDown(Level level) {
        BlockEntity below = level.getBlockEntity(this.worldPosition.below());
        if (!(below instanceof FoundryTankBlockEntity tank)
                || tank.type != null && tank.amount > 0 && tank.type != this.type
                || tank.amount >= CAPACITY) {
            return false;
        }
        tank.type = this.type;
        int transferred = Math.min(this.amount, CAPACITY - tank.amount);
        this.amount -= transferred;
        tank.amount += transferred;
        normalize();
        tank.normalize();
        setChangedAndSync();
        tank.setChangedAndSync();
        return transferred > 0;
    }

    private boolean flowToAcceptor(Level level, List<Direction> directions) {
        for (Direction direction : directions) {
            BlockPos targetPos = this.worldPosition.relative(direction);
            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (!(blockEntity instanceof CrucibleAcceptor acceptor)
                    || blockEntity instanceof FoundryTankBlockEntity
                    || blockEntity instanceof FoundryFlowBlockEntity flow && flow.capacity() > 0) {
                continue;
            }
            FoundryMaterialStack offered = new FoundryMaterialStack(this.type, this.amount);
            Direction side = direction.getOpposite();
            if (!acceptor.canAcceptPartialFlow(level, targetPos, side, offered)) {
                continue;
            }
            FoundryMaterialStack left = acceptor.flow(level, targetPos, side, offered);
            int remaining = left == null ? 0 : left.amount();
            if (remaining >= this.amount) {
                continue;
            }
            this.amount = remaining;
            normalize();
            setChangedAndSync();
            return true;
        }
        return false;
    }

    private void equalize(Level level, List<Direction> directions) {
        for (Direction direction : directions) {
            if (!(level.getBlockEntity(this.worldPosition.relative(direction))
                    instanceof FoundryTankBlockEntity tank)) {
                continue;
            }
            if (tank.type != null && tank.amount > 0 && tank.type != this.type) {
                continue;
            }
            tank.type = this.type;
            if (level.random.nextInt(5) == 0) {
                int swap = this.amount;
                this.amount = tank.amount;
                tank.amount = swap;
            } else {
                int difference = this.amount - tank.amount;
                if (difference > 0) {
                    int transferred = difference / 2;
                    this.amount -= transferred;
                    tank.amount += transferred;
                }
            }
            normalize();
            tank.normalize();
            setChangedAndSync();
            tank.setChangedAndSync();
        }
    }

    private FoundryMaterialStack standardAdd(FoundryMaterialStack stack) {
        this.type = stack.material();
        int accepted = Math.min(stack.amount(), CAPACITY - this.amount);
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
        return this.amount < CAPACITY;
    }

    private void normalize() {
        if (this.amount <= 0) {
            this.amount = 0;
            this.type = null;
        } else if (this.amount > CAPACITY) {
            this.amount = CAPACITY;
        }
    }

    private List<Direction> shuffledDirections(Level level) {
        List<Direction> directions = new ArrayList<>(List.of(HORIZONTALS));
        for (int index = directions.size() - 1; index > 0; index--) {
            Collections.swap(directions, index, level.random.nextInt(index + 1));
        }
        return directions;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
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
        tag.putInt("Amount", this.amount);
        tag.putInt("NextUpdate", this.nextUpdate);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.type = FoundryMaterial.byName(tag.getString("Type")).orElse(null);
        this.amount = tag.getInt("Amount");
        this.nextUpdate = tag.getInt("NextUpdate");
        normalize();
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
