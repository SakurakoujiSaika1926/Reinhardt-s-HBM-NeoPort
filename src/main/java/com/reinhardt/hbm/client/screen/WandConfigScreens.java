package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.blockentity.WandJigsawBlockEntity;
import com.reinhardt.hbm.blockentity.WandStructureBlockEntity;
import com.reinhardt.hbm.blockentity.WandTandemBlockEntity;
import com.reinhardt.hbm.network.WandConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class WandConfigScreens {
    private WandConfigScreens() {
    }

    public static void openStructure(BlockPos pos, boolean load) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof WandStructureBlockEntity structure) {
            minecraft.setScreen(new StructureScreen(pos, structure.configTag(), load));
        }
    }

    public static void openJigsaw(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof WandJigsawBlockEntity jigsaw) {
            minecraft.setScreen(new JigsawScreen(pos, jigsaw.configTag()));
        }
    }

    public static void openTandem(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof WandTandemBlockEntity tandem) {
            minecraft.setScreen(new TandemScreen(pos, tandem.configTag()));
        }
    }

    private abstract static class WandScreen extends Screen {
        protected final BlockPos pos;
        protected final CompoundTag tag;

        protected WandScreen(Component title, BlockPos pos, CompoundTag tag) {
            super(title);
            this.pos = pos;
            this.tag = tag.copy();
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                onClose();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        protected EditBox textBox(int x, int y, int width, String initial) {
            EditBox box = new EditBox(this.font, x, y, width, 20, Component.empty());
            box.setMaxLength(256);
            box.setValue(initial);
            addRenderableWidget(box);
            return box;
        }

        protected static int parseInt(EditBox box, int fallback) {
            try {
                return Integer.parseInt(box.getValue());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        protected void send(CompoundTag data, int action) {
            PacketDistributor.sendToServer(new WandConfigPayload(this.pos, data, action));
        }
    }

    private static final class StructureScreen extends WandScreen {
        private static final int LIST_WIDTH = 300;
        private static final int ROW_HEIGHT = 16;

        private final boolean load;
        private EditBox name;
        private EditBox sizeX;
        private EditBox sizeY;
        private EditBox sizeZ;
        private List<String> files = List.of();
        private int scroll;
        private int action = WandConfigPayload.ACTION_NONE;

        private StructureScreen(BlockPos pos, CompoundTag tag, boolean load) {
            super(Component.translatable(load ? "block.reinhardtshbm.wand_structure.load" : "block.reinhardtshbm.wand_structure.save"), pos, tag);
            this.load = load;
        }

        @Override
        protected void init() {
            int left = this.width / 2 - 150;
            this.name = textBox(left, 50, LIST_WIDTH, this.tag.getString("name"));
            if (this.load) {
                refreshFiles();
                this.name.setResponder(value -> {
                    refreshFiles();
                    this.scroll = Math.min(this.scroll, maxScroll());
                });
            }
            if (!this.load) {
                this.sizeX = textBox(left, 100, 50, Integer.toString(Math.max(1, this.tag.getInt("sizeX"))));
                this.sizeY = textBox(left + 50, 100, 50, Integer.toString(Math.max(1, this.tag.getInt("sizeY"))));
                this.sizeZ = textBox(left + 100, 100, 50, Integer.toString(Math.max(1, this.tag.getInt("sizeZ"))));
            }
            addRenderableWidget(Button.builder(Component.translatable(this.load ? "gui.reinhardtshbm.wand.load" : "gui.reinhardtshbm.wand.save"), button -> {
                this.action = this.load ? WandConfigPayload.ACTION_LOAD : WandConfigPayload.ACTION_SAVE;
                onClose();
            }).bounds(left, this.load ? this.height - 70 : 150, LIST_WIDTH, 20).build());
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(graphics, mouseX, mouseY, partialTick);
            int left = this.width / 2 - 150;
            graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.wand.filename"), left, 37, 0xA0A0A0, false);
            if (this.load) {
                renderFileList(graphics, mouseX, mouseY, left);
            } else {
                graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.wand.size"), left, 87, 0xA0A0A0, false);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        private void renderFileList(GuiGraphics graphics, int mouseX, int mouseY, int left) {
            int top = listTop();
            int bottom = listBottom();
            graphics.fill(left - 1, top - 1, left + LIST_WIDTH + 1, bottom + 1, 0xAA101010);
            graphics.fill(left, top, left + LIST_WIDTH, bottom, 0xAA000000);

            if (this.files.isEmpty()) {
                graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.wand.no_structures"), left + 5, top + 5, 0x808080, false);
                return;
            }

            int visibleRows = visibleRows();
            for (int row = 0; row < visibleRows; row++) {
                int index = this.scroll + row;
                if (index >= this.files.size()) {
                    break;
                }
                String file = this.files.get(index);
                int y = top + row * ROW_HEIGHT;
                boolean selected = file.equals(this.name.getValue());
                boolean hovered = mouseX >= left && mouseX < left + LIST_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT;
                if (selected || hovered) {
                    graphics.fill(left, y, left + LIST_WIDTH, y + ROW_HEIGHT, selected ? 0x80404000 : 0x50303030);
                }
                graphics.drawString(this.font, file, left + 5, y + 4, selected ? 0xFFFF55 : 0xE0E0E0, false);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.load && button == 0 && isInsideFileList(mouseX, mouseY)) {
                int index = this.scroll + (int) ((mouseY - listTop()) / ROW_HEIGHT);
                if (index >= 0 && index < this.files.size()) {
                    this.name.setValue(this.files.get(index));
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (this.load && isInsideFileList(mouseX, mouseY)) {
                this.scroll = Math.max(0, Math.min(maxScroll(), this.scroll - (int) Math.signum(scrollY)));
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public void onClose() {
            CompoundTag data = this.tag.copy();
            data.putString("name", this.name.getValue());
            if (!this.load) {
                data.putInt("sizeX", Math.max(1, parseInt(this.sizeX, data.getInt("sizeX"))));
                data.putInt("sizeY", Math.max(1, parseInt(this.sizeY, data.getInt("sizeY"))));
                data.putInt("sizeZ", Math.max(1, parseInt(this.sizeZ, data.getInt("sizeZ"))));
            }
            send(data, this.action);
            super.onClose();
        }

        private void refreshFiles() {
            String filter = this.name == null ? "" : this.name.getValue().toLowerCase(Locale.ROOT);
            Path directory = Minecraft.getInstance().gameDirectory.toPath().resolve("structures");
            try {
                Files.createDirectories(directory);
            } catch (IOException ignored) {
                this.files = List.of();
                return;
            }
            try (Stream<Path> stream = Files.list(directory)) {
                this.files = stream
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> name.endsWith(".nbt"))
                        .map(name -> name.substring(0, name.length() - 4))
                        .filter(name -> filter.isBlank() || name.toLowerCase(Locale.ROOT).contains(filter))
                        .sorted(Comparator.naturalOrder())
                        .toList();
            } catch (IOException ignored) {
                this.files = List.of();
            }
        }

        private boolean isInsideFileList(double mouseX, double mouseY) {
            int left = this.width / 2 - 150;
            return mouseX >= left && mouseX < left + LIST_WIDTH && mouseY >= listTop() && mouseY < listBottom();
        }

        private int listTop() {
            return 78;
        }

        private int listBottom() {
            return this.height - 90;
        }

        private int visibleRows() {
            return Math.max(1, (listBottom() - listTop()) / ROW_HEIGHT);
        }

        private int maxScroll() {
            return Math.max(0, this.files.size() - visibleRows());
        }
    }

    private static final class JigsawScreen extends WandScreen {
        private EditBox pool;
        private EditBox name;
        private EditBox target;
        private EditBox selection;
        private EditBox placement;
        private boolean rollable;
        private Button rollButton;

        private JigsawScreen(BlockPos pos, CompoundTag tag) {
            super(Component.translatable("block.reinhardtshbm.wand_jigsaw"), pos, tag);
            this.rollable = !tag.contains("roll") || tag.getBoolean("roll");
        }

        @Override
        protected void init() {
            int left = this.width / 2 - 150;
            this.pool = textBox(left, 50, 300, this.tag.getString("pool"));
            this.name = textBox(left, 100, 140, this.tag.getString("name"));
            this.target = textBox(left + 160, 100, 140, this.tag.getString("target"));
            this.selection = textBox(left, 150, 90, Integer.toString(this.tag.getInt("selection")));
            this.placement = textBox(left + 110, 150, 90, Integer.toString(this.tag.getInt("placement")));
            this.rollButton = Button.builder(jointName(), button -> {
                this.rollable = !this.rollable;
                this.rollButton.setMessage(jointName());
            }).bounds(left + 210, 150, 90, 20).build();
            addRenderableWidget(this.rollButton);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(graphics, mouseX, mouseY, partialTick);
            int left = this.width / 2 - 150;
            graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.wand.target_pool"), left, 37, 0xA0A0A0, false);
            graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.wand.name"), left, 87, 0xA0A0A0, false);
            graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.wand.target_name"), left + 160, 87, 0xA0A0A0, false);
            graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.wand.selection_priority"), left, 137, 0xA0A0A0, false);
            graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.wand.placement_priority"), left + 110, 137, 0xA0A0A0, false);
            graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.wand.joint_type"), left + 210, 137, 0xA0A0A0, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            CompoundTag data = this.tag.copy();
            data.putString("pool", this.pool.getValue());
            data.putString("name", this.name.getValue());
            data.putString("target", this.target.getValue());
            data.putInt("selection", parseInt(this.selection, data.getInt("selection")));
            data.putInt("placement", parseInt(this.placement, data.getInt("placement")));
            data.putBoolean("roll", this.rollable);
            send(data, WandConfigPayload.ACTION_NONE);
            super.onClose();
        }

        private Component jointName() {
            return Component.translatable(this.rollable ? "gui.reinhardtshbm.wand.rollable" : "gui.reinhardtshbm.wand.aligned");
        }
    }

    private static final class TandemScreen extends WandScreen {
        private EditBox pool;
        private EditBox target;
        private boolean rollable;
        private Button rollButton;

        private TandemScreen(BlockPos pos, CompoundTag tag) {
            super(Component.translatable("block.reinhardtshbm.wand_tandem"), pos, tag);
            this.rollable = !tag.contains("roll") || tag.getBoolean("roll");
        }

        @Override
        protected void init() {
            int left = this.width / 2 - 150;
            this.pool = textBox(left, 50, 300, this.tag.getString("pool"));
            this.target = textBox(left + 160, 100, 140, this.tag.getString("target"));
            this.rollButton = Button.builder(jointName(), button -> {
                this.rollable = !this.rollable;
                this.rollButton.setMessage(jointName());
            }).bounds(left + 210, 150, 90, 20).build();
            addRenderableWidget(this.rollButton);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(graphics, mouseX, mouseY, partialTick);
            int left = this.width / 2 - 150;
            graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.wand.target_pool"), left, 37, 0xA0A0A0, false);
            graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.wand.target_name"), left + 160, 87, 0xA0A0A0, false);
            graphics.drawString(this.font, Component.translatable("gui.reinhardtshbm.wand.joint_type"), left + 210, 137, 0xA0A0A0, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            CompoundTag data = this.tag.copy();
            data.putString("pool", this.pool.getValue());
            data.putString("target", this.target.getValue());
            data.putBoolean("roll", this.rollable);
            send(data, WandConfigPayload.ACTION_NONE);
            super.onClose();
        }

        private Component jointName() {
            return Component.translatable(this.rollable ? "gui.reinhardtshbm.wand.rollable" : "gui.reinhardtshbm.wand.aligned");
        }
    }
}
