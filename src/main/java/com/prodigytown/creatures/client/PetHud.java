package com.prodigytown.creatures.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.prodigytown.creatures.CarnotaurusEntity;
import com.prodigytown.creatures.DilophosaurusEntity;
import com.prodigytown.creatures.PetOrders;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.progress.ProdigyProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * HUD unico al montar el estegosaurio, el carnotauro o el dilofosaurio: retrato, nombre, nivel, vida, experiencia, orden actual y una casilla por habilidad
 * (tecla, icono, recarga circular/numero y candado). Mismo marco y misma disposicion para las tres criaturas, con el color de acento de cada una.
 */
public final class PetHud {
    private PetHud() {}

    private static boolean riding(Entity v) {
        return v instanceof StegosaurusEntity || v instanceof CarnotaurusEntity || v instanceof DilophosaurusEntity;
    }

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "pet_hud", (gui, g, partial, w, h) -> render(g, w, h));
        }
    }

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        /** Se oculta la barra de corazones de montura de vanilla (cientos de PV): la reemplaza este HUD. */
        @SubscribeEvent public static void hideVanilla(RenderGuiOverlayEvent.Pre event) {
            if (event.getOverlay() != VanillaGuiOverlay.MOUNT_HEALTH.type()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && riding(mc.player.getVehicle())) event.setCanceled(true);
        }
    }

    private static void text(GuiGraphics g, Font font, String s, float x, float y, float scale, int color, boolean centered, boolean shadow) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        if (centered) g.drawString(font, s, -font.width(s) / 2, 0, color, shadow); else g.drawString(font, s, 0, 0, color, shadow);
        g.pose().popPose();
    }

    private static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int c1, int c2) {
        g.fill(x, y, x + w, y + h, 0xFF16100C);
        g.renderOutline(x, y, w, h, 0xFF4A3A22);
        int fw = Math.round((w - 2) * Math.max(0.0F, Math.min(1.0F, frac)));
        if (fw > 0) { g.fillGradient(x + 1, y + 1, x + 1 + fw, y + h - 1, c1, c2); g.fill(x + 1, y + 1, x + 1 + fw, y + 2, 0x55FFFFFF); }
    }

    private static void render(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        Entity v = mc.player.getVehicle();
        if (!riding(v) || !(v instanceof TamableAnimal pet)) return;
        PetSpec spec = PetSpec.of(pet);
        if (spec == null) return;
        Font font = mc.font;
        ProdigyProgress p = spec.progress();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // ---- panel de estado (encima de la barra de objetos, a la derecha)
        final int pw = 160, ph = 40;
        int px = w / 2 + 4, py = h - 31 - ph;
        g.fill(px, py, px + pw, py + ph, 0xDD0E0A08);
        g.renderOutline(px, py, pw, ph, 0xFF9C7A3A);
        g.renderOutline(px + 1, py + 1, pw - 2, ph - 2, 0xFF2A1C10);
        g.fill(px + 3, py + 3, px + 37, py + 37, 0xFF0B100E);
        g.renderOutline(px + 3, py + 3, 34, 34, spec.accent);
        g.enableScissor(px + 4, py + 4, px + 36, py + 36);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, px + 20, py + 35, spec.adult() ? 8 : 14, -10.0F, -22.0F, pet);
        g.disableScissor();
        String name = pet.hasCustomName() ? pet.getCustomName().getString() : spec.species;
        String lv = Component.translatable("prodigycreatures.hud.level").getString() + " " + p.level();
        float lw = font.width(lv) * 0.7F;
        float ns = 0.75F;
        while (font.width(name) * ns > pw - 44 - lw - 6 && name.length() > 3) name = name.substring(0, name.length() - 1);
        text(g, font, name, px + 42, py + 4, ns, 0xFFF2E6CC, false, true);
        text(g, font, lv, px + pw - 4 - lw, py + 5, 0.7F, 0xFFF2C75A, false, true);
        float hp = Math.max(0.0F, pet.getHealth()), max = Math.max(1.0F, pet.getMaxHealth());
        float frac = Math.min(1.0F, hp / max);
        bar(g, px + 42, py + 15, pw - 46, 10, frac, frac > 0.5F ? 0xFFE8403A : frac > 0.25F ? 0xFFEE9A30 : 0xFFE02828,
            frac > 0.5F ? 0xFF931616 : frac > 0.25F ? 0xFFA25A12 : 0xFF780C0C);
        text(g, font, Math.round(hp) + " / " + Math.round(max), px + 42 + (pw - 46) / 2.0F, py + 16.5F, 0.6F, 0xFFFFFFFF, true, true);
        int need = Math.max(1, p.xpToNext());
        float xf = p.level() >= 100 ? 1.0F : Math.min(1.0F, p.xp() / (float) need);
        bar(g, px + 42, py + 28, pw - 46, 5, xf, 0xFF58B8F0, 0xFF2A6CC0);
        int ord = Math.max(0, Math.min(PetOrders.COUNT - 1, spec.order()));
        String os = spec.fallen() ? "Fuera de combate" : PetOrders.NAMES[ord];
        text(g, font, os, px + 42, py + 34, 0.55F, spec.fallen() ? 0xFFE0453A : 0xFFB8A46A, false, false);

        // ---- habilidades: una casilla cada una; en fila si caben a la derecha del panel, si no en 2 columnas
        List<PetSpec.Ability> abs = spec.abilities.stream().filter(a -> a.cd() >= 0).toList();
        final int bs = 26, gap = 2, step = bs + gap;
        int n = abs.size();
        boolean row = w - n * step - 4 >= px + pw + 4;
        for (int i = 0; i < n; i++) {
            PetSpec.Ability a = abs.get(i);
            int x, y;
            if (row) { x = w - n * step - 2 + i * step; y = h - 4 - bs; }
            else { x = w - 2 * step - 2 + (i % 2) * step; y = h - 4 - bs - (i / 2) * step; }
            boolean locked = a.locked().getAsBoolean();
            g.fill(x, y, x + bs, y + bs, 0xDD0E0A08);
            g.renderOutline(x, y, bs, bs, locked ? 0xFF4A4F54 : spec.accent);
            g.renderOutline(x + 1, y + 1, bs - 2, bs - 2, 0xFF2A1C10);
            g.pose().pushPose();
            g.pose().translate(x + 5.0F, y + 5.0F, 0.0F);
            if (locked) g.setColor(1.0F, 1.0F, 1.0F, 0.35F);
            g.renderItem(new ItemStack(a.icon()), 0, 0);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.pose().popPose();
            if (locked) {
                text(g, font, a.lockedText(), x + bs / 2.0F, y + bs - 9, 0.5F, 0xFFE0A040, true, true);
            } else {
                float left = spec.cdLeft(a.cd());
                if (left > 0.0F) {
                    int hgt = Math.round((bs - 2) * left);
                    g.fill(x + 1, y + 1, x + bs - 1, y + 1 + hgt, 0xB8081018);
                    text(g, font, Integer.toString(spec.cdSeconds(a.cd())), x + bs / 2.0F, y + bs / 2.0F - 3.5F, 0.85F, 0xFFFFFFFF, true, true);
                }
            }
            // etiqueta de la tecla (esquina superior izquierda)
            String kk = a.hudKey();
            float ks = Math.min(0.55F, 18.0F / Math.max(1, font.width(kk)));
            g.fill(x + 1, y + 1, x + 2 + Math.round(font.width(kk) * ks) + 2, y + 1 + Math.round(8 * ks) + 2, 0xCC000000);
            text(g, font, kk, x + 3, y + 2, ks, 0xFFF6DFA0, false, false);
        }
        RenderSystem.disableBlend();
    }
}
