package com.prodigytown.creatures.client;

import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.PetOrders;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.net.ModNetwork;
import com.prodigytown.creatures.progress.ProdigyProgress;
import com.prodigytown.creatures.progress.Talent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Pantalla del carnotauro (tecla K), con el arte de los mockups del usuario: tres paginas (Comandos, Ataques, Talentos) sobre fondos limpios
 * (textures/gui/carno/bg_*.png, 1086x814) y todo lo variable dibujado en vivo. Coordenadas "virtuales" de 724x543 (el tamaño del mockup a mitad
 * de escala) que se reducen con sc para que quepa siempre en pantalla. Solo cliente; el servidor valida cada accion.
 */
public final class CarnoScreen extends Screen {
    private static final int W = 724, H = 543;
    private static final String MOD = ProdigyCreatures.MODID;
    private static final ResourceLocation[] BG = {
        new ResourceLocation(MOD, "textures/gui/carno/bg_cmd.png"),
        new ResourceLocation(MOD, "textures/gui/carno/bg_atk.png"),
        new ResourceLocation(MOD, "textures/gui/carno/bg_tal.png")};
    private static final ResourceLocation[] ICON = {
        new ResourceLocation(MOD, "textures/gui/carno/icon_bite.png"), new ResourceLocation(MOD, "textures/gui/carno/icon_charge.png"),
        new ResourceLocation(MOD, "textures/gui/carno/icon_roar.png"), new ResourceLocation(MOD, "textures/gui/carno/icon_butt.png"),
        new ResourceLocation(MOD, "textures/gui/carno/icon_eye.png")};
    private static final ResourceLocation ATLAS = TexButton.TEX;

    // ---- zonas por pagina (coordenadas virtuales)
    /** Pestañas inferiores de cada pagina: {x0, y0, x1, y1} de Comandos, Ataques y Talentos. */
    private static final int[][][] TABS = {
        {{110, 464, 277, 502}, {283, 464, 441, 502}, {455, 464, 617, 502}},
        {{115, 459, 275, 494}, {284, 459, 449, 494}, {459, 459, 616, 494}},
        {{105, 486, 275, 514}, {286, 486, 439, 514}, {449, 486, 619, 514}}};
    private static final int[][] CLOSE = {{669, 43, 704, 74}, {681, 45, 711, 74}, {673, 39, 703, 67}};
    private static final int[][] PORTRAIT = {{47, 113, 285, 291}, {47, 111, 271, 272}, {28, 98, 184, 214}};
    private static final int[][] TITLE = {{116, 60, 310, 92}, {122, 61, 312, 92}, {100, 56, 247, 80}};
    private static final int[][] HP = {{75, 333, 285, 355}, {74, 317, 269, 337}, {0, 0, 0, 0}};
    private static final float[][] LEVEL_C = {{165, 313}, {153, 298}, {108, 256}};
    private static final float[][] STAT_Y = {{375, 395.7F, 416, 436}, {356, 376, 395, 414}, {307, 323, 338, 354}};
    private static final int[] STAT_X = {272, 262, 183};
    private static final int[] ORDER_OF_CARD = {PetOrders.STAY, PetOrders.WANDER, PetOrders.FOLLOW, PetOrders.PATROL, PetOrders.SIT, PetOrders.SLEEP};
    private static final float[][] CARD_CMD = {{307, 108}, {437.5F, 108}, {568, 108}, {307, 220}, {437.5F, 220}, {568, 220}};
    private static final float[][] CARD_ATK = {{286, 109}, {446, 109}, {286, 199.5F}, {446, 199.5F}};

    private static final int GOLD = 0xFFFFD35A, CREAM = 0xFFF6E3A1, TEXT = 0xFFEDEDED, DIM = 0xFF9AA0A6;
    private static final int[] BRANCH_COLOR = {0xFFE8602C, 0xFF3FB4FF, 0xFF7CE03C};
    private static final String[] VARIANT = {"Rojo", "Verde", "Acero", "Arena"};

    private final CarnotaurusEntity pet;
    private int tab = 0, selCmd = -1, selAtk = 0, selected = 0;
    private boolean ultSel = false, renaming = false;
    private float sc = 1.0F;
    private int left, top;
    private EditBox nameBox;
    private long resetConfirmUntil = 0L;

    public CarnoScreen(CarnotaurusEntity pet) {
        super(Component.literal("Carnotauro"));
        this.pet = pet;
    }

    private ProdigyProgress prog() { return pet.getProgress(); }

