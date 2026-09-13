package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.ArmorFSBItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** One-shot server-authoritative trigger for the 1.7.10 bismuth armour dash. */
public record ArmorDashPayload() implements CustomPacketPayload {
    public static final Type<ArmorDashPayload> TYPE = new Type<>(ReinhardtsHBM.id("armor_dash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorDashPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ArmorDashPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ArmorDashPayload();
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ArmorDashPayload payload) {
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ArmorDashPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ArmorFSBItem.performDash(player);
        }
    }
}
