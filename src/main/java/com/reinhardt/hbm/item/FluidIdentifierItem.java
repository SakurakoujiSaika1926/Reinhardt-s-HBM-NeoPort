package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.FluidDuctBlock;
import com.reinhardt.hbm.blockentity.FluidPipeBlockEntity;
import com.reinhardt.hbm.client.screen.FluidIdentifierScreen;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

public class FluidIdentifierItem extends Item {
    private static final String PRIMARY = "fluid1";
    private static final String SECONDARY = "fluid2";

    public FluidIdentifierItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        HbmFluidDefinition primary = primary(stack);
        if (!primary.isNone()) {
            return Component.translatable("item.reinhardtshbm.fluid_identifier_multi.named", Component.translatable(primary.translationKey()));
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        HbmFluidDefinition primary = primary(stack);
        HbmFluidDefinition secondary = secondary(stack);
        tooltip.add(Component.translatable("item.reinhardtshbm.fluid_identifier_multi.info").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.reinhardtshbm.fluid_identifier_multi.usage0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.reinhardtshbm.fluid_identifier_multi.usage1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.reinhardtshbm.fluid_identifier_multi.primary",
                Component.translatable(primary.translationKey())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.reinhardtshbm.fluid_identifier_multi.secondary",
                Component.translatable(secondary.translationKey())).withStyle(ChatFormatting.DARK_AQUA));
        if (!primary.isNone()) {
            HbmFluidTooltip.appendTraitInfo(tooltip, primary, flag.isAdvanced());
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide && player.isShiftKeyDown() && FMLEnvironment.dist.isClient()) {
            FluidIdentifierScreen.open(stack);
            return InteractionResultHolder.success(stack);
        }
        if (!level.isClientSide && !player.isShiftKeyDown()) {
            HbmFluidDefinition primary = primary(stack);
            HbmFluidDefinition secondary = secondary(stack);
            setType(stack, secondary, true);
            setType(stack, primary, false);
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 1.25F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOnPipe(stack, context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return useOnPipe(context.getItemInHand(), context);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
        ItemStack copy = itemStack.copy();
        copy.setCount(1);
        return copy;
    }

    public static ItemStack forFluid(HbmFluidDefinition definition) {
        ItemStack stack = new ItemStack(HbmItems.FLUID_IDENTIFIER_MULTI.get());
        setType(stack, definition, true);
        setType(stack, HbmFluids.none(), false);
        return stack;
    }

    public static HbmFluidDefinition primary(ItemStack stack) {
        return getType(stack, PRIMARY);
    }

    public static HbmFluidDefinition secondary(ItemStack stack) {
        return getType(stack, SECONDARY);
    }

    public static void setType(ItemStack stack, HbmFluidDefinition definition, boolean primary) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        tag.putString(primary ? PRIMARY : SECONDARY, definition == null ? "none" : definition.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean applyToPipe(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FluidDuctBlock duct && duct.kind().isExhaust()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.reinhardtshbm.fluid.exhaust_fixed"), true);
            }
            return true;
        }

        HbmFluidDefinition fluid = primary(stack);
        if (!level.isClientSide) {
            HbmFluidDefinition previousType = pipe.type();
            boolean batch = player.isShiftKeyDown() || SettingsToolItem.isCtrlDown(player);
            int changed = 0;
            if (previousType != fluid) {
                if (batch) {
                    changed = FluidDuctBlock.changeTypeRecursively(level, pos, previousType, fluid, 256);
                } else {
                    pipe.setType(fluid);
                    changed = 1;
                }
            }

            if (changed > 0) {
                player.displayClientMessage(Component.translatable(
                        batch
                                ? "message.reinhardtshbm.fluid.changed_type_batch"
                                : "message.reinhardtshbm.fluid.changed_type",
                        Component.translatable(fluid.translationKey()),
                        changed
                ), true);
                level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.25F, 1.2F);
            } else {
                player.displayClientMessage(Component.translatable(
                        "message.reinhardtshbm.fluid.changed_type_unchanged",
                        Component.translatable(fluid.translationKey())
                ), true);
            }
        }
        return true;
    }

    private static HbmFluidDefinition getType(ItemStack stack, String key) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return HbmFluids.byName(tag.getString(key)).orElse(HbmFluids.none());
    }

    private static InteractionResult useOnPipe(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return applyToPipe(stack, context.getLevel(), context.getClickedPos(), player)
                ? InteractionResult.sidedSuccess(context.getLevel().isClientSide)
                : InteractionResult.PASS;
    }
}
