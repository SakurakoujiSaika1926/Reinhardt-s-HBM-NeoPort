package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.LegacyVortexEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** ItemDrop behavior for the remaining destructive 1.7.10 standalone items. */
public final class LegacyDropItem extends Item {
    private static final String TARGET_KEY = "target";

    public enum Kind {
        BETA, BLACK_HOLE, DETONATOR_DE, DETONATOR_DEADMAN, PELLET_ANTIMATTER,
        SINGULARITY, SINGULARITY_COUNTER_RESONANT, SINGULARITY_SUPER_HEATED
    }

    private final Kind kind;

    public LegacyDropItem(Kind kind) {
        super(new Properties().stacksTo(1));
        this.kind = kind;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (this.kind == Kind.BETA) {
            entity.discard();
            return true;
        }
        if (level.isClientSide) {
            return false;
        }
        if (this.kind == Kind.DETONATOR_DE) {
            level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 15.0F, true, Level.ExplosionInteraction.BLOCK);
            entity.discard();
            return true;
        }
        if (this.kind == Kind.DETONATOR_DEADMAN) {
            detonateDeadman(level, entity, stack);
            entity.discard();
            return true;
        }
        if (!entity.onGround()) {
            return false;
        }

        switch (this.kind) {
            case PELLET_ANTIMATTER -> level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 20.0F, true, Level.ExplosionInteraction.BLOCK);
            case SINGULARITY -> LegacyVortexEntity.spawn(level, entity.position(), 1.5F, 0.0025F, false);
            case SINGULARITY_COUNTER_RESONANT, SINGULARITY_SUPER_HEATED ->
                    LegacyVortexEntity.spawn(level, entity.position(), 2.5F, 0.0025F, false);
            case BLACK_HOLE -> LegacyVortexEntity.spawn(level, entity.position(), 1.5F, 0.0025F, false);
            default -> {
                return false;
            }
        }
        entity.discard();
        return true;
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        if (this.kind != Kind.DETONATOR_DEADMAN || !context.getPlayer().isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        BlockPos target = context.getClickedPos();
        root.putLong(TARGET_KEY, target.asLong());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        context.getLevel().playSound(null, target, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(), net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.0F);
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private static void detonateDeadman(Level level, ItemEntity entity, ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (root.contains(TARGET_KEY)) {
            BlockPos target = BlockPos.of(root.getLong(TARGET_KEY));
            level.explode(entity, target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 0.0F, true, Level.ExplosionInteraction.NONE);
        }
        level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 0.0F, true, Level.ExplosionInteraction.NONE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        switch (this.kind) {
            case PELLET_ANTIMATTER -> tooltip.add(Component.literal("Very heavy antimatter cluster.").withStyle(ChatFormatting.RED));
            case DETONATOR_DE -> tooltip.add(Component.literal("Explodes when dropped!").withStyle(ChatFormatting.RED));
            case DETONATOR_DEADMAN -> tooltip.add(Component.literal("Shift right-click to set position, drop to detonate!").withStyle(ChatFormatting.RED));
            default -> {
            }
        }
    }
}
