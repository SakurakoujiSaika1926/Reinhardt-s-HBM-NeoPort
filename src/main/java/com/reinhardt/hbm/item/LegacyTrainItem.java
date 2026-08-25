package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.HbmRailBlock;
import com.reinhardt.hbm.entity.LegacyTrainEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7.10 ItemTrain: cargo tram and non-powered cargo tram trailer. */
public final class LegacyTrainItem extends Item {
    private static final String TYPE = "trainType";

    public LegacyTrainItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof HbmRailBlock)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        LegacyTrainEntity train = new LegacyTrainEntity(HbmEntityTypes.LEGACY_TRAIN.get(), level);
        train.setVariant(type(stack));
        train.setPos(pos.getX() + 0.5D, pos.getY() + 0.0625D, pos.getZ() + 0.5D);
        train.setYRot(context.getPlayer() == null ? 0.0F : context.getPlayer().getYRot());
        if (!level.noCollision(train, train.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            if (level instanceof ServerLevel serverLevel) {
                net.minecraft.world.entity.EntityType.<LegacyTrainEntity>createDefaultStackConfig(serverLevel, stack, context.getPlayer()).accept(train);
            }
            level.addFreshEntity(train);
            stack.consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static ItemStack stackFor(Type type) {
        ItemStack stack = new ItemStack(HbmItems.TRAIN.get());
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putInt(TYPE, type.ordinal());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    public static Type type(ItemStack stack) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Type.byId(data.getInt(TYPE));
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Type type : Type.values()) {
            output.accept(stackFor(type));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm.train." + type(stack).id());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        Type type = type(stack);
        if (type.electric()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.train.engine", "Electric").withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.train.acceleration", "0.2m/s2").withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.train.engine_brake", "<1m/s").withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.train.gauge", "Standard Gauge").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.train.max_speed", type.electric() ? "10m/s" : "Yes").withStyle(ChatFormatting.GREEN));
    }

    public enum Type {
        CARGO_TRAM("cargo_tram", true, 29),
        CARGO_TRAM_TRAILER("cargo_tram_trailer", false, 45);

        private final String id;
        private final boolean electric;
        private final int slots;

        Type(String id, boolean electric, int slots) {
            this.id = id;
            this.electric = electric;
            this.slots = slots;
        }

        public String id() { return id; }
        public boolean electric() { return electric; }
        public int slots() { return slots; }

        public static Type byId(int id) {
            Type[] values = values();
            return id >= 0 && id < values.length ? values[id] : CARGO_TRAM;
        }
    }
}
