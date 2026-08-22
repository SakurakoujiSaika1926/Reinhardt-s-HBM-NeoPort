package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.LandmineClientEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LandmineEffectPayload(double x, double y, double z, int smokeCount, int foamCount)
        implements CustomPacketPayload {
    public static final Type<LandmineEffectPayload> TYPE = new Type<>(ReinhardtsHBM.id("landmine_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LandmineEffectPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public LandmineEffectPayload decode(RegistryFriendlyByteBuf buffer) {
            return new LandmineEffectPayload(
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, LandmineEffectPayload payload) {
            buffer.writeDouble(payload.x());
            buffer.writeDouble(payload.y());
            buffer.writeDouble(payload.z());
            buffer.writeVarInt(payload.smokeCount());
            buffer.writeVarInt(payload.foamCount());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LandmineEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> LandmineClientEffects.accept(payload));
    }
}
