package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import com.reinhardt.hbm.blockentity.ZirnoxReactorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Direct 1.7.10 ItemDyatlov target set: RBMK components and Zirnox only. */
public final class LegacyMeltdownToolItem extends Item {
    public LegacyMeltdownToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        BlockEntity clicked = level.getBlockEntity(pos);
        if (clicked instanceof MachineDummyBlockEntity dummy) {
            pos = dummy.getCorePos();
        }
        BlockEntity target = level.getBlockEntity(pos);
        if (target instanceof RbmkComponentBlockEntity rbmk) {
            rbmk.forceMeltdown(level, pos);
            return InteractionResult.CONSUME;
        }
        if (target instanceof ZirnoxReactorBlockEntity zirnox) {
            zirnox.forceOverheat();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
