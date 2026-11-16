package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.net.ModNetwork;
import com.prodigytown.creatures.quests.QuestData;
import com.prodigytown.creatures.quests.QuestPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Pantalla del Cronista: lista de misiones a la izquierda (Disponibles / En curso / Completadas) y detalle con progreso y recompensas a la derecha. */
public final class QuestScreen extends Screen {
    private static final int W = 400, H = 252, ROW_H = 27, ROWS = 7;
    private static final int GOLD = 0xFFFFD35A, TEXT = 0xFFEDE6D0, DIM = 0xFF9AA39C, GREEN = 0xFF6FD05A, ACCENT = 0xFF6FA84A;
    private static final ResourceLocation BG = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/dilo/screen_bg.png");
    private static final String[] TABS = {"Disponibles", "En curso", "Completadas"};

    private int left, top, tab, scroll;
    private String selId = "";

    public QuestScreen() { super(Component.literal("El Cronista de Prodigy")); }

    /** Constructor de botones con el estilo del mod (como en la pantalla del dilofosaurio). */
    private static final class Themed extends Button {
        boolean selected;
        Themed(int x, int y, int w, int h, Component msg, OnPress press) { super(x, y, w, h, msg, press, DEFAULT_NARRATION); }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hover = this.isHoveredOrFocused() && this.active;
            int a = selected ? 0xFF3A2C0E : hover ? 0xFF26302C : 0xFF171D1B, b = selected ? 0xFF1E1606 : hover ? 0xFF161C1A : 0xFF0C100F;
            g.fillGradient(getX(), getY(), getX() + width, getY() + height, a, b);
            g.renderOutline(getX(), getY(), width, height, selected ? 0xFFFFD35A : !active ? 0xFF2C3330 : hover ? 0xFFE8C46A : 0xFF8A7A44);
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, !active ? 0xFF6A706C : selected ? 0xFFFFE9A8 : 0xFFEDE6D0);
        }
    }

    private Themed btn(String label, int x, int y, int w, int h, Button.OnPress p) {
        return this.addRenderableWidget(new Themed(x, y, w, h, Component.literal(label), p));
    }

    private static int tabOf(int state) { return state == QuestData.CLAIMED ? 2 : state >= QuestData.ACTIVE ? 1 : 0; }

    private List<QuestPackets.QuestView> filtered() {
        List<QuestPackets.QuestView> out = new ArrayList<>();
        for (QuestPackets.QuestView v : QuestClient.views) if (tabOf(v.state()) == tab) out.add(v);
        // en curso: las listas para reclamar primero
        if (tab == 1) out.sort((a, b) -> Integer.compare(b.state() == QuestData.READY ? 1 : 0, a.state() == QuestData.READY ? 1 : 0));
        return out;
    }

    private QuestPackets.QuestView selected() {
        for (QuestPackets.QuestView v : filtered()) if (v.id().equals(selId)) return v;
        return null;
    }

    /** Llamado cuando el servidor manda datos nuevos con la pantalla abierta. */
    public void refresh() {
        QuestPackets.QuestView moved = null;
        for (QuestPackets.QuestView v : QuestClient.views) if (v.id().equals(selId)) moved = v;
        if (moved != null && tabOf(moved.state()) != tab) tab = tabOf(moved.state());   // p. ej. al aceptar, salta a "En curso"
        ensureSelection();
        rebuild();
    }

    private void ensureSelection() {
        List<QuestPackets.QuestView> list = filtered();
        if (selected() == null) selId = list.isEmpty() ? "" : list.get(0).id();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, list.size() - ROWS)));
    }

    @Override protected void init() {
        this.left = (this.width - W) / 2;
        this.top = Math.max(12, (this.height - H) / 2);
        for (int i = 0; i < 3; i++) if (!QuestClient.views.isEmpty() && selected() == null) { /* se resuelve en ensureSelection */ }
        if (QuestClient.views.stream().noneMatch(v -> tabOf(v.state()) == tab)) {
            for (int t = 0; t < 3; t++) { final int tt = t; if (QuestClient.views.stream().anyMatch(v -> tabOf(v.state()) == tt)) { tab = t; break; } }
        }
        ensureSelection();
        rebuild();
    }

    private int count(int t) { int n = 0; for (QuestPackets.QuestView v : QuestClient.views) if (tabOf(v.state()) == t) n++; return n; }

    private void rebuild() {
        this.clearWidgets();
        for (int i = 0; i < 3; i++) {
            final int t = i;
            Themed b = btn(TABS[i] + " (" + count(i) + ")", left + 6 + i * 98, top + 4, 96, 16, x -> { tab = t; scroll = 0; ensureSelection(); rebuild(); });
            b.selected = tab == i;
        }
        btn("x", left + W - 20, top + 4, 14, 16, x -> this.onClose());
        QuestPackets.QuestView v = selected();
        if (v != null) {
            int bx = left + 206, by = top + H - 30;
            if (v.state() == QuestData.AVAILABLE) btn("Aceptar mision", bx, by, 120, 20, x -> act(0, v.id()));
            else if (v.state() == QuestData.READY) btn("Reclamar recompensa", bx, by, 140, 20, x -> act(1, v.id()));
            else if (v.state() == QuestData.ACTIVE) btn("Abandonar", bx, by, 80, 20, x -> act(2, v.id()));
        }
        btn("Rastreador: " + (QuestClient.trackerOn ? "si" : "no"), left + 10, top + H - 22, 92, 16, x -> { QuestClient.trackerOn = !QuestClient.trackerOn; rebuild(); });
    }

    private void act(int action, String id) {
        ModNetwork.CHANNEL.sendToServer(new QuestPackets.QuestAction(action, id));
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, filtered().size() - ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx >= left + 10 && mx < left + 196 && my >= top + 30 && my < top + 30 + ROWS * ROW_H) {
            int idx = scroll + (int) ((my - (top + 30)) / ROW_H);
            List<QuestPackets.QuestView> list = filtered();
            if (idx >= 0 && idx < list.size()) { selId = list.get(idx).id(); rebuild(); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean isPauseScreen() { return false; }

    private static int stateColor(int state) {
        return switch (state) {
            case QuestData.READY -> GOLD;
            case QuestData.ACTIVE -> 0xFF66C7E8;
            case QuestData.CLAIMED -> 0xFF6A706C;
            case QuestData.LOCKED -> 0xFF7A6A6A;
            default -> GREEN;
        };
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.pose().pushPose();
        g.pose().translate(left - 10, top - 10, 0.0F);
        g.pose().scale((W + 20) / 840.0F, (H + 20) / 544.0F, 1.0F);
        g.blit(BG, 0, 0, 840, 544, 0.0F, 0.0F, 840, 544, 840, 544);
        g.pose().popPose();
        g.fill(left + 4, top + 22, left + W - 4, top + 23, ACCENT);

        List<QuestPackets.QuestView> list = filtered();
        if (list.isEmpty()) {
            g.drawString(this.font, tab == 0 ? "No hay misiones disponibles ahora." : tab == 1 ? "No tienes misiones en curso." : "Aun no completas ninguna.", left + 14, top + 40, DIM, false);
        }
        // lista
        for (int r = 0; r < ROWS; r++) {
            int idx = scroll + r;
            if (idx >= list.size()) break;
            QuestPackets.QuestView v = list.get(idx);
            int y = top + 30 + r * ROW_H;
            boolean sel = v.id().equals(selId);
            g.fill(left + 10, y, left + 196, y + ROW_H - 2, sel ? 0xD0302308 : 0xB0080C0B);
            g.renderOutline(left + 10, y, 186, ROW_H - 2, sel ? GOLD : 0xFF3A4440);
            g.drawString(this.font, this.font.plainSubstrByWidth(v.title(), 176), left + 15, y + 4, v.state() == QuestData.LOCKED ? DIM : TEXT, false);
            String sub = v.state() == QuestData.LOCKED ? "Bloqueada" : v.state() == QuestData.CLAIMED ? "Completada" : v.state() == QuestData.READY ? "Lista: reclama tu premio"
                : v.state() == QuestData.ACTIVE ? v.progress() + " / " + v.target() : v.category();
            g.drawString(this.font, sub, left + 15, y + 15, stateColor(v.state()), false);
        }
        if (list.size() > ROWS) {   // barra de desplazamiento
            int th = ROWS * ROW_H, bh = Math.max(12, th * ROWS / list.size()), by = top + 30 + (th - bh) * scroll / Math.max(1, list.size() - ROWS);
            g.fill(left + 198, top + 30, left + 200, top + 30 + th, 0xFF1A201E);
            g.fill(left + 198, by, left + 200, by + bh, GOLD);
        }
        // detalle
        QuestPackets.QuestView v = selected();
        if (v != null) detail(g, v, mx, my);
        super.render(g, mx, my, pt);
        if (v != null) tooltips(g, v, mx, my);
    }

    private void detail(GuiGraphics g, QuestPackets.QuestView v, int mx, int my) {
        int x = left + 206, w = W - 216;
        g.drawString(this.font, this.font.plainSubstrByWidth(v.title(), w), x, top + 30, GOLD, false);
        g.drawString(this.font, v.category(), x, top + 41, DIM, false);
        int y = top + 56;
        for (FormattedCharSequence line : this.font.split(Component.literal(v.description()), w)) { g.drawString(this.font, line, x, y, TEXT, false); y += 10; if (y > top + 110) break; }
        y = Math.max(y + 4, top + 100);
        if (v.state() == QuestData.LOCKED) {
            g.drawString(this.font, "Completa antes: " + v.requiresTitle(), x, y, 0xFFE0A040, false);
        } else {
            g.drawString(this.font, "Objetivo: " + v.progress() + " / " + v.target(), x, y, TEXT, false);
            int bw = w - 4;
            g.fill(x, y + 11, x + bw, y + 17, 0xFF0B1210);
            g.renderOutline(x, y + 11, bw, 6, 0xFF3A4440);
            g.fill(x + 1, y + 12, x + 1 + (bw - 2) * v.progress() / Math.max(1, v.target()), y + 16, v.state() >= QuestData.READY ? GOLD : GREEN);
        }
        int ry = top + 140;
        g.drawString(this.font, "Recompensas", x, ry, GOLD, false);
        int ix = x;
        for (ItemStack s : v.items()) {
            g.fill(ix - 1, ry + 11, ix + 17, ry + 29, 0xB0080C0B);
            g.renderOutline(ix - 1, ry + 11, 18, 18, 0xFF5A4C26);
            g.renderItem(s, ix, ry + 12);
            g.renderItemDecorations(this.font, s, ix, ry + 12);
            ix += 21;
        }
        if (v.xp() > 0) g.drawString(this.font, "+" + v.xp() + " XP", ix + 4, ry + 16, 0xFF9CFF7A, false);
        if (v.items().isEmpty() && v.xp() <= 0) g.drawString(this.font, "(sin recompensa)", x, ry + 14, DIM, false);
    }

    private void tooltips(GuiGraphics g, QuestPackets.QuestView v, int mx, int my) {
        int ix = left + 206, ry = top + 140;
        for (ItemStack s : v.items()) {
            if (mx >= ix - 1 && mx < ix + 17 && my >= ry + 11 && my < ry + 29) { g.renderTooltip(this.font, s, mx, my); return; }
            ix += 21;
        }
    }
}
