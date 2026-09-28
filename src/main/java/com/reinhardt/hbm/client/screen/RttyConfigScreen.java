package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.RadioTorchBlock;
import com.reinhardt.hbm.blockentity.RadioTorchBlockEntity;
import com.reinhardt.hbm.client.screen.widget.SearchEditBox;
import com.reinhardt.hbm.menu.RttyConfigMenu;
import com.reinhardt.hbm.network.RttyConfigPayload;
import com.reinhardt.hbm.rtty.RttyInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Exact 1.7.10 layouts for the RTTY controller and eight-channel value reader. */
public final class RttyConfigScreen extends AbstractContainerScreen<RttyConfigMenu> {
    private static final ResourceLocation CONTROLLER_TEXTURE =
            ReinhardtsHBM.id("textures/gui/machine/gui_rtty_controller.png");
    private static final ResourceLocation READER_TEXTURE =
            ReinhardtsHBM.id("textures/gui/machine/gui_rtty_reader.png");
    private static final int TEXTURE_SIZE = 256;
    private EditBox controllerChannel;
    private EditBox[] readerChannels;
    private EditBox[] readerNames;
    private boolean polling;

    public RttyConfigScreen(RttyConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = menu.kind() == RadioTorchBlock.Kind.READER ? 204 : 42;
    }

    @Override
    protected void init() {
        super.init();
        RadioTorchBlockEntity radio = radio();
        polling = radio != null && radio.polling();
        if (menu.kind() == RadioTorchBlock.Kind.CONTROLLER) {
            controllerChannel = textField(leftPos + 29, topPos + 21, 82,
                    RadioTorchBlockEntity.CHANNEL_LIMIT, radio == null ? "" : radio.channel());
            addRenderableWidget(controllerChannel);
        } else {
            readerChannels = new EditBox[8];
            readerNames = new EditBox[8];
            for (int index = 0; index < 8; index++) {
                readerChannels[index] = textField(leftPos + 29, topPos + 57 + index * 18, 64,
                        RadioTorchBlockEntity.CHANNEL_LIMIT,
                        radio == null ? "" : radio.readerChannel(index));
                readerNames[index] = textField(leftPos + 123, topPos + 57 + index * 18, 118,
                        RadioTorchBlockEntity.VALUE_NAME_LIMIT,
                        radio == null ? "" : radio.readerName(index));
                addRenderableWidget(readerChannels[index]);
                addRenderableWidget(readerNames[index]);
            }
        }
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    private EditBox textField(int x, int y, int width, int maxLength, String value) {
        SearchEditBox field = new SearchEditBox(font, x, y, width, 14, Component.empty());
        field.setTextColor(0x00FF00);
        field.setTextColorUneditable(0x00FF00);
        field.setBordered(false);
        field.setMaxLength(maxLength);
        field.setValue(value);
        return field;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = menu.kind() == RadioTorchBlock.Kind.READER ? READER_TEXTURE : CONTROLLER_TEXTURE;
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        if (polling) {
            int v = menu.kind() == RadioTorchBlock.Kind.READER ? 204 : 42;
            graphics.blit(texture, leftPos + 173, topPos + 17, 0, v, 18, 18, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, 6, 0x404040, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovering(173, 17, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, List.of(Component.translatable(
                    polling ? "gui.reinhardtshbm.rtty.polling" : "gui.reinhardtshbm.rtty.state_change")), mouseX, mouseY);
            return;
        }
        if (isHovering(209, 17, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font,
                    List.of(Component.translatable("gui.reinhardtshbm.radio.save")), mouseX, mouseY);
            return;
        }
        int infoX = menu.kind() == RadioTorchBlock.Kind.READER ? 29 : 137;
        if (isHovering(infoX, 17, 18, 18, mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, informationLines(), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private List<Component> informationLines() {
        boolean reader = menu.kind() == RadioTorchBlock.Kind.READER;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(reader
                ? "gui.reinhardtshbm.rtty.readable_values"
                : "gui.reinhardtshbm.rtty.usable_functions"));
        String prefix = reader ? RttyInfo.PREFIX_VALUE : RttyInfo.PREFIX_FUNCTION;
        ChatFormatting color = reader ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA;
        RadioTorchBlockEntity radio = radio();
        if (radio != null) {
            for (String entry : radio.supportedFunctions()) {
                if (entry.startsWith(prefix)) lines.add(Component.literal(entry.substring(prefix.length())).withStyle(color));
            }
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovering(173, 17, 18, 18, mouseX, mouseY)) {
            polling = !polling;
            PacketDistributor.sendToServer(new RttyConfigPayload(menu.blockPos(),
                    RttyConfigPayload.ACTION_POLLING, polling, "", List.of(), List.of()));
            click();
            return true;
        }
        if (button == 0 && isHovering(209, 17, 18, 18, mouseX, mouseY)) {
            save();
            click();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void save() {
        if (menu.kind() == RadioTorchBlock.Kind.CONTROLLER) {
            PacketDistributor.sendToServer(new RttyConfigPayload(menu.blockPos(),
                    RttyConfigPayload.ACTION_CONTROLLER_SAVE, polling,
                    controllerChannel == null ? "" : controllerChannel.getValue(), List.of(), List.of()));
            return;
        }
        List<String> channels = new ArrayList<>(8);
        List<String> names = new ArrayList<>(8);
        for (int index = 0; index < 8; index++) {
            channels.add(readerChannels[index].getValue());
            names.add(readerNames[index].getValue());
        }
        PacketDistributor.sendToServer(new RttyConfigPayload(menu.blockPos(),
                RttyConfigPayload.ACTION_READER_SAVE, polling, "", channels, names));
    }

    private RadioTorchBlockEntity radio() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.blockPos()) instanceof RadioTorchBlockEntity radio ? radio : null;
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
