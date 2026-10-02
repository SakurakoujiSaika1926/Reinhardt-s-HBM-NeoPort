package com.reinhardt.hbm.client.qmaw;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.HbmKeyMappings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, value = Dist.CLIENT)
public final class QmawClientEvents {
    private static QuickManualAndWiki lastManual;
    private static long lastManualTimestamp;

    private QmawClientEvents() {
    }

    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null || Minecraft.getInstance().screen == null) {
            return;
        }
        QmawLoader.pageFor(event.getItemStack()).ifPresent(manual -> {
            event.getToolTip().add(Component.translatable("qmaw.tab", HbmKeyMappings.QMAW.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.YELLOW));
            lastManual = manual;
            lastManualTimestamp = System.nanoTime();
        });
    }

    @SubscribeEvent
    public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof QmawScreen
                || !HbmKeyMappings.QMAW.matches(event.getKeyCode(), event.getScanCode())
                || lastManual == null
                || System.nanoTime() - lastManualTimestamp > 250_000_000L) {
            return;
        }
        Minecraft.getInstance().setScreen(new QmawScreen(lastManual));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ntmwikirender")
                .then(Commands.argument("type", StringArgumentType.word())
                        .executes(context -> openWikiRenderer(context.getSource(),
                                StringArgumentType.getString(context, "type")))));
    }

    private static int openWikiRenderer(net.minecraft.commands.CommandSourceStack source, String type) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(ReinhardtsHBM.MOD_ID)) {
                continue;
            }
            String itemType = item.getClass().getSimpleName();
            String blockType = item instanceof BlockItem blockItem
                    ? blockItem.getBlock().getClass().getSimpleName() : "";
            if (itemType.equalsIgnoreCase(type) || blockType.equalsIgnoreCase(type)) {
                stacks.add(item.getDefaultInstance());
            }
        }
        if (stacks.isEmpty()) {
            source.sendFailure(Component.translatable("command.reinhardtshbm.ntmwikirender.empty", type));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "command.reinhardtshbm.ntmwikirender.start", stacks.size(), type), false);
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(
                new WikiRenderScreen(stacks, sanitize(type))));
        return stacks.size();
    }

    private static String sanitize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_");
    }

    @EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new QmawLoader());
        }
    }
}
