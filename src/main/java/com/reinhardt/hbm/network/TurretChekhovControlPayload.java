package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.TurretChekhovBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TurretChekhovControlPayload(BlockPos pos, int action, int index, String name) implements CustomPacketPayload {
    public static final Type<TurretChekhovControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("turret_chekhov_control"));
    public static final StreamCodec<ByteBuf, TurretChekhovControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TurretChekhovControlPayload::pos,
            ByteBufCodecs.VAR_INT,
            TurretChekhovControlPayload::action,
            ByteBufCodecs.VAR_INT,
            TurretChekhovControlPayload::index,
            ByteBufCodecs.STRING_UTF8,
            TurretChekhovControlPayload::name,
            TurretChekhovControlPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TurretChekhovControlPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.level().getBlockEntity(payload.pos) instanceof TurretChekhovBlockEntity turret
                    && turret.stillValid(player)) {
                if (payload.action == 0) {
                    turret.addWhitelistName(payload.name);
                } else if (payload.action == 1) {
                    turret.removeWhitelistName(payload.index);
                }
            }
        });
    }
}
