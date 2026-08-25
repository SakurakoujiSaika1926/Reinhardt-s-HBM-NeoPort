package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.screen.HolotapeImageScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/** Direct 1.7.10 ItemHolotapeImage port, including every old tape entry. */
public final class LegacyHolotapeImageItem extends LegacyVariantItem {
    public LegacyHolotapeImageItem(Properties properties) {
        super(properties.stacksTo(1), "holotape_image", variants(
                "holo_digamma", "holo_restored", "holo_fe_hall", "holo_fe_corridor", "holo_fe_server",
                "holo_feh_dome", "holo_feh_boat", "holo_feh_lsc", "holo_f3_rc", "holo_f3_iv", "holo_f3_wm",
                "holo_nv_crater", "holo_nv_divide", "holo_nv_bm", "holo_o_1", "holo_o_2", "holo_o_3", "holo_challenge"
        ), false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && FMLEnvironment.dist.isClient()) {
            HolotapeImageScreen.open(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Tape tape = tape(stack);
        tooltip.add(Component.literal("Band Color: ").append(Component.literal(tape.colorName()).withStyle(tape.color())));
        tooltip.add(Component.literal("Label: " + tape.label()));
    }

    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        for (Tape tape : Tape.values()) {
            output.accept(LegacyVariantItem.stackFor(this, tape.id()));
        }
    }

    public static Tape tape(ItemStack stack) {
        CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return Tape.byIndex(model == null ? 0 : model.value());
    }

    public enum Tape {
        HOLO_DIGAMMA(ChatFormatting.RED, "Crimson", "D#", "The tape contains a music track that has degraded heavily in quality, making it near-impossible to make out what it once was. There is an image file on it that has also lost its quality, being reduced to a blur of crimson and cream colors. The disk has small shreds of greasy wrapping paper stuck to it."),
        HOLO_RESTORED(ChatFormatting.RED, "Crimson", "D0", "The tape contains a music track that you do not recognize, consisting of mostly electric guitars with lyrics telling the story of a man being left by someone who is moving to another city. The tape also contains an image file, the crimson and cream colors sharp on an otherwise colorless background. You try to look closer but you can't. It feels as if reality itself is twisted and stretched and snapped back into shape like a rubber band."),
        HOLO_FE_HALL(ChatFormatting.GREEN, "Lime", "001-HALL", "The tape contains an audio track that is mostly gabled sound and garbage noise. There is an image file on it, depicting a small hall with a fountain in the center, a metal door to the left and an open wooden door to the right, with faint green light coming through the doorway. On the left wall of the room, there is a wooden bench with a skeleton sitting on it."),
        HOLO_FE_CORRIDOR(ChatFormatting.GREEN, "Lime", "002-CORRIDOR", "The tape contains an audio track that is mostly gabled sound and garbage noise. There is an image file on it, depicting a short hallway with a terminal screen mounted to the right wall, bathing the corridor in a phosphorus-green light. In front of the terminal, an unusually large skeleton is piled up on the floor. On the back of the hallway there's a sturdy metal door standing open."),
        HOLO_FE_SERVER(ChatFormatting.GREEN, "Lime", "003-SERVER", "The tape contains an audio track that is mostly gabled sound and garbage noise. There is an image file on it, depicting what appears to be a server room with racks covering every wall. In the center, what appears to be some sort of super computer is standing tall, with wires coming out from it, going in every direction. On the right side of the room, a small brass trapdoor stands open where one of the wall racks would be."),
        HOLO_FEH_DOME(ChatFormatting.RED, "Red", "011-DOME", "The tape contains an audio track that is mostly gabled sound and garbage noise. There is an image file on it, depicting the insides of a large dome-like concrete structure that is mostly empty, save for a few catwalks and a shiny blueish metal capsule suspended in the center. In the background, the faint outline of what appears to be a tank is visible, sporting mechanical legs instead of treads."),
        HOLO_FEH_BOAT(ChatFormatting.RED, "Red", "012-BOAT", "The tape contains an audio track that is mostly gabled sound and garbage noise. There is an image file on it, depicting the wooden deck of what appears to be an old river boat. There are four rusted railway spikes stuck in the planks in a roughly square shape."),
        HOLO_FEH_LSC(ChatFormatting.RED, "Red", "013-LAUNCH", "The tape contains an audio track that is mostly gabled sound and garbage noise. There is an image file on it, depicting an array of launch pads surrounded by large metal bulwarks. Two of the launch pads are empty, the remaining rockets seem to be heavily damaged. A tipped-over booster is visible, creating plumes of fog."),
        HOLO_F3_RC(ChatFormatting.DARK_GREEN, "Green", "021-RIVET", "The tape contains an audio track that is mostly gabled sound and garbage noise. There is an image file on it, depicting an old aircraft carrier that has broken in two. A makeshift bridge held up by the ship's crane connects the tower with a small building on the shore."),
        HOLO_F3_IV(ChatFormatting.DARK_GREEN, "Green", "022-V87", "The tape contains an audio track that is mostly gabled sound and garbage noise. There is a very grainy image file on it, depicting what appears to be a crater with a small tunnel leading into the ground at the very bottom, closed off with a small wooden door."),
        HOLO_F3_WM(ChatFormatting.DARK_GREEN, "Green", "023-MONUMENT", "The tape contains an audio track that is mostly gabled sound and garbage noise. There is an image file on it, depicting a large white obelisk that seems half destroyed. At the top there is a radio dish sticking out of the structure."),
        HOLO_NV_CRATER(ChatFormatting.GOLD, "Brown", "031-MOUNTAIN", "The tape contains an audio track that is mostly gabled sound and garbage noise. There is an image file on it, depicting a large dome in blue light surrounded by many smaller buildings. In the distance, there is a smaller dome with red lights."),
        HOLO_NV_DIVIDE(ChatFormatting.GOLD, "Brown", "032-ROAD", "The tape contains an audio track that is mostly gabled sound and garbage noise. There is an image file on it, depicting a large chasm with broken highways and destroyed buildings littering the landscape."),
        HOLO_NV_BM(ChatFormatting.GOLD, "Brown", "033-BROADCAST", "The tape contains an audio track that is mostly gabled sound and garbage noise. There is an image file on it, depicting a satellite broadcasting station on top of a hill. In the distance, there is a very large person walking hand in hand with a robot into the sunset."),
        HOLO_O_1(ChatFormatting.WHITE, "Chroma", "X00-TRANSCRIPT", "[Transcript redacted]"),
        HOLO_O_2(ChatFormatting.WHITE, "Chroma", "X01-NEWS", "The tape contains a news article, reporting an unusually pale person throwing flashbangs at people in public. The image at the bottom shows one of the incidents, unsurprisingly the light from one of the flashbangs made it unrecognizable."),
        HOLO_O_3(ChatFormatting.WHITE, "Chroma", "X02-FICTION", "The tape contains an article from a science fiction magazine, engaging with various reader comments about what to do with a time machine. One of those comments suggests engaging in various unsanitary acts with the future self, being signed off with just the initial '~D'."),
        HOLO_CHALLENGE(ChatFormatting.GRAY, "None", "-", "An empty holotape. The back has the following message scribbled on it with black marker: \"official challenge - convince me that lyons' brotherhood isn't the best brotherhood of steel chapter and win a custom cape!\" The tape smells like chicken nuggets.");

        private final ChatFormatting color;
        private final String colorName;
        private final String label;
        private final String text;

        Tape(ChatFormatting color, String colorName, String label, String text) {
            this.color = color;
            this.colorName = colorName;
            this.label = label;
            this.text = text;
        }

        public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
        public ChatFormatting color() { return color; }
        public String colorName() { return colorName; }
        public String label() { return label; }
        public String text() { return text; }

        private static Tape byIndex(int index) {
            Tape[] values = values();
            return index >= 0 && index < values.length ? values[index] : values[0];
        }
    }
}
