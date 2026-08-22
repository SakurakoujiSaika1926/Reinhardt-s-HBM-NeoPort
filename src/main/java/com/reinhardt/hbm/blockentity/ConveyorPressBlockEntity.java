package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.item.StampItem;
import com.reinhardt.hbm.entity.ConveyorMovingItem;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.PressRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/** Direct port of 1.7.10 TileEntityConveyorPress. */
public final class ConveyorPressBlockEntity extends BlockEntity
        implements PowerEndpoint, MachineInventory, WorldlyContainer {
    public static final long MAX_POWER = 50_000L;
    public static final long POWER_PER_TICK = 100L;
    public static final double PRESS_SPEED = 0.125D;
    private static final int[] STAMP_SLOT = {0};

    private ItemStack stamp = ItemStack.EMPTY;
    private long power;
    private long lastInput;
    private double press;
    private boolean retracting;
    private int delay;
    private boolean lastInteractionInstalledStamp;

    private double clientPreviousPress;
    private double clientRenderedPress;
    private long clientAnimationTick = Long.MIN_VALUE;

    public ConveyorPressBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CONVEYOR_PRESS.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ConveyorPressBlockEntity press) {
        if (level.isClientSide) {
            press.tickClient();
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, press);
        press.tickServer(level);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    /** TileEntityConveyorPress#getConPos: the four horizontal core faces. */
    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return List.of(
                this.worldPosition.north().immutable(),
                this.worldPosition.east().immutable(),
                this.worldPosition.south().immutable(),
                this.worldPosition.west().immutable()
        );
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return machineSide != null && getPowerConnectorPositions(level).contains(connectorPos);
    }

    @Override
    public long getAvailableOutput() {
        return 0L;
    }

    @Override
    public long getRequestedInput() {
        return this.power >= MAX_POWER ? 0L : Math.min(POWER_PER_TICK, MAX_POWER - this.power);
    }

    @Override
    public void applyPower(long usedOutput, long receivedInput) {
        long previous = this.power;
        this.power = Math.min(MAX_POWER, Math.max(0L, this.power + Math.max(0L, receivedInput)));
        this.lastInput = Math.max(0L, receivedInput);
        if (this.power != previous || receivedInput != 0L) {
            setChanged();
        }
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable(
                "message.reinhardtshbm.conveyor_press.power",
                this.lastInput,
                POWER_PER_TICK,
                this.power,
                MAX_POWER
        );
    }

    public long power() {
        return this.power;
    }

    public ItemStack stamp() {
        return this.stamp;
    }

    public boolean hasStamp() {
        return !this.stamp.isEmpty();
    }

    public double press(float partialTick) {
        if (this.level == null || !this.level.isClientSide) {
            return this.press;
        }
        return this.clientPreviousPress + (this.clientRenderedPress - this.clientPreviousPress) * partialTick;
    }

    public boolean canHandleItem(ItemStack held) {
        return (!held.isEmpty() && StampItem.isStamp(held) && this.stamp.isEmpty())
                || (ScrewdriverItem.isScrewdriver(held) && !this.stamp.isEmpty());
    }

    /** Handles old block activation and screwdriver removal on core or dummy. */
    public boolean handleItemInteraction(Player player, ItemStack held) {
        this.lastInteractionInstalledStamp = false;
        if (this.level == null || this.level.isClientSide) {
            return canHandleItem(held);
        }

        if (StampItem.isStamp(held) && this.stamp.isEmpty()) {
            this.stamp = held.copyWithCount(1);
            held.shrink(1);
            this.lastInteractionInstalledStamp = true;
            setChangedAndSync();
            return true;
        }

        if (ScrewdriverItem.isScrewdriver(held) && !this.stamp.isEmpty()) {
            ItemStack removed = this.stamp.copy();
            this.stamp = ItemStack.EMPTY;
            // Player inventory retains and drops any remainder in modern MC.
            player.getInventory().placeItemBackInInventory(removed);
            setChangedAndSync();
            return true;
        }
        return false;
    }

    public boolean lastInteractionInstalledStamp() {
        return this.lastInteractionInstalledStamp;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.stamp.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? this.stamp : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0 || this.stamp.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.stamp.split(amount);
        if (this.stamp.isEmpty()) {
            this.stamp = ItemStack.EMPTY;
        }
        setChangedAndSync();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.stamp;
        this.stamp = ItemStack.EMPTY;
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0 || !stack.isEmpty() && !StampItem.isStamp(stack)) {
            return;
        }
        this.stamp = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChangedAndSync();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && this.stamp.isEmpty() && StampItem.isStamp(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return STAMP_SLOT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // TileEntityConveyorPress#canExtractItem inherited the default false.
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.stamp = ItemStack.EMPTY;
        setChangedAndSync();
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        if (!this.stamp.isEmpty()) {
            Block.popResource(level, pos, this.stamp);
            this.stamp = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Stamp", this.stamp.saveOptional(registries));
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putDouble("Press", this.press);
        tag.putBoolean("Retracting", this.retracting);
        tag.putInt("Delay", this.delay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.stamp = ItemStack.parseOptional(registries, tag.getCompound("Stamp"));
        if (!this.stamp.isEmpty() && !StampItem.isStamp(this.stamp)) {
            this.stamp = ItemStack.EMPTY;
        }
        this.power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("Power")));
        this.lastInput = Math.max(0L, tag.getLong("LastInput"));
        this.press = Math.max(0.0D, Math.min(1.0D, tag.getDouble("Press")));
        this.retracting = tag.getBoolean("Retracting");
        this.delay = Math.max(0, tag.getInt("Delay"));
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
        if (this.delay > 0) {
            this.delay--;
            changed = true;
        } else if (this.retracting) {
            if (this.power >= POWER_PER_TICK) {
                this.press = Math.max(0.0D, this.press - PRESS_SPEED);
                this.power -= POWER_PER_TICK;
                changed = true;
                if (this.press <= 0.0D) {
                    this.retracting = false;
                }
            }
        } else {
            ConveyorMovingItem input = findInput(level);
            if (input != null && this.power >= POWER_PER_TICK) {
                this.press = Math.min(1.0D, this.press + PRESS_SPEED);
                this.power -= POWER_PER_TICK;
                changed = true;
                if (this.press >= 1.0D) {
                    this.retracting = true;
                    this.delay = 5;
                    processInputs(level);
                }
            }
        }

        // Old networkPackNT sent a state packet at least once per second.
        if (changed || level.getGameTime() % 20L == 0L) {
            setChangedAndSync();
        }
    }

    @Nullable
    private ConveyorMovingItem findInput(Level level) {
        if (this.stamp.isEmpty() || !StampItem.isStamp(this.stamp)) {
            return null;
        }
        AABB belt = beltBounds();
        for (ConveyorMovingItem entity : level.getEntitiesOfClass(ConveyorMovingItem.class, belt, ConveyorMovingItem::isAlive)) {
            ItemStack stack = entity.getItemStack();
            if (stack.getCount() != 1 || conveyorRecipe(level, stack).isEmpty()) {
                continue;
            }
            if (entity.getX() > this.worldPosition.getX() + 0.35D && entity.getX() < this.worldPosition.getX() + 0.65D
                    && entity.getZ() > this.worldPosition.getZ() + 0.35D && entity.getZ() < this.worldPosition.getZ() + 0.65D) {
                entity.setPos(this.worldPosition.getX() + 0.5D, entity.getY(), this.worldPosition.getZ() + 0.5D);
            }
            return entity;
        }
        return null;
    }

    /** TileEntityConveyorPress#process scans every eligible one-item entity. */
    private void processInputs(Level level) {
        boolean processedAny = false;
        for (ConveyorMovingItem entity : level.getEntitiesOfClass(ConveyorMovingItem.class, beltBounds(), ConveyorMovingItem::isAlive)) {
            ItemStack input = entity.getItemStack();
            if (input.getCount() != 1) {
                continue;
            }
            Optional<RecipeHolder<PressRecipe>> holder = conveyorRecipe(level, input);
            if (holder.isEmpty()) {
                continue;
            }
            ItemStack output = holder.get().value().assemble(new PressRecipe.Input(input, this.stamp), level.registryAccess());
            if (output.isEmpty()) {
                continue;
            }
            ConveyorMovingItem result = new ConveyorMovingItem(level, output.copy());
            result.setPos(entity.getX(), entity.getY(), entity.getZ());
            result.setDeltaMovement(entity.getDeltaMovement());
            entity.discard();
            level.addFreshEntity(result);
            processedAny = true;
        }

        if (processedAny) {
            level.playSound(null, this.worldPosition, HbmSoundEvents.PRESS_OPERATE.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
            StampItem.damageStamp(this.stamp);
            if (this.stamp.isEmpty()) {
                this.stamp = ItemStack.EMPTY;
            }
        }
    }

    private Optional<RecipeHolder<PressRecipe>> conveyorRecipe(Level level, ItemStack input) {
        PressRecipe.Input recipeInput = new PressRecipe.Input(input, this.stamp);
        return level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PRESS.get()).stream()
                .filter(recipe -> recipe.value().matches(recipeInput, level))
                .findFirst();
    }

    private AABB beltBounds() {
        return new AABB(
                this.worldPosition.getX(), this.worldPosition.getY() + 1.0D, this.worldPosition.getZ(),
                this.worldPosition.getX() + 1.0D, this.worldPosition.getY() + 1.5D, this.worldPosition.getZ() + 1.0D
        );
    }

    /** MachineConveyorPress#canItemStay: only the first dummy directly above the core is a belt. */
    public boolean isBeltPosition(BlockPos pos) {
        return pos.equals(this.worldPosition.above());
    }

    /** MachineConveyorPress#getTravelLocation using the old core-facing rotation. */
    public Vec3 travelLocation(Vec3 itemPos, double speed) {
        Direction facing = this.getBlockState().hasProperty(LargeMachineBlock.FACING)
                ? this.getBlockState().getValue(LargeMachineBlock.FACING)
                : Direction.NORTH;
        Direction travelDirection = LegacyMachineGeometry.forgeRotateUp(facing);
        double x = Mth.clamp(itemPos.x, this.worldPosition.getX(), this.worldPosition.getX() + 1.0D);
        double z = Mth.clamp(itemPos.z, this.worldPosition.getZ(), this.worldPosition.getZ() + 1.0D);
        double snappedX = travelDirection.getStepX() != 0 ? x : this.worldPosition.getX() + 0.5D;
        double snappedZ = travelDirection.getStepZ() != 0 ? z : this.worldPosition.getZ() + 0.5D;
        Vec3 snap = new Vec3(snappedX, this.worldPosition.getY() + 1.25D, snappedZ);
        Vec3 destination = snap.subtract(
                travelDirection.getStepX() * speed,
                0.0D,
                travelDirection.getStepZ() * speed
        );
        Vec3 delta = destination.subtract(itemPos);
        return delta.lengthSqr() <= 1.0E-12D ? destination : itemPos.add(delta.scale(speed / delta.length()));
    }

    private void tickClient() {
        if (this.level == null) {
            return;
        }
        long time = this.level.getGameTime();
        if (this.clientAnimationTick == time) {
            return;
        }
        this.clientPreviousPress = this.clientRenderedPress;
        if (this.clientAnimationTick == Long.MIN_VALUE) {
            this.clientRenderedPress = this.press;
            this.clientPreviousPress = this.press;
        } else {
            // Same two-tick approach interpolation used by the old client TE.
            this.clientRenderedPress += (this.press - this.clientRenderedPress) * 0.5D;
        }
        this.clientAnimationTick = time;
    }

    private void setChangedAndSync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
