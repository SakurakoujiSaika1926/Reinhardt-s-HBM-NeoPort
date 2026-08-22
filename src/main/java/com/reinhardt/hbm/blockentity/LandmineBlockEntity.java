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
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class LandmineBlockEntity extends BlockEntity {
    private boolean primed;
    private boolean waitingForPlayer;

    public LandmineBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LANDMINE.get(), pos, state);
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
}
