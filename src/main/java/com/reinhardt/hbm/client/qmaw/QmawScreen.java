package com.reinhardt.hbm.client.qmaw;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Modern client implementation of the latest 1.7.10 QMAW reader. */
public final class QmawScreen extends Screen {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/gui_wiki.png");
    private static final int GUI_WIDTH = 340;
    private static final int GUI_HEIGHT = 224;
    private static final int CONTENT_LEFT = 7;
    private static final int CONTENT_TOP = 30;
    private static final int CONTENT_RIGHT = 326;
    private static final int CONTENT_BOTTOM = 219;

    private final QuickManualAndWiki manual;
    private final List<String> back = new ArrayList<>();
    private final List<String> forward = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private int left;
    private int top;
    private int scrollLine;

    public QmawScreen(QuickManualAndWiki manual) {
        super(Component.literal(manual.name()));
        this.manual = manual;
    }

    private QmawScreen(QuickManualAndWiki manual, List<String> back, List<String> forward) {
        this(manual);
        this.back.addAll(back);
        this.forward.addAll(forward);
    }

    @Override
    protected void init() {
        left = (width - GUI_WIDTH) / 2;
        top = (height - GUI_HEIGHT) / 2;
        rebuildLines();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE0000000);
        graphics.blit(TEXTURE, left, top, 0, 0, 170, GUI_HEIGHT, 256, 256);
        graphics.blit(TEXTURE, left + 170, top, 52, 0, 30, GUI_HEIGHT, 256, 256);
        graphics.blit(TEXTURE, left + 200, top, 52, 0, 140, GUI_HEIGHT, 256, 256);

        if (!back.isEmpty()) {
            graphics.blit(TEXTURE, left + 3, top + 3, 204, 0, 18, 18, 256, 256);
        }
        if (!forward.isEmpty()) {
            graphics.blit(TEXTURE, left + 21, top + 3, 222, 0, 18, 18, 256, 256);
        }

        int sliderY = lines.size() <= 1 ? 25
                : 25 + Mth.floor(180.0D * scrollLine / Math.max(1, lines.size() - 1));
        graphics.blit(TEXTURE, left + GUI_WIDTH - 15, top + sliderY, 192, 0, 12, 16, 256, 256);

