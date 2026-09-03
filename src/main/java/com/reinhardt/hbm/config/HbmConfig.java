package com.reinhardt.hbm.config;

import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class HbmConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_528_MODE;
    public static final ModConfigSpec.BooleanValue ENABLE_EXPENSIVE_MODE;
    public static final ModConfigSpec.BooleanValue ENABLE_INFINITE_WATER_TANK_RECIPES;
    public static final ModConfigSpec.BooleanValue ENABLE_BOMBER_SHORT_MODE;
    public static final ModConfigSpec.BooleanValue ENABLE_VIRUS;
    public static final ModConfigSpec.BooleanValue DROPPED_XEN_CRYSTAL_EFFECT;
    public static final ModConfigSpec.BooleanValue SCALE_RTG_POWER;
    public static final ModConfigSpec.BooleanValue ENABLE_RTG_DECAY;
    public static final ModConfigSpec.IntValue AUTOCAL_MAX_CLOCK;
    public static final ModConfigSpec.IntValue INDUSTRIAL_TURBINE_INPUT_CAPACITY;
    public static final ModConfigSpec.IntValue INDUSTRIAL_TURBINE_OUTPUT_CAPACITY;
    public static final ModConfigSpec.DoubleValue INDUSTRIAL_TURBINE_EFFICIENCY;
    public static final ModConfigSpec.LongValue STEAM_TURBINE_MAX_POWER;
    public static final ModConfigSpec.IntValue STEAM_TURBINE_INPUT_CAPACITY;
    public static final ModConfigSpec.IntValue STEAM_TURBINE_OUTPUT_CAPACITY;
    public static final ModConfigSpec.IntValue STEAM_TURBINE_MAX_STEAM_PER_TICK;
    public static final ModConfigSpec.DoubleValue STEAM_TURBINE_EFFICIENCY;
    public static final ModConfigSpec.IntValue STEAM_ENGINE_STEAM_CAPACITY;
    public static final ModConfigSpec.IntValue STEAM_ENGINE_SPENT_STEAM_CAPACITY;
    public static final ModConfigSpec.DoubleValue STEAM_ENGINE_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue STIRLING_DIFFUSION;
    public static final ModConfigSpec.DoubleValue STIRLING_EFFICIENCY;
    public static final ModConfigSpec.IntValue STIRLING_MAX_HEAT_NORMAL;
    public static final ModConfigSpec.IntValue STIRLING_MAX_HEAT_STEEL;
    public static final ModConfigSpec.IntValue STIRLING_OVERSPEED_LIMIT;
    public static final ModConfigSpec.LongValue FORCEFIELD_MAX_POWER;
    public static final ModConfigSpec.IntValue FORCEFIELD_BASE_CONSUMPTION;
    public static final ModConfigSpec.IntValue FORCEFIELD_RADIUS_CONSUMPTION;
    public static final ModConfigSpec.IntValue FORCEFIELD_SHIELD_CONSUMPTION;
    public static final ModConfigSpec.IntValue FORCEFIELD_BASE_RADIUS;
    public static final ModConfigSpec.IntValue FORCEFIELD_RADIUS_UPGRADE;
    public static final ModConfigSpec.IntValue FORCEFIELD_SHIELD_UPGRADE;
    public static final ModConfigSpec.DoubleValue FORCEFIELD_COOLDOWN_MODIFIER;
    public static final ModConfigSpec.DoubleValue FORCEFIELD_HEALTH_REGEN_MODIFIER;
    public static final ModConfigSpec.LongValue RADAR_POWER_CAP;
    public static final ModConfigSpec.LongValue RADAR_CONSUMPTION;
    public static final ModConfigSpec.IntValue RADAR_RANGE;
    public static final ModConfigSpec.IntValue RADAR_LARGE_RANGE;
    public static final ModConfigSpec.IntValue RADAR_BUFFER;
    public static final ModConfigSpec.IntValue RADAR_ALTITUDE;
    public static final ModConfigSpec.IntValue RADAR_CHUNK_LOAD_CAP;
    public static final ModConfigSpec.BooleanValue RADAR_GENERATE_CHUNKS;
    public static final ModConfigSpec.IntValue CIWS_ACCURACY;
    public static final ModConfigSpec.DoubleValue MINE_AP_DAMAGE;
    public static final ModConfigSpec.DoubleValue MINE_HE_DAMAGE;
    public static final ModConfigSpec.DoubleValue MINE_SHRAP_DAMAGE;
    public static final ModConfigSpec.DoubleValue MINE_NUKE_DAMAGE;
    public static final ModConfigSpec.DoubleValue MINE_NAVAL_DAMAGE;
    public static final ModConfigSpec.LongValue HE_TO_FE_HE_USED;
    public static final ModConfigSpec.LongValue HE_TO_FE_FE_CREATED;
    public static final ModConfigSpec.DoubleValue HE_TO_FE_INPUT_DECAY;
    public static final ModConfigSpec.LongValue FE_TO_HE_FE_USED;
    public static final ModConfigSpec.LongValue FE_TO_HE_HE_CREATED;
    public static final ModConfigSpec.DoubleValue FE_TO_HE_INPUT_DECAY;
    public static final ModConfigSpec.BooleanValue ENABLE_POLLUTION;
    public static final ModConfigSpec.BooleanValue ENABLE_LEAD_FROM_BLOCKS;
    public static final ModConfigSpec.BooleanValue ENABLE_LEAD_POISONING;
    public static final ModConfigSpec.BooleanValue ENABLE_SOOT_FOG;
    public static final ModConfigSpec.BooleanValue ENABLE_POISON_EFFECT;
    public static final ModConfigSpec.BooleanValue TAINT_TRAILS;
    public static final ModConfigSpec.DoubleValue POLLUTION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue POLLUTION_MOB_BUFF_THRESHOLD;
    public static final ModConfigSpec.DoubleValue POLLUTION_SOOT_FOG_THRESHOLD;
    public static final ModConfigSpec.DoubleValue POLLUTION_SOOT_FOG_DIVISOR;
    public static final ModConfigSpec.DoubleValue GLYPHID_RAMPANT_SMOKESTACK_OVERRIDE;
    public static final ModConfigSpec.BooleanValue GLYPHID_ENABLE_HIVES;
    public static final ModConfigSpec.IntValue GLYPHID_HIVE_SPAWN;
    public static final ModConfigSpec.DoubleValue GLYPHID_SCOUT_SOOT_THRESHOLD;
    public static final ModConfigSpec.IntValue GLYPHID_SCOUT_SWARM_CHANCE;
    public static final ModConfigSpec.IntValue GLYPHID_LARGE_HIVE_CHANCE;
    public static final ModConfigSpec.DoubleValue GLYPHID_TARGETING_THRESHOLD;
    public static final ModConfigSpec.BooleanValue GLYPHID_NATURAL_SCOUT_SPAWN;
    public static final ModConfigSpec.DoubleValue GLYPHID_NATURAL_SCOUT_THRESHOLD;
    public static final ModConfigSpec.IntValue GLYPHID_NATURAL_SCOUT_CHANCE;
    public static final ModConfigSpec.BooleanValue GLYPHID_RAMPANT_MODE;
    public static final ModConfigSpec.BooleanValue GLYPHID_RAMPANT_EXTENDED_TARGETING;
    public static final ModConfigSpec.BooleanValue GLYPHID_RAMPANT_DIG;
    public static final ModConfigSpec.BooleanValue GLYPHID_RAMPANT_GUIDANCE;
    public static final ModConfigSpec.BooleanValue GLYPHID_SCOUT_INITIAL_SPAWN;
    public static final ModConfigSpec.BooleanValue GLYPHID_WAYPOINT_DEBUG;
    public static final ModConfigSpec.IntValue RBMK_COLUMN_HEIGHT;
    public static final ModConfigSpec.DoubleValue RBMK_PASSIVE_COOLING;
    public static final ModConfigSpec.DoubleValue RBMK_PASSIVE_COOLING_INNER;
    public static final ModConfigSpec.DoubleValue RBMK_COLUMN_HEAT_FLOW;
    public static final ModConfigSpec.DoubleValue RBMK_FUEL_DIFFUSION_MOD;
    public static final ModConfigSpec.DoubleValue RBMK_HEAT_PROVISION;
    public static final ModConfigSpec.DoubleValue RBMK_BOILER_HEAT_CONSUMPTION;
    public static final ModConfigSpec.DoubleValue RBMK_CONTROL_SPEED;
    public static final ModConfigSpec.DoubleValue RBMK_REACTIVITY_MOD;
    public static final ModConfigSpec.DoubleValue RBMK_SURGE_MOD;
    public static final ModConfigSpec.DoubleValue RBMK_OUTGASSER_SPEED_MOD;
    public static final ModConfigSpec.IntValue RBMK_FLUX_RANGE;
    public static final ModConfigSpec.DoubleValue RBMK_MODERATOR_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue RBMK_ABSORBER_EFFICIENCY;
    public static final ModConfigSpec.DoubleValue RBMK_ABSORBER_HEAT_CONVERSION;
    public static final ModConfigSpec.DoubleValue RBMK_REFLECTOR_EFFICIENCY;
    public static final ModConfigSpec.BooleanValue RBMK_REASIM_BOILERS;
    public static final ModConfigSpec.DoubleValue RBMK_REASIM_BOILER_SPEED;
    public static final ModConfigSpec.BooleanValue RBMK_DISABLE_MELTDOWNS;
    public static final ModConfigSpec.BooleanValue RBMK_OVERPRESSURE;
    public static final ModConfigSpec.BooleanValue RBMK_ENABLE_DEPLETION;
    public static final ModConfigSpec.BooleanValue RBMK_ENABLE_XENON;
    public static final ModConfigSpec.IntValue RBMK_FALLOUT_RANGE;
    public static final ModConfigSpec.IntValue RBMK_FALLOUT_DELAY;

    public static final ModConfigSpec.BooleanValue GENERATE_OIL_DEPOSITS;
    public static final ModConfigSpec.IntValue OIL_DEPOSIT_SPAWN_RATE;
    public static final ModConfigSpec.IntValue OIL_DEPOSIT_DRY_BIOME_DIVISOR;
    public static final ModConfigSpec.IntValue OIL_DEPOSIT_MIN_RADIUS;
    public static final ModConfigSpec.IntValue OIL_DEPOSIT_MAX_RADIUS;

    public static final ModConfigSpec.BooleanValue GENERATE_OIL_SAND_DEPOSITS;
    public static final ModConfigSpec.IntValue OIL_SAND_DEPOSIT_SPAWN_RATE;
    public static final ModConfigSpec.IntValue OIL_SAND_DEPOSIT_MIN_RADIUS;
    public static final ModConfigSpec.IntValue OIL_SAND_DEPOSIT_MAX_RADIUS;

    public static final ModConfigSpec.BooleanValue GENERATE_BEDROCK_OIL_DEPOSITS;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_DEPOSIT_SPAWN_RATE;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_DXZ_LIMIT;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_MAX_Y_OFFSET;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_L1_MAX;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_SURFACE_RADIUS;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_SURFACE_ATTEMPTS;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_POROUS_VEIN_COUNT;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_POROUS_VEIN_SIZE;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_POROUS_MIN_Y;
    public static final ModConfigSpec.IntValue BEDROCK_OIL_POROUS_Y_VARIANCE;
    public static final ModConfigSpec.BooleanValue GENERATE_BEDROCK_ORES;
    public static final ModConfigSpec.IntValue BEDROCK_ORE_SPAWN_RATE;
    public static final ModConfigSpec.IntValue BEDROCK_ORE_NETHER_GLOWSTONE_WEIGHT;
    public static final ModConfigSpec.IntValue BEDROCK_ORE_NETHER_PHOSPHORUS_WEIGHT;
    public static final ModConfigSpec.IntValue BEDROCK_ORE_NETHER_QUARTZ_WEIGHT;
    public static final ModConfigSpec.IntValue METEORITE_SPAWN;
    public static final ModConfigSpec.IntValue CHLORINE_GEYSER_SPAWN_RATE;
    public static final ModConfigSpec.BooleanValue ENABLE_METEOR_STRIKES;
    public static final ModConfigSpec.BooleanValue ENABLE_METEOR_SHOWERS;
    public static final ModConfigSpec.BooleanValue ENABLE_METEOR_TAILS;
    public static final ModConfigSpec.BooleanValue ENABLE_SPECIAL_METEORS;
    public static final ModConfigSpec.IntValue METEOR_STRIKE_CHANCE;
    public static final ModConfigSpec.IntValue METEOR_SHOWER_CHANCE;
    public static final ModConfigSpec.IntValue METEOR_SHOWER_DURATION;

    public static final ModConfigSpec.BooleanValue GENERATE_HBM_STRUCTURES;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_MIN_CHUNKS;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_MAX_CHUNKS;
    public static final ModConfigSpec.BooleanValue HBM_STRUCTURE_ENABLE_RUINS;
    public static final ModConfigSpec.BooleanValue HBM_STRUCTURE_ENABLE_OCEAN;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_SPIRE_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_VERTIBIRD_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_CRASHED_VERTIBIRD_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_AIRCRAFT_CARRIER_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_OIL_RIG_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_LIGHTHOUSE_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_BEACHED_PATROL_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_DISH_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_FOREST_CHEM_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_PLANE1_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_PLANE2_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_DESERT_SHACK1_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_DESERT_SHACK2_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_DESERT_SHACK3_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_LABORATORY_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_FOREST_POST_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_FACTORY_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_CRANE_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_BROADCASTING_TOWER_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RADIO_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_FEATURES_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_BUNKER_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_METEOR_DUNGEON_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_A_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_B_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_C_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_D_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_E_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_F_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_G_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_H_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_I_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_RUIN_J_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_PLAINS_NULL_WEIGHT;
    public static final ModConfigSpec.IntValue HBM_STRUCTURE_OCEAN_NULL_WEIGHT;
    public static final ModConfigSpec.IntValue ANCIENT_TOMB_SPAWN_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("528");
        ENABLE_528_MODE = builder
                .comment("是否启用 HBM 528 模式。默认与 HBM 1.12.2 一致：false。启用后部分机器和配方会使用 528 模式。")
                .define("enable528Mode", false);
        ENABLE_EXPENSIVE_MODE = builder
                .comment("是否启用 HBM 1.7.10 昂贵模式。影响精密装配机故障产物的材料回收率。")
                .define("enableExpensiveMode", false);
        builder.pop();

        builder.push("recipes");
        ENABLE_INFINITE_WATER_TANK_RECIPES = builder
                .comment(
                        "是否启用无限水罐与无限水罐 Mk2 的制造配方。已有水泵的生存环境建议保持关闭；默认：false。",
                        "Enable crafting recipes for the Infinite Water Tank and Infinite Water Tank Mk2. Default: false."
                )
                .define("enableInfiniteWaterTankRecipes", false);
        builder.pop();

        builder.push("airstrikes");
        ENABLE_BOMBER_SHORT_MODE = builder
                .comment("Spawn bombers closer to the target. HBM 1.7.10 default: false.")
                .define("enableBomberShortMode", false);
        builder.pop();

        builder.push("dangerousItems");
        DROPPED_XEN_CRYSTAL_EFFECT = builder
                .comment("Whether dropped Xen crystals move terrain. HBM 1.7.10 default: true.")
                .define("droppedXenCrystalEffect", true);
        ENABLE_VIRUS = builder
                .comment("Allow crystal virus blocks to spread. HBM 1.7.10 default: false.")
                .define("enableVirus", false);
        builder.pop();

        builder.push("machines");
        SCALE_RTG_POWER = builder
                .comment("RTG 燃料输出是否随剩余寿命降低。HBM 1.7.10 默认值：false。")
                .define("scaleRtgPower", false);
        ENABLE_RTG_DECAY = builder
                .comment("RTG 燃料是否衰变为枯竭靶丸。HBM 1.7.10 默认值：true；528 模式强制启用。")
                .define("enableRtgDecay", true);
        AUTOCAL_MAX_CLOCK = builder
                .comment("AUTOCAL MS-ES1 maximum instructions per tick. HBM 1.7.10 default: 20.")
                .defineInRange("autocalMaxClock", 20, 1, 100);
        builder.pop();

        builder.push("industrialTurbine");
        INDUSTRIAL_TURBINE_INPUT_CAPACITY = builder
                .comment("工业汽轮机输入蒸汽容量。1.7.10 默认值：750000 mB。")
                .defineInRange("inputTankSize", 750_000, 1, Integer.MAX_VALUE);
        INDUSTRIAL_TURBINE_OUTPUT_CAPACITY = builder
                .comment("工业汽轮机输出乏蒸汽容量。1.7.10 默认值：3000000 mB。")
                .defineInRange("outputTankSize", 3_000_000, 1, Integer.MAX_VALUE);
        INDUSTRIAL_TURBINE_EFFICIENCY = builder
                .comment("工业汽轮机效率倍率。1.7.10 默认值：1.0。")
                .defineInRange("efficiency", 1.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("steamTurbine");
        STEAM_TURBINE_MAX_POWER = builder
                .comment("小型汽轮机的最大电力缓存。HBM 1.7.10 默认值：1000000 HE。")
                .defineInRange("maxPower", 1_000_000L, 1L, Long.MAX_VALUE);
        STEAM_TURBINE_INPUT_CAPACITY = builder
                .comment("小型汽轮机输入蒸汽容量。HBM 1.7.10 默认值：64000 mB。")
                .defineInRange("inputTankSize", 64_000, 1, Integer.MAX_VALUE);
        STEAM_TURBINE_OUTPUT_CAPACITY = builder
                .comment("小型汽轮机输出乏蒸汽容量。HBM 1.7.10 默认值：128000 mB。")
                .defineInRange("outputTankSize", 128_000, 1, Integer.MAX_VALUE);
        STEAM_TURBINE_MAX_STEAM_PER_TICK = builder
                .comment("小型汽轮机每 tick 最多处理的蒸汽量。HBM 1.7.10 默认值：6000 mB。")
                .defineInRange("maxSteamPerTick", 6_000, 1, Integer.MAX_VALUE);
        STEAM_TURBINE_EFFICIENCY = builder
                .comment("小型汽轮机效率倍率。HBM 1.7.10 默认值：0.85。")
                .defineInRange("efficiency", 0.85D, 0.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("steamEngine");
        STEAM_ENGINE_STEAM_CAPACITY = builder
                .comment("蒸汽机输入蒸汽容量。HBM 1.7.10 默认值：2000 mB。")
                .defineInRange("steamCap", 2_000, 1, Integer.MAX_VALUE);
        STEAM_ENGINE_SPENT_STEAM_CAPACITY = builder
                .comment("蒸汽机输出乏蒸汽容量。HBM 1.7.10 默认值：20 mB。")
                .defineInRange("spentSteamCap", 20, 1, Integer.MAX_VALUE);
        STEAM_ENGINE_EFFICIENCY = builder
                .comment("蒸汽机发电效率倍率。HBM 1.7.10 默认值：0.85。")
                .defineInRange("efficiency", 0.85D, 0.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("stirling");
        STIRLING_DIFFUSION = builder
                .comment("斯特林发电机每 tick 从热源提取的热量比例。HBM 1.7.10 默认值：0.1。")
                .defineInRange("diffusion", 0.1D, 0.0D, Double.MAX_VALUE);
        STIRLING_EFFICIENCY = builder
                .comment("斯特林发电机热能转电能效率。HBM 1.7.10 默认值：0.5。")
                .defineInRange("efficiency", 0.5D, 0.0D, Double.MAX_VALUE);
        STIRLING_MAX_HEAT_NORMAL = builder
                .comment("普通斯特林发电机的超速热量阈值。HBM 1.7.10 默认值：300 TU/t。")
                .defineInRange("maxHeatNormal", 300, 1, Integer.MAX_VALUE);
        STIRLING_MAX_HEAT_STEEL = builder
                .comment("重型斯特林发电机的超速热量阈值。HBM 1.7.10 默认值：1500 TU/t。")
                .defineInRange("maxHeatSteel", 1_500, 1, Integer.MAX_VALUE);
        STIRLING_OVERSPEED_LIMIT = builder
                .comment("斯特林发电机连续超速多少 tick 后抛出齿轮。HBM 1.7.10 默认值：300。")
                .defineInRange("overspeedLimit", 300, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("forcefield");
        FORCEFIELD_MAX_POWER = builder
                .comment("力场发生器的最大储能。1.7.10 默认值：1000000。")
                .defineInRange("maxPower", 1_000_000L, 0L, Long.MAX_VALUE);
        FORCEFIELD_BASE_CONSUMPTION = builder
                .comment("力场发生器的基础耗电量。1.7.10 默认值：1000 HE/t。")
                .defineInRange("baseConsumption", 1000, 0, Integer.MAX_VALUE);
        FORCEFIELD_RADIUS_CONSUMPTION = builder
                .comment("每个半径升级增加的耗电量。1.7.10 默认值：500 HE/t。")
                .defineInRange("radiusConsumption", 500, 0, Integer.MAX_VALUE);
        FORCEFIELD_SHIELD_CONSUMPTION = builder
                .comment("每个护盾升级增加的耗电量。1.7.10 默认值：250 HE/t。")
                .defineInRange("shieldConsumption", 250, 0, Integer.MAX_VALUE);
        FORCEFIELD_BASE_RADIUS = builder
                .comment("力场发生器的基础半径。1.7.10 默认值：16。")
                .defineInRange("baseRadius", 16, 0, Integer.MAX_VALUE);
        FORCEFIELD_RADIUS_UPGRADE = builder
                .comment("每个半径升级增加的半径。1.7.10 默认值：16。")
                .defineInRange("radiusUpgrade", 16, 0, Integer.MAX_VALUE);
        FORCEFIELD_SHIELD_UPGRADE = builder
                .comment("每个护盾升级增加的生命值。1.7.10 默认值：50。")
                .defineInRange("shieldUpgrade", 50, 0, Integer.MAX_VALUE);
        FORCEFIELD_COOLDOWN_MODIFIER = builder
                .comment("力场耗尽后的冷却时间倍率。1.7.10 默认值：1.0。")
                .defineInRange("cooldownModifier", 1.0D, 0.0D, Double.MAX_VALUE);
        FORCEFIELD_HEALTH_REGEN_MODIFIER = builder
                .comment("力场生命值恢复倍率。1.7.10 默认值：1.0。")
                .defineInRange("healthRegenModifier", 1.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("radar");
        RADAR_POWER_CAP = builder
                .comment("雷达最大储能。1.7.10 默认值：100000 HE。")
                .defineInRange("powerCap", 100_000L, 0L, Long.MAX_VALUE);
        RADAR_CONSUMPTION = builder
                .comment("雷达每 tick 扫描耗能。1.7.10 默认值：500 HE/t。")
                .defineInRange("consumption", 500L, 0L, Long.MAX_VALUE);
        RADAR_RANGE = builder
                .comment("雷达扫描半径。1.7.10 默认值：1000 格。")
                .defineInRange("radarRange", 1_000, 1, 30_000);
        RADAR_LARGE_RANGE = builder
                .comment("大型雷达扫描半径。1.7.10 默认值：3000 格。")
                .defineInRange("radarLargeRange", 3_000, 1, 30_000);
        RADAR_BUFFER = builder
                .comment("雷达忽略低于本体该高度范围内目标的缓冲距离。1.7.10 默认值：30 格。")
                .defineInRange("radarBuffer", 30, 0, 1_024);
        RADAR_ALTITUDE = builder
                .comment("雷达允许工作的最低高度。1.7.10 默认值：Y=55。")
                .defineInRange("radarAltitude", 55, -64, 2_048);
        RADAR_CHUNK_LOAD_CAP = builder
                .comment("雷达地形图每 tick 最多请求的未加载区块数。1.7.10 默认值：10。")
                .defineInRange("chunkLoadCap", 10, 0, 100);
        RADAR_GENERATE_CHUNKS = builder
                .comment("雷达地形图是否允许生成此前不存在的区块。1.7.10 默认值：false。")
                .define("generateChunks", false);
        builder.pop();

        builder.push("weapons");
        CIWS_ACCURACY = builder
                .comment("近防炮命中率修正值。默认值与 HBM 1.7.10 的 7.03_ciwsAccuracy 一致：50。")
                .defineInRange("ciwsAccuracy", 50, Integer.MIN_VALUE, Integer.MAX_VALUE);
        MINE_AP_DAMAGE = builder
                .comment("Anti-personnel mine base entity damage. HBM 1.7.10 default: 10.")
                .defineInRange("mineApDamage", 10.0D, 0.0D, Double.MAX_VALUE);
        MINE_HE_DAMAGE = builder
                .comment("High explosive mine base entity damage. HBM 1.7.10 default: 35.")
                .defineInRange("mineHeDamage", 35.0D, 0.0D, Double.MAX_VALUE);
        MINE_SHRAP_DAMAGE = builder
                .comment("Shrapnel mine base entity damage. HBM 1.7.10 default: 7.5.")
                .defineInRange("mineShrapnelDamage", 7.5D, 0.0D, Double.MAX_VALUE);
        MINE_NUKE_DAMAGE = builder
                .comment("Nuclear mine base entity damage. HBM 1.7.10 default: 100.")
                .defineInRange("mineNuclearDamage", 100.0D, 0.0D, Double.MAX_VALUE);
        MINE_NAVAL_DAMAGE = builder
                .comment("Naval mine base entity damage. HBM 1.7.10 default: 60.")
                .defineInRange("mineNavalDamage", 60.0D, 0.0D, Double.MAX_VALUE);
        builder.pop();

        builder.push("energyConverters");
        builder.push("heToFe");
        HE_TO_FE_HE_USED = builder
                .comment("每次换算使用的 HE。对应 HBM 1.7.10 HEToRFConverter 的 L:HE_Used，默认：5。")
                .defineInRange("heUsed", 5L, 1L, Long.MAX_VALUE);
        HE_TO_FE_FE_CREATED = builder
                .comment("每次换算产生的 FE（旧版为 RF）。对应 HBM 1.7.10 的 L:RF_Created，默认：1。")
                .defineInRange("feCreated", 1L, 1L, Long.MAX_VALUE);
        HE_TO_FE_INPUT_DECAY = builder
                .comment("换算后剩余 HE 每 tick 的损耗比例。对应 HBM 1.7.10 的 D:inputDecay2，默认：0。")
                .defineInRange("inputDecay", 0.0D, 0.0D, 1.0D);
        builder.pop();
        builder.push("feToHe");
        FE_TO_HE_FE_USED = builder
                .comment("每次换算使用的 FE（旧版为 RF）。对应 HBM 1.7.10 RFToHEConverter 的 L:RF_Used2，默认：2。")
                .defineInRange("feUsed", 2L, 1L, Long.MAX_VALUE);
        FE_TO_HE_HE_CREATED = builder
                .comment("每次换算产生的 HE。对应 HBM 1.7.10 的 L:HE_Created2，默认：5。")
                .defineInRange("heCreated", 5L, 1L, Long.MAX_VALUE);
        FE_TO_HE_INPUT_DECAY = builder
                .comment("换算后剩余 FE 每 tick 的损耗比例。对应 HBM 1.7.10 的 D:inputDecay2，默认：0。")
                .defineInRange("inputDecay", 0.0D, 0.0D, 1.0D);
        builder.pop();
        builder.pop();

        builder.push("pollution");
        ENABLE_POLLUTION = builder
                .comment("是否启用 HBM 污染系统。默认与旧版一致：true。关闭后机器、流体和烟囱不会增加世界污染。")
                .define("enablePollution", true);
        ENABLE_LEAD_FROM_BLOCKS = builder
                .comment("是否启用旧版重金属污染区域破坏方块导致铅中毒的机制。默认与旧版一致：true。")
                .define("enableLeadFromBlocks", true);
        ENABLE_LEAD_POISONING = builder
                .comment("是否启用旧版重金属污染区域停留导致铅中毒的机制。默认与旧版一致：true。")
                .define("enableLeadPoisoning", true);
        ENABLE_SOOT_FOG = builder
                .comment("是否启用旧版烟尘雾效。默认与旧版一致：true。")
                .define("enableSootFog", true);
        ENABLE_POISON_EFFECT = builder
                .comment("是否启用毒性污染区域对实体施加中毒/凋零效果。默认与旧版一致：true。")
                .define("enablePoisonEffect", true);
        TAINT_TRAILS = builder
                .comment("Enable the low-age taint trails used by the 1.7.10 taint effect. Default: false.")
                .define("taintTrails", false);
        POLLUTION_MULTIPLIER = builder
                .comment("污染生成倍率。默认与旧版配置读取值一致：1.0。旧版开启 rampant 模式时会额外改为 3.0。")
                .defineInRange("pollutionMultiplier", 1.0D, 0.0D, 1_000.0D);
        POLLUTION_MOB_BUFF_THRESHOLD = builder
                .comment("烟尘污染超过该值时，新生成的敌对生物获得旧版烟尘强化。默认与旧版一致：15。")
                .defineInRange("pollutionMobBuffThreshold", 15.0D, 0.0D, 10_000.0D);
        POLLUTION_SOOT_FOG_THRESHOLD = builder
                .comment("烟尘污染超过该值后客户端显示烟雾雾效。默认与旧版一致：35。")
                .defineInRange("pollutionSootFogThreshold", 35.0D, 0.0D, 10_000.0D);
        POLLUTION_SOOT_FOG_DIVISOR = builder
                .comment("烟尘雾效强度除数，数值越大雾越弱。默认与 HBM 1.7.10 一致：120。")
                .defineInRange("pollutionSootFogDivisor", 120.0D, 1.0D, 10_000.0D);
        builder.pop();

        builder.push("glyphids");
        GLYPHID_ENABLE_HIVES = builder
                .comment("Allow small Glyphid hives to generate in the Overworld. HBM 1.7.10 default: true.")
                .define("enableHives", true);
        GLYPHID_HIVE_SPAWN = builder
                .comment("Average number of Overworld chunks per naturally generated Glyphid hive. HBM 1.7.10 default: 256.")
                .defineInRange("hiveSpawn", 256, 1, Integer.MAX_VALUE);
        GLYPHID_SCOUT_SOOT_THRESHOLD = builder
                .comment("Minimum soot for Scout behavior to use polluted-world expansion. HBM 1.7.10 default: 1.")
                .defineInRange("scoutSootThreshold", 1.0D, 0.0D, 10_000.0D);
        GLYPHID_SCOUT_SWARM_CHANCE = builder
                .comment("Scout swarm chance denominator. HBM 1.7.10 default: 3.")
                .defineInRange("scoutSwarmChance", 3, 1, Integer.MAX_VALUE);
        GLYPHID_LARGE_HIVE_CHANCE = builder
                .comment("Large hive chance denominator. HBM 1.7.10 default: 5.")
                .defineInRange("largeHiveChance", 5, 1, Integer.MAX_VALUE);
        GLYPHID_TARGETING_THRESHOLD = builder
                .comment("Soot threshold for extended Glyphid targeting. HBM 1.7.10 default: 1.")
                .defineInRange("targetingThreshold", 1.0D, 0.0D, 10_000.0D);
        GLYPHID_NATURAL_SCOUT_SPAWN = builder
                .comment("Allow Scouts to spawn alongside natural mobs in polluted areas. HBM 1.7.10 default: false.")
                .define("rampantNaturalScoutSpawn", false);
        GLYPHID_NATURAL_SCOUT_THRESHOLD = builder
                .comment("Soot threshold for natural Scout spawning. HBM 1.7.10 default: 13.")
                .defineInRange("rampantScoutSpawnThreshold", 13.0D, 0.0D, 10_000.0D);
        GLYPHID_NATURAL_SCOUT_CHANCE = builder
                .comment("Natural Scout spawn chance denominator. HBM 1.7.10 default: 1400.")
                .defineInRange("rampantScoutSpawnChance", 1400, 1, Integer.MAX_VALUE);
        GLYPHID_RAMPANT_MODE = builder
                .comment("Enable the complete Rampant Glyphid behavior set. HBM 1.7.10 default: false.")
                .define("rampantMode", false);
        GLYPHID_RAMPANT_SMOKESTACK_OVERRIDE = builder
                .comment("Rampant-mode smokestack pollution multiplier. HBM 1.7.10 default: 0.4.")
                .defineInRange("rampantSmokeStackOverride", 0.4D, 0.0D, 10_000.0D);
        GLYPHID_RAMPANT_EXTENDED_TARGETING = builder
                .comment("Always give Glyphids extended targeting. HBM 1.7.10 default: false.")
                .define("rampantExtendedTargeting", false);
        GLYPHID_RAMPANT_DIG = builder
                .comment("Allow Glyphids to dig to waypoints. HBM 1.7.10 default: false.")
                .define("rampantDig", false);
        GLYPHID_RAMPANT_GUIDANCE = builder
                .comment("Make Scouts expand toward the player's respawn point. HBM 1.7.10 default: false.")
                .define("rampantGlyphidGuidance", false);
        GLYPHID_SCOUT_INITIAL_SPAWN = builder
                .comment("Allow a Scout to spawn in a hive's first swarm. HBM 1.7.10 default: false.")
                .define("scoutInitialSpawn", false);
        GLYPHID_WAYPOINT_DEBUG = builder
                .comment("Show Glyphid task waypoints for debugging. HBM 1.7.10 default: false.")
                .define("waypointDebug", false);
        builder.pop();

        builder.push("rbmk");
        RBMK_COLUMN_HEIGHT = builder
                .comment("RBMK 柱体总高度。默认与 HBM 1.7.10 的 dialColumnHeight 一致：4。允许范围与旧版一致：2 到 16。")
                .defineInRange("rbmkColumnHeight", 4, 2, 16);
        RBMK_PASSIVE_COOLING = builder
                .comment("RBMK 边缘被动冷却。默认与 HBM 1.7.10 的 dialPassiveCooling 一致：2.5。")
                .defineInRange("rbmkPassiveCooling", 2.5D, 0.0D, 1_000.0D);
        RBMK_PASSIVE_COOLING_INNER = builder
                .comment("RBMK 内部被动冷却。默认与 HBM 1.7.10 的 dialPassiveCoolingInner 一致：0.1。")
                .defineInRange("rbmkPassiveCoolingInner", 0.1D, 0.0D, 1_000.0D);
        RBMK_COLUMN_HEAT_FLOW = builder
                .comment("RBMK 相邻柱体热量均衡速度。默认与 HBM 1.7.10 的 dialColumnHeatFlow 一致：0.2。")
                .defineInRange("rbmkColumnHeatFlow", 0.2D, 0.0D, 1.0D);
        RBMK_FUEL_DIFFUSION_MOD = builder
                .comment("RBMK 燃料芯到包壳的热扩散倍率。默认与 HBM 1.7.10 的 dialDiffusionMod 一致：1.0。")
                .defineInRange("rbmkFuelDiffusionMod", 1.0D, 0.0D, 1_000.0D);
        RBMK_HEAT_PROVISION = builder
                .comment("RBMK 燃料棒向柱体供热倍率。默认与 HBM 1.7.10 的 dialHeatProvision 一致：0.2。")
                .defineInRange("rbmkHeatProvision", 0.2D, 0.0D, 1.0D);
        RBMK_BOILER_HEAT_CONSUMPTION = builder
                .comment("RBMK 锅炉每 mB 水消耗的热量。默认与 HBM 1.7.10 的 dialBoilerHeatConsumption 一致：0.1。")
                .defineInRange("rbmkBoilerHeatConsumption", 0.1D, 0.0D, 1_000.0D);
        RBMK_CONTROL_SPEED = builder
                .comment("RBMK 控制棒移动速度倍率。默认与 HBM 1.7.10 的 dialControlSpeed 一致：1.0。")
                .defineInRange("rbmkControlSpeed", 1.0D, 0.0D, 1_000.0D);
        RBMK_REACTIVITY_MOD = builder
                .comment("RBMK 燃料反应性倍率。默认与 HBM 1.7.10 的 dialReactivityMod 一致：1.0。")
                .defineInRange("rbmkReactivityMod", 1.0D, 0.0D, 1_000.0D);
        RBMK_SURGE_MOD = builder
                .comment("RBMK 手动控制棒快速抽出时的瞬态反应性倍率。默认与 HBM 1.7.10 的 dialSurgeMod 一致：1.0。")
                .defineInRange("rbmkSurgeMod", 1.0D, 0.0D, 1_000.0D);
        RBMK_OUTGASSER_SPEED_MOD = builder
                .comment("RBMK 脱气器速度倍率。默认与 HBM 1.7.10 的 dialOutgasserSpeedMod 一致：1.0。")
                .defineInRange("rbmkOutgasserSpeedMod", 1.0D, 0.0D, 1_000.0D);
        RBMK_FLUX_RANGE = builder
                .comment("RBMK 中子通量传播范围。默认与 HBM 1.7.10 的 dialFluxRange 一致：5。")
                .defineInRange("rbmkFluxRange", 5, 1, 100);
        RBMK_MODERATOR_EFFICIENCY = builder
                .comment("RBMK 慢化效率。默认与 HBM 1.7.10 的 dialModeratorEfficiency 一致：1.0。")
                .defineInRange("rbmkModeratorEfficiency", 1.0D, 0.0D, 1.0D);
        RBMK_ABSORBER_EFFICIENCY = builder
                .comment("RBMK 吸收柱吸收效率。默认与 HBM 1.7.10 的 dialAbsorberEfficiency 一致：1.0。")
                .defineInRange("rbmkAbsorberEfficiency", 1.0D, 0.0D, 1.0D);
        RBMK_ABSORBER_HEAT_CONVERSION = builder
                .comment("RBMK 吸收柱每单位通量转换的热量。默认与 HBM 1.7.10 的 dialAbsorberHeatConversion 一致：0.05。")
                .defineInRange("rbmkAbsorberHeatConversion", 0.05D, 0.0D, 1_000.0D);
        RBMK_REFLECTOR_EFFICIENCY = builder
                .comment("RBMK 反射器效率。默认与 HBM 1.7.10 的 dialReflectorEfficiency 一致：1.0。")
                .defineInRange("rbmkReflectorEfficiency", 1.0D, 0.0D, 1.0D);
        RBMK_REASIM_BOILERS = builder
                .comment("是否启用 RBMK ReaSim 锅炉机制。默认与 HBM 1.7.10 的 dialReasimBoilers 一致：false。启用后所有 RBMK 柱体都具有内部水/蒸汽缓存，并通过进水口与蒸汽出口交互。")
                .define("rbmkReasimBoilers", false);
        RBMK_REASIM_BOILER_SPEED = builder
                .comment("RBMK ReaSim 锅炉速度。默认与 HBM 1.7.10 的 dialReasimBoilerSpeed 一致：0.05，表示每 tick 处理理论最大量的 5%。")
                .defineInRange("rbmkReasimBoilerSpeed", 0.05D, 0.0D, 1.0D);
        RBMK_DISABLE_MELTDOWNS = builder
                .comment("是否禁用 RBMK 融毁机制。默认与 HBM 1.7.10 一致：false。设为 true 后 RBMK 超温不会触发整堆融毁。")
                .define("rbmkDisableMeltdowns", false);
        RBMK_OVERPRESSURE = builder
                .comment("RBMK 融毁时是否触发相连流体管网过压破坏。默认与 HBM 1.7.10 的 dialEnableMeltdownOverpressure 一致：false。")
                .define("rbmkOverpressure", false);
        RBMK_ENABLE_DEPLETION = builder
                .comment("是否启用 RBMK 燃料消耗。默认与 HBM 1.7.10 的 dialDisableDepletion=false 一致：true。")
                .define("rbmkEnableDepletion", true);
        RBMK_ENABLE_XENON = builder
                .comment("是否启用 RBMK 氙毒机制。默认与 HBM 1.7.10 的 dialDisableXenon=false 一致：true。")
                .define("rbmkEnableXenon", true);
        RBMK_FALLOUT_RANGE = builder
                .comment("RBMK 融毁触发核爆焦土/辐射尘地形效果的半径。默认与 HBM 1.7.10 核爆 falloutRange 基准一致：100。")
                .defineInRange("rbmkFalloutRange", 100, 0, 1024);
        RBMK_FALLOUT_DELAY = builder
                .comment("RBMK 融毁焦土效果每批区块处理之间的 tick 间隔。默认与 HBM 1.7.10 falloutDelay 一致：4。")
                .defineInRange("rbmkFalloutDelay", 4, 0, 200);
        builder.pop();

        builder.push("worldgen");
        builder.push("oil");

        GENERATE_OIL_DEPOSITS = builder
                .comment("是否生成油田。默认与 HBM 1.12.2 一致：true。")
                .define("generateOilDeposits", true);
        OIL_DEPOSIT_SPAWN_RATE = builder
                .comment("油田生成间隔，数值越大越稀有。HBM 1.12.2 默认：100。")
                .defineInRange("oilDepositSpawnRate", 100, 1, 1_000_000);
        OIL_DEPOSIT_DRY_BIOME_DIVISOR = builder
                .comment("炎热干燥群系中的油田生成间隔除数。HBM 1.12.2 默认：3。")
                .defineInRange("oilDepositHotDryBiomeDivisor", 3, 1, 1_000);
        OIL_DEPOSIT_MIN_RADIUS = builder
                .comment("油田最小半径。HBM 1.12.2 默认：10。")
                .defineInRange("oilDepositMinRadius", 10, 1, 128);
        OIL_DEPOSIT_MAX_RADIUS = builder
                .comment("油田最大半径。HBM 1.12.2 默认：16。")
                .defineInRange("oilDepositMaxRadius", 16, 1, 128);

        GENERATE_OIL_SAND_DEPOSITS = builder
                .comment("是否生成油砂矿床。HBM 1.12.2 默认：true。")
                .define("generateOilSandDeposits", true);
        OIL_SAND_DEPOSIT_SPAWN_RATE = builder
                .comment("油砂矿床生成间隔，数值越大越稀有。HBM 1.12.2 默认：600。")
                .defineInRange("oilSandDepositSpawnRate", 600, 1, 1_000_000);
        OIL_SAND_DEPOSIT_MIN_RADIUS = builder
                .comment("油砂矿床最小半径。HBM 1.12.2 默认：15。")
                .defineInRange("oilSandDepositMinRadius", 15, 1, 128);
        OIL_SAND_DEPOSIT_MAX_RADIUS = builder
                .comment("油砂矿床最大半径。HBM 1.12.2 默认：45。")
                .defineInRange("oilSandDepositMaxRadius", 45, 1, 128);

        GENERATE_BEDROCK_OIL_DEPOSITS = builder
                .comment("是否生成基岩油田。HBM 1.12.2 默认：true。")
                .define("generateBedrockOilDeposits", true);
        BEDROCK_OIL_DEPOSIT_SPAWN_RATE = builder
                .comment("基岩油田生成间隔，数值越大越稀有。HBM 1.12.2 默认：200。")
                .defineInRange("bedrockOilDepositSpawnRate", 200, 1, 1_000_000);
        BEDROCK_OIL_DXZ_LIMIT = builder
                .comment("基岩油田水平偏移限制。HBM 1.12.2 默认：4。")
                .defineInRange("bedrockOilHorizontalLimit", 4, 1, 64);
        BEDROCK_OIL_MAX_Y_OFFSET = builder
                .comment("基岩油田最大垂直偏移。HBM 1.12.2 默认：4。")
                .defineInRange("bedrockOilMaxYOffset", 4, 0, 64);
        BEDROCK_OIL_L1_MAX = builder
                .comment("基岩油田 L1 半径。HBM 1.12.2 默认：6。")
                .defineInRange("bedrockOilL1Radius", 6, 1, 128);
        BEDROCK_OIL_SURFACE_RADIUS = builder
                .comment("基岩油田地表油泥半径。HBM 1.12.2 默认：5。")
                .defineInRange("bedrockOilSurfaceRadius", 5, 1, 128);
        BEDROCK_OIL_SURFACE_ATTEMPTS = builder
                .comment("基岩油田地表油泥放置尝试次数。HBM 1.12.2 默认：50。")
                .defineInRange("bedrockOilSurfaceAttempts", 50, 0, 10_000);
        BEDROCK_OIL_POROUS_VEIN_COUNT = builder
                .comment("基岩油田多孔岩脉数量。HBM 1.12.2 默认：16。")
                .defineInRange("bedrockOilPorousStoneVeinCount", 16, 0, 10_000);
        BEDROCK_OIL_POROUS_VEIN_SIZE = builder
                .comment("基岩油田多孔岩脉大小。HBM 1.12.2 默认：8。")
                .defineInRange("bedrockOilPorousStoneVeinSize", 8, 1, 128);
        BEDROCK_OIL_POROUS_MIN_Y = builder
                .comment("基岩油田多孔岩脉最低 Y。HBM 1.12.2 默认：10。")
                .defineInRange("bedrockOilPorousStoneMinY", 10, -256, 512);
        BEDROCK_OIL_POROUS_Y_VARIANCE = builder
                .comment("基岩油田多孔岩脉 Y 随机范围。HBM 1.12.2 默认：50。")
                .defineInRange("bedrockOilPorousStoneYVariance", 50, 0, 512);
        GENERATE_BEDROCK_ORES = builder
                .comment("是否生成通用基岩矿。HBM 1.7.10 默认：true。")
                .define("generateBedrockOres", true);
        BEDROCK_ORE_SPAWN_RATE = builder
                .comment("通用基岩矿生成间隔，数值越大越稀有。HBM 1.7.10 默认：10。")
                .defineInRange("bedrockOreSpawnRate", 10, 1, 1_000_000);
        BEDROCK_ORE_NETHER_GLOWSTONE_WEIGHT = builder
                .comment("下界荧石通用基岩矿的生成权重。HBM 1.7.10 默认：100。")
                .defineInRange("bedrockOreNetherGlowstoneWeight", 100, 0, 1_000_000);
        BEDROCK_ORE_NETHER_PHOSPHORUS_WEIGHT = builder
                .comment("下界磷通用基岩矿的生成权重。HBM 1.7.10 默认：50。")
                .defineInRange("bedrockOreNetherPhosphorusWeight", 50, 0, 1_000_000);
        BEDROCK_ORE_NETHER_QUARTZ_WEIGHT = builder
                .comment("下界石英通用基岩矿的生成权重。HBM 1.7.10 默认：100。")
                .defineInRange("bedrockOreNetherQuartzWeight", 100, 0, 1_000_000);
        METEORITE_SPAWN = builder
                .comment("Fallen meteorite worldgen interval in chunks. Matches HBM 1.7.10 default: 200.")
                .defineInRange("meteoriteSpawnRate", 200, 1, 1_000_000);

        builder.pop();

        builder.push("geysers");
        CHLORINE_GEYSER_SPAWN_RATE = builder
                .comment("氯气间歇泉生成间隔，数值表示平均每多少个平原区块尝试生成一次；设置为 0 可禁用。默认与 HBM 1.7.10 一致：3000。")
                .defineInRange("chlorineGeyserSpawnRate", 3000, 0, 1_000_000);
        builder.pop();

        builder.push("meteors");
        ENABLE_METEOR_STRIKES = builder
                .comment("Enable falling meteor strikes. Matches HBM 1.7.10 default: true.")
                .define("enableMeteorStrikes", true);
        ENABLE_METEOR_SHOWERS = builder
                .comment("Enable meteor showers. Matches HBM 1.7.10 default: true.")
                .define("enableMeteorShowers", true);
        ENABLE_METEOR_TAILS = builder
                .comment("Enable falling meteor tail particles and impact trail effects. Matches HBM 1.7.10 default: true.")
                .define("enableMeteorTails", true);
        ENABLE_SPECIAL_METEORS = builder
                .comment("Enable rare special meteorite variants. Matches HBM 1.7.10 default: true.")
                .define("enableSpecialMeteors", true);
        METEOR_STRIKE_CHANCE = builder
                .comment("Average falling meteor spawn interval in ticks. Matches HBM 1.7.10 default: 360000.")
                .defineInRange("meteorStrikeChance", 20 * 60 * 60 * 5, 1, Integer.MAX_VALUE / 100);
        METEOR_SHOWER_CHANCE = builder
                .comment("Average falling meteor spawn interval during a meteor shower, in ticks. Matches HBM 1.7.10 default: 18000.")
                .defineInRange("meteorShowerChance", 20 * 60 * 15, 1, Integer.MAX_VALUE);
        METEOR_SHOWER_DURATION = builder
                .comment("Maximum meteor shower duration in ticks. Matches HBM 1.7.10 default: 36000.")
                .defineInRange("meteorShowerDuration", 20 * 60 * 30, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.push("structures");
        GENERATE_HBM_STRUCTURES = builder
                .comment("是否生成 HBM 旧版结构。")
                .define("generateHbmStructures", true);
        HBM_STRUCTURE_MIN_CHUNKS = builder
                .comment("HBM 结构最小区块间距。HBM 1.12.2 默认：4。")
                .defineInRange("hbmStructureMinChunks", 4, 0, 1024);
        HBM_STRUCTURE_MAX_CHUNKS = builder
                .comment("HBM 结构最大区块间距。HBM 1.12.2 默认：12。")
                .defineInRange("hbmStructureMaxChunks", 12, 1, 1024);
        HBM_STRUCTURE_ENABLE_RUINS = builder
                .comment("是否生成 NTM 废墟 A-J。HBM 1.12.2 默认：true。")
                .define("hbmStructureEnableRuins", true);
        HBM_STRUCTURE_ENABLE_OCEAN = builder
                .comment("是否生成海洋结构。HBM 1.12.2 默认：true。")
                .define("hbmStructureEnableOcean", true);

        HBM_STRUCTURE_SPIRE_WEIGHT = weight(builder, "spireSpawnWeight", "尖塔结构生成权重。", 2);
        HBM_STRUCTURE_VERTIBIRD_WEIGHT = weight(builder, "vertibirdSpawnWeight", "飞鸟直升机结构生成权重。", 6);
        HBM_STRUCTURE_CRASHED_VERTIBIRD_WEIGHT = weight(builder, "crashedVertibirdSpawnWeight", "坠毁飞鸟直升机结构生成权重。", 10);
        HBM_STRUCTURE_AIRCRAFT_CARRIER_WEIGHT = weight(builder, "aircraftCarrierSpawnWeight", "航空母舰结构生成权重。", 3);
        HBM_STRUCTURE_OIL_RIG_WEIGHT = weight(builder, "oilRigSpawnWeight", "钻油平台结构生成权重。", 5);
        HBM_STRUCTURE_LIGHTHOUSE_WEIGHT = weight(builder, "lighthouseSpawnWeight", "灯塔结构生成权重。", 1);
        HBM_STRUCTURE_BEACHED_PATROL_WEIGHT = weight(builder, "beachedPatrolSpawnWeight", "搁浅巡逻艇结构生成权重。", 15);
        HBM_STRUCTURE_DISH_WEIGHT = weight(builder, "dishSpawnWeight", "碟形天线结构生成权重。", 10);
        HBM_STRUCTURE_FOREST_CHEM_WEIGHT = weight(builder, "forestChemSpawnWeight", "森林化工结构生成权重。", 30);
        HBM_STRUCTURE_PLANE1_WEIGHT = weight(builder, "plane1SpawnWeight", "坠毁飞机 1 结构生成权重。", 25);
        HBM_STRUCTURE_PLANE2_WEIGHT = weight(builder, "plane2SpawnWeight", "坠毁飞机 2 结构生成权重。", 25);
        HBM_STRUCTURE_DESERT_SHACK1_WEIGHT = weight(builder, "desertShack1SpawnWeight", "沙漠棚屋 1 结构生成权重。", 18);
        HBM_STRUCTURE_DESERT_SHACK2_WEIGHT = weight(builder, "desertShack2SpawnWeight", "沙漠棚屋 2 结构生成权重。", 20);
        HBM_STRUCTURE_DESERT_SHACK3_WEIGHT = weight(builder, "desertShack3SpawnWeight", "沙漠棚屋 3 结构生成权重。", 22);
        HBM_STRUCTURE_LABORATORY_WEIGHT = weight(builder, "laboratorySpawnWeight", "实验室结构生成权重。", 20);
        HBM_STRUCTURE_FOREST_POST_WEIGHT = weight(builder, "forestPostSpawnWeight", "森林哨站结构生成权重。", 30);
        HBM_STRUCTURE_FACTORY_WEIGHT = weight(builder, "factorySpawnWeight", "工厂结构生成权重。", 40);
        HBM_STRUCTURE_CRANE_WEIGHT = weight(builder, "craneSpawnWeight", "起重机结构生成权重。", 20);
        HBM_STRUCTURE_BROADCASTING_TOWER_WEIGHT = weight(builder, "broadcastingTowerSpawnWeight", "广播塔结构生成权重。", 25);
        HBM_STRUCTURE_RADIO_WEIGHT = weight(builder, "radioSpawnWeight", "无线电结构生成权重。", 25);
        HBM_STRUCTURE_FEATURES_WEIGHT = weight(builder, "featuresSpawnWeight", "地表特征结构生成权重。", 50);
        HBM_STRUCTURE_BUNKER_WEIGHT = weight(builder, "bunkerSpawnWeight", "地堡结构生成权重。", 6);
        HBM_STRUCTURE_METEOR_DUNGEON_WEIGHT = weight(builder, "meteorDungeonSpawnWeight", "陨石地牢结构生成权重。", 1);
        HBM_STRUCTURE_RUIN_A_WEIGHT = weight(builder, "ruinASpawnWeight", "废墟 A 生成权重。", 10);
        HBM_STRUCTURE_RUIN_B_WEIGHT = weight(builder, "ruinBSpawnWeight", "废墟 B 生成权重。", 12);
        HBM_STRUCTURE_RUIN_C_WEIGHT = weight(builder, "ruinCSpawnWeight", "废墟 C 生成权重。", 12);
        HBM_STRUCTURE_RUIN_D_WEIGHT = weight(builder, "ruinDSpawnWeight", "废墟 D 生成权重。", 12);
        HBM_STRUCTURE_RUIN_E_WEIGHT = weight(builder, "ruinESpawnWeight", "废墟 E 生成权重。", 12);
        HBM_STRUCTURE_RUIN_F_WEIGHT = weight(builder, "ruinFSpawnWeight", "废墟 F 生成权重。", 12);
        HBM_STRUCTURE_RUIN_G_WEIGHT = weight(builder, "ruinGSpawnWeight", "废墟 G 生成权重。", 12);
        HBM_STRUCTURE_RUIN_H_WEIGHT = weight(builder, "ruinHSpawnWeight", "废墟 H 生成权重。", 12);
        HBM_STRUCTURE_RUIN_I_WEIGHT = weight(builder, "ruinISpawnWeight", "废墟 I 生成权重。", 12);
        HBM_STRUCTURE_RUIN_J_WEIGHT = weight(builder, "ruinJSpawnWeight", "废墟 J 生成权重。", 12);
        HBM_STRUCTURE_PLAINS_NULL_WEIGHT = weight(builder, "plainsNullWeight", "平原结构空结果权重。", 20);
        HBM_STRUCTURE_OCEAN_NULL_WEIGHT = weight(builder, "oceanNullWeight", "海洋结构空结果权重。", 35);
        ANCIENT_TOMB_SPAWN_CHANCE = builder
                .comment("古代墓穴生成概率，旧版 HBM 1.7.10 默认每 4000 个区块尝试一次；设置为 0 可禁用。")
                .defineInRange("ancientTombSpawnChance", 4000, 0, Integer.MAX_VALUE);

        builder.pop();
        builder.pop();

        SPEC = builder.build();
    }

    private HbmConfig() {
    }

    private static ModConfigSpec.IntValue weight(ModConfigSpec.Builder builder, String name, String comment, int defaultValue) {
        return builder
                .comment(comment + " HBM 1.12.2 默认：" + defaultValue + "。设置为 0 可禁用该结构。")
                .defineInRange(name, defaultValue, 0, 1_000_000);
    }

    public static int radius(ModConfigSpec.IntValue minValue, ModConfigSpec.IntValue maxValue, RandomSource random) {
        int min = Math.min(minValue.get(), maxValue.get());
        int max = Math.max(minValue.get(), maxValue.get());
        return min + random.nextInt(max - min + 1);
    }

    /** Mirrors VersatileConfig's 1.7.10 RTG rule: 528 mode forces decay. */
    public static boolean rtgDecay() {
        return ENABLE_528_MODE.get() || ENABLE_RTG_DECAY.get();
    }

    /** Mirrors VersatileConfig's 1.7.10 RTG rule: 528 mode forces output scaling. */
    public static boolean scaleRtgPower() {
        return ENABLE_528_MODE.get() || SCALE_RTG_POWER.get();
    }

    public static boolean glyphidNaturalScoutSpawn() {
        return GLYPHID_RAMPANT_MODE.get() || GLYPHID_NATURAL_SCOUT_SPAWN.get();
    }

    public static boolean glyphidExtendedTargeting() {
        return GLYPHID_RAMPANT_MODE.get() || GLYPHID_RAMPANT_EXTENDED_TARGETING.get();
    }

    public static boolean glyphidDig() {
        return GLYPHID_RAMPANT_MODE.get() || GLYPHID_RAMPANT_DIG.get();
    }

    public static boolean glyphidGuidance() {
        return GLYPHID_RAMPANT_MODE.get() || GLYPHID_RAMPANT_GUIDANCE.get();
    }

    public static int glyphidScoutSwarmChance() {
        return GLYPHID_RAMPANT_MODE.get() ? 1 : GLYPHID_SCOUT_SWARM_CHANCE.get();
    }

    public static double glyphidScoutSootThreshold() {
        return GLYPHID_RAMPANT_MODE.get() ? 0.1D : GLYPHID_SCOUT_SOOT_THRESHOLD.get();
    }

    /** Rampant mode raises pollution output exactly as MobConfig did in 1.7.10. */
    public static double pollutionMultiplier() {
        double configured = POLLUTION_MULTIPLIER.get();
        return GLYPHID_RAMPANT_MODE.get() && configured == 1.0D ? 3.0D : configured;
    }

    public static double smokestackPollutionModifier(boolean industrial) {
        if (GLYPHID_RAMPANT_MODE.get()) {
            double override = GLYPHID_RAMPANT_SMOKESTACK_OVERRIDE.get();
            return industrial ? override / 2.0D : override;
        }
        return industrial ? 0.1D : 0.25D;
    }
}
