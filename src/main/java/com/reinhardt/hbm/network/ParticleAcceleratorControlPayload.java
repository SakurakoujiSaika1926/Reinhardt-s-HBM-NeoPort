package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.ParticleAcceleratorBlockEntity;
import com.reinhardt.hbm.menu.ParticleAcceleratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ParticleAcceleratorControlPayload(BlockPos pos, int action, int value) implements CustomPacketPayload {
    public static final int CANCEL = 0;
    public static final int CYCLE_LOWER = 1;
    public static final int CYCLE_UPPER = 2;
    public static final int CYCLE_REDSTONE = 3;
    public static final int SET_THRESHOLD = 4;

    public static final Type<ParticleAcceleratorControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("particle_accelerator_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ParticleAcceleratorControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ParticleAcceleratorControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ParticleAcceleratorControlPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ParticleAcceleratorControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.action());
            buffer.writeVarInt(payload.value());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ParticleAcceleratorControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof ParticleAcceleratorMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (!(blockEntity instanceof ParticleAcceleratorBlockEntity accelerator)) {
            return;
        }
        switch (payload.action()) {
            case CANCEL -> accelerator.cancelParticle();
            case CYCLE_LOWER -> accelerator.cycleLowerDirection();
            case CYCLE_UPPER -> accelerator.cycleUpperDirection();
            case CYCLE_REDSTONE -> accelerator.cycleRedstoneDirection();
            case SET_THRESHOLD -> accelerator.setThreshold(payload.value());
            default -> {
            }
        }
        player.containerMenu.broadcastChanges();
    }
}
