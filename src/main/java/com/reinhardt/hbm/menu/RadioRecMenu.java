package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.blockentity.RadioRecBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** The 220x42 RadioRec screen has controls only, no inventory slots. */
public final class RadioRecMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;

    public RadioRecMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    private RadioRecMenu(int containerId, Inventory inventory, BlockPos blockPos) {
        super(HbmMenus.RADIOREC.get(), containerId);
        this.blockPos = blockPos.immutable();
    }

    public RadioRecMenu(int containerId, Inventory inventory, RadioRecBlockEntity radio) {
        this(containerId, inventory, radio.getBlockPos());
    }

    public BlockPos blockPos() {
        return blockPos;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(blockPos).is(HbmBlocks.RADIOREC.get())
                && player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
