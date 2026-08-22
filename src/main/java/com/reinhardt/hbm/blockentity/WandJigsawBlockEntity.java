package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.worldgen.structure.HbmStructureIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class WandJigsawBlockEntity extends BlockEntity {
    public int selectionPriority;
    public int placementPriority;
    public String pool = "default";
    public String name = "default";
    public String target = "default";
    public String replaceBlock = "minecraft:air";
    public int replaceMeta;
    public boolean rollable = true;

    public WandJigsawBlockEntity(BlockPos pos, BlockState state) {
        this(HbmBlockEntities.WAND_JIGSAW.get(), pos, state);
    }

    protected WandJigsawBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setReplacement(Block block, int meta) {
        this.replaceBlock = block == null ? "minecraft:air" : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
        this.replaceMeta = meta;
        sync();
    }

    public Block replacementBlock() {
        var id = net.minecraft.resources.ResourceLocation.tryParse(this.replaceBlock);
        if (id == null || !net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id)) {
            return Blocks.AIR;
        }
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
    }

    public CompoundTag configTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("selection", this.selectionPriority);
        tag.putInt("placement", this.placementPriority);
        tag.putString("pool", this.pool);
        tag.putString("name", this.name);
        tag.putString("target", this.target);
        tag.putString("block", this.replaceBlock);
        tag.putInt("meta", this.replaceMeta);
        tag.putBoolean("roll", this.rollable);
        return tag;
    }

    public void applyConfig(CompoundTag tag) {
        this.selectionPriority = tag.getInt("selection");
        this.placementPriority = tag.getInt("placement");
        this.pool = tag.getString("pool").isBlank() ? "default" : tag.getString("pool");
        this.name = tag.getString("name").isBlank() ? "default" : tag.getString("name");
        this.target = tag.getString("target").isBlank() ? "default" : tag.getString("target");
        this.replaceBlock = HbmStructureIO.normalizeId(tag.getString("block"));
        this.replaceMeta = tag.getInt("meta");
        this.rollable = !tag.contains("roll") || tag.getBoolean("roll");
        sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.merge(configTag());
        tag.putInt("direction", getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)
                ? getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING).get3DDataValue()
                : 3);
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

    protected void sync() {
        setChanged();
        Level level = this.level;
        if (level != null) {
            level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
