package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.RadioTelexBlockEntity;
import com.reinhardt.hbm.menu.RadioTelexMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Server controls matching the old telex GUI's NBTControlPacket commands. */
public record RadioTelexControlPayload(BlockPos pos, int action, String transmit, String receive) implements CustomPacketPayload {
    public static final int SAVE_CHANNELS = 0;
    public static final int SEND = 1;
    public static final int DELETE_TRANSMIT = 2;
    public static final int PRINT_RECEIVE = 3;
    public static final int CLEAR_RECEIVE = 4;
    public static final int UPDATE_TRANSMIT = 5;
    private static final int MAX_TEXT_LENGTH = RadioTelexBlockEntity.LINE_WIDTH * RadioTelexBlockEntity.LINE_COUNT
            + RadioTelexBlockEntity.LINE_COUNT - 1;
    private static final int MAX_CHANNEL_LENGTH = RadioTelexBlockEntity.CHANNEL_WIDTH;

    public static final Type<RadioTelexControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("radio_telex_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadioTelexControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RadioTelexControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RadioTelexControlPayload(buffer.readBlockPos(), buffer.readVarInt(),
                    buffer.readUtf(MAX_TEXT_LENGTH), buffer.readUtf(MAX_CHANNEL_LENGTH));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RadioTelexControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.action());
            buffer.writeUtf(payload.transmit(), MAX_TEXT_LENGTH);
            buffer.writeUtf(payload.receive(), MAX_CHANNEL_LENGTH);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RadioTelexControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof RadioTelexMenu menu)
                || !menu.blockPos().equals(payload.pos())) return;
        BlockEntity entity = player.level().getBlockEntity(payload.pos());
        if (!(entity instanceof RadioTelexBlockEntity telex)) return;
        switch (payload.action()) {
            case SAVE_CHANNELS -> telex.saveChannels(payload.transmit(), payload.receive());
            case SEND -> telex.sendMessage(lines(payload.transmit()));
            case DELETE_TRANSMIT -> telex.updateTransmitBuffer(List.of("", "", "", "", ""));
            case PRINT_RECEIVE -> telex.printMessage();
            case CLEAR_RECEIVE -> telex.clearReceiveBuffer();
            case UPDATE_TRANSMIT -> telex.updateTransmitBuffer(lines(payload.transmit()));
            default -> {
            }
        }
    }

    private static List<String> lines(String text) {
        String[] lines = (text == null ? "" : text).split("\\n", -1);
        java.util.ArrayList<String> result = new java.util.ArrayList<>(RadioTelexBlockEntity.LINE_COUNT);
        for (int index = 0; index < RadioTelexBlockEntity.LINE_COUNT; index++) {
            result.add(index < lines.length ? lines[index] : "");
        }
        return result;
    }
}
