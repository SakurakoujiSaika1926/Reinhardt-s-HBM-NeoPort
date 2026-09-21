package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.menu.AshpitMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.MenuProvider;

public class AshpitBlockEntity extends BlockEntity implements WorldlyContainer, MachineInventory, MenuProvider {
    private static final int SLOT_COUNT = 5;
    private static final int[] SLOTS = {0, 1, 2, 3, 4};
    private static final int THRESHOLD_WOOD = 2000;
    private static final int THRESHOLD_COAL = 2000;
    private static final int THRESHOLD_MISC = 2000;
    private static final int THRESHOLD_FLY = 2000;
    private static final int THRESHOLD_SOOT = 8000;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int ashLevelWood;
    private int ashLevelCoal;
    private int ashLevelMisc;
    private int ashLevelFly;
    private int ashLevelSoot;
    private int playersUsing;
    private float doorAngle;
    private float previousDoorAngle;
    private boolean full;

    public AshpitBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.ASHPIT.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AshpitBlockEntity ashpit) {
        if (level.isClientSide) {
            ashpit.tickClientDoor();
        } else {
            ashpit.processAsh();
        }
    }

    public void addFlyAsh(long amount) {
        this.ashLevelFly = saturatingAdd(this.ashLevelFly, amount);
        setChanged();
    }

    public void addSoot(long amount) {
        this.ashLevelSoot = saturatingAdd(this.ashLevelSoot, amount);
        setChanged();
    }

    public void addWoodAsh(long amount) {
        this.ashLevelWood = saturatingAdd(this.ashLevelWood, amount);
        setChanged();
    }

    public void addCoalAsh(long amount) {
        this.ashLevelCoal = saturatingAdd(this.ashLevelCoal, amount);
        setChanged();
    }

    public void addMiscAsh(long amount) {
        this.ashLevelMisc = saturatingAdd(this.ashLevelMisc, amount);
        setChanged();
    }

    private void processAsh() {
        boolean wasFull = this.full;
        if (processAsh(this.ashLevelWood, "wood", THRESHOLD_WOOD)) this.ashLevelWood -= THRESHOLD_WOOD;
        if (processAsh(this.ashLevelCoal, "coal", THRESHOLD_COAL)) this.ashLevelCoal -= THRESHOLD_COAL;
        if (processAsh(this.ashLevelMisc, "misc", THRESHOLD_MISC)) this.ashLevelMisc -= THRESHOLD_MISC;
        if (processAsh(this.ashLevelFly, "fly", THRESHOLD_FLY)) this.ashLevelFly -= THRESHOLD_FLY;
        if (processAsh(this.ashLevelSoot, "soot", THRESHOLD_SOOT)) this.ashLevelSoot -= THRESHOLD_SOOT;
        this.full = !this.isEmpty();
        if (wasFull != this.full) {
            syncStatus();
        }
    }

    /** Direct port of TileEntityAshpit#processAsh, including its legacy wood-counter side effect. */
    private boolean processAsh(int level, String variant, int threshold) {
        if (level < threshold) {
            return false;
        }
        ItemStack produced = HbmItems.variantStack(HbmItems.POWDER_ASH_ITEMS, variant);
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack current = items.get(slot);
            if (current.isEmpty()) {
                items.set(slot, produced);
                // 1.7.10 writes this field directly even for coal/fly/soot.
                this.ashLevelWood -= threshold;
                setChanged();
                return true;
            }
            if (ItemStack.isSameItemSameComponents(current, produced) && current.getCount() < current.getMaxStackSize()) {
                current.grow(1);
                setChanged();
                return true;
            }
        }
        return false;
    }

    /** Direct client port of TileEntityAshpit's door animation. */
    private void tickClientDoor() {
        this.previousDoorAngle = this.doorAngle;
        float swingSpeed = this.doorAngle / 10.0F + 3.0F;
        if (this.playersUsing > 0) {
            this.doorAngle = Math.min(135.0F, this.doorAngle + swingSpeed);
        } else {
            this.doorAngle = Math.max(0.0F, this.doorAngle - swingSpeed);
        }
    }

    public float doorAngle(float partialTick) {
        return this.previousDoorAngle + (this.doorAngle - this.previousDoorAngle) * partialTick;
    }

    public boolean isFull() {
        return this.full;
    }

    @Override
    public void startOpen(Player player) {
        if (!player.level().isClientSide) {
            this.playersUsing++;
            syncStatus();
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!player.level().isClientSide && this.playersUsing > 0) {
            this.playersUsing--;
            syncStatus();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putInt("ashLevelWood", ashLevelWood);
        tag.putInt("ashLevelCoal", ashLevelCoal);
        tag.putInt("ashLevelMisc", ashLevelMisc);
        tag.putInt("ashLevelFly", ashLevelFly);
        tag.putInt("ashLevelSoot", ashLevelSoot);
        tag.putInt("playersUsing", this.playersUsing);
        tag.putBoolean("full", this.full);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.ashLevelWood = tag.getInt("ashLevelWood");
        this.ashLevelCoal = tag.getInt("ashLevelCoal");
        this.ashLevelMisc = tag.getInt("ashLevelMisc");
        this.ashLevelFly = tag.getInt("ashLevelFly");
        this.ashLevelSoot = tag.getInt("ashLevelSoot");
        this.playersUsing = Math.max(0, tag.getInt("playersUsing"));
        this.full = tag.getBoolean("full");
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.items.size()) {
            this.items.set(slot, stack);
            setChanged();
        }
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.reinhardtshbm.machine_ashpit");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AshpitMenu(containerId, playerInventory, this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncStatus() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static int saturatingAdd(int current, long amount) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) current + amount));
    }
}
