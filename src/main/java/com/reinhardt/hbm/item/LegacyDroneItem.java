package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.LegacyDeliveryDroneEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Direct 1.7.10 transport-drone item, including its five legacy variants. */
public final class LegacyDroneItem extends Item {
    public enum Type {
        PATROL,
        PATROL_CHUNKLOADING,
        PATROL_EXPRESS,
        PATROL_EXPRESS_CHUNKLOADING,
        REQUEST;

        public static Type fromStack(ItemStack stack) {
            int value = stack.getOrDefault(LegacyItemComponents.DRONE_TYPE.get(), PATROL.ordinal());
            return value >= 0 && value < values().length ? values()[value] : PATROL;
        }
    }

    public LegacyDroneItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    public static ItemStack stack(Type type, int count) {
        ItemStack stack = new ItemStack(com.reinhardt.hbm.registry.HbmItems.DRONE.get(), count);
        stack.set(LegacyItemComponents.DRONE_TYPE.get(), type.ordinal());
        return stack;
    }

    public void addCreativeVariants(net.minecraft.world.item.CreativeModeTab.Output output) {
        for (Type type : Type.values()) {
            output.accept(stack(type, 1));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // ItemDrone only launched from an upper face. The request variant is
        // consumed here as well but does not create a patrol drone.
        if (context.getClickedFace() != net.minecraft.core.Direction.UP) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Type type = Type.fromStack(context.getItemInHand());
        if (type != Type.REQUEST) {
            spawn(context.getLevel(), context.getPlayer(), context.getItemInHand(), context.getClickedPos().above());
        }
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        // Legacy ItemDrone deliberately returned false after the server-side
        // launch, allowing the clicked block to retain its normal use result.
        return InteractionResult.PASS;
    }

    private static void spawn(net.minecraft.world.level.Level level, Player player, ItemStack stack, BlockPos pos) {
        LegacyDeliveryDroneEntity drone = HbmEntityTypes.DELIVERY_DRONE.get().create(level);
        if (drone == null) {
            return;
        }
        Type type = Type.fromStack(stack);
        drone.configure(type == Type.PATROL_EXPRESS || type == Type.PATROL_EXPRESS_CHUNKLOADING,
                type == Type.PATROL_CHUNKLOADING || type == Type.PATROL_EXPRESS_CHUNKLOADING);
        drone.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), 0.0F);
        if (!level.noCollision(drone, drone.getBoundingBox())) {
            return;
        }
        level.addFreshEntity(drone);
    }

    @Override
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        return net.minecraft.network.chat.Component.translatable("item.reinhardtshbm.drone." + Type.fromStack(stack).name().toLowerCase());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<net.minecraft.network.chat.Component> tooltip,
                                TooltipFlag flag) {
        Type type = Type.fromStack(stack);
        tooltip.add(net.minecraft.network.chat.Component.translatable(
                "tooltip.reinhardtshbm.drone." + type.name().toLowerCase()).withStyle(ChatFormatting.YELLOW));
    }
}
