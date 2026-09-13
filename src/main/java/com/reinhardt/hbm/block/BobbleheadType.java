package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.resources.ResourceLocation;

/** Immutable 1.7.10 BlockBobble subtype table. Ordinals are the legacy item damage values. */
public enum BobbleheadType {
    NONE("none", "null", "null", null, null, false, "board_blank", "thegadget3_"),
    STRENGTH("strength", "Strength", "Strength", null, "It's essential to give your arguments impact.", false, "bridge_bios", "vaultboy"),
    PERCEPTION("perception", "Perception", "Perception", null, "Only through observation will you perceive weakness.", false, "bridge_north", "vaultboy"),
    ENDURANCE("endurance", "Endurance", "Endurance", null, "Always be ready to take one for the team.", false, "bridge_south", "vaultboy"),
    CHARISMA("charisma", "Charisma", "Charisma", null, "Nothing says pizzaz like a winning smile.", false, "bridge_io", "vaultboy"),
    INTELLIGENCE("intelligence", "Intelligence", "Intelligence", null, "It takes the smartest individuals to realize$there's always more to learn.", false, "bridge_bus", "vaultboy"),
    AGILITY("agility", "Agility", "Agility", null, "Never be afraid to dodge the sensitive issues.", false, "bridge_chipset", "vaultboy"),
    LUCK("luck", "Luck", "Luck", null, "There's only one way to give 110%.", false, "bridge_cmos", "vaultboy"),
    BOB("bob", "Robert \"The Bobcat\" Katzinsky", "HbMinecraft", "Hbm's Nuclear Tech Mod", "I know where you live, " + System.getProperty("user.name"), false, "cpu_socket", "hbm"),
    FRIZZLE("frizzle", "Frooz", "Frooz", "Weapon models", "BLOOD IS FUEL", true, "cpu_clock", "frizzle"),
    PU238("pu238", "Pu-238", "Pu-238", "Improved Tom impact mechanics", null, false, "cpu_register", "pellet"),
    VT("vt", "VT-6/24", "VT-6/24", "Balefire warhead model and general texturework", "You cannot unfuck a horse.", true, "cpu_ext", "vt"),
    DOC("doc", "The Doctor", "Doctor17PH", "Russian localization, lunar miner", "Perhaps the moon rocks were too expensive", true, "cpu_cache", "doctor17ph"),
    BLUEHAT("bluehat", "The Blue Hat", "The Blue Hat", "Textures", "payday 2's deagle freeaim champ of the year 2022", true, "mem_16k_a", "thebluehat"),
    PHEO("pheo", "Pheo", "Pheonix", "Deuterium machines, tantalium textures, Reliant Rocket", "RUN TO THE BEDROOM, ON THE SUITCASE ON THE LEFT,$YOU'LL FIND MY FAVORITE AXE", true, "mem_16k_b", "pheo"),
    ADAM29("adam29", "Adam29", "Adam29", "Ethanol, liquid petroleum gas", "You know, nukes are really quite beatiful.$It's like watching a star be born for a split second.", true, "mem_16k_c", "adam29"),
    UFFR("uffr", "UFFR", "UFFR", "All sorts of things from his PR", "fried shrimp", false, "mem_socket", "uffr"),
    VAER("vaer", "vaer", "vaer", "ZIRNOX", "taken de family out to the weekend cigarette festival", true, "mem_16k_d", "vaer"),
    NOS("nos", "Dr Nostalgia", "Dr Nostalgia", "SSG and Vortex models", "Take a picture, I'ma pose, paparazzi$I've been drinking, moving like a zombie", true, "board_transistor", "nos"),
    DRILLGON("drillgon", "Drillgon200", "Drillgon200", "1.12 Port", null, false, "cpu_logic", "drillgon200"),
    CIRNO("cirno", "Cirno", "Cirno", "the only multi layered skin i had", "No brain. Head empty.", true, "board_blank", "cirno"),
    MICROWAVE("microwave", "Microwave", "Microwave", "OC Compatibility and massive RBMK/packet optimizations", "they call me the food heater$john optimization", true, "board_converter", "microwave"),
    PEEP("peep", "Peep", "LePeeperSauvage", "Coilgun, Leadburster and Congo Lake models, BDCL QC", "Fluffy ears can't hide in ash, nor snow.", true, "card_board", "peep"),
    MELLOW("mellow", "MELLOWARPEGGIATION", "Mellow", "NBT Structures, industrial lighting, animation tools", "Make something cool now, ask for permission later.", true, "card_processor", "mellowrpg8"),
    ABEL("abel", "Abel1502", "Abel1502", "Abilities GUI, optimizations and many QoL improvements", "NANTO SUBARASHII", true, "cpu_register", "abel");

    private final String id;
    private final String title;
    private final String label;
    private final String contribution;
    private final String inscription;
    private final boolean skinLayers;
    private final String scrapVariant;
    private final String texture;

    BobbleheadType(String id, String title, String label, String contribution, String inscription,
                   boolean skinLayers, String scrapVariant, String texture) {
        this.id = id;
        this.title = title;
        this.label = label;
        this.contribution = contribution;
        this.inscription = inscription;
        this.skinLayers = skinLayers;
        this.scrapVariant = scrapVariant;
        this.texture = texture;
    }

    public String id() { return id; }
    public String title() { return title; }
    public String label() { return label; }
    public String contribution() { return contribution; }
    public String inscription() { return inscription; }
    public boolean skinLayers() { return skinLayers; }
    public String scrapVariant() { return scrapVariant; }

    public ResourceLocation texture() {
        // RenderBobble's NONE/default branch used ResourceManager.universal,
        // which lives beside the other model textures rather than in the
        // trinkets directory. Keep that legacy path exact for an untyped
        // stack; every actual bobble subtype remains under trinkets/.
        if (this == NONE) {
            return ReinhardtsHBM.id("textures/models/thegadget3_.png");
        }
        return ReinhardtsHBM.id("textures/models/trinkets/" + texture + ".png");
    }

    public ResourceLocation glowTexture() {
        return switch (this) {
            case MELLOW -> ReinhardtsHBM.id("textures/models/trinkets/mellowrpg8_glow.png");
            case ABEL -> ReinhardtsHBM.id("textures/models/trinkets/abel_glow.png");
            default -> texture();
        };
    }

    public static BobbleheadType byId(String id) {
        for (BobbleheadType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return NONE;
    }

    public static BobbleheadType byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
    }
}
