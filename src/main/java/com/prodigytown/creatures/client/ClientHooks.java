package com.prodigytown.creatures.client;

import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.client.Minecraft;

/** Punto de entrada solo-cliente (se invoca via DistExecutor para no cargar clases de cliente en servidor). */
public final class ClientHooks {
    private ClientHooks() {}
    public static void openTalents(StegosaurusEntity stego) {
        Minecraft.getInstance().setScreen(new TalentScreen(stego));
    }
}
