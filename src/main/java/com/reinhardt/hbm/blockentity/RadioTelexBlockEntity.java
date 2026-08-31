package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.menu.RadioTelexMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.rtty.HbmRttySystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Direct TileEntityRadioTelex port, including its character-at-a-time RTTY protocol. */
public final class RadioTelexBlockEntity extends BlockEntity implements MenuProvider {
    public static final int LINE_COUNT = 5;
    public static final int LINE_WIDTH = 33;
    public static final int CHANNEL_WIDTH = 10;
    public static final char EOL = '\n';
    public static final char EOT = '\u0004';
    public static final char BELL = '\u0007';
    public static final char PRINT = '\u000c';
    public static final char PAUSE = '\u0016';
    public static final char CLEAR = '\u007f';

    private String txChannel = "";
    private String rxChannel = "";
    private final String[] txBuffer = new String[LINE_COUNT];
    private final String[] rxBuffer = new String[LINE_COUNT];
    private int sendingLine;
    private int sendingIndex;
    private boolean sending;
    private int sendingWait;
    private int writingLine;
    private boolean printAfterReceive;
    private boolean deleteOnReceive = true;
    private char sendingChar = ' ';
    private long lastReceiveTick = Long.MIN_VALUE;

    public RadioTelexBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.RADIO_TELEX.get(), pos, state);
        Arrays.fill(txBuffer, "");
        Arrays.fill(rxBuffer, "");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioTelexBlockEntity telex) {
        if (!level.isClientSide) telex.tickServer(level);
    }

    private void tickServer(Level level) {
        sendingChar = ' ';
        if (sending && txChannel.isEmpty()) sending = false;

        if (sending) {
            if (sendingWait > 0) {
                sendingWait--;
            } else {
                String line = txBuffer[sendingLine];
                if (line.length() > sendingIndex) {
                    char character = line.charAt(sendingIndex++);
                    if (character == PAUSE) {
                        sendingWait = 20;
                    } else {
                        HbmRttySystem.broadcast(level, txChannel, character);
                        sendingChar = character;
                    }
                } else if (sendingLine >= LINE_COUNT - 1) {
                    sending = false;
                    HbmRttySystem.broadcast(level, txChannel, EOT);
                    sendingLine = 0;
                    sendingIndex = 0;
                } else {
                    HbmRttySystem.broadcast(level, txChannel, EOL);
                    sendingLine++;
                    sendingIndex = 0;
                }
            }
        }

        receive(level);
        sync();
    }

    private void receive(Level level) {
        if (rxChannel.isEmpty()) return;
        HbmRttySystem.Channel channel = HbmRttySystem.listen(level, rxChannel);
        if (channel == null || channel.timestamp() <= level.getGameTime() - 2L
                || channel.timestamp() == lastReceiveTick || channel.signal().length() != 1) return;
        lastReceiveTick = channel.timestamp();
        char character = channel.signal().charAt(0);

        if (deleteOnReceive) {
            deleteOnReceive = false;
            clearReceiveBuffer();
        }

        if (character == EOT) {
            if (printAfterReceive) {
                printAfterReceive = false;
                printMessage();
            }
            deleteOnReceive = true;
        } else if (character == EOL) {
            if (writingLine < LINE_COUNT - 1) writingLine++;
        } else if (character == BELL) {
            level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.BLOCKS, 2.0F, 0.5F);
        } else if (character == PRINT) {
            printAfterReceive = true;
        } else if (character == CLEAR) {
            clearReceiveBuffer();
        } else {
            rxBuffer[writingLine] += character;
        }
    }

    public String txChannel() {
        return txChannel;
    }

    public String rxChannel() {
        return rxChannel;
    }

    public String[] txBuffer() {
        return txBuffer.clone();
    }

    public String[] rxBuffer() {
        return rxBuffer.clone();
    }

    public char sendingChar() {
        return sendingChar;
    }

    public void updateTransmitBuffer(List<String> lines) {
        for (int index = 0; index < LINE_COUNT; index++) {
            String line = index < lines.size() && lines.get(index) != null ? lines.get(index) : "";
            txBuffer[index] = line.length() > LINE_WIDTH ? line.substring(0, LINE_WIDTH) : line;
        }
        sync();
    }

    public void saveChannels(String transmit, String receive) {
        txChannel = truncate(transmit, CHANNEL_WIDTH);
        rxChannel = truncate(receive, CHANNEL_WIDTH);
        sync();
    }

    public void sendMessage(List<String> lines) {
        updateTransmitBuffer(lines);
        if (!sending) {
            sending = true;
            sendingLine = 0;
            sendingIndex = 0;
        }
        sync();
    }

    public void clearReceiveBuffer() {
        Arrays.fill(rxBuffer, "");
        writingLine = 0;
        sync();
    }

    public void printMessage() {
        if (level == null || level.isClientSide) return;
        ItemStack paper = new ItemStack(Items.PAPER);
        List<Component> lines = new ArrayList<>();
        for (String line : rxBuffer) if (!line.isEmpty()) lines.add(Component.literal(line));
        paper.set(DataComponents.CUSTOM_NAME, Component.literal("Message"));
        paper.set(DataComponents.LORE, new ItemLore(lines));
        level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5D,
                worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D, paper));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.reinhardtshbm.radio_telex");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RadioTelexMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int index = 0; index < LINE_COUNT; index++) {
            tag.putString("tx" + index, txBuffer[index]);
            tag.putString("rx" + index, rxBuffer[index]);
        }
        tag.putString("txChan", txChannel);
        tag.putString("rxChan", rxChannel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int index = 0; index < LINE_COUNT; index++) {
            txBuffer[index] = truncate(tag.getString("tx" + index), LINE_WIDTH);
            rxBuffer[index] = tag.getString("rx" + index);
        }
        txChannel = truncate(tag.getString("txChan"), CHANNEL_WIDTH);
        rxChannel = truncate(tag.getString("rxChan"), CHANNEL_WIDTH);
        sendingChar = tag.contains("sendingChar") ? (char) tag.getInt("sendingChar") : ' ';
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        tag.putInt("sendingChar", sendingChar);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private static String truncate(String value, int length) {
        String normalized = value == null ? "" : value;
        return normalized.length() > length ? normalized.substring(0, length) : normalized;
    }
}
