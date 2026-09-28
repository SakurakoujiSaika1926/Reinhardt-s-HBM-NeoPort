package com.reinhardt.hbm.menu;

import com.reinhardt.hbm.block.RadioTorchBlock;
import com.reinhardt.hbm.blockentity.RadioTorchBlockEntity;
import com.reinhardt.hbm.registry.HbmMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Slotless configuration menu for the old RTTY reader and controller screens. */
public final class RttyConfigMenu extends AbstractContainerMenu {
    private final BlockPos blockPos;
    private final RadioTorchBlock.Kind kind;

    public RttyConfigMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(), buffer.readEnum(RadioTorchBlock.Kind.class));
    }

    private RttyConfigMenu(int containerId, Inventory inventory, BlockPos blockPos, RadioTorchBlock.Kind kind) {
        super(HbmMenus.RTTY_CONFIG.get(), containerId);
        this.blockPos = blockPos.immutable();
        this.kind = kind;
    }

    public RttyConfigMenu(int containerId, Inventory inventory, RadioTorchBlockEntity radio) {
        this(containerId, inventory, radio.getBlockPos(), radio.kind());
    }

    public BlockPos blockPos() {
        return blockPos;
    }

    public RadioTorchBlock.Kind kind() {
        return kind;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(blockPos) instanceof RadioTorchBlockEntity radio
                && radio.kind() == kind
                && (kind == RadioTorchBlock.Kind.READER || kind == RadioTorchBlock.Kind.CONTROLLER)
                && player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) <= 256.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
