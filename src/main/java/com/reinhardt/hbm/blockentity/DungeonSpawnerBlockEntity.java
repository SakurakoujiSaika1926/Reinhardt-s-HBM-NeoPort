package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.entity.LegacyUndeadSoldierEntity;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;

/** Exact phase sequencing from 1.7.10 DungeonSpawner.EnumSpawnerType.ABERRATOR. */
public final class DungeonSpawnerBlockEntity extends BlockEntity {
    private int phase;
    private int timer;

    public DungeonSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.DUNGEON_SPAWNER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DungeonSpawnerBlockEntity spawner) {
        if (level instanceof ServerLevel serverLevel) {
            spawner.tickServer(serverLevel);
        }
    }

    private void tickServer(ServerLevel level) {
        runPhase(level);
        if (condition(level)) {
            phase++;
            timer = 0;
        } else {
            timer++;
        }
        setChanged();
    }

    private boolean condition(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (phase == 0) {
            return level.getGameTime() % 20L == 0L && hasNearbyPlayer(level);
        }
        if (phase < 3) {
            return level.getGameTime() % 20L == 0L && timer >= 60 && noSoldiersRemain(level);
        }
        return false;
    }

    private void runPhase(ServerLevel level) {
        if ((phase == 1 || phase == 2) && timer == 0) {
            spawnWave(level);
        }
        if (phase > 2) {
            BlockEntity target = level.getBlockEntity(worldPosition.above(18));
            if (target instanceof LegacyDisplayStandBlockEntity stand) {
                stand.setDisplayedItem(level.random.nextInt(5) == 0
                        ? HbmItems.variantStack(HbmItems.ITEM_SECRET_ITEMS, "aberrator")
                        : new net.minecraft.world.item.ItemStack(HbmItems.CLAY_TABLET.get()));
            }
            level.setBlock(worldPosition, Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private boolean hasNearbyPlayer(ServerLevel level) {
        // 1.7.10: getBoundingBox(x, y, z, x + 1, y - 2, z + 1).expand(20, 10, 20).
        AABB bounds = new AABB(worldPosition.getX() - 20.0D, worldPosition.getY() - 10.0D,
                worldPosition.getZ() - 20.0D, worldPosition.getX() + 21.0D,
                worldPosition.getY() + 8.0D, worldPosition.getZ() + 21.0D);
        return !level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, bounds).isEmpty();
    }

    private boolean noSoldiersRemain(ServerLevel level) {
        // 1.7.10 retains the original reversed X extent before expanding it.
        AABB bounds = new AABB(worldPosition.getX() - 50.0D, worldPosition.getY() - 20.0D,
                worldPosition.getZ() - 20.0D, worldPosition.getX() + 48.0D,
                worldPosition.getY() + 21.0D, worldPosition.getZ() + 21.0D);
        return level.getEntitiesOfClass(LegacyUndeadSoldierEntity.class, bounds).isEmpty();
    }

    private void spawnWave(ServerLevel level) {
        for (int index = 0; index < 10; index++) {
            double angle = Math.toRadians(index * 36.0D);
            double x = worldPosition.getX() + 0.5D + Math.cos(angle) * 10.0D;
            // Vec3NT#rotateAroundYDeg(36): z' = z cos(a) - x sin(a).
            double z = worldPosition.getZ() + 0.5D - Math.sin(angle) * 10.0D;
            for (int attempt = 0; attempt < 7; attempt++) {
                LegacyUndeadSoldierEntity soldier = new LegacyUndeadSoldierEntity(HbmEntityTypes.UNDEAD_SOLDIER.get(), level);
                soldier.moveTo(x, worldPosition.getY() - 5.0D, z, index * 36.0F, 0.0F);
                if (!level.noCollision(soldier) || !level.getEntities(soldier, soldier.getBoundingBox()).isEmpty()
                        || level.containsAnyLiquid(soldier.getBoundingBox())) {
                    continue;
                }
                EventHooks.finalizeMobSpawn(soldier, level,
                        level.getCurrentDifficultyAt(soldier.blockPosition()), MobSpawnType.EVENT, null);
                level.addFreshEntity(soldier);
                break;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // The original tile writes phase and type, but deliberately does not persist its timer.
        tag.putInt("phase", phase);
        tag.putByte("type", (byte) 0);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        phase = tag.getInt("phase");
        timer = 0;
    }
}
