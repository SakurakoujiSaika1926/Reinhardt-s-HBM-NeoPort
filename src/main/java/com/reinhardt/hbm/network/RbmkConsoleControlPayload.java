package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RbmkConsoleControlPayload(BlockPos pos, int action, int value, int[] selected) implements CustomPacketPayload {
    public static final int ACTION_SET_CONTROL = 0;
    public static final int ACTION_CYCLE_COMPRESSOR = 1;
    public static final int ACTION_AZ5 = 2;
    public static final int ACTION_ASSIGN_COLOR = 3;
    public static final int ACTION_TOGGLE_SCREEN = 4;
    public static final int ACTION_ASSIGN_SCREEN = 5;

    public static final Type<RbmkConsoleControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("rbmk_console_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RbmkConsoleControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RbmkConsoleControlPayload decode(RegistryFriendlyByteBuf buffer) {
            BlockPos pos = buffer.readBlockPos();
            int action = buffer.readVarInt();
            int value = buffer.readVarInt();
            int count = Math.min(buffer.readVarInt(), RbmkComponentBlockEntity.CONSOLE_COLUMN_COUNT);
            int[] selected = new int[count];
            for (int i = 0; i < count; i++) {
                selected[i] = buffer.readVarInt();
            }
            return new RbmkConsoleControlPayload(pos, action, value, selected);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RbmkConsoleControlPayload payload) {
            buffer.writeBlockPos(payload.pos);
            buffer.writeVarInt(payload.action);
            buffer.writeVarInt(payload.value);
            int count = Math.min(payload.selected.length, RbmkComponentBlockEntity.CONSOLE_COLUMN_COUNT);
            buffer.writeVarInt(count);
            for (int i = 0; i < count; i++) {
                buffer.writeVarInt(payload.selected[i]);
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RbmkConsoleControlPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.level().getBlockEntity(payload.pos) instanceof RbmkComponentBlockEntity rbmk
                // TileEntityRBMKConsole.hasPermission used a strict
                // 20-block radius in 1.7.10.
                // TileEntityRBMKConsole.hasPermission compared against the
                // integer block origin, while menus use the legacy +0.5
                // centre check separately.
                && player.distanceToSqr(payload.pos.getX(), payload.pos.getY(), payload.pos.getZ()) < 400.0D) {
            rbmk.applyConsoleControl(payload.action, payload.value, payload.selected);
        }
    }
}
