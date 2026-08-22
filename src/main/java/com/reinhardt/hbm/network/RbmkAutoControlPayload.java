package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RbmkAutoControlPayload(
        BlockPos pos,
        int function,
        int levelUpper,
        int levelLower,
        int heatUpper,
        int heatLower
) implements CustomPacketPayload {
    public static final Type<RbmkAutoControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("rbmk_auto_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RbmkAutoControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RbmkAutoControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RbmkAutoControlPayload(
                    buffer.readBlockPos(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RbmkAutoControlPayload payload) {
            buffer.writeBlockPos(payload.pos);
            buffer.writeVarInt(payload.function);
            buffer.writeVarInt(payload.levelUpper);
            buffer.writeVarInt(payload.levelLower);
            buffer.writeVarInt(payload.heatUpper);
            buffer.writeVarInt(payload.heatLower);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RbmkAutoControlPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.level().getBlockEntity(payload.pos) instanceof RbmkComponentBlockEntity rbmk) {
            rbmk.applyAutoControl(
                    payload.function,
                    payload.levelUpper,
                    payload.levelLower,
                    payload.heatUpper,
                    payload.heatLower
            );
        }
    }
}
