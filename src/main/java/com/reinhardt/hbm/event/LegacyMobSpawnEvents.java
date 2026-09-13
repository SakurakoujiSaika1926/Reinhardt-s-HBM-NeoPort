package com.reinhardt.hbm.event;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.entity.LegacyMobEquipment;
import com.reinhardt.hbm.entity.LegacyFbiDroneEntity;
import com.reinhardt.hbm.entity.LegacyFbiEntity;
import com.reinhardt.hbm.entity.LegacyGhostEntity;
import com.reinhardt.hbm.entity.LegacyMaskManEntity;
import com.reinhardt.hbm.entity.LegacyRadBeastEntity;
import com.reinhardt.hbm.network.PlayerInformPayload;
import com.reinhardt.hbm.player.HbmLegacyMobSpawnState;
import com.reinhardt.hbm.pollution.PollutionEvents;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmDataAttachments;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.pollution.HbmPollutionData;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.item.LegacyJetpackItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Exact 1.7.10 BossSpawnHandler rolls and its player-persisted FBI/rad marks. */
@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class LegacyMobSpawnEvents {
    private static final long FBI_MARK_DURATION = 20L * 60L * 20L;

    private LegacyMobSpawnEvents() {
    }

    /** The literal 1.7.10 PlayerInformPacket sent on login before the first duck. */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // A client can disconnect while holding jump; clear the server-side
        // jetpack state before the next connection starts ticking armor.
        LegacyJetpackItem.setInputActive(player, false);
        if (!HbmConfig.ENABLE_DUCKS.get()
                || player.getData(HbmDataAttachments.LEGACY_MOB_SPAWN_STATE).hasDucked()) {
            return;
        }
        PacketDistributor.sendToPlayer(player,
                new PlayerInformPayload(0, "Press O to Duck!", 0xFFFFFF, 30_000));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LegacyJetpackItem.setInputActive(player, false);
        }
    }

    /**
     * 1.7.10 ModEventHandler.decorateMob received both LivingSpawnEvent.CheckSpawn
     * and LivingSpawnEvent.SpecialSpawn from SpawnerAnimals.  PositionCheck and
     * FinalizeSpawnEvent are the corresponding modern natural-spawn phases.
     * At creation, direct level.addFreshEntity calls are intentionally not
     * observed: old World.spawnEntityInWorld only posted EntityJoinWorldEvent,
     * not either LivingSpawnEvent child.
     */
    @SubscribeEvent
    public static void decorateLegacyMobCheckSpawn(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL) {
            decorateLegacyNaturalMob(event.getEntity(), event.getLevel());
        }
    }

    @SubscribeEvent
    public static void decorateLegacyMobSpecialSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL) {
            decorateLegacyNaturalMob(event.getEntity(), event.getLevel());
        }
    }

    /**
     * The 1.7.10 base-class listener also received AllowDespawn.  Forge called
     * that child event only when EntityLiving.entityAge & 31 was 31, and only
     * when persistenceRequired was false.  This deliberately lets direct
     * Logic Block zombies keep their tier equipment until that old despawn
     * check, rather than overwriting it at creation.
     */
    @SubscribeEvent
    public static void decorateLegacyMobDespawn(MobDespawnEvent event) {
        Mob mob = event.getEntity();
        if (!mob.isPersistenceRequired() && (mob.getNoActionTime() & 0x1F) == 0x1F) {
            decorateLegacyNaturalMob(mob, event.getLevel());
        }
    }

    private static void decorateLegacyNaturalMob(Mob mob, net.minecraft.world.level.ServerLevelAccessor levelAccessor) {
        if (!HbmConfig.ENABLE_MOB_GEAR.get()
                || !(levelAccessor instanceof ServerLevel level)) {
            return;
        }
        if (mob.getType() == EntityType.ZOMBIE && mob instanceof Zombie zombie && !zombie.isBaby()) {
            double soot = HbmPollutionData.get(level).get(zombie.blockPosition(), HbmPollutionType.SOOT);
            if (level.random.nextFloat() < 0.005F && soot > 2.0D) {
                LegacyMobEquipment.equipHazmat(zombie);
                return;
            }
            LegacyMobEquipment.assignCommonSoot(zombie);
            return;
        }
        if (mob.getType() == EntityType.SKELETON && mob instanceof Skeleton skeleton && !skeleton.isBaby()) {
            LegacyMobEquipment.assignSootRangedSkeleton(skeleton);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isLegacySurfaceWorld(level)) {
            return;
        }
        rollTheDice(level);
    }

    /** Equivalent to the old WorldProvider#isSurfaceWorld gate. */
    private static boolean isLegacySurfaceWorld(ServerLevel level) {
        return level.dimensionType().natural() && level.dimensionType().hasSkyLight();
    }

    private static void rollTheDice(ServerLevel level) {
        rollMaskMan(level);
        rollFbiRaid(level);
        rollRadiationBeasts(level);
        rollGhost(level);
    }

    private static void rollMaskMan(ServerLevel level) {
        if (!HbmConfig.ENABLE_MASK_MAN.get() || level.getGameTime() % HbmConfig.MASK_MAN_DELAY.get() != 0L) {
            return;
        }

        RandomSource random = level.random;
        if (random.nextInt(HbmConfig.MASK_MAN_CHANCE.get()) != 0 || level.players().isEmpty()) {
            return;
        }

        ServerPlayer player = level.players().get(random.nextInt(level.players().size()));
        boolean crystallizerStat = !HbmConfig.ENABLE_STAT_REREGISTERING.get()
                || player.getStats().getValue(Stats.ITEM_CRAFTED.get(HbmBlocks.MACHINE_CRYSTALLIZER.get().asItem())) > 0
                || player.getStats().getValue(Stats.ITEM_USED.get(HbmBlocks.MACHINE_CRYSTALLIZER.get().asItem())) > 0;
        if (!crystallizerStat || HbmLivingRadiation.get(player).getRadiation() < HbmConfig.MASK_MAN_MIN_RAD.get()) {
            return;
        }

        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) player.getX(), (int) player.getZ());
        if (HbmConfig.MASK_MAN_UNDERGROUND.get() && surface <= player.getY() + 3.0D) {
            return;
        }

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "The mask man is about to claim another victim.").withStyle(ChatFormatting.RED));
        double x = player.getX() + random.nextGaussian() * 20.0D;
        double z = player.getZ() + random.nextGaussian() * 20.0D;
        double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
        trySpawn(level, new LegacyMaskManEntity(HbmEntityTypes.MASK_MAN.get(), level), x, y, z);
    }

    private static void rollFbiRaid(ServerLevel level) {
        if (!HbmConfig.ENABLE_FBI_RAIDS.get() || level.getGameTime() % HbmConfig.FBI_RAID_DELAY.get() != 0L) {
            return;
        }

        RandomSource random = level.random;
        if (random.nextInt(HbmConfig.FBI_RAID_CHANCE.get()) != 0 || level.players().isEmpty()) {
            return;
        }

        ServerPlayer player = level.players().get(random.nextInt(level.players().size()));
        if (player.getData(HbmDataAttachments.LEGACY_MOB_SPAWN_STATE).fbiMarkUntil() >= level.getGameTime()) {
            return;
        }

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("FBI, OPEN UP!").withStyle(ChatFormatting.RED));
        Vec3 vector = rotateAroundYOld(new Vec3(HbmConfig.FBI_RAID_ATTACK_DISTANCE.get(), 0.0D, 0.0D),
                Mth.PI * 2.0F * random.nextFloat());
        for (int index = 0; index < HbmConfig.FBI_RAID_AMOUNT.get(); index++) {
            double x = player.getX() + vector.x + random.nextGaussian() * 5.0D;
            double z = player.getZ() + vector.z + random.nextGaussian() * 5.0D;
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
            trySpawn(level, new LegacyFbiEntity(HbmEntityTypes.FBI.get(), level), x, y, z);
        }
        for (int index = 0; index < HbmConfig.FBI_RAID_DRONES.get(); index++) {
            double x = player.getX() + vector.x + random.nextGaussian() * 5.0D;
            double z = player.getZ() + vector.z + random.nextGaussian() * 5.0D;
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
            trySpawn(level, new LegacyFbiDroneEntity(HbmEntityTypes.FBI_DRONE.get(), level), x, y + 10.0D, z);
        }
    }

    private static void rollRadiationBeasts(ServerLevel level) {
        if (!HbmConfig.ENABLE_MELTDOWN_ELEMENTALS.get()
                || level.getGameTime() % HbmConfig.ELEMENTAL_DELAY.get() != 0L) {
            return;
        }

        RandomSource random = level.random;
        if (random.nextInt(HbmConfig.ELEMENTAL_CHANCE.get()) != 0 || level.players().isEmpty()) {
            return;
        }

        ServerPlayer player = level.players().get(random.nextInt(level.players().size()));
        HbmLegacyMobSpawnState state = player.getData(HbmDataAttachments.LEGACY_MOB_SPAWN_STATE);
        if (!state.radMarked()) {
            return;
        }

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("You hear a faint clicking...")
                .withStyle(ChatFormatting.YELLOW));
        player.setData(HbmDataAttachments.LEGACY_MOB_SPAWN_STATE,
                new HbmLegacyMobSpawnState(state.fbiMarkUntil(), false, state.hasDucked()));

        // This intentionally uses raidAttackDistance, not elementalDistance: that
        // is the actual 1.7.10 BossSpawnHandler implementation.
        Vec3 vector = new Vec3(HbmConfig.FBI_RAID_ATTACK_DISTANCE.get(), 0.0D, 0.0D);
        for (int index = 0; index < HbmConfig.ELEMENTAL_AMOUNT.get(); index++) {
            vector = rotateAroundYOld(vector, Mth.PI * 2.0F * random.nextFloat());
            double x = player.getX() + vector.x + random.nextGaussian();
            double z = player.getZ() + vector.z + random.nextGaussian();
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
            LegacyRadBeastEntity beast = new LegacyRadBeastEntity(HbmEntityTypes.RAD_BEAST.get(), level);
            if (index == 0) {
                beast.makeLeader();
            }
            trySpawn(level, beast, x, y, z);
        }
    }

    /** The unconditional 1.7.10 digamma ghost roll at one-second intervals. */
    private static void rollGhost(ServerLevel level) {
        if (level.getGameTime() % 20L != 0L) {
            return;
        }

        RandomSource random = level.random;
        if (random.nextInt(5) != 0 || level.players().isEmpty()) {
            return;
        }

        ServerPlayer player = level.players().get(random.nextInt(level.players().size()));
        if (HbmLivingRadiation.get(player).getDigamma() <= 0.0F) {
            return;
        }

        Vec3 vector = rotateAroundYOld(new Vec3(75.0D, 0.0D, 0.0D),
                Mth.PI * 2.0F * random.nextFloat());
        double x = player.getX() + vector.x + random.nextGaussian();
        double z = player.getZ() + vector.z + random.nextGaussian();
        double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
        trySpawn(level, new LegacyGhostEntity(HbmEntityTypes.GHOST.get(), level), x, y, z);
    }

    /** Old BossSpawnHandler.trySpawn: event/finalization only, no extra collision or placement predicate. */
    private static void trySpawn(ServerLevel level, Mob mob, double x, double y, double z) {
        mob.moveTo((float) x, (float) y, (float) z, level.random.nextFloat() * 360.0F, 0.0F);

        // 1.7.10 ForgeEventFactory.canEntitySpawn returned DEFAULT unless a
        // listener explicitly allowed or denied it. BossSpawnHandler accepted
        // both DEFAULT and ALLOW, without calling getCanSpawnHere. Do not use
        // EventHooks.checkSpawnPosition here: its DEFAULT path adds modern
        // collision and placement predicates that the old method did not run.
        MobSpawnEvent.PositionCheck positionCheck = new MobSpawnEvent.PositionCheck(
                mob, level, MobSpawnType.EVENT, null);
        NeoForge.EVENT_BUS.post(positionCheck);
        if (positionCheck.getResult() == MobSpawnEvent.PositionCheck.Result.FAIL) {
            return;
        }

        // This is NeoForge's replacement for old doSpecialSpawn followed by
        // onSpawnWithEgg. It preserves FinalizeSpawn listeners and their
        // cancellation semantics without inventing an additional spawn test.
        EventHooks.finalizeMobSpawn(mob, level,
                level.getCurrentDifficultyAt(BlockPos.containing(mob.position())), MobSpawnType.EVENT, null);
        level.addFreshEntity(mob);
        // BossSpawnHandler called doSpecialSpawn only after spawnEntityInWorld.
        // That old event reached PollutionHandler.decorateMob, but no other
        // direct HBM spawn path did.
        PollutionEvents.onLegacySpecialSpawn(mob, level);
    }

    private static Vec3 rotateAroundYOld(Vec3 vector, float angle) {
        float cosine = Mth.cos(angle);
        float sine = Mth.sin(angle);
        return new Vec3(vector.x * cosine + vector.z * sine, vector.y,
                vector.z * cosine - vector.x * sine);
    }

    public static void markFbi(ServerPlayer player) {
        HbmLegacyMobSpawnState state = player.getData(HbmDataAttachments.LEGACY_MOB_SPAWN_STATE);
        player.setData(HbmDataAttachments.LEGACY_MOB_SPAWN_STATE,
                new HbmLegacyMobSpawnState(player.level().getGameTime() + FBI_MARK_DURATION, state.radMarked(),
                        state.hasDucked()));
    }

    public static void markRadiationBeastTarget(ServerPlayer player) {
        HbmLegacyMobSpawnState state = player.getData(HbmDataAttachments.LEGACY_MOB_SPAWN_STATE);
        player.setData(HbmDataAttachments.LEGACY_MOB_SPAWN_STATE,
                new HbmLegacyMobSpawnState(state.fbiMarkUntil(), true, state.hasDucked()));
    }
}
