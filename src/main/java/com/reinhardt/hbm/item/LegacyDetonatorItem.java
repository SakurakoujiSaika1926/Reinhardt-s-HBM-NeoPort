package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.LandmineBlock;
import com.reinhardt.hbm.block.TimedExplosiveBlock;
import com.reinhardt.hbm.block.WallChargeBlock;
import com.reinhardt.hbm.blockentity.NukeBoyBlockEntity;
import com.reinhardt.hbm.blockentity.WallChargeExplosions;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/** Direct 1.7.10 ItemDetonator, ItemLaserDetonator and ItemMultiDetonator port. */
public class LegacyDetonatorItem extends Item {
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";
    private static final String XS = "xValues";
    private static final String YS = "yValues";
    private static final String ZS = "zValues";
    private final String legacyId;

    public LegacyDetonatorItem(Properties properties, String legacyId) {
        super(properties.stacksTo(1));
        this.legacyId = legacyId;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOn(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown() || isLaser()) {
            return InteractionResult.PASS;
        }

        if (!context.getLevel().isClientSide) {
            if (isMulti()) {
                addLocation(context.getItemInHand(), context.getClickedPos());
                player.displayClientMessage(Component.translatable("item.reinhardtshbm.detonator.position_added")
                        .withStyle(ChatFormatting.GREEN), false);
            } else {
                setLocation(context.getItemInHand(), context.getClickedPos());
                player.displayClientMessage(Component.translatable("item.reinhardtshbm.detonator.position_set")
                        .withStyle(ChatFormatting.GREEN), false);
            }
            context.getLevel().playSound(null, context.getClickedPos(), HbmSoundEvents.TECH_BOOP.get(),
                    SoundSource.PLAYERS, 2.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isLaser()) {
            return useLaser(level, player, stack);
        }
        if (isMulti() && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                clearLocations(stack);
                player.displayClientMessage(Component.translatable("item.reinhardtshbm.detonator.locations_cleared")
                        .withStyle(ChatFormatting.RED), false);
                level.playSound(null, player.blockPosition(), HbmSoundEvents.TECH_BOOP.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (isMulti()) {
            List<BlockPos> positions = locations(stack);
            if (positions.isEmpty()) {
                noPosition(player);
                return InteractionResultHolder.fail(stack);
            }
            int triggered = 0;
            for (BlockPos pos : positions) {
                if (detonate(level, pos, player)) {
                    triggered++;
                }
            }
            level.playSound(null, player.blockPosition(), HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("item.reinhardtshbm.detonator.triggered", triggered, positions.size())
                    .withStyle(ChatFormatting.YELLOW), false);
            return InteractionResultHolder.consume(stack);
        }

        BlockPos pos = location(stack);
        if (pos == null) {
            noPosition(player);
            return InteractionResultHolder.fail(stack);
        }
        boolean triggered = detonate(level, pos, player);
        level.playSound(null, player.blockPosition(), triggered ? HbmSoundEvents.TECH_BLEEP.get() : HbmSoundEvents.TECH_BOOP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable(triggered
                        ? "item.reinhardtshbm.detonator.success"
                        : "item.reinhardtshbm.detonator.no_bomb")
                .withStyle(triggered ? ChatFormatting.YELLOW : ChatFormatting.RED), false);
        return triggered ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (isLaser()) {
            tooltip.add(Component.translatable("item.reinhardtshbm.detonator.laser_hint").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable(isMulti()
                        ? "item.reinhardtshbm.detonator.multi_hint"
                        : "item.reinhardtshbm.detonator.hint")
                .withStyle(ChatFormatting.GRAY));
        if (isMulti()) {
            List<BlockPos> positions = locations(stack);
            if (positions.isEmpty()) {
                tooltip.add(Component.translatable("item.reinhardtshbm.detonator.no_position").withStyle(ChatFormatting.RED));
            } else {
                for (BlockPos pos : positions) {
                    tooltip.add(Component.literal(pos.getX() + " / " + pos.getY() + " / " + pos.getZ())
                            .withStyle(ChatFormatting.YELLOW));
                }
            }
            return;
        }
        BlockPos pos = location(stack);
        tooltip.add(pos == null
                ? Component.translatable("item.reinhardtshbm.detonator.no_position").withStyle(ChatFormatting.RED)
                : Component.translatable("item.reinhardtshbm.detonator.linked", pos.getX(), pos.getY(), pos.getZ())
                        .withStyle(ChatFormatting.YELLOW));
    }

    private InteractionResultHolder<ItemStack> useLaser(Level level, Player player, ItemStack stack) {
        HitResult hit = player.pick(500.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) {
            return InteractionResultHolder.fail(stack);
        }
        BlockPos pos = blockHit.getBlockPos();
        if (level.isClientSide) {
            double distance = Math.min(player.getEyePosition().distanceTo(blockHit.getLocation()), 15.0D);
            var direction = blockHit.getLocation().subtract(player.getEyePosition()).normalize();
            for (int index = 0; index < (int) distance; index++) {
                double offset = 3.0D + level.random.nextDouble() * distance;
                level.addParticle(DustParticleOptions.REDSTONE,
                        player.getX() + direction.x * offset,
                        player.getEyeY() + direction.y * offset,
                        player.getZ() + direction.z * offset,
                        0.0D, 0.0D, 0.0D);
            }
            return InteractionResultHolder.success(stack);
        }
        boolean triggered = detonate(level, pos, player);
        level.playSound(null, player.blockPosition(), triggered ? HbmSoundEvents.TECH_BLEEP.get() : HbmSoundEvents.TECH_BOOP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable(triggered
                        ? "item.reinhardtshbm.detonator.success"
                        : "item.reinhardtshbm.detonator.no_bomb")
                .withStyle(triggered ? ChatFormatting.YELLOW : ChatFormatting.RED), true);
        return triggered ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    private static boolean detonate(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof NukeBoyBlockEntity nukeBoy && nukeBoy.isReady()) {
            nukeBoy.detonate();
            return true;
        }
        if (level.getBlockState(pos).getBlock() instanceof TimedExplosiveBlock explosive) {
            explosive.detonate(server, pos, player);
            return true;
        }
        if (level.getBlockState(pos).getBlock() instanceof WallChargeBlock charge) {
            level.removeBlock(pos, false);
            WallChargeExplosions.detonate(server, pos, charge.kind());
            return true;
        }
        if (level.getBlockState(pos).getBlock() instanceof LandmineBlock mine) {
            mine.detonate(server, pos);
            return true;
        }
        return false;
    }

    private boolean isLaser() {
        return legacyId.equals("detonator_laser");
    }

    private boolean isMulti() {
        return legacyId.equals("detonator_multi");
    }

    private static void noPosition(Player player) {
        player.displayClientMessage(Component.translatable("item.reinhardtshbm.detonator.no_position")
                .withStyle(ChatFormatting.RED), false);
    }

    private static BlockPos location(ItemStack stack) {
        CompoundTag tag = data(stack);
        return tag.contains(X) && tag.contains(Y) && tag.contains(Z)
                ? new BlockPos(tag.getInt(X), tag.getInt(Y), tag.getInt(Z))
                : null;
    }

    private static void setLocation(ItemStack stack, BlockPos pos) {
        CompoundTag tag = data(stack);
        tag.putInt(X, pos.getX());
        tag.putInt(Y, pos.getY());
        tag.putInt(Z, pos.getZ());
        setData(stack, tag);
    }

    private static void addLocation(ItemStack stack, BlockPos pos) {
        CompoundTag tag = data(stack);
        int[] xs = tag.getIntArray(XS);
        int[] ys = tag.getIntArray(YS);
        int[] zs = tag.getIntArray(ZS);
        tag.putIntArray(XS, append(xs, pos.getX()));
        tag.putIntArray(YS, append(ys, pos.getY()));
        tag.putIntArray(ZS, append(zs, pos.getZ()));
        setData(stack, tag);
    }

    private static void clearLocations(ItemStack stack) {
        CompoundTag tag = data(stack);
        tag.putIntArray(XS, new int[0]);
        tag.putIntArray(YS, new int[0]);
        tag.putIntArray(ZS, new int[0]);
        setData(stack, tag);
    }

    private static List<BlockPos> locations(ItemStack stack) {
        CompoundTag tag = data(stack);
        int[] xs = tag.getIntArray(XS);
        int[] ys = tag.getIntArray(YS);
        int[] zs = tag.getIntArray(ZS);
        int length = Math.min(xs.length, Math.min(ys.length, zs.length));
        List<BlockPos> positions = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            positions.add(new BlockPos(xs[index], ys[index], zs[index]));
        }
        return positions;
    }

    private static int[] append(int[] values, int value) {
        int[] result = new int[values.length + 1];
        System.arraycopy(values, 0, result, 0, values.length);
        result[values.length] = value;
        return result;
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void setData(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
