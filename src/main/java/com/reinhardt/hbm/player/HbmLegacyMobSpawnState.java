package com.reinhardt.hbm.player;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;

/**
 * Values stored in EntityPlayer.PERSISTED_NBT_TAG by HBM 1.7.10's mob-spawn
 * systems. They deliberately stay on the player through death.
 */
public record HbmLegacyMobSpawnState(long fbiMarkUntil, boolean radMarked, boolean hasDucked) {
    public static final HbmLegacyMobSpawnState EMPTY = new HbmLegacyMobSpawnState(0L, false, false);

    public static final Codec<HbmLegacyMobSpawnState> CODEC = CompoundTag.CODEC.xmap(
            tag -> new HbmLegacyMobSpawnState(
                    tag.getLong("fbiMark"),
                    tag.getBoolean("radMark"),
                    tag.getBoolean("hasDucked")
            ),
            state -> {
                CompoundTag tag = new CompoundTag();
                tag.putLong("fbiMark", state.fbiMarkUntil);
                tag.putBoolean("radMark", state.radMarked);
                tag.putBoolean("hasDucked", state.hasDucked);
                return tag;
            }
    );
}
