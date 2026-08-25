package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.StrandCasterBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.foundry.CrucibleAcceptor;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.item.FoundryMoldItem;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.menu.StrandCasterMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StrandCasterBlockEntity extends BlockEntity implements CrucibleAcceptor, WorldlyContainer, MenuProvider, MachineInventory {
    public static final int MOLD_SLOT = 0;
    public static final int OUTPUT_START = 1;
    public static final int OUTPUT_END = 7;
    public static final int SLOT_COUNT = 7;
    public static final int DATA_COUNT = 9;
    public static final int FLUID_CAPACITY = 64_000;
    public static final List<BlockPos> UNROTATED_BOTTOM_FLUID_PORTS = List.of(
            new BlockPos(-1, 0, -1),
            new BlockPos(0, 0, -1),
            new BlockPos(-1, 0, -5),
            new BlockPos(0, 0, -5)
    );
    public static final List<BlockPos> UNROTATED_TOP_POUR_PORTS = List.of(
            new BlockPos(-1, 2, -1),
            new BlockPos(0, 2, -1),
            new BlockPos(-1, 2, 0),
            new BlockPos(0, 2, 0)
    );

    private static final int[] OUTPUT_SLOTS = {1, 2, 3, 4, 5, 6};
    private static final HbmFluidDefinition WATER = HbmFluids.byName("water").orElse(HbmFluids.none());
    private static final HbmFluidDefinition SPENT_STEAM = HbmFluids.byName("spentsteam").orElse(HbmFluids.none());

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final HbmFluidTank water = new HbmFluidTank(WATER, FLUID_CAPACITY);
    private final HbmFluidTank steam = new HbmFluidTank(SPENT_STEAM, FLUID_CAPACITY);
    @Nullable
    private FoundryMaterial type;
    private int amount;
    private long lastProgressTick;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> StrandCasterBlockEntity.this.type == null ? -1 : StrandCasterBlockEntity.this.type.id();
                case 1 -> StrandCasterBlockEntity.this.amount;
                case 2 -> StrandCasterBlockEntity.this.getCapacity();
                case 3 -> StrandCasterBlockEntity.this.water.type().oldId();
                case 4 -> StrandCasterBlockEntity.this.water.amount();
                case 5 -> StrandCasterBlockEntity.this.water.capacity();
                case 6 -> StrandCasterBlockEntity.this.steam.type().oldId();
                case 7 -> StrandCasterBlockEntity.this.steam.amount();
                case 8 -> StrandCasterBlockEntity.this.steam.capacity();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> StrandCasterBlockEntity.this.type = FoundryMaterial.byId(value).orElse(null);
                case 1 -> StrandCasterBlockEntity.this.amount = value;
                case 3 -> StrandCasterBlockEntity.this.water.setType(HbmFluids.byOldId(value).orElse(WATER));
                case 4 -> StrandCasterBlockEntity.this.water.setAmount(value);
                case 6 -> StrandCasterBlockEntity.this.steam.setType(HbmFluids.byOldId(value).orElse(SPENT_STEAM));
                case 7 -> StrandCasterBlockEntity.this.steam.setAmount(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public StrandCasterBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.STRAND_CASTER.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, StrandCasterBlockEntity caster) {
        if (level.isClientSide) {
            return;
        }
        caster.tickServer(level);
    }

    @Nullable
    public FoundryMaterial material() {
        return this.type;
    }

    public int amount() {
        return this.amount;
    }

    public HbmFluidTank waterTank() {
        return this.water;
    }

    public HbmFluidTank steamTank() {
        return this.steam;
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    @Nullable
    public FoundryMoldItem.Mold getInstalledMold() {
        ItemStack stack = this.items.get(MOLD_SLOT);
        return stack.getItem() instanceof FoundryMoldItem ? FoundryMoldItem.mold(stack) : null;
    }

    public int getCapacity() {
        FoundryMoldItem.Mold mold = getInstalledMold();
        return mold == null ? 50_000 : mold.cost() * 10;
    }

    public int getWaterRequired() {
        FoundryMoldItem.Mold mold = getInstalledMold();
        return mold == null ? 50 : 5 * mold.cost();
    }

    public boolean installMold(ItemStack stack, Player player) {
        if (!(stack.getItem() instanceof FoundryMoldItem) || !this.items.get(MOLD_SLOT).isEmpty()) {
            return false;
        }
        this.items.set(MOLD_SLOT, stack.copyWithCount(1));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        sync();
        return true;
    }

    public boolean removeMold(Player player) {
        ItemStack mold = this.items.get(MOLD_SLOT);
        if (mold.isEmpty()) {
            return false;
        }
        giveOrDrop(player, mold.copy());
        this.items.set(MOLD_SLOT, ItemStack.EMPTY);
        sync();
        return true;
    }

    public boolean scrape(Player player) {
        if (this.type == null || this.amount <= 0) {
            return false;
        }
        giveOrDrop(player, ScrapsItem.create(new FoundryMaterialStack(this.type, this.amount), false));
        this.type = null;
        this.amount = 0;
        sync();
        return true;
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        BlockPos unrotated = unrotatedOffset(queriedPos);
        if (unrotated == null || !UNROTATED_BOTTOM_FLUID_PORTS.contains(unrotated)) {
            return null;
        }
        if (side != null && side != portSide(unrotated)) {
            return null;
        }
        return new FluidPortHandler();
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, double x, double y, double z, Direction side, FoundryMaterialStack stack) {
        if (side != Direction.UP || stack == null) {
            return false;
        }
        BlockPos unrotated = unrotatedOffset(pos);
        return unrotated != null && UNROTATED_TOP_POUR_PORTS.contains(unrotated) && standardCheck(stack);
    }

    @Override
    public FoundryMaterialStack pour(Level level, BlockPos pos, double x, double y, double z, Direction side, FoundryMaterialStack stack) {
        if (!canAcceptPartialPour(level, pos, x, y, z, side, stack)) {
            return stack;
        }
        int limit = castingLimit();
        int room = Math.max(0, limit - this.amount);
        int accepted = Math.min(room, stack.amount());
        if (accepted <= 0) {
            return stack;
        }
        this.type = stack.material();
        this.amount += accepted;
        int leftover = stack.amount() - accepted;
        if (leftover > 0) {
            this.lastProgressTick = level.getGameTime();
        }
        sync();
        return leftover <= 0 ? null : new FoundryMaterialStack(stack.material(), leftover);
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
    public ItemStack removeItem(int slot, int count) {
        if (!validSlot(slot) || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        ItemStack removed = stack.split(count);
        if (stack.isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        sync();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!validSlot(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return stack;
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
        sync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == MOLD_SLOT && stack.getItem() instanceof FoundryMoldItem;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        sync();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return OUTPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= OUTPUT_START && slot < OUTPUT_END;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_strand_caster");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new StrandCasterMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        if (this.type != null && this.amount > 0) {
            level.addFreshEntity(new ItemEntity(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 2.0D,
                    pos.getZ() + 0.5D,
                    ScrapsItem.create(new FoundryMaterialStack(this.type, this.amount), false)
            ));
        }
        this.items.replaceAll(ignored -> ItemStack.EMPTY);
        this.type = null;
        this.amount = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        if (this.type != null) {
            tag.putString("Type", this.type.name());
        }
        tag.putInt("Amount", this.amount);
        tag.putLong("LastProgressTick", this.lastProgressTick);
        tag.put("Water", this.water.save());
        tag.put("Steam", this.steam.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.type = FoundryMaterial.byName(tag.getString("Type")).orElse(null);
        this.amount = tag.getInt("Amount");
        this.lastProgressTick = tag.getLong("LastProgressTick");
        this.water.load(tag.getCompound("Water"));
        this.steam.load(tag.getCompound("Steam"));
        if (this.water.amount() == 0) {
            this.water.setType(WATER);
        }
        if (this.steam.amount() == 0) {
            this.steam.setType(SPENT_STEAM);
        }
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
        boolean changed = false;
        int capacity = getCapacity();
        if (this.amount > capacity) {
            int excess = this.amount - capacity;
            if (this.type != null && excess > 0) {
                level.addFreshEntity(new ItemEntity(
                        level,
                        this.worldPosition.getX() + 0.5D,
                        this.worldPosition.getY() + 2.0D,
                        this.worldPosition.getZ() + 0.5D,
                        ScrapsItem.create(new FoundryMaterialStack(this.type, excess), false)
                ));
            }
            this.amount = capacity;
            changed = true;
        }
        if (this.amount <= 0 && this.type != null) {
            this.amount = 0;
            this.type = null;
            changed = true;
        }
        this.water.setType(WATER);
        this.steam.setType(SPENT_STEAM);

        int casts = maxProcessable();
        if (casts > 0 && (casts >= 9 || level.getGameTime() >= this.lastProgressTick + 200L)) {
            processCasts(casts);
            this.lastProgressTick = level.getGameTime();
            changed = true;
        }
        if (changed) {
            sync();
        } else {
            setChanged();
        }
    }

    private int maxProcessable() {
        FoundryMoldItem.Mold mold = getInstalledMold();
        if (this.type == null || this.amount <= 0 || mold == null) {
            return 0;
        }
        ItemStack output = mold.outputFor(this.type).orElse(ItemStack.EMPTY);
        if (output.isEmpty()) {
            return 0;
        }
        int freeItems = 0;
        int stackLimit = output.getMaxStackSize();
        for (int slot = OUTPUT_START; slot < OUTPUT_END; slot++) {
            ItemStack held = this.items.get(slot);
            if (held.isEmpty()) {
                freeItems += stackLimit;
            } else if (ItemStack.isSameItemSameComponents(held, output)) {
                freeItems += Math.max(0, stackLimit - held.getCount());
            }
        }
        int waterRequired = getWaterRequired();
        int casts = this.amount / mold.cost();
        casts = Math.min(casts, freeItems / output.getCount());
        casts = Math.min(casts, this.water.amount() / waterRequired);
        casts = Math.min(casts, (this.steam.capacity() - this.steam.amount()) / waterRequired);
        return Math.max(0, casts);
    }

    private void processCasts(int casts) {
        FoundryMoldItem.Mold mold = getInstalledMold();
        if (mold == null || this.type == null) {
            return;
        }
        ItemStack output = mold.outputFor(this.type).orElse(ItemStack.EMPTY);
        if (output.isEmpty()) {
            return;
        }

        this.amount -= casts * mold.cost();
        int remaining = output.getCount() * casts;
        int stackLimit = output.getMaxStackSize();
        for (int slot = OUTPUT_START; slot < OUTPUT_END && remaining > 0; slot++) {
            ItemStack held = this.items.get(slot);
            if (held.isEmpty()) {
                int deposited = Math.min(remaining, stackLimit);
                this.items.set(slot, output.copyWithCount(deposited));
                remaining -= deposited;
            } else if (ItemStack.isSameItemSameComponents(held, output)) {
                int deposited = Math.min(remaining, stackLimit - held.getCount());
                held.grow(deposited);
                remaining -= deposited;
            }
        }

        int waterUsed = getWaterRequired() * casts;
        this.water.drain(WATER, waterUsed, false);
        this.steam.fill(SPENT_STEAM, waterUsed, false);
        if (this.amount <= 0) {
            this.amount = 0;
            this.type = null;
        }
    }

    private boolean standardCheck(FoundryMaterialStack stack) {
        if (stack == null || stack.amount() <= 0) {
            return false;
        }
        if (this.type != null && this.type != stack.material()) {
            return false;
        }
        FoundryMoldItem.Mold mold = getInstalledMold();
        return mold != null && this.amount < castingLimit();
    }

    private int castingLimit() {
        FoundryMoldItem.Mold mold = getInstalledMold();
        return mold == null ? getCapacity() : mold.cost() * 9;
    }

    @Nullable
    private BlockPos unrotatedOffset(BlockPos queriedPos) {
        Direction facing = getFacing();
        BlockPos relative = queriedPos.subtract(this.worldPosition);
        for (BlockPos offset : UNROTATED_BOTTOM_FLUID_PORTS) {
            if (com.reinhardt.hbm.util.LegacyMachineGeometry.rotateLegacySouth(offset, facing).equals(relative)) {
                return offset;
            }
        }
        for (BlockPos offset : UNROTATED_TOP_POUR_PORTS) {
            if (com.reinhardt.hbm.util.LegacyMachineGeometry.rotateLegacySouth(offset, facing).equals(relative)) {
                return offset;
            }
        }
        return null;
    }

    private Direction portSide(BlockPos unrotatedOffset) {
        Direction local = switch (unrotatedOffset.getX()) {
            case -1 -> Direction.WEST;
            case 0 -> Direction.EAST;
            default -> throw new IllegalArgumentException("Unexpected strand caster fluid port " + unrotatedOffset);
        };
        return com.reinhardt.hbm.util.LegacyMachineGeometry.rotateDirection(local, getFacing(), LargeMachineBlock.RotationBasis.HBM_LEGACY_SOUTH);
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;
    }

    private void sync() {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            if (!this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private final class FluidPortHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> StrandCasterBlockEntity.this.water.getFluidInTank(0);
                case 1 -> StrandCasterBlockEntity.this.steam.getFluidInTank(0);
                default -> FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> StrandCasterBlockEntity.this.water.capacity();
                case 1 -> StrandCasterBlockEntity.this.steam.capacity();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            return HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none()) == WATER;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != WATER) {
                return 0;
            }
            int filled = StrandCasterBlockEntity.this.water.fill(WATER, resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                sync();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid != SPENT_STEAM) {
                return FluidStack.EMPTY;
            }
            HbmFluidStack drained = StrandCasterBlockEntity.this.steam.drain(SPENT_STEAM, resource.getAmount(), action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(SPENT_STEAM, drained.amount());
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            HbmFluidStack drained = StrandCasterBlockEntity.this.steam.drain(SPENT_STEAM, maxDrain, action.simulate());
            if (!drained.isEmpty() && action.execute()) {
                sync();
            }
            return drained.isEmpty() ? FluidStack.EMPTY : HbmFluids.toNeoStack(SPENT_STEAM, drained.amount());
        }
    }
}
