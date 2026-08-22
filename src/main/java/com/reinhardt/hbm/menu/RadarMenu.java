package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.blockentity.RadarTarget;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/** The non-inventory radar display from GUIMachineRadarNT. */
public final class RadarMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    private final BlockPos blockPos;

    public RadarMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, findMachine(inventory, buffer.readBlockPos()), new SimpleContainerData(LegacyMachineBlockEntity.DATA_COUNT));
    }

    public RadarMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine) {
        this(containerId, inventory, machine, machine.menuData());
    }

    private RadarMenu(int containerId, Inventory inventory, LegacyMachineBlockEntity machine, ContainerData data) {
        super(HbmMenus.RADAR.get(), containerId);
        this.container = machine == null ? new SimpleContainer(0) : machine;
        this.data = data;
        this.blockPos = machine == null ? BlockPos.ZERO : machine.getBlockPos().immutable();
        checkContainerDataCount(data, LegacyMachineBlockEntity.DATA_COUNT);
        addDataSlots(data);
    }

    @Override
    public boolean stillValid(Player player) {
        return isBoundRadar(player, this.blockPos, this.container);
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        // The main radar display has no slots. Its separate link GUI owns all inventory transfers.
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(this.container instanceof LegacyMachineBlockEntity machine) || !stillValid(player)) return false;
        if (id == 7 && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            machine.openRadarSlots(serverPlayer);
            return true;
        }
        if (id >= 0 && id <= 6) {
            machine.applyRadarControl(id);
            return true;
        }
        return false;
    }

    public BlockPos blockPos() { return this.blockPos; }
    public long power() { return Integer.toUnsignedLong(this.data.get(0)); }
    public long powerCapacity() { return this.container instanceof LegacyMachineBlockEntity machine ? machine.energyCapacity() : 100_000L; }
    public LegacyMachineBlockEntity machine() { return this.container instanceof LegacyMachineBlockEntity machine ? machine : null; }
    public List<RadarTarget> targets() { return machine() == null ? List.of() : machine().radarTargets(); }
    public byte mapCell(int index) { return machine() == null ? 0 : machine().radarMapCell(index); }

    private static LegacyMachineBlockEntity findMachine(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof LegacyMachineBlockEntity machine
                && (machine.machineId().equals("machine_radar") || machine.machineId().equals("machine_radar_large"))
                ? machine : null;
    }

    static boolean isBoundRadar(Player player, BlockPos pos, Container container) {
        BlockEntity entity = player.level().getBlockEntity(pos);
        return entity == container
                && entity instanceof LegacyMachineBlockEntity machine
                && (machine.machineId().equals("machine_radar") || machine.machineId().equals("machine_radar_large"));
    }
}
