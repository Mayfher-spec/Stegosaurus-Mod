package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Boton dibujado con el atlas de la interfaz de talentos (9-slice horizontal). */
public final class TexButton extends Button {
    static final ResourceLocation TEX = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/talents.png");
    public boolean gold = false;

    public TexButton(int x, int y, int w, int h, Component label, OnPress onPress) {
        super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
    }

    @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        int u = !this.active ? 96 : gold ? 144 : this.isHoveredOrFocused() ? 48 : 0;
        int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();
        g.blit(TEX, x, y, 4, h, (float) u, 100.0F, 4, 20, 256, 256);
        g.blit(TEX, x + 4, y, w - 8, h, (float) (u + 4), 100.0F, 40, 20, 256, 256);
        g.blit(TEX, x + w - 4, y, 4, h, (float) (u + 44), 100.0F, 4, 20, 256, 256);
        int color = !this.active ? 0xFF7A7F85 : gold ? 0xFFFFE9A8 : 0xFFFFFFFF;
        g.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), x + w / 2, y + (h - 8) / 2, color);
    }
}
