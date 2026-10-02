package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.TomImpactClientEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TomImpactSyncPayload(float dust, float fire, boolean impact) implements CustomPacketPayload {
    public static final Type<TomImpactSyncPayload> TYPE = new Type<>(ReinhardtsHBM.id("tom_impact_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TomImpactSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public TomImpactSyncPayload decode(RegistryFriendlyByteBuf buffer) {
            return new TomImpactSyncPayload(buffer.readFloat(), buffer.readFloat(), buffer.readBoolean());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, TomImpactSyncPayload payload) {
            buffer.writeFloat(payload.dust());
            buffer.writeFloat(payload.fire());
            buffer.writeBoolean(payload.impact());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(TomImpactSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TomImpactClientEffects.accept(payload));
    }
}
