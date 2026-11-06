package com.prodigytown.creatures.client;

import com.prodigytown.chibi.ChibiStego;
import com.prodigytown.creatures.PetOrders;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.net.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/** Pantalla de ordenes (adulto y chibi), con una imagen por orden: seguir, quieto, deambular, patrullar, sentarse, dormir. Solo cliente; el servidor valida. */
public final class OrdersScreen extends Screen {
    private static final int W = 400, H = 232;
    private static final int GOLD = 0xFFFFD35A, EDGE = 0xFFB8862A, DIM = 0xFF9AA0A6;
    /** Imagen de cada orden segun el dino: a<variante adulta>_<orden> (0 cibernetico, 1 dorado, 2 magma, 3 esmeralda, 4 natural) o c<raza chibi>_<orden> (0 natural, 1 cibernetico, 2 magma, 3 dorado, 4 glacial). */
    private static ResourceLocation icon(TamableAnimal pet, int order) {
        String key;
        if (pet instanceof StegosaurusEntity s) key = "a" + Math.max(0, Math.min(4, s.getVariant()));
        else if (pet instanceof ChibiStego c) key = "c" + (c.isGlacial() ? 4 : Math.max(0, Math.min(3, c.variant())));
        else key = "a4";
        return new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/orders/" + key + "_" + order + ".png");
    }
    private final TamableAnimal pet;
    private final StegosaurusEntity adult;
    private int left, top;
    private final OrderTile[] tiles = new OrderTile[PetOrders.COUNT];

    public OrdersScreen(TamableAnimal pet) {
        super(Component.literal("Ordenes"));
        this.pet = pet;
        this.adult = pet instanceof StegosaurusEntity s ? s : null;
    }

    private PetOrders.Holder holder() { return (PetOrders.Holder) pet; }

    /** Casilla con la imagen de la orden (el marco viene del fondo). */
    private static final class OrderTile extends Button {
        final int order; boolean selected; final ResourceLocation icon;
        OrderTile(int x, int y, int order, ResourceLocation icon, OnPress press) {
            super(x, y, 72, 64, Component.literal(PetOrders.NAMES[order]), press, DEFAULT_NARRATION);
            this.order = order; this.icon = icon;
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int x = getX(), y = getY();
            boolean hot = isHoveredOrFocused() && active;
            if (hot) g.fill(x + 2, y + 2, x + width - 2, y + 48, 0x22FFFFFF);
            if (selected) {
                g.fillGradient(x + 2, y + 2, x + width - 2, y + height - 2, 0x30FFD35A, 0x10FFD35A);
                g.renderOutline(x, y, width, height, GOLD);
                g.renderOutline(x + 1, y + 1, width - 2, height - 2, 0x99FFD35A);
            } else if (hot) g.renderOutline(x, y, width, height, 0xFFE8C46A);
            g.setColor(1.0F, 1.0F, 1.0F, active ? 1.0F : 0.4F);
            g.blit(icon, x + 3, y + 3, 66, 44, 0.0F, 0.0F, 96, 64, 96, 64);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            g.drawCenteredString(font, getMessage(), x + width / 2, y + 52, selected ? GOLD : active ? 0xFFFFFFFF : 0xFF7A7F85);
        }
    }