    /** Boton invisible sobre una zona del fondo: solo se ilumina al pasar el raton. */
    private static final class Hit extends Button {
        Hit(int x, int y, int w, int h, OnPress press) { super(x, y, w, h, Component.empty(), press, DEFAULT_NARRATION); }
        /** Zona dada por sus esquinas (x0, y0, x1, y1). */
        static Hit box(float x0, float y0, float x1, float y1, OnPress press) { return new Hit((int) x0, (int) y0, (int) (x1 - x0), (int) (y1 - y0), press); }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            if (this.active && this.isHoveredOrFocused()) g.fill(getX(), getY(), getX() + width, getY() + height, 0x22FFFFFF);
        }
    }

    /** Boton con el icono de un objeto (quitar silla / armadura). */
    private final class GearButton extends Button {
        final boolean armor;
        GearButton(int x, int y, boolean armor, OnPress press) {
            super(x, y, 22, 22, Component.empty(), press, DEFAULT_NARRATION);
            this.armor = armor;
        }
        @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int x = getX(), y = getY();
            boolean on = this.active;
            g.fill(x, y, x + width, y + height, on ? 0xCC10161C : 0x8810161C);
            g.renderOutline(x, y, width, height, on ? (isHoveredOrFocused() ? GOLD : 0xFFB8862A) : 0xFF4A4F54);
            Item it = armor ? new Item[]{Items.LEATHER_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE}[Math.max(0, Math.min(4, pet.getArmorTier()))] : Items.SADDLE;
            g.pose().pushPose();
            g.pose().translate(x + 3.0F, y + 3.0F, 0.0F);
            g.pose().scale(1.0F, 1.0F, 1.0F);
            if (!on) g.setColor(1.0F, 1.0F, 1.0F, 0.35F);
            g.renderItem(new ItemStack(it), 0, 0);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.pose().popPose();
            if (on) { // x roja: se quita al pulsar
                g.fill(x + width - 7, y + 1, x + width - 1, y + 7, 0xFFB02A22);
            }
        }
    }

    // ---------------------------------------------------------------- inicio
    @Override protected void init() {
        sc = Math.max(0.4F, Math.min(1.35F, Math.min((this.width - 8.0F) / W, (this.height - 8.0F) / H)));
        left = (int) ((this.width / sc - W) / 2.0F);
        top = (int) ((this.height / sc - H) / 2.0F);
        int[] cl = CLOSE[tab];
        addRenderableWidget(Hit.box(left + cl[0], top + cl[1], left + cl[2], top + cl[3], b -> this.onClose()));
        for (int i = 0; i < 3; i++) {
            if (i == tab) continue;
            final int t = i; int[] r = TABS[tab][i];
            addRenderableWidget(Hit.box(left + r[0], top + r[1], left + r[2], top + r[3], b -> { tab = t; renaming = false; rebuildWidgets(); }));
        }
        int[] tt = TITLE[tab];
        nameBox = new EditBox(this.font, left + tt[0] + 4, top + tt[1] + 6, tt[2] - tt[0] - 8, 18, Component.literal("Nombre"));
        nameBox.setMaxLength(32);
        nameBox.setValue(pet.hasCustomName() ? pet.getCustomName().getString() : "");
        nameBox.setHint(Component.literal("Nombre del carnotauro"));
        nameBox.visible = renaming; nameBox.active = renaming;
        addRenderableWidget(Hit.box(left + tt[0], top + tt[1], left + tt[2], top + tt[3], b -> { renaming = true; nameBox.visible = true; nameBox.active = true; setFocused(nameBox); nameBox.setFocused(true); }));
        addRenderableWidget(nameBox);
        int[] pr = PORTRAIT[tab];
        addRenderableWidget(new GearButton(left + pr[0] + 6, top + pr[3] - 28, false, b -> ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 3, 0))));
        addRenderableWidget(new GearButton(left + pr[2] - 28, top + pr[3] - 28, true, b -> ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 4, 0))));
        switch (tab) {
            case 0 -> {
                for (int i = 0; i < 6; i++) {
                    final int k = i;
                    float[] c = CARD_CMD[i];
                    addRenderableWidget(Hit.box(left + c[0], top + c[1], left + c[0] + 124, top + c[1] + 106, b -> {
                        selCmd = k;
                        ModNetwork.CHANNEL.sendToServer(new ModNetwork.SetOrder(pet.getId(), ORDER_OF_CARD[k]));
                    }));
                }
            }
            case 1 -> {
                for (int i = 0; i < 4; i++) {
                    final int k = i;
                    float[] c = CARD_ATK[i];
                    addRenderableWidget(Hit.box(left + c[0], top + c[1], left + c[0] + 152, top + c[1] + 84, b -> selAtk = k));
                }
                addRenderableWidget(Hit.box(left + 606, top + 109, left + 698, top + 284, b -> selAtk = 4));
            }
            default -> {
                addRenderableWidget(Hit.box(left + 537, top + 380, left + 693, top + 411, b -> {
                    if (!ultSel && pet.isAdultStage()) ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 0, selected));
                }));
                addRenderableWidget(Hit.box(left + 48, top + 421, left + 170, top + 442, b -> {
                    long now = System.currentTimeMillis();
                    if (now < resetConfirmUntil) { ModNetwork.CHANNEL.sendToServer(new ModNetwork.TalentAction(pet.getId(), 1, 0)); resetConfirmUntil = 0L; }
                    else resetConfirmUntil = now + 3000L;
                }));
                addRenderableWidget(Hit.box(left + 322, top + 352, left + 408, top + 420, b -> ultSel = true));
            }
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void tick() {
        super.tick();
        if (!pet.isAlive() || pet.isRemoved()) this.onClose();
    }

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
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        mx /= sc; my /= sc;
        if (tab == 2 && button == 0 && pet.isAdultStage()) {
            for (int i = 0; i < prog().table().length; i++) {
                int[] n = nodeBox(i);
                if (mx >= left + n[0] && mx < left + n[0] + NS && my >= top + n[1] && my < top + n[1] + NS) { selected = i; ultSel = false; return true; }
            }
        }
        return super.mouseClicked(mx, my, button);
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
    /** Parrafo con ajuste de linea; devuelve la y siguiente. */
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
    private void frame(GuiGraphics g, float x0, float y0, float x1, float y1, int color, int glow) {
        int a = (int) x0, b = (int) y0, c = (int) x1, d = (int) y1;
        g.fill(a, b, c, d, glow);
        g.renderOutline(a, b, c - a, d - b, color);
        g.renderOutline(a + 1, b + 1, c - a - 2, d - b - 2, (color & 0x00FFFFFF) | 0x66000000);
    }
    private static String sec(double ticks) {
        double s = ticks / 20.0;
        return (s == Math.rint(s) ? String.valueOf((int) s) : String.format("%.1f", s).replace('.', ',')) + " s";
    }

    // ---------------------------------------------------------------- dibujo
    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int rmx = mx, rmy = my;
        mx = (int) (mx / sc); my = (int) (my / sc);
        g.pose().pushPose();
        g.pose().scale(sc, sc, 1.0F);
        g.blit(BG[tab], left, top, W, H, 0.0F, 0.0F, 1086, 814, 1086, 814);
        renderLeft(g, mx, my);
        switch (tab) {
            case 0 -> renderCommands(g);
            case 1 -> renderAttacks(g);
            default -> renderTalents(g);
        }
        boolean adult = pet.isAdultStage();
        if (!adult && tab > 0) {
            int x0 = tab == 1 ? 284 : 202, y0 = tab == 1 ? 106 : 128, x1 = tab == 1 ? 700 : 698, y1 = tab == 1 ? 432 : 458;
            g.fill(left + x0, top + y0, left + x1, top + y1, 0xF0101418);
            g.renderOutline(left + x0, top + y0, x1 - x0, y1 - y0, 0xFFB8862A);
            textC(g, tab == 1 ? "Sus ataques se despiertan al crecer" : "Los talentos se desbloquean al crecer", left + (x0 + x1) / 2.0F, top + (y0 + y1) / 2.0F - 10, 1.5F, GOLD, true);
            textC(g, "Alimentalo con carne cruda hasta que sea adulto.", left + (x0 + x1) / 2.0F, top + (y0 + y1) / 2.0F + 10, 1.1F, TEXT, false);
        }
        super.render(g, mx, my, pt);
        // tooltips de los botones de equipo
        int[] pr = PORTRAIT[tab];
        if (my >= top + pr[3] - 28 && my < top + pr[3] - 6) {
            if (mx >= left + pr[0] + 6 && mx < left + pr[0] + 28) g.renderTooltip(this.font, Component.literal(pet.isSaddled() ? "Quitar silla" : "Sin silla (clic con una silla en la mano)"), rmx, rmy);
            else if (mx >= left + pr[2] - 28 && mx < left + pr[2] - 6) g.renderTooltip(this.font, Component.literal(pet.getArmorTier() > 0 ? "Quitar armadura" : "Sin armadura (clic con una en la mano)"), rmx, rmy);
        }
        int[] tt = TITLE[tab];
        if (!renaming && mx >= left + tt[0] && mx < left + tt[2] && my >= top + tt[1] && my < top + tt[3]) g.renderTooltip(this.font, Component.literal("Clic para ponerle nombre"), rmx, rmy);
        g.pose().popPose();
        if (tab == 2 && adult) renderTalentTooltip(g, mx, my, rmx, rmy);
    }

    /** Panel izquierdo: nombre, retrato con el carnotauro real, nivel, vida y estadisticas (y XP / puntos en la pagina de talentos). */
    private void renderLeft(GuiGraphics g, int mx, int my) {
        ProdigyProgress p = prog();
        int[] tt = TITLE[tab];
        if (!renaming) {
            String nm = pet.hasCustomName() ? pet.getCustomName().getString() : "Carnotaurus";
            float scale = tab == 2 ? 1.5F : 2.0F;
            nm = shorten(nm, (tt[2] - tt[0] - 14) / 1.0F, scale);
            float cx = left + (tt[0] + tt[2]) / 2.0F;
            textC(g, nm, cx + 1.0F, top + (tt[1] + tt[3]) / 2.0F - 4.0F * scale + 1.0F, scale, 0xFF2A1A08, false);
            textC(g, nm, cx, top + (tt[1] + tt[3]) / 2.0F - 4.0F * scale, scale, CREAM, false);
        }
        // retrato
        int[] pr = PORTRAIT[tab];
        int x0 = left + pr[0], y0 = top + pr[1], x1 = left + pr[2], y1 = top + pr[3];
        g.enableScissor((int) (x0 * sc), (int) (y0 * sc), (int) (x1 * sc), (int) (y1 * sc));
        boolean ad = pet.isAdultStage();
        float k = (pr[2] - pr[0]) / 238.0F;
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, (x0 + x1) / 2, y1 - (int) (14 * k) - 4, (int) ((ad ? 24.0F : 56.0F) * k),
            (float) ((x0 + x1) / 2 - mx), (float) (y0 + (y1 - y0) / 3 - my), pet);
        g.disableScissor();
        // nivel
        textC(g, "Nv. " + p.level(), left + LEVEL_C[tab][0], top + LEVEL_C[tab][1] - 4.0F * 1.5F, 1.5F, CREAM, true);
        // vida
        float hp = Math.max(0.0F, pet.getHealth()), max = Math.max(1.0F, pet.getMaxHealth());
        if (tab < 2) {
            int[] h = HP[tab];
            int bx = left + h[0], by = top + h[1], bw = h[2] - h[0], bh = h[3] - h[1];
            int w = Math.round(bw * Math.min(1.0F, hp / max));
            boolean weak = hp / max < PetOrders.WEAK_FRACTION;
            if (w > 0) {
                g.fillGradient(bx, by, bx + w, by + bh, weak ? 0xFFE8483C : 0xFF4CD83A, weak ? 0xFF9A1E16 : 0xFF1E8C14);
                g.fill(bx, by, bx + w, by + 2, 0x55FFFFFF);
            }
            textC(g, Math.round(hp) + " / " + Math.round(max), bx + bw / 2.0F, by + bh / 2.0F - 4.5F * 1.3F, 1.3F, 0xFFFFFFFF, true);
        }
        // estadisticas
        String[] v = {String.valueOf(Math.round(max)), String.valueOf((int) pet.getAttributeValue(Attributes.ATTACK_DAMAGE)),
            String.valueOf((int) pet.getAttributeValue(Attributes.ARMOR)), String.format("%.2f", pet.getAttributeValue(Attributes.MOVEMENT_SPEED))};
        for (int i = 0; i < 4; i++) textR(g, v[i], left + STAT_X[tab], top + STAT_Y[tab][i] - 5.0F, tab == 2 ? 1.0F : 1.25F, 0xFFFFFFFF, true);
        if (tab == 2) {
            textC(g, "Variante: " + VARIANT[Math.max(0, Math.min(3, pet.getVariant()))] + (pet.isFemale() ? " (hembra)" : " (macho)"), left + 104, top + 229, 1.0F, 0xFF9BE08A, false);
            int bx = left + 34, by = top + 276, bw = 149, bh = 11;
            int need = p.xpToNext();
            int fill = need <= 0 ? bw : Math.min(bw, (int) (bw * (p.xp() / (float) Math.max(1, need))));
            if (fill > 0) { g.fillGradient(bx, by, bx + fill, by + bh, 0xFFB060FF, 0xFF6A2CC0); g.fill(bx, by, bx + fill, by + 1, 0x66FFFFFF); }
            textC(g, need <= 0 ? "Nivel maximo" : "EXP " + p.xp() + " / " + need, bx + bw / 2.0F, by + 1.0F, 0.95F, 0xFFFFFFFF, true);
            String pts = String.valueOf(p.pointsAvailable());
            textC(g, pts, left + 124, top + 393, 2.0F, 0xFFFFFFFF, true);
            if (System.currentTimeMillis() < resetConfirmUntil) {
                g.fill(left + 48, top + 421, left + 170, top + 442, 0xCC3A1010);
                g.renderOutline(left + 48, top + 421, 122, 21, 0xFFE0453A);
                textC(g, "Seguro? Clic otra vez", left + 109, top + 428, 1.0F, 0xFFFFE0D0, true);
            }
        }
    }

    // ---------------------------------------------------------------- comandos
    private void renderCommands(GuiGraphics g) {
        boolean fallen = pet.isFallen();
        int cur = pet.getOrder();
        for (int i = 0; i < 6; i++) {
            float[] c = CARD_CMD[i];
            int order = ORDER_OF_CARD[i];
            boolean sel = !fallen && order == cur;
            float x0 = left + c[0], y0 = top + c[1];
            String nm = PetOrders.NAMES[order];
            float cy = top + (i < 3 ? 196.5F : 305.0F); // etiqueta en la placa inferior de cada carta
            if (fallen) g.fill((int) x0 + 3, (int) y0 + 3, (int) (x0 + 121), (int) (y0 + 103), 0x99000000);
            if (sel) {
                frame(g, x0 - 1, y0 - 1, x0 + 125, y0 + 107, GOLD, 0x24FFD35A);
                g.fill((int) (x0 + 29), (int) (cy - 7), (int) (x0 + 99), (int) (cy + 9), 0x30FFD35A);
            }
            textC(g, nm, x0 + 64.0F, cy - 4.8F, 1.2F, sel ? GOLD : fallen ? 0xFF7A7F85 : 0xFFFFFFFF, true);
        }
        // descripcion
        int show = fallen ? -1 : cur;
        String title = fallen ? "Fuera de combate" : PetOrders.NAMES[show];
        text(g, title, left + 346, top + 351, 1.5F, fallen ? 0xFFE0453A : GOLD, true);
        String body = fallen ? "Dormido: no recibe ordenes. Reanimalo con la ensalada de domesticacion (clic derecho): se recupera con 20 de vida."
            : PetOrders.HELP[show];
        para(g, body, left + 332, top + 381, 268, 1.2F, 12.0F, TEXT, 4);
    }

    // ---------------------------------------------------------------- ataques
    private record Atk(String[] name, String pill, String title, String desc, String kind) {}
    private static final Atk[] ATK = {
        new Atk(new String[]{"Mordida", "feroz"}, "Daño", "Mordida feroz",
            "Cierra sus mandíbulas sobre el enemigo que tiene delante. Es su arma más rápida: ataca sin parar.", "bite"),
        new Atk(new String[]{"Embestida", "brutal"}, "Control", "Embestida brutal",
            "Se lanza hacia adelante, golpea a todo el que toca y lo empuja con fuerza. Con la Embestida de cacería también lo deja lento.", "charge"),
        new Atk(new String[]{"Rugido", "aterrador"}, "Debilita", "Rugido aterrador",
            "Ruge con todo el cuerpo: sus aliados cercanos ganan Fuerza y los enemigos a su alrededor quedan lentos y débiles. Desde el nivel 50 es el Rugido ancestral.", "roar"),
        new Atk(new String[]{"Cabezazo", "demoledor"}, "Impacto", "Cabezazo demoledor",
            "Echa la cabeza atrás y embiste con los cuernos: daño fuerte, retroceso y los enemigos salen disparados hacia arriba.", "butt"),
        new Atk(new String[]{"Instinto", "depredador"}, "Pasiva", "Instinto depredador",
            "Ataca a todo lo que ve, corre más rápido al perseguir una presa y se enfurece cuando le queda poca vida.", "passive")};

    private float cdFactor() { return 1.0F - 0.10F * prog().rank("C7"); }

    private String[] atkStats(int a) {
        ProdigyProgress p = prog();
        float dmg = (float) pet.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float f = cdFactor();
        switch (a) {
            case 0 -> { return new String[]{"Daño: " + Math.round(dmg), "Recarga: " + sec(Math.max(2, Math.round(12 * f))), "Tecla: clic izq."}; }
            case 1 -> { return new String[]{"Daño: " + Math.round(dmg * (1.4F + 0.2F * p.rank("C3"))), "Recarga: " + sec(Math.round(160 * f)),
                "Tecla: " + ClientKeys.CHARGE.getTranslatedKeyMessage().getString()}; }
            case 2 -> {
                int ul = pet.ultimateLevel();
                int cd = ul > 0 ? 1200 - 20 * new int[]{0, 8, 14, 20}[p.rank("U3")] : 500;
                return new String[]{"Alcance: 12 m", "Recarga: " + sec(Math.round(cd * f)), "Tecla: " + ClientKeys.ULTIMATE.getTranslatedKeyMessage().getString()};
            }
            case 3 -> { return new String[]{"Daño: " + Math.round(dmg * (1.3F + 0.5F * p.rank("C5"))), "Recarga: " + sec(Math.round(70 * (1.0F - 0.10F * p.rank("C5")) * f)),
                "Tecla: " + ClientKeys.TAIL.getTranslatedKeyMessage().getString()}; }
            default -> { return new String[]{"Efecto: siempre activo", "Recarga: ninguna", "Tecla: ninguna"}; }
        }
    }

    private void renderAttacks(GuiGraphics g) {
        for (int i = 0; i < 4; i++) {
            float[] c = CARD_ATK[i];
            boolean sel = selAtk == i;
            float x0 = left + c[0], y0 = top + c[1];
            if (sel) frame(g, x0 - 1, y0 - 1, x0 + 153, y0 + 85, GOLD, 0x22FFD35A);
            text(g, ATK[i].name[0], x0 + 76, y0 + 14, 1.25F, sel ? GOLD : 0xFFFFFFFF, true);
            text(g, ATK[i].name[1], x0 + 76, y0 + 28, 1.25F, sel ? GOLD : 0xFFFFFFFF, true);
            text(g, ATK[i].pill, x0 + 105, y0 + 59, 1.15F, 0xFFFFFFFF, true);
        }
        if (selAtk == 4) frame(g, left + 604, top + 107, left + 700, top + 286, GOLD, 0x22FFD35A);
        Atk a = ATK[selAtk];
        // icono grande + titulo
        g.blit(ICON[selAtk], left + 311, top + 306, 46, 46, 0.0F, 0.0F, 128, 128, 128, 128);
        text(g, a.title, left + 368, top + 314, 1.4F, GOLD, true);
        para(g, a.desc, left + 360, top + 342, 310, 1.15F, 11.5F, TEXT, 4);
        String[] st = atkStats(selAtk);
        Item[] icons = {selAtk == 2 ? Items.GOLDEN_APPLE : Items.IRON_SWORD, Items.CLOCK, Items.TRIPWIRE_HOOK};
        int[][] pill = {{303, 405}, {417, 550}, {562, 680}};
        for (int i = 0; i < 3; i++) {
            int px = left + pill[i][0], pw = pill[i][1] - pill[i][0];
            g.pose().pushPose(); g.pose().translate(px + 6.0F, top + 394.0F, 0.0F); g.pose().scale(0.95F, 0.95F, 1.0F);
            g.renderItem(new ItemStack(icons[i]), 0, 0); g.pose().popPose();
            text(g, shorten(st[i], pw - 30, 1.15F), px + 25, top + 398, 1.15F, 0xFFFFFFFF, true);
        }
    }

    // ---------------------------------------------------------------- talentos
    private static final int NS = 28;

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

    /** Esquina superior izquierda (virtual) del nodo i. */
    private int[] nodeBox(int i) {
        Talent t = prog().table()[i];
        if (t.id.startsWith("U")) {
            int k = t.id.charAt(1) - '1';
            return new int[]{318 + k * 46 - NS / 2, 332 - NS / 2};
        }
        int[] sl = slot(t.id);
        int cx = new int[]{255, 364, 472}[Math.max(0, Math.min(2, t.branch))];
        return new int[]{cx + sl[0] * 34 - NS / 2, 133 + sl[1] * 35};
    }

    private static Item icon(String id) {
        return switch (id) {
            case "D1" -> Items.IRON_CHESTPLATE; case "D2" -> Items.GOLDEN_APPLE; case "D3" -> Items.ANVIL;
            case "D4" -> Items.TOTEM_OF_UNDYING; case "D5" -> Items.SHIELD;
            case "C1" -> Items.BONE; case "C2" -> Items.DIAMOND_AXE; case "C3" -> Items.FIRE_CHARGE;
            case "C4" -> Items.CACTUS; case "C5" -> Items.TNT; case "C6" -> Items.BLAZE_POWDER; case "C7" -> Items.NETHER_STAR;
            case "S1" -> Items.SUGAR; case "S2" -> Items.GLISTERING_MELON_SLICE; case "S3" -> Items.LEAD;
            case "S4" -> Items.SLIME_BALL; case "S5" -> Items.RED_BED; case "S6" -> Items.GOAT_HORN; case "S7" -> Items.BEACON;
            case "U1" -> Items.NETHER_STAR; case "U2" -> Items.ENDER_EYE; case "U3" -> Items.CLOCK;
            default -> Items.PAPER;
        };
    }

    private void node(GuiGraphics g, int x, int y, int color, boolean owned, boolean maxed, boolean can) {
        int bg = maxed ? 0xFF4A3A10 : owned ? 0xFF1E2A1E : 0xFF14181C;
        int edge = maxed ? GOLD : owned || can ? color : 0xFF4A4F54;
        if (can || owned) { g.fill(x - 2, y - 2, x + NS + 2, y + NS + 2, (edge & 0x00FFFFFF) | 0x30000000); }
        g.fill(x, y, x + NS, y + NS, bg);
        g.fill(x, y, x + 3, y + 3, 0xFF000000); g.fill(x + NS - 3, y, x + NS, y + 3, 0xFF000000);
        g.fill(x, y + NS - 3, x + 3, y + NS, 0xFF000000); g.fill(x + NS - 3, y + NS - 3, x + NS, y + NS, 0xFF000000);
        g.renderOutline(x, y, NS, NS, edge);
        g.renderOutline(x + 1, y + 1, NS - 2, NS - 2, (edge & 0x00FFFFFF) | 0x55000000);
    }

    private void renderTalents(GuiGraphics g) {
        if (!pet.isAdultStage()) return;
        ProdigyProgress p = prog();
        Talent[] all = p.table();
        // lineas de requisito (debajo de los nodos)
        for (int i = 0; i < all.length; i++) {
            for (String[] req : all[i].reqs) {
                int r = -1;
                for (int k = 0; k < all.length; k++) if (all[k].id.equals(req[0])) r = k;
                if (r < 0) continue;
                int[] a = nodeBox(r), b = nodeBox(i);
                int x1 = left + a[0] + NS / 2, y1 = top + a[1] + NS - 1, x2 = left + b[0] + NS / 2, y2 = top + b[1] + 1;
                boolean met = p.rank(r) >= Integer.parseInt(req[1]);
                int col = met ? BRANCH_COLOR[all[i].branch] : 0xFF3A4046;
                int ym = (y1 + y2) / 2;
                g.fill(x1 - 1, y1, x1 + 1, ym + 1, col);
                g.fill(Math.min(x1, x2) - 1, ym, Math.max(x1, x2) + 1, ym + 2, col);
                g.fill(x2 - 1, ym, x2 + 1, y2, col);
            }
        }
        for (int i = 0; i < all.length; i++) {
            Talent t = all[i];
            int[] n = nodeBox(i);
            int x = left + n[0], y = top + n[1];
            boolean owned = p.rank(i) > 0, maxed = p.rank(i) >= t.maxRank, can = p.whyNotUnlock(i) == null;
            int color = BRANCH_COLOR[Math.max(0, Math.min(2, t.branch))];
            node(g, x, y, color, owned, maxed, can);
            g.pose().pushPose();
            g.pose().translate(x + (NS - 16) / 2.0F, y + (NS - 16) / 2.0F - 1.0F, 0.0F);
            g.renderItem(new ItemStack(icon(t.id)), 0, 0);
            g.pose().popPose();
            if (!owned && !can) {
                g.fill(x + 2, y + 2, x + NS - 2, y + NS - 2, 0xAA000000);
                g.fill(x + NS / 2 - 3, y + NS / 2 - 1, x + NS / 2 + 3, y + NS / 2 + 4, 0xFFA0A4A8);          // candado
                g.fill(x + NS / 2 - 2, y + NS / 2 - 4, x + NS / 2 + 2, y + NS / 2 - 1, 0xFF70747A);
            }
            if (i == selected && !ultSel) g.renderOutline(x - 3, y - 3, NS + 6, NS + 6, 0xFFFFFFFF);
            String rk = p.rank(i) + "/" + t.maxRank;
            text(g, rk, x + NS - this.font.width(rk) * 0.7F, y + NS - 6.5F, 0.7F, owned ? (maxed ? GOLD : TEXT) : DIM, true);
        }
        // medallon de la definitiva (rugido ancestral)
        int ul = pet.ultimateLevel();
        if (ultSel) g.renderOutline(left + 320, top + 352, 90, 68, GOLD);
        textC(g, "Rugido ancestral", left + 362, top + 418.5F, 1.0F, GOLD, true);
        textC(g, ul > 0 ? "Definitiva nivel " + ul + " / 3" : "Se obtiene al nivel 50", left + 362, top + 437.5F, 0.85F, ul > 0 ? 0xFFE6D6FF : 0xFFC8CDD2, false);
        textC(g, "Mejoras de la definitiva", left + 364, top + 349.5F, 0.8F, 0xFFC8CDD2, true);
        // progreso del arbol
        int total = all.length, got = p.talentsUnlocked();
        int bx = left + 288, by = top + 459, bw = 167, bh = 9;
        int f = Math.min(bw, bw * got / Math.max(1, total));
        if (f > 0) { g.fillGradient(bx, by, bx + f, by + bh, 0xFFFFD35A, 0xFFE08A1C); g.fill(bx, by, bx + f, by + 1, 0x66FFFFFF); }
        text(g, got + " / " + total + " talentos", left + 470, top + 459.5F, 0.95F, 0xFFFFFFFF, true);
        renderDetail(g, p);
    }

    private int req(GuiGraphics g, float x, float y, boolean ok, String s) {
        int ix = (int) x, iy = (int) y;
        if (ok) g.blit(ATLAS, ix, iy, 9, 7, 56.0F, 30.0F, 10, 7, 256, 256); else g.blit(ATLAS, ix + 1, iy, 7, 7, 68.0F, 30.0F, 8, 7, 256, 256);
        text(g, shorten(s, 140, 1.0F), x + 13, y, 1.0F, ok ? 0xFF7CE08A : 0xFFE07C7C, false);
        return (int) y + 11;
    }

    private void renderDetail(GuiGraphics g, ProdigyProgress p) {
        int x = left + 541, y = top + 96;
        if (ultSel) {
            frame(g, x, y, x + 44, y + 44, 0xFFB060FF, 0x80100A1C);
            g.pose().pushPose(); g.pose().translate(x + 6.0F, y + 6.0F, 0.0F); g.pose().scale(2.0F, 2.0F, 1.0F);
            g.renderItem(new ItemStack(Items.NETHER_STAR), 0, 0); g.pose().popPose();
            para(g, "Rugido ancestral", x + 52, y + 4, 100, 1.1F, 10.5F, GOLD, 2);
            frame(g, x + 52, y + 32, x + 118, y + 46, 0xFFB060FF, 0x40100A1C);
            textC(g, "Definitiva", x + 85, y + 36, 0.95F, 0xFFE6D6FF, false);
            para(g, ul(0), x, y + 62, 150, 1.0F, 10.0F, TEXT, 6);
            int yy = top + 262;
            text(g, "Requisitos", x, yy, 1.1F, GOLD, true); yy += 14;
            yy = req(g, x, yy, p.level() >= 50, "Nivel 50: definitiva I");
            yy = req(g, x, yy, p.level() >= 75, "Nivel 75: definitiva II");
            yy = req(g, x, yy, p.level() >= 100, "Nivel 100: definitiva III");
            text(g, "Las mejoras U1-U3 la hacen más fuerte.", x, yy + 4, 0.85F, DIM, false);
            button(g, p.level() >= 50 ? "Desbloqueada" : "Por nivel", p.level() >= 50, false);
            return;
        }
        Talent t = p.table()[selected];
        int col = BRANCH_COLOR[Math.max(0, Math.min(2, t.branch))];
        int cur = p.rank(selected);
        frame(g, x, y, x + 44, y + 44, cur > 0 ? col : 0xFF5A6066, 0x80101418);
        g.pose().pushPose(); g.pose().translate(x + 6.0F, y + 6.0F, 0.0F); g.pose().scale(2.0F, 2.0F, 1.0F);
        g.renderItem(new ItemStack(icon(t.id)), 0, 0); g.pose().popPose();
        para(g, t.name, x + 52, y + 2, 100, 1.1F, 10.5F, 0xFFFFF0C0, 3);
        frame(g, x + 52, y + 32, x + 118, y + 46, col, 0x40101418);
        textC(g, "Rango " + cur + " / " + t.maxRank, x + 85, y + 36, 0.95F, 0xFFFFFFFF, false);
        float yy = para(g, t.effect, x, y + 62, 150, 1.0F, 10.0F, TEXT, 9);
        String note = switch (t.id) {
            case "S3" -> "Cruzar portales: pendiente.";
            case "D5" -> "Efecto solo montado.";
            default -> null;
        };
        if (note != null) para(g, note, x, yy + 2, 150, 0.9F, 9.5F, 0xFFE0B04C, 2);
        int ry = top + 262;
        text(g, "Requisitos", x, ry, 1.1F, GOLD, true); ry += 14;
        int next = Math.min(cur, t.maxRank - 1);
        ry = req(g, x, ry, p.level() >= t.minLevel[next], "Nivel " + t.minLevel[next]);
        for (String[] r : t.reqs) {
            int need = Integer.parseInt(r[1]);
            String nm = r[0];
            for (Talent o : p.table()) if (o.id.equals(r[0])) nm = o.name;
            ry = req(g, x, ry, p.rank(r[0]) >= need, nm + " " + need);
        }
        ry = req(g, x, ry, cur >= t.maxRank || p.pointsAvailable() >= t.cost[next], "Coste: " + t.cost[next] + " pts");
        for (String ex : t.exclusive) {
            String nm = ex;
            for (Talent o : p.table()) if (o.id.equals(ex)) nm = o.name;
            ry = req(g, x, ry, p.rank(ex) == 0, "Excluye " + nm);
        }
        boolean can = p.whyNotUnlock(selected) == null;
        button(g, cur >= t.maxRank ? "Máximo" : "Desbloquear", can, cur >= t.maxRank);
    }

    private static String ul(int unused) { return "Se obtiene por nivel: I a nivel 50, II a nivel 75 y III a nivel 100. Solo montado o cerca, con la tecla de la definitiva."; }

    /** Texto sobre el boton verde del fondo; atenuado cuando no se puede pulsar. */
    private void button(GuiGraphics g, String label, boolean enabled, boolean maxed) {
        int x0 = left + 537, y0 = top + 380, x1 = left + 693, y1 = top + 411;
        if (!enabled) g.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, maxed ? 0x9A3A3010 : 0xC0101418);
        textC(g, label, (x0 + x1) / 2.0F, y0 + 11.0F, 1.5F, enabled ? 0xFFFFFFFF : maxed ? GOLD : 0xFF8A9096, true);
    }

    private void renderTalentTooltip(GuiGraphics g, int mx, int my, int rmx, int rmy) {
        Talent[] all = prog().table();
        for (int i = 0; i < all.length; i++) {
            int[] n = nodeBox(i);
            if (mx >= left + n[0] && mx < left + n[0] + NS && my >= top + n[1] && my < top + n[1] + NS) {
                g.renderTooltip(this.font, Component.literal(all[i].name + "  " + prog().rank(i) + "/" + all[i].maxRank), rmx, rmy);
                return;
            }
        }
    }
}
