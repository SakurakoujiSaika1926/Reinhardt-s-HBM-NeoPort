package com.reinhardt.hbm.util;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public interface FluidCopiable extends SettingsCopiable {
    String FLUID_ID = "fluidID";

    int[] getFluidIdsToCopy();

    void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos);

    @Override
    default CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        int[] ids = getFluidIdsToCopy();
        if (ids.length > 0) {
            tag.putIntArray(FLUID_ID, ids);
        }
        return tag;
    }

    @Override
    default void pasteSettings(CompoundTag settings, int index, Level level, Player player, BlockPos pos) {
        int[] ids = settings.getIntArray(FLUID_ID);
        if (ids.length == 0) {
            return;
        }
        int id = index >= 0 && index < ids.length ? ids[index] : 0;
        HbmFluidDefinition fluid = HbmFluids.byOldId(id).orElse(HbmFluids.none());
        pasteFluidSetting(fluid, level, player, pos);
    }

    static List<Component> fluidInfo(CompoundTag settings) {
        int[] ids = settings.getIntArray(FLUID_ID);
        List<Component> info = new ArrayList<>(ids.length);
        for (int id : ids) {
            HbmFluidDefinition fluid = HbmFluids.byOldId(id).orElse(HbmFluids.none());
            info.add(Component.translatable(fluid.translationKey()));
        }
        return info;
    }
}
