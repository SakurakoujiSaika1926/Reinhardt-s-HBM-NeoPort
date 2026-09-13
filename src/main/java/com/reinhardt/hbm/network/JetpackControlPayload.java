package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyJetpackItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client jump-key state used by the legacy fuelled jetpacks. */
public record JetpackControlPayload(boolean active) implements CustomPacketPayload {
    public static final Type<JetpackControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("jetpack_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JetpackControlPayload> STREAM_CODEC =
            StreamCodec.composite(net.minecraft.network.codec.ByteBufCodecs.BOOL,
                    JetpackControlPayload::active, JetpackControlPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(JetpackControlPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            LegacyJetpackItem.setInputActive(player, payload.active());
        }
    }
}
