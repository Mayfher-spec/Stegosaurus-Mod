package com.prodigytown.creatures.client;

import com.prodigytown.creatures.CarnotaurusEntity;
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

/** HUD al montar el carnotauro: nombre, nivel, vida y recargas de embestida (V), cabezazo (B / clic derecho), mordida (clic izquierdo) y rugido (R). */
public final class CarnoMountHud {
    private CarnoMountHud() {}

    /** Color de acento por variante: rojo, verde, acero, arena. */
    private static final int[] ACCENT = {0xFFD9442E, 0xFF5FB35A, 0xFF8FA6B8, 0xFFD9B56A};

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "carno_mount_hud", (gui, g, partial, w, h) -> render(g, w, h));
        }
    }

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        /** Se oculta la barra de corazones de montura de vanilla (220 PV serian 30 corazones): la reemplaza este HUD. */
        @SubscribeEvent public static void hideVanilla(RenderGuiOverlayEvent.Pre event) {
            if (event.getOverlay() != VanillaGuiOverlay.MOUNT_HEALTH.type()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getVehicle() instanceof CarnotaurusEntity) event.setCanceled(true);
        }
    }

    private static void text(GuiGraphics g, Font font, String s, float x, float y, float scale, int color, boolean centered) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        if (centered) g.drawCenteredString(font, s, 0, 0, color); else g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    private static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof CarnotaurusEntity c)) return;
        Font font = mc.font;
        int accent = ACCENT[Math.max(0, Math.min(3, c.getVariant()))];

        // ---- vida (encima de la barra de objetos, a la derecha)
        int bw = 88, bh = 9, bx = w / 2 + 9, by = h - 40;
        float hp = Math.max(0.0F, c.getHealth()), max = Math.max(1.0F, c.getMaxHealth());
        float frac = Math.min(1.0F, hp / max);
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF0B0E12);
        g.fill(bx, by, bx + bw, by + bh, 0xFF2A2F36);
        int fillW = Math.round(bw * frac);
        if (fillW > 0) {
            int hi = frac > 0.5F ? 0xFFE23A34 : frac > 0.25F ? 0xFFE88A2A : 0xFFD02020;
            int lo = frac > 0.5F ? 0xFF8E1414 : frac > 0.25F ? 0xFF9A5210 : 0xFF700C0C;
            g.fillGradient(bx, by, bx + fillW, by + bh, hi, lo);
            g.fill(bx, by, bx + fillW, by + 1, 0x66FFFFFF);
        }
        text(g, font, Math.round(hp) + " / " + Math.round(max), bx + bw / 2.0F, by + 1.5F, 0.6F, 0xFFFFFFFF, true);
        String name = c.hasCustomName() ? c.getCustomName().getString() : "Carnotauro";
        text(g, font, name + "  Nv " + c.getProgress().level(), bx + bw / 2.0F, by - 8.0F, 0.65F, 0xFFF2D27A, true);
        // barra de XP fina bajo la vida
        int xpNeed = Math.max(1, c.getProgress().xpToNext());
        int xw = c.getProgress().level() >= 100 ? bw : Math.round(bw * Math.min(1.0F, c.getProgress().xp() / (float) xpNeed));
        g.fill(bx, by + bh + 2, bx + bw, by + bh + 4, 0xFF1B2128);
        if (xw > 0) g.fill(bx, by + bh + 2, bx + xw, by + bh + 4, accent);

        // ---- poderes: cuatro casillas a la derecha con barrido de recarga
        String[] names = {"Embestida", "Cabezazo", "Mordida", "Rugido"};
        String[] keys = {ClientKeys.CHARGE.getTranslatedKeyMessage().getString(), ClientKeys.TAIL.getTranslatedKeyMessage().getString(), "Izq", ClientKeys.ULTIMATE.getTranslatedKeyMessage().getString()};
        int[] which = {0, 1, 2, 3};
        int size = 22, gap = 3, total = 4 * size + 3 * gap;
        int x0 = w - total - 8, y0 = h - 31;
        for (int i = 0; i < 4; i++) {
            int x = x0 + i * (size + gap);
            boolean ult = i == 3 && c.ultimateLevel() > 0;
            g.fill(x - 1, y0 - 1, x + size + 1, y0 + size + 1, ult ? 0xFFF2D27A : accent);
            g.fill(x, y0, x + size, y0 + size, 0xFF161B21);
            float left = c.cooldownLeft(which[i]);
            if (left > 0.0F) {
                int hgt = Math.round(size * left);
                g.fill(x, y0 + size - hgt, x + size, y0 + size, 0xC0081018);
                text(g, font, Integer.toString(c.cooldownSeconds(which[i])), x + size / 2.0F, y0 + size / 2.0F - 3.5F, 0.9F, 0xFFFFFFFF, true);
            } else {
                text(g, font, names[i].substring(0, 1), x + size / 2.0F, y0 + 4.0F, 1.3F, ult ? 0xFFF2D27A : accent, true);
            }
            text(g, font, names[i], x + size / 2.0F, y0 + size + 2.0F, 0.5F, 0xFFC8CDD2, true);
            text(g, font, keys[i], x + 2.0F, y0 + 1.5F, 0.5F, 0xFFF2D27A, false);
        }
        int ul = c.ultimateLevel();
        text(g, font, ul > 0 ? "Rugido ancestral " + "I".repeat(ul) : "Rugido ancestral: nivel 50", x0 + total / 2.0F, y0 - 8.0F, 0.55F, ul > 0 ? 0xFFF2D27A : 0xFF7C858E, true);
    }
}
