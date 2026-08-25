package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.EnergyCableBlock;
import com.reinhardt.hbm.block.FluidDuctBlock;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class MachineDummyBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final int[] NO_SLOTS = {};
    private BlockPos corePos = BlockPos.ZERO;

    public MachineDummyBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.MACHINE_DUMMY.get(), pos, blockState);
    }

    public BlockPos getCorePos() {
        return this.corePos;
    }

    @Nullable
    public BlockEntity core() {
        return this.level == null ? null : this.level.getBlockEntity(this.corePos);
    }

    public void setCorePos(BlockPos corePos) {
        this.corePos = corePos.immutable();
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            refreshAdjacentNetworks();
            PowerNetworkManager.markDirty(this.level);
        }
    }

    private void refreshAdjacentNetworks() {
        if (this.level == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = this.worldPosition.relative(direction);
            BlockState neighborState = this.level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof FluidDuctBlock duct) {
                duct.refreshConnections(this.level, neighborPos);
            } else if (neighborState.getBlock() instanceof EnergyCableBlock) {
                EnergyCableBlock.refreshConnections(this.level, neighborPos);
            }
        }
    }

    @Override
    public int getContainerSize() {
        WorldlyContainer core = coreContainer();
        return core == null ? 0 : core.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        WorldlyContainer core = coreContainer();
        return core == null || core.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        WorldlyContainer core = coreContainer();
        return core == null ? ItemStack.EMPTY : core.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        WorldlyContainer core = coreContainer();
        return core == null ? ItemStack.EMPTY : core.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        WorldlyContainer core = coreContainer();
        return core == null ? ItemStack.EMPTY : core.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        WorldlyContainer core = coreContainer();
        if (core != null) {
            core.setItem(slot, stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        WorldlyContainer core = coreContainer();
        return core != null && core.stillValid(player);
    }

    @Override
    public void clearContent() {
        WorldlyContainer core = coreContainer();
        if (core != null) {
            core.clearContent();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        WorldlyContainer core = coreContainer();
        return core != null && core.canPlaceItem(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        WorldlyContainer core = coreContainer();
        if (core instanceof ArcFurnaceBlockEntity furnace) {
            return furnace.isAutomationPort(this.worldPosition) ? furnace.getSlotsForFace(side) : NO_SLOTS;
        }
        if (core instanceof AssemblyFactoryBlockEntity factory) {
            return factory.allowsAutomationPort(this.worldPosition, side) ? factory.getSlotsForAccessor(this.worldPosition, side) : NO_SLOTS;
        }
        if (core instanceof ChemicalFactoryBlockEntity factory) {
            return factory.allowsAutomationPort(this.worldPosition, side) ? factory.getSlotsForFace(side) : NO_SLOTS;
        }
        if (core instanceof ParticleAcceleratorBlockEntity accelerator) {
            return accelerator.getSlotsForAccessor(this.worldPosition, side);
        }
        if (core instanceof RotaryFurnaceBlockEntity furnace) {
            return furnace.getSlotsForAccessor(this.worldPosition, side);
        }
        if (core instanceof LegacyMachineBlockEntity machine) {
            return machine.allowsItemAutomationPort(this.worldPosition) ? machine.getSlotsForFace(side) : NO_SLOTS;
        }
        return core == null ? NO_SLOTS : core.getSlotsForFace(side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        WorldlyContainer core = coreContainer();
        if (core instanceof ArcFurnaceBlockEntity furnace) {
            return furnace.isAutomationPort(this.worldPosition)
                    && containsSlot(furnace.getSlotsForFace(side), slot)
                    && furnace.canPlaceItemThroughFace(slot, stack, side);
        }
        if (core instanceof AssemblyFactoryBlockEntity factory) {
            return factory.allowsAutomationPort(this.worldPosition, side)
                    && containsSlot(factory.getSlotsForAccessor(this.worldPosition, side), slot)
                    && factory.canPlaceItemThroughFace(slot, stack, side);
        }
        if (core instanceof ChemicalFactoryBlockEntity factory) {
            return factory.allowsAutomationPort(this.worldPosition, side)
                    && factory.canPlaceItemThroughFace(slot, stack, side);
        }
        if (core instanceof ParticleAcceleratorBlockEntity accelerator) {
            return containsSlot(accelerator.getSlotsForAccessor(this.worldPosition, side), slot)
                    && accelerator.canPlaceItemThroughFace(slot, stack, side);
        }
        if (core instanceof RotaryFurnaceBlockEntity furnace) {
            return containsSlot(furnace.getSlotsForAccessor(this.worldPosition, side), slot)
                    && furnace.canPlaceItemThroughFace(slot, stack, side);
        }
        if (core instanceof LegacyMachineBlockEntity machine) {
            return machine.allowsItemAutomationPort(this.worldPosition)
                    && containsSlot(machine.getSlotsForFace(side), slot)
                    && machine.canPlaceItemThroughFace(slot, stack, side);
        }
        return core != null && core.canPlaceItemThroughFace(slot, stack, side);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        WorldlyContainer core = coreContainer();
        if (core instanceof ArcFurnaceBlockEntity furnace) {
            return furnace.isAutomationPort(this.worldPosition)
                    && containsSlot(furnace.getSlotsForFace(side), slot)
                    && furnace.canTakeItemThroughFace(slot, stack, side);
        }
        if (core instanceof AssemblyFactoryBlockEntity factory) {
            return factory.allowsAutomationPort(this.worldPosition, side)
                    && containsSlot(factory.getSlotsForAccessor(this.worldPosition, side), slot)
                    && factory.canTakeItemThroughFace(slot, stack, side);
        }
        if (core instanceof ChemicalFactoryBlockEntity factory) {
            return factory.allowsAutomationPort(this.worldPosition, side)
                    && factory.canTakeItemThroughFace(slot, stack, side);
        }
        if (core instanceof ParticleAcceleratorBlockEntity accelerator) {
            return containsSlot(accelerator.getSlotsForAccessor(this.worldPosition, side), slot)
                    && accelerator.canTakeItemThroughFace(slot, stack, side);
        }
        if (core instanceof RotaryFurnaceBlockEntity furnace) {
            return containsSlot(furnace.getSlotsForAccessor(this.worldPosition, side), slot)
                    && furnace.canTakeItemThroughFace(slot, stack, side);
        }
        if (core instanceof LegacyMachineBlockEntity machine) {
            return machine.allowsItemAutomationPort(this.worldPosition)
                    && containsSlot(machine.getSlotsForFace(side), slot)
                    && machine.canTakeItemThroughFace(slot, stack, side);
        }
        return core != null && core.canTakeItemThroughFace(slot, stack, side);
    }

    private static boolean containsSlot(int[] slots, int slot) {
        for (int accessible : slots) {
            if (accessible == slot) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private WorldlyContainer coreContainer() {
        if (this.level != null && this.level.getBlockEntity(this.corePos) instanceof GasFlareBlockEntity) {
            return null;
        }
        if (this.level != null && this.level.getBlockEntity(this.corePos) instanceof WorldlyContainer container) {
            return container;
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CoreX", this.corePos.getX());
        tag.putInt("CoreY", this.corePos.getY());
        tag.putInt("CoreZ", this.corePos.getZ());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.corePos = new BlockPos(tag.getInt("CoreX"), tag.getInt("CoreY"), tag.getInt("CoreZ"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
