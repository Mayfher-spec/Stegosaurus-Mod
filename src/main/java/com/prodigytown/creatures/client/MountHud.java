package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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

    private static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !(mc.player.getVehicle() instanceof StegosaurusEntity s)) return;
        Font font = mc.font;
        int cx = w / 2;
        int top = h - 98;

        // nombre y nivel
        String name = s.hasCustomName() ? s.getCustomName().getString() : "Estegosaurio";
        String title = name + "  " + Component.translatable("prodigycreatures.hud.level").getString() + s.getProgress().level();
        g.drawCenteredString(font, title, cx, top, 0xFFF2D27A);

        // barra de vida
        int bw = 182, bx = cx - bw / 2, by = top + 12;
        float hp = Math.max(0.0F, s.getHealth()), max = Math.max(1.0F, s.getMaxHealth());
        float frac = Math.min(1.0F, hp / max);
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 11, 0xFF101010);
        g.fill(bx, by, bx + bw, by + 10, 0xFF3A1414);
        int col = frac > 0.5F ? 0xFF5CC24A : frac > 0.25F ? 0xFFE0B030 : 0xFFD83A30;
        int fw = Math.round(bw * frac);
        g.fill(bx, by, bx + fw, by + 10, col);
        g.fill(bx, by, bx + fw, by + 3, 0x55FFFFFF);
        String num = Math.round(hp) + " / " + Math.round(max);
        g.drawCenteredString(font, num, cx, by + 1, 0xFFFFFFFF);

        // habilidades: mordida (clic izq), coletazo (clic der), embestida (V)
        String[] labels = {"Mordida", "Coletazo", "Embestida"};
        String[] keys = {"Clic izq", "Clic der", ClientKeys.CHARGE.getTranslatedKeyMessage().getString()};
        int[] which = {2, 1, 0};
        int[] tint = {0xFFB8483C, 0xFFC8A040, 0xFF4A86C8};
        int sw = 58, gap = 4, total = sw * 3 + gap * 2, sx = cx - total / 2, sy = by + 15;
        for (int i = 0; i < 3; i++) {
            int x = sx + i * (sw + gap);
            float left = s.cooldownLeft(which[i]);
            g.fill(x - 1, sy - 1, x + sw + 1, sy + 21, 0xFF101010);
            g.fill(x, sy, x + sw, sy + 20, left > 0 ? 0xFF2A2A2A : tint[i]);
            if (left > 0) { // barrido: la parte inferior se va llenando de color a medida que recarga
                int filled = Math.round(20 * (1.0F - left));
                g.fill(x, sy + 20 - filled, x + sw, sy + 20, (tint[i] & 0x00FFFFFF) | 0xAA000000);
                g.drawCenteredString(font, s.cooldownSeconds(which[i]) + "s", x + sw / 2, sy + 6, 0xFFFFFFFF);
            } else {
                g.drawCenteredString(font, labels[i], x + sw / 2, sy + 6, 0xFFFFFFFF);
            }
            g.drawCenteredString(font, keys[i], x + sw / 2, sy + 22, 0xFFBFBFBF);
        }
    }
}
