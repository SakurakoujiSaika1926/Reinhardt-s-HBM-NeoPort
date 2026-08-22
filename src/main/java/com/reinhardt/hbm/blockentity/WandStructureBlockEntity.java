package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.block.WandStructureBlock;
import com.reinhardt.hbm.worldgen.structure.HbmStructureIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class WandStructureBlockEntity extends BlockEntity {
    public String name = "";
    public int sizeX = 1;
    public int sizeY = 1;
    public int sizeZ = 1;
    private final Set<HbmStructureIO.LegacyBlockKey> blacklist = new HashSet<>();

    public WandStructureBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.WAND_STRUCTURE.get(), pos, state);
    }

    public Set<HbmStructureIO.LegacyBlockKey> blacklist() {
        return this.blacklist;
    }

    public void toggleBlacklist(HbmStructureIO.LegacyBlockKey key) {
        if (!this.blacklist.remove(key)) {
            this.blacklist.add(key);
        }
        sync();
    }

    public boolean saveStructure(net.minecraft.server.level.ServerPlayer player) {
        if (!(this.level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }
        if (this.name.isBlank() || this.sizeX <= 0 || this.sizeY <= 0 || this.sizeZ <= 0) {
            return false;
        }
        Set<HbmStructureIO.LegacyBlockKey> excluded = new HashSet<>(this.blacklist);
        excluded.add(new HbmStructureIO.LegacyBlockKey("minecraft:air", 0));
        BlockPos from = this.worldPosition.above();
        BlockPos to = this.worldPosition.offset(this.sizeX - 1, this.sizeY, this.sizeZ - 1);
        boolean saved = HbmStructureIO.saveArea(serverLevel, this.name, from, to, excluded);
        if (saved) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("chat.reinhardtshbm.wand_structure.saved", this.name), false);
        } else {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("chat.reinhardtshbm.wand_structure.save_failed"), false);
        }
        return saved;
    }

    public boolean loadStructure(net.minecraft.server.level.ServerPlayer player) {
        if (!(this.level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return false;
        }
        if (this.name.isBlank()) {
            return false;
        }
        boolean debug = !serverLevel.hasNeighborSignal(this.worldPosition);
        boolean loaded = HbmStructureIO.loadArea(serverLevel, this.name, this.worldPosition.above(), 0, debug);
        if (loaded) {
            BlockState state = getBlockState();
            if (state.hasProperty(WandStructureBlock.LOAD)) {
                serverLevel.setBlock(this.worldPosition, state.setValue(WandStructureBlock.LOAD, false), 3);
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("chat.reinhardtshbm.wand_structure.loaded", this.name), false);
        } else {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("chat.reinhardtshbm.wand_structure.load_failed"), false);
        }
        return loaded;
    }

    public CompoundTag configTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", this.name);
        tag.putInt("sizeX", this.sizeX);
        tag.putInt("sizeY", this.sizeY);
        tag.putInt("sizeZ", this.sizeZ);
        ListTag blocks = new ListTag();
        for (HbmStructureIO.LegacyBlockKey key : this.blacklist) {
            CompoundTag entry = new CompoundTag();
            entry.putString("block", key.id());
            entry.putInt("meta", key.meta());
            blocks.add(entry);
        }
        tag.put("blacklist", blocks);
        return tag;
    }

    public void applyConfig(CompoundTag tag) {
        this.name = tag.getString("name");
        this.sizeX = Math.max(1, tag.getInt("sizeX"));
        this.sizeY = Math.max(1, tag.getInt("sizeY"));
        this.sizeZ = Math.max(1, tag.getInt("sizeZ"));
        this.blacklist.clear();
        ListTag blocks = tag.getList("blacklist", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            this.blacklist.add(new HbmStructureIO.LegacyBlockKey(entry.getString("block"), entry.getInt("meta")));
        }
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.merge(configTag());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        applyConfig(tag);
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

    public void sync() {
        setChanged();
        Level level = this.level;
        if (level != null) {
            level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
