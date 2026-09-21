package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.PressAnimationClientEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Compact server-authoritative synchronization for the press head. The 1.7.10
 * machine sent its changing press value every tick; keeping this separate from
 * the full block-entity tag preserves that timing without resending thirteen
 * inventory slots on every animation frame.
 */
public record PressAnimationPayload(
        BlockPos pos,
        int progress,
        int speed,
        int delay,
        boolean retracting,
        boolean impact
) implements CustomPacketPayload {
    private static final double TRACKING_RANGE_SQR = 50.0D * 50.0D;

    public static final Type<PressAnimationPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("press_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PressAnimationPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PressAnimationPayload decode(RegistryFriendlyByteBuf buffer) {
            BlockPos pos = buffer.readBlockPos();
            int progress = buffer.readVarInt();
            int speed = buffer.readVarInt();
            int delay = buffer.readVarInt();
            int flags = buffer.readUnsignedByte();
            return new PressAnimationPayload(pos, progress, speed, delay, (flags & 1) != 0, (flags & 2) != 0);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PressAnimationPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.progress());
            buffer.writeVarInt(payload.speed());
            buffer.writeVarInt(payload.delay());
            buffer.writeByte((payload.retracting() ? 1 : 0) | (payload.impact() ? 2 : 0));
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendToTracking(
            Level level,
            BlockPos pos,
            int progress,
            int speed,
            int delay,
            boolean retracting,
            boolean impact
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        PressAnimationPayload payload = new PressAnimationPayload(pos, progress, speed, delay, retracting, impact);
        double centerX = pos.getX() + 0.5D;
        double centerY = pos.getY() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(centerX, centerY, centerZ) <= TRACKING_RANGE_SQR) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static void handle(PressAnimationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PressAnimationClientEffects.accept(payload));
    }
}
