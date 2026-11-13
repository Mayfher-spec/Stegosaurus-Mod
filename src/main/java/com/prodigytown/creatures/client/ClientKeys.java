package com.prodigytown.creatures.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.prodigytown.creatures.ProdigyCreatures;
import com.prodigytown.creatures.StegosaurusEntity;
import com.prodigytown.creatures.net.ModNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Teclas: K abre el arbol del adulto o las ordenes del chibi, V embestida, B coletazo; montado: clic izquierdo muerde y clic derecho da el coletazo. */
public final class ClientKeys {
    private static final String CATEGORY = "key.categories.prodigycreatures";
    public static final KeyMapping OPEN = new KeyMapping("key.prodigycreatures.open_talents", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping CHARGE = new KeyMapping("key.prodigycreatures.charge", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping TAIL = new KeyMapping("key.prodigycreatures.tail", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping ULTIMATE = new KeyMapping("key.prodigycreatures.ultimate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping SHRINK = new KeyMapping("key.prodigycreatures.shrink_dragon", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping DILO_BITE = new KeyMapping("key.prodigycreatures.dilo_bite", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping DILO_CLAW = new KeyMapping("key.prodigycreatures.dilo_claw", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping DILO_POUNCE = new KeyMapping("key.prodigycreatures.dilo_pounce", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping DILO_FRENZY = new KeyMapping("key.prodigycreatures.dilo_frenzy", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    private ClientKeys() {}

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
            event.register(CHARGE);
            event.register(TAIL);
            event.register(ULTIMATE);
            event.register(SHRINK);
            event.register(DILO_BITE);
            event.register(DILO_CLAW);
            event.register(DILO_POUNCE);
            event.register(DILO_FRENZY);
        }
    }

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        /** Montado: clic izquierdo = mordida; clic derecho = coletazo (si la mano no lleva comida ni algo que se use, como un arco o un escudo). */
        @SubscribeEvent public static void rightClick(InputEvent.InteractionKeyMappingTriggered event) {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null) return;
            if (p.getVehicle() instanceof com.prodigytown.creatures.DilophosaurusEntity) { // Dilofosaurio montado: clic izq = mordida rapida, clic der = zarpazo doble
                if (event.isAttack()) {
                    event.setCanceled(true); event.setSwingHand(false);
                    ModNetwork.CHANNEL.sendToServer(new ModNetwork.UseAbility(10));
                    return;
                }
                if (!event.isUseItem()) return;
                ItemStack h = p.getItemInHand(event.getHand());
                if (!h.isEmpty() && (h.getItem().isEdible() || h.getUseAnimation() != UseAnim.NONE)) return;
                event.setCanceled(true); event.setSwingHand(false);
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.UseAbility(11));
                return;
            }
            if (!(p.getVehicle() instanceof StegosaurusEntity || p.getVehicle() instanceof com.prodigytown.creatures.CarnotaurusEntity)) return;
            if (event.isAttack()) { // clic izquierdo montado: mordida
                event.setCanceled(true);
                event.setSwingHand(false);
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.UseAbility(2));
                return;
            }
            if (!event.isUseItem()) return;
            ItemStack held = p.getItemInHand(event.getHand());
            if (!held.isEmpty() && (held.getItem().isEdible() || held.getUseAnimation() != UseAnim.NONE)) return;
            event.setCanceled(true);
            event.setSwingHand(false);
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.UseAbility(1));
        }
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null || mc.level == null) return;
            while (CHARGE.consumeClick()) ModNetwork.CHANNEL.sendToServer(new ModNetwork.UseAbility(0));
            while (TAIL.consumeClick()) ModNetwork.CHANNEL.sendToServer(new ModNetwork.UseAbility(1));
            while (ULTIMATE.consumeClick()) ModNetwork.CHANNEL.sendToServer(new ModNetwork.UseAbility(3));
            while (DILO_BITE.consumeClick()) ModNetwork.CHANNEL.sendToServer(new ModNetwork.UseAbility(10));
            while (DILO_CLAW.consumeClick()) ModNetwork.CHANNEL.sendToServer(new ModNetwork.UseAbility(11));
            while (DILO_POUNCE.consumeClick()) ModNetwork.CHANNEL.sendToServer(new ModNetwork.UseAbility(12));
            while (DILO_FRENZY.consumeClick()) ModNetwork.CHANNEL.sendToServer(new ModNetwork.UseAbility(13));
            while (SHRINK.consumeClick()) { // Saint's Dragons: H = encoger/agrandar el dragon que miras (Mayus+H = todos los tuyos cercanos)
                net.minecraft.world.entity.Entity look = mc.crosshairPickEntity;
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.ShrinkDragon(com.prodigytown.creatures.compat.SaintsDragonsCompat.isDragon(look) ? look.getId() : -1, player.isShiftKeyDown()));
            }
            while (OPEN.consumeClick()) {
                if (mc.screen != null) continue;
                net.minecraft.world.entity.TamableAnimal best = null;
                for (net.minecraft.world.entity.TamableAnimal s : mc.level.getEntitiesOfClass(net.minecraft.world.entity.TamableAnimal.class,
                        player.getBoundingBox().inflate(24.0), e -> (e instanceof StegosaurusEntity || e instanceof com.prodigytown.chibi.ChibiStego || e instanceof com.prodigytown.creatures.CarnotaurusEntity || e instanceof com.prodigytown.creatures.DilophosaurusEntity) && e.isTame() && e.isOwnedBy(player))) {
                    if (best == null || s.distanceToSqr(player) < best.distanceToSqr(player)) best = s;
                }
                if (best instanceof StegosaurusEntity adult) ClientHooks.openTalents(adult);
                else if (best instanceof com.prodigytown.chibi.ChibiStego chibi) ClientHooks.openOrders(chibi);
                else if (best instanceof com.prodigytown.creatures.CarnotaurusEntity carno) ClientHooks.openCarno(carno);
                else if (best instanceof com.prodigytown.creatures.DilophosaurusEntity dilo) ClientHooks.openDilo(dilo);
                else player.displayClientMessage(net.minecraft.network.chat.Component.literal("No hay un companero tuyo cerca"), true);
            }
        }
    }
}
