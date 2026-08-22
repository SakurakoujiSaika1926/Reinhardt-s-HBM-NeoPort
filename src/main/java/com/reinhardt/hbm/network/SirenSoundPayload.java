package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.sound.SirenClientSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SirenSoundPayload(BlockPos pos, int trackId, boolean active) implements CustomPacketPayload {
    public static final Type<SirenSoundPayload> TYPE = new Type<>(ReinhardtsHBM.id("siren_sound"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SirenSoundPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SirenSoundPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SirenSoundPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SirenSoundPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.trackId());
            buffer.writeBoolean(payload.active());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SirenSoundPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SirenClientSounds.accept(payload));
    }
}
