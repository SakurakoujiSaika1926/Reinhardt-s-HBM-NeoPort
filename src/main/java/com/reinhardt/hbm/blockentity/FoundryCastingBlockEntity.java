package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FoundryCastingBlock;
import com.reinhardt.hbm.foundry.CrucibleAcceptor;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.item.FoundryMoldItem;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class FoundryCastingBlockEntity extends BlockEntity implements CrucibleAcceptor, WorldlyContainer, MachineInventory {
    public static final int MOLD_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int[] SLOTS = {MOLD_SLOT, OUTPUT_SLOT};

    private ItemStack mold = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private FoundryMaterial type;
    private int amount;
    private int cooloff = 200;

    public FoundryCastingBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.FOUNDRY_CASTING.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FoundryCastingBlockEntity casting) {
        if (level.isClientSide) {
            return;
        }

        boolean changed = false;
        int capacity = casting.getCapacity();
        if (casting.amount > capacity) {
            casting.amount = capacity;
            changed = true;
        }
        if (casting.amount <= 0) {
            if (casting.type != null) {
                casting.type = null;
                changed = true;
            }
        }

        FoundryMoldItem.Mold installed = casting.getInstalledMold();
        if (installed != null && casting.type != null && casting.amount == capacity && casting.output.isEmpty()) {
            casting.cooloff--;
            changed = true;
            if (casting.cooloff <= 0) {
                ItemStack result = installed.outputFor(casting.type).orElse(ItemStack.EMPTY);
                if (!result.isEmpty()) {
                    casting.output = result.copy();
                    casting.amount = 0;
                    casting.type = null;
                }
                casting.cooloff = 200;
            }
        } else if (casting.cooloff != 200) {
            casting.cooloff = 200;
            changed = true;
        }

        if (changed) {
            casting.setChanged();
            casting.syncToClient();
        }
    }

    public int getMoldSize() {
        return this.getBlockState().getBlock() instanceof FoundryCastingBlock castingBlock ? castingBlock.kind().moldSize() : 0;
    }

    @Nullable
    public FoundryMoldItem.Mold getInstalledMold() {
        if (!(this.mold.getItem() instanceof FoundryMoldItem)) {
            return null;
        }
        FoundryMoldItem.Mold installed = FoundryMoldItem.mold(this.mold);
        return installed.size() == getMoldSize() ? installed : null;
    }

    public int getCapacity() {
        FoundryMoldItem.Mold installed = getInstalledMold();
        return installed == null ? 0 : installed.cost();
    }

    @Nullable
    public FoundryMaterial material() {
        return this.type;
    }

    public int amount() {
        return this.amount;
    }

    public int cooloff() {
        return this.cooloff;
    }

    public boolean isFull() {
        int capacity = getCapacity();
        return capacity > 0 && this.amount >= capacity;
    }

    public boolean installMold(ItemStack stack, Player player) {
        if (!(stack.getItem() instanceof FoundryMoldItem)) {
            return false;
        }
        FoundryMoldItem.Mold next = FoundryMoldItem.mold(stack);
        if (next.size() != getMoldSize()) {
            return false;
        }
        if (!this.mold.isEmpty()) {
            giveOrDrop(player, this.mold.copy());
        }
        this.mold = stack.copyWithCount(1);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        setChanged();
        syncToClient();
        return true;
    }

    public boolean extractOutput(Player player) {
        if (this.output.isEmpty()) {
            return false;
        }
        giveOrDrop(player, this.output.copy());
        this.output = ItemStack.EMPTY;
        setChanged();
        syncToClient();
        return true;
    }

    public boolean removeMold(Player player) {
        if (this.mold.isEmpty() || this.amount > 0) {
            return false;
        }
        giveOrDrop(player, this.mold.copy());
        this.mold = ItemStack.EMPTY;
        setChanged();
        syncToClient();
        return true;
    }

    public boolean scrape(Player player) {
        if (this.type == null || this.amount <= 0) {
            return false;
        }
        giveOrDrop(player, ScrapsItem.create(new FoundryMaterialStack(this.type, this.amount), false));
        this.type = null;
        this.amount = 0;
        setChanged();
        syncToClient();
        return true;
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, double x, double y, double z, Direction side, FoundryMaterialStack stack) {
        if (side != Direction.UP || stack.material().behavior() != FoundryMaterial.SmeltingBehavior.SMELTABLE) {
            return false;
        }
        return standardCheck(stack);
    }

    @Override
    public FoundryMaterialStack pour(Level level, BlockPos pos, double x, double y, double z, Direction side, FoundryMaterialStack stack) {
        if (!standardCheck(stack)) {
            return stack;
        }
        this.type = stack.material();
        int capacity = getCapacity();
        int accepted = Math.min(stack.amount(), capacity - this.amount);
        this.amount += accepted;
        setChanged();
        syncToClient();
        int leftover = stack.amount() - accepted;
        return leftover <= 0 ? null : new FoundryMaterialStack(stack.material(), leftover);
    }

    @Override
    public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, FoundryMaterialStack stack) {
        return getMoldSize() == 0 && standardCheck(stack);
    }

    @Override
    public FoundryMaterialStack flow(Level level, BlockPos pos, Direction side, FoundryMaterialStack stack) {
        return getMoldSize() == 0 ? pour(level, pos, 0.0D, 0.0D, 0.0D, side, stack) : stack;
    }

    private boolean standardCheck(FoundryMaterialStack stack) {
        if (this.type != null && this.amount > 0 && this.type != stack.material()) {
            return false;
        }
        if (!this.output.isEmpty()) {
            return false;
        }
        FoundryMoldItem.Mold installed = getInstalledMold();
        return installed != null && this.amount < getCapacity() && installed.outputFor(stack.material()).isPresent();
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return this.mold.isEmpty() && this.output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case MOLD_SLOT -> this.mold;
            case OUTPUT_SLOT -> this.output;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(count);
        if (stack.isEmpty()) {
            setItem(slot, ItemStack.EMPTY);
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        if (slot == MOLD_SLOT) {
            this.mold = ItemStack.EMPTY;
        } else if (slot == OUTPUT_SLOT) {
            this.output = ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == MOLD_SLOT) {
            this.mold = stack;
        } else if (slot == OUTPUT_SLOT) {
            this.output = stack;
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == MOLD_SLOT && stack.getItem() instanceof FoundryMoldItem
                && FoundryMoldItem.mold(stack).size() == getMoldSize();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.mold = ItemStack.EMPTY;
        this.output = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        drop(level, pos, this.mold);
        drop(level, pos, this.output);
        if (this.type != null && this.amount > 0) {
            drop(level, pos, ScrapsItem.create(new FoundryMaterialStack(this.type, this.amount), false));
        }
        this.mold = ItemStack.EMPTY;
        this.output = ItemStack.EMPTY;
        this.type = null;
        this.amount = 0;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            ContainersCompat.dropItemStack(level, pos, stack);
        }
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private void syncToClient() {
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Mold", this.mold.saveOptional(registries));
        tag.put("Output", this.output.saveOptional(registries));
        if (this.type != null) {
            tag.putString("Type", this.type.name());
        }
        tag.putInt("Amount", this.amount);
        tag.putInt("Cooloff", this.cooloff);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.mold = ItemStack.parseOptional(registries, tag.getCompound("Mold"));
        this.output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        this.type = FoundryMaterial.byName(tag.getString("Type")).orElse(null);
        this.amount = tag.getInt("Amount");
        this.cooloff = tag.contains("Cooloff") ? tag.getInt("Cooloff") : 200;
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

    private static final class ContainersCompat {
        private static void dropItemStack(Level level, BlockPos pos, ItemStack stack) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
        }
    }
}
