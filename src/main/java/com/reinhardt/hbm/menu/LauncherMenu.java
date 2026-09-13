package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LauncherBlockEntity;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.MissilePartItem;
import com.reinhardt.hbm.registry.HbmMenus;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The four distinct old containers retain their own slot and shift-click rules. */
public final class LauncherMenu extends AbstractContainerMenu {
    private final LauncherBlockEntity launcher;
    public LauncherMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, resolve(inventory, buffer.readBlockPos()));
    }
    private LauncherMenu(int id, Inventory inventory, LauncherBlockEntity launcher) {
        this(id, inventory, launcher, launcher.menuData());
    }
    public LauncherMenu(int id, Inventory inventory, LauncherBlockEntity launcher, ContainerData data) {
        super(HbmMenus.LAUNCHER.get(), id);
        this.launcher = launcher;
        int[][] positions = switch (launcher.kind()) {
            case COMPACT, TABLE -> new int[][]{{26,36},{26,72},{116,72},{134,72},{152,90},{116,108},{116,90},{134,90}};
            case PAD_RUSTED -> new int[][]{{26,72},{116,45},{134,45},{26,99}};
            case PAD_SMALL, PAD_LARGE -> new int[][]{{26,36},{26,72},{107,90},{125,90},{125,108},{143,90},{143,108}};
            default -> throw new IllegalArgumentException("Not an ordinary launcher: " + launcher.kind());
        };
        checkContainerSize(launcher, positions.length);
        for (int i = 0; i < positions.length; i++) {
            boolean output = launcher.kind() == LauncherBlockEntity.Kind.PAD_RUSTED ? i == 0 : !custom() && (i == 4 || i == 6);
            addSlot(output ? new TakeOnlySlot(launcher, i, positions[i][0], positions[i][1])
                    : custom() ? new AssemblySlot(launcher, i, positions[i][0], positions[i][1])
                    : new Slot(launcher, i, positions[i][0], positions[i][1]));
        }
        int inventoryY = custom() ? 140 : 154;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, inventoryY + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, inventoryY + 58));
        // LauncherBlockEntity synchronizes its full-width gauges through its block
        // entity update tag. Vanilla ContainerData uses signed shorts and would
        // truncate the 100,000-unit tanks/power values used by the legacy GUI.
    }
    private static LauncherBlockEntity resolve(Inventory inventory, BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof LauncherBlockEntity launcher) return launcher;
        throw new IllegalStateException("Launcher menu has no launcher at " + pos);
    }
    public LauncherBlockEntity launcher() { return launcher; }
    public BlockPos blockPos() { return launcher.getBlockPos(); }
    public boolean custom() { return launcher.kind() == LauncherBlockEntity.Kind.COMPACT || launcher.kind() == LauncherBlockEntity.Kind.TABLE; }
    @Override public boolean stillValid(Player player) { return launcher.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player)) return false;
        if (launcher.kind() == LauncherBlockEntity.Kind.PAD_RUSTED && button == 0) {
            launcher.releaseMissile(); return true;
        }
        if (launcher.kind() == LauncherBlockEntity.Kind.TABLE && button >= 2 && button <= 4) {
            launcher.setTableSize(MissilePartItem.Size.values()[button]); return true;
        }
        return false;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        ItemStack source = slot.getItem(), copy = source.copy();
        int count = launcher.getContainerSize();
        boolean moved;
        if (index < count) moved = moveItemStackTo(source, count, slots.size(), true);
        else if (custom()) moved = moveItemStackTo(source, 0, 8, false);
        else if (launcher.kind() == LauncherBlockEntity.Kind.PAD_RUSTED) {
            int target = LauncherBlockEntity.isDesignator(source) ? 3 : itemIs(source, "launch_code") ? 1 : itemIs(source, "launch_key") ? 2 : -1;
            moved = target >= 0 && moveItemStackTo(source, target, target + 1, false);
        } else if (BatteryPackItem.isBattery(source)) moved = moveItemStackTo(source, 2, 3, false);
        else if (launcher.validMissile(source)) moved = moveItemStackTo(source, 0, 1, false);
        else if (itemIs(source, "fluid_barrel_infinite")) moved = moveItemStackTo(source, 3, 4, false) || moveItemStackTo(source, 5, 6, false);
        else if (HbmFluidContainerTransfer.canDrainIntoTank(source, launcher.fuelTank(), f -> f == launcher.fuelTank().type(), out -> true))
            moved = moveItemStackTo(source, 3, 4, false);
        else if (HbmFluidContainerTransfer.canDrainIntoTank(source, launcher.oxidizerTank(), f -> f == launcher.oxidizerTank().type(), out -> true))
            moved = moveItemStackTo(source, 5, 6, false);
        else moved = LauncherBlockEntity.isDesignator(source) && moveItemStackTo(source, 1, 2, false);
        if (!moved) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }
    private static boolean itemIs(ItemStack stack, String id) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ReinhardtsHBM.id(id)); }
    private static final class TakeOnlySlot extends Slot {
        TakeOnlySlot(LauncherBlockEntity launcher, int slot, int x, int y) { super(launcher, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
    /** The 1.7.10 custom launcher containers used plain slots even though the
     * tile's automation validator rejects direct insertion. Keep that split:
     * GUI assembly accepts the component stack; automation remains restricted. */
    private static final class AssemblySlot extends Slot {
        AssemblySlot(LauncherBlockEntity launcher, int slot, int x, int y) { super(launcher, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return true; }
    }
}
