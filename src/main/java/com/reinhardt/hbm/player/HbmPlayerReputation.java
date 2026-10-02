package com.reinhardt.hbm.player;

import com.reinhardt.hbm.registry.HbmDataAttachments;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Persistent replacement for the lantern reputation field in HbmPlayerProps. */
public final class HbmPlayerReputation {
    public static final int MIN = -25;
    public static final int MAX = 25;

    private HbmPlayerReputation() {
    }

    public static int get(Player player) {
        return player.getData(HbmDataAttachments.PLAYER_REPUTATION);
    }

    public static void set(Player player, int value) {
        player.setData(HbmDataAttachments.PLAYER_REPUTATION, Mth.clamp(value, MIN, MAX));
    }

    public static void add(Player player, int delta) {
        set(player, get(player) + delta);
    }
}
