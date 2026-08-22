package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.MachineUpgradeItem;
import com.reinhardt.hbm.menu.ExposureChamberMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.recipe.ExposureChamberRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ExposureChamberBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int PARTICLE_SLOT = 0;
    public static final int CACHED_PARTICLE_SLOT = 1;
    public static final int PARTICLE_CONTAINER_OUTPUT_SLOT = 2;
    public static final int INGREDIENT_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    public static final int BATTERY_SLOT = 5;
    public static final int UPGRADE_A_SLOT = 6;
    public static final int UPGRADE_B_SLOT = 7;
    public static final int SLOT_COUNT = 8;
    public static final int DATA_COUNT = 8;
    public static final long MAX_POWER = 1_000_000L;
    public static final int PROCESS_TIME_BASE = 200;
    public static final int CONSUMPTION_BASE = 10_000;
    public static final int MAX_PARTICLES = 8;

    private static final int[] ACCESSIBLE = {PARTICLE_SLOT, PARTICLE_CONTAINER_OUTPUT_SLOT, INGREDIENT_SLOT, OUTPUT_SLOT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private long power;
    private int progress;
    private int processTime = PROCESS_TIME_BASE;
    private int consumption = CONSUMPTION_BASE;
    private int savedParticles;
    private boolean on;
    private float rotation;
    private float prevRotation;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, power);
                case 1 -> progress;
                case 2 -> processTime;
                case 3 -> consumption;
                case 4 -> savedParticles;
                case 5 -> on ? 1 : 0;
                case 6 -> (int) Math.min(Integer.MAX_VALUE, MAX_POWER);
                case 7 -> MAX_PARTICLES;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> power = Math.max(0, value);
                case 1 -> progress = Math.max(0, value);
                case 2 -> processTime = Math.max(1, value);
                case 3 -> consumption = Math.max(0, value);
                case 4 -> savedParticles = Math.max(0, value);
                case 5 -> on = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ExposureChamberBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.EXPOSURE_CHAMBER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ExposureChamberBlockEntity chamber) {
        if (level.isClientSide) {
            chamber.tickClient();
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, chamber);
        chamber.tickServer();
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public boolean isOn() {
        return this.on;
    }

    public float rotation(float partialTick) {
        return this.prevRotation + (this.rotation - this.prevRotation) * partialTick;
    }

    private void tickClient() {
        this.prevRotation = this.rotation;
        if (this.on) {
            this.rotation += 10.0F;
            if (this.rotation >= 720.0F) {
                this.rotation -= 720.0F;
                this.prevRotation -= 720.0F;
            }
        }
    }

    private void tickServer() {
        this.on = false;
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        updateUpgrades();
        loadParticleCache();

        if (!items.get(CACHED_PARTICLE_SLOT).isEmpty() && this.savedParticles > 0 && this.power >= this.consumption) {
            Optional<RecipeHolder<ExposureChamberRecipe>> holder = currentRecipe();
            if (holder.isPresent() && canAcceptOutput(holder.get().value().output())) {
                this.progress++;
                this.power -= this.consumption;
                this.on = true;
                if (this.progress >= this.processTime) {
                    this.progress = 0;
                    this.savedParticles--;
                    items.get(INGREDIENT_SLOT).shrink(1);
                    if (items.get(INGREDIENT_SLOT).isEmpty()) {
                        items.set(INGREDIENT_SLOT, ItemStack.EMPTY);
                    }
                    addOutput(holder.get().value().output());
                }
            } else {
                this.progress = 0;
            }
        } else {
            this.progress = 0;
        }

        if (this.savedParticles <= 0) {
            this.savedParticles = 0;
            items.set(CACHED_PARTICLE_SLOT, ItemStack.EMPTY);
        }
        sync();
    }

    private void updateUpgrades() {
        int speedLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.SPEED);
        int powerLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.POWER);
        int overdriveLevel = upgradeLevel(MachineUpgradeItem.UpgradeType.OVERDRIVE);
        this.consumption = CONSUMPTION_BASE;
        this.processTime = PROCESS_TIME_BASE - PROCESS_TIME_BASE / 4 * speedLevel;
        this.consumption *= (speedLevel / 2 + 1);
        this.processTime *= (powerLevel / 2 + 1);
        this.consumption /= (powerLevel + 1);
        this.processTime /= (overdriveLevel + 1);
        this.consumption *= (overdriveLevel * 2 + 1);
        this.processTime = Math.max(1, this.processTime);
    }

    private int upgradeLevel(MachineUpgradeItem.UpgradeType type) {
        int level = 0;
        for (int slot = UPGRADE_A_SLOT; slot <= UPGRADE_B_SLOT; slot++) {
            ItemStack stack = items.get(slot);
            if (MachineUpgradeItem.upgradeType(stack) == type) {
                level += Math.max(0, MachineUpgradeItem.upgradeTier(stack));
            }
        }
        return Math.min(3, level);
    }

    private void loadParticleCache() {
        if (!items.get(CACHED_PARTICLE_SLOT).isEmpty() || items.get(PARTICLE_SLOT).isEmpty() || items.get(INGREDIENT_SLOT).isEmpty() || this.savedParticles > 0) {
            return;
        }
        Optional<RecipeHolder<ExposureChamberRecipe>> holder = currentRecipe(items.get(PARTICLE_SLOT), items.get(INGREDIENT_SLOT));
        if (holder.isEmpty()) {
            return;
        }
        ItemStack container = items.get(PARTICLE_SLOT).getCraftingRemainingItem();
        if (!container.isEmpty()) {
            ItemStack out = items.get(PARTICLE_CONTAINER_OUTPUT_SLOT);
            if (!out.isEmpty() && (!ItemStack.isSameItemSameComponents(out, container) || out.getCount() + container.getCount() > out.getMaxStackSize())) {
                return;
            }
            if (out.isEmpty()) {
                items.set(PARTICLE_CONTAINER_OUTPUT_SLOT, container.copy());
            } else {
                out.grow(container.getCount());
            }
        }
        ItemStack cached = items.get(PARTICLE_SLOT).copy();
        cached.setCount(1);
        items.set(CACHED_PARTICLE_SLOT, cached);
        items.get(PARTICLE_SLOT).shrink(1);
        if (items.get(PARTICLE_SLOT).isEmpty()) {
            items.set(PARTICLE_SLOT, ItemStack.EMPTY);
        }
        this.savedParticles = MAX_PARTICLES;
    }

    private Optional<RecipeHolder<ExposureChamberRecipe>> currentRecipe() {
        return currentRecipe(items.get(CACHED_PARTICLE_SLOT), items.get(INGREDIENT_SLOT));
    }

    private Optional<RecipeHolder<ExposureChamberRecipe>> currentRecipe(ItemStack particle, ItemStack ingredient) {
        if (level == null || particle.isEmpty() || ingredient.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(HbmRecipeTypes.EXPOSURE_CHAMBER.get(), new ExposureChamberRecipe.Input(particle, ingredient), level);
    }

    private boolean canAcceptOutput(ItemStack output) {
        ItemStack slot = items.get(OUTPUT_SLOT);
        return slot.isEmpty() || (ItemStack.isSameItemSameComponents(slot, output) && slot.getCount() + output.getCount() <= slot.getMaxStackSize());
    }

    private void addOutput(ItemStack output) {
        ItemStack result = output.copy();
        ItemStack slot = items.get(OUTPUT_SLOT);
        if (slot.isEmpty()) {
            items.set(OUTPUT_SLOT, result);
        } else {
            slot.grow(result.getCount());
        }
    }

    @Override
    public BlockPos getPowerPos() {
        return worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        Direction dir = getBlockState().getValue(LargeMachineBlock.FACING);
        return powerConnectors(this.worldPosition, dir);
    }

    public static List<BlockPos> powerConnectors(BlockPos corePos, Direction dir) {
        Direction rot = LegacyMachineGeometry.forgeRotateUp(dir).getOpposite();
        return List.of(
                corePos.relative(rot, 7).relative(dir, 2),
                corePos.relative(rot, 7).relative(dir.getOpposite(), 2),
                corePos.relative(rot, 8).relative(dir, 2),
                corePos.relative(rot, 8).relative(dir.getOpposite(), 2),
                corePos.relative(rot, 9)
        );
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        Direction dir = getBlockState().getValue(LargeMachineBlock.FACING);
        Direction rot = LegacyMachineGeometry.forgeRotateUp(dir).getOpposite();
        return (connectorPos.equals(worldPosition.relative(rot, 7).relative(dir, 2)) && machineSide == dir)
                || (connectorPos.equals(worldPosition.relative(rot, 7).relative(dir.getOpposite(), 2)) && machineSide == dir.getOpposite())
                || (connectorPos.equals(worldPosition.relative(rot, 8).relative(dir, 2)) && machineSide == dir)
                || (connectorPos.equals(worldPosition.relative(rot, 8).relative(dir.getOpposite(), 2)) && machineSide == dir.getOpposite())
                || (connectorPos.equals(worldPosition.relative(rot, 9)) && machineSide == rot);
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
            this.power = Math.min(MAX_POWER, this.power + receivedInput);
            sync();
        }
    }

    @Override
    public Component getPowerStatus() {
        return Component.literal(power + " / " + MAX_POWER + " HE");
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
        if (slot == PARTICLE_SLOT || slot == INGREDIENT_SLOT) {
            return true;
        }
        if (slot == BATTERY_SLOT) {
            return BatteryPackItem.isBattery(stack);
        }
        return slot == UPGRADE_A_SLOT || slot == UPGRADE_B_SLOT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return (slot == PARTICLE_SLOT || slot == INGREDIENT_SLOT) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == PARTICLE_CONTAINER_OUTPUT_SLOT || slot == OUTPUT_SLOT;
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
        return Component.translatable("container.reinhardtshbm.machine_exposure_chamber");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ExposureChamberMenu(containerId, inventory, this, menuData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
        tag.putInt("savedParticles", savedParticles);
        tag.putBoolean("isOn", on);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        power = Math.max(0L, Math.min(MAX_POWER, tag.getLong("power")));
        progress = Math.max(0, tag.getInt("progress"));
        savedParticles = Math.max(0, Math.min(MAX_PARTICLES, tag.getInt("savedParticles")));
        on = tag.getBoolean("isOn");
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

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

}
