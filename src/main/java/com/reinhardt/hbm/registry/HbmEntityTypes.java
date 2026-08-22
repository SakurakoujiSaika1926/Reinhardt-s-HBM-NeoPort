package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.ChekhovBulletEntity;
import com.reinhardt.hbm.entity.CogEntity;
import com.reinhardt.hbm.entity.ConveyorMovingItem;
import com.reinhardt.hbm.entity.DigammaSpearEntity;
import com.reinhardt.hbm.entity.JeremyShellEntity;
import com.reinhardt.hbm.entity.LegacyArtilleryShellEntity;
import com.reinhardt.hbm.entity.LegacyBulletEntity;
import com.reinhardt.hbm.entity.LegacyHimarsRocketEntity;
import com.reinhardt.hbm.entity.LegacyLingeringFireEntity;
import com.reinhardt.hbm.entity.LegacyMistEntity;
import com.reinhardt.hbm.entity.LegacyShrapnelEntity;
import com.reinhardt.hbm.entity.MeteorEntity;
import com.reinhardt.hbm.entity.MineRubbleEntity;
import com.reinhardt.hbm.entity.NukeTorexEntity;
import com.reinhardt.hbm.entity.RbmkDebrisEntity;
import com.reinhardt.hbm.entity.SoyuzCapsuleEntity;
import com.reinhardt.hbm.entity.SoyuzEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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

    public static final DeferredHolder<EntityType<?>, EntityType<ConveyorMovingItem>> CONVEYOR_ITEM =
            ENTITY_TYPES.register("entity_conveyor_item", () -> EntityType.Builder
                    .<ConveyorMovingItem>of(ConveyorMovingItem::new, MobCategory.MISC)
                    .sized(0.375F, 0.375F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("entity_conveyor_item"));

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

    public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR =
            ENTITY_TYPES.register("entity_meteor", () -> EntityType.Builder
                    .<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                    .sized(4.0F, 4.0F)
                    .fireImmune()
                    .clientTrackingRange(256)
                    .updateInterval(2)
                    .build("entity_meteor"));

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

    private HbmEntityTypes() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
