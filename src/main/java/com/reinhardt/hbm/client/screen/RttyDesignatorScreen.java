package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.LegacyRttyPagerItem;
import com.reinhardt.hbm.network.SetRttyDesignatorChannelPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Original 184x42 RTTY designator channel editor. */
public final class RttyDesignatorScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            ReinhardtsHBM.id("textures/gui/machine/gui_rtty_designator.png");
    private static final int IMAGE_WIDTH = 184;
    private static final int IMAGE_HEIGHT = 42;

    private final ItemStack designator;
    private final InteractionHand hand;
    private EditBox channel;
    private int left;
    private int top;

    private RttyDesignatorScreen(ItemStack designator, InteractionHand hand) {
        super(Component.translatable("container.reinhardtshbm.rtty_designator"));
        this.designator = designator;
        this.hand = hand;
    }

    public static void open(ItemStack designator, InteractionHand hand) {
        Minecraft.getInstance().setScreen(new RttyDesignatorScreen(designator, hand));
    }

    @Override
    protected void init() {
        left = (width - IMAGE_WIDTH) / 2;
        top = (height - IMAGE_HEIGHT) / 2;
        channel = new EditBox(font, left + 31, top + 23, 82, 12, Component.empty());
        channel.setMaxLength(LegacyRttyPagerItem.MAX_CHANNEL_LENGTH);
        channel.setValue(LegacyRttyPagerItem.channel(designator));
        addRenderableWidget(channel);
        setInitialFocus(channel);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(TEXTURE, left, top, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        graphics.drawString(font, title, left + (IMAGE_WIDTH - font.width(title)) / 2, top + 6, 0x404000, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= left + 137 && mouseX < left + 155 && mouseY >= top + 17 && mouseY < top + 35) {
            applyChannel();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            applyChannel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void applyChannel() {
        LegacyRttyPagerItem.setChannel(designator, channel.getValue());
        PacketDistributor.sendToServer(new SetRttyDesignatorChannelPayload(
                hand == InteractionHand.OFF_HAND, channel.getValue()));
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.level().playSound(minecraft.player, minecraft.player.blockPosition(),
                    SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
