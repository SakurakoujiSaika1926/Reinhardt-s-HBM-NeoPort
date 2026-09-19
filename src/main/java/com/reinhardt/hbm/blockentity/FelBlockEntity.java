package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.client.sound.FelClientSounds;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.FelCrystalItem;
import com.reinhardt.hbm.menu.FelMenu;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import com.reinhardt.hbm.util.Wavelength;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;

public class FelBlockEntity extends BlockEntity implements PowerEndpoint, MachineInventory, WorldlyContainer, MenuProvider {
    public static final int BATTERY_SLOT = 0;
    public static final int CRYSTAL_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    public static final long MAX_POWER = 20_000_000L;
    public static final int POWER_REQ = 1250;
    public static final int DATA_COUNT = 8;

    private static final int[] ACCESSIBLE_SLOTS = {BATTERY_SLOT, CRYSTAL_SLOT};
    private static final int VISIBLE_BLINDNESS_TICKS = 60 * 60 * 65536;
    private static final int RANGE = 24;
    // HBM 1.7.10 MachineFEL marks the rear top-center block as TileEntityProxyEnergy:
    // makeExtra(world, x + dir.offsetX * (o - 4), y + 1, z + dir.offsetZ * (o - 4)), with getOffset() = 2.
    private static final BlockPos POWER_PROXY_OFFSET = new BlockPos(0, 1, -4);

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private long power;
    private long lastInput;
    private Wavelength mode = Wavelength.NULL;
    private boolean isOn;
    private boolean missingValidSilex = true;
    private int distance = RANGE;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) FelBlockEntity.this.power;
                case 1 -> (int) MAX_POWER;
                case 2 -> (int) FelBlockEntity.this.lastInput;
                case 3 -> FelBlockEntity.this.mode.ordinal();
                case 4 -> FelBlockEntity.this.isOn ? 1 : 0;
                case 5 -> FelBlockEntity.this.missingValidSilex ? 1 : 0;
                case 6 -> FelBlockEntity.this.distance;
                case 7 -> activeCost();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> FelBlockEntity.this.power = Integer.toUnsignedLong(value);
                case 2 -> FelBlockEntity.this.lastInput = Integer.toUnsignedLong(value);
                case 3 -> {
                    Wavelength[] values = Wavelength.values();
                    FelBlockEntity.this.mode = value >= 0 && value < values.length ? values[value] : Wavelength.NULL;
                }
                case 4 -> FelBlockEntity.this.isOn = value != 0;
                case 5 -> FelBlockEntity.this.missingValidSilex = value != 0;
                case 6 -> FelBlockEntity.this.distance = Math.max(0, value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FelBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.FEL.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FelBlockEntity fel) {
        if (level.isClientSide) {
            FelClientSounds.tick(fel);
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, fel);
        fel.tickServer(level);
    }

    public ContainerData menuData() {
        return this.menuData;
    }

    public long power() {
        return this.power;
    }

    public boolean isOn() {
        return this.isOn;
    }

    public boolean missingValidSilex() {
        return this.missingValidSilex;
    }

    public int distance() {
        return this.distance;
    }

    public Wavelength mode() {
        return this.mode;
    }

    public boolean beamVisible() {
        return this.isOn && this.mode != Wavelength.NULL && this.power > visibleThreshold(this.mode) && this.distance - 3 > 0;
    }

    public void toggleEnabled() {
        this.isOn = !this.isOn;
        sync(true);
    }

    @Override
    public BlockPos getPowerPos() {
        return this.worldPosition;
    }

    @Override
    public List<BlockPos> getPowerConnectorPositions(LevelAccessor level) {
        return List.of(powerCablePos().immutable());
    }

    @Override
    public boolean canConnectPower(LevelAccessor level, BlockPos connectorPos, Direction machineSide) {
        return connectorPos.equals(powerCablePos()) && machineSide == facing().getOpposite();
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
        this.power = Math.min(MAX_POWER, this.power + receivedInput);
        setChanged();
    }

    @Override
    public Component getPowerStatus() {
        return Component.translatable("message.reinhardtshbm.power.fel", this.lastInput, this.power, MAX_POWER);
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
        return slot >= 0 && slot < SLOT_COUNT ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= SLOT_COUNT || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot).split(amount);
        if (this.items.get(slot).isEmpty()) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        if (!removed.isEmpty()) {
            sync(false);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return;
        }
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        sync(false);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case BATTERY_SLOT -> BatteryPackItem.isBattery(stack);
            case CRYSTAL_SLOT -> stack.getItem() instanceof FelCrystalItem;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        sync(true);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.machine_fel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FelMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack.copy()));
            }
        }
        clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            tag.put("Slot" + slot, this.items.get(slot).saveOptional(registries));
        }
        tag.putLong("Power", this.power);
        tag.putLong("LastInput", this.lastInput);
        tag.putString("Mode", this.mode.getSerializedName());
        tag.putBoolean("Enabled", this.isOn);
        tag.putBoolean("MissingValidSilex", this.missingValidSilex);
        tag.putInt("Distance", this.distance);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.parseOptional(registries, tag.getCompound("Slot" + slot)));
        }
        this.power = tag.getLong("Power");
        this.lastInput = tag.getLong("LastInput");
        this.mode = Wavelength.byName(tag.getString("Mode"));
        this.isOn = tag.getBoolean("Enabled");
        this.missingValidSilex = tag.getBoolean("MissingValidSilex");
        this.distance = tag.contains("Distance") ? tag.getInt("Distance") : RANGE;
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
        this.power = BatteryPackItem.dischargeIntoMachine(this.items.get(BATTERY_SLOT), this.power, MAX_POWER);
        this.mode = currentMode();
        this.missingValidSilex = true;
        this.distance = RANGE;

        int req = activeCost();
        if (this.isOn && this.mode != Wavelength.NULL && this.power < req) {
            this.power = 0L;
        }

        if (this.isOn && this.mode != Wavelength.NULL && this.power >= req) {
            affectEntities(level);
            this.power -= req;
            scanBeam(level);
        }

        sync(level.getGameTime() % 10L == 0L);
    }

    private Wavelength currentMode() {
        if (!this.isOn) {
            return Wavelength.NULL;
        }
        ItemStack stack = this.items.get(CRYSTAL_SLOT);
        if (stack.getItem() instanceof FelCrystalItem crystal) {
            return crystal.wavelength();
        }
        return Wavelength.NULL;
    }

    private void affectEntities(Level level) {
        Direction facing = facing();
        int maxDistance = Math.max(0, this.distance - 1);
        if (maxDistance <= 0) {
            return;
        }
        double minX = Math.min(this.worldPosition.getX(), this.worldPosition.getX() + facing.getStepX() * maxDistance) + 0.2D;
        double maxX = Math.max(this.worldPosition.getX(), this.worldPosition.getX() + facing.getStepX() * maxDistance) + 0.8D;
        double minY = this.worldPosition.getY() + 1.2D;
        double maxY = this.worldPosition.getY() + 1.8D;
        double minZ = Math.min(this.worldPosition.getZ(), this.worldPosition.getZ() + facing.getStepZ() * maxDistance) + 0.2D;
        double maxZ = Math.max(this.worldPosition.getZ(), this.worldPosition.getZ() + facing.getStepZ() * maxDistance) + 0.8D;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(minX, minY, minZ, maxX, maxY, maxZ))) {
            switch (this.mode) {
                case VISIBLE -> {
                    living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, VISIBLE_BLINDNESS_TICKS, 0));
                    living.igniteForSeconds(10.0F);
                }
                case IR, UV -> living.igniteForSeconds(10.0F);
                case GAMMA -> {
                    HbmLivingRadiation data = HbmLivingRadiation.get(living);
                    data.addRadiationWithReadout(25.0F);
                    HbmLivingRadiation.set(living, data);
                }
                case DRX -> {
                    HbmLivingRadiation data = HbmLivingRadiation.get(living);
                    data.addDigamma(0.1F);
                    HbmLivingRadiation.set(living, data);
                }
                default -> {
                }
            }
        }
    }

    private void scanBeam(Level level) {
        Direction facing = facing();
        boolean silexSpacing = false;
        for (int i = 3; i < RANGE; i++) {
            BlockPos beamPos = this.worldPosition.relative(facing, i).above();
            BlockState state = level.getBlockState(beamPos);
            Block block = state.getBlock();

            SilexHit silexHit = resolveSilexHit(level, beamPos, facing);
            if (silexHit != null) {
                if (rotationIsValid(silexHit.silex()) && i >= 5 && !silexSpacing) {
                    if (silexHit.silex().mode() != this.mode) {
                        silexHit.silex().setWavelength(this.mode);
                    }
                    this.missingValidSilex = false;
                    silexSpacing = true;
                    continue;
                }
                level.destroyBlock(silexHit.corePos(), true);
                continue;
            }

            if (isSilexPart(level, beamPos)) {
                continue;
            }

            FluidState fluidState = level.getFluidState(beamPos);
            if (!fluidState.isEmpty()) {
                this.distance = i;
                level.removeBlock(beamPos, false);
                level.levelEvent(1501, beamPos, 0);
                break;
            }

            if (state.isAir() || (!state.canOcclude() && block != Blocks.TNT)) {
                this.distance = RANGE;
                silexSpacing = false;
                continue;
            }

            if (state.canOcclude() || block == Blocks.TNT) {
                this.distance = i;
                if (state.getExplosionResistance(level, beamPos, null) < 75.0F && level.random.nextInt(5) == 0) {
                    if (this.mode == Wavelength.DRX) {
                        level.setBlock(beamPos, HbmBlocks.FIRE_DIGAMMA.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                        BlockPos below = beamPos.below();
                        if (level.getBlockState(below).canBeReplaced()) {
                            level.setBlock(below, HbmBlocks.ASH_DIGAMMA.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                        }
                    } else {
                        level.setBlock(beamPos, Blocks.FIRE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                break;
            }
        }
    }

    @Nullable
    private SilexHit resolveSilexHit(Level level, BlockPos beamPos, Direction facing) {
        BlockPos corePos = beamPos.relative(facing).below();
        if (!(level.getBlockEntity(corePos) instanceof SilexBlockEntity silex)) {
            return null;
        }
        if (level.getBlockEntity(beamPos) instanceof MachineDummyBlockEntity dummy && dummy.getCorePos().equals(corePos)) {
            return new SilexHit(corePos.immutable(), silex);
        }
        return null;
    }

    private boolean isSilexPart(Level level, BlockPos beamPos) {
        BlockEntity hitEntity = level.getBlockEntity(beamPos);
        if (hitEntity instanceof SilexBlockEntity) {
            return true;
        }
        return hitEntity instanceof MachineDummyBlockEntity dummy
                && level.getBlockEntity(dummy.getCorePos()) instanceof SilexBlockEntity;
    }

    private boolean rotationIsValid(SilexBlockEntity silex) {
        Direction other = silex.getBlockState().hasProperty(LargeMachineBlock.FACING)
                ? silex.getBlockState().getValue(LargeMachineBlock.FACING)
                : Direction.SOUTH;
        Direction self = facing();
        return other == self || other == self.getOpposite();
    }

    private Direction facing() {
        return this.getBlockState().hasProperty(LargeMachineBlock.FACING)
                ? this.getBlockState().getValue(LargeMachineBlock.FACING)
                : Direction.SOUTH;
    }

    private BlockPos powerProxyPos() {
        return this.worldPosition.offset(LegacyMachineGeometry.rotateLegacySouth(POWER_PROXY_OFFSET, facing())).immutable();
    }

    private BlockPos powerCablePos() {
        return powerProxyPos().relative(facing().getOpposite()).immutable();
    }

    private int activeCost() {
        if (this.mode == Wavelength.NULL) {
            return 0;
        }
        return switch (this.mode) {
            case IR -> POWER_REQ;
            case VISIBLE -> POWER_REQ * 3;
            case UV -> POWER_REQ * 9;
            case GAMMA -> POWER_REQ * 27;
            case DRX -> POWER_REQ * 81;
            default -> 0;
        };
    }

    private static long visibleThreshold(Wavelength mode) {
        return switch (mode) {
            case IR -> POWER_REQ;
            case VISIBLE -> POWER_REQ * 2L;
            case UV -> POWER_REQ * 4L;
            case GAMMA -> POWER_REQ * 8L;
            case DRX -> POWER_REQ * 16L;
            default -> Long.MAX_VALUE;
        };
    }

    private record SilexHit(BlockPos corePos, SilexBlockEntity silex) {
    }

    private void sync(boolean fullClientSync) {
        setChanged();
        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
            if (fullClientSync && !this.level.isClientSide) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }
}
