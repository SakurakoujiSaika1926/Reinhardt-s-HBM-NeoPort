package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.menu.BatteryReddMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.math.BigInteger;

public record BatteryReddSyncPayload(
        BlockPos pos,
        String power,
        String delta,
        int redLow,
        int redHigh,
        int priority
) implements CustomPacketPayload {
    public static final Type<BatteryReddSyncPayload> TYPE = new Type<>(ReinhardtsHBM.id("battery_redd_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BatteryReddSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BatteryReddSyncPayload decode(RegistryFriendlyByteBuf buffer) {
            return new BatteryReddSyncPayload(
                    buffer.readBlockPos(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, BatteryReddSyncPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeUtf(payload.power());
            buffer.writeUtf(payload.delta());
            buffer.writeVarInt(payload.redLow());
            buffer.writeVarInt(payload.redHigh());
            buffer.writeVarInt(payload.priority());
        }
    };

    public static BatteryReddSyncPayload of(BlockPos pos, BigInteger power, BigInteger delta, int redLow, int redHigh, PowerEndpoint.ConnectionPriority priority) {
        return new BatteryReddSyncPayload(pos, power.toString(), delta.toString(), redLow, redHigh, priority.ordinal());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BatteryReddSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof BatteryReddMenu menu) {
                menu.acceptSync(payload);
            }
        });
    }
}
