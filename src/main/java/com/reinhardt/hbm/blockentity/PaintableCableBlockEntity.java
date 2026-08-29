package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.util.SettingsCopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Saved paint and port-display state for the original paintable red-copper cable. */
public final class PaintableCableBlockEntity extends BlockEntity implements SettingsCopiable {
    private static final String PAINT_BLOCK = "PaintBlock";
    private static final String PORT_VISIBLE = "PortVisible";

    @Nullable
    private ResourceLocation paintBlock;
    private boolean portVisible = true;

    public PaintableCableBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.PAINTABLE_CABLE.get(), pos, state);
    }

    @Nullable
    public Block paintBlock() {
        return this.paintBlock == null ? null : BuiltInRegistries.BLOCK.getOptional(this.paintBlock).orElse(null);
    }

    public boolean portVisible() {
        return this.portVisible;
    }

    public void setPaintBlock(@Nullable Block block) {
        this.paintBlock = block == null ? null : BuiltInRegistries.BLOCK.getKey(block);
        sync();
    }

    public void togglePortVisible() {
        this.portVisible = !this.portVisible;
        sync();
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        if (this.paintBlock != null) {
            tag.putString(PAINT_BLOCK, this.paintBlock.toString());
        }
        tag.putBoolean(PORT_VISIBLE, this.portVisible);
        return tag;
    }

    @Override
    public void pasteSettings(CompoundTag settings, int index, Level level, Player player, BlockPos pos) {
        this.paintBlock = settings.contains(PAINT_BLOCK) ? ResourceLocation.tryParse(settings.getString(PAINT_BLOCK)) : null;
        this.portVisible = !settings.contains(PORT_VISIBLE) || settings.getBoolean(PORT_VISIBLE);
        sync();
    }

    @Override
    public List<net.minecraft.network.chat.Component> settingsInfo(Level level, BlockPos pos, CompoundTag settings) {
        Block block = paintBlock();
        return block == null ? List.of() : List.of(block.getName());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.paintBlock != null) {
            tag.putString(PAINT_BLOCK, this.paintBlock.toString());
        }
        tag.putBoolean(PORT_VISIBLE, this.portVisible);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.paintBlock = tag.contains(PAINT_BLOCK) ? ResourceLocation.tryParse(tag.getString(PAINT_BLOCK)) : null;
        this.portVisible = !tag.contains(PORT_VISIBLE) || tag.getBoolean(PORT_VISIBLE);
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
}
