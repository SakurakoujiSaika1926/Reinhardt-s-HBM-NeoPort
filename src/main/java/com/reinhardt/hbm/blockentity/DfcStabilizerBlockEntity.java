package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.DfcComponentBlock;
import com.reinhardt.hbm.item.AmsLensItem;
import com.reinhardt.hbm.menu.DfcStabilizerMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DfcStabilizerBlockEntity extends DfcInventoryBlockEntity implements MenuProvider, PowerEndpoint {
    public static final int LENS_SLOT = 0;
    public static final int SLOT_COUNT = 1;
    public static final long MAX_POWER = 2_500_000_000L;
    public static final int RANGE = 15;
    public static final int DATA_COUNT = 8;

    private long power;
    private long lastInput;
    private int watts = 1;
    private int beam;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) DfcStabilizerBlockEntity.this.power;
                case 1 -> (int) MAX_POWER;
                case 2 -> DfcStabilizerBlockEntity.this.watts;
                case 3 -> DfcStabilizerBlockEntity.this.beam;
                case 4 -> (int) AmsLensItem.lensDamage(DfcStabilizerBlockEntity.this.items.get(LENS_SLOT));
                case 5 -> (int) AmsLensItem.MAX_DAMAGE;
                case 6 -> demand() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) demand();
                case 7 -> (int) DfcStabilizerBlockEntity.this.lastInput;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> DfcStabilizerBlockEntity.this.power = Integer.toUnsignedLong(value);
                case 2 -> DfcStabilizerBlockEntity.this.watts = value;
                case 3 -> DfcStabilizerBlockEntity.this.beam = value;
                case 7 -> DfcStabilizerBlockEntity.this.lastInput = Integer.toUnsignedLong(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public DfcStabilizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.DFC_STABILIZER.get(), pos, blockState, SLOT_COUNT);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DfcStabilizerBlockEntity stabilizer) {
        if (!level.isClientSide) {
            stabilizer.tickServer(level);
        }
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public int watts() {
        return this.watts;
    }

    public int beam() {
        return this.beam;
    }

    public void setWatts(int watts) {
        this.watts = clampWatts(watts);
        setChangedAndSync(true);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == LENS_SLOT && stack.getItem() instanceof AmsLensItem;
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return Math.max(0L, MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        this.lastInput = receivedInput;
        this.power = Math.min(MAX_POWER, this.power + Math.max(0L, receivedInput));
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(this.power + "/" + MAX_POWER + " HE");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.dfc_stabilizer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DfcStabilizerMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putInt("Watts", this.watts);
        tag.putInt("Beam", this.beam);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.lastInput = Math.max(0L, tag.getLong("LastInput"));
        this.watts = clampWatts(tag.getInt("Watts"));
        this.beam = tag.getInt("Beam");
    }

    private void tickServer(Level level) {
        PowerNetworkManager.tickFromEndpoint(level, this);
        this.watts = clampWatts(this.watts);
        this.beam = 0;

        long demand = demand();
        ItemStack lens = this.items.get(LENS_SLOT);
        if (this.power >= demand && AmsLensItem.isValidLens(lens)) {
            Direction direction = facing();
            for (int distance = 1; distance <= RANGE; distance++) {
                BlockPos targetPos = this.worldPosition.relative(direction, distance);
                BlockEntity target = level.getBlockEntity(targetPos);
                if (target instanceof DfcCoreBlockEntity core) {
                    core.stabilize(this.watts);
                    this.power -= demand;
                    this.beam = distance;
                    long damage = AmsLensItem.lensDamage(lens) + this.watts;
                    if (damage >= AmsLensItem.MAX_DAMAGE) {
                        this.items.set(LENS_SLOT, ItemStack.EMPTY);
                    } else {
                        AmsLensItem.setLensDamage(lens, damage);
                    }
                    break;
                }
                if (!level.getBlockState(targetPos).isAir()) {
                    break;
                }
            }
        }

        setChangedAndSync(level.getGameTime() % 10L == 0L || this.beam > 0);
    }

    private long demand() {
        long watts = clampWatts(this.watts);
        return watts * watts * watts * watts;
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DfcComponentBlock.FACING) ? state.getValue(DfcComponentBlock.FACING) : Direction.NORTH;
    }

    private static int clampWatts(int watts) {
        return Math.max(1, Math.min(100, watts));
    }
}
