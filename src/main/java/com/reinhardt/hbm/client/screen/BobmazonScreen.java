package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.handler.LegacyBobmazonOffers;
import com.reinhardt.hbm.item.LegacyBobmazonItem;
import com.reinhardt.hbm.network.BobmazonOrderPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Original 217x229 Bobmazon catalog layout with three order buttons per page. */
public final class BobmazonScreen extends Screen {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_bobmazon.png");
    private static final int WIDTH = 217;
    private static final int HEIGHT = 229;
    private final boolean hidden;
    private final List<LegacyBobmazonOffers.Offer> offers;
    private int leftPos;
    private int topPos;
    private int page;

    private BobmazonScreen(boolean hidden) {
        super(Component.translatable(hidden ? "item.reinhardtshbm.bobmazon_hidden" : "item.reinhardtshbm.bobmazon"));
        this.hidden = hidden;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) throw new IllegalStateException("Bobmazon catalog opened without a client level");
        this.offers = LegacyBobmazonOffers.offers(hidden, minecraft.level.registryAccess());
    }

    public static void open(boolean hidden) { Minecraft.getInstance().setScreen(new BobmazonScreen(hidden)); }

    @Override protected void init() { leftPos = (width - WIDTH) / 2; topPos = (height - HEIGHT) / 2; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT);
        int start = page * 3;
        for (int row = 0; row < 3; row++) {
            int index = start + row;
            if (index >= offers.size()) break;
            LegacyBobmazonOffers.Offer offer = offers.get(index);
            int x = leftPos + 34;
            int y = topPos + 35 + row * 54;
            boolean hovered = x <= mouseX && mouseX < x + 18 && y <= mouseY && mouseY < y + 18;
            graphics.blit(TEXTURE, x, y, hovered ? 235 : 217, 0, 18, 18);
            graphics.renderItem(offer.stack(), x + 1, y + 1);
            graphics.drawString(font, offer.stack().getHoverName(), leftPos + 54, y + 2, 0x404040, false);
            graphics.drawString(font, offer.caps() + (offer.caps() == 1 ? " Cap" : " Caps"), leftPos + 96, y + 18, 0x404040, false);
            graphics.drawString(font, requirementName(offer.requirement()), leftPos + 54, y + 32, 0x606060, false);
            graphics.blit(TEXTURE, leftPos + 73, y + 14, 217, 62, 39, 8);
            if (offer.rating() > 0) {
                graphics.blit(TEXTURE, leftPos + 73, y + 14, 217, 54, Math.min(39, offer.rating() * 4 - 1), 8);
            }
            if (hovered) graphics.renderTooltip(font, offer.stack(), mouseX, mouseY);
        }
        int pages = Math.max(1, (offers.size() + 2) / 3);
        if (page > 0) graphics.blit(TEXTURE, leftPos + 7, topPos + 107, 217, 18, 18, 18);
        if (page + 1 < pages) graphics.blit(TEXTURE, leftPos + 192, topPos + 107, 235, 18, 18, 18);
        graphics.drawCenteredString(font, (page + 1) + "/" + pages, leftPos + WIDTH / 2, topPos + 205, 0x404040);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int pages = Math.max(1, (offers.size() + 2) / 3);
            if (page > 0 && mouseX >= leftPos + 7 && mouseX < leftPos + 25 && mouseY >= topPos + 107 && mouseY < topPos + 125) {
                page--;
                clickSound();
                return true;
            }
            if (page + 1 < pages && mouseX >= leftPos + 192 && mouseX < leftPos + 210 && mouseY >= topPos + 107 && mouseY < topPos + 125) {
                page++;
                clickSound();
                return true;
            }
            int start = page * 3;
            for (int row = 0; row < 3; row++) {
                int index = start + row;
                int x = leftPos + 34;
                int y = topPos + 35 + row * 54;
                if (index < offers.size() && mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    PacketDistributor.sendToServer(new BobmazonOrderPayload(index));
                    clickSound();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static void clickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player == null) return;
        if (!isCatalog(minecraft.player.getMainHandItem()) && !isCatalog(minecraft.player.getOffhandItem())) onClose();
    }

    private boolean isCatalog(ItemStack stack) {
        return stack.getItem() instanceof LegacyBobmazonItem item && item.hidden() == hidden;
    }

    private static Component requirementName(LegacyBobmazonOffers.Requirement requirement) {
        return switch (requirement) {
            case NONE -> Component.empty();
            case STEEL -> Component.translatable("advancements.reinhardtshbm.blast_furnace.title");
            case ASSEMBLY -> Component.translatable("advancements.reinhardtshbm.assembly.title");
            case CHEMICS -> Component.translatable("advancements.reinhardtshbm.chemplant.title");
            case OIL -> Component.translatable("advancements.reinhardtshbm.desh.title");
            case NUCLEAR -> Component.translatable("advancements.reinhardtshbm.technetium.title");
            case HIDDEN -> Component.translatable("advancements.reinhardtshbm.hidden.title");
        };
    }

    @Override public boolean isPauseScreen() { return false; }
}
