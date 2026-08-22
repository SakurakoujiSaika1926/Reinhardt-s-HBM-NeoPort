package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.recipe.CrackingRecipe;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CatalyticCrackerBlockEntity extends BlockEntity {
    public static final int TANK_COUNT = 5;
    public static final int INPUT_TANK = 0;
    public static final int STEAM_TANK = 1;
    public static final int OUTPUT_LEFT_TANK = 2;
    public static final int OUTPUT_RIGHT_TANK = 3;
    public static final int SPENT_STEAM_TANK = 4;

    private static final int INPUT_CAPACITY = 4_000;
    private static final int STEAM_CAPACITY = 8_000;
    private static final int OUTPUT_CAPACITY = 4_000;
    private static final int PUSH_PER_PORT = 4_000;

    private final HbmFluidTank[] tanks = new HbmFluidTank[TANK_COUNT];

    public CatalyticCrackerBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.CATALYTIC_CRACKER.get(), pos, blockState);
        this.tanks[INPUT_TANK] = new HbmFluidTank(bitumen(), INPUT_CAPACITY);
        this.tanks[STEAM_TANK] = new HbmFluidTank(steam(), STEAM_CAPACITY);
        this.tanks[OUTPUT_LEFT_TANK] = new HbmFluidTank(oil(), OUTPUT_CAPACITY);
        this.tanks[OUTPUT_RIGHT_TANK] = new HbmFluidTank(petroleum(), OUTPUT_CAPACITY);
        this.tanks[SPENT_STEAM_TANK] = new HbmFluidTank(spentSteam(), OUTPUT_CAPACITY);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CatalyticCrackerBlockEntity cracker) {
        if (!level.isClientSide) {
            cracker.tickServer(level);
        }
    }

    public HbmFluidTank tank(int index) {
        return index >= 0 && index < this.tanks.length ? this.tanks[index] : this.tanks[0];
    }

    public void setInputType(HbmFluidDefinition definition) {
        if (definition == null || definition.isNone()) {
            return;
        }
        this.tanks[INPUT_TANK].setType(definition);
        setupTanks();
        sync();
    }

    public Component tankLine(int index) {
        HbmFluidTank tank = tank(index);
        String arrow = index < OUTPUT_LEFT_TANK ? "-> " : "<- ";
        return Component.literal(arrow)
                .append(Component.translatable(tank.type().translationKey()))
                .append(Component.literal(": " + tank.amount() + "/" + tank.capacity() + "mB"));
    }

    @Nullable
    public IFluidHandler fluidHandler(BlockPos queriedPos, @Nullable Direction side) {
        if (!allowsFluidPort(queriedPos, side)) {
            return null;
        }
        return new CrackerFluidHandler(queriedPos.immutable(), side);
    }

    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return fluidHandler(this.worldPosition, side);
    }

    public List<Port> ports(LevelAccessor level) {
        return portsFor(this.worldPosition, facing());
    }

    public static List<Port> portsFor(BlockPos pos, Direction facing) {
        Direction dir = horizontal(facing);
        Direction rot = LegacyMachineGeometry.forgeRotateUp(dir);
        List<Port> ports = new ArrayList<>(8);
        addConnector(ports, pos, dir, 4, rot, 1, dir);
        addConnector(ports, pos, dir, 4, rot, -2, dir);
        addConnector(ports, pos, dir, -4, rot, 1, dir.getOpposite());
        addConnector(ports, pos, dir, -4, rot, -2, dir.getOpposite());
        addConnector(ports, pos, dir, 2, rot, 3, rot);
        addConnector(ports, pos, dir, 2, rot, -4, rot);
        addConnector(ports, pos, dir, -2, rot, 3, rot.getOpposite());
        addConnector(ports, pos, dir, -2, rot, -4, rot.getOpposite());
        return List.copyOf(ports);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int index = 0; index < this.tanks.length; index++) {
            tag.put("Tank" + index, this.tanks[index].save());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int index = 0; index < this.tanks.length; index++) {
            this.tanks[index].load(tag.getCompound("Tank" + index));
        }
        if (this.tanks[INPUT_TANK].amount() == 0 && this.tanks[INPUT_TANK].type().isNone()) {
            this.tanks[INPUT_TANK].setType(bitumen());
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
        setupTanks();
        if (level.getGameTime() % 5L == 0L) {
            crack();
        }
        if (level.getGameTime() % 10L == 0L) {
            pushOutputs(level);
            sync();
        }
        setChanged();
    }

    private void setupTanks() {
        Optional<net.minecraft.world.item.crafting.RecipeHolder<CrackingRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            if (this.tanks[INPUT_TANK].amount() == 0) {
                this.tanks[INPUT_TANK].clear();
            }
            if (this.tanks[STEAM_TANK].amount() == 0) {
                this.tanks[STEAM_TANK].clear();
            }
            for (int index = OUTPUT_LEFT_TANK; index < TANK_COUNT; index++) {
                if (this.tanks[index].amount() == 0) {
                    this.tanks[index].clear();
                }
            }
            return;
        }

        CrackingRecipe recipe = holder.get().value();
        this.tanks[STEAM_TANK].setType(steam());
        this.tanks[OUTPUT_LEFT_TANK].setType(recipe.output1().fluid());
        this.tanks[OUTPUT_RIGHT_TANK].setType(recipe.output2().fluid());
        this.tanks[SPENT_STEAM_TANK].setType(spentSteam());
    }

    private void crack() {
        Optional<net.minecraft.world.item.crafting.RecipeHolder<CrackingRecipe>> holder = currentRecipe();
        if (holder.isEmpty()) {
            return;
        }

        CrackingRecipe recipe = holder.get().value();
        for (int operation = 0; operation < 2; operation++) {
            if (this.tanks[INPUT_TANK].amount() < recipe.input().amount()
                    || this.tanks[STEAM_TANK].amount() < CrackingRecipe.STEAM_PER_OPERATION
                    || !hasSpace(recipe)) {
                return;
            }

            this.tanks[INPUT_TANK].drain(recipe.input().fluid(), recipe.input().amount(), false);
            this.tanks[STEAM_TANK].drain(steam(), CrackingRecipe.STEAM_PER_OPERATION, false);
            fillOutput(OUTPUT_LEFT_TANK, recipe.output1());
            fillOutput(OUTPUT_RIGHT_TANK, recipe.output2());
            this.tanks[SPENT_STEAM_TANK].fill(spentSteam(), CrackingRecipe.SPENT_STEAM_PER_OPERATION, false);
        }
    }

    private boolean hasSpace(CrackingRecipe recipe) {
        return hasSpace(OUTPUT_LEFT_TANK, recipe.output1())
                && hasSpace(OUTPUT_RIGHT_TANK, recipe.output2())
                && this.tanks[SPENT_STEAM_TANK].amount() + CrackingRecipe.SPENT_STEAM_PER_OPERATION <= this.tanks[SPENT_STEAM_TANK].capacity();
    }

    private boolean hasSpace(int tankIndex, CrackingRecipe.FluidOutput output) {
        if (output.isEmpty()) {
            return true;
        }
        HbmFluidTank tank = this.tanks[tankIndex];
        return tank.type() == output.fluid() && tank.amount() + output.amount() <= tank.capacity();
    }

    private void fillOutput(int tankIndex, CrackingRecipe.FluidOutput output) {
        if (!output.isEmpty()) {
            this.tanks[tankIndex].fill(output.fluid(), output.amount(), false);
        }
    }

    private void pushOutputs(Level level) {
        for (int index = OUTPUT_LEFT_TANK; index < TANK_COUNT; index++) {
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
                }
            }
        }
    }

    private Optional<net.minecraft.world.item.crafting.RecipeHolder<CrackingRecipe>> currentRecipe() {
        if (this.level == null || this.tanks[INPUT_TANK].type().isNone()) {
            return Optional.empty();
        }
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CRACKING.get())
                .stream()
                .filter(holder -> holder.value().input().fluid() == this.tanks[INPUT_TANK].type())
                .findFirst();
    }

    private boolean acceptsInputFluid(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone() || this.level == null) {
            return false;
        }
        return this.level.getRecipeManager()
                .getAllRecipesFor(HbmRecipeTypes.CRACKING.get())
                .stream()
                .anyMatch(holder -> holder.value().input().fluid() == fluid);
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
                this.level.invalidateCapabilities(port.connectorPos());
            }
        }
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
    }

    private static void addConnector(List<Port> ports, BlockPos core, Direction first, int firstDistance, Direction second, int secondDistance, Direction face) {
        BlockPos connector = core.offset(
                first.getStepX() * firstDistance + second.getStepX() * secondDistance,
                0,
                first.getStepZ() * firstDistance + second.getStepZ() * secondDistance
        );
        ports.add(Port.fromConnector(connector, face));
    }

    private static Direction horizontal(Direction direction) {
        return direction.getAxis().isHorizontal() ? direction : Direction.NORTH;
    }

    private static HbmFluidDefinition bitumen() {
        return HbmFluids.byName("bitumen").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition steam() {
        return HbmFluids.byName("steam").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition spentSteam() {
        return HbmFluids.byName("spentsteam").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition oil() {
        return HbmFluids.byName("oil").orElse(HbmFluids.none());
    }

    private static HbmFluidDefinition petroleum() {
        return HbmFluids.byName("petroleum").orElse(HbmFluids.none());
    }

    public record Port(BlockPos pos, Direction face) {
        private static Port fromConnector(BlockPos connectorPos, Direction face) {
            return new Port(connectorPos.relative(face.getOpposite()).immutable(), face);
        }

        public BlockPos connectorPos() {
            return this.pos.relative(this.face).immutable();
        }
    }

    private final class CrackerFluidHandler implements IFluidHandler {
        private final BlockPos queriedPos;
        @Nullable
        private final Direction side;

        private CrackerFluidHandler(BlockPos queriedPos, @Nullable Direction side) {
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
            if (stack.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return false;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
            if (tank == INPUT_TANK) {
                return acceptsInputFluid(fluid);
            }
            return tank == STEAM_TANK && fluid == steam();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !allowsFluidPort(this.queriedPos, this.side)) {
                return 0;
            }
            HbmFluidDefinition fluid = HbmFluids.fromNeoFluid(resource.getFluid()).orElse(HbmFluids.none());
            HbmFluidTank target;
            if (fluid == steam()) {
                target = tanks[STEAM_TANK];
            } else if (acceptsInputFluid(fluid)) {
                target = tanks[INPUT_TANK];
            } else {
                return 0;
            }
            int accepted = target.fill(fluid, resource.getAmount(), action.simulate());
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
            for (int index = OUTPUT_LEFT_TANK; index < TANK_COUNT; index++) {
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
            for (int index = OUTPUT_LEFT_TANK; index < TANK_COUNT; index++) {
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
