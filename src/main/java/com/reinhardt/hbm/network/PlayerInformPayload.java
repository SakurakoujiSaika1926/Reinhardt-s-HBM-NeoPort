package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.PlayerInformOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record PlayerInformPayload(int line, String message, List<String> args, boolean translated, int color, int lifetimeMillis) implements CustomPacketPayload {
    public static final Type<PlayerInformPayload> TYPE = new Type<>(ReinhardtsHBM.id("player_inform"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerInformPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PlayerInformPayload::line,
            ByteBufCodecs.STRING_UTF8,
            PlayerInformPayload::message,
            stringListCodec(),
            PlayerInformPayload::args,
            ByteBufCodecs.BOOL,
            PlayerInformPayload::translated,
            ByteBufCodecs.VAR_INT,
            PlayerInformPayload::color,
            ByteBufCodecs.VAR_INT,
            PlayerInformPayload::lifetimeMillis,
            PlayerInformPayload::new
    );

    public PlayerInformPayload(int line, String message, int color, int lifetimeMillis) {
        this(line, message, List.of(), false, color, lifetimeMillis);
    }

    public PlayerInformPayload(int line, String message, List<String> args, int color, int lifetimeMillis) {
        this(line, message, args, false, color, lifetimeMillis);
    }

    public static PlayerInformPayload translated(int line, String translationKey, int color, int lifetimeMillis, Object... args) {
        return new PlayerInformPayload(line, translationKey, stringify(args), true, color, lifetimeMillis);
    }

    private static List<String> stringify(Object[] args) {
        return java.util.Arrays.stream(args).map(String::valueOf).toList();
    }

    private static StreamCodec<RegistryFriendlyByteBuf, List<String>> stringListCodec() {
        return new StreamCodec<>() {
            @Override
            public List<String> decode(RegistryFriendlyByteBuf buffer) {
                return buffer.readList(FriendlyByteBuf::readUtf);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, List<String> value) {
                buffer.writeCollection(value, FriendlyByteBuf::writeUtf);
            }
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerInformPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PlayerInformOverlay.accept(payload));
    }
}
