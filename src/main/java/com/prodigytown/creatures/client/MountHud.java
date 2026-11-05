package com.prodigytown.creatures.client;

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

/** HUD al montar: nombre, nivel, vida y recargas de mordida / coletazo / embestida (estilo Saint's Dragons). */
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

    private static final ResourceLocation FRAME = new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/hud_frame.png");
    private static final ResourceLocation[] ICON = {
        new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/ab_bite.png"),
        new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/ab_tail.png"),
        new ResourceLocation(ProdigyCreatures.MODID, "textures/gui/ab_charge.png")};

    private static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof StegosaurusEntity s)) return;
        Font font = mc.font;

        // ---- marco con la vida (encima de la barra de objetos); textura 512x171, interior x148..451 y83..118
        final int fw = 124, fh = Math.round(fw * 171.0F / 512.0F);
        int fx = w / 2 - fw / 2, fy = h - 54 - fh;
        float k = fw / 512.0F;
        int ix = fx + Math.round(148 * k), iw = Math.round((451 - 148) * k), iy = fy + Math.round(83 * k), ih = Math.round((118 - 83) * k);
        float hp = Math.max(0.0F, s.getHealth()), max = Math.max(1.0F, s.getMaxHealth());
        float frac = Math.min(1.0F, hp / max);
        g.blit(FRAME, fx, fy, fw, fh, 0.0F, 0.0F, 512, 171, 512, 171);
        int fillW = Math.round(iw * frac);
        if (fillW > 0) {
            int hi = frac > 0.5F ? 0xFFE23A34 : frac > 0.25F ? 0xFFE88A2A : 0xFFD02020;
            int lo = frac > 0.5F ? 0xFF8E1414 : frac > 0.25F ? 0xFF9A5210 : 0xFF700C0C;
            g.fillGradient(ix, iy, ix + fillW, iy + ih, hi, lo);
            g.fill(ix, iy, ix + fillW, iy + 1, 0x66FFFFFF);
            for (int t = 1; t < 10; t++) { // marcas cada 10 %
                int tx = ix + iw * t / 10;
                if (tx < ix + fillW) g.fill(tx, iy, tx + 1, iy + ih, 0x33000000);
            }
        }
        String num = Math.round(hp) + " / " + Math.round(max);
        g.pose().pushPose();
        g.pose().translate(ix + iw / 2.0F, iy + (ih - 6.0F) / 2.0F + 0.5F, 0.0F);
        g.pose().scale(0.72F, 0.72F, 1.0F);
        g.drawCenteredString(font, num, 0, 0, 0xFFFFFFFF);
        g.pose().popPose();
        String name = s.hasCustomName() ? s.getCustomName().getString() : "Estegosaurio";
        g.pose().pushPose();
        g.pose().translate(fx + fw / 2.0F + 9.0F, fy - 1.0F, 0.0F);
        g.pose().scale(0.72F, 0.72F, 1.0F);
        g.drawCenteredString(font, name + "  " + Component.translatable("prodigycreatures.hud.level").getString() + s.getProgress().level(), 0, 0, 0xFFF2D27A);
        g.pose().popPose();

        // ---- poderes: iconos circulares a la derecha, con barrido de recarga (mordida grande, coletazo y embestida pequenos)
        String[] keys = {"Izq", "Der", ClientKeys.CHARGE.getTranslatedKeyMessage().getString()};
        int[] which = {2, 1, 0};
        int[] cx = {w - 19, w - 48, w - 42};
        int[] cy = {h - 21, h - 14, h - 41};
        int[] rad = {13, 10, 10};
        for (int i = 0; i < 3; i++) {
            int r = rad[i], x = cx[i] - r, y = cy[i] - r;
            float left = s.cooldownLeft(which[i]);
            g.blit(ICON[i], x, y, 2 * r, 2 * r, 0.0F, 0.0F, 64, 64, 64, 64);
            if (left > 0.0F) {
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
                            g.fill(cx[i] + runStart, cy[i] + dy, cx[i] + dx, cy[i] + dy + 1, 0xB0081018);
                            runStart = Integer.MIN_VALUE;
                        }
                    }
                }
                g.pose().pushPose();
                g.pose().translate(cx[i], cy[i] - 3.0F, 0.0F);
                g.pose().scale(0.85F, 0.85F, 1.0F);
                g.drawCenteredString(font, Integer.toString(s.cooldownSeconds(which[i])), 0, 0, 0xFFFFFFFF);
                g.pose().popPose();
            }
            // etiqueta de la tecla en la esquina inferior derecha del circulo
            float kw = font.width(keys[i]) * 0.55F;
            int bx2 = cx[i] + r - 3, by2 = cy[i] + r - 5;
            g.fill((int) (bx2 - kw / 2 - 1), by2 - 1, (int) (bx2 + kw / 2 + 2), by2 + 5, 0xCC000000);
            g.pose().pushPose();
            g.pose().translate(bx2 - kw / 2.0F, by2, 0.0F);
            g.pose().scale(0.55F, 0.55F, 1.0F);
            g.drawString(font, keys[i], 0, 0, 0xFFF2D27A, false);
            g.pose().popPose();
        }
    }
}
