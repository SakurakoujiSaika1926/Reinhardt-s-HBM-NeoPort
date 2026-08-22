package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetFluidIdentifierPayload(String fluidName, boolean primary) implements CustomPacketPayload {
    public static final Type<SetFluidIdentifierPayload> TYPE = new Type<>(ReinhardtsHBM.id("set_fluid_identifier"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetFluidIdentifierPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SetFluidIdentifierPayload::fluidName,
            ByteBufCodecs.BOOL,
            SetFluidIdentifierPayload::primary,
            SetFluidIdentifierPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetFluidIdentifierPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            stack = player.getOffhandItem();
        }
        if (!(stack.getItem() instanceof FluidIdentifierItem)) {
            return;
        }

        HbmFluidDefinition fluid = HbmFluids.byName(payload.fluidName()).orElse(HbmFluids.none());
        if (!fluid.isNone() && !fluid.allowsFluidIdentifier()) {
            return;
        }
        FluidIdentifierItem.setType(stack, fluid, payload.primary());
        player.containerMenu.broadcastChanges();
    }
}
