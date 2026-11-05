package com.prodigytown.creatures.client;

import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.net.ModNetwork;
import com.prodigytown.creatures.progress.ProdigyProgress;
import com.prodigytown.creatures.progress.Talent;
import com.prodigytown.creatures.progress.Talents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla del arbol de habilidades (4 pestañas). Dibujada por codigo, sin texturas propias:
 * los iconos son items de Minecraft. Solo cliente; toda accion se valida en el servidor.
 */
public final class TalentScreen extends Screen {
    private static final int W = 384, H = 240;
    private static final int[] BRANCH_COLOR = {0xFFD9534F, 0xFF4AA3DF, 0xFF5CB85C};
    private static final String[] BRANCH_NAME = {"Combate", "Defensa", "Soporte"};
    private static final String[] BRANCH_SUB = {"Ofensiva y control", "Resistencia y proteccion", "Exploracion y utilidad"};
    private static final String[] VARIANT_NAME = {"Cibernetico", "Dorado", "Magma", "Glacial", "Natural"};
    private static final String[] TABS = {"Habilidades", "Resumen", "Equipamiento", "Variantes"};
    private static final int GOLD = 0xFFFFD35A, TEXT = 0xFFEDEDED, DIM = 0xFF9AA0A6;

    private final StegosaurusEntity stego;
    private int left, top, tab = 0, selected = 0;
    private Button unlock, reset;
    private final int[] nodeX = new int[Talents.ALL.length], nodeY = new int[Talents.ALL.length];

    public TalentScreen(StegosaurusEntity stego) {
        super(Component.literal("Arbol de habilidades"));
        this.stego = stego;
    }

