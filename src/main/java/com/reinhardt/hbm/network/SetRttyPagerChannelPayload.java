package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyRttyPagerItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative channel update for the hand-held RTTY pager. */
public record SetRttyPagerChannelPayload(boolean offHand, String channel) implements CustomPacketPayload {
    public static final Type<SetRttyPagerChannelPayload> TYPE = new Type<>(ReinhardtsHBM.id("set_rtty_pager_channel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetRttyPagerChannelPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            SetRttyPagerChannelPayload::offHand,
            ByteBufCodecs.stringUtf8(LegacyRttyPagerItem.MAX_CHANNEL_LENGTH),
            SetRttyPagerChannelPayload::channel,
            SetRttyPagerChannelPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetRttyPagerChannelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            InteractionHand hand = payload.offHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof LegacyRttyPagerItem) {
                LegacyRttyPagerItem.setChannel(stack, payload.channel());
                player.getInventory().setChanged();
            }
        });
    }
}
