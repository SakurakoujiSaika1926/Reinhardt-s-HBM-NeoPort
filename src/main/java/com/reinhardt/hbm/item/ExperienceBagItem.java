package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.List;

/**
 * The two fixed experience containers from 1.7.10.
 *
 * <p>An empty bag stores exactly 100 experience points and becomes a full bag;
 * a full bag returns the same 100 points.  The pair also exposes the original
 * 100 mB XPJUICE fluid-container mapping to machines and pipes.</p>
 */
public final class ExperienceBagItem extends Item {
    public static final int EXPERIENCE_POINTS = 100;
    public static final int FLUID_AMOUNT_MB = 100;

    private final boolean filled;

    public ExperienceBagItem(Properties properties, boolean filled) {
        super(properties);
        this.filled = filled;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.filled && player instanceof FakePlayer) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (this.filled) {
            setTotalExperience(player, (int) Math.min(
                    Integer.MAX_VALUE,
                    (long) totalExperience(player) + EXPERIENCE_POINTS
            ));
            ItemStack result = replaceOne(player, hand, stack, HbmItems.IV_XP_EMPTY.get());
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResultHolder.success(result);
        }

        int available = totalExperience(player);
        if (available < EXPERIENCE_POINTS) {
            player.displayClientMessage(Component.translatable(
                    "message.reinhardtshbm.experience_bag.insufficient", EXPERIENCE_POINTS, available
            ), true);
            return InteractionResultHolder.fail(stack);
        }

        setTotalExperience(player, available - EXPERIENCE_POINTS);
        ItemStack result = replaceOne(player, hand, stack, HbmItems.IV_XP.get());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.success(result);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                this.filled
                        ? "tooltip.reinhardtshbm.experience_bag.retrieve"
                        : "tooltip.reinhardtshbm.experience_bag.store",
                EXPERIENCE_POINTS
        ).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(
                "tooltip.reinhardtshbm.experience_bag.fluid", FLUID_AMOUNT_MB
        ).withStyle(ChatFormatting.DARK_AQUA));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public IFluidHandlerItem createFluidHandler(ItemStack stack) {
        return new Handler(stack);
    }

    /**
     * Reconstruct the player's real XP total from level and bar progress.
     * Player.totalExperience is not authoritative after every level-changing
     * operation, which was the reason the generic port could reject valid use.
     */
    static int totalExperience(Player player) {
        long completedLevels = experienceAtLevel(player.experienceLevel);
        int levelCapacity = player.getXpNeededForNextLevel();
        long currentLevel = Math.max(0L, Math.min(
                levelCapacity - 1L,
                Math.round(player.experienceProgress * levelCapacity)
        ));
        return (int) Math.min(Integer.MAX_VALUE, completedLevels + currentLevel);
    }

    private static long experienceAtLevel(int level) {
        long value = Math.max(0, level);
        if (value <= 16L) {
            return value * value + 6L * value;
        }
        if (value <= 31L) {
            return (5L * value * value - 81L * value + 720L) / 2L;
        }
        return (9L * value * value - 325L * value + 4_440L) / 2L;
    }

    private static void setTotalExperience(Player player, int experience) {
        int target = Math.max(0, experience);
        int low = 0;
        int high = 1;
        while (high < Integer.MAX_VALUE / 2 && experienceAtLevel(high) <= target) {
            high *= 2;
        }
        while (low + 1 < high) {
            int middle = low + (high - low) / 2;
            if (experienceAtLevel(middle) <= target) {
                low = middle;
            } else {
                high = middle;
            }
        }

        int level = low;
        int progressPoints = target - (int) Math.min(Integer.MAX_VALUE, experienceAtLevel(level));
        int levelCapacity = xpNeededForLevel(level);
        player.experienceLevel = level;
        player.experienceProgress = levelCapacity == 0 ? 0.0F : (float) progressPoints / (float) levelCapacity;
        player.totalExperience = target;
    }

    private static int xpNeededForLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        }
        if (level >= 15) {
            return 37 + (level - 15) * 5;
        }
        return 7 + level * 2;
    }

    private static ItemStack replaceOne(Player player, InteractionHand hand, ItemStack stack, Item replacement) {
        stack.shrink(1);
        ItemStack output = new ItemStack(replacement);
        if (stack.isEmpty()) {
            player.setItemInHand(hand, output);
            return output;
        }
        if (!player.getInventory().add(output)) {
            player.drop(output, false);
        }
        return stack;
    }

    private static HbmFluidDefinition experienceFluid() {
        return HbmFluids.byName("xpjuice").orElse(HbmFluids.none());
    }

    private static FluidStack experienceFluidStack() {
        HbmFluidDefinition fluid = experienceFluid();
        return fluid.isNone() ? FluidStack.EMPTY : HbmFluids.toNeoStack(fluid, FLUID_AMOUNT_MB);
    }

    private static boolean isExperienceFluid(FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
        return !fluid.isNone() && fluid.name().equals("xpjuice");
    }

    private static final class Handler implements IFluidHandlerItem {
        private ItemStack container;

        private Handler(ItemStack container) {
            this.container = container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 && isFull() ? experienceFluidStack() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? FLUID_AMOUNT_MB : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && isEmpty() && isExperienceFluid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (this.container.getCount() != 1 || !isEmpty()
                    || resource.getAmount() < FLUID_AMOUNT_MB || !isExperienceFluid(resource)) {
                return 0;
            }
            if (action.execute()) {
                this.container = new ItemStack(HbmItems.IV_XP.get());
            }
            return FLUID_AMOUNT_MB;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getAmount() < FLUID_AMOUNT_MB) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = getFluidInTank(0);
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, resource)) {
                return FluidStack.EMPTY;
            }
            return drain(FLUID_AMOUNT_MB, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (this.container.getCount() != 1 || !isFull() || maxDrain < FLUID_AMOUNT_MB) {
                return FluidStack.EMPTY;
            }
            FluidStack stored = experienceFluidStack();
            if (stored.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (action.execute()) {
                this.container = new ItemStack(HbmItems.IV_XP_EMPTY.get());
            }
            return stored;
        }

        @Override
        public ItemStack getContainer() {
            return this.container;
        }

        private boolean isFull() {
            return this.container.is(HbmItems.IV_XP.get());
        }

        private boolean isEmpty() {
            return this.container.is(HbmItems.IV_XP_EMPTY.get());
        }
    }
}
