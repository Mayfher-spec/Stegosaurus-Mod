package com.prodigytown.creatures.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.prodigytown.creatures.DilophosaurusEntity;
import com.prodigytown.creatures.ProdigyCreatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
    private static final String[] ICON = {"M", "Z", "S", "F"};

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "dilo_mount_hud", (gui, g, partial, w, h) -> render(g, w, h));
        }
    }

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
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

    private static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof DilophosaurusEntity d)) return;
        Font font = mc.font;
        int accent = ACCENT[Math.max(0, Math.min(4, d.getVariant()))];
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int x0 = w / 2 + 6, y0 = h - 56;
        portrait(g, x0, y0 - 2, 24, accent);
        int bx = x0 + 28, bw = 78;
        String name = d.hasCustomName() ? d.getCustomName().getString() : "Dilofosaurio";
        g.drawString(font, name, bx, y0 - 2, 0xFFF2E6CC, true);
        g.drawString(font, "Nv " + d.getProgress().level(), bx + bw - font.width("Nv " + d.getProgress().level()), y0 + 8, 0xFFFFD35A, true);
        float hp = Math.max(0.0F, d.getHealth()), max = Math.max(1.0F, d.getMaxHealth()), frac = Math.min(1.0F, hp / max);
        int by = y0 + 18;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 8, 0xFF0A0F0D);
        int fw = Math.round(bw * frac);
        if (fw > 0) {
            int hi = frac > 0.5F ? 0xFF5FD04A : frac > 0.25F ? 0xFFEEA030 : 0xFFE03030;
            int lo = frac > 0.5F ? 0xFF2E7A24 : frac > 0.25F ? 0xFF9A5A12 : 0xFF7A1010;
            g.fillGradient(bx, by, bx + fw, by + 7, hi, lo);
        }
        g.renderOutline(bx - 1, by - 1, bw + 2, 9, accent);
        String hpTxt = Math.round(hp) + " / " + Math.round(max);
        g.drawString(font, hpTxt, bx + (bw - font.width(hpTxt)) / 2, by, 0xFFFFFFFF, true);
        int need = Math.max(1, d.getProgress().xpToNext());
        int xw = d.getProgress().level() >= 100 ? bw : Math.round(bw * Math.min(1.0F, d.getProgress().xp() / (float) need));
        g.fill(bx, by + 8, bx + bw, by + 9, 0x66000000);
        if (xw > 0) g.fill(bx, by + 8, bx + xw, by + 9, accent);

        // habilidades: mordida (clic izq / Z), zarpazo (clic der / X), salto (C), frenesi (G)
        String[] keys = {"Izq", "Der", ClientKeys.DILO_POUNCE.getTranslatedKeyMessage().getString(), ClientKeys.DILO_FRENZY.getTranslatedKeyMessage().getString()};
        int sx = w - 4 * 26 - 4, sy = h - 30;
        if (sx < x0 + 120) { sx = x0 + 112; }
        for (int i = 0; i < 4; i++) {
            int x = sx + i * 26;
            boolean active = i == 3 && d.isFrenzied();
            g.fill(x, sy, x + 24, sy + 24, 0xCC0E1412);
            g.renderOutline(x, sy, 24, 24, active ? 0xFFFFE066 : accent);
            g.drawString(font, ICON[i], x + 12 - font.width(ICON[i]) / 2, sy + 4, active ? 0xFFFFE066 : 0xFFF2E6CC, true);
            float left = d.cooldownLeft(i);
            if (left > 0.0F) {
                int oh = Math.round(24 * left);
                g.fill(x + 1, sy + 24 - oh, x + 23, sy + 23, 0xB0081018);
                String s = Integer.toString(d.cooldownSeconds(i));
                g.drawString(font, s, x + 12 - font.width(s) / 2, sy + 8, 0xFFFFFFFF, true);
            }
            String k = keys[i].length() > 3 ? keys[i].substring(0, 3) : keys[i];
            g.drawString(font, k, x + 12 - font.width(k) / 2, sy + 25, 0xFFF2D27A, true);
        }
        if (d.isFrenzied()) {
            int fx = sx, fw2 = 4 * 26 - 2, fy = sy - 6;
            g.fill(fx, fy, fx + fw2, fy + 3, 0xAA000000);
            g.fill(fx, fy, fx + Math.round(fw2 * Math.min(1.0F, d.frenzyLeft() / (float) Math.max(1, com.prodigytown.creatures.DiloConfig.get(com.prodigytown.creatures.DiloConfig.FRENZY_SECONDS, 10) * 20 + 60))), fy + 3, 0xFFFFE066);
        }
        RenderSystem.disableBlend();
    }
}
