package com.reinhardt.hbm.item;

import com.reinhardt.hbm.satellite.SatelliteSavedData;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/** Satellite remote/interface family from ItemSatInterface and ItemSatDesignator. */
public final class LegacySatelliteToolItem extends SatelliteChipItem {
    public enum Mode { COORDINATE, LASER, INTERFACE }

    private final Mode mode;

    public LegacySatelliteToolItem(Properties properties, Mode mode) {
        super(properties.stacksTo(1), "");
        this.mode = mode;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (mode == Mode.INTERFACE) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.reinhardtshbm.satellite.interface_open", frequency(stack)), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        HitResult hit = player.pick(300.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos target = blockHit.getBlockPos().relative(blockHit.getDirection());
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            SatelliteSavedData data = SatelliteSavedData.get(serverLevel);
            SatelliteSavedData.SatelliteRecord record = data.satellite(frequency(stack)).orElse(null);
            if (record == null) {
                player.displayClientMessage(Component.translatable("message.reinhardtshbm.satellite.missing"), true);
            } else if (mode == Mode.COORDINATE) {
                if (!data.performCoordinateAction(serverLevel, (ServerPlayer) player, record, target)) {
                    player.displayClientMessage(Component.translatable("message.reinhardtshbm.satellite.no_coordinate_action"), true);
                }
            } else if (record.kind() == SatelliteSavedData.SatelliteKind.LASER) {
                data.performPanelAction(serverLevel, record, target);
                level.playSound(null, target, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
            } else {
                player.displayClientMessage(Component.translatable("message.reinhardtshbm.satellite.panel_only"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.satellite.tool." + mode.name().toLowerCase())
                .withStyle(ChatFormatting.GRAY));
    }
}
