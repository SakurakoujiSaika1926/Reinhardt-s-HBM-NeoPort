package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.ConveyorBlock;
import com.reinhardt.hbm.block.CraneMachineBlock;
import com.reinhardt.hbm.drone.DroneItemMatcher;
import com.reinhardt.hbm.entity.ConveyorMovingItem;
import com.reinhardt.hbm.entity.ConveyorMovingPackage;
import com.reinhardt.hbm.item.LegacyMetaUpgradeItem;
import com.reinhardt.hbm.menu.CraneMachineMenu;
import com.reinhardt.hbm.recipe.CrystallizerRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.ExternalCapabilitySafety;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Stateful implementation of the complete 1.7.10 conveyor-crane family.
 *
 * <p>The eight registered blocks deliberately share persistence code, but the
 * individual machines keep their original slot maps and transfer rules.  In
 * particular, extractor input/output is reversed, unboxers accept packages
 * from the old output side, and splitters retain their ratio cursor between
 * stacks.</p>
 */
public final class CraneMachineBlockEntity extends DroneInventoryBlockEntity implements MenuProvider {
    public static final int STORAGE_SIZE = 90;
    public static final int PARTITIONER_QUEUE_SIZE = 45;

    public static final int BOXER_MODE_4 = 0;
    public static final int BOXER_MODE_8 = 1;
    public static final int BOXER_MODE_16 = 2;
    public static final int BOXER_MODE_REDSTONE = 3;

    public static final int ROUTER_MODE_NONE = 0;
    public static final int ROUTER_MODE_WHITELIST = 1;
    public static final int ROUTER_MODE_BLACKLIST = 2;
    public static final int ROUTER_MODE_WILDCARD = 3;

    private static final int[] NO_SLOTS = new int[0];
    private static final int[] SLOTS_0_20 = range(0, 21);
    private static final int[] SLOTS_0_22 = range(0, 23);
    private static final int[] SLOTS_9_17 = range(9, 18);
    private static final int[] SLOTS_45_89 = range(45, 90);
    private static final int[] SLOTS_0_89 = range(0, 90);

    private final String[] filterModes = new String[30];
    private final int[] routerModes = new int[6];

