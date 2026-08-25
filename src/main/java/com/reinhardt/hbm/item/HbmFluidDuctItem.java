package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class HbmFluidDuctItem extends Item {
    private static final String FLUID = "fluid";
    private final String itemId;

    public HbmFluidDuctItem(Properties properties) {
        this(properties, "ff_fluid_duct");
    }

    public HbmFluidDuctItem(Properties properties, String itemId) {
        super(properties);
        this.itemId = itemId;
    }

    @Override
    public Component getName(ItemStack stack) {
        HbmFluidDefinition fluid = fluid(stack);
        if (fluid.isNone()) {
            return super.getName(stack);
        }
        return Component.translatable("item.reinhardtshbm.ff_fluid_duct.named", Component.translatable(fluid.translationKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        HbmFluidDefinition fluid = fluid(stack);
        if (!fluid.isNone()) {
            tooltip.add(Component.translatable(fluid.translationKey()).withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        if (!level.getBlockState(pos).canBeReplaced(placeContext)) {
            pos = pos.relative(context.getClickedFace());
            if (!level.getBlockState(pos).isAir()) {
                return InteractionResult.FAIL;
            }
        }
        if (context.getPlayer() != null && !context.getPlayer().mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        HbmFluidDefinition fluid = fluid(stack);
        if (fluid.isNone()) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            level.setBlock(pos, HbmBlocks.FLUID_DUCT_MK2.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof com.reinhardt.hbm.blockentity.FluidPipeBlockEntity pipe) {
                pipe.setType(fluid);
            }
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
                stack.shrink(1);
            }
            level.playSound(null, pos, HbmSoundEvents.PIPE_PLACED.get(), SoundSource.PLAYERS, 1.0F, 0.8F + level.random.nextFloat() * 0.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (HbmFluidDefinition definition : HbmFluids.niceOrder()) {
            if (definition.allowsFluidIdentifier()) {
                output.accept(stackForFluid(definition, 1));
            }
        }
    }

    public ItemStack stackForFluid(HbmFluidDefinition definition, int count) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(this.itemId)), count);
        setFluid(stack, definition);
        return stack;
    }

    public static ItemStack forFluid(HbmFluidDefinition definition, int count) {
        return ((HbmFluidDuctItem) com.reinhardt.hbm.registry.HbmItems.FF_FLUID_DUCT.get()).stackForFluid(definition, count);
    }

    public static HbmFluidDefinition fluid(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return HbmFluids.byName(tag.getString(FLUID)).orElse(HbmFluids.none());
    }

    public static void setFluid(ItemStack stack, HbmFluidDefinition fluid) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(FLUID, fluid == null ? "none" : fluid.name());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int tint(ItemStack stack, int tintIndex) {
        if (tintIndex != 1) {
            return 0xFFFFFFFF;
        }
        HbmFluidDefinition fluid = fluid(stack);
        return fluid.isNone() ? 0xFFFFFFFF : 0xFF000000 | fluid.color();
    }
}
