package com.reinhardt.hbm.client.screen.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Search field that keeps a printable inventory-key binding available for text input.
 *
 * <p>Keyboard characters are delivered after {@link #keyPressed(int, int, int)} through
 * {@code charTyped}.  A normal {@link EditBox} therefore returns {@code false} for a letter
 * such as the default inventory key {@code E}; an enclosing screen can then close before the
 * character event reaches the field.  Consuming only the configured inventory key while this
 * search field is focused prevents that close without swallowing the later character event.</p>
 */
public final class SearchEditBox extends EditBox {
    public SearchEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (this.isFocused() && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
