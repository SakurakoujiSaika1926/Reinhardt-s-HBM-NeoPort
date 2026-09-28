package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.screen.SatelliteInterfaceScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative snapshot used only to open the old satellite panel. */
public record SatellitePanelOpenPayload(
        int frequency,
        String satelliteKind,
        boolean offhand
) implements CustomPacketPayload {
    public static final Type<SatellitePanelOpenPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("satellite_panel_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SatellitePanelOpenPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SatellitePanelOpenPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SatellitePanelOpenPayload(
                    buffer.readInt(),
                    buffer.readUtf(32),
                    buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SatellitePanelOpenPayload payload) {
            buffer.writeInt(payload.frequency());
            buffer.writeUtf(payload.satelliteKind(), 32);
            buffer.writeBoolean(payload.offhand());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SatellitePanelOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SatelliteInterfaceScreen.open(
                payload.frequency(), payload.satelliteKind(), payload.offhand()));
    }
}
