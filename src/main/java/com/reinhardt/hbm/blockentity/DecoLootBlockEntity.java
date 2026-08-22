package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DecoLootBlockEntity extends BlockEntity {
    private final List<LootEntry> items = new ArrayList<>();

    public DecoLootBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DECO_LOOT.get(), pos, state);
    }

    public List<LootEntry> items() {
        return this.items;
    }

    public void addItem(ItemStack stack, double x, double y, double z) {
        if (!stack.isEmpty()) {
            this.items.add(new LootEntry(stack.copy(), x, y, z));
            sync();
        }
    }

    public void dropContents() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        for (LootEntry entry : this.items) {
            ItemEntity item = new ItemEntity(
                    this.level,
                    this.worldPosition.getX() + 0.5D,
                    this.worldPosition.getY(),
                    this.worldPosition.getZ() + 0.5D,
                    entry.stack.copy()
            );
            this.level.addFreshEntity(item);
        }
        this.items.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (LootEntry entry : this.items) {
            CompoundTag item = new CompoundTag();
            item.put("stack", entry.stack.saveOptional(registries));
            item.putDouble("x", entry.x);
            item.putDouble("y", entry.y);
            item.putDouble("z", entry.z);
            list.add(item);
        }
        tag.put("items", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            ItemStack stack = ItemStack.parseOptional(registries, item.getCompound("stack"));
            if (!stack.isEmpty()) {
                this.items.add(new LootEntry(stack, item.getDouble("x"), item.getDouble("y"), item.getDouble("z")));
            }
        }
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

    private void sync() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public record LootEntry(ItemStack stack, double x, double y, double z) {
    }
}
