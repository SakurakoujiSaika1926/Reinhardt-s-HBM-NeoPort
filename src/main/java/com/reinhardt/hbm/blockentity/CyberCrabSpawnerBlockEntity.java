package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.entity.LegacyCyberCrabEntity;
import com.reinhardt.hbm.entity.LegacyTeslaCrabEntity;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Exact server tick from 1.7.10 TileEntityCyberCrab. */
public final class CyberCrabSpawnerBlockEntity extends BlockEntity {
    private int age;

    public CyberCrabSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.CYBER_CRAB_SPAWNER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CyberCrabSpawnerBlockEntity spawner) {
        if (level.isClientSide) {
            return;
        }

        spawner.age++;
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;
        boolean playerNearby = !level.getEntitiesOfClass(Player.class,
                new AABB(x - 25.0D, y - 25.0D, z - 25.0D, x + 25.0D, y + 25.0D, z + 25.0D),
                player -> player.distanceToSqr(x, y, z) <= 25.0D * 25.0D).isEmpty();
        if (spawner.age <= 200 || !level.getBlockState(pos.above()).isAir() || !playerNearby) {
            return;
        }

        AABB crabBox = new AABB(pos.getX() - 5.0D, pos.getY() - 2.0D, pos.getZ() - 5.0D,
                pos.getX() + 6.0D, pos.getY() + 4.0D, pos.getZ() + 6.0D);
        if (level.getEntitiesOfClass(LegacyCyberCrabEntity.class, crabBox).size() < 5) {
            LegacyCyberCrabEntity crab = level.random.nextInt(5) == 0
                    ? new LegacyTeslaCrabEntity(HbmEntityTypes.TESLA_CRAB.get(), level)
                    : new LegacyCyberCrabEntity(HbmEntityTypes.CYBER_CRAB.get(), level, LegacyCyberCrabEntity.Kind.CYBER);
            crab.setPos(x, y, z);
            level.addFreshEntity(crab);
        }
        spawner.age = 0;
    }
}
