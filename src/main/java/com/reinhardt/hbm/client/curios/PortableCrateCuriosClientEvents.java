package com.reinhardt.hbm.client.curios;

import com.reinhardt.hbm.integration.curios.PortableCrateCuriosIntegration;
import com.reinhardt.hbm.integration.curios.PortableCrateStorage;
import com.reinhardt.hbm.network.PortableCrateOpenPayload;
import com.reinhardt.hbm.network.PortableCrateSlotClickPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class PortableCrateCuriosClientEvents {
    private static final int MAPPED_SLOT_SIZE = 20;
    private static Screen trackedScreen;
    private static Button openButton;
    private static MappedCrateSlot mappedSlot;

    private PortableCrateCuriosClientEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PortableCrateCuriosClientEvents::onScreenInit);
        NeoForge.EVENT_BUS.addListener(PortableCrateCuriosClientEvents::onScreenRenderPre);
        NeoForge.EVENT_BUS.addListener(PortableCrateCuriosClientEvents::onScreenRenderPost);
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        trackedScreen = null;
        openButton = null;
        mappedSlot = null;

        if (!(event.getScreen() instanceof AbstractContainerScreen<?> inventoryScreen)
                || Minecraft.getInstance().player == null
                || inventoryScreen.getMenu() != Minecraft.getInstance().player.inventoryMenu) {
            return;
        }

        trackedScreen = inventoryScreen;
        mappedSlot = new MappedCrateSlot();
        openButton = Button.builder(Component.translatable("gui.reinhardtshbm.portable_crate.open"),
                        button -> PacketDistributor.sendToServer(new PortableCrateOpenPayload()))
                .bounds(0, 0, 28, 14)
                .build();
        positionWidgets(inventoryScreen);
        event.addListener(mappedSlot);
        event.addListener(openButton);
    }

    private static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (event.getScreen() != trackedScreen || openButton == null || mappedSlot == null
                || !(trackedScreen instanceof AbstractContainerScreen<?> inventoryScreen)) {
            return;
        }
        positionWidgets(inventoryScreen);
        openButton.active = PortableCrateStorage.isCrate(mappedSlot.stack());
    }

    private static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (event.getScreen() != trackedScreen || mappedSlot == null || !mappedSlot.isHovered()) {
            return;
        }
        ItemStack stack = mappedSlot.stack();
        if (stack.isEmpty()) {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,
                    Component.translatable("curios.identifier.hbm_crate"),
                    event.getMouseX(), event.getMouseY());
        } else {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, stack,
                    event.getMouseX(), event.getMouseY());
        }
    }

    private static void positionWidgets(AbstractContainerScreen<?> screen) {
        int slotX = screen.getGuiLeft() + screen.getXSize() + 6;
        if (slotX + MAPPED_SLOT_SIZE + 4 > screen.width) {
            slotX = screen.getGuiLeft() - MAPPED_SLOT_SIZE - 6;
        }
        int slotY = screen.getGuiTop() + 84;
        mappedSlot.setPosition(slotX, slotY);
        openButton.setPosition(slotX - 4, slotY + MAPPED_SLOT_SIZE + 2);
    }

    private static IDynamicStackHandler crateHandler() {
        if (Minecraft.getInstance().player == null) {
            return null;
        }
        return CuriosApi.getCuriosInventory(Minecraft.getInstance().player)
                .flatMap(inventory -> inventory.getStacksHandler(PortableCrateCuriosIntegration.SLOT_ID))
                .map(stacks -> stacks.getStacks())
                .orElse(null);
    }

    private static final class MappedCrateSlot extends AbstractWidget {
        private MappedCrateSlot() {
            super(0, 0, MAPPED_SLOT_SIZE, MAPPED_SLOT_SIZE,
                    Component.translatable("curios.identifier.hbm_crate"));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            renderPanel(graphics, x, y);
            graphics.fill(x, y, x + MAPPED_SLOT_SIZE, y + MAPPED_SLOT_SIZE, 0xFF000000);
            graphics.fill(x + 1, y + 1, x + MAPPED_SLOT_SIZE - 1, y + MAPPED_SLOT_SIZE - 1, 0xFFFFFFFF);
            graphics.fill(x + 1, y + 1, x + MAPPED_SLOT_SIZE - 2, y + MAPPED_SLOT_SIZE - 2, 0xFF373737);
            graphics.fill(x + 2, y + 2, x + MAPPED_SLOT_SIZE - 2, y + MAPPED_SLOT_SIZE - 2, 0xFF8B8B8B);
            if (isHovered()) {
                graphics.fill(x + 2, y + 2, x + MAPPED_SLOT_SIZE - 2, y + MAPPED_SLOT_SIZE - 2, 0x80FFFFFF);
            }

            ItemStack stack = stack();
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x + 2, y + 2);
                graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x + 2, y + 2);
            }
        }

        private static void renderPanel(GuiGraphics graphics, int slotX, int slotY) {
            int left = slotX - 6;
            int top = slotY - 6;
            int right = slotX + MAPPED_SLOT_SIZE + 6;
            int bottom = slotY + MAPPED_SLOT_SIZE + 22;
            graphics.fill(left, top, right, bottom, 0xFF000000);
            graphics.fill(left + 1, top + 1, right - 1, bottom - 1, 0xFFFFFFFF);
            graphics.fill(left + 2, top + 2, right - 1, bottom - 1, 0xFF555555);
            graphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0xFFC6C6C6);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!this.visible || !this.active || button < 0 || button > 1 || !clicked(mouseX, mouseY)) {
                return false;
            }
            playDownSound(Minecraft.getInstance().getSoundManager());
            PacketDistributor.sendToServer(new PortableCrateSlotClickPayload());
            return true;
        }

        private ItemStack stack() {
            IDynamicStackHandler handler = crateHandler();
            return handler == null || handler.getSlots() <= 0 ? ItemStack.EMPTY : handler.getStackInSlot(0);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
