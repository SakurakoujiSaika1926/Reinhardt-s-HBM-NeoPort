package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.MachineMuffler;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MufflerUpgradeItem extends MachineUpgradeItem {
    public MufflerUpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockEntity machine = MachineMuffler.resolveCore(context.getLevel(), context.getClickedPos());
        if (!MachineMuffler.install(machine)) {
            return InteractionResult.PASS;
        }
        context.getLevel().playSound(null, context.getClickedPos(), HbmSoundEvents.UPGRADE_PLUG.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        ItemStack stack = context.getItemInHand();
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
