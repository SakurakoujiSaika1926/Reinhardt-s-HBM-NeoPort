package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.StorageCrateBlock;
import com.reinhardt.hbm.menu.StorageCrateMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class StorageCrateBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer, MachineInventory, LockableBlockEntity {
    public static final String ITEM_DATA_KEY = "crate_data";

    private final ItemStack[] items;
    private final Kind kind;
    private final int[] slotsForFace;
    private int heatTimer;
    private long joules;
    private int lock;
    private boolean locked;
    private double lockMod = 0.1D;
    private boolean cheesable = true;

    public StorageCrateBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.STORAGE_CRATE.get(), pos, blockState);
        this.kind = blockState.getBlock() instanceof StorageCrateBlock crate ? crate.kind() : Kind.IRON;
        this.items = new ItemStack[this.kind.slots()];
        Arrays.fill(this.items, ItemStack.EMPTY);
        this.slotsForFace = new int[this.items.length];
        for (int i = 0; i < this.slotsForFace.length; i++) {
            this.slotsForFace[i] = i;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StorageCrateBlockEntity crate) {
        if (crate.heatTimer > 0) {
            crate.heatTimer--;
            if (crate.heatTimer == 0) {
                crate.sync();
            }
        }
    }

    public Kind kind() {
        return this.kind;
    }

    public int heatTimer() {
        return this.heatTimer;
    }

    public long joules() {
        return this.joules;
    }

    @Override
    public int getContainerSize() {
        return this.items.length;
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
        return isValidSlot(slot) ? this.items[slot] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            this.items[slot] = ItemStack.EMPTY;
        }
        setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items[slot];
        this.items[slot] = ItemStack.EMPTY;
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.items[slot] = stack;
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isValidSlot(slot);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return this.slotsForFace;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isValidSlot(slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        Arrays.fill(this.items, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.kind.containerKey());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StorageCrateMenu(containerId, playerInventory, this);
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    @Override
    public void lock() {
        this.locked = true;
        lockChanged();
    }

    @Override
    public void unlock() {
        this.locked = false;
        lockChanged();
    }

    @Override
    public int pins() {
        return this.lock;
    }

    @Override
    public void setPins(int pins) {
        this.lock = pins;
        lockChanged();
    }

    @Override
    public double lockMod() {
        return this.lockMod;
    }

    @Override
    public void setLockMod(double mod) {
        this.lockMod = mod;
        lockChanged();
    }

    @Override
    public boolean cheesable() {
        return this.cheesable;
    }

    @Override
    public void setCheesable(boolean cheesable) {
        this.cheesable = cheesable;
        lockChanged();
    }

    @Override
    public void lockChanged() {
        sync();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < this.items.length; i++) {
            drop(level, pos, this.items[i]);
            this.items[i] = ItemStack.EMPTY;
        }
    }

    public void loadFromItem(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root.contains(ITEM_DATA_KEY)) {
            readItemData(root.getCompound(ITEM_DATA_KEY), registries);
            sync();
        }
    }

    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag data = writeItemData(registries);
        if (data.isEmpty()) {
            return;
        }
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        root.put(ITEM_DATA_KEY, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    public static int usedSlots(ItemStack stack, int maxSlots) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(ITEM_DATA_KEY)) {
            return 0;
        }
        CompoundTag data = root.getCompound(ITEM_DATA_KEY);
        int used = 0;
        for (int i = 0; i < maxSlots; i++) {
            if (data.contains("slot" + i)) {
                used++;
            }
        }
        return used;
    }

    public static void copyData(ItemStack source, ItemStack result) {
        CustomData data = source.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            result.set(DataComponents.CUSTOM_DATA, data);
        }
    }

    private CompoundTag writeItemData(HolderLookup.Provider registries) {
        CompoundTag data = new CompoundTag();
        for (int i = 0; i < this.items.length; i++) {
            if (!this.items[i].isEmpty()) {
                data.put("slot" + i, this.items[i].saveOptional(registries));
            }
        }
        if (this.lock != 0 || this.locked || this.lockMod != 0.1D || !this.cheesable) {
            data.putInt("lock", this.lock);
            data.putBoolean("isLocked", this.locked);
            data.putDouble("lockMod", this.lockMod);
            data.putBoolean("cheesable", this.cheesable);
        }
        return data;
    }

    private void readItemData(CompoundTag data, HolderLookup.Provider registries) {
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = ItemStack.parseOptional(registries, data.getCompound("slot" + i));
        }
        this.lock = data.getInt("lock");
        this.locked = data.getBoolean("isLocked");
        this.lockMod = data.contains("lockMod") ? data.getDouble("lockMod") : 0.1D;
        this.cheesable = !data.contains("cheesable") || data.getBoolean("cheesable");
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < this.items.length;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", writeItemData(registries));
        tag.putInt("HeatTimer", this.heatTimer);
        tag.putLong("Joules", this.joules);
        tag.putInt("lock", this.lock);
        tag.putBoolean("isLocked", this.locked);
        tag.putDouble("lockMod", this.lockMod);
        tag.putBoolean("cheesable", this.cheesable);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readItemData(tag.getCompound("Items"), registries);
        this.heatTimer = tag.getInt("HeatTimer");
        this.joules = tag.getLong("Joules");
        this.lock = tag.getInt("lock");
        this.locked = tag.getBoolean("isLocked");
        this.lockMod = tag.getDouble("lockMod");
        this.cheesable = tag.getBoolean("cheesable");
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
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public enum Kind {
        IRON("crate_iron", 36, 9, 4, 8, 18, 8, 104, 162, 176, 186, 8, 0x404040, 0x404040, "gui_crate_iron.png"),
        STEEL("crate_steel", 54, 9, 6, 8, 18, 8, 140, 198, 176, 222, 8, 0x1C1C1C, 0x1C1C1C, "gui_crate_steel.png"),
        DESH("crate_desh", 104, 13, 8, 8, 18, 44, 174, 232, 248, 256, 44, 0x3F1515, 0x3F1515, "gui_crate_desh.png"),
        TEMPLATE("crate_template", 27, 9, 3, 8, 18, 8, 86, 144, 176, 168, 8, 0x404040, 0x404040, "gui_crate_template.png"),
        TUNGSTEN("crate_tungsten", 27, 9, 3, 8, 18, 8, 86, 144, 176, 168, 8, 0xA0A0A0, 0xA0A0A0, "gui_crate_tungsten.png");

        private final String id;
        private final int slots;
        private final int columns;
        private final int rows;
        private final int crateX;
        private final int crateY;
        private final int playerInventoryX;
        private final int playerInventoryY;
        private final int hotbarY;
        private final int guiWidth;
        private final int guiHeight;
        private final int inventoryLabelX;
        private final int titleColor;
        private final int inventoryLabelColor;
        private final ResourceLocation texture;
        private final ResourceLocation hotTexture;

        Kind(String id, int slots, int columns, int rows, int crateX, int crateY, int playerInventoryX, int playerInventoryY,
             int hotbarY, int guiWidth, int guiHeight, int inventoryLabelX, int titleColor, int inventoryLabelColor, String texture) {
            this.id = id;
            this.slots = slots;
            this.columns = columns;
            this.rows = rows;
            this.crateX = crateX;
            this.crateY = crateY;
            this.playerInventoryX = playerInventoryX;
            this.playerInventoryY = playerInventoryY;
            this.hotbarY = hotbarY;
            this.guiWidth = guiWidth;
            this.guiHeight = guiHeight;
            this.inventoryLabelX = inventoryLabelX;
            this.titleColor = titleColor;
            this.inventoryLabelColor = inventoryLabelColor;
            this.texture = ReinhardtsHBM.id("textures/gui/storage/" + texture);
            this.hotTexture = "crate_tungsten".equals(id) ? ReinhardtsHBM.id("textures/gui/storage/gui_crate_tungsten_hot.png") : this.texture;
        }

        public String id() {
            return id;
        }

        public int slots() {
            return slots;
        }

        public int columns() {
            return columns;
        }

        public int rows() {
            return rows;
        }

        public int crateX() {
            return crateX;
        }

        public int crateY() {
            return crateY;
        }

        public int playerInventoryX() {
            return playerInventoryX;
        }

        public int playerInventoryY() {
            return playerInventoryY;
        }

        public int hotbarY() {
            return hotbarY;
        }

        public int guiWidth() {
            return guiWidth;
        }

        public int guiHeight() {
            return guiHeight;
        }

        public int inventoryLabelX() {
            return inventoryLabelX;
        }

        public int titleColor(boolean hot) {
            return hot && this == TUNGSTEN ? 0xFFCA53 : titleColor;
        }

        public int inventoryLabelColor(boolean hot) {
            return hot && this == TUNGSTEN ? 0xFFCA53 : inventoryLabelColor;
        }

        public ResourceLocation texture(boolean hot) {
            return hot ? hotTexture : texture;
        }

        public String containerKey() {
            return "container.reinhardtshbm." + this.id;
        }
    }
}
