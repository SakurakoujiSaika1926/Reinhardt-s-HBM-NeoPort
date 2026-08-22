package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import com.reinhardt.hbm.menu.HeaterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HeatExchangerControlPayload(BlockPos pos, int amountToCool, int tickDelay) implements CustomPacketPayload {
    public static final Type<HeatExchangerControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("heatex_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HeatExchangerControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public HeatExchangerControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new HeatExchangerControlPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, HeatExchangerControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.amountToCool());
            buffer.writeVarInt(payload.tickDelay());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HeatExchangerControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof HeaterMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof HeaterBlockEntity heater && heater.kind() == HeaterBlockEntity.Kind.HEATEX) {
            heater.setHeatExchangerSettings(payload.amountToCool(), payload.tickDelay());
            player.containerMenu.broadcastChanges();
        }
    }
}
