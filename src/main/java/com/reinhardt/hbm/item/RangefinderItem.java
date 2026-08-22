package com.reinhardt.hbm.item;

import com.reinhardt.hbm.network.PlayerInformPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class RangefinderItem extends Item {
    public RangefinderItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            HitResult hit = player.pick(300.0D, 1.0F, false);
            if (hit instanceof BlockHitResult && hit.getType() == HitResult.Type.BLOCK) {
                Vec3 eye = player.getEyePosition(1.0F);
                double distance = Math.floor(eye.distanceTo(hit.getLocation()) * 10.0D) / 10.0D;
                PacketDistributor.sendToPlayer(serverPlayer, new PlayerInformPayload(8, distance + "m", 0xFFFFFF, 5_000));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
