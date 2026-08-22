package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.menu.RadarMenu;
import com.reinhardt.hbm.menu.RadarSlotsMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative controls copied from GUIMachineRadarNT. */
public record RadarControlPayload(BlockPos pos, int action) implements CustomPacketPayload {
    public static final Type<RadarControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("radar_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadarControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public RadarControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RadarControlPayload(buffer.readBlockPos(), buffer.readVarInt());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, RadarControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.action());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RadarControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || (!(player.containerMenu instanceof RadarMenu menu) && !(player.containerMenu instanceof RadarSlotsMenu))) return;
        BlockEntity entity = player.level().getBlockEntity(payload.pos());
        if (!(entity instanceof LegacyMachineBlockEntity machine)
                || (!machine.machineId().equals("machine_radar") && !machine.machineId().equals("machine_radar_large"))
                || !player.containerMenu.stillValid(player)) return;
        if (payload.action() == 7 && player.containerMenu instanceof RadarMenu) {
            machine.openRadarSlots(player);
        } else if (payload.action() >= 0 && payload.action() <= 6) {
            machine.applyRadarControl(payload.action());
            player.containerMenu.broadcastChanges();
        }
    }
}
