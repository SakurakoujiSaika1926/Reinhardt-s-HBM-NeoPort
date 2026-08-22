package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.util.SettingsCopiable;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WandLogicBlockEntity extends BlockEntity implements SettingsCopiable {
    public static final List<String> ACTIONS = List.of(
            "FODDER_WAVE",
            "POWER_LOCK",
            "COLLAPSE_ROOF_RAD_5",
            "COLLAPSE_ROOF_RAD_10",
            "BOMB_TRAP",
            "BOMB_CRANE",
            "DEAD_GUY_CRANE",
            "SKELETON_GUN_TIER_1",
            "SKELETON_GUN_TIER_2",
            "SKELETON_GUN_TIER_3",
            "ZOMBIE_TIER_1",
            "ZOMBIE_TIER_2",
            "ABERRATOR",
            "PUZZLE_TEST",
            "MISSILE_STRIKE",
            "IRRADIATE_ENTITIES_AOE"
    );
    public static final List<String> CONDITIONS = List.of(
            "EMPTY",
            "PLAYER_CUBE_3",
            "PLAYER_CUBE_5",
            "PLAYER_CUBE_25",
            "BOMB_CRANE",
            "ABERRATOR",
            "REDSTONE",
            "PUZZLE_TEST"
    );
    public static final List<String> INTERACTIONS = List.of(
            "POWER_LOCK",
            "TEST",
            "RADAWAY_INJECTOR"
    );

    public boolean triggerReplace;
    public int placedRotation = Direction.SOUTH.get3DDataValue();
    public String disguise = "";
    public int disguiseMeta;
    public String actionID = "FODDER_WAVE";
    public String conditionID = "PLAYER_CUBE_5";
    public String interactionID = "";

    public WandLogicBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.WAND_LOGIC.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WandLogicBlockEntity logic) {
        if (!level.isClientSide && logic.triggerReplace) {
            logic.replace();
        }
    }

    public void triggerReplace() {
        this.triggerReplace = true;
        sync();
    }

    private void replace() {
        BlockState state = this.disguise == null || this.disguise.isBlank()
                ? HbmBlocks.LOGIC_BLOCK_INVIS.get().defaultBlockState()
                : HbmBlocks.LOGIC_BLOCK.get().defaultBlockState();
        this.level.setBlock(this.worldPosition, state, 3);
        if (this.level.getBlockEntity(this.worldPosition) instanceof LogicBlockEntity logic) {
            logic.actionID = normalizeActionId(this.actionID);
            logic.conditionID = normalizeConditionId(this.conditionID);
            logic.interactionID = normalizeInteractionId(this.interactionID);
            logic.direction = Direction.from3DDataValue(this.placedRotation);
            logic.disguise = this.disguise;
            logic.disguiseMeta = this.disguiseMeta;
            logic.sync();
        }
    }

    public void setDisguise(Block block, int meta) {
        this.disguise = BuiltInRegistries.BLOCK.getKey(block).toString();
        this.disguiseMeta = meta;
        sync();
    }

    public void cycleAction(boolean reverse) {
        this.actionID = cycle(ACTIONS, normalizeActionId(this.actionID), reverse);
        sync();
    }

    public void cycleCondition(boolean reverse) {
        this.conditionID = cycle(CONDITIONS, normalizeConditionId(this.conditionID), reverse);
        sync();
    }

    public void cycleInteraction(boolean reverse) {
        this.interactionID = cycle(INTERACTIONS, normalizeInteractionId(this.interactionID), reverse);
        sync();
    }

    private static String cycle(List<String> values, String current, boolean reverse) {
        int index = values.indexOf(current);
        index += reverse ? -1 : 1;
        if (index < 0) {
            index = 0;
        }
        if (index >= values.size()) {
            index = values.size() - 1;
        }
        return values.get(index);
    }

    public static String normalizeActionId(String id) {
        if (id == null || id.isBlank()) {
            return "FODDER_WAVE";
        }
        return switch (id) {
            case "ZOMBIES_TIER_1" -> "ZOMBIE_TIER_1";
            case "ZOMBIES_TIER_2" -> "ZOMBIE_TIER_2";
            case "SKELETONS_GUN_TIER_1" -> "SKELETON_GUN_TIER_1";
            case "SKELETONS_GUN_TIER_2" -> "SKELETON_GUN_TIER_2";
            case "SKELETONS_GUN_TIER_3" -> "SKELETON_GUN_TIER_3";
            case "RAD_CONTAINMENT_SYSTEM" -> "IRRADIATE_ENTITIES_AOE";
            default -> ACTIONS.contains(id) ? id : "FODDER_WAVE";
        };
    }

    public static String normalizeConditionId(String id) {
        if (id == null || id.isBlank()) {
            return "PLAYER_CUBE_5";
        }
        return CONDITIONS.contains(id) ? id : "PLAYER_CUBE_5";
    }

    public static String normalizeInteractionId(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        return INTERACTIONS.contains(id) ? id : "";
    }

    public Block disguiseBlock() {
        ResourceLocation id = ResourceLocation.tryParse(HbmStructureIO.normalizeId(this.disguise));
        return id != null && BuiltInRegistries.BLOCK.containsKey(id) ? BuiltInRegistries.BLOCK.get(id) : Blocks.AIR;
    }

    public CompoundTag configTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("trigger", this.triggerReplace);
        tag.putInt("rotation", this.placedRotation);
        tag.putString("actionID", normalizeActionId(this.actionID));
        tag.putString("conditionID", normalizeConditionId(this.conditionID));
        if (this.interactionID != null && !this.interactionID.isBlank()) {
            tag.putString("interactionID", normalizeInteractionId(this.interactionID));
        }
        if (this.disguise != null && !this.disguise.isBlank()) {
            tag.putString("disguise", this.disguise);
            tag.putInt("disguiseMeta", this.disguiseMeta);
        }
        return tag;
    }

    public void applyConfig(CompoundTag tag) {
        this.triggerReplace = tag.getBoolean("trigger");
        this.placedRotation = tag.getInt("rotation");
        this.actionID = normalizeActionId(tag.getString("actionID"));
        this.conditionID = normalizeConditionId(tag.getString("conditionID"));
        this.interactionID = normalizeInteractionId(tag.getString("interactionID"));
        this.disguise = tag.getString("disguise");
        this.disguiseMeta = tag.getInt("disguiseMeta");
        sync();
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putString("actionID", normalizeActionId(this.actionID));
        tag.putString("conditionID", normalizeConditionId(this.conditionID));
        if (this.interactionID != null && !this.interactionID.isBlank()) {
            tag.putString("interactionID", normalizeInteractionId(this.interactionID));
        }
        tag.putInt("rotation", this.placedRotation);
        if (this.disguise != null && !this.disguise.isBlank()) {
            tag.putString("disguise", this.disguise);
            tag.putInt("disguiseMeta", this.disguiseMeta);
        }
        return tag;
    }

    @Override
    public void pasteSettings(CompoundTag settings, int index, Level level, net.minecraft.world.entity.player.Player player, BlockPos pos) {
        this.actionID = normalizeActionId(settings.getString("actionID"));
        this.conditionID = normalizeConditionId(settings.getString("conditionID"));
        this.interactionID = normalizeInteractionId(settings.getString("interactionID"));
        if (settings.contains("rotation")) {
            this.placedRotation = settings.getInt("rotation");
        }
        this.disguise = settings.getString("disguise");
        this.disguiseMeta = settings.getInt("disguiseMeta");
        sync();
    }

    @Override
    public List<Component> settingsInfo(Level level, BlockPos pos, CompoundTag settings) {
        return List.of(
                Component.literal("Action: " + normalizeActionId(this.actionID)),
                Component.literal("Condition: " + normalizeConditionId(this.conditionID)),
                Component.literal("Interaction: " + (this.interactionID == null || this.interactionID.isBlank() ? "None" : normalizeInteractionId(this.interactionID)))
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
