package com.prodigytown.creatures.client;

import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.quests.QuestData;
import com.prodigytown.creatures.quests.QuestPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Rastreador discreto de las misiones en curso (esquina izquierda); se puede apagar desde la pantalla del Cronista. */
public final class QuestTrackerHud {
    private QuestTrackerHud() {}

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class QuestHudModBus {
        @SubscribeEvent public static void overlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "quest_tracker", (gui, g, partial, w, h) -> render(g));
        }
    }

    private static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!QuestClient.trackerOn || mc.player == null || mc.options.hideGui || mc.options.renderDebug) return;
        List<QuestPackets.QuestView> act = new ArrayList<>();
        for (QuestPackets.QuestView v : QuestClient.views) if (v.state() == QuestData.ACTIVE || v.state() == QuestData.READY) act.add(v);
        if (act.isEmpty()) return;
        act.sort((a, b) -> Integer.compare(b.state(), a.state()));   // las listas arriba
        Font font = mc.font;
        final float sc = 0.8F;
        int n = Math.min(4, act.size());
        int bw = 112;
        g.pose().pushPose();
        g.pose().translate(4.0F, 30.0F, 0.0F);
        g.pose().scale(sc, sc, 1.0F);
        g.fill(0, 0, bw, 12 + n * 20, 0x80000000);
        g.fill(0, 0, 2, 12 + n * 20, 0xFF6FA84A);
        g.drawString(font, "Misiones" + (act.size() > n ? " (+" + (act.size() - n) + ")" : ""), 5, 2, 0xFFFFD35A, false);
        for (int i = 0; i < n; i++) {
            QuestPackets.QuestView v = act.get(i);
            boolean ready = v.state() == QuestData.READY;
            g.drawString(font, font.plainSubstrByWidth(v.title(), bw - 10), 5, 13 + i * 20, 0xFFEDE6D0, false);
            g.drawString(font, ready ? "Lista: habla con el Cronista" : v.progress() + " / " + v.target(), 5, 22 + i * 20, ready ? 0xFFFFD35A : 0xFF66C7E8, false);
        }
        g.pose().popPose();
    }
}
