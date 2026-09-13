package com.reinhardt.hbm.item;

import com.reinhardt.hbm.menu.RebarPlacerMenu;
import com.reinhardt.hbm.util.SavedItemStackPreview;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import com.reinhardt.hbm.blockentity.RebarBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Persistent material selector and two-point selection half of the original ItemRebarPlacer. */
public final class LegacyRebarPlacerItem extends Item {
    private static final String SELECTED_CONCRETE = "selected_concrete";
    private static final String FIRST_POSITION = "rebar_first_position";

    public LegacyRebarPlacerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new SimpleMenuProvider(
                        (id, inventory, ignored) -> new RebarPlacerMenu(id, inventory, hand),
                        Component.translatable("container.reinhardtshbm.rebar_placer")
                ), buffer -> buffer.writeBoolean(hand == InteractionHand.OFF_HAND));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack placer = context.getItemInHand();
        ItemStack concrete = selectedConcrete(placer, player.registryAccess());
        if (!isValidConcrete(concrete)) {
            inform(player, context.getLevel(), "message.reinhardtshbm.rebar_placer.invalid", ChatFormatting.RED);
            return InteractionResult.FAIL;
        }

        CompoundTag tag = data(placer);
        var target = context.getClickedPos().relative(context.getClickedFace());
        if (!tag.contains(FIRST_POSITION)) {
            tag.putLong(FIRST_POSITION, target.asLong());
            store(placer, tag);
            inform(player, context.getLevel(), "message.reinhardtshbm.rebar_placer.first", ChatFormatting.AQUA);
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }

        var first = net.minecraft.core.BlockPos.of(tag.getLong(FIRST_POSITION));
        tag.remove(FIRST_POSITION);
        store(placer, tag);
        if (!context.getLevel().isClientSide) {
            int remaining = countRebar(player);
            int used = 0;
            int minX = Math.min(first.getX(), target.getX());
            int minY = Math.min(first.getY(), target.getY());
            int minZ = Math.min(first.getZ(), target.getZ());
            int maxX = Math.max(first.getX(), target.getX());
            int maxY = Math.max(first.getY(), target.getY());
            int maxZ = Math.max(first.getZ(), target.getZ());
            outer: for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++) {
                if (remaining <= 0) break outer;
                var pos = new net.minecraft.core.BlockPos(x, y, z);
                if (!context.getLevel().getBlockState(pos).canBeReplaced()) continue;
                context.getLevel().setBlock(pos, HbmBlocks.REBAR.get().defaultBlockState(), 3);
                if (context.getLevel().getBlockEntity(pos) instanceof RebarBlockEntity rebar) rebar.setup(concrete);
                remaining--;
                used++;
            }
            consumeRebar(player, used);
            player.displayClientMessage(Component.translatable("message.reinhardtshbm.rebar_placer.placed", used).withStyle(ChatFormatting.GREEN), false);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    public static boolean isValidConcrete(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        if (!id.getNamespace().equals("reinhardtshbm")) return false;
        String path = id.getPath();
        return path.equals("concrete") || path.equals("concrete_rebar") || path.equals("concrete_smooth")
                || path.equals("concrete_pillar") || path.equals("concrete_colored") || path.equals("concrete_colored_ext");
    }

    public static ItemStack selectedConcrete(ItemStack placer, HolderLookup.Provider registries) {
        CompoundTag root = data(placer);
        return root.contains(SELECTED_CONCRETE)
                ? ItemStack.parseOptional(registries, root.getCompound(SELECTED_CONCRETE))
                : ItemStack.EMPTY;
    }

    public static void setSelectedConcrete(ItemStack placer, ItemStack selected, HolderLookup.Provider registries) {
        CompoundTag root = data(placer);
        if (selected.isEmpty()) {
            root.remove(SELECTED_CONCRETE);
        } else {
            root.put(SELECTED_CONCRETE, selected.copyWithCount(1).saveOptional(registries));
        }
        store(placer, root);
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void store(ItemStack stack, CompoundTag tag) {
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA); else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static void inform(Player player, Level level, String key, ChatFormatting color) {
        if (!level.isClientSide) player.displayClientMessage(Component.translatable(key).withStyle(color), false);
    }

    private static int countRebar(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) if (stack.is(HbmBlocks.REBAR.get().asItem())) count += stack.getCount();
        return count;
    }

    private static void consumeRebar(Player player, int amount) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(HbmBlocks.REBAR.get().asItem())) continue;
            int removed = Math.min(amount, stack.getCount());
            stack.shrink(removed);
            amount -= removed;
            if (amount <= 0) return;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemStack selected = selectedConcreteForTooltip(stack);
        if (selected.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rebar_placer.none").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("tooltip.reinhardtshbm.rebar_placer.selected", selected.getHoverName()).withStyle(ChatFormatting.AQUA));
        }
    }

    public static ItemStack selectedConcreteForTooltip(ItemStack placer) {
        return SavedItemStackPreview.fromCustomData(placer, SELECTED_CONCRETE);
    }
}
