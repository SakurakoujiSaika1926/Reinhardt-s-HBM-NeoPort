package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.RadioTorchBlock;
import com.reinhardt.hbm.blockentity.RadioTorchBlockEntity;
import com.reinhardt.hbm.menu.RttyConfigMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative configuration changes from the two old RTTY torch GUIs. */
public record RttyConfigPayload(BlockPos pos, int action, boolean polling,
                                String controllerChannel, List<String> channels, List<String> names)
        implements CustomPacketPayload {
    private static final int ENTRY_COUNT = 8;
    public static final int ACTION_POLLING = 0;
    public static final int ACTION_CONTROLLER_SAVE = 1;
    public static final int ACTION_READER_SAVE = 2;
    public static final Type<RttyConfigPayload> TYPE = new Type<>(ReinhardtsHBM.id("rtty_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RttyConfigPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RttyConfigPayload decode(RegistryFriendlyByteBuf buffer) {
            BlockPos pos = buffer.readBlockPos();
            int action = buffer.readVarInt();
            boolean polling = buffer.readBoolean();
            String controllerChannel = buffer.readUtf(RadioTorchBlockEntity.CHANNEL_LIMIT);
            List<String> channels = new ArrayList<>(ENTRY_COUNT);
            List<String> names = new ArrayList<>(ENTRY_COUNT);
            for (int index = 0; index < ENTRY_COUNT; index++) {
                channels.add(buffer.readUtf(RadioTorchBlockEntity.CHANNEL_LIMIT));
            }
            for (int index = 0; index < ENTRY_COUNT; index++) {
                names.add(buffer.readUtf(RadioTorchBlockEntity.VALUE_NAME_LIMIT));
            }
            return new RttyConfigPayload(pos, action, polling, controllerChannel,
                    List.copyOf(channels), List.copyOf(names));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RttyConfigPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.action());
            buffer.writeBoolean(payload.polling());
            buffer.writeUtf(payload.controllerChannel(), RadioTorchBlockEntity.CHANNEL_LIMIT);
            for (int index = 0; index < ENTRY_COUNT; index++) {
                buffer.writeUtf(entry(payload.channels(), index), RadioTorchBlockEntity.CHANNEL_LIMIT);
            }
            for (int index = 0; index < ENTRY_COUNT; index++) {
                buffer.writeUtf(entry(payload.names(), index), RadioTorchBlockEntity.VALUE_NAME_LIMIT);
            }
        }
    };

    public RttyConfigPayload {
        controllerChannel = controllerChannel == null ? "" : controllerChannel;
        channels = channels == null ? List.of() : List.copyOf(channels);
        names = names == null ? List.of() : List.copyOf(names);
    }

    private static String entry(List<String> values, int index) {
        return index < values.size() && values.get(index) != null ? values.get(index) : "";
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RttyConfigPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof RttyConfigMenu menu)
                || !menu.blockPos().equals(payload.pos())
                || !(player.level().getBlockEntity(payload.pos()) instanceof RadioTorchBlockEntity radio)
                || radio.kind() != menu.kind()) {
            return;
        }
        switch (payload.action()) {
            case ACTION_POLLING -> radio.setPolling(payload.polling());
            case ACTION_CONTROLLER_SAVE -> {
                if (radio.kind() == RadioTorchBlock.Kind.CONTROLLER) radio.setChannel(payload.controllerChannel());
            }
            case ACTION_READER_SAVE -> {
                if (radio.kind() == RadioTorchBlock.Kind.READER) {
                    for (int index = 0; index < ENTRY_COUNT; index++) {
                        radio.setReaderEntry(index, entry(payload.channels(), index), entry(payload.names(), index));
                    }
                }
            }
            default -> {
            }
        }
    }
}
