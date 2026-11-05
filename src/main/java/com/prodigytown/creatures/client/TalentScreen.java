package com.prodigytown.creatures.client;

import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.net.ModNetwork;
import com.prodigytown.creatures.progress.ProdigyProgress;
import com.prodigytown.creatures.progress.Talent;
import com.prodigytown.creatures.progress.Talents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla del arbol de habilidades. Arte propio en textures/gui/talents.png (atlas 256x256, regiones:
 * nodos 30x30 en v=0: bloqueado u0, disponible u30/60/90, obtenido u120/150/180, maximo u210; anillo 34x34 en (0,30);
 * candado (40,30) check (56,30) cruz (68,30) estrella (80,30); placas de rama 72x28 en v=70; botones 48x20 en v=100;
 * barra XP marco (0,124) relleno (100,124); esquinas 16x16 en v=176; placa dorsal 24x30 en (64,176)).
 * Los iconos de talento son items de Minecraft. Solo cliente; toda accion se valida en el servidor.
 */
public final class TalentScreen extends Screen {
    private static final ResourceLocation TEX = TexButton.TEX;
    private static final int W = 448, H = 262;
    private static final int[] BRANCH_COLOR = {0xFFE0605C, 0xFF58B0EE, 0xFF6CCB6C};
    private static final String[] BRANCH_NAME = {"Combate", "Defensa", "Soporte"};
    private static final String[] BRANCH_SUB = {"Ofensiva", "Proteccion", "Utilidad"};
    private static final String[] VARIANT_NAME = {"Cibernetico", "Dorado", "Magma", "Glacial", "Natural"};
    private static final String[] TABS = {"Habilidades", "Resumen", "Equipamiento", "Variantes"};
    private static final int GOLD = 0xFFFFD35A, TEXT = 0xFFEDEDED, DIM = 0xFF9AA0A6;

    private final StegosaurusEntity stego;
    private int left, top, tab = 0, selected = 0;
    /** Escala para que el panel quepa siempre en pantalla; las coordenadas internas son "virtuales" (divididas por sc). */
    private float sc = 1.0F;
    private TexButton unlock, reset, sit;
    private final TexButton[] tabButtons = new TexButton[TABS.length];
    private final int[] nodeX = new int[Talents.ALL.length], nodeY = new int[Talents.ALL.length];

    public TalentScreen(StegosaurusEntity stego) {
        super(Component.literal("Arbol de habilidades"));
        this.stego = stego;
    }

