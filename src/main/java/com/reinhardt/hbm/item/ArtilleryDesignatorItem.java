package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.LegacyTurretBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyTurretType;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class ArtilleryDesignatorItem extends Item {
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";

    public ArtilleryDesignatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return bind(stack, context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return bind(context.getItemInHand(), context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        BlockPos turretPos = linkedTurret(stack);
        if (turretPos == null) {
            return InteractionResultHolder.pass(stack);
        }
        HitResult hit = player.pick(500.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && level.getBlockEntity(turretPos) instanceof LegacyTurretBlockEntity turret && isArtillery(turret)) {
            BlockPos target = blockHit.getBlockPos();
            turret.enqueueTarget(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
            level.playSound(null, player.blockPosition(), HbmSoundEvents.TECH_BOOP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos target = linkedTurret(stack);
        if (target == null) {
            tooltip.add(Component.translatable("item.reinhardtshbm.designator_arty_range.unlinked").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.reinhardtshbm.designator_arty_range.linked", target.getX(), target.getY(), target.getZ())
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    private static InteractionResult bind(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos corePos = resolveCore(level, context.getClickedPos());
        BlockEntity blockEntity = level.getBlockEntity(corePos);
        if (!(blockEntity instanceof LegacyTurretBlockEntity turret) || !isArtillery(turret)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putInt(X, corePos.getX());
            tag.putInt(Y, corePos.getY());
            tag.putInt(Z, corePos.getZ());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            level.playSound(null, player.blockPosition(), HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean isArtillery(LegacyTurretBlockEntity turret) {
        return turret.type() == LegacyTurretType.ARTY || turret.type() == LegacyTurretType.HIMARS;
    }

    private static BlockPos resolveCore(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            return dummy.getCorePos();
        }
        return pos;
    }

    private static BlockPos linkedTurret(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(X) || !tag.contains(Y) || !tag.contains(Z)) {
            return null;
        }
        return new BlockPos(tag.getInt(X), tag.getInt(Y), tag.getInt(Z));
    }
}
