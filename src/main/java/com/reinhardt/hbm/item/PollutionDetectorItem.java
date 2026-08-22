package com.reinhardt.hbm.item;

import com.reinhardt.hbm.network.PlayerInformPayload;
import com.reinhardt.hbm.pollution.HbmPollutionData;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class PollutionDetectorItem extends Item {
    public PollutionDetectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel) || level.getGameTime() % 10L != 0L) {
            return;
        }
        HbmPollutionData.PollutionValues values = HbmPollutionData.get(serverLevel).get(player.blockPosition());
        PacketDistributor.sendToPlayer(player, PlayerInformPayload.translated(100, "message.reinhardtshbm.pollution.soot", 0xFFFF55, 4000, round(values.get(HbmPollutionType.SOOT))));
        PacketDistributor.sendToPlayer(player, PlayerInformPayload.translated(101, "message.reinhardtshbm.pollution.poison", 0xFFFF55, 4000, round(values.get(HbmPollutionType.POISON))));
        PacketDistributor.sendToPlayer(player, PlayerInformPayload.translated(102, "message.reinhardtshbm.pollution.heavy_metal", 0xFFFF55, 4000, round(values.get(HbmPollutionType.HEAVYMETAL))));
    }

    private static double round(double value) {
        return Math.floor(value * 100.0D) / 100.0D;
    }
}
