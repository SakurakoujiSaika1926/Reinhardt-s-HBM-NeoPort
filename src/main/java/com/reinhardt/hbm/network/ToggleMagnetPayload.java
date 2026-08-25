package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.player.HbmPlayerArmorState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative packet for 1.7.10's Z-key magnet toggle. */
public record ToggleMagnetPayload() implements CustomPacketPayload {
    public static final Type<ToggleMagnetPayload> TYPE = new Type<>(ReinhardtsHBM.id("toggle_magnet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMagnetPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ToggleMagnetPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ToggleMagnetPayload();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ToggleMagnetPayload payload) {
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleMagnetPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            boolean enabled = HbmPlayerArmorState.toggleMagnet(player);
            player.displayClientMessage(Component.translatable(
                    enabled ? "message.reinhardtshbm.magnet_on" : "message.reinhardtshbm.magnet_off"
            ).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        }
    }
}
