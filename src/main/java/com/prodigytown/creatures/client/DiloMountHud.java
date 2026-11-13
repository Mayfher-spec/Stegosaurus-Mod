package com.prodigytown.creatures.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.prodigytown.creatures.DilophosaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
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

/** HUD al montar el dilofosaurio: retrato con las dos crestas, barra de vida propia, nivel y las cuatro habilidades con su recarga. */
public final class DiloMountHud {
    private DiloMountHud() {}

    /** Color de cada variante: natural, azul electrico, magma, desierto, espectral. */
    public static final int[] ACCENT = {0xFF6FA84A, 0xFF3FB4FF, 0xFFE8502C, 0xFFD9B56A, 0xFF3CE0A0};
    private static final String[] AB = {"bite", "claw", "pounce", "frenzy", "spit"};
    private static final ResourceLocation[] FRAME = new ResourceLocation[5];
    private static final ResourceLocation[] ICON = new ResourceLocation[5];
    static {
        for (int v = 0; v < 5; v++) FRAME[v] = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/dilo/hud_frame_" + v + ".png");
        for (int i = 0; i < 5; i++) ICON[i] = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/dilo/ab_" + AB[i] + ".png");
    }
    /** Marco (maqueta del usuario): 768x233. Huecos medidos en la textura. */
    private static final int TW = 768, TH = 233;

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class DiloModBus {
        @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "dilo_mount_hud", (gui, g, partial, w, h) -> render(g, w, h));
        }
    }

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, value = Dist.CLIENT)
    public static final class DiloForgeBus {
        /** La barra de corazones de montura de vanilla se sustituye por este HUD. */
        @SubscribeEvent public static void hideVanilla(RenderGuiOverlayEvent.Pre event) {
            if (event.getOverlay() != VanillaGuiOverlay.MOUNT_HEALTH.type()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getVehicle() instanceof DilophosaurusEntity) event.setCanceled(true);
        }
    }

    /** Retrato: cabeza con las dos crestas siempre visibles (vistas como dos puas hacia atras). */
    public static void portrait(GuiGraphics g, int x, int y, int size, int accent) {
        g.fill(x, y, x + size, y + size, 0xFF0E1412);
        g.renderOutline(x, y, size, size, accent);
        int u = Math.max(1, size / 12);
        // crestas
        g.fill(x + 3 * u, y + 2 * u, x + 5 * u, y + 5 * u, 0xFFE9DDB8);
        g.fill(x + 2 * u, y + 3 * u, x + 3 * u, y + 5 * u, 0xFFD8C89A);
        g.fill(x + 5 * u, y + 1 * u, x + 7 * u, y + 5 * u, 0xFFF2E8C8);
        g.fill(x + 4 * u, y + 2 * u, x + 5 * u, y + 5 * u, 0xFFE9DDB8);
        // cabeza y hocico
        g.fill(x + 3 * u, y + 5 * u, x + 9 * u, y + 9 * u, accent);
        g.fill(x + 8 * u, y + 6 * u, x + 11 * u, y + 8 * u, accent);
        g.fill(x + 8 * u, y + 8 * u, x + 11 * u, y + 9 * u, 0xFFE8E0D0);
        g.fill(x + 6 * u, y + 5 * u + u / 2, x + 7 * u, y + 7 * u, 0xFFFFE066); // ojo
    }

    private static void text(GuiGraphics g, Font font, String s, float x, float y, float scale, int color, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, s, -font.width(s) / 2, 0, color, shadow);
        g.pose().popPose();
    }

    private static void fitText(GuiGraphics g, Font font, String s, float cx, float cy, float maxW, float maxScale, int color) {
        float sc = Math.min(maxScale, maxW / Math.max(1, font.width(s)));
        text(g, font, s, cx, cy - 4.0F * sc, sc, color, true);
    }

    private static void tex(GuiGraphics g, ResourceLocation t, float x, float y, float w, float h, int texW, int texH) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(w / texW, h / texH, 1.0F);
        g.blit(t, 0, 0, texW, texH, 0.0F, 0.0F, texW, texH, texW, texH);
        g.pose().popPose();
    }

    /** Barrido circular de recarga (sentido horario) sobre un disco de radio r. */
    private static void fillSweep(GuiGraphics g, int cx, int cy, int r, float left, int color) {
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
                    g.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, color);
                    runStart = Integer.MIN_VALUE;
                }
            }
        }
    }

    private static String key(String s) { return s.length() > 4 ? s.substring(0, 3) : s; }

    private static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof DilophosaurusEntity d)) return;
        Font font = mc.font;
        int variant = Math.max(0, Math.min(4, d.getVariant()));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // ---- marco con retrato, nombre, vida y XP (encima de la barra de objetos, a la derecha)
        final float fw = 176.0F, k = fw / TW, fh = TH * k;
        float fx = w / 2.0F + 2.0F, fy = h - 28.0F - fh;
        tex(g, FRAME[variant], fx, fy, fw, fh, TW, TH);
        float hp = Math.max(0.0F, d.getHealth()), max = Math.max(1.0F, d.getMaxHealth()), frac = Math.min(1.0F, hp / max);
        int bx0 = Math.round(fx + 231.0F * k), bx1 = Math.round(fx + 680.0F * k), by0 = Math.round(fy + 96.5F * k), by1 = Math.round(fy + 123.5F * k);
        int fillW = Math.round((bx1 - bx0) * frac);
        if (fillW > 0) {
            int hi = frac > 0.5F ? 0xFFE8403A : frac > 0.25F ? 0xFFEE9A30 : 0xFFE02828;
            int lo = frac > 0.5F ? 0xFF931616 : frac > 0.25F ? 0xFFA25A12 : 0xFF780C0C;
            g.fillGradient(bx0, by0, bx0 + fillW, by1, hi, lo);
            g.fill(bx0, by0, bx0 + fillW, by0 + 1, 0x66FFFFFF);
        }
        text(g, font, Math.round(hp) + " / " + Math.round(max), (bx0 + bx1) / 2.0F, (by0 + by1) / 2.0F - 2.0F, 0.5F, 0xFFFFFFFF, true);
        // XP: 6 segmentos como en la maqueta
        int xx0 = Math.round(fx + 234.0F * k), xx1 = Math.round(fx + 676.0F * k), xy0 = Math.round(fy + 138.5F * k), xy1 = Math.round(fy + 151.5F * k);
        int lvl = d.getProgress().level();
        float xf = lvl >= 100 ? 1.0F : Math.min(1.0F, d.getProgress().xp() / (float) Math.max(1, d.getProgress().xpToNext()));
        float segW = (xx1 - xx0 - 5.0F * 2.0F) / 6.0F;
        for (int i = 0; i < 6; i++) {
            int sx0 = Math.round(xx0 + i * (segW + 2.0F)), sx1 = Math.round(sx0 + segW);
            g.fill(sx0, xy0, sx1, xy1, 0xFF0B1A20);
            float part = Math.max(0.0F, Math.min(1.0F, xf * 6.0F - i));
            if (part > 0.0F) {
                g.fill(sx0, xy0, sx0 + Math.max(1, Math.round((sx1 - sx0) * part)), xy1, 0xFF18E6F2);
                g.fill(sx0, xy0, sx0 + Math.max(1, Math.round((sx1 - sx0) * part)), xy0 + 1, 0x88FFFFFF);
            }
        }
        String name = d.hasCustomName() ? d.getCustomName().getString() : "Dilofosaurio";
        String sub = (d.isAdultStage() ? "Adulto" : "Cria") + " / " + lvl;
        fitText(g, font, name + " / " + sub, fx + 447.0F * k, fy + 70.0F * k, 400.0F * k, 0.62F, 0xFFF2DDA8);

        // ---- poderes: discos con la recarga; mordida (clic izq), zarpazo (clic der), salto, frenesi y, con el talento, escupitajo
        String[] keys = {"Izq", "Der", key(ClientKeys.DILO_POUNCE.getTranslatedKeyMessage().getString()), key(ClientKeys.DILO_FRENZY.getTranslatedKeyMessage().getString()), key(ClientKeys.DILO_SPIT.getTranslatedKeyMessage().getString())};
        int n = d.getProgress().rank("S7") > 0 ? 5 : 4;
        final float bs = 26.0F, step = bs + 4.0F;
        float sx0 = fx + 40.0F, sy0 = fy - bs - 6.0F;
        for (int i = 0; i < n; i++) {
            float x = sx0 + i * step, y = sy0;
            boolean active = i == 3 && d.isFrenzied();
            float ccx = x + bs / 2.0F, ccy = y + bs / 2.0F;
            tex(g, ICON[i], x, y, bs, bs, 128, 128);
            int icx = Math.round(ccx), icy = Math.round(ccy), r = Math.round(bs / 2.0F) - 1;
            float left = d.cooldownLeft(i);
            if (left > 0.0F) {
                fillSweep(g, icx, icy, r, left, 0xB8081018);
                text(g, font, Integer.toString(d.cooldownSeconds(i)), ccx, ccy - 3.2F, 0.8F, 0xFFFFFFFF, true);
            }
            if (active) fillSweep(g, icx, icy, r, 1.0F, 0x30FFE066);
            String kk = keys[i];
            float kw = Math.max(5.0F, font.width(kk) * 0.5F);
            float kx = ccx, ky = y + bs - 3.0F;
            g.fill(Math.round(kx - kw / 2 - 1), Math.round(ky - 1), Math.round(kx + kw / 2 + 1), Math.round(ky + 5), active ? 0xE0806000 : 0xD0000000);
            text(g, font, kk, kx, ky, 0.5F, active ? 0xFFFFE066 : 0xFFF2D27A, false);
        }
        if (d.isFrenzied()) {
            int fx0 = Math.round(sx0), fw2 = Math.round(n * step - 4.0F), fy2 = Math.round(sy0 - 4.0F);
            int total = Math.max(1, com.prodigytown.creatures.DiloConfig.get(com.prodigytown.creatures.DiloConfig.FRENZY_SECONDS, 10) * 20 + 60);
            g.fill(fx0, fy2, fx0 + fw2, fy2 + 2, 0xAA000000);
            g.fill(fx0, fy2, fx0 + Math.round(fw2 * Math.min(1.0F, d.frenzyLeft() / (float) total)), fy2 + 2, 0xFFFFE066);
        }
        RenderSystem.disableBlend();
    }
}
