package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.ZirnoxReactorBlockEntity;
import com.reinhardt.hbm.menu.ZirnoxReactorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ZirnoxControlPayload(BlockPos pos, int action) implements CustomPacketPayload {
    public static final int ACTION_TOGGLE = 0;
    public static final int ACTION_VENT = 1;
    public static final Type<ZirnoxControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("zirnox_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ZirnoxControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ZirnoxControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ZirnoxControlPayload(buffer.readBlockPos(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ZirnoxControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.action());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ZirnoxControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof ZirnoxReactorMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (!(blockEntity instanceof ZirnoxReactorBlockEntity reactor)) {
            return;
        }
        if (payload.action() == ACTION_TOGGLE) {
            reactor.toggleActive();
        } else if (payload.action() == ACTION_VENT) {
            reactor.ventCarbonDioxide();
        }
        player.containerMenu.broadcastChanges();
    }
}
