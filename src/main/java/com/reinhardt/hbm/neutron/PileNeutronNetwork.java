package com.reinhardt.hbm.neutron;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.PileGraphiteBlock;
import com.reinhardt.hbm.blockentity.PileGraphiteBlockEntity;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Delayed neutron stream execution matching the legacy NeutronHandler timing:
 * block entities create streams during a level tick and they resolve on the
 * next level-tick pre phase. World access stays on the server thread.
 */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class PileNeutronNetwork {
    private static final int RANGE = 5;
    private static final Map<ServerLevel, List<Ray>> PENDING = new IdentityHashMap<>();

    private PileNeutronNetwork() {
    }

    public static void emit(Level level, BlockPos origin, int flux) {
        if (!(level instanceof ServerLevel serverLevel) || flux <= 0) {
            return;
        }
        Vec3 vector = new Vec3(1.0D, 0.0D, 0.0D)
                .zRot((float) (Math.PI * 2.0D * level.random.nextDouble()))
                .yRot((float) (Math.PI * 2.0D * level.random.nextDouble()))
                .xRot((float) (Math.PI * 2.0D * level.random.nextDouble()));
        PENDING.computeIfAbsent(serverLevel, ignored -> new ArrayList<>()).add(new Ray(origin.immutable(), vector, flux));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<Ray> rays = PENDING.remove(level);
        if (rays == null || rays.isEmpty()) {
            return;
        }
        for (Ray ray : rays) {
            resolve(level, ray);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PENDING.remove(level);
        }
    }

    private static void resolve(ServerLevel level, Ray ray) {
        double flux = ray.flux();
        BlockPos origin = ray.origin();
        for (float distance = 1.0F; distance <= RANGE; distance += 0.5F) {
            BlockPos targetPos = BlockPos.containing(
                    Math.floor(origin.getX() + 0.5D + ray.vector().x * distance),
                    Math.floor(origin.getY() + 0.5D + ray.vector().y * distance),
                    Math.floor(origin.getZ() + 0.5D + ray.vector().z * distance)
            );
            if (targetPos.equals(origin)) {
                continue;
            }

            BlockEntity target = level.getBlockEntity(targetPos);
            if (target == null) {
                return;
            }
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.is(HbmBlocks.BLOCK_BORON.get())) {
                return;
            }
            if (targetState.is(HbmBlocks.CONCRETE.get())
                    || targetState.is(HbmBlocks.CONCRETE_SMOOTH.get())
                    || targetState.is(HbmBlocks.CONCRETE_ASBESTOS.get())
                    || targetState.is(HbmBlocks.CONCRETE_COLORED.get())
                    || targetState.is(HbmBlocks.BRICK_CONCRETE.get())) {
                flux *= 0.25D;
            }

            if (target instanceof PileGraphiteBlockEntity pile) {
                PileGraphiteBlock.Kind kind = targetState.getBlock() instanceof PileGraphiteBlock block
                        ? block.kind()
                        : PileGraphiteBlock.Kind.DRILLED;
                if (kind == PileGraphiteBlock.Kind.FUEL
                        || kind == PileGraphiteBlock.Kind.LITHIUM
                        || kind == PileGraphiteBlock.Kind.DETECTOR) {
                    pile.receiveNeutrons((int) Math.floor(flux));
                    if (kind != PileGraphiteBlock.Kind.DETECTOR || !targetState.getValue(PileGraphiteBlock.ACTIVE)) {
                        return;
                    }
                }
            }

            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(targetPos))) {
                HbmLivingRadiation radiation = HbmLivingRadiation.get(living);
                radiation.setNeutron(radiation.getNeutron() + (float) (flux / 4.0D));
                HbmLivingRadiation.set(living, radiation);
            }
        }
    }

    private record Ray(BlockPos origin, Vec3 vector, int flux) {
    }
}
