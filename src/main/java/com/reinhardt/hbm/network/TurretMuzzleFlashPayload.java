package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.render.TurretVisualClientEffects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TurretMuzzleFlashPayload(double x, double y, double z, float size, int count) implements CustomPacketPayload {
    public static final Type<TurretMuzzleFlashPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("turret_muzzle_flash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TurretMuzzleFlashPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TurretMuzzleFlashPayload decode(RegistryFriendlyByteBuf buffer) {
            return new TurretMuzzleFlashPayload(
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readFloat(), buffer.readVarInt()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, TurretMuzzleFlashPayload payload) {
            buffer.writeDouble(payload.x());
            buffer.writeDouble(payload.y());
            buffer.writeDouble(payload.z());
            buffer.writeFloat(payload.size());
            buffer.writeVarInt(payload.count());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TurretMuzzleFlashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TurretVisualClientEffects.accept(payload));
    }
}
