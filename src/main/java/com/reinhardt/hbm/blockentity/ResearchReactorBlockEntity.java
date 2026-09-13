package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.item.PlateFuelItem;
import com.reinhardt.hbm.item.WasteFuelItem;
import com.reinhardt.hbm.menu.ResearchReactorMenu;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.event.LegacyMobSpawnEvents;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Map;

public class ResearchReactorBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer, MenuProvider, NeutronFluxProvider {
    public static final int SLOT_COUNT = 12;
    public static final int DATA_COUNT = 6;
    public static final int MAX_HEAT = 50_000;
    public static final int LEVEL_SCALE = 10_000;

    private static final int[] AUTOMATION_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    private static final Map<String, String> DEPLETED_FUELS = Map.of(
            "plate_fuel_u233", "waste_plate_u233",
            "plate_fuel_u235", "waste_plate_u235",
            "plate_fuel_mox", "waste_plate_mox",
            "plate_fuel_pu239", "waste_plate_pu239",
            "plate_fuel_sa326", "waste_plate_sa326",
            "plate_fuel_ra226be", "waste_plate_ra226be",
            "plate_fuel_pu238be", "waste_plate_pu238be"
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final int[] slotFlux = new int[SLOT_COUNT];

    private int heat;
    private byte water;
    private int totalFlux;
    private double controlLevel;
    private double targetLevel;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ResearchReactorBlockEntity.this.heat;
                case 1 -> ResearchReactorBlockEntity.this.water;
                case 2 -> ResearchReactorBlockEntity.this.totalFlux;
                case 3 -> (int) Math.round(ResearchReactorBlockEntity.this.controlLevel * LEVEL_SCALE);
                case 4 -> (int) Math.round(ResearchReactorBlockEntity.this.targetLevel * LEVEL_SCALE);
                case 5 -> temperature();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> ResearchReactorBlockEntity.this.heat = value;
                case 1 -> ResearchReactorBlockEntity.this.water = (byte) value;
                case 2 -> ResearchReactorBlockEntity.this.totalFlux = value;
                case 3 -> ResearchReactorBlockEntity.this.controlLevel = value / (double) LEVEL_SCALE;
                case 4 -> ResearchReactorBlockEntity.this.targetLevel = value / (double) LEVEL_SCALE;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ResearchReactorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.RESEARCH_REACTOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ResearchReactorBlockEntity reactor) {
        reactor.tickServer(level);
    }

    public ContainerData getMenuData() {
        return this.menuData;
    }

    public int totalFlux() {
        return this.totalFlux;
    }

    public int heat() {
        return this.heat;
    }

    public double controlLevel(float partialTick) {
        return this.controlLevel;
    }

    public double controlLevel() {
        return this.controlLevel;
    }

    public void setTargetLevel(double level) {
        this.targetLevel = Math.max(0.0D, Math.min(1.0D, level));
        setChangedAndSync(true);
    }

    public static int temperatureForHeat(int heat) {
        return (int) Math.round(heat * 0.00002D * 980.0D + 20.0D);
    }

    public boolean isSubmerged() {
        if (this.level == null) {
            return false;
        }
        return isWater(this.level, this.worldPosition.east().above())
                || isWater(this.level, this.worldPosition.west().above())
                || isWater(this.level, this.worldPosition.north().above())
                || isWater(this.level, this.worldPosition.south().above());
    }

    public void setTargetPercent(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        this.targetLevel = clamped / 100.0D;
        setChangedAndSync(true);
    }

    public BlockPos getBlockPos() {
        return this.worldPosition;
    }

