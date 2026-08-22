package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class FoundrySlagBlockEntity extends BlockEntity {
    public static final int MAX_AMOUNT = FoundryShape.BLOCK.q(16);

    @Nullable
    private FoundryMaterial material;
    private int amount;

    public FoundrySlagBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.FOUNDRY_SLAG.get(), pos, state);
    }

    @Nullable
    public FoundryMaterial material() {
        return this.material;
    }

    public int amount() {
        return this.amount;
    }

    public boolean isEmpty() {
        return this.material == null || this.amount <= 0;
    }

    public boolean canAccept(FoundryMaterial material) {
        return this.isEmpty() || this.material == material;
    }

    public int remainingCapacity() {
        return Math.max(0, MAX_AMOUNT - this.amount);
    }

    public int add(FoundryMaterial material, int amount) {
        if (material == null || amount <= 0 || !canAccept(material)) {
            return 0;
        }
        this.material = material;
        int accepted = Math.min(amount, remainingCapacity());
        this.amount += accepted;
        setChangedAndSync();
        return accepted;
    }

    public void setContents(FoundryMaterial material, int amount) {
        this.material = material;
        this.amount = Math.max(0, Math.min(MAX_AMOUNT, amount));
        if (this.amount <= 0) {
            this.material = null;
        }
        setChangedAndSync();
    }

    public FoundryMaterialStack stack() {
        return this.material == null || this.amount <= 0 ? null : new FoundryMaterialStack(this.material, this.amount);
    }

    public void shrink(int amount) {
        if (amount <= 0) {
            return;
        }
        this.amount -= amount;
        if (this.amount <= 0) {
            this.amount = 0;
            this.material = null;
        }
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.material != null) {
            tag.putInt("material_id", this.material.id());
            tag.putString("material", this.material.name());
        }
        tag.putInt("amount", this.amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.material = FoundryMaterial.byId(tag.getInt("material_id"))
                .or(() -> FoundryMaterial.byName(tag.getString("material")))
                .orElse(null);
        this.amount = tag.getInt("amount");
        if (this.amount <= 0) {
            this.amount = 0;
            this.material = null;
        }
        if (this.amount > MAX_AMOUNT) {
            this.amount = MAX_AMOUNT;
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
}
