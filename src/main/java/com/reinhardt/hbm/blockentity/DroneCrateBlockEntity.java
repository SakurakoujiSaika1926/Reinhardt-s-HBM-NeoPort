package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.entity.LegacyDeliveryDroneEntity;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.DroneCrateMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** 1.7.10 transport drone crate: 18 cargo slots, a fluid identifier and a 64 B tank. */
public final class DroneCrateBlockEntity extends DroneInventoryBlockEntity implements DroneLinkable, MenuProvider {
    public static final int CARGO_SLOTS = 18;
    public static final int IDENTIFIER_SLOT = 18;
    public static final int FLUID_CAPACITY = 64_000;

    private final HbmFluidTank tank = new HbmFluidTank(FLUID_CAPACITY);
    @Nullable
    private BlockPos nextTarget;
    private boolean sendingMode;
    private boolean itemType = true;
    private final int[] automationSlots = slots(0, CARGO_SLOTS);

    public DroneCrateBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DRONE_CRATE.get(), pos, state, CARGO_SLOTS + 1);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DroneCrateBlockEntity crate) {
        if (!level.isClientSide) {
            crate.serverTick(level);
        }
    }

    @Override
    public BlockPos dronePoint() {
        return worldPosition.above();
    }

    @Override
    public void setDroneTarget(BlockPos target) {
        nextTarget = target.immutable();
        sync();
    }

    public HbmFluidTank tank() {
        return tank;
    }

    public boolean sendingMode() {
        return sendingMode;
    }

    public boolean itemType() {
        return itemType;
    }

    public void toggleSendingMode() {
        sendingMode = !sendingMode;
        sync();
    }

    public void togglePayloadType() {
        itemType = !itemType;
        sync();
    }

    @Nullable
    public BlockPos nextTarget() {
        return nextTarget;
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int index) {
                return index == 0 ? tank.getFluidInTank(0) : FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int index) {
                return index == 0 ? FLUID_CAPACITY : 0;
            }

            @Override
            public boolean isFluidValid(int index, FluidStack stack) {
                return index == 0 && !itemType && sendingMode && tank.isFluidValid(0, stack);
            }

            @Override
            public int fill(FluidStack stack, FluidAction action) {
                if (itemType || !sendingMode) {
                    return 0;
                }
                int filled = tank.fill(stack, action);
                if (filled > 0 && action.execute()) {
                    sync();
                }
                return filled;
            }

            @Override
            public FluidStack drain(FluidStack stack, FluidAction action) {
                if (itemType || sendingMode) {
                    return FluidStack.EMPTY;
                }
                FluidStack drained = tank.drain(stack, action);
                if (!drained.isEmpty() && action.execute()) {
                    sync();
                }
                return drained;
            }

            @Override
            public FluidStack drain(int amount, FluidAction action) {
                if (itemType || sendingMode) {
                    return FluidStack.EMPTY;
                }
                FluidStack drained = tank.drain(amount, action);
                if (!drained.isEmpty() && action.execute()) {
                    sync();
                }
                return drained;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.drone_crate");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new DroneCrateMenu(id, inventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return automationSlots;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != IDENTIFIER_SLOT || stack.getItem() instanceof FluidIdentifierItem;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot >= 0 && slot < CARGO_SLOTS;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.save());
        tag.putBoolean("sending_mode", sendingMode);
        tag.putBoolean("item_type", itemType);
        if (nextTarget != null) {
            tag.putLong("next", nextTarget.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.load(tag.getCompound("tank"));
        sendingMode = tag.getBoolean("sending_mode");
        itemType = !tag.contains("item_type") || tag.getBoolean("item_type");
        nextTarget = tag.contains("next") ? BlockPos.of(tag.getLong("next")) : null;
    }

    private void serverTick(Level level) {
        ItemStack identifier = items.get(IDENTIFIER_SLOT);
        if (identifier.getItem() instanceof FluidIdentifierItem) {
            tank.setType(FluidIdentifierItem.primary(identifier));
        }
        if (nextTarget == null) {
            return;
        }

        AABB dockSpace = new AABB(worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 2, worldPosition.getZ() + 1);
        for (LegacyDeliveryDroneEntity drone : level.getEntitiesOfClass(LegacyDeliveryDroneEntity.class, dockSpace)) {
            if (drone.getDeltaMovement().lengthSqr() >= 0.0025D) {
                continue;
            }
            drone.setTarget(nextTarget.getX() + 0.5D, nextTarget.getY(), nextTarget.getZ() + 0.5D);
            if (sendingMode && itemType) {
                loadItems(level, drone);
            } else if (!sendingMode && itemType) {
                unloadItems(level, drone);
            } else if (sendingMode) {
                loadFluid(level, drone);
            } else {
                unloadFluid(level, drone);
            }
        }
    }

    private void loadItems(Level level, LegacyDeliveryDroneEntity drone) {
        if (drone.appearance() != 0) {
            return;
        }
        boolean loaded = false;
        for (int slot = 0; slot < CARGO_SLOTS; slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                drone.setCargo(slot, stack);
                items.set(slot, ItemStack.EMPTY);
                loaded = true;
            }
        }
        if (loaded) {
            drone.setAppearance(1);
            unpack(level);
            sync();
        }
    }

    private void unloadItems(Level level, LegacyDeliveryDroneEntity drone) {
        if (drone.appearance() != 1) {
            return;
        }
        boolean emptied = true;
        for (int slot = 0; slot < CARGO_SLOTS; slot++) {
            ItemStack stack = drone.getCargo(slot);
            if (items.get(slot).isEmpty() && !stack.isEmpty()) {
                items.set(slot, stack.copy());
                drone.setCargo(slot, ItemStack.EMPTY);
            } else if (!items.get(slot).isEmpty() && !stack.isEmpty()) {
                emptied = false;
            }
        }
        if (emptied) {
            drone.setAppearance(0);
            unpack(level);
        }
        sync();
    }

    private void loadFluid(Level level, LegacyDeliveryDroneEntity drone) {
        if (drone.appearance() != 0 || tank.amount() <= 0) {
            return;
        }
        drone.setFluidPayload(tank.type(), tank.amount());
        tank.setAmount(0);
        drone.setAppearance(2);
        unpack(level);
        sync();
    }

    private void unloadFluid(Level level, LegacyDeliveryDroneEntity drone) {
        if (drone.appearance() != 2 || drone.fluidAmount() <= 0 || drone.fluidType() != tank.type()) {
            return;
        }
        int accepted = tank.fill(drone.fluidType(), drone.fluidAmount(), false);
        if (accepted <= 0) {
            return;
        }
        drone.setFluidPayload(drone.fluidType(), drone.fluidAmount() - accepted);
        if (drone.fluidAmount() <= 0) {
            drone.clearFluidPayload();
            drone.setAppearance(0);
        }
        unpack(level);
        sync();
    }

    private void unpack(Level level) {
        level.playSound(null, worldPosition, HbmSoundEvents.ITEM_UNPACK.get(), SoundSource.BLOCKS, 0.5F, 0.75F);
    }

    private static int[] slots(int startInclusive, int endExclusive) {
        int[] slots = new int[endExclusive - startInclusive];
        for (int index = 0; index < slots.length; index++) {
            slots[index] = startInclusive + index;
        }
        return slots;
    }
}
