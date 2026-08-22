package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.DfcComponentBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.menu.DfcInjectorMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidContainerTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public class DfcInjectorBlockEntity extends DfcInventoryBlockEntity implements MenuProvider {
    public static final int INPUT_0 = 0;
    public static final int OUTPUT_0 = 1;
    public static final int INPUT_1 = 2;
    public static final int OUTPUT_1 = 3;
    public static final int SLOT_COUNT = 4;
    public static final int TANK_CAPACITY = 128_000;
    public static final int RANGE = 15;
    public static final int DATA_COUNT = 8;

    private final HbmFluidTank[] tanks = {
            new HbmFluidTank(fluid("deuterium"), TANK_CAPACITY),
            new HbmFluidTank(fluid("tritium"), TANK_CAPACITY)
    };
    private final IFluidHandler fluidHandler = new DfcFluidHandler(this.tanks, DfcCoreBlockEntity::isValidFuel);
    private int beam;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> DfcInjectorBlockEntity.this.tanks[0].type().oldId();
                case 1 -> DfcInjectorBlockEntity.this.tanks[0].amount();
                case 2 -> DfcInjectorBlockEntity.this.tanks[0].capacity();
                case 3 -> DfcInjectorBlockEntity.this.tanks[1].type().oldId();
                case 4 -> DfcInjectorBlockEntity.this.tanks[1].amount();
                case 5 -> DfcInjectorBlockEntity.this.tanks[1].capacity();
                case 6 -> DfcInjectorBlockEntity.this.beam;
                case 7 -> 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> DfcInjectorBlockEntity.this.tanks[0].setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 1 -> DfcInjectorBlockEntity.this.tanks[0].setAmount(value);
                case 3 -> DfcInjectorBlockEntity.this.tanks[1].setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 4 -> DfcInjectorBlockEntity.this.tanks[1].setAmount(value);
                case 6 -> DfcInjectorBlockEntity.this.beam = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public DfcInjectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.DFC_INJECTOR.get(), pos, blockState, SLOT_COUNT);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DfcInjectorBlockEntity injector) {
        if (!level.isClientSide) {
            injector.tickServer(level);
        }
    }

    public HbmFluidTank tank(int index) {
        return this.tanks[index];
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return this.fluidHandler;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public int beam() {
        return this.beam;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_0 -> HbmFluidContainerTransfer.canDrainIntoTank(stack, this.tanks[0], DfcCoreBlockEntity::isValidFuel, output -> canPlaceOutput(OUTPUT_0, output));
            case INPUT_1 -> HbmFluidContainerTransfer.canDrainIntoTank(stack, this.tanks[1], DfcCoreBlockEntity::isValidFuel, output -> canPlaceOutput(OUTPUT_1, output));
            default -> false;
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.dfc_injector");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DfcInjectorMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fuel1", this.tanks[0].save());
        tag.put("Fuel2", this.tanks[1].save());
        tag.putInt("Beam", this.beam);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tanks[0].load(tag.getCompound("Fuel1"));
        this.tanks[1].load(tag.getCompound("Fuel2"));
        if (this.tanks[0].type().isNone() && this.tanks[0].amount() == 0) {
            this.tanks[0].setType(fluid("deuterium"));
        }
        if (this.tanks[1].type().isNone() && this.tanks[1].amount() == 0) {
            this.tanks[1].setType(fluid("tritium"));
        }
        this.beam = tag.getInt("Beam");
    }

    private void tickServer(Level level) {
        boolean changed = transferContainers();
        this.beam = 0;
        Direction direction = facing();
        for (int distance = 1; distance <= RANGE; distance++) {
            BlockPos targetPos = this.worldPosition.relative(direction, distance);
            BlockEntity target = level.getBlockEntity(targetPos);
            if (target instanceof DfcCoreBlockEntity core) {
                for (int index = 0; index < this.tanks.length; index++) {
                    changed |= transferTankToCore(this.tanks[index], core.tank(index));
                }
                this.beam = distance;
                break;
            }
            if (!level.getBlockState(targetPos).isAir()) {
                break;
            }
        }
        if (changed || level.getGameTime() % 10L == 0L || this.beam > 0) {
            setChangedAndSync(true);
        }
    }

    private boolean transferContainers() {
        boolean changed = HbmFluidContainerTransfer.drainIntoTank(
                this.items.get(INPUT_0),
                this.tanks[0],
                DfcCoreBlockEntity::isValidFuel,
                output -> canPlaceOutput(OUTPUT_0, output),
                output -> placeOutput(OUTPUT_0, output)
        );
        changed |= HbmFluidContainerTransfer.drainIntoTank(
                this.items.get(INPUT_1),
                this.tanks[1],
                DfcCoreBlockEntity::isValidFuel,
                output -> canPlaceOutput(OUTPUT_1, output),
                output -> placeOutput(OUTPUT_1, output)
        );
        return changed;
    }

    private boolean transferTankToCore(HbmFluidTank source, HbmFluidTank target) {
        if (source.amount() <= 0 || source.type().isNone()) {
            return false;
        }
        if (target.amount() > 0 && target.type() != source.type()) {
            return false;
        }
        int moved = target.fill(source.type(), source.amount(), true);
        if (moved <= 0) {
            return false;
        }
        source.drain(source.type(), moved, false);
        target.fill(source.type(), moved, false);
        return true;
    }

    private boolean canPlaceOutput(int slot, ItemStack output) {
        if (output.isEmpty()) {
            return true;
        }
        ItemStack current = this.items.get(slot);
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, output) && current.getCount() + output.getCount() <= current.getMaxStackSize());
    }

    private void placeOutput(int slot, ItemStack output) {
        if (output.isEmpty()) {
            return;
        }
        ItemStack current = this.items.get(slot);
        if (current.isEmpty()) {
            this.items.set(slot, output.copy());
        } else if (ItemStack.isSameItemSameComponents(current, output)) {
            current.grow(output.getCount());
        }
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DfcComponentBlock.FACING) ? state.getValue(DfcComponentBlock.FACING) : Direction.NORTH;
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }
}
