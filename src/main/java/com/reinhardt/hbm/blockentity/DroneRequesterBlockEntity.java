package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.drone.DroneItemMatcher;
import com.reinhardt.hbm.drone.DroneRequestNetwork;
import com.reinhardt.hbm.menu.DroneRequesterMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Direct TileEntityDroneRequester port: nine filter patterns followed by nine stock slots. */
public final class DroneRequesterBlockEntity extends DroneNetworkContainerBlockEntity implements MenuProvider {
    public static final int FILTER_START = 0;
    public static final int STOCK_START = 9;
    private static final int[] STOCK_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private final String[] modes = new String[9];

    public DroneRequesterBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DRONE_REQUESTER.get(), pos, state, 18);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DroneRequesterBlockEntity requester) {
        if (!level.isClientSide) {
            requester.tickNetwork(level);
        }
    }

    @Override
    public DroneRequestNetwork.NodeKind droneNodeKind() {
        return DroneRequestNetwork.NodeKind.REQUESTER;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return STOCK_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= FILTER_START && slot < STOCK_START;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= STOCK_START && slot < STOCK_START + 9;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= STOCK_START && slot < STOCK_START + 9;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.drone_requester");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DroneRequesterMenu(id, inventory, this);
    }

    public void setFilter(int slot, ItemStack pattern) {
        if (slot < 0 || slot >= 9) {
            return;
        }
        items.set(slot, pattern.copy());
        modes[slot] = pattern.isEmpty() ? null : DroneItemMatcher.defaultMode(pattern);
        sync();
    }

    public void cycleFilterMode(int slot) {
        if (slot < 0 || slot >= 9 || items.get(slot).isEmpty()) {
            return;
        }
        ItemStack pattern = items.get(slot);
        String mode = modes[slot];
        if (mode == null || DroneItemMatcher.EXACT.equals(mode)) {
            modes[slot] = DroneItemMatcher.WILDCARD;
        } else if (DroneItemMatcher.WILDCARD.equals(mode)) {
            List<ResourceLocation> tags = pattern.getTags().map(TagKey::location).toList();
            modes[slot] = tags.isEmpty() ? DroneItemMatcher.EXACT : tags.getFirst().toString();
        } else {
            List<ResourceLocation> tags = pattern.getTags().map(TagKey::location).toList();
            int index = tags.indexOf(ResourceLocation.tryParse(mode));
            modes[slot] = index < 0 || index + 1 >= tags.size() ? DroneItemMatcher.EXACT : tags.get(index + 1).toString();
        }
        sync();
    }

    public String filterMode(int slot) {
        return slot >= 0 && slot < modes.length ? modes[slot] : null;
    }

    public List<DroneItemMatcher> outstandingRequests() {
        List<DroneItemMatcher> requests = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack pattern = items.get(slot);
            if (pattern.isEmpty()) {
                continue;
            }
            ItemStack stock = items.get(slot + STOCK_START);
            if (stock.isEmpty() || !new DroneItemMatcher(pattern, modes[slot]).matches(stock)) {
                requests.add(new DroneItemMatcher(pattern, modes[slot]));
            }
        }
        return requests;
    }

    public ItemStack insertRequested(ItemStack incoming) {
        ItemStack remaining = incoming.copy();
        for (int slot = STOCK_START; slot < STOCK_START + 9 && !remaining.isEmpty(); slot++) {
            ItemStack existing = items.get(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                int moved = Math.min(existing.getMaxStackSize() - existing.getCount(), remaining.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    remaining.shrink(moved);
                }
            }
        }
        for (int slot = STOCK_START; slot < STOCK_START + 9 && !remaining.isEmpty(); slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, remaining.copy());
                remaining = ItemStack.EMPTY;
            }
        }
        if (remaining.getCount() != incoming.getCount()) {
            sync();
        }
        return remaining;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < modes.length; slot++) {
            if (modes[slot] != null) {
                tag.putString("mode" + slot, modes[slot]);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < modes.length; slot++) {
            modes[slot] = tag.contains("mode" + slot) ? tag.getString("mode" + slot) : null;
        }
    }
}
