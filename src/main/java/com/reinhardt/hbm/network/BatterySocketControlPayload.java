package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.BatterySocketBlockEntity;
import com.reinhardt.hbm.menu.BatterySocketMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BatterySocketControlPayload(BlockPos pos, int action) implements CustomPacketPayload {
    public static final int ACTION_LOW = 0;
    public static final int ACTION_HIGH = 1;
    public static final int ACTION_PRIORITY = 2;
    public static final Type<BatterySocketControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("battery_socket_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BatterySocketControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BatterySocketControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new BatterySocketControlPayload(buffer.readBlockPos(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, BatterySocketControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.action());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BatterySocketControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof BatterySocketMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (!(blockEntity instanceof BatterySocketBlockEntity socket)) {
            return;
        }
        switch (payload.action()) {
            case ACTION_LOW -> socket.cycleLowMode();
            case ACTION_HIGH -> socket.cycleHighMode();
            case ACTION_PRIORITY -> socket.cyclePriority();
            default -> {
            }
        }
        player.containerMenu.broadcastChanges();
    }
}
