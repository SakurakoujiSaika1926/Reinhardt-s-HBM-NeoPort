package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.PollutionFogOverlay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PollutionSyncPayload(int soot, int poison, int heavyMetal, int fallout) implements CustomPacketPayload {
    public static final Type<PollutionSyncPayload> TYPE = new Type<>(ReinhardtsHBM.id("pollution_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PollutionSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PollutionSyncPayload::soot,
            ByteBufCodecs.VAR_INT,
            PollutionSyncPayload::poison,
            ByteBufCodecs.VAR_INT,
            PollutionSyncPayload::heavyMetal,
            ByteBufCodecs.VAR_INT,
            PollutionSyncPayload::fallout,
            PollutionSyncPayload::new
    );

    public static PollutionSyncPayload scaled(double soot, double poison, double heavyMetal, double fallout) {
        return new PollutionSyncPayload(scale(soot), scale(poison), scale(heavyMetal), scale(fallout));
    }

    private static int scale(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.round(value * 1000.0D));
    }

    public double sootValue() {
        return soot / 1000.0D;
    }

    public double poisonValue() {
        return poison / 1000.0D;
    }

    public double heavyMetalValue() {
        return heavyMetal / 1000.0D;
    }

    public double falloutValue() {
        return fallout / 1000.0D;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PollutionSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PollutionFogOverlay.accept(payload));
    }
}
