package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.NuclearWasteItem;
import com.reinhardt.hbm.menu.StorageDrumMenu;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.radiation.HbmHazardSystem;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 1.7.10 TileEntityStorageDrum port. The old metadata waste class is retained
 * in {@link NuclearWasteItem}; the tank amounts and decay timing are unchanged.
 */
public final class StorageDrumBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider {
    public static final int SLOT_COUNT = 24;
    public static final int TANK_CAPACITY = 16_000;
    private static final int LONG_DECAY_TICKS = 3 * 60 * 60 * 20;
    private static final int SHORT_DECAY_TICKS = 15 * 60 * 20;
    private static final int OUTPUT_PER_SIDE = 16_000;
    private static final int[] SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank liquidTank = new HbmFluidTank(wasteFluid(), TANK_CAPACITY);
    private final HbmFluidTank gasTank = new HbmFluidTank(wasteGas(), TANK_CAPACITY);
    private final IFluidHandler fluidHandler = new WasteOutputHandler();
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> liquidTank.amount();
                case 1 -> liquidTank.capacity();
                case 2 -> gasTank.amount();
                case 3 -> gasTank.capacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public StorageDrumBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.STORAGE_DRUM.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StorageDrumBlockEntity drum) {
        drum.tickServer(level);
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public HbmFluidTank liquidTank() {
        return this.liquidTank;
    }

    public HbmFluidTank gasTank() {
        return this.gasTank;
    }

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return this.fluidHandler;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return validSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = validSlot(slot) ? ContainerHelper.removeItem(this.items, slot, amount) : ItemStack.EMPTY;
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return validSlot(slot) ? ContainerHelper.takeItem(this.items, slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!validSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return validSlot(slot) && isInput(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isOutput(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.storage_drum");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new StorageDrumMenu(containerId, inventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = this.items.get(slot);
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
                this.items.set(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.put("LiquidTank", this.liquidTank.save());
        tag.put("GasTank", this.gasTank.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.liquidTank.load(tag.getCompound("LiquidTank"));
        this.gasTank.load(tag.getCompound("GasTank"));
        ensureFixedTanks();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void tickServer(Level level) {
        ensureFixedTanks();
        int liquid = 0;
        int gas = 0;
        double radiation = 0.0D;
        boolean changed = false;
        boolean radiationTick = level.getGameTime() % 20L == 0L;

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = this.items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (radiationTick) {
                radiation += HbmHazardSystem.rawRadiation(stack);
            }

            DecayResult result = decay(stack, level);
            if (result == null) {
                continue;
            }
            this.items.set(slot, result.output());
            liquid += result.liquid();
            gas += result.gas();
            changed = true;
        }

        boolean tankChanged = addOutput(this.liquidTank, wasteFluid(), liquid)
                | addOutput(this.gasTank, wasteGas(), gas);
        tankChanged |= pushOutput(level, this.liquidTank, wasteFluid());
        tankChanged |= pushOutput(level, this.gasTank, wasteGas());

        if (radiationTick && radiation > 0.0D) {
            radiate(level, radiation);
        }
        if (changed || tankChanged) {
            sync();
        }
    }

    private DecayResult decay(ItemStack stack, Level level) {
        String id = idOf(stack);
        int longChance = HbmConfig.ENABLE_528_MODE.get() ? 15 * 60 * 60 * 20 : LONG_DECAY_TICKS;
        int shortChance = HbmConfig.ENABLE_528_MODE.get() ? 3 * 60 * 60 * 20 : SHORT_DECAY_TICKS;

        if (id.equals("nuclear_waste_long") && level.random.nextInt(longChance) == 0) {
            NuclearWasteItem.WasteClass waste = NuclearWasteItem.classOf(stack);
            return wasteDecay(stack, "nuclear_waste_long_depleted", waste.liquid(), waste.gas());
        }
        if (id.equals("nuclear_waste_long_tiny") && level.random.nextInt(longChance / 10) == 0) {
            NuclearWasteItem.WasteClass waste = NuclearWasteItem.classOf(stack);
            return wasteDecay(stack, "nuclear_waste_long_depleted_tiny", waste.liquid() / 10, waste.gas() / 10);
        }
        if (id.equals("nuclear_waste_short") && level.random.nextInt(shortChance) == 0) {
            NuclearWasteItem.WasteClass waste = NuclearWasteItem.classOf(stack);
            return wasteDecay(stack, "nuclear_waste_short_depleted", waste.liquid(), waste.gas());
        }
        if (id.equals("nuclear_waste_short_tiny") && level.random.nextInt(shortChance / 10) == 0) {
            NuclearWasteItem.WasteClass waste = NuclearWasteItem.classOf(stack);
            return wasteDecay(stack, "nuclear_waste_short_depleted_tiny", waste.liquid() / 10, waste.gas() / 10);
        }
        if (id.equals("ingot_au198") && level.random.nextInt(shortChance / 100) == 0) {
            return new DecayResult(new ItemStack(stackFor("bottle_mercury", stack)), 500, 500);
        }
        if (id.equals("nugget_au198") && level.random.nextInt(shortChance / 1_000) == 0) {
            return new DecayResult(new ItemStack(stackFor("nugget_mercury", stack)), 50, 50);
        }
        if (id.equals("ingot_pb209") && level.random.nextInt(shortChance / 10) == 0) {
            return new DecayResult(new ItemStack(stackFor("ingot_bismuth", stack)), 0, 0);
        }
        if (id.equals("nugget_pb209") && level.random.nextInt(shortChance / 50) == 0) {
            return new DecayResult(new ItemStack(stackFor("nugget_bismuth", stack)), 0, 0);
        }
        if (id.equals("powder_sr90") && level.random.nextInt(shortChance / 10) == 0) {
            return new DecayResult(new ItemStack(stackFor("powder_zirconium", stack)), 0, 0);
        }
        if (id.equals("nugget_sr90") && level.random.nextInt(shortChance / 50) == 0) {
            return new DecayResult(new ItemStack(stackFor("nugget_zirconium", stack)), 0, 0);
        }
        return null;
    }

    private static DecayResult wasteDecay(ItemStack source, String outputId, int liquid, int gas) {
        Item target = item(outputId);
        return new DecayResult(NuclearWasteItem.copyWasteClass(source, target), liquid, gas);
    }

    private boolean addOutput(HbmFluidTank tank, HbmFluidDefinition type, int amount) {
        if (amount <= 0) {
            return false;
        }
        int total = tank.amount() + amount;
        int overflow = Math.max(0, total - tank.capacity());
        tank.setAmount(Math.min(total, tank.capacity()));
        if (overflow > 0 && this.level instanceof ServerLevel serverLevel) {
            ChunkRadiationData.get(serverLevel).incrementRadiation(this.worldPosition, overflow * HbmHazardSystem.fluidRadiation(type));
        }
        return true;
    }

    private boolean pushOutput(Level level, HbmFluidTank tank, HbmFluidDefinition type) {
        boolean changed = false;
        for (Direction direction : Direction.values()) {
            if (tank.amount() <= 0) {
                break;
            }
            int offered = Math.min(OUTPUT_PER_SIDE, tank.amount());
            int accepted = HbmFluidNetworks.fillInto(
                    level,
                    this.worldPosition.relative(direction),
                    direction.getOpposite(),
                    HbmFluids.toNeoStack(type, offered),
                    this.worldPosition,
                    true
            );
            if (accepted > 0) {
                tank.drain(type, accepted, false);
                changed = true;
            }
        }
        return changed;
    }

    private void radiate(Level level, double rads) {
        Vec3 source = Vec3.atCenterOf(this.worldPosition);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(source, source).inflate(32.0D));
        for (LivingEntity entity : entities) {
            Vec3 target = entity.getEyePosition();
            Vec3 delta = target.subtract(source);
            double length = delta.length();
            if (length <= 0.001D) {
                length = 0.001D;
            }
            Vec3 direction = delta.scale(1.0D / length);
            float resistance = 0.0F;
            for (int distance = 1; distance < length; distance++) {
                BlockPos sample = BlockPos.containing(source.add(direction.scale(distance)));
                resistance += level.getBlockState(sample).getBlock().getExplosionResistance();
            }
            resistance = Math.max(1.0F, resistance);
            float dose = (float) (rads / resistance / (length * length));
            HbmLivingRadiation radiation = HbmLivingRadiation.get(entity);
            radiation.addEnvironmentRadiation(dose);
            radiation.addRadiation(dose);
        }
    }

    private void ensureFixedTanks() {
        HbmFluidDefinition liquid = wasteFluid();
        HbmFluidDefinition gas = wasteGas();
        if (this.liquidTank.amount() == 0 && this.liquidTank.type() != liquid) {
            this.liquidTank.setType(liquid);
        }
        if (this.gasTank.amount() == 0 && this.gasTank.type() != gas) {
            this.gasTank.setType(gas);
        }
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isInput(ItemStack stack) {
        String id = idOf(stack);
        return id.equals("nuclear_waste_long")
                || id.equals("nuclear_waste_long_tiny")
                || id.equals("nuclear_waste_short")
                || id.equals("nuclear_waste_short_tiny")
                || id.equals("ingot_au198")
                || id.equals("nugget_au198");
    }

    private static boolean isOutput(ItemStack stack) {
        String id = idOf(stack);
        return id.equals("nuclear_waste_long_depleted")
                || id.equals("nuclear_waste_long_depleted_tiny")
                || id.equals("nuclear_waste_short_depleted")
                || id.equals("nuclear_waste_short_depleted_tiny")
                || id.equals("bottle_mercury")
                || id.equals("nugget_mercury");
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static String idOf(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && ReinhardtsHBM.MOD_ID.equals(id.getNamespace()) ? id.getPath() : "";
    }

    private static Item stackFor(String id, ItemStack fallback) {
        Item item = item(id);
        return item == Items.AIR ? fallback.getItem() : item;
    }

    private static Item item(String id) {
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(id));
        return item == null ? Items.AIR : item;
    }

    private static HbmFluidDefinition wasteFluid() {
        return HbmFluids.byName("wastefluid").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition wasteGas() {
        return HbmFluids.byName("wastegas").orElse(HbmFluids.none());
    }

    private record DecayResult(ItemStack output, int liquid, int gas) {
    }

    private final class WasteOutputHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> liquidTank.getFluidInTank(0);
                case 1 -> gasTank.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 || tank == 1 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition requested = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            HbmFluidTank tank = requested == wasteFluid() ? liquidTank : requested == wasteGas() ? gasTank : null;
            if (tank == null) {
                return FluidStack.EMPTY;
            }
            var drained = tank.drain(requested, resource.getAmount(), action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            HbmFluidTank tank = liquidTank.amount() > 0 ? liquidTank : gasTank;
            HbmFluidDefinition type = tank == liquidTank ? wasteFluid() : wasteGas();
            var drained = tank.drain(type, maxDrain, action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(drained.type(), drained.amount());
        }
    }
}