    @Override protected void init() {
        left = (this.width - W) / 2;
        top = (this.height - H) / 2;
        for (int i = 0; i < PetOrders.COUNT; i++) {
            final int order = i;
            tiles[i] = this.addRenderableWidget(new OrderTile(left + 166 + (i % 3) * 78, top + 34 + (i / 3) * 70, i, icon(pet, i), b ->
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.SetOrder(pet.getId(), order))));
        }
        if (adult != null) {
            this.addRenderableWidget(Button.builder(Component.literal("Habilidades"), b -> Minecraft.getInstance().setScreen(new TalentScreen(adult)))
                .bounds(left + W - 100, top + 8, 90, 16).build());
        } else if (pet instanceof ChibiStego) {
            this.addRenderableWidget(Button.builder(Component.literal("Evolucionar a adulto"), b ->
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.SetOrder(pet.getId(), 9))).bounds(left + 12, top + H - 28, 140, 16).build());
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    /** Color del adorno segun la raza (0 cibernetico, 1 dorado, 2 magma, 3 esmeralda, 4 natural). */
    private int bgIndex() {
        if (pet instanceof StegosaurusEntity s) return Math.max(0, Math.min(4, s.getVariant()));
        if (pet instanceof ChibiStego c) return c.isGlacial() ? 3 : new int[]{4, 0, 2, 1}[Math.max(0, Math.min(3, c.variant()))];
        return 4;
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.blit(new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/orders_bg_" + bgIndex() + ".png"), left, top, W, H, 0.0F, 0.0F, 800, 464, 800, 464);
        boolean fallen = holder().isFallen();
        int cur = holder().getOrder();
        for (OrderTile t : tiles) { t.selected = !fallen && t.order == cur; t.active = !fallen; }
        String name = pet.hasCustomName() ? pet.getCustomName().getString() : (adult != null ? "Estegosaurio" : "Chibi estegosaurio");
        g.drawCenteredString(this.font, name, left + 82, top + 14, GOLD);
        g.drawCenteredString(this.font, "Ordenes", left + 212, top + 13, GOLD);
        // retrato
        int px0 = left + 15, py0 = top + 37, pw = 136, ph = 98;
        g.enableScissor(px0, py0, px0 + pw, py0 + ph);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, px0 + pw / 2, py0 + ph - 8, adult != null ? 9 : 38,
            (float) (px0 + pw / 2 - mx), (float) (py0 + 30 - my), pet);
        g.disableScissor();
        int level = adult != null ? adult.getProgress().level() : pet instanceof ChibiStego c ? c.syncedLevel() : 0;
        g.drawCenteredString(this.font, "Nv. " + level, left + 83, top + 146, 0xFFEDEDED);
        float hp = Math.max(0.0F, pet.getHealth()), max = Math.max(1.0F, pet.getMaxHealth());
        int bx = left + 28, by = top + 165, bw = 124;
        g.fill(bx, by, bx + bw, by + 12, 0xFF3A2020);
        g.fill(bx, by, bx + (int) (bw * Math.min(1.0F, hp / max)), by + 12, hp / max < PetOrders.WEAK_FRACTION ? 0xFFE0453A : 0xFF58C84A);
        g.fill(bx, by, bx + (int) (bw * Math.min(1.0F, hp / max)), by + 4, 0x30FFFFFF);
        String hpText = (int) Math.ceil(hp) + " / " + (int) Math.ceil(max);
        g.drawCenteredString(this.font, hpText, bx + bw / 2, by + 2, 0xFFFFFFFF);
        double atk = pet.getAttributeValue(Attributes.ATTACK_DAMAGE), arm = pet.getAttributeValue(Attributes.ARMOR);
        g.drawString(this.font, "Ataque " + (int) atk, left + 27, top + 188, 0xFFD8D8D8, false);
        g.drawString(this.font, "Armadura " + (int) arm, left + 100, top + 188, 0xFFD8D8D8, false);
        // descripcion
        int dx = left + 166, dy = top + 176, dw = W - 166 - 12;
        if (fallen) {
            g.drawString(this.font, "Fuera de combate", dx + 20, dy + 6, 0xFFE0453A, true);
            List<FormattedCharSequence> lines = this.font.split(Component.literal("Dormido: no recibe ordenes. Reanimalo con la ensalada de domesticacion (clic derecho): se recupera con 20 de vida."), dw - 24);
            for (int i = 0; i < Math.min(3, lines.size()); i++) g.drawString(this.font, lines.get(i), dx + 8, dy + 18 + i * 9, 0xFFCFCFCF, false);
        } else {
            g.drawString(this.font, PetOrders.NAMES[cur], dx + 20, dy + 6, GOLD, true);
            List<FormattedCharSequence> lines = this.font.split(Component.literal(PetOrders.HELP[cur]), dw - 24);
            for (int i = 0; i < Math.min(3, lines.size()); i++) g.drawString(this.font, lines.get(i), dx + 8, dy + 18 + i * 9, 0xFFCFCFCF, false);
        }
        super.render(g, mx, my, pt);
    }
}
