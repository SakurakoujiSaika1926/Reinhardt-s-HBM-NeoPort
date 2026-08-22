package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RbmkLidItem extends Item {
    private final RbmkComponentBlockEntity.LidType lidType;

    public RbmkLidItem(Properties properties, RbmkComponentBlockEntity.LidType lidType) {
        super(properties);
        this.lidType = lidType;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOn(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        RbmkComponentBlockEntity rbmk = resolveRbmk(level, context.getClickedPos());
        if (rbmk == null || !rbmk.canUseLid() || rbmk.hasLid()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            rbmk.installLid(lidType);
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            level.playSound(
                    null,
                    rbmk.getBlockPos(),
                    lidType == RbmkComponentBlockEntity.LidType.GLASS ? SoundEvents.GLASS_PLACE : SoundEvents.STONE_PLACE,
                    SoundSource.BLOCKS,
                    0.75F,
                    0.8F
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static RbmkComponentBlockEntity resolveRbmk(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RbmkComponentBlockEntity rbmk) {
            return rbmk;
        }
        if (blockEntity instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof RbmkComponentBlockEntity rbmk) {
            return rbmk;
        }
        return null;
    }
}
