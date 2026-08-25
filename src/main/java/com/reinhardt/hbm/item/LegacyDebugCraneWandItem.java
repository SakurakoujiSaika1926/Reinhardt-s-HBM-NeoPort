package com.reinhardt.hbm.item;

import com.reinhardt.hbm.worldgen.structure.HbmStructureIO;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Direct port of ItemWandD: ray trace, then build the bundled crane at terrain height. */
public final class LegacyDebugCraneWandItem extends Item {
    public LegacyDebugCraneWandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(500.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            BlockPos target = blockHit.getBlockPos();
            int y = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
            HbmStructureIO.placeResourceStructure(serverLevel, "crane", new BlockPos(target.getX(), y, target.getZ()), 0, false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
