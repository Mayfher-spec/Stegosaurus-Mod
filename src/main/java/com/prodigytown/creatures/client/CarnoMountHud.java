package com.prodigytown.creatures.client;

import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** HUD al montar el carnotauro: nombre, nivel, vida y recargas de embestida (V), cabezazo (B / clic derecho), mordida (clic izquierdo) y rugido (R). */
public final class CarnoMountHud {
    private CarnoMountHud() {}

    /** Color de acento por variante: rojo, verde, acero, arena. */
    private static final int[] ACCENT = {0xFFD9442E, 0xFF5FB35A, 0xFF8FA6B8, 0xFFD9B56A};
    private static final String[] BTN = {"charge", "butt", "bite", "roar"};
    private static final ResourceLocation[] FRAME_BTN = new ResourceLocation[4];
    private static final ResourceLocation[][] ICON = new ResourceLocation[4][4];
    private static final ResourceLocation[] FRAME_HP = new ResourceLocation[4];
    static {
        for (int v = 0; v < 4; v++) {
            FRAME_HP[v] = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/carno/hud_hp_" + v + ".png");
            for (int b = 0; b < 4; b++) ICON[b][v] = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/carno/hud_ic_" + BTN[b] + "_" + v + ".png");
        }
        for (int b = 0; b < 4; b++) FRAME_BTN[b] = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/carno/hud_btn_" + BTN[b] + ".png");
    }

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class CarnoModBus {
        @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "carno_mount_hud", (gui, g, partial, w, h) -> render(g, w, h));
        }
    }

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, value = Dist.CLIENT)
    public static final class CarnoForgeBus {
        /** Se oculta la barra de corazones de montura de vanilla (220 PV serian 30 corazones): la reemplaza este HUD. */
        @SubscribeEvent public static void hideVanilla(RenderGuiOverlayEvent.Pre event) {
            if (event.getOverlay() != VanillaGuiOverlay.MOUNT_HEALTH.type()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getVehicle() instanceof CarnotaurusEntity) event.setCanceled(true);
        }
    }

    private static void text(GuiGraphics g, Font font, String s, float x, float y, float scale, int color, boolean centered, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        if (centered) { if (shadow) g.drawCenteredString(font, s, 0, 0, color); else g.drawString(font, s, -font.width(s) / 2, 0, color, false); }
        else g.drawString(font, s, 0, 0, color, shadow);
        g.pose().popPose();
    }

    /** Texto centrado que se encoge para caber en maxW pixeles de pantalla. */
    private static void fitText(GuiGraphics g, Font font, String s, float cx, float cy, float maxW, float maxScale, int color, boolean shadow) {
        float sc = Math.min(maxScale, maxW / Math.max(1, font.width(s)));
        text(g, font, s, cx, cy - 4.0F * sc, sc, color, true, shadow);
    }

    /** Barrido circular de recarga (sentido horario) sobre un disco de radio r. */
    private static void fillSweep(GuiGraphics g, int cx, int cy, int r, float left) {
        double start = (1.0 - left) * Math.PI * 2.0;
        for (int dy = -r; dy <= r; dy++) {
            int runStart = Integer.MIN_VALUE;
            for (int dx = -r; dx <= r + 1; dx++) {
                boolean on = false;
                if (dx <= r && dx * dx + dy * dy <= r * r) {
                    double ang = Math.atan2(dx + 0.5, -(dy + 0.5));
                    if (ang < 0) ang += Math.PI * 2.0;
                    on = ang >= start;
                }
                if (on && runStart == Integer.MIN_VALUE) runStart = dx;
                if (!on && runStart != Integer.MIN_VALUE) {
                    g.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, 0xB8081018);
                    runStart = Integer.MIN_VALUE;
                }
            }
        }
    }

    private static void tex(GuiGraphics g, ResourceLocation t, float x, float y, float w, float h, int texW, int texH) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(w / texW, h / texH, 1.0F);
        g.blit(t, 0, 0, texW, texH, 0.0F, 0.0F, texW, texH, texW, texH);
        g.pose().popPose();
    }

    private static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof CarnotaurusEntity c)) return;
        Font font = mc.font;
        int variant = Math.max(0, Math.min(3, c.getVariant()));
        int accent = ACCENT[variant];
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // ---- marco con la vida (encima de la barra de objetos, a la derecha); textura 512x171
        final float fw = 104.0F, fh = fw * 171.0F / 512.0F, k = fw / 512.0F;
        float fx = w / 2.0F + 2.0F, fy = h - 31 - fh;
        tex(g, FRAME_HP[variant], fx, fy, fw, fh, 512, 171);
        float hp = Math.max(0.0F, c.getHealth()), max = Math.max(1.0F, c.getMaxHealth());
        float frac = Math.min(1.0F, hp / max);
        // hueco de la barra: x138..449, y84..116 en la textura
        int bx0 = Math.round(fx + 138 * k), bx1 = Math.round(fx + 449 * k), by0 = Math.round(fy + 84 * k), by1 = Math.round(fy + 116 * k);
        int fillW = Math.round((bx1 - bx0) * frac);
        if (fillW > 0) {
            int hi = frac > 0.5F ? 0xFFE8403A : frac > 0.25F ? 0xFFEE9A30 : 0xFFE02828;
            int lo = frac > 0.5F ? 0xFF931616 : frac > 0.25F ? 0xFFA25A12 : 0xFF780C0C;
            g.fillGradient(bx0, by0, bx0 + fillW, by1, hi, lo);
            g.fill(bx0, by0, bx0 + fillW, by0 + 1, 0x66FFFFFF);
        }
        text(g, font, Math.round(hp) + " / " + Math.round(max), (bx0 + bx1) / 2.0F, (by0 + by1) / 2.0F - 2.4F, 0.6F, 0xFFFFFFFF, true, true);
        // XP: linea fina del color de la variante en el borde inferior del hueco
        int xpNeed = Math.max(1, c.getProgress().xpToNext());
        int xw = c.getProgress().level() >= 100 ? bx1 - bx0 : Math.round((bx1 - bx0) * Math.min(1.0F, c.getProgress().xp() / (float) xpNeed));
        if (xw > 0) g.fill(bx0, by1 - 1, bx0 + xw, by1, accent);
        // nombre y nivel sobre las placas
        String name = c.hasCustomName() ? c.getCustomName().getString() : "Carnotauro";
        fitText(g, font, name, fx + 257 * k, fy + 56 * k, 190 * k, 0.6F, 0xFFF2E6CC, true);
        fitText(g, font, "Nv " + c.getProgress().level(), fx + 405 * k, fy + 53.5F * k, 56 * k, 0.5F, 0xFF3A1D06, false);

        // ---- poderes: botones de 125x140 con el icono debajo del marco; fila si cabe a la derecha de la barra, si no 2x2
        String[] keys = {key(ClientKeys.CHARGE.getTranslatedKeyMessage().getString()), key(ClientKeys.TAIL.getTranslatedKeyMessage().getString()), "Izq", key(ClientKeys.ULTIMATE.getTranslatedKeyMessage().getString())};
        final float bw = 38.0F, bh = bw * 140.0F / 125.0F, bk = bw / 125.0F, step = bw + 2.0F;
        boolean row = w - 4 * step - 3 >= w / 2.0F + 95.0F;
        for (int i = 0; i < 4; i++) {
            float x, y;
            if (row) { x = w - 4 * step - 3 + i * step; y = h - 4 - bh; }
            else { x = w - 2 * step - 3 + (i % 2) * step; y = h - 4 - 2 * bh - 2 + (i / 2) * (bh + 2); }
            float ccx = x + 62.5F * bk, ccy = y + 57.5F * bk, ir = 27.8F * bk;
            boolean locked = i == 3 && c.ultimateLevel() <= 0;
            tex(g, ICON[i][variant], ccx - 28.5F * bk, ccy - 28.5F * bk, 57.0F * bk, 57.0F * bk, 96, 96);
            int icx = Math.round(ccx), icy = Math.round(ccy), r = Math.round(ir);
            if (locked) {
                fillSweep(g, icx, icy, r, 1.0F);
                fillSweep(g, icx, icy, r, 1.0F);
                text(g, font, "50", ccx, ccy - 3.0F, 0.75F, 0xFFC8CDD2, true, true);
            } else {
                float left = c.cooldownLeft(i);
                if (left > 0.0F) {
                    fillSweep(g, icx, icy, r, left);
                    text(g, font, Integer.toString(c.cooldownSeconds(i)), ccx, ccy - 3.2F, 0.8F, 0xFFFFFFFF, true, true);
                }
            }
            tex(g, FRAME_BTN[i], x, y, bw, bh, 125, 140);
            // tecla en una pastilla sobre el borde inferior derecho del anillo
            float kw = Math.max(5.0F, font.width(keys[i]) * 0.5F);
            float kx = ccx + ir * 0.78F, ky = ccy + ir * 0.92F;
            g.fill(Math.round(kx - kw / 2 - 1), Math.round(ky - 1), Math.round(kx + kw / 2 + 1), Math.round(ky + 5), 0xD0000000);
            text(g, font, keys[i], kx, ky, 0.5F, 0xFFF2D27A, true, false);
            if (i == 3 && c.ultimateLevel() > 0) text(g, font, "I".repeat(c.ultimateLevel()), ccx - ir * 0.72F, ccy - ir * 0.95F, 0.55F, 0xFFFFE08A, true, true);
        }
        RenderSystem.disableBlend();
    }

    private static String key(String s) {
        return s.length() > 4 ? s.substring(0, 3) : s;
    }
}
