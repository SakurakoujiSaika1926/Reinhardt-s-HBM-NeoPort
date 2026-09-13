package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.LegacyDuckEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.EventHooks;

/** Direct ItemChopper implementation for the old standalone duck spawner. */
public final class LegacyDuckSpawnItem extends Item {
    public LegacyDuckSpawnItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        return spawn(context.getLevel(), context.getPlayer(), context.getItemInHand(), target);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK
                || !(level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof LiquidBlock)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.mayInteract(player, blockHit.getBlockPos())
                || !player.mayUseItemAt(blockHit.getBlockPos(), blockHit.getDirection(), stack)) {
            return InteractionResultHolder.pass(stack);
        }
        InteractionResult result = spawn(level, player, stack, blockHit.getBlockPos());
        return new InteractionResultHolder<>(result, stack);
    }

    private static InteractionResult spawn(Level level, Player player, ItemStack stack, BlockPos pos) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        LegacyDuckEntity duck = new LegacyDuckEntity(HbmEntityTypes.DUCK.get(), serverLevel);
        duck.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, Mth.wrapDegrees(level.random.nextFloat() * 360.0F), 0.0F);
        duck.setYHeadRot(duck.getYRot());
        duck.setYBodyRot(duck.getYRot());
        EventHooks.finalizeMobSpawn(duck, serverLevel,
                level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            duck.setCustomName(stack.getHoverName());
        }
        level.addFreshEntity(duck);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
