package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.recipe.FractionTowerRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FractionTowerBlockEntity extends BlockEntity {
    public static final int TANK_COUNT = 3;
    public static final int TANK_CAPACITY = 4_000;
    private static final int PUSH_PER_PORT = 4_000;

    private final HbmFluidTank[] tanks = new HbmFluidTank[TANK_COUNT];
    private HbmFluidDefinition configuredInput = defaultInput();

    public FractionTowerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.FRACTION_TOWER.get(), pos, blockState);
        this.tanks[0] = new HbmFluidTank(defaultInput(), TANK_CAPACITY);
        this.tanks[1] = new HbmFluidTank(defaultLeft(), TANK_CAPACITY);
        this.tanks[2] = new HbmFluidTank(defaultRight(), TANK_CAPACITY);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FractionTowerBlockEntity tower) {
        if (level.isClientSide) {
            return;
        }
        tower.tickServer(level);
    }

    public HbmFluidTank tank(int index) {
        return index >= 0 && index < TANK_COUNT ? this.tanks[index] : this.tanks[0];
    }

    public void setInputType(HbmFluidDefinition definition) {
        if (definition == null || definition.isNone()) {
            return;
        }
        this.configuredInput = definition;
        this.tanks[0].setType(definition);
        setupTanks();
        sync();
    }

    public Component tankLine(int index) {
        HbmFluidTank tank = tank(index);
        return Component.translatable(tank.type().translationKey())
                .append(Component.literal(": " + tank.amount() + "/" + tank.capacity() + "mB"));
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new FractionTowerFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        return portsFor(this.worldPosition);
    }

    public static List<Port> portsFor(BlockPos pos) {
        return List.of(
                new Port(pos.offset(1, 0, 0), Direction.EAST),
                new Port(pos.offset(-1, 0, 0), Direction.WEST),
                new Port(pos.offset(0, 0, 1), Direction.SOUTH),
                new Port(pos.offset(0, 0, -1), Direction.NORTH)
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int index = 0; index < TANK_COUNT; index++) {
            tag.put("Tank" + index, this.tanks[index].save());
        }
        tag.putString("ConfiguredInput", this.configuredInput.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int index = 0; index < TANK_COUNT; index++) {
            this.tanks[index].load(tag.getCompound("Tank" + index));
        }
        this.configuredInput = HbmFluids.byName(tag.getString("ConfiguredInput"))
                .orElseGet(() -> this.tanks[0].type().isNone() ? defaultInput() : this.tanks[0].type());
        if (this.tanks[0].amount() == 0) {
            this.tanks[0].setType(this.configuredInput);
        }
        setupTanks();
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
        transferWithUpperTower(level);
        setupTanks();
        if (level.getGameTime() % 10L == 0L) {
            fractionate();
        }
        pushOutputs(level);
        setChanged();
        if (level.getGameTime() % 10L == 0L) {
            sync();
        }
    }

    private void transferWithUpperTower(Level level) {
        if (!(level.getBlockEntity(this.worldPosition.above(3)) instanceof FractionTowerBlockEntity upper)) {
            return;
        }

        for (int index = 0; index < TANK_COUNT; index++) {
            upper.tanks[index].setType(this.tanks[index].type());
        }
        upper.configuredInput = this.configuredInput;

        int oil = Math.min(this.tanks[0].amount(), upper.tanks[0].capacity() - upper.tanks[0].amount());
        int left = Math.min(upper.tanks[1].amount(), this.tanks[1].capacity() - this.tanks[1].amount());
        int right = Math.min(upper.tanks[2].amount(), this.tanks[2].capacity() - this.tanks[2].amount());

        this.tanks[0].drain(this.tanks[0].type(), oil, false);
        upper.tanks[0].fill(upper.tanks[0].type(), oil, false);

        HbmFluidDefinition leftType = upper.tanks[1].type();
        HbmFluidDefinition rightType = upper.tanks[2].type();
        upper.tanks[1].drain(leftType, left, false);
        upper.tanks[2].drain(rightType, right, false);
        this.tanks[1].fill(this.tanks[1].type(), left, false);
        this.tanks[2].fill(this.tanks[2].type(), right, false);
        upper.sync();
    }

    private void setupTanks() {
        Optional<net.minecraft.world.item.crafting.RecipeHolder<FractionTowerRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            if (this.tanks[0].amount() == 0) {
                this.tanks[0].setType(this.configuredInput);
            }
            if (this.tanks[1].amount() == 0) {
                this.tanks[1].clear();
            }
            if (this.tanks[2].amount() == 0) {
                this.tanks[2].clear();
            }
            return;
        }

        FractionTowerRecipe recipe = holder.get().value();
        this.tanks[1].setType(recipe.output1().fluid());
        this.tanks[2].setType(recipe.output2().fluid());
    }

    private void fractionate() {
        Optional<net.minecraft.world.item.crafting.RecipeHolder<FractionTowerRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            return;
        }

        FractionTowerRecipe recipe = holder.get().value();
        if (this.tanks[0].amount() < recipe.input().amount() || !hasSpace(recipe)) {
            return;
        }

        this.tanks[0].drain(recipe.input().fluid(), recipe.input().amount(), false);
        if (this.tanks[0].amount() == 0) {
            this.tanks[0].setType(this.configuredInput);
        }
        this.tanks[1].fill(recipe.output1().fluid(), recipe.output1().amount(), false);
        this.tanks[2].fill(recipe.output2().fluid(), recipe.output2().amount(), false);
    }

    private boolean hasSpace(FractionTowerRecipe recipe) {
        return this.tanks[1].type() == recipe.output1().fluid()
                && this.tanks[2].type() == recipe.output2().fluid()
                && this.tanks[1].amount() + recipe.output1().amount() <= this.tanks[1].capacity()
                && this.tanks[2].amount() + recipe.output2().amount() <= this.tanks[2].capacity();
    }

    private void pushOutputs(Level level) {
        for (int index = 1; index < TANK_COUNT; index++) {
            HbmFluidTank tank = this.tanks[index];
            if (tank.amount() <= 0 || tank.type().isNone()) {
                continue;
            }
            for (Port port : ports(level)) {
                if (tank.amount() <= 0) {
                    break;
                }
                FluidStack stack = HbmFluids.toNeoStack(tank.type(), Math.min(PUSH_PER_PORT, tank.amount()));
                int accepted = HbmFluidNetworks.fillInto(level, port.connectorPos(), port.face().getOpposite(), stack, this.worldPosition, true);
                if (accepted > 0) {
                    tank.drain(tank.type(), accepted, false);
                    sync();
                }
            }
        }
    }

    private Optional<net.minecraft.world.item.crafting.RecipeHolder<FractionTowerRecipe>> currentRecipe() {
        if (this.level == null || this.configuredInput.isNone()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.FRACTION_TOWER.get())
                .stream()
                .filter(holder -> holder.value().input().fluid() == this.configuredInput)
                .findFirst();
    }

    private boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        if (this.level == null) {
            return false;
        }
        for (Port port : ports(this.level)) {
            if (port.pos().equals(queriedPos) && (side == null || side == port.face())) {
                return true;
            }
        }
        return false;
    }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.invalidateCapabilities(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            for (Port port : ports(this.level)) {
                this.level.invalidateCapabilities(port.pos());
            }
        }
    }

    private static HbmFluidDefinition defaultInput() {
        return HbmFluids.byName("heavyoil").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition defaultLeft() {
        return HbmFluids.byName("bitumen").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition defaultRight() {
        return HbmFluids.byName("smear").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }
    }

    private final class FractionTowerFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private FractionTowerFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
            this.queriedPos = queriedPos;
            this.side = side;
        }

        @Override
        public int getTanks() {
            return TANK_COUNT;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank >= 0 && tank < TANK_COUNT ? tanks[tank].getFluidInTank(0) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank >= 0 && tank < TANK_COUNT ? tanks[tank].capacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            if (tank != 0 || stack.isEmpty()) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            return !fluid.isNone() && fluid == configuredInput;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone() || fluid != configuredInput) {
                return 0;
            }
            int accepted = tanks[0].fill(fluid, resource.getAmount(), action.simulate());
            if (accepted > 0 && action.execute()) {
                setupTanks();
                sync();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            if (fluid.isNone()) {
                return FluidStack.EMPTY;
            }
            for (int index = 1; index < TANK_COUNT; index++) {
                if (tanks[index].type() != fluid) {
                    continue;
                }
                HbmFluidStack drained = tanks[index].drain(fluid, resource.getAmount(), action.simulate());
                if (!drained.isEmpty()) {
                    if (action.execute()) {
                        sync();
                    }
                    return HbmFluids.toNeoStack(fluid, drained.amount());
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || !allowsFluidPort(this.queriedPos, this.side)) {
                return FluidStack.EMPTY;
            }
            for (int index = 1; index < TANK_COUNT; index++) {
                HbmFluidTank tank = tanks[index];
                if (tank.amount() <= 0 || tank.type().isNone()) {
                    continue;
                }
                HbmFluidDefinition fluid = tank.type();
                HbmFluidStack drained = tank.drain(fluid, maxDrain, action.simulate());
                if (!drained.isEmpty()) {
                    if (action.execute()) {
                        sync();
                    }
                    return HbmFluids.toNeoStack(fluid, drained.amount());
                }
            }
            return FluidStack.EMPTY;
        }
    }
}
