package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Direct 1.7.10 ItemBDCL port, including its staged drinking sounds. */
public final class LegacyBdclItem extends Item {
    public LegacyBdclItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide && remainingUseDuration >= 10 && remainingUseDuration % 5 == 0) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(), HbmSoundEvents.PLAYER_GULP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        if (!level.isClientSide) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(), HbmSoundEvents.PLAYER_GROAN.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (!(living instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 40;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
}
