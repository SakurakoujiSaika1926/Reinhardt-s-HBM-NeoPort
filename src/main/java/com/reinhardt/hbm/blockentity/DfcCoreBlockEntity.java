package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.explosion.NukeExplosionManager;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.item.AmsCatalystItem;
import com.reinhardt.hbm.item.AmsCoreItem;
import com.reinhardt.hbm.menu.DfcCoreMenu;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;
import java.util.Set;

public class DfcCoreBlockEntity extends DfcInventoryBlockEntity implements MenuProvider {
    public static final int CATALYST_LEFT_SLOT = 0;
    public static final int CORE_SLOT = 1;
    public static final int CATALYST_RIGHT_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int TANK_CAPACITY = 128_000;
    public static final int DATA_COUNT = 14;

    private static final Set<String> VALID_FUELS = Set.of(
            "hydrogen",
            "deuterium",
            "tritium",
            "oxygen",
            "peroxide",
            "xenon",
            "sas3",
            "balefire",
            "amat",
            "aschrab"
    );

    private final HbmFluidTank[] tanks = {
            new HbmFluidTank(fluid("deuterium"), TANK_CAPACITY),
            new HbmFluidTank(fluid("tritium"), TANK_CAPACITY)
    };
    private final IFluidHandler fluidHandler = new DfcFluidHandler(this.tanks, DfcCoreBlockEntity::isValidFuel);
    private int field;
    private int heat;
    private int color;
    private int consumption;
    private int prevConsumption;
    private boolean lastTickValid;
    private boolean meltdownTick;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> DfcCoreBlockEntity.this.tanks[0].type().oldId();
                case 1 -> DfcCoreBlockEntity.this.tanks[0].amount();
                case 2 -> DfcCoreBlockEntity.this.tanks[0].capacity();
                case 3 -> DfcCoreBlockEntity.this.tanks[1].type().oldId();
                case 4 -> DfcCoreBlockEntity.this.tanks[1].amount();
                case 5 -> DfcCoreBlockEntity.this.tanks[1].capacity();
                case 6 -> DfcCoreBlockEntity.this.field;
                case 7 -> DfcCoreBlockEntity.this.heat;
                case 8 -> DfcCoreBlockEntity.this.color;
                case 9 -> DfcCoreBlockEntity.this.meltdownTick ? 1 : 0;
                case 10 -> DfcCoreBlockEntity.this.prevConsumption;
                case 11 -> DfcCoreBlockEntity.this.coreMultiplier();
                case 12 -> Math.round(DfcCoreBlockEntity.this.fuelEfficiency(DfcCoreBlockEntity.this.tanks[0].type()) * 100.0F);
                case 13 -> Math.round(DfcCoreBlockEntity.this.fuelEfficiency(DfcCoreBlockEntity.this.tanks[1].type()) * 100.0F);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> DfcCoreBlockEntity.this.tanks[0].setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 1 -> DfcCoreBlockEntity.this.tanks[0].setAmount(value);
                case 3 -> DfcCoreBlockEntity.this.tanks[1].setType(HbmFluids.byOldId(value).orElse(HbmFluids.none()));
                case 4 -> DfcCoreBlockEntity.this.tanks[1].setAmount(value);
                case 6 -> DfcCoreBlockEntity.this.field = value;
                case 7 -> DfcCoreBlockEntity.this.heat = value;
                case 8 -> DfcCoreBlockEntity.this.color = value;
                case 9 -> DfcCoreBlockEntity.this.meltdownTick = value != 0;
                case 10 -> DfcCoreBlockEntity.this.prevConsumption = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public DfcCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.DFC_CORE.get(), pos, blockState, SLOT_COUNT);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DfcCoreBlockEntity core) {
        if (level.isClientSide) {
            return;
        }
        core.tickServer(level);
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

    public int field() {
        return this.field;
    }

    public void stabilize(int watts) {
        this.field = Math.max(this.field, watts);
        setChangedAndSync(true);
    }

    public int heat() {
        return this.heat;
    }

    public int color() {
        return this.color;
    }

    public boolean meltdownTick() {
        return this.meltdownTick;
    }

    public long burn(long joules) {
        if (!isReady()) {
            return joules;
        }
        int demand = (int) Math.ceil(joules / 1000.0D);
        if (this.tanks[0].amount() < demand || this.tanks[1].amount() < demand) {
            return joules;
        }
        this.consumption += demand;
        this.heat += (int) Math.ceil(joules / 10000.0D);
        this.tanks[0].drain(this.tanks[0].type(), demand, false);
        this.tanks[1].drain(this.tanks[1].type(), demand, false);
        setChangedAndSync(true);
        return (long) (joules * coreMultiplier() * fuelEfficiency(this.tanks[0].type()) * fuelEfficiency(this.tanks[1].type()));
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case CATALYST_LEFT_SLOT, CATALYST_RIGHT_SLOT -> AmsCatalystItem.isCatalyst(stack);
            case CORE_SLOT -> stack.getItem() instanceof AmsCoreItem;
            default -> false;
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.reinhardtshbm.dfc_core");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DfcCoreMenu(containerId, playerInventory, this, this.menuData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fuel1", this.tanks[0].save());
        tag.put("Fuel2", this.tanks[1].save());
        tag.putInt("Field", this.field);
        tag.putInt("Heat", this.heat);
        tag.putInt("Color", this.color);
        tag.putInt("Consumption", this.consumption);
        tag.putInt("PrevConsumption", this.prevConsumption);
        tag.putBoolean("MeltdownTick", this.meltdownTick);
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
        this.field = tag.getInt("Field");
        this.heat = tag.getInt("Heat");
        this.color = tag.getInt("Color");
        this.consumption = tag.getInt("Consumption");
        this.prevConsumption = tag.getInt("PrevConsumption");
        this.meltdownTick = tag.getBoolean("MeltdownTick");
    }

    private void tickServer(Level level) {
        this.prevConsumption = this.consumption;
        this.consumption = 0;
        this.meltdownTick = false;
        this.lastTickValid = surroundingChunksLoaded(level);

        if (this.lastTickValid && this.heat > 0 && this.heat >= this.field) {
            triggerMeltdown(level);
        }

        this.color = calculateColor();
        if (this.heat > 0) {
            irradiateEntities(level);
        }

        this.heat = 0;
        if (this.lastTickValid && this.field > 0) {
            this.field -= 1;
        }
        setChangedAndSync(level.getGameTime() % 10L == 0L || this.meltdownTick);
    }

    private boolean isReady() {
        return this.lastTickValid
                && coreMultiplier() > 0
                && this.color != 0
                && fuelEfficiency(this.tanks[0].type()) > 0.0F
                && fuelEfficiency(this.tanks[1].type()) > 0.0F;
    }

    private int coreMultiplier() {
        ItemStack stack = this.items.get(CORE_SLOT);
        return stack.getItem() instanceof AmsCoreItem core ? core.dfcMultiplier() : 0;
    }

    private int calculateColor() {
        ItemStack left = this.items.get(CATALYST_LEFT_SLOT);
        ItemStack right = this.items.get(CATALYST_RIGHT_SLOT);
        if (!AmsCatalystItem.isCatalyst(left) || !AmsCatalystItem.isCatalyst(right)) {
            return 0;
        }
        int c1 = AmsCatalystItem.color(left);
        int c2 = AmsCatalystItem.color(right);
        int red = (((c1 & 0xFF0000) >>> 16) + ((c2 & 0xFF0000) >>> 16)) / 2;
        int green = (((c1 & 0x00FF00) >>> 8) + ((c2 & 0x00FF00) >>> 8)) / 2;
        int blue = ((c1 & 0x0000FF) + (c2 & 0x0000FF)) / 2;
        return (red << 16) | (green << 8) | blue;
    }

    private void triggerMeltdown(Level level) {
        int fill = this.tanks[0].amount() + this.tanks[1].amount();
        int max = this.tanks[0].capacity() + this.tanks[1].capacity();
        int size = Math.max(Math.min(fill * this.heat * 10 / Math.max(1, max), 1000), 50);
        if (level instanceof ServerLevel serverLevel) {
            NukeExplosionManager.scheduleLittleBoy(serverLevel, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D);
            ChunkRadiationData.get(serverLevel).incrementRadiation(this.worldPosition, 100.0D, 50_000.0D);
        } else {
            level.explode(null, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D, Math.max(5.0F, size / 25.0F), Level.ExplosionInteraction.BLOCK);
        }
        this.meltdownTick = true;
    }

    private void irradiateEntities(Level level) {
        double scale = this.meltdownTick ? 5.0D : 3.0D;
        double range = this.meltdownTick ? 50.0D : 10.0D;
        AABB area = new AABB(this.worldPosition).inflate(range);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) <= range * range) {
                entity.hurt(entity.damageSources().magic(), 1000.0F);
                entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 60));
            }
        }
        AABB coreArea = new AABB(this.worldPosition).inflate(scale);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, coreArea)) {
            HbmLivingRadiation data = HbmLivingRadiation.get(entity);
            data.addRadiation(HbmLivingRadiation.MAX_RADIATION);
            HbmLivingRadiation.set(entity, data);
        }
    }

    private boolean surroundingChunksLoaded(Level level) {
        ChunkPos center = new ChunkPos(this.worldPosition);
        return level.hasChunk(center.x, center.z)
                && level.hasChunk(center.x + 1, center.z + 1)
                && level.hasChunk(center.x + 1, center.z - 1)
                && level.hasChunk(center.x - 1, center.z + 1)
                && level.hasChunk(center.x - 1, center.z - 1);
    }

    public static boolean isValidFuel(HbmFluidDefinition fluid) {
        return fluid != null && VALID_FUELS.contains(fluid.name());
    }

    public float fuelEfficiency(HbmFluidDefinition fluid) {
        if (fluid == null) {
            return 0.0F;
        }
        return switch (fluid.name()) {
            case "hydrogen" -> 1.0F;
            case "deuterium" -> 1.5F;
            case "tritium" -> 1.7F;
            case "oxygen" -> 1.2F;
            case "peroxide" -> 1.4F;
            case "xenon" -> 1.5F;
            case "sas3" -> 2.0F;
            case "balefire" -> 2.5F;
            case "amat" -> 2.2F;
            case "aschrab" -> 2.7F;
            default -> 0.0F;
        };
    }

    private static HbmFluidDefinition fluid(String name) {
        return HbmFluids.byName(name).orElse(HbmFluids.none());
    }
}
