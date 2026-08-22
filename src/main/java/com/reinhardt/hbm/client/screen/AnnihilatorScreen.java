package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.item.FluidIdentifierItem;
import com.reinhardt.hbm.menu.AnnihilatorMenu;
import com.reinhardt.hbm.network.AnnihilatorControlPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** Direct layout port of GUIMachineAnnihilator. */
public final class AnnihilatorScreen extends AbstractContainerScreen<AnnihilatorMenu> {
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("textures/gui/processing/gui_annihilator.png");
    private EditBox pool;

    public AnnihilatorScreen(AnnihilatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 208;
        this.inventoryLabelY = 114;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 70 - this.font.width(this.title) / 2;
        this.pool = new EditBox(this.font, this.leftPos + 31, this.topPos + 84, 80, 10, Component.empty());
        this.pool.setTextColor(0x00FF00);
        this.pool.setTextColorUneditable(0x00FF00);
        this.pool.setBordered(false);
        this.pool.setMaxLength(20);
        this.pool.setValue(this.menu.annihilatorPool());
        this.pool.setResponder(value -> PacketDistributor.sendToServer(new AnnihilatorControlPayload(this.menu.blockPos(), value)));
        addRenderableWidget(this.pool);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        // GUIMachineAnnihilator checks this display area, not the monitor slot's raw position.
        if (isHovering(151, 35, 18, 18, mouseX, mouseY)) {
            renderMonitorTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, 6, 0xFF404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0xFF404040, false);
    }

    private void renderMonitorTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack stack = this.menu.annihilatorMonitorStack();
        if (stack.isEmpty()) {
            return;
        }
        Component name = stack.getHoverName();
        if (stack.getItem() instanceof FluidIdentifierItem) {
            HbmFluidDefinition fluid = FluidIdentifierItem.primary(stack);
            if (!fluid.isNone()) {
                name = Component.translatable(fluid.translationKey());
            }
        }
        String amount = NumberFormat.getIntegerInstance(Locale.US).format(this.menu.annihilatorMonitor());
        graphics.renderComponentTooltip(this.font, List.of(Component.literal(name.getString() + ":"), Component.literal(amount)), mouseX, mouseY);
    }
}
