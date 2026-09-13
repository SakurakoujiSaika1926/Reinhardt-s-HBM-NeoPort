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
import com.reinhardt.hbm.entity.LegacyBlackHoleEntity;
import com.reinhardt.hbm.entity.LegacyEmpEntity;
import com.reinhardt.hbm.entity.LegacyTomEntity;
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
import com.reinhardt.hbm.entity.FireworksEntity;
import com.reinhardt.hbm.entity.GlyphidEntity;
import com.reinhardt.hbm.entity.LegacyGlyphidVariantEntity;
import com.reinhardt.hbm.entity.GlyphidAcidBombEntity;
import com.reinhardt.hbm.entity.GlyphidAcidSprayEntity;
import com.reinhardt.hbm.entity.ParasiteMaggotEntity;
import com.reinhardt.hbm.entity.LegacyUndeadSoldierEntity;
import com.reinhardt.hbm.entity.LegacyNuclearCreeperEntity;
import com.reinhardt.hbm.entity.LegacyTaintedCreeperEntity;
import com.reinhardt.hbm.entity.LegacyPhosgeneCreeperEntity;
import com.reinhardt.hbm.entity.LegacyVolatileCreeperEntity;
import com.reinhardt.hbm.entity.LegacyGoldCreeperEntity;
import com.reinhardt.hbm.entity.LegacyCyberCrabEntity;
import com.reinhardt.hbm.entity.LegacyTeslaCrabEntity;
import com.reinhardt.hbm.entity.LegacyTaintCrabEntity;
import com.reinhardt.hbm.entity.LegacyMaskManEntity;
import com.reinhardt.hbm.entity.LegacyQuackosEntity;
import com.reinhardt.hbm.entity.LegacyPigeonEntity;
import com.reinhardt.hbm.entity.LegacyFbiEntity;
import com.reinhardt.hbm.entity.LegacyFbiDroneEntity;
import com.reinhardt.hbm.entity.LegacyRadBeastEntity;
import com.reinhardt.hbm.entity.LegacyGhostEntity;
import com.reinhardt.hbm.entity.LegacyPlasticBagEntity;
import com.reinhardt.hbm.entity.LegacyDummyEntity;
import com.reinhardt.hbm.entity.LegacyBlockSpiderEntity;
import com.reinhardt.hbm.entity.LegacyBuoyantItemEntity;
import com.reinhardt.hbm.entity.LegacyWasteItemEntity;
import com.reinhardt.hbm.entity.logic.EntityWaypoint;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class HbmEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ReinhardtsHBM.MOD_ID);
    private static final TagKey<Biome> LEGACY_PLAINS =
            TagKey.create(Registries.BIOME, ReinhardtsHBM.id("legacy_plains"));

    public static final DeferredHolder<EntityType<?>, EntityType<CogEntity>> COG =
            ENTITY_TYPES.register("entity_cog", () -> EntityType.Builder
                    .<CogEntity>of(CogEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_cog"));

    public static final DeferredHolder<EntityType<?>, EntityType<SawbladeEntity>> SAWBLADE =
            ENTITY_TYPES.register("entity_sawblade", () -> EntityType.Builder
                    .<SawbladeEntity>of(SawbladeEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_sawblade"));

    public static final DeferredHolder<EntityType<?>, EntityType<ConveyorMovingItem>> CONVEYOR_ITEM =
            ENTITY_TYPES.register("entity_conveyor_item", () -> EntityType.Builder
                    .<ConveyorMovingItem>of(ConveyorMovingItem::new, MobCategory.MISC)
                    .sized(0.375F, 0.375F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_conveyor_item"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyDeliveryDroneEntity>> DELIVERY_DRONE =
            ENTITY_TYPES.register("entity_delivery_drone", () -> EntityType.Builder
                    .<LegacyDeliveryDroneEntity>of(LegacyDeliveryDroneEntity::new, MobCategory.MISC)
                    .sized(0.75F, 0.75F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_delivery_drone"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyRequestDroneEntity>> REQUEST_DRONE =
            ENTITY_TYPES.register("entity_request_drone", () -> EntityType.Builder
                    .<LegacyRequestDroneEntity>of(LegacyRequestDroneEntity::new, MobCategory.MISC)
                    .sized(0.75F, 0.75F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_request_drone"));

    public static final DeferredHolder<EntityType<?>, EntityType<RbmkDebrisEntity>> RBMK_DEBRIS =
            ENTITY_TYPES.register("entity_rbmk_debris", () -> EntityType.Builder
                    .<RbmkDebrisEntity>of(RbmkDebrisEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_rbmk_debris"));

    public static final DeferredHolder<EntityType<?>, EntityType<MineRubbleEntity>> MINE_RUBBLE =
            ENTITY_TYPES.register("entity_mine_rubble", () -> EntityType.Builder
                    .<MineRubbleEntity>of(MineRubbleEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_mine_rubble"));

    public static final DeferredHolder<EntityType<?>, EntityType<RubberBoatEntity>> RUBBER_BOAT =
            ENTITY_TYPES.register("entity_rubber_boat", () -> EntityType.Builder
                    .<RubberBoatEntity>of(RubberBoatEntity::new, MobCategory.MISC)
                    .sized(1.5F, 0.6F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_rubber_boat"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyDuckEntity>> DUCK =
            ENTITY_TYPES.register("entity_fucc_a_ducc", () -> EntityType.Builder
                    .<LegacyDuckEntity>of(LegacyDuckEntity::new, MobCategory.CREATURE)
                    .sized(0.3F, 0.7F)
                    .clientTrackingRange(5)
                    .build("entity_fucc_a_ducc"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyNuclearCreeperEntity>> NUCLEAR_CREEPER =
            ENTITY_TYPES.register("entity_mob_nuclear_creeper", () -> EntityType.Builder
                    .<LegacyNuclearCreeperEntity>of(LegacyNuclearCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F).clientTrackingRange(5).build("entity_mob_nuclear_creeper"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyTaintedCreeperEntity>> TAINTED_CREEPER =
            ENTITY_TYPES.register("entity_mob_tainted_creeper", () -> EntityType.Builder
                    .<LegacyTaintedCreeperEntity>of(LegacyTaintedCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F).clientTrackingRange(5).build("entity_mob_tainted_creeper"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyPhosgeneCreeperEntity>> PHOSGENE_CREEPER =
            ENTITY_TYPES.register("entity_mob_phosgene_creeper", () -> EntityType.Builder
                    .<LegacyPhosgeneCreeperEntity>of(LegacyPhosgeneCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F).clientTrackingRange(5).build("entity_mob_phosgene_creeper"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyVolatileCreeperEntity>> VOLATILE_CREEPER =
            ENTITY_TYPES.register("entity_mob_volatile_creeper", () -> EntityType.Builder
                    .<LegacyVolatileCreeperEntity>of(LegacyVolatileCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F).clientTrackingRange(5).build("entity_mob_volatile_creeper"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyGoldCreeperEntity>> GOLD_CREEPER =
            ENTITY_TYPES.register("entity_mob_gold_creeper", () -> EntityType.Builder
                    .<LegacyGoldCreeperEntity>of(LegacyGoldCreeperEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.7F).clientTrackingRange(5).build("entity_mob_gold_creeper"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyCyberCrabEntity>> CYBER_CRAB =
            ENTITY_TYPES.register("entity_cyber_crab", () -> EntityType.Builder
                    .<LegacyCyberCrabEntity>of((type, level) -> new LegacyCyberCrabEntity(type, level, LegacyCyberCrabEntity.Kind.CYBER), MobCategory.MONSTER)
                    .sized(0.75F, 0.35F).clientTrackingRange(5).build("entity_cyber_crab"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyTeslaCrabEntity>> TESLA_CRAB =
            ENTITY_TYPES.register("entity_tesla_crab", () -> EntityType.Builder
                    .<LegacyTeslaCrabEntity>of(LegacyTeslaCrabEntity::new, MobCategory.MONSTER)
                    .sized(0.75F, 1.25F).clientTrackingRange(5).build("entity_tesla_crab"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyTaintCrabEntity>> TAINT_CRAB =
            ENTITY_TYPES.register("entity_taint_crab", () -> EntityType.Builder
                    .<LegacyTaintCrabEntity>of(LegacyTaintCrabEntity::new, MobCategory.MONSTER)
                    .sized(1.25F, 1.25F).clientTrackingRange(5).build("entity_taint_crab"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyMaskManEntity>> MASK_MAN =
            ENTITY_TYPES.register("entity_mob_mask_man", () -> EntityType.Builder
                    .<LegacyMaskManEntity>of(LegacyMaskManEntity::new, MobCategory.MONSTER)
                    .sized(2.0F, 5.0F).fireImmune().clientTrackingRange(5).build("entity_mob_mask_man"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyQuackosEntity>> QUACKOS =
            ENTITY_TYPES.register("entity_elder_one", () -> EntityType.Builder
                    .<LegacyQuackosEntity>of(LegacyQuackosEntity::new, MobCategory.CREATURE)
                    .sized(7.5F, 17.5F).clientTrackingRange(5).build("entity_elder_one"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyPigeonEntity>> PIGEON =
            ENTITY_TYPES.register("entity_pigeon", () -> EntityType.Builder
                    .<LegacyPigeonEntity>of(LegacyPigeonEntity::new, MobCategory.CREATURE)
                    .sized(0.5F, 1.0F).clientTrackingRange(5).build("entity_pigeon"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyFbiEntity>> FBI =
            ENTITY_TYPES.register("entity_ntm_fbi", () -> EntityType.Builder
                    .<LegacyFbiEntity>of(LegacyFbiEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).fireImmune().clientTrackingRange(5).build("entity_ntm_fbi"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyFbiDroneEntity>> FBI_DRONE =
            ENTITY_TYPES.register("entity_ntm_fbi_drone", () -> EntityType.Builder
                    .<LegacyFbiDroneEntity>of(LegacyFbiDroneEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).clientTrackingRange(5).build("entity_ntm_fbi_drone"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyRadBeastEntity>> RAD_BEAST =
            ENTITY_TYPES.register("entity_ntm_radiation_blaze", () -> EntityType.Builder
                    .<LegacyRadBeastEntity>of(LegacyRadBeastEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.8F).fireImmune().clientTrackingRange(5).build("entity_ntm_radiation_blaze"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyGhostEntity>> GHOST =
            ENTITY_TYPES.register("entity_ntm_ghost", () -> EntityType.Builder
                    .<LegacyGhostEntity>of(LegacyGhostEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(63).updateInterval(1).build("entity_ntm_ghost"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyPlasticBagEntity>> PLASTIC_BAG =
            ENTITY_TYPES.register("entity_plastic_bag", () -> EntityType.Builder
                    .<LegacyPlasticBagEntity>of(LegacyPlasticBagEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.45F, 0.45F).clientTrackingRange(5).build("entity_plastic_bag"));
    public static final DeferredHolder<EntityType<?>, EntityType<LegacyDummyEntity>> DUMMY =
            ENTITY_TYPES.register("entity_ntm_test_dummy", () -> EntityType.Builder
                    .<LegacyDummyEntity>of(LegacyDummyEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(5).build("entity_ntm_test_dummy"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBlockSpiderEntity>> BLOCK_SPIDER =
            ENTITY_TYPES.register("entity_taintcrawler", () -> EntityType.Builder
                    .<LegacyBlockSpiderEntity>of(LegacyBlockSpiderEntity::new, MobCategory.MONSTER)
                    .sized(0.95F, 1.25F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_taintcrawler"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBuoyantItemEntity>> BUOYANT_ITEM =
            ENTITY_TYPES.register("entity_item_buoyant", () -> EntityType.Builder
                    .<LegacyBuoyantItemEntity>of(LegacyBuoyantItemEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(7).updateInterval(1)
                    .build("entity_item_buoyant"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyWasteItemEntity>> WASTE_ITEM =
            ENTITY_TYPES.register("entity_item_waste", () -> EntityType.Builder
                    .<LegacyWasteItemEntity>of(LegacyWasteItemEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(7).updateInterval(1)
                    .build("entity_item_waste"));

    public static final DeferredHolder<EntityType<?>, EntityType<GlyphidEntity>> GLYPHID =
            ENTITY_TYPES.register("entity_glyphid", () -> EntityType.Builder
                    .<GlyphidEntity>of(GlyphidEntity::new, MobCategory.MONSTER)
                    .sized(1.75F, 1.0F)
                    .clientTrackingRange(5)
                    .updateInterval(3)
                    .build("entity_glyphid"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyGlyphidVariantEntity>> GLYPHID_BRAWLER =
            ENTITY_TYPES.register("entity_glyphid_brawler", () -> EntityType.Builder
                    .<LegacyGlyphidVariantEntity>of((type, level) -> new LegacyGlyphidVariantEntity(type, level, GlyphidEntity.Variant.BRAWLER), MobCategory.MONSTER)
                    .sized(2.0F, 1.125F).clientTrackingRange(5).updateInterval(3)
                    .build("entity_glyphid_brawler"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyGlyphidVariantEntity>> GLYPHID_BEHEMOTH =
            ENTITY_TYPES.register("entity_glyphid_behemoth", () -> EntityType.Builder
                    .<LegacyGlyphidVariantEntity>of((type, level) -> new LegacyGlyphidVariantEntity(type, level, GlyphidEntity.Variant.BEHEMOTH), MobCategory.MONSTER)
                    .sized(2.5F, 1.5F).clientTrackingRange(5).updateInterval(3)
                    .build("entity_glyphid_behemoth"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyGlyphidVariantEntity>> GLYPHID_BRENDA =
            ENTITY_TYPES.register("entity_glyphid_brenda", () -> EntityType.Builder
                    .<LegacyGlyphidVariantEntity>of((type, level) -> new LegacyGlyphidVariantEntity(type, level, GlyphidEntity.Variant.BRENDA), MobCategory.MONSTER)
                    .sized(2.5F, 1.75F).clientTrackingRange(5).updateInterval(3)
                    .build("entity_glyphid_brenda"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyGlyphidVariantEntity>> GLYPHID_BOMBARDIER =
            ENTITY_TYPES.register("entity_glyphid_bombardier", () -> EntityType.Builder
                    .<LegacyGlyphidVariantEntity>of((type, level) -> new LegacyGlyphidVariantEntity(type, level, GlyphidEntity.Variant.BOMBARDIER), MobCategory.MONSTER)
                    .sized(1.75F, 1.0F).clientTrackingRange(5).updateInterval(3)
                    .build("entity_glyphid_bombardier"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyGlyphidVariantEntity>> GLYPHID_BLASTER =
            ENTITY_TYPES.register("entity_glyphid_blaster", () -> EntityType.Builder
                    .<LegacyGlyphidVariantEntity>of((type, level) -> new LegacyGlyphidVariantEntity(type, level, GlyphidEntity.Variant.BLASTER), MobCategory.MONSTER)
                    .sized(2.0F, 1.125F).clientTrackingRange(5).updateInterval(3)
                    .build("entity_glyphid_blaster"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyGlyphidVariantEntity>> GLYPHID_SCOUT =
            ENTITY_TYPES.register("entity_glyphid_scout", () -> EntityType.Builder
                    .<LegacyGlyphidVariantEntity>of((type, level) -> new LegacyGlyphidVariantEntity(type, level, GlyphidEntity.Variant.SCOUT), MobCategory.MONSTER)
                    .sized(1.25F, 0.75F).clientTrackingRange(5).updateInterval(3)
                    .build("entity_glyphid_scout"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyGlyphidVariantEntity>> GLYPHID_NUCLEAR =
            ENTITY_TYPES.register("entity_glyphid_nuclear", () -> EntityType.Builder
                    .<LegacyGlyphidVariantEntity>of((type, level) -> new LegacyGlyphidVariantEntity(type, level, GlyphidEntity.Variant.NUCLEAR), MobCategory.MONSTER)
                    .sized(2.5F, 1.75F).clientTrackingRange(5).updateInterval(3)
                    .build("entity_glyphid_nuclear"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyGlyphidVariantEntity>> GLYPHID_DIGGER =
            ENTITY_TYPES.register("entity_glyphid_digger", () -> EntityType.Builder
                    .<LegacyGlyphidVariantEntity>of((type, level) -> new LegacyGlyphidVariantEntity(type, level, GlyphidEntity.Variant.DIGGER), MobCategory.MONSTER)
                    .sized(1.75F, 1.0F).clientTrackingRange(5).updateInterval(3)
                    .build("entity_glyphid_digger"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyUndeadSoldierEntity>> UNDEAD_SOLDIER =
            ENTITY_TYPES.register("entity_ntm_undead_soldier", () -> EntityType.Builder
                    .<LegacyUndeadSoldierEntity>of(LegacyUndeadSoldierEntity::new, MobCategory.MONSTER)
                    // EntityMob's default 1.7.10 size is 0.6 x 1.8; the
                    // soldier must not inherit the modern Zombie height.
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(5)
                    .updateInterval(3)
                    .build("entity_ntm_undead_soldier"));

    public static final DeferredHolder<EntityType<?>, EntityType<GlyphidAcidBombEntity>> GLYPHID_ACID_BOMB =
            ENTITY_TYPES.register("glyphid_acid_bomb", () -> EntityType.Builder
                    .<GlyphidAcidBombEntity>of(GlyphidAcidBombEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("glyphid_acid_bomb"));

    public static final DeferredHolder<EntityType<?>, EntityType<GlyphidAcidSprayEntity>> GLYPHID_ACID_SPRAY =
            ENTITY_TYPES.register("glyphid_acid_spray", () -> EntityType.Builder
                    .<GlyphidAcidSprayEntity>of(GlyphidAcidSprayEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("glyphid_acid_spray"));

    public static final DeferredHolder<EntityType<?>, EntityType<ParasiteMaggotEntity>> PARASITE_MAGGOT =
            ENTITY_TYPES.register("entity_parasite_maggot", () -> EntityType.Builder
                    .<ParasiteMaggotEntity>of(ParasiteMaggotEntity::new, MobCategory.MONSTER)
                    .sized(0.3F, 0.7F)
                    .clientTrackingRange(5)
                    .updateInterval(3)
                    .build("entity_parasite_maggot"));

    public static final DeferredHolder<EntityType<?>, EntityType<EntityWaypoint>> GLYPHID_WAYPOINT =
            ENTITY_TYPES.register("glyphid_waypoint", () -> EntityType.Builder
                    .<EntityWaypoint>of(EntityWaypoint::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("glyphid_waypoint"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBomberEntity>> LEGACY_BOMBER =
            ENTITY_TYPES.register("entity_bomber", () -> EntityType.Builder
                    .<LegacyBomberEntity>of(LegacyBomberEntity::new, MobCategory.MISC)
                    .sized(8.0F, 4.0F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_bomber"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyUfoEntity>> LEGACY_UFO =
            ENTITY_TYPES.register("entity_ufo", () -> EntityType.Builder
                    .<LegacyUfoEntity>of(LegacyUfoEntity::new, MobCategory.MONSTER)
                    .sized(15.0F, 4.0F)
                    .fireImmune()
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_ufo"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyChopperEntity>> LEGACY_CHOPPER =
            ENTITY_TYPES.register("entity_hunter_chopper", () -> EntityType.Builder
                    .<LegacyChopperEntity>of(LegacyChopperEntity::new, MobCategory.MONSTER)
                    .sized(8.25F, 3.0F)
                    .fireImmune()
                    .clientTrackingRange(5)
                    .updateInterval(3)
                    .build("entity_hunter_chopper"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyChopperMineEntity>> LEGACY_CHOPPER_MINE =
            ENTITY_TYPES.register("entity_chopper_mine", () -> EntityType.Builder
                    .<LegacyChopperMineEntity>of(LegacyChopperMineEntity::new, MobCategory.MISC)
                    .sized(12.0F, 12.0F)
                    .fireImmune()
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_chopper_mine"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyWormHeadEntity>> LEGACY_WORM_HEAD =
            ENTITY_TYPES.register("entity_bot_prime_head", () -> EntityType.Builder
                    .<LegacyWormHeadEntity>of(LegacyWormHeadEntity::new, MobCategory.MONSTER)
                    .sized(3.0F, 3.0F)
                    .fireImmune()
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_bot_prime_head"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyWormBodyEntity>> LEGACY_WORM_BODY =
            ENTITY_TYPES.register("entity_bot_prime_body", () -> EntityType.Builder
                    .<LegacyWormBodyEntity>of(LegacyWormBodyEntity::new, MobCategory.MONSTER)
                    .sized(2.0F, 2.0F)
                    .fireImmune()
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_bot_prime_body"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBossProjectileEntity>> LEGACY_BOSS_PROJECTILE =
            ENTITY_TYPES.register("entity_boss_projectile", () -> EntityType.Builder
                    .<LegacyBossProjectileEntity>of(LegacyBossProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_boss_projectile"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBombletEntity>> LEGACY_BOMBLET =
            ENTITY_TYPES.register("entity_bomblet", () -> EntityType.Builder
                    .<LegacyBombletEntity>of(LegacyBombletEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_bomblet"));

    public static final DeferredHolder<EntityType<?>, EntityType<ClusterSubmunitionEntity>> CLUSTER_SUBMUNITION =
            ENTITY_TYPES.register("entity_cluster_submunition", () -> EntityType.Builder
                    .<ClusterSubmunitionEntity>of(ClusterSubmunitionEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_cluster_submunition"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBoxcarEntity>> LEGACY_BOXCAR =
            ENTITY_TYPES.register("entity_boxcar", () -> EntityType.Builder
                    .<LegacyBoxcarEntity>of(LegacyBoxcarEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .fireImmune()
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_boxcar"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBobmazonEntity>> LEGACY_BOBMAZON =
            ENTITY_TYPES.register("entity_bobmazon", () -> EntityType.Builder
                    .<LegacyBobmazonEntity>of(LegacyBobmazonEntity::new, MobCategory.MISC)
                    .sized(1.0F, 3.0F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_bobmazon"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyMinecartEntity>> LEGACY_MINECART =
            ENTITY_TYPES.register("entity_legacy_minecart", () -> EntityType.Builder
                    .<LegacyMinecartEntity>of(LegacyMinecartEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.7F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_legacy_minecart"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyTrainEntity>> LEGACY_TRAIN =
            ENTITY_TYPES.register("entity_legacy_train", () -> EntityType.Builder
                    .<LegacyTrainEntity>of(LegacyTrainEntity::new, MobCategory.MISC)
                    .sized(5.0F, 2.0F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_legacy_train"));

    public static final DeferredHolder<EntityType<?>, EntityType<DigammaSpearEntity>> DIGAMMA_SPEAR =
            ENTITY_TYPES.register("entity_digamma_spear", () -> EntityType.Builder
                    .<DigammaSpearEntity>of(DigammaSpearEntity::new, MobCategory.MISC)
                    .sized(2.0F, 10.0F)
                    .fireImmune()
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_digamma_spear"));

    public static final DeferredHolder<EntityType<?>, EntityType<JeremyShellEntity>> JEREMY_SHELL =
            ENTITY_TYPES.register("entity_jeremy_shell", () -> EntityType.Builder
                    .<JeremyShellEntity>of(JeremyShellEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_jeremy_shell"));

    public static final DeferredHolder<EntityType<?>, EntityType<ChekhovBulletEntity>> CHEKHOV_BULLET =
            ENTITY_TYPES.register("entity_chekhov_bullet", () -> EntityType.Builder
                    .<ChekhovBulletEntity>of(ChekhovBulletEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_chekhov_bullet"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBulletEntity>> LEGACY_BULLET =
            ENTITY_TYPES.register("entity_legacy_bullet", () -> EntityType.Builder
                    .<LegacyBulletEntity>of(LegacyBulletEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_legacy_bullet"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyArtilleryShellEntity>> LEGACY_ARTILLERY_SHELL =
            ENTITY_TYPES.register("entity_artillery_shell", () -> EntityType.Builder
                    .<LegacyArtilleryShellEntity>of(LegacyArtilleryShellEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_artillery_shell"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.reinhardt.hbm.entity.LegacyLauncherMissileEntity>> LEGACY_LAUNCHER_MISSILE =
            ENTITY_TYPES.register("launcher_missile", () -> EntityType.Builder
                    .<com.reinhardt.hbm.entity.LegacyLauncherMissileEntity>of(com.reinhardt.hbm.entity.LegacyLauncherMissileEntity::new, MobCategory.MISC)
                    .sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1).build("launcher_missile"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyHimarsRocketEntity>> LEGACY_HIMARS_ROCKET =
            ENTITY_TYPES.register("entity_himars", () -> EntityType.Builder
                    .<LegacyHimarsRocketEntity>of(LegacyHimarsRocketEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_himars"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyLingeringFireEntity>> LEGACY_LINGERING_FIRE =
            ENTITY_TYPES.register("entity_fire_lingering", () -> EntityType.Builder
                    .<LegacyLingeringFireEntity>of(LegacyLingeringFireEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_fire_lingering"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyMistEntity>> LEGACY_MIST =
            ENTITY_TYPES.register("entity_mist", () -> EntityType.Builder
                    .<LegacyMistEntity>of(LegacyMistEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_mist"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyShrapnelEntity>> LEGACY_SHRAPNEL =
            ENTITY_TYPES.register("entity_shrapnel", () -> EntityType.Builder
                    .<LegacyShrapnelEntity>of(LegacyShrapnelEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .fireImmune()
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_shrapnel"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyVortexEntity>> LEGACY_VORTEX =
            ENTITY_TYPES.register("entity_vortex", () -> EntityType.Builder
                    .<LegacyVortexEntity>of(LegacyVortexEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("entity_vortex"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyBlackHoleEntity>> LEGACY_BLACK_HOLE =
            ENTITY_TYPES.register("entity_black_hole", () -> EntityType.Builder
                    .<LegacyBlackHoleEntity>of(LegacyBlackHoleEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("entity_black_hole"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyEmpEntity>> LEGACY_EMP =
            ENTITY_TYPES.register("entity_emp", () -> EntityType.Builder
                    .<LegacyEmpEntity>of(LegacyEmpEntity::new, MobCategory.MISC)
                    .sized(1.5F, 1.5F).clientTrackingRange(63).updateInterval(1)
                    .build("entity_emp"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyTomEntity>> LEGACY_TOM =
            ENTITY_TYPES.register("entity_tom", () -> EntityType.Builder
                    .<LegacyTomEntity>of(LegacyTomEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(63).updateInterval(1)
                    .build("entity_tom"));

    public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR =
            ENTITY_TYPES.register("entity_meteor", () -> EntityType.Builder
                    .<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                    .sized(4.0F, 4.0F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("entity_meteor"));

    public static final DeferredHolder<EntityType<?>, EntityType<TimedExplosiveEntity>> TIMED_EXPLOSIVE =
            ENTITY_TYPES.register("entity_timed_explosive", () -> EntityType.Builder
                    .<TimedExplosiveEntity>of(TimedExplosiveEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_timed_explosive"));

    public static final DeferredHolder<EntityType<?>, EntityType<LegacyGrenadeEntity>> LEGACY_GRENADE =
            ENTITY_TYPES.register("entity_legacy_grenade", () -> EntityType.Builder
                    .<LegacyGrenadeEntity>of(LegacyGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("entity_legacy_grenade"));

    public static final DeferredHolder<EntityType<?>, EntityType<UniversalGrenadeEntity>> UNIVERSAL_GRENADE =
            ENTITY_TYPES.register("entity_universal_grenade", () -> EntityType.Builder
                    .<UniversalGrenadeEntity>of(UniversalGrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_universal_grenade"));

    public static final DeferredHolder<EntityType<?>, EntityType<NukeTorexEntity>> NUKE_TOREX =
            ENTITY_TYPES.register("entity_nuke_torex", () -> EntityType.Builder
                    .<NukeTorexEntity>of(NukeTorexEntity::new, MobCategory.MISC)
                    .sized(1.0F, 50.0F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .setShouldReceiveVelocityUpdates(false)
                    .build("entity_nuke_torex"));

    public static final DeferredHolder<EntityType<?>, EntityType<SoyuzEntity>> SOYUZ =
            ENTITY_TYPES.register("entity_soyuz", () -> EntityType.Builder
                    .<SoyuzEntity>of(SoyuzEntity::new, MobCategory.MISC)
                    .sized(5.0F, 50.0F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_soyuz"));

    public static final DeferredHolder<EntityType<?>, EntityType<SoyuzCapsuleEntity>> SOYUZ_CAPSULE =
            ENTITY_TYPES.register("entity_soyuz_capsule", () -> EntityType.Builder
                    .<SoyuzCapsuleEntity>of(SoyuzCapsuleEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .fireImmune()
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_soyuz_capsule"));

    public static final DeferredHolder<EntityType<?>, EntityType<MinerRocketEntity>> MINER_ROCKET =
            ENTITY_TYPES.register("entity_miner_lander", () -> EntityType.Builder
                    .<MinerRocketEntity>of(MinerRocketEntity::new, MobCategory.MISC)
                    .sized(1.0F, 3.0F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_miner_lander"));

    public static final DeferredHolder<EntityType<?>, EntityType<FireworksEntity>> FIREWORKS =
            ENTITY_TYPES.register("entity_fireworks", () -> EntityType.Builder
                    .<FireworksEntity>of(FireworksEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(63)
                    .updateInterval(1)
                    .build("entity_fireworks"));

    private HbmEntityTypes() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(DUCK.get(), net.minecraft.world.entity.animal.Chicken.createAttributes().build());
        event.put(NUCLEAR_CREEPER.get(), LegacyNuclearCreeperEntity.createAttributes().build());
        event.put(TAINTED_CREEPER.get(), LegacyTaintedCreeperEntity.createAttributes().build());
        event.put(PHOSGENE_CREEPER.get(), LegacyPhosgeneCreeperEntity.createAttributes().build());
        event.put(VOLATILE_CREEPER.get(), LegacyVolatileCreeperEntity.createAttributes().build());
        event.put(GOLD_CREEPER.get(), LegacyGoldCreeperEntity.createAttributes().build());
        event.put(CYBER_CRAB.get(), LegacyCyberCrabEntity.createAttributes(4.0D, 0.75D).build());
        event.put(TESLA_CRAB.get(), LegacyTeslaCrabEntity.createAttributes().build());
        event.put(TAINT_CRAB.get(), LegacyTaintCrabEntity.createAttributes().build());
        event.put(MASK_MAN.get(), LegacyMaskManEntity.createAttributes().build());
        event.put(QUACKOS.get(), LegacyQuackosEntity.createAttributes().build());
        event.put(PIGEON.get(), LegacyPigeonEntity.createAttributes().build());
        event.put(FBI.get(), LegacyFbiEntity.createAttributes().build());
        event.put(FBI_DRONE.get(), LegacyFbiDroneEntity.createAttributes().build());
        event.put(RAD_BEAST.get(), LegacyRadBeastEntity.createAttributes().build());
        event.put(GHOST.get(), LegacyGhostEntity.createAttributes().build());
        event.put(PLASTIC_BAG.get(), LegacyPlasticBagEntity.createAttributes().build());
        event.put(DUMMY.get(), LegacyDummyEntity.createAttributes().build());
        event.put(BLOCK_SPIDER.get(), LegacyBlockSpiderEntity.createAttributes().build());
        event.put(GLYPHID.get(), GlyphidEntity.createAttributes().build());
        event.put(GLYPHID_BRAWLER.get(), GlyphidEntity.createAttributes().build());
        event.put(GLYPHID_BEHEMOTH.get(), GlyphidEntity.createAttributes().build());
        event.put(GLYPHID_BRENDA.get(), GlyphidEntity.createAttributes().build());
        event.put(GLYPHID_BOMBARDIER.get(), GlyphidEntity.createAttributes().build());
        event.put(GLYPHID_BLASTER.get(), GlyphidEntity.createAttributes().build());
        event.put(GLYPHID_SCOUT.get(), GlyphidEntity.createAttributes().build());
        event.put(GLYPHID_NUCLEAR.get(), GlyphidEntity.createAttributes().build());
        event.put(GLYPHID_DIGGER.get(), GlyphidEntity.createAttributes().build());
        event.put(UNDEAD_SOLDIER.get(), LegacyUndeadSoldierEntity.createAttributes().build());
        event.put(PARASITE_MAGGOT.get(), ParasiteMaggotEntity.createAttributes().build());
        event.put(LEGACY_UFO.get(), LegacyUfoEntity.createAttributes().build());
        event.put(LEGACY_CHOPPER.get(), LegacyChopperEntity.createAttributes().build());
        event.put(LEGACY_WORM_HEAD.get(), LegacyWormHeadEntity.createAttributes().build());
        event.put(LEGACY_WORM_BODY.get(), LegacyWormBodyEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(PHOSGENE_CREEPER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getLevel().dimension() == net.minecraft.world.level.Level.OVERWORLD
                        && Monster.checkMonsterSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(VOLATILE_CREEPER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> pos.getY() <= 40
                        && level.getLevel().dimension() == net.minecraft.world.level.Level.OVERWORLD
                        && Monster.checkMonsterSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GOLD_CREEPER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> pos.getY() <= 40
                        && level.getLevel().dimension() == net.minecraft.world.level.Level.OVERWORLD
                        && Monster.checkMonsterSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PIGEON.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getBiome(pos).is(LEGACY_PLAINS)
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PLASTIC_BAG.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> pos.getY() > 45 && pos.getY() < 63
                        && random.nextInt(10) == 0
                        && level.getBiome(pos).is(BiomeTags.IS_OCEAN)
                        && WaterAnimal.checkSurfaceWaterAnimalSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
