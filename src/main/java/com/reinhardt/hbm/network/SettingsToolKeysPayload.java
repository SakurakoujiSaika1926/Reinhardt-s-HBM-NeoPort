package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.SettingsToolItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SettingsToolKeysPayload(boolean ctrl, boolean alt) implements CustomPacketPayload {
    public static final Type<SettingsToolKeysPayload> TYPE = new Type<>(ReinhardtsHBM.id("settings_tool_keys"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SettingsToolKeysPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            SettingsToolKeysPayload::ctrl,
            ByteBufCodecs.BOOL,
            SettingsToolKeysPayload::alt,
            SettingsToolKeysPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SettingsToolKeysPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            SettingsToolItem.setKeyState(player, payload.ctrl(), payload.alt());
        }
    }
}
