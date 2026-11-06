package com.prodigytown.creatures.client;

import com.prodigytown.creatures.PetOrders;
import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.net.ModNetwork;
import com.prodigytown.creatures.progress.ProdigyPet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.TamableAnimal;

import java.util.List;

/** Pestana de ordenes (adulto y chibi): seguir, quieto, deambular, patrullar, sentarse, dormir. Solo cliente; el servidor valida. */
public final class OrdersScreen extends Screen {
    private static final int W = 300, H = 196;
    private static final int GOLD = 0xFFFFD35A, DIM = 0xFF9AA0A6;
    private final TamableAnimal pet;
    private final StegosaurusEntity adult;
    private int left, top;
    private final TexButton[] buttons = new TexButton[PetOrders.COUNT];

    public OrdersScreen(TamableAnimal pet) {
        super(Component.literal("Ordenes"));
        this.pet = pet;
        this.adult = pet instanceof StegosaurusEntity s ? s : null;
    }

    private PetOrders.Holder holder() { return (PetOrders.Holder) pet; }

    @Override protected void init() {
        left = (this.width - W) / 2;
        top = (this.height - H) / 2;
        for (int i = 0; i < PetOrders.COUNT; i++) {
            final int order = i;
            int x = left + 12 + (i % 3) * 96, y = top + 58 + (i / 3) * 30;
            buttons[i] = this.addRenderableWidget(new TexButton(x, y, 92, 24, Component.literal(PetOrders.NAMES[i]), b ->
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.SetOrder(pet.getId(), order))));
        }
        if (adult != null) {
            this.addRenderableWidget(new TexButton(left + W - 96, top + 8, 84, 16, Component.literal("Habilidades"), b ->
                Minecraft.getInstance().setScreen(new TalentScreen(adult))));
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFB8862A);
        g.fill(left, top, left + W, top + H, 0xFF14161A);
        g.fill(left + 4, top + 4, left + W - 4, top + H - 4, 0xFF1C1F25);
        boolean fallen = holder().isFallen();
        int cur = holder().getOrder();
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].gold = !fallen && i == cur;
            buttons[i].active = !fallen;
        }
        String name = pet.hasCustomName() ? pet.getCustomName().getString() : (adult != null ? "Estegosaurio" : "Chibi estegosaurio");
        g.drawString(this.font, name, left + 12, top + 10, GOLD, true);
        int level = pet instanceof ProdigyPet pp ? pp.getProgress().level() : 0;
        g.drawString(this.font, "Nv. " + level, left + 12, top + 24, 0xFFEDEDED, false);
        // vida
        float hp = Math.max(0.0F, pet.getHealth()), max = Math.max(1.0F, pet.getMaxHealth());
        int bx = left + 12, by = top + 38, bw = W - 24;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 9, 0xFF000000);
        g.fill(bx, by, bx + bw, by + 8, 0xFF3A2020);
        int col = hp / max < PetOrders.WEAK_FRACTION ? 0xFFE0453A : 0xFF58C84A;
        g.fill(bx, by, bx + (int) (bw * Math.min(1.0F, hp / max)), by + 8, col);
        String hpText = (int) Math.ceil(hp) + " / " + (int) Math.ceil(max);
        g.drawString(this.font, hpText, bx + bw / 2 - this.font.width(hpText) / 2, by, 0xFFFFFFFF, true);
        // descripcion
        int dy = top + 122;
        g.fill(left + 12, dy, left + W - 12, top + H - 12, 0xFF101216);
        if (fallen) {
            g.drawString(this.font, "Fuera de combate", left + 20, dy + 6, 0xFFE0453A, true);
            List<FormattedCharSequence> lines = this.font.split(Component.literal("Esta dormido y no puede recibir ordenes. Reanimalo con la ensalada de domesticacion (clic derecho) y se recuperara con 20 de vida."), W - 48);
            for (int i = 0; i < lines.size(); i++) g.drawString(this.font, lines.get(i), left + 20, dy + 20 + i * 10, 0xFFCFCFCF, false);
        } else {
            g.drawString(this.font, PetOrders.NAMES[cur], left + 20, dy + 6, GOLD, true);
            List<FormattedCharSequence> lines = this.font.split(Component.literal(PetOrders.HELP[cur]), W - 48);
            for (int i = 0; i < lines.size(); i++) g.drawString(this.font, lines.get(i), left + 20, dy + 20 + i * 10, 0xFFCFCFCF, false);
        }
        super.render(g, mx, my, pt);
    }
}