        int titleX = left + 43;
        ItemStack icon = manual.icon();
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, titleX, top + 4);
            titleX += 18;
        }
        graphics.drawString(font, manual.title(language()), titleX, top + 8, 0xFFFFFF, false);

        int y = top + CONTENT_TOP;
        for (int index = scrollLine; index < lines.size(); index++) {
            Line line = lines.get(index);
            if (y + line.height() > top + CONTENT_BOTTOM) {
                break;
            }
            for (Segment segment : line.segments()) {
                int x = left + CONTENT_LEFT + segment.x();
                int segmentY = y + (line.height() - segment.height()) / 2;
                boolean hovered = mouseX >= x && mouseX < x + segment.width()
                        && mouseY >= segmentY && mouseY < segmentY + segment.height();
                if (!segment.icon().isEmpty()) {
                    graphics.renderItem(segment.icon(), x, segmentY - 1);
                    x += 18;
                }
                if (segment.target() == null) {
                    graphics.drawString(font, segment.text(), x, segmentY, 0xFFFFFF, false);
                } else {
                    int color = segment.current() ? 0xA0A0A0
                            : segment.broken() ? 0xFF7F7F
                            : hovered ? 0xFFD800 : 0x0094FF;
                    graphics.drawString(font, segment.text(), x, segmentY, color, false);
                }
            }
            y += line.height() + 2;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (inside(mouseX, mouseY, left + 3, top + 3, 18, 18)) {
            goBack();
            return true;
        }
        if (inside(mouseX, mouseY, left + 21, top + 3, 18, 18)) {
            goForward();
            return true;
        }
        int y = top + CONTENT_TOP;
        for (int index = scrollLine; index < lines.size(); index++) {
            Line line = lines.get(index);
            if (y + line.height() > top + CONTENT_BOTTOM) {
                break;
            }
            for (Segment segment : line.segments()) {
                int x = left + CONTENT_LEFT + segment.x();
                int segmentY = y + (line.height() - segment.height()) / 2;
                if (segment.target() != null && !segment.current() && !segment.broken()
                        && inside(mouseX, mouseY, x, segmentY, segment.width(), segment.height())) {
                    openLinkedPage(segment.target());
                    return true;
                }
            }
            y += line.height() + 2;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scrollLine = Mth.clamp(scrollLine + (scrollY < 0 ? 1 : -1), 0, Math.max(0, lines.size() - 1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            goBack();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            goForward();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void rebuildLines() {
        lines.clear();
        String text = QmawLoader.compose(manual.content(language()), language())
                .replace("\r", "")
                .replace("<br>", "\n");
        List<RawSegment> raw = parse(text);
        LineBuilder line = new LineBuilder();
        for (RawSegment segment : raw) {
            if (segment.newLine()) {
                lines.add(line.build());
                line = new LineBuilder();
                continue;
            }
            List<String> parts = segment.target() == null ? splitWords(segment.text()) : List.of(segment.text());
            for (String part : parts) {
                QuickManualAndWiki target = segment.target() == null
                        ? null : QmawLoader.page(segment.target()).orElse(null);
                ItemStack targetIcon = target == null ? ItemStack.EMPTY : target.icon();
                int iconWidth = segment.target() != null && !targetIcon.isEmpty() ? 18 : 0;
                int width = font.width(part) + iconWidth;
                if (!line.isEmpty() && line.width() + width > CONTENT_RIGHT - CONTENT_LEFT) {
                    lines.add(line.build());
                    line = new LineBuilder();
                }
                line.add(new Segment(line.width(), width, Math.max(font.lineHeight, iconWidth == 0 ? 0 : 16),
                        part, segment.target(), targetIcon,
                        segment.target() != null && target == null,
                        segment.target() != null && segment.target().equals(manual.name())));
            }
        }
        lines.add(line.build());
        scrollLine = Mth.clamp(scrollLine, 0, Math.max(0, lines.size() - 1));
    }

    private List<RawSegment> parse(String value) {
        List<RawSegment> result = new ArrayList<>();
        int cursor = 0;
        while (cursor < value.length()) {
            if (value.charAt(cursor) == '\n') {
                result.add(RawSegment.LINE_BREAK);
                cursor++;
                continue;
            }
            if (value.startsWith("[[", cursor)) {
                int end = value.indexOf("]]", cursor + 2);
                if (end >= 0) {
                    String link = value.substring(cursor + 2, end);
                    int pipe = link.indexOf('|');
                    String display = pipe < 0 ? link : link.substring(0, pipe);
                    String target = pipe < 0 ? link : link.substring(pipe + 1);
                    if (end + 2 < value.length() && value.charAt(end + 2) == ' ') {
                        display += " ";
                    }
                    result.add(new RawSegment(display, target, false));
                    cursor = end + 2;
                    continue;
                }
            }
            int nextLink = value.indexOf("[[", cursor);
            int nextBreak = value.indexOf('\n', cursor);
            int end = value.length();
            if (nextLink >= 0) end = Math.min(end, nextLink);
            if (nextBreak >= 0) end = Math.min(end, nextBreak);
            if (end == cursor) {
                cursor++;
            } else {
                result.add(new RawSegment(value.substring(cursor, end), null, false));
                cursor = end;
            }
        }
        return result;
    }

    private static List<String> splitWords(String text) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == ' ') {
                parts.add(text.substring(start, index + 1));
                start = index + 1;
            }
        }
        if (start < text.length()) {
            parts.add(text.substring(start));
        }
        if (parts.isEmpty()) {
            parts.add(text);
        }
        return parts;
    }

    private void openLinkedPage(String targetName) {
        QmawLoader.page(targetName).ifPresent(target -> {
            playClick();
            List<String> newBack = new ArrayList<>(back);
            newBack.add(manual.name());
            Minecraft.getInstance().setScreen(new QmawScreen(target, newBack, List.of()));
        });
    }

    private void goBack() {
        if (back.isEmpty()) return;
        String targetName = back.getLast();
        QmawLoader.page(targetName).ifPresent(target -> {
            playClick();
            List<String> newBack = new ArrayList<>(back);
            newBack.removeLast();
            List<String> newForward = new ArrayList<>(forward);
            newForward.add(manual.name());
            Minecraft.getInstance().setScreen(new QmawScreen(target, newBack, newForward));
        });
    }

    private void goForward() {
        if (forward.isEmpty()) return;
        String targetName = forward.getLast();
        QmawLoader.page(targetName).ifPresent(target -> {
            playClick();
            List<String> newBack = new ArrayList<>(back);
            newBack.add(manual.name());
            List<String> newForward = new ArrayList<>(forward);
            newForward.removeLast();
            Minecraft.getInstance().setScreen(new QmawScreen(target, newBack, newForward));
        });
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String language() {
        String selected = Minecraft.getInstance().getLanguageManager().getSelected();
        int underscore = selected.indexOf('_');
        return underscore < 0 || underscore + 1 >= selected.length()
                ? selected : selected.substring(0, underscore).toLowerCase(Locale.ROOT) + "_"
                + selected.substring(underscore + 1).toUpperCase(Locale.ROOT);
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private record RawSegment(String text, String target, boolean newLine) {
        private static final RawSegment LINE_BREAK = new RawSegment("", null, true);
    }

    private record Segment(int x, int width, int height, String text, String target, ItemStack icon,
                           boolean broken, boolean current) {
    }

    private record Line(List<Segment> segments, int height) {
    }

    private static final class LineBuilder {
        private final List<Segment> segments = new ArrayList<>();
        private int width;
        private int height;

        void add(Segment segment) {
            segments.add(segment);
            width += segment.width();
            height = Math.max(height, segment.height());
        }

        boolean isEmpty() {
            return segments.isEmpty();
        }

        int width() {
            return width;
        }

        Line build() {
            return new Line(List.copyOf(segments), Math.max(Minecraft.getInstance().font.lineHeight, height));
        }
    }
}
