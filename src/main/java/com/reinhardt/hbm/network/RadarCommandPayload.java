package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.menu.RadarMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The 1.7.10 radar GUI's 1-8 target dispatch, validated on the server. */
public record RadarCommandPayload(BlockPos pos, int relaySlot, int targetEntityId, int targetX, int targetZ)
        implements CustomPacketPayload {
    public static final Type<RadarCommandPayload> TYPE = new Type<>(ReinhardtsHBM.id("radar_command"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadarCommandPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RadarCommandPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RadarCommandPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RadarCommandPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.relaySlot());
            buffer.writeVarInt(payload.targetEntityId());
            buffer.writeVarInt(payload.targetX());
            buffer.writeVarInt(payload.targetZ());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RadarCommandPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof RadarMenu menu)) {
            return;
        }
        BlockEntity entity = player.level().getBlockEntity(payload.pos());
        if (!(entity instanceof LegacyMachineBlockEntity machine)
                || (!machine.machineId().equals("machine_radar") && !machine.machineId().equals("machine_radar_large"))
                || !menu.stillValid(player)) {
            return;
        }
        machine.issueRadarCommand(player, payload.relaySlot(), payload.targetEntityId(), payload.targetX(), payload.targetZ());
    }
}
