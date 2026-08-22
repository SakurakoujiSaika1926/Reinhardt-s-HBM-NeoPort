package com.reinhardt.hbm.client.screen;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.FusionMachineBlock;
import com.reinhardt.hbm.blockentity.FusionMachineBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.menu.FusionMachineMenu;
import com.reinhardt.hbm.network.FusionMachineControlPayload;
import com.reinhardt.hbm.recipe.PlasmaForgeRecipe;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.util.HbmFluidTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FusionMachineScreen extends AbstractContainerScreen<FusionMachineMenu> {
    private static final ResourceLocation TORUS = ReinhardtsHBM.id("textures/gui/reactors/gui_fusion_torus.png");
    private static final ResourceLocation KLYSTRON = ReinhardtsHBM.id("textures/gui/reactors/gui_fusion_klystron.png");
    private static final ResourceLocation BREEDER = ReinhardtsHBM.id("textures/gui/reactors/gui_fusion_breeder.png");
    private static final ResourceLocation PLASMA_FORGE = ReinhardtsHBM.id("textures/gui/reactors/gui_fusion_plasmaforge.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static final float GHOST_ALPHA = 0.20F;

    @Nullable
    private EditBox klystronTarget;

    public FusionMachineScreen(FusionMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        switch (menu.kind()) {
            case TORUS -> {
                this.imageWidth = 230;
                this.imageHeight = 244;
                this.inventoryLabelX = 35;
                this.inventoryLabelY = 151;
            }
            case KLYSTRON -> {
                this.imageWidth = 194;
                this.imageHeight = 200;
                this.inventoryLabelX = 35;
                this.inventoryLabelY = 107;
            }
            case PLASMA_FORGE -> {
                this.imageWidth = 176;
                this.imageHeight = 244;
                this.inventoryLabelX = 8;
                this.inventoryLabelY = 150;
            }
            default -> {
                this.imageWidth = 176;
                this.imageHeight = 200;
                this.inventoryLabelX = 35;
                this.inventoryLabelY = 107;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = switch (this.menu.kind()) {
            case TORUS -> 106 - this.font.width(this.title) / 2;
            case KLYSTRON -> 115 - this.font.width(this.title) / 2;
            case PLASMA_FORGE -> 70 - this.font.width(this.title) / 2;
            default -> this.imageWidth / 2 - this.font.width(this.title) / 2;
        };
        if (this.menu.kind() == FusionMachineBlock.Kind.KLYSTRON) {
            this.klystronTarget = new EditBox(this.font, this.leftPos + 84, this.topPos + 22, 102, 12, Component.empty());
            this.klystronTarget.setBordered(false);
            this.klystronTarget.setTextColor(0x00FF00);
            this.klystronTarget.setTextColorUneditable(0x00FF00);
            this.klystronTarget.setMaxLength(12);
            this.klystronTarget.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
            this.klystronTarget.setValue(Long.toString(this.menu.outputTarget()));
            this.klystronTarget.setResponder(this::sendKlystronTarget);
            this.addWidget(this.klystronTarget);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.menu.kind() == FusionMachineBlock.Kind.TORUS && in(mouseX, mouseY, 43, 80, 18, 18)) {
            playClick();
            if (this.minecraft != null) {
                this.minecraft.setScreen(new FusionRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        if (this.menu.kind() == FusionMachineBlock.Kind.PLASMA_FORGE && in(mouseX, mouseY, 7, 80, 18, 18)) {
            playClick();
            if (this.minecraft != null) {
                this.minecraft.setScreen(new PlasmaForgeRecipeSelectorScreen(this, this.menu));
            }
            return true;
        }
        if (this.klystronTarget != null && this.klystronTarget.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.klystronTarget);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.klystronTarget != null && this.klystronTarget.isFocused() && this.klystronTarget.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.klystronTarget != null && this.klystronTarget.isFocused() && this.klystronTarget.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.klystronTarget != null) {
            this.klystronTarget.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderHoverTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        switch (this.menu.kind()) {
            case TORUS -> renderTorus(guiGraphics);
            case KLYSTRON -> renderKlystron(guiGraphics);
            case BREEDER -> renderBreeder(guiGraphics);
            case PLASMA_FORGE -> renderPlasmaForge(guiGraphics);
            default -> {
            }
        }
    }

    private void renderTorus(GuiGraphics graphics) {
        graphics.blit(TORUS, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);
        int power = this.menu.powerScaled(62);
        if (power > 0) {
            graphics.blit(TORUS, this.leftPos + 8, this.topPos + 80 - power, 230, 62 - power, 16, power, TEX_W, TEX_H);
        }
        int progress = this.menu.progressScaled(70);
        if (progress > 0) {
            graphics.blit(TORUS, this.leftPos + 98, this.topPos + 81, 0, 244, progress, 6, TEX_W, TEX_H);
        }
        if (this.menu.didProcess()) {
            graphics.blit(TORUS, this.leftPos + 160, this.topPos + 115, 246, 14, 8, 8, TEX_W, TEX_H);
            graphics.blit(TORUS, this.leftPos + 170, this.topPos + 115, 246, 14, 8, 8, TEX_W, TEX_H);
            graphics.blit(TORUS, this.leftPos + 180, this.topPos + 115, 246, 14, 8, 8, TEX_W, TEX_H);
            graphics.blit(TORUS, this.leftPos + 87, this.topPos + 76, 249, 0, 3, 6, TEX_W, TEX_H);
            graphics.blit(TORUS, this.leftPos + 92, this.topPos + 76, 249, 0, 3, 6, TEX_W, TEX_H);
        } else if (this.menu.selectedRecipeIndex() >= 0) {
            graphics.blit(TORUS, this.leftPos + 87, this.topPos + 76, 246, 0, 3, 6, TEX_W, TEX_H);
            graphics.blit(TORUS, this.leftPos + 92, this.topPos + 76, 246, 0, 3, 6, TEX_W, TEX_H);
        }
        drawGauge(graphics, 52, 124, ratio(this.menu.klystronEnergy(), this.menu.selectedFusionIgnitionTemp()), 5, 2, 0xFFA00000);
        drawGauge(graphics, 88, 124, ratio(this.menu.plasmaEnergy(), this.menu.selectedFusionOutputTemp()), 5, 2, 0xFFA00000);
        drawGauge(graphics, 124, 124, this.menu.fuelConsumption() / 10_000.0D, 5, 2, 0xFFA00000);
        graphics.renderItem(this.menu.recipeIcon(), this.leftPos + 44, this.topPos + 81);
        drawFluid(graphics, 44, 70, 16, 52, 0);
        drawFluid(graphics, 62, 70, 16, 52, 1);
        drawFluid(graphics, 80, 70, 16, 52, 2);
        drawFluid(graphics, 152, 70, 16, 52, 3);
        drawFluid(graphics, 188, 98, 16, 52, 4);
        drawFluid(graphics, 206, 98, 16, 52, 5);
    }

    private void renderKlystron(GuiGraphics graphics) {
        graphics.blit(KLYSTRON, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);
        int power = this.menu.powerScaled(52);
        if (power > 0) {
            graphics.blit(KLYSTRON, this.leftPos + 8, this.topPos + 70 - power, 194, 52 - power, 16, power, TEX_W, TEX_H);
        }
        double outputGauge = ratio(this.menu.output(), this.menu.outputTarget());
        double airGauge = ratio(this.menu.tankAmount(0), this.menu.tankCapacity(0));
        double powerGauge = ratio(this.menu.power(), this.menu.maxPower());
        if (powerGauge > 0.0D) {
            graphics.blit(KLYSTRON, this.leftPos + 160, this.topPos + 71, 210, this.menu.output() > 0 ? 8 : 0, 8, 8, TEX_W, TEX_H);
        }
        if (airGauge > 0.0D) {
            graphics.blit(KLYSTRON, this.leftPos + 170, this.topPos + 71, 210, this.menu.output() > 0 ? 8 : 0, 8, 8, TEX_W, TEX_H);
        }
        if (this.menu.output() > 0) {
            graphics.blit(KLYSTRON, this.leftPos + 180, this.topPos + 71, 210, this.menu.output() >= this.menu.outputTarget() ? 8 : 0, 8, 8, TEX_W, TEX_H);
        }
        drawGauge(graphics, 52, 80, outputGauge, 5, 2, 0xFFA00000);
        drawGauge(graphics, 88, 80, airGauge, 5, 2, 0xFFA00000);
        drawGauge(graphics, 124, 80, powerGauge, 5, 2, 0xFFA00000);
    }

    private void renderBreeder(GuiGraphics graphics) {
        graphics.blit(BREEDER, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);
        int progress = this.menu.progressScaled(42);
        if (progress > 0) {
            graphics.blit(BREEDER, this.leftPos + 67, this.topPos + 48, 176, 0, progress, 10, TEX_W, TEX_H);
        }
        double gauge = 1.0D - Math.pow(Math.E, -this.menu.neutronEnergy() * 10.0D / FusionMachineBlockEntity.BREEDER_CAPACITY);
        drawGauge(graphics, 88, 32, gauge, 5, 2, 0xFFA00000);
        drawFluid(graphics, 26, 70, 16, 52, 0);
        drawFluid(graphics, 134, 70, 16, 52, 1);
    }

    private void renderPlasmaForge(GuiGraphics graphics) {
        graphics.blit(PLASMA_FORGE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_W, TEX_H);
        int power = this.menu.powerScaled(62);
        if (power > 0) {
            graphics.blit(PLASMA_FORGE, this.leftPos + 152, this.topPos + 80 - power, 176, 62 - power, 16, power, TEX_W, TEX_H);
        }
        if (this.menu.progress() > 0) {
            graphics.blit(PLASMA_FORGE, this.leftPos + 62, this.topPos + 81, 176, 62, this.menu.progressScaled(70), 16, TEX_W, TEX_H);
        }
        if (this.menu.didProcess()) {
            graphics.blit(PLASMA_FORGE, this.leftPos + 51, this.topPos + 76, 195, 0, 3, 6, TEX_W, TEX_H);
            graphics.blit(PLASMA_FORGE, this.leftPos + 56, this.topPos + 76, 195, 0, 3, 6, TEX_W, TEX_H);
        }
        drawGauge(graphics, 34, 124, ratio(this.menu.plasmaEnergy(), this.menu.selectedPlasmaForgeIgnitionTemp()), 5, 2, 0xFFA00000);
        drawGauge(graphics, 70, 124, this.menu.booster() / (double) this.menu.maxBooster(), 5, 2, 0xFFA00000);
        graphics.renderItem(this.menu.recipeIcon(), this.leftPos + 8, this.topPos + 81);
        renderPlasmaForgeGhosts(graphics);
        drawFluid(graphics, 80, 70, 16, 52, 0);
    }

    private void renderPlasmaForgeGhosts(GuiGraphics graphics) {
        RecipeHolder<PlasmaForgeRecipe> holder = this.menu.selectedPlasmaForgeRecipe().orElse(null);
        if (holder == null) {
            return;
        }
        List<PlasmaForgeRecipe.CountedIngredient> inputs = holder.value().inputItems();
        for (int index = 0; index < inputs.size(); index++) {
            int slotIndex = FusionMachineBlockEntity.PLASMA_INPUT_START + index;
            if (slotIndex >= this.menu.slots.size()) {
                break;
            }
            Slot slot = this.menu.slots.get(slotIndex);
            if (slot.hasItem()) {
                continue;
            }
            ItemStack ghost = PlasmaForgeRecipeSelectorScreen.displayIngredient(inputs.get(index));
            GhostItemRenderer.render(graphics, ghost, this.leftPos + slot.x, this.topPos + slot.y, GHOST_ALPHA);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        if (this.menu.kind() == FusionMachineBlock.Kind.TORUS) {
            int heat = this.menu.temperature();
            int color = heat > 123 ? 0xAA0000 : 0x00AAAA;
            String label = heat + "K";
            graphics.drawString(this.font, label, 220 - this.font.width(label), 22, color, false);
            graphics.drawString(this.font, "/123K", 190, 32, 0x00AAAA, false);
        } else if (this.menu.kind() == FusionMachineBlock.Kind.KLYSTRON) {
            String result = "= " + HbmFluidTooltip.shortNumber(this.menu.outputTarget()) + "KyU";
            if (this.menu.outputTarget() == FusionMachineBlockEntity.KLYSTRON_MAX_OUTPUT) {
                result += " (max)";
            }
            graphics.drawString(this.font, result, 183 - this.font.width(result), 40, 0x00FF00, false);
        }
    }

    private void renderHoverTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.menu.kind() == FusionMachineBlock.Kind.TORUS) {
            energyTooltip(graphics, mouseX, mouseY, 8, 18, 16, 62);
            tankTooltip(graphics, mouseX, mouseY, 44, 18, 16, 52, 0);
            tankTooltip(graphics, mouseX, mouseY, 62, 18, 16, 52, 1);
            tankTooltip(graphics, mouseX, mouseY, 80, 18, 16, 52, 2);
            tankTooltip(graphics, mouseX, mouseY, 152, 18, 16, 52, 3);
            tankTooltip(graphics, mouseX, mouseY, 188, 46, 16, 52, 4);
            tankTooltip(graphics, mouseX, mouseY, 206, 46, 16, 52, 5);
            if (isHovering(43, 80, 18, 18, mouseX, mouseY)) {
                graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.reinhardtshbm.fusion.select_recipe")), mouseX, mouseY);
            }
        } else if (this.menu.kind() == FusionMachineBlock.Kind.KLYSTRON) {
            energyTooltip(graphics, mouseX, mouseY, 8, 18, 16, 52);
            tankTooltip(graphics, mouseX, mouseY, 76, 71, 18, 18, 0);
        } else if (this.menu.kind() == FusionMachineBlock.Kind.BREEDER) {
            tankTooltip(graphics, mouseX, mouseY, 26, 18, 16, 52, 0);
            tankTooltip(graphics, mouseX, mouseY, 134, 18, 16, 52, 1);
        } else if (this.menu.kind() == FusionMachineBlock.Kind.PLASMA_FORGE) {
            energyTooltip(graphics, mouseX, mouseY, 152, 18, 16, 62);
            tankTooltip(graphics, mouseX, mouseY, 80, 18, 16, 52, 0);
            if (isHovering(7, 80, 18, 18, mouseX, mouseY)) {
                RecipeHolder<PlasmaForgeRecipe> holder = this.menu.selectedPlasmaForgeRecipe().orElse(null);
                if (holder != null) {
                    graphics.renderComponentTooltip(this.font, PlasmaForgeRecipeSelectorScreen.describeRecipe(holder), mouseX, mouseY);
                } else {
                    graphics.renderComponentTooltip(this.font, List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)), mouseX, mouseY);
                }
            }
            if (isHovering(25, 115, 18, 18, mouseX, mouseY)) {
                graphics.renderComponentTooltip(this.font, List.of(Component.literal(HbmFluidTooltip.shortNumber(this.menu.plasmaEnergy())
                        + " TU / "
                        + HbmFluidTooltip.shortNumber(this.menu.selectedPlasmaForgeIgnitionTemp())
                        + " TU")), mouseX, mouseY);
            }
        }
    }

    private void tankTooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height, int tank) {
        if (isHovering(x, y, width, height, mouseX, mouseY)) {
            HbmFluidDefinition fluid = fluid(tank);
            graphics.renderComponentTooltip(this.font, HbmFluidTooltip.forTank(fluid, this.menu.tankAmount(tank), this.menu.tankCapacity(tank)), mouseX, mouseY);
        }
    }

    private void energyTooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        if (isHovering(x, y, width, height, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.translatable(
                    "tooltip.reinhardtshbm.energy",
                    this.menu.power(),
                    this.menu.maxPower()
            )), mouseX, mouseY);
        }
    }

    private void drawFluid(GuiGraphics graphics, int x, int bottomY, int width, int height, int tank) {
        int scaled = this.menu.tankScaled(tank, height);
        HbmFluidDefinition fluid = fluid(tank);
        if (scaled <= 0 || fluid.isNone()) {
            return;
        }
        graphics.fill(this.leftPos + x, this.topPos + bottomY - scaled, this.leftPos + x + width, this.topPos + bottomY, 0xFF000000 | fluid.color());
        graphics.fill(this.leftPos + x, this.topPos + bottomY - scaled, this.leftPos + x + width, this.topPos + bottomY - scaled + 1, 0x66FFFFFF);
    }

    private HbmFluidDefinition fluid(int tank) {
        return HbmFluids.byOldId(this.menu.tankFluidId(tank)).orElse(HbmFluids.none());
    }

    private void drawGauge(GuiGraphics graphics, int centerX, int centerY, double value, int radius, int thickness, int color) {
        double clamped = Math.max(0.0D, Math.min(1.0D, value));
        int width = (int) Math.round((radius * 2 + 1) * clamped);
        if (width <= 0) {
            return;
        }
        int left = this.leftPos + centerX - radius;
        int top = this.topPos + centerY - thickness / 2;
        graphics.fill(left, top, left + width, top + Math.max(1, thickness), color);
    }

    private static double ratio(long value, long max) {
        return max <= 0L ? 0.0D : Math.min(1.0D, value / (double) max);
    }

    private boolean in(double mouseX, double mouseY, int x, int y, int width, int height) {
        double left = this.leftPos + x;
        double top = this.topPos + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private void playClick() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void sendKlystronTarget(String value) {
        long target = 0L;
        if (!value.isBlank()) {
            try {
                target = Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                target = FusionMachineBlockEntity.KLYSTRON_MAX_OUTPUT;
            }
        }
        PacketDistributor.sendToServer(new FusionMachineControlPayload(this.menu.blockPos(), FusionMachineControlPayload.ACTION_SET_KLYSTRON_TARGET, target));
    }
}
