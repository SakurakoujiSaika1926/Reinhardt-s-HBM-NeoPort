package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.drone.DroneRequestNetwork;
import com.reinhardt.hbm.menu.DroneGridMenu;
import com.reinhardt.hbm.registry.HbmMenus;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Direct TileEntityDroneProvider port: a nine-slot offer-only logistics inventory. */
public final class DroneProviderBlockEntity extends DroneNetworkContainerBlockEntity implements MenuProvider {
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    public DroneProviderBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DRONE_PROVIDER.get(), pos, state, 9);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DroneProviderBlockEntity provider) {
        if (!level.isClientSide) {
            provider.tickNetwork(level);
        }
    }

    @Override
    public DroneRequestNetwork.NodeKind droneNodeKind() {
        return DroneRequestNetwork.NodeKind.PROVIDER;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < 9;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.drone_provider");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DroneGridMenu(id, inventory, this, HbmMenus.DRONE_PROVIDER.get());
    }

    @Nullable
    public ItemStack takeMatching(com.reinhardt.hbm.drone.DroneItemMatcher matcher) {
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (matcher.matches(stack)) {
                items.set(slot, ItemStack.EMPTY);
                sync();
                return stack;
            }
        }
        return null;
    }

    public boolean hasMatching(com.reinhardt.hbm.drone.DroneItemMatcher matcher) {
        for (ItemStack stack : items) {
            if (matcher.matches(stack)) {
                return true;
            }
        }
        return false;
    }
}
