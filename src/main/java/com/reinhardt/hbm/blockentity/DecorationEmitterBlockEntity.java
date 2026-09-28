package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.DecorationEmitterBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/** Server state and ray length calculation from 1.7.10 BlockEmitter.TileEntityEmitter. */
public final class DecorationEmitterBlockEntity extends BlockEntity {
    public static final int RANGE = 100;
    public static final int EFFECT_COUNT = 5;

    private int color;
    private int beam;
    private float girth = 0.5F;
    private int effect;
    private volatile RenderSnapshot renderSnapshot;

    public DecorationEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DECO_EMITTER.get(), pos, state);
        this.renderSnapshot = buildRenderSnapshot(0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DecorationEmitterBlockEntity emitter) {
        if (level.isClientSide) {
            emitter.publishRenderSnapshot(level.getGameTime(), false);
        } else if (level.getGameTime() % 20L == 0L) {
            emitter.updateBeam(level, state);
        }
    }

    public int color() {
        return color;
    }

    public int beam() {
        return beam;
    }

    public float girth() {
        return girth;
    }

    public int effect() {
        return effect;
    }

    public void setColor(int color) {
        this.color = color;
        sync();
    }

    public void increaseGirth() {
        // The original only limits the lower bound while narrowing.
        this.girth += 0.125F;
        sync();
    }

    public void decreaseGirth() {
        this.girth = Math.max(0.125F, this.girth - 0.125F);
        sync();
    }

    public void cycleEffect() {
        this.effect = (this.effect + 1) % EFFECT_COUNT;
        sync();
    }

    private void updateBeam(Level level, BlockState state) {
        Direction direction = state.getValue(DecorationEmitterBlock.FACING);
        int previousBeam = this.beam;
        for (int distance = 1; distance <= RANGE; distance++) {
            this.beam = distance;
            BlockPos target = worldPosition.relative(direction, distance);
            BlockState targetState = level.getBlockState(target);
            if (targetState.isFaceSturdy(level, target, direction.getOpposite())) {
                break;
            }
        }
        if (this.beam != previousBeam) {
            sync();
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            publishRenderSnapshot(level.getGameTime(), level.isClientSide);
            if (!level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        } else {
            publishRenderSnapshot(0L, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("color", color);
        tag.putInt("beam", beam);
        tag.putFloat("girth", girth);
        tag.putInt("effect", effect);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        color = tag.getInt("color");
        beam = tag.getInt("beam");
        girth = Math.max(0.125F, tag.contains("girth") ? tag.getFloat("girth") : 0.5F);
        effect = Math.floorMod(tag.getInt("effect"), EFFECT_COUNT);
        publishRenderSnapshot(level == null ? 0L : level.getGameTime(), true);
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

    public RenderSnapshot renderSnapshot() {
        return this.renderSnapshot;
    }

    private void publishRenderSnapshot(long gameTime, boolean rebuildClientSection) {
        RenderSnapshot previous = this.renderSnapshot;
        RenderSnapshot next = buildRenderSnapshot(gameTime);
        this.renderSnapshot = next;
        if (rebuildClientSection && this.level != null && this.level.isClientSide
                && previous != null && !sameSectionCoverage(previous.bounds(), next.bounds())) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private RenderSnapshot buildRenderSnapshot(long gameTime) {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(DecorationEmitterBlock.FACING)
                ? state.getValue(DecorationEmitterBlock.FACING)
                : Direction.NORTH;
        boolean hasFacing = state.hasProperty(DecorationEmitterBlock.FACING);
        AABB bounds = calculateBounds(facing, hasFacing);
        return new RenderSnapshot(this.color, this.beam, this.girth, this.effect,
                facing, hasFacing, gameTime, bounds);
    }

    private AABB calculateBounds(Direction facing, boolean hasFacing) {
        if (this.beam <= 1 || !hasFacing) {
            return new AABB(this.worldPosition);
        }
        double perpendicularRadius = this.girth;
        double axialRadius = 0.0D;
        if (this.effect >= 1 && this.effect <= 3) {
            double tubeSweep = Math.sqrt(2.0D) * this.girth * 0.1D;
            perpendicularRadius = this.girth * 2.0D + tubeSweep;
            axialRadius = tubeSweep;
        } else if (this.effect == 4) {
            perpendicularRadius = this.girth * 5.0D;
            axialRadius = 0.08D;
        }

        double startX = 0.5D + facing.getStepX() * 0.5D;
        double startY = 0.5D + facing.getStepY() * 0.5D;
        double startZ = 0.5D + facing.getStepZ() * 0.5D;
        double length = this.beam - 1.0D;
        double endX = startX + facing.getStepX() * length;
        double endY = startY + facing.getStepY() * length;
        double endZ = startZ + facing.getStepZ() * length;
        double xRadius = facing.getAxis() == Direction.Axis.X ? axialRadius : perpendicularRadius;
        double yRadius = facing.getAxis() == Direction.Axis.Y ? axialRadius : perpendicularRadius;
        double zRadius = facing.getAxis() == Direction.Axis.Z ? axialRadius : perpendicularRadius;
        return new AABB(
                this.worldPosition.getX() + lowerEmittedFloat(Math.min(startX, endX) - xRadius),
                this.worldPosition.getY() + lowerEmittedFloat(Math.min(startY, endY) - yRadius),
                this.worldPosition.getZ() + lowerEmittedFloat(Math.min(startZ, endZ) - zRadius),
                this.worldPosition.getX() + upperEmittedFloat(Math.max(startX, endX) + xRadius),
                this.worldPosition.getY() + upperEmittedFloat(Math.max(startY, endY) + yRadius),
                this.worldPosition.getZ() + upperEmittedFloat(Math.max(startZ, endZ) + zRadius)
        );
    }

    /** Outward image of a proved double envelope through vertex((float)value). */
    private static double lowerEmittedFloat(double value) {
        float rounded = (float) value;
        return rounded <= value ? rounded : Math.nextDown(rounded);
    }

    /** Outward image of a proved double envelope through vertex((float)value). */
    private static double upperEmittedFloat(double value) {
        float rounded = (float) value;
        return rounded >= value ? rounded : Math.nextUp(rounded);
    }

    private static boolean sameSectionCoverage(AABB first, AABB second) {
        return section(first.minX) == section(second.minX)
                && section(inclusiveMaximum(first.minX, first.maxX)) == section(inclusiveMaximum(second.minX, second.maxX))
                && section(first.minY) == section(second.minY)
                && section(inclusiveMaximum(first.minY, first.maxY)) == section(inclusiveMaximum(second.minY, second.maxY))
                && section(first.minZ) == section(second.minZ)
                && section(inclusiveMaximum(first.minZ, first.maxZ)) == section(inclusiveMaximum(second.minZ, second.maxZ));
    }

    private static int section(double coordinate) {
        return SectionPos.blockToSectionCoord(Mth.floor(coordinate));
    }

    private static double inclusiveMaximum(double min, double max) {
        return max > min ? Math.nextDown(max) : max;
    }

    public record RenderSnapshot(int color, int beam, float girth, int effect,
                                 Direction facing, boolean hasFacing,
                                 long gameTime, AABB bounds) {
    }
}
