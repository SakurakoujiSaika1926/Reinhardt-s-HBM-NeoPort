package com.reinhardt.hbm.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps a large-machine menu anchored to the structure part that the player
 * actually used instead of always measuring the distance to the controller.
 *
 * <p>The entry is tied to the exact server menu id and is accepted only while
 * the clicked proxy still points at the same access controller and the opened
 * container is still the same block entity.  This prevents the fix from
 * extending the interaction range of unrelated or subsequently opened menus.</p>
 */
public final class MachineDummyMenuAccess {
    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 64.0D;
    private static final Map<ServerPlayer, Access> ACTIVE = new WeakHashMap<>();

    private MachineDummyMenuAccess() {
    }

    public static void record(ServerPlayer player, BlockPos accessPos, BlockPos corePos) {
        record(player, accessPos, corePos, corePos);
    }

    /**
     * @param accessCorePos controller to which the clicked proxy belongs
     * @param menuCorePos   block entity backing the menu (normally identical;
     *                      radar screens deliberately open their linked radar)
     */
    public static void record(
            ServerPlayer player,
            BlockPos accessPos,
            BlockPos accessCorePos,
            BlockPos menuCorePos
    ) {
        BlockEntity accessCore = player.level().getBlockEntity(accessCorePos);
        if (player.containerMenu == player.inventoryMenu || accessCore == null) {
            ACTIVE.remove(player);
            return;
        }
        ACTIVE.put(player, new Access(
                player.containerMenu.containerId,
                new WeakReference<>(player.containerMenu),
                accessPos.immutable(),
                accessCorePos.immutable(),
                new WeakReference<>(accessCore),
                menuCorePos.immutable()
        ));
    }

    /** Standard block-entity validity plus the exact clicked-proxy path. */
    public static boolean stillValid(BlockEntity container, Player player) {
        return validDummyAccess(container, player) || Container.stillValidBlockEntity(container, player);
    }

    /** Adds dummy access without changing a machine's existing custom range. */
    public static boolean validDummyAccess(BlockEntity container, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        Access access = ACTIVE.get(serverPlayer);
        if (access == null) {
            return false;
        }
        if (serverPlayer.containerMenu.containerId != access.menuId()
                || access.menu().get() != serverPlayer.containerMenu) {
            ACTIVE.remove(serverPlayer);
            return false;
        }

        Level level = container.getLevel();
        if (level == null
                || level != serverPlayer.level()
                || !container.getBlockPos().equals(access.menuCorePos())
                || level.getBlockEntity(access.menuCorePos()) != container
                || serverPlayer.distanceToSqr(
                        access.accessPos().getX() + 0.5D,
                        access.accessPos().getY() + 0.5D,
                        access.accessPos().getZ() + 0.5D
                ) > MAX_INTERACTION_DISTANCE_SQUARED
                || !validAccessPart(level, access)) {
            ACTIVE.remove(serverPlayer);
            return false;
        }
        return true;
    }

    private static boolean validAccessPart(Level level, Access access) {
        BlockEntity accessEntity = level.getBlockEntity(access.accessPos());
        BlockEntity accessCore = access.accessCore().get();
        if (accessCore == null || level.getBlockEntity(access.accessCorePos()) != accessCore) {
            return false;
        }
        if (access.accessPos().equals(access.accessCorePos())) {
            return accessEntity == accessCore;
        }
        if (accessEntity instanceof MachineDummyBlockEntity dummy) {
            return dummy.getCorePos().equals(access.accessCorePos())
                    && dummy.core() == accessCore;
        }
        if (accessEntity instanceof SatelliteDockDummyBlockEntity dummy) {
            return dummy.corePos().equals(access.accessCorePos())
                    && accessCore instanceof SatelliteDockBlockEntity;
        }
        return false;
    }

    private record Access(
            int menuId,
            WeakReference<AbstractContainerMenu> menu,
            BlockPos accessPos,
            BlockPos accessCorePos,
            WeakReference<BlockEntity> accessCore,
            BlockPos menuCorePos
    ) {
    }
}
