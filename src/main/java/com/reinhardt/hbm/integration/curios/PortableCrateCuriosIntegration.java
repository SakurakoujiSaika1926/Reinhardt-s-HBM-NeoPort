package com.reinhardt.hbm.integration.curios;

import com.reinhardt.hbm.blockentity.StorageCrateBlockEntity;
import com.reinhardt.hbm.client.curios.PortableCrateCuriosClientEvents;
import com.reinhardt.hbm.menu.PortableCrateMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Optional;
import java.util.UUID;

/** Optional Curios-backed portable storage. This class is loaded only when Curios is present. */
public final class PortableCrateCuriosIntegration {
    public static final String SLOT_ID = "hbm_crate";

    private PortableCrateCuriosIntegration() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PortableCrateCuriosIntegration::onItemPickupPre);
        NeoForge.EVENT_BUS.addListener(PortableCrateCuriosIntegration::onItemPickupPost);
        if (FMLEnvironment.dist.isClient()) {
            PortableCrateCuriosClientEvents.register();
        }
    }

    public static void open(ServerPlayer player) {
        EquippedCrate equipped = findEquipped(player).orElse(null);
        if (equipped == null) {
            return;
        }

        StorageCrateBlockEntity.Kind kind = PortableCrateStorage.kind(equipped.stack());
        SimpleMenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new PortableCrateMenu(containerId, inventory, kind,
                        new CuriosCrateContainer(player, equipped.handler(), kind)),
                equipped.stack().getHoverName()
        );
        player.openMenu(provider, buffer -> buffer.writeByte(kind.ordinal()));
    }

    /**
     * Maps the extra slot drawn beside the vanilla inventory to Curios' real slot.
     * The server owns both the carried stack and the Curios mutation so a stale
     * client cannot duplicate or overwrite a crate.
     */
    public static void clickMappedSlot(ServerPlayer player) {
        if (player.containerMenu != player.inventoryMenu) {
            return;
        }

        IDynamicStackHandler handler = findHandler(player).orElse(null);
        if (handler == null || handler.getSlots() <= 0) {
            return;
        }

        ItemStack equipped = handler.getStackInSlot(0);
        ItemStack carried = player.containerMenu.getCarried();
        if (equipped.isEmpty()) {
            if (!PortableCrateStorage.isCrate(carried)) {
                return;
            }
            handler.setStackInSlot(0, carried.copyWithCount(1));
            carried.shrink(1);
            player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        } else if (carried.isEmpty()) {
            handler.setStackInSlot(0, ItemStack.EMPTY);
            player.containerMenu.setCarried(equipped.copy());
        } else if (PortableCrateStorage.isCrate(carried) && carried.getCount() == 1) {
            handler.setStackInSlot(0, carried.copy());
            player.containerMenu.setCarried(equipped.copy());
        } else {
            return;
        }

        player.containerMenu.broadcastChanges();
    }

    private static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        ItemStack stack = entity.getItem();
        UUID target = entity.getTarget();
        if (stack.isEmpty() || PortableCrateStorage.isCrate(stack) || entity.hasPickUpDelay()
                || target != null && !target.equals(player.getUUID())) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (inventory.getFreeSlot() >= 0 || inventory.getSlotWithRemainingSpace(stack) >= 0) {
            return;
        }

        Item pickedItem = stack.getItem();
        int moved = insert(player, stack);
        if (moved <= 0) {
            return;
        }

        player.take(entity, moved);
        player.awardStat(Stats.ITEM_PICKED_UP.get(pickedItem), moved);
        if (stack.isEmpty()) {
            entity.discard();
        }
        event.setCanPickup(TriState.FALSE);
    }

    private static void onItemPickupPost(ItemEntityPickupEvent.Post event) {
        ItemStack remaining = event.getCurrentStack();
        if (!remaining.isEmpty() && !PortableCrateStorage.isCrate(remaining)) {
            insert(event.getPlayer(), remaining);
        }
    }

    private static int insert(Player player, ItemStack source) {
        if (player.containerMenu instanceof PortableCrateMenu menu) {
            int moved = menu.insertOverflow(source);
            if (moved > 0) {
                return moved;
            }
        }

        EquippedCrate equipped = findEquipped(player).orElse(null);
        if (equipped == null) {
            return 0;
        }
        StorageCrateBlockEntity.Kind kind = PortableCrateStorage.kind(equipped.stack());
        NonNullList<ItemStack> contents = PortableCrateStorage.load(equipped.stack(), player.registryAccess(), kind);
        int moved = PortableCrateStorage.insert(contents, source);
        if (moved > 0) {
            PortableCrateStorage.save(equipped.stack(), contents, player.registryAccess());
            equipped.handler().setStackInSlot(0, equipped.stack());
        }
        return moved;
    }

    private static Optional<EquippedCrate> findEquipped(Player player) {
        return findHandler(player)
                .map(handler -> new EquippedCrate(handler, handler.getStackInSlot(0)))
                .filter(equipped -> PortableCrateStorage.isCrate(equipped.stack()));
    }

    private static Optional<IDynamicStackHandler> findHandler(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inventory -> inventory.getStacksHandler(SLOT_ID))
                .map(stacks -> stacks.getStacks())
                .filter(handler -> handler.getSlots() > 0);
    }

    private record EquippedCrate(IDynamicStackHandler handler, ItemStack stack) {
    }

    private static final class CuriosCrateContainer implements PortableCrateMenu.OverflowContainer {
        private final Player player;
        private final IDynamicStackHandler handler;
        private final StorageCrateBlockEntity.Kind kind;
        private final NonNullList<ItemStack> contents;

        private CuriosCrateContainer(Player player, IDynamicStackHandler handler,
                                     StorageCrateBlockEntity.Kind kind) {
            this.player = player;
            this.handler = handler;
            this.kind = kind;
            this.contents = PortableCrateStorage.load(handler.getStackInSlot(0), player.registryAccess(), kind);
        }

        @Override
        public int getContainerSize() {
            return this.contents.size();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : this.contents) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return validSlot(slot) ? this.contents.get(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (!validSlot(slot) || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = this.contents.get(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = stack.split(amount);
            if (stack.isEmpty()) {
                this.contents.set(slot, ItemStack.EMPTY);
            }
            setChanged();
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (!validSlot(slot)) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = this.contents.get(slot);
            this.contents.set(slot, ItemStack.EMPTY);
            setChanged();
            return removed;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (!validSlot(slot) || PortableCrateStorage.isCrate(stack)) {
                return;
            }
            ItemStack stored = stack.isEmpty() ? ItemStack.EMPTY : stack;
            if (!stored.isEmpty() && stored.getCount() > getMaxStackSize(stored)) {
                stored.setCount(getMaxStackSize(stored));
            }
            this.contents.set(slot, stored);
            setChanged();
        }

        @Override
        public void setChanged() {
            ItemStack crate = this.handler.getStackInSlot(0);
            if (!PortableCrateStorage.isCrate(crate) || PortableCrateStorage.kind(crate) != this.kind) {
                return;
            }
            PortableCrateStorage.save(crate, this.contents, this.player.registryAccess());
            this.handler.setStackInSlot(0, crate);
        }

        @Override
        public boolean stillValid(Player player) {
            ItemStack crate = this.handler.getStackInSlot(0);
            return player == this.player && PortableCrateStorage.isCrate(crate)
                    && PortableCrateStorage.kind(crate) == this.kind;
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < this.contents.size(); slot++) {
                this.contents.set(slot, ItemStack.EMPTY);
            }
            setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return validSlot(slot) && !PortableCrateStorage.isCrate(stack);
        }

        @Override
        public int insert(ItemStack source) {
            int moved = PortableCrateStorage.insert(this.contents, source);
            if (moved > 0) {
                setChanged();
            }
            return moved;
        }

        private boolean validSlot(int slot) {
            return slot >= 0 && slot < this.contents.size();
        }
    }
}
