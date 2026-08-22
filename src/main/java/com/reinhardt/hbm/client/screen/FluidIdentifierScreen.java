package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.network.SetFluidIdentifierPayload;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FluidIdentifierScreen extends Screen {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/machine/gui_fluid.png");
    private static final int WIDTH = 176;
    private static final int HEIGHT = 54;
    private static final int VISIBLE_FLUIDS = 9;

    private final ItemStack stack;
    private final List<HbmFluidDefinition> fluids = new ArrayList<>();
    private EditBox search;
    private int leftPos;
    private int topPos;
    private int startIndex;
    private HbmFluidDefinition primary;
    private HbmFluidDefinition secondary;

    public FluidIdentifierScreen(ItemStack stack) {
        super(Component.translatable("gui.reinhardtshbm.fluid_identifier"));
        this.stack = stack;
        this.primary = FluidIdentifierItem.primary(stack);
        this.secondary = FluidIdentifierItem.secondary(stack);
        regenerateFluids("");
    }

    public static void open(ItemStack stack) {
        Minecraft.getInstance().setScreen(new FluidIdentifierScreen(stack));
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = (this.height - HEIGHT) / 2;
        this.search = new EditBox(this.font, this.leftPos + 46, this.topPos + 11, 86, 12, Component.translatable("gui.reinhardtshbm.fluid_identifier.search"));
        this.search.setTextColor(0xFFFFFF);
        this.search.setTextColorUneditable(0xFFFFFF);
        this.search.setBordered(false);
        this.search.setMaxLength(32);
        this.search.setFocused(true);
        this.search.setResponder(this::regenerateFluids);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        renderWindow(guiGraphics, mouseX, mouseY);
        this.search.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderWindow(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, WIDTH, HEIGHT);
        if (this.search.isFocused()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 43, this.topPos + 7, 166, 54, 90, 18);
        }
        for (int i = 0; i < VISIBLE_FLUIDS; i++) {
            int fluidIndex = this.startIndex + i;
            if (fluidIndex >= this.fluids.size()) {
                break;
            }
            HbmFluidDefinition fluid = this.fluids.get(fluidIndex);
            int x = this.leftPos + 7 + i * 18;
            int y = this.topPos + 29;
            boolean hovered = x <= mouseX && x + 18 > mouseX && y <= mouseY && y + 18 > mouseY;

            if (hovered) {
                guiGraphics.fill(x, y, x + 18, y + 18, 0x66303030);
            }

            int color = fluid.color();
            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            guiGraphics.setColor(r, g, b, 1.0F);
            guiGraphics.blit(TEXTURE, this.leftPos + 12 + i * 18, this.topPos + 31, 12 + i * 18, 56, 8, 14);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (fluid == this.primary && fluid == this.secondary) {
                guiGraphics.blit(TEXTURE, x, y, 176, 36, 18, 18);
            } else if (fluid == this.primary) {
                guiGraphics.blit(TEXTURE, x, y, 176, 0, 18, 18);
            } else if (fluid == this.secondary) {
                guiGraphics.blit(TEXTURE, x, y, 176, 18, 18, 18);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        if (!(this.minecraft.player.getMainHandItem().getItem() instanceof FluidIdentifierItem)
                && !(this.minecraft.player.getOffhandItem().getItem() instanceof FluidIdentifierItem)) {
            this.minecraft.setScreen(null);
        }
    }

    private void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int index = fluidAt(mouseX, mouseY);
        if (index < 0) {
            return;
        }
        HbmFluidDefinition fluid = this.fluids.get(index);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(fluid.translationKey()).withStyle(ChatFormatting.YELLOW));
        if (fluid == this.primary) {
            lines.add(Component.translatable("gui.reinhardtshbm.fluid_identifier.primary").withStyle(ChatFormatting.AQUA));
        }
        if (fluid == this.secondary) {
            lines.add(Component.translatable("gui.reinhardtshbm.fluid_identifier.secondary").withStyle(ChatFormatting.GOLD));
        }
        guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.search.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int fluidIndex = fluidAt((int) mouseX, (int) mouseY);
        if (fluidIndex >= 0) {
            HbmFluidDefinition fluid = this.fluids.get(fluidIndex);
            if (button == 0) {
                setFluid(fluid, true);
                return true;
            }
            if (button == 1) {
                setFluid(fluid, false);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0.0D) {
            this.startIndex = Math.max(0, this.startIndex - 1);
            return true;
        }
        if (scrollY < 0.0D) {
            this.startIndex = Math.min(maxStartIndex(), this.startIndex + 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (this.search.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.search.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void setFluid(HbmFluidDefinition fluid, boolean primarySlot) {
        if (primarySlot) {
            this.primary = fluid;
        } else {
            this.secondary = fluid;
        }
        FluidIdentifierItem.setType(this.stack, fluid, primarySlot);
        PacketDistributor.sendToServer(new SetFluidIdentifierPayload(fluid.name(), primarySlot));
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void regenerateFluids(String query) {
        this.fluids.clear();
        String normalized = query.toLowerCase(Locale.ROOT);
        for (HbmFluidDefinition fluid : HbmFluids.niceOrder()) {
            if (!fluid.allowsFluidIdentifier()) {
                continue;
            }
            if (normalized.isBlank()
                    || fluid.name().contains(normalized)
                    || Component.translatable(fluid.translationKey()).getString().toLowerCase(Locale.ROOT).contains(normalized)) {
                this.fluids.add(fluid);
            }
        }
        this.startIndex = Mth.clamp(this.startIndex, 0, maxStartIndex());
    }

    private int fluidAt(int mouseX, int mouseY) {
        int y = this.topPos + 29;
        if (mouseY < y || mouseY >= y + 18) {
            return -1;
        }
        for (int i = 0; i < VISIBLE_FLUIDS; i++) {
            int x = this.leftPos + 7 + i * 18;
            if (mouseX >= x && mouseX < x + 18) {
                int index = this.startIndex + i;
                return index < this.fluids.size() ? index : -1;
            }
        }
        return -1;
    }

    private int maxStartIndex() {
        return Math.max(0, this.fluids.size() - VISIBLE_FLUIDS);
    }
}
