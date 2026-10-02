package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.network.TomImpactSyncPayload;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.world.TomImpactSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Persistent TOM aftermath, ported from ModEventHandlerImpact and ImpactWorldHandler. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class TomImpactEvents {
    private TomImpactEvents() { }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            TomImpactSavedData.get(level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TomImpactSavedData.release(level);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        TomImpactSavedData data = TomImpactSavedData.get(level);
        if (data.dust() > 0.0F || data.fire() > 0.0F) {
            applyRandomWorldEffects(level, data);
            burnExposedLiving(level, data);
        }
        data.tickAftermath();
        if ((level.getGameTime() % 20L) == 0L) sync(level, data);
    }

    @SubscribeEvent
    public static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD
                || !TomImpactSavedData.get(level).impact()) return;
        Mob mob = event.getEntity();
        // PopulateChunkEvent.ANIMALS was denied after impact in 1.7.10.
        // CHUNK_GENERATION + CREATURE is the direct modern spawn path.
        if (event.getSpawnType() == MobSpawnType.CHUNK_GENERATION
                && mob.getType().getCategory() == MobCategory.CREATURE) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            return;
        }
        boolean waterMob = mob instanceof WaterAnimal;
        // Keep the original operator precedence: height alone is enough to deny.
        if (mob.getBbHeight() >= 0.85F
                || mob.getBbWidth() >= 0.85F && !waterMob && !mob.isBaby()) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
        if (waterMob && level.random.nextInt(5) != 0) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onNewChunk(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) return;
        TomImpactSavedData data = TomImpactSavedData.get(level);
        if (!data.impact()) return;
        boolean activeAtmosphere = data.dust() > 0.0F || data.fire() > 0.0F;
        if (activeAtmosphere) {
            // BiomeEvent in 1.7.10 swapped the biome top block even below the
            // stronger post-population scorch threshold.
            replaceSurfaceGrass(event.getChunk());
        }
        if (data.dust() > 0.25F || data.fire() > 0.0F) {
            scorchNewChunk(event.getChunk());
        }
    }

    private static void applyRandomWorldEffects(ServerLevel level, TomImpactSavedData data) {
        if (level.players().isEmpty()) return;
        int viewDistance = Math.max(2, level.getServer().getPlayerList().getViewDistance());
        for (int pass = 0; pass < 3; pass++) {
            ServerPlayer player = level.players().get(level.random.nextInt(level.players().size()));
            int chunkX = player.chunkPosition().x + level.random.nextInt(viewDistance * 2 + 1) - viewDistance;
            int chunkZ = player.chunkPosition().z + level.random.nextInt(viewDistance * 2 + 1) - viewDistance;
            if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) continue;
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    if (level.random.nextBoolean()) continue;
                    int x = (chunkX << 4) + localX;
                    int z = (chunkZ << 4) + localZ;
                    int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    int span = Math.max(1, top - level.getMinBuildHeight());
                    int y = level.getMinBuildHeight() + level.random.nextInt(span);
                    BlockPos pos = new BlockPos(x, y, z);
                    if (data.dust() > 0.0F) killDarkenedPlant(level, pos, data.dust());
                    if (data.fire() > 0.0F) burnWorld(level, pos);
                }
            }
        }
    }

    private static void killDarkenedPlant(ServerLevel level, BlockPos pos, float dust) {
        BlockPos above = pos.above();
        int light = Math.max(level.getBrightness(LightLayer.BLOCK, above),
                (int) (level.getMaxLocalRawBrightness(above) * (1.0F - dust)));
        if (light >= 4) return;
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.getBlock() instanceof BushBlock
                || state.getBlock() instanceof LeavesBlock
                || state.getBlock() instanceof VineBlock) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void burnWorld(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockPos above = pos.above();
        boolean exposed = level.getBrightness(LightLayer.SKY, above) >= 7;
        if (state.isFlammable(level, pos, Direction.UP) && level.isEmptyBlock(above) && exposed) {
            if (state.getBlock() instanceof LeavesBlock || state.getBlock() instanceof BushBlock) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            level.setBlock(above, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
        } else if ((state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM)
                || state.is(HbmBlocks.WASTE_EARTH.get()) || state.is(HbmBlocks.FROZEN_GRASS.get())
                || state.is(HbmBlocks.WASTE_MYCELIUM.get()))
                && !level.isRainingAt(above) && exposed) {
            level.setBlock(pos, HbmBlocks.BURNING_EARTH.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(HbmBlocks.FROZEN_DIRT.get()) && exposed) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void burnExposedLiving(ServerLevel level, TomImpactSavedData data) {
        if (data.fire() <= 0.0F || data.dust() >= 0.75F) return;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity living) || !living.isAlive()) continue;
            if (level.getBrightness(LightLayer.SKY, living.blockPosition()) > 7) {
                living.igniteForSeconds(5.0F);
                living.hurt(level.damageSources().onFire(), 2.0F);
            }
        }
    }

    private static void scorchNewChunk(ChunkAccess chunk) {
        int minY = chunk.getMinBuildHeight();
        int maxY = minY + chunk.getHeight();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = minY; y < maxY; y++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    cursor.set(minX + localX, y, minZ + localZ);
                    BlockState state = chunk.getBlockState(cursor);
                    if (state.isAir()) continue;
                    if (state.is(Blocks.GRASS_BLOCK)) {
                        chunk.setBlockState(cursor, HbmBlocks.IMPACT_DIRT.get().defaultBlockState(), false);
                    } else if (state.is(BlockTags.LOGS) || state.getBlock() instanceof LeavesBlock
                            || state.getBlock() instanceof BushBlock || state.getBlock() instanceof VineBlock) {
                        chunk.setBlockState(cursor, Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static void replaceSurfaceGrass(ChunkAccess chunk) {
        int minY = chunk.getMinBuildHeight();
        int maxY = minY + chunk.getHeight() - 1;
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                for (int y = maxY; y >= minY; y--) {
                    cursor.set(minX + localX, y, minZ + localZ);
                    if (chunk.getBlockState(cursor).is(Blocks.GRASS_BLOCK)) {
                        chunk.setBlockState(cursor, HbmBlocks.IMPACT_DIRT.get().defaultBlockState(), false);
                        break;
                    }
                }
            }
        }
    }

    private static void sync(ServerLevel level, TomImpactSavedData data) {
        TomImpactSyncPayload payload = new TomImpactSyncPayload(data.dust(), data.fire(), data.impact());
        for (ServerPlayer player : level.players()) PacketDistributor.sendToPlayer(player, payload);
    }
}
