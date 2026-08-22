package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.TurretJeremyBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TurretJeremyControlPayload(BlockPos pos, int action, int index, String name) implements CustomPacketPayload {
    public static final Type<TurretJeremyControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("turret_jeremy_control"));
    public static final StreamCodec<ByteBuf, TurretJeremyControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TurretJeremyControlPayload::pos,
            ByteBufCodecs.VAR_INT,
            TurretJeremyControlPayload::action,
            ByteBufCodecs.VAR_INT,
            TurretJeremyControlPayload::index,
            ByteBufCodecs.STRING_UTF8,
            TurretJeremyControlPayload::name,
            TurretJeremyControlPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TurretJeremyControlPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.level().getBlockEntity(payload.pos) instanceof TurretJeremyBlockEntity turret
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
