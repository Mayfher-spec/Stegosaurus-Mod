package com.prodigytown.creatures.client;

import com.prodigytown.creatures.PetOrders;
import com.prodigytown.creatures.net.ModNetwork;
import com.prodigytown.creatures.progress.ProdigyProgress;
import com.prodigytown.creatures.progress.Talent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Pantalla unica de companero (tecla K) para el estegosaurio, el carnotauro y el dilofosaurio: mismo panel principal, mismos comandos (las 8 ordenes),
 * misma lista de habilidades y el mismo arbol de talentos (tres ramas + definitiva), con el estilo del poster (piedra oscura, marcos dorados).
 * Todo se dibuja en vivo con datos reales de la entidad; el servidor valida cada accion. Coordenadas virtuales de 720x560 que se reducen para caber.
 */
public final class PetScreen extends Screen {
    private static final int W = 720, H = 560;
    private static final int GOLD = 0xFFFFD35A, CREAM = 0xFFF6E3A1, TEXT = 0xFFEDEDED, DIM = 0xFF9AA0A6, EDGE = 0xFF9C7A3A;
    private static final int[] BRANCH_COLOR = {0xFFE8602C, 0xFF3FB4FF, 0xFF7CE03C, 0xFFFFD35A};
    private static final ResourceLocation STONE = new ResourceLocation("minecraft", "textures/block/stone_bricks.png");
    /** Orden de las 8 casillas de comandos (4 x 2) y su icono. */
    private static final int[] GRID = {PetOrders.FOLLOW, PetOrders.STAY, PetOrders.DEFEND, PetOrders.PATROL,
        PetOrders.WANDER, PetOrders.SLEEP, PetOrders.SIT, PetOrders.ATTACK};
    private static final Item[] ORDER_ICON = {Items.LEAD, Items.CLOCK, Items.COMPASS, Items.RED_BANNER, Items.OAK_STAIRS, Items.RED_BED, Items.SHIELD, Items.IRON_SWORD};
    private static final int NS = 28;

    private final PetSpec spec;
    private final TamableAnimal pet;
    private float sc = 1.0F;
    private int left, top;
    private int selected = -1;
    private boolean renaming = false;
    private EditBox nameBox;
    private long resetUntil = 0L, chibiUntil = 0L;
    private int[] nodeX, nodeY;

    public PetScreen(TamableAnimal pet) {
        super(Component.literal("Compañero"));
        this.pet = pet;
        this.spec = PetSpec.of(pet);
    }

    private ProdigyProgress prog() { return spec.progress(); }

