package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmMenus;
import com.reinhardt.hbm.blockentity.RadioTelexBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** The legacy telex screen has no inventory slots; its fields are packet-backed. */
public final class RadioTelexMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;

    public RadioTelexMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    private RadioTelexMenu(int containerId, Inventory inventory, BlockPos blockPos) {
        super(HbmMenus.RADIO_TELEX.get(), containerId);
        this.blockPos = blockPos.immutable();
    }

    public RadioTelexMenu(int containerId, Inventory inventory, RadioTelexBlockEntity telex) {
        this(containerId, inventory, telex.getBlockPos());
    }

    public BlockPos blockPos() {
        return blockPos;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(blockPos).is(HbmBlocks.RADIO_TELEX.get())
                && player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D,
                blockPos.getZ() + 0.5D) <= 256.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
