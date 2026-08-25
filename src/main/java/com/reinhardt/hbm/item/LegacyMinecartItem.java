package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.LegacyMinecartEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Modern counterpart of ItemModMinecart, including its base/type stack data. */
public final class LegacyMinecartItem extends Item {
    public static final String CART_BASE_NBT = "cartBase";
    private static final String CART_TYPE_NBT = "cartType";
    private static final String CART_ITEMS_NBT = "cartItems";

    public LegacyMinecartItem(Properties properties) {
        super(properties.stacksTo(4));
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BaseRailBlock)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        LegacyMinecartEntity cart = new LegacyMinecartEntity(HbmEntityTypes.LEGACY_MINECART.get(), level);
        cart.setVariant(type(stack));
        cart.setBase(base(stack));
        cart.restoreCargo(stack, level.registryAccess());
        cart.setPos(pos.getX() + 0.5D, pos.getY() + 0.0625D, pos.getZ() + 0.5D);
        cart.setYRot(context.getPlayer() == null ? 0.0F : context.getPlayer().getYRot());
        if (!level.noCollision(cart, cart.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            if (level instanceof ServerLevel serverLevel) {
                net.minecraft.world.entity.EntityType.<LegacyMinecartEntity>createDefaultStackConfig(serverLevel, stack, context.getPlayer()).accept(cart);
            }
            level.addFreshEntity(cart);
            stack.consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static ItemStack createCartItem(Base base, Type type) {
        ItemStack stack = new ItemStack(HbmItems.CART.get());
        CompoundTag data = data(stack);
        data.putInt(CART_BASE_NBT, base.ordinal());
        data.putInt(CART_TYPE_NBT, type.ordinal());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    public static Base base(ItemStack stack) {
        return Base.byId(data(stack).getInt(CART_BASE_NBT));
    }

    public static Type type(ItemStack stack) {
        return Type.byId(data(stack).getInt(CART_TYPE_NBT));
    }

    public static CompoundTag cargo(ItemStack stack) {
        return data(stack).getCompound(CART_ITEMS_NBT).copy();
    }

    public static void setCargo(ItemStack stack, CompoundTag cargo) {
        CompoundTag data = data(stack);
        if (cargo.isEmpty()) {
            data.remove(CART_ITEMS_NBT);
        } else {
            data.put(CART_ITEMS_NBT, cargo.copy());
        }
        if (data.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        }
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Type type : Type.values()) {
            for (Base base : Base.values()) {
                if (type.supports(base)) {
                    output.accept(createCartItem(base, type));
                }
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.reinhardtshbm.cart." + type(stack).id());
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public enum Base {
        VANILLA, WOOD, STEEL, PAINTED;

        public static Base byId(int id) {
            Base[] values = values();
            return id >= 0 && id < values.length ? values[id] : VANILLA;
        }
    }

    public enum Type {
        EMPTY("empty", Base.WOOD, Base.STEEL, Base.PAINTED),
        CRATE("crate", Base.VANILLA),
        DESTROYER("destroyer", Base.STEEL, Base.PAINTED),
        POWDER("powder", Base.WOOD, Base.STEEL, Base.PAINTED),
        SEMTEX("semtex", Base.WOOD, Base.STEEL, Base.PAINTED);

        private final String id;
        private final Base[] supportedBases;

        Type(String id, Base... supportedBases) {
            this.id = id;
            this.supportedBases = supportedBases;
        }

        public String id() {
            return id;
        }

        public boolean supports(Base base) {
            for (Base supported : supportedBases) {
                if (supported == base) {
                    return true;
                }
            }
            return false;
        }

        public static Type byId(int id) {
            Type[] values = values();
            return id >= 0 && id < values.length ? values[id] : EMPTY;
        }
    }
}
