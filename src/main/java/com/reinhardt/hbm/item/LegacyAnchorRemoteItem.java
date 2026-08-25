package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;

/** Exact gameplay port of 1.7.10 ItemAnchorRemote and MachineTeleanchor. */
public final class LegacyAnchorRemoteItem extends FixedBatteryItem {
    private static final long CAPACITY = 1_000_000L;
    private static final long TELEPORT_COST = 10_000L;
    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";
    private static final String Z_KEY = "z";

    public LegacyAnchorRemoteItem(Item.Properties properties) {
        super(properties, CAPACITY, 10_000L, 0L);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().getBlockState(context.getClickedPos()).is(HbmBlocks.TELEANCHOR.get())) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide) {
            ItemStack stack = context.getItemInHand();
            CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            data.putInt(X_KEY, context.getClickedPos().getX());
            data.putInt(Y_KEY, context.getClickedPos().getY());
            data.putInt(Z_KEY, context.getClickedPos().getZ());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.isEmpty() || hbmCharge(stack) < TELEPORT_COST) {
            playFailure(level, player);
            return InteractionResultHolder.consume(stack);
        }

        BlockPos anchor = new BlockPos(data.getInt(X_KEY), data.getInt(Y_KEY), data.getInt(Z_KEY));
        // The old implementation force-loaded the target chunk before checking it.
        level.getChunk(anchor);
        if (!level.getBlockState(anchor).is(HbmBlocks.TELEANCHOR.get())) {
            playFailure(level, player);
            return InteractionResultHolder.consume(stack);
        }

        player.stopRiding();
        level.explode(player, anchor.getX() + 0.5D, anchor.getY() + 1.0D + player.getBbHeight() * 0.5D,
                anchor.getZ() + 0.5D, 2.0F, false, ExplosionInteraction.NONE);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.teleportTo(anchor.getX() + 0.5D, anchor.getY() + 1.0D, anchor.getZ() + 0.5D);
        player.resetFallDistance();
        hbmSetCharge(stack, hbmCharge(stack) - TELEPORT_COST);
        return InteractionResultHolder.consume(stack);
    }

    private static void playFailure(Level level, Player player) {
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 0.75F);
    }
}
