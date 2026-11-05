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

/** Teclas: K abre el arbol, V embestida, B coletazo; montado: clic izquierdo muerde y clic derecho da el coletazo. */
public final class ClientKeys {
    private static final String CATEGORY = "key.categories.prodigycreatures";
    public static final KeyMapping OPEN = new KeyMapping("key.prodigycreatures.open_talents", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping CHARGE = new KeyMapping("key.prodigycreatures.charge", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping TAIL = new KeyMapping("key.prodigycreatures.tail", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    private ClientKeys() {}

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
            event.register(CHARGE);
            event.register(TAIL);
        }
    }

    @Mod.EventBusSubscriber(modid = ProdigyCreatures.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        /** Montado: clic izquierdo = mordida; clic derecho = coletazo (si la mano no lleva comida ni algo que se use, como un arco o un escudo). */
        @SubscribeEvent public static void rightClick(InputEvent.InteractionKeyMappingTriggered event) {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null || !(p.getVehicle() instanceof StegosaurusEntity)) return;
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
            while (OPEN.consumeClick()) {
                if (mc.screen != null) continue;
                List<StegosaurusEntity> pets = mc.level.getEntitiesOfClass(StegosaurusEntity.class,
                    player.getBoundingBox().inflate(24.0), s -> s.isTame() && s.isOwnedBy(player));
                StegosaurusEntity best = null;
                for (StegosaurusEntity s : pets) if (best == null || s.distanceToSqr(player) < best.distanceToSqr(player)) best = s;
                if (best != null) ClientHooks.openTalents(best);
                else player.displayClientMessage(net.minecraft.network.chat.Component.literal("No hay un estegosaurio tuyo cerca"), true);
            }
        }
    }
}
