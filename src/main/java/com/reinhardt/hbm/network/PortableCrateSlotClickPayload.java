package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.integration.curios.PortableCrateCuriosIntegration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PortableCrateSlotClickPayload() implements CustomPacketPayload {
    public static final Type<PortableCrateSlotClickPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("portable_crate_slot_click"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PortableCrateSlotClickPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public PortableCrateSlotClickPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new PortableCrateSlotClickPayload();
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, PortableCrateSlotClickPayload payload) {
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PortableCrateSlotClickPayload payload, IPayloadContext context) {
        if (ModList.get().isLoaded("curios") && context.player() instanceof ServerPlayer player) {
            PortableCrateCuriosIntegration.clickMappedSlot(player);
        }
    }
}
