package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FloodlightBlock;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public final class FloodlightBlockEntity extends BlockEntity implements PowerEndpoint {
    public static final int LIGHT_COUNT = 15;
    public static final int MAX_POWER = 5_000;
    public static final int POWER_PER_TICK = 100;
    public static final int MAX_LENGTH = 64;
    private static final Set<FloodlightBlockEntity> CLIENT_INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static volatile Consumer<FloodlightBlockEntity> clientRenderSnapshotUpdater = ignored -> { };
    private static volatile long clientRenderSnapshotRevision;

    private final BlockPos[] lightPositions = new BlockPos[LIGHT_COUNT];
    private int power;
    private int delay;
    private float rotation;
    private boolean on;
    private volatile RenderSnapshot renderSnapshot;
    private boolean clientRenderSnapshotDirty = true;

    public FloodlightBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.FLOODLIGHT.get(), pos, state);
        this.renderSnapshot = new RenderSnapshot(
                renderMeta(state), 0.0F, false, null, emptyBounds(pos), -1L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FloodlightBlockEntity light) {
        if (level.isClientSide) {
            RenderSnapshot snapshot = light.renderSnapshot;
            if (snapshot.meta() != renderMeta(state)
                    || Float.floatToIntBits(snapshot.rotation()) != Float.floatToIntBits(light.rotation)
                    || snapshot.on() != light.on
                    || snapshot.revision() != clientRenderSnapshotRevision) {
                light.clientRenderSnapshotDirty = true;
            }
            light.refreshClientRenderSnapshot();
            return;
        }
        PowerNetworkManager.tickFromEndpoint(level, light);
        if (light.delay > 0) {
            light.delay--;
            return;
        }
        if (light.power >= POWER_PER_TICK) {
            light.power -= POWER_PER_TICK;
            if (!light.on) {
                light.on = true;
                light.castLights();
                light.sync();
            } else if (level.getGameTime() % 5L == 0L) {
                light.castLight((int) Math.abs((level.getGameTime() / 5L) % LIGHT_COUNT));
            }
        } else if (light.on) {
            light.on = false;
            light.delay = 60;
            light.destroyLights();
            light.sync();
        }
        light.setChanged();
    }

    public static void setAngle(Level level, BlockPos pos, BlockState state, LivingEntity player, boolean updateState) {
        if (level.getBlockEntity(pos) instanceof FloodlightBlockEntity light) {
            light.setAngle(player, updateState);
        }
    }

    public void setAngle(LivingEntity player, boolean updateState) {
        int quadrant = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        BlockState state = getBlockState();
        Direction facing = state.getValue(FloodlightBlock.FACING);
        float pitch = player.getXRot();
        boolean flipped = state.getValue(FloodlightBlock.FLIPPED);
        if (facing == Direction.DOWN || facing == Direction.UP) {
            if (updateState && (quadrant == 0 || quadrant == 2)) {
                flipped = true;
                level.setBlock(worldPosition, state.setValue(FloodlightBlock.FLIPPED, true), Block.UPDATE_ALL);
            }
            if (facing == Direction.UP && (quadrant == 0 || quadrant == 1)) {
                pitch = 180.0F - pitch;
            }
            if (facing == Direction.DOWN && (quadrant == 0 || quadrant == 3)) {
                pitch = 180.0F - pitch;
            }
        }
        this.rotation = -Math.round(pitch / 5.0F) * 5.0F;
        setChanged();
        if (level != null && !level.isClientSide) {
            destroyLights();
            sync();
        } else {
            clientRenderSnapshotDirty = true;
            refreshClientRenderSnapshot();
        }
    }

    private void castLights() {
        for (int index = 0; index < LIGHT_COUNT; index++) {
            castLight(index);
        }
    }

    private void castLight(int index) {
        BlockPos oldPos = lightPositions[index];
        BlockPos newPos = getRayEndpoint(index);
        lightPositions[index] = null;
        if (oldPos != null && (newPos == null || !newPos.equals(oldPos))
                && level.getBlockEntity(oldPos) instanceof FloodlightBeamBlockEntity beam
                && beam.isSource(this, index)) {
            level.removeBlock(oldPos, false);
        }
        if (newPos == null) {
            return;
        }
        BlockState target = level.getBlockState(newPos);
        if (target.isAir()) {
            level.setBlock(newPos, HbmBlocks.FLOODLIGHT_BEAM.get().defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(newPos) instanceof FloodlightBeamBlockEntity beam) {
                beam.setSource(worldPosition, index);
                lightPositions[index] = newPos;
            }
        } else if (target.is(HbmBlocks.FLOODLIGHT_BEAM.get())
                && level.getBlockEntity(newPos) instanceof FloodlightBeamBlockEntity beam
                && beam.isSource(this, index)) {
            lightPositions[index] = newPos;
        }
    }

    private BlockPos getRayEndpoint(int index) {
        if (index < 0 || index >= LIGHT_COUNT || level == null) {
            return null;
        }
        Direction facing = getBlockState().getValue(FloodlightBlock.FACING);
        int meta = facing.ordinal() + (getBlockState().getValue(FloodlightBlock.FLIPPED) ? 6 : 0);
        float currentRotation = rotation;
        if (meta == 1 || meta == 7 || meta == 6) {
            currentRotation = 180.0F - currentRotation;
        }
        float[] variation = variation(index);
        Vec3 direction = new Vec3(1.0D, 0.0D, 0.0D).zRot((float) Math.toRadians(currentRotation) + variation[0]);
        if (meta == 6 || meta == 7 || meta == 2) {
            direction = direction.yRot((float) (Math.PI / 2.0D));
        }
        if (meta == 3) {
            direction = direction.yRot((float) -(Math.PI / 2.0D));
        }
        if (meta == 4) {
            direction = direction.yRot((float) Math.PI);
        }
        direction = direction.yRot(variation[1]);
        for (int distance = 1; distance < MAX_LENGTH; distance++) {
            int x = (int) Math.floor(worldPosition.getX() + 0.5D + direction.x * distance);
            int y = (int) Math.floor(worldPosition.getY() + 0.5D + direction.y * distance);
            int z = (int) Math.floor(worldPosition.getZ() + 0.5D + direction.z * distance);
            BlockPos current = new BlockPos(x, y, z);
            // 1.7.10 compared the old 0..255 light-opacity value with 127.
            // Modern getLightBlock() is a 0..15 value, so the equivalent
            // opaque-stop test is 15; using 127 made every ray miss and no
            // floodlight beam light source was ever created.
            if (current.equals(worldPosition) || level.getBlockState(current).getLightBlock(level, current) < 15) {
                continue;
            }
            if (distance > 1) {
                return new BlockPos(
                        (int) Math.floor(worldPosition.getX() + 0.5D + direction.x * (distance - 1)),
                        (int) Math.floor(worldPosition.getY() + 0.5D + direction.y * (distance - 1)),
                        (int) Math.floor(worldPosition.getZ() + 0.5D + direction.z * (distance - 1))
                );
            }
        }
        return null;
    }

    private static float[] variation(int index) {
        return new float[]{
                (float) Math.toRadians(((index / 3) - 2) * 7.5F),
                (float) Math.toRadians(((index % 3) - 1) * 15.0F)
        };
    }

    public void destroyLights() {
        if (level == null) {
            return;
        }
        for (int index = 0; index < lightPositions.length; index++) {
            BlockPos lightPos = lightPositions[index];
            if (lightPos != null && level.getBlockState(lightPos).is(HbmBlocks.FLOODLIGHT_BEAM.get())
                    && level.getBlockEntity(lightPos) instanceof FloodlightBeamBlockEntity beam
                    && beam.isSource(this, index)) {
                level.removeBlock(lightPos, false);
            }
            lightPositions[index] = null;
        }
    }

    public boolean isOn() { return on; }
    public float rotation() { return rotation; }
    public RenderSnapshot renderSnapshot() { return renderSnapshot; }

    /** Installed by the client renderer without introducing client classes on a dedicated server. */
    public static void setClientRenderSnapshotUpdater(Consumer<FloodlightBlockEntity> updater) {
        clientRenderSnapshotUpdater = Objects.requireNonNull(updater);
        markLoadedClientSnapshotsDirty(false);
    }

    /** Publishes a new immutable post-bake generation to every loaded client instance. */
    public static void invalidateClientRenderSnapshotsAfterBake() {
        clientRenderSnapshotRevision++;
        markLoadedClientSnapshotsDirty(true);
    }

    /** Removes old baked-quad generations from block entities during client logout. */
    public static void clearClientRenderSnapshotsAfterLogout() {
        clientRenderSnapshotRevision++;
        synchronized (CLIENT_INSTANCES) {
            for (FloodlightBlockEntity light : CLIENT_INSTANCES) {
                light.renderSnapshot = new RenderSnapshot(
                        renderMeta(light.getBlockState()), light.rotation, light.on,
                        null, emptyBounds(light.worldPosition), -1L);
                light.clientRenderSnapshotDirty = true;
            }
            CLIENT_INSTANCES.clear();
        }
    }

    private static void markLoadedClientSnapshotsDirty(boolean refreshNow) {
        ArrayList<FloodlightBlockEntity> loaded;
        synchronized (CLIENT_INSTANCES) {
            loaded = new ArrayList<>(CLIENT_INSTANCES);
        }
        for (FloodlightBlockEntity light : loaded) {
            light.clientRenderSnapshotDirty = true;
            if (refreshNow) {
                light.refreshClientRenderSnapshot();
            }
        }
    }

    public static long clientRenderSnapshotRevision() {
        return clientRenderSnapshotRevision;
    }

    public void refreshClientRenderSnapshot() {
        if (clientRenderSnapshotDirty && level != null && level.isClientSide) {
            clientRenderSnapshotUpdater.accept(this);
        }
    }

    public void publishClientRenderSnapshot(RenderSnapshot snapshot) {
        if (level == null || !level.isClientSide) {
            return;
        }
        RenderSnapshot previous = renderSnapshot;
        renderSnapshot = Objects.requireNonNull(snapshot);
        clientRenderSnapshotDirty = false;
        if (!sameSectionCoverage(previous.bounds(), snapshot.bounds())) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        clientRenderSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    public boolean isLightPosition(BlockPos pos, int index) {
        return index >= 0 && index < LIGHT_COUNT && pos.equals(lightPositions[index]);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override public BlockPos getPowerPos() { return worldPosition; }
    @Override public long getAvailableOutput() { return 0L; }
    @Override public long getRequestedInput() { return Math.max(0L, MAX_POWER - power); }
    @Override public void applyPower(long usedOutput, long receivedInput) {
        int accepted = (int) Math.min((long) MAX_POWER, Math.max(0L, receivedInput));
        power = Math.min(MAX_POWER, power + accepted);
    }
    @Override public PowerEndpoint.ConnectionPriority getPowerPriority() { return PowerEndpoint.ConnectionPriority.NORMAL; }
    @Override public java.util.List<BlockPos> getPowerConnectorPositions(net.minecraft.world.level.LevelAccessor level) {
        Direction facing = getBlockState().getValue(FloodlightBlock.FACING);
        return java.util.List.of(worldPosition.relative(facing.getOpposite()).immutable());
    }
    @Override public Component getPowerStatus() { return Component.literal(power + " / " + MAX_POWER + " HE"); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Power", power);
        tag.putInt("Delay", delay);
        tag.putFloat("Rotation", rotation);
        tag.putBoolean("On", on);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = Math.max(0, Math.min(MAX_POWER, tag.getInt("Power")));
        delay = Math.max(0, tag.getInt("Delay"));
        rotation = tag.getFloat("Rotation");
        on = tag.getBoolean("On");
        clientRenderSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            synchronized (CLIENT_INSTANCES) {
                CLIENT_INSTANCES.add(this);
            }
            clientRenderSnapshotDirty = true;
            refreshClientRenderSnapshot();
        }
    }

    @Override
    public void setRemoved() {
        synchronized (CLIENT_INSTANCES) {
            CLIENT_INSTANCES.remove(this);
        }
        super.setRemoved();
    }

    private static int renderMeta(BlockState state) {
        return state.getValue(FloodlightBlock.FACING).ordinal()
                + (state.getValue(FloodlightBlock.FLIPPED) ? 6 : 0);
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

    private static AABB emptyBounds(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX(), pos.getY(), pos.getZ());
    }

    public record RenderSnapshot(int meta, float rotation, boolean on, Object geometryToken,
                                 AABB bounds, long revision) {
        public RenderSnapshot {
            Objects.requireNonNull(bounds);
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { CompoundTag tag = super.getUpdateTag(registries); saveAdditional(tag, registries); return tag; }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
