package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.DfcComponentBlock;
import com.reinhardt.hbm.client.render.DfcComponentClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;
import java.util.Arrays;

public abstract class DfcInventoryBlockEntity extends BlockEntity implements MachineInventory, WorldlyContainer {
    protected final NonNullList<ItemStack> items;
    private final int[] automationSlots;
    private int renderBeamLength;
    private volatile BeamRenderSnapshot beamRenderSnapshot;

    protected DfcInventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, int slots) {
        super(type, pos, blockState);
        this.beamRenderSnapshot = new BeamRenderSnapshot(
                blockState, facing(blockState), 0, 0, null,
                new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()), -1L);
        this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
        this.automationSlots = new int[slots];
        for (int slot = 0; slot < slots; slot++) {
            this.automationSlots[slot] = slot;
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
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
        return validSlot(slot) ? this.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!validSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChangedAndSync(false);
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
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize(stack)) {
            stack.setCount(this.getMaxStackSize(stack));
        }
        setChangedAndSync(false);
    }

    @Override
    public boolean stillValid(Player player) {
        return MachineDummyMenuAccess.stillValid(this, player);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return Arrays.copyOf(this.automationSlots, this.automationSlots.length);
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
    public void clearContent() {
        for (int slot = 0; slot < this.items.size(); slot++) {
            this.items.set(slot, ItemStack.EMPTY);
        }
        setChangedAndSync(false);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.items.size(); slot++) {
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, this.items, registries);
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

    protected void setChangedAndSync(boolean sync) {
        setChanged();
        if (sync && this.level != null) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public final BeamRenderSnapshot beamRenderSnapshot() {
        return this.beamRenderSnapshot;
    }

    protected final void refreshClientBeamSnapshotRevision() {
        if (this.level != null && this.level.isClientSide && FMLEnvironment.dist.isClient()
                && this.beamRenderSnapshot.modelRevision() != DfcComponentClientHooks.modelRevision()) {
            publishBeamRenderSnapshot(this.renderBeamLength, true);
        }
    }

    /** Client reload hook: publishes the new immutable model generation immediately. */
    public final void refreshClientBeamSnapshotAfterModelReload() {
        if (this.level != null && this.level.isClientSide) {
            publishBeamRenderSnapshot(this.renderBeamLength, true);
        }
    }

    /**
     * Publishes the complete beam state through one immutable volatile snapshot. Render code and
     * render-bounds code must both capture this object once instead of reading a mutable machine
     * field independently.
     */
    protected final void publishBeamRenderSnapshot(int beamLength, boolean rebuildClientSection) {
        this.renderBeamLength = Math.max(0, beamLength);
        if (this.level == null || !this.level.isClientSide) {
            return;
        }
        BeamRenderSnapshot previous = this.beamRenderSnapshot;
        BeamRenderSnapshot next = buildBeamRenderSnapshot(this.renderBeamLength);
        if (previous.equals(next)) {
            return;
        }
        this.beamRenderSnapshot = next;
        if (rebuildClientSection && !sameSectionCoverage(previous.bounds(), next.bounds())) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide) {
            publishBeamRenderSnapshot(this.renderBeamLength, true);
        }
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        if (this.beamRenderSnapshot != null && this.level != null && this.level.isClientSide) {
            publishBeamRenderSnapshot(this.renderBeamLength, true);
        }
    }

    private BeamRenderSnapshot buildBeamRenderSnapshot(int length) {
        BlockState state = getBlockState();
        Direction direction = facing(state);
        // The client hook walks the actual baked quad float positions and applies the
        // renderer's exact PoseStack.  This is intentionally not a hand-maintained OBJ
        // envelope: resource reloads/model changes cannot silently invalidate it.
        DfcComponentClientHooks.GeometryResult geometry = FMLEnvironment.dist.isClient()
                ? DfcComponentClientHooks.staticModelGeometry(this, state, direction)
                : null;
        AABB bounds = geometry != null ? geometry.bounds()
                : new AABB(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(),
                this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ());
        Object geometryToken = geometry != null ? geometry.geometryToken() : null;
        long modelRevision = geometry != null ? geometry.revision() : -1L;
        double thickness = beamThickness();
        if (length > 0 && thickness > 0.0D) {
            bounds = bounds.minmax(beamBounds(direction, length, thickness));
        }
        return new BeamRenderSnapshot(state, direction, length, beamColor(), geometryToken,
                bounds, modelRevision);
    }

    private int beamColor() {
        if (this instanceof DfcEmitterBlockEntity) {
            return 0xE0FF6600;
        }
        if (this instanceof DfcInjectorBlockEntity injector) {
            int color = injector.tank(0).amount() > 0
                    ? injector.tank(0).type().color()
                    : injector.tank(1).type().color();
            return 0xC0000000 | color;
        }
        if (this instanceof DfcStabilizerBlockEntity) {
            return 0xE0FFA200;
        }
        return 0;
    }

    private double beamThickness() {
        if (this instanceof DfcEmitterBlockEntity) {
            return 0.07D;
        }
        if (this instanceof DfcInjectorBlockEntity) {
            return 0.035D;
        }
        if (this instanceof DfcStabilizerBlockEntity) {
            return 0.04D;
        }
        return 0.0D;
    }

    private AABB beamBounds(Direction direction, int length, double thickness) {
        Vec3 start = new Vec3(0.5D, 0.5D, 0.5D);
        Vec3 end = start.add(direction.getStepX() * length, direction.getStepY() * length,
                direction.getStepZ() * length);
        double[] bounds = {
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        includeBeamQuad(bounds, start, end, thickness);
        includeBeamQuad(bounds, start.add(0.0D, thickness, 0.0D),
                end.add(0.0D, thickness, 0.0D), thickness * 0.5D);
        return new AABB(
                this.worldPosition.getX() + bounds[0],
                this.worldPosition.getY() + bounds[1],
                this.worldPosition.getZ() + bounds[2],
                this.worldPosition.getX() + bounds[3],
                this.worldPosition.getY() + bounds[4],
                this.worldPosition.getZ() + bounds[5]);
    }

    private static void includeBeamQuad(double[] bounds, Vec3 start, Vec3 end, double thickness) {
        Vec3 direction = end.subtract(start);
        Vec3 axisX = new Vec3(direction.z, 0.0D, -direction.x);
        if (axisX.lengthSqr() < 1.0E-5D) {
            axisX = new Vec3(1.0D, 0.0D, 0.0D);
        }
        axisX = axisX.normalize().scale(thickness);
        Vec3 axisY = new Vec3(0.0D, thickness, 0.0D);
        includeFloatVertex(bounds, start.add(axisX).add(axisY));
        includeFloatVertex(bounds, start.subtract(axisX).add(axisY));
        includeFloatVertex(bounds, end.subtract(axisX).subtract(axisY));
        includeFloatVertex(bounds, end.add(axisX).subtract(axisY));
    }

    private static void includeFloatVertex(double[] bounds, Vec3 point) {
        double x = (float) point.x;
        double y = (float) point.y;
        double z = (float) point.z;
        bounds[0] = Math.min(bounds[0], x);
        bounds[1] = Math.min(bounds[1], y);
        bounds[2] = Math.min(bounds[2], z);
        bounds[3] = Math.max(bounds[3], x);
        bounds[4] = Math.max(bounds[4], y);
        bounds[5] = Math.max(bounds[5], z);
    }

    private static Direction facing(BlockState state) {
        return state.hasProperty(DfcComponentBlock.FACING)
                ? state.getValue(DfcComponentBlock.FACING)
                : Direction.NORTH;
    }

    private static boolean sameSectionCoverage(AABB first, AABB second) {
        return sectionMin(first.minX) == sectionMin(second.minX)
                && sectionMin(first.minY) == sectionMin(second.minY)
                && sectionMin(first.minZ) == sectionMin(second.minZ)
                && sectionMax(first.maxX) == sectionMax(second.maxX)
                && sectionMax(first.maxY) == sectionMax(second.maxY)
                && sectionMax(first.maxZ) == sectionMax(second.maxZ);
    }

    private static int sectionMin(double value) {
        return net.minecraft.util.Mth.floor(value) >> 4;
    }

    private static int sectionMax(double value) {
        return net.minecraft.util.Mth.floor(Math.nextDown(value)) >> 4;
    }

    protected boolean validSlot(int slot) {
        return slot >= 0 && slot < this.items.size();
    }

    public record BeamRenderSnapshot(
            BlockState blockState, Direction facing, int length, int color,
            Object geometryToken, AABB bounds, long modelRevision) {
    }
}
