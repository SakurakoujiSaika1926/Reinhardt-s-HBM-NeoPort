package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.render.TurretVisualClientEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TurretCasingEffectPayload(
        double x,
        double y,
        double z,
        double motionX,
        double motionY,
        double motionZ,
        float rotationPitch,
        float rotationYaw,
        float momentumPitch,
        float momentumYaw,
        int casingKind,
        boolean smoking,
        int smokeLife,
        double smokeLift,
        int smokeNodeLife
) implements CustomPacketPayload {
    public static final Type<TurretCasingEffectPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("turret_casing_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TurretCasingEffectPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TurretCasingEffectPayload decode(RegistryFriendlyByteBuf buffer) {
            return new TurretCasingEffectPayload(
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(), buffer.readDouble(), buffer.readVarInt()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, TurretCasingEffectPayload payload) {
            buffer.writeDouble(payload.x());
            buffer.writeDouble(payload.y());
            buffer.writeDouble(payload.z());
            buffer.writeDouble(payload.motionX());
            buffer.writeDouble(payload.motionY());
            buffer.writeDouble(payload.motionZ());
            buffer.writeFloat(payload.rotationPitch());
            buffer.writeFloat(payload.rotationYaw());
            buffer.writeFloat(payload.momentumPitch());
            buffer.writeFloat(payload.momentumYaw());
            buffer.writeVarInt(payload.casingKind());
            buffer.writeBoolean(payload.smoking());
            buffer.writeVarInt(payload.smokeLife());
            buffer.writeDouble(payload.smokeLift());
            buffer.writeVarInt(payload.smokeNodeLife());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TurretCasingEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TurretVisualClientEffects.accept(payload));
    }
}
