package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.MaxwellGibClientEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MaxwellGibEffectPayload(
        int entityId,
        double x,
        double y,
        double z,
        float width,
        float height,
        int gibType
) implements CustomPacketPayload {
    public static final Type<MaxwellGibEffectPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("maxwell_gib_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaxwellGibEffectPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MaxwellGibEffectPayload decode(RegistryFriendlyByteBuf buffer) {
            return new MaxwellGibEffectPayload(
                    buffer.readVarInt(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readVarInt()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, MaxwellGibEffectPayload payload) {
            buffer.writeVarInt(payload.entityId());
            buffer.writeDouble(payload.x());
            buffer.writeDouble(payload.y());
            buffer.writeDouble(payload.z());
            buffer.writeFloat(payload.width());
            buffer.writeFloat(payload.height());
            buffer.writeVarInt(payload.gibType());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MaxwellGibEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> MaxwellGibClientEffects.accept(payload));
    }
}
