package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.GuideBookItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Pixel-for-pixel layout port of the old GUIScreenGuide, including its two-page spreads. */
public final class GuideBookScreen extends Screen {
    private static final ResourceLocation BOOK_TEXTURE = ReinhardtsHBM.id("textures/gui/book/book.png");
    private static final ResourceLocation COVER_TEXTURE = ReinhardtsHBM.id("textures/gui/book/book_cover.png");
    private static final int WIDTH = 272;
    private static final int HEIGHT = 182;
    private static final int PAGE_WIDTH = 100;
    private static final int PAGE_OFFSET = 130;

    private final GuideBookItem.Type type;
    private final List<Page> pages;
    private int leftPos;
    private int topPos;
    private int spread = -1;

    private GuideBookScreen(ItemStack stack) {
        super(Component.translatable("item.reinhardtshbm.book_guide_book"));
        this.type = GuideBookItem.type(stack);
        this.pages = pagesFor(this.type);
    }

    public static void open(ItemStack stack) {
        Minecraft.getInstance().setScreen(new GuideBookScreen(stack));
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = (this.height - HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        if (spread < 0) {
            drawCover(graphics);
            return;
        }

        graphics.blit(BOOK_TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, 512, 512);
        drawArrows(graphics, mouseX, mouseY);
        drawPages(graphics);
    }

    private void drawCover(GuiGraphics graphics) {
        graphics.blit(COVER_TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT, 512, 512);
        String[] lines = Component.translatable(type.coverKey()).getString().split("\\$");
        float scale = type.titleScale();
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            if (line.isEmpty()) {
                continue;
            }
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1.0F);
            int x = (int) ((leftPos + WIDTH / 2) / scale - font.width(line) / 2.0F);
            int y = (int) ((topPos + 50 + index * 10 * scale) / scale);
            graphics.drawString(font, line, x, y, 0xFFFECE00, false);
            graphics.pose().popPose();
        }
    }

    private void drawArrows(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean overLeft = inside(mouseX, mouseY, leftPos + 24, topPos + 155, 18, 10);
        boolean overRight = inside(mouseX, mouseY, leftPos + 230, topPos + 155, 18, 10);
        if (spread > 0) {
            graphics.blit(BOOK_TEXTURE, leftPos + 24, topPos + 155, overLeft ? 26 : 3, 207, 18, 10, 512, 512);
        }
        if (spread < maxSpread()) {
            graphics.blit(BOOK_TEXTURE, leftPos + 230, topPos + 155, overRight ? 26 : 3, 194, 18, 10, 512, 512);
        }
    }

    private void drawPages(GuiGraphics graphics) {
        for (int side = 0; side < 2; side++) {
            int pageIndex = spread * 2 + side;
            if (pageIndex >= pages.size()) {
                continue;
            }
            Page page = pages.get(pageIndex);
            int pageLeft = leftPos + 20 + side * PAGE_OFFSET;
            for (TextBlock text : page.texts()) {
                drawTextBlock(graphics, page, pageLeft, topPos, text);
            }
            if (page.title() != null) {
                drawTitle(graphics, page, pageLeft, topPos);
            }
            for (BookImage image : page.images()) {
                int imageX = image.x() < 0 ? PAGE_WIDTH / 2 - image.width() / 2 : image.x();
                int imageY = topPos + image.y();
                if (!image.item().isEmpty()) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(pageLeft + imageX, imageY, 0.0F);
                    graphics.pose().scale(image.width() / 16.0F, image.height() / 16.0F, 1.0F);
                    graphics.renderItem(image.item(), 0, 0);
                    graphics.pose().popPose();
                } else if (image.texture() != null) {
                    graphics.blit(image.texture(), pageLeft + imageX, imageY, 0, 0,
                            image.width(), image.height(), image.width(), image.height());
                }
            }
            String label = (pageIndex + 1) + "/" + pages.size();
            int labelX = leftPos + 44 + side * 185 - side * font.width(label);
            graphics.drawString(font, label, labelX, topPos + 156, 0xFF404040, false);
        }
    }

    private void drawTextBlock(GuiGraphics graphics, Page page, int pageLeft, int pageTop, TextBlock textBlock) {
        float scale = textBlock.scale();
        String text = Component.translatable(textBlock.key()).getString();
        List<FormattedCharSequence> lines = font.split(Component.literal(text), Math.round(textBlock.width() * scale));
        float titleScale = page.titleScale();
        float topOffset = textBlock.y() < 0 ? (page.title() == null ? -10.0F : 6.0F / titleScale) : textBlock.y();

        graphics.pose().pushPose();
        graphics.pose().scale(1.0F / scale, 1.0F / scale, 1.0F);
        int x = Math.round((pageLeft + textBlock.x()) * scale);
        int y = Math.round((pageTop + 30 + topOffset) * scale);
        for (int line = 0; line < lines.size(); line++) {
            graphics.drawString(font, lines.get(line), x, y + 12 * line, 0xFF404040, false);
        }
        graphics.pose().popPose();
    }

    private void drawTitle(GuiGraphics graphics, Page page, int pageLeft, int pageTop) {
        float scale = page.titleScale();
        String title = Component.translatable(page.title()).getString();
        graphics.pose().pushPose();
        graphics.pose().scale(1.0F / scale, 1.0F / scale, 1.0F);
        int x = Math.round((pageLeft + PAGE_WIDTH / 2.0F - font.width(title) / (2.0F * scale)) * scale);
        int y = Math.round((pageTop + 20) * scale);
        graphics.drawString(font, title, x, y, page.titleColor(), false);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (spread < 0) {
            spread = 0;
            click();
            return true;
        }
        if (inside((int) mouseX, (int) mouseY, leftPos + 24, topPos + 155, 18, 10) && spread > 0) {
            spread--;
            click();
            return true;
        }
        if (inside((int) mouseX, (int) mouseY, leftPos + 230, topPos + 155, 18, 10) && spread < maxSpread()) {
            spread++;
            click();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int maxSpread() {
        return Math.max(0, (int) Math.ceil(pages.size() / 2.0D) - 1);
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static List<Page> pagesFor(GuideBookItem.Type type) {
        return switch (type) {
            case TEST -> TEST_PAGES;
            case RBMK -> RBMK_PAGES;
            case HADRON -> HADRON_PAGES;
            case STARTER -> STARTER_PAGES;
        };
    }

    private static final List<Page> TEST_PAGES = List.of(
            page("Title LMAO", 0xFF800000, 1.0F, texts(text("book.test.page1", 2.0F)), image("smileman.png", -1, 100, 40, 40)),
            page("LA SEXO", 0xFF800000, 0.5F, texts(text("book.test.page1", 1.75F)), image("smileman.png", -1, 100, 40, 40)),
            page(null, 0, 1.0F, texts(text("test test", 1.0F))),
            page(null, 0, 1.0F, texts(text("test test test", 1.0F))),
            page(null, 0, 1.0F, texts(text("test test", 1.0F))),
            page(null, 0, 1.0F, texts(text("test test test", 1.0F))),
            page(null, 0, 1.0F, texts(text("test test", 1.0F)))
    );

    private static final List<Page> RBMK_PAGES = List.of(
            page("book.rbmk.title1", 0xFF800000, 1.0F, texts(text("book.rbmk.page1", 2.0F)), image("rbmk1.png", -1, 90, 80, 60)),
            page("book.rbmk.title2", 0xFF800000, 1.0F, texts(text("book.rbmk.page2", 2.0F)), image("rbmk2.png", -1, 95, 52, 52)),
            page("book.rbmk.title3", 0xFF800000, 1.0F, texts(text("book.rbmk.page3", 2.0F)), image("rbmk3.png", -1, 95, 88, 52)),
            page("book.rbmk.title4", 0xFF800000, 1.0F, texts(text("book.rbmk.page4", 2.0F)), image("rbmk4.png", -1, 95, 88, 52)),
            page("book.rbmk.title5", 0xFF800000, 0.9F, texts(text("book.rbmk.page5", 2.0F)), image("rbmk5.png", -1, 95, 80, 42)),
            page("book.rbmk.title6", 0xFF800000, 1.0F, texts(text("book.rbmk.page6", 2.0F)), image("rbmk6.png", -1, 90, 100, 60)),
            page("book.rbmk.title7", 0xFF800000, 1.0F, texts(text("book.rbmk.page7", 2.0F)), image("rbmk7.png", -1, 95, 52, 52)),
            page("book.rbmk.title8", 0xFF800000, 1.0F, texts(text("book.rbmk.page8", 2.0F)), image("rbmk8.png", -1, 95, 88, 52)),
            page("book.rbmk.title9", 0xFF800000, 1.0F, texts(text("book.rbmk.page9", 2.0F)), image("rbmk9.png", -1, 95, 88, 52)),
            page("book.rbmk.title10", 0xFF800000, 1.0F, texts(text("book.rbmk.page10", 2.0F)), image("rbmk10.png", -1, 95, 88, 52)),
            page("book.rbmk.title11", 0xFF800000, 1.0F, texts(text("book.rbmk.page11", 2.0F)), image("rbmk11.png", -1, 75, 85, 72)),
            page("book.rbmk.title12", 0xFF800000, 1.0F, texts(text("book.rbmk.page12", 2.0F)), image("rbmk12.png", -1, 90, 80, 60)),
            page("book.rbmk.title13", 0xFF800000, 1.0F, texts(text("book.rbmk.page13", 2.0F))),
            page(null, 0, 1.0F, texts(text("book.rbmk.page14", 2.0F)), image("rbmk13.png", -1, 70, 103, 78)),
            page("book.rbmk.title15", 0xFF800000, 1.0F, texts(text("book.rbmk.page15", 2.0F)), image("rbmk15.png", -1, 100, 48, 48)),
            page("book.rbmk.title16", 0xFF800000, 1.0F, texts(text("book.rbmk.page16", 2.0F)), image("rbmk16.png", -1, 50, 70, 100))
    );

    private static final List<Page> HADRON_PAGES = hadronPages();

    private static final List<Page> STARTER_PAGES = List.of(
            page("book.starter.title1", 0xFF800000, 1.0F, texts(text("book.starter.page1", 2.0F)), image("starter1.png", -1, 96, 101, 56)),
            page("book.starter.title2", 0xFF800000, 1.0F, texts(text("book.starter.page2", 2.0F)), itemImage("mask_piss", -1, 85, 64, 64)),
            page("book.starter.title3", 0xFF800000, 1.0F, texts(text("book.starter.page3", 2.0F)), image("starter3.png", -1, 89, 100, 64)),
            page("book.starter.title4", 0xFF800000, 1.0F, texts(text("book.starter.page4", 1.4F, 0, 6, 72)),
                    itemImage("template_folder", 72, 30, 24, 24), itemImage("stamp_iron_flat", 72, 60, 24, 24),
                    itemImage("assembly_template", 72, 90, 24, 24), itemImage("chemistry_template", 72, 120, 24, 24)),
            page("book.starter.title5", 0xFF800000, 1.0F, texts(text("book.starter.page5", 2.0F))),
            page("book.starter.title6", 0xFF800000, 1.0F, texts(text("book.starter.page6a", 2.0F), text("book.starter.page6b", 2.0F, 0, 96, 100)), image("starter6.png", 9, 89, 84, 36)),
            page(null, 0, 1.0F, texts(text("book.starter.page7a", 2.0F), text("book.starter.page7b", 2.0F, 0, 95, 100)), image("starter7.png", 9, 67, 84, 58)),
            page("book.starter.title8", 0xFF800000, 1.0F, texts(text("book.starter.page8a", 2.0F, 0, -1, 50), text("book.starter.page8b", 2.0F, 50, 70, 50)),
                    image("starter8a.png", 53, 36, 47, 61), image("starter8b.png", 0, 102, 47, 61)),
            page("book.starter.title9", 0xFF800000, 1.0F, texts(text("book.starter.page9", 2.0F)),
                    itemImage("ingot_polymer", 4, 106, 24, 24), itemImage("ingot_desh", 28, 130, 24, 24),
                    itemImage("solid_fuel_presto_triplet", 52, 106, 24, 24), itemImage("canister_gasoline", 76, 130, 24, 24)),
            page("book.starter.title10", 0xFF800000, 1.0F, texts(text("book.starter.page10", 2.0F)), image("starter10.png", 0, 115, 100, 39)),
            page("book.starter.title11", 0xFF800000, 1.0F, texts(text("book.starter.page11", 2.0F, 0, -1, 60)),
                    image("starter11a.png", 61, 36, 45, 57), image("starter11b.png", 61, 97, 45, 57)),
            page("book.starter.title12", 0xFFFECE00, 1.0F, texts(text("book.starter.page12a", 3.0F), text("book.starter.page12b", 2.0F, 0, 20, 100))),
            page("book.starter.title13", 0xFF800000, 1.0F, texts(text("book.starter.page13", 2.0F)), image("starter13.png", -1, 110, 84, 42)),
            page("book.starter.title14", 0xFF800000, 1.0F, texts(text("book.starter.page14", 2.0F, 0, 54, 100)), image("starter14.png", -1, 34, 100, 46)),
            page("book.starter.title15", 0xFF800000, 1.0F, texts(text("book.starter.page15", 2.0F))),
            page("book.starter.title16", 0xFF800000, 1.0F, texts(text("book.starter.page16", 2.0F))),
            page(null, 0, 1.0F, List.of()),
            page("book.starter.title18", 0xFF800000, 1.0F, texts(text("book.starter.page18", 2.0F)), image("starter18.png", 10, 69, 100, 100))
    );

    private static List<Page> hadronPages() {
        List<Page> pages = new ArrayList<>();
        for (int page = 1; page <= 9; page++) {
            pages.add(page("book.error.title" + page, 0xFF800000, 1.0F, texts(text("book.error.page" + page, 2.0F))));
        }
        return List.copyOf(pages);
    }

    private static Page page(String title, int titleColor, float titleScale, List<TextBlock> texts, BookImage... images) {
        return new Page(title, titleColor, titleScale, texts, List.of(images));
    }

    private static List<TextBlock> texts(TextBlock... blocks) {
        return List.of(blocks);
    }

    private static TextBlock text(String key, float scale) {
        return text(key, scale, 0, -1, PAGE_WIDTH);
    }

    private static TextBlock text(String key, float scale, int x, int y, int width) {
        return new TextBlock(key, scale, x, y, width);
    }

    private static BookImage image(String name, int x, int y, int width, int height) {
        return new BookImage(ReinhardtsHBM.id("textures/gui/book/" + name), ItemStack.EMPTY, x, y, width, height);
    }

    private static BookImage image(String name, int y, int width, int height) {
        return image(name, -1, y, width, height);
    }

    private static BookImage itemImage(String name, int x, int y, int width, int height) {
        ItemStack stack = BuiltInRegistries.ITEM.getOptional(ReinhardtsHBM.id(name)).map(ItemStack::new).orElse(ItemStack.EMPTY);
        return new BookImage(null, stack, x, y, width, height);
    }

    private record Page(String title, int titleColor, float titleScale, List<TextBlock> texts, List<BookImage> images) {
    }

    private record TextBlock(String key, float scale, int x, int y, int width) {
    }

    private record BookImage(ResourceLocation texture, ItemStack item, int x, int y, int width, int height) {
    }
}
