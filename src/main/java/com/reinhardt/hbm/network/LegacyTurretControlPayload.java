package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LegacyTurretBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LegacyTurretControlPayload(BlockPos pos, int action, int index, String name) implements CustomPacketPayload {
    public static final Type<LegacyTurretControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("legacy_turret_control"));
    public static final StreamCodec<ByteBuf, LegacyTurretControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            LegacyTurretControlPayload::pos,
            ByteBufCodecs.VAR_INT,
            LegacyTurretControlPayload::action,
            ByteBufCodecs.VAR_INT,
            LegacyTurretControlPayload::index,
            ByteBufCodecs.STRING_UTF8,
            LegacyTurretControlPayload::name,
            LegacyTurretControlPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LegacyTurretControlPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.level().getBlockEntity(payload.pos) instanceof LegacyTurretBlockEntity turret
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
