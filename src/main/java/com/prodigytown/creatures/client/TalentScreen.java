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
    private static final int W = 448, H = 252;
    /** Fondo por variante (0 cibernetico, 1 dorado, 2 magma, 3 glacial, 4 natural). */
    private static final ResourceLocation[] BG = new ResourceLocation[5];
    static { for (int i = 0; i < 5; i++) BG[i] = new ResourceLocation(com.prodigytown.creatures.ProdigyCreatures.MODID, "textures/gui/talent_bg_" + i + ".png"); }
    private static final int[] ACCENT = {0xFF4FE6FF, 0xFFFFC83A, 0xFFFF7A3A, 0xFFB4ECFF, 0xFFFFD35A};
    private static final int[] EDGE = {0xFF3CBEDC, 0xFFC88C1E, 0xFFD24A28, 0xFF78AAC8, 0xFF8A6A24};
    private static final int[] XP_HI = {0xFF40D8FF, 0xFFFFC83A, 0xFFFF6A30, 0xFFA8E8FF, 0xFFB060FF};
    private static final int[] XP_LO = {0xFF1478A8, 0xFFB87812, 0xFF9A2010, 0xFF5890B8, 0xFF6A2CC0};
    private int GOLD = 0xFFFFD35A, VI = 4;
    private boolean ultSel = false;
    private HitButton ult;
    /** Lado de los nodos del arbol sobre el fondo (los sprites del atlas son de 30 px). */
    private static final int NS = 20;
    private static final int[] BRANCH_COLOR = {0xFFE0605C, 0xFF58B0EE, 0xFF6CCB6C};
    private static final String[] BRANCH_NAME = {"Combate", "Defensa", "Soporte"};
    private static final String[] BRANCH_SUB = {"Ofensiva", "Proteccion", "Utilidad"};
    /** Color del texto de la habilidad definitiva segun la raza (cian, dorado, magma, esmeralda, natural). */
    private static final int[] ULT_COLOR = {0xFF5CE8FF, 0xFFFFD24C, 0xFFFF6A3C, 0xFF5CFF9A, 0xFFA6E34C};
    private static final String[] VARIANT_NAME = {"Cibernetico", "Dorado", "Magma", "Esmeralda", "Natural"};
    private static final String[] TABS = {"Habilidades", "Resumen", "Equipamiento", "Variantes"};
    private static final int TEXT = 0xFFEDEDED, DIM = 0xFF9AA0A6;

    private final StegosaurusEntity stego;
    private int left, top, tab = 0, selected = 0;
    /** Escala para que el panel quepa siempre en pantalla; las coordenadas internas son "virtuales" (divididas por sc). */
    private float sc = 1.0F;
    private TexButton unlock, sit, rename, dropSaddle, dropArmor, toChibi;
    private long chibiConfirmUntil = 0L;
    private HitButton reset;
    private net.minecraft.client.gui.components.EditBox nameBox;
    private final TexButton[] tabButtons = new TexButton[TABS.length];
    private final int[] nodeX = new int[Talents.ALL.length], nodeY = new int[Talents.ALL.length];

    public TalentScreen(StegosaurusEntity stego) {
        super(Component.literal("Arbol de habilidades"));
        this.stego = stego;
    }

    /** Boton invisible sobre una zona del fondo: solo se ilumina al pasar el raton. */
    private static final class HitButton extends net.minecraft.client.gui.components.Button {
        HitButton(int x, int y, int w, int h, OnPress press) { super(x, y, w, h, Component.empty(), press, DEFAULT_NARRATION); }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            if (this.isHoveredOrFocused()) g.fill(getX(), getY(), getX() + width, getY() + height, 0x33FFFFFF);
        }
    }

    @Override protected void init() {
        sc = Math.max(0.4F, Math.min(0.78F, Math.min((this.width - 8.0F) / W, (this.height - 8.0F) / H)));
        VI = Math.max(0, Math.min(4, stego.getVariant()));
        GOLD = ACCENT[VI];
        left = (int) ((this.width / sc - W) / 2.0F);
        top = (int) ((this.height / sc - H) / 2.0F);
        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            tabButtons[i] = this.addRenderableWidget(new TexButton(left + 134 + i * 64, top + 6, 62, 15, Component.literal(TABS[i]), b -> tab = t));
        }
        this.addRenderableWidget(new HitButton(left + 426, top + 4, 18, 18, b -> this.onClose()));
        ult = this.addRenderableWidget(new HitButton(left + 196, top + 178, 43, 34, b -> ultSel = true));
        unlock = this.addRenderableWidget(new TexButton(left + 343, top + 191, 90, 15, Component.literal("Desbloquear"), b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(stego.getId(), 0, selected))));
        unlock.gold = true;
        reset = this.addRenderableWidget(new HitButton(left + 13, top + 214, 76, 13, b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(stego.getId(), 1, 0))));
        sit = this.addRenderableWidget(new TexButton(left + 124, top + 128, 110, 14, Component.literal("Sentar / Levantar"), b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(stego.getId(), 2, 0))));
        nameBox = this.addRenderableWidget(new net.minecraft.client.gui.components.EditBox(this.font, left + 124, top + 46, 200, 14, Component.literal("Nombre")));
        nameBox.setMaxLength(32);
        nameBox.setValue(stego.hasCustomName() ? stego.getCustomName().getString() : "");
        nameBox.setHint(Component.literal("Nombre del estegosaurio"));
        rename = this.addRenderableWidget(new TexButton(left + 328, top + 46, 84, 14, Component.literal("Poner nombre"), b -> sendName()));
        rename.gold = true;
        dropSaddle = this.addRenderableWidget(new TexButton(left + 124, top + 110, 110, 14, Component.literal("Quitar silla"), b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(stego.getId(), 3, 0))));
        dropArmor = this.addRenderableWidget(new TexButton(left + 240, top + 110, 110, 14, Component.literal("Quitar armadura"), b ->
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(stego.getId(), 4, 0))));
        toChibi = this.addRenderableWidget(new TexButton(left + 240, top + 128, 110, 14, Component.literal("Volver a chibi"), b -> {
            long now = System.currentTimeMillis();
            if (now < chibiConfirmUntil) {
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(stego.getId(), 5, 0));
                this.onClose();
            } else chibiConfirmUntil = now + 3000L; // pide confirmar con un segundo clic
        }));
        layoutNodes();
    }

    private void sendName() {
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.RenamePet(stego.getId(), nameBox.getValue()));
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (tab == 2 && nameBox.isFocused() && (key == 257 || key == 335)) { sendName(); nameBox.setFocused(false); return true; }
        if (tab == 2 && nameBox.isFocused() && key != 256) return nameBox.keyPressed(key, scan, mods) || true;
        return super.keyPressed(key, scan, mods);
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
            int cx = left + new int[]{146, 219, 293}[t.branch];
            nodeX[i] = cx + sl[0] * 14 - NS / 2;
            nodeY[i] = top + 86 + sl[1] * 20;
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

    private void text(GuiGraphics g, String str, float x, float y, float scale, int color, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(this.font, str, 0, 0, color, shadow);
        g.pose().popPose();
    }

    private void textRight(GuiGraphics g, String str, float xr, float y, float scale, int color) {
        text(g, str, xr - this.font.width(str) * scale, y, scale, color, false);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int rmx = mx, rmy = my;
        mx = (int) (mx / sc); my = (int) (my / sc);
        g.pose().pushPose();
        g.pose().scale(sc, sc, 1.0F);
        g.blit(BG[VI], left, top, W, H, 0.0F, 0.0F, 1568, 882, 1568, 882);
        renderInfoPanel(g);
        renderPortrait(g, mx, my);
        if (tab == 0) {
            renderTree(g);
        } else {
            g.fill(left + 112, top + 27, left + 442, top + 251, 0xF20B1016);
            g.fillGradient(left + 113, top + 28, left + 441, top + 60, (EDGE[VI] & 0x00FFFFFF) | 0x30000000, 0x00000000);
            g.renderOutline(left + 112, top + 27, 330, 224, EDGE[VI]);
            g.renderOutline(left + 114, top + 29, 326, 220, (EDGE[VI] & 0x00FFFFFF) | 0x66000000);
            switch (tab) {
                case 1 -> renderSummary(g);
                case 2 -> renderEquipment(g);
                default -> renderVariants(g);
            }
        }
        for (int i = 0; i < tabButtons.length; i++) tabButtons[i].gold = i == tab;
        unlock.visible = tab == 0;
        ult.visible = tab == 0;
        boolean eq = tab == 2;
        nameBox.visible = eq; nameBox.active = eq; rename.visible = eq; sit.visible = eq;
        dropSaddle.visible = eq; dropSaddle.active = eq && stego.isSaddled();
        dropArmor.visible = eq; dropArmor.active = eq && stego.getArmorTier() > 0;
        toChibi.visible = eq; toChibi.gold = System.currentTimeMillis() < chibiConfirmUntil;
        toChibi.setMessage(Component.literal(toChibi.gold ? "Seguro? Clic otra vez" : "Volver a chibi"));
        if (!eq && nameBox.isFocused()) nameBox.setFocused(false);
        unlock.active = tab == 0 && !ultSel && prog().whyNotUnlock(selected) == null;
        super.render(g, mx, my, pt);
        g.pose().popPose();
        if (tab == 0) renderTooltip(g, mx, my, rmx, rmy);
    }

    private void renderInfoPanel(GuiGraphics g) {
        ProdigyProgress p = prog();
        // placa de nombre y variante
        String nm = stego.hasCustomName() ? stego.getCustomName().getString() : "Stegosaurus";
        text(g, shorten(nm, 62), left + 37, top + 109, 0.85F, 0xFFFFFFFF, true);
        text(g, "Variante: " + VARIANT_NAME[Math.max(0, Math.min(4, stego.getVariant()))], left + 37, top + 117, 0.7F, 0xFF9BE08A, false);
        // nivel y XP
        text(g, "Nivel " + p.level(), left + 11, top + 126, 1.1F, 0xFFFFE9A8, true);
        int bx = left + 11, by = top + 137, bw = 81, bh = 7;
        g.fill(bx, by, bx + bw, by + bh, 0xFF1A1030);
        int need = p.xpToNext();
        int fill = need <= 0 ? bw : Math.min(bw, (int) (bw * (p.xp() / (float) need)));
        if (fill > 0) { g.fillGradient(bx, by, bx + fill, by + bh, XP_HI[VI], XP_LO[VI]); g.fill(bx, by, bx + fill, by + 1, 0x66FFFFFF); }
        g.renderOutline(bx - 1, by - 1, bw + 2, bh + 2, EDGE[VI]);
        String xs = need <= 0 ? "Nivel maximo" : "EXP " + p.xp() + " / " + need;
        text(g, xs, bx + (bw - this.font.width(xs) * 0.62F) / 2.0F, by + 1, 0.62F, 0xFFFFFFFF, true);
        // estadisticas
        int sy = top + 152, sx = left + 14, ex = left + 89;
        String[] names = {"Vida", "Ataque", "Armadura", "Velocidad"};
        String[] vals = {String.valueOf((int) stego.getMaxHealth()), String.valueOf((int) stego.getAttributeValue(Attributes.ATTACK_DAMAGE)),
            String.valueOf((int) stego.getAttributeValue(Attributes.ARMOR)), String.format("%.2f", stego.getAttributeValue(Attributes.MOVEMENT_SPEED))};
        int[] cols = {0xFFFF7A7A, 0xFFFFD27A, 0xFF7ABBFF, 0xFF9BE08A};
        for (int i = 0; i < 4; i++) {
            text(g, names[i], sx, sy + i * 8.2F, 0.72F, 0xFFD9D9D9, false);
            textRight(g, vals[i], ex, sy + i * 8.2F, 0.72F, cols[i]);
        }
        // puntos libres
        String pts = String.valueOf(p.pointsAvailable());
        text(g, pts, left + 57 - this.font.width(pts) * 1.2F / 2.0F, top + 199, 1.2F, 0xFFFFFFFF, true);
        // texto del boton de reinicio
        text(g, "Restablecer", left + 33, top + 218, 0.8F, 0xFFFFFFFF, true);
    }

    private void renderTree(GuiGraphics g) {
        ProdigyProgress p = prog();
        // lineas de requisito (debajo de los nodos)
        for (int i = 0; i < Talents.ALL.length; i++) {
            for (String[] req : Talents.ALL[i].reqs) {
                int r = Talents.index(req[0]);
                if (r < 0) continue;
                int x1 = nodeX[r] + NS / 2, y1 = nodeY[r] + NS - 1, x2 = nodeX[i] + NS / 2, y2 = nodeY[i] + 1;
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
            g.blit(TEX, x, y, NS, NS, (float) u, 0.0F, 30, 30, 256, 256);
            g.pose().pushPose();
            g.pose().translate(x + (NS - 13) / 2.0F, y + (NS - 13) / 2.0F, 0.0F);
            g.pose().scale(13.0F / 16.0F, 13.0F / 16.0F, 1.0F);
            g.renderItem(new ItemStack(icon(t.id)), 0, 0);
            g.pose().popPose();
            if (!owned && !can) {
                g.fill(x + 3, y + 3, x + NS - 3, y + NS - 3, 0x99000000);
                g.blit(TEX, x + 7, y + 6, 8, 10, 40.0F, 30.0F, 8, 10, 256, 256);
            }
            if (i == selected && !ultSel) g.blit(TEX, x - 2, y - 2, NS + 4, NS + 4, 0.0F, 30.0F, 34, 34, 256, 256);
            String rk = p.rank(i) + "/" + t.maxRank;
            text(g, rk, x + NS - this.font.width(rk) * 0.6F + 1, y + NS - 5, 0.6F, owned ? (maxed ? GOLD : TEXT) : DIM, true);
        }
        if (ultSel) g.renderOutline(left + 195, top + 177, 45, 36, GOLD);
        // progreso del arbol
        int total = Talents.ALL.length, got = p.talentsUnlocked();
        int bx = left + 175, by = top + 239, bw = 102, bh = 6;
        g.fill(bx, by, bx + bw, by + bh, 0xFF241808);
        int f = Math.min(bw, bw * got / total);
        if (f > 0) { g.fillGradient(bx, by, bx + f, by + bh, 0xFFFFD35A, 0xFFE08A1C); g.fill(bx, by, bx + f, by + 1, 0x66FFFFFF); }
        text(g, got + " / " + total + " habilidades", left + 284, top + 239, 0.75F, 0xFFFFFFFF, true);
        renderDetail(g);
    }

    private String shorten(String s, int px) {
        while (this.font.width(s) > px && s.length() > 3) s = s.substring(0, s.length() - 1);
        return s;
    }

    private void renderDetail(GuiGraphics g) {
        ProdigyProgress p = prog();
        if (ultSel) {
            int ux = left + 343, uy = top + 45, uw = 90;
            g.pose().pushPose();
            g.pose().translate(ux + 1.0F, uy + 1.0F, 0.0F);
            g.pose().scale(1.4F, 1.4F, 1.0F);
            g.renderItem(new ItemStack(Items.NETHER_STAR), 0, 0);
            g.pose().popPose();
            text(g, "Habilidad", ux + 28, uy + 3, 0.85F, ULT_COLOR[VI], false);
            text(g, "Definitiva", ux + 28, uy + 12, 0.85F, ULT_COLOR[VI], false);
            text(g, "Rango 0 / 1", ux, uy + 28, 0.85F, GOLD, false);
            int uy2 = uy + 40;
            for (String l : new String[]{"Se desbloquea al completar las 3 ramas principales (Combate, Defensa y Soporte)."}) {
                for (FormattedCharSequence line : this.font.split(Component.literal(l), uw * 10 / 8)) {
                    g.pose().pushPose(); g.pose().translate(ux, uy2, 0.0F); g.pose().scale(0.8F, 0.8F, 1.0F);
                    g.drawString(this.font, line, 0, 0, TEXT, false); g.pose().popPose(); uy2 += 8;
                }
            }
            uy2 += 4;
            for (FormattedCharSequence line : this.font.split(Component.literal("Aun no esta implementada en esta version."), uw * 10 / 8)) {
                g.pose().pushPose(); g.pose().translate(ux, uy2, 0.0F); g.pose().scale(0.8F, 0.8F, 1.0F);
                g.drawString(this.font, line, 0, 0, 0xFFE0B04C, false); g.pose().popPose(); uy2 += 8;
            }
            int yy = Math.max(uy2 + 3, top + 118);
            text(g, "Requisitos", ux, yy, 0.9F, GOLD, false); yy += 10;
            String[] bn = {"Combate", "Defensa", "Soporte"};
            for (int b = 0; b < 3; b++) {
                int done = 0, tot = 0;
                for (int i = 0; i < Talents.ALL.length; i++) if (Talents.ALL[i].branch == b) { tot++; if (p.rank(i) >= Talents.ALL[i].maxRank) done++; }
                yy = req(g, ux, yy, done >= tot, bn[b] + " " + done + "/" + tot);
            }
            return;
        }
        Talent t = Talents.ALL[selected];
        int x = left + 343, y = top + 45, w = 90;
        // icono
        g.blit(TEX, x, y, 24, 24, (float) (p.rank(selected) > 0 ? 120 + 30 * t.branch : 30 + 30 * t.branch), 0.0F, 30, 30, 256, 256);
        g.pose().pushPose();
        g.pose().translate(x + 5.0F, y + 5.0F, 0.0F);
        g.pose().scale(14.0F / 16.0F, 14.0F / 16.0F, 1.0F);
        g.renderItem(new ItemStack(icon(t.id)), 0, 0);
        g.pose().popPose();
        List<FormattedCharSequence> name = this.font.split(Component.literal(t.name), (w - 28) * 10 / 8);
        for (int i = 0; i < Math.min(3, name.size()); i++) {
            g.pose().pushPose();
            g.pose().translate(x + 28, y + 1 + i * 8, 0.0F);
            g.pose().scale(0.8F, 0.8F, 1.0F);
            g.drawString(this.font, name.get(i), 0, 0, BRANCH_COLOR[t.branch], false);
            g.pose().popPose();
        }
        int cur = p.rank(selected);
        text(g, "Rango " + cur + " / " + t.maxRank, x, y + 28, 0.85F, GOLD, false);
        int yy = y + 40;
        for (FormattedCharSequence line : this.font.split(Component.literal(t.effect), w * 10 / 8)) {
            if (yy > top + 114) break;
            g.pose().pushPose(); g.pose().translate(x, yy, 0.0F); g.pose().scale(0.8F, 0.8F, 1.0F);
            g.drawString(this.font, line, 0, 0, TEXT, false); g.pose().popPose();
            yy += 8;
        }
        String note = switch (t.id) {
            case "S6" -> "Aviso: aun sin efecto (falta transformacion).";
            case "S3" -> "Cruzar portales: pendiente.";
            case "D5" -> "Efecto solo montado (clic con silla).";
            default -> null;
        };
        if (note != null) {
            for (FormattedCharSequence line : this.font.split(Component.literal(note), w * 10 / 8)) {
                g.pose().pushPose(); g.pose().translate(x, yy, 0.0F); g.pose().scale(0.8F, 0.8F, 1.0F);
                g.drawString(this.font, line, 0, 0, 0xFFE0B04C, false); g.pose().popPose();
                yy += 8;
            }
        }
        yy = Math.max(yy + 3, top + 118);
        text(g, "Requisitos", x, yy, 0.9F, GOLD, false); yy += 10;
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
        if (ok) g.blit(TEX, x, y, 8, 6, 56.0F, 30.0F, 10, 7, 256, 256); else g.blit(TEX, x + 1, y, 6, 6, 68.0F, 30.0F, 8, 7, 256, 256);
        text(g, text, x + 11, y, 0.8F, ok ? 0xFF7CE08A : 0xFFE07C7C, false);
        return y + 8;
    }

    /** Estegosaurio real en el retrato (con su variante, silla y armadura), siguiendo al raton. */
    private void renderPortrait(GuiGraphics g, int mx, int my) {
        int x0 = left + 15, y0 = top + 36, x1 = left + 97, y1 = top + 103;
        g.enableScissor((int) (x0 * sc), (int) (y0 * sc), (int) (x1 * sc), (int) (y1 * sc));
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(g, left + 56, top + 100, 11,
            (float) (left + 56 - mx), (float) (top + 62 - my), stego);
        g.disableScissor();
    }

    private void renderSummary(GuiGraphics g) {
        ProdigyProgress p = prog();
        int x = left + 124;
        text(g, "Resumen", x, top + 34, 1.0F, GOLD, true);
        text(g, "Nivel " + p.level() + " (max. 100)", x, top + 48, 0.85F, TEXT, false);
        text(g, "Puntos: ganados " + p.pointsEarned() + ", gastados " + p.pointsSpent() + ", libres " + p.pointsAvailable(), x, top + 59, 0.85F, TEXT, false);
        text(g, "Talentos desbloqueados:", x, top + 74, 0.85F, GOLD, false);
        int n = 0;
        for (int i = 0; i < Talents.ALL.length; i++) if (p.rank(i) > 0) {
            int col = n / 8, row = n % 8;
            text(g, "- " + shorten(Talents.ALL[i].name, 100) + " " + p.rank(i) + "/" + Talents.ALL[i].maxRank, x + col * 106, top + 86 + row * 10, 0.8F, TEXT, false);
            n++;
        }
        if (n == 0) text(g, "(ninguno)", x, top + 86, 0.8F, DIM, false);
    }

    private static final String[] ARMOR_NAME = {"sin armadura", "hierro", "oro", "diamante", "netherita"};

    private void renderEquipment(GuiGraphics g) {
        int x = left + 124;
        text(g, "Nombre", x, top + 34, 1.0F, GOLD, true);
        text(g, "Equipamiento", x, top + 72, 1.0F, GOLD, true);
        int t = stego.getArmorTier();
        text(g, "Silla: " + (stego.isSaddled() ? "equipada" : "sin silla (clic con una silla en la mano)"), x, top + 84, 0.85F, TEXT, false);
        text(g, "Armadura: " + ARMOR_NAME[Math.max(0, Math.min(4, t))] + (t > 0
            ? "  (+" + StegosaurusEntity.ARMOR_VALUE[t] + " armadura" + (StegosaurusEntity.TOUGHNESS_VALUE[t] > 0 ? ", +" + StegosaurusEntity.TOUGHNESS_VALUE[t] + " dureza" : "") + ")" : ""), x, top + 95, 0.85F, TEXT, false);
        text(g, "Armadura: clic derecho sobre el estegosaurio con ella en la mano.", x, top + 156, 0.75F, DIM, false);
        text(g, "Montado: clic izq. mordida, clic der. coletazo, V embestida.", x, top + 166, 0.75F, DIM, false);
        text(g, "Tambien puedes ponerle nombre con una etiqueta (name tag).", x, top + 176, 0.75F, DIM, false);
    }

    private void renderVariants(GuiGraphics g) {
        int x = left + 124;
        text(g, "Variantes", x, top + 34, 1.0F, GOLD, true);
        text(g, "Variante actual: " + VARIANT_NAME[VI], x, top + 50, 0.9F, TEXT, false);
        text(g, "La interfaz y el HUD usan los colores de tu variante.", x, top + 62, 0.8F, DIM, false);
        text(g, "Pasivas por variante y habilidad definitiva:", x, top + 82, 0.85F, DIM, false);
        text(g, "aun no estan implementadas en esta version.", x, top + 92, 0.85F, DIM, false);
        text(g, "Hoy la variante Magma es inmune al fuego.", x, top + 108, 0.85F, TEXT, false);
    }

    private void renderTooltip(GuiGraphics g, int mx, int my, int rmx, int rmy) {
        for (int i = 0; i < Talents.ALL.length; i++) {
            if (mx >= nodeX[i] && mx < nodeX[i] + NS && my >= nodeY[i] && my < nodeY[i] + NS) {
                g.renderTooltip(this.font, Component.literal(Talents.ALL[i].name + "  " + prog().rank(i) + "/" + Talents.ALL[i].maxRank), rmx, rmy);
                return;
            }
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        mx /= sc; my /= sc;
        if (tab == 0 && button == 0) {
            for (int i = 0; i < Talents.ALL.length; i++) {
                if (mx >= nodeX[i] && mx < nodeX[i] + NS && my >= nodeY[i] && my < nodeY[i] + NS) {
                    selected = i;
                    ultSel = false;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
