package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.LegacyExplosionClientEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LegacyExplosionEffectPayload(
        double x,
        double y,
        double z,
        int cloudCount,
        float cloudScale,
        float cloudSpeedMultiplier,
        float waveScale,
        int debrisCount,
        int debrisSize,
        int debrisRetry,
        float debrisVelocity,
        float debrisHorizontalDeviation,
        float debrisVerticalOffset,
        float soundRange
) implements CustomPacketPayload {
    public static final Type<LegacyExplosionEffectPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("legacy_explosion_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LegacyExplosionEffectPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public LegacyExplosionEffectPayload decode(RegistryFriendlyByteBuf buffer) {
            return new LegacyExplosionEffectPayload(
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readVarInt(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, LegacyExplosionEffectPayload payload) {
            buffer.writeDouble(payload.x());
            buffer.writeDouble(payload.y());
            buffer.writeDouble(payload.z());
            buffer.writeVarInt(payload.cloudCount());
            buffer.writeFloat(payload.cloudScale());
            buffer.writeFloat(payload.cloudSpeedMultiplier());
            buffer.writeFloat(payload.waveScale());
            buffer.writeVarInt(payload.debrisCount());
            buffer.writeVarInt(payload.debrisSize());
            buffer.writeVarInt(payload.debrisRetry());
            buffer.writeFloat(payload.debrisVelocity());
            buffer.writeFloat(payload.debrisHorizontalDeviation());
            buffer.writeFloat(payload.debrisVerticalOffset());
            buffer.writeFloat(payload.soundRange());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LegacyExplosionEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> LegacyExplosionClientEffects.accept(payload));
    }
}
