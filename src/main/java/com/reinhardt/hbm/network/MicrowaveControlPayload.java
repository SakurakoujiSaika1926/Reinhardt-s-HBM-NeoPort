package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.MicrowaveBlockEntity;
import com.reinhardt.hbm.menu.MicrowaveMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MicrowaveControlPayload(BlockPos pos, int delta) implements CustomPacketPayload {
    public static final Type<MicrowaveControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("microwave_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MicrowaveControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MicrowaveControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new MicrowaveControlPayload(buffer.readBlockPos(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, MicrowaveControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.delta());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MicrowaveControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof MicrowaveMenu menu)
                || menu.blockPos() == null
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity entity = player.level().getBlockEntity(payload.pos());
        if (entity instanceof MicrowaveBlockEntity microwave) {
            microwave.changeSpeed(Integer.signum(payload.delta()));
            player.containerMenu.broadcastChanges();
        }
    }
}