    private Direction inputSide;
    private Direction outputSide;
    private boolean whitelist;
    private boolean maxEject;
    private boolean destroyer = true;
    private int boxerMode;
    private boolean lastRedstone;
    private long lastGrabbedTick;
    private boolean splitterRightPreferred;
    private int splitterRemaining;
    private int leftRatio = 1;
    private int rightRatio = 1;
    private boolean splitterSecondary;
    private BlockPos splitterCorePos;
    private boolean partitionerDirty = true;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> inputSide.ordinal();
                case 1 -> outputSide.ordinal();
                case 2 -> switch (kind()) {
                    case BOXER -> boxerMode;
                    case EXTRACTOR, GRABBER -> whitelist ? 1 : 0;
                    case INSERTER -> destroyer ? 1 : 0;
                    default -> 0;
                };
                case 3 -> maxEject ? 1 : 0;
                case 4 -> leftRatio;
                case 5 -> rightRatio;
                case 6, 7, 8, 9, 10, 11 -> routerModes[index - 6];
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> inputSide = Direction.from3DDataValue(value);
                case 1 -> outputSide = Direction.from3DDataValue(value);
                case 2 -> {
                    switch (kind()) {
                        case BOXER -> boxerMode = Math.clamp(value, BOXER_MODE_4, BOXER_MODE_REDSTONE);
                        case EXTRACTOR, GRABBER -> whitelist = value != 0;
                        case INSERTER -> destroyer = value != 0;
                        default -> { }
                    }
                }
                case 3 -> maxEject = value != 0;
                case 4 -> leftRatio = Math.clamp(value, 1, 16);
                case 5 -> rightRatio = Math.clamp(value, 1, 16);
                case 6, 7, 8, 9, 10, 11 -> routerModes[index - 6] = Math.clamp(value, ROUTER_MODE_NONE, ROUTER_MODE_WILDCARD);
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return 12;
        }
    };

    public CraneMachineBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CRANE_MACHINE.get(), pos, state, STORAGE_SIZE);
        Direction initial = state.hasProperty(CraneMachineBlock.FACING)
                ? state.getValue(CraneMachineBlock.FACING)
                : Direction.NORTH;
        this.inputSide = initial;
        this.outputSide = initial.getOpposite();
        this.splitterCorePos = pos;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CraneMachineBlockEntity crane) {
        if (level.isClientSide || crane.splitterSecondary) {
            return;
        }
        switch (crane.kind()) {
            case BOXER -> crane.tickBoxer(level);
            case EXTRACTOR -> crane.tickExtractor(level);
            case GRABBER -> crane.tickGrabber(level);
            case INSERTER -> crane.tickInserter(level);
            case PARTITIONER -> crane.tickPartitioner(level);
            case ROUTER, SPLITTER -> { }
            case UNBOXER -> crane.tickUnboxer(level);
        }
    }

    public CraneMachineBlock.Kind kind() {
        return getBlockState().getBlock() instanceof CraneMachineBlock crane
                ? crane.kind()
                : CraneMachineBlock.Kind.BOXER;
    }

    public Direction inputSide() {
        CraneMachineBlockEntity core = splitterCore();
        return core == this ? inputSide : core.inputSide;
    }

    public Direction outputSide() {
        CraneMachineBlockEntity core = splitterCore();
        return core == this ? outputSide : core.outputSide;
    }

    public ContainerData menuData() {
        return menuData;
    }

    public boolean splitterSecondary() {
        return splitterSecondary;
    }

    public BlockPos splitterCounterpart() {
        if (kind() != CraneMachineBlock.Kind.SPLITTER) {
            return worldPosition;
        }
        if (splitterSecondary) {
            return splitterCorePos;
        }
        return worldPosition.relative(outputSide.getClockWise());
    }

    public void configureSplitterPart(boolean secondary, BlockPos corePos, Direction input) {
        this.splitterSecondary = secondary;
        this.splitterCorePos = corePos.immutable();
        this.inputSide = input;
        this.outputSide = input.getOpposite();
        sync();
    }

    public void setInputSide(Direction direction) {
        if (kind() == CraneMachineBlock.Kind.SPLITTER || kind() == CraneMachineBlock.Kind.PARTITIONER) {
            return;
        }
        Direction oldInput = inputSide;
        Direction oldOutput = outputSide;
        if (direction == oldInput) {
            direction = direction.getOpposite();
        }
        inputSide = direction;
        if (direction == oldOutput) {
            outputSide = oldInput;
        }
        updateHorizontalFacing();
        sync();
    }

    public void setOutputSide(Direction direction) {
        if (kind() == CraneMachineBlock.Kind.SPLITTER || kind() == CraneMachineBlock.Kind.PARTITIONER) {
            return;
        }
        Direction oldOutput = outputSide;
        if (direction == oldOutput) {
            direction = direction.getOpposite();
        }
        if (direction == inputSide) {
            inputSide = oldOutput;
            updateHorizontalFacing();
        }
        outputSide = direction;
        sync();
    }

    public int adjustSplitterRatio(boolean rightHalf, int delta) {
        CraneMachineBlockEntity core = splitterCore();
        if (rightHalf) {
            core.rightRatio = Math.clamp(core.rightRatio + delta, 1, 16);
        } else {
            core.leftRatio = Math.clamp(core.leftRatio + delta, 1, 16);
        }
        core.sync();
        return rightHalf ? core.rightRatio : core.leftRatio;
    }

    public int leftRatio() {
        return splitterCore().leftRatio;
    }

    public int rightRatio() {
        return splitterCore().rightRatio;
    }

    public boolean isConveyorSurface() {
        CraneMachineBlock.Kind current = kind();
        return current == CraneMachineBlock.Kind.PARTITIONER || current == CraneMachineBlock.Kind.SPLITTER;
    }

    public Vec3 travelLocation(Vec3 itemPosition, double speed) {
        Direction travel = outputSide();
        double x = Mth.clamp(itemPosition.x, worldPosition.getX(), worldPosition.getX() + 1.0D);
        double z = Mth.clamp(itemPosition.z, worldPosition.getZ(), worldPosition.getZ() + 1.0D);
        double snappedX = travel.getStepX() != 0 ? x : worldPosition.getX() + 0.5D;
        double snappedZ = travel.getStepZ() != 0 ? z : worldPosition.getZ() + 0.5D;
        Vec3 snap = new Vec3(snappedX, worldPosition.getY() + 0.25D, snappedZ);
        Vec3 destination = snap.add(travel.getStepX() * speed, travel.getStepY() * speed, travel.getStepZ() * speed);
        Vec3 delta = destination.subtract(itemPosition);
        return delta.lengthSqr() <= 1.0E-12D ? destination : itemPosition.add(delta.scale(speed / delta.length()));
    }

    /** Called exactly once when a moving item crosses into this block. */
    public boolean acceptMovingItem(ItemStack incoming, Direction enteredFrom) {
        if (level == null || level.isClientSide || incoming.isEmpty()) {
            return false;
        }
        return switch (kind()) {
            case BOXER -> enteredFrom == inputSide() && acceptIntoStorage(incoming, 0, 21, false);
            case INSERTER -> enteredFrom == inputSide() && acceptInserterStack(incoming);
            case PARTITIONER -> enteredFrom == inputSide() && acceptPartitionerStack(incoming);
            case ROUTER -> routeItem(incoming);
            case SPLITTER -> enteredFrom == inputSide() && splitIncoming(incoming);
            case EXTRACTOR, GRABBER, UNBOXER -> false;
        };
    }

    /** Called exactly once when a moving package crosses into this block. */
    public boolean acceptMovingPackage(List<ItemStack> incoming, Direction enteredFrom) {
        if (level == null || level.isClientSide || incoming.isEmpty()) {
            return false;
        }
        return switch (kind()) {
            case BOXER -> acceptPackageIntoStorage(incoming, 0, 21);
            case INSERTER -> acceptInserterPackage(incoming);
            case ROUTER -> routePackage(incoming);
            case UNBOXER -> enteredFrom == outputSide() && acceptPackageIntoStorage(incoming, 0, 21);
            case EXTRACTOR, GRABBER, PARTITIONER, SPLITTER -> false;
        };
    }

    public void handleMenuButton(int id) {
        switch (kind()) {
            case BOXER -> {
                if (id == 0) boxerMode = (boxerMode + 1) & 3;
            }
            case EXTRACTOR -> {
                if (id == 0) whitelist = !whitelist;
                if (id == 1) maxEject = !maxEject;
            }
            case GRABBER -> {
                if (id == 0) whitelist = !whitelist;
            }
            case INSERTER -> {
                if (id == 0) destroyer = !destroyer;
            }
            case ROUTER -> {
                if (id >= 0 && id < 6) routerModes[id] = (routerModes[id] + 1) & 3;
            }
            case PARTITIONER, SPLITTER, UNBOXER -> { }
        }
        sync();
    }

    public void setFilter(int slot, ItemStack pattern) {
        if (!isFilterSlot(slot)) {
            return;
        }
        ItemStack copy = pattern.isEmpty() ? ItemStack.EMPTY : pattern.copyWithCount(1);
        items.set(slot, copy);
        filterModes[slot] = copy.isEmpty() ? null : DroneItemMatcher.defaultMode(copy);
        sync();
    }

    public void cycleFilterMode(int slot) {
        if (!isFilterSlot(slot) || items.get(slot).isEmpty()) {
            return;
        }
        ItemStack pattern = items.get(slot);
        String mode = filterModes[slot];
        if (mode == null || DroneItemMatcher.EXACT.equals(mode)) {
            filterModes[slot] = DroneItemMatcher.WILDCARD;
        } else if (DroneItemMatcher.WILDCARD.equals(mode)) {
            List<ResourceLocation> tags = pattern.getTags().map(TagKey::location).toList();
            filterModes[slot] = tags.isEmpty() ? DroneItemMatcher.EXACT : tags.getFirst().toString();
        } else {
            List<ResourceLocation> tags = pattern.getTags().map(TagKey::location).toList();
            int index = tags.indexOf(ResourceLocation.tryParse(mode));
            filterModes[slot] = index < 0 || index + 1 >= tags.size()
                    ? DroneItemMatcher.EXACT
                    : tags.get(index + 1).toString();
        }
        sync();
    }

    @Nullable
    public String filterMode(int slot) {
        return slot >= 0 && slot < filterModes.length ? filterModes[slot] : null;
    }

    public int comparatorSignal() {
        int[] slots = switch (kind()) {
            case BOXER, INSERTER -> SLOTS_0_20;
            case EXTRACTOR -> SLOTS_9_17;
            case UNBOXER -> SLOTS_0_20;
            case PARTITIONER -> SLOTS_0_89;
            case GRABBER, ROUTER, SPLITTER -> NO_SLOTS;
        };
        if (slots.length == 0) {
            return 0;
        }
        float fullness = 0.0F;
        int occupied = 0;
        for (int slot : slots) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            fullness += (float) stack.getCount() / Math.min(getMaxStackSize(stack), stack.getMaxStackSize());
            occupied++;
        }
        fullness /= slots.length;
        return Mth.floor(fullness * 14.0F) + (occupied > 0 ? 1 : 0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CraneMachineMenu(id, inventory, this);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (kind()) {
            case BOXER, INSERTER -> slot >= 0 && slot < 21;
            case EXTRACTOR -> slot >= 9 && slot < 18
                    || slot == 18 && stack.is(HbmItems.UPGRADE_STACK.get())
                    || slot == 19 && stack.is(HbmItems.UPGRADE_EJECTOR.get());
            case GRABBER -> slot == 9 && stack.is(HbmItems.UPGRADE_STACK.get())
                    || slot == 10 && stack.is(HbmItems.UPGRADE_EJECTOR.get());
            case UNBOXER -> slot >= 0 && slot < 21
                    || slot == 21 && stack.is(HbmItems.UPGRADE_STACK.get())
                    || slot == 22 && stack.is(HbmItems.UPGRADE_EJECTOR.get());
            case PARTITIONER -> slot >= 0 && slot < PARTITIONER_QUEUE_SIZE && crystallizerInputAmount(stack) > 0;
            case ROUTER, SPLITTER -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (kind()) {
            case BOXER, INSERTER, UNBOXER -> SLOTS_0_20;
            case EXTRACTOR -> SLOTS_9_17;
            case PARTITIONER -> SLOTS_0_89;
            case GRABBER, ROUTER, SPLITTER -> NO_SLOTS;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return switch (kind()) {
            case BOXER, INSERTER, UNBOXER -> slot >= 0 && slot < 21;
            case EXTRACTOR -> slot >= 9 && slot < 18;
            case PARTITIONER -> slot >= 0 && slot < PARTITIONER_QUEUE_SIZE && crystallizerInputAmount(stack) > 0;
            case GRABBER, ROUTER, SPLITTER -> false;
        };
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return switch (kind()) {
            case BOXER, INSERTER, UNBOXER -> slot >= 0 && slot < 21;
            case EXTRACTOR -> slot >= 9 && slot < 18;
            case PARTITIONER -> slot >= PARTITIONER_QUEUE_SIZE && slot < PARTITIONER_QUEUE_SIZE * 2;
            case GRABBER, ROUTER, SPLITTER -> false;
        };
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (kind() == CraneMachineBlock.Kind.PARTITIONER) {
            partitionerDirty = true;
        }
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        int[] drops = switch (kind()) {
            case BOXER, INSERTER -> SLOTS_0_20;
            case EXTRACTOR -> range(9, 20);
            case GRABBER -> range(9, 11);
            case UNBOXER -> SLOTS_0_22;
            case PARTITIONER -> SLOTS_0_89;
            case ROUTER, SPLITTER -> NO_SLOTS;
        };
        for (int slot : drops) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack.copy());
            }
        }
        for (int slot = 0; slot < items.size(); slot++) {
            items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    private void tickExtractor(Level level) {
        if (level.hasNeighborSignal(worldPosition) || level.getGameTime() % ejectDelay(19) != 0L) {
            return;
        }
        int amount = stackAmount(18);
        Direction sourceSide = outputSide;
        Direction beltSide = inputSide;
        boolean hasBelt = isBeltTarget(level, worldPosition.relative(beltSide));
        boolean sent = false;
        IItemHandler source = adjacentHandler(sourceSide, "extractor source lookup");
        if (source != null) {
            try {
                for (int slot = 0; slot < source.getSlots(); slot++) {
                    ItemStack visible = source.getStackInSlot(slot);
                    if (visible.isEmpty() || !passesFilter(visible, 0, 9, whitelist)) {
                        continue;
                    }
                    int target = Math.min(amount, visible.getMaxStackSize());
                    if (maxEject && visible.getCount() < target) {
                        continue;
                    }
                    ItemStack simulated = source.extractItem(slot, target, true);
                    if (simulated.isEmpty() || maxEject && simulated.getCount() < target) {
                        continue;
                    }
                    int transferable = simulated.getCount();
                    if (!hasBelt) {
                        transferable -= insertIntoRange(simulated, 9, 18, true).getCount();
                    }
                    if (transferable <= 0) {
                        continue;
                    }
                    ItemStack extracted = source.extractItem(slot, transferable, false);
                    if (extracted.isEmpty()) {
                        continue;
                    }
                    if (hasBelt) {
                        spawnMovingItemOnBelt(beltSide, extracted, true);
                    } else {
                        ItemStack remainder = insertIntoRange(extracted, 9, 18, false);
                        if (!remainder.isEmpty()) {
                            dropStack(remainder, Vec3.atCenterOf(worldPosition));
                        }
                    }
                    sent = true;
                    break;
                }
            } catch (RuntimeException | LinkageError failure) {
                ExternalCapabilitySafety.report(level, worldPosition.relative(sourceSide), sourceSide.getOpposite(),
                        "item handler", "conveyor extractor", failure);
            }
        }

        if (!sent && hasBelt) {
            for (int slot = 9; slot < 18; slot++) {
                ItemStack buffered = items.get(slot);
                if (buffered.isEmpty()) {
                    continue;
                }
                int target = Math.min(amount, buffered.getMaxStackSize());
                if (maxEject && buffered.getCount() < target) {
                    continue;
                }
                ItemStack emitted = buffered.split(Math.min(target, buffered.getCount()));
                if (buffered.isEmpty()) {
                    items.set(slot, ItemStack.EMPTY);
                }
                spawnMovingItemOnBelt(beltSide, emitted, true);
                sync();
                break;
            }
        }
    }

    private void tickInserter(Level level) {
        if (level.hasNeighborSignal(worldPosition)) {
            return;
        }
        for (int slot = 0; slot < 21; slot++) {
            ItemStack buffered = items.get(slot);
            if (buffered.isEmpty()) {
                continue;
            }
            ItemStack remainder = insertAdjacent(outputSide, buffered);
            if (remainder.getCount() == buffered.getCount()) {
                ItemStack one = buffered.copyWithCount(1);
                ItemStack oneRemainder = insertAdjacent(outputSide, one);
                if (oneRemainder.isEmpty()) {
                    buffered.shrink(1);
                    if (buffered.isEmpty()) items.set(slot, ItemStack.EMPTY);
                    sync();
                    break;
                }
                continue;
            }
            items.set(slot, remainder);
            sync();
            break;
        }
    }

    private void tickGrabber(Level level) {
        int delay = ejectDelay(10);
        long now = level.getGameTime();
        if (level.hasNeighborSignal(worldPosition) || now < lastGrabbedTick + delay) {
            return;
        }
        Direction input = inputSide;
        Direction output = outputSide;
        double reach = grabberReach(level, input);
        double x = worldPosition.getX() + input.getStepX() * reach;
        double y = worldPosition.getY() + input.getStepY() * reach;
        double z = worldPosition.getZ() + input.getStepZ() * reach;
        AABB bounds = new AABB(x + 0.1875D, y + 0.1875D, z + 0.1875D,
                x + 0.8125D, y + 0.8125D, z + 0.8125D);
        List<ConveyorMovingItem> movingItems = level.getEntitiesOfClass(ConveyorMovingItem.class, bounds,
                Entity::isAlive);
        if (movingItems.isEmpty()) {
            return;
        }
        if (isBeltTarget(level, worldPosition.relative(output))) {
            for (ConveyorMovingItem moving : movingItems) {
                ItemStack stack = moving.getItemStack();
                if (!passesFilter(stack, 0, 9, whitelist)) {
                    continue;
                }
                moving.discard();
                spawnMovingItemOnBelt(output, stack.copy(), false);
                lastGrabbedTick = now;
                sync();
                return;
            }
            return;
        }

        int remainingAmount = stackAmount(9);
        for (ConveyorMovingItem moving : movingItems) {
            ItemStack stack = moving.getItemStack();
            if (!passesFilter(stack, 0, 9, whitelist)) {
                continue;
            }
            int offered = Math.min(remainingAmount, stack.getCount());
            ItemStack remainder = insertAdjacent(output, stack.copyWithCount(offered));
            int moved = offered - remainder.getCount();
            if (moved <= 0) {
                continue;
            }
            stack.shrink(moved);
            if (stack.isEmpty()) moving.discard();
            else moving.setItemStack(stack);
            remainingAmount -= moved;
            lastGrabbedTick = now;
            if (remainingAmount <= 0) break;
        }
        if (remainingAmount < stackAmount(9)) {
            sync();
        }
    }

    private void tickBoxer(Level level) {
        boolean powered = level.hasNeighborSignal(worldPosition);
        if (boxerMode == BOXER_MODE_REDSTONE && powered && !lastRedstone) {
            emitPackage(allOccupiedSlots(0, 21));
        }
        lastRedstone = powered;
        if (boxerMode == BOXER_MODE_REDSTONE || (level.getGameTime() & 1L) != 0L) {
            return;
        }
        int required = switch (boxerMode) {
            case BOXER_MODE_8 -> 8;
            case BOXER_MODE_16 -> 16;
            default -> 4;
        };
        List<Integer> full = new ArrayList<>();
        for (int slot = 0; slot < 21; slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty() && stack.getCount() == stack.getMaxStackSize()) {
                full.add(slot);
            }
        }
        if (full.size() >= required) {
            emitPackage(full.subList(0, required));
        }
    }

    private void tickUnboxer(Level level) {
        if (level.hasNeighborSignal(worldPosition) || level.getGameTime() % ejectDelay(22) != 0L
                || !isBeltTarget(level, worldPosition.relative(inputSide))) {
            return;
        }
        int amount = stackAmount(21);
        for (int slot = 0; slot < 21; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;
            ItemStack emitted = stack.split(Math.min(amount, stack.getCount()));
            if (stack.isEmpty()) items.set(slot, ItemStack.EMPTY);
            spawnMovingItemOnBelt(inputSide, emitted, false);
            sync();
            break;
        }
    }

    private void tickPartitioner(Level level) {
        if (!partitionerDirty) {
            return;
        }
        partitionerDirty = false;
        List<Integer> occupied = new ArrayList<>();
        for (int slot = 0; slot < PARTITIONER_QUEUE_SIZE; slot++) {
            if (!items.get(slot).isEmpty()) occupied.add(slot);
        }
        occupied.sort(Comparator.comparingInt(slot -> items.get(slot).getCount()));
        boolean changed = false;
        for (int slot : occupied) {
            ItemStack stack = items.get(slot);
            int amount = crystallizerInputAmount(stack);
            if (amount <= 0) amount = stack.getCount();
            while (!stack.isEmpty() && stack.getCount() >= amount) {
                ItemStack emitted = stack.copyWithCount(amount);
                stack.shrink(amount);
                spawnMovingItemOnOwnSurface(emitted);
                changed = true;
            }
            if (stack.isEmpty()) items.set(slot, ItemStack.EMPTY);
        }
        if (changed) sync();
    }

    private boolean acceptInserterStack(ItemStack incoming) {
        ItemStack remainder = incoming.copy();
        if (level != null && !level.hasNeighborSignal(worldPosition)) {
            remainder = insertAdjacent(outputSide, remainder);
        }
        remainder = insertIntoRange(remainder, 0, 21, false);
        if (!remainder.isEmpty() && !destroyer) {
            dropStack(remainder, Vec3.atCenterOf(worldPosition));
        }
        return true;
    }

    private boolean acceptInserterPackage(List<ItemStack> incoming) {
        for (ItemStack stack : incoming) {
            acceptInserterStack(stack);
        }
        return true;
    }

    private boolean acceptPackageIntoStorage(List<ItemStack> incoming, int start, int end) {
        for (ItemStack stack : incoming) {
            ItemStack remainder = insertIntoRange(stack, start, end, false);
            if (!remainder.isEmpty()) {
                dropStack(remainder, Vec3.atCenterOf(worldPosition));
            }
        }
        return true;
    }

    private boolean acceptIntoStorage(ItemStack incoming, int start, int end, boolean discardOverflow) {
        ItemStack remainder = insertIntoRange(incoming, start, end, false);
        if (!remainder.isEmpty() && !discardOverflow) {
            dropStack(remainder, Vec3.atCenterOf(worldPosition));
        }
        return true;
    }

    private boolean acceptPartitionerStack(ItemStack incoming) {
        int start = crystallizerInputAmount(incoming) > 0 ? 0 : PARTITIONER_QUEUE_SIZE;
        int end = start + PARTITIONER_QUEUE_SIZE;
        ItemStack remainder = insertIntoRange(incoming, start, end, false);
        if (!remainder.isEmpty()) {
            dropStack(remainder, Vec3.atCenterOf(worldPosition));
        }
        partitionerDirty = true;
        return true;
    }

    private boolean routeItem(ItemStack incoming) {
        Direction route = selectRoute(incoming);
        if (route == null) {
            dropStack(incoming, Vec3.atCenterOf(worldPosition));
        } else if (!spawnMovingItemOnBelt(route, incoming, false)) {
            Vec3 drop = Vec3.atCenterOf(worldPosition).add(
                    route.getStepX() * 0.55D, route.getStepY() * 0.55D, route.getStepZ() * 0.55D);
            dropStack(incoming, drop);
        }
        return true;
    }

    private boolean routePackage(List<ItemStack> incoming) {
        @SuppressWarnings("unchecked")
        List<ItemStack>[] routed = new List[6];
        List<ItemStack> rejected = new ArrayList<>();
        for (int side = 0; side < routed.length; side++) routed[side] = new ArrayList<>();
        for (ItemStack stack : incoming) {
            Direction route = selectRoute(stack);
            if (route == null) rejected.add(stack.copy());
            else routed[route.ordinal()].add(stack.copy());
        }
        for (ItemStack stack : rejected) dropStack(stack, Vec3.atCenterOf(worldPosition));
        for (Direction direction : Direction.values()) {
            List<ItemStack> contents = routed[direction.ordinal()];
            if (contents.isEmpty()) continue;
            if (!spawnPackageOnBelt(direction, contents)) {
                Vec3 drop = Vec3.atCenterOf(worldPosition).add(
                        direction.getStepX() * 0.55D, direction.getStepY() * 0.55D, direction.getStepZ() * 0.55D);
                for (ItemStack stack : contents) dropStack(stack, drop);
            }
        }
        return true;
    }

    @Nullable
    private Direction selectRoute(ItemStack stack) {
        List<Direction> valid = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            int mode = routerModes[direction.ordinal()];
            if (mode == ROUTER_MODE_NONE || mode == ROUTER_MODE_WILDCARD) continue;
            boolean matches = false;
            int start = direction.ordinal() * 5;
            for (int slot = start; slot < start + 5; slot++) {
                ItemStack pattern = items.get(slot);
                if (!pattern.isEmpty() && new DroneItemMatcher(pattern, filterModes[slot]).matches(stack)) {
                    matches = true;
                    break;
                }
            }
            if (mode == ROUTER_MODE_WHITELIST && matches || mode == ROUTER_MODE_BLACKLIST && !matches) {
                valid.add(direction);
            }
        }
        if (valid.isEmpty()) {
            for (Direction direction : Direction.values()) {
                if (routerModes[direction.ordinal()] == ROUTER_MODE_WILDCARD) valid.add(direction);
            }
        }
        return valid.isEmpty() || level == null ? null : valid.get(level.random.nextInt(valid.size()));
    }

    private boolean splitIncoming(ItemStack incoming) {
        CraneMachineBlockEntity core = splitterCore();
        int left = 0;
        int right = 0;
        int count = incoming.getCount();
        if (core.splitterRemaining <= 0) {
            core.splitterRemaining = core.splitterRightPreferred ? core.rightRatio : core.leftRatio;
        }
        while (count > 0) {
            int extracted = Math.min(core.splitterRemaining, count);
            core.splitterRemaining -= extracted;
            count -= extracted;
            if (core.splitterRightPreferred) right += extracted;
            else left += extracted;
            if (core.splitterRemaining <= 0) {
                core.splitterRightPreferred = !core.splitterRightPreferred;
                core.splitterRemaining = core.splitterRightPreferred ? core.rightRatio : core.leftRatio;
            }
        }
        if (left > 0) core.spawnMovingItemOnOwnSurface(incoming.copyWithCount(left));
        if (right > 0) {
            CraneMachineBlockEntity secondary = core.splitterOtherHalf();
            if (secondary != null) secondary.spawnMovingItemOnOwnSurface(incoming.copyWithCount(right));
            else core.dropStack(incoming.copyWithCount(right), Vec3.atCenterOf(core.worldPosition));
        }
        core.sync();
        return true;
    }

    private void emitPackage(List<Integer> slotsToPack) {
        if (slotsToPack.isEmpty() || level == null || !isBeltTarget(level, worldPosition.relative(outputSide))) {
            return;
        }
        List<ItemStack> contents = new ArrayList<>();
        for (int index = slotsToPack.size() - 1; index >= 0; index--) {
            int slot = slotsToPack.get(index);
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty()) contents.add(stack.copy());
        }
        if (contents.isEmpty() || !spawnPackageOnBelt(outputSide, contents)) {
            return;
        }
        for (int slot : slotsToPack) items.set(slot, ItemStack.EMPTY);
        sync();
    }

    private List<Integer> allOccupiedSlots(int start, int end) {
        List<Integer> occupied = new ArrayList<>();
        for (int slot = start; slot < end; slot++) if (!items.get(slot).isEmpty()) occupied.add(slot);
        return occupied;
    }

    private int ejectDelay(int slot) {
        ItemStack upgrade = items.get(slot);
        if (!(upgrade.getItem() instanceof LegacyMetaUpgradeItem) || !upgrade.is(HbmItems.UPGRADE_EJECTOR.get())) {
            return 20;
        }
        return switch (LegacyMetaUpgradeItem.tier(upgrade)) {
            case 1 -> 10;
            case 2 -> 5;
            case 3 -> 2;
            default -> 20;
        };
    }

    private int stackAmount(int slot) {
        ItemStack upgrade = items.get(slot);
        if (!(upgrade.getItem() instanceof LegacyMetaUpgradeItem) || !upgrade.is(HbmItems.UPGRADE_STACK.get())) {
            return 1;
        }
        return switch (LegacyMetaUpgradeItem.tier(upgrade)) {
            case 1 -> 4;
            case 2 -> 16;
            case 3 -> 64;
            default -> 1;
        };
    }

    private double grabberReach(Level level, Direction input) {
        if (!input.getAxis().isHorizontal()) return 1.0D;
        BlockState source = level.getBlockState(worldPosition.relative(input));
        if (source.getBlock() instanceof ConveyorBlock conveyor) {
            if (conveyor.kind() == ConveyorBlock.Kind.DOUBLE) return 0.5D;
            if (conveyor.kind() == ConveyorBlock.Kind.TRIPLE) return 0.33D;
        }
        return 1.0D;
    }

    private boolean passesFilter(ItemStack stack, int start, int end, boolean whitelistMode) {
        boolean matches = false;
        for (int slot = start; slot < end; slot++) {
            ItemStack pattern = items.get(slot);
            if (!pattern.isEmpty() && new DroneItemMatcher(pattern, filterModes[slot]).matches(stack)) {
                matches = true;
                break;
            }
        }
        return whitelistMode ? matches : !matches;
    }

    private boolean isFilterSlot(int slot) {
        return switch (kind()) {
            case EXTRACTOR, GRABBER -> slot >= 0 && slot < 9;
            case ROUTER -> slot >= 0 && slot < 30;
            default -> false;
        };
    }

    private ItemStack insertIntoRange(ItemStack incoming, int start, int end, boolean simulate) {
        if (incoming.isEmpty()) return ItemStack.EMPTY;
        ItemStack remaining = incoming.copy();
        for (int slot = start; slot < end && !remaining.isEmpty(); slot++) {
            ItemStack existing = items.get(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) continue;
            int space = Math.min(existing.getMaxStackSize(), getMaxStackSize(existing)) - existing.getCount();
            int moved = Math.min(space, remaining.getCount());
            if (moved <= 0) continue;
            if (!simulate) existing.grow(moved);
            remaining.shrink(moved);
        }
        for (int slot = start; slot < end && !remaining.isEmpty(); slot++) {
            if (!items.get(slot).isEmpty()) continue;
            int moved = Math.min(remaining.getCount(), Math.min(remaining.getMaxStackSize(), getMaxStackSize(remaining)));
            if (!simulate) items.set(slot, remaining.copyWithCount(moved));
            remaining.shrink(moved);
        }
        if (!simulate && remaining.getCount() != incoming.getCount()) sync();
        return remaining;
    }

    private ItemStack insertAdjacent(Direction direction, ItemStack stack) {
        if (level == null || stack.isEmpty()) return stack.copy();
        BlockPos target = worldPosition.relative(direction);
        Direction capabilitySide = direction.getOpposite();
        try {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, capabilitySide);
            return handler == null ? stack.copy() : ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
        } catch (RuntimeException | LinkageError failure) {
            ExternalCapabilitySafety.report(level, target, capabilitySide,
                    "item handler", "conveyor inserter/grabber output", failure);
            return stack.copy();
        }
    }

    @Nullable
    private IItemHandler adjacentHandler(Direction direction, String operation) {
        if (level == null) return null;
        BlockPos target = worldPosition.relative(direction);
        Direction capabilitySide = direction.getOpposite();
        try {
            return level.getCapability(Capabilities.ItemHandler.BLOCK, target, capabilitySide);
        } catch (RuntimeException | LinkageError failure) {
            ExternalCapabilitySafety.report(level, target, capabilitySide, "item handler", operation, failure);
            return null;
        }
    }

    private boolean spawnMovingItemOnBelt(Direction direction, ItemStack stack, boolean enterImmediately) {
        if (level == null || stack.isEmpty()) return false;
        BlockPos target = worldPosition.relative(direction);
        BlockEntity targetEntity = level.getBlockEntity(target);
        if (targetEntity instanceof CraneMachineBlockEntity crane && crane.isConveyorSurface()) {
            if (enterImmediately && crane.acceptMovingItem(stack.copy(), direction.getOpposite())) return true;
            ConveyorMovingItem moving = new ConveyorMovingItem(level, stack);
            moving.setPos(target.getX() + 0.5D, target.getY() + 0.25D, target.getZ() + 0.5D);
            level.addFreshEntity(moving);
            return true;
        }
        BlockState targetState = level.getBlockState(target);
        if (targetState.getBlock() instanceof ConveyorBlock conveyor) {
            Vec3 source = Vec3.atCenterOf(worldPosition).add(
                    direction.getStepX() * 0.55D, direction.getStepY() * 0.55D, direction.getStepZ() * 0.55D);
            Vec3 snap = conveyor.closestSnappingPosition(level, target, targetState, source);
            ConveyorMovingItem moving = new ConveyorMovingItem(level, stack);
            moving.setPos(snap.x, snap.y, snap.z);
            level.addFreshEntity(moving);
            return true;
        }
        if (targetEntity instanceof MachineDummyBlockEntity dummy
                && dummy.core() instanceof ConveyorPressBlockEntity press
                && press.isBeltPosition(target)) {
            ConveyorMovingItem moving = new ConveyorMovingItem(level, stack);
            moving.setPos(target.getX() + 0.5D, target.getY() + 0.25D, target.getZ() + 0.5D);
            level.addFreshEntity(moving);
            return true;
        }
        return false;
    }

    private boolean spawnPackageOnBelt(Direction direction, List<ItemStack> contents) {
        if (level == null || contents.isEmpty()) return false;
        BlockPos target = worldPosition.relative(direction);
        BlockEntity targetEntity = level.getBlockEntity(target);
        Vec3 spawn;
        if (targetEntity instanceof CraneMachineBlockEntity crane && crane.isConveyorSurface()) {
            spawn = new Vec3(target.getX() + 0.5D, target.getY() + 0.25D, target.getZ() + 0.5D);
        } else {
            BlockState state = level.getBlockState(target);
            if (state.getBlock() instanceof ConveyorBlock conveyor) {
                Vec3 source = Vec3.atCenterOf(worldPosition).add(
                        direction.getStepX() * 0.55D, direction.getStepY() * 0.55D, direction.getStepZ() * 0.55D);
                spawn = conveyor.closestSnappingPosition(level, target, state, source);
            } else if (targetEntity instanceof MachineDummyBlockEntity dummy
                    && dummy.core() instanceof ConveyorPressBlockEntity press
                    && press.isBeltPosition(target)) {
                spawn = new Vec3(target.getX() + 0.5D, target.getY() + 0.25D, target.getZ() + 0.5D);
            } else {
                return false;
            }
        }
        ConveyorMovingPackage moving = new ConveyorMovingPackage(level, contents);
        moving.setPos(spawn.x, spawn.y, spawn.z);
        level.addFreshEntity(moving);
        return true;
    }

    private void spawnMovingItemOnOwnSurface(ItemStack stack) {
        if (level == null || stack.isEmpty()) return;
        ConveyorMovingItem moving = new ConveyorMovingItem(level, stack);
        moving.setPos(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.25D, worldPosition.getZ() + 0.5D);
        level.addFreshEntity(moving);
    }

    private void dropStack(ItemStack stack, Vec3 position) {
        if (level == null || stack.isEmpty()) return;
        level.addFreshEntity(new ItemEntity(level, position.x, position.y, position.z, stack.copy()));
    }

    private int crystallizerInputAmount(ItemStack stack) {
        if (level == null || stack.isEmpty()) return 0;
        for (RecipeHolder<CrystallizerRecipe> holder : level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CRYSTALLIZER.get())) {
            CrystallizerRecipe recipe = holder.value();
            if (recipe.ingredient().test(stack)) return recipe.inputCount();
        }
        return 0;
    }

    private void updateHorizontalFacing() {
        if (level == null || !inputSide.getAxis().isHorizontal()) return;
        BlockState state = getBlockState();
        if (state.hasProperty(CraneMachineBlock.FACING) && state.getValue(CraneMachineBlock.FACING) != inputSide) {
            level.setBlock(worldPosition, state.setValue(CraneMachineBlock.FACING, inputSide), Block.UPDATE_CLIENTS);
        }
    }

    private CraneMachineBlockEntity splitterCore() {
        if (!splitterSecondary || level == null || splitterCorePos.equals(worldPosition)) return this;
        return level.getBlockEntity(splitterCorePos) instanceof CraneMachineBlockEntity crane
                && crane.kind() == CraneMachineBlock.Kind.SPLITTER
                ? crane
                : this;
    }

    @Nullable
    private CraneMachineBlockEntity splitterOtherHalf() {
        if (level == null) return null;
        BlockPos other = worldPosition.relative(outputSide.getClockWise());
        return level.getBlockEntity(other) instanceof CraneMachineBlockEntity crane
                && crane.kind() == CraneMachineBlock.Kind.SPLITTER
                ? crane
                : null;
    }

    public static boolean isBeltTarget(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof ConveyorBlock) return true;
        if (level.getBlockEntity(pos) instanceof CraneMachineBlockEntity crane && crane.isConveyorSurface()) return true;
        return level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && dummy.core() instanceof ConveyorPressBlockEntity press
                && press.isBeltPosition(pos);
    }

    private static int[] range(int start, int end) {
        int[] values = new int[Math.max(0, end - start)];
        Arrays.setAll(values, index -> start + index);
        return values;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("CraneInput", inputSide.ordinal());
        tag.putInt("CraneOutput", outputSide.ordinal());
        tag.putBoolean("Whitelist", whitelist);
        tag.putBoolean("MaxEject", maxEject);
        tag.putBoolean("Destroyer", destroyer);
        tag.putInt("BoxerMode", boxerMode);
        tag.putBoolean("LastRedstone", lastRedstone);
        tag.putLong("LastGrabbedTick", lastGrabbedTick);
        tag.putBoolean("SplitterRight", splitterRightPreferred);
        tag.putInt("SplitterRemaining", splitterRemaining);
        tag.putInt("LeftRatio", leftRatio);
        tag.putInt("RightRatio", rightRatio);
        tag.putBoolean("SplitterSecondary", splitterSecondary);
        tag.putLong("SplitterCore", splitterCorePos.asLong());
        tag.putIntArray("RouterModes", routerModes);
        for (int slot = 0; slot < filterModes.length; slot++) {
            if (filterModes[slot] != null) tag.putString("FilterMode" + slot, filterModes[slot]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("CraneInput")) inputSide = Direction.from3DDataValue(tag.getInt("CraneInput"));
        if (tag.contains("CraneOutput")) outputSide = Direction.from3DDataValue(tag.getInt("CraneOutput"));
        whitelist = tag.getBoolean("Whitelist");
        maxEject = tag.getBoolean("MaxEject");
        destroyer = !tag.contains("Destroyer") || tag.getBoolean("Destroyer");
        boxerMode = Math.clamp(tag.getInt("BoxerMode"), BOXER_MODE_4, BOXER_MODE_REDSTONE);
        lastRedstone = tag.getBoolean("LastRedstone");
        lastGrabbedTick = tag.getLong("LastGrabbedTick");
        splitterRightPreferred = tag.getBoolean("SplitterRight");
        splitterRemaining = Math.max(0, tag.getInt("SplitterRemaining"));
        leftRatio = Math.clamp(tag.contains("LeftRatio") ? tag.getInt("LeftRatio") : 1, 1, 16);
        rightRatio = Math.clamp(tag.contains("RightRatio") ? tag.getInt("RightRatio") : 1, 1, 16);
        splitterSecondary = tag.getBoolean("SplitterSecondary");
        splitterCorePos = tag.contains("SplitterCore") ? BlockPos.of(tag.getLong("SplitterCore")) : worldPosition;
        int[] savedRouterModes = tag.getIntArray("RouterModes");
        for (int side = 0; side < routerModes.length && side < savedRouterModes.length; side++) {
            routerModes[side] = Math.clamp(savedRouterModes[side], ROUTER_MODE_NONE, ROUTER_MODE_WILDCARD);
        }
        for (int slot = 0; slot < filterModes.length; slot++) {
            filterModes[slot] = tag.contains("FilterMode" + slot) ? tag.getString("FilterMode" + slot) : null;
        }
        partitionerDirty = true;
    }
}
