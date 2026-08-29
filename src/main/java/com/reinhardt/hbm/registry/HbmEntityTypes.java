package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.ChekhovBulletEntity;
import com.reinhardt.hbm.entity.LegacyDeliveryDroneEntity;
import com.reinhardt.hbm.entity.LegacyRequestDroneEntity;
import com.reinhardt.hbm.entity.CogEntity;
import com.reinhardt.hbm.entity.SawbladeEntity;
import com.reinhardt.hbm.entity.ConveyorMovingItem;
import com.reinhardt.hbm.entity.DigammaSpearEntity;
import com.reinhardt.hbm.entity.LegacyDuckEntity;
import com.reinhardt.hbm.entity.LegacyBomberEntity;
import com.reinhardt.hbm.entity.LegacyBossProjectileEntity;
import com.reinhardt.hbm.entity.LegacyChopperEntity;
import com.reinhardt.hbm.entity.LegacyChopperMineEntity;
import com.reinhardt.hbm.entity.LegacyUfoEntity;
import com.reinhardt.hbm.entity.LegacyWormBodyEntity;
import com.reinhardt.hbm.entity.LegacyWormHeadEntity;
import com.reinhardt.hbm.entity.LegacyBombletEntity;
import com.reinhardt.hbm.entity.ClusterSubmunitionEntity;
import com.reinhardt.hbm.entity.LegacyBoxcarEntity;
import com.reinhardt.hbm.entity.LegacyBobmazonEntity;
import com.reinhardt.hbm.entity.LegacyMinecartEntity;
import com.reinhardt.hbm.entity.LegacyTrainEntity;
import com.reinhardt.hbm.entity.JeremyShellEntity;
import com.reinhardt.hbm.entity.LegacyArtilleryShellEntity;
import com.reinhardt.hbm.entity.LegacyBulletEntity;
import com.reinhardt.hbm.entity.LegacyHimarsRocketEntity;
import com.reinhardt.hbm.entity.LegacyLingeringFireEntity;
import com.reinhardt.hbm.entity.LegacyMistEntity;
import com.reinhardt.hbm.entity.LegacyShrapnelEntity;
import com.reinhardt.hbm.entity.LegacyVortexEntity;
import com.reinhardt.hbm.entity.MeteorEntity;
import com.reinhardt.hbm.entity.MineRubbleEntity;
import com.reinhardt.hbm.entity.NukeTorexEntity;
import com.reinhardt.hbm.entity.RbmkDebrisEntity;
import com.reinhardt.hbm.entity.RubberBoatEntity;
import com.reinhardt.hbm.entity.TimedExplosiveEntity;
import com.reinhardt.hbm.entity.LegacyGrenadeEntity;
import com.reinhardt.hbm.entity.UniversalGrenadeEntity;
import com.reinhardt.hbm.entity.SoyuzCapsuleEntity;
import com.reinhardt.hbm.entity.SoyuzEntity;
import com.reinhardt.hbm.entity.MinerRocketEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class HbmEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ReinhardtsHBM.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CogEntity>> COG =
            ENTITY_TYPES.register("entity_cog", () -> EntityType.Builder
                    .<CogEntity>of(CogEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(1000)
                    .updateInterval(10)
                    .build("entity_cog"));

    public static final DeferredHolder<EntityType<?>, EntityType<SawbladeEntity>> SAWBLADE =
            ENTITY_TYPES.register("entity_sawblade", () -> EntityType.Builder
                    .<SawbladeEntity>of(SawbladeEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(1000)
                    .updateInterval(10)
                    .build("entity_sawblade"));

    public static final DeferredHolder<EntityType<?>, EntityType<ConveyorMovingItem>> CONVEYOR_ITEM =
            ENTITY_TYPES.register("entity_conveyor_item", () -> EntityType.Builder
                    .<ConveyorMovingItem>of(ConveyorMovingItem::new, MobCategory.MISC)
                    .sized(0.375F, 0.375F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("entity_conveyor_item"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyDeliveryDroneEntity>> DELIVERY_DRONE =
            ENTITY_TYPES.register("entity_delivery_drone", () -> EntityType.Builder
                    .<LegacyDeliveryDroneEntity>of(LegacyDeliveryDroneEntity::new, MobCategory.MISC)
                    .sized(0.75F, 0.75F)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .build("entity_delivery_drone"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyRequestDroneEntity>> REQUEST_DRONE =
            ENTITY_TYPES.register("entity_request_drone", () -> EntityType.Builder
                    .<LegacyRequestDroneEntity>of(LegacyRequestDroneEntity::new, MobCategory.MISC)
                    .sized(0.75F, 0.75F)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .build("entity_request_drone"));

    public static final DeferredHolder<EntityType<?>, EntityType<RbmkDebrisEntity>> RBMK_DEBRIS =
            ENTITY_TYPES.register("entity_rbmk_debris", () -> EntityType.Builder
                    .<RbmkDebrisEntity>of(RbmkDebrisEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(128)
                    .updateInterval(2)
                    .build("entity_rbmk_debris"));

    public static final DeferredHolder<EntityType<?>, EntityType<MineRubbleEntity>> MINE_RUBBLE =
            ENTITY_TYPES.register("entity_mine_rubble", () -> EntityType.Builder
                    .<MineRubbleEntity>of(MineRubbleEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("entity_mine_rubble"));

    public static final DeferredHolder<EntityType<?>, EntityType<RubberBoatEntity>> RUBBER_BOAT =
            ENTITY_TYPES.register("entity_rubber_boat", () -> EntityType.Builder
                    .<RubberBoatEntity>of(RubberBoatEntity::new, MobCategory.MISC)
                    .sized(1.5F, 0.6F)
                    .clientTrackingRange(80)
                    .updateInterval(3)
                    .build("entity_rubber_boat"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyDuckEntity>> DUCK =
            ENTITY_TYPES.register("entity_duck", () -> EntityType.Builder
                    .<LegacyDuckEntity>of(LegacyDuckEntity::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.7F)
                    .clientTrackingRange(80)
                    .build("entity_duck"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBomberEntity>> LEGACY_BOMBER =
            ENTITY_TYPES.register("entity_bomber", () -> EntityType.Builder
                    .<LegacyBomberEntity>of(LegacyBomberEntity::new, MobCategory.MISC)
                    .sized(8.0F, 4.0F)
                    .fireImmune()
                    .clientTrackingRange(512)
                    .updateInterval(1)
                    .build("entity_bomber"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyUfoEntity>> LEGACY_UFO =
            ENTITY_TYPES.register("entity_ufo", () -> EntityType.Builder
                    .<LegacyUfoEntity>of(LegacyUfoEntity::new, MobCategory.MONSTER)
                    .sized(15.0F, 4.0F)
                    .fireImmune()
                    .clientTrackingRange(512)
                    .updateInterval(1)
                    .build("entity_ufo"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyChopperEntity>> LEGACY_CHOPPER =
            ENTITY_TYPES.register("entity_hunter_chopper", () -> EntityType.Builder
                    .<LegacyChopperEntity>of(LegacyChopperEntity::new, MobCategory.MONSTER)
                    .sized(8.25F, 3.0F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_hunter_chopper"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyChopperMineEntity>> LEGACY_CHOPPER_MINE =
            ENTITY_TYPES.register("entity_chopper_mine", () -> EntityType.Builder
                    .<LegacyChopperMineEntity>of(LegacyChopperMineEntity::new, MobCategory.MISC)
                    .sized(12.0F, 12.0F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_chopper_mine"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyWormHeadEntity>> LEGACY_WORM_HEAD =
            ENTITY_TYPES.register("entity_bot_prime_head", () -> EntityType.Builder
                    .<LegacyWormHeadEntity>of(LegacyWormHeadEntity::new, MobCategory.MONSTER)
                    .sized(3.0F, 3.0F)
                    .fireImmune()
                    .clientTrackingRange(512)
                    .updateInterval(1)
                    .build("entity_bot_prime_head"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyWormBodyEntity>> LEGACY_WORM_BODY =
            ENTITY_TYPES.register("entity_bot_prime_body", () -> EntityType.Builder
                    .<LegacyWormBodyEntity>of(LegacyWormBodyEntity::new, MobCategory.MONSTER)
                    .sized(2.0F, 2.0F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_bot_prime_body"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBossProjectileEntity>> LEGACY_BOSS_PROJECTILE =
            ENTITY_TYPES.register("entity_boss_projectile", () -> EntityType.Builder
                    .<LegacyBossProjectileEntity>of(LegacyBossProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_boss_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBombletEntity>> LEGACY_BOMBLET =
            ENTITY_TYPES.register("entity_bomblet", () -> EntityType.Builder
                    .<LegacyBombletEntity>of(LegacyBombletEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_bomblet"));

    public static final DeferredHolder<EntityType<?>, EntityType<ClusterSubmunitionEntity>> CLUSTER_SUBMUNITION =
            ENTITY_TYPES.register("entity_cluster_submunition", () -> EntityType.Builder
                    .<ClusterSubmunitionEntity>of(ClusterSubmunitionEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_cluster_submunition"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBoxcarEntity>> LEGACY_BOXCAR =
            ENTITY_TYPES.register("entity_boxcar", () -> EntityType.Builder
                    .<LegacyBoxcarEntity>of(LegacyBoxcarEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_boxcar"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBobmazonEntity>> LEGACY_BOBMAZON =
            ENTITY_TYPES.register("entity_bobmazon", () -> EntityType.Builder
                    .<LegacyBobmazonEntity>of(LegacyBobmazonEntity::new, MobCategory.MISC)
                    .sized(1.0F, 3.0F)
                    .fireImmune()
                    .clientTrackingRange(512)
                    .updateInterval(1)
                    .build("entity_bobmazon"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyMinecartEntity>> LEGACY_MINECART =
            ENTITY_TYPES.register("entity_legacy_minecart", () -> EntityType.Builder
                    .<LegacyMinecartEntity>of(LegacyMinecartEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.7F)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .build("entity_legacy_minecart"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyTrainEntity>> LEGACY_TRAIN =
            ENTITY_TYPES.register("entity_legacy_train", () -> EntityType.Builder
                    .<LegacyTrainEntity>of(LegacyTrainEntity::new, MobCategory.MISC)
                    .sized(5.0F, 2.0F)
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_legacy_train"));

    public static final DeferredHolder<EntityType<?>, EntityType<DigammaSpearEntity>> DIGAMMA_SPEAR =
            ENTITY_TYPES.register("entity_digamma_spear", () -> EntityType.Builder
                    .<DigammaSpearEntity>of(DigammaSpearEntity::new, MobCategory.MISC)
                    .sized(2.0F, 10.0F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(2)
                    .build("entity_digamma_spear"));

    public static final DeferredHolder<EntityType<?>, EntityType<JeremyShellEntity>> JEREMY_SHELL =
            ENTITY_TYPES.register("entity_jeremy_shell", () -> EntityType.Builder
                    .<JeremyShellEntity>of(JeremyShellEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_jeremy_shell"));

    public static final DeferredHolder<EntityType<?>, EntityType<ChekhovBulletEntity>> CHEKHOV_BULLET =
            ENTITY_TYPES.register("entity_chekhov_bullet", () -> EntityType.Builder
                    .<ChekhovBulletEntity>of(ChekhovBulletEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_chekhov_bullet"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBulletEntity>> LEGACY_BULLET =
            ENTITY_TYPES.register("entity_legacy_bullet", () -> EntityType.Builder
                    .<LegacyBulletEntity>of(LegacyBulletEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_legacy_bullet"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyArtilleryShellEntity>> LEGACY_ARTILLERY_SHELL =
            ENTITY_TYPES.register("entity_artillery_shell", () -> EntityType.Builder
                    .<LegacyArtilleryShellEntity>of(LegacyArtilleryShellEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(512)
                    .updateInterval(1)
                    .build("entity_artillery_shell"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyHimarsRocketEntity>> LEGACY_HIMARS_ROCKET =
            ENTITY_TYPES.register("entity_himars", () -> EntityType.Builder
                    .<LegacyHimarsRocketEntity>of(LegacyHimarsRocketEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(512)
                    .updateInterval(1)
                    .build("entity_himars"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyLingeringFireEntity>> LEGACY_LINGERING_FIRE =
            ENTITY_TYPES.register("entity_fire_lingering", () -> EntityType.Builder
                    .<LegacyLingeringFireEntity>of(LegacyLingeringFireEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .fireImmune()
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .build("entity_fire_lingering"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyMistEntity>> LEGACY_MIST =
            ENTITY_TYPES.register("entity_mist", () -> EntityType.Builder
                    .<LegacyMistEntity>of(LegacyMistEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_mist"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyShrapnelEntity>> LEGACY_SHRAPNEL =
            ENTITY_TYPES.register("entity_shrapnel", () -> EntityType.Builder
                    .<LegacyShrapnelEntity>of(LegacyShrapnelEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_shrapnel"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyVortexEntity>> LEGACY_VORTEX =
            ENTITY_TYPES.register("entity_vortex", () -> EntityType.Builder
                    .<LegacyVortexEntity>of(LegacyVortexEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("entity_vortex"));

    public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR =
            ENTITY_TYPES.register("entity_meteor", () -> EntityType.Builder
                    .<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                    .sized(4.0F, 4.0F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(2)
                    .build("entity_meteor"));

    public static final DeferredHolder<EntityType<?>, EntityType<TimedExplosiveEntity>> TIMED_EXPLOSIVE =
            ENTITY_TYPES.register("entity_timed_explosive", () -> EntityType.Builder
                    .<TimedExplosiveEntity>of(TimedExplosiveEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("entity_timed_explosive"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyGrenadeEntity>> LEGACY_GRENADE =
            ENTITY_TYPES.register("entity_legacy_grenade", () -> EntityType.Builder
                    .<LegacyGrenadeEntity>of(LegacyGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("entity_legacy_grenade"));

    public static final DeferredHolder<EntityType<?>, EntityType<UniversalGrenadeEntity>> UNIVERSAL_GRENADE =
            ENTITY_TYPES.register("entity_universal_grenade", () -> EntityType.Builder
                    .<UniversalGrenadeEntity>of(UniversalGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("entity_universal_grenade"));

    public static final DeferredHolder<EntityType<?>, EntityType<NukeTorexEntity>> NUKE_TOREX =
            ENTITY_TYPES.register("entity_nuke_torex", () -> EntityType.Builder
                    .<NukeTorexEntity>of(NukeTorexEntity::new, MobCategory.MISC)
                    .sized(1.0F, 50.0F)
                    .fireImmune()
                    .clientTrackingRange(1000)
                    .updateInterval(2)
                    .build("entity_nuke_torex"));

    public static final DeferredHolder<EntityType<?>, EntityType<SoyuzEntity>> SOYUZ =
            ENTITY_TYPES.register("entity_soyuz", () -> EntityType.Builder
                    .<SoyuzEntity>of(SoyuzEntity::new, MobCategory.MISC)
                    .sized(5.0F, 50.0F)
                    .fireImmune()
                    .clientTrackingRange(1000)
                    .updateInterval(2)
                    .build("entity_soyuz"));

    public static final DeferredHolder<EntityType<?>, EntityType<SoyuzCapsuleEntity>> SOYUZ_CAPSULE =
            ENTITY_TYPES.register("entity_soyuz_capsule", () -> EntityType.Builder
                    .<SoyuzCapsuleEntity>of(SoyuzCapsuleEntity::new, MobCategory.MISC)
                    .sized(3.0F, 8.0F)
                    .fireImmune()
                    .clientTrackingRange(1000)
                    .updateInterval(2)
                    .build("entity_soyuz_capsule"));

    public static final DeferredHolder<EntityType<?>, EntityType<MinerRocketEntity>> MINER_ROCKET =
            ENTITY_TYPES.register("entity_miner_lander", () -> EntityType.Builder
                    .<MinerRocketEntity>of(MinerRocketEntity::new, MobCategory.MISC)
                    .sized(1.0F, 3.0F)
                    .fireImmune()
                    .clientTrackingRange(1_000)
                    .updateInterval(1)
                    .build("entity_miner_lander"));

    private HbmEntityTypes() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(DUCK.get(), net.minecraft.world.entity.animal.Chicken.createAttributes().build());
        event.put(LEGACY_UFO.get(), LegacyUfoEntity.createAttributes().build());
        event.put(LEGACY_CHOPPER.get(), LegacyChopperEntity.createAttributes().build());
        event.put(LEGACY_WORM_HEAD.get(), LegacyWormHeadEntity.createAttributes().build());
        event.put(LEGACY_WORM_BODY.get(), LegacyWormBodyEntity.createAttributes().build());
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
