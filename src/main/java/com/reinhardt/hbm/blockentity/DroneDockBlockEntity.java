package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.drone.DroneItemMatcher;
import com.reinhardt.hbm.drone.DroneRequestNetwork;
import com.reinhardt.hbm.entity.LegacyRequestDroneEntity;
import com.reinhardt.hbm.item.LegacyDroneItem;
import com.reinhardt.hbm.menu.DroneGridMenu;
import com.reinhardt.hbm.registry.HbmMenus;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Old TileEntityDroneDock request dispatcher, preserving its five-attempt, depth-ten search. */
public final class DroneDockBlockEntity extends DroneNetworkContainerBlockEntity implements MenuProvider {
    public DroneDockBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DRONE_DOCK.get(), pos, state, 9);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DroneDockBlockEntity dock) {
        if (!level.isClientSide) {
            dock.tickNetwork(level);
            if (level.getGameTime() % 20L == 0L && dock.hasRequestDrone()) {
                dock.dispatch(level);
            }
        }
    }

    @Override
    public DroneRequestNetwork.NodeKind droneNodeKind() {
        return DroneRequestNetwork.NodeKind.DOCK;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < 9;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.drone_dock");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DroneGridMenu(id, inventory, this, HbmMenus.DRONE_DOCK.get());
    }

    private boolean hasRequestDrone() {
        for (ItemStack stack : items) {
            if (LegacyDroneItem.Type.fromStack(stack) == LegacyDroneItem.Type.REQUEST) {
                return true;
            }
        }
        return false;
    }

    private void dispatch(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        List<DroneRequestNetwork.Node> nodes = DroneRequestNetwork.localNodes(serverLevel, worldPosition, 5);
        DroneRequestNetwork.Node dockNode = DroneRequestNetwork.node(serverLevel, droneNodePosition());
        if (dockNode == null) {
            return;
        }

        List<DroneRequestNetwork.Node> requesters = nodes.stream()
                .filter(node -> node.kind() == DroneRequestNetwork.NodeKind.REQUESTER && node.active())
                .toList();
        List<DroneRequestNetwork.Node> providers = nodes.stream()
                .filter(node -> node.kind() == DroneRequestNetwork.NodeKind.PROVIDER && node.active())
                .toList();

        for (int attempt = 0; attempt < 5; attempt++) {
            List<DroneRequestNetwork.Node> shuffledRequesters = new ArrayList<>(requesters);
            List<DroneRequestNetwork.Node> shuffledProviders = new ArrayList<>(providers);
            shuffle(shuffledRequesters, level.random);
            shuffle(shuffledProviders, level.random);
            for (DroneRequestNetwork.Node requesterNode : shuffledRequesters) {
                if (!(level.getBlockEntity(requesterNode.pos().below()) instanceof DroneRequesterBlockEntity requester)) {
                    continue;
                }
                List<DroneItemMatcher> outstanding = requester.outstandingRequests();
                if (outstanding.isEmpty()) {
                    continue;
                }
                DroneItemMatcher requested = outstanding.get(level.random.nextInt(outstanding.size()));
                for (DroneRequestNetwork.Node providerNode : shuffledProviders) {
                    if (!(level.getBlockEntity(providerNode.pos().below()) instanceof DroneProviderBlockEntity provider)
                            || !provider.hasMatching(requested)) {
                        continue;
                    }
                    if (embark(serverLevel, dockNode, providerNode, requesterNode, requested)) {
                        return;
                    }
                }
            }
        }
    }

    private boolean embark(net.minecraft.server.level.ServerLevel level, DroneRequestNetwork.Node dock,
                           DroneRequestNetwork.Node provider, DroneRequestNetwork.Node requester, DroneItemMatcher matcher) {
        List<DroneRequestNetwork.Node> toProvider = DroneRequestNetwork.path(level, dock, provider);
        List<DroneRequestNetwork.Node> toRequester = DroneRequestNetwork.path(level, provider, requester);
        List<DroneRequestNetwork.Node> toDock = DroneRequestNetwork.path(level, requester, dock);
        if (toProvider == null || toRequester == null || toDock == null) {
            return false;
        }

        int slot = requestDroneSlot();
        if (slot < 0) {
            return false;
        }
        removeItem(slot, 1);
        LegacyRequestDroneEntity drone = HbmEntityTypes.REQUEST_DRONE.get().create(level);
        if (drone == null) {
            setItem(slot, LegacyDroneItem.stack(LegacyDroneItem.Type.REQUEST, 1));
            return false;
        }
        drone.moveTo(worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D, 0.0F, 0.0F);
        for (DroneRequestNetwork.Node node : toProvider) {
            drone.addFlyStep(node.pos());
        }
        drone.addLoadStep(provider.pos(), matcher);
        for (DroneRequestNetwork.Node node : toRequester) {
            drone.addFlyStep(node.pos());
        }
        drone.addUnloadStep(requester.pos());
        for (DroneRequestNetwork.Node node : toDock) {
            drone.addFlyStep(node.pos());
        }
        drone.addDockStep(dock.pos());
        level.addFreshEntity(drone);
        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 2.0F, 1.0F);
        return true;
    }

    private int requestDroneSlot() {
        for (int slot = 0; slot < items.size(); slot++) {
            if (LegacyDroneItem.Type.fromStack(items.get(slot)) == LegacyDroneItem.Type.REQUEST) {
                return slot;
            }
        }
        return -1;
    }

    private static <T> void shuffle(List<T> values, net.minecraft.util.RandomSource random) {
        for (int index = values.size() - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            Collections.swap(values, index, swap);
        }
    }
}
