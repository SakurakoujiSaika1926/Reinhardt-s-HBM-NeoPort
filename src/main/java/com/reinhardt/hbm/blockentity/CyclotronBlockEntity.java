package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.menu.CyclotronMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.recipe.CyclotronRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.FluidCopiable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CyclotronBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider, FluidCopiable {
    public static final int PARTICLE_A_SLOT = 0;
    public static final int PARTICLE_B_SLOT = 1;
    public static final int PARTICLE_C_SLOT = 2;
    public static final int INPUT_A_SLOT = 3;
    public static final int INPUT_B_SLOT = 4;
    public static final int INPUT_C_SLOT = 5;
    public static final int OUTPUT_A_SLOT = 6;
    public static final int OUTPUT_B_SLOT = 7;
    public static final int OUTPUT_C_SLOT = 8;
    public static final int BATTERY_SLOT = 9;
    public static final int UPGRADE_A_SLOT = 10;
    public static final int UPGRADE_B_SLOT = 11;
    public static final int SLOT_COUNT = 12;
    public static final int DATA_COUNT = 11;
    public static final int DURATION = 690;
    public static final long MAX_POWER = 100_000_000L;
    public static final int BASE_CONSUMPTION = 1_000_000;
    public static final int WATER_CAPACITY = 32_000;
    public static final int SPENT_STEAM_CAPACITY = 32_000;
    public static final int ANTIMATTER_CAPACITY = 8_000;

    private static final int[] PARTICLE_INPUTS = {PARTICLE_A_SLOT, PARTICLE_B_SLOT, PARTICLE_C_SLOT, INPUT_A_SLOT, INPUT_B_SLOT, INPUT_C_SLOT};
    private static final int[] OUTPUTS = {OUTPUT_A_SLOT, OUTPUT_B_SLOT, OUTPUT_C_SLOT};
    private static final int[] BATTERY = {BATTERY_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank waterTank = new HbmFluidTank(fluid("water"), WATER_CAPACITY);
    private final HbmFluidTank spentSteamTank = new HbmFluidTank(fluid("spentsteam"), SPENT_STEAM_CAPACITY);
    private final HbmFluidTank antimatterTank = new HbmFluidTank(fluid("amat"), ANTIMATTER_CAPACITY);
    private long power;
    private int progress;
    private byte plugs;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, power);
                case 1 -> progress;
                case 2 -> Byte.toUnsignedInt(plugs);
                case 3 -> waterTank.amount();
                case 4 -> spentSteamTank.amount();
                case 5 -> antimatterTank.amount();
                case 6 -> WATER_CAPACITY;
                case 7 -> SPENT_STEAM_CAPACITY;
                case 8 -> ANTIMATTER_CAPACITY;
                case 9 -> getConsumption();
                case 10 -> canProcess() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> power = Math.max(0, value);
                case 1 -> progress = Math.max(0, value);
                case 2 -> plugs = (byte) value;
                case 3 -> waterTank.setAmount(value);
                case 4 -> spentSteamTank.setAmount(value);
                case 5 -> antimatterTank.setAmount(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public CyclotronBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CYCLOTRON.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CyclotronBlockEntity cyclotron) {
        if (level.isClientSide) {
            return;
        }
        com.reinhardt.hbm.power.PowerNetworkManager.tickFromEndpoint(level, cyclotron);
        cyclotron.tickServer();
    }

    public static int plugIndex(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (id) {
            case "powder_balefire" -> 0;
            case "book_of_" -> 1;
            case "diamond_gavel" -> 2;
            case "coin_maskman" -> 3;
            default -> -1;
        };
    }

    public boolean getPlug(int index) {
        return index >= 0 && index < 4 && (this.plugs & (1 << index)) != 0;
    }

    public void setPlug(int index) {
        if (index >= 0 && index < 4) {
            this.plugs |= (byte) (1 << index);
            sync();
        }
    }

    private void tickServer() {
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        if (canProcess()) {
            progress += getSpeed();
            power -= getConsumption();
            int convert = getCoolantConsumption();
            waterTank.drain(convert, IFluidHandler.FluidAction.EXECUTE);
            spentSteamTank.fill(fluid("spentsteam"), convert, false);
            if (progress >= DURATION) {
                process();
                progress = 0;
            }
        } else {
            progress = 0;
        }
        outputFluids();
        sync();
    }

    private boolean canProcess() {
        if (power < getConsumption()) {
            return false;
        }
        int convert = getCoolantConsumption();
        if (waterTank.amount() < convert || spentSteamTank.amount() + convert > spentSteamTank.capacity()) {
            return false;
        }
        return currentRecipe(0).isPresent() || currentRecipe(1).isPresent() || currentRecipe(2).isPresent();
    }

    private Optional<RecipeHolder<CyclotronRecipe>> currentRecipe(int lane) {
        if (level == null) {
            return Optional.empty();
        }
        ItemStack particle = items.get(PARTICLE_A_SLOT + lane);
        ItemStack input = items.get(INPUT_A_SLOT + lane);
        if (particle.isEmpty() || input.isEmpty()) {
            return Optional.empty();
        }
        Optional<RecipeHolder<CyclotronRecipe>> recipe = level.getRecipeManager().getRecipeFor(
                HbmRecipeTypes.CYCLOTRON.get(),
                new CyclotronRecipe.Input(particle, input),
                level
        );
        if (recipe.isEmpty()) {
            return Optional.empty();
        }
        ItemStack output = recipe.get().value().output();
        ItemStack slot = items.get(OUTPUT_A_SLOT + lane);
        if (!slot.isEmpty() && (!ItemStack.isSameItemSameComponents(slot, output) || slot.getCount() + output.getCount() > slot.getMaxStackSize())) {
            return Optional.empty();
        }
        if (antimatterTank.amount() + recipe.get().value().antimatter() > antimatterTank.capacity()) {
            return Optional.empty();
        }
        return recipe;
    }

    private void process() {
        for (int lane = 0; lane < 3; lane++) {
            Optional<RecipeHolder<CyclotronRecipe>> holder = currentRecipe(lane);
            if (holder.isEmpty()) {
                continue;
            }
            CyclotronRecipe recipe = holder.get().value();
            items.get(PARTICLE_A_SLOT + lane).shrink(1);
            items.get(INPUT_A_SLOT + lane).shrink(1);
            if (items.get(PARTICLE_A_SLOT + lane).isEmpty()) items.set(PARTICLE_A_SLOT + lane, ItemStack.EMPTY);
            if (items.get(INPUT_A_SLOT + lane).isEmpty()) items.set(INPUT_A_SLOT + lane, ItemStack.EMPTY);
            ItemStack output = recipe.output().copy();
            ItemStack slot = items.get(OUTPUT_A_SLOT + lane);
            if (slot.isEmpty()) {
                items.set(OUTPUT_A_SLOT + lane, output);
            } else {
                slot.grow(output.getCount());
            }
            antimatterTank.fill(fluid("amat"), recipe.antimatter(), false);
        }
    }

    public int getSpeed() {
        return 1;
    }

    public int getConsumption() {
        return BASE_CONSUMPTION;
    }

    public int getCoolantConsumption() {
        return 500 * getSpeed();
    }

    public HbmFluidTank waterTank() {
        return waterTank;
    }

    public HbmFluidTank spentSteamTank() {
        return spentSteamTank;
    }

    public HbmFluidTank antimatterTank() {
        return antimatterTank;
    }

    public long power() {
        return power;
    }

    public int progress() {
        return progress;
    }

    public ContainerData menuData() {
        return menuData;
    }

    @Override
    public BlockPos getPowerPos() {
        return worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return connectorPositions();
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return connectorPositions().contains(connectorPos);
    }

    private List<BlockPos> connectorPositions() {
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (BlockPos offset : List.of(
                new BlockPos(3, 0, 1), new BlockPos(3, 0, -1),
                new BlockPos(-3, 0, 1), new BlockPos(-3, 0, -1),
                new BlockPos(1, 0, 3), new BlockPos(-1, 0, 3),
                new BlockPos(1, 0, -3), new BlockPos(-1, 0, -3)
        )) {
            positions.add(worldPosition.offset(rotate(offset)));
        }
        return List.copyOf(positions);
    }

    private BlockPos rotate(BlockPos pos) {
        Direction facing = getBlockState().getValue(LargeMachineBlock.FACING);
        return switch (facing) {
            case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
            case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
            case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
            default -> pos;
        };
    }

    @Override
    public long getAvailableOutput() {
        return 0;
    }

    @Override
    public long getRequestedInput() {
        return Math.max(0L, MAX_POWER - power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        if (receivedInput > 0) {
            power = Math.min(MAX_POWER, power + receivedInput);
            sync();
        }
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(power + " / " + MAX_POWER + " HE");
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        return new FluidPortHandler(queriedPos);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) sync();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= items.size()) return;
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        sync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= PARTICLE_A_SLOT && slot <= INPUT_C_SLOT) return true;
        if (slot == BATTERY_SLOT) return BatteryPackItem.isBattery(stack);
        return slot == UPGRADE_A_SLOT || slot == UPGRADE_B_SLOT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUTS : side == Direction.UP ? PARTICLE_INPUTS : BATTERY;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_A_SLOT && slot <= OUTPUT_C_SLOT;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        sync();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_cyclotron");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CyclotronMenu(containerId, inventory, this, menuData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
        tag.putByte("plugs", plugs);
        tag.put("water", waterTank.save());
        tag.put("spentSteam", spentSteamTank.save());
        tag.put("antimatter", antimatterTank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("power")));
        progress = Math.max(0, tag.getInt("progress"));
        plugs = tag.getByte("plugs");
        waterTank.load(tag.getCompound("water"));
        spentSteamTank.load(tag.getCompound("spentSteam"));
        antimatterTank.load(tag.getCompound("antimatter"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int[] getFluidIdsToCopy() {
        return new int[] { waterTank.type().oldId(), spentSteamTank.type().oldId(), antimatterTank.type().oldId() };
    }

    @Override
    public void pasteFluidSetting(HbmFluidDefinition fluid, Level level, Player player, BlockPos pos) {
        waterTank.conform(fluid, 0);
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void outputFluids() {
        // Fluid network pull/push is handled through exposed tank handlers.
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }

    private final class FluidPortHandler implements IFluidHandler {
        private final BlockPos queriedPos;

        private FluidPortHandler(BlockPos queriedPos) {
            this.queriedPos = queriedPos;
        }

        @Override public int getTanks() { return 3; }
        @Override public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) { return tank(tank).getFluidInTank(0); }
        @Override public int getTankCapacity(int tank) { return tank(tank).capacity(); }
        @Override public boolean isFluidValid(int tank, net.neoforged.neoforge.fluids.FluidStack stack) {
            return tank == 0 && HbmFluids.fromNeoFluid(stack.getFluid()).filter(def -> def == waterTank.type()).isPresent();
        }
        @Override public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            return waterTank.fill(resource, action);
        }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            if (HbmFluids.fromNeoFluid(resource.getFluid()).filter(def -> def == spentSteamTank.type()).isPresent()) return spentSteamTank.drain(resource.getAmount(), action);
            if (HbmFluids.fromNeoFluid(resource.getFluid()).filter(def -> def == antimatterTank.type()).isPresent()) return antimatterTank.drain(resource.getAmount(), action);
            return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
        }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (antimatterTank.amount() > 0) return antimatterTank.drain(maxDrain, action);
            return spentSteamTank.drain(maxDrain, action);
        }
        private HbmFluidTank tank(int index) {
            return switch (index) {
                case 1 -> spentSteamTank;
                case 2 -> antimatterTank;
                default -> waterTank;
            };
        }
    }
}