    @Override protected void init() {
        sc = Math.max(0.4F, Math.min(1.0F, Math.min((this.width - 8.0F) / W, (this.height - 8.0F) / H)));
        left = (int) ((this.width / sc - W) / 2.0F);
        top = (int) ((this.height / sc - H) / 2.0F);
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            tabButtons[i] = this.addRenderableWidget(new TexButton(left + 118 + i * 76, top + 8, 74, 18, Component.literal(TABS[i]), b -> tab = t));
        }
        unlock = this.addRenderableWidget(new TexButton(left + 336, top + H - 46, 100, 20, Component.literal("Desbloquear"), b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(stego.getId(), 0, selected))));
        unlock.gold = true;
        reset = this.addRenderableWidget(new TexButton(left + 10, top + H - 46, 92, 18, Component.literal("Restablecer"), b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(stego.getId(), 1, 0))));
        sit = this.addRenderableWidget(new TexButton(left + 10, top + H - 26, 92, 18, Component.literal("Sentar / Levantar"), b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(stego.getId(), 2, 0))));
        layoutNodes();
    }

    /** Posicion de cada nodo: {columna -1/0/1, fila}. Pensado para que cada requisito quede siempre mas arriba. */
    private static int[] slot(String id) {
        return switch (id) {
            case "C1" -> new int[]{0, 0}; case "C2" -> new int[]{-1, 1}; case "C3" -> new int[]{1, 1};
            case "C4" -> new int[]{-1, 2}; case "C5" -> new int[]{1, 2}; case "C6" -> new int[]{0, 3}; case "C7" -> new int[]{0, 4};
            case "D1" -> new int[]{-1, 0}; case "D2" -> new int[]{1, 0}; case "D3" -> new int[]{-1, 1};
            case "D4" -> new int[]{1, 1}; case "D5" -> new int[]{0, 2};
            case "S1" -> new int[]{-1, 0}; case "S3" -> new int[]{1, 0}; case "S2" -> new int[]{-1, 1};
            case "S4" -> new int[]{1, 1}; case "S5" -> new int[]{-1, 2}; case "S6" -> new int[]{0, 3}; case "S7" -> new int[]{0, 4};
            default -> new int[]{0, 0};
        };
    }

    private void layoutNodes() {
        for (int i = 0; i < Talents.ALL.length; i++) {
            Talent t = Talents.ALL[i];
            int[] sl = slot(t.id);
            int cx = left + 148 + t.branch * 70;
            nodeX[i] = cx + sl[0] * 17 - 15;
            nodeY[i] = top + 68 + sl[1] * 33;
        }
    }

    private static Item icon(String id) {
        return switch (id) {
            case "D1" -> Items.IRON_CHESTPLATE; case "D2" -> Items.GOLDEN_APPLE; case "D3" -> Items.ANVIL;
            case "D4" -> Items.TOTEM_OF_UNDYING; case "D5" -> Items.SHIELD;
            case "C1" -> Items.BONE; case "C2" -> Items.DIAMOND_AXE; case "C3" -> Items.FIRE_CHARGE;
            case "C4" -> Items.CACTUS; case "C5" -> Items.TNT; case "C6" -> Items.BLAZE_POWDER; case "C7" -> Items.NETHER_STAR;
            case "S1" -> Items.SUGAR; case "S2" -> Items.GLISTERING_MELON_SLICE; case "S3" -> Items.LEAD;
            case "S4" -> Items.SLIME_BALL; case "S5" -> Items.RED_BED; case "S6" -> Items.ENDER_PEARL; case "S7" -> Items.BEACON;
            default -> Items.PAPER;
        };
    }

    private ProdigyProgress prog() { return stego.getProgress(); }
    private void sprite(GuiGraphics g, int x, int y, int u, int v, int w, int h) { g.blit(TEX, x, y, (float) u, (float) v, w, h, 256, 256); }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int rmx = mx, rmy = my;
        mx = (int) (mx / sc); my = (int) (my / sc);
        g.pose().pushPose();
        g.pose().scale(sc, sc, 1.0F);
        drawPanel(g);
        renderInfoPanel(g);
        switch (tab) {
            case 0 -> renderTree(g);
            case 1 -> renderSummary(g);
            case 2 -> renderEquipment(g);
            default -> renderVariants(g);
        }
        for (int i = 0; i < tabButtons.length; i++) tabButtons[i].gold = i == tab;
        unlock.visible = tab == 0;
        unlock.active = tab == 0 && prog().whyNotUnlock(selected) == null;
        super.render(g, mx, my, pt);
        g.pose().popPose();
        if (tab == 0) renderTooltip(g, mx, my, rmx, rmy);
    }

    private void drawPanel(GuiGraphics g) {
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF050807);
        g.fillGradient(left, top, left + W, top + H, 0xF2142019, 0xF20A1210);
        g.renderOutline(left, top, W, H, 0xFF8A6A24);
        g.renderOutline(left + 1, top + 1, W - 2, H - 2, 0xFF3A2E10);
        sprite(g, left, top, 0, 176, 16, 16);
        sprite(g, left + W - 16, top, 16, 176, 16, 16);
        sprite(g, left, top + H - 16, 32, 176, 16, 16);
        sprite(g, left + W - 16, top + H - 16, 48, 176, 16, 16);
        // centro del arbol
        g.fill(left + 112, top + 32, left + 326, top + H - 30, 0x55000000);
        g.renderOutline(left + 112, top + 32, 214, H - 62, 0xFF3A2E10);
        // placas dorsales de fondo (silueta del estegosaurio detras del arbol)
        g.setColor(1.0F, 1.0F, 1.0F, 0.30F);
        int[] px = {147, 165, 189, 219, 249, 273, 291};
        int[] ph = {26, 34, 42, 48, 42, 34, 26};
        for (int i = 0; i < px.length; i++) {
            int w = ph[i] * 24 / 30;
            g.blit(TEX, left + px[i] - w / 2, top + 54 - ph[i] / 2, w, ph[i], 64.0F, 176.0F, 24, 30, 256, 256);
        }
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderInfoPanel(GuiGraphics g) {
        ProdigyProgress p = prog();
        int x = left + 12, y = top + 8;
        g.drawString(this.font, "Estegosaurio", x, y, GOLD, true);
        g.drawString(this.font, "Arbol de Habilidades", x, y + 11, 0xFFC9B27A, false);
        // variante
        g.fill(x - 2, y + 26, x + 94, y + 48, 0x66000000);
        g.renderOutline(x - 2, y + 26, 96, 22, 0xFF5C4A1C);
        g.drawString(this.font, "Variante", x + 2, y + 29, DIM, false);
        g.drawString(this.font, VARIANT_NAME[Math.max(0, Math.min(4, stego.getVariant()))], x + 2, y + 39, TEXT, false);
        // nivel y XP
        g.drawString(this.font, "Nivel " + p.level(), x, y + 56, TEXT, true);
        sprite(g, x - 2, y + 68, 0, 124, 100, 10);
        int need = p.xpToNext();
        int fill = need <= 0 ? 96 : Math.min(96, (int) (96 * (p.xp() / (float) need)));
        if (fill > 0) g.blit(TEX, x, y + 68, fill, 10, 102.0F, 124.0F, fill, 10, 256, 256);
        g.drawString(this.font, need <= 0 ? "Nivel maximo" : "XP " + p.xp() + " / " + need, x, y + 81, DIM, false);
        // estadisticas
        int sy = y + 98;
        g.fill(x - 2, sy - 3, x + 94, sy + 50, 0x66000000);
        g.renderOutline(x - 2, sy - 3, 96, 53, 0xFF3A2E10);
        stat(g, x, sy, "Vida", String.valueOf((int) stego.getMaxHealth()), 0xFFFF6E6E);
        stat(g, x, sy + 12, "Ataque", String.valueOf((int) stego.getAttributeValue(Attributes.ATTACK_DAMAGE)), 0xFFFFD27A);
        stat(g, x, sy + 24, "Armadura", String.valueOf((int) stego.getAttributeValue(Attributes.ARMOR)), 0xFF7ABBFF);
        stat(g, x, sy + 36, "Velocidad", String.format("%.2f", stego.getAttributeValue(Attributes.MOVEMENT_SPEED)), 0xFF9BE08A);
        // puntos
        int py = sy + 56;
        g.fill(x - 2, py - 2, x + 94, py + 24, 0x66000000);
        g.renderOutline(x - 2, py - 2, 96, 26, 0xFF8A6A24);
        sprite(g, x + 2, py + 6, 80, 30, 10, 8);
        g.drawString(this.font, "Puntos libres", x + 16, py + 1, GOLD, false);
        g.drawString(this.font, String.valueOf(p.pointsAvailable()), x + 16, py + 12, TEXT, true);
    }

    private void stat(GuiGraphics g, int x, int y, String name, String value, int color) {
        g.drawString(this.font, name, x + 2, y, DIM, false);
        g.drawString(this.font, value, x + 92 - this.font.width(value), y, color, false);
    }

    private void renderTree(GuiGraphics g) {
        ProdigyProgress p = prog();
        for (int b = 0; b < 3; b++) {
            int cx = left + 148 + b * 70;
            g.blit(TEX, cx - 31, top + 38, 62, 24, (float) (72 * b), 70.0F, 72, 28, 256, 256);
            g.drawCenteredString(this.font, BRANCH_NAME[b], cx, top + 42, 0xFFFFFFFF);
            g.drawCenteredString(this.font, BRANCH_SUB[b], cx, top + 52, BRANCH_COLOR[b]);
        }
        // lineas de requisito (debajo de los nodos)
        for (int i = 0; i < Talents.ALL.length; i++) {
            for (String[] req : Talents.ALL[i].reqs) {
                int r = Talents.index(req[0]);
                if (r < 0) continue;
                int x1 = nodeX[r] + 15, y1 = nodeY[r] + 28, x2 = nodeX[i] + 15, y2 = nodeY[i] + 2;
                boolean met = p.rank(r) >= Integer.parseInt(req[1]);
                int col = met ? BRANCH_COLOR[Talents.ALL[i].branch] : 0xFF3C4044;
                int ym = (y1 + y2) / 2;
                g.fill(x1 - 1, y1, x1 + 1, ym + 1, col);
                g.fill(Math.min(x1, x2) - 1, ym, Math.max(x1, x2) + 1, ym + 2, col);
                g.fill(x2 - 1, ym, x2 + 1, y2, col);
            }
        }
        for (int i = 0; i < Talents.ALL.length; i++) {
            Talent t = Talents.ALL[i];
            int x = nodeX[i], y = nodeY[i];
            boolean owned = p.rank(i) > 0, maxed = p.rank(i) >= t.maxRank, can = p.whyNotUnlock(i) == null;
            int u = maxed ? 210 : owned ? 120 + 30 * t.branch : can ? 30 + 30 * t.branch : 0;
            sprite(g, x, y, u, 0, 30, 30);
            g.renderItem(new ItemStack(icon(t.id)), x + 7, y + 7);
            if (!owned && !can) {
                g.fill(x + 4, y + 4, x + 26, y + 26, 0x99000000);
                sprite(g, x + 11, y + 10, 40, 30, 8, 10);
            }
            if (i == selected) sprite(g, x - 2, y - 2, 0, 30, 34, 34);
            String rk = p.rank(i) + "/" + t.maxRank;
            g.drawString(this.font, rk, x + 27 - this.font.width(rk), y + 19, owned ? (maxed ? GOLD : TEXT) : DIM, true);
        }
        // progreso del arbol
        int total = Talents.ALL.length, got = p.talentsUnlocked();
        int by = top + H - 22;
        g.drawString(this.font, "Progreso del arbol", left + 118, by, TEXT, false);
        int bw = 100, bx = left + 224;
        sprite(g, bx, by - 1, 0, 124, 100, 10);
        int f = Math.min(96, 96 * got / total);
        if (f > 0) g.blit(TEX, bx + 2, by - 1, f, 10, 102.0F, 124.0F, f, 10, 256, 256);
        g.drawString(this.font, got + "/" + total, bx + bw + 6, by, DIM, false);
        renderDetail(g);
    }

    private String shorten(String s, int px) {
        while (this.font.width(s) > px && s.length() > 3) s = s.substring(0, s.length() - 1);
        return s;
    }

    private void renderDetail(GuiGraphics g) {
        ProdigyProgress p = prog();
        Talent t = Talents.ALL[selected];
        int x = left + 338, y = top + 34, w = 98;
        g.fill(left + 332, top + 30, left + W - 6, top + H - 52, 0x66000000);
        g.renderOutline(left + 332, top + 30, W - 338, H - 82, 0xFF8A6A24);
        // icono
        sprite(g, x - 2, y, p.rank(selected) > 0 ? 120 + 30 * t.branch : 30 + 30 * t.branch, 0, 30, 30);
        g.renderItem(new ItemStack(icon(t.id)), x + 5, y + 7);
        List<FormattedCharSequence> name = this.font.split(Component.literal(t.name), w - 34);
        for (int i = 0; i < Math.min(3, name.size()); i++) g.drawString(this.font, name.get(i), x + 34, y + 2 + i * 10, BRANCH_COLOR[t.branch], false);
        int cur = p.rank(selected);
        g.drawString(this.font, "Rango " + cur + " / " + t.maxRank, x, y + 36, GOLD, false);
        int yy = y + 48;
        for (FormattedCharSequence line : this.font.split(Component.literal(t.effect), w)) {
            if (yy > top + H - 118) break;
            g.drawString(this.font, line, x, yy, TEXT, false);
            yy += 9;
        }
        String note = switch (t.id) {
            case "S6" -> "Aviso: aun sin efecto (falta transformacion).";
            case "S3" -> "Cruzar portales: pendiente.";
            case "D5" -> "Efecto solo montado (clic con silla).";
            default -> null;
        };
        if (note != null) {
            for (FormattedCharSequence line : this.font.split(Component.literal(note), w)) {
                g.drawString(this.font, line, x, yy, 0xFFE0B04C, false);
                yy += 9;
            }
        }
        yy = Math.max(yy + 4, top + H - 112);
        g.drawString(this.font, "Requisitos", x, yy, GOLD, false); yy += 11;
        int next = Math.min(cur, t.maxRank - 1);
        yy = req(g, x, yy, p.level() >= t.minLevel[next], "Nivel " + t.minLevel[next]);
        for (String[] r : t.reqs) {
            int need = Integer.parseInt(r[1]);
            yy = req(g, x, yy, p.rank(r[0]) >= need, shorten(Talents.ALL[Talents.index(r[0])].name, 70) + " " + need);
        }
        yy = req(g, x, yy, cur >= t.maxRank || p.pointsAvailable() >= t.cost[next], "Coste: " + t.cost[next] + " pts");
        for (String ex : t.exclusive) {
            yy = req(g, x, yy, p.rank(ex) == 0, "Excluye " + shorten(Talents.ALL[Talents.index(ex)].name, 60));
        }
    }

    private int req(GuiGraphics g, int x, int y, boolean ok, String text) {
        if (ok) sprite(g, x, y + 1, 56, 30, 10, 7); else sprite(g, x + 1, y + 1, 68, 30, 8, 7);
        g.drawString(this.font, text, x + 13, y, ok ? 0xFF7CE08A : 0xFFE07C7C, false);
        return y + 10;
    }

    private void renderSummary(GuiGraphics g) {
        ProdigyProgress p = prog();
        List<String> lines = new ArrayList<>();
        lines.add("Nivel " + p.level() + " (max. 100)");
        lines.add("Puntos ganados " + p.pointsEarned() + ", gastados " + p.pointsSpent() + ", libres " + p.pointsAvailable());
        lines.add("");
        lines.add("Talentos desbloqueados:");
        boolean any = false;
        for (int i = 0; i < Talents.ALL.length; i++) if (p.rank(i) > 0) {
            lines.add(" - " + Talents.ALL[i].name + " " + p.rank(i) + "/" + Talents.ALL[i].maxRank);
            any = true;
        }
        if (!any) lines.add(" (ninguno)");
        renderText(g, "Resumen", lines);
    }

    private void renderEquipment(GuiGraphics g) {
        renderText(g, "Equipamiento", List.of(
            "Silla: " + (stego.isSaddled() ? "equipada (clic para montar)" : "sin silla (clic con una silla en la mano)"),
            "Montado: clic izq. = mordida, clic der. = coletazo,",
            "V = embestida (talento).",
            "Montado, el talento Guardia absorbe dano por ti.",
            "",
            "Armaduras para el estegosaurio: aun no estan",
            "implementadas en esta version."));
    }

    private void renderVariants(GuiGraphics g) {
        renderText(g, "Variantes", List.of(
            "Variante actual: " + VARIANT_NAME[Math.max(0, Math.min(4, stego.getVariant()))],
            "",
            "Pasivas por variante y habilidad definitiva:",
            "aun no estan implementadas en esta version.",
            "Hoy la variante Magma es inmune al fuego."));
    }

    private void renderText(GuiGraphics g, String title, List<String> lines) {
        int x = left + 120, y = top + 42;
        g.drawString(this.font, title, x, y, GOLD, true);
        y += 18;
        for (String l : lines) {
            g.drawString(this.font, l, x, y, TEXT, false);
            y += 11;
        }
    }

    private void renderTooltip(GuiGraphics g, int mx, int my, int rmx, int rmy) {
        for (int i = 0; i < Talents.ALL.length; i++) {
            if (mx >= nodeX[i] && mx < nodeX[i] + 30 && my >= nodeY[i] && my < nodeY[i] + 30) {
                g.renderTooltip(this.font, Component.literal(Talents.ALL[i].name + "  " + prog().rank(i) + "/" + Talents.ALL[i].maxRank), rmx, rmy);
                return;
            }
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        mx /= sc; my /= sc;
        if (tab == 0 && button == 0) {
            for (int i = 0; i < Talents.ALL.length; i++) {
                if (mx >= nodeX[i] && mx < nodeX[i] + 30 && my >= nodeY[i] && my < nodeY[i] + 30) {
                    selected = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
