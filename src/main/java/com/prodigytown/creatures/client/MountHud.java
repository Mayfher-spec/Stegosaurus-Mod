package com.prodigytown.creatures.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** HUD al montar el estegosaurio: marco con retrato, nombre, nivel, vida, XP y los 4 poderes (embestida, cabezazo/mordida, coletazo y rugido ancestral). */
public final class MountHud {
    private MountHud() {}

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "mount_hud", (gui, g, partial, w, h) -> render(g, w, h));
        }
    }

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        /** Se oculta la barra de corazones de montura de vanilla: la reemplaza este HUD. */
        @SubscribeEvent public static void hideVanilla(RenderGuiOverlayEvent.Pre event) {
            if (event.getOverlay() != VanillaGuiOverlay.MOUNT_HEALTH.type()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getVehicle() instanceof StegosaurusEntity) event.setCanceled(true);
        }
    }

    private static final ResourceLocation[] FRAME = new ResourceLocation[5];
    private static final ResourceLocation[] BTN = new ResourceLocation[4];
    /** ICON[boton][variante]: boton 0 embestida, 1 cabezazo (mordida), 2 coletazo, 3 rugido. */
    private static final ResourceLocation[][] ICON = new ResourceLocation[4][5];
    /** Indice de recarga de la entidad por boton: embestida 0, mordida 2, coletazo 1, definitiva 3. */
    private static final int[] WHICH = {0, 2, 1, 3};
    /** Centro de la casilla hexagonal de la tecla en cada boton (px de la textura 150x168). */
    private static final float[][] HEX = {{75.3F, 103.5F}, {75.0F, 104.4F}, {75.0F, 104.4F}, {74.1F, 104.7F}};
    static {
        for (int v = 0; v < 5; v++) {
            FRAME[v] = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/hud_shp_" + v + ".png");
            for (int b = 0; b < 4; b++) ICON[b][v] = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/hud_sic_" + b + "_" + v + ".png");
        }
        for (int b = 0; b < 4; b++) BTN[b] = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/hud_sbtn_" + b + ".png");
    }

    private static void text(GuiGraphics g, Font font, String s, float x, float y, float scale, int color, boolean centered, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        if (centered) g.drawString(font, s, -font.width(s) / 2, 0, color, shadow); else g.drawString(font, s, 0, 0, color, shadow);
        g.pose().popPose();
    }

    private static void tex(GuiGraphics g, ResourceLocation t, float x, float y, float w, float h, int texW, int texH) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(w / texW, h / texH, 1.0F);
        g.blit(t, 0, 0, texW, texH, 0.0F, 0.0F, texW, texH, texW, texH);
        g.pose().popPose();
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

    private static String key(String s) {
        return s.length() > 4 ? s.substring(0, 3) : s;
    }

    private static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof StegosaurusEntity s)) return;
        Font font = mc.font;
        int variant = Math.max(0, Math.min(4, s.getVariant()));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // ---- marco con retrato, vida y XP (encima de la barra de objetos, a la derecha); textura 640x180
        final float fw = 150.0F, fh = fw * 180.0F / 640.0F, k = fw / 640.0F;
        float fx = w / 2.0F + 2.0F, fy = h - 31 - fh;
        tex(g, FRAME[variant], fx, fy, fw, fh, 640, 180);
        // barra de vida: x205..570, y84..103.5
        int bx0 = Math.round(fx + 205 * k), bx1 = Math.round(fx + 570 * k), by0 = Math.round(fy + 84 * k), by1 = Math.round(fy + 103.5F * k);
        float hp = Math.max(0.0F, s.getHealth()), max = Math.max(1.0F, s.getMaxHealth());
        float frac = Math.min(1.0F, hp / max);
        int fillW = Math.round((bx1 - bx0) * frac);
        if (fillW > 0) {
            int hi = frac > 0.5F ? 0xFFE8403A : frac > 0.25F ? 0xFFEE9A30 : 0xFFE02828;
            int lo = frac > 0.5F ? 0xFF931616 : frac > 0.25F ? 0xFFA25A12 : 0xFF780C0C;
            g.fillGradient(bx0, by0, bx0 + fillW, by1, hi, lo);
            g.fill(bx0, by0, bx0 + fillW, by0 + 1, 0x66FFFFFF);
        }
        text(g, font, Math.round(hp) + " / " + Math.round(max), (bx0 + bx1) / 2.0F, (by0 + by1) / 2.0F - 2.2F, 0.55F, 0xFFFFFFFF, true, true);
        // XP: siete segmentos cian; los no alcanzados se apagan (x205..531, y114.5..125.6)
        int sx0 = Math.round(fx + 205 * k), sx1 = Math.round(fx + 531 * k), sy0 = Math.round(fy + 114.0F * k), sy1 = Math.round(fy + 126.0F * k);
        int xpNeed = Math.max(1, s.getProgress().xpToNext());
        float xf = s.getProgress().level() >= 100 ? 1.0F : Math.min(1.0F, s.getProgress().xp() / (float) xpNeed);
        int lit = sx0 + Math.round((sx1 - sx0) * xf);
        if (lit < sx1) g.fill(lit, sy0, sx1, sy1, 0xC00E1218);
        // cabecera: nombre / nivel sobre la placa oscura (x207..503, centro y63)
        String name = s.hasCustomName() ? s.getCustomName().getString() : "Estegosaurio";
        String lv = Component.translatable("prodigycreatures.hud.level").getString() + " " + s.getProgress().level();
        String sep = "  /  ";
        float total = font.width(name) + font.width(sep) + font.width(lv);
        float hs = Math.min(0.55F, (503 - 207) * k / Math.max(1.0F, total));
        float hx = fx + 207 * k, hy = fy + 63.0F * k - 4.0F * hs;
        text(g, font, name, hx, hy, hs, 0xFFF2E6CC, false, true);
        hx += font.width(name) * hs;
        text(g, font, sep, hx, hy, hs, 0xFF8A8F96, false, false);
        hx += font.width(sep) * hs;
        text(g, font, lv, hx, hy, hs, 0xFFF2C75A, false, true);

        // ---- poderes: cuatro botones; en fila si caben a la derecha del marco, si no en cuadricula 2x2
        String[] keys = {key(ClientKeys.CHARGE.getTranslatedKeyMessage().getString()), "Izq", "Der", key(ClientKeys.ULTIMATE.getTranslatedKeyMessage().getString())};
        final float bw = 40.0F, bh = bw * 168.0F / 150.0F, bk = bw / 150.0F, step = bw + 2.0F;
        boolean row = w - 4 * step - 3 >= fx + fw + 4.0F;
        for (int i = 0; i < 4; i++) {
            float x, y;
            if (row) { x = w - 4 * step - 3 + i * step; y = h - 4 - bh; }
            else { x = w - 2 * step - 3 + (i % 2) * step; y = h - 4 - 2 * bh - 2 + (i / 2) * (bh + 2); }
            float ccx = x + 75.0F * bk, ccy = y + 69.0F * bk, ir = 35.4F * bk;
            tex(g, ICON[i][variant], ccx - 36.9F * bk, ccy - 36.9F * bk, 73.8F * bk, 73.8F * bk, 96, 96);
            int icx = Math.round(ccx), icy = Math.round(ccy), r = Math.round(ir);
            boolean locked = i == 3 && s.ultimateLevel() <= 0;
            if (locked) {
                fillSweep(g, icx, icy, r, 1.0F);
                fillSweep(g, icx, icy, r, 1.0F);
                text(g, font, "Nv.50", ccx, ccy - 2.4F, 0.6F, 0xFFE0E4E8, true, true);
            } else {
                float left = s.cooldownLeft(WHICH[i]);
                if (left > 0.0F) {
                    fillSweep(g, icx, icy, r, left);
                    text(g, font, Integer.toString(s.cooldownSeconds(WHICH[i])), ccx, ccy - 3.2F, 0.8F, 0xFFFFFFFF, true, true);
                }
            }
            tex(g, BTN[i], x, y, bw, bh, 150, 168);
            // tecla dentro de la casilla hexagonal
            float hxc = x + HEX[i][0] * bk, hyc = y + HEX[i][1] * bk;
            float ks = Math.min(0.6F, 6.4F / Math.max(1, font.width(keys[i])));
            text(g, font, keys[i], hxc, hyc - 4.0F * ks, ks, 0xFFF6DFA0, true, false);
            if (i == 3 && s.ultimateLevel() > 0) text(g, font, "I".repeat(s.ultimateLevel()), ccx - ir * 0.72F, ccy - ir * 0.95F, 0.55F, 0xFFFFE08A, true, true);
        }
        RenderSystem.disableBlend();
    }
}
