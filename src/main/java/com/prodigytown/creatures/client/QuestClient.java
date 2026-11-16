package com.prodigytown.creatures.client;

import com.prodigytown.creatures.quests.QuestPackets;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Estado de misiones en el cliente (lo manda el servidor) y apertura de la pantalla del Cronista. */
public final class QuestClient {
    private QuestClient() {}
    public static volatile List<QuestPackets.QuestView> views = List.of();
    /** Rastreador de misiones activas en el HUD (se activa/desactiva desde la pantalla). */
    public static boolean trackerOn = true;

    public static void onSync(QuestPackets.QuestSync m) {
        views = m.views();
        Minecraft mc = Minecraft.getInstance();
        if (m.npcId() >= 0) mc.setScreen(new QuestScreen());
        else if (mc.screen instanceof QuestScreen qs) qs.refresh();
    }
}
