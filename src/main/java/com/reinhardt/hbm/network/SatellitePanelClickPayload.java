package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacySatelliteToolItem;
import com.reinhardt.hbm.item.SatelliteChipItem;
import com.reinhardt.hbm.satellite.SatelliteSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A click on the 200x200 orbital targeting map. */
public record SatellitePanelClickPayload(
        int x,
        int z,
        int frequency,
        boolean offhand
) implements CustomPacketPayload {
    public static final Type<SatellitePanelClickPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("satellite_panel_click"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SatellitePanelClickPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SatellitePanelClickPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SatellitePanelClickPayload(
                    buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SatellitePanelClickPayload payload) {
            buffer.writeInt(payload.x());
            buffer.writeInt(payload.z());
            buffer.writeInt(payload.frequency());
            buffer.writeBoolean(payload.offhand());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SatellitePanelClickPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof LegacySatelliteToolItem tool)
                || !tool.isInterface()
                || SatelliteChipItem.frequency(held) != payload.frequency()) {
            return;
        }

        SatelliteSavedData data = SatelliteSavedData.get(level);
        SatelliteSavedData.SatelliteRecord record = data.satellite(payload.frequency()).orElse(null);
        if (record != null) {
            data.performPanelAction(level, record, new BlockPos(payload.x(), 0, payload.z()));
        }
    }
}
