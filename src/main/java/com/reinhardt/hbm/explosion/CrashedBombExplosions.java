package com.reinhardt.hbm.explosion;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.CrashedBombBlock;
import com.reinhardt.hbm.registry.HbmParticleTypes;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.reinhardt.hbm.worldgen.NuclearFalloutTerrainEffects;

/** Explosion and defusal dispatch copied from BlockCrashedBomb. */
public final class CrashedBombExplosions {
    private CrashedBombExplosions() {
    }

    public static void dropDefusedContents(ServerLevel level, BlockPos pos, CrashedBombBlock.Type type) {
        switch (type) {
            case BALEFIRE -> drop(level, pos, "egg_balefire_shard", 1);
            case CONVENTIONAL -> drop(level, pos, "ball_tnt", 16);
            case NUKE -> {
                drop(level, pos, "ball_tnt", 8);
                drop(level, pos, "billet_plutonium", 4);
            }
            case SALTED -> {
                drop(level, pos, "ball_tnt", 8);
                drop(level, pos, "billet_plutonium", 2);
                drop(level, pos, "ingot_cobalt", 12);
            }
        }
    }

    public static void detonate(ServerLevel level, BlockPos pos, CrashedBombBlock.Type type) {
        Vec3 center = Vec3.atCenterOf(pos);
        switch (type) {
            case BALEFIRE -> {
                BalefireExplosionManager.schedule(level, pos, 43);
                spawnMushroom(level, center, true);
            }
            case CONVENTIONAL -> {
                level.explode(null, center.x, center.y, center.z, 35.0F, false, Level.ExplosionInteraction.BLOCK);
                spawnMushroom(level, center, false);
            }
            case NUKE -> {
                NukeExplosionManager.scheduleLegacyNuke(level, center.x, center.y, center.z, 35);
                spawnMushroom(level, center, level.random.nextInt(100) == 0);
            }
            case SALTED -> {
                NukeExplosionManager.scheduleLegacyNuke(level, center.x, center.y, center.z, 25);
                NuclearFalloutTerrainEffects.schedule(level, pos, 25);
                spawnMushroom(level, center, level.random.nextInt(100) == 0);
            }
        }
    }

    private static void spawnMushroom(ServerLevel level, Vec3 center, boolean balefire) {
        level.playSound(null, center.x, center.y, center.z, HbmSoundEvents.WEAPON_MUKE_EXPLOSION.get(),
                SoundSource.BLOCKS, 15.0F, 1.0F);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) > 250.0D * 250.0D) {
                continue;
            }
            level.sendParticles(player,
                    balefire ? HbmParticleTypes.MUKE_FLASH_BALEFIRE.get() : HbmParticleTypes.MUKE_FLASH.get(),
                    true, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(player, HbmParticleTypes.MUKE_WAVE.get(),
                    true, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void drop(Level level, BlockPos pos, String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(id));
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            ReinhardtsHBM.LOGGER.error("Crashed bomb defusal drop is not registered: {}", id);
            return;
        }
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, new ItemStack(item, count)));
    }
}
