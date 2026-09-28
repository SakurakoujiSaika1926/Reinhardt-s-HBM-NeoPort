package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LandmineBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;

public final class LandmineBlockEntity extends BlockEntity {
    private boolean primed;
    private boolean waitingForPlayer;
    private volatile RenderSnapshot renderSnapshot;
    private boolean clientRenderSnapshotDirty = true;
    private long clientRenderEnvironmentVersion;

    public LandmineBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LANDMINE.get(), pos, state);
        this.renderSnapshot = new RenderSnapshot(
                state, landmineType(state), ApSurface.GRASS, 0L);
    }

    /**
     * Samples the AP appearance once per client tick instead of once per rendered frame.
     * Heightmap updates can originate anywhere in the column and biome mutation has no
     * complete client notification, so the tick comparison is the event-complete path:
     * a received world change is reflected no later than the following client tick.
     */
    public static void tickClient(net.minecraft.world.level.Level level, BlockPos pos,
                                  BlockState state, LandmineBlockEntity mine) {
        if (!level.isClientSide) {
            return;
        }
        LandmineBlock.LandmineType type = landmineType(state);
        ApSurface surface = type == LandmineBlock.LandmineType.AP
                ? apSurface(level, pos)
                : ApSurface.GRASS;
        RenderSnapshot previous = mine.renderSnapshot;
        if (mine.clientRenderSnapshotDirty || previous.blockState() != state
                || previous.type() != type || previous.apSurface() != surface) {
            mine.publishClientRenderSnapshot(state, type, surface);
        }
    }

    public static void tick(ServerLevel level, BlockPos pos, BlockState state, LandmineBlockEntity mine) {
        if (!(state.getBlock() instanceof LandmineBlock landmine) || !level.getBlockState(pos.above()).isAir()) {
            return;
        }

        double range = landmine.type().range();
        double height = landmine.type().height();
        if (mine.waitingForPlayer) {
            range = 25.0D;
            height = 25.0D;
        } else if (!mine.primed) {
            range *= 2.0D;
            height *= 2.0D;
        }

        AABB trigger = new AABB(
                pos.getX() - range,
                pos.getY() - height,
                pos.getZ() - range,
                pos.getX() + range + 1.0D,
                pos.getY() + height,
                pos.getZ() + range + 1.0D
        );
        List<Entity> entities = level.getEntities((Entity) null, trigger,
                entity -> !(entity instanceof WaterAnimal) && !(entity instanceof AmbientCreature));

        for (Entity entity : entities) {
            if (mine.waitingForPlayer) {
                if (entity instanceof Player) {
                    mine.waitingForPlayer = false;
                    mine.setChanged();
                    return;
                }
            } else if (entity instanceof LivingEntity) {
                if (mine.primed) {
                    landmine.detonate(level, pos);
                }
                return;
            }
        }

        if (!mine.primed && !mine.waitingForPlayer) {
            level.playSound(null, pos, HbmSoundEvents.WEAPON_LANDMINE_ARM.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
            mine.primed = true;
            mine.setChanged();
        }
    }

    public void setWaitingForPlayer(boolean waitingForPlayer) {
        this.waitingForPlayer = waitingForPlayer;
        setChanged();
    }

    public RenderSnapshot renderSnapshot() {
        return renderSnapshot;
    }

    /** Immediate path for direct neighbour notifications; the client tick covers the full column. */
    public void markClientEnvironmentDirty() {
        if (level != null && level.isClientSide) {
            clientRenderSnapshotDirty = true;
            refreshClientRenderSnapshot();
        }
    }

    private void refreshClientRenderSnapshot() {
        if (!clientRenderSnapshotDirty || level == null || !level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        LandmineBlock.LandmineType type = landmineType(state);
        ApSurface surface = type == LandmineBlock.LandmineType.AP
                ? apSurface(level, worldPosition)
                : ApSurface.GRASS;
        publishClientRenderSnapshot(state, type, surface);
    }

    private void publishClientRenderSnapshot(BlockState state, LandmineBlock.LandmineType type,
                                             ApSurface surface) {
        renderSnapshot = new RenderSnapshot(
                state, type, surface, ++clientRenderEnvironmentVersion);
        clientRenderSnapshotDirty = false;
    }

    private static LandmineBlock.LandmineType landmineType(BlockState state) {
        return state.getBlock() instanceof LandmineBlock landmine
                ? landmine.type()
                : LandmineBlock.LandmineType.AP;
    }

    /** Exact AP grass/desert/snow/stone selection order from the existing renderer. */
    private static ApSurface apSurface(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) > pos.getY() + 2) {
            return ApSurface.STONE;
        }
        Biome biome = level.getBiome(pos).value();
        if (biome.coldEnoughToSnow(pos)) {
            return ApSurface.SNOW;
        }
        if (biome.getBaseTemperature() >= 1.5F
                && biome.getModifiedClimateSettings().downfall() <= 0.1F) {
            return ApSurface.DESERT;
        }
        return ApSurface.GRASS;
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        clientRenderSnapshotDirty = true;
        refreshClientRenderSnapshot();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            clientRenderSnapshotDirty = true;
            refreshClientRenderSnapshot();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("primed", this.primed);
        tag.putBoolean("waiting", this.waitingForPlayer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.primed = tag.getBoolean("primed");
        this.waitingForPlayer = tag.getBoolean("waiting");
    }

    public enum ApSurface {
        GRASS,
        DESERT,
        SNOW,
        STONE
    }

    public record RenderSnapshot(BlockState blockState, LandmineBlock.LandmineType type,
                                 ApSurface apSurface, long environmentVersion) {
        public RenderSnapshot {
            Objects.requireNonNull(blockState);
            Objects.requireNonNull(type);
            Objects.requireNonNull(apSurface);
        }
    }
}
