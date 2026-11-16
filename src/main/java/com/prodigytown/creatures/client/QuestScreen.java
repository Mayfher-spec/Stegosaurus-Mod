package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.net.ModNetwork;
import com.prodigytown.creatures.quests.CronistaEntity;
import com.prodigytown.creatures.quests.QuestData;
import com.prodigytown.creatures.quests.QuestPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Interfaz del Cronista (recreada del poster "Prodigy Quests"): dialogo principal, lista de misiones por pestanas, detalle y progreso.
 * Todo se dibuja en coordenadas virtuales de 420x277 (ajustadas a la ventana) sobre las texturas de textures/gui/quests/.
 */
public final class QuestScreen extends Screen {
    private static final int W = 420, H = 277;
    private static final String TEX = "textures/gui/quests/";
    private static final ResourceLocation SCENE = tex("scene.png"), PLATE = tex("plate.png"), PARCH = tex("parch.png"),
        BTN_G = tex("btn_green.png"), BTN_N = tex("btn_grey.png"), ROW = tex("row.png"), ROW_S = tex("row_sel.png");
    private static final int INK = 0xFF2B1B0C, INK2 = 0xFF5B4630, GOLD = 0xFFFFD35A, WHITE = 0xFFF6EEDC, DIM = 0xFF8A7A62, GREEN = 0xFF2F8A24, OKTXT = 0xFF1F6A18;
    private static final String[] TAB_NAMES = {"Fundación", "Combate", "Dinosaurios", "Jefes"};

    private enum Mode { MAIN, LIST, DETAIL, PROGRESS, REWARDS }

    private record Hit(int x, int y, int w, int h, Runnable run) {}
    private record Tip(int x, int y, int w, int h, ItemStack stack) {}

    private final int npcId;
    private final List<Hit> hits = new ArrayList<>();
    private final List<Tip> tips = new ArrayList<>();
    private LivingEntity npc;
    private Mode mode = Mode.MAIN, backMode = Mode.MAIN;
    private int listTab, progTab, scroll;
    private String selId = "";
    private float ox, oy, fit = 1.0F, vmx, vmy;

    public QuestScreen(int npcId) {
        super(Component.literal("El Cronista Mutante"));
        this.npcId = npcId;
    }

    private static ResourceLocation tex(String name) { return new ResourceLocation(ProdigyCreatures.MODID, TEX + name); }

    private static int tabOf(int state) { return state == QuestData.CLAIMED ? 2 : state >= QuestData.ACTIVE ? 1 : 0; }

    public void refresh() { clampScroll(); }

    // ------------------------------------------------------------------ datos

    private List<QuestPackets.QuestView> all() { return QuestClient.views; }

    private QuestPackets.QuestView find(String id) {
        for (QuestPackets.QuestView v : all()) if (v.id().equals(id)) return v;
        return null;
    }

    private int count(int state) { int n = 0; for (QuestPackets.QuestView v : all()) if (v.state() == state) n++; return n; }

    private List<QuestPackets.QuestView> listItems() {
        List<QuestPackets.QuestView> out = new ArrayList<>();
        for (QuestPackets.QuestView v : all()) if (v.tab() == listTab && v.state() <= QuestData.AVAILABLE) out.add(v);
        out.sort((a, b) -> Integer.compare(b.state(), a.state()));   // disponibles antes que bloqueadas (el orden interno ya viene del servidor)
        return out;
    }

    private List<QuestPackets.QuestView> progressItems() {
        List<QuestPackets.QuestView> out = new ArrayList<>();
        for (QuestPackets.QuestView v : all()) {
            if (mode == Mode.REWARDS) { if (v.state() == QuestData.READY) out.add(v); }
            else if (progTab == 0 ? (v.state() == QuestData.ACTIVE || v.state() == QuestData.READY) : v.state() == QuestData.CLAIMED) out.add(v);
        }
        if (mode != Mode.REWARDS && progTab == 0) out.sort((a, b) -> Integer.compare(b.state(), a.state()));   // las listas para reclamar primero
        return out;
    }

