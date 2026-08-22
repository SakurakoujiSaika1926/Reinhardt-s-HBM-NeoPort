package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.ReactorControlBlockEntity;
import com.reinhardt.hbm.menu.ReactorControlMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Replaces the old NBTControlPacket with the two explicit controller actions. */
public record ReactorControlPayload(BlockPos pos, int action, int levelUpper, int levelLower, int heatUpper, int heatLower) implements CustomPacketPayload {
    public static final int ACTION_FUNCTION = 0;
    public static final int ACTION_PARAMETERS = 1;
    public static final Type<ReactorControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("reactor_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReactorControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ReactorControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ReactorControlPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ReactorControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.action());
            buffer.writeVarInt(payload.levelUpper());
            buffer.writeVarInt(payload.levelLower());
            buffer.writeVarInt(payload.heatUpper());
            buffer.writeVarInt(payload.heatLower());
        }
    };

    public static ReactorControlPayload function(BlockPos pos, int function) {
        return new ReactorControlPayload(pos, ACTION_FUNCTION, function, 0, 0, 0);
    }

    public static ReactorControlPayload parameters(BlockPos pos, int levelUpper, int levelLower, int heatUpper, int heatLower) {
        return new ReactorControlPayload(pos, ACTION_PARAMETERS, levelUpper, levelLower, heatUpper, heatLower);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReactorControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof ReactorControlMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (!(blockEntity instanceof ReactorControlBlockEntity controller)) {
            return;
        }

        if (payload.action() == ACTION_FUNCTION) {
            controller.setFunction(payload.levelUpper());
        } else if (payload.action() == ACTION_PARAMETERS) {
            controller.setParameters(payload.levelUpper(), payload.levelLower(), payload.heatUpper(), payload.heatLower());
        } else {
            return;
        }
        player.containerMenu.broadcastChanges();
    }
}
