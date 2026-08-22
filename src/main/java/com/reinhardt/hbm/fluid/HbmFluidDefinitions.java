package com.reinhardt.hbm.fluid;

import java.util.EnumSet;
import java.util.List;

public final class HbmFluidDefinitions {
    private static final List<HbmFluidDefinition> DEFINITIONS = List.of(
            definition(0, 0, "NONE", "none", 0x888888, 0, 0, 0, HbmFluidSymbol.NONE, 20, "NO_FORGE", "", ""),
            definition(152, 1, "AIR", "air", 0xE7EAEB, 0, 0, 0, HbmFluidSymbol.NONE, 20, "GASEOUS", "", ""),
            definition(159, 1, "AIRBLAST", "airblast", 0xFFDADA, 0, 3, 0, HbmFluidSymbol.NONE, 1200, "GASEOUS", "", ""),
            definition(1, 2, "WATER", "water", 0x3333FF, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|UNSIPHONABLE", "", ""),
            definition(60, 3, "HEAVYWATER", "heavywater", 0x00a0b0, 1, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(125, 4, "HEAVYWATER_HOT", "heavywater_hot", 0x4D007B, 1, 0, 0, HbmFluidSymbol.NONE, 600, "LIQUID|VISCOUS", "", ""),
            definition(7, 5, "LAVA", "lava", 0xFF3300, 4, 0, 0, HbmFluidSymbol.NOWATER, 1200, "LIQUID|VISCOUS", "", ""),
            definition(2, 6, "STEAM", "steam", 0xe5e5e5, 3, 0, 0, HbmFluidSymbol.NONE, 100, "GASEOUS|UNSIPHONABLE", "", ""),
            definition(3, 7, "HOTSTEAM", "hotsteam", 0xE7D6D6, 4, 0, 0, HbmFluidSymbol.NONE, 300, "GASEOUS|UNSIPHONABLE", "", ""),
            definition(4, 8, "SUPERHOTSTEAM", "superhotsteam", 0xE7B7B7, 4, 0, 0, HbmFluidSymbol.NONE, 450, "GASEOUS|UNSIPHONABLE", "", ""),
            definition(5, 9, "ULTRAHOTSTEAM", "ultrahotsteam", 0xE39393, 4, 0, 0, HbmFluidSymbol.NONE, 600, "GASEOUS|UNSIPHONABLE", "", ""),
            definition(48, 10, "SPENTSTEAM", "spentsteam", 0x445772, 2, 0, 0, HbmFluidSymbol.NONE, 20, "GASEOUS|NOCON", "", ""),
            definition(160, 10, "FLUE", "flue", 0x4D4D4D, 2, 0, 0, HbmFluidSymbol.NONE, 120, "GASEOUS|NOCON", "", ""),
            definition(161, 10, "CORIUM_FLUID", "corium_fluid", 0xFF5A00, 4, 0, 4, HbmFluidSymbol.RADIATION, 1500, "LIQUID|VISCOUS|NOCON|RADIATION:5", "", ""),
            definition(162, 11, "VOLCANIC_LAVA_FLUID", "volcanic_lava_fluid", 0xFF3300, 4, 0, 0, HbmFluidSymbol.NOWATER, 1300, "LIQUID|VISCOUS|NOCON", "", ""),
            definition(163, 12, "MUD_FLUID", "mud_fluid", 0x5B5A32, 4, 0, 2, HbmFluidSymbol.ACID, 2500, "LIQUID|VISCOUS|POISON:true:4", "", ""),
            definition(164, 13, "ACID_FLUID", "acid_fluid", 0xB5D600, 5, 0, 5, HbmFluidSymbol.ACID, 2500, "LIQUID|VISCOUS|CORROSIVE:100", "", ""),
            definition(165, 14, "TOXIC_FLUID", "toxic_fluid", 0x43FF21, 5, 0, 4, HbmFluidSymbol.RADIATION, 2500, "LIQUID|VISCOUS|RADIATION:1|POISON:true:4", "", ""),
            definition(166, 15, "RAD_LAVA_FLUID", "rad_lava_fluid", 0xB6FF00, 5, 0, 5, HbmFluidSymbol.RADIATION, 1300, "LIQUID|VISCOUS|NOCON|RADIATION:5", "", ""),
            definition(55, 11, "CARBONDIOXIDE", "carbondioxide", 0x404040, 3, 0, 0, HbmFluidSymbol.ASPHYXIANT, 20, "GASEOUS", "", ""),
            definition(6, 12, "COOLANT", "coolant", 0xd8fcff, 1, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(76, 13, "COOLANT_HOT", "coolant_hot", 0x99525E, 1, 0, 0, HbmFluidSymbol.NONE, 600, "LIQUID", "", ""),
            definition(145, 14, "PERFLUOROMETHYL", "perfluoromethyl", 0xBDC8DC, 1, 0, 1, HbmFluidSymbol.NONE, 15, "LIQUID", "", ""),
            definition(146, 15, "PERFLUOROMETHYL_COLD", "perfluoromethyl_cold", 0x99DADE, 1, 0, 1, HbmFluidSymbol.NONE, -150, "LIQUID", "", ""),
            definition(147, 16, "PERFLUOROMETHYL_HOT", "perfluoromethyl_hot", 0xB899DE, 1, 0, 1, HbmFluidSymbol.NONE, 250, "LIQUID", "", ""),
            definition(37, 17, "CRYOGEL", "cryogel", 0x32ffff, 2, 0, 0, HbmFluidSymbol.CROYGENIC, -170, "LIQUID|VISCOUS", "", ""),
            definition(77, 18, "MUG", "mug", 0x4B2D28, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|DELICIOUS", "", ""),
            definition(78, 19, "MUG_HOT", "mug_hot", 0x6B2A20, 0, 0, 0, HbmFluidSymbol.NONE, 500, "LIQUID|DELICIOUS", "", ""),
            definition(84, 20, "BLOOD", "blood", 0xB22424, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|DELICIOUS", "", ""),
            definition(85, 21, "BLOOD_HOT", "blood_hot", 0xF22419, 3, 0, 0, HbmFluidSymbol.NONE, 666, "LIQUID|VISCOUS", "", ""),
            definition(126, 22, "SODIUM", "sodium", 0xCCD4D5, 1, 2, 3, HbmFluidSymbol.NONE, 400, "LIQUID|VISCOUS", "", ""),
            definition(127, 23, "SODIUM_HOT", "sodium_hot", 0xE2ADC1, 1, 2, 3, HbmFluidSymbol.NONE, 1200, "LIQUID|VISCOUS", "", ""),
            definition(143, 24, "LEAD", "lead", 0x666672, 4, 0, 0, HbmFluidSymbol.NONE, 350, "LIQUID|VISCOUS", "", ""),
            definition(144, 25, "LEAD_HOT", "lead_hot", 0x776563, 4, 0, 0, HbmFluidSymbol.NONE, 1500, "LIQUID|VISCOUS", "", ""),
            definition(128, 26, "THORIUM_SALT", "thorium_salt", 0x7A5542, 2, 0, 3, HbmFluidSymbol.NONE, 800, "LIQUID|VISCOUS|CORROSIVE:65", "", ""),
            definition(129, 27, "THORIUM_SALT_HOT", "thorium_salt_hot", 0x3E3627, 2, 0, 3, HbmFluidSymbol.NONE, 1600, "LIQUID|VISCOUS|CORROSIVE:65", "", ""),
            definition(130, 28, "THORIUM_SALT_DEPLETED", "thorium_salt_depleted", 0x302D1C, 2, 0, 3, HbmFluidSymbol.NONE, 800, "LIQUID|VISCOUS|CORROSIVE:65", "", ""),
            definition(38, 29, "HYDROGEN", "hydrogen", 0x4286f4, 3, 4, 0, HbmFluidSymbol.CROYGENIC, -260, "LIQUID|EVAP|FLAMMABLE:5000|COMBUSTIBLE:HIGH:10000", "", "0x4286f4/0xffffff"),
            definition(8, 30, "DEUTERIUM", "deuterium", 0x0000FF, 3, 4, 0, HbmFluidSymbol.NONE, 20, "GASEOUS|FLAMMABLE:5000|COMBUSTIBLE:HIGH:10000", "", "0x0000FF/0xFFFFFF"),
            definition(9, 31, "TRITIUM", "tritium", 0x000099, 3, 4, 0, HbmFluidSymbol.RADIATION, 20, "GASEOUS|FLAMMABLE:5000|COMBUSTIBLE:HIGH:10000|RADIATION:0.001", "", "0x000099/0xE9FFAA"),
            definition(57, 32, "HELIUM3", "helium3", 0xFCF0C4, 0, 0, 0, HbmFluidSymbol.ASPHYXIANT, 20, "GASEOUS", "", "0xFD631F/0xffffff"),
            definition(124, 33, "HELIUM4", "helium4", 0xE54B0A, 0, 0, 0, HbmFluidSymbol.ASPHYXIANT, 20, "GASEOUS", "", "0xFD631F/0xffff00"),
            definition(39, 34, "OXYGEN", "oxygen", 0x98bdf9, 3, 0, 0, HbmFluidSymbol.CROYGENIC, -100, "LIQUID|EVAP", "", "0x98bdf9/0xffffff"),
            definition(40, 35, "XENON", "xenon", 0xba45e8, 0, 0, 0, HbmFluidSymbol.ASPHYXIANT, 20, "GASEOUS", "", "0x8C21FF/0x303030"),
            definition(90, 36, "CHLORINE", "chlorine", 0xBAB572, 3, 0, 0, HbmFluidSymbol.OXIDIZER, 20, "GASEOUS|CORROSIVE:25", "", "0xBAB572/0x887B34"),
            definition(42, 37, "MERCURY", "mercury", 0x808080, 2, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|POISON:false:2", "", ""),
            definition(10, 38, "OIL", "oil", 0x020202, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|FLAMMABLE:10000", "0x424242", ""),
            definition(134, 39, "OIL_DS", "oil_ds", 0x121212, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "0x424242", ""),
            definition(61, 40, "CRACKOIL", "crackoil", 0x020202, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|FLAMMABLE:10000", "0x424242", ""),
            definition(136, 41, "CRACKOIL_DS", "crackoil_ds", 0x2A1C11, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "0x424242", ""),
            definition(62, 42, "COALOIL", "coaloil", 0x020202, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|FLAMMABLE:10000", "0x424242", ""),
            definition(105, 43, "OIL_COKER", "oil_coker", 0x001802, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "", ""),
            definition(11, 44, "HOTOIL", "hotoil", 0x300900, 2, 3, 0, HbmFluidSymbol.NONE, 350, "LIQUID|VISCOUS", "", ""),
            definition(135, 45, "HOTOIL_DS", "hotoil_ds", 0x3F180F, 2, 3, 0, HbmFluidSymbol.NONE, 350, "LIQUID|VISCOUS", "", ""),
            definition(63, 46, "HOTCRACKOIL", "hotcrackoil", 0x300900, 2, 3, 0, HbmFluidSymbol.NONE, 350, "LIQUID|VISCOUS", "", ""),
            definition(137, 47, "HOTCRACKOIL_DS", "hotcrackoil_ds", 0x3A1A28, 2, 3, 0, HbmFluidSymbol.NONE, 350, "LIQUID|VISCOUS", "", ""),
            definition(12, 48, "HEAVYOIL", "heavyoil", 0x141312, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|FLAMMABLE:50000|COMBUSTIBLE:LOW:25000", "0x513F39", ""),
            definition(91, 49, "HEAVYOIL_VACUUM", "heavyoil_vacuum", 0x131214, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "0x513F39", ""),
            definition(19, 50, "NAPHTHA", "naphtha", 0x595744, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|FLAMMABLE:125000|COMBUSTIBLE:MEDIUM:200000", "0x5F6D44", ""),
            definition(138, 51, "NAPHTHA_DS", "naphtha_ds", 0x63614E, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "0x5F6D44", ""),
            definition(64, 52, "NAPHTHA_CRACK", "naphtha_crack", 0x595744, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|FLAMMABLE:125000|COMBUSTIBLE:MEDIUM:200000", "0x5F6D44", ""),
            definition(106, 53, "NAPHTHA_COKER", "naphtha_coker", 0x495944, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "", ""),
            definition(92, 54, "REFORMATE", "reformate", 0x835472, 2, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "0xD180D6", ""),
            definition(21, 55, "LIGHTOIL", "lightoil", 0x8c7451, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:200000|COMBUSTIBLE:MEDIUM:500000", "0xB46B52", ""),
            definition(139, 56, "LIGHTOIL_DS", "lightoil_ds", 0x63543E, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "0xB46B52", ""),
            definition(65, 57, "LIGHTOIL_CRACK", "lightoil_crack", 0x8c7451, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:200000|COMBUSTIBLE:MEDIUM:500000", "0xB46B52", ""),
            definition(93, 58, "LIGHTOIL_VACUUM", "lightoil_vacuum", 0x8C8851, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "0xB46B52", ""),
            definition(13, 59, "BITUMEN", "bitumen", 0x1f2426, 2, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "0x5A5877", ""),
            definition(14, 60, "SMEAR", "smear", 0x190f01, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|FLAMMABLE:50000", "0x624F3B", ""),
            definition(15, 61, "HEATINGOIL", "heatingoil", 0x211806, 2, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|FLAMMABLE:150000|COMBUSTIBLE:LOW:100000", "0x694235", ""),
            definition(96, 62, "HEATINGOIL_VACUUM", "heatingoil_vacuum", 0x211D06, 2, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "0x694235", ""),
            definition(16, 63, "RECLAIMED", "reclaimed", 0x332b22, 2, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|FLAMMABLE:100000|COMBUSTIBLE:LOW:200000", "0xF65723", ""),
            definition(18, 64, "LUBRICANT", "lubricant", 0x606060, 2, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "0xF1CC05", ""),
            definition(23, 65, "GAS", "gas", 0xfffeed, 1, 4, 1, HbmFluidSymbol.NONE, 20, "GASEOUS|FLAMMABLE:10000", "", "0xFF4545/0xFFE97F"),
            definition(107, 66, "GAS_COKER", "gas_coker", 0xDEF4CA, 1, 4, 0, HbmFluidSymbol.NONE, 20, "GASEOUS", "", ""),
            definition(24, 67, "PETROLEUM", "petroleum", 0x7cb7c9, 1, 4, 1, HbmFluidSymbol.NONE, 20, "GASEOUS|FLAMMABLE:25000", "", "0x5E7CFF/0xFFE97F"),
            definition(94, 68, "SOURGAS", "sourgas", 0xC9BE0D, 4, 4, 0, HbmFluidSymbol.ACID, 20, "GASEOUS|CORROSIVE:10|POISON:false:1", "", "0xC9BE0D/0x303030"),
            definition(25, 69, "LPG", "lpg", 0x4747EA, 1, 3, 1, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:200000|COMBUSTIBLE:HIGH:400000", "", ""),
            definition(86, 70, "SYNGAS", "syngas", 0x131313, 1, 4, 2, HbmFluidSymbol.NONE, 20, "GASEOUS", "", "0xFFFFFF/0x131313"),
            definition(87, 71, "OXYHYDROGEN", "oxyhydrogen", 0x483FC1, 0, 4, 2, HbmFluidSymbol.NONE, 20, "GASEOUS", "", ""),
            definition(67, 72, "AROMATICS", "aromatics", 0x68A09A, 1, 4, 1, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|FLAMMABLE:25000", "", "0x68A09A/0xEDCF27"),
            definition(68, 73, "UNSATURATEDS", "unsaturateds", 0x628FAE, 1, 4, 1, HbmFluidSymbol.NONE, 20, "GASEOUS|FLAMMABLE:1000000", "", "0x628FAE/0xEDCF27"),
            definition(95, 74, "XYLENE", "xylene", 0x5C4E76, 2, 3, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "0xA380D6", ""),
            definition(100, 75, "REFORMGAS", "reformgas", 0x6362AE, 1, 4, 1, HbmFluidSymbol.NONE, 20, "GASEOUS", "", "0x9392FF/0xFFB992"),
            definition(20, 76, "DIESEL", "diesel", 0xf2eed5, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:200000|COMBUSTIBLE:HIGH:500000", "0xFF2C2C", ""),
            definition(97, 77, "DIESEL_REFORM", "diesel_reform", 0xCDC3C6, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "0xFFC500", ""),
            definition(66, 78, "DIESEL_CRACK", "diesel_crack", 0xf2eed5, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:200000|COMBUSTIBLE:HIGH:450000", "0xFF2C2C", ""),
            definition(98, 79, "DIESEL_CRACK_REFORM", "diesel_crack_reform", 0xCDC3CC, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "0xFFC500", ""),
            definition(22, 80, "KEROSENE", "kerosene", 0xffa5d2, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:300000|COMBUSTIBLE:AERO:1250000", "0xFF377D", ""),
            definition(99, 81, "KEROSENE_REFORM", "kerosene_reform", 0xFFA5F3, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "0xFF377D", ""),
            definition(17, 82, "PETROIL", "petroil", 0x44413d, 1, 3, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:125000|COMBUSTIBLE:MEDIUM:300000", "0x2369F6", ""),
            definition(72, 83, "PETROIL_LEADED", "petroil_leaded", 0x44413d, 1, 3, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:125000|COMBUSTIBLE:MEDIUM:450000", "0x2331F6", ""),
            definition(46, 84, "GASOLINE", "gasoline", 0x445772, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:400000|COMBUSTIBLE:HIGH:1000000", "0x2F7747", ""),
            definition(73, 85, "GASOLINE_LEADED", "gasoline_leaded", 0x445772, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:400000|COMBUSTIBLE:HIGH:1500000", "0x2F775A", ""),
            definition(47, 86, "COALGAS", "coalgas", 0x445772, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:75000|COMBUSTIBLE:MEDIUM:150000", "0x2E155F", ""),
            definition(74, 87, "COALGAS_LEADED", "coalgas_leaded", 0x445772, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:75000|COMBUSTIBLE:MEDIUM:250000", "0x1E155F", ""),
            definition(80, 88, "COALCREOSOTE", "coalcreosote", 0x51694F, 3, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "0x285A3F", ""),
            definition(79, 89, "WOODOIL", "woodoil", 0x847D54, 2, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "0xBF7E4F", ""),
            definition(26, 90, "BIOGAS", "biogas", 0xbfd37c, 1, 4, 1, HbmFluidSymbol.NONE, 20, "GASEOUS|FLAMMABLE:25000", "", "0xC8FF1F/0x303030"),
            definition(27, 91, "BIOFUEL", "biofuel", 0xeef274, 1, 2, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:150000|COMBUSTIBLE:HIGH:400000", "0x9EB623", ""),
            definition(59, 92, "ETHANOL", "ethanol", 0xe0ffff, 2, 3, 0, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:75000|COMBUSTIBLE:HIGH:200000", "0xEAFFF3", ""),
            definition(111, 93, "FISHOIL", "fishoil", 0x4B4A45, 0, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(112, 94, "SUNFLOWEROIL", "sunfloweroil", 0xCBAD45, 0, 1, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(28, 95, "NITAN", "nitan", 0x8018ad, 2, 4, 1, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:2000000|COMBUSTIBLE:HIGH:5000000", "0x6B238C", ""),
            definition(154, 96, "DHC", "dhc", 0xD2AFFF, 0, 0, 0, HbmFluidSymbol.NONE, 20, "GASEOUS", "", ""),
            definition(41, 97, "BALEFIRE", "balefire", 0x28e02e, 4, 4, 3, HbmFluidSymbol.RADIATION, 1500, "LIQUID|VISCOUS|FLAMMABLE:1000000|COMBUSTIBLE:HIGH:2500000|CORROSIVE:50", "", ""),
            definition(69, 98, "SALIENT", "salient", 0x457F2D, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|DELICIOUS", "", ""),
            definition(81, 99, "SEEDSLURRY", "seedslurry", 0x7CC35E, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "0x7CC35E", ""),
            definition(101, 100, "COLLOID", "colloid", 0x787878, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "", ""),
            definition(141, 101, "VITRIOL", "vitriol", 0x6E5222, 2, 0, 1, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "", ""),
            definition(142, 102, "SLOP", "slop", 0x929D45, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "", ""),
            definition(104, 103, "IONGEL", "iongel", 0xB8FFFF, 1, 0, 4, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "", ""),
            definition(35, 104, "PEROXIDE", "peroxide", 0xfff7aa, 3, 0, 3, HbmFluidSymbol.OXIDIZER, 20, "LIQUID|CORROSIVE:40", "", ""),
            definition(75, 105, "SULFURIC_ACID", "sulfuric_acid", 0xB0AA64, 3, 0, 2, HbmFluidSymbol.ACID, 20, "LIQUID|CORROSIVE:50", "", ""),
            definition(82, 106, "NITRIC_ACID", "nitric_acid", 0xBB7A1E, 3, 0, 2, HbmFluidSymbol.OXIDIZER, 20, "LIQUID|CORROSIVE:60", "", ""),
            definition(83, 107, "SOLVENT", "solvent", 0xE4E3EF, 2, 3, 0, HbmFluidSymbol.NONE, 20, "LIQUID|CORROSIVE:30", "0xE4E3EF", ""),
            definition(88, 108, "RADIOSOLVENT", "radiosolvent", 0xA4D7DD, 3, 3, 0, HbmFluidSymbol.NONE, 20, "LIQUID|CORROSIVE:50", "", ""),
            definition(32, 109, "SCHRABIDIC", "schrabidic", 0x006B6B, 5, 0, 5, HbmFluidSymbol.ACID, 20, "LIQUID|CORROSIVE:75|RADIATION:1|POISON:true:2", "", ""),
            definition(29, 110, "UF6", "uf6", 0xD1CEBE, 4, 0, 2, HbmFluidSymbol.RADIATION, 20, "GASEOUS|CORROSIVE:15|RADIATION:0.2", "", ""),
            definition(155, 111, "LEUF6", "leuf6", 0xC8D5A6, 4, 0, 2, HbmFluidSymbol.RADIATION, 20, "GASEOUS|CORROSIVE:15|RADIATION:0.2", "", ""),
            definition(156, 112, "MEUF6", "meuf6", 0xD4DD8A, 4, 0, 2, HbmFluidSymbol.RADIATION, 20, "GASEOUS|CORROSIVE:15|RADIATION:0.2", "", ""),
            definition(157, 113, "HEUF6", "heuf6", 0xE2E870, 4, 0, 2, HbmFluidSymbol.RADIATION, 20, "GASEOUS|CORROSIVE:15|RADIATION:0.2", "", ""),
            definition(30, 114, "PUF6", "puf6", 0x4C4C4C, 4, 0, 4, HbmFluidSymbol.RADIATION, 20, "GASEOUS|CORROSIVE:15|RADIATION:0.1", "", ""),
            definition(31, 112, "SAS3", "sas3", 0x4ffffc, 5, 0, 4, HbmFluidSymbol.RADIATION, 20, "LIQUID|CORROSIVE:30|RADIATION:1", "", ""),
            definition(43, 113, "PAIN", "pain", 0x938541, 2, 0, 1, HbmFluidSymbol.ACID, 300, "LIQUID|VISCOUS|CORROSIVE:30|POISON:true:2", "", ""),
            definition(58, 114, "DEATH", "death", 0x717A88, 2, 0, 1, HbmFluidSymbol.ACID, 300, "LIQUID|VISCOUS|LEADCON|CORROSIVE:80|POISON:true:4", "", ""),
            definition(36, 115, "WATZ", "watz", 0x86653E, 4, 0, 3, HbmFluidSymbol.ACID, 20, "LIQUID|VISCOUS|CORROSIVE:60|RADIATION:0.1", "", ""),
            definition(158, 116, "WATZ_HEAVY", "watz_heavy", 0x4D352C, 4, 0, 3, HbmFluidSymbol.ACID, 20, "LIQUID|VISCOUS|CORROSIVE:60|RADIATION:0.2", "", ""),
            definition(114, 116, "REDMUD", "redmud", 0xD85638, 3, 0, 4, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS|LEADCON|FLAMMABLE:1000|CORROSIVE:60", "", ""),
            definition(131, 117, "FULLERENE", "fullerene", 0xFF7FED, 3, 3, 3, HbmFluidSymbol.NONE, 20, "LIQUID|CORROSIVE:65", "", ""),
            definition(108, 118, "EGG", "egg", 0xD2C273, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(109, 119, "CHOLESTEROL", "cholesterol", 0xD6D2BD, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(115, 120, "CHLOROCALCITE_SOLUTION", "chlorocalcite_solution", 0x808080, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|NOCON|CORROSIVE:60", "", ""),
            definition(116, 121, "CHLOROCALCITE_MIX", "chlorocalcite_mix", 0x808080, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|NOCON|CORROSIVE:60", "", ""),
            definition(117, 122, "CHLOROCALCITE_CLEANED", "chlorocalcite_cleaned", 0x808080, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|NOCON|CORROSIVE:60", "", ""),
            definition(118, 123, "POTASSIUM_CHLORIDE", "potassium_chloride", 0x808080, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|NOCON|CORROSIVE:60", "", ""),
            definition(119, 124, "CALCIUM_CHLORIDE", "calcium_chloride", 0x808080, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|NOCON|CORROSIVE:60", "", ""),
            definition(120, 125, "CALCIUM_SOLUTION", "calcium_solution", 0x808080, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|NOCON|CORROSIVE:60", "", ""),
            definition(149, 126, "SODIUM_ALUMINATE", "sodium_aluminate", 0xFFD191, 3, 0, 1, HbmFluidSymbol.ACID, 20, "LIQUID|CORROSIVE:30", "", ""),
            definition(150, 127, "BAUXITE_SOLUTION", "bauxite_solution", 0xE2560F, 3, 0, 3, HbmFluidSymbol.ACID, 20, "LIQUID|VISCOUS|CORROSIVE:40", "", ""),
            definition(151, 128, "ALUMINA", "alumina", 0xDDFFFF, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(153, 129, "CONCRETE", "concrete", 0xA2A2A2, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(49, 130, "FRACKSOL", "fracksol", 0x798A6B, 1, 3, 3, HbmFluidSymbol.ACID, 20, "LIQUID|VISCOUS|CORROSIVE:15|POISON:false:0", "0x4F887F", ""),
            definition(148, 131, "LYE", "lye", 0xFFECCC, 3, 0, 1, HbmFluidSymbol.ACID, 20, "LIQUID|CORROSIVE:40", "", ""),
            definition(102, 132, "PHOSGENE", "phosgene", 0xCFC4A4, 4, 0, 1, HbmFluidSymbol.NONE, 20, "GASEOUS", "", "0xCFC4A4/0x361414"),
            definition(103, 133, "MUSTARDGAS", "mustardgas", 0xBAB572, 4, 1, 1, HbmFluidSymbol.NONE, 20, "GASEOUS", "", "0xBAB572/0x361414"),
            definition(110, 134, "ESTRADIOL", "estradiol", 0xCDD5D8, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(113, 135, "NITROGLYCERIN", "nitroglycerin", 0x92ACA6, 0, 4, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(33, 136, "AMAT", "amat", 0x010101, 5, 0, 5, HbmFluidSymbol.ANTIMATTER, 20, "GASEOUS|ANTI", "", ""),
            definition(34, 137, "ASCHRAB", "aschrab", 0xb50000, 5, 0, 5, HbmFluidSymbol.ANTIMATTER, 20, "GASEOUS|ANTI", "", ""),
            definition(44, 138, "WASTEFLUID", "wastefluid", 0x544400, 2, 0, 1, HbmFluidSymbol.RADIATION, 20, "LIQUID|VISCOUS|NOCON|RADIATION:0.5", "", ""),
            definition(45, 139, "WASTEGAS", "wastegas", 0xB8B8B8, 2, 0, 1, HbmFluidSymbol.RADIATION, 20, "GASEOUS|NOCON|RADIATION:0.5", "", ""),
            definition(70, 140, "XPJUICE", "xpjuice", 0xBBFF09, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID|VISCOUS", "", ""),
            definition(71, 141, "ENDERJUICE", "enderjuice", 0x127766, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(140, 142, "STELLAR_FLUX", "stellar_flux", 0xE300FF, 0, 4, 4, HbmFluidSymbol.ANTIMATTER, 20, "GASEOUS|ANTI", "", ""),
            definition(50, 143, "PLASMA_DT", "plasma_dt", 0xF7AFDE, 0, 4, 0, HbmFluidSymbol.RADIATION, 3250, "PLASMA|NOCON|NOID", "", ""),
            definition(51, 144, "PLASMA_HD", "plasma_hd", 0xF0ADF4, 0, 4, 0, HbmFluidSymbol.RADIATION, 2500, "PLASMA|NOCON|NOID", "", ""),
            definition(52, 145, "PLASMA_HT", "plasma_ht", 0xD1ABF2, 0, 4, 0, HbmFluidSymbol.RADIATION, 3000, "PLASMA|NOCON|NOID", "", ""),
            definition(56, 146, "PLASMA_DH3", "plasma_dh3", 0xFF83AA, 0, 4, 0, HbmFluidSymbol.RADIATION, 3480, "PLASMA|NOCON|NOID", "", ""),
            definition(53, 147, "PLASMA_XM", "plasma_xm", 0xC6A5FF, 0, 4, 1, HbmFluidSymbol.RADIATION, 4250, "PLASMA|NOCON|NOID", "", ""),
            definition(54, 148, "PLASMA_BF", "plasma_bf", 0xA7F1A3, 4, 5, 4, HbmFluidSymbol.ANTIMATTER, 8500, "PLASMA|NOCON|NOID", "", ""),
            definition(121, 149, "SMOKE", "smoke", 0x808080, 0, 0, 0, HbmFluidSymbol.NONE, 20, "GASEOUS|NOCON|NOID", "", ""),
            definition(122, 150, "SMOKE_LEADED", "smoke_leaded", 0x808080, 0, 0, 0, HbmFluidSymbol.NONE, 20, "GASEOUS|NOCON|NOID", "", ""),
            definition(123, 151, "SMOKE_POISON", "smoke_poison", 0x808080, 0, 0, 0, HbmFluidSymbol.NONE, 20, "GASEOUS|NOCON|NOID", "", ""),
            definition(132, 152, "PHEROMONE", "pheromone", 0x5FA6E8, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(133, 153, "PHEROMONE_M", "pheromone_m", 0x48C9B0, 0, 0, 0, HbmFluidSymbol.NONE, 20, "LIQUID", "", ""),
            definition(89, 154, "HYDRAZINE", "hydrazine", 0x31517D, 2, 3, 2, HbmFluidSymbol.NONE, 20, "LIQUID|FLAMMABLE:500000|COMBUSTIBLE:HIGH:1250000|CORROSIVE:30|ROCKET:210:277810", "0x31517D", "")
    );

    private HbmFluidDefinitions() {
    }

    public static List<HbmFluidDefinition> all() {
        return DEFINITIONS;
    }

    private static HbmFluidDefinition definition(
            int oldId,
            int niceOrder,
            String legacyField,
            String name,
            int color,
            int poison,
            int flammability,
            int reactivity,
            HbmFluidSymbol symbol,
            int temperature,
            String rawTraits,
            String canisterColor,
            String gasTankColors
    ) {
        return new HbmFluidDefinition(oldId, legacyField, name, color, poison, flammability, reactivity, symbol, temperature, parseTraits(rawTraits), rawTraits, canisterColor, gasTankColors, niceOrder);
    }

    private static EnumSet<HbmFluidTrait> parseTraits(String raw) {
        EnumSet<HbmFluidTrait> traits = EnumSet.noneOf(HbmFluidTrait.class);
        if (raw == null || raw.isBlank()) {
            return traits;
        }
        for (String token : raw.split("\\|")) {
            if (token.isBlank()) {
                continue;
            }
            String key = token.contains(":") ? token.substring(0, token.indexOf(':')) : token;
            switch (key) {
                case "LIQUID" -> traits.add(HbmFluidTrait.LIQUID);
                case "VISCOUS" -> traits.add(HbmFluidTrait.VISCOUS);
                case "GASEOUS" -> traits.add(HbmFluidTrait.GASEOUS);
                case "EVAP" -> traits.add(HbmFluidTrait.EVAPORATES);
                case "PLASMA" -> traits.add(HbmFluidTrait.PLASMA);
                case "ANTI" -> traits.add(HbmFluidTrait.ANTIMATTER);
                case "LEADCON" -> traits.add(HbmFluidTrait.LEAD_CONTAINER);
                case "NOCON" -> traits.add(HbmFluidTrait.NO_CONTAINER);
                case "NOID" -> traits.add(HbmFluidTrait.NO_IDENTIFIER);
                case "DELICIOUS" -> traits.add(HbmFluidTrait.DELICIOUS);
                case "UNSIPHONABLE" -> traits.add(HbmFluidTrait.UNSIPHONABLE);
                case "NO_FORGE" -> traits.add(HbmFluidTrait.NO_FORGE);
                case "FLAMMABLE" -> traits.add(HbmFluidTrait.FLAMMABLE);
                case "COMBUSTIBLE" -> traits.add(HbmFluidTrait.COMBUSTIBLE);
                case "CORROSIVE" -> traits.add(HbmFluidTrait.CORROSIVE);
                case "RADIATION" -> traits.add(HbmFluidTrait.RADIATING);
                case "POISON" -> traits.add(HbmFluidTrait.POISON);
                case "ROCKET" -> traits.add(HbmFluidTrait.ROCKET_FUEL);
                default -> {
                }
            }
        }
        return traits;
    }
}