    // ---------------------------------------------------------------- inicio
    @Override protected void init() {
        sc = Math.max(0.4F, Math.min(1.5F, Math.min((this.width - 8.0F) / W, (this.height - 8.0F) / H)));
        left = (int) ((this.width / sc - W) / 2.0F);
        top = (int) ((this.height / sc - H) / 2.0F);
        nameBox = new EditBox(this.font, left + 156, top + 62, 134, 16, Component.literal("Nombre"));
        nameBox.setMaxLength(32);
        nameBox.setValue(pet.hasCustomName() ? pet.getCustomName().getString() : "");
        nameBox.setHint(Component.literal("Nombre"));
        nameBox.visible = renaming; nameBox.active = renaming;
        addRenderableWidget(nameBox);
        layoutTree();
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void tick() {
        super.tick();
        if (!pet.isAlive() || pet.isRemoved()) this.onClose();
    }

    // ---------------------------------------------------------------- arbol: colocacion automatica (profundidad por requisitos)
    private int depthOf(Talent[] all, int i, int[] memo) {
        if (memo[i] >= 0) return memo[i];
        int d = 0;
        for (String[] r : all[i].reqs)
            for (int k = 0; k < all.length; k++) if (all[k].id.equals(r[0])) d = Math.max(d, depthOf(all, k, memo) + 1);
        return memo[i] = d;
    }

    private void layoutTree() {
        Talent[] all = prog().table();
        nodeX = new int[all.length]; nodeY = new int[all.length];
        int[] memo = new int[all.length];
        java.util.Arrays.fill(memo, -1);
        int[] colCx = {302, 406, 510};
        for (int b = 0; b < 3; b++) {
            for (int d = 0; d < 8; d++) {
                int n = 0;
                for (int i = 0; i < all.length; i++) if (!PetSpec.isFinalTalent(all[i].id) && all[i].branch == b && depthOf(all, i, memo) == d) n++;
                int k = 0;
                for (int i = 0; i < all.length; i++) {
                    if (PetSpec.isFinalTalent(all[i].id) || all[i].branch != b || depthOf(all, i, memo) != d) continue;
                    nodeX[i] = Math.round(colCx[b] + (k - (n - 1) / 2.0F) * 40.0F);
                    nodeY[i] = 336 + d * 34;
                    k++;
                }
            }
        }
        int nf = 0;
        for (Talent t : all) if (PetSpec.isFinalTalent(t.id)) nf++;
        int k = 0;
        for (int i = 0; i < all.length; i++) {
            if (!PetSpec.isFinalTalent(all[i].id)) continue;
            nodeX[i] = nf == 1 ? 406 : Math.round(302 + k * (208.0F / Math.max(1, nf - 1)));
            nodeY[i] = 522;
            k++;
        }
        if (selected < 0 || selected >= all.length) selected = 0;
    }

    // ---------------------------------------------------------------- entrada
    private boolean in(double vx, double vy, int x, int y, int w, int h) { return vx >= x && vx < x + w && vy >= y && vy < y + h; }

    private void send(int action, int index) { ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), action, index)); }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        mx /= sc; my /= sc;
        double vx = mx - left, vy = my - top;
        if (button == 0) {
            if (in(vx, vy, 688, 6, 24, 22)) { this.onClose(); return true; }
            // nombre
            if (in(vx, vy, 158, 62, 132, 16) && !renaming) { renaming = true; nameBox.visible = true; nameBox.active = true; setFocused(nameBox); nameBox.setFocused(true); return true; }
            if (renaming && !in(vx, vy, 156, 62, 134, 16)) { sendName(); }
            // equipo
            if (in(vx, vy, 296, 66, 56, 48) && spec.adult()) { send(3, 0); return true; }
            if (in(vx, vy, 296, 120, 56, 48) && spec.adult()) { send(4, 0); return true; }
            if (in(vx, vy, 296, 174, 56, 48) && spec.adult() && !spec.fallen()) {
                long now = System.currentTimeMillis();
                if (now < chibiUntil) { send(5, 0); this.onClose(); } else chibiUntil = now + 3000L;
                return true;
            }
            // comandos
            if (!spec.fallen()) {
                for (int i = 0; i < GRID.length; i++) {
                    int[] r = tile(i);
                    if (in(vx, vy, r[0], r[1], 74, 54)) {
                        int order = GRID[i];
                        ModNetwork.CHANNEL.sendToServer(new ModNetwork.SetOrder(pet.getId(), order));
                        if (order == PetOrders.ATTACK) this.onClose(); // para apuntar al objetivo
                        return true;
                    }
                }
            }
            // talentos
            if (spec.adult()) {
                Talent[] all = prog().table();
                for (int i = 0; i < all.length; i++)
                    if (in(vx, vy, nodeX[i] - NS / 2, nodeY[i] - NS / 2, NS, NS)) { selected = i; return true; }
                if (in(vx, vy, 574, 508, 124, 26) && prog().whyNotUnlock(selected) == null) { send(0, selected); return true; }
                if (in(vx, vy, 608, 266, 94, 18)) {
                    long now = System.currentTimeMillis();
                    if (now < resetUntil) { send(1, 0); resetUntil = 0L; } else resetUntil = now + 3000L;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private int[] tile(int i) { return new int[]{394 + (i % 4) * 80, 72 + (i / 4) * 60}; }

    private void sendName() {
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.RenamePet(pet.getId(), nameBox.getValue()));
        renaming = false; nameBox.setFocused(false); nameBox.visible = false; nameBox.active = false;
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (renaming) {
            if (key == 257 || key == 335) { sendName(); return true; }
            if (key == 256) { renaming = false; nameBox.setFocused(false); nameBox.visible = false; nameBox.active = false; return true; }
            return nameBox.keyPressed(key, scan, mods) || true;
        }
        if (ClientKeys.OPEN.matches(key, scan)) { this.onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    // ---------------------------------------------------------------- utilidades de dibujo
    private void text(GuiGraphics g, String s, float x, float y, float scale, int color, boolean shadow) {
        g.pose().pushPose(); g.pose().translate(x, y, 0.0F); g.pose().scale(scale, scale, 1.0F);
        g.drawString(this.font, s, 0, 0, color, shadow); g.pose().popPose();
    }
    private void textC(GuiGraphics g, String s, float cx, float y, float scale, int color, boolean shadow) {
        text(g, s, cx - this.font.width(s) * scale / 2.0F, y, scale, color, shadow);
    }
    private void textR(GuiGraphics g, String s, float xr, float y, float scale, int color, boolean shadow) {
        text(g, s, xr - this.font.width(s) * scale, y, scale, color, shadow);
    }
    private float para(GuiGraphics g, String s, float x, float y, float width, float scale, float pitch, int color, int maxLines) {
        List<FormattedCharSequence> lines = this.font.split(Component.literal(s), (int) (width / scale));
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            g.pose().pushPose(); g.pose().translate(x, y, 0.0F); g.pose().scale(scale, scale, 1.0F);
            g.drawString(this.font, lines.get(i), 0, 0, color, false); g.pose().popPose();
            y += pitch;
        }
        return y;
    }
    private String shorten(String s, float px, float scale) {
        while (this.font.width(s) * scale > px && s.length() > 3) s = s.substring(0, s.length() - 1);
        return s;
    }
    private void item(GuiGraphics g, Item it, float x, float y, float scale, float alpha) {
        g.pose().pushPose(); g.pose().translate(x, y, 0.0F); g.pose().scale(scale, scale, 1.0F);
        if (alpha < 1.0F) g.setColor(1.0F, 1.0F, 1.0F, alpha);
        g.renderItem(new ItemStack(it), 0, 0);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.pose().popPose();
    }
    private static String sec(int s) { return s + " s"; }

    /** Panel con marco dorado y placa de titulo (estilo del poster). */
    private void panel(GuiGraphics g, int x, int y, int w, int h, String title, Item icon) {
        x += left; y += top;
        g.fill(x, y, x + w, y + h, 0xE60E0A08);
        g.renderOutline(x, y, w, h, 0xFF2A1C10);
        g.renderOutline(x + 1, y + 1, w - 2, h - 2, EDGE);
        g.renderOutline(x + 2, y + 2, w - 4, h - 4, 0xFF3A2A14);
        for (int cx : new int[]{x - 1, x + w - 4}) for (int cy : new int[]{y - 1, y + h - 4}) g.fill(cx, cy, cx + 5, cy + 5, GOLD);
        int tw = (int) (this.font.width(title) * 1.15F) + 38;
        g.fill(x + 12, y - 10, x + 12 + tw, y + 12, 0xFF1C130B);
        g.renderOutline(x + 12, y - 10, tw, 22, EDGE);
        item(g, icon, x + 17, y - 6, 0.9F, 1.0F);
        text(g, title, x + 36, y - 3, 1.15F, CREAM, true);
    }

    private void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int c1, int c2, String label) {
        g.fill(x, y, x + w, y + h, 0xFF1A1210);
        g.renderOutline(x, y, w, h, 0xFF4A3A22);
        int fw = Math.round((w - 2) * Math.max(0.0F, Math.min(1.0F, frac)));
        if (fw > 0) { g.fillGradient(x + 1, y + 1, x + 1 + fw, y + h - 1, c1, c2); g.fill(x + 1, y + 1, x + 1 + fw, y + 2, 0x55FFFFFF); }
        if (label != null) textC(g, label, x + w / 2.0F, y + (h - 7.0F * 0.85F) / 2.0F, 0.85F, 0xFFFFFFFF, true);
    }

    // ---------------------------------------------------------------- dibujo
    @Override public void render(GuiGraphics g, int rmx, int rmy, float pt) {
        this.renderBackground(g);
        double mxv = rmx / sc, myv = rmy / sc;
        int mx = (int) mxv, my = (int) myv;
        g.pose().pushPose();
        g.pose().scale(sc, sc, 1.0F);
        // fondo de piedra oscura
        g.fill(left - 6, top - 6, left + W + 6, top + H + 6, 0xFF120D09);
        g.setColor(0.42F, 0.36F, 0.32F, 1.0F);
        for (int ty = 0; ty < H; ty += 32) for (int tx = 0; tx < W; tx += 32)
            g.blit(STONE, left + tx, top + ty, Math.min(32, W - tx), Math.min(32, H - ty), 0.0F, 0.0F, Math.min(32, W - tx) / 2, Math.min(32, H - ty) / 2, 16, 16);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.renderOutline(left - 6, top - 6, W + 12, H + 12, EDGE);
        g.renderOutline(left - 5, top - 5, W + 10, H + 10, 0xFF2A1C10);
        renderHeader(g);
        renderMain(g, mx, my);
        renderCommands(g, mx, my);
        renderAbilities(g);
        renderTalents(g, mx, my);
        super.render(g, mx, my, pt);
        g.pose().popPose();
        renderTooltips(g, mx, my, rmx, rmy);
    }

    private void renderHeader(GuiGraphics g) {
        int cx = left + W / 2;
        g.fill(cx - 190, top - 2, cx + 190, top + 31, 0xFF1C130B);
        g.renderOutline(cx - 190, top - 2, 380, 33, EDGE);
        g.fill(cx - 189, top + 29, cx + 189, top + 31, spec.accent);
        textC(g, spec.species.toUpperCase(), cx, top + 1, 2.0F, GOLD, true);
        textC(g, spec.subtitle.toUpperCase(), cx, top + 19, 0.8F, CREAM, false);
        g.fill(left + 688, top + 6, left + 712, top + 28, 0xFF3A1A14);
        g.renderOutline(left + 688, top + 6, 24, 22, 0xFFB8503A);
        textC(g, "X", left + 700, top + 13, 1.2F, 0xFFFFE0D0, true);
    }

    // ---- panel principal
    private void renderMain(GuiGraphics g, int mx, int my) {
        panel(g, 12, 44, 360, 208, "Panel principal", spec.species.startsWith("Dilo") ? Items.SPIDER_EYE : Items.BONE);
        ProdigyProgress p = prog();
        // retrato
        int px0 = left + 20, py0 = top + 66, px1 = left + 150, py1 = top + 238;
        g.fill(px0, py0, px1, py1, 0xFF0B100E);
        g.renderOutline(px0, py0, px1 - px0, py1 - py0, spec.accent);
        g.enableScissor((int) (px0 * sc), (int) (py0 * sc), (int) (px1 * sc), (int) (py1 * sc));
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, (px0 + px1) / 2, py1 - 14, spec.adult() ? 26 : 46,
            (float) ((px0 + px1) / 2 - mx), (float) (py0 + 60 - my), pet);
        g.disableScissor();
        // nombre (clic para renombrar)
        if (!renaming) {
            String nm = pet.hasCustomName() ? pet.getCustomName().getString() : spec.species;
            text(g, shorten(nm, 130, 1.3F), left + 158, top + 64, 1.3F, GOLD, true);
        }
        // nivel y chips
        text(g, "Nv. " + p.level(), left + 158, top + 84, 1.0F, CREAM, true);
        text(g, spec.female() ? "Hembra" : "Macho", left + 200, top + 84, 0.9F, spec.female() ? 0xFFF08AC0 : 0xFF7CB8F0, true);
        text(g, spec.variantName(), left + 244, top + 84, 0.9F, 0xFF9BE08A, true);
        // barras
        float hp = Math.max(0.0F, pet.getHealth()), max = Math.max(1.0F, pet.getMaxHealth());
        boolean weak = hp / max < PetOrders.WEAK_FRACTION;
        text(g, "Vida", left + 158, top + 98, 0.8F, DIM, false);
        bar(g, left + 158, top + 106, 132, 12, hp / max, weak ? 0xFFE8483C : 0xFFE8403A, weak ? 0xFF9A1E16 : 0xFF931616, Math.round(hp) + " / " + Math.round(max));
        int need = p.xpToNext();
        text(g, "Experiencia", left + 158, top + 121, 0.8F, DIM, false);
        bar(g, left + 158, top + 129, 132, 12, need <= 0 ? 1.0F : p.xp() / (float) Math.max(1, need), 0xFF58B8F0, 0xFF2A6CC0, need <= 0 ? "Nivel máximo" : p.xp() + " / " + need);
        // estadisticas
        String[][] rows = {
            {"Daño", String.valueOf((int) pet.getAttributeValue(Attributes.ATTACK_DAMAGE))},
            {"Velocidad", String.format("%.2f", pet.getAttributeValue(Attributes.MOVEMENT_SPEED))},
            {"Armadura", String.valueOf((int) pet.getAttributeValue(Attributes.ARMOR))},
            {"Puntos libres", String.valueOf(p.pointsAvailable())}};
        Item[] ri = {Items.IRON_SWORD, Items.FEATHER, Items.IRON_CHESTPLATE, Items.EXPERIENCE_BOTTLE};
        for (int i = 0; i < 4; i++) {
            int y = top + 148 + i * 17;
            item(g, ri[i], left + 158, y - 3, 0.7F, 1.0F);
            text(g, rows[i][0], left + 173, y, 0.9F, DIM, false);
            textR(g, rows[i][1], left + 290, y, 0.9F, i == 3 ? GOLD : 0xFFFFFFFF, true);
        }
        // orden actual
        String cur = spec.fallen() ? "Fuera de combate" : PetOrders.NAMES[Math.max(0, Math.min(PetOrders.COUNT - 1, spec.order()))];
        g.fill(left + 158, top + 218, left + 290, top + 236, 0xFF1C130B);
        g.renderOutline(left + 158, top + 218, 132, 18, spec.fallen() ? 0xFFE0453A : EDGE);
        textC(g, "Orden: " + cur, left + 224, top + 223, 0.9F, spec.fallen() ? 0xFFE0453A : GOLD, true);
        // equipo
        slot(g, 66, "Montura", Items.SADDLE, spec.saddled(), spec.saddled() ? "Quitar" : "Sin silla", false, mx, my);
        slot(g, 120, "Armadura", spec.armorItem(), spec.armorTier() > 0, spec.armorTier() > 0 ? "Quitar" : "Sin armadura", false, mx, my);
        boolean armed = System.currentTimeMillis() < chibiUntil;
        slot(g, 174, "Cría", Items.EGG, spec.adult() && !spec.fallen(), armed ? "¿Seguro?" : spec.adult() ? "Volver a cría" : "Ya es cría", armed, mx, my);
    }

    private void slot(GuiGraphics g, int y, String label, Item it, boolean on, String status, boolean armed, int mx, int my) {
        int x = left + 296, yy = top + y;
        boolean hot = on && in(mx - left, my - top, 296, y, 56, 48);
        g.fill(x, yy, x + 56, yy + 48, on ? 0xE0141A1C : 0x90101418);
        g.renderOutline(x, yy, 56, 48, armed ? 0xFFE0453A : !on ? 0xFF4A4F54 : hot ? GOLD : EDGE);
        textC(g, label, x + 28, yy + 3, 0.8F, on ? CREAM : DIM, false);
        item(g, it, x + 20, yy + 14, 1.0F, on ? 1.0F : 0.4F);
        textC(g, shorten(status, 52, 0.7F), x + 28, yy + 37, 0.7F, armed ? 0xFFFFB0A0 : on ? 0xFF9BE08A : DIM, false);
    }

    // ---- comandos
    private void renderCommands(GuiGraphics g, int mx, int my) {
        panel(g, 382, 44, 326, 208, "Comandos", Items.LEAD);
        boolean fallen = spec.fallen();
        int cur = spec.order();
        for (int i = 0; i < GRID.length; i++) {
            int order = GRID[i];
            int[] r = tile(i);
            int x = left + r[0], y = top + r[1];
            boolean sel = !fallen && order == cur;
            boolean hot = !fallen && in(mx - left, my - top, r[0], r[1], 74, 54);
            g.fillGradient(x, y, x + 74, y + 54, sel ? 0xFF3A2C0E : hot ? 0xFF2A241C : 0xFF1A1511, sel ? 0xFF1E1606 : 0xFF0E0B09);
            g.renderOutline(x, y, 74, 54, sel ? GOLD : hot ? 0xFFE8C46A : 0xFF5A4A2A);
            if (sel) g.renderOutline(x + 1, y + 1, 72, 52, 0x99FFD35A);
            item(g, ORDER_ICON[order], x + 25, y + 7, 1.5F, fallen ? 0.35F : 1.0F);
            textC(g, PetOrders.NAMES[order], x + 37, y + 41, 0.85F, sel ? GOLD : fallen ? 0xFF7A7F85 : 0xFFFFFFFF, true);
        }
        String title = fallen ? "Fuera de combate" : PetOrders.NAMES[Math.max(0, Math.min(PetOrders.COUNT - 1, cur))];
        String body = fallen ? "Dormido: no recibe órdenes. Reanímalo con " + spec.reviveHint + " (clic derecho): se recupera con 20 de vida."
            : PetOrders.HELP[Math.max(0, Math.min(PetOrders.COUNT - 1, cur))];
        text(g, title, left + 394, top + 198, 1.1F, fallen ? 0xFFE0453A : GOLD, true);
        para(g, body, left + 394, top + 212, 306, 0.9F, 10.0F, TEXT, 3);
    }

    // ---- habilidades
    private void renderAbilities(GuiGraphics g) {
        panel(g, 12, 262, 222, 286, "Habilidades activas", Items.BLAZE_POWDER);
        List<PetSpec.Ability> list = spec.abilities;
        int pitch = Math.min(52, 248 / Math.max(1, list.size()));
        boolean adult = spec.adult();
        for (int i = 0; i < list.size(); i++) {
            PetSpec.Ability a = list.get(i);
            int x = left + 20, y = top + 288 + i * pitch;
            g.fill(x, y, x + 206, y + pitch - 3, 0xB0100C09);
            g.renderOutline(x, y, 206, pitch - 3, 0xFF4A3A22);
            boolean locked = a.cd() >= 0 && a.locked().getAsBoolean();
            g.fill(x + 4, y + 4, x + 38, y + 38, 0xFF0B100E);
            g.renderOutline(x + 4, y + 4, 34, 34, locked ? 0xFF4A4F54 : spec.accent);
            item(g, a.icon(), x + 13, y + 13, 1.0F, locked ? 0.35F : 1.0F);
            text(g, shorten(a.name(), 152, 1.0F), x + 44, y + 4, 1.0F, GOLD, true);
            text(g, shorten("Tecla: " + a.key().get(), 100, 0.8F), x + 44, y + 15, 0.8F, CREAM, false);
            float left2 = spec.cdLeft(a.cd());
            String st = a.cd() < 0 ? "Pasiva" : locked ? a.lockedText() : left2 > 0 ? sec(spec.cdSeconds(a.cd())) : "Lista";
            textR(g, st, x + 202, y + 15, 0.8F, locked ? 0xFFE0A040 : left2 > 0 ? 0xFFE0A040 : 0xFF6FD05A, true);
            para(g, a.desc(), x + 44, y + 25, 158, 0.7F, 7.6F, DIM, Math.max(1, (pitch - 28) / 8));
        }
        if (!adult) shade(g, 20, 288, 206, 248, "Crece para despertar sus habilidades");
    }

    private void shade(GuiGraphics g, int x, int y, int w, int h, String msg) {
        g.fill(left + x, top + y, left + x + w, top + y + h, 0xE8101418);
        g.renderOutline(left + x, top + y, w, h, EDGE);
        para(g, msg, left + x + 14, top + y + h / 2 - 10, w - 28, 1.1F, 12.0F, GOLD, 3);
    }

    // ---- arbol de talentos
    private void renderTalents(GuiGraphics g, int mx, int my) {
        panel(g, 242, 262, 466, 286, "Árbol de talentos", Items.EXPERIENCE_BOTTLE);
        ProdigyProgress p = prog();
        Talent[] all = p.table();
        // cabecera: progreso, puntos y restablecer
        int total = all.length, got = p.talentsUnlocked();
        textR(g, got + " / " + total + " talentos", left + 596, top + 270, 0.85F, DIM, false);
        boolean arm = System.currentTimeMillis() < resetUntil;
        g.fill(left + 608, top + 266, left + 702, top + 284, arm ? 0xFF3A1010 : 0xFF1C130B);
        g.renderOutline(left + 608, top + 266, 94, 18, arm ? 0xFFE0453A : EDGE);
        textC(g, arm ? "¿Seguro? Otra vez" : "Restablecer", left + 655, top + 271, 0.85F, arm ? 0xFFFFE0D0 : CREAM, true);
        // etiquetas de rama
        int[] colCx = {302, 406, 510};
        for (int b = 0; b < 3; b++) {
            int c = BRANCH_COLOR[b];
            g.fill(left + colCx[b] - 48, top + 290, left + colCx[b] + 48, top + 308, (c & 0x00FFFFFF) | 0x40000000);
            g.renderOutline(left + colCx[b] - 48, top + 290, 96, 18, c);
            textC(g, spec.branchNames[b], left + colCx[b], top + 295, 0.95F, 0xFFFFFFFF, true);
        }
        g.fill(left + 258, top + 490, left + 566, top + 491, 0xFF3A2A14);
        text(g, spec.finalName, left + 262, top + 494, 0.85F, GOLD, true);
        // lineas de requisito
        for (int i = 0; i < all.length; i++) {
            for (String[] req : all[i].reqs) {
                int r = -1;
                for (int k = 0; k < all.length; k++) if (all[k].id.equals(req[0])) r = k;
                if (r < 0) continue;
                int x1 = left + nodeX[r], y1 = top + nodeY[r] + NS / 2, x2 = left + nodeX[i], y2 = top + nodeY[i] - NS / 2;
                boolean met = p.rank(r) >= Integer.parseInt(req[1]);
                int col = met ? BRANCH_COLOR[Math.max(0, Math.min(3, all[i].branch))] : 0xFF3A4046;
                int ym = (y1 + y2) / 2;
                g.fill(x1 - 1, y1, x1 + 1, ym + 1, col);
                g.fill(Math.min(x1, x2) - 1, ym, Math.max(x1, x2) + 1, ym + 2, col);
                g.fill(x2 - 1, ym, x2 + 1, y2, col);
            }
        }
        for (int i = 0; i < all.length; i++) {
            Talent t = all[i];
            int x = left + nodeX[i] - NS / 2, y = top + nodeY[i] - NS / 2;
            boolean owned = p.rank(i) > 0, maxed = p.rank(i) >= t.maxRank, can = p.whyNotUnlock(i) == null;
            int color = BRANCH_COLOR[PetSpec.isFinalTalent(t.id) ? 3 : Math.max(0, Math.min(2, t.branch))];
            int bg = maxed ? 0xFF4A3A10 : owned ? 0xFF1E2A1E : 0xFF14181C;
            int edge = maxed ? GOLD : owned || can ? color : 0xFF4A4F54;
            if (can || owned) g.fill(x - 2, y - 2, x + NS + 2, y + NS + 2, (edge & 0x00FFFFFF) | 0x30000000);
            g.fill(x, y, x + NS, y + NS, bg);
            g.renderOutline(x, y, NS, NS, edge);
            g.renderOutline(x + 1, y + 1, NS - 2, NS - 2, (edge & 0x00FFFFFF) | 0x55000000);
            item(g, spec.talentIcon(t.id), x + (NS - 16) / 2.0F, y + (NS - 16) / 2.0F - 1.0F, 1.0F, 1.0F);
            if (!owned && !can) {
                g.fill(x + 2, y + 2, x + NS - 2, y + NS - 2, 0xAA000000);
                g.fill(x + NS / 2 - 3, y + NS / 2 - 1, x + NS / 2 + 3, y + NS / 2 + 4, 0xFFA0A4A8);
                g.fill(x + NS / 2 - 2, y + NS / 2 - 4, x + NS / 2 + 2, y + NS / 2 - 1, 0xFF70747A);
            }
            if (i == selected) g.renderOutline(x - 3, y - 3, NS + 6, NS + 6, 0xFFFFFFFF);
            String rk = p.rank(i) + "/" + t.maxRank;
            text(g, rk, x + NS - this.font.width(rk) * 0.7F, y + NS - 6.5F, 0.7F, owned ? (maxed ? GOLD : TEXT) : DIM, true);
        }
        renderDetail(g, p, all);
        if (!spec.adult()) shade(g, 250, 290, 452, 250, "Los talentos se desbloquean al crecer. Aliméntalo hasta que sea adulto.");
    }

    private void req(GuiGraphics g, float x, float y, boolean ok, String s) {
        text(g, ok ? "+" : "-", x, y, 0.85F, ok ? 0xFF7CE08A : 0xFFE07C7C, false);
        text(g, shorten(s, 118, 0.8F), x + 9, y, 0.8F, ok ? 0xFF7CE08A : 0xFFE07C7C, false);
    }

    private void renderDetail(GuiGraphics g, ProdigyProgress p, Talent[] all) {
        if (selected < 0 || selected >= all.length) return;
        Talent t = all[selected];
        int x = left + 570, y = top + 292;
        int col = BRANCH_COLOR[PetSpec.isFinalTalent(t.id) ? 3 : Math.max(0, Math.min(2, t.branch))];
        int cur = p.rank(selected);
        g.fill(x, y, x + 36, y + 36, 0xFF0B100E);
        g.renderOutline(x, y, 36, 36, cur > 0 ? col : 0xFF5A6066);
        item(g, spec.talentIcon(t.id), x + 6, y + 6, 1.5F, 1.0F);
        para(g, t.name, x + 42, y + 2, 92, 0.9F, 10.0F, 0xFFFFF0C0, 3);
        g.fill(x, y + 40, x + 90, y + 52, (col & 0x00FFFFFF) | 0x40000000);
        g.renderOutline(x, y + 40, 90, 12, col);
        textC(g, "Rango " + cur + " / " + t.maxRank, x + 45, y + 42, 0.8F, 0xFFFFFFFF, false);
        float yy = para(g, t.effect, x, y + 58, 132, 0.78F, 8.6F, TEXT, 9);
        float ry = Math.max(yy + 6, top + 418);
        text(g, "Requisitos", x, ry, 0.9F, GOLD, true); ry += 11;
        int next = Math.min(cur, t.maxRank - 1);
        req(g, x, ry, p.level() >= t.minLevel[next], "Nivel " + t.minLevel[next]); ry += 9.5F;
        for (String[] r : t.reqs) {
            int need = Integer.parseInt(r[1]);
            String nm = r[0];
            for (Talent o : all) if (o.id.equals(r[0])) nm = o.name;
            req(g, x, ry, p.rank(r[0]) >= need, nm + " " + need); ry += 9.5F;
        }
        req(g, x, ry, cur >= t.maxRank || p.pointsAvailable() >= t.cost[next], "Coste: " + t.cost[next] + " pts"); ry += 9.5F;
        for (String ex : t.exclusive) {
            String nm = ex;
            for (Talent o : all) if (o.id.equals(ex)) nm = o.name;
            req(g, x, ry, p.rank(ex) == 0, "Excluye " + nm); ry += 9.5F;
        }
        boolean can = p.whyNotUnlock(selected) == null;
        boolean maxed = cur >= t.maxRank;
        int bx = left + 574, by = top + 508;
        g.fillGradient(bx, by, bx + 124, by + 26, can ? 0xFF3C7A2A : 0xFF1C1F22, can ? 0xFF1E4A14 : 0xFF101214);
        g.renderOutline(bx, by, 124, 26, can ? 0xFF7CE03C : maxed ? GOLD : 0xFF4A4F54);
        textC(g, maxed ? "Máximo" : "Desbloquear", bx + 62, by + 8, 1.2F, can ? 0xFFFFFFFF : maxed ? GOLD : 0xFF8A9096, true);
        if (!can && !maxed) {
            String why = p.whyNotUnlock(selected);
            if (why != null) para(g, why, left + 574, top + 494, 126, 0.7F, 8.0F, 0xFFE0A04C, 1);
        }
    }

    private void renderTooltips(GuiGraphics g, int mx, int my, int rmx, int rmy) {
        double vx = mx - left, vy = my - top;
        if (in(vx, vy, 296, 66, 56, 48)) g.renderTooltip(this.font, Component.literal(spec.saddled() ? "Quitar silla" : "Sin silla (clic con una silla en la mano)"), rmx, rmy);
        else if (in(vx, vy, 296, 120, 56, 48)) g.renderTooltip(this.font, Component.literal(spec.armorTier() > 0 ? "Quitar armadura" : "Sin armadura (clic con una en la mano)"), rmx, rmy);
        else if (in(vx, vy, 296, 174, 56, 48)) g.renderTooltip(this.font, Component.literal(!spec.adult() ? "Ya es una cría"
            : System.currentTimeMillis() < chibiUntil ? "Pulsa otra vez para confirmar" : "Volver a cría (suelta silla y armadura)"), rmx, rmy);
        else if (in(vx, vy, 158, 62, 132, 16) && !renaming) g.renderTooltip(this.font, Component.literal("Clic para ponerle nombre"), rmx, rmy);
        else if (in(vx, vy, 394, 72, 314, 114)) {
            for (int i = 0; i < GRID.length; i++) {
                int[] r = tile(i);
                if (in(vx, vy, r[0], r[1], 74, 54)) { g.renderTooltip(this.font, Component.literal(PetOrders.HELP[GRID[i]]), rmx, rmy); break; }
            }
        } else if (spec.adult()) {
            Talent[] all = prog().table();
            for (int i = 0; i < all.length; i++)
                if (in(vx, vy, nodeX[i] - NS / 2, nodeY[i] - NS / 2, NS, NS)) {
                    g.renderTooltip(this.font, Component.literal(all[i].name + "  " + prog().rank(i) + "/" + all[i].maxRank), rmx, rmy);
                    break;
                }
        }
    }
}