    @Override protected void init() {
        left = (this.width - W) / 2;
        top = (this.height - H) / 2;
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            this.addRenderableWidget(Button.builder(Component.literal(TABS[i]), b -> tab = t)
                .bounds(left + 100 + i * 70, top + 4, 68, 16).build());
        }
        unlock = this.addRenderableWidget(Button.builder(Component.literal("Desbloquear"), b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(stego.getId(), 0, selected)))
            .bounds(left + 280, top + H - 44, 98, 18).build());
        reset = this.addRenderableWidget(Button.builder(Component.literal("Restablecer"), b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(stego.getId(), 1, 0)))
            .bounds(left + 6, top + H - 24, 88, 18).build());
        layoutNodes();
    }

    private void layoutNodes() {
        int[] count = new int[3];
        for (int i = 0; i < Talents.ALL.length; i++) {
            Talent t = Talents.ALL[i];
            int k = count[t.branch]++;
            int cx = left + 104 + t.branch * 58 + 29;
            nodeX[i] = cx + (k % 2 == 0 ? -14 : 14) - 11;
            nodeY[i] = top + 70 + (k / 2) * 36;
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

    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        g.fill(left, top, left + W, top + H, 0xE0141C18);
        g.renderOutline(left, top, W, H, GOLD);
        renderInfoPanel(g);
        switch (tab) {
            case 0 -> renderTree(g, mx, my);
            case 1 -> renderSummary(g);
            case 2 -> renderText(g, "Equipamiento", List.of("Silla de montar y armaduras para el estegosaurio:", "aun no estan implementadas en esta version.", "El talento Guardia (D5) depende de la montura."));
            default -> renderVariants(g);
        }
        unlock.visible = tab == 0;
        unlock.active = tab == 0 && prog().whyNotUnlock(selected) == null;
        super.render(g, mx, my, pt);
        if (tab == 0) renderTooltip(g, mx, my);
    }

    private void renderInfoPanel(GuiGraphics g) {
        ProdigyProgress p = prog();
        int x = left + 6, y = top + 6;
        g.drawString(this.font, "Estegosaurio", x, y, GOLD, false);
        g.drawString(this.font, "Variante: " + VARIANT_NAME[Math.max(0, Math.min(4, stego.getVariant()))], x, y + 12, TEXT, false);
        g.drawString(this.font, "Nivel " + p.level(), x, y + 30, TEXT, false);
        int barW = 86, bx = x, by = y + 42;
        g.fill(bx, by, bx + barW, by + 8, 0xFF2A2A2A);
        int need = p.xpToNext();
        int fill = need <= 0 ? barW : (int) (barW * (p.xp() / (float) need));
        g.fill(bx, by, bx + fill, by + 8, 0xFF8A4DFF);
        g.drawString(this.font, need <= 0 ? "Nivel maximo" : "XP " + p.xp() + " / " + need, x, by + 11, DIM, false);
        int sy = by + 30;
        g.drawString(this.font, "Vida      " + (int) stego.getMaxHealth(), x, sy, TEXT, false);
        g.drawString(this.font, "Ataque    " + (int) stego.getAttributeValue(Attributes.ATTACK_DAMAGE), x, sy + 12, TEXT, false);
        g.drawString(this.font, "Armadura  " + (int) stego.getAttributeValue(Attributes.ARMOR), x, sy + 24, TEXT, false);
        g.drawString(this.font, String.format("Velocidad %.2f", stego.getAttributeValue(Attributes.MOVEMENT_SPEED)), x, sy + 36, TEXT, false);
        g.drawString(this.font, "Puntos de habilidad", x, sy + 56, GOLD, false);
        g.drawString(this.font, String.valueOf(p.pointsAvailable()), x, sy + 68, TEXT, false);
        g.drawString(this.font, "Talentos " + p.talentsUnlocked() + " / " + Talents.ALL.length, x, sy + 84, DIM, false);
    }

    private void renderTree(GuiGraphics g, int mx, int my) {
        ProdigyProgress p = prog();
        for (int b = 0; b < 3; b++) {
            int cx = left + 104 + b * 58 + 29;
            g.fill(cx - 28, top + 26, cx + 28, top + 52, 0x80000000);
            g.renderOutline(cx - 28, top + 26, 56, 26, BRANCH_COLOR[b]);
            g.drawCenteredString(this.font, BRANCH_NAME[b], cx, top + 30, BRANCH_COLOR[b]);
            g.drawCenteredString(this.font, shorten(BRANCH_SUB[b], 56), cx, top + 41, DIM);
        }
        // lineas de requisito
        for (int i = 0; i < Talents.ALL.length; i++) {
            for (String[] req : Talents.ALL[i].reqs) {
                int r = Talents.index(req[0]);
                if (r < 0) continue;
                int x1 = nodeX[r] + 11, y1 = nodeY[r] + 22, x2 = nodeX[i] + 11, y2 = nodeY[i];
                int col = p.rank(r) >= Integer.parseInt(req[1]) ? BRANCH_COLOR[Talents.ALL[i].branch] : 0xFF444444;
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
            int base = BRANCH_COLOR[t.branch];
            g.fill(x, y, x + 22, y + 22, owned ? 0xFF1F3A2A : can ? 0xFF22303A : 0xFF222222);
            g.renderOutline(x, y, 22, 22, maxed ? GOLD : owned || can ? base : 0xFF555555);
            if (i == selected) g.renderOutline(x - 2, y - 2, 26, 26, 0xFFFFFFFF);
            g.renderItem(new ItemStack(icon(t.id)), x + 3, y + 3);
            if (!owned && !can) g.fill(x + 1, y + 1, x + 21, y + 21, 0x99000000);
            g.drawCenteredString(this.font, p.rank(i) + "/" + t.maxRank, x + 11, y + 24, owned ? TEXT : DIM);
        }
        renderDetail(g);
    }

    private String shorten(String s, int px) {
        while (this.font.width(s) > px && s.length() > 3) s = s.substring(0, s.length() - 1);
        return s;
    }

    private void renderDetail(GuiGraphics g) {
        ProdigyProgress p = prog();
        Talent t = Talents.ALL[selected];
        int x = left + 280, y = top + 28, w = 98;
        g.fill(x - 4, y - 6, left + W - 2, top + H - 2, 0x60000000);
        g.drawString(this.font, this.font.split(Component.literal(t.name), w).get(0), x, y, BRANCH_COLOR[t.branch], false);
        int cur = p.rank(selected);
        g.drawString(this.font, "Rango " + cur + " / " + t.maxRank, x, y + 12, GOLD, false);
        int yy = y + 26;
        for (FormattedCharSequence line : this.font.split(Component.literal(t.effect), w)) {
            if (yy > top + H - 100) break;
            g.drawString(this.font, line, x, yy, TEXT, false);
            yy += 9;
        }
        String note = switch (t.id) {
            case "D5" -> "Aviso: aun sin efecto (no hay montura).";
            case "S6" -> "Aviso: aun sin efecto (falta transformacion).";
            case "S3" -> "Cruzar portales: pendiente.";
            default -> null;
        };
        if (note != null) {
            for (FormattedCharSequence line : this.font.split(Component.literal(note), w)) {
                g.drawString(this.font, line, x, yy, 0xFFE0B04C, false);
                yy += 9;
            }
        }
        yy = Math.max(yy + 4, top + H - 98);
        g.drawString(this.font, "Requisitos:", x, yy, GOLD, false); yy += 11;
        int next = Math.min(cur, t.maxRank - 1);
        yy = req(g, x, yy, p.level() >= t.minLevel[next], "Nivel " + t.minLevel[next]);
        for (String[] r : t.reqs) {
            int need = Integer.parseInt(r[1]);
            int ti = Talents.index(r[0]);
            yy = req(g, x, yy, p.rank(r[0]) >= need, shorten(Talents.ALL[ti].name, 70) + " " + need);
        }
        yy = req(g, x, yy, cur >= t.maxRank || p.pointsAvailable() >= t.cost[next], "Coste: " + t.cost[next] + " pts");
        for (String ex : t.exclusive) {
            yy = req(g, x, yy, p.rank(ex) == 0, "Excluye " + shorten(Talents.ALL[Talents.index(ex)].name, 60));
        }
    }

    private int req(GuiGraphics g, int x, int y, boolean ok, String text) {
        g.drawString(this.font, (ok ? "✔ " : "✖ ") + text, x, y, ok ? 0xFF7CE08A : 0xFFE07C7C, false);
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

    private void renderVariants(GuiGraphics g) {
        renderText(g, "Variantes", List.of(
            "Variante actual: " + VARIANT_NAME[Math.max(0, Math.min(4, stego.getVariant()))],
            "",
            "Pasivas por variante y habilidad definitiva:",
            "aun no estan implementadas en esta version.",
            "Hoy la variante Magma es inmune al fuego."));
    }

    private void renderText(GuiGraphics g, String title, List<String> lines) {
        int x = left + 104, y = top + 30;
        g.drawString(this.font, title, x, y, GOLD, false);
        y += 16;
        for (String l : lines) {
            g.drawString(this.font, l, x, y, TEXT, false);
            y += 11;
        }
    }

    private void renderTooltip(GuiGraphics g, int mx, int my) {
        for (int i = 0; i < Talents.ALL.length; i++) {
            if (mx >= nodeX[i] && mx < nodeX[i] + 22 && my >= nodeY[i] && my < nodeY[i] + 22) {
                g.renderTooltip(this.font, Component.literal(Talents.ALL[i].name), mx, my);
                return;
            }
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (tab == 0 && button == 0) {
            for (int i = 0; i < Talents.ALL.length; i++) {
                if (mx >= nodeX[i] && mx < nodeX[i] + 22 && my >= nodeY[i] && my < nodeY[i] + 22) {
                    selected = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