    private int pageSize() { return mode == Mode.LIST ? 5 : 3; }

    private int totalItems() { return mode == Mode.LIST ? listItems().size() : progressItems().size(); }

    private void clampScroll() { scroll = Math.max(0, Math.min(scroll, Math.max(0, totalItems() - pageSize()))); }

    private void go(Mode m) { backMode = mode == Mode.DETAIL ? backMode : mode; mode = m; scroll = 0; }

    private void act(int action, String id) { ModNetwork.CHANNEL.sendToServer(new QuestPackets.QuestAction(action, id)); }

    private void click() { Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)); }

    // ------------------------------------------------------------------ entrada

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mode == Mode.LIST || mode == Mode.PROGRESS || mode == Mode.REWARDS) { scroll -= (int) Math.signum(delta); clampScroll(); return true; }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        double vx = (mx - ox) / fit, vy = (my - oy) / fit;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (vx >= h.x && vx < h.x + h.w && vy >= h.y && vy < h.y + h.h) { click(); h.run.run(); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    // ------------------------------------------------------------------ dibujo: utilidades

    /** 9-slice: capX/capY en pixeles de textura (la textura es el doble de las unidades virtuales). */
    private static void nine(GuiGraphics g, ResourceLocation t, int tw, int th, int capX, int capY, int x, int y, int w, int h) {
        int cx = capX / 2, cy = capY / 2;
        int mw = w - 2 * cx, mh = h - 2 * cy, tmw = tw - 2 * capX, tmh = th - 2 * capY;
        g.blit(t, x, y, cx, cy, 0, 0, capX, capY, tw, th);
        g.blit(t, x + w - cx, y, cx, cy, tw - capX, 0, capX, capY, tw, th);
        g.blit(t, x, y + h - cy, cx, cy, 0, th - capY, capX, capY, tw, th);
        g.blit(t, x + w - cx, y + h - cy, cx, cy, tw - capX, th - capY, capX, capY, tw, th);
        g.blit(t, x + cx, y, mw, cy, capX, 0, tmw, capY, tw, th);
        g.blit(t, x + cx, y + h - cy, mw, cy, capX, th - capY, tmw, capY, tw, th);
        g.blit(t, x, y + cy, cx, mh, 0, capY, capX, tmh, tw, th);
        g.blit(t, x + w - cx, y + cy, cx, mh, tw - capX, capY, capX, tmh, tw, th);
        g.blit(t, x + cx, y + cy, mw, mh, capX, capY, tmw, tmh, tw, th);
    }

    private void plate(GuiGraphics g, String title, ItemStack icon) {
        int x = 128, y = 17, w = 276, h = 31, capL = 40, capR = 36, tw = 369, th = 63;
        g.blit(PLATE, x, y, capL / 2, h, 0, 0, capL, th, tw, th);
        g.blit(PLATE, x + capL / 2, y, w - capL / 2 - capR / 2, h, capL, 0, tw - capL - capR, th, tw, th);
        g.blit(PLATE, x + w - capR / 2, y, capR / 2, h, tw - capR, 0, capR, th, tw, th);
        if (icon != null) item(g, icon, x + 26, y + 7, 1.0F);
        g.drawCenteredString(this.font, Component.literal(title).withStyle(ChatFormatting.BOLD), x + w / 2 + (icon != null ? 8 : 0), y + 11, WHITE);
    }

    private void item(GuiGraphics g, ItemStack s, int x, int y, float k) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(k, k, 1.0F);
        g.renderItem(s, 0, 0);
        g.pose().popPose();
    }

    private boolean hover(int x, int y, int w, int h) { return vmx >= x && vmx < x + w && vmy >= y && vmy < y + h; }

    /** Boton con el estilo del poster. Devuelve nada; registra el area de clic. */
    private void button(GuiGraphics g, String label, int x, int y, int w, int h, boolean green, boolean enabled, ItemStack icon, Runnable run) {
        boolean hv = enabled && hover(x, y, w, h);
        g.setColor(1.0F, 1.0F, 1.0F, enabled ? 1.0F : 0.55F);
        nine(g, green ? BTN_G : BTN_N, green ? 249 : 243, 49, 22, 14, x, y, w, h);
        if (hv) { g.fill(x + 2, y + 2, x + w - 2, y + h - 2, green ? 0x28FFFFFF : 0x20FFFFFF); g.renderOutline(x, y, w, h, green ? 0xFFB7FF8A : 0xFFE8C46A); }
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int tx = x + w / 2, textW = this.font.width(label);
        if (icon != null) { item(g, icon, x + 8, y + (h - 12) / 2, 0.75F); tx += 7; }
        g.drawString(this.font, Component.literal(label).withStyle(ChatFormatting.BOLD), tx - textW / 2, y + (h - 8) / 2 + 1, enabled ? WHITE : 0xFF9A9A92, true);
        if (enabled && run != null) hits.add(new Hit(x, y, w, h, run));
    }

    private void tab(GuiGraphics g, String label, ItemStack icon, int x, int y, int w, boolean selected, Runnable run) {
        boolean hv = hover(x, y, w, 22);
        nine(g, selected ? BTN_G : BTN_N, selected ? 249 : 243, 49, 22, 14, x, y, w, 22);
        if (hv && !selected) g.renderOutline(x, y, w, 22, 0xFFE8C46A);
        if (icon != null) item(g, icon, x + 5, y + 5, 0.75F);
        g.drawString(this.font, label, x + (icon != null ? 19 : (w - this.font.width(label)) / 2), y + 7, selected ? WHITE : 0xFFE6DFCC, true);
        hits.add(new Hit(x, y, w, 22, run));
    }

    /** Recorta con puntos suspensivos para que el texto nunca se corte a media palabra sin avisar. */
    private String ell(String s, int w) {
        if (this.font.width(s) <= w) return s;
        return this.font.plainSubstrByWidth(s, w - this.font.width("…")) + "…";
    }

    private static ItemStack stack(net.minecraft.world.level.ItemLike i) { return new ItemStack(i); }

    private void parchment(GuiGraphics g, int x, int y, int w, int h) { nine(g, PARCH, 115, 115, 24, 24, x, y, w, h); }

    private void checkbox(GuiGraphics g, int x, int y, boolean done) {
        g.fill(x, y, x + 9, y + 9, 0xFF3A2A16);
        g.fill(x + 1, y + 1, x + 8, y + 8, done ? 0xFF6BD04A : 0xFFE9D9B3);
        if (done) {   // tilde
            g.fill(x + 2, y + 4, x + 4, y + 6, 0xFF14400E); g.fill(x + 3, y + 5, x + 5, y + 7, 0xFF14400E);
            g.fill(x + 4, y + 4, x + 6, y + 6, 0xFF14400E); g.fill(x + 5, y + 3, x + 7, y + 5, 0xFF14400E);
        }
    }

    private void bar(GuiGraphics g, int x, int y, int w, int h, int cur, int max, boolean done) {
        g.fill(x, y, x + w, y + h, 0xFF16110B);
        g.renderOutline(x, y, w, h, 0xFF050403);
        int fw = (w - 2) * Math.min(cur, max) / Math.max(1, max);
        if (fw > 0) {
            g.fillGradient(x + 1, y + 1, x + 1 + fw, y + h - 1, done ? 0xFF8CF05A : 0xFF63D03B, done ? 0xFF3C9E20 : 0xFF2F8A1C);
            g.fill(x + 1, y + 1, x + 1 + fw, y + 2, 0x66FFFFFF);
        }
    }

    private void scrollbar(GuiGraphics g, int x, int y, int h, int total, int page) {
        if (total <= page) return;
        g.fill(x, y, x + 5, y + h, 0xFF1A130C);
        int bh = Math.max(14, h * page / total), by = y + (h - bh) * scroll / Math.max(1, total - page);
        g.fillGradient(x + 1, by, x + 4, by + bh, 0xFF8CF05A, 0xFF3C9E20);
    }

    private void rewards(GuiGraphics g, QuestPackets.QuestView v, int x, int y, float k, boolean labels) {
        int cx = x;
        int step = labels ? 40 : 30;
        for (ItemStack s : v.items()) {
            item(g, s, cx, y, k);
            int sz = (int) (16 * k);
            tips.add(new Tip(cx, y, sz, sz, s));
            g.drawString(this.font, "×" + s.getCount(), cx + sz + 1, y + (sz - 7) / 2 + 1, INK, false);
            cx += step + (labels ? 0 : 0);
        }
        if (v.xp() > 0) {
            int sz = (int) (12 * k);
            g.fill(cx + 1, y + 2, cx + 1 + sz, y + 2 + sz, 0xFF2B4A10);
            g.fill(cx + 2, y + 3, cx + sz, y + 1 + sz, 0xFFA5E83A);
            g.fill(cx + 4, y + 5, cx + sz - 2, y + sz - 1, 0xFFD7FF6B);
            g.drawString(this.font, "×" + v.xp(), cx + sz + 3, y + (sz - 7) / 2 + 2, INK, false);
        }
    }

    private int rewardsWidth(QuestPackets.QuestView v, boolean labels) { return v.items().size() * (labels ? 40 : 30) + (v.xp() > 0 ? 40 : 0); }

    // ------------------------------------------------------------------ dibujo: pantalla

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        fit = Math.min(1.0F, Math.min((this.width - 6) / (float) W, (this.height - 6) / (float) H));
        ox = (this.width - W * fit) / 2.0F;
        oy = (this.height - H * fit) / 2.0F;
        vmx = (mx - ox) / fit;
        vmy = (my - oy) / fit;
        hits.clear();
        tips.clear();
        clampScroll();

        g.pose().pushPose();
        g.pose().translate(ox, oy, 0.0F);
        g.pose().scale(fit, fit, 1.0F);
        g.blit(SCENE, 0, 0, W, H, 0.0F, 0.0F, 840, 554, 840, 554);
        drawNpc(g);
        switch (mode) {
            case MAIN -> drawMain(g);
            case LIST -> drawList(g);
            case DETAIL -> drawDetail(g);
            default -> drawProgress(g);
        }
        if (mode != Mode.MAIN) button(g, "‹ Volver", 18, 246, 92, 18, false, true, null, this::back);
        g.pose().popPose();

        for (Tip t : tips) if (hover(t.x, t.y, t.w, t.h)) { g.renderTooltip(this.font, t.stack, mx, my); break; }
    }

    private void back() {
        if (mode == Mode.DETAIL) { mode = backMode == Mode.DETAIL ? Mode.LIST : backMode; scroll = 0; }
        else { mode = Mode.MAIN; scroll = 0; }
    }

    private void drawNpc(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (npc == null && mc.level != null) {
            Entity e = npcId >= 0 ? mc.level.getEntity(npcId) : null;
            npc = e instanceof LivingEntity le ? le : new CronistaEntity(ProdigyCreatures.CRONISTA.get(), mc.level);
        }
        if (npc != null) InventoryScreen.renderEntityInInventoryFollowsMouse(g, 88, 236, 84, 88.0F - vmx, 110.0F - vmy, npc);
    }

    // ---- 1. dialogo principal
    private void drawMain(GuiGraphics g) {
        plate(g, "El Cronista Mutante", stack(Items.WRITABLE_BOOK));
        parchment(g, 134, 56, 266, 118);
        int y = 66;
        String[] paras = {
            "Bienvenido, viajero.\nSoy el Cronista Mutante, guardián de los registros de este mundo.",
            "A través de misiones, documentaremos tu progreso, fortaleceremos nuestra colonia y desentrañaremos los misterios de los dinosaurios y sus mutaciones."};
        for (String p : paras) {
            for (FormattedCharSequence line : this.font.split(Component.literal(p), 246)) { g.drawString(this.font, line, 144, y, INK, false); y += 10; }
            y += 4;
        }
        int ready = count(QuestData.READY), active = count(QuestData.ACTIVE) + ready;
        g.drawString(this.font, active == 0 ? "No tienes misiones en curso." : "En curso: " + active + (ready > 0 ? "  ·  Por reclamar: " + ready : ""), 144, 160, ready > 0 ? OKTXT : INK2, false);

        button(g, "Misiones", 134, 184, 130, 24, true, true, stack(Items.PAPER), () -> { go(Mode.LIST); });
        button(g, "Progreso", 270, 184, 130, 24, false, true, stack(Items.EXPERIENCE_BOTTLE), () -> { go(Mode.PROGRESS); progTab = 0; });
        button(g, "Recompensas", 134, 214, 130, 24, false, true, stack(Items.CHEST), () -> go(Mode.REWARDS));
        button(g, "Salir", 270, 214, 130, 24, false, true, stack(Items.BARRIER), this::onClose);
        if (ready > 0) {   // globo con las recompensas por reclamar
            g.fill(250, 211, 264, 223, 0xFF3A0B0B); g.fill(251, 212, 263, 222, 0xFFE24B3B);
            g.drawCenteredString(this.font, String.valueOf(ready), 257, 214, 0xFFFFFFFF);
        }
    }

    // ---- 2. lista de misiones
    private void drawList(GuiGraphics g) {
        plate(g, "Misiones Disponibles", stack(Items.PAPER));
        ItemStack[] icons = {stack(Items.OAK_DOOR), stack(Items.IRON_SWORD), stack(Items.BONE), stack(Items.GOLDEN_HELMET)};
        int[] tw = new int[4];
        int total = 6;
        for (int i = 0; i < 4; i++) { tw[i] = 15 + this.font.width(TAB_NAMES[i]) + 7; total += tw[i]; }
        boolean withIcons = total <= 266;
        if (!withIcons) for (int i = 0; i < 4; i++) tw[i] = this.font.width(TAB_NAMES[i]) + 12;
        int tx = 134;
        for (int i = 0; i < 4; i++) { final int t = i; tab(g, TAB_NAMES[i], withIcons ? icons[i] : null, tx, 52, tw[i], listTab == i, () -> { listTab = t; scroll = 0; }); tx += tw[i] + 2; }
        parchment(g, 134, 76, 266, 186);
        List<QuestPackets.QuestView> list = listItems();
        if (list.isEmpty()) g.drawCenteredString(this.font, "No hay misiones disponibles en esta categoría.", 267, 160, INK2);
        for (int r = 0; r < 5; r++) {
            int idx = scroll + r;
            if (idx >= list.size()) break;
            QuestPackets.QuestView v = list.get(idx);
            int y = 84 + r * 34;
            boolean locked = v.state() == QuestData.LOCKED, sel = hover(140, y, 244, 30) && !locked;
            nine(g, sel ? ROW_S : ROW, 506, sel ? 61 : 62, 18, 16, 140, y, 244, 30);
            if (locked) g.fill(142, y + 2, 382, y + 28, 0x30401810);   // velo suave: se nota bloqueada pero el texto sigue legible
            item(g, v.icon(), 148, y + 3, 1.5F);
            g.drawString(this.font, Component.literal(ell(v.title(), 178)).withStyle(ChatFormatting.BOLD), 180, y + 6, locked ? 0xFF4A3A2A : 0xFF1A0F05, false);
            String sub = locked ? "Requiere: " + v.requiresTitle() : v.description();
            g.drawString(this.font, ell(sub, 178), 180, y + 17, locked ? 0xFF8A2A12 : INK2, false);
            // flecha
            int ax = 370, ay = y + 11;
            for (int i = 0; i < 4; i++) g.fill(ax + i, ay + i, ax + i + 1, ay + 8 - i, locked ? 0xFF8A7A62 : 0xFF3A3A32);
            final String id = v.id();
            hits.add(new Hit(140, y, 244, 30, () -> { selId = id; go(Mode.DETAIL); }));
        }
        scrollbar(g, 388, 84, 170, list.size(), 5);
    }

    // ---- 3. detalle de mision
    private void drawDetail(GuiGraphics g) {
        QuestPackets.QuestView v = find(selId);
        if (v == null) { mode = Mode.LIST; return; }
        plate(g, ell(v.title(), 200), v.icon());
        parchment(g, 134, 56, 266, 182);
        int x = 144;
        item(g, stack(Items.BOOK), x, 62, 0.75F);
        g.drawString(this.font, Component.literal("Descripción").withStyle(ChatFormatting.BOLD), x + 16, 65, INK, false);
        int y = 78;
        int lines = 0;
        for (FormattedCharSequence line : this.font.split(Component.literal(v.description()), 168)) { if (lines++ >= 5) break; g.drawString(this.font, line, x, y, INK, false); y += 10; }
        // marco con la ilustracion de la mision
        g.fill(330, 64, 394, 130, 0xFF2A1D0E);
        g.fillGradient(332, 66, 392, 128, 0xFF4F7F8C, 0xFF2C4A2A);
        g.fill(332, 112, 392, 128, 0xFF35502A);
        item(g, v.icon(), 342, 82, 2.5F);
        g.drawCenteredString(this.font, this.font.plainSubstrByWidth(v.category(), 58), 362, 118, WHITE);

        int oy2 = 120;
        item(g, stack(Items.TARGET), x, oy2 - 3, 0.75F);
        g.drawString(this.font, Component.literal("Objetivos").withStyle(ChatFormatting.BOLD), x + 16, oy2, INK, false);
        boolean done = v.state() >= QuestData.READY;
        checkbox(g, x + 2, oy2 + 14, done);
        String obj = (v.objective().isEmpty() ? "Cumple el objetivo" : v.objective()) + (v.state() == QuestData.LOCKED || v.state() == QuestData.AVAILABLE ? " (0/" + v.target() + ")" : " (" + v.progress() + "/" + v.target() + ")");
        g.drawString(this.font, ell(obj, 230), x + 16, oy2 + 15, INK, false);
        if (v.state() >= QuestData.ACTIVE) bar(g, x + 16, oy2 + 27, 200, 6, v.progress(), v.target(), done);

        int ry = 166;
        item(g, stack(Items.CHEST), x, ry - 3, 0.75F);
        g.drawString(this.font, Component.literal("Recompensas").withStyle(ChatFormatting.BOLD), x + 16, ry, INK, false);
        if (v.items().isEmpty() && v.xp() <= 0) g.drawString(this.font, "(sin recompensa)", x + 4, ry + 18, DIM, false);
        else rewards(g, v, x + 2, ry + 14, 1.0F, true);

        // accion
        final String id = v.id();
        switch (v.state()) {
            case QuestData.AVAILABLE -> button(g, "Aceptar misión", 187, 243, 160, 20, true, true, stack(Items.EMERALD), () -> act(0, id));
            case QuestData.ACTIVE -> {
                g.drawString(this.font, "En progreso", 144, 247, INK2, false);
                button(g, "Abandonar", 290, 243, 100, 20, false, true, null, () -> act(2, id));
            }
            case QuestData.READY -> button(g, "Reclamar recompensa", 187, 243, 160, 20, true, true, stack(Items.CHEST), () -> act(1, id));
            case QuestData.CLAIMED -> button(g, "Completada", 187, 243, 160, 20, false, false, null, null);
            default -> button(g, "Bloqueada", 187, 243, 160, 20, false, false, null, null);
        }
        if (v.state() == QuestData.LOCKED) {
            g.fill(142, 205, 392, 221, 0x40A04020);
            g.renderOutline(142, 205, 250, 16, 0x80803010);
            g.drawCenteredString(this.font, ell("Completa antes: " + v.requiresTitle(), 240), 267, 209, 0xFF6A1E0A);
        }
    }

    // ---- 4. progreso (y recompensas por reclamar)
    private void drawProgress(GuiGraphics g) {
        boolean rw = mode == Mode.REWARDS;
        plate(g, rw ? "Recompensas" : "Progreso de Misiones", rw ? stack(Items.CHEST) : stack(Items.EXPERIENCE_BOTTLE));
        if (!rw) {
            tab(g, "Misiones Activas", stack(Items.PAPER), 134, 52, 130, progTab == 0, () -> { progTab = 0; scroll = 0; });
            tab(g, "Completadas", stack(Items.LIME_DYE), 270, 52, 130, progTab == 1, () -> { progTab = 1; scroll = 0; });
        }
        int top = rw ? 56 : 76, bottom = 262;
        parchment(g, 134, top, 266, bottom - top);
        List<QuestPackets.QuestView> list = progressItems();
        if (list.isEmpty()) g.drawCenteredString(this.font, rw ? "Nada por reclamar por ahora." : progTab == 0 ? "No tienes misiones en curso." : "Aún no completas ninguna misión.", 267, top + 70, INK2);
        int cardH = 54, pitch = 58, y0 = top + 8;
        int page = rw ? 3 : 3;
        for (int r = 0; r < page; r++) {
            int idx = scroll + r;
            if (idx >= list.size()) break;
            QuestPackets.QuestView v = list.get(idx);
            int y = y0 + r * pitch, x = 140, w = 244;
            boolean ready = v.state() == QuestData.READY, claimed = v.state() == QuestData.CLAIMED;
            nine(g, ready ? ROW_S : ROW, 506, ready ? 61 : 62, 18, 16, x, y, w, cardH);
            item(g, v.icon(), x + 7, y + 5, 1.0F);
            g.drawString(this.font, Component.literal(ell(v.title(), 120)).withStyle(ChatFormatting.BOLD), x + 28, y + 6, 0xFF1A0F05, false);
            String st = claimed ? "Reclamada" : ready ? "Completada" : "En progreso";
            g.drawString(this.font, st, x + w - 8 - this.font.width(st), y + 6, ready || claimed ? OKTXT : INK2, false);
            boolean done = ready || claimed;
            checkbox(g, x + 29, y + 18, done);
            String obj = (v.objective().isEmpty() ? "Cumple el objetivo" : v.objective()) + " (" + v.progress() + "/" + v.target() + ")";
            g.drawString(this.font, ell(obj, 196), x + 43, y + 19, INK, false);
            bar(g, x + 29, y + 31, w - 78, 6, v.progress(), v.target(), done);
            g.drawString(this.font, (100 * Math.min(v.progress(), v.target()) / Math.max(1, v.target())) + "%", x + w - 44, y + 30, INK, false);
            // recompensas
            if (!claimed) {
                int rx = x + 8;
                g.drawString(this.font, "Premio:", rx, y + 42, INK2, false);
                rewards(g, v, rx + 38, y + 39, 0.75F, false);
                if (ready) { final String id = v.id(); button(g, "Reclamar", x + w - 64, y + 36, 58, 15, true, true, null, () -> act(1, id)); }
            }
            final String id2 = v.id();
            hits.add(new Hit(x, y, w - 70, 30, () -> { selId = id2; go(Mode.DETAIL); }));
        }
        scrollbar(g, 388, top + 8, bottom - top - 16, list.size(), page);
        if (rw && !list.isEmpty()) {
            button(g, "Reclamar todo", 187, 243, 160, 18, true, true, stack(Items.CHEST), () -> { for (QuestPackets.QuestView v : list) act(1, v.id()); });
        }
    }
}
