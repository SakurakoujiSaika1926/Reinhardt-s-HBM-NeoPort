package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.CompressorBlockEntity;
import com.reinhardt.hbm.menu.CompressorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CompressorControlPayload(BlockPos pos, int pressure) implements CustomPacketPayload {
    public static final Type<CompressorControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("compressor_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CompressorControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CompressorControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new CompressorControlPayload(buffer.readBlockPos(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, CompressorControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.pressure());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompressorControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof CompressorMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof CompressorBlockEntity compressor) {
            compressor.setCompression(payload.pressure());
            player.containerMenu.broadcastChanges();
        }
    }
}
