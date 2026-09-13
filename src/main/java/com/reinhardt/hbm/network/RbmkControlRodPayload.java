package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RbmkControlRodPayload(BlockPos pos, int action, int value) implements CustomPacketPayload {
    public static final int ACTION_SET_LEVEL = 0;
    public static final int ACTION_ASSIGN_COLOR = 1;

    public static final Type<RbmkControlRodPayload> TYPE = new Type<>(ReinhardtsHBM.id("rbmk_control_rod"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RbmkControlRodPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RbmkControlRodPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RbmkControlRodPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RbmkControlRodPayload payload) {
            buffer.writeBlockPos(payload.pos);
            buffer.writeVarInt(payload.action);
            buffer.writeVarInt(payload.value);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RbmkControlRodPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && player.level().getBlockEntity(payload.pos) instanceof RbmkComponentBlockEntity rbmk
                // TileEntityRBMKControlManual.hasPermission used a strict
                // 20-block radius in 1.7.10.
                // The legacy TileEntityRBMKControlManual.hasPermission used
                // the integer block origin (xCoord/yCoord/zCoord), not the
                // block centre used by menu validity checks.
                && player.distanceToSqr(payload.pos.getX(), payload.pos.getY(), payload.pos.getZ()) < 400.0D) {
            if (payload.action == ACTION_SET_LEVEL) {
                rbmk.applyManualControlLevel(payload.value);
            } else if (payload.action == ACTION_ASSIGN_COLOR) {
                rbmk.applyManualControlColor(payload.value);
            }
        }
    }
}
