package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.PowerPylonBlockEntity;
import com.reinhardt.hbm.blockentity.PipeAnchorBlockEntity;
import com.reinhardt.hbm.blockentity.ClientRenderNeighborSnapshot;
import com.reinhardt.hbm.blockentity.LauncherBlockEntity;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Releases client-only GPU caches when leaving a world. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class ClientRenderCacheEvents {
    private static final Map<ClientLevel, Set<ChunkPos>> PENDING_NEIGHBOR_SNAPSHOT_CHUNKS = new WeakHashMap<>();

    private ClientRenderCacheEvents() {
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MachineModelRenderer.invalidateModelCache();
        PowerPylonClientGeometry.clearGeometryGenerationAfterLogout();
        DfcComponentClientHooks.clearGeometryGenerationAfterLogout();
        PistonInserterBlockEntityRenderer.clearGeometryGenerationAfterLogout();
        DecoLootBlockEntityRenderer.clearGeometryGenerationAfterLogout();
        LegacyDisplayStandBlockEntityRenderer.clearGeometryGenerationAfterLogout();
        BobbleheadBlockEntityRenderer.clearGeometryGenerationAfterLogout();
        PlushieBlockEntityRenderer.clearGeometryGenerationAfterLogout();
        SnowglobeBlockEntityRenderer.clearGeometryGenerationAfterLogout();
        PressBlockEntityRenderer.clearGeometryGenerationAfterLogout();
        CrashedBombBlockEntityRenderer.clearClientStateAfterLogout();
        FloodlightBlockEntityRenderer.clearClientStateAfterLogout();
        PowerPylonBlockEntityRenderer.invalidateWireCaches();
        LauncherStructCoreBlockEntityRenderer.invalidateModelCache();
        IcfStructBlockEntityRenderer.invalidateModelCache();
        FusionTorusStructBlockEntityRenderer.invalidateModelCache();
        WatzStructBlockEntityRenderer.invalidateModelCache();
        CargoElevatorBlockEntityRenderer.closeGpuCachesOnLogout();
        ExcavatorBlockEntityRenderer.closeGpuCachesOnLogout();
        PipeAnchorBlockEntityRenderer.closeGpuCachesOnLogout();
        RbmkComponentBlockEntityRenderer.closeGpuCachesOnLogout();
        LauncherBlockEntityRenderer.closeGpuCachesOnLogout();
        LauncherBlockEntity.clearClientRenderSnapshotsAfterLogout();
        PipeAnchorBlockEntity.clearClientRenderInstances();
        PENDING_NEIGHBOR_SNAPSHOT_CHUNKS.clear();
    }

    @SubscribeEvent
    public static void onClientChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ClientLevel level) {
            PipeAnchorBlockEntity.onClientEndpointChunkChanged(level, event.getChunk().getPos());
            queueNeighborSnapshotRefresh(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onClientChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ClientLevel level) {
            PowerPylonBlockEntity.onClientChunkUnload(level, event.getChunk().getPos());
            PowerPylonBlockEntityRenderer.invalidateWireCaches(level, event.getChunk().getPos());
            PipeAnchorBlockEntity.onClientEndpointChunkChanged(level, event.getChunk().getPos());
            queueNeighborSnapshotRefresh(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            PENDING_NEIGHBOR_SNAPSHOT_CHUNKS.clear();
            return;
        }
        Set<ChunkPos> changedChunks = PENDING_NEIGHBOR_SNAPSHOT_CHUNKS.remove(level);
        if (changedChunks == null || changedChunks.isEmpty()) {
            return;
        }
        Set<BlockPos> owners = new HashSet<>();
        for (ChunkPos changed : changedChunks) {
            // Every cached dependency in this audit is at most three blocks
            // from its owner, so it cannot cross more than one chunk border.
            for (int chunkX = changed.x - 1; chunkX <= changed.x + 1; chunkX++) {
                for (int chunkZ = changed.z - 1; chunkZ <= changed.z + 1; chunkZ++) {
                    if (!level.hasChunk(chunkX, chunkZ)) {
                        continue;
                    }
                    LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        if (blockEntity instanceof ClientRenderNeighborSnapshot) {
                            owners.add(blockEntity.getBlockPos().immutable());
                        } else if (blockEntity instanceof MachineDummyBlockEntity dummy) {
                            owners.add(dummy.getCorePos().immutable());
                        }
                    }
                }
            }
        }
        for (BlockPos owner : owners) {
            ClientRenderNeighborSnapshot.refreshAt(level, owner);
        }
    }

    private static void queueNeighborSnapshotRefresh(ClientLevel level, ChunkPos changedChunk) {
        PENDING_NEIGHBOR_SNAPSHOT_CHUNKS
                .computeIfAbsent(level, ignored -> new HashSet<>())
                .add(changedChunk);
    }
}
