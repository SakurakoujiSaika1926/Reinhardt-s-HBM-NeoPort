package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.util.SettingsCopiable;
import com.reinhardt.hbm.worldgen.structure.HbmLegacyNbtTemplate;
import com.reinhardt.hbm.worldgen.structure.HbmStructureIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WandLootBlockEntity extends BlockEntity implements SettingsCopiable {
    public boolean triggerReplace;
    public String replaceBlock = "reinhardtshbm:deco_loot";
    public int replaceMeta;
    public String poolName = "LOOT_BOOKLET";
    public int minItems;
    public int maxItems = 1;
    public float placedRotation;
    public float lockMod;
    public int lockCode;
    public boolean cheesable = true;

    public WandLootBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.WAND_LOOT.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WandLootBlockEntity loot) {
        if (!level.isClientSide && loot.triggerReplace && level instanceof ServerLevel serverLevel) {
            loot.replace(serverLevel);
        }
    }

    public void triggerReplace() {
        this.triggerReplace = true;
        sync();
    }

    public void setReplacement(Block block, int meta) {
        this.replaceBlock = BuiltInRegistries.BLOCK.getKey(block).toString();
        this.replaceMeta = meta;
        validatePoolForReplacement();
        sync();
    }

    public void setLock(int lockCode, float lockMod) {
        this.lockCode = lockCode;
        this.lockMod = lockMod;
        this.cheesable = lockMod != 0.0F;
        sync();
    }

    public void adjustMin(boolean decrease) {
        this.minItems += decrease ? -1 : 1;
        this.minItems = Math.max(0, this.minItems);
        this.maxItems = Math.max(this.minItems, this.maxItems);
        sync();
    }

    public void adjustMax(boolean decrease) {
        this.maxItems += decrease ? -1 : 1;
        this.maxItems = Math.max(0, this.maxItems);
        this.minItems = Math.min(this.minItems, this.maxItems);
        sync();
    }

    public void cyclePool(boolean reverse) {
        java.util.List<String> pools = poolNamesForReplacement();
        if (pools.isEmpty()) {
            return;
        }
        int index = 0;
        for (int i = 0; i < pools.size(); i++) {
            if (pools.get(i).equals(this.poolName)) {
                index = i;
                break;
            }
        }
        index += reverse ? -1 : 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= pools.size()) {
            index = pools.size() - 1;
        }
        this.poolName = pools.get(index);
        sync();
    }

    private void validatePoolForReplacement() {
        java.util.List<String> pools = poolNamesForReplacement();
        if (!pools.isEmpty() && !pools.contains(this.poolName)) {
            this.poolName = pools.get(0);
        }
    }

    private java.util.List<String> poolNamesForReplacement() {
        return this.replaceBlock.endsWith(":deco_loot")
                ? HbmStructureLoot.lootNames()
                : HbmStructureLoot.containerPoolNames();
    }

    private void replace(ServerLevel level) {
        Block block = blockById(this.replaceBlock);
        BlockState state = HbmLegacyNbtTemplate.stateFromLegacyId(BuiltInRegistries.BLOCK.getKey(block).toString(), this.replaceMeta);
        state = rotatePlacedContainer(state);
        level.setBlock(this.worldPosition, state, 3);
        BlockEntity blockEntity = level.getBlockEntity(this.worldPosition);
        if ((blockEntity == null || blockEntity instanceof WandLootBlockEntity) && block instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
            blockEntity = entityBlock.newBlockEntity(this.worldPosition, state);
            if (blockEntity != null) {
                level.setBlockEntity(blockEntity);
            }
        }
        if (blockEntity instanceof DecoLootBlockEntity decoLoot) {
            HbmStructureLoot.applyPileLoot(decoLoot, this.poolName, level.random);
        } else if (blockEntity instanceof Container container) {
            if (blockEntity instanceof LockableBlockEntity lockable && this.lockCode != 0) {
                lockable.setPins(this.lockCode);
                lockable.setLockMod(this.lockMod);
                lockable.setCheesable(this.lockMod != 0.0F);
                lockable.lock();
            }
            HbmStructureLoot.fillContainer(container, this.poolName, this.minItems, this.maxItems, level.random);
        }
    }

    private BlockState rotatePlacedContainer(BlockState state) {
        Direction direction = Direction.fromYRot(this.placedRotation);
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return state.setValue(HorizontalDirectionalBlock.FACING, direction);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.setValue(BlockStateProperties.FACING, direction);
        }
        return state;
    }

    private static Block blockById(String id) {
        ResourceLocation location = ResourceLocation.tryParse(HbmStructureIO.normalizeId(id));
        if (location != null && BuiltInRegistries.BLOCK.containsKey(location)) {
            return BuiltInRegistries.BLOCK.get(location);
        }
        return HbmBlocks.DECO_LOOT.get();
    }

    public CompoundTag configTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("block", this.replaceBlock);
        tag.putInt("meta", this.replaceMeta);
        tag.putInt("min", this.minItems);
        tag.putInt("max", this.maxItems);
        tag.putString("pool", this.poolName);
        tag.putFloat("rot", this.placedRotation);
        tag.putInt("lockCode", this.lockCode);
        tag.putFloat("lockMod", this.lockMod);
        tag.putBoolean("cheesable", this.cheesable);
        tag.putBoolean("trigger", this.triggerReplace);
        return tag;
    }

    public void applyConfig(CompoundTag tag) {
        this.replaceBlock = HbmStructureIO.normalizeId(tag.getString("block"));
        if (this.replaceBlock.isBlank() || this.replaceBlock.equals("minecraft:air")) {
            this.replaceBlock = "reinhardtshbm:deco_loot";
        }
        this.replaceMeta = tag.getInt("meta");
        this.minItems = Math.max(0, tag.getInt("min"));
        this.maxItems = Math.max(this.minItems, tag.getInt("max"));
        this.poolName = tag.getString("pool").isBlank() ? "LOOT_BOOKLET" : tag.getString("pool");
        this.placedRotation = tag.getFloat("rot");
        this.lockCode = tag.getInt("lockCode");
        this.lockMod = tag.getFloat("lockMod");
        this.cheesable = !tag.contains("cheesable") || tag.getBoolean("cheesable");
        this.triggerReplace = tag.getBoolean("trigger");
        validatePoolForReplacement();
        sync();
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putString("replaceBlock", this.replaceBlock);
        tag.putInt("replaceMeta", this.replaceMeta);
        tag.putInt("minItems", this.minItems);
        tag.putInt("maxItems", this.maxItems);
        tag.putString("poolName", this.poolName);
        tag.putInt("lockCode", this.lockCode);
        tag.putFloat("lockMod", this.lockMod);
        tag.putBoolean("cheesable", this.cheesable);
        return tag;
    }

    @Override
    public void pasteSettings(CompoundTag settings, int index, Level level, net.minecraft.world.entity.player.Player player, BlockPos pos) {
        String block = settings.contains("replaceBlock")
                ? settings.getString("replaceBlock")
                : settings.getString("block");
        this.replaceBlock = HbmStructureIO.normalizeId(block);
        if (this.replaceBlock.isBlank() || this.replaceBlock.equals("minecraft:air")) {
            this.replaceBlock = "reinhardtshbm:deco_loot";
        }
        this.replaceMeta = settings.contains("replaceMeta") ? settings.getInt("replaceMeta") : settings.getInt("meta");
        this.minItems = Math.max(0, settings.contains("minItems") ? settings.getInt("minItems") : settings.getInt("min"));
        this.maxItems = Math.max(this.minItems, settings.contains("maxItems") ? settings.getInt("maxItems") : settings.getInt("max"));
        this.poolName = settings.contains("poolName") ? settings.getString("poolName") : settings.getString("pool");
        if (this.poolName.isBlank()) {
            this.poolName = "LOOT_BOOKLET";
        }
        this.lockCode = settings.getInt("lockCode");
        this.lockMod = settings.getFloat("lockMod");
        this.cheesable = !settings.contains("cheesable") || settings.getBoolean("cheesable");
        validatePoolForReplacement();
        sync();
    }

    @Override
    public List<Component> settingsInfo(Level level, BlockPos pos, CompoundTag settings) {
        return List.of(
                Component.literal(blockById(this.replaceBlock).getName().getString()),
                Component.literal(this.poolName)
        );
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
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
