package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.integration.curios.PortableCrateCuriosIntegration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PortableCrateOpenPayload() implements CustomPacketPayload {
    public static final Type<PortableCrateOpenPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("open_portable_crate"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PortableCrateOpenPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public PortableCrateOpenPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new PortableCrateOpenPayload();
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, PortableCrateOpenPayload payload) {
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PortableCrateOpenPayload payload, IPayloadContext context) {
        if (ModList.get().isLoaded("curios") && context.player() instanceof ServerPlayer player) {
            PortableCrateCuriosIntegration.open(player);
        }
    }
}
