package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.menu.AnnihilatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative pool-name edit used by the old annihilator text field. */
public record AnnihilatorControlPayload(BlockPos pos, String pool) implements CustomPacketPayload {
    public static final Type<AnnihilatorControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("annihilator_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnnihilatorControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AnnihilatorControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new AnnihilatorControlPayload(buffer.readBlockPos(), buffer.readUtf(20));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, AnnihilatorControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeUtf(payload.pool(), 20);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AnnihilatorControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof AnnihilatorMenu menu)
                || !menu.isAnnihilator()
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof LegacyMachineBlockEntity machine
                && machine.machineId().equals("machine_annihilator")) {
            machine.setAnnihilatorPool(payload.pool());
            player.containerMenu.broadcastChanges();
        }
    }
}
