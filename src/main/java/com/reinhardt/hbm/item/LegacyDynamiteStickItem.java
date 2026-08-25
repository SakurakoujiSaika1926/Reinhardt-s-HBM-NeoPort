package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.LegacyGrenadeEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Direct 1.7.10 ItemGrenadeDynamite and ItemGrenadeFishing port. */
public final class LegacyDynamiteStickItem extends Item {
    private final LegacyGrenadeEntity.Kind kind;

    public LegacyDynamiteStickItem(Properties properties, LegacyGrenadeEntity.Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 0.5F, 0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            level.addFreshEntity(new LegacyGrenadeEntity(level, player, kind));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
