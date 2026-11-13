package com.prodigytown.creatures.client;

import com.prodigytown.creatures.StegosaurusEntity;
import net.minecraft.client.Minecraft;

/** Punto de entrada solo-cliente (se invoca via DistExecutor para no cargar clases de cliente en servidor). */
public final class ClientHooks {
    private ClientHooks() {}
    public static void openOrders(com.prodigytown.chibi.ChibiStego chibi) {
        Minecraft.getInstance().setScreen(new OrdersScreen(chibi));
    }
    public static void openOrders(StegosaurusEntity stego) {
        Minecraft.getInstance().setScreen(new OrdersScreen(stego));
    }
    public static void openCarno(com.prodigytown.creatures.CarnotaurusEntity carno) {
        Minecraft.getInstance().setScreen(new CarnoScreen(carno));
    }
    public static void openDilo(com.prodigytown.creatures.DilophosaurusEntity dilo) {
        Minecraft.getInstance().setScreen(new DiloScreen(dilo));
    }
    public static void openTalents(StegosaurusEntity stego) {
        Minecraft.getInstance().setScreen(new TalentScreen(stego));
    }
}
