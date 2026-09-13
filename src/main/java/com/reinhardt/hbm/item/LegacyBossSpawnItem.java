package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.LegacyChopperEntity;
import com.reinhardt.hbm.entity.LegacyUfoEntity;
import com.reinhardt.hbm.entity.LegacyWormHeadEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.EventHooks;

import java.util.List;

/** Exact ItemChopper placement and fluid-use behavior for the three boss spawners. */
public final class LegacyBossSpawnItem extends Item {
    public enum Type { CHOPPER, UFO, WORM }

    private final Type type;

    public LegacyBossSpawnItem(Properties properties, Type type) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos clicked = context.getClickedPos();
        BlockPos spawn = clicked.relative(context.getClickedFace());
        double verticalOffset = context.getClickedFace().getAxis().isVertical() && context.getClickedFace().getStepY() > 0
                && context.getLevel().getBlockState(clicked).getBlock() instanceof SnowLayerBlock ? 0.5D : 0.0D;
        return spawn(context.getLevel(), context.getPlayer(), context.getItemInHand(), spawn, verticalOffset);
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
        return new InteractionResultHolder<>(spawn(level, player, stack, blockHit.getBlockPos(), 0.0D), stack);
    }

    private InteractionResult spawn(Level level, Player player, ItemStack stack, BlockPos pos, double verticalOffset) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Mob entity = switch (type) {
            case CHOPPER -> new LegacyChopperEntity(HbmEntityTypes.LEGACY_CHOPPER.get(), level);
            case UFO -> new LegacyUfoEntity(HbmEntityTypes.LEGACY_UFO.get(), level);
            case WORM -> new LegacyWormHeadEntity(HbmEntityTypes.LEGACY_WORM_HEAD.get(), level);
        };

        double y = pos.getY() + verticalOffset;
        if (entity instanceof LegacyUfoEntity ufo) {
            ufo.setScanCooldown(100);
            y += 35.0D;
        }
        entity.moveTo(pos.getX() + 0.5D, y, pos.getZ() + 0.5D,
                Mth.wrapDegrees(level.random.nextFloat() * 360.0F), 0.0F);
        entity.setYHeadRot(entity.getYRot());
        entity.setYBodyRot(entity.getYRot());
        if (level instanceof ServerLevel serverLevel) {
            EventHooks.finalizeMobSpawn(entity, serverLevel,
                    level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);
        }
        if (entity instanceof LivingEntity living && stack.has(DataComponents.CUSTOM_NAME)) {
            living.setCustomName(stack.getHoverName());
        }
        level.addFreshEntity(entity);
        if (entity instanceof LegacyWormHeadEntity worm && level instanceof ServerLevel serverLevel) {
            worm.initializeLegacySpawn(serverLevel);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> tooltip,
                                TooltipFlag flag) {
        if (type == Type.WORM) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.reinhardtshbm.spawn_worm.1")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.reinhardtshbm.spawn_worm.2")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(net.minecraft.network.chat.Component.empty());
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.reinhardtshbm.spawn_worm.3")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.reinhardtshbm.spawn_worm.4")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
