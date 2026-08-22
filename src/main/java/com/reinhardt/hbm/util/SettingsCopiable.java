package com.reinhardt.hbm.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public interface SettingsCopiable {
    CompoundTag getSettings(Level level, BlockPos pos);

    void pasteSettings(CompoundTag settings, int index, Level level, Player player, BlockPos pos);

    default String getSettingsSourceId(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock().getDescriptionId();
    }

    default List<Component> settingsInfo(Level level, BlockPos pos, CompoundTag settings) {
        return List.of();
    }
}
