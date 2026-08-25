package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyBobmazonEntity;
import com.reinhardt.hbm.handler.LegacyBobmazonOffers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative 1.7.10 ItemBobmazonPacket replacement. */
public record BobmazonOrderPayload(int offerIndex) implements CustomPacketPayload {
    public static final Type<BobmazonOrderPayload> TYPE = new Type<>(ReinhardtsHBM.id("bobmazon_order"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BobmazonOrderPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BobmazonOrderPayload::offerIndex, BobmazonOrderPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BobmazonOrderPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        LegacyBobmazonOffers.Offer offer = LegacyBobmazonOffers.request(player, payload.offerIndex());
        if (offer == null) {
            player.displayClientMessage(Component.literal("[BOBMAZON] Invalid catalog order.").withStyle(ChatFormatting.RED), false);
            return;
        }
        if (!offer.requirement().isMet(player)) {
            player.displayClientMessage(Component.literal("[BOBMAZON] Achievement requirement not met.").withStyle(ChatFormatting.RED), false);
            return;
        }
        if (!player.getAbilities().instabuild && !consumeCaps(player, offer.caps())) {
            player.displayClientMessage(Component.literal("[BOBMAZON] Not enough caps!").withStyle(ChatFormatting.RED), false);
            return;
        }
        LegacyBobmazonEntity courier = new LegacyBobmazonEntity(player.level(),
                player.getX() + player.getRandom().nextGaussian() * 10.0D,
                player.getZ() + player.getRandom().nextGaussian() * 10.0D, offer.stack().copy());
        player.level().addFreshEntity(courier);
        player.inventoryMenu.broadcastChanges();
    }

    private static boolean consumeCaps(ServerPlayer player, int required) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) if (LegacyBobmazonOffers.isBottleCap(stack.getItem())) total += stack.getCount();
        if (total < required) return false;
        int remaining = required;
        for (ItemStack stack : player.getInventory().items) {
            if (!LegacyBobmazonOffers.isBottleCap(stack.getItem())) continue;
            int paid = Math.min(remaining, stack.getCount());
            stack.shrink(paid);
            remaining -= paid;
            if (remaining == 0) return true;
        }
        return false;
    }
}
