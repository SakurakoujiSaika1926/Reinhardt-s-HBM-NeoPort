package com.reinhardt.hbm.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.context.UseOnContext;

/** 1.7.10 HBM music record backed by the modern jukebox song component. */
public class HbmRecordItem extends Item {
    public HbmRecordItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        ItemInteractionResult result = JukeboxPlayable.tryInsertIntoJukebox(
                context.getLevel(), context.getClickedPos(), context.getItemInHand(), context.getPlayer()
        );
        return result.consumesAction() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
