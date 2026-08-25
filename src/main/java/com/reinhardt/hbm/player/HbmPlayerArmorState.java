package com.reinhardt.hbm.player;

import com.mojang.serialization.Codec;
import com.reinhardt.hbm.registry.HbmDataAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/** Persisted replacement for the magnet toggle in 1.7.10 HbmPlayerProps. */
public final class HbmPlayerArmorState {
    public static final Codec<HbmPlayerArmorState> CODEC = Codec.BOOL.xmap(HbmPlayerArmorState::new, HbmPlayerArmorState::magnetActive);
    public static final StreamCodec<RegistryFriendlyByteBuf, HbmPlayerArmorState> STREAM_CODEC = StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.BOOL,
            HbmPlayerArmorState::magnetActive,
            HbmPlayerArmorState::new
    );

    private boolean magnetActive;

    public HbmPlayerArmorState() {
        this(true);
    }

    private HbmPlayerArmorState(boolean magnetActive) {
        this.magnetActive = magnetActive;
    }

    public static boolean isMagnetActive(Player player) {
        return player.getData(HbmDataAttachments.PLAYER_ARMOR_STATE).magnetActive;
    }

    public static boolean toggleMagnet(Player player) {
        HbmPlayerArmorState state = player.getData(HbmDataAttachments.PLAYER_ARMOR_STATE);
        state.magnetActive = !state.magnetActive;
        player.setData(HbmDataAttachments.PLAYER_ARMOR_STATE, state);
        return state.magnetActive;
    }

    public boolean magnetActive() {
        return this.magnetActive;
    }
}
