package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.UniversalGrenadeEntity;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * The configurable grenade from 1.7.10.  Its four component choices are
 * carried on the completed item so copies, entities and recipes retain the
 * exact selected configuration.
 */
public final class UniversalGrenadeItem extends Item {
    private static final String SHELL_KEY = "shell";
    private static final String FILLING_KEY = "filling";
    private static final String FUZE_KEY = "fuze";
    private static final String EXTRA_KEY = "extra";
    private static final String DEPLOYMENT_KEY = "deployment";

    public UniversalGrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return shell(stack).stackLimit();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (deployment(stack) < shell(stack).drawDuration()) {
            return InteractionResultHolder.pass(stack);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS, 0.5F, 0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide) {
            level.addFreshEntity(new UniversalGrenadeEntity(level, player, stack));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!(entity instanceof Player)) {
            return;
        }
        int next = selected ? Math.min(shell(stack).drawDuration(), deployment(stack) + 1) : 0;
        if (next != deployment(stack)) {
            CompoundTag tag = customData(stack);
            tag.putInt(DEPLOYMENT_KEY, next);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(componentName("grenade_shell", shell(stack).id()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(componentName("grenade_filling", filling(stack).id()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(componentName("grenade_fuze", fuze(stack).id()).withStyle(ChatFormatting.YELLOW));
        Extra extra = extra(stack);
        if (extra != null) {
            tooltip.add(componentName("grenade_extra", extra.id()).withStyle(ChatFormatting.RED));
        }
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Shell shell : Shell.values()) {
            for (Filling filling : Filling.values()) {
                if (!filling.supports(shell)) {
                    continue;
                }
                for (Fuze fuze : Fuze.values()) {
                    output.accept(make(shell, filling, fuze, null));
                    for (Extra extra : Extra.values()) {
                        output.accept(make(shell, filling, fuze, extra));
                    }
                }
            }
        }
    }

    public static ItemStack make(Shell shell, Filling filling, Fuze fuze, @Nullable Extra extra) {
        ItemStack stack = new ItemStack(HbmItems.GRENADE_UNIVERSAL.get());
        CompoundTag tag = customData(stack);
        tag.putString(SHELL_KEY, shell.id());
        tag.putString(FILLING_KEY, filling.id());
        tag.putString(FUZE_KEY, fuze.id());
        if (extra != null) {
            tag.putString(EXTRA_KEY, extra.id());
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        // The legacy renderer used the same complete-grenade mesh for every
        // configuration; the component information is shown by tint/tooltips.
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(0));
        return stack;
    }

    public static Shell shell(ItemStack stack) {
        return Shell.byId(readId(stack, SHELL_KEY));
    }

    public static Filling filling(ItemStack stack) {
        return Filling.byId(readId(stack, FILLING_KEY));
    }

    public static Fuze fuze(ItemStack stack) {
        return Fuze.byId(readId(stack, FUZE_KEY));
    }

    @Nullable
    public static Extra extra(ItemStack stack) {
        String value = readId(stack, EXTRA_KEY);
        return value.isEmpty() ? null : Extra.byId(value);
    }

    private static MutableComponent componentName(String baseId, String variant) {
        return Component.translatable("item.reinhardtshbm." + baseId + "." + variant);
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static String readId(ItemStack stack, String key) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString(key);
    }

    private static int deployment(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(DEPLOYMENT_KEY);
    }

    public enum Shell {
        FRAG(4, 30, 0.5D, 1.0D),
        STICK(4, 43, 0.25D, 1.5D),
        TECH(2, 30, 0.5D, 1.0D),
        NUKE(1, 43, 0.25D, 1.5D);

        private final int stackLimit;
        private final int drawDuration;
        private final double bounce;
        private final double throwForce;

        Shell(int stackLimit, int drawDuration, double bounce, double throwForce) {
            this.stackLimit = stackLimit;
            this.drawDuration = drawDuration;
            this.bounce = bounce;
            this.throwForce = throwForce;
        }

        public String id() { return name().toLowerCase(Locale.ROOT); }
        public int stackLimit() { return stackLimit; }
        public int drawDuration() { return drawDuration; }
        public double bounce() { return bounce; }
        public double throwForce() { return throwForce; }
        public static Shell byId(String id) {
            for (Shell value : values()) if (value.id().equals(id)) return value;
            return FRAG;
        }
    }

    public enum Filling {
        POWDER(Shell.FRAG, Shell.STICK),
        HE(Shell.FRAG, Shell.STICK),
        DEMO(Shell.FRAG, Shell.STICK),
        INC(Shell.FRAG, Shell.STICK),
        WP(Shell.FRAG, Shell.STICK),
        CLUSTER(Shell.FRAG, Shell.STICK),
        EMP(Shell.TECH),
        PLASMA(Shell.TECH),
        LASER(Shell.TECH),
        CLUSTER_HEAVY(Shell.NUKE),
        NUCLEAR(Shell.NUKE),
        NUCLEAR_DEMO(Shell.NUKE),
        SCHRAB(Shell.NUKE);

        private final Shell[] shells;
        Filling(Shell... shells) { this.shells = shells; }
        public String id() { return name().toLowerCase(Locale.ROOT); }
        public boolean supports(Shell shell) {
            for (Shell compatible : shells) if (compatible == shell) return true;
            return false;
        }
        public static Filling byId(String id) {
            for (Filling value : values()) if (value.id().equals(id)) return value;
            return HE;
        }
    }

    public enum Fuze {
        S3, S7, S15, IMPACT, AIRBURST;
        public String id() { return name().toLowerCase(Locale.ROOT); }
        public static Fuze byId(String id) {
            for (Fuze value : values()) if (value.id().equals(id)) return value;
            return S3;
        }
    }

    public enum Extra {
        GLUE, PROXY_FUZE, FRAG_SLEEVE, TRIPLEX;
        public String id() { return name().toLowerCase(Locale.ROOT); }
        public static Extra byId(String id) {
            for (Extra value : values()) if (value.id().equals(id)) return value;
            return null;
        }
    }
}
