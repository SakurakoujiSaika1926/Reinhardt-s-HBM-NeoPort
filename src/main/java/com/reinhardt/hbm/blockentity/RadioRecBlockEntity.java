package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.menu.RadioRecMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.rtty.HbmRttySystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Old RTTY receiver format and one-tick-late note playback. */
public final class RadioRecBlockEntity extends BlockEntity implements MenuProvider {
    private static final int CHANNEL_LIMIT = 10;
    private String channel = "";
    private boolean on;
    private long lastMessageTick = Long.MIN_VALUE;

    public RadioRecBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.RADIOREC.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioRecBlockEntity radio) {
        if (!level.isClientSide) {
            radio.tickServer(level);
        }
    }

    private void tickServer(Level level) {
        if (!on || channel.isEmpty()) {
            return;
        }
        HbmRttySystem.Channel message = HbmRttySystem.listen(level, channel);
        if (message == null || message.timestamp() != level.getGameTime() - 1L || message.timestamp() == lastMessageTick) {
            return;
        }
        lastMessageTick = message.timestamp();
        String[] encodedNotes = message.signal().split("-");
        int[][] notes = new int[encodedNotes.length][3];
        for (int index = 0; index < encodedNotes.length; index++) {
            String[] pieces = encodedNotes[index].split(":");
            if (pieces.length != 3) {
                return;
            }
            try {
                int instrument = Integer.parseInt(pieces[0]);
                int note = Integer.parseInt(pieces[1]);
                int octave = Integer.parseInt(pieces[2]);
                if (instrument < 0 || instrument > 4 || note < 0 || note > 11 || octave < 0 || octave > 2) {
                    return;
                }
                notes[index][0] = instrument;
                notes[index][1] = note;
                notes[index][2] = octave;
            } catch (NumberFormatException ignored) {
                return;
            }
        }
        for (int[] encoded : notes) {
            int instrument = encoded[0];
            int note = encoded[1];
            int octave = encoded[2];
            int noteId = note + octave * 12;
            float pitch = (float) Math.pow(2.0D, (noteId - 12) / 12.0D);
            level.playSound(null, worldPosition, noteSound(instrument), SoundSource.RECORDS, 3.0F, pitch);
        }
    }

    private static net.minecraft.sounds.SoundEvent noteSound(int instrument) {
        return switch (instrument) {
            case 1 -> SoundEvents.NOTE_BLOCK_BASEDRUM.value();
            case 2 -> SoundEvents.NOTE_BLOCK_SNARE.value();
            case 3 -> SoundEvents.NOTE_BLOCK_HAT.value();
            case 4 -> SoundEvents.NOTE_BLOCK_BASS.value();
            default -> SoundEvents.NOTE_BLOCK_HARP.value();
        };
    }

    public String channel() {
        return channel;
    }

    public boolean isOn() {
        return on;
    }

    public void setChannel(String value) {
        String normalized = value == null ? "" : value.trim();
        channel = normalized.length() > CHANNEL_LIMIT ? normalized.substring(0, CHANNEL_LIMIT) : normalized;
        sync();
    }

    public void toggleOn() {
        on = !on;
        sync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.reinhardtshbm.radiorec");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RadioRecMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("channel", channel);
        tag.putBoolean("isOn", on);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setChannelWithoutSync(tag.getString("channel"));
        on = tag.getBoolean("isOn");
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

    private void setChannelWithoutSync(String value) {
        String normalized = value == null ? "" : value.trim();
        channel = normalized.length() > CHANNEL_LIMIT ? normalized.substring(0, CHANNEL_LIMIT) : normalized;
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
