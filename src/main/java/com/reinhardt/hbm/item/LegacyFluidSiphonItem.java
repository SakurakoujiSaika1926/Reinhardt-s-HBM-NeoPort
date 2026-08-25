package com.reinhardt.hbm.item;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** Direct 1.7.10 ItemFluidSiphon port using the modern fluid capability. */
public final class LegacyFluidSiphonItem extends Item {
    private static final int BUCKET = 1_000;

    public LegacyFluidSiphonItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        IFluidHandler tanks = fluidHandler(level, context.getClickedPos(), context.getClickedFace());
        if (tanks == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        for (int tank = 0; tank < tanks.getTanks(); tank++) {
            FluidStack stored = tanks.getFluidInTank(tank);
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stored.getFluid()).orElse(HbmFluids.none());
            if (stored.isEmpty() || fluid.isNone() || fluid.hasTrait(HbmFluidTrait.UNSIPHONABLE)) {
                continue;
            }

            boolean drained = fillFullContainers(player, tanks, stored);
            FluidStack remaining = tanks.getFluidInTank(tank);
            if (remaining.getAmount() < BUCKET) {
                drained |= fillPipette(player, tanks, remaining);
            }
            // 1.7.10 intentionally stopped after draining one compatible tank.
            if (drained) {
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    private static IFluidHandler fluidHandler(Level level, BlockPos pos, Direction clickedFace) {
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        return handler != null ? handler : level.getCapability(Capabilities.FluidHandler.BLOCK, pos, clickedFace);
    }

    private static boolean fillFullContainers(Player player, IFluidHandler tanks, FluidStack stored) {
        Inventory inventory = player.getInventory();
        boolean drainedAny = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            while (!stack.isEmpty()) {
                FluidStack available = tanks.drain(stored.copyWithAmount(Integer.MAX_VALUE), IFluidHandler.FluidAction.SIMULATE);
                if (available.isEmpty()) {
                    break;
                }
                ItemStack emptyContainer = stack.copyWithCount(1);
                IFluidHandlerItem container = FluidUtil.getFluidHandler(emptyContainer).orElse(null);
                if (container == null || !container.getFluidInTank(0).isEmpty()) {
                    break;
                }
                int capacity = container.getTankCapacity(0);
                if (capacity <= 0 || available.getAmount() < capacity) {
                    break;
                }
                FluidStack fill = available.copyWithAmount(capacity);
                if (container.fill(fill, IFluidHandler.FluidAction.SIMULATE) != capacity
                        || tanks.drain(fill, IFluidHandler.FluidAction.SIMULATE).getAmount() != capacity) {
                    break;
                }

                FluidStack drained = tanks.drain(fill, IFluidHandler.FluidAction.EXECUTE);
                if (drained.getAmount() != capacity || container.fill(fill, IFluidHandler.FluidAction.EXECUTE) != capacity) {
                    break;
                }
                stack.shrink(1);
                inventory.setItem(slot, stack);
                giveFilledContainer(player, container.getContainer());
                drainedAny = true;
            }
        }
        return drainedAny;
    }

    private static boolean fillPipette(Player player, IFluidHandler tanks, FluidStack stored) {
        if (stored.isEmpty()) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof LegacyPipetteItem pipette)
                    || pipette.kind() == LegacyPipetteItem.Kind.LABORATORY) {
                continue;
            }
            IFluidHandlerItem container = FluidUtil.getFluidHandler(stack).orElse(null);
            if (container == null) {
                continue;
            }
            int accepted = container.fill(stored, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) {
                continue;
            }
            FluidStack requested = stored.copyWithAmount(accepted);
            if (tanks.drain(requested, IFluidHandler.FluidAction.SIMULATE).getAmount() != accepted) {
                continue;
            }
            FluidStack drained = tanks.drain(requested, IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() != accepted || container.fill(requested, IFluidHandler.FluidAction.EXECUTE) != accepted) {
                return false;
            }
            inventory.setItem(slot, container.getContainer());
            return true;
        }
        return false;
    }

    private static void giveFilledContainer(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