    @Override
    public NeutronFluxProvider.NeutronFlux neutronFluxSpectrum(Level level, BlockPos requesterPos) {
        return NeutronFluxProvider.NeutronFlux.slow(this.totalFlux);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return isValidSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChangedAndSync(false);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        setChangedAndSync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isValidSlot(slot) && stack.getItem() instanceof PlateFuelItem;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isValidSlot(slot) && depletedFuel(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
            this.slotFlux[slot] = 0;
        }
        setChangedAndSync(false);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.research_reactor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ResearchReactorMenu(containerId, playerInventory, this, this.menuData, this.worldPosition);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.items.size(); slot++) {
            drop(level, pos, this.items.get(slot));
            this.items.set(slot, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
            tag.putInt("Flux" + slot, this.slotFlux[slot]);
        }
        tag.putInt("Heat", this.heat);
        tag.putByte("Water", this.water);
        tag.putInt("TotalFlux", this.totalFlux);
        tag.putDouble("Level", this.controlLevel);
        tag.putDouble("TargetLevel", this.targetLevel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
            this.slotFlux[slot] = tag.getInt("Flux" + slot);
        }
        this.heat = tag.getInt("Heat");
        this.water = tag.getByte("Water");
        this.totalFlux = tag.getInt("TotalFlux");
        this.controlLevel = tag.getDouble("Level");
        this.targetLevel = tag.getDouble("TargetLevel");
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

    private void tickServer(Level level) {
        rodControl();
        this.totalFlux = 0;

        if (this.controlLevel > 0.0D) {
            reaction(level);
        } else {
            for (int slot = 0; slot < this.slotFlux.length; slot++) {
                this.slotFlux[slot] = 0;
            }
        }

        if (this.heat > 0) {
            this.water = waterAround(level);
            if (this.water > 0) {
                this.heat -= (int) (this.heat * 0.07F * this.water / 12.0F);
            } else {
                this.heat -= 1;
            }
            if (this.heat < 0) {
                this.heat = 0;
            }
        }

        if (this.heat > MAX_HEAT) {
            explode(level);
            return;
        }

        if (this.controlLevel > 0.0D && this.heat > 0 && !isRadiationBlocked(level)) {
            double radiation = this.heat / (double) MAX_HEAT * 50.0D;
            if (level instanceof ServerLevel serverLevel) {
                ChunkRadiationData.get(serverLevel).incrementRadiation(this.worldPosition, radiation, 25_000.0D);
            }
        }

        setChangedAndSync(level.getGameTime() % 20L == 0L);
    }

    private void rodControl() {
        double speed = 0.04D;
        if (this.controlLevel < this.targetLevel) {
            this.controlLevel = Math.min(this.targetLevel, this.controlLevel + speed);
        } else if (this.controlLevel > this.targetLevel) {
            this.controlLevel = Math.max(this.targetLevel, this.controlLevel - speed);
        }
    }

    private void reaction(Level level) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = this.items.get(slot);
            if (stack.isEmpty()) {
                this.slotFlux[slot] = 0;
                continue;
            }

            if (stack.getItem() instanceof PlateFuelItem fuel) {
                int outFlux = fuel.react(level, stack, this.slotFlux[slot]);
                this.heat += outFlux * 2;
                this.slotFlux[slot] = 0;
                this.totalFlux += outFlux;

                if (fuel.isDepleted(stack)) {
                    this.items.set(slot, depletedResult(stack));
                }

                for (int neighbor : neighboringSlots(slot)) {
                    this.slotFlux[neighbor] += (int) (outFlux * this.controlLevel);
                }
                continue;
            }

            if (stack.is(HbmItems.METEORITE_SWORD_BRED.get())) {
                this.items.set(slot, new ItemStack(HbmItems.METEORITE_SWORD_IRRADIATED.get()));
            }
            this.slotFlux[slot] = 0;
        }
    }

    private ItemStack depletedResult(ItemStack fuel) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(fuel.getItem()).getPath();
        String depletedId = DEPLETED_FUELS.get(id);
        if (depletedId == null) {
            return ItemStack.EMPTY;
        }
        return switch (depletedId) {
            case "waste_plate_u233" -> hotWaste(HbmItems.WASTE_PLATE_U233.get());
            case "waste_plate_u235" -> hotWaste(HbmItems.WASTE_PLATE_U235.get());
            case "waste_plate_mox" -> hotWaste(HbmItems.WASTE_PLATE_MOX.get());
            case "waste_plate_pu239" -> hotWaste(HbmItems.WASTE_PLATE_PU239.get());
            case "waste_plate_sa326" -> hotWaste(HbmItems.WASTE_PLATE_SA326.get());
            case "waste_plate_ra226be" -> hotWaste(HbmItems.WASTE_PLATE_RA226BE.get());
            case "waste_plate_pu238be" -> hotWaste(HbmItems.WASTE_PLATE_PU238BE.get());
            default -> ItemStack.EMPTY;
        };
    }

    private static ItemStack hotWaste(net.minecraft.world.level.ItemLike item) {
        return WasteFuelItem.hot(new ItemStack(item));
    }

    private void explode(Level level) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.EMPTY);
            this.slotFlux[slot] = 0;
        }
        level.setBlock(this.worldPosition, Blocks.AIR.defaultBlockState(), 3);
        clearWater(level);
        level.explode(null, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D, 18.0F, Level.ExplosionInteraction.BLOCK);
        level.setBlock(this.worldPosition, legacyBlock("deco_steel").defaultBlockState(), 3);
        level.setBlock(this.worldPosition.above(), legacyBlock("block_corium").defaultBlockState(), 3);
        level.setBlock(this.worldPosition.above(2), legacyBlock("deco_steel").defaultBlockState(), 3);
        if (level instanceof ServerLevel serverLevel) {
            ChunkRadiationData.get(serverLevel).incrementRadiation(this.worldPosition, 50.0D, 15_000.0D);
            if (HbmConfig.ENABLE_MELTDOWN_ELEMENTALS.get()) {
                // Old: AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1).expand(100, 100, 100).
                AABB area = new AABB(worldPosition.getX() - 100.0D, worldPosition.getY() - 100.0D,
                        worldPosition.getZ() - 100.0D, worldPosition.getX() + 101.0D,
                        worldPosition.getY() + 101.0D, worldPosition.getZ() + 101.0D);
                for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, area)) {
                    LegacyMobSpawnEvents.markRadiationBeastTarget(player);
                }
            }
        }
    }

    private int temperature() {
        return temperatureForHeat(this.heat);
    }

    private boolean isRadiationBlocked(Level level) {
        return blocksRadiation(level, this.worldPosition.east().above())
                && blocksRadiation(level, this.worldPosition.west().above())
                && blocksRadiation(level, this.worldPosition.north().above())
                && blocksRadiation(level, this.worldPosition.south().above());
    }

    private boolean blocksRadiation(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        return isWater(level, pos)
                || block == HbmBlocks.BLOCK_LEAD.get()
                || block == HbmBlocks.BLOCK_DESH.get()
                || block == HbmBlocks.MACHINE_REACTOR_SMALL.get()
                || block == HbmBlocks.MACHINE_REACTOR_BREEDING.get()
                || state.getExplosionResistance(level, pos, null) >= 100.0F;
    }

    private byte waterAround(Level level) {
        byte water = 0;
        if (isWater(level, this.worldPosition.above(3))) {
            water++;
        }
        if (isWater(level, this.worldPosition.below())) {
            water++;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int y = 0; y < 3; y++) {
                if (isWater(level, this.worldPosition.relative(direction).above(y))) {
                    water++;
                }
            }
        }
        return water;
    }

    private void clearWater(Level level) {
        if (isWater(level, this.worldPosition.above(3))) {
            level.setBlock(this.worldPosition.above(3), Blocks.AIR.defaultBlockState(), 3);
        }
        if (isWater(level, this.worldPosition.below())) {
            level.setBlock(this.worldPosition.below(), Blocks.AIR.defaultBlockState(), 3);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int y = 0; y < 3; y++) {
                BlockPos pos = this.worldPosition.relative(direction).above(y);
                if (isWater(level, pos)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static boolean isWater(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        return fluid.is(Fluids.WATER) && fluid.isSource();
    }

    private static boolean depletedFuel(ItemStack stack) {
        return stack.is(HbmItems.WASTE_PLATE_U233.get())
                || stack.is(HbmItems.WASTE_PLATE_U235.get())
                || stack.is(HbmItems.WASTE_PLATE_MOX.get())
                || stack.is(HbmItems.WASTE_PLATE_PU239.get())
                || stack.is(HbmItems.WASTE_PLATE_SA326.get())
                || stack.is(HbmItems.WASTE_PLATE_RA226BE.get())
                || stack.is(HbmItems.WASTE_PLATE_PU238BE.get());
    }

    private static Block legacyBlock(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ReinhardtsHBM.id(id));
        return block == Blocks.AIR ? Blocks.IRON_BLOCK : block;
    }

    private static int[] neighboringSlots(int id) {
        return switch (id) {
            case 0 -> new int[]{1, 5};
            case 1 -> new int[]{0, 6};
            case 2 -> new int[]{3, 7};
            case 3 -> new int[]{2, 4, 8};
            case 4 -> new int[]{3, 9};
            case 5 -> new int[]{0, 6, 10};
            case 6 -> new int[]{1, 5, 11};
            case 7 -> new int[]{2, 8};
            case 8 -> new int[]{3, 7, 9};
            case 9 -> new int[]{4, 8};
            case 10 -> new int[]{5, 11};
            case 11 -> new int[]{6, 10};
            default -> new int[0];
        };
    }

    private void setChangedAndSync(boolean sync) {
        setChanged();
        if (sync && this.level != null) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        level.addFreshEntity(new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                stack.copy()
        ));
    }
}
