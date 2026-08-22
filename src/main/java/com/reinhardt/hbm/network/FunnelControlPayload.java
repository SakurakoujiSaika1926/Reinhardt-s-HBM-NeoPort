package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.FunnelBlockEntity;
import com.reinhardt.hbm.menu.FunnelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FunnelControlPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<FunnelControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("funnel_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FunnelControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FunnelControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new FunnelControlPayload(buffer.readBlockPos());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FunnelControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FunnelControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof FunnelMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof FunnelBlockEntity funnel) {
            funnel.cycleMode();
            player.containerMenu.broadcastChanges();
        }
    }
}
