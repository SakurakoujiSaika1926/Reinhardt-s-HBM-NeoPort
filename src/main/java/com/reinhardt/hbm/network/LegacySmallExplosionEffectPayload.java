package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.LegacyExplosionClientEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LegacySmallExplosionEffectPayload(
        double x,
        double y,
        double z,
        int cloudCount,
        float cloudScale,
        float cloudSpeedMultiplier
) implements CustomPacketPayload {
    public static final Type<LegacySmallExplosionEffectPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("legacy_small_explosion_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LegacySmallExplosionEffectPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public LegacySmallExplosionEffectPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new LegacySmallExplosionEffectPayload(
                            buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                            buffer.readVarInt(), buffer.readFloat(), buffer.readFloat());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, LegacySmallExplosionEffectPayload payload) {
                    buffer.writeDouble(payload.x());
                    buffer.writeDouble(payload.y());
                    buffer.writeDouble(payload.z());
                    buffer.writeVarInt(payload.cloudCount());
                    buffer.writeFloat(payload.cloudScale());
                    buffer.writeFloat(payload.cloudSpeedMultiplier());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LegacySmallExplosionEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> LegacyExplosionClientEffects.acceptSmall(payload));
    }
}
