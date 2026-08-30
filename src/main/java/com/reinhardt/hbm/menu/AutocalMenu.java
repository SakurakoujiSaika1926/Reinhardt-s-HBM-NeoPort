package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.AutocalBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** AUTOCAL has the old no-inventory control screen. */
public final class AutocalMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;

    public AutocalMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    private AutocalMenu(int containerId, Inventory inventory, BlockPos blockPos) {
        super(HbmMenus.AUTOCAL.get(), containerId);
        this.blockPos = blockPos.immutable();
    }

    public AutocalMenu(int containerId, Inventory inventory, AutocalBlockEntity autocal) {
        this(containerId, inventory, autocal.getBlockPos());
    }

    public BlockPos blockPos() {
        return blockPos;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(blockPos).is(HbmBlocks.RADIO_AUTOCAL.get())
                && player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 1.0D, blockPos.getZ() + 0.5D) <= 225.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
